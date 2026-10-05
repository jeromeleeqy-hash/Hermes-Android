package com.qingyu.hermescompanion.today

import com.qingyu.hermescompanion.data.HermesApiClient
import com.qingyu.hermescompanion.model.WorkspaceDocument
import com.qingyu.hermescompanion.model.WorkspaceEntry
import com.qingyu.hermescompanion.model.WorkspaceListing
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.mockito.Mockito.*
import java.io.File

class TodayInteractionTest {
    private fun raw() = File("src/main/assets/hermes-today-interactions.json").readText()
    private fun cards() = TodayBoard.decode(raw(), "/work").cards
    private fun card(type: TodayLayout) = cards().single { it.presentation.interaction?.type == type }
    private fun validInput(c: TodayCard): JSONObject = JSONObject(initialTodayInput(c)).apply {
        val i = c.presentation.interaction!!
        if (i.type in setOf(TodayLayout.CLARIFICATION, TodayLayout.COMPARISON, TodayLayout.TIME_PICKER)) put("selection", i.items.first().id)
        if (i.type == TodayLayout.MILESTONES) put("reviewed", true)
        if (i.type == TodayLayout.DELIVERABLE) put("operation", "adopt")
        if (i.type == TodayLayout.CHANGE) put("operation", "keep")
        if (i.type == TodayLayout.MEMORY_CHANGE) put("operation", "undo")
        if (i.type in setOf(TodayLayout.BLOCKER, TodayLayout.TIMELINE)) put("note", "用户补充的具体事实")
    }

    @Test fun allTwentyFixturesRoundTripIntoScopedActionsWithoutMutatingRecords() {
        val all = cards()
        assertEquals(TodayLayout.entries.toSet(), all.map { it.presentation.interaction!!.type }.toSet())
        all.forEach { c ->
            val snapshot = c.contextDocument()
            val request = JSONObject(todayInteractionRequest(c, validInput(c).toString(), "test-${c.id}"))
            assertEquals(c.id, request.getString("card_id"))
            assertEquals(c.interactionFingerprint, request.getString("expected_card"))
            assertEquals(snapshot, c.contextDocument())
            assertEquals("open", c.status)
        }
    }

    @Test fun requiredInputsClosedCardsAndUnrecognizedSelectionsAreRejected() {
        val clarify = card(TodayLayout.CLARIFICATION)
        assertThrows(IllegalArgumentException::class.java) { todayInteractionRequest(clarify, initialTodayInput(clarify)) }
        assertThrows(IllegalArgumentException::class.java) { todayInteractionRequest(clarify.copy(status = "done"), validInput(clarify).toString()) }
        assertThrows(IllegalArgumentException::class.java) { todayInteractionRequest(clarify, validInput(clarify).put("selection", "invented-id").toString()) }
        val log = card(TodayLayout.QUICK_LOG)
        listOf("NaN", "Infinity", "-1", "1441", "").forEach { invalid ->
            val input = validInput(log); input.getJSONObject("fields").put("minutes", invalid)
            assertThrows(IllegalArgumentException::class.java) { todayInteractionRequest(log, input.toString()) }
        }
        val rank = card(TodayLayout.PRIORITIES)
        assertThrows(IllegalArgumentException::class.java) { todayInteractionRequest(rank, validInput(rank).put("order", JSONArray(listOf("decision"))).toString()) }
    }

    @Test fun inputsCannotAddCommandsOrNewFieldTargetsAndReflectionDoesNotCarryCompletedWork() {
        val c = card(TodayLayout.QUICK_LOG)
        val draft = validInput(c).put("shell", "untrusted command")
        draft.getJSONObject("fields").put("path", "/outside")
        val result = normalizeTodayInput(c, draft.toString())
        assertFalse(result.has("shell")); assertFalse(result.getJSONObject("fields").has("path"))
        assertEquals(0, JSONObject(initialTodayInput(card(TodayLayout.REFLECTION))).getJSONArray("selected_ids").length())
    }

    @Test fun unsupportedOrMalformedOptionalLayoutKeepsLegacyContentAndContext() {
        listOf("unknown_type", "bad_version", "duplicate_id", "too_many_fields").forEach { problem ->
            val root = JSONObject(raw()); val source = root.getJSONArray("cards").getJSONObject(0)
            val i = source.getJSONObject("presentation").getJSONObject("interaction")
            when (problem) {
                "unknown_type" -> i.put("type", "execute_arbitrary_code")
                "bad_version" -> i.put("version", 99)
                "duplicate_id" -> i.getJSONArray("items").getJSONObject(1).put("id", "same")
                else -> i.put("fields", JSONArray((1..6).map { JSONObject().put("id", "f$it").put("kind", "text").put("label", "Field") }))
            }
            val parsed = TodayBoard.decode(root.toString(), "/work").cards.first()
            assertNull(parsed.presentation.interaction)
            assertNotNull(parsed.presentation.issue)
            assertTrue(parsed.presentation.options.isNotEmpty())
            assertEquals(source.getString("summary"), parsed.summary)
            assertTrue(parsed.contextDocument().contains("invalid_presentation_read_only"))
        }
    }

    @Test fun submissionRereadsConfiguredWorkspaceAndRejectsChangedSourceOrScope() {
        val client = mock(HermesApiClient::class.java)
        val repo = TodayRepository(client)
        val c = cards().first()
        fun stub(root: String, content: String) {
            doAnswer { throw com.qingyu.hermescompanion.data.ApiException(404, "Missing directory") }.`when`(client)
                .listWorkspaceForProfile("$root/.hermes-app/today", "personal")
            `when`(client.initialWorkspaceForProfile("personal")).thenReturn(WorkspaceListing(path = root, entries = listOf(WorkspaceEntry(TodayBoard.FILE, "$root/${TodayBoard.FILE}", false))))
            `when`(client.readWorkspaceDocumentForProfile("$root/${TodayBoard.FILE}", "personal")).thenReturn(WorkspaceDocument(TodayBoard.FILE, "$root/${TodayBoard.FILE}", "application/json", content))
        }
        stub("/work", raw())
        assertEquals(todayFingerprint(raw()), repo.verifySubmission("personal", "/work", c))
        val changed = JSONObject(raw()); changed.getJSONArray("cards").getJSONObject(0).put("summary", "资料发生了变化")
        stub("/work", changed.toString())
        assertThrows(IllegalArgumentException::class.java) { repo.verifySubmission("personal", "/work", c) }
        stub("/other", raw())
        assertThrows(IllegalArgumentException::class.java) { repo.verifySubmission("personal", "/work", c) }
        verify(client, never()).listWorkspaceForProfile(isNull(), anyString())
        verify(client, never()).saveWorkspaceDocumentForProfile(anyString(), anyString(), anyString())
    }

    @Test fun schedulesValidateWallTimesAndExplicitTimezone() {
        assertEquals("Asia/Shanghai", todayScheduleConfig("09:00", "21:00", "Asia/Shanghai").getString("timezone"))
        assertThrows(IllegalArgumentException::class.java) { todayScheduleConfig("24:00", "21:00", "Asia/Shanghai") }
        assertThrows(IllegalArgumentException::class.java) { todayScheduleConfig("9:00", "21:00", "Asia/Shanghai") }
        assertThrows(IllegalArgumentException::class.java) { todayScheduleConfig("09:00", "09:00", "Asia/Shanghai") }
        assertThrows(IllegalArgumentException::class.java) { todayScheduleConfig("09:00", "21:00", "invalid-zone") }
    }
}

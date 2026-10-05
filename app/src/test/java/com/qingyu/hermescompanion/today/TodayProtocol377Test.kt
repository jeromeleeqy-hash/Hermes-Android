package com.qingyu.hermescompanion.today

import com.qingyu.hermescompanion.model.*
import org.json.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class TodayProtocol377Test {
    private fun samples() = JSONObject(File("src/main/assets/hermes-today-interactions.json").readText())
    @Test fun oldParagraphCardsStayReadableButCannotMasqueradeAsStructuredCards() {
        val source = samples().put("presentation_version", 2)
        val card = source.getJSONArray("cards").getJSONObject(0)
        card.getJSONObject("presentation").remove("interaction")
        card.getJSONObject("presentation").remove("layout")
        val result = TodayBoard.decode(source.toString(), "/work").cards.first()
        assertTrue(result.needsStructure)
        assertNull(result.presentation.interaction)
        assertEquals(card.getString("summary"), result.summary)
        source.put("presentation_version", 3)
        assertNotNull(TodayBoard.decode(source.toString(), "/work").cards.first().presentation.issue)
    }
    @Test fun allShippedTypesSatisfyStrictSchemaAndKeepContextSeparateFromBackground() {
        val result = TodayBoard.decode(samples().toString(), "/work")
        assertEquals(4, result.presentationVersion)
        assertEquals(20, result.cards.mapNotNull { it.presentation.interaction?.type }.distinct().size)
        result.cards.forEach { assertFalse(it.needsStructure); assertTrue(it.readableContext.length in 1..100) }
    }
    @Test fun receiptMustMatchBothOperationAndCardAndSurvivesRestartWithoutResending() {
        val a = TodayActionState("op-1", "one", "default", "/work", "{}", "session-1", "running", "Processing")
        assertEquals("uncertain", decodeTodayActions(encodeTodayActions(listOf(a))).getValue(a.key).status)
        assertNull(confirmedTodayAction("""{"summary":"done"}""", a))
        val wrong = """{"action_receipts":[{"operation_id":"op-1","card_id":"other","status":"applied","message":"Updated"}]}"""
        assertNull(confirmedTodayAction(wrong, a))
        assertEquals("Updated", confirmedTodayAction(wrong.replace("other", "one"), a))
    }
    @Test fun journalKeepsPendingActionsWhenTrimmingOldCompletions() {
        val pending = TodayActionState("op", "p", "default", "/work", "{}", status = "uncertain")
        val done = (1..90).map { pending.copy(operationId = "op-$it", cardId = "c$it", status = "applied") }
        val read = decodeTodayActions(encodeTodayActions(listOf(pending) + done))
        assertEquals(80, read.size); assertTrue(read.getValue(pending.key).pending)
    }
    @Test fun scheduleVerificationRequiresRealMatchingEnabledJobs() {
        val marker = "hermes-app-today:" + todayFingerprint("default\n/work").take(20)
        val settings = JSONObject().put("profile", "default").put("workspace", "/work").put("schedule_marker", marker)
            .put("contract_version", "3.7.7").put("morning", "09:00").put("evening", "21:00").put("timezone", "Asia/Shanghai")
            .put("cron_ids", JSONObject().put("morning", "m").put("evening", "e"))
        fun job(id: String, period: String) = CronJob(id, period, "$marker /work/.hermes-app/today/ $period", CronSchedule(expression = "0 9 * * *"), nextRunAt = "2026-10-05T09:00:00+08:00")
        val jobs = listOf(job("m", "morning"), job("e", "evening"))
        assertTrue(verifyTodaySchedule(settings.toString(), jobs, "default", "/work").verified)
        settings.put("contract_version", "3.8.4")
        assertTrue(verifyTodaySchedule(settings.toString(), jobs, "default", "/work").verified)
        assertFalse(verifyTodaySchedule(settings.toString(), emptyList(), "default", "/work").verified)
        assertFalse(verifyTodaySchedule(settings.toString(), listOf(jobs[0].copy(enabled = false), jobs[1]), "default", "/work").verified)
        assertThrows(Exception::class.java) { verifyTodaySchedule(settings.toString(), jobs, "other", "/work") }
    }
}

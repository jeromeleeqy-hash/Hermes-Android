package com.qingyu.hermescompanion.today

import com.qingyu.hermescompanion.data.*
import com.qingyu.hermescompanion.model.*
import org.junit.Assert.*
import org.junit.Test
import org.mockito.Mockito.*

class TodayStorage383Test {
    private val client = mock(HermesApiClient::class.java)
    private val root = "/work"
    private val modern = "$root/${TodayBoard.PATH}"
    private val legacy = "$root/${TodayBoard.FILE}"
    private val directory = "$root/${TodayBoard.DIRECTORY}"
    private val raw = """{"schema_version":1,"date":"2026-10-05","generated_at":"2026-10-05T10:00:00+08:00","headline":"One","summary":"","cards":[]}"""
    private fun rootListing(old: Boolean = true) = WorkspaceListing(path = root,
        entries = if (old) listOf(WorkspaceEntry(TodayBoard.FILE, legacy, false, size = raw.length.toLong(), modifiedAt = 12.0)) else emptyList())
    private fun arrange() {
        `when`(client.initialWorkspaceForProfile("work")).thenReturn(rootListing())
        `when`(client.readWorkspaceDocumentForProfile(legacy, "work")).thenReturn(WorkspaceDocument(TodayBoard.FILE, legacy, "application/json", raw))
    }
    private fun newFile(value: String = raw) {
        `when`(client.listWorkspaceForProfile(directory, "work")).thenReturn(WorkspaceListing(path = directory,
            entries = listOf(WorkspaceEntry(TodayBoard.FILE, modern, false, size = value.length.toLong(), modifiedAt = 12.0))))
        `when`(client.readWorkspaceDocumentForProfile(modern, "work")).thenReturn(WorkspaceDocument(TodayBoard.FILE, modern, "application/json", value))
    }
    @Test fun confirmedMissingDirectoryUsesLegacy() {
        arrange()
        doAnswer { throw ApiException(404, "missing") }.`when`(client).listWorkspaceForProfile(directory, "work")
        val result = TodayRepository(client).load("work")
        assertNull(result.error); assertEquals(legacy, result.filePath); assertNotNull(result.board)
    }
    @Test fun permissionAndTransportFailuresNeverMasqueradeAsMissing() {
        arrange()
        for (error in listOf(ApiException(403, "denied"), java.io.IOException("offline"))) {
            doAnswer { throw error }.`when`(client).listWorkspaceForProfile(directory, "work")
            assertNotNull(TodayRepository(client).load("work").error)
        }
        verify(client, never()).readWorkspaceDocumentForProfile(legacy, "work")
    }
    @Test fun corruptModernFileDoesNotFallBackToOldFacts() {
        arrange(); newFile("broken json")
        val result = TodayRepository(client).load("work")
        assertNotNull(result.error); assertTrue(result.fileExists); assertEquals(modern, result.filePath)
        verify(client, never()).readWorkspaceDocumentForProfile(legacy, "work")
    }
    @Test fun conflictingCopiesAreReportedWithoutClaimingLatest() {
        arrange(); newFile(raw.replace("One", "Two"))
        val result = TodayRepository(client).load("work")
        assertNotNull(result.error); assertNull(result.board)
    }
    @Test fun migratedPathForcesReadEvenWithIdenticalMetadata() {
        arrange()
        doAnswer { throw ApiException(404, "missing") }.`when`(client).listWorkspaceForProfile(directory, "work")
        val repository = TodayRepository(client) { 1_000L }
        val before = repository.load("work")
        `when`(client.initialWorkspaceForProfile("work")).thenReturn(rootListing(false))
        doReturn(WorkspaceListing(path = directory, entries = listOf(WorkspaceEntry(TodayBoard.FILE, modern, false,
            size = raw.length.toLong(), modifiedAt = 12.0)))).`when`(client).listWorkspaceForProfile(directory, "work")
        `when`(client.readWorkspaceDocumentForProfile(modern, "work")).thenReturn(WorkspaceDocument(TodayBoard.FILE, modern, "application/json", raw.replace("One", "Two")))
        val after = repository.load("work", before, forceRead = false)
        assertNull(after.error); assertEquals("Two", after.board!!.headline); assertEquals(modern, after.filePath)
    }
    @Test fun refreshReceiptMustMatchThisRequestAndStatus() {
        assertFalse(confirmedTodayRefresh("""{"refresh_receipts":[{"request_id":"old","status":"applied"}]}""", "new"))
        assertFalse(confirmedTodayRefresh("""{"refresh_receipts":[{"request_id":"new","status":"running"}]}""", "new"))
        assertTrue(confirmedTodayRefresh("""{"refresh_receipts":[{"request_id":"new","status":"applied"}]}""", "new"))
    }
    @Test fun lostSessionIsNotInferredFromNetworkOrPermissionErrors() {
        assertTrue(isMissingTodaySession(ApiException(404, "Session not found")))
        assertTrue(isMissingTodaySession(ApiException(-32000, "Session not found")))
        assertFalse(isMissingTodaySession(ApiException(403, "Session not found")))
        assertFalse(isMissingTodaySession(java.io.IOException("Session not found")))
    }
    @Test fun evidenceCannotCrossProfileOrWorkspaceAndExcludesTools() {
        val good = HermesSession("one", "Recent", profile = "work", workspacePath = root)
        val candidates = overviewConversationCandidates(listOf(good, good.copy(id="two", profile="other"),
            good.copy(id="three", workspacePath="/elsewhere")), "work", root, emptySet(), emptySet())
        assertEquals(listOf(good), candidates)
        val daily = good.copy(id="daily", workspacePath="")
        assertEquals(listOf(daily), overviewConversationCandidates(listOf(daily), "work", root, setOf("daily"), emptySet()))
        assertTrue(overviewConversationCandidates(listOf(daily), "work", root, emptySet(), emptySet()).isEmpty())
        assertTrue(overviewConversationCandidates(listOf(daily.copy(workspacePath="/elsewhere")), "work", root, setOf("daily"), emptySet()).isEmpty())
        val evidence = overviewConversationEvidence(good, listOf(ChatMessage(role=MessageRole.USER,content="已处理完成"),
            ChatMessage(role=MessageRole.TOOL,content="internal log"), ChatMessage(role=MessageRole.SYSTEM,content="instructions")))
        assertEquals(1, evidence.getJSONArray("messages").length())
    }
}

package com.qingyu.hermescompanion.today

import com.qingyu.hermescompanion.data.HermesApiClient
import com.qingyu.hermescompanion.model.*
import org.junit.Assert.*
import org.junit.Test
import org.mockito.Mockito.*

class TodayRepositoryTest {
    private val client = mock(HermesApiClient::class.java)
    @org.junit.Before fun legacyStorage() {
        doAnswer { throw com.qingyu.hermescompanion.data.ApiException(404, "Missing directory") }.`when`(client)
            .listWorkspaceForProfile(org.mockito.ArgumentMatchers.endsWith("/.hermes-app/today"), org.mockito.ArgumentMatchers.anyString())
    }
    private val repo = TodayRepository(client)
    @Test fun homeReadOnlyListsRootAndReadsOverviewWithExplicitProfile() {
        val json = """{"schema_version":1,"date":"2026-10-03","generated_at":"2026-10-03T09:00:00+08:00","headline":"Today","summary":"","cards":[]}"""
        `when`(client.initialWorkspaceForProfile("work")).thenReturn(WorkspaceListing(path = "/work", entries = listOf(
            WorkspaceEntry("people", "/work/people", true), WorkspaceEntry(TodayBoard.FILE, "/work/${TodayBoard.FILE}", false))))
        `when`(client.readWorkspaceDocumentForProfile("/work/${TodayBoard.FILE}", "work")).thenReturn(WorkspaceDocument(TodayBoard.FILE, "/work/${TodayBoard.FILE}", "application/json", json))
        val result = repo.load("work")
        assertNotNull(result.board); assertEquals("people", result.library.single().name)
        verify(client).initialWorkspaceForProfile("work"); verify(client).readWorkspaceDocumentForProfile("/work/${TodayBoard.FILE}", "work"); verify(client).listWorkspaceForProfile("/work/.hermes-app/today", "work"); verifyNoMoreInteractions(client)
    }
    @Test fun readFailureIsDifferentFromMissingFile() {
        `when`(client.initialWorkspaceForProfile("work")).thenReturn(WorkspaceListing(path = "/work", entries = listOf(WorkspaceEntry(TodayBoard.FILE, "/work/${TodayBoard.FILE}", false))))
        `when`(client.readWorkspaceDocumentForProfile("/work/${TodayBoard.FILE}", "work")).thenThrow(IllegalStateException("offline"))
        val result = repo.load("work")
        assertTrue(result.fileExists); assertEquals("offline", result.error); assertNull(result.board)
    }
    @Test fun failedFolderCountStaysUnknownAndNeverZero() {
        `when`(client.listWorkspaceForProfile("/work/people", "work")).thenThrow(IllegalStateException("denied"))
        val result = repo.countEntries(TodayState(profile = "work", root = "/work", library = listOf(TodayLibraryEntry("people", "/work/people", true))))
        assertNull(result.single().count)
    }
    @Test fun wrongReadResponseIsRejectedBeforeParsing() {
        `when`(client.initialWorkspaceForProfile("work")).thenReturn(WorkspaceListing(path = "/work", entries = listOf(WorkspaceEntry(TodayBoard.FILE, "/work/${TodayBoard.FILE}", false))))
        `when`(client.readWorkspaceDocumentForProfile("/work/${TodayBoard.FILE}", "work")).thenReturn(WorkspaceDocument(TodayBoard.FILE, "/outside/${TodayBoard.FILE}", "application/json", "{}"))
        assertEquals("Unexpected response path", repo.load("work").error)
    }
    @Test fun pollingKeepsKnownFolderCountsButNewFoldersRequireRecounting() {
        val old = TodayState(profile = "work", root = "/work", countsLoaded = true,
            library = listOf(TodayLibraryEntry("people", "/work/people", true, 12)))
        val fresh = old.copy(countsLoaded = false, library = old.library.map { it.copy(count = null) })
        assertTrue(retainTodayCounts(fresh, old).countsLoaded)
        assertEquals(12, retainTodayCounts(fresh, old).library.single().count)
        assertFalse(retainTodayCounts(fresh.copy(library = emptyList()), old).countsLoaded)
        assertFalse(retainTodayCounts(fresh.copy(root = "/different"), old).countsLoaded)
    }
    @Test fun unchangedMetadataSkipsBodyButManualAndPeriodicReadsRevalidate() {
        var now = 1_000L
        val repo = TodayRepository(client) { now }
        val json = """{"schema_version":1,"date":"2026-10-03","generated_at":"2026-10-03T09:00:00+08:00","headline":"Today","summary":"","cards":[]}"""
        val path = "/work/${TodayBoard.FILE}"
        fun listing(modified: Double = 10.0, root: String = "/work") = WorkspaceListing(path = root, entries = listOf(
            WorkspaceEntry(TodayBoard.FILE, "$root/${TodayBoard.FILE}", false, size = json.toByteArray().size.toLong(), modifiedAt = modified)))
        `when`(client.initialWorkspaceForProfile("work")).thenReturn(listing())
        `when`(client.readWorkspaceDocumentForProfile(path, "work")).thenReturn(WorkspaceDocument(TodayBoard.FILE, path, "application/json", json))
        val first = repo.load("work")
        assertNull(first.error); assertNotNull(first.board)
        now += 60_000
        val unchanged = repo.load("work", first, false)
        assertSame(first.board, unchanged.board)
        verify(client, times(1)).readWorkspaceDocumentForProfile(path, "work")
        repo.load("work", unchanged, true)
        verify(client, times(2)).readWorkspaceDocumentForProfile(path, "work")
        now = 301_000
        repo.load("work", unchanged, false)
        verify(client, times(3)).readWorkspaceDocumentForProfile(path, "work")
        now = 65_000
        `when`(client.initialWorkspaceForProfile("work")).thenReturn(listing(11.0))
        repo.load("work", first, false)
        verify(client, times(4)).readWorkspaceDocumentForProfile(path, "work")
        `when`(client.initialWorkspaceForProfile("work")).thenReturn(listing(0.0))
        repo.load("work", first, false)
        verify(client, times(5)).readWorkspaceDocumentForProfile(path, "work")
        `when`(client.initialWorkspaceForProfile("work")).thenReturn(listing(root = "/other"))
        `when`(client.readWorkspaceDocumentForProfile("/other/${TodayBoard.FILE}", "work")).thenReturn(WorkspaceDocument(TodayBoard.FILE, "/other/${TodayBoard.FILE}", "application/json", json))
        val changedRoot = repo.load("work", first, false)
        assertEquals("/other", changedRoot.root)
        assertNotSame(first.board, changedRoot.board)
        verify(client).readWorkspaceDocumentForProfile("/other/${TodayBoard.FILE}", "work")
    }
}

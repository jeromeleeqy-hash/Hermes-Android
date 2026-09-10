package com.qingyu.hermescompanion.assistant

import com.qingyu.hermescompanion.data.ApiException
import com.qingyu.hermescompanion.data.HermesApiClient
import com.qingyu.hermescompanion.model.HermesSession
import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mockito.*

class DailyConversationTest {
    private val client = mock(HermesApiClient::class.java)
    private val daily = HermesSession("daily", DailyConversation.TITLE, profile = "work", messageCount = 6, workspacePath = "/original")

    @Test fun savedConversationKeepsItsWorkspaceAndDoesNotCreateAnother() {
        `when`(client.sessionForProfile("daily", "work")).thenReturn(daily)
        var remembered = ""
        assertEquals(daily, DailyConversation.resolve(client, "work", "daily") { remembered = it.id })
        assertEquals("daily", remembered)
        verify(client, never()).findSessionByTitleForProfile(anyString(), anyString())
        verify(client, never()).createSessionForProfile(any(), anyString())
    }
    @Test fun reinstallFindsExistingServerConversationWithoutLocalBinding() {
        `when`(client.findSessionByTitleForProfile(DailyConversation.TITLE, "work")).thenReturn(daily)
        assertEquals(daily, DailyConversation.resolve(client, "work", null) {})
        verify(client, never()).createSessionForProfile(any(), anyString())
    }
    @Test fun onlyMissingBindingFallsBackToSearch() {
        `when`(client.sessionForProfile("deleted", "work")).thenAnswer { throw ApiException(404, "gone") }
        `when`(client.findSessionByTitleForProfile(DailyConversation.TITLE, "work")).thenReturn(daily)
        assertEquals(daily, DailyConversation.resolve(client, "work", "deleted") {})
        `when`(client.sessionForProfile("offline", "work")).thenAnswer { throw ApiException(503, "offline") }
        assertThrows(ApiException::class.java) { DailyConversation.resolve(client, "work", "offline") {} }
        verify(client, times(1)).findSessionByTitleForProfile(anyString(), anyString())
        verify(client, never()).createSessionForProfile(any(), anyString())
    }
    @Test fun newConversationUsesServerDefaultAndRemembersIdBeforeRenameFailure() {
        val created = daily.copy(id = "new", title = "新会话", runtimeId = "runtime", messageCount = 0)
        `when`(client.createSessionForProfile(null, "work")).thenReturn(created)
        `when`(client.renameSessionForProfile("new", DailyConversation.TITLE, "work")).thenAnswer { throw ApiException(503, "lost acknowledgement") }
        var remembered: String? = null
        assertThrows(ApiException::class.java) { DailyConversation.resolve(client, "work", null) { remembered = it.id } }
        assertEquals("new", remembered)
        `when`(client.sessionForProfile("new", "work")).thenReturn(created.copy(title = DailyConversation.TITLE))
        assertEquals("new", DailyConversation.resolve(client, "work", remembered) {}.id)
        verify(client, times(1)).createSessionForProfile(null, "work")
    }
    @Test fun emptyLiveConversationMayWaitUntilFirstMessageForServerTitle() {
        val created = daily.copy(title = "新会话", runtimeId = "runtime", messageCount = 0)
        `when`(client.createSessionForProfile(null, "work")).thenReturn(created)
        `when`(client.renameSessionForProfile("daily", DailyConversation.TITLE, "work")).thenAnswer { throw ApiException(404, "not stored yet") }
        val result = DailyConversation.resolve(client, "work", null) {}
        assertEquals("runtime", result.runtimeId)
        assertEquals(DailyConversation.TITLE, result.title)
    }
    @Test fun wrongProfileCannotBecomeTheDailyBinding() {
        `when`(client.findSessionByTitleForProfile(DailyConversation.TITLE, "work")).thenReturn(daily.copy(profile = "personal"))
        var remembered = false
        assertThrows(IllegalArgumentException::class.java) { DailyConversation.resolve(client, "work", null) { remembered = true } }
        assertFalse(remembered)
    }
}

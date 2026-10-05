package com.qingyu.hermescompanion.data

import com.qingyu.hermescompanion.assistant.AssistantPrompts
import com.qingyu.hermescompanion.model.*
import org.json.JSONObject
import org.junit.Test
import org.junit.Assert.*

class AppTaskConversationsTest {
    private val session = HermesSession("task", "整理首页", profile = "work", workspacePath = "/work")
    private fun messages(mode: String = "refresh_overview_only", profile: String = "work", root: String = "/work"): List<ChatMessage> {
        val contract = JSONObject().put("mode", mode).put("profile", profile).put("workspace", root)
        return listOf(ChatMessage(role = MessageRole.USER, content = AssistantPrompts.envelope("整理一下", "附件：hermes-today-request.json\n$contract")))
    }
    @Test fun explicitAppTaskEnvelopeIsRecognizedAcrossSupportedModes() {
        appTaskModes.forEach { assertTrue(it, isLegacyAppTask(session, messages(it))) }
    }
    @Test fun ordinaryChatAndUserProgressDiscussionAreNeverClassifiedByTitle() {
        assertFalse(isLegacyAppTask(session, listOf(ChatMessage(role=MessageRole.USER, content="帮我整理一下首页待办"))))
        assertFalse(isLegacyAppTask(session, messages("update_selected_record")))
        assertFalse(isTaskConversation(session, emptySet()))
        assertTrue(isTaskConversation(session, setOf(session.scopedId)))
        assertFalse(isTaskConversation(session.copy(profile="personal"), setOf(session.scopedId)))
    }
    @Test fun malformedEnvelopeOrMismatchedScopeDoesNotHideAConversation() {
        assertFalse(isLegacyAppTask(session, messages(profile="personal")))
        assertFalse(isLegacyAppTask(session, messages(root="/other")))
        assertFalse(isLegacyAppTask(session, messages(root="relative")))
        val broken = messages().map { it.copy(content=it.content.replace("<!-- /hermes-mobile-context-v1 -->", "")) }
        assertFalse(isLegacyAppTask(session, broken))
    }
}

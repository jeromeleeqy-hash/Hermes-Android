package com.qingyu.hermescompanion.ui

import com.qingyu.hermescompanion.model.ChatMessage
import com.qingyu.hermescompanion.model.MessageRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StreamRecoveryTest {
    @Test
    fun replyNeedsNoAttentionWhileViewingItsChatInForeground() {
        assertFalse(replyNeedsAttention(AppRoute.CHAT, "session-1", "session-1", appInForeground = true))
    }

    @Test
    fun replyNeedsAttentionAfterReturningToSessionList() {
        assertTrue(replyNeedsAttention(AppRoute.SESSIONS, null, "session-1", appInForeground = true))
    }

    @Test
    fun voiceConversationDoesNotNotifyWhileOpen() {
        assertFalse(replyNeedsAttention(AppRoute.VOICE_CHAT, "session-1", "session-1", appInForeground = true))
    }

    @Test
    fun replyNeedsAttentionWhenAppIsInBackground() {
        assertTrue(replyNeedsAttention(AppRoute.CHAT, "session-1", "session-1", appInForeground = false))
    }

    @Test
    fun replyNeedsAttentionWhileViewingAnotherChat() {
        assertTrue(replyNeedsAttention(AppRoute.CHAT, "session-2", "session-1", appInForeground = true))
    }

    @Test
    fun findsReplyPersistedAfterInterruptedPrompt() {
        val previous = ChatMessage(
            role = MessageRole.ASSISTANT,
            content = "上一条回复",
            createdAt = "2026-08-02T10:00:00Z",
        )
        val recovered = ChatMessage(
            role = MessageRole.ASSISTANT,
            content = "服务器继续生成完成的回复",
            createdAt = "2026-08-02T10:01:10Z",
        )
        val messages = listOf(
            ChatMessage(role = MessageRole.USER, content = "上一条问题"),
            previous,
            ChatMessage(role = MessageRole.USER, content = "帮我分析行业方向"),
            recovered,
        )

        assertEquals(
            recovered,
            findRecoveredAssistant(messages, "帮我分析行业方向", previous.recoverySignature()),
        )
    }

    @Test
    fun doesNotMistakePreviousReplyForRecoveredReply() {
        val previous = ChatMessage(role = MessageRole.ASSISTANT, content = "上一条回复")
        val messages = listOf(
            ChatMessage(role = MessageRole.USER, content = "上一条问题"),
            previous,
            ChatMessage(role = MessageRole.USER, content = "尚未生成的新问题"),
        )

        assertNull(
            findRecoveredAssistant(messages, "尚未生成的新问题", previous.recoverySignature()),
        )
    }
}

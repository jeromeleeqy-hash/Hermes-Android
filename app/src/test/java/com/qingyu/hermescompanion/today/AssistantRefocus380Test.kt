package com.qingyu.hermescompanion.today

import com.qingyu.hermescompanion.assistant.AssistantPrompts
import com.qingyu.hermescompanion.data.*
import com.qingyu.hermescompanion.model.*
import com.qingyu.hermescompanion.ui.screen.taskScheduleLabel
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class AssistantRefocus380Test {
    private fun brief(version: Int = 4, kind: String = "followup", layout: String = "note") = """{
      "schema_version":1,"presentation_version":$version,"editorial_version":2,
      "date":"2026-10-04","generated_at":"2026-10-04T14:00:00+08:00","headline":"今天的重点","summary":"",
      "cards":[{"id":"plan","title":"方案还差分工确认","summary":"方案方向已确定，负责人尚未确认。", "kind":"$kind",
      "attention":{"group":"followup","reason":"用户已明确要求继续跟进。","priority":1},
      "presentation":{"layout":"$layout","caption":"方案方向已确定，负责人尚未确认。"}}]}"""

    @Test fun followupAndDecisionCanBeReadableWithoutInventingInteractiveOptions() {
        listOf("followup", "decision").forEach { kind ->
            val card = TodayBoard.decode(brief(kind = kind), "/work").cards.single()
            assertNull(card.presentation.issue)
            assertNull(card.presentation.interaction)
            assertFalse(card.needsStructure)
            assertEquals(TodayAttentionGroup.FOLLOWUP, card.attentionGroup)
            assertEquals(1, card.attention?.priority)
            assertEquals("followup", JSONObject(card.contextDocument()).getJSONObject("attention").getString("group"))
        }
    }
    @Test fun legacyInteractiveSchemaStillDetectsInvalidActionsAndNewUnknownLayoutsStayReadOnly() {
        assertNotNull(TodayBoard.decode(brief(version = 3), "/work").cards.single().presentation.issue)
        assertNotNull(TodayBoard.decode(brief(layout = "auto_execute"), "/work").cards.single().presentation.issue)
        val schedule = brief(layout = "schedule")
        assertNotNull(TodayBoard.decode(schedule, "/work").cards.single().presentation.issue)
    }
    @Test fun explicitRelevanceControlsOrderAndReadingNeverChangesBusinessState() {
        val normal = TodayBoard.decode(brief(), "/work").cards.single().copy(id = "normal", attention = null)
        val important = normal.copy(id = "important", attention = TodayAttention(TodayAttentionGroup.FOCUS, "An explicit priority", 1))
        val closed = important.copy(id = "closed", status = "done")
        assertEquals(listOf("important", "normal"), focusCards(listOf(normal, closed, important)).map { it.id })
        assertEquals(listOf("important", "normal", "closed"), focusCards(listOf(normal, closed, important), true).map { it.id })
        assertEquals("open", normal.status)
    }
    @Test fun referenceListBelongsToCurrentTurnIsDeduplicatedAndOmitsOnlyKnownInternalFiles() {
        val messages = listOf(
            ChatMessage(role = MessageRole.USER, content = "旧问题 /work/old.pdf"),
            ChatMessage(role = MessageRole.ASSISTANT, content = "旧答复 /work/archive.md"),
            ChatMessage(role = MessageRole.USER, content = "请参考 /work/meeting.md"),
            ChatMessage(role = MessageRole.ASSISTANT, content = "[会议记录](/work/meeting.md) [方案](/work/plan.docx) /work/.hermes-app/today/contract.md /tmp/hermes-today-contract.md"),
        )
        assertEquals(listOf("meeting.md", "plan.docx"), currentConversationMaterials(messages).map { it.name })
        assertFalse(isAssistantSupportFile("/work/my-contract.md"))
        assertFalse(isAssistantSupportFile("/work/writer.py"))
        assertTrue(isAssistantSupportFile("today-conversation-context.txt"))
    }
    @Test fun communicationPreferenceAndScopedBackgroundDoNotLeakIntoRestoredMessages() {
        val original = "请详细分析这个方案，保留依据。"
        val scoped = AssistantPrompts.envelope(original, "Quoted card context with exact sources.")
        val wire = assistantConversationPrompt(scoped) + AssistantPrompts.envelope("", "附件：today-card-context.json.txt\n{\"id\":\"one\"}")
        assertEquals(original, AssistantPrompts.visibleText(wire))
        assertTrue(wire.contains("不能隐藏实质性失败"))
        assertEquals("/compact", assistantConversationPrompt("/compact"))
    }
    @Test fun scheduleSummaryUsesHumanTimeWithoutGuessingUnknownExpressions() {
        val daily = CronJob("a", "早间整理", "internal instructions", CronSchedule(expression = "0 9 * * *"))
        assertTrue(taskScheduleLabel(daily).contains("09:00"))
        assertFalse(taskScheduleLabel(daily.copy(schedule = CronSchedule(expression = "*/17 3-21 * * 2,4"))).contains("*/17"))
    }
}

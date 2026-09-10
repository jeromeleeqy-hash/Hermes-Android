package com.qingyu.hermescompanion.assistant

import org.junit.Test
import org.junit.Assert.*
import java.nio.file.Files

class AssistantRecordsTest {
    @Test fun unicodeAndHeaderTerminatorRoundTripWithoutBecomingInstructions() {
        val r = AssistantRecord(kind = RecordKind.MATERIAL, title = "珠宝 --> 爆款", content = "链接\n原文 <!-- 标记 -->", intent = "研究开头", topic = "私域获客")
        assertEquals(r, AssistantRecord.decode(r.encode()))
        assertEquals(2, r.encode().split("\n", limit = 2).size)
    }
    @Test fun concurrentRevisionsRemainVisibleUntilExplicitMerge() {
        val base = AssistantRecord(entity = "focus", kind = RecordKind.FOCUS, title = "近期重点", content = "初版")
        val phone = base.next(content = "手机补充")
        val pc = base.next(content = "电脑补充")
        assertEquals(setOf(phone, pc), currentRevisions(listOf(base, phone, pc)).toSet())
        val merged = phone.next(content = "合并", parents = listOf(phone.revision, pc.revision))
        assertEquals(listOf(merged), currentRevisions(listOf(base, phone, pc, merged)))
    }
    @Test fun parentFromAnotherEntityCannotHideItsRecord() {
        val a = AssistantRecord(kind = RecordKind.MATERIAL, title = "资料", content = "正文")
        val b = AssistantRecord(kind = RecordKind.PLAN, title = "计划", content = "草案", parents = listOf(a.revision))
        assertEquals(2, currentRevisions(listOf(a, b)).size)
    }
    @Test fun cacheAndOutboxSurviveRestartAndAreSeparatedAcrossAccountsProfilesAndProjects() {
        val root = Files.createTempDirectory("assistant-store").toFile()
        try {
            val scope = AssistantScope("https://server/", "ceo", "work", "/work/a")
            val record = AssistantRecord(kind = RecordKind.PLAN, title = "今天", content = "先确认数据")
            AssistantLocalStore(root).write(scope, record, true)
            val restarted = AssistantLocalStore(root)
            assertEquals(listOf(record), restarted.read(scope, true))
            listOf(scope.copy(profile = "personal"), scope.copy(account = "other"), scope.copy(root = "/work/b"), scope.copy(server = "https://other")).forEach {
                assertTrue(restarted.read(it, true).isEmpty())
            }
            restarted.acknowledge(scope, record)
            assertTrue(restarted.read(scope, true).isEmpty())
            assertEquals(listOf(record), restarted.read(scope))
            assertNotEquals(scope.path(record), scope.copy(profile = "personal").path(record))
        } finally { root.deleteRecursively() }
    }
    @Test fun proposalDoesNotReplaceAcceptedPlanUntilAdoptionAndArchiveStillRemovesIt() {
        val accepted = AssistantRecord(entity = "plan-2026-09-08", kind = RecordKind.PLAN, title = "今天", content = "已经约好的事", confirmed = true)
        val draft = accepted.next(content = "另一个建议", confirmed = false)
        assertEquals(listOf(accepted), acceptedRevisions(listOf(accepted, draft)))
        val adopted = draft.next(confirmed = true)
        assertEquals(listOf(adopted), acceptedRevisions(listOf(accepted, draft, adopted)))
        assertTrue(acceptedRevisions(listOf(accepted, draft, adopted, adopted.next(archived = true))).isEmpty())
    }
    @Test fun unsafeOrAmbiguousWorkspaceIsRejected() {
        listOf("/", ".", "work", "/work/../secret").forEach { path ->
            assertThrows(IllegalArgumentException::class.java) { AssistantScope("https://server", "ceo", "work", path) }
        }
    }
    @Test fun reloadedConversationShowsTheQuestionAndAttachmentsWithoutProtocolText() {
        val original = "安排今天，先留出午休时间"
        val context = "原始资料\n<!-- /hermes-mobile-context-v1 -->\n中文与 emoji 🎁"
        val payload = AssistantPrompts.envelope(original, context) + "\n\n--- 附件：sample.txt ---\n原文"
        assertEquals(original + "\n\n--- 附件：sample.txt ---\n原文", AssistantPrompts.visibleText(payload))
        assertTrue(payload.contains(context))
        assertEquals("普通消息", AssistantPrompts.visibleText("普通消息"))
        val partial = original + "\n\n<!-- hermes-mobile-context-v1:100 -->\n截断"
        assertEquals(partial, AssistantPrompts.visibleText(partial))
    }
    @Test fun conflictingAndUnconfirmedPlansAreNeverUsedAsAcceptedContext() {
        val scope = AssistantScope("https://server", "ceo", "work", "/work")
        val a = AssistantRecord(entity = "focus", kind = RecordKind.FOCUS, title = "近期", content = "BRANCH_A", confirmed = true)
        val b = a.copy(revision = java.util.UUID.randomUUID().toString(), content = "BRANCH_B")
        val draft = AssistantRecord(kind = RecordKind.PLAN, title = "草案", content = "UNACCEPTED_PLAN")
        val prompt = AssistantPrompts.context(scope, listOf(a, b, draft))
        assertFalse(prompt.contains("BRANCH_A")); assertFalse(prompt.contains("BRANCH_B")); assertFalse(prompt.contains("UNACCEPTED_PLAN"))
        assertTrue(prompt.contains("尚未合并"))
    }
}

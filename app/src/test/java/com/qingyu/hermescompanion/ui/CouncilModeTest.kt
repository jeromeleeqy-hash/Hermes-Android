package com.qingyu.hermescompanion.ui

import com.qingyu.hermescompanion.model.HermesSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CouncilModeTest {
    @Test
    fun offModeKeepsOriginalPrompt() {
        assertEquals("原始问题", buildCouncilPrompt("原始问题", CouncilMode.OFF))
    }

    @Test
    fun deepCouncilRequiresRealDelegationAndBoundedReview() {
        val prompt = buildCouncilPrompt("评估这个方案", CouncilMode.DEEP)

        assertTrue(prompt.contains("delegate_task"))
        assertTrue(prompt.contains("3 个隔离上下文"))
        assertTrue(prompt.contains("至多 1 轮"))
        assertTrue(prompt.contains("不得伪造专家意见"))
        assertTrue(prompt.endsWith("评估这个方案"))
    }

    @Test
    fun quickCouncilUsesMoaEvidenceWithoutRawChats() {
        val prompt = buildCouncilPrompt("选择数据库", CouncilMode.QUICK)

        assertTrue(prompt.contains("MoA"))
        assertTrue(prompt.contains("不要输出参考模型的原始聊天记录"))
        assertTrue(prompt.contains("关键分歧与裁决"))
    }

    @Test
    fun artifactFingerprintChangesOnlyWhenSessionMetadataChanges() {
        val original = HermesSession(id = "s1", title = "测试", preview = "旧摘要", updatedAt = "1", messageCount = 4)
        val same = original.copy(title = "新标题")
        val changed = original.copy(preview = "新摘要", messageCount = 5)

        assertEquals(artifactIndexFingerprint(original), artifactIndexFingerprint(same))
        assertNotEquals(artifactIndexFingerprint(original), artifactIndexFingerprint(changed))
    }
}

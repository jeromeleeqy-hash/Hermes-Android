package com.qingyu.hermescompanion.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CouncilTranscriptTest {
    @Test
    fun parsesAsyncDelegationBatchIntoIndependentExperts() {
        val messages = parseCouncilAgentMessages(
            """
            [ASYNC DELEGATION BATCH COMPLETE — deleg_demo]
            A background fan-out of 3 subagent(s) has finished.
            --- ✓ TASK 1/3: 你是【证据分析员】。任务：核对事实和假设
            ## 证据判断
            当前证据只能支持两个结论。
            --- ✓ TASK 2/3: 你是【反方审查员】。任务：寻找盲点
            最大风险是把相关性误当成因果关系。
            --- ✓ TASK 3/3: 你是【落地评审员】。任务：给出执行方案
            建议先做两周小范围验证。
            """.trimIndent(),
        )

        assertEquals(listOf("证据分析员", "反方审查员", "落地评审员"), messages.map(CouncilAgentMessage::name))
        assertEquals("证", messages.first().badge)
        assertTrue(messages.first().content.startsWith("## 证据判断"))
        assertEquals("建议先做两周小范围验证。", messages.last().content)
    }

    @Test
    fun leavesNormalConversationMessagesUntouched() {
        assertTrue(parseCouncilAgentMessages("Hermes 的普通回答").isEmpty())
        assertTrue(isSyntheticProcessingStatus("Hermes 正在处理 · 1 项进行中"))
        assertTrue(!isSyntheticProcessingStatus("正在处理这项业务决策的风险"))
    }
}

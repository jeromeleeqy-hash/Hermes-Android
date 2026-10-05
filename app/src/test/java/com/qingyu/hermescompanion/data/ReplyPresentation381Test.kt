package com.qingyu.hermescompanion.data

import com.qingyu.hermescompanion.model.*
import com.qingyu.hermescompanion.ui.screen.isWorkspaceSystemEntry
import com.qingyu.hermescompanion.ui.screen.taskScheduleLabel
import org.junit.Assert.*
import org.junit.Test

class ReplyPresentation381Test {
    @Test fun explicitLogsFoldWithoutLosingTheOutcomeOrOriginal() {
        val raw = "已整理好。\n\n**处理日志（详情）**\n- SHA256: abc\n- 读取了 4 份文件。\n\n**结论：评审时间已改到周五。**\n\n需要你补的：参会人名单。"
        val reading = replyPresentation(raw)
        assertFalse(reading.body.contains("SHA256"))
        assertTrue(reading.body.contains("评审时间已改到周五"))
        assertTrue(reading.body.contains("参会人名单"))
        assertTrue(reading.details.single().contains("读取了 4 份文件"))
        assertTrue(raw.contains("SHA256"))
        assertTrue(replyExcerpt(raw).contains("评审时间已改到周五"))
    }
    @Test fun blockersAndApprovalsAreNeverFoldedIntoDiagnosticDetails() {
        for (blocker in listOf("保存失败，尚未同步到手机。", "需要你审批：修改原记录。", "存在真实冲突，需确认。", "The operation failed.", "Permission required.")) {
            val result = replyPresentation("**处理日志（详情）**\n$blocker\n\n**结果**\n原内容保留。")
            assertTrue(result.body.contains(blocker))
            assertTrue(result.details.isEmpty())
        }
    }
    @Test fun technicalAnswersAndCodeAreNotBlanketFiltered() {
        val raw = "JSON 里的 version 为 3。\n\n```md\n**处理日志（详情）**\n这是示例代码。\n```\n\n## 技术方案\n使用 SHA256 校验。"
        assertEquals(raw, replyPresentation(raw).body)
        assertTrue(replyPresentation(raw).details.isEmpty())
    }
    @Test fun nestedLogSectionsKeepTheirCodeButFollowingTopLevelAnswerRemainsVisible() {
        val raw = "## 处理日志\n### 文件\n```\n## 看起来像标题但属于代码\n```\n\n## 结论\n方案已整理。"
        val result = replyPresentation(raw)
        assertEquals("## 结论\n方案已整理。", result.body)
        assertTrue(result.details.single().contains("看起来像标题"))
    }
    @Test fun narrationOnlyDoesNotPretendToBeASuccessfulResult() {
        assertEquals("", replyExcerpt("I'll resume only the unfinished part of this card action."))
        assertEquals("", replyExcerpt("Let me start by reading the files."))
        assertEquals("方案已整理。", replyExcerpt("I'll first check the files.\n\n方案已整理。"))
        assertEquals("**处理日志（详情）**\n读取了文件。", replyPresentation("**处理日志（详情）**\n读取了文件。").details.single())
    }
    @Test fun filesFilterIsNarrowAndReversible() {
        assertTrue(isWorkspaceSystemEntry(WorkspaceEntry(".snapshots", "/work/.snapshots", true), true))
        assertTrue(isWorkspaceSystemEntry(WorkspaceEntry("hermes-today.json", "/work/hermes-today.json", false), true))
        assertFalse(isWorkspaceSystemEntry(WorkspaceEntry("plan.json", "/work/plan.json", false), true))
        assertFalse(isWorkspaceSystemEntry(WorkspaceEntry("hermes-today.json", "/work/research/hermes-today.json", false), false))
    }
    @Test fun weeklySchedulesAreReadableWithoutInventingUnsupportedTimings() {
        fun job(expr: String) = CronJob("weekly", "复盘", "", CronSchedule(expression = expr))
        assertEquals("每周五 18:00", taskScheduleLabel(job("0 18 * * 5")))
        assertEquals("周一至周五 09:30", taskScheduleLabel(job("30 9 * * 1-5")))
        assertEquals("每周日 09:00", taskScheduleLabel(job("0 9 * * 7")))
        assertEquals("每周二、四 09:00", taskScheduleLabel(job("0 9 * * 2,4")))
        assertFalse(taskScheduleLabel(job("*/17 9-18 * * 2,4")).contains("09:00"))
        assertFalse(taskScheduleLabel(job("0 9 15 * 1")).contains("每周"))
    }
}

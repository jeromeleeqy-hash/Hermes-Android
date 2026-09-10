package com.qingyu.hermescompanion.ui.format

import org.junit.Test
import org.junit.Assert.*
import java.time.Instant

class ConversationPreviewTest {
    @Test fun suppressesOnlyTheKnownInternalEnvelopeIncludingTruncatedPreviews() {
        assertEquals("测试一下",conversationPreview("测试一下 <!-- hermes-mobile-context-v1:309 ..."))
        assertEquals("讨论 <!-- 保留普通注释 -->",conversationPreview("讨论 <!-- 保留普通注释 -->"))
    }
    @Test fun attachmentPreviewUsesFilenameWithoutChangingSource() {
        val raw = "请分析 @file:/root/.hermes/attachments/会议纪要.md"
        assertEquals("请分析 附件 · 会议纪要.md",conversationPreview(raw))
        assertTrue(raw.contains("/root/.hermes/"))
    }
    @Test fun numericCronTimestampsAndDateGroupsAgree() {
        val instant = parseHermesInstant("1.788840338944524E9")!!
        assertEquals(1788840338,instant.epochSecond)
        assertEquals("今天",conversationDateGroup(instant.toString(), now=instant))
        assertEquals("昨天",conversationDateGroup(instant.minusSeconds(86400).toString(),now=instant))
        assertEquals("置顶",conversationDateGroup("",pinned=true,now=instant))
        assertEquals("更早",conversationDateGroup("bad",now=Instant.now()))
    }
}

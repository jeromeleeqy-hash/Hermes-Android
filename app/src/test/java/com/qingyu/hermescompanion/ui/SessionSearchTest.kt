package com.qingyu.hermescompanion.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionSearchTest {
    @Test
    fun shortSnippetCompactsWhitespace() {
        assertEquals("Hermes 已生成 日报.md", searchSnippet("Hermes\n\n已生成\t日报.md", "日报"))
    }

    @Test
    fun longSnippetKeepsMatchInContext() {
        val content = "开头".repeat(70) + "目标消息" + "结尾".repeat(70)
        val snippet = searchSnippet(content, "目标消息")

        assertTrue(snippet.startsWith("…"))
        assertTrue(snippet.endsWith("…"))
        assertTrue(snippet.contains("目标消息"))
        assertTrue(snippet.length <= 122)
    }

    @Test
    fun matchLookupIsCaseInsensitive() {
        val content = "前文".repeat(70) + "Hermes Agent" + "后文".repeat(70)
        assertTrue(searchSnippet(content, "hermes agent").contains("Hermes Agent"))
    }
}

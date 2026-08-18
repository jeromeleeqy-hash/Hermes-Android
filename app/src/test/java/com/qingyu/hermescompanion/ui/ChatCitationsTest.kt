package com.qingyu.hermescompanion.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatCitationsTest {
    @Test
    fun extractsMarkdownAndPlainSourcesInOrderWithoutDuplicates() {
        val sources = findCitationSources(
            """
            结论见 [Hermes Agent release](https://github.com/NousResearch/hermes-agent/releases/tag/v2026.8.3)。
            补充资料：https://docs.example.com/guide，重复链接 https://github.com/NousResearch/hermes-agent/releases/tag/v2026.8.3
            """.trimIndent(),
        )

        assertEquals(2, sources.size)
        assertEquals("Hermes Agent release", sources[0].label)
        assertEquals("github.com", sources[0].host)
        assertEquals("https://docs.example.com/guide", sources[1].url)
        assertEquals("docs.example.com", sources[1].host)
    }

    @Test
    fun ignoresImagesCleansFormattingAndHonorsLimit() {
        val sources = findCitationSources(
            "![图](https://img.example/a.png) [**来源 A**](https://a.example/x) https://b.example/y https://c.example/z",
            limit = 2,
        )

        assertEquals(2, sources.size)
        assertEquals("来源 A", sources[0].label)
        assertTrue(sources.none { it.url.contains("img.example") })
    }

    @Test
    fun ignoresUrlsInsideCodeAndPreservesDocumentOrder() {
        val sources = findCitationSources(
            """
            先看 https://first.example/guide。
            `https://inline-code.example/hidden`
            ```
            https://code-block.example/hidden
            ```
            再看 [第二份资料](https://second.example/readme)。
            """.trimIndent(),
        )

        assertEquals(listOf("first.example", "second.example"), sources.map(CitationSource::host))
        assertTrue(sources.none { it.url.contains("hidden") })
    }

    @Test
    fun resolvesReferenceLinksAndKeepsBalancedUrlParentheses() {
        val sources = findCitationSources(
            """
            参见 [Kotlin 文档][docs] 与 [URI](https://en.example/wiki/URI_(computing))。

            [docs]: https://kotlinlang.org/docs/home.html "Kotlin"
            """.trimIndent(),
        )

        assertEquals(2, sources.size)
        assertEquals("Kotlin 文档", sources[0].label)
        assertEquals("https://kotlinlang.org/docs/home.html", sources[0].url)
        assertEquals("https://en.example/wiki/URI_(computing)", sources[1].url)
    }
}

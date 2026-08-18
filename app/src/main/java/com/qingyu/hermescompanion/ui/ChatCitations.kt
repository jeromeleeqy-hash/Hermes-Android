package com.qingyu.hermescompanion.ui

import java.net.URI

data class CitationSource(
    val label: String,
    val url: String,
    val host: String,
)

private data class CitationCandidate(
    val position: Int,
    val label: String,
    val url: String,
    val priority: Int,
)

private val inlineLinkStartPattern = Regex("(?<!!)\\[([^]\\n]{1,160})]\\(")
private val referenceDefinitionPattern = Regex(
    pattern = "(?m)^ {0,3}\\[([^]\\n]{1,160})]\\s*:\\s*<?(https?://[^\\s>]+)>?(?:\\s+.*)?$",
    option = RegexOption.IGNORE_CASE,
)
private val referenceLinkPattern = Regex("(?<!!)\\[([^]\\n]{1,160})]\\[([^]\\n]{0,160})]")
private val plainUrlPattern = Regex("https?://[^\\s<>\\\"，。；！？、【】《》]+", RegexOption.IGNORE_CASE)
private val fencedCodePattern = Regex("(?s)```.*?```|~~~.*?~~~")
private val inlineCodePattern = Regex("`[^`\\n]*`")
private val markdownImagePattern = Regex("!\\[[^]\\n]*]\\([^\\n]*?\\)", RegexOption.IGNORE_CASE)

internal fun findCitationSources(markdown: String, limit: Int = 8): List<CitationSource> {
    if (markdown.isBlank() || limit <= 0) return emptyList()
    val searchable = markdown
        .masked(fencedCodePattern)
        .masked(inlineCodePattern)
        .masked(markdownImagePattern)
    val candidates = mutableListOf<CitationCandidate>()

    inlineLinkStartPattern.findAll(searchable).forEach { match ->
        parseInlineDestination(searchable, match.range.last + 1)?.let { url ->
            candidates += CitationCandidate(
                position = match.range.first,
                label = cleanCitationLabel(match.groupValues[1]),
                url = url,
                priority = 0,
            )
        }
    }

    val definitions = linkedMapOf<String, Pair<String, Int>>()
    referenceDefinitionPattern.findAll(searchable).forEach { match ->
        val id = normalizeReferenceId(match.groupValues[1])
        val url = cleanCitationUrl(match.groupValues[2])
        if (id.isNotBlank() && url.isNotBlank()) definitions[id] = url to match.range.first
    }
    referenceLinkPattern.findAll(searchable).forEach { match ->
        val label = cleanCitationLabel(match.groupValues[1])
        val id = normalizeReferenceId(match.groupValues[2].ifBlank { match.groupValues[1] })
        definitions[id]?.first?.let { url ->
            candidates += CitationCandidate(match.range.first, label, url, priority = 0)
        }
    }
    definitions.forEach { (id, value) ->
        candidates += CitationCandidate(value.second, cleanCitationLabel(id), value.first, priority = 1)
    }

    plainUrlPattern.findAll(searchable).forEach { match ->
        val url = cleanCitationUrl(match.value)
        if (url.isNotBlank()) {
            candidates += CitationCandidate(match.range.first, citationHost(url), url, priority = 2)
        }
    }

    val sources = linkedMapOf<String, CitationSource>()
    candidates.sortedWith(compareBy<CitationCandidate> { it.position }.thenBy { it.priority }).forEach { candidate ->
        val url = cleanCitationUrl(candidate.url)
        if (url.isNotBlank()) {
            val host = citationHost(url)
            sources.putIfAbsent(
                url,
                CitationSource(candidate.label.ifBlank { host }, url, host),
            )
        }
    }
    return sources.values.take(limit)
}

private fun parseInlineDestination(markdown: String, start: Int): String? {
    if (start !in markdown.indices) return null
    var index = start
    val angleWrapped = markdown[index] == '<'
    if (angleWrapped) index++
    val urlStart = index
    var nestedParentheses = 0
    while (index < markdown.length) {
        when (markdown[index]) {
            '(' -> nestedParentheses++
            ')' -> {
                if (nestedParentheses == 0) break
                nestedParentheses--
            }
            '>' -> if (angleWrapped) break
            ' ', '\t', '\r', '\n' -> break
            else -> Unit
        }
        index++
    }
    if (index <= urlStart) return null
    return cleanCitationUrl(markdown.substring(urlStart, index)).takeIf(String::isNotBlank)
}

private fun String.masked(pattern: Regex): String = pattern.replace(this) { match ->
    match.value.map { if (it == '\n') '\n' else ' ' }.joinToString("")
}

private fun cleanCitationUrl(raw: String): String {
    var value = raw.trim().trim('<', '>').trimEnd(
        '.', ',', ':', ';', '!', '?',
        '。', '，', '：', '；', '！', '？', '、', '）', '】', '》',
    )
    while (value.endsWith(')') && value.count { it == ')' } > value.count { it == '(' }) value = value.dropLast(1)
    while ((value.endsWith(']') || value.endsWith('】')) && value.count { it == ']' } > value.count { it == '[' }) value = value.dropLast(1)
    while (value.endsWith('}') && value.count { it == '}' } > value.count { it == '{' }) value = value.dropLast(1)
    return value.takeIf(::isWebUrl).orEmpty()
}

private fun isWebUrl(value: String): Boolean = runCatching {
    val uri = URI(value)
    (uri.scheme.equals("http", ignoreCase = true) || uri.scheme.equals("https", ignoreCase = true)) && !uri.host.isNullOrBlank()
}.getOrDefault(false)

private fun cleanCitationLabel(raw: String): String = raw
    .replace(Regex("[`*_~]"), "")
    .replace(Regex("\\s+"), " ")
    .trim()

private fun normalizeReferenceId(raw: String): String = cleanCitationLabel(raw).lowercase()

private fun citationHost(url: String): String = runCatching {
    URI(url).host?.removePrefix("www.")?.takeIf(String::isNotBlank)
}.getOrNull() ?: "网页来源"

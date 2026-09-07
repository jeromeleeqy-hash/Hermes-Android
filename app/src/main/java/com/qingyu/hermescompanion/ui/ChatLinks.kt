package com.qingyu.hermescompanion.ui

internal data class ChatFileLink(
    val label: String,
    val target: String,
)

internal data class ChatImageLink(
    val label: String,
    val target: String,
)

private val markdownLinkLine = Regex("^\\[([^]]+)]\\(([^)]+)\\)$")
private val markdownImageLine = Regex("^!\\[([^]]*)]\\(([^)]+)\\)$")
private val absoluteMarkdownPath = Regex(
    pattern = """(?:MEDIA:)?((?:/|~/)[^"'`\r\n{}<>|]+?\.(?:md|markdown))""",
    option = RegexOption.IGNORE_CASE,
)
private val imageExtensions = setOf("png", "jpg", "jpeg", "webp", "gif", "bmp")

internal fun normalizeChatLinkTarget(rawTarget: String): String {
    return com.qingyu.hermescompanion.data.normalizeArtifactTarget(rawTarget)
}

internal fun parseChatFileLinkLine(rawLine: String): ChatFileLink? {
    val line = rawLine.trim().trim('`').trim()
    markdownLinkLine.matchEntire(line)?.let { match ->
        val target = normalizeChatLinkTarget(match.groupValues[2])
        if (target.isMarkdownPath()) {
            return ChatFileLink(
                label = match.groupValues[1].trim().ifBlank { target.fileName() },
                target = target,
            )
        }
    }

    val pathMatch = absoluteMarkdownPath.find(line) ?: return null
    val target = normalizeChatLinkTarget(pathMatch.groupValues[1])
    return target.takeIf(String::isMarkdownPath)?.let {
        ChatFileLink(label = it.fileName(), target = it)
    }
}

internal fun parseChatImageLinkLine(rawLine: String): ChatImageLink? {
    val line = rawLine.trim().trim('`').trim()
    if (line.startsWith("@image:", ignoreCase = true)) {
        val target = line.substringAfter(':').trim().trim('`', '"', '\'')
        return target.takeIf(String::isImagePath)?.let {
            ChatImageLink(label = it.fileName().ifBlank { "聊天图片" }, target = it)
        }
    }

    markdownImageLine.matchEntire(line)?.let { match ->
        val target = match.groupValues[2].trim().trim('`', '"', '\'')
        if (target.isImagePath()) {
            return ChatImageLink(
                label = match.groupValues[1].trim().ifBlank { target.fileName().ifBlank { "聊天图片" } },
                target = target,
            )
        }
    }
    return null
}

internal fun findChatImageTargets(markdown: String): List<String> =
    markdown.lineSequence()
        .mapNotNull(::parseChatImageLinkLine)
        .map(ChatImageLink::target)
        .distinct()
        .toList()

private fun String.isMarkdownPath(): Boolean =
    substringBefore('#').substringBefore('?').substringAfterLast('.', "").lowercase() in setOf("md", "markdown")

private fun String.isImagePath(): Boolean =
    startsWith("data:image/", ignoreCase = true) ||
        substringBefore('#').substringBefore('?').substringAfterLast('.', "").lowercase() in imageExtensions

private fun String.fileName(): String =
    substringBefore('#').substringBefore('?').substringAfterLast('/').ifBlank { "Markdown 文档" }

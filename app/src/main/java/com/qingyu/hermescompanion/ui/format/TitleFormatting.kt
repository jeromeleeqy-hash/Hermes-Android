package com.qingyu.hermescompanion.ui.format

import com.qingyu.hermescompanion.i18n.uiText
import com.qingyu.hermescompanion.R


private const val MAX_SESSION_TITLE_LENGTH = 15

fun isPlaceholderSessionTitle(value: String): Boolean {
    val normalized = value.trim().lowercase()
    return normalized.isBlank() || normalized in setOf(
        "untitled",
        "untitled session",
        "new session",
        "new conversation",
        "新会话",
        "新对话",
    )
}

fun compactSessionTitle(raw: String, preview: String = ""): String {
    return resolvedSessionTitle(raw, preview).take(MAX_SESSION_TITLE_LENGTH).ifBlank { uiText(R.string.ui_0079, "新会话") }
}

fun resolvedSessionTitle(raw: String, preview: String = ""): String {
    val candidate = raw.trim().takeUnless(::isPlaceholderSessionTitle) ?: preview.trim()
    if (candidate.isBlank()) return uiText(R.string.ui_0079, "新会话")
    return candidate
        .replace(Regex("https?://", RegexOption.IGNORE_CASE), "")
        .replace(Regex("\\s+"), " ")
        .trim(' ', '\n', '\r', '\t', '"', '\'', '。', '，', ',', '.', ':', '：')
        .ifBlank { uiText(R.string.ui_0079, "新会话") }
}

fun ellipsizeSessionTitle(title: String): String =
    if (title.length <= MAX_SESSION_TITLE_LENGTH) title else title.take(MAX_SESSION_TITLE_LENGTH - 1) + "…"

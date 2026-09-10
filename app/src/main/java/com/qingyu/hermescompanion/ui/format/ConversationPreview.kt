package com.qingyu.hermescompanion.ui.format

import com.qingyu.hermescompanion.i18n.uiText
import com.qingyu.hermescompanion.R


import java.time.Instant
import java.time.ZoneId

/** Presentation only: do not rewrite the source message or ordinary HTML comments. */
fun conversationPreview(raw: String): String {
    val marker = Regex("<!--\\s*hermes-mobile-context-v1:").find(raw)
    val visible = if (marker == null) raw else raw.substring(0, marker.range.first)
    return visible.replace(Regex("@file:(?:/[^\\s]+)")) { match ->
        val file = match.value.substringAfterLast('/').ifBlank { uiText(R.string.ui_0051, "附件") }
        uiText(R.string.ui_0474, "附件 · %1\$s", file)
    }.replace(Regex("\\s+"), " ").trim()
}

fun conversationDateGroup(raw: String, pinned: Boolean = false, now: Instant = Instant.now()): String {
    if (pinned) return uiText(R.string.ui_0475, "置顶")
    val date = parseHermesInstant(raw)?.atZone(ZoneId.systemDefault())?.toLocalDate() ?: return uiText(R.string.ui_0476, "更早")
    val today = now.atZone(ZoneId.systemDefault()).toLocalDate()
    return when (date) {
        today -> uiText(R.string.ui_0477, "今天")
        today.minusDays(1) -> uiText(R.string.ui_0478, "昨天")
        else -> if (date.isAfter(today.minusDays(7)) && date.isBefore(today)) uiText(R.string.ui_0479, "最近 7 天") else uiText(R.string.ui_0476, "更早")
    }
}

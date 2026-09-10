package com.qingyu.hermescompanion.ui.format

import com.qingyu.hermescompanion.i18n.uiText
import com.qingyu.hermescompanion.R


import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

enum class SessionTimeFilter(private val labelProvider: () -> String, val maxAgeSeconds: Long?) {
    ALL({ uiText(R.string.ui_0480, "全部时间") }, null),
    TODAY({ uiText(R.string.ui_0477, "今天") }, 0),
    SEVEN_DAYS({ uiText(R.string.ui_0481, "近 7 天") }, 7 * 24 * 60 * 60),
    THIRTY_DAYS({ uiText(R.string.ui_0482, "近 30 天") }, 30 * 24 * 60 * 60);
    val label: String get() = labelProvider()
}

fun sessionMatchesTime(
    updatedAt: String,
    filter: SessionTimeFilter,
    now: Instant = Instant.now(),
): Boolean {
    if (filter == SessionTimeFilter.ALL) return true
    val updated = parseSessionInstant(updatedAt) ?: return false
    if (filter == SessionTimeFilter.TODAY) {
        val zone = ZoneId.systemDefault()
        return updated.atZone(zone).toLocalDate() == now.atZone(zone).toLocalDate()
    }
    val maxAge = requireNotNull(filter.maxAgeSeconds)
    val age = now.epochSecond - updated.epochSecond
    return age <= maxAge && age >= -300
}

internal fun parseSessionInstant(raw: String): Instant? {
    val value = raw.trim()
    if (value.isBlank()) return null
    value.toDoubleOrNull()?.let { numeric ->
        val millis = if (numeric > 10_000_000_000L) numeric.toLong() else (numeric * 1000).toLong()
        return runCatching { Instant.ofEpochMilli(millis) }.getOrNull()
    }
    runCatching { Instant.parse(value) }.getOrNull()?.let { return it }
    runCatching { OffsetDateTime.parse(value).toInstant() }.getOrNull()?.let { return it }
    return runCatching {
        LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            .atZone(ZoneId.systemDefault())
            .toInstant()
    }.getOrNull()
}

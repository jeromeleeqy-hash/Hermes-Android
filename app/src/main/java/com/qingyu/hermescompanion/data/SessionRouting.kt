package com.qingyu.hermescompanion.data

/** Routes a gateway event without borrowing another conversation's registration. */
internal fun <T> routeSessionEvent(streams: Map<String, T>, runtimeId: String?): T? =
    if (!runtimeId.isNullOrBlank()) streams[runtimeId] else streams.values.singleOrNull()

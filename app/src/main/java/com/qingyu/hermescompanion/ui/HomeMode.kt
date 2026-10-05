package com.qingyu.hermescompanion.ui

/** Home content is independent of the selected visual skin and server configuration. */
enum class HomeMode { SIMPLE, DEEP }

fun resolveHomeMode(saved: String?, hasExistingBriefing: Boolean): HomeMode =
    HomeMode.entries.firstOrNull { it.name == saved }
        ?: if (hasExistingBriefing) HomeMode.DEEP else HomeMode.SIMPLE

package com.qingyu.hermescompanion.ui.component

import androidx.compose.runtime.*

/** One tap starts one complete gesture. Never queue/restart gestures on repeated taps. */
@Stable
internal class MascotInteraction {
    var motion by mutableStateOf<MascotMotion?>(null)
        private set
    private var next = MascotMotion.CONFIDENT
    fun tap(enabled: Boolean) {
        if (!enabled || motion != null) return
        motion = next
        next = if (next == MascotMotion.CONFIDENT) MascotMotion.STRETCH else MascotMotion.CONFIDENT
    }
    fun finish() { motion = null }
}

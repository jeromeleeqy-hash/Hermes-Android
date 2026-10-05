package com.qingyu.hermescompanion.today

/** Monotonic throttling survives leaving/re-entering Home; manual refresh bypasses it. */
internal class TodaySyncPolicy(private val clock: () -> Long = { System.nanoTime() / 1_000_000 }) {
    private var nextAutomaticAt = Long.MIN_VALUE
    private var failures = 0
    fun begin(force: Boolean): Boolean {
        if (!force && clock() < nextAutomaticAt) return false
        nextAutomaticAt = clock() + 60_000
        return true
    }
    fun finished(success: Boolean) {
        failures = if (success) 0 else (failures + 1).coerceAtMost(4)
        nextAutomaticAt = clock() + if (success) 60_000 else (60_000L * (1L shl (failures - 1))).coerceAtMost(300_000)
    }
    fun reset() { nextAutomaticAt = Long.MIN_VALUE; failures = 0 }
}

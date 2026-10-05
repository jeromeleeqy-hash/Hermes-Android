package com.qingyu.hermescompanion.today

import org.junit.Assert.*
import org.junit.Test

class TodaySyncPolicyTest {
    private var now = 0L
    private val policy = TodaySyncPolicy { now }
    @Test fun returningHomeAndConcurrentTriggersDoNotBypassCooldown() {
        assertTrue(policy.begin(false)); assertFalse(policy.begin(false))
        now = 1_000; policy.finished(true)
        now = 60_999; assertFalse(policy.begin(false))
        now = 61_000; assertTrue(policy.begin(false))
    }
    @Test fun offlineRetriesBackOffButManualAndProfileChangeCanRefresh() {
        for (delay in listOf(60_000L, 120_000L, 240_000L, 300_000L, 300_000L)) {
            assertTrue(policy.begin(false)); policy.finished(false)
            now += delay - 1; assertFalse(policy.begin(false))
            now++
        }
        policy.finished(false)
        assertTrue(policy.begin(true))
        policy.finished(true)
        assertFalse(policy.begin(false))
        policy.reset(); assertTrue(policy.begin(false))
    }
}

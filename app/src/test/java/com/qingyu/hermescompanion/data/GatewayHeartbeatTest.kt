package com.qingyu.hermescompanion.data

import org.junit.Assert.*
import org.junit.Test

class GatewayHeartbeatTest {
    @Test fun streamingTrafficKeepsConnectionAliveAndSilenceExpiresFromActualProbe() {
        var now = 0L
        val state = GatewayHeartbeat { now }
        state.start()
        repeat(12) {
            now += 15_000
            state.received()
            assertEquals(GatewayHeartbeat.Tick.PING, state.tick())
        }
        now += 15_000; assertEquals(GatewayHeartbeat.Tick.PING, state.tick())
        now += 15_000; assertEquals(GatewayHeartbeat.Tick.PING, state.tick())
        now += 14_999; assertEquals(GatewayHeartbeat.Tick.WAIT, state.tick())
        now++; assertEquals(GatewayHeartbeat.Tick.EXPIRED, state.tick())
    }
    @Test fun delayedFirstTimerAndSuspendedCallbacksGetNewProbeInsteadOfFalseTimeout() {
        var now = 0L
        val state = GatewayHeartbeat { now }
        state.start()
        now = 95_000; assertEquals(GatewayHeartbeat.Tick.PING, state.tick())
        now += 90_000; assertEquals(GatewayHeartbeat.Tick.PING, state.tick())
        now += 15_000; assertEquals(GatewayHeartbeat.Tick.PING, state.tick())
        state.received()
        now += 15_000; assertEquals(GatewayHeartbeat.Tick.PING, state.tick())
    }
    @Test fun backgroundTimeDoesNotConsumeForegroundProbeDeadline() {
        var now = 0L
        val state = GatewayHeartbeat { now }
        state.start()
        now = 15_000; assertEquals(GatewayHeartbeat.Tick.PING, state.tick())
        state.setForeground(false)
        repeat(6) { now += 15_000; assertEquals(GatewayHeartbeat.Tick.WAIT, state.tick()) }
        state.setForeground(true)
        assertEquals(GatewayHeartbeat.Tick.PING, state.tick())
        now += 15_000; assertEquals(GatewayHeartbeat.Tick.PING, state.tick())
        now += 15_000; assertEquals(GatewayHeartbeat.Tick.PING, state.tick())
        now += 15_000; assertEquals(GatewayHeartbeat.Tick.EXPIRED, state.tick())
        state.stop(); assertEquals(GatewayHeartbeat.Tick.WAIT, state.tick())
        state.start(); assertEquals(GatewayHeartbeat.Tick.WAIT, state.tick())
    }
}

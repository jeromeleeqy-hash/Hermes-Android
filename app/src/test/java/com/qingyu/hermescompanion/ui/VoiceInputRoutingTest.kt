package com.qingyu.hermescompanion.ui

import com.qingyu.hermescompanion.model.VoicePhase
import org.junit.Assert.assertEquals
import org.junit.Test

class VoiceInputRoutingTest {
    @Test
    fun activeRecordingStopsInsteadOfOpeningAnotherSurface() {
        assertEquals(
            VoiceInputAction.STOP_RECORDING,
            resolveVoiceInputAction("automatic", VoicePhase.LISTENING, true),
        )
    }

    @Test
    fun automaticModeFallsBackToSystemWhenAgentSttIsUnavailable() {
        assertEquals(
            VoiceInputAction.LAUNCH_SYSTEM,
            resolveVoiceInputAction("automatic", VoicePhase.ERROR, false),
        )
    }

    @Test
    fun agentOnlyModeDoesNotSilentlySwitchProviders() {
        assertEquals(
            VoiceInputAction.START_AGENT,
            resolveVoiceInputAction("agent", VoicePhase.ERROR, false),
        )
    }

    @Test
    fun transcriptionCannotStartASecondRecording() {
        assertEquals(
            VoiceInputAction.WAIT,
            resolveVoiceInputAction("automatic", VoicePhase.TRANSCRIBING, true),
        )
    }
}

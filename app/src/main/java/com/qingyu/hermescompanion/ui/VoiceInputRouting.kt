package com.qingyu.hermescompanion.ui

import com.qingyu.hermescompanion.model.VoicePhase

internal enum class VoiceInputAction {
    START_AGENT,
    LAUNCH_SYSTEM,
    STOP_RECORDING,
    WAIT,
}

internal fun resolveVoiceInputAction(
    engine: String,
    phase: VoicePhase,
    agentSttAvailable: Boolean?,
): VoiceInputAction = when (phase) {
    VoicePhase.LISTENING -> VoiceInputAction.STOP_RECORDING
    VoicePhase.TRANSCRIBING -> VoiceInputAction.WAIT
    else -> if (engine == "system" || (engine == "automatic" && agentSttAvailable == false)) {
        VoiceInputAction.LAUNCH_SYSTEM
    } else {
        VoiceInputAction.START_AGENT
    }
}

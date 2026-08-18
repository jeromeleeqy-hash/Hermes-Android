package com.qingyu.hermescompanion.ui

import com.qingyu.hermescompanion.data.ApiException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceCompatibilityTest {
    @Test
    fun recognizesAgentSttSourceSignatureMismatch() {
        val failure = ApiException(
            400,
            "Transcription failed: transcribe_audio() got an unexpected keyword argument 'source'",
        )

        assertTrue(isAgentSttCompatibilityFailure(failure))
        assertEquals(
            "服务器语音组件与 Hermes Agent 版本不匹配。请更新 Hermes Agent，待网关重启后再试。",
            agentSttCompatibilityMessage(),
        )
    }

    @Test
    fun leavesNormalTranscriptionFailuresUnchanged() {
        assertFalse(isAgentSttCompatibilityFailure(ApiException(400, "Hermes 没有识别到语音内容")))
        assertFalse(isAgentSttCompatibilityFailure(null))
    }
}

package com.qingyu.hermescompanion.data

import com.qingyu.hermescompanion.model.ServerTtsSettings
import org.junit.Assert.*
import org.junit.Test

class VoiceTurnPolicyTest {
    private class Recording(val detector: VoiceSilenceDetector = VoiceSilenceDetector()) {
        var time = 0L
        var sentAt: Long? = null
        fun frames(count: Int, level: (Int) -> Float) {
            repeat(count) { index ->
                if (detector.sampleDb(level(index), time) && sentAt == null) sentAt = time
                time += 70
            }
        }
        fun frames(count: Int, db: Float) = frames(count) { db }
    }
    @Test fun animationGainCannotChangeTheEndpointLevel() {
        val noise = voiceInputSample(300)
        assertTrue(noise.displayLevel > .075f) // This falsely kept 3.1.6 listening.
        assertEquals(-40.77f, noise.dbFs, .1f)
        assertEquals(-96f, voiceInputSample(0).dbFs, .01f)
        assertEquals(0f, voiceInputSample(32767).dbFs, .01f)
    }
    @Test fun constantBackgroundAndIsolatedClicksNeverSubmit() {
        for (background in listOf(-75f, -48f, -36f, -25f)) {
            val r = Recording()
            r.frames(120) { if (it in listOf(30, 60, 90)) -10f else background + (it % 3 - 1) }
            assertNull("Noise at $background dBFS is not a sentence", r.sentAt)
        }
    }
    @Test fun speechEndsEvenWhenFanNoiseNeverReachesSilence() {
        val r = Recording()
        r.frames(14) { -38f + (it % 4) }
        r.frames(20) { -19f + (it % 3) }
        val speechEnded = r.time - 70
        r.frames(25) { -38f + (it % 4) }
        assertNotNull(r.sentAt)
        assertTrue(r.sentAt!! - speechEnded in 1_250..1_390)
    }
    @Test fun shortPauseAndResumedSpeechKeepTheSameTurn() {
        val r = Recording()
        r.frames(12, -65f); r.frames(14, -22f)
        r.frames(8, -65f)
        assertNull(r.sentAt)
        r.frames(12, -23f)
        val speechEnded = r.time - 70
        r.frames(25, -65f)
        assertTrue(r.sentAt!! - speechEnded in 1_250..1_390)
    }
    @Test fun isolatedTapAfterSpeechDoesNotRestartTheSilenceTimer() {
        val r = Recording()
        r.frames(12, -60f); r.frames(15, -20f)
        val speechEnded = r.time - 70
        r.frames(8, -60f); r.frames(1, -8f); r.frames(16, -60f)
        assertTrue(r.sentAt!! - speechEnded in 1_250..1_390)
    }
    @Test fun speechStartedImmediatelyIsRetainedWhenTheFirstPauseSuppliesTheNoiseFloor() {
        val r = Recording()
        r.frames(20) { -24f + (it % 4) }
        val speechEnded = r.time - 70
        r.frames(30, -58f)
        assertNotNull(r.sentAt)
        assertTrue(r.sentAt!! - speechEnded in 1_250..1_750)
    }
    @Test fun quietModeCanRecognizeSoftSpeechWithoutArmingOnRoomNoise() {
        val r = Recording(VoiceSilenceDetector(sensitivity = "quiet"))
        r.frames(15, -74f); r.frames(15, -51f); r.frames(25, -74f)
        assertNotNull(r.sentAt)
    }
    @Test fun noisyModeFiltersMoreBackgroundButStillDetectsNearbySpeech() {
        val r = Recording(VoiceSilenceDetector(sensitivity = "noisy"))
        r.frames(12, -40f); r.frames(25, -30f)
        assertNull(r.sentAt)
        r.frames(20, -12f); r.frames(25, -39f)
        assertNotNull(r.sentAt)
    }
    @Test fun invalidLevelsDoNotArmAndLongValidSpeechHasABoundedTurn() {
        val r = Recording(VoiceSilenceDetector(maximumMillis = 3_000))
        r.frames(12, -60f)
        assertFalse(r.detector.sampleDb(Float.NaN, r.time))
        assertFalse(r.detector.sampleDb(Float.POSITIVE_INFINITY, r.time))
        r.frames(40, -20f)
        assertNotNull(r.sentAt)
        assertTrue(r.sentAt!! in 3_000..3_140)
    }
    @Test fun chineseAndMixedNumbersSurviveMarkdownCleaning() {
        assertEquals("运营日报\n今天新增客户 12 位，增长 20%。\n查看完整结果。", spokenReply("# 运营日报\n**今天新增客户 12 位，增长 20%。**\n查看[完整结果](https://example.com/report)。"))
        assertEquals("zh-CN", speechLanguage("客户增长 20%", "en-US"))
        assertEquals("zh-TW", speechLanguage("客戶", "zh-TW"))
    }
    @Test fun longSpeechKeepsEveryCharacterAndUnicodePair() {
        val text = "今天的进展很顺利，新增客户 12 位。🌟".repeat(300)
        val chunks = speechChunks(text, 81)
        assertEquals(text, chunks.joinToString(""))
        assertTrue(chunks.all { it.length <= 81 && !Character.isHighSurrogate(it.last()) && !Character.isLowSurrogate(it.first()) })
    }
    @Test fun englishVoicesCannotSilentlyDropChinese() {
        assertFalse(agentVoiceSupportsChinese(ServerTtsSettings("kokoro", voice = "af_heart")))
        assertTrue(agentVoiceSupportsChinese(ServerTtsSettings("kokoro", voice = "zf_xiaobei")))
        assertFalse(agentVoiceSupportsChinese(ServerTtsSettings("edge", voice = "en-US-AriaNeural")))
        assertTrue(agentVoiceSupportsChinese(ServerTtsSettings("edge", voice = "zh-CN-XiaoxiaoNeural")))
    }
}

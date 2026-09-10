package com.qingyu.hermescompanion.data

import com.qingyu.hermescompanion.i18n.uiText
import com.qingyu.hermescompanion.R


import com.qingyu.hermescompanion.model.ServerTtsSettings

/** Raw peak dBFS is deliberately separate from the amplified animation envelope.
 * This is a relative digital level, not a calibrated sound-pressure measurement. */
data class VoiceInputSample(val dbFs: Float, val displayLevel: Float)

internal fun voiceInputSample(peakAmplitude: Int): VoiceInputSample {
    val fraction = peakAmplitude.coerceIn(0, 32767) / 32767f
    val db = if (fraction > 0f) (20 * kotlin.math.log10(fraction)).coerceAtLeast(-96f) else -96f
    return VoiceInputSample(db, kotlin.math.sqrt(fraction))
}

/** Adapt to room noise before endpointing; isolated peaks cannot restart the pause timer. */
class VoiceSilenceDetector(
    private val silenceMillis: Long = 1_250,
    private val maximumMillis: Long = 60_000,
    var sensitivity: String = "balanced",
) {
    private data class Frame(val db: Float, val time: Long)
    private val calibration = mutableListOf<Frame>()
    private var startedAt: Long? = null
    private var lastSample: Long? = null
    private var lastSpeech: Long? = null
    private var candidateMillis = 0L
    private var speechMillis = 0L
    private var voiceActive = false
    var isCalibrating = true
        private set
    var noiseFloorDb = -65f
        private set
    val hasSpeech: Boolean get() = speechMillis >= 280
    val isSpeaking: Boolean get() = voiceActive
    private val marginDb: Float get() = when (sensitivity) { "quiet" -> 6f; "noisy" -> 13f; else -> 9f }
    private val minimumDb: Float get() = when (sensitivity) { "quiet" -> -54f; "noisy" -> -42f; else -> -48f }
    val speechThresholdDb: Float get() = maxOf(minimumDb, noiseFloorDb + marginDb)

    fun sampleDb(dbFs: Float, now: Long): Boolean {
        if (!dbFs.isFinite()) return false
        if (lastSample?.let { now < it } == true) return false
        val db = dbFs.coerceIn(-96f, 0f)
        val start = startedAt ?: now.also { startedAt = it }
        if (!hasSpeech) {
            calibration += Frame(db, now)
            calibration.removeAll { now - it.time > 2_500 }
            if (now - start < 500 || calibration.size < 5) return false
            // The first MediaRecorder sample is zero. Ignore it when estimating an audible room.
            val audible = calibration.map { it.db }.filter { it > -90f }.sorted()
            noiseFloorDb = if (audible.isEmpty()) -76f else audible[((audible.size - 1) * .3f).toInt()].coerceAtLeast(-76f)
            isCalibrating = false
            // Keep surveying until speech is confirmed. Someone who starts talking immediately
            // can supply the room baseline in their first pause; replay retains those first words.
            candidateMillis = 0
            speechMillis = 0
            voiceActive = false
            lastSpeech = null
            lastSample = null
            calibration.forEach { classify(it.db, it.time, adaptNoise = false) }
            if (hasSpeech) calibration.clear()
        } else classify(db, now, adaptNoise = true)
        val last = lastSpeech ?: return false
        return hasSpeech && (now - last >= silenceMillis || now - start >= maximumMillis)
    }

    private fun classify(db: Float, now: Long, adaptNoise: Boolean) {
        val elapsed = (now - (lastSample ?: now)).coerceIn(0, 150)
        lastSample = now
        // Hysteresis keeps quiet syllables inside speech, while requiring stronger onset evidence.
        val threshold = speechThresholdDb - if (voiceActive) 3f else 0f
        if (db >= threshold) {
            candidateMillis += elapsed
            val confirmation = if (hasSpeech) 120 else 210
            if (candidateMillis >= confirmation) {
                if (!voiceActive) speechMillis += candidateMillis else speechMillis += elapsed
                voiceActive = true
                lastSpeech = now
            }
        } else {
            candidateMillis = 0
            voiceActive = false
            if (adaptNoise) {
                // Follow changing background levels only outside detected speech.
                val factor = if (db < noiseFloorDb) .18f else .045f
                noiseFloorDb = (noiseFloorDb + factor * (db - noiseFloorDb)).coerceAtLeast(-76f)
            }
        }
    }
}

internal fun containsChinese(text: String): Boolean = text.codePoints().anyMatch {
    Character.UnicodeScript.of(it) == Character.UnicodeScript.HAN
}

internal fun speechLanguage(text: String, preferred: String): String =
    if (containsChinese(text) && !preferred.startsWith("zh", true)) "zh-CN" else preferred

/** Strip presentation syntax, preserving Chinese, punctuation and the actual answer. */
internal fun spokenReply(markdown: String): String = markdown
    .replace(Regex("(?s)<think>.*?</think>"), "")
    .replace(Regex("(?s)```.*?```"), uiText(R.string.ui_0149, "\n代码已放在对话中。\n"))
    .replace(Regex("!\\[([^]]*)]\\([^)]+\\)"), "$1")
    .replace(Regex("\\[([^]]+)]\\([^)]+\\)"), "$1")
    .replace(Regex("https?://\\S+"), "")
    .replace(Regex("(?m)^\\s{0,3}(?:#{1,6} |[>*+-] |\\d+[.)] )"), "")
    .replace(Regex("[`*_~]"), "")
    .replace(Regex("(?m)^\\s*[:| -]{3,}\\s*$"), "")
    .replace('|', '，')
    .replace(Regex("[ \\t]+"), " ")
    .replace(Regex("\\n{3,}"), "\n\n")
    .trim()

/** Never silently truncate long answers or split a surrogate pair. */
internal fun speechChunks(text: String, limit: Int = 400): List<String> {
    require(limit >= 2)
    val chunks = mutableListOf<String>()
    var rest = text.trim()
    while (rest.length > limit) {
        val cut = rest.take(limit).indexOfLast { it in "。！？；.!?;\n" }
        var end = if (cut >= limit / 3) cut + 1 else limit
        if (Character.isHighSurrogate(rest[end - 1])) end--
        chunks += rest.substring(0, end)
        rest = rest.substring(end) // Keep spaces so joining chunks never changes the answer.
    }
    if (rest.isNotBlank()) chunks += rest
    return chunks
}

internal fun agentVoiceSupportsChinese(settings: ServerTtsSettings): Boolean = when (settings.provider.lowercase()) {
    "edge" -> settings.voice.startsWith("zh-", true) || settings.voice.contains("Multilingual", true)
    "kokoro" -> settings.voice.startsWith("zf_") || settings.voice.startsWith("zm_")
    "piper" -> settings.voice.startsWith("zh_", true) || settings.model.startsWith("zh_", true)
    "elevenlabs" -> settings.model.isBlank() || settings.model.contains("multilingual", true) || settings.model.contains("v3", true) || settings.model.contains("v2_5", true)
    "openai", "minimax", "qwen", "dashscope" -> true
    else -> false
}

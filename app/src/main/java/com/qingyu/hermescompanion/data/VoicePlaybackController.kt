package com.qingyu.hermescompanion.data

import com.qingyu.hermescompanion.i18n.uiText
import com.qingyu.hermescompanion.R


import android.content.Context
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.qingyu.hermescompanion.model.SpeechAudio
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File
import java.io.IOException
import java.util.Locale
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class VoicePlaybackController(private val context: Context) {
    private var player: MediaPlayer? = null
    private var playerFile: File? = null
    private var tts: TextToSpeech? = null
    private val main = Handler(Looper.getMainLooper())

    suspend fun play(audio: SpeechAudio) = withContext(Dispatchers.Main.immediate) {
        withTimeout(180_000) {
            suspendCancellableCoroutine<Unit> { continuation ->
                stopPlayer()
                val suffix = when {
                    audio.mimeType.contains("wav", true) -> ".wav"
                    audio.mimeType.contains("ogg", true) -> ".ogg"
                    audio.mimeType.contains("mp4", true) || audio.mimeType.contains("m4a", true) -> ".m4a"
                    else -> ".mp3"
                }
                val mediaPlayer = MediaPlayer()
                player = mediaPlayer
                fun complete(error: Exception? = null) {
                    if (player === mediaPlayer) stopPlayer()
                    if (continuation.isActive) {
                        if (error == null) continuation.resume(Unit) else continuation.resumeWithException(error)
                    }
                }
                continuation.invokeOnCancellation { main.post { if (player === mediaPlayer) stopPlayer() } }
                try {
                    val file = File.createTempFile("hermes-speech-", suffix, context.cacheDir).apply { writeBytes(audio.bytes) }
                    playerFile = file
                    mediaPlayer.setDataSource(file.absolutePath)
                    mediaPlayer.setOnCompletionListener { complete() }
                    mediaPlayer.setOnErrorListener { _, _, _ -> complete(IOException(uiText(R.string.ui_0140, "语音音频无法播放"))); true }
                    mediaPlayer.setOnPreparedListener { if (continuation.isActive) it.start() }
                    mediaPlayer.prepareAsync()
                } catch (error: Exception) { complete(error) }
            }
        }
    }

    suspend fun speakSystem(text: String, language: String, rate: Float) = withContext(Dispatchers.Main.immediate) {
        stop()
        val engine = withTimeout(12_000) {
            suspendCancellableCoroutine<TextToSpeech> { continuation ->
                lateinit var created: TextToSpeech
                created = TextToSpeech(context) { status ->
                    // Always defer: some engines invoke initialization before construction returns.
                    main.post {
                        if (!continuation.isActive) { created.shutdown(); return@post }
                        if (status == TextToSpeech.SUCCESS) continuation.resume(created)
                        else { created.shutdown(); continuation.resumeWithException(IOException(uiText(R.string.ui_0141, "手机语音引擎初始化失败"))) }
                    }
                }
                tts = created
                continuation.invokeOnCancellation { main.post { if (tts === created) { created.shutdown(); tts = null } } }
            }
        }
        try {
            val locale = Locale.forLanguageTag(speechLanguage(text, language))
            val available = engine.isLanguageAvailable(locale)
            if (available < TextToSpeech.LANG_AVAILABLE) throw IOException(
                if (locale.language == "zh") uiText(R.string.ui_0142, "手机缺少可用的中文语音。安装中文语音包，或在语音设置中选择支持中文的 Agent TTS。")
                else uiText(R.string.ui_0143, "手机未安装 %1\$s 语音包", locale.displayLanguage),
            )
            if (engine.setLanguage(locale) < TextToSpeech.LANG_AVAILABLE) throw IOException(uiText(R.string.ui_0144, "无法启用所选语言的语音"))
            val voice = engine.voices.orEmpty().filter { it.locale.language == locale.language && TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED !in it.features }
                .sortedWith(compareBy<android.speech.tts.Voice> { it.locale.country != locale.country }
                    .thenBy { it.isNetworkConnectionRequired }.thenByDescending { it.quality }).firstOrNull()
            if (voice != null && engine.setVoice(voice) != TextToSpeech.SUCCESS) throw IOException(uiText(R.string.ui_0145, "无法启用所选发音人"))
            // Legacy engines can support setLanguage without exposing a Voice object.
            @Suppress("DEPRECATION")
            val activeLanguage = engine.voice?.locale?.language ?: engine.language?.language
            val acceptedLanguages = setOf(locale.language, locale.isO3Language) +
                if (locale.language == "zh") setOf("chi") else emptySet()
            if (activeLanguage !in acceptedLanguages) throw IOException(uiText(R.string.ui_0146, "手机语音服务没有匹配的%1\$s发音人", locale.displayLanguage))
            engine.setSpeechRate(rate.coerceIn(.6f, 1.6f))
            for (chunk in speechChunks(text, minOf(400, TextToSpeech.getMaxSpeechInputLength() - 1))) {
                val timeout = (chunk.length * 700L / rate.coerceIn(.6f, 1.6f) + 30_000).toLong().coerceAtLeast(60_000)
                withTimeout(timeout) {
                    suspendCancellableCoroutine<Unit> { continuation ->
                        val id = UUID.randomUUID().toString()
                        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                            override fun onStart(utteranceId: String?) = Unit
                            override fun onDone(utteranceId: String?) { if (utteranceId == id && continuation.isActive) continuation.resume(Unit) }
                            @Deprecated("Deprecated in Java")
                            override fun onError(utteranceId: String?) { if (utteranceId == id && continuation.isActive) continuation.resumeWithException(IOException(uiText(R.string.ui_0147, "手机语音朗读失败"))) }
                        })
                        continuation.invokeOnCancellation { main.post { if (tts === engine) engine.stop() } }
                        if (engine.speak(chunk, TextToSpeech.QUEUE_FLUSH, null, id) == TextToSpeech.ERROR && continuation.isActive) {
                            continuation.resumeWithException(IOException(uiText(R.string.ui_0148, "手机语音服务拒绝了朗读请求")))
                        }
                    }
                }
            }
        } finally {
            engine.stop(); engine.shutdown()
            if (tts === engine) tts = null
        }
    }

    fun stop() {
        stopPlayer()
        tts?.stop(); tts?.shutdown(); tts = null
    }

    private fun stopPlayer() {
        runCatching { player?.stop() }; runCatching { player?.release() }
        player = null
        playerFile?.delete(); playerFile = null
    }
}

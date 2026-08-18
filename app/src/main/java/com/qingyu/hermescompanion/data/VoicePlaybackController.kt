package com.qingyu.hermescompanion.data

import android.content.Context
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import com.qingyu.hermescompanion.model.SpeechAudio
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.util.Locale
import kotlin.coroutines.resume

class VoicePlaybackController(private val context: Context) {
    private var player: MediaPlayer? = null
    private var playerFile: File? = null
    private var tts: TextToSpeech? = null

    suspend fun play(audio: SpeechAudio) = suspendCancellableCoroutine<Unit> { continuation ->
        stopPlayer()
        val suffix = when {
            audio.mimeType.contains("wav", true) -> ".wav"
            audio.mimeType.contains("ogg", true) -> ".ogg"
            audio.mimeType.contains("mp4", true) || audio.mimeType.contains("m4a", true) -> ".m4a"
            else -> ".mp3"
        }
        val file = File.createTempFile("hermes-speech-", suffix, context.cacheDir).apply {
            writeBytes(audio.bytes)
        }
        playerFile = file
        val mediaPlayer = MediaPlayer()
        player = mediaPlayer
        mediaPlayer.setDataSource(file.absolutePath)
        mediaPlayer.setOnCompletionListener {
            stopPlayer()
            if (continuation.isActive) continuation.resume(Unit)
        }
        mediaPlayer.setOnErrorListener { _, _, _ ->
            stopPlayer()
            if (continuation.isActive) continuation.resume(Unit)
            true
        }
        continuation.invokeOnCancellation { stopPlayer() }
        mediaPlayer.prepare()
        mediaPlayer.start()
    }

    suspend fun speakSystem(text: String, language: String, rate: Float) = suspendCancellableCoroutine<Unit> { continuation ->
        stopPlayer()
        val utteranceId = "hermes-${System.currentTimeMillis()}"
        val engine = TextToSpeech(context) { status ->
            if (status != TextToSpeech.SUCCESS) {
                if (continuation.isActive) continuation.resume(Unit)
                return@TextToSpeech
            }
            tts?.language = Locale.forLanguageTag(language)
            tts?.setSpeechRate(rate.coerceIn(0.6f, 1.6f))
            tts?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                override fun onStart(id: String?) = Unit
                override fun onDone(id: String?) {
                    if (id == utteranceId && continuation.isActive) continuation.resume(Unit)
                }
                @Deprecated("Deprecated in Java")
                override fun onError(id: String?) {
                    if (id == utteranceId && continuation.isActive) continuation.resume(Unit)
                }
            })
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        }
        tts = engine
        continuation.invokeOnCancellation { stop() }
    }

    fun stop() {
        stopPlayer()
        tts?.stop()
        tts?.shutdown()
        tts = null
    }

    private fun stopPlayer() {
        runCatching { player?.stop() }
        runCatching { player?.release() }
        player = null
        playerFile?.delete()
        playerFile = null
    }
}

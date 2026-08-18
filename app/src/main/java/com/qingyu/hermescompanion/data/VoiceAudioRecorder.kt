package com.qingyu.hermescompanion.data

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File

class VoiceAudioRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null

    @Suppress("DEPRECATION")
    fun start() {
        cancel()
        val file = File.createTempFile("hermes-voice-", ".m4a", context.cacheDir)
        val activeRecorder = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(context) else MediaRecorder()
        activeRecorder.apply {
            setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(96_000)
            setAudioSamplingRate(44_100)
            setOutputFile(file.absolutePath)
            prepare()
            start()
        }
        outputFile = file
        recorder = activeRecorder
    }

    fun stop(): Pair<ByteArray, String> {
        val activeRecorder = recorder ?: error("语音录制尚未开始")
        val file = outputFile ?: error("没有找到录音文件")
        recorder = null
        outputFile = null
        try {
            activeRecorder.stop()
        } finally {
            activeRecorder.release()
        }
        val bytes = file.readBytes()
        file.delete()
        require(bytes.isNotEmpty()) { "录音内容为空，请靠近麦克风后重试" }
        return bytes to "audio/mp4"
    }

    fun inputLevel(): Float {
        val amplitude = runCatching { recorder?.maxAmplitude ?: 0 }.getOrDefault(0)
        if (amplitude <= 0) return 0f
        return kotlin.math.sqrt((amplitude / 32767f).coerceIn(0f, 1f))
    }

    fun cancel() {
        val activeRecorder = recorder
        recorder = null
        runCatching { activeRecorder?.stop() }
        runCatching { activeRecorder?.release() }
        outputFile?.delete()
        outputFile = null
    }
}

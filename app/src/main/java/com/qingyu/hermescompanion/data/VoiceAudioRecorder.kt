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
        outputFile = file
        recorder = activeRecorder
        try { activeRecorder.apply {
            setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(96_000)
            setAudioSamplingRate(44_100)
            setOutputFile(file.absolutePath)
            prepare()
            start()
        }
        } catch (error: Exception) { cancel(); throw error }
    }

    fun stop(): Pair<ByteArray, String> {
        val activeRecorder = recorder ?: error("语音录制尚未开始")
        val file = outputFile ?: error("没有找到录音文件")
        recorder = null
        outputFile = null
        val bytes = try {
            activeRecorder.stop()
            file.readBytes()
        } finally {
            activeRecorder.release()
            file.delete()
        }
        require(bytes.isNotEmpty()) { "录音内容为空，请靠近麦克风后重试" }
        return bytes to "audio/mp4"
    }

    fun inputSample(): VoiceInputSample {
        val amplitude = runCatching { recorder?.maxAmplitude ?: 0 }.getOrDefault(0)
        return voiceInputSample(amplitude)
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

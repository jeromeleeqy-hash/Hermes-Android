package com.qingyu.hermescompanion.data

import com.qingyu.hermescompanion.i18n.uiText
import com.qingyu.hermescompanion.R


import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File

class VoiceAudioRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null

    @Suppress("DEPRECATION")
    fun start(destination: File? = null) {
        cancel()
        val file = destination ?: File.createTempFile("hermes-voice-", ".m4a", context.cacheDir)
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

    fun stopToFile(): File {
        val activeRecorder = recorder ?: error(uiText(R.string.ui_0135, "语音录制尚未开始"))
        val file = outputFile ?: error(uiText(R.string.ui_0136, "没有找到录音文件"))
        recorder = null
        outputFile = null
        try {
            activeRecorder.stop()
        } catch (error: Exception) {
            file.delete()
            throw IllegalStateException(uiText(R.string.ui_0137, "录音过短或被中断，请重新录制"), error)
        } finally { activeRecorder.release() }
        require(file.length() >= 128) { uiText(R.string.ui_0138, "录音内容为空，请靠近麦克风后重试") }
        return file
    }

    fun stop(): Pair<ByteArray, String> {
        val file = stopToFile()
        return try { file.readBytes() to "audio/mp4" } finally { file.delete() }
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

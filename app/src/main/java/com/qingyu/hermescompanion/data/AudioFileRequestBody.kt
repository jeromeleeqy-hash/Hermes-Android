package com.qingyu.hermescompanion.data

import com.qingyu.hermescompanion.i18n.uiText
import com.qingyu.hermescompanion.R


import java.io.File
import java.util.Base64
import okhttp3.RequestBody
import okhttp3.MediaType.Companion.toMediaType
import okio.BufferedSink

/** Stream base64 in aligned blocks; never hold the recording and its JSON copy in memory. */
class AudioFileRequestBody(private val file: File) : RequestBody() {
    private val prefix = "{\"mime_type\":\"audio/mp4\",\"data_url\":\"data:audio/mp4;base64,"
    private val suffix = "\"}"
    private val size = file.length()
    init { require(size in 128..15L * 1024 * 1024) { uiText(R.string.ui_0055, "录音为空或超过 15 MB，请重新录制") } }
    override fun contentType() = "application/json; charset=utf-8".toMediaType()
    override fun contentLength(): Long = prefix.toByteArray().size + 4 * ((size + 2) / 3) + suffix.toByteArray().size
    override fun writeTo(sink: BufferedSink) {
        require(file.length() == size) { uiText(R.string.ui_0056, "录音文件已变化，请重试") }
        sink.writeUtf8(prefix)
        file.inputStream().use { input ->
            val block = ByteArray(3 * 8192)
            while (true) {
                var count = 0
                while (count < block.size) {
                    val read = input.read(block, count, block.size - count)
                    if (read == -1) break
                    count += read
                }
                if (count == 0) break
                sink.write(Base64.getEncoder().encode(if (count == block.size) block else block.copyOf(count)))
            }
        }
        sink.writeUtf8(suffix)
    }
}

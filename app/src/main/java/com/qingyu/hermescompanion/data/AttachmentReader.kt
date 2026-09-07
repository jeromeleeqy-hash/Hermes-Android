package com.qingyu.hermescompanion.data

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import com.qingyu.hermescompanion.model.PendingAttachment
import java.io.ByteArrayOutputStream

object AttachmentReader {
    private const val MAX_IMAGE_BYTES = 8 * 1024 * 1024
    private const val MAX_TEXT_BYTES = 512 * 1024

    private val textMimeTypes = setOf(
        "application/json",
        "application/xml",
        "application/javascript",
        "application/x-yaml",
        "application/yaml",
    )

    fun read(resolver: ContentResolver, uri: Uri): PendingAttachment {
        val mimeType = resolver.getType(uri) ?: "application/octet-stream"
        val name = queryName(resolver, uri) ?: "附件"

        return when {
            mimeType.startsWith("image/") -> {
                val bytes = readLimited(resolver, uri, MAX_IMAGE_BYTES)
                PendingAttachment(
                    name = name,
                    mimeType = mimeType,
                    dataUrl = "data:$mimeType;base64,${Base64.encodeToString(bytes, Base64.NO_WRAP)}",
                )
            }

            mimeType.startsWith("text/") || mimeType in textMimeTypes || isTextFileName(name) -> {
                val bytes = readLimited(resolver, uri, MAX_TEXT_BYTES)
                PendingAttachment(
                    name = name,
                    mimeType = mimeType,
                    textContent = bytes.toString(Charsets.UTF_8),
                )
            }

            else -> error("首版暂不支持 $name；目前支持图片和常见文本文件")
        }
    }

    /** Remote files already exist on the server; binary documents remain real path attachments. */
    fun fromWorkspaceDocument(document: com.qingyu.hermescompanion.model.WorkspaceDocument): PendingAttachment {
        val mime = document.mimeType.substringBefore(';').lowercase()
        val attachment = PendingAttachment(name = document.name, mimeType = mime, remotePath = document.path)
        return when {
            mime.startsWith("image/") && document.bytes.size <= MAX_IMAGE_BYTES -> attachment.copy(
                dataUrl = "data:$mime;base64,${java.util.Base64.getEncoder().encodeToString(document.bytes)}",
            )
            (mime.startsWith("text/") || mime in textMimeTypes || isTextFileName(document.name)) &&
                document.bytes.size <= MAX_TEXT_BYTES -> attachment.copy(textContent = document.bytes.toString(Charsets.UTF_8))
            else -> attachment
        }
    }

    private fun readLimited(resolver: ContentResolver, uri: Uri, limit: Int): ByteArray {
        resolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "无法读取所选文件" }
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(16 * 1024)
            var total = 0
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                total += count
                require(total <= limit) { "文件过大，请选择更小的文件" }
                output.write(buffer, 0, count)
            }
            return output.toByteArray()
        }
    }

    private fun queryName(resolver: ContentResolver, uri: Uri): String? {
        return resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            cursor.getString(0)
        }
    }

    private fun isTextFileName(name: String): Boolean {
        return name.substringAfterLast('.', "").lowercase() in setOf(
            "txt", "md", "markdown", "csv", "tsv", "json", "xml", "yaml", "yml", "log",
            "kt", "java", "py", "js", "ts", "html", "css", "sh", "sql",
        )
    }
}

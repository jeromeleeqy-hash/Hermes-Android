package com.qingyu.hermescompanion.storage

import com.qingyu.hermescompanion.i18n.uiText
import com.qingyu.hermescompanion.R


import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import com.qingyu.hermescompanion.model.UserProfilePreferences
import java.io.File
import java.io.FileOutputStream
import java.net.URI
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.UUID
import kotlin.math.roundToInt

enum class AvatarTarget(internal val filePrefix: String) {
    USER("user"),
    HERMES("hermes"),
}

data class AvatarCropSpec(
    val zoom: Float = 1f,
    val horizontalBias: Float = 0f,
    val verticalBias: Float = 0f,
)

/**
 * Stores avatar files copied from Android's document picker.
 *
 * Picker content URIs are never persisted: their grants can expire after the activity or
 * process is recreated. Only private file URIs rooted under filesDir/avatars are returned.
 */
class AvatarStorage(private val context: Context) {
    private val avatarDirectory: File
        get() = File(context.filesDir, AVATAR_DIRECTORY)

    fun save(source: Uri, target: AvatarTarget, crop: AvatarCropSpec? = null): String {
        val mimeType = runCatching { context.contentResolver.getType(source) }.getOrNull()
        require(mimeType == null || mimeType.startsWith("image/")) { uiText(R.string.ui_0166, "请选择图片文件") }

        val directory = avatarDirectory.apply {
            check(exists() || mkdirs()) { uiText(R.string.ui_0167, "无法创建头像目录") }
        }
        val decoded = decodeScaledBitmap(source, if (crop == null) MAX_AVATAR_EDGE else MAX_CROP_SOURCE_EDGE)
        val bitmap = crop?.let { cropAvatarBitmap(decoded, it, MAX_AVATAR_EDGE) } ?: decoded
        val destination = File(directory, "${target.filePrefix}-${UUID.randomUUID()}.avatar")
        val temporary = File(directory, ".${destination.name}.tmp")
        try {
            FileOutputStream(temporary).use { output ->
                val format = if (bitmap.hasAlpha()) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
                check(bitmap.compress(format, AVATAR_QUALITY, output)) { uiText(R.string.ui_0168, "头像保存失败") }
                output.fd.sync()
            }
            try {
                Files.move(
                    temporary.toPath(),
                    destination.toPath(),
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE,
                )
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(
                    temporary.toPath(),
                    destination.toPath(),
                    StandardCopyOption.REPLACE_EXISTING,
                )
            }
            directory.listFiles()
                .orEmpty()
                .filter { it.isFile && it != destination && it.name.startsWith("${target.filePrefix}-") }
                .forEach(File::delete)
            return destination.toURI().toString()
        } finally {
            temporary.delete()
            if (!bitmap.isRecycled) bitmap.recycle()
            if (decoded !== bitmap && !decoded.isRecycled) decoded.recycle()
        }
    }

    fun delete(target: AvatarTarget) {
        avatarDirectory.listFiles()
            .orEmpty()
            .filter { it.isFile && it.name.startsWith("${target.filePrefix}-") }
            .forEach(File::delete)
    }

    fun sanitize(profile: UserProfilePreferences): UserProfilePreferences = profile.copy(
        avatarUri = validPrivateUri(profile.avatarUri),
        hermesAvatarUri = validPrivateUri(profile.hermesAvatarUri),
    )

    private fun validPrivateUri(stored: String): String =
        resolvePrivateAvatarFile(stored, avatarDirectory)?.toURI()?.toString().orEmpty()

    private fun decodeScaledBitmap(source: Uri, maxEdge: Int): Bitmap {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val imageSource = ImageDecoder.createSource(context.contentResolver, source)
            ImageDecoder.decodeBitmap(imageSource) { decoder, info, _ ->
                val width = info.size.width.coerceAtLeast(1)
                val height = info.size.height.coerceAtLeast(1)
                val scale = (maxEdge.toFloat() / maxOf(width, height)).coerceAtMost(1f)
                decoder.setTargetSize(
                    (width * scale).roundToInt().coerceAtLeast(1),
                    (height * scale).roundToInt().coerceAtLeast(1),
                )
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(source).use { input ->
                requireNotNull(input) { uiText(R.string.ui_0169, "无法读取所选图片") }
                BitmapFactory.decodeStream(input, null, bounds)
            }
            require(bounds.outWidth > 0 && bounds.outHeight > 0) { uiText(R.string.ui_0170, "所选文件不是有效图片") }
            var sampleSize = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / sampleSize > maxEdge * 2) {
                sampleSize *= 2
            }
            val decoded = context.contentResolver.openInputStream(source).use { input ->
                requireNotNull(input) { uiText(R.string.ui_0169, "无法读取所选图片") }
                BitmapFactory.decodeStream(
                    input,
                    null,
                    BitmapFactory.Options().apply { inSampleSize = sampleSize },
                )
            } ?: error(uiText(R.string.ui_0170, "所选文件不是有效图片"))
            val longestEdge = maxOf(decoded.width, decoded.height)
            if (longestEdge <= maxEdge) {
                decoded
            } else {
                val scale = maxEdge.toFloat() / longestEdge
                Bitmap.createScaledBitmap(
                    decoded,
                    (decoded.width * scale).roundToInt().coerceAtLeast(1),
                    (decoded.height * scale).roundToInt().coerceAtLeast(1),
                    true,
                ).also { scaled ->
                    if (scaled !== decoded) decoded.recycle()
                }
            }
        }
    }

    private companion object {
        const val AVATAR_DIRECTORY = "avatars"
        const val MAX_AVATAR_EDGE = 1024
        const val MAX_CROP_SOURCE_EDGE = 2048
        const val AVATAR_QUALITY = 90
    }
}

internal fun cropAvatarBitmap(source: Bitmap, spec: AvatarCropSpec, maxEdge: Int = 1024): Bitmap {
    val bounds = avatarCropBounds(source.width, source.height, spec)
    val cropped = Bitmap.createBitmap(source, bounds.left, bounds.top, bounds.size, bounds.size)
    if (bounds.size <= maxEdge) return cropped
    val scaled = Bitmap.createScaledBitmap(cropped, maxEdge, maxEdge, true)
    if (scaled !== cropped && cropped !== source && !cropped.isRecycled) cropped.recycle()
    return scaled
}

internal data class AvatarCropBounds(val left: Int, val top: Int, val size: Int)

internal fun avatarCropBounds(width: Int, height: Int, spec: AvatarCropSpec): AvatarCropBounds {
    require(width > 0 && height > 0) { uiText(R.string.ui_0171, "图片尺寸无效") }
    val zoom = spec.zoom.coerceIn(1f, 4f)
    val side = (minOf(width, height) / zoom).roundToInt().coerceAtLeast(1)
    val horizontalSpace = (width - side).coerceAtLeast(0)
    val verticalSpace = (height - side).coerceAtLeast(0)
    val left = (horizontalSpace * ((spec.horizontalBias.coerceIn(-1f, 1f) + 1f) / 2f))
        .roundToInt().coerceIn(0, horizontalSpace)
    val top = (verticalSpace * ((spec.verticalBias.coerceIn(-1f, 1f) + 1f) / 2f))
        .roundToInt().coerceIn(0, verticalSpace)
    return AvatarCropBounds(left, top, side)
}

internal fun resolvePrivateAvatarFile(stored: String, avatarDirectory: File): File? {
    if (stored.isBlank()) return null
    val parsed = runCatching { URI(stored) }.getOrNull() ?: return null
    if (!parsed.scheme.equals("file", ignoreCase = true)) return null
    val candidate = runCatching { File(parsed).canonicalFile }.getOrNull() ?: return null
    val root = runCatching { avatarDirectory.canonicalFile }.getOrNull() ?: return null
    val insideRoot = candidate.path.startsWith(root.path + File.separator)
    return candidate.takeIf { insideRoot && it.isFile }
}

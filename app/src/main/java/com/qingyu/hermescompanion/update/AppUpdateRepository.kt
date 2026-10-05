package com.qingyu.hermescompanion.update

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import com.qingyu.hermescompanion.BuildConfig
import java.io.File
import java.security.MessageDigest

internal data class PendingAppDownload(val id: Long, val release: AppRelease)
internal data class AppDownloadProgress(val status: Int, val bytes: Long, val reason: Int)

/** All durable state belongs to this installation; no gateway credentials or workspace access. */
internal class AppUpdateRepository(private val context: Context) {
    private val prefs = context.getSharedPreferences("app_updates", Context.MODE_PRIVATE)
    private val manager = context.getSystemService(DownloadManager::class.java)
    private val client = ReleaseClient()
    var automatic: Boolean
        get() = prefs.getBoolean("automatic", true)
        set(value) { prefs.edit().putBoolean("automatic", value).apply() }
    var wifiOnly: Boolean
        get() = prefs.getBoolean("wifi_only", true)
        set(value) { prefs.edit().putBoolean("wifi_only", value).apply() }
    fun shouldCheck(now: Long): Boolean = automatic &&
        (now < prefs.getLong("last_attempt", 0) || now - prefs.getLong("last_attempt", 0) >= 86_400_000L)
    fun markAttempt(now: Long) { prefs.edit().putLong("last_attempt", now).apply() }
    fun cachedRelease(): AppRelease? = parseStored("release")
    fun fetch(): AppRelease = client.fetch(context.packageName).also {
        prefs.edit().putString("release", it.toJson()).apply()
    }
    private fun parseStored(key: String): AppRelease? = prefs.getString(key, null)?.let {
        runCatching { AppRelease.parse(it, context.packageName) }.getOrNull()
    }
    fun pending(): PendingAppDownload? {
        val id = prefs.getLong("download_id", -1)
        return if (id >= 0) parseStored("download_release")?.let { PendingAppDownload(id, it) } else null
    }
    private fun destination(release: AppRelease): File {
        val base = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: throw UpdateFailure(UpdateError.STORAGE)
        return File(base, "app-updates/hermes-${release.versionCode}.apk")
    }
    fun start(release: AppRelease): PendingAppDownload {
        if (release.versionCode <= BuildConfig.VERSION_CODE || release.minSdk > Build.VERSION.SDK_INT)
            throw UpdateFailure(UpdateError.INCOMPATIBLE)
        clearDownload()
        val file = destination(release)
        if (!file.parentFile!!.isDirectory && !file.parentFile!!.mkdirs()) throw UpdateFailure(UpdateError.STORAGE)
        if (file.exists() && !file.delete()) throw UpdateFailure(UpdateError.STORAGE)
        if (file.parentFile!!.usableSpace < release.sizeBytes + 16L * 1024 * 1024) throw UpdateFailure(UpdateError.STORAGE)
        val request = DownloadManager.Request(Uri.parse(release.apkUrl))
            .setTitle("Hermes ${release.versionName}")
            .setMimeType("application/vnd.android.package-archive")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            .setAllowedOverRoaming(false)
            .setAllowedOverMetered(!wifiOnly)
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS,
                "app-updates/${file.name}")
        if (wifiOnly) request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI)
        val id = manager.enqueue(request)
        if (!prefs.edit().putLong("download_id", id).putString("download_release", release.toJson()).commit()) {
            manager.remove(id)
            throw UpdateFailure(UpdateError.STORAGE)
        }
        return PendingAppDownload(id, release)
    }
    fun progress(download: PendingAppDownload): AppDownloadProgress {
        manager.query(DownloadManager.Query().setFilterById(download.id))?.use { cursor ->
            if (cursor.moveToFirst()) {
                val bytes = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                if (bytes > download.release.sizeBytes) {
                    clearDownload()
                    throw UpdateFailure(UpdateError.INTEGRITY)
                }
                return AppDownloadProgress(
                    cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)), bytes,
                    cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON)))
            }
        }
        throw UpdateFailure(UpdateError.MISSING_FILE)
    }
    fun clearDownload() {
        pending()?.let {
            manager.remove(it.id)
            runCatching { destination(it.release).delete() }
        }
        prefs.edit().remove("download_id").remove("download_release").commit()
    }
    @Suppress("DEPRECATION")
    fun verify(release: AppRelease): File {
        val file = destination(release)
        verifyReleaseBytes(file, release)
        val flags = if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
        val archive = context.packageManager.getPackageArchiveInfo(file.path, flags)
            ?: throw UpdateFailure(UpdateError.INTEGRITY)
        val installed = context.packageManager.getPackageInfo(context.packageName, flags)
        fun PackageInfo.code() = if (Build.VERSION.SDK_INT >= 28) longVersionCode else versionCode.toLong()
        fun PackageInfo.signers(): Set<String> {
            val signatures = if (Build.VERSION.SDK_INT >= 28) signingInfo?.apkContentsSigners else signatures
            return signatures.orEmpty().map { sig ->
                MessageDigest.getInstance("SHA-256").digest(sig.toByteArray()).joinToString("") { "%02x".format(it) }
            }.toSet()
        }
        verifyReleaseIdentity(release, archive.packageName, archive.code(), archive.versionName,
            installed.code(), installed.signers(), archive.signers())
        if (archive.applicationInfo?.minSdkVersion?.let { it > Build.VERSION.SDK_INT } != false)
            throw UpdateFailure(UpdateError.INCOMPATIBLE)
        return file
    }
    fun installerIntent(release: AppRelease): Intent {
        // Revalidate immediately before granting temporary file access to the system installer.
        val file = verify(release)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        return Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            .apply { clipData = android.content.ClipData.newRawUri("Hermes update", uri) }
    }
}

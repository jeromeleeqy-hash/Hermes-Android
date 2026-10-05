package com.qingyu.hermescompanion.update

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

const val APP_UPDATE_URL = "https://download.leaier.com/releases/android-latest.json"
private const val MAX_MANIFEST_BYTES = 64 * 1024L

/** Public metadata only. Never use the Agent's authenticated HTTP client here. */
data class AppRelease(
    val packageName: String,
    val versionCode: Long,
    val versionName: String,
    val apkUrl: String,
    val sha256: String,
    val sizeBytes: Long,
    val minSdk: Int,
    val notes: List<String>,
    val notesEn: List<String> = emptyList(),
) {
    fun toJson(): String = JSONObject().apply {
        put("schemaVersion", 1); put("packageName", packageName)
        put("versionCode", versionCode); put("versionName", versionName)
        put("apkUrl", apkUrl); put("sha256", sha256); put("sizeBytes", sizeBytes); put("minSdk", minSdk)
        put("notes", org.json.JSONArray(notes)); put("notesEn", org.json.JSONArray(notesEn))
    }.toString()

    companion object {
        fun parse(text: String, expectedPackage: String): AppRelease {
            try {
                require(text.toByteArray(Charsets.UTF_8).size <= MAX_MANIFEST_BYTES)
                val obj = JSONObject(text)
                fun integer(key: String): Long {
                    val value = obj.get(key)
                    require(value is Int || value is Long) { key }
                    return (value as Number).toLong()
                }
                fun string(key: String): String = (obj.get(key) as? String ?: error(key)).also {
                    require(it.isNotBlank() && !it.any { c -> c.isISOControl() })
                }
                require(integer("schemaVersion") == 1L)
                val pkg = string("packageName")
                if (pkg != expectedPackage) throw UpdateFailure(UpdateError.WRONG_PACKAGE)
                val code = integer("versionCode").also { require(it in 1..2_100_000_000L) }
                val name = string("versionName").also { require(it.matches(Regex("[0-9][A-Za-z0-9.+_-]{0,39}"))) }
                val url = string("apkUrl").toHttpUrlOrNull() ?: error("apkUrl")
                require(url.isHttps && url.host == "download.leaier.com" && url.port == 443)
                require(url.username.isEmpty() && url.password.isEmpty() && url.query == null && url.fragment == null)
                require(url.encodedPath.matches(Regex("/releases/[A-Za-z0-9][A-Za-z0-9._-]*\\.apk")))
                val hash = string("sha256").lowercase().also { require(it.matches(Regex("[0-9a-f]{64}"))) }
                val size = integer("sizeBytes").also { require(it in 1..512L * 1024 * 1024) }
                val sdk = integer("minSdk").also { require(it in 26..1000) }.toInt()
                fun notes(key: String): List<String> {
                    if (!obj.has(key)) return emptyList()
                    val items = obj.getJSONArray(key)
                    require(items.length() <= 12)
                    return (0 until items.length()).map { i ->
                        (items.get(i) as? String ?: error(key)).also {
                            require(it.isNotBlank() && it.length <= 500 && !it.any { c -> c.isISOControl() && c != '\n' })
                        }
                    }
                }
                return AppRelease(pkg, code, name, url.toString(), hash, size, sdk, notes("notes"), notes("notesEn"))
            } catch (e: UpdateFailure) { throw e }
            catch (e: Exception) { throw UpdateFailure(UpdateError.INVALID_MANIFEST, e) }
        }
    }
}

enum class UpdateError {
    NOT_PUBLISHED, FORBIDDEN, SERVER, NETWORK, INVALID_MANIFEST, WRONG_PACKAGE,
    INTEGRITY, SIGNATURE, INCOMPATIBLE, STORAGE, DOWNLOAD, MISSING_FILE, INSTALLER, PERMISSION,
}
class UpdateFailure(val reason: UpdateError, cause: Throwable? = null) : IOException(reason.name, cause)

class ReleaseClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS).readTimeout(15, TimeUnit.SECONDS)
        .callTimeout(20, TimeUnit.SECONDS).followRedirects(false).followSslRedirects(false).build(),
) {
    // URL parameter is an internal test seam, never a server or user-supplied setting.
    internal fun fetch(packageName: String, url: String = APP_UPDATE_URL): AppRelease {
        client.newCall(Request.Builder().url(url).header("Cache-Control", "no-cache")
            .header("Accept", "application/json").build()).execute().use { response ->
            if (!response.isSuccessful) throw UpdateFailure(when (response.code) {
                404 -> UpdateError.NOT_PUBLISHED
                401, 403 -> UpdateError.FORBIDDEN
                else -> UpdateError.SERVER
            })
            val body = response.body ?: throw UpdateFailure(UpdateError.INVALID_MANIFEST)
            if (body.contentLength() > MAX_MANIFEST_BYTES) throw UpdateFailure(UpdateError.INVALID_MANIFEST)
            val source = body.source()
            if (source.request(MAX_MANIFEST_BYTES + 1)) throw UpdateFailure(UpdateError.INVALID_MANIFEST)
            return AppRelease.parse(source.readUtf8(), packageName)
        }
    }
}

internal fun verifyReleaseBytes(file: File, release: AppRelease) {
    if (!file.isFile) throw UpdateFailure(UpdateError.MISSING_FILE)
    if (file.length() != release.sizeBytes) throw UpdateFailure(UpdateError.INTEGRITY)
    val digest = MessageDigest.getInstance("SHA-256")
    file.inputStream().buffered().use { input ->
        val buffer = ByteArray(64 * 1024)
        while (true) {
            val size = input.read(buffer)
            if (size < 0) break
            digest.update(buffer, 0, size)
        }
    }
    val actual = digest.digest().joinToString("") { "%02x".format(it) }
    if (actual != release.sha256) throw UpdateFailure(UpdateError.INTEGRITY)
}

internal fun verifyReleaseIdentity(
    release: AppRelease, actualPackage: String, actualCode: Long, actualName: String?,
    installedCode: Long, installedSigners: Set<String>, archiveSigners: Set<String>,
) {
    if (actualPackage != release.packageName) throw UpdateFailure(UpdateError.WRONG_PACKAGE)
    if (actualCode != release.versionCode || actualName != release.versionName || actualCode <= installedCode)
        throw UpdateFailure(UpdateError.INTEGRITY)
    // Deliberately require the same current signing identity. Key rotation needs an explicit migration.
    if (installedSigners.isEmpty() || archiveSigners != installedSigners) throw UpdateFailure(UpdateError.SIGNATURE)
}

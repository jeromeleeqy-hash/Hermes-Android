package com.qingyu.hermescompanion.today

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class TodayCacheScope(val server: String, val account: String, val profile: String) {
    // Length-safe encoding; credentials and identifiers never appear in filenames.
    val key: String get() = MessageDigest.getInstance("SHA-256").digest(
        JSONArray(listOf(server.trimEnd('/'), account, profile)).toString().toByteArray(Charsets.UTF_8)
    ).joinToString("") { "%02x".format(it) }
}

internal interface TodayCipher {
    fun seal(bytes: ByteArray, scope: String): ByteArray
    fun open(bytes: ByteArray, scope: String): ByteArray
}

/** App-private, encrypted, one last-good snapshot per server/account/profile. No source bodies. */
class TodayCache internal constructor(private val directory: File, private val cipher: TodayCipher) {
    constructor(context: Context) : this(File(context.noBackupFilesDir, "today-cache"), AesTodayCipher(::androidTodayKey))
    private var generation = 0L
    @Synchronized fun ticket(): Long = ++generation
    @Synchronized fun invalidateWrites() { generation++ }

    @Synchronized fun read(scope: TodayCacheScope): TodayState? {
        val file = AtomicFile(File(directory, scope.key))
        if (!file.baseFile.exists()) return null
        require(file.baseFile.length() <= TodayBoard.MAX_BYTES * 7L) { "Invalid local overview size" }
        val payload = JSONObject(String(cipher.open(file.readFully(), scope.key), Charsets.UTF_8))
        require(payload.getInt("cache_version") == 1 && payload.getString("scope") == scope.key)
        val root = payload.getString("root")
        val raw = payload.getString("json")
        val board = TodayBoard.decode(raw, root)
        val library = payload.getJSONArray("library").let { values ->
            require(values.length() <= 500)
            (0 until values.length()).map { i ->
                val item = values.getJSONObject(i)
                val path = item.getString("path")
                require(safeTodayPath(root, path) != null)
                TodayLibraryEntry(item.getString("name"), path, item.getBoolean("directory"))
            }
        }
        return TodayState(profile = scope.profile, root = root, board = board, library = library,
            loaded = true, fileExists = true, rawJson = raw, fromCache = true, syncedAt = payload.getLong("synced_at"), localSaved = true,
            filePath = payload.optString("file_path", com.qingyu.hermescompanion.data.joinServerPath(root, TodayBoard.FILE)).also {
                require(it in listOf(TodayBoard.FILE, TodayBoard.PATH).map { relative -> com.qingyu.hermescompanion.data.joinServerPath(root, relative) })
            })
    }

    @Synchronized fun save(scope: TodayCacheScope, state: TodayState, ticket: Long): Boolean {
        if (ticket != generation) return false
        require(state.profile == scope.profile && state.rootVerified && state.error == null)
        val raw = requireNotNull(state.rawJson)
        TodayBoard.decode(raw, state.root) // A malformed response never replaces the last good copy.
        val payload = JSONObject().apply {
            put("cache_version", 1); put("scope", scope.key); put("root", state.root)
            put("file_path", state.filePath.ifBlank { com.qingyu.hermescompanion.data.joinServerPath(state.root, TodayBoard.FILE) }); put("json", raw); put("synced_at", requireNotNull(state.syncedAt))
            put("library", JSONArray(state.library.take(500).map { entry ->
                require(safeTodayPath(state.root, entry.path) != null)
                JSONObject().put("name", entry.name).put("path", entry.path).put("directory", entry.isDirectory)
            }))
        }.toString().toByteArray(Charsets.UTF_8)
        val encrypted = cipher.seal(payload, scope.key)
        require(directory.isDirectory || directory.mkdirs()) { "Cannot create local overview cache" }
        val file = AtomicFile(File(directory, scope.key))
        val stream = file.startWrite()
        try { stream.write(encrypted); file.finishWrite(stream) }
        catch (e: Exception) { file.failWrite(stream); throw e }
        return true
    }

    @Synchronized fun remove(scope: TodayCacheScope, ticket: Long) {
        if (ticket == generation) AtomicFile(File(directory, scope.key)).delete()
    }

    @Synchronized fun clear() {
        generation++ // Prevent an already-running sync from restoring data after logout.
        directory.listFiles()?.forEach { require(it.delete() || !it.exists()) { "Cannot clear local overview" } }
    }
}

private fun androidTodayKey(): SecretKey {
        val alias = "hermes-today-cache-v1"
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(alias, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256).build())
        }.generateKey()
}

internal class AesTodayCipher(private val key: () -> SecretKey) : TodayCipher {
    override fun seal(bytes: ByteArray, scope: String): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        cipher.updateAAD(scope.toByteArray(Charsets.UTF_8))
        require(cipher.iv.size == 12)
        return byteArrayOf(1) + cipher.iv + cipher.doFinal(bytes)
    }

    override fun open(bytes: ByteArray, scope: String): ByteArray {
        require(bytes.size >= 29 && bytes[0] == 1.toByte()) { "Invalid encrypted overview" }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes.copyOfRange(1, 13)))
        cipher.updateAAD(scope.toByteArray(Charsets.UTF_8))
        return cipher.doFinal(bytes.copyOfRange(13, bytes.size))
    }
}

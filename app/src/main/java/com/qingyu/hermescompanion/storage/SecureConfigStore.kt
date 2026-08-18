package com.qingyu.hermescompanion.storage

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.core.content.edit
import com.qingyu.hermescompanion.model.ConnectionConfig
import com.qingyu.hermescompanion.model.RecentArtifact
import com.qingyu.hermescompanion.model.AgentRequest
import com.qingyu.hermescompanion.model.AgentRequestChoice
import com.qingyu.hermescompanion.model.AgentRequestType
import com.qingyu.hermescompanion.model.ChatArtifact
import com.qingyu.hermescompanion.model.RunCompletionSummary
import com.qingyu.hermescompanion.model.ActiveRunSnapshot
import com.qingyu.hermescompanion.model.NotificationPreferences
import com.qingyu.hermescompanion.model.VoicePreferences
import com.qingyu.hermescompanion.model.UserProfilePreferences
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import org.json.JSONArray
import org.json.JSONObject

class SecureConfigStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun read(): ConnectionConfig? = runCatching {
        val baseUrl = preferences.getString(KEY_GATEWAY_URL, null)?.takeIf { it.isNotBlank() } ?: return null
        val username = preferences.getString(KEY_USERNAME, null)?.takeIf { it.isNotBlank() } ?: return null
        ConnectionConfig(baseUrl = baseUrl, username = username)
    }.getOrNull()

    fun save(config: ConnectionConfig) {
        val previousGateway = preferences.getString(KEY_GATEWAY_URL, null)
        preferences.edit {
            if (!previousGateway.isNullOrBlank() && previousGateway != config.baseUrl) {
                remove(KEY_RECENT_ARTIFACTS)
                remove(KEY_ARTIFACT_INDEX_SNAPSHOT)
                remove(KEY_PENDING_AGENT_REQUESTS)
                remove(KEY_RECENT_COMPLETIONS)
                remove(KEY_ACTIVE_RUN_SNAPSHOT)
            }
            putString(KEY_GATEWAY_URL, config.baseUrl)
            putString(KEY_USERNAME, config.username)
        }
    }

    fun readCookies(): String? = runCatching {
        val encrypted = preferences.getString(KEY_AUTH_COOKIES, null)
            ?.takeIf { it.isNotBlank() }
            ?: return@runCatching null
        decrypt(encrypted)
    }.getOrNull()

    fun saveCookies(json: String) {
        preferences.edit {
            if (json.isBlank() || json == "[]") remove(KEY_AUTH_COOKIES)
            else putString(KEY_AUTH_COOKIES, encrypt(json))
        }
    }

    fun clearCookies() {
        preferences.edit { remove(KEY_AUTH_COOKIES) }
    }

    fun clear() {
        preferences.edit {
            remove(KEY_GATEWAY_URL)
            remove(KEY_USERNAME)
            remove(KEY_AUTH_COOKIES)
            remove(KEY_RECENT_ARTIFACTS)
            remove(KEY_ARTIFACT_INDEX_SNAPSHOT)
            remove(KEY_PENDING_AGENT_REQUESTS)
            remove(KEY_RECENT_COMPLETIONS)
            remove(KEY_ACTIVE_RUN_SNAPSHOT)
            // Remove keys written by the API-key based 0.1 prototype.
            remove(LEGACY_KEY_BASE_URL)
            remove(LEGACY_KEY_API_KEY)
        }
    }

    fun readThemeMode(): String? = runCatching { preferences.getString(KEY_THEME_MODE, null) }.getOrNull()

    fun saveThemeMode(value: String) {
        preferences.edit { putString(KEY_THEME_MODE, value) }
    }

    fun readSkinMode(): String? = runCatching { preferences.getString(KEY_SKIN_MODE, null) }.getOrNull()

    fun saveSkinMode(value: String) {
        preferences.edit { putString(KEY_SKIN_MODE, value) }
    }

    fun readNotificationPreferences(): NotificationPreferences = runCatching {
        NotificationPreferences(
            enabled = preferences.getBoolean(KEY_NOTIFICATION_ENABLED, true),
            messageAlerts = preferences.getBoolean(KEY_NOTIFICATION_MESSAGES, true),
            taskAlerts = preferences.getBoolean(KEY_NOTIFICATION_TASKS, true),
            sound = preferences.getBoolean(KEY_NOTIFICATION_SOUND, true),
            vibration = preferences.getBoolean(KEY_NOTIFICATION_VIBRATION, true),
            badge = preferences.getBoolean(KEY_NOTIFICATION_BADGE, true),
        )
    }.getOrDefault(NotificationPreferences())

    fun saveNotificationPreferences(value: NotificationPreferences) {
        preferences.edit {
            putBoolean(KEY_NOTIFICATION_ENABLED, value.enabled)
            putBoolean(KEY_NOTIFICATION_MESSAGES, value.messageAlerts)
            putBoolean(KEY_NOTIFICATION_TASKS, value.taskAlerts)
            putBoolean(KEY_NOTIFICATION_SOUND, value.sound)
            putBoolean(KEY_NOTIFICATION_VIBRATION, value.vibration)
            putBoolean(KEY_NOTIFICATION_BADGE, value.badge)
        }
    }

    fun readVoicePreferences(): VoicePreferences = runCatching {
        VoicePreferences(
            enabled = preferences.getBoolean(KEY_VOICE_ENABLED, true),
            language = preferences.getString(KEY_VOICE_LANGUAGE, "zh-CN").orEmpty().ifBlank { "zh-CN" },
            transcriptScript = preferences.getString(KEY_VOICE_TRANSCRIPT_SCRIPT, "simplified").orEmpty().ifBlank { "simplified" },
            autoSend = preferences.getBoolean(KEY_VOICE_AUTO_SEND, false),
            engine = preferences.getString(KEY_VOICE_ENGINE, "automatic").orEmpty().ifBlank { "automatic" },
            autoRead = preferences.getBoolean(KEY_VOICE_AUTO_READ, true),
            continuous = preferences.getBoolean(KEY_VOICE_CONTINUOUS, false),
            speechRate = preferences.getFloat(KEY_VOICE_SPEECH_RATE, 1.0f),
        )
    }.getOrDefault(VoicePreferences())

    fun saveVoicePreferences(value: VoicePreferences) {
        preferences.edit {
            putBoolean(KEY_VOICE_ENABLED, value.enabled)
            putString(KEY_VOICE_LANGUAGE, value.language)
            putString(KEY_VOICE_TRANSCRIPT_SCRIPT, value.transcriptScript)
            putBoolean(KEY_VOICE_AUTO_SEND, value.autoSend)
            putString(KEY_VOICE_ENGINE, value.engine)
            putBoolean(KEY_VOICE_AUTO_READ, value.autoRead)
            putBoolean(KEY_VOICE_CONTINUOUS, value.continuous)
            putFloat(KEY_VOICE_SPEECH_RATE, value.speechRate)
        }
    }

    fun readUserProfile(): UserProfilePreferences = runCatching {
        UserProfilePreferences(
            displayName = preferences.getString(KEY_PROFILE_NAME, "").orEmpty(),
            bio = preferences.getString(KEY_PROFILE_BIO, "个人工作助理").orEmpty().ifBlank { "个人工作助理" },
            avatarUri = preferences.getString(KEY_PROFILE_AVATAR, "").orEmpty(),
            hermesDisplayName = preferences.getString(KEY_HERMES_PROFILE_NAME, "Hermes").orEmpty().ifBlank { "Hermes" },
            hermesAvatarUri = preferences.getString(KEY_HERMES_PROFILE_AVATAR, "").orEmpty(),
        )
    }.getOrDefault(UserProfilePreferences())

    fun saveUserProfile(value: UserProfilePreferences) {
        preferences.edit {
            putString(KEY_PROFILE_NAME, value.displayName.trim())
            putString(KEY_PROFILE_BIO, value.bio.trim())
            putString(KEY_PROFILE_AVATAR, value.avatarUri)
            putString(KEY_HERMES_PROFILE_NAME, value.hermesDisplayName.trim().ifBlank { "Hermes" })
            putString(KEY_HERMES_PROFILE_AVATAR, value.hermesAvatarUri)
        }
    }

    fun readActiveHermesProfile(): String = runCatching {
        preferences.getString(KEY_ACTIVE_HERMES_PROFILE, "default").orEmpty().ifBlank { "default" }
    }.getOrDefault("default")

    fun saveActiveHermesProfile(value: String) {
        preferences.edit { putString(KEY_ACTIVE_HERMES_PROFILE, value.trim().ifBlank { "default" }) }
    }

    fun readDraft(profile: String, sessionId: String): String = runCatching {
        preferences.getString(draftKey(profile, sessionId), "").orEmpty().take(MAX_DRAFT_LENGTH)
    }.getOrDefault("")

    fun saveDraft(profile: String, sessionId: String, value: String) {
        if (profile.isBlank() || sessionId.isBlank()) return
        preferences.edit {
            if (value.isBlank()) remove(draftKey(profile, sessionId))
            else putString(draftKey(profile, sessionId), value.take(MAX_DRAFT_LENGTH))
        }
    }

    fun clearDraft(profile: String, sessionId: String) {
        if (profile.isBlank() || sessionId.isBlank()) return
        preferences.edit { remove(draftKey(profile, sessionId)) }
    }

    fun readUnreadSessionIds(): Set<String> = runCatching {
        preferences.getStringSet(KEY_UNREAD_SESSION_IDS, emptySet()).orEmpty().toSet()
    }.getOrDefault(emptySet())

    fun saveUnreadSessionIds(value: Set<String>) {
        preferences.edit { putStringSet(KEY_UNREAD_SESSION_IDS, value.toSet()) }
    }

    fun readCronSnapshot(): Map<String, String> = runCatching {
        val raw = preferences.getString(KEY_CRON_SNAPSHOT, null).orEmpty()
        if (raw.isBlank()) return@runCatching emptyMap()
        raw.lineSequence().mapNotNull { line ->
            val index = line.indexOf('\t')
            if (index <= 0) null else line.substring(0, index) to line.substring(index + 1)
        }.toMap()
    }.getOrDefault(emptyMap())

    fun saveCronSnapshot(value: Map<String, String>) {
        preferences.edit {
            putString(KEY_CRON_SNAPSHOT, value.entries.joinToString("\n") { "${it.key}\t${it.value}" })
        }
    }

    fun readRecentArtifacts(): List<RecentArtifact> = runCatching {
        preferences.getString(KEY_RECENT_ARTIFACTS, "").orEmpty()
            .lineSequence()
            .filter(String::isNotBlank)
            .mapNotNull { line ->
                val parts = line.split('\t')
                if (parts.size != 8) return@mapNotNull null
                runCatching {
                    RecentArtifact(
                        profile = decodeField(parts[0]),
                        sessionId = decodeField(parts[1]),
                        sessionTitle = decodeField(parts[2]),
                        messageId = decodeField(parts[3]),
                        path = decodeField(parts[4]),
                        name = decodeField(parts[5]),
                        kind = decodeField(parts[6]),
                        seenAtMillis = parts[7].toLong(),
                    )
                }.getOrNull()
            }
            .take(MAX_RECENT_ARTIFACTS)
            .toList()
    }.getOrDefault(emptyList())

    fun saveRecentArtifacts(value: List<RecentArtifact>) {
        val encoded = value.take(MAX_RECENT_ARTIFACTS).joinToString("\n") { item ->
            listOf(
                item.profile,
                item.sessionId,
                item.sessionTitle,
                item.messageId,
                item.path,
                item.name,
                item.kind,
            ).joinToString("\t", postfix = "\t${item.seenAtMillis}") { encodeField(it) }
        }
        preferences.edit { putString(KEY_RECENT_ARTIFACTS, encoded) }
    }

    fun readArtifactIndexSnapshot(): Map<String, String> = runCatching {
        preferences.getString(KEY_ARTIFACT_INDEX_SNAPSHOT, "").orEmpty()
            .lineSequence()
            .filter(String::isNotBlank)
            .mapNotNull { line ->
                val parts = line.split('\t')
                if (parts.size != 2) null else decodeField(parts[0]) to decodeField(parts[1])
            }
            .toMap()
    }.getOrDefault(emptyMap())

    fun saveArtifactIndexSnapshot(value: Map<String, String>) {
        val encoded = value.entries.joinToString("\n") { (key, fingerprint) ->
            "${encodeField(key)}\t${encodeField(fingerprint)}"
        }
        preferences.edit { putString(KEY_ARTIFACT_INDEX_SNAPSHOT, encoded) }
    }

    fun readPendingAgentRequests(): List<AgentRequest> = runCatching {
        val raw = readEncryptedJson(KEY_PENDING_AGENT_REQUESTS) ?: return@runCatching emptyList()
        val array = JSONArray(raw)
        buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val choices = buildList {
                    val values = item.optJSONArray("choices") ?: JSONArray()
                    for (choiceIndex in 0 until values.length()) {
                        val choice = values.optJSONObject(choiceIndex) ?: continue
                        add(AgentRequestChoice(choice.optString("label"), choice.optString("value")))
                    }
                }
                add(
                    AgentRequest(
                        requestId = item.optString("requestId"),
                        runtimeSessionId = item.optString("runtimeSessionId"),
                        conversationId = item.optString("conversationId"),
                        type = runCatching { AgentRequestType.valueOf(item.optString("type")) }
                            .getOrDefault(AgentRequestType.CLARIFICATION),
                        title = item.optString("title"),
                        detail = item.optString("detail"),
                        choices = choices,
                        allowSession = item.optBoolean("allowSession", true),
                        allowPermanent = item.optBoolean("allowPermanent"),
                    ),
                )
            }
        }.filter { it.requestId.isNotBlank() }.take(MAX_PENDING_REQUESTS)
    }.getOrDefault(emptyList())

    fun savePendingAgentRequests(value: List<AgentRequest>) {
        val array = JSONArray()
        value.takeLast(MAX_PENDING_REQUESTS).forEach { request ->
            array.put(
                JSONObject()
                    .put("requestId", request.requestId)
                    .put("runtimeSessionId", request.runtimeSessionId)
                    .put("conversationId", request.conversationId)
                    .put("type", request.type.name)
                    .put("title", request.title)
                    .put("detail", request.detail)
                    .put("allowSession", request.allowSession)
                    .put("allowPermanent", request.allowPermanent)
                    .put("choices", JSONArray().apply {
                        request.choices.forEach { choice ->
                            put(JSONObject().put("label", choice.label).put("value", choice.value))
                        }
                    }),
            )
        }
        saveEncryptedJson(KEY_PENDING_AGENT_REQUESTS, array.toString())
    }

    fun readRecentCompletions(): List<RunCompletionSummary> = runCatching {
        val raw = readEncryptedJson(KEY_RECENT_COMPLETIONS) ?: return@runCatching emptyList()
        val array = JSONArray(raw)
        buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val artifacts = buildList {
                    val values = item.optJSONArray("artifacts") ?: JSONArray()
                    for (artifactIndex in 0 until values.length()) {
                        val artifact = values.optJSONObject(artifactIndex) ?: continue
                        add(ChatArtifact(artifact.optString("path"), artifact.optString("name"), artifact.optString("kind")))
                    }
                }
                add(
                    RunCompletionSummary(
                        sessionId = item.optString("sessionId"),
                        title = item.optString("title"),
                        summary = item.optString("summary"),
                        artifacts = artifacts,
                        completedAtMillis = item.optLong("completedAtMillis"),
                    ),
                )
            }
        }.filter { it.sessionId.isNotBlank() }.take(MAX_RECENT_COMPLETIONS)
    }.getOrDefault(emptyList())

    fun saveRecentCompletions(value: List<RunCompletionSummary>) {
        val array = JSONArray()
        value.take(MAX_RECENT_COMPLETIONS).forEach { completion ->
            array.put(
                JSONObject()
                    .put("sessionId", completion.sessionId)
                    .put("title", completion.title)
                    .put("summary", completion.summary)
                    .put("completedAtMillis", completion.completedAtMillis)
                    .put("artifacts", JSONArray().apply {
                        completion.artifacts.forEach { artifact ->
                            put(JSONObject().put("path", artifact.path).put("name", artifact.name).put("kind", artifact.kind))
                        }
                    }),
            )
        }
        saveEncryptedJson(KEY_RECENT_COMPLETIONS, array.toString())
    }

    fun readActiveRunSnapshot(): ActiveRunSnapshot? = runCatching {
        val raw = readEncryptedJson(KEY_ACTIVE_RUN_SNAPSHOT) ?: return@runCatching null
        val item = JSONObject(raw)
        ActiveRunSnapshot(
            profile = item.optString("profile"),
            sessionId = item.optString("sessionId"),
            title = item.optString("title"),
            submittedPrompt = item.optString("submittedPrompt"),
            baselineAssistantSignature = item.optString("baselineAssistantSignature"),
            startedAtMillis = item.optLong("startedAtMillis"),
        ).takeIf { it.sessionId.isNotBlank() && it.startedAtMillis > 0L }
    }.getOrNull()

    fun saveActiveRunSnapshot(value: ActiveRunSnapshot) {
        saveEncryptedJson(
            KEY_ACTIVE_RUN_SNAPSHOT,
            JSONObject()
                .put("profile", value.profile)
                .put("sessionId", value.sessionId)
                .put("title", value.title)
                .put("submittedPrompt", value.submittedPrompt)
                .put("baselineAssistantSignature", value.baselineAssistantSignature)
                .put("startedAtMillis", value.startedAtMillis)
                .toString(),
        )
    }

    fun clearActiveRunSnapshot() {
        preferences.edit { remove(KEY_ACTIVE_RUN_SNAPSHOT) }
    }

    private fun readEncryptedJson(key: String): String? {
        val payload = preferences.getString(key, null)?.takeIf(String::isNotBlank) ?: return null
        return decrypt(payload)
    }

    private fun saveEncryptedJson(key: String, value: String) {
        preferences.edit {
            if (value.isBlank() || value == "[]") remove(key) else putString(key, encrypt(value))
        }
    }

    private fun encrypt(value: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encrypted = cipher.doFinal(value.toByteArray(StandardCharsets.UTF_8))
        val payload = cipher.iv + encrypted
        return Base64.encodeToString(payload, Base64.NO_WRAP)
    }

    private fun decrypt(payload: String): String {
        val bytes = Base64.decode(payload, Base64.NO_WRAP)
        require(bytes.size > IV_SIZE_BYTES) { "Invalid encrypted configuration" }
        val iv = bytes.copyOfRange(0, IV_SIZE_BYTES)
        val encrypted = bytes.copyOfRange(IV_SIZE_BYTES, bytes.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
        return String(cipher.doFinal(encrypted), StandardCharsets.UTF_8)
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build(),
        )
        return generator.generateKey()
    }

    private fun draftKey(profile: String, sessionId: String): String {
        val scope = "$profile::$sessionId".toByteArray(StandardCharsets.UTF_8)
        return KEY_DRAFT_PREFIX + Base64.encodeToString(scope, Base64.NO_WRAP or Base64.URL_SAFE)
    }

    private fun encodeField(value: String): String = Base64.encodeToString(
        value.toByteArray(StandardCharsets.UTF_8),
        Base64.NO_WRAP or Base64.URL_SAFE,
    )

    private fun decodeField(value: String): String = String(
        Base64.decode(value, Base64.NO_WRAP or Base64.URL_SAFE),
        StandardCharsets.UTF_8,
    )

    private companion object {
        const val PREFERENCES_NAME = "hermes_secure_connection"
        const val KEY_GATEWAY_URL = "gateway_url"
        const val KEY_USERNAME = "gateway_username"
        const val KEY_AUTH_COOKIES = "gateway_auth_cookies"
        const val LEGACY_KEY_BASE_URL = "base_url"
        const val LEGACY_KEY_API_KEY = "api_key"
        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_SKIN_MODE = "skin_mode"
        const val KEY_NOTIFICATION_ENABLED = "notification_enabled"
        const val KEY_NOTIFICATION_MESSAGES = "notification_messages"
        const val KEY_NOTIFICATION_TASKS = "notification_tasks"
        const val KEY_NOTIFICATION_SOUND = "notification_sound"
        const val KEY_NOTIFICATION_VIBRATION = "notification_vibration"
        const val KEY_NOTIFICATION_BADGE = "notification_badge"
        const val KEY_VOICE_ENABLED = "voice_enabled"
        const val KEY_VOICE_LANGUAGE = "voice_language"
        const val KEY_VOICE_TRANSCRIPT_SCRIPT = "voice_transcript_script"
        const val KEY_VOICE_AUTO_SEND = "voice_auto_send"
        const val KEY_VOICE_ENGINE = "voice_engine"
        const val KEY_VOICE_AUTO_READ = "voice_auto_read"
        const val KEY_VOICE_CONTINUOUS = "voice_continuous"
        const val KEY_VOICE_SPEECH_RATE = "voice_speech_rate"
        const val KEY_PROFILE_NAME = "profile_display_name"
        const val KEY_PROFILE_BIO = "profile_bio"
        const val KEY_PROFILE_AVATAR = "profile_avatar_uri"
        const val KEY_HERMES_PROFILE_NAME = "hermes_profile_display_name"
        const val KEY_HERMES_PROFILE_AVATAR = "hermes_profile_avatar_uri"
        const val KEY_ACTIVE_HERMES_PROFILE = "active_hermes_profile"
        const val KEY_DRAFT_PREFIX = "chat_draft_"
        const val MAX_DRAFT_LENGTH = 50_000
        const val KEY_UNREAD_SESSION_IDS = "unread_session_ids"
        const val KEY_CRON_SNAPSHOT = "cron_snapshot"
        const val KEY_RECENT_ARTIFACTS = "recent_artifacts"
        const val KEY_ARTIFACT_INDEX_SNAPSHOT = "artifact_index_snapshot"
        const val KEY_PENDING_AGENT_REQUESTS = "pending_agent_requests"
        const val KEY_RECENT_COMPLETIONS = "recent_completions"
        const val KEY_ACTIVE_RUN_SNAPSHOT = "active_run_snapshot"
        const val MAX_RECENT_ARTIFACTS = 60
        const val MAX_PENDING_REQUESTS = 20
        const val MAX_RECENT_COMPLETIONS = 30
        const val KEY_ALIAS = "hermes_companion_api_key"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_SIZE_BYTES = 12
        const val GCM_TAG_BITS = 128
    }
}

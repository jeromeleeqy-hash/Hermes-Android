package com.qingyu.hermescompanion.data

import com.qingyu.hermescompanion.model.ChatMessage
import com.qingyu.hermescompanion.model.ChatImage
import com.qingyu.hermescompanion.model.ConnectionConfig
import com.qingyu.hermescompanion.model.CronJob
import com.qingyu.hermescompanion.model.CronSchedule
import com.qingyu.hermescompanion.model.HermesSession
import com.qingyu.hermescompanion.model.HermesProject
import com.qingyu.hermescompanion.model.HermesProfile
import com.qingyu.hermescompanion.model.HermesProfileFile
import com.qingyu.hermescompanion.model.SessionPage
import com.qingyu.hermescompanion.model.MessageRole
import com.qingyu.hermescompanion.model.ModelCatalog
import com.qingyu.hermescompanion.model.MessagePage
import com.qingyu.hermescompanion.model.ModelProvider
import com.qingyu.hermescompanion.model.ModelChoice
import com.qingyu.hermescompanion.model.FallbackModel
import com.qingyu.hermescompanion.model.ServerModelSettings
import com.qingyu.hermescompanion.model.ConversationStyleSettings
import com.qingyu.hermescompanion.model.ApprovalSettings
import com.qingyu.hermescompanion.model.AgentRequest
import com.qingyu.hermescompanion.model.AgentRequestChoice
import com.qingyu.hermescompanion.model.AgentRequestType
import com.qingyu.hermescompanion.model.MemoryContextSettings
import com.qingyu.hermescompanion.model.ServerSettings
import com.qingyu.hermescompanion.model.ServerSttSettings
import com.qingyu.hermescompanion.model.ServerTtsSettings
import com.qingyu.hermescompanion.model.ServerVoiceSettings
import com.qingyu.hermescompanion.model.SlashCommand
import com.qingyu.hermescompanion.model.ServerSkill
import com.qingyu.hermescompanion.model.ToolsetInfo
import com.qingyu.hermescompanion.model.McpServerInfo
import com.qingyu.hermescompanion.model.PendingAttachment
import com.qingyu.hermescompanion.model.ImagePreview
import com.qingyu.hermescompanion.model.GatewayInfo
import com.qingyu.hermescompanion.model.AgentUpdateCommit
import com.qingyu.hermescompanion.model.AgentUpdateInfo
import com.qingyu.hermescompanion.model.AgentUpdateProgress
import com.qingyu.hermescompanion.model.SpeechAudio
import com.qingyu.hermescompanion.model.SpeechTranscription
import com.qingyu.hermescompanion.model.StreamEvent
import com.qingyu.hermescompanion.model.WorkspaceDocument
import com.qingyu.hermescompanion.model.WorkspaceEntry
import com.qingyu.hermescompanion.model.WorkspaceListing
import com.qingyu.hermescompanion.storage.SecureCookieJar
import com.qingyu.hermescompanion.ui.format.compactSessionTitle
import com.qingyu.hermescompanion.ui.format.resolvedSessionTitle
import android.util.Base64
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

internal fun parseServerVoiceSettings(config: JSONObject): ServerVoiceSettings {
    val stt = config.optJSONObject("stt") ?: JSONObject()
    val sttProvider = stt.optString("provider").trim().ifBlank { "local" }
    val sttProviderConfig = stt.optJSONObject(sttProvider) ?: JSONObject()
    val sttModel = sttProviderConfig.optString("model").trim()
        .ifBlank { stt.optString("model").trim() }
        .ifBlank { defaultServerSttModel(sttProvider) }

    val tts = config.optJSONObject("tts") ?: JSONObject()
    val ttsProvider = tts.optString("provider").trim().ifBlank { "edge" }
    val ttsProviderConfig = tts.optJSONObject(ttsProvider) ?: JSONObject()
    val ttsModel = ttsProviderConfig.optString("model").trim()
        .ifBlank { ttsProviderConfig.optString("model_id").trim() }
    val ttsVoice = ttsProviderConfig.optString("voice").trim()
        .ifBlank { ttsProviderConfig.optString("voice_id").trim() }

    return ServerVoiceSettings(
        stt = ServerSttSettings(
            enabled = !stt.has("enabled") || stt.optBoolean("enabled"),
            provider = sttProvider,
            model = sttModel,
            language = sttProviderConfig.optString("language").trim(),
        ),
        tts = ServerTtsSettings(
            provider = ttsProvider,
            model = ttsModel,
            voice = ttsVoice,
        ),
    )
}

private fun defaultServerSttModel(provider: String): String = when (provider) {
    "groq" -> "whisper-large-v3-turbo"
    "openai" -> "whisper-1"
    "mistral" -> "voxtral-mini-latest"
    "xai" -> "grok-stt"
    else -> "base"
}

class HermesApiClient(
    private val config: ConnectionConfig,
    private val cookieJar: SecureCookieJar,
) {
    private val http = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .pingInterval(20, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val socketLock = Any()
    private val requestIds = AtomicLong(0)
    private val pendingCalls = ConcurrentHashMap<String, CompletableFuture<Any?>>()

    @Volatile
    private var socket: WebSocket? = null

    @Volatile
    private var socketOpen = false

    @Volatile
    private var socketOpenFuture: CompletableFuture<Unit>? = null

    @Volatile
    private var gatewayReadyFuture: CompletableFuture<Unit>? = null

    @Volatile
    private var activeStream: ActiveStream? = null

    @Volatile
    private var activeProfile: String = "default"

    fun setProfile(name: String) {
        activeProfile = name.trim().ifBlank { "default" }
    }

    fun currentProfile(): String = activeProfile

    fun listProfiles(): List<HermesProfile> {
        val raw = try {
            request("GET", "/api/profiles", includeProfile = false)
        } catch (error: ApiException) {
            if (error.statusCode == 404) return listOf(HermesProfile(name = "default", isDefault = true))
            throw error
        }
        return parseHermesProfiles(raw).ifEmpty {
            listOf(HermesProfile(name = "default", isDefault = true))
        }
    }

    fun hasSavedSession(): Boolean = cookieJar.hasCookies()

    fun checkSavedSession(): String {
        checkGatewayStatus()
        return authenticatedUsername()
    }

    fun checkGatewayAccess() {
        checkGatewayStatus()
    }

    fun gatewayInfo(): GatewayInfo {
        val status = checkGatewayStatus()
        val health = runCatching {
            JSONObject(request("GET", "/api/health", includeProfile = false))
        }.getOrDefault(JSONObject())
        val build = status.optJSONObject("build") ?: JSONObject()
        val gateway = status.optJSONObject("gateway") ?: JSONObject()
        val capabilities = buildList {
            status.optJSONArray("capabilities")?.let { array ->
                for (index in 0 until array.length()) {
                    array.optString(index).trim().takeIf(String::isNotBlank)?.let(::add)
                }
            }
            status.optJSONObject("capabilities")?.let { values ->
                values.keys().forEach { key -> if (values.optBoolean(key)) add(key) }
            }
        }.distinct()
        return GatewayInfo(
            agentVersion = (
                firstString(status, "agent_version", "hermes_version", "version")
                    ?: firstString(build, "agent_version", "hermes_version", "version")
                    ?: firstString(health, "agent_version", "hermes_version", "version")
                ).orEmpty(),
            gatewayVersion = (
                firstString(status, "gateway_version", "api_version")
                    ?: firstString(gateway, "version", "api_version")
                ).orEmpty(),
            capabilities = capabilities,
        )
    }

    fun checkAgentUpdate(force: Boolean = false): AgentUpdateInfo {
        val root = JSONObject(
            request(
                "GET",
                "/api/hermes/update/check?force=${if (force) "true" else "false"}",
                includeProfile = false,
            ),
        )
        val commits = buildList {
            val array = root.optJSONArray("commits") ?: JSONArray()
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                add(
                    AgentUpdateCommit(
                        sha = firstString(item, "sha", "hash").orEmpty(),
                        summary = firstString(item, "summary", "message", "title").orEmpty(),
                        author = firstString(item, "author").orEmpty(),
                        at = firstString(item, "at", "date", "timestamp").orEmpty(),
                    ),
                )
            }
        }
        return AgentUpdateInfo(
            currentVersion = firstString(root, "current_version", "version").orEmpty(),
            installMethod = firstString(root, "install_method").orEmpty(),
            behind = root.takeIf { it.has("behind") && !it.isNull("behind") }?.optInt("behind"),
            updateAvailable = root.optBoolean("update_available"),
            canApply = root.optBoolean("can_apply"),
            updateCommand = firstString(root, "update_command").orEmpty(),
            message = firstString(root, "message", "error").orEmpty(),
            commits = commits,
        )
    }

    fun startAgentUpdate(): AgentUpdateProgress {
        val root = JSONObject(request("POST", "/api/hermes/update", "{}", includeProfile = false))
        if (!root.optBoolean("ok", false)) {
            throw ApiException(400, firstString(root, "message", "error") ?: "服务器未能启动 Hermes 更新")
        }
        return AgentUpdateProgress(
            started = true,
            running = true,
            lines = firstString(root, "message").orEmpty(),
        )
    }

    fun agentUpdateStatus(lines: Int = 80): AgentUpdateProgress {
        val root = JSONObject(
            request(
                "GET",
                "/api/actions/hermes-update/status?lines=${lines.coerceIn(20, 300)}",
                includeProfile = false,
            ),
        )
        val output = when (val value = root.opt("lines")) {
            is JSONArray -> buildList {
                for (index in 0 until value.length()) add(value.optString(index))
            }.joinToString("\n")
            else -> value?.toString().orEmpty()
        }
        return AgentUpdateProgress(
            started = true,
            running = root.optBoolean("running"),
            exitCode = root.takeIf { it.has("exit_code") && !it.isNull("exit_code") }?.optInt("exit_code"),
            lines = output,
        )
    }

    fun transcribeAudio(bytes: ByteArray, mimeType: String): SpeechTranscription {
        val encoded = Base64.encodeToString(bytes, Base64.NO_WRAP)
        val body = JSONObject()
            .put("data_url", "data:$mimeType;base64,$encoded")
            .put("mime_type", mimeType)
        val root = JSONObject(request("POST", "/api/audio/transcribe", body.toString()))
        if (!root.optBoolean("ok", true)) {
            throw ApiException(400, firstString(root, "message", "error") ?: "Hermes 语音识别失败")
        }
        val transcript = firstString(root, "transcript", "text").orEmpty().trim()
        if (transcript.isBlank()) throw ApiException(400, "Hermes 没有识别到语音内容")
        return SpeechTranscription(transcript, firstString(root, "provider").orEmpty())
    }

    fun synthesizeSpeech(text: String): SpeechAudio {
        val root = JSONObject(
            request("POST", "/api/audio/speak", JSONObject().put("text", text.take(8_000)).toString()),
        )
        if (!root.optBoolean("ok", true)) {
            throw ApiException(400, firstString(root, "message", "error") ?: "Hermes 语音合成失败")
        }
        val dataUrl = firstString(root, "data_url")
            ?: throw ApiException(500, "Hermes 没有返回语音数据")
        val mimeType = firstString(root, "mime_type")
            ?: dataUrl.substringAfter("data:", "audio/mpeg").substringBefore(';')
        val encoded = dataUrl.substringAfter(',', "")
        if (encoded.isBlank()) throw ApiException(500, "Hermes 返回了无效的语音数据")
        return SpeechAudio(
            bytes = Base64.decode(encoded, Base64.DEFAULT),
            mimeType = mimeType,
            provider = firstString(root, "provider").orEmpty(),
        )
    }

    fun login(username: String, password: String): String {
        checkGatewayStatus()
        val providers = JSONObject(request("GET", "/api/auth/providers"))
            .optJSONArray("providers") ?: JSONArray()
        var passwordProvider: String? = null
        for (index in 0 until providers.length()) {
            val provider = providers.optJSONObject(index) ?: continue
            if (provider.optBoolean("supports_password")) {
                passwordProvider = provider.optString("name").takeIf { it.isNotBlank() }
                if (passwordProvider == "basic") break
            }
        }
        val provider = passwordProvider
            ?: throw ApiException(400, "这个远程网关没有启用用户名密码登录")

        closeSocket()
        cookieJar.clear()
        val body = JSONObject()
            .put("provider", provider)
            .put("username", username)
            .put("password", password)
            .put("next", "")
        request("POST", "/auth/password-login", body.toString())
        return authenticatedUsername()
    }

    fun logout() {
        runCatching { request("POST", "/auth/logout", "{}") }
        closeSocket()
        cookieJar.clear()
    }

    fun listSessions(): SessionPage {
        val raw = request("GET", "/api/sessions?limit=60&offset=0&include_children=false&order=recent")
        val root = JSONTokener(raw).nextValue()
        val array = findArray(root, "sessions", "items", "data") ?: JSONArray()
        val sessions = buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                parseSession(item)?.let(::add)
            }
        }
        val total = (root as? JSONObject)?.let { objectRoot ->
            listOf("total", "total_count", "count")
                .firstNotNullOfOrNull { key ->
                    objectRoot.takeIf { it.has(key) && !it.isNull(key) }
                        ?.optInt(key, -1)
                        ?.takeIf { it >= 0 }
                }
        } ?: sessions.size
        return SessionPage(sessions = sessions, totalCount = total.coerceAtLeast(sessions.size))
    }

    fun listArchivedSessions(): List<HermesSession> {
        val raw = request("GET", "/api/sessions?limit=100&offset=0&include_children=false&order=recent&archived=only")
        val root = JSONTokener(raw).nextValue()
        val array = findArray(root, "sessions", "items", "data") ?: JSONArray()
        return buildList {
            for (index in 0 until array.length()) {
                array.optJSONObject(index)?.let(::parseSession)?.let(::add)
            }
        }
    }

    fun restoreSession(sessionId: String) {
        request(
            "PATCH",
            "/api/sessions/${pathSegment(sessionId)}",
            JSONObject().put("archived", false).toString(),
        )
    }

    fun loadMessages(session: HermesSession, pageSize: Int = 60): List<ChatMessage> =
        loadRecentMessagePage(session, pageSize).messages

    fun loadRecentMessagePage(session: HermesSession, pageSize: Int = 60): MessagePage {
        val offset = (session.messageCount - pageSize).coerceAtLeast(0)
        return MessagePage(
            messages = loadMessagePage(session.id, pageSize, offset),
            offset = offset,
            totalCount = session.messageCount,
        )
    }

    fun loadMessagePage(session: HermesSession, pageSize: Int, offset: Int): MessagePage {
        val safeOffset = offset.coerceAtLeast(0)
        return MessagePage(
            messages = loadMessagePage(session.id, pageSize, safeOffset),
            offset = safeOffset,
            totalCount = session.messageCount,
        )
    }

    fun loadLatestMessages(session: HermesSession): List<ChatMessage> {
        val pageSize = 200
        val root = JSONObject(request("GET", "/api/sessions/${pathSegment(session.id)}"))
        val item = root.optJSONObject("session") ?: root
        val latestCount = item.optInt("message_count", session.messageCount)
        val offset = (latestCount - pageSize).coerceAtLeast(0)
        return loadMessagePage(session.id, pageSize, offset)
    }

    private fun loadMessagePage(sessionId: String, pageSize: Int, offset: Int): List<ChatMessage> {
        val raw = request(
            "GET",
            "/api/sessions/${pathSegment(sessionId)}/messages?limit=$pageSize&offset=$offset",
        )
        val root = JSONTokener(raw).nextValue()
        val array = findArray(root, "messages", "items", "data") ?: JSONArray()
        return parseMessages(array)
    }

    fun createSession(): HermesSession {
        val result = rpcObject(
            "session.create",
            JSONObject()
                .put("cols", 72)
                .put("source", "android"),
        )
        val runtimeId = result.optString("session_id").takeIf { it.isNotBlank() }
            ?: error("远程网关没有返回运行会话 ID")
        val storedId = result.optString("stored_session_id").takeIf { it.isNotBlank() } ?: runtimeId
        val info = result.optJSONObject("info") ?: JSONObject()
        return HermesSession(
            id = storedId,
            title = "新会话",
            source = "android",
            model = firstString(info, "model").orEmpty(),
            provider = firstString(info, "provider").orEmpty(),
            runtimeId = runtimeId,
            profile = activeProfile,
        )
    }

    fun resumeSession(session: HermesSession): ResumedSession {
        val result = rpcObject(
            "session.resume",
            JSONObject()
                .put("session_id", session.id)
                .put("cols", 72)
                .put("source", "android"),
            timeoutSeconds = 120,
        )
        val runtimeId = result.optString("session_id").takeIf { it.isNotBlank() }
            ?: error("远程网关没有返回运行会话 ID")
        val info = result.optJSONObject("info") ?: JSONObject()
        val messages = parseMessages(result.optJSONArray("messages") ?: JSONArray())
        return ResumedSession(
            session.copy(
                runtimeId = runtimeId,
                model = firstString(info, "model") ?: session.model,
                provider = firstString(info, "provider") ?: session.provider,
            ),
            messages,
        )
    }

    fun modelCatalog(): ModelCatalog {
        val root = JSONObject(request("GET", "/api/model/options?explicit_only=1"))
        val current = root.optJSONObject("current")
            ?: root.optJSONObject("main")
            ?: root.optJSONObject("data")?.optJSONObject("current")
            ?: root.optJSONObject("data")?.optJSONObject("main")
            ?: JSONObject()
        val providers = root.optJSONArray("providers") ?: JSONArray()
        return ModelCatalog(
            currentModel = (
                firstString(root, "model", "current_model", "default_model")
                    ?: firstString(current, "model", "default", "current_model")
                ).orEmpty(),
            currentProvider = (
                firstString(root, "provider", "current_provider", "default_provider")
                    ?: firstString(current, "provider", "current_provider")
                ).orEmpty(),
            providers = buildList {
                for (index in 0 until providers.length()) {
                    val item = providers.optJSONObject(index) ?: continue
                    if (item.has("authenticated") && !item.optBoolean("authenticated")) continue
                    val models = item.optJSONArray("models") ?: JSONArray()
                    val ids = buildList {
                        for (modelIndex in 0 until models.length()) {
                            models.optString(modelIndex).trim().takeIf(String::isNotBlank)?.let(::add)
                        }
                    }
                    if (ids.isEmpty()) continue
                    val slug = firstString(item, "slug") ?: continue
                    add(ModelProvider(slug = slug, name = firstString(item, "name") ?: slug, models = ids))
                }
            },
        )
    }

    fun slashCommands(query: String = ""): List<SlashCommand> {
        val cleanQuery = query.trim().removePrefix("/")
        val result = if (cleanQuery.isBlank()) {
            rpcObject("commands.catalog")
        } else {
            rpcObject("complete.slash", JSONObject().put("text", "/$cleanQuery"))
        }
        return buildList {
            fun addPair(pair: JSONArray, category: String) {
                val raw = pair.optString(0).trim()
                if (raw.isBlank()) return
                add(
                    SlashCommand(
                        command = raw.substringBefore(' ').let { if (it.startsWith('/')) it else "/$it" },
                        description = pair.optString(1),
                        category = category,
                        argsHint = raw.substringAfter(' ', ""),
                    ),
                )
            }

            if (cleanQuery.isBlank()) {
                val categories = result.optJSONArray("categories") ?: JSONArray()
                for (categoryIndex in 0 until categories.length()) {
                    val category = categories.optJSONObject(categoryIndex) ?: continue
                    val categoryName = firstString(category, "name", "category", "group").orEmpty()
                    val pairs = category.optJSONArray("pairs") ?: category.optJSONArray("commands") ?: JSONArray()
                    for (pairIndex in 0 until pairs.length()) {
                        when (val item = pairs.opt(pairIndex)) {
                            is JSONArray -> addPair(item, categoryName)
                            is JSONObject -> addCompletionItem(item, categoryName)?.let(::add)
                        }
                    }
                }
                val pairs = result.optJSONArray("pairs") ?: JSONArray()
                for (pairIndex in 0 until pairs.length()) {
                    (pairs.opt(pairIndex) as? JSONArray)?.let { addPair(it, "") }
                }
            } else {
                val items = result.optJSONArray("items") ?: JSONArray()
                for (itemIndex in 0 until items.length()) {
                    when (val item = items.opt(itemIndex)) {
                        is JSONObject -> addCompletionItem(item)?.let(::add)
                        is String -> item.trim().takeIf(String::isNotBlank)?.let { raw ->
                            add(SlashCommand(command = raw.let { if (it.startsWith('/')) it else "/$it" }))
                        }
                    }
                }
            }
        }.distinctBy(SlashCommand::command)
    }

    private fun addCompletionItem(item: JSONObject, fallbackCategory: String = ""): SlashCommand? {
        val raw = completionString(item, "text", "command", "name", "value", "display") ?: return null
        val command = raw.substringBefore(' ').let { if (it.startsWith('/')) it else "/$it" }
        return SlashCommand(
            command = command,
            description = completionString(item, "meta", "description", "help", "detail", "summary").orEmpty(),
            category = completionString(item, "group", "category", "section") ?: fallbackCategory,
            argsHint = completionString(item, "args_hint", "args", "usage").orEmpty(),
        )
    }

    private fun completionString(item: JSONObject, vararg keys: String): String? {
        keys.forEach { key ->
            val value = item.opt(key)
            val text = when (value) {
                is String -> value
                is JSONArray -> buildString {
                    for (index in 0 until value.length()) {
                        val part = value.opt(index)
                        append(
                            when (part) {
                                is JSONArray -> part.optString(1).ifBlank { part.optString(0) }
                                is String -> part
                                else -> ""
                            },
                        )
                    }
                }
                else -> ""
            }.trim()
            if (text.isNotBlank()) return text
        }
        return null
    }

    fun serverSettings(): ServerSettings {
        val parsed = parseServerSettings(readConfig())
        if (parsed.models.provider.isNotBlank() && parsed.models.model.isNotBlank()) return parsed
        val live = runCatching {
            JSONObject(request("GET", "/api/model/auxiliary"))
        }.getOrNull() ?: return parsed
        return parsed.copy(models = mergeLiveModelSettings(parsed.models, live))
    }

    fun saveModelSettings(value: ServerModelSettings): ServerSettings = updateConfig { config ->
        val model = config.ensureObject("model")
        model.put("provider", value.provider)
        model.put("default", value.model)
        if (value.contextLength > 0) model.put("context_length", value.contextLength) else model.remove("context_length")
        config.ensureObject("agent").put("reasoning_effort", value.reasoningEffort)

        val auxiliary = config.ensureObject("auxiliary")
        AUXILIARY_TASK_KEYS.forEach { key ->
            val choice = value.auxiliary[key] ?: ModelChoice()
            auxiliary.ensureObject(key)
                .put("provider", choice.provider.ifBlank { "auto" })
                .put("model", choice.model)
        }
        val fallbacks = JSONArray()
        value.fallbackModels.filter { it.provider.isNotBlank() && it.model.isNotBlank() }.forEach { fallback ->
            fallbacks.put(JSONObject().put("provider", fallback.provider).put("model", fallback.model))
        }
        config.put("fallback_providers", fallbacks)

        val moa = config.ensureObject("moa")
        moa.put("reference_models", JSONArray(value.moaReferenceModels.filter(String::isNotBlank)))
        moa.put("aggregator_model", value.moaAggregatorModel)
        val activePreset = moa.optString("active_preset", "default").ifBlank { "default" }
        val presets = moa.optJSONObject("presets")
        if (presets != null) {
            val preset = presets.ensureObject(activePreset)
            preset.put(
                "reference_models",
                JSONArray().apply {
                    value.moaReferenceModels.filter(String::isNotBlank).forEach { spec ->
                        put(modelSpecObject(spec))
                    }
                },
            )
            preset.put("aggregator", modelSpecObject(value.moaAggregatorModel))
        }
    }

    fun saveConversationStyle(value: ConversationStyleSettings): ServerSettings = updateConfig { config ->
        config.ensureObject("agent").put("personality", value.personality)
        config.put("timezone", value.timezone)
        config.ensureObject("display").put("show_reasoning", value.showReasoning)
    }

    fun saveApprovalSettings(value: ApprovalSettings): ServerSettings = updateConfig { config ->
        config.ensureObject("approvals")
            .put("mode", value.mode)
            .put("timeout", value.timeoutSeconds.coerceIn(10, 3_600))
    }

    fun saveMemorySettings(value: MemoryContextSettings): ServerSettings = updateConfig { config ->
        config.ensureObject("memory")
            .put("memory_enabled", value.memoryEnabled)
            .put("user_profile_enabled", value.userProfileEnabled)
            .put("memory_char_limit", value.memoryCharLimit.coerceAtLeast(256))
            .put("user_char_limit", value.userCharLimit.coerceAtLeast(256))
        config.ensureObject("compression")
            .put("enabled", value.compressionEnabled)
            .put("threshold", value.compressionThreshold.coerceIn(0.10, 0.95))
            .put("target_ratio", value.compressionTargetRatio.coerceIn(0.05, 0.80))
            .put("protect_last_n", value.protectLastMessages.coerceAtLeast(1))
    }

    fun saveVoiceSettings(value: ServerVoiceSettings): ServerSettings = updateConfig { config ->
        val stt = config.ensureObject("stt")
            .put("enabled", value.stt.enabled)
            .put("provider", value.stt.provider)
        val sttProvider = stt.ensureObject(value.stt.provider)
        if (value.stt.model.isNotBlank()) sttProvider.put("model", value.stt.model) else sttProvider.remove("model")
        sttProvider.put("language", value.stt.language)

        val tts = config.ensureObject("tts").put("provider", value.tts.provider)
        val ttsProvider = tts.ensureObject(value.tts.provider)
        if (value.tts.model.isNotBlank()) {
            ttsProvider.put(if (value.tts.provider == "elevenlabs") "model_id" else "model", value.tts.model)
        } else {
            ttsProvider.remove("model")
            ttsProvider.remove("model_id")
        }
        if (value.tts.voice.isNotBlank()) {
            ttsProvider.put(if (value.tts.provider == "elevenlabs") "voice_id" else "voice", value.tts.voice)
        } else {
            ttsProvider.remove("voice")
            ttsProvider.remove("voice_id")
        }
    }

    fun addCustomProvider(
        id: String,
        displayName: String,
        baseUrl: String,
        model: String,
        apiKey: String,
    ): ServerSettings {
        val slug = id.trim().lowercase().replace(Regex("[^a-z0-9_-]+"), "-").trim('-')
        if (slug.isBlank()) throw ApiException(400, "请填写有效的提供商标识")
        val envKey = "HERMES_PROVIDER_${slug.uppercase().replace('-', '_')}_API_KEY"
        if (apiKey.isNotBlank()) {
            request("PUT", "/api/env", JSONObject().put("key", envKey).put("value", apiKey).toString())
        }
        return updateConfig { config ->
            val provider = config.ensureObject("providers").ensureObject(slug)
                .put("name", displayName.trim().ifBlank { slug })
                .put("api", baseUrl.trim().trimEnd('/'))
                .put("transport", "chat_completions")
                .put("default_model", model.trim())
                .put("enabled", true)
            if (apiKey.isNotBlank()) provider.put("key_env", envKey)
        }
    }

    fun listSkills(): List<ServerSkill> {
        val root = JSONTokener(request("GET", "/api/skills")).nextValue()
        val array = findArray(root, "skills", "items", "data") ?: (root as? JSONArray) ?: JSONArray()
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val name = firstString(item, "name", "id") ?: continue
                add(
                    ServerSkill(
                        name = name,
                        description = firstString(item, "description", "summary").orEmpty(),
                        category = firstString(item, "category").orEmpty().ifBlank { "其他" },
                        enabled = !item.has("enabled") || item.optBoolean("enabled"),
                        provenance = firstString(item, "provenance", "source").orEmpty(),
                    ),
                )
            }
        }
    }

    fun setSkillEnabled(name: String, enabled: Boolean) {
        request(
            "PUT",
            "/api/skills/toggle",
            JSONObject().put("name", name).put("enabled", enabled).toString(),
        )
    }

    fun skillContent(name: String): String {
        val root = JSONObject(request("GET", "/api/skills/content?name=${queryValue(name)}"))
        return firstString(root, "content").orEmpty()
    }

    fun listToolsets(): List<ToolsetInfo> {
        val rootValue = JSONTokener(request("GET", "/api/tools/toolsets")).nextValue()
        val array = findArray(rootValue, "toolsets", "items", "data") ?: (rootValue as? JSONArray) ?: JSONArray()
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val name = firstString(item, "name", "id", "key") ?: continue
                val tools = item.optJSONArray("tools") ?: JSONArray()
                add(
                    ToolsetInfo(
                        name = name,
                        label = firstString(item, "label", "display_name") ?: name,
                        description = firstString(item, "description").orEmpty(),
                        tools = (0 until tools.length()).mapNotNull { tools.optString(it).takeIf(String::isNotBlank) },
                        enabled = when {
                            item.has("active") -> item.optBoolean("active")
                            item.has("enabled") -> item.optBoolean("enabled")
                            else -> true
                        },
                        configured = !item.has("configured") || item.optBoolean("configured"),
                    ),
                )
            }
        }
    }

    fun setToolsetEnabled(name: String, enabled: Boolean) {
        updateConfig { config ->
            val agent = config.ensureObject("agent")
            val disabled = agent.optJSONArray("disabled_toolsets") ?: JSONArray()
            val values = (0 until disabled.length()).mapNotNull {
                disabled.optString(it).takeIf(String::isNotBlank)
            }.toMutableSet()
            if (enabled) values.remove(name) else values.add(name)
            agent.put("disabled_toolsets", JSONArray(values.sorted()))
        }
    }

    fun listMcpServers(): List<McpServerInfo> {
        val rootValue = JSONTokener(request("GET", "/api/mcp/servers")).nextValue()
        val array = findArray(rootValue, "servers", "items", "data") ?: (rootValue as? JSONArray) ?: JSONArray()
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val name = firstString(item, "name", "id") ?: continue
                val tools = item.optJSONArray("tools")
                add(
                    McpServerInfo(
                        name = name,
                        transport = firstString(item, "transport", "type").orEmpty(),
                        enabled = !item.has("enabled") || item.optBoolean("enabled"),
                        status = firstString(item, "status", "state").orEmpty(),
                        toolCount = item.optInt("tool_count", tools?.length() ?: 0),
                    ),
                )
            }
        }
    }

    fun setMcpServerEnabled(name: String, enabled: Boolean) {
        request(
            "PUT",
            "/api/mcp/servers/${pathSegment(name)}/enabled",
            JSONObject().put("enabled", enabled).toString(),
        )
    }

    fun switchSessionModel(session: HermesSession, provider: String, model: String): HermesSession {
        val active = if (session.runtimeId.isNullOrBlank()) resumeSession(session).session else session
        rpcObject(
            "slash.exec",
            JSONObject()
                .put("session_id", active.runtimeId)
                .put("command", "/model $model --provider $provider --session"),
            timeoutSeconds = 120,
        )
        return active.copy(model = model, provider = provider)
    }

    fun sessionTitle(sessionId: String): String {
        val item = JSONObject(request("GET", "/api/sessions/${pathSegment(sessionId)}"))
        return firstString(item, "title").orEmpty()
    }

    fun renameSession(sessionId: String, title: String): String {
        val body = JSONObject().put("title", compactSessionTitle(title)).toString()
        val result = JSONObject(request("PATCH", "/api/sessions/${pathSegment(sessionId)}", body))
        return compactSessionTitle(firstString(result, "title") ?: title)
    }

    fun setSessionPinned(sessionId: String, pinned: Boolean) {
        val body = JSONObject().put("pinned", pinned).toString()
        request("PATCH", "/api/sessions/${pathSegment(sessionId)}", body)
    }

    fun archiveSession(sessionId: String) {
        val body = JSONObject().put("archived", true).toString()
        request("PATCH", "/api/sessions/${pathSegment(sessionId)}", body)
    }

    fun projectCatalog(): List<HermesProject> {
        val result = rpcObject(
            "projects.tree",
            JSONObject().put("preview_limit", 0).put("session_limit", 2_000),
            timeoutSeconds = 120,
        )
        val projects = result.optJSONArray("projects") ?: JSONArray()
        return buildList {
            for (index in 0 until projects.length()) {
                val item = projects.optJSONObject(index) ?: continue
                if (item.optBoolean("isNoProject")) continue
                parseProject(item)?.let(::add)
            }
        }
    }

    fun createProject(name: String, primaryPath: String): HermesProject {
        val normalizedPath = primaryPath.trim().trimEnd('/')
        val result = rpcObject(
            "projects.create",
            JSONObject()
                .put("name", name.trim())
                .put("folders", JSONArray().put(normalizedPath))
                .put("primary_path", normalizedPath)
                .put("use", false),
            timeoutSeconds = 120,
        )
        val item = result.optJSONObject("project") ?: result
        return parseProject(item)
            ?: throw ApiException(500, "Hermes 没有返回新建项目")
    }

    fun moveSessionToProject(session: HermesSession, project: HermesProject): HermesSession {
        val active = if (session.runtimeId.isNullOrBlank()) resumeSession(session).session else session
        rpcObject(
            "session.cwd.set",
            JSONObject()
                .put("session_id", active.runtimeId)
                .put("cwd", project.primaryPath),
            timeoutSeconds = 120,
        )
        return active.copy(workspacePath = project.primaryPath)
    }

    fun generateSessionTitles(sessions: List<HermesSession>): Map<String, String> {
        if (sessions.isEmpty()) return emptyMap()
        return buildMap {
            sessions.chunked(20).forEach { chunk ->
                val input = JSONArray().apply {
                    chunk.forEach { session ->
                        put(
                            JSONObject()
                                .put("id", session.id)
                                .put("current_title", session.title)
                                .put("conversation_preview", session.preview.take(600)),
                        )
                    }
                }
                val result = rpcObject(
                    "llm.oneshot",
                    JSONObject()
                        .put(
                            "instructions",
                            "你负责为 Hermes AI 助理的对话生成简洁中文标题。根据每条记录的现有标题和对话摘要改写标题。" +
                                "每个标题必须准确、自然，不超过15个汉字字符，不加引号、序号、句号或解释。" +
                                "只返回严格 JSON 数组，每项格式为 {\"id\":\"原id\",\"title\":\"新标题\"}。",
                        )
                        .put("input", input.toString())
                        .put("task", "title_generation")
                        .put("max_tokens", 1_200)
                        .put("temperature", 0.2),
                    timeoutSeconds = 180,
                )
                val text = firstString(result, "text").orEmpty()
                val array = extractJsonArray(text)
                    ?: throw ApiException(500, "Hermes 没有返回可识别的标题列表")
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val id = firstString(item, "id") ?: continue
                    val title = compactSessionTitle(firstString(item, "title").orEmpty())
                    if (id in chunk.map(HermesSession::id) && title != "新会话") put(id, title)
                }
            }
        }
    }

    fun deleteSession(sessionId: String) {
        request("DELETE", "/api/sessions/${pathSegment(sessionId)}")
    }

    fun initialWorkspace(): WorkspaceListing {
        val project = runCatching { activeProjectRoot() }.getOrNull()
        if (project != null) {
            runCatching { listWorkspace(project.second) }
                .getOrNull()
                ?.let { return it.copy(projectName = project.first) }
        }
        return listWorkspace(null)
    }

    fun listWorkspace(path: String?): WorkspaceListing {
        val suffix = path?.takeIf { it.isNotBlank() }
            ?.let { "?path=${queryValue(it)}" }
            .orEmpty()
        val root = JSONObject(request("GET", "/api/files$suffix"))
        val entries = root.optJSONArray("entries") ?: JSONArray()
        return WorkspaceListing(
            path = root.optString("path"),
            parent = firstString(root, "parent"),
            root = firstString(root, "locked_root", "root"),
            entries = buildList {
                for (index in 0 until entries.length()) {
                    val item = entries.optJSONObject(index) ?: continue
                    val entryPath = firstString(item, "path") ?: continue
                    add(
                        WorkspaceEntry(
                            name = firstString(item, "name") ?: entryPath.substringAfterLast('/'),
                            path = entryPath,
                            isDirectory = item.optBoolean("is_directory"),
                            size = item.optLong("size").takeIf { !item.isNull("size") },
                            modifiedAt = item.optDouble("mtime", 0.0),
                            mimeType = firstString(item, "mime_type"),
                        ),
                    )
                }
            },
        )
    }

    fun readWorkspaceDocument(path: String): WorkspaceDocument {
        val root = JSONObject(request("GET", "/api/files/read?path=${queryValue(path)}"))
        val dataUrl = root.optString("data_url")
        val encoded = dataUrl.substringAfter(',', missingDelimiterValue = "")
        if (encoded.isBlank()) throw ApiException(500, "Hermes 没有返回文件内容")
        val bytes = runCatching { Base64.decode(encoded, Base64.DEFAULT) }
            .getOrElse { throw ApiException(500, "无法解析远程文件内容") }
        return WorkspaceDocument(
            name = firstString(root, "name") ?: path.substringAfterLast('/'),
            path = firstString(root, "path") ?: path,
            mimeType = firstString(root, "mime_type") ?: "text/markdown",
            content = if (isTextDocument(path, firstString(root, "mime_type").orEmpty())) {
                bytes.toString(Charsets.UTF_8)
            } else {
                ""
            },
            bytes = bytes,
        )
    }

    fun readProfileFile(file: HermesProfileFile): WorkspaceDocument {
        val profile = currentProfile().trim().ifBlank { "default" }
        if (!PROFILE_NAME_PATTERN.matches(profile)) {
            throw ApiException(400, "当前 Hermes Profile 名称无法用于读取文件")
        }
        val filesRoot = listWorkspace(null).path
        val candidates = hermesProfileFileCandidates(filesRoot, profile, file)
        var lastFailure: Throwable? = null
        candidates.forEach { path ->
            runCatching { readWorkspaceDocument(path) }
                .onSuccess { return it }
                .onFailure { lastFailure = it }
        }
        val hint = if (file == HermesProfileFile.MEMORY) {
            "当前 Profile 可能还没有生成 MEMORY.md；让 Hermes 记录一条记忆后再试"
        } else {
            "请确认当前 Profile 已创建 SOUL.md"
        }
        val status = (lastFailure as? ApiException)?.statusCode ?: 404
        throw ApiException(status, "无法读取 ${file.fileName}。$hint")
    }

    fun saveWorkspaceDocument(path: String, content: String): WorkspaceDocument {
        val dataUrl = "data:text/markdown;charset=utf-8;base64," +
            Base64.encodeToString(content.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        val body = JSONObject()
            .put("path", path)
            .put("data_url", dataUrl)
            .put("overwrite", true)
        request("POST", "/api/files/upload", body.toString())
        return WorkspaceDocument(
            name = path.substringAfterLast('/'),
            path = path,
            mimeType = "text/markdown",
            content = content,
            bytes = content.toByteArray(Charsets.UTF_8),
        )
    }

    fun readImage(path: String): ImagePreview {
        if (path.startsWith("data:image/", ignoreCase = true)) {
            return decodeImageDataUrl(path, "图片", path)
        }
        if (path.startsWith("http://", ignoreCase = true) || path.startsWith("https://", ignoreCase = true)) {
            val request = Request.Builder()
                .url(path)
                .header("Accept", "image/*")
                .header("User-Agent", USER_AGENT)
                .build()
            http.newCall(request).execute().use { response ->
                val bytes = response.body?.bytes() ?: ByteArray(0)
                if (!response.isSuccessful) throw ApiException(response.code, "无法读取聊天图片")
                if (bytes.isEmpty()) throw ApiException(500, "图片内容为空")
                return ImagePreview(
                    name = path.substringBefore('?').substringAfterLast('/').ifBlank { "图片" },
                    source = path,
                    mimeType = response.header("Content-Type")?.substringBefore(';') ?: "image/*",
                    bytes = bytes,
                )
            }
        }
        val root = JSONObject(request("GET", "/api/files/read?path=${queryValue(path)}"))
        return decodeImageDataUrl(
            dataUrl = root.optString("data_url"),
            name = firstString(root, "name") ?: path.substringAfterLast('/'),
            source = firstString(root, "path") ?: path,
        )
    }

    fun listCronJobs(): List<CronJob> {
        val root = JSONTokener(request("GET", "/api/cron/jobs")).nextValue()
        val array = findArray(root, "jobs", "items", "data") ?: JSONArray()
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                parseCronJob(item)?.let(::add)
            }
        }
    }

    fun createCronJob(name: String, prompt: String, schedule: String): CronJob {
        val body = JSONObject()
            .put("name", name.trim())
            .put("prompt", prompt.trim())
            .put("schedule", schedule.trim())
            .put("deliver", "local")
        val root = JSONObject(request("POST", "/api/cron/jobs", body.toString()))
        return parseCronJob(root.optJSONObject("job") ?: root)
            ?: throw ApiException(500, "Hermes 没有返回定时任务")
    }

    fun cronJob(jobId: String): CronJob {
        val root = JSONObject(request("GET", "/api/cron/jobs/${pathSegment(jobId)}"))
        return parseCronJob(root.optJSONObject("job") ?: root)
            ?: throw ApiException(500, "Hermes 没有返回定时任务详情")
    }

    fun updateCronJob(jobId: String, name: String, prompt: String, schedule: String): CronJob {
        val body = JSONObject().put(
            "updates",
            JSONObject()
                .put("name", name.trim())
                .put("prompt", prompt.trim())
                .put("schedule", schedule.trim()),
        )
        val root = JSONObject(request("PUT", "/api/cron/jobs/${pathSegment(jobId)}", body.toString()))
        return parseCronJob(root.optJSONObject("job") ?: root)
            ?: cronJob(jobId)
    }

    fun pauseCronJob(jobId: String) {
        request("POST", "/api/cron/jobs/${pathSegment(jobId)}/pause", "{}")
    }

    fun resumeCronJob(jobId: String) {
        request("POST", "/api/cron/jobs/${pathSegment(jobId)}/resume", "{}")
    }

    fun triggerCronJob(jobId: String) {
        request("POST", "/api/cron/jobs/${pathSegment(jobId)}/trigger", "{}")
    }

    fun deleteCronJob(jobId: String) {
        request("DELETE", "/api/cron/jobs/${pathSegment(jobId)}")
    }

    fun streamMessage(
        controller: StreamController,
        session: HermesSession,
        prompt: String,
        attachments: List<PendingAttachment>,
        onEvent: (StreamEvent) -> Unit,
    ) {
        val runtimeId = session.runtimeId ?: resumeSession(session).session.runtimeId
            ?: error("无法恢复 Hermes 会话")
        controller.runtimeSessionId = runtimeId
        val registration = ActiveStream(runtimeId, controller, onEvent)
        activeStream = registration

        try {
            attachments.filter { it.dataUrl != null }.forEach { attachment ->
                rpcObject(
                    "image.attach_bytes",
                    JSONObject()
                        .put("session_id", runtimeId)
                        .put("content_base64", attachment.dataUrl)
                        .put("filename", attachment.name),
                )
            }
            val text = appendTextAttachments(prompt, attachments)
            rpcObject(
                "prompt.submit",
                JSONObject()
                    .put("session_id", runtimeId)
                    .put("text", text),
            )
            onEvent(StreamEvent.RunStarted(runtimeId))
            if (!controller.awaitCompletion()) {
                throw ApiException(408, "等待 Hermes 回复超时")
            }
        } finally {
            if (activeStream === registration) activeStream = null
        }
    }

    fun stopRun(runtimeSessionId: String) {
        runCatching {
            rpcObject("session.interrupt", JSONObject().put("session_id", runtimeSessionId))
        }
    }

    fun steerSession(runtimeSessionId: String, text: String): String {
        val result = rpcObject(
            "session.steer",
            JSONObject()
                .put("session_id", runtimeSessionId)
                .put("text", text.trim()),
        )
        return firstString(result, "status").orEmpty().ifBlank { "queued" }
    }

    fun respondAgentRequest(request: AgentRequest, answer: String) {
        val params = when (request.type) {
            AgentRequestType.APPROVAL -> JSONObject()
                .put("session_id", request.runtimeSessionId)
                .put("request_id", request.requestId)
                .put("choice", answer)

            AgentRequestType.CLARIFICATION -> JSONObject()
                .put("request_id", request.requestId)
                .put("answer", answer)
        }
        rpcObject(
            if (request.type == AgentRequestType.APPROVAL) "approval.respond" else "clarify.respond",
            params,
        )
    }

    fun reconnectGateway() {
        synchronized(socketLock) {
            closeSocketLocked()
        }
        ensureGatewayConnected()
    }

    fun close() {
        closeSocket()
        http.dispatcher.executorService.shutdown()
        http.connectionPool.evictAll()
    }

    private fun checkGatewayStatus(): JSONObject {
        val status = JSONObject(request("GET", "/api/status"))
        if (!status.optBoolean("auth_required", false)) {
            throw ApiException(400, "该地址不是已启用登录的 Hermes 远程网关")
        }
        val advertised = status.optJSONArray("auth_providers")
        if (advertised != null && (0 until advertised.length()).none { advertised.optString(it) == "basic" }) {
            throw ApiException(400, "该网关没有启用 Hermes 用户名密码登录")
        }
        return status
    }

    private fun authenticatedUsername(): String {
        val me = JSONObject(request("GET", "/api/auth/me"))
        return firstString(me, "display_name", "user_id", "email") ?: config.username
    }

    private fun ensureGatewayConnected() {
        if (socketOpen && socket != null) return
        synchronized(socketLock) {
            if (socketOpen && socket != null) return
            closeSocketLocked()

            val ticketResponse = JSONObject(request("POST", "/api/auth/ws-ticket", "{}"))
            val ticket = ticketResponse.optString("ticket").takeIf { it.isNotBlank() }
                ?: error("远程网关没有返回 WebSocket 票据")
            val openFuture = CompletableFuture<Unit>()
            val readyFuture = CompletableFuture<Unit>()
            socketOpenFuture = openFuture
            gatewayReadyFuture = readyFuture

            val httpUrl = endpoint("/api/ws").toHttpUrl()
            val wsUrl = httpUrl.newBuilder()
                .addQueryParameter("ticket", ticket)
                .build()
            val request = Request.Builder()
                .url(toWebSocketUrl(wsUrl.toString()))
                .header("User-Agent", USER_AGENT)
                .build()
            socket = http.newWebSocket(request, GatewayWebSocketListener())
            try {
                openFuture.get(20, TimeUnit.SECONDS)
                readyFuture.get(20, TimeUnit.SECONDS)
            } catch (error: Exception) {
                closeSocketLocked()
                throw error.cause ?: error
            } finally {
                socketOpenFuture = null
                gatewayReadyFuture = null
            }
        }
    }

    private fun rpcObject(
        method: String,
        params: JSONObject = JSONObject(),
        timeoutSeconds: Long = 60,
    ): JSONObject {
        ensureGatewayConnected()
        val id = "android-${requestIds.incrementAndGet()}"
        val future = CompletableFuture<Any?>()
        pendingCalls[id] = future
        if (!params.has("profile")) params.put("profile", activeProfile)
        val frame = JSONObject()
            .put("jsonrpc", "2.0")
            .put("id", id)
            .put("method", method)
            .put("params", params)
        if (socket?.send(frame.toString()) != true) {
            pendingCalls.remove(id)
            socketOpen = false
            throw ApiException(0, "Hermes 实时连接已断开")
        }
        val result = try {
            future.get(timeoutSeconds, TimeUnit.SECONDS)
        } catch (error: TimeoutException) {
            pendingCalls.remove(id)
            throw ApiException(408, "Hermes 请求超时：$method")
        }
        return when (result) {
            is JSONObject -> result
            null, JSONObject.NULL -> JSONObject()
            else -> JSONObject().put("value", result)
        }
    }

    private fun request(
        method: String,
        path: String,
        body: String? = null,
        includeProfile: Boolean = shouldScopeRequest(path),
    ): String {
        val resolvedPath = if (includeProfile) appendProfileQuery(path, activeProfile) else path
        val resolvedBody = if (includeProfile && body != null) {
            runCatching {
                JSONObject(body).apply {
                    if (!has("profile")) put("profile", activeProfile)
                }.toString()
            }.getOrDefault(body)
        } else body
        val builder = Request.Builder()
            .url(endpoint(resolvedPath))
            .header("Accept", "application/json")
            .header("User-Agent", USER_AGENT)
        if (resolvedBody != null) {
            builder.method(method, resolvedBody.toRequestBody(JSON_MEDIA_TYPE))
        } else {
            builder.method(method, null)
        }
        http.newCall(builder.build()).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw ApiException(response.code, extractErrorMessage(raw))
            return raw
        }
    }

    private fun endpoint(path: String): String = config.baseUrl.trimEnd('/') + "/" + path.trimStart('/')

    private fun activeProjectRoot(): Pair<String, String>? {
        val result = rpcObject("projects.list")
        val projects = result.optJSONArray("projects") ?: return null
        val activeId = result.optString("active_id")
        val candidates = buildList {
            for (index in 0 until projects.length()) {
                projects.optJSONObject(index)?.takeIf { !it.optBoolean("archived") }?.let(::add)
            }
        }
        val project = candidates.firstOrNull { it.optString("id") == activeId }
            ?: candidates.firstOrNull()
            ?: return null
        val folders = project.optJSONArray("folders") ?: JSONArray()
        var firstFolder: String? = null
        var primaryFolder: String? = null
        for (index in 0 until folders.length()) {
            val folder = folders.optJSONObject(index) ?: continue
            val folderPath = firstString(folder, "path") ?: continue
            if (firstFolder == null) firstFolder = folderPath
            if (folder.optBoolean("is_primary")) primaryFolder = folderPath
        }
        val rootPath = firstString(project, "primary_path") ?: primaryFolder ?: firstFolder ?: return null
        return (firstString(project, "name") ?: "Hermes 项目") to rootPath
    }

    private fun parseSession(item: JSONObject): HermesSession? {
        val id = firstString(item, "id", "session_id", "sessionId") ?: return null
        val preview = firstString(item, "preview")
            ?.replace(Regex("\\s+"), " ")
            ?.trim()
            .orEmpty()
        return HermesSession(
            id = id,
            title = resolvedSessionTitle(firstString(item, "title", "name").orEmpty(), preview),
            preview = preview,
            updatedAt = firstString(
                item,
                "last_active",
                "updated_at",
                "updatedAt",
                "started_at",
                "created_at",
                "createdAt",
            ).orEmpty(),
            source = firstString(item, "source", "platform").orEmpty(),
            messageCount = item.optInt("message_count", 0),
            model = firstString(item, "model").orEmpty(),
            provider = firstString(item, "provider").orEmpty(),
            isPinned = item.optBoolean("pinned"),
            workspacePath = firstString(item, "cwd", "git_repo_root").orEmpty(),
            profile = activeProfile,
        )
    }

    private fun shouldScopeRequest(path: String): Boolean {
        val clean = path.substringBefore('?').trimEnd('/')
        return clean.startsWith("/api/") &&
            clean != "/api/status" &&
            clean != "/api/profiles" &&
            !clean.startsWith("/api/auth/") &&
            clean != "/api/ws"
    }

    private fun parseProject(item: JSONObject): HermesProject? {
        val id = firstString(item, "id", "slug") ?: return null
        val directPrimary = firstString(item, "path", "primary_path").orEmpty()
        var folderPrimary = ""
        val paths = buildList {
            directPrimary.takeIf(String::isNotBlank)?.let(::add)
            listOf("folders", "repos").forEach { key ->
                val entries = item.optJSONArray(key) ?: JSONArray()
                for (index in 0 until entries.length()) {
                    val value = entries.opt(index)
                    val path = when (value) {
                        is JSONObject -> firstString(value, "path")?.also {
                            if (value.optBoolean("is_primary")) folderPrimary = it
                        }
                        is String -> value.trim().takeIf(String::isNotBlank)
                        else -> null
                    }
                    path?.let(::add)
                }
            }
        }.distinct()
        val primaryPath = directPrimary.ifBlank { folderPrimary }.ifBlank { paths.firstOrNull().orEmpty() }
        if (primaryPath.isBlank()) return null
        return HermesProject(
            id = id,
            name = firstString(item, "label", "name") ?: id.substringAfterLast('/'),
            primaryPath = primaryPath,
            paths = (paths + primaryPath).distinct(),
            isAuto = item.optBoolean("isAuto") || item.optBoolean("is_auto"),
        )
    }

    private fun extractJsonArray(text: String): JSONArray? {
        val start = text.indexOf('[')
        val end = text.lastIndexOf(']')
        if (start < 0 || end <= start) return null
        return runCatching { JSONArray(text.substring(start, end + 1)) }.getOrNull()
    }

    private fun parseMessages(array: JSONArray): List<ChatMessage> = buildList {
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            parseMessage(item)?.let(::add)
        }
    }

    private fun parseMessage(item: JSONObject): ChatMessage? {
        val role = when (item.optString("role").lowercase()) {
            "user" -> MessageRole.USER
            "assistant" -> MessageRole.ASSISTANT
            "tool" -> MessageRole.TOOL
            "system" -> MessageRole.SYSTEM
            else -> return null
        }
        val content = flattenContent(item.opt("content")).ifBlank {
            flattenContent(item.opt("text"))
        }
        val images = extractImages(item.opt("content")) + extractImages(item.opt("attachments"))
        if (content.isBlank() && images.isEmpty() && role != MessageRole.TOOL) return null
        return ChatMessage(
            id = firstString(item, "row_id", "id", "message_id", "messageId")
                ?: UUID.randomUUID().toString(),
            role = role,
            content = content,
            createdAt = firstString(item, "created_at", "createdAt", "timestamp").orEmpty(),
            images = images.distinctBy(ChatImage::source),
        )
    }

    private fun parseCronJob(item: JSONObject): CronJob? {
        val id = firstString(item, "id", "job_id") ?: return null
        val scheduleValue = item.opt("schedule")
        val schedule = when (scheduleValue) {
            is JSONObject -> CronSchedule(
                kind = firstString(scheduleValue, "kind", "type") ?: "cron",
                expression = firstString(scheduleValue, "expr", "expression", "cron").orEmpty(),
                display = firstString(scheduleValue, "display", "label")
                    ?: firstString(scheduleValue, "expr", "expression", "cron").orEmpty(),
            )
            is String -> CronSchedule(expression = scheduleValue, display = scheduleValue)
            else -> CronSchedule(expression = "", display = "未设置")
        }
        return CronJob(
            id = id,
            name = firstString(item, "name", "title") ?: "定时任务",
            prompt = firstString(item, "prompt", "message", "command").orEmpty(),
            schedule = schedule,
            enabled = if (item.has("enabled")) item.optBoolean("enabled") else !item.optBoolean("paused"),
            state = firstString(item, "state", "status").orEmpty().ifBlank { "scheduled" },
            deliver = firstString(item, "deliver", "delivery").orEmpty().ifBlank { "local" },
            nextRunAt = firstString(item, "next_run_at", "nextRunAt", "next_run").orEmpty(),
            lastRunAt = firstString(item, "last_run_at", "lastRunAt", "last_run").orEmpty(),
            lastStatus = firstString(item, "last_status", "lastStatus").orEmpty(),
            model = firstString(item, "model").orEmpty(),
            provider = firstString(item, "provider").orEmpty(),
        )
    }

    private fun appendTextAttachments(prompt: String, attachments: List<PendingAttachment>): String {
        val documents = attachments.filter { it.textContent != null }
        if (documents.isEmpty()) return prompt
        return buildString {
            append(prompt)
            documents.forEach { attachment ->
                append("\n\n--- 附件：")
                append(attachment.name)
                append(" ---\n")
                append(attachment.textContent)
                append("\n--- 附件结束 ---")
            }
        }
    }

    private fun handleGatewayEvent(params: JSONObject) {
        val type = params.optString("type")
        if (type == "gateway.ready") {
            gatewayReadyFuture?.complete(Unit)
            return
        }
        val stream = activeStream ?: return
        val payload = params.optJSONObject("payload") ?: params
        val sessionId = firstString(params, "session_id") ?: firstString(payload, "session_id")
        if (!sessionId.isNullOrBlank() && sessionId != stream.sessionId) return
        when (type) {
            "message.delta" -> payload.optString("text").takeIf { it.isNotEmpty() }
                ?.let { stream.onEvent(StreamEvent.AssistantDelta(it)) }

            "message.interim" -> payload.optString("text").takeIf { it.isNotBlank() }
                ?.let { stream.onEvent(StreamEvent.AssistantDelta("\n\n$it")) }

            "message.complete" -> {
                val status = payload.optString("status")
                if (status in setOf("error", "failed", "failure")) {
                    stream.onEvent(StreamEvent.Error(firstString(payload, "error", "text") ?: "Hermes 运行失败"))
                } else {
                    stream.onEvent(StreamEvent.AssistantCompleted(payload.optString("text")))
                    stream.onEvent(StreamEvent.Completed)
                }
                stream.controller.finish()
            }

            "tool.start", "tool.generating" -> stream.onEvent(
                StreamEvent.ToolStarted(
                    name = firstString(payload, "name", "tool_name") ?: "正在使用工具",
                    preview = firstString(payload, "context", "preview", "args_text").orEmpty(),
                    todos = ChatInsightParser.parseTodos(payload.optJSONArray("todos")),
                ),
            )

            "tool.complete" -> stream.onEvent(
                StreamEvent.ToolCompleted(
                    name = firstString(payload, "name", "tool_name") ?: "工具",
                    preview = firstString(payload, "summary", "context", "result_text").orEmpty(),
                    todos = ChatInsightParser.parseTodos(payload.optJSONArray("todos")),
                ),
            )

            "tool.progress" -> stream.onEvent(
                StreamEvent.ToolProgress(
                    name = firstString(payload, "name", "tool_name") ?: "正在使用工具",
                    preview = firstString(payload, "message", "summary", "context", "preview").orEmpty(),
                ),
            )

            "tool.error", "tool.failed", "tool.failure" -> stream.onEvent(
                StreamEvent.ToolFailed(
                    name = firstString(payload, "name", "tool_name") ?: "工具",
                    preview = firstString(payload, "message", "error", "summary", "context").orEmpty(),
                ),
            )

            "approval.request" -> stream.onEvent(
                StreamEvent.AgentRequestPending(parseAgentRequest(payload, stream.sessionId, AgentRequestType.APPROVAL)),
            )

            "clarify.request" -> stream.onEvent(
                StreamEvent.AgentRequestPending(parseAgentRequest(payload, stream.sessionId, AgentRequestType.CLARIFICATION)),
            )

            "approval.expire", "approval.expired", "clarify.expire", "clarify.expired" -> {
                firstString(payload, "request_id", "id")
                    ?.let { stream.onEvent(StreamEvent.AgentRequestExpired(it)) }
            }

            "error" -> {
                stream.onEvent(StreamEvent.Error(firstString(payload, "message", "error") ?: "Hermes 运行失败"))
                stream.controller.finish()
            }
        }
    }

    private fun parseAgentRequest(
        payload: JSONObject,
        fallbackSessionId: String,
        type: AgentRequestType,
    ): AgentRequest {
        val item = payload.optJSONObject("request") ?: payload
        val choices = item.optJSONArray("choices") ?: item.optJSONArray("options")
        val parsedChoices = buildList {
            if (choices != null) {
                for (index in 0 until choices.length()) {
                    when (val choice = choices.opt(index)) {
                        is JSONObject -> {
                            val label = firstString(choice, "label", "text", "title", "value") ?: continue
                            add(AgentRequestChoice(label, firstString(choice, "value", "answer", "id") ?: label))
                        }
                        is String -> choice.takeIf(String::isNotBlank)?.let { add(AgentRequestChoice(it)) }
                    }
                }
            }
        }
        val requestId = firstString(item, "request_id", "id")
            ?: firstString(payload, "request_id", "id")
            ?: UUID.randomUUID().toString()
        val title = if (type == AgentRequestType.APPROVAL) {
            firstString(item, "title", "command", "tool_name", "action") ?: "需要确认操作"
        } else {
            firstString(item, "question", "title", "prompt") ?: "Hermes 需要补充信息"
        }
        return AgentRequest(
            requestId = requestId,
            runtimeSessionId = firstString(item, "session_id")
                ?: firstString(payload, "session_id")
                ?: fallbackSessionId,
            type = type,
            title = title,
            detail = firstString(item, "detail", "description", "reason", "message", "context").orEmpty(),
            choices = parsedChoices,
            allowSession = !item.has("allow_session") || item.optBoolean("allow_session"),
            allowPermanent = item.optBoolean("allow_permanent") || item.optBoolean("allow_always"),
        )
    }

    private fun handleSocketClosed(message: String) {
        socketOpen = false
        socket = null
        socketOpenFuture?.completeExceptionally(ApiException(0, message))
        gatewayReadyFuture?.completeExceptionally(ApiException(0, message))
        val error = ApiException(0, message)
        pendingCalls.values.forEach { it.completeExceptionally(error) }
        pendingCalls.clear()
        val interruptedStream = activeStream
        activeStream = null
        interruptedStream?.let { stream ->
            if (!stream.controller.isStopped() && stream.controller.markDisconnected()) {
                stream.onEvent(StreamEvent.ConnectionInterrupted(message))
            }
            stream.controller.finish()
        }
    }

    private fun closeSocket() {
        synchronized(socketLock) { closeSocketLocked() }
    }

    private fun closeSocketLocked() {
        val current = socket
        socket = null
        socketOpen = false
        current?.close(1000, "client closing")
        pendingCalls.values.forEach { it.completeExceptionally(ApiException(0, "Hermes 实时连接已关闭")) }
        pendingCalls.clear()
    }

    private inner class GatewayWebSocketListener : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            socketOpen = true
            socketOpenFuture?.complete(Unit)
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            val frame = runCatching { JSONObject(text) }.getOrNull() ?: return
            if (frame.optString("method") == "event") {
                frame.optJSONObject("params")?.let(::handleGatewayEvent)
                return
            }
            val id = frame.opt("id")?.toString()?.takeIf { it.isNotBlank() } ?: return
            val future = pendingCalls.remove(id) ?: return
            val error = frame.optJSONObject("error")
            if (error != null) {
                future.completeExceptionally(
                    RpcException(error.optInt("code"), error.optString("message", "Hermes RPC 失败")),
                )
            } else {
                future.complete(frame.opt("result"))
            }
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            webSocket.close(code, reason)
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            if (socket === webSocket) handleSocketClosed("网络连接发生波动，正在尝试恢复")
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            if (socket === webSocket) {
                handleSocketClosed(webSocketFailureMessage(response?.code))
            }
        }
    }

    private fun flattenContent(content: Any?): String {
        return when (content) {
            null, JSONObject.NULL -> ""
            is String -> content
            is JSONObject -> firstString(content, "text", "content", "output_text").orEmpty()
            is JSONArray -> buildList {
                for (index in 0 until content.length()) {
                    val value = flattenContent(content.opt(index))
                    if (value.isNotBlank()) add(value)
                }
            }.joinToString("\n")
            else -> content.toString()
        }
    }

    private fun extractImages(content: Any?): List<ChatImage> = when (content) {
        null, JSONObject.NULL -> emptyList()
        is JSONArray -> buildList {
            for (index in 0 until content.length()) addAll(extractImages(content.opt(index)))
        }
        is JSONObject -> {
            val type = content.optString("type").lowercase()
            val imageUrlValue = content.opt("image_url")
            val sourceObject = content.optJSONObject("source")
            val direct = when {
                imageUrlValue is String -> imageUrlValue
                imageUrlValue is JSONObject -> firstString(imageUrlValue, "url")
                type in setOf("image", "input_image") -> firstString(content, "url", "path", "data_url")
                else -> null
            }
            val base64Source = sourceObject?.takeIf { it.optString("type") == "base64" }?.let { source ->
                val data = source.optString("data")
                val mime = firstString(source, "media_type", "mime_type") ?: "image/png"
                data.takeIf(String::isNotBlank)?.let { "data:$mime;base64,$it" }
            }
            val source = direct ?: base64Source
            if (!source.isNullOrBlank()) {
                listOf(
                    ChatImage(
                        name = firstString(content, "name", "filename", "alt") ?: "图片",
                        source = source,
                        mimeType = firstString(content, "mime_type", "media_type")
                            ?: source.substringAfter("data:", "image/*").substringBefore(';'),
                    ),
                )
            } else {
                buildList {
                    content.keys().forEach { key ->
                        if (key !in setOf("text", "content", "output_text")) addAll(extractImages(content.opt(key)))
                    }
                }
            }
        }
        else -> emptyList()
    }

    private fun decodeImageDataUrl(dataUrl: String, name: String, source: String): ImagePreview {
        if (!dataUrl.startsWith("data:image/", ignoreCase = true)) {
            throw ApiException(415, "该文件不是可预览的图片")
        }
        val encoded = dataUrl.substringAfter(',', missingDelimiterValue = "")
        if (encoded.isBlank()) throw ApiException(500, "Hermes 没有返回图片内容")
        val bytes = runCatching { Base64.decode(encoded, Base64.DEFAULT) }
            .getOrElse { throw ApiException(500, "无法解析图片内容") }
        return ImagePreview(
            name = name,
            source = source,
            mimeType = dataUrl.substringAfter("data:").substringBefore(';'),
            bytes = bytes,
        )
    }

    private fun readConfig(): JSONObject {
        val root = JSONObject(request("GET", "/api/config"))
        return root.optJSONObject("config") ?: root
    }

    private fun updateConfig(transform: (JSONObject) -> Unit): ServerSettings {
        val config = readConfig()
        transform(config)
        request("PUT", "/api/config", JSONObject().put("config", config).toString())
        return parseServerSettings(config)
    }

    private fun parseServerSettings(config: JSONObject): ServerSettings {
        val model = config.optJSONObject("model") ?: JSONObject()
        val agent = config.optJSONObject("agent") ?: JSONObject()
        val auxiliary = config.optJSONObject("auxiliary") ?: JSONObject()
        val fallbackArray = config.optJSONArray("fallback_providers") ?: JSONArray()
        val moa = config.optJSONObject("moa") ?: JSONObject()
        val refs = mutableListOf<String>()
        val legacyRefs = moa.optJSONArray("reference_models") ?: JSONArray()
        for (index in 0 until legacyRefs.length()) {
            val item = legacyRefs.opt(index)
            modelSpecString(item)?.let(refs::add)
        }
        var aggregator = modelSpecString(moa.opt("aggregator_model")).orEmpty()
        if (refs.isEmpty()) {
            val activePreset = moa.optString("active_preset", "default").ifBlank { "default" }
            val preset = moa.optJSONObject("presets")?.optJSONObject(activePreset)
            val presetRefs = preset?.optJSONArray("reference_models") ?: JSONArray()
            for (index in 0 until presetRefs.length()) modelSpecString(presetRefs.opt(index))?.let(refs::add)
            aggregator = aggregator.ifBlank { modelSpecString(preset?.opt("aggregator")).orEmpty() }
        }
        val memory = config.optJSONObject("memory") ?: JSONObject()
        val compression = config.optJSONObject("compression") ?: JSONObject()
        val approvals = config.optJSONObject("approvals") ?: JSONObject()
        val display = config.optJSONObject("display") ?: JSONObject()
        return ServerSettings(
            rawConfig = config.toString(),
            models = ServerModelSettings(
                provider = firstString(model, "provider").orEmpty(),
                model = firstString(model, "default", "model").orEmpty(),
                reasoningEffort = firstString(agent, "reasoning_effort").orEmpty(),
                contextLength = model.optInt("context_length", 0),
                auxiliary = AUXILIARY_TASK_KEYS.associateWith { key ->
                    val slot = auxiliary.optJSONObject(key) ?: JSONObject()
                    ModelChoice(
                        provider = firstString(slot, "provider") ?: "auto",
                        model = firstString(slot, "model").orEmpty(),
                    )
                },
                fallbackModels = buildList {
                    for (index in 0 until fallbackArray.length()) {
                        val item = fallbackArray.optJSONObject(index) ?: continue
                        val provider = firstString(item, "provider").orEmpty()
                        val fallbackModel = firstString(item, "model").orEmpty()
                        if (provider.isNotBlank() || fallbackModel.isNotBlank()) add(FallbackModel(provider, fallbackModel))
                    }
                },
                moaReferenceModels = refs.distinct(),
                moaAggregatorModel = aggregator,
            ),
            conversation = ConversationStyleSettings(
                personality = firstString(agent, "personality").orEmpty(),
                timezone = firstString(config, "timezone").orEmpty(),
                showReasoning = !display.has("show_reasoning") || display.optBoolean("show_reasoning"),
            ),
            approvals = ApprovalSettings(
                mode = firstString(approvals, "mode") ?: "smart",
                timeoutSeconds = approvals.optInt("timeout", 60),
            ),
            memory = MemoryContextSettings(
                memoryEnabled = !memory.has("memory_enabled") || memory.optBoolean("memory_enabled"),
                userProfileEnabled = !memory.has("user_profile_enabled") || memory.optBoolean("user_profile_enabled"),
                memoryCharLimit = memory.optInt("memory_char_limit", 2200),
                userCharLimit = memory.optInt("user_char_limit", 1375),
                compressionEnabled = !compression.has("enabled") || compression.optBoolean("enabled"),
                compressionThreshold = compression.optDouble("threshold", 0.50),
                compressionTargetRatio = compression.optDouble("target_ratio", 0.20),
                protectLastMessages = compression.optInt("protect_last_n", 20),
            ),
            voice = parseServerVoiceSettings(config),
        )
    }

    private fun mergeLiveModelSettings(current: ServerModelSettings, root: JSONObject): ServerModelSettings {
        val data = root.optJSONObject("data") ?: root
        val main = data.optJSONObject("main")
            ?: data.optJSONObject("current")
            ?: data.optJSONObject("model")
            ?: JSONObject()
        val provider = current.provider.ifBlank {
            firstString(main, "provider", "current_provider")
                ?: firstString(data, "provider", "current_provider", "default_provider")
                ?: ""
        }
        val model = current.model.ifBlank {
            firstString(main, "model", "default", "current_model")
                ?: firstString(data, "model", "current_model", "default_model")
                ?: ""
        }
        val liveAuxiliary = data.optJSONObject("auxiliary")
            ?: data.optJSONObject("assignments")
            ?: JSONObject()
        val auxiliary = current.auxiliary.toMutableMap()
        AUXILIARY_TASK_KEYS.forEach { key ->
            val existing = auxiliary[key] ?: ModelChoice()
            if (existing.model.isNotBlank() || existing.provider !in setOf("", "auto")) return@forEach
            val slot = liveAuxiliary.optJSONObject(key) ?: return@forEach
            auxiliary[key] = ModelChoice(
                provider = firstString(slot, "provider") ?: existing.provider.ifBlank { "auto" },
                model = firstString(slot, "model", "default").orEmpty(),
            )
        }
        return current.copy(provider = provider, model = model, auxiliary = auxiliary)
    }

    private fun modelSpecString(value: Any?): String? = when (value) {
        is String -> value.trim().takeIf(String::isNotBlank)
        is JSONObject -> {
            val provider = firstString(value, "provider").orEmpty()
            val model = firstString(value, "model", "default").orEmpty()
            when {
                provider.isNotBlank() && model.isNotBlank() -> "$provider:$model"
                model.isNotBlank() -> model
                else -> null
            }
        }
        else -> null
    }

    private fun modelSpecObject(spec: String): JSONObject {
        val value = spec.trim()
        val provider = value.substringBefore(':', "").takeIf { ':' in value }.orEmpty()
        val model = if (provider.isBlank()) value else value.substringAfter(':')
        return JSONObject().put("provider", provider.ifBlank { "openrouter" }).put("model", model)
    }

    private fun JSONObject.ensureObject(key: String): JSONObject {
        val existing = optJSONObject(key)
        if (existing != null) return existing
        return JSONObject().also { put(key, it) }
    }

    private fun findArray(root: Any?, vararg keys: String): JSONArray? {
        if (root is JSONArray) return root
        if (root !is JSONObject) return null
        keys.forEach { key ->
            root.optJSONArray(key)?.let { return it }
            val nested = root.optJSONObject(key)
            nested?.optJSONArray("items")?.let { return it }
            nested?.optJSONArray("data")?.let { return it }
        }
        return null
    }

    private fun firstString(item: JSONObject, vararg keys: String): String? {
        keys.forEach { key ->
            if (!item.has(key) || item.isNull(key)) return@forEach
            val value = item.optString(key).trim()
            if (value.isNotBlank()) return value
        }
        return null
    }

    private fun pathSegment(value: String): String {
        return URLEncoder.encode(value, StandardCharsets.UTF_8.name()).replace("+", "%20")
    }

    private fun queryValue(value: String): String = pathSegment(value)

    private fun extractErrorMessage(raw: String): String {
        return runCatching {
            val root = JSONObject(raw)
            val error = root.opt("error")
            when (error) {
                is JSONObject -> error.optString("message", root.optString("detail", raw))
                is String -> root.optString("detail", error)
                else -> root.optString("detail", root.optString("message", raw))
            }
        }.getOrDefault(raw.ifBlank { "请求失败" })
    }

    private data class ActiveStream(
        val sessionId: String,
        val controller: StreamController,
        val onEvent: (StreamEvent) -> Unit,
    )

    private companion object {
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        const val USER_AGENT = "Hermes-Android/3.0.0"
        val AUXILIARY_TASK_KEYS = listOf(
            "vision",
            "web_extract",
            "compression",
            "skills_hub",
            "approval",
            "mcp",
            "title_generation",
            "curator",
        )
    }
}

internal fun toWebSocketUrl(httpUrl: String): String = when {
    httpUrl.startsWith("https://", ignoreCase = true) -> "wss://" + httpUrl.substring(8)
    httpUrl.startsWith("http://", ignoreCase = true) -> "ws://" + httpUrl.substring(7)
    else -> throw IllegalArgumentException("WebSocket 地址必须由 HTTP(S) 地址转换")
}

data class ResumedSession(
    val session: HermesSession,
    val messages: List<ChatMessage>,
)

class StreamController {
    private val stopped = AtomicBoolean(false)
    private val disconnected = AtomicBoolean(false)
    private val completion = CountDownLatch(1)

    @Volatile
    var runtimeSessionId: String? = null
        internal set

    fun stop() {
        stopped.set(true)
        completion.countDown()
    }

    internal fun finish() {
        completion.countDown()
    }

    internal fun markDisconnected(): Boolean = disconnected.compareAndSet(false, true)

    fun wasDisconnected(): Boolean = disconnected.get()

    internal fun awaitCompletion(): Boolean = completion.await(30, TimeUnit.MINUTES)

    fun isStopped(): Boolean = stopped.get()
}

internal fun webSocketFailureMessage(statusCode: Int?): String = when (statusCode) {
    401, 403 -> "登录状态已失效，请重新登录"
    else -> "网络连接发生波动，正在尝试恢复"
}

class ApiException(val statusCode: Int, override val message: String) : Exception(message)

private val PROFILE_NAME_PATTERN = Regex("[A-Za-z0-9][A-Za-z0-9._-]*")

internal fun hermesProfileFileCandidates(
    filesRoot: String,
    profile: String,
    file: HermesProfileFile,
): List<String> {
    val cleanRoot = filesRoot.trim().trimEnd('/').ifBlank { "/" }
    val hermesRoots = linkedSetOf<String>()
    if (profile != "default" && cleanRoot.endsWith("/profiles/$profile")) {
        return listOf(joinRemotePath(cleanRoot, when (file) {
            HermesProfileFile.MEMORY -> "memories/MEMORY.md"
            HermesProfileFile.SOUL -> "SOUL.md"
        }))
    }
    if (cleanRoot.endsWith("/.hermes") || cleanRoot == ".hermes") {
        hermesRoots += cleanRoot
    } else {
        hermesRoots += joinRemotePath(cleanRoot, ".hermes")
        // Hosted/Docker deployments may expose HERMES_HOME itself as the managed root.
        hermesRoots += cleanRoot
    }
    val relativePath = when (file) {
        HermesProfileFile.MEMORY -> "memories/MEMORY.md"
        HermesProfileFile.SOUL -> "SOUL.md"
    }
    return hermesRoots.map { hermesRoot ->
        val profileRoot = if (profile == "default") {
            hermesRoot
        } else {
            joinRemotePath(hermesRoot, "profiles/$profile")
        }
        joinRemotePath(profileRoot, relativePath)
    }.distinct()
}

private fun joinRemotePath(root: String, child: String): String = when {
    root == "/" -> "/${child.trimStart('/')}"
    else -> "${root.trimEnd('/')}/${child.trimStart('/')}"
}

internal fun appendProfileQuery(path: String, profile: String): String {
    if (profile.isBlank() || Regex("(?:[?&])profile=").containsMatchIn(path)) return path
    val separator = if ('?' in path) '&' else '?'
    val encoded = URLEncoder.encode(profile, StandardCharsets.UTF_8.name()).replace("+", "%20")
    return "$path$separator" + "profile=$encoded"
}

internal fun parseHermesProfiles(raw: String): List<HermesProfile> {
    val root = runCatching { JSONTokener(raw).nextValue() }.getOrNull()
    val array = when (root) {
        is JSONArray -> root
        is JSONObject -> root.optJSONArray("profiles")
            ?: root.optJSONArray("items")
            ?: root.optJSONArray("data")
        else -> null
    }
    val parsed = buildList {
        for (index in 0 until (array?.length() ?: 0)) {
            checkNotNull(array)
            val item = array.optJSONObject(index) ?: continue
            val name = item.optString("name").trim().takeIf(String::isNotBlank) ?: continue
            add(
                HermesProfile(
                    name = name,
                    path = item.profileText("path"),
                    isDefault = item.optBoolean("is_default"),
                    model = item.profileText("model"),
                    provider = item.profileText("provider"),
                    description = item.profileText("description")
                        .ifBlank { item.profileText("description_auto") },
                    skillCount = item.optInt("skill_count"),
                    gatewayRunning = item.optBoolean("gateway_running"),
                ),
            )
        }
    }
    if (parsed.isNotEmpty()) return parsed

    // Android's org.json implementation is unavailable in local JVM tests. Profile
    // records are flat objects, so keep a narrow fallback for tests and wrapped payloads.
    val arrayBody = Regex(
        """"(?:profiles|items|data)"\s*:\s*\[([\s\S]*?)]""",
        RegexOption.IGNORE_CASE,
    ).find(raw)?.groupValues?.getOrNull(1)
        ?: raw.trim().takeIf { it.startsWith('[') && it.endsWith(']') }?.drop(1)?.dropLast(1)
        ?: return emptyList()
    return Regex("""\{([^{}]*)}""").findAll(arrayBody).mapNotNull { match ->
        val body = match.groupValues[1]
        val name = body.profileStringField("name")?.trim()?.takeIf(String::isNotBlank)
            ?: return@mapNotNull null
        HermesProfile(
            name = name,
            path = body.profileStringField("path").orEmpty(),
            isDefault = body.profileBooleanField("is_default"),
            model = body.profileStringField("model").orEmpty(),
            provider = body.profileStringField("provider").orEmpty(),
            description = body.profileStringField("description")
                .orEmpty()
                .ifBlank { body.profileStringField("description_auto").orEmpty() },
            skillCount = body.profileIntField("skill_count"),
            gatewayRunning = body.profileBooleanField("gateway_running"),
        )
    }.toList()
}

private fun JSONObject.profileText(key: String): String = profileTextValue(opt(key))

internal fun profileTextValue(value: Any?): String = (value as? String)?.trim().orEmpty()

private fun isTextDocument(path: String, mimeType: String): Boolean {
    if (mimeType.startsWith("text/", ignoreCase = true)) return true
    if (mimeType.substringBefore(';').lowercase() in setOf(
            "application/json", "application/xml", "application/javascript",
            "application/x-yaml", "application/yaml",
        )
    ) return true
    return path.substringAfterLast('.', "").lowercase() in setOf(
        "md", "markdown", "txt", "csv", "tsv", "json", "xml", "yaml", "yml", "log",
        "kt", "java", "py", "js", "ts", "html", "htm", "css", "sh", "sql",
    )
}

private fun String.profileStringField(key: String): String? {
    val escapedKey = Regex.escape(key)
    val encoded = Regex(""""$escapedKey"\s*:\s*"((?:\\.|[^"\\])*)"""")
        .find(this)
        ?.groupValues
        ?.getOrNull(1)
        ?: return null
    return Regex("""\\u([0-9a-fA-F]{4})""").replace(encoded) { result ->
        result.groupValues[1].toInt(16).toChar().toString()
    }.replace("\\\"", "\"")
        .replace("\\/", "/")
        .replace("\\n", "\n")
        .replace("\\r", "\r")
        .replace("\\t", "\t")
        .replace("\\b", "\b")
        .replace("\\f", "\u000C")
        .replace("\\\\", "\\")
}

private fun String.profileBooleanField(key: String): Boolean =
    Regex(""""${Regex.escape(key)}"\s*:\s*(true|false)""", RegexOption.IGNORE_CASE)
        .find(this)
        ?.groupValues
        ?.getOrNull(1)
        ?.lowercase()
        ?.toBooleanStrictOrNull()
        ?: false

private fun String.profileIntField(key: String): Int =
    Regex(""""${Regex.escape(key)}"\s*:\s*(-?\d+)""")
        .find(this)
        ?.groupValues
        ?.getOrNull(1)
        ?.toIntOrNull()
        ?: 0

class RpcException(val rpcCode: Int, override val message: String) : Exception(message)

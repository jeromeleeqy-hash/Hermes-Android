package com.qingyu.hermescompanion.ui

import android.app.Application
import android.app.ActivityManager
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.core.content.FileProvider
import com.qingyu.hermescompanion.data.ApiException
import com.qingyu.hermescompanion.data.AttachmentReader
import com.qingyu.hermescompanion.data.ChatInsightParser
import com.qingyu.hermescompanion.data.HermesApiClient
import com.qingyu.hermescompanion.data.StreamController
import com.qingyu.hermescompanion.data.VoiceAudioRecorder
import com.qingyu.hermescompanion.data.VoicePlaybackController
import com.qingyu.hermescompanion.diagnostics.CrashDiagnostics
import com.qingyu.hermescompanion.model.ChatMessage
import com.qingyu.hermescompanion.model.ChatImage
import com.qingyu.hermescompanion.model.ChatArtifact
import com.qingyu.hermescompanion.model.ChatTodo
import com.qingyu.hermescompanion.model.ConnectionConfig
import com.qingyu.hermescompanion.model.GatewayInfo
import com.qingyu.hermescompanion.model.AgentUpdateInfo
import com.qingyu.hermescompanion.model.AgentUpdateProgress
import com.qingyu.hermescompanion.model.IncomingShare
import com.qingyu.hermescompanion.model.CronJob
import com.qingyu.hermescompanion.model.HermesSession
import com.qingyu.hermescompanion.model.HermesProject
import com.qingyu.hermescompanion.model.HermesProfile
import com.qingyu.hermescompanion.model.HermesProfileFile
import com.qingyu.hermescompanion.model.scopedId
import com.qingyu.hermescompanion.model.MessageRole
import com.qingyu.hermescompanion.model.ImagePreview
import com.qingyu.hermescompanion.model.ModelCatalog
import com.qingyu.hermescompanion.model.PendingAttachment
import com.qingyu.hermescompanion.model.FailedSend
import com.qingyu.hermescompanion.model.NotificationPreferences
import com.qingyu.hermescompanion.model.StreamEvent
import com.qingyu.hermescompanion.model.ToolActivity
import com.qingyu.hermescompanion.model.ToolStatus
import com.qingyu.hermescompanion.model.VoicePreferences
import com.qingyu.hermescompanion.model.VoiceConversationState
import com.qingyu.hermescompanion.model.VoiceCaptureState
import com.qingyu.hermescompanion.model.VoiceCaptureTarget
import com.qingyu.hermescompanion.model.VoicePhase
import com.qingyu.hermescompanion.model.UserProfilePreferences
import com.qingyu.hermescompanion.model.ServerSettings
import com.qingyu.hermescompanion.model.SlashCommand
import com.qingyu.hermescompanion.model.ServerModelSettings
import com.qingyu.hermescompanion.model.ConversationStyleSettings
import com.qingyu.hermescompanion.model.ApprovalSettings
import com.qingyu.hermescompanion.model.AgentRequest
import com.qingyu.hermescompanion.model.AgentRequestType
import com.qingyu.hermescompanion.model.MemoryContextSettings
import com.qingyu.hermescompanion.model.ServerVoiceSettings
import com.qingyu.hermescompanion.model.ServerSkill
import com.qingyu.hermescompanion.model.ToolsetInfo
import com.qingyu.hermescompanion.model.McpServerInfo
import com.qingyu.hermescompanion.model.WorkspaceDocument
import com.qingyu.hermescompanion.model.WorkspaceListing
import com.qingyu.hermescompanion.model.QueuedRunMessage
import com.qingyu.hermescompanion.model.RunCompletionSummary
import com.qingyu.hermescompanion.model.ActiveRunSnapshot
import com.qingyu.hermescompanion.model.RecentArtifact
import com.qingyu.hermescompanion.model.SessionSearchResult
import com.qingyu.hermescompanion.model.ConnectionDiagnosticItem
import com.qingyu.hermescompanion.model.DiagnosticStatus
import com.qingyu.hermescompanion.storage.SecureConfigStore
import com.qingyu.hermescompanion.storage.SecureCookieJar
import com.qingyu.hermescompanion.storage.AvatarStorage
import com.qingyu.hermescompanion.storage.AvatarTarget
import com.qingyu.hermescompanion.storage.AvatarCropSpec
import com.qingyu.hermescompanion.ui.format.compactSessionTitle
import com.qingyu.hermescompanion.ui.format.isPlaceholderSessionTitle
import com.qingyu.hermescompanion.notification.HermesNotifications
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.File
import java.net.URI

enum class AppRoute {
    SETUP,
    SESSIONS,
    SEARCH,
    CHAT,
    WORKSPACE,
    TASKS,
    CRON_DETAIL,
    PROFILE,
    SETTINGS,
    PROFILE_FILE,
    PROFILE_SETTINGS,
    SKILLS_TOOLS,
    MODEL_SETTINGS,
    CONVERSATION_STYLE,
    APPROVAL_SETTINGS,
    MEMORY_CONTEXT,
    ARCHIVED_SESSIONS,
    NOTIFICATIONS,
    VOICE_SETTINGS,
    VOICE_CHAT,
    ABOUT,
    CHANGELOG,
}

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

enum class SkinMode {
    CLEAN,
    GLASS,
}

enum class CouncilMode {
    OFF,
    QUICK,
    DEEP,
}

private data class HermesDeepLink(
    val route: String,
    val profile: String?,
    val sessionId: String?,
)

private data class ChatScrollPosition(val index: Int, val offset: Int)

data class AppUiState(
    val route: AppRoute = AppRoute.SETUP,
    val baseUrl: String = "",
    val username: String = "",
    val hasSavedConnection: Boolean = false,
    val sessions: List<HermesSession> = emptyList(),
    val sessionTotalCount: Int = 0,
    val projects: List<HermesProject> = emptyList(),
    val profiles: List<HermesProfile> = emptyList(),
    val activeProfile: String = "default",
    val isProfilesLoading: Boolean = false,
    val isProfileSwitching: Boolean = false,
    val selectedSession: HermesSession? = null,
    val messages: List<ChatMessage> = emptyList(),
    val hasOlderMessages: Boolean = false,
    val isOlderMessagesLoading: Boolean = false,
    val toolActivities: List<ToolActivity> = emptyList(),
    val chatArtifacts: List<ChatArtifact> = emptyList(),
    val chatTodos: List<ChatTodo> = emptyList(),
    val attachments: List<PendingAttachment> = emptyList(),
    val draft: String = "",
    val failedSend: FailedSend? = null,
    val slashCommands: List<SlashCommand> = emptyList(),
    val isSlashCommandsLoading: Boolean = false,
    val commandCatalog: List<SlashCommand> = emptyList(),
    val isCommandCatalogLoading: Boolean = false,
    val councilMode: CouncilMode = CouncilMode.OFF,
    val activeCouncilMode: CouncilMode = CouncilMode.OFF,
    val isBusy: Boolean = false,
    val isStreaming: Boolean = false,
    val streamingSessionId: String? = null,
    val runStage: String = "",
    val runStartedAtMillis: Long = 0L,
    val runLastActivityAtMillis: Long = 0L,
    val isSteering: Boolean = false,
    val queuedRunMessage: QueuedRunMessage? = null,
    val pendingAgentRequests: List<AgentRequest> = emptyList(),
    val latestCompletion: RunCompletionSummary? = null,
    val recentCompletions: List<RunCompletionSummary> = emptyList(),
    val recentArtifacts: List<RecentArtifact> = emptyList(),
    val isRecentArtifactsLoading: Boolean = false,
    val workspaceSourceArtifact: RecentArtifact? = null,
    val highlightedMessageId: String? = null,
    val searchQuery: String = "",
    val searchResults: List<SessionSearchResult> = emptyList(),
    val isSearchLoading: Boolean = false,
    val connectionDiagnostics: List<ConnectionDiagnosticItem> = emptyList(),
    val isConnectionDiagnosing: Boolean = false,
    val gatewayInfo: GatewayInfo = GatewayInfo(),
    val agentUpdateInfo: AgentUpdateInfo = AgentUpdateInfo(),
    val agentUpdateProgress: AgentUpdateProgress = AgentUpdateProgress(),
    val isAgentUpdateChecking: Boolean = false,
    val incomingShare: IncomingShare? = null,
    val isSharePreparing: Boolean = false,
    val isShareSending: Boolean = false,
    val chatScrollIndex: Int = 0,
    val chatScrollOffset: Int = 0,
    val hasSavedChatScroll: Boolean = false,
    val unreadSessionIds: Set<String> = emptySet(),
    val isRecoveringConnection: Boolean = false,
    val modelCatalog: ModelCatalog = ModelCatalog(),
    val isModelsLoading: Boolean = false,
    val isModelSwitching: Boolean = false,
    val isProjectsLoading: Boolean = false,
    val sessionActionId: String? = null,
    val isBatchRenaming: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val skinMode: SkinMode = SkinMode.CLEAN,
    val workspaceListing: WorkspaceListing? = null,
    val projectPickerListing: WorkspaceListing? = null,
    val isProjectPickerLoading: Boolean = false,
    val workspaceRootPath: String? = null,
    val workspaceDocument: WorkspaceDocument? = null,
    val workspaceDocumentOrigin: AppRoute? = null,
    val imagePreview: ImagePreview? = null,
    val isImageLoading: Boolean = false,
    val inlineImagePreviews: Map<String, ImagePreview> = emptyMap(),
    val inlineImageLoading: Set<String> = emptySet(),
    val inlineImageFailures: Set<String> = emptySet(),
    val workspaceDraft: String = "",
    val isWorkspaceLoading: Boolean = false,
    val isWorkspaceEditing: Boolean = false,
    val isWorkspaceSaving: Boolean = false,
    val cronJobs: List<CronJob> = emptyList(),
    val selectedCronJob: CronJob? = null,
    val isCronLoading: Boolean = false,
    val cronActionId: String? = null,
    val serverSettings: ServerSettings = ServerSettings(),
    val serverSkills: List<ServerSkill> = emptyList(),
    val toolsets: List<ToolsetInfo> = emptyList(),
    val mcpServers: List<McpServerInfo> = emptyList(),
    val selectedSkill: ServerSkill? = null,
    val selectedSkillContent: String = "",
    val archivedSessions: List<HermesSession> = emptyList(),
    val isAdvancedSettingsLoading: Boolean = false,
    val settingsActionKey: String? = null,
    val notificationPreferences: NotificationPreferences = NotificationPreferences(),
    val voicePreferences: VoicePreferences = VoicePreferences(),
    val voiceConversation: VoiceConversationState = VoiceConversationState(),
    val voiceCapture: VoiceCaptureState = VoiceCaptureState(),
    val userProfile: UserProfilePreferences = UserProfilePreferences(),
    val isAvatarUpdating: Boolean = false,
    val crashReport: String? = null,
    val errorMessage: String? = null,
    val noticeMessage: String? = null,
)

class HermesViewModel(application: Application) : AndroidViewModel(application) {
    private val configStore = SecureConfigStore(application)
    private val avatarStorage = AvatarStorage(application)
    private val cookieJar = SecureCookieJar(configStore)
    private var apiClient: HermesApiClient? = null
    private var streamController: StreamController? = null
    private var streamJob: Job? = null
    private var streamRecoveryJob: Job? = null
    private var streamWatchdogJob: Job? = null
    private var streamRecoveryPrompt: String = ""
    private var streamBaselineAssistantSignature: String = ""
    private var activeSubmittedPrompt: String = ""
    private var activeSubmittedAttachments: List<PendingAttachment> = emptyList()
    private var activeUserMessageId: String? = null
    private var titleRefreshJob: Job? = null
    private var slashCommandJob: Job? = null
    private var sessionSearchJob: Job? = null
    private var agentUpdateJob: Job? = null
    private var voicePlaybackJob: Job? = null
    private var voiceCaptureJob: Job? = null
    private var voiceLevelJob: Job? = null
    private var slashCommandQuery: String = ""
    private val commandCatalogCache = mutableMapOf<String, List<SlashCommand>>()
    private val streamingDeltaBuffer = StringBuilder()
    private var streamingDeltaFlushJob: Job? = null
    private val messageCache = LinkedHashMap<String, List<ChatMessage>>()
    private val oldestMessageOffsets = mutableMapOf<String, Int>()
    private val indexedArtifactProfiles = mutableSetOf<String>()
    private var activeStreamSession: HermesSession? = null
    private var activeStreamToolActivities: List<ToolActivity> = emptyList()
    private var activeStreamArtifacts: List<ChatArtifact> = emptyList()
    private var activeStreamTodos: List<ChatTodo> = emptyList()
    private val pendingTitleSessionIds = mutableSetOf<String>()
    private val chatScrollPositions = mutableMapOf<String, ChatScrollPosition>()
    private var chatReturnRoute: AppRoute = AppRoute.SESSIONS
    private var settingsReturnRoute: AppRoute = AppRoute.PROFILE
    private var pendingDeepLink: HermesDeepLink? = null
    private var savedRunRecoveryAttempted = false
    private val voiceRecorder = VoiceAudioRecorder(application)
    private val voicePlayback = VoicePlaybackController(application)
    private var voiceReturnRoute: AppRoute = AppRoute.CHAT

    var uiState by androidx.compose.runtime.mutableStateOf(AppUiState())
        private set

    init {
        val crashReport = CrashDiagnostics.read(application)
        val savedActiveProfile = configStore.readActiveHermesProfile()
        val unreadSessionIds = configStore.readUnreadSessionIds().mapTo(mutableSetOf()) { id ->
            if ("::" in id) id else "$savedActiveProfile::$id"
        }
        configStore.saveUnreadSessionIds(unreadSessionIds)
        val savedTheme = configStore.readThemeMode()
            ?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
            ?: ThemeMode.SYSTEM
        // 2.1 unifies the old clean/glass split into one interface language.
        // Persist CLEAN once so upgraded installs do not carry a hidden skin
        // preference that can make screens disagree.
        val savedSkin = SkinMode.CLEAN
        configStore.saveSkinMode(savedSkin.name)
        val storedProfile = configStore.readUserProfile()
        val safeProfile = avatarStorage.sanitize(storedProfile)
        if (safeProfile != storedProfile) configStore.saveUserProfile(safeProfile)
        uiState = uiState.copy(themeMode = savedTheme, skinMode = savedSkin)
        uiState = uiState.copy(
            notificationPreferences = configStore.readNotificationPreferences(),
            voicePreferences = configStore.readVoicePreferences(),
            userProfile = safeProfile,
            activeProfile = savedActiveProfile,
            unreadSessionIds = unreadSessionIds,
            crashReport = crashReport,
            recentArtifacts = configStore.readRecentArtifacts(),
            pendingAgentRequests = configStore.readPendingAgentRequests(),
            recentCompletions = configStore.readRecentCompletions(),
            latestCompletion = configStore.readRecentCompletions().firstOrNull(),
        )
        val saved = configStore.read()
        if (saved != null) {
            val client = HermesApiClient(saved, cookieJar)
            apiClient = client
            uiState = uiState.copy(
                route = AppRoute.SETUP,
                baseUrl = saved.baseUrl,
                username = saved.username,
                hasSavedConnection = true,
                isBusy = client.hasSavedSession(),
            )
            // If the previous process crashed, stay on the safe setup route so the report
            // can be copied instead of immediately repeating the same route transition.
            if (client.hasSavedSession() && crashReport == null) resumeSavedConnection(client)
        }
    }

    fun dismissCrashReport() {
        CrashDiagnostics.clear(getApplication())
        uiState = uiState.copy(crashReport = null)
    }

    fun connect(baseUrl: String, username: String, password: String, allowInsecureHttp: Boolean) {
        val normalizedUrl = normalizeBaseUrl(baseUrl)
        if (normalizedUrl == null) {
            showError("请输入有效的远程网关地址，例如 http://服务器IP:9119")
            return
        }
        if (normalizedUrl.startsWith("http://") && !allowInsecureHttp) {
            showError("这是未加密的 HTTP 连接，请勾选风险确认后再连接")
            return
        }
        if (username.isBlank() || password.isBlank()) {
            showError("请输入 Hermes 用户名和密码")
            return
        }

        uiState = uiState.copy(isBusy = true, errorMessage = null, noticeMessage = null)
        viewModelScope.launch {
            runCatching {
                val config = ConnectionConfig(normalizedUrl, username.trim())
                val client = HermesApiClient(config, cookieJar)
                val signedInAs = withContext(Dispatchers.IO) { client.login(config.username, password) }
                configStore.save(config)
                apiClient?.takeIf { it !== client }?.close()
                apiClient = client
                Triple(config, signedInAs, client)
            }.onSuccess { (config, signedInAs, client) ->
                indexedArtifactProfiles.clear()
                commandCatalogCache.clear()
                messageCache.clear()
                oldestMessageOffsets.clear()
                uiState = uiState.copy(
                    route = AppRoute.SESSIONS,
                    baseUrl = config.baseUrl,
                    username = config.username,
                    hasSavedConnection = true,
                    recentArtifacts = configStore.readRecentArtifacts(),
                    connectionDiagnostics = emptyList(),
                    isBusy = false,
                    noticeMessage = "已登录：$signedInAs",
                )
                loadGatewayInfo(client)
                loadProfilesAndSessions(client)
            }.onFailure(::handleFailure)
        }
    }

    fun refreshSessions() {
        val client = apiClient ?: return
        val expectedProfile = client.currentProfile()
        uiState = uiState.copy(isBusy = uiState.sessions.isEmpty(), errorMessage = null)
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) { client.listSessions() } }
                .onSuccess { page ->
                    if (client.currentProfile() != expectedProfile) return@onSuccess
                    uiState = uiState.copy(
                        // Compose LazyColumn requires globally unique item keys. Some Hermes
                        // gateway builds can repeat a stored session in the first page.
                        sessions = page.sessions.distinctBy(HermesSession::id),
                        sessionTotalCount = page.totalCount,
                        isBusy = false,
                        isProfileSwitching = false,
                    )
                    consumePendingDeepLink()
                    refreshProjects()
                    restoreSavedRunIfNeeded(page.sessions)
                }
                .onFailure { error ->
                    if (client.currentProfile() == expectedProfile) handleFailure(error)
                }
        }
    }

    fun refreshProfiles() {
        val client = apiClient ?: return
        uiState = uiState.copy(isProfilesLoading = true, errorMessage = null)
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) { client.listProfiles() } }
                .onSuccess { profiles ->
                    val available = profiles.ifEmpty { listOf(HermesProfile(name = "default", isDefault = true)) }
                    val activeStillExists = available.any { it.name == client.currentProfile() }
                    if (activeStillExists) {
                        uiState = uiState.copy(profiles = available, isProfilesLoading = false)
                    } else {
                        val fallback = available.firstOrNull(HermesProfile::isDefault) ?: available.first()
                        client.setProfile(fallback.name)
                        configStore.saveActiveHermesProfile(fallback.name)
                        messageCache.clear()
                        uiState = uiState.copy(
                            profiles = available,
                            activeProfile = fallback.name,
                            isProfilesLoading = false,
                            isProfileSwitching = true,
                            sessions = emptyList(),
                            projects = emptyList(),
                            noticeMessage = "原 Profile 已不存在，已切换到 ${fallback.name}",
                        )
                        refreshSessions()
                    }
                }
                .onFailure {
                    uiState = uiState.copy(isProfilesLoading = false)
                    handleFailure(it)
                }
        }
    }

    fun selectProfile(profile: HermesProfile) {
        val client = apiClient ?: return
        if (uiState.isStreaming) {
            showNotice("当前回复仍在生成，请等待完成后再切换 Profile")
            return
        }
        if (profile.name == client.currentProfile()) return
        titleRefreshJob?.cancel()
        slashCommandJob?.cancel()
        sessionSearchJob?.cancel()
        client.setProfile(profile.name)
        configStore.saveActiveHermesProfile(profile.name)
        messageCache.clear()
        oldestMessageOffsets.clear()
        pendingTitleSessionIds.clear()
        uiState = uiState.copy(
            route = AppRoute.SESSIONS,
            activeProfile = profile.name,
            isProfileSwitching = true,
            isProfilesLoading = false,
            isProjectsLoading = false,
            sessions = emptyList(),
            sessionTotalCount = 0,
            projects = emptyList(),
            selectedSession = null,
            messages = emptyList(),
            toolActivities = emptyList(),
            chatArtifacts = emptyList(),
            chatTodos = emptyList(),
            attachments = emptyList(),
            draft = "",
            failedSend = null,
            slashCommands = emptyList(),
            commandCatalog = commandCatalogCache[profile.name].orEmpty(),
            isCommandCatalogLoading = false,
            councilMode = CouncilMode.OFF,
            workspaceListing = null,
            workspaceDocument = null,
            cronJobs = emptyList(),
            selectedCronJob = null,
            serverSkills = emptyList(),
            toolsets = emptyList(),
            mcpServers = emptyList(),
            archivedSessions = emptyList(),
            searchQuery = "",
            searchResults = emptyList(),
            isSearchLoading = false,
            isRecentArtifactsLoading = false,
            highlightedMessageId = null,
            workspaceSourceArtifact = null,
            noticeMessage = "已切换到 Profile：${profile.name}",
            errorMessage = null,
        )
        refreshSessions()
    }

    fun createSession() {
        if (uiState.isStreaming) {
            showNotice("当前回复仍在后台生成，请等待完成后再新建对话")
            return
        }
        val client = apiClient ?: return
        chatReturnRoute = AppRoute.SESSIONS
        uiState = uiState.copy(isBusy = true, errorMessage = null)
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) { client.createSession() } }
                .onSuccess { session ->
                    uiState = uiState.copy(
                        route = AppRoute.CHAT,
                        selectedSession = session,
                        messages = emptyList(),
                        toolActivities = emptyList(),
                        chatArtifacts = emptyList(),
                        chatTodos = emptyList(),
                        inlineImagePreviews = emptyMap(),
                        inlineImageLoading = emptySet(),
                        inlineImageFailures = emptySet(),
                        draft = configStore.readDraft(session.profile, session.id),
                        failedSend = null,
                        hasOlderMessages = false,
                        isOlderMessagesLoading = false,
                        councilMode = CouncilMode.OFF,
                        chatScrollIndex = 0,
                        chatScrollOffset = 0,
                        hasSavedChatScroll = false,
                        isBusy = false,
                    )
                }
                .onFailure(::handleFailure)
        }
    }

    fun openSession(session: HermesSession) {
        chatReturnRoute = AppRoute.SESSIONS
        openSessionInternal(session, targetMessageId = null)
    }

    fun openTaskSession(session: HermesSession) {
        chatReturnRoute = AppRoute.TASKS
        openSessionInternal(session, targetMessageId = null)
    }

    private fun openSessionInternal(session: HermesSession, targetMessageId: String?) {
        val client = apiClient ?: return
        markSessionRead(session.scopedId)
        val cached = messageCache[session.scopedId]
        val cachedInsights = cached?.let(ChatInsightParser::fromMessages)
        val isActiveStream = uiState.isStreaming && uiState.streamingSessionId == session.id
        val visibleCached = cached.visibleConversationMessages()
        val targetIndex = targetMessageId?.let { id -> visibleCached.indexOfFirst { it.id == id }.takeIf { it >= 0 } }
        val savedScroll = targetIndex?.let { ChatScrollPosition(it, 0) } ?: chatScrollPositions[session.scopedId]
        val cachedOffset = oldestMessageOffsets[session.scopedId]
            ?: (session.messageCount - cached.orEmpty().size).coerceAtLeast(0)
        uiState = uiState.copy(
            route = AppRoute.CHAT,
            selectedSession = session,
            messages = visibleCached,
            hasOlderMessages = cached != null && cachedOffset > 0,
            isOlderMessagesLoading = false,
            toolActivities = if (isActiveStream) activeStreamToolActivities else emptyList(),
            chatArtifacts = if (isActiveStream) activeStreamArtifacts else cachedInsights?.artifacts.orEmpty(),
            chatTodos = if (isActiveStream) activeStreamTodos else cachedInsights?.todos.orEmpty(),
            attachments = emptyList(),
            draft = configStore.readDraft(session.profile, session.id),
            failedSend = null,
            councilMode = CouncilMode.OFF,
            chatScrollIndex = savedScroll?.index ?: 0,
            chatScrollOffset = savedScroll?.offset ?: 0,
            hasSavedChatScroll = savedScroll != null,
            highlightedMessageId = targetMessageId,
            inlineImagePreviews = emptyMap(),
            inlineImageLoading = emptySet(),
            inlineImageFailures = emptySet(),
            isBusy = cached == null && !isActiveStream,
            errorMessage = null,
        )
        if (isActiveStream) return
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    client.loadRecentMessagePage(session, if (targetMessageId == null) CHAT_PAGE_SIZE else SEARCH_MESSAGE_LIMIT)
                }
            }
                .onSuccess { page ->
                    if (uiState.route != AppRoute.CHAT || uiState.selectedSession?.id != session.id) {
                        return@onSuccess
                    }
                    val messages = page.messages
                    messageCache[session.scopedId] = messages
                    oldestMessageOffsets[session.scopedId] = page.offset
                    while (messageCache.size > 8) messageCache.remove(messageCache.keys.first())
                    val insights = ChatInsightParser.fromMessages(messages)
                    val visibleMessages = messages.visibleConversationMessages()
                    val loadedTargetIndex = targetMessageId?.let { id ->
                        visibleMessages.indexOfFirst { it.id == id }.takeIf { it >= 0 }
                    }
                    rememberRecentArtifacts(session, messages)
                    uiState = uiState.copy(
                        messages = visibleMessages,
                        hasOlderMessages = page.offset > 0,
                        isOlderMessagesLoading = false,
                        chatArtifacts = insights.artifacts,
                        chatTodos = insights.todos,
                        chatScrollIndex = loadedTargetIndex ?: uiState.chatScrollIndex,
                        chatScrollOffset = if (loadedTargetIndex != null) 0 else uiState.chatScrollOffset,
                        hasSavedChatScroll = loadedTargetIndex != null || uiState.hasSavedChatScroll,
                        isBusy = false,
                    )
                }
                .onFailure(::handleFailure)
        }
    }

    fun loadOlderMessages() {
        val client = apiClient ?: return
        val session = uiState.selectedSession ?: return
        if (uiState.isOlderMessagesLoading) return
        val current = messageCache[session.scopedId].orEmpty()
        val currentOffset = oldestMessageOffsets[session.scopedId]
            ?: (session.messageCount - current.size).coerceAtLeast(0)
        if (currentOffset <= 0) {
            uiState = uiState.copy(hasOlderMessages = false)
            return
        }
        val pageSize = minOf(CHAT_PAGE_SIZE, currentOffset)
        val nextOffset = (currentOffset - pageSize).coerceAtLeast(0)
        uiState = uiState.copy(isOlderMessagesLoading = true, errorMessage = null)
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) { client.loadMessagePage(session, pageSize, nextOffset) }
            }.onSuccess { page ->
                val merged = (page.messages + current).distinctBy(ChatMessage::id)
                messageCache[session.scopedId] = merged
                oldestMessageOffsets[session.scopedId] = page.offset
                while (messageCache.size > 8) messageCache.remove(messageCache.keys.first())
                if (uiState.selectedSession?.scopedId == session.scopedId) {
                    val insights = ChatInsightParser.fromMessages(merged)
                    uiState = uiState.copy(
                        messages = merged.visibleConversationMessages(),
                        chatArtifacts = insights.artifacts,
                        chatTodos = insights.todos,
                        hasOlderMessages = page.offset > 0,
                        isOlderMessagesLoading = false,
                    )
                }
            }.onFailure { error ->
                uiState = uiState.copy(isOlderMessagesLoading = false)
                handleFailure(error)
            }
        }
    }

    fun saveChatScrollPosition(sessionKey: String, index: Int, offset: Int) {
        if (sessionKey.isBlank()) return
        val position = ChatScrollPosition(index.coerceAtLeast(0), offset.coerceAtLeast(0))
        chatScrollPositions[sessionKey] = position
        if (uiState.selectedSession?.scopedId == sessionKey) {
            uiState = uiState.copy(
                chatScrollIndex = position.index,
                chatScrollOffset = position.offset,
                hasSavedChatScroll = true,
            )
        }
    }

    fun deleteSession(session: HermesSession) {
        val client = apiClient ?: return
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) { client.deleteSession(session.id) } }
                .onSuccess {
                    val unreadSessionIds = uiState.unreadSessionIds - session.scopedId
                    configStore.saveUnreadSessionIds(unreadSessionIds)
                    configStore.clearDraft(session.profile, session.id)
                    uiState = uiState.copy(
                        sessions = uiState.sessions.filterNot { it.id == session.id },
                        sessionTotalCount = (uiState.sessionTotalCount - 1).coerceAtLeast(0),
                        unreadSessionIds = unreadSessionIds,
                        noticeMessage = "会话已删除",
                    )
                }
                .onFailure(::handleFailure)
        }
    }

    fun refreshProjects() {
        val client = apiClient ?: return
        if (uiState.isProjectsLoading) return
        val expectedProfile = client.currentProfile()
        uiState = uiState.copy(isProjectsLoading = true)
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) { client.projectCatalog() } }
                .onSuccess { projects ->
                    if (client.currentProfile() != expectedProfile) return@onSuccess
                    uiState = uiState.copy(projects = projects, isProjectsLoading = false)
                }
                .onFailure {
                    if (client.currentProfile() == expectedProfile) {
                        uiState = uiState.copy(isProjectsLoading = false)
                    }
                }
        }
    }

    fun createProject(name: String, primaryPath: String) {
        val client = apiClient ?: return
        val cleanName = name.trim()
        val cleanPath = primaryPath.trim()
        when {
            cleanName.isBlank() -> showError("请输入项目名称")
            cleanPath.isBlank() -> showError("请输入服务器上的项目目录")
            !cleanPath.startsWith('/') -> showError("项目目录需要使用绝对路径，例如 /root/workspace/my-project")
            uiState.isProjectsLoading -> return
            else -> {
                uiState = uiState.copy(isProjectsLoading = true, errorMessage = null)
                viewModelScope.launch {
                    runCatching {
                        withContext(Dispatchers.IO) { client.createProject(cleanName, cleanPath) }
                    }.onSuccess { project ->
                        uiState = uiState.copy(
                            projects = (uiState.projects + project).distinctBy(HermesProject::id),
                            isProjectsLoading = false,
                            noticeMessage = "项目“${project.name}”已创建",
                        )
                    }.onFailure(::handleFailure)
                }
            }
        }
    }

    fun loadProjectDirectoryPicker(path: String? = null) {
        val client = apiClient ?: return
        if (uiState.isProjectPickerLoading) return
        uiState = uiState.copy(isProjectPickerLoading = true, errorMessage = null)
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    if (path.isNullOrBlank()) client.initialWorkspace() else client.listWorkspace(path)
                }
            }.onSuccess { listing ->
                uiState = uiState.copy(projectPickerListing = listing, isProjectPickerLoading = false)
            }.onFailure(::handleFailure)
        }
    }

    fun closeProjectDirectoryPicker() {
        uiState = uiState.copy(projectPickerListing = null, isProjectPickerLoading = false)
    }

    fun aiRenameSession(session: HermesSession) {
        val client = apiClient ?: return
        if (uiState.sessionActionId != null || uiState.isBatchRenaming) return
        uiState = uiState.copy(sessionActionId = session.id, errorMessage = null)
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val generated = client.generateSessionTitles(listOf(session))[session.id]
                        ?: throw ApiException(500, "Hermes 没有生成新的会话标题")
                    client.renameSession(session.id, generated)
                }
            }.onSuccess { title ->
                updateSession(session.id) { it.copy(title = title) }
                uiState = uiState.copy(
                    sessionActionId = null,
                    noticeMessage = "已重命名为“$title”",
                )
            }.onFailure(::handleFailure)
        }
    }

    fun batchAiRenameSessions() {
        val client = apiClient ?: return
        if (uiState.isBatchRenaming || uiState.sessionActionId != null) return
        val targets = uiState.sessions.filter { !it.source.equals("cron", true) && it.messageCount > 0 }
        if (targets.isEmpty()) {
            showNotice("当前没有可重命名的对话")
            return
        }
        uiState = uiState.copy(isBatchRenaming = true, errorMessage = null)
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val generated = client.generateSessionTitles(targets)
                    buildMap {
                        targets.forEach { session ->
                            val title = generated[session.id] ?: return@forEach
                            put(session.id, client.renameSession(session.id, title))
                        }
                    }
                }
            }.onSuccess { titles ->
                uiState = uiState.copy(
                    sessions = uiState.sessions.map { session ->
                        titles[session.id]?.let { session.copy(title = it) } ?: session
                    },
                    isBatchRenaming = false,
                    noticeMessage = "已完成 ${titles.size} 个对话改名",
                )
            }.onFailure(::handleFailure)
        }
    }

    fun toggleSessionPinned(session: HermesSession) {
        val client = apiClient ?: return
        if (uiState.sessionActionId != null) return
        val pinned = !session.isPinned
        uiState = uiState.copy(sessionActionId = session.id, errorMessage = null)
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) { client.setSessionPinned(session.id, pinned) } }
                .onSuccess {
                    updateSession(session.id) { it.copy(isPinned = pinned) }
                    uiState = uiState.copy(
                        sessionActionId = null,
                        noticeMessage = if (pinned) "会话已置顶" else "已取消置顶",
                    )
                }
                .onFailure(::handleFailure)
        }
    }

    fun archiveSession(session: HermesSession) {
        val client = apiClient ?: return
        if (uiState.sessionActionId != null) return
        uiState = uiState.copy(sessionActionId = session.id, errorMessage = null)
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) { client.archiveSession(session.id) } }
                .onSuccess {
                    val unreadSessionIds = uiState.unreadSessionIds - session.scopedId
                    configStore.saveUnreadSessionIds(unreadSessionIds)
                    uiState = uiState.copy(
                        sessions = uiState.sessions.filterNot { it.id == session.id },
                        sessionTotalCount = (uiState.sessionTotalCount - 1).coerceAtLeast(0),
                        unreadSessionIds = unreadSessionIds,
                        sessionActionId = null,
                        noticeMessage = "会话已归档，可在 Hermes 电脑端恢复",
                    )
                }
                .onFailure(::handleFailure)
        }
    }

    fun moveSessionToProject(session: HermesSession, project: HermesProject) {
        val client = apiClient ?: return
        if (uiState.sessionActionId != null) return
        uiState = uiState.copy(sessionActionId = session.id, errorMessage = null)
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) { client.moveSessionToProject(session, project) }
            }.onSuccess { updated ->
                updateSession(session.id) { current ->
                    current.copy(runtimeId = updated.runtimeId, workspacePath = updated.workspacePath)
                }
                uiState = uiState.copy(
                    sessionActionId = null,
                    noticeMessage = "已移至项目“${project.name}”",
                )
                refreshProjects()
            }.onFailure(::handleFailure)
        }
    }

    fun updateDraft(value: String) {
        uiState.selectedSession?.let { session ->
            configStore.saveDraft(session.profile, session.id, value)
        }
        val token = value.trimStart()
        val slashQuery = token.takeIf { it.startsWith('/') && !it.contains(Regex("\\s")) }
        uiState = uiState.copy(
            draft = value,
            slashCommands = if (slashQuery == null) emptyList() else uiState.slashCommands,
            isSlashCommandsLoading = if (slashQuery == null) false else uiState.isSlashCommandsLoading,
        )
        if (slashQuery == null) {
            slashCommandJob?.cancel()
            slashCommandQuery = ""
        } else {
            loadSlashCommands(slashQuery)
        }
    }

    fun loadCommandCatalog(force: Boolean = false) {
        val client = apiClient ?: return
        val profile = client.currentProfile()
        val cached = commandCatalogCache[profile]
        if (!force && cached != null) {
            uiState = uiState.copy(commandCatalog = cached, isCommandCatalogLoading = false)
            return
        }
        if (uiState.isCommandCatalogLoading) return
        uiState = uiState.copy(isCommandCatalogLoading = true)
        viewModelScope.launch {
            val commands = runCatching { withContext(Dispatchers.IO) { client.slashCommands() } }
                .getOrElse { DEFAULT_SLASH_COMMANDS }
                .ifEmpty { DEFAULT_SLASH_COMMANDS }
                .distinctBy(SlashCommand::command)
            if (client.currentProfile() == profile) {
                commandCatalogCache[profile] = commands
                uiState = uiState.copy(commandCatalog = commands, isCommandCatalogLoading = false)
            }
        }
    }

    private fun loadSlashCommands(query: String) {
        val client = apiClient ?: return
        val cachedCatalog = commandCatalogCache[client.currentProfile()].orEmpty()
        if (cachedCatalog.isNotEmpty()) {
            val cleanQuery = query.trim().removePrefix("/")
            uiState = uiState.copy(
                slashCommands = cachedCatalog.filter { it.command.removePrefix("/").startsWith(cleanQuery, ignoreCase = true) },
                isSlashCommandsLoading = false,
            )
            slashCommandQuery = query
            return
        }
        if (query == slashCommandQuery && (uiState.slashCommands.isNotEmpty() || uiState.isSlashCommandsLoading)) return
        slashCommandQuery = query
        slashCommandJob?.cancel()
        uiState = uiState.copy(isSlashCommandsLoading = true)
        slashCommandJob = viewModelScope.launch {
            delay(120)
            val commands = runCatching { withContext(Dispatchers.IO) { client.slashCommands(query) } }
                .getOrElse { DEFAULT_SLASH_COMMANDS.filter { it.command.startsWith(query, ignoreCase = true) } }
            if (slashCommandQuery == query) {
                uiState = uiState.copy(
                    slashCommands = commands.ifEmpty { DEFAULT_SLASH_COMMANDS.filter { it.command.startsWith(query, ignoreCase = true) } },
                    isSlashCommandsLoading = false,
                )
            }
        }
    }

    fun addAttachments(uris: List<Uri>) {
        if (uris.isEmpty()) return
        val resolver = getApplication<Application>().contentResolver
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) { uris.map { AttachmentReader.read(resolver, it) } }
            }.onSuccess { attachments ->
                uiState = uiState.copy(
                    attachments = (uiState.attachments + attachments).take(5),
                    noticeMessage = if (uiState.attachments.size + attachments.size > 5) {
                        "单次最多添加 5 个附件"
                    } else {
                        null
                    },
                )
            }.onFailure(::handleFailure)
        }
    }

    @Suppress("DEPRECATION")
    fun handleShareIntent(intent: Intent?) {
        intent ?: return
        if (intent.action !in setOf(Intent.ACTION_SEND, Intent.ACTION_SEND_MULTIPLE)) return
        val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty().trim()
        val uris = buildList {
            intent.clipData?.let { clip ->
                for (index in 0 until clip.itemCount) clip.getItemAt(index).uri?.let(::add)
            }
            (intent.getParcelableExtra(Intent.EXTRA_STREAM) as? Uri)?.let(::add)
            intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)?.let(::addAll)
        }.distinct().take(5)
        if (sharedText.isBlank() && uris.isEmpty()) return

        if (uris.isEmpty()) {
            uiState = uiState.copy(incomingShare = IncomingShare(sharedText = sharedText))
            return
        }
        uiState = uiState.copy(isSharePreparing = true, errorMessage = null)
        val resolver = getApplication<Application>().contentResolver
        viewModelScope.launch {
            val results = withContext(Dispatchers.IO) {
                uris.map { uri -> runCatching { AttachmentReader.read(resolver, uri) } }
            }
            val attachments = results.mapNotNull(Result<PendingAttachment>::getOrNull)
            val skipped = results.count(Result<PendingAttachment>::isFailure)
            uiState = uiState.copy(
                incomingShare = IncomingShare(sharedText = sharedText, attachments = attachments),
                isSharePreparing = false,
                noticeMessage = if (skipped > 0) "$skipped 个暂不支持的文件未加入分享" else null,
            )
        }
    }

    fun updateIncomingShareInstruction(value: String) {
        uiState.incomingShare?.let { uiState = uiState.copy(incomingShare = it.copy(instruction = value)) }
    }

    fun dismissIncomingShare() {
        if (uiState.isShareSending) return
        uiState = uiState.copy(incomingShare = null, isSharePreparing = false)
    }

    fun sendIncomingShare(sessionId: String?) {
        val client = apiClient ?: return showNotice("请先连接 Hermes，再发送分享内容")
        val payload = uiState.incomingShare ?: return
        if (uiState.isStreaming) return showNotice("当前任务仍在执行，请完成后再发送分享内容")
        if (uiState.isShareSending) return
        uiState = uiState.copy(isShareSending = true, errorMessage = null)
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val session = sessionId?.let { id -> uiState.sessions.firstOrNull { it.id == id } }
                        ?: client.createSession()
                    val history = if (session.messageCount > 0) client.loadMessages(session) else emptyList()
                    session to history
                }
            }.onSuccess { (session, history) ->
                messageCache[session.scopedId] = history
                val insights = ChatInsightParser.fromMessages(history)
                val prompt = buildString {
                    payload.instruction.trim().takeIf(String::isNotBlank)?.let(::append)
                    payload.sharedText.trim().takeIf(String::isNotBlank)?.let { text ->
                        if (isNotEmpty()) append("\n\n")
                        append("分享内容：\n").append(text)
                    }
                }
                uiState = uiState.copy(
                    route = AppRoute.CHAT,
                    selectedSession = session,
                    messages = history.visibleConversationMessages(),
                    toolActivities = emptyList(),
                    chatArtifacts = insights.artifacts,
                    chatTodos = insights.todos,
                    draft = "",
                    attachments = emptyList(),
                    incomingShare = null,
                    isShareSending = false,
                    highlightedMessageId = null,
                )
                startMessage(session, prompt, payload.attachments)
            }.onFailure {
                uiState = uiState.copy(isShareSending = false)
                handleFailure(it)
            }
        }
    }

    fun removeAttachment(id: String) {
        uiState = uiState.copy(attachments = uiState.attachments.filterNot { it.id == id })
    }

    fun loadModelCatalog() {
        val client = apiClient ?: return
        if (uiState.isModelsLoading || uiState.modelCatalog.providers.isNotEmpty()) return
        uiState = uiState.copy(isModelsLoading = true, errorMessage = null)
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) { client.modelCatalog() } }
                .onSuccess { catalog ->
                    uiState = uiState.copy(modelCatalog = catalog, isModelsLoading = false)
                }
                .onFailure(::handleFailure)
        }
    }

    fun switchModel(provider: String, model: String) {
        val client = apiClient ?: return
        val session = uiState.selectedSession ?: return
        if (uiState.isStreaming || uiState.isModelSwitching) return
        uiState = uiState.copy(isModelSwitching = true, errorMessage = null)
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) { client.switchSessionModel(session, provider, model) }
            }.onSuccess { updated ->
                uiState = uiState.copy(
                    selectedSession = updated,
                    isModelSwitching = false,
                    noticeMessage = "当前会话已切换到 ${model.substringAfterLast('/')}",
                )
            }.onFailure(::handleFailure)
        }
    }

    fun setCouncilMode(mode: CouncilMode) {
        if (uiState.isStreaming) return showNotice("请在当前任务完成后开启专家会审")
        if (mode == CouncilMode.OFF || mode == CouncilMode.DEEP) {
            uiState = uiState.copy(councilMode = mode)
            return
        }
        val client = apiClient ?: return
        val session = uiState.selectedSession ?: return
        if (session.provider.isMoaProvider()) {
            uiState = uiState.copy(councilMode = CouncilMode.QUICK)
            return
        }
        val moaProvider = uiState.modelCatalog.providers.firstOrNull { it.slug.isMoaProvider() }
        val moaModel = moaProvider?.models?.firstOrNull()
        if (moaProvider == null || moaModel == null) {
            if (uiState.modelCatalog.providers.isEmpty()) loadModelCatalog()
            return showNotice("服务器尚未提供 MoA 预设；可先使用深度会审，或在 Hermes 中配置 MoA")
        }
        if (uiState.isModelSwitching) return
        uiState = uiState.copy(
            isModelSwitching = true,
            errorMessage = null,
            noticeMessage = "正在切换到 MoA 会审模型…",
        )
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) { client.switchSessionModel(session, moaProvider.slug, moaModel) }
            }.onSuccess { updated ->
                uiState = uiState.copy(
                    selectedSession = updated,
                    councilMode = CouncilMode.QUICK,
                    isModelSwitching = false,
                    noticeMessage = "已切换到 MoA：${moaModel.substringAfterLast('/')}",
                )
            }.onFailure { error ->
                uiState = uiState.copy(isModelSwitching = false)
                handleFailure(error)
            }
        }
    }

    fun openChatArtifact(artifact: ChatArtifact) {
        val client = apiClient ?: return
        val source = uiState.selectedSession?.let { session -> recentArtifactSource(session, artifact) }
        if (source != null) {
            val merged = (listOf(source) + uiState.recentArtifacts)
                .distinctBy { "${it.profile}::${it.path}" }
                .take(60)
            uiState = uiState.copy(recentArtifacts = merged)
            configStore.saveRecentArtifacts(merged)
        }
        if (artifact.path.substringAfterLast('.', "").lowercase() in setOf("png", "jpg", "jpeg", "webp", "gif", "bmp")) {
            openImage(artifact.path, artifact.name)
            return
        }
        if (!artifact.path.isPreviewableArtifact()) {
            showNotice("${artifact.name} 已列入聊天产物；当前版本支持图片、Markdown、PDF、HTML 和常见文本预览")
            return
        }
        uiState = uiState.copy(isWorkspaceLoading = true, errorMessage = null)
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) { client.readWorkspaceDocument(artifact.path) } }
                .onSuccess { document ->
                    uiState = uiState.copy(
                        route = AppRoute.WORKSPACE,
                        workspaceDocument = document,
                        workspaceDocumentOrigin = AppRoute.CHAT,
                        workspaceSourceArtifact = source,
                        workspaceDraft = document.content,
                        isWorkspaceEditing = false,
                        isWorkspaceLoading = false,
                    )
                }
                .onFailure(::handleFailure)
        }
    }

    fun openChatLink(rawTarget: String) {
        val target = normalizeChatLinkTarget(rawTarget)
        if (target.isBlank()) {
            showNotice("文件链接为空，无法打开")
            return
        }
        if (target.startsWith("http://", ignoreCase = true) || target.startsWith("https://", ignoreCase = true)) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(target)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { getApplication<Application>().startActivity(intent) }
                .onFailure { showNotice("没有找到可以打开这个链接的应用") }
            return
        }
        openChatArtifact(
            ChatArtifact(
                path = target,
                name = target.substringAfterLast('/').ifBlank { "聊天文件" },
                kind = "文件",
            ),
        )
    }

    fun openRecentArtifact(item: RecentArtifact) {
        val artifact = ChatArtifact(item.path, item.name, item.kind)
        if (item.path.substringAfterLast('.', "").lowercase() in setOf("png", "jpg", "jpeg", "webp", "gif", "bmp")) {
            openImage(item.path, item.name)
            return
        }
        if (!item.path.isPreviewableArtifact()) {
            showNotice("${item.name} 暂不支持在 APP 内预览，可保存到手机或回到来源对话继续处理")
            return
        }
        val client = apiClient ?: return
        uiState = uiState.copy(isWorkspaceLoading = true, errorMessage = null)
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) { client.readWorkspaceDocument(artifact.path) } }
                .onSuccess { document ->
                    uiState = uiState.copy(
                        route = AppRoute.WORKSPACE,
                        workspaceDocument = document,
                        workspaceDocumentOrigin = null,
                        workspaceSourceArtifact = item,
                        workspaceDraft = document.content,
                        isWorkspaceEditing = false,
                        isWorkspaceLoading = false,
                    )
                }
                .onFailure(::handleFailure)
        }
    }

    fun openArtifactSource(item: RecentArtifact) {
        if (item.profile != uiState.activeProfile) {
            showNotice("请先切换到 Profile：${item.profile}")
            return
        }
        val session = uiState.sessions.firstOrNull { it.id == item.sessionId }
            ?: HermesSession(
                id = item.sessionId,
                title = item.sessionTitle.ifBlank { "Hermes 对话" },
                profile = item.profile,
            )
        uiState = uiState.copy(
            workspaceDocument = null,
            workspaceSourceArtifact = null,
            workspaceDocumentOrigin = null,
        )
        openSessionInternal(session, item.messageId.takeIf(String::isNotBlank))
    }

    fun sendMessage() {
        val session = uiState.selectedSession ?: return
        val prompt = uiState.draft.trim()
        val attachments = uiState.attachments
        if (prompt.isBlank() && attachments.isEmpty()) return
        if (uiState.isStreaming) return
        if (uiState.isModelSwitching) return showNotice("模型正在切换，请稍后发送")

        val requestedMode = uiState.councilMode
        val effectiveMode = requestedMode.takeUnless { prompt.trimStart().startsWith('/') } ?: CouncilMode.OFF
        val submittedPrompt = buildCouncilPrompt(
            prompt = prompt.ifBlank { "请查看我发送的附件。" },
            mode = effectiveMode,
        )
        startMessage(session, prompt, attachments, submittedPrompt, effectiveMode)
    }

    private fun startMessage(
        session: HermesSession,
        prompt: String,
        attachments: List<PendingAttachment>,
        submittedPromptOverride: String? = null,
        councilMode: CouncilMode = CouncilMode.OFF,
    ) {
        val client = apiClient ?: return
        if (uiState.isStreaming) return

        val submittedPrompt = submittedPromptOverride ?: prompt.ifBlank { "请查看我发送的附件。" }

        val displayText = buildString {
            if (prompt.isNotBlank()) append(prompt)
            attachments.forEach { attachment ->
                if (isNotEmpty()) append('\n')
                append("📎 ").append(attachment.name)
            }
        }
        val userMessage = ChatMessage(
            role = MessageRole.USER,
            content = displayText,
            images = attachments.mapNotNull { attachment ->
                attachment.dataUrl?.let { ChatImage(attachment.name, it, attachment.mimeType) }
            },
        )
        val assistantMessage = ChatMessage(
            role = MessageRole.ASSISTANT,
            content = "",
            isStreaming = true,
        )
        if (session.messageCount == 0 || session.title == "新会话") {
            pendingTitleSessionIds += session.id
        }
        streamRecoveryJob?.cancel()
        streamingDeltaFlushJob?.cancel()
        streamingDeltaFlushJob = null
        streamingDeltaBuffer.clear()
        streamRecoveryPrompt = submittedPrompt
        activeSubmittedPrompt = prompt
        activeSubmittedAttachments = attachments
        activeUserMessageId = userMessage.id
        streamBaselineAssistantSignature = uiState.messages
            .lastOrNull { it.role == MessageRole.ASSISTANT }
            ?.recoverySignature()
            .orEmpty()
        activeStreamSession = session
        activeStreamToolActivities = emptyList()
        activeStreamArtifacts = emptyList()
        activeStreamTodos = emptyList()
        val baseMessages = if (uiState.selectedSession?.scopedId == session.scopedId) {
            uiState.messages
        } else {
            messageCache[session.scopedId].visibleConversationMessages()
        }
        val runningMessages = baseMessages + userMessage + assistantMessage
        val startedAtMillis = System.currentTimeMillis()
        configStore.saveActiveRunSnapshot(
            ActiveRunSnapshot(
                profile = session.profile,
                sessionId = session.id,
                title = session.title,
                submittedPrompt = submittedPrompt,
                baselineAssistantSignature = streamBaselineAssistantSignature,
                startedAtMillis = startedAtMillis,
            ),
        )
        val unreadSessionIds = uiState.unreadSessionIds - session.scopedId
        configStore.saveUnreadSessionIds(unreadSessionIds)
        configStore.clearDraft(session.profile, session.id)
        uiState = uiState.copy(
            draft = if (uiState.selectedSession?.scopedId == session.scopedId) "" else uiState.draft,
            attachments = if (uiState.selectedSession?.scopedId == session.scopedId) emptyList() else uiState.attachments,
            failedSend = if (uiState.selectedSession?.scopedId == session.scopedId) null else uiState.failedSend,
            messages = if (uiState.selectedSession?.scopedId == session.scopedId) runningMessages else uiState.messages,
            toolActivities = if (uiState.selectedSession?.scopedId == session.scopedId) emptyList() else uiState.toolActivities,
            councilMode = CouncilMode.OFF,
            activeCouncilMode = councilMode,
            isStreaming = true,
            streamingSessionId = session.id,
            runStage = "正在连接 Hermes",
            runStartedAtMillis = startedAtMillis,
            runLastActivityAtMillis = startedAtMillis,
            unreadSessionIds = unreadSessionIds,
            isRecoveringConnection = false,
            errorMessage = null,
            noticeMessage = null,
        )
        cacheMessages(session.id, runningMessages)

        val controller = StreamController()
        streamController = controller
        streamJob = viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                client.streamMessage(
                    controller = controller,
                    session = session,
                    prompt = submittedPrompt,
                    attachments = attachments,
                    onEvent = { event ->
                        viewModelScope.launch { handleStreamEvent(event) }
                    },
                )
            }.onFailure { throwable ->
                if (!controller.isStopped() && !controller.wasDisconnected()) {
                    withContext(Dispatchers.Main) { handleStreamFailure(throwable) }
                }
            }
        }
        startStreamWatchdog(session)
    }

    fun steerCurrentRun() {
        val client = apiClient ?: return
        val runtimeSessionId = streamController?.runtimeSessionId ?: return showNotice("Hermes 运行尚未就绪，请稍后再试")
        val text = uiState.draft.trim()
        if (text.isBlank()) return
        if (uiState.attachments.isNotEmpty()) {
            showNotice("追加要求暂不支持附件；可改用排队发送")
            return
        }
        if (uiState.isSteering) return
        uiState = uiState.copy(isSteering = true, errorMessage = null)
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) { client.steerSession(runtimeSessionId, text) } }
                .onSuccess { status ->
                    uiState.selectedSession?.let { configStore.clearDraft(it.profile, it.id) }
                    uiState = uiState.copy(
                        draft = "",
                        isSteering = false,
                        runStage = "已收到追加要求",
                        runLastActivityAtMillis = System.currentTimeMillis(),
                        noticeMessage = if (status.equals("queued", true)) "追加要求已送达 Hermes" else "Hermes 已接收追加要求",
                    )
                }
                .onFailure {
                    uiState = uiState.copy(isSteering = false)
                    handleFailure(it)
                }
        }
    }

    fun queueCurrentMessage() {
        val session = activeStreamSession ?: return
        if (uiState.selectedSession?.scopedId != session.scopedId) return
        val prompt = uiState.draft.trim()
        val attachments = uiState.attachments
        if (prompt.isBlank() && attachments.isEmpty()) return
        val queued = QueuedRunMessage(session, prompt.ifBlank { "请查看我发送的附件。" }, attachments)
        configStore.clearDraft(session.profile, session.id)
        uiState = uiState.copy(
            draft = "",
            attachments = emptyList(),
            queuedRunMessage = queued,
            noticeMessage = if (uiState.queuedRunMessage == null) "消息已排队，将在本轮完成后发送" else "已替换排队中的消息",
        )
    }

    fun cancelQueuedMessage() {
        uiState = uiState.copy(queuedRunMessage = null, noticeMessage = "已取消排队消息")
    }

    fun respondToAgentRequest(request: AgentRequest, answer: String) {
        val client = apiClient ?: return
        if (answer.isBlank() || request.isResponding) return
        uiState = uiState.copy(
            pendingAgentRequests = uiState.pendingAgentRequests.map {
                if (it.requestId == request.requestId) it.copy(isResponding = true) else it
            },
            errorMessage = null,
        )
        configStore.savePendingAgentRequests(uiState.pendingAgentRequests)
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) { client.respondAgentRequest(request, answer) } }
                .onSuccess {
                    uiState = uiState.copy(
                        pendingAgentRequests = uiState.pendingAgentRequests.filterNot { it.requestId == request.requestId },
                        runStage = "已处理，Hermes 正在继续",
                        runLastActivityAtMillis = System.currentTimeMillis(),
                        noticeMessage = "已提交给 Hermes",
                    )
                    configStore.savePendingAgentRequests(uiState.pendingAgentRequests)
                    resumeRecoveryAfterAgentResponse(request)
                }
                .onFailure { throwable ->
                    uiState = uiState.copy(
                        pendingAgentRequests = uiState.pendingAgentRequests.map {
                            if (it.requestId == request.requestId) it.copy(isResponding = false) else it
                        },
                    )
                    configStore.savePendingAgentRequests(uiState.pendingAgentRequests)
                    handleFailure(throwable)
                }
        }
    }

    fun retryFailedMessage() {
        if (uiState.isStreaming || uiState.failedSend == null) return
        sendMessage()
    }

    fun stopGeneration() {
        flushStreamingDelta()
        val controller = streamController
        val runtimeSessionId = controller?.runtimeSessionId
        val sessionId = uiState.streamingSessionId
        controller?.stop()
        streamJob?.cancel()
        streamRecoveryJob?.cancel()
        streamRecoveryJob = null
        streamWatchdogJob?.cancel()
        streamWatchdogJob = null
        streamRecoveryPrompt = ""
        streamBaselineAssistantSignature = ""
        val stoppedMessages = activeStreamMessages().mapNotNull { message ->
            if (!message.isStreaming) return@mapNotNull message
            message.takeIf { it.content.isNotBlank() || it.images.isNotEmpty() }
                ?.copy(isStreaming = false)
        }
        if (sessionId != null) cacheMessages(sessionId, stoppedMessages)
        uiState = uiState.copy(
            isStreaming = false,
            streamingSessionId = null,
            runStage = "",
            activeCouncilMode = CouncilMode.OFF,
            isSteering = false,
            queuedRunMessage = null,
            pendingAgentRequests = uiState.pendingAgentRequests.filterNot { it.conversationId == sessionId },
            isRecoveringConnection = false,
            messages = if (uiState.selectedSession?.id == sessionId) stoppedMessages else uiState.messages,
            noticeMessage = "已请求停止",
        )
        configStore.savePendingAgentRequests(uiState.pendingAgentRequests)
        clearActiveStreamState()
        if (!runtimeSessionId.isNullOrBlank()) {
            val client = apiClient ?: return consumePendingDeepLink()
            viewModelScope.launch(Dispatchers.IO) {
                runCatching { client.stopRun(runtimeSessionId) }
                withContext(Dispatchers.Main) { consumePendingDeepLink() }
            }
        } else {
            consumePendingDeepLink()
        }
    }

    fun backToSessions() {
        uiState.selectedSession?.id?.let { sessionId ->
            if (uiState.messages.isNotEmpty()) cacheMessages(sessionId, uiState.messages)
        }
        val returnRoute = chatReturnRoute
        chatReturnRoute = AppRoute.SESSIONS
        uiState = uiState.copy(
            route = returnRoute,
            selectedSession = null,
            messages = emptyList(),
            toolActivities = emptyList(),
            attachments = emptyList(),
            draft = "",
            failedSend = null,
            councilMode = CouncilMode.OFF,
            inlineImagePreviews = emptyMap(),
            inlineImageLoading = emptySet(),
            inlineImageFailures = emptySet(),
        )
        if (returnRoute == AppRoute.TASKS) refreshTasks() else refreshSessions()
    }

    fun showSessions() {
        if (uiState.route == AppRoute.SESSIONS) return
        uiState = uiState.copy(route = AppRoute.SESSIONS, errorMessage = null, noticeMessage = null)
        refreshSessions()
    }

    fun openActiveRun() {
        val session = activeStreamSession ?: return
        openTaskSession(session)
    }

    fun openRunCompletion(completion: RunCompletionSummary) {
        val session = uiState.sessions.firstOrNull { it.id == completion.sessionId }
            ?: HermesSession(
                id = completion.sessionId,
                title = completion.title.ifBlank { "Hermes 对话" },
                profile = uiState.activeProfile,
            )
        openTaskSession(session)
    }

    fun handleDeepLink(intent: Intent?) {
        intent ?: return
        val route = intent.getStringExtra(HermesNotifications.EXTRA_ROUTE)
            ?: intent.data?.pathSegments?.firstOrNull()
            ?: return
        val profile = intent.getStringExtra(HermesNotifications.EXTRA_PROFILE)
            ?: intent.data?.getQueryParameter("profile")
        val sessionId = intent.getStringExtra(HermesNotifications.EXTRA_SESSION_ID)
            ?: intent.data?.getQueryParameter("session")
        pendingDeepLink = HermesDeepLink(route, profile, sessionId)
        consumePendingDeepLink()
    }

    private fun consumePendingDeepLink() {
        val target = pendingDeepLink ?: return
        val client = apiClient ?: return
        val targetProfile = target.profile?.takeIf(String::isNotBlank)
        if (targetProfile != null && targetProfile != client.currentProfile()) {
            if (uiState.isStreaming) {
                showNotice("当前任务仍在执行，完成后可打开通知对应的 Profile")
                return
            }
            val profile = uiState.profiles.firstOrNull { it.name == targetProfile } ?: return
            selectProfile(profile)
            return
        }
        when (target.route.lowercase()) {
            "tasks" -> {
                pendingDeepLink = null
                showTasks()
            }
            "chat" -> {
                val sessionId = target.sessionId?.takeIf(String::isNotBlank) ?: return
                if (uiState.isBusy && uiState.sessions.isEmpty()) return
                val session = uiState.sessions.firstOrNull { it.id == sessionId }
                    ?: HermesSession(
                        id = sessionId,
                        title = "Hermes 对话",
                        profile = targetProfile ?: uiState.activeProfile,
                    )
                pendingDeepLink = null
                openSession(session)
            }
            else -> {
                pendingDeepLink = null
                showSessions()
            }
        }
    }

    fun showSessionSearch() {
        uiState = uiState.copy(
            route = AppRoute.SEARCH,
            searchQuery = "",
            searchResults = emptyList(),
            isSearchLoading = false,
            errorMessage = null,
            noticeMessage = null,
        )
    }

    fun closeSessionSearch() {
        sessionSearchJob?.cancel()
        sessionSearchJob = null
        uiState = uiState.copy(
            route = AppRoute.SESSIONS,
            searchQuery = "",
            searchResults = emptyList(),
            isSearchLoading = false,
            errorMessage = null,
            noticeMessage = null,
        )
    }

    fun searchSessions(query: String) {
        val keyword = query.trim()
        sessionSearchJob?.cancel()
        uiState = uiState.copy(searchQuery = query, errorMessage = null)
        if (keyword.length < 2) {
            uiState = uiState.copy(searchResults = emptyList(), isSearchLoading = false)
            return
        }
        val client = apiClient ?: return
        val sessions = uiState.sessions.take(100)
        val expectedProfile = client.currentProfile()
        val cachedResults = sessions.mapNotNull { session ->
            val cached = messageCache[session.scopedId]
            searchResult(session, cached, keyword)
                ?: metadataSearchResult(session, keyword)
        }
        uiState = uiState.copy(searchResults = cachedResults, isSearchLoading = true)
        sessionSearchJob = viewModelScope.launch {
            val loaded = withContext(Dispatchers.IO) {
                sessions.chunked(4).flatMap { chunk ->
                    coroutineScope {
                        chunk.map { session ->
                            async { session to runCatching { client.loadMessages(session, SEARCH_MESSAGE_LIMIT) }.getOrNull() }
                        }.awaitAll()
                    }
                }
            }
            if (client.currentProfile() != expectedProfile || uiState.searchQuery.trim() != keyword) return@launch
            loaded.forEach { (session, messages) ->
                if (messages != null) {
                    messageCache[session.scopedId] = messages
                    rememberRecentArtifacts(session, messages)
                }
            }
            while (messageCache.size > 24) messageCache.remove(messageCache.keys.first())
            val results = loaded.mapNotNull { (session, messages) ->
                searchResult(session, messages, keyword) ?: metadataSearchResult(session, keyword)
            }
            uiState = uiState.copy(searchResults = results, isSearchLoading = false)
        }
    }

    fun openSearchResult(result: SessionSearchResult) {
        sessionSearchJob?.cancel()
        openSessionInternal(result.session, result.messageId)
    }

    fun showWorkspace() {
        uiState = uiState.copy(
            route = AppRoute.WORKSPACE,
            workspaceDocumentOrigin = null,
            workspaceSourceArtifact = null,
            errorMessage = null,
            noticeMessage = null,
        )
        if (uiState.workspaceListing == null) refreshWorkspace(resetToRoot = true)
        indexRecentArtifacts(force = false)
    }

    fun refreshRecentArtifacts() {
        indexRecentArtifacts(force = true)
    }

    private fun indexRecentArtifacts(force: Boolean) {
        val client = apiClient ?: return
        val expectedProfile = client.currentProfile()
        if (uiState.isRecentArtifactsLoading || (!force && expectedProfile in indexedArtifactProfiles)) return
        uiState = uiState.copy(isRecentArtifactsLoading = true)
        viewModelScope.launch {
            val sessions = withContext(Dispatchers.IO) {
                if (force) runCatching { client.listSessions().sessions }.getOrNull() else null
            }?.distinctBy(HermesSession::id)?.also { refreshed ->
                if (client.currentProfile() == expectedProfile) {
                    uiState = uiState.copy(sessions = refreshed, sessionTotalCount = maxOf(uiState.sessionTotalCount, refreshed.size))
                }
            } ?: uiState.sessions
            val candidates = sessions.take(RECENT_ARTIFACT_SESSION_LIMIT)
            if (candidates.isEmpty()) {
                uiState = uiState.copy(isRecentArtifactsLoading = false)
                return@launch
            }
            val snapshot = configStore.readArtifactIndexSnapshot().toMutableMap()
            val changed = candidates.filter { session ->
                snapshot[session.scopedId] != artifactIndexFingerprint(session)
            }
            if (changed.isEmpty()) {
                indexedArtifactProfiles += expectedProfile
                uiState = uiState.copy(isRecentArtifactsLoading = false)
                return@launch
            }
            val loaded = withContext(Dispatchers.IO) {
                changed.chunked(4).flatMap { chunk ->
                    coroutineScope {
                        chunk.map { session ->
                            async {
                                session to runCatching {
                                    client.loadMessages(session, RECENT_ARTIFACT_MESSAGE_LIMIT)
                                }.getOrNull()
                            }
                        }.awaitAll()
                    }
                }
            }
            if (client.currentProfile() != expectedProfile) {
                uiState = uiState.copy(isRecentArtifactsLoading = false)
                return@launch
            }
            val discovered = loaded.flatMap { (session, messages) ->
                if (messages == null) emptyList() else {
                    messageCache[session.scopedId] = messages
                    snapshot[session.scopedId] = artifactIndexFingerprint(session)
                    discoverRecentArtifacts(session, messages)
                }
            }
            while (messageCache.size > 24) messageCache.remove(messageCache.keys.first())
            if (loaded.any { it.second != null }) {
                indexedArtifactProfiles += expectedProfile
                mergeRecentArtifacts(discovered)
                configStore.saveArtifactIndexSnapshot(snapshot)
            }
            uiState = uiState.copy(isRecentArtifactsLoading = false)
        }
    }

    fun refreshWorkspace(resetToRoot: Boolean = false) {
        val client = apiClient ?: return
        val existing = uiState.workspaceListing
        uiState = uiState.copy(isWorkspaceLoading = true, errorMessage = null)
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    if (resetToRoot || existing == null) client.initialWorkspace()
                    else client.listWorkspace(existing.path).copy(projectName = existing.projectName)
                }
            }.onSuccess { listing ->
                uiState = uiState.copy(
                    workspaceListing = listing,
                    workspaceRootPath = if (resetToRoot || uiState.workspaceRootPath == null) listing.path else uiState.workspaceRootPath,
                    workspaceDocument = null,
                    workspaceDocumentOrigin = null,
                    workspaceSourceArtifact = null,
                    workspaceDraft = "",
                    isWorkspaceEditing = false,
                    isWorkspaceLoading = false,
                )
            }.onFailure(::handleFailure)
        }
    }

    fun openWorkspaceDirectory(path: String) {
        val client = apiClient ?: return
        val current = uiState.workspaceListing ?: return
        val root = uiState.workspaceRootPath ?: current.path
        if (!pathIsWithin(root, path)) {
            showError("不能离开当前 Hermes 项目目录")
            return
        }
        uiState = uiState.copy(isWorkspaceLoading = true, errorMessage = null)
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) { client.listWorkspace(path) } }
                .onSuccess { listing ->
                    uiState = uiState.copy(
                        workspaceListing = listing.copy(projectName = current.projectName),
                        isWorkspaceLoading = false,
                    )
                }
                .onFailure(::handleFailure)
        }
    }

    fun openWorkspaceDocument(path: String) {
        val client = apiClient ?: return
        uiState = uiState.copy(isWorkspaceLoading = true, errorMessage = null)
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) { client.readWorkspaceDocument(path) } }
                .onSuccess { document ->
                    uiState = uiState.copy(
                        workspaceDocument = document,
                        workspaceDocumentOrigin = null,
                        workspaceSourceArtifact = null,
                        workspaceDraft = document.content,
                        isWorkspaceEditing = false,
                        isWorkspaceLoading = false,
                    )
                }
                .onFailure(::handleFailure)
        }
    }

    fun openImage(source: String, name: String = "图片") {
        val client = apiClient ?: return
        uiState = uiState.copy(isImageLoading = true, errorMessage = null)
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) { client.readImage(source) } }
                .onSuccess { image ->
                    uiState = uiState.copy(
                        imagePreview = image.copy(name = image.name.takeUnless { it == "图片" }.orEmpty().ifBlank { name }),
                        isImageLoading = false,
                    )
                }
                .onFailure(::handleFailure)
        }
    }

    fun loadInlineChatImages(sources: List<String>) {
        val client = apiClient ?: return
        val sessionId = uiState.selectedSession?.id ?: return
        val pending = sources.asSequence()
            .filter(String::isNotBlank)
            .distinct()
            .filterNot { source ->
                source in uiState.inlineImagePreviews ||
                    source in uiState.inlineImageLoading ||
                    source in uiState.inlineImageFailures ||
                    source.startsWith("data:image/", ignoreCase = true)
            }
            .take(12)
            .toList()
        if (pending.isEmpty()) return

        uiState = uiState.copy(inlineImageLoading = uiState.inlineImageLoading + pending)
        viewModelScope.launch {
            pending.forEach { source ->
                val result = runCatching { withContext(Dispatchers.IO) { client.readImage(source) } }
                if (uiState.selectedSession?.id != sessionId) return@launch
                result.onSuccess { image ->
                    uiState = uiState.copy(
                        inlineImagePreviews = uiState.inlineImagePreviews + (source to image),
                        inlineImageLoading = uiState.inlineImageLoading - source,
                    )
                }.onFailure {
                    uiState = uiState.copy(
                        inlineImageLoading = uiState.inlineImageLoading - source,
                        inlineImageFailures = uiState.inlineImageFailures + source,
                    )
                }
            }
        }
    }

    fun closeImagePreview() {
        uiState = uiState.copy(imagePreview = null, isImageLoading = false)
    }

    fun closeWorkspaceDocument() {
        val returnRoute = uiState.workspaceDocumentOrigin
        uiState = uiState.copy(
            route = returnRoute ?: uiState.route,
            workspaceDocument = null,
            workspaceDocumentOrigin = null,
            workspaceSourceArtifact = null,
            workspaceDraft = "",
            isWorkspaceEditing = false,
            errorMessage = null,
        )
    }

    fun openMemoryFile() = openProfileFile(HermesProfileFile.MEMORY)

    fun openSoulFile() = openProfileFile(HermesProfileFile.SOUL)

    private fun openProfileFile(file: HermesProfileFile) {
        val client = apiClient ?: return
        uiState = uiState.copy(
            route = AppRoute.PROFILE_FILE,
            workspaceDocument = null,
            workspaceDocumentOrigin = AppRoute.PROFILE,
            workspaceSourceArtifact = null,
            workspaceDraft = "",
            isWorkspaceEditing = false,
            isWorkspaceLoading = true,
            errorMessage = null,
            noticeMessage = null,
        )
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) { client.readProfileFile(file) } }
                .onSuccess { document ->
                    if (uiState.route != AppRoute.PROFILE_FILE) return@onSuccess
                    uiState = uiState.copy(
                        workspaceDocument = document,
                        workspaceDraft = document.content,
                        isWorkspaceLoading = false,
                    )
                }
                .onFailure { error ->
                    if (uiState.route != AppRoute.PROFILE_FILE) return@onFailure
                    uiState = uiState.copy(
                        route = AppRoute.PROFILE,
                        workspaceDocument = null,
                        workspaceDocumentOrigin = null,
                        workspaceDraft = "",
                        isWorkspaceLoading = false,
                    )
                    handleFailure(error)
                }
        }
    }

    fun setWorkspaceEditing(editing: Boolean) {
        uiState = uiState.copy(
            isWorkspaceEditing = editing,
            workspaceDraft = if (!editing) uiState.workspaceDocument?.content.orEmpty() else uiState.workspaceDraft,
        )
    }

    fun updateWorkspaceDraft(value: String) {
        uiState = uiState.copy(workspaceDraft = value)
    }

    fun saveWorkspaceDocument() {
        val client = apiClient ?: return
        val document = uiState.workspaceDocument ?: return
        if (uiState.isWorkspaceSaving) return
        uiState = uiState.copy(isWorkspaceSaving = true, errorMessage = null)
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    client.saveWorkspaceDocument(document.path, uiState.workspaceDraft)
                }
            }.onSuccess { saved ->
                uiState = uiState.copy(
                    workspaceDocument = saved,
                    workspaceDraft = saved.content,
                    isWorkspaceEditing = false,
                    isWorkspaceSaving = false,
                    noticeMessage = "文档已保存到 Hermes 工作区",
                )
            }.onFailure(::handleFailure)
        }
    }

    fun exportWorkspaceDocument(destination: Uri) {
        val document = uiState.workspaceDocument ?: return
        val resolver = getApplication<Application>().contentResolver
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    resolver.openOutputStream(destination, "w").use { output ->
                        requireNotNull(output) { "无法写入所选位置" }
                        output.write(document.bytesForTransfer())
                    }
                }
            }.onSuccess {
                uiState = uiState.copy(noticeMessage = "${document.name} 已保存到手机")
            }.onFailure(::handleFailure)
        }
    }

    fun shareWorkspaceDocument() {
        val document = uiState.workspaceDocument ?: return
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val context = getApplication<Application>()
                    val directory = File(context.cacheDir, "shared-artifacts").apply { mkdirs() }
                    val safeName = document.name.replace(Regex("[^A-Za-z0-9._\\-\\u4e00-\\u9fff]"), "_")
                        .ifBlank { "Hermes-artifact" }
                    val file = File(directory, safeName)
                    file.writeBytes(document.bytesForTransfer())
                    FileProvider.getUriForFile(context, "${context.packageName}.files", file)
                }
            }.onSuccess { uri ->
                val context = getApplication<Application>()
                val intent = Intent(Intent.ACTION_SEND)
                    .setType(document.mimeType.ifBlank { "application/octet-stream" })
                    .putExtra(Intent.EXTRA_STREAM, uri)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(Intent.createChooser(intent, "分享 ${document.name}").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }.onFailure(::handleFailure)
        }
    }

    fun showTasks() {
        uiState = uiState.copy(route = AppRoute.TASKS, errorMessage = null, noticeMessage = null)
        refreshTasks()
    }

    fun refreshTasks() {
        refreshCronJobs()
        refreshSessions()
    }

    fun openCronJob(job: CronJob) {
        uiState = uiState.copy(
            route = AppRoute.CRON_DETAIL,
            selectedCronJob = job,
            errorMessage = null,
            noticeMessage = null,
        )
    }

    fun closeCronJob() {
        uiState = uiState.copy(route = AppRoute.TASKS, selectedCronJob = null, errorMessage = null)
    }

    fun refreshCronJobs() {
        val client = apiClient ?: return
        if (uiState.isCronLoading) return
        uiState = uiState.copy(isCronLoading = true, errorMessage = null)
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) { client.listCronJobs() } }
                .onSuccess { jobs ->
                    uiState = uiState.copy(cronJobs = jobs, isCronLoading = false)
                    configStore.saveCronSnapshot(jobs.associate { it.id to "${it.lastRunAt}|${it.lastStatus}" })
                }
                .onFailure(::handleFailure)
        }
    }

    fun createCronJob(name: String, prompt: String, schedule: String) {
        val client = apiClient ?: return
        if (name.isBlank() || prompt.isBlank() || schedule.isBlank()) {
            showError("请填写任务名称、执行内容和时间计划")
            return
        }
        uiState = uiState.copy(isCronLoading = true, errorMessage = null)
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) { client.createCronJob(name, prompt, schedule) } }
                .onSuccess { job ->
                    uiState = uiState.copy(
                        cronJobs = (uiState.cronJobs + job).distinctBy(CronJob::id),
                        isCronLoading = false,
                        noticeMessage = "定时任务已创建",
                    )
                    HermesNotifications.scheduleCronPolling(
                        getApplication(),
                        uiState.notificationPreferences.enabled && uiState.notificationPreferences.taskAlerts,
                    )
                }
                .onFailure(::handleFailure)
        }
    }

    fun updateCronJob(job: CronJob, name: String, prompt: String, schedule: String) {
        val client = apiClient ?: return
        if (name.isBlank() || prompt.isBlank() || schedule.isBlank()) {
            showError("请填写任务名称、执行内容和时间计划")
            return
        }
        if (uiState.cronActionId != null) return
        uiState = uiState.copy(cronActionId = job.id, errorMessage = null)
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    client.updateCronJob(job.id, name, prompt, schedule)
                }
            }.onSuccess { updated ->
                uiState = uiState.copy(
                    cronJobs = uiState.cronJobs.map { if (it.id == job.id) updated else it },
                    selectedCronJob = uiState.selectedCronJob?.let { if (it.id == job.id) updated else it },
                    cronActionId = null,
                    noticeMessage = "定时任务已更新",
                )
            }.onFailure(::handleFailure)
        }
    }

    fun toggleCronJob(job: CronJob) {
        val client = apiClient ?: return
        if (uiState.cronActionId != null) return
        uiState = uiState.copy(cronActionId = job.id, errorMessage = null)
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    if (job.enabled) client.pauseCronJob(job.id) else client.resumeCronJob(job.id)
                }
            }.onSuccess {
                uiState = uiState.copy(
                    cronJobs = uiState.cronJobs.map { if (it.id == job.id) it.copy(enabled = !job.enabled) else it },
                    selectedCronJob = uiState.selectedCronJob?.let {
                        if (it.id == job.id) it.copy(enabled = !job.enabled) else it
                    },
                    cronActionId = null,
                    noticeMessage = if (job.enabled) "定时任务已暂停" else "定时任务已恢复",
                )
            }.onFailure(::handleFailure)
        }
    }

    fun triggerCronJob(job: CronJob) {
        val client = apiClient ?: return
        if (uiState.cronActionId != null) return
        uiState = uiState.copy(cronActionId = job.id, errorMessage = null)
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) { client.triggerCronJob(job.id) } }
                .onSuccess {
                    uiState = uiState.copy(cronActionId = null, noticeMessage = "已开始执行“${job.name}”")
                    delay(1_000)
                    refreshCronJobs()
                }
                .onFailure(::handleFailure)
        }
    }

    fun deleteCronJob(job: CronJob) {
        val client = apiClient ?: return
        if (uiState.cronActionId != null) return
        uiState = uiState.copy(cronActionId = job.id, errorMessage = null)
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) { client.deleteCronJob(job.id) } }
                .onSuccess {
                    uiState = uiState.copy(
                        cronJobs = uiState.cronJobs.filterNot { it.id == job.id },
                        route = if (uiState.route == AppRoute.CRON_DETAIL) AppRoute.TASKS else uiState.route,
                        selectedCronJob = null,
                        cronActionId = null,
                        noticeMessage = "定时任务已删除",
                    )
                }
                .onFailure(::handleFailure)
        }
    }

    fun showProfile() {
        uiState = uiState.copy(route = AppRoute.PROFILE, errorMessage = null, noticeMessage = null)
    }

    fun showSettings() {
        uiState = uiState.copy(route = AppRoute.SETTINGS, errorMessage = null, noticeMessage = null)
    }

    fun updateUserProfile(value: UserProfilePreferences) {
        val safeProfile = avatarStorage.sanitize(value)
        configStore.saveUserProfile(safeProfile)
        uiState = uiState.copy(userProfile = safeProfile, noticeMessage = "个人资料已保存")
    }

    fun updateUserAvatar(source: Uri, crop: AvatarCropSpec) {
        replaceAvatar(source, AvatarTarget.USER, crop)
    }

    fun updateHermesAvatar(source: Uri, crop: AvatarCropSpec) {
        replaceAvatar(source, AvatarTarget.HERMES, crop)
    }

    fun resetUserAvatar() {
        resetAvatar(AvatarTarget.USER)
    }

    fun resetHermesAvatar() {
        resetAvatar(AvatarTarget.HERMES)
    }

    fun showProfileSettings() {
        uiState = uiState.copy(route = AppRoute.PROFILE_SETTINGS, errorMessage = null, noticeMessage = null)
    }

    private fun replaceAvatar(source: Uri, target: AvatarTarget, crop: AvatarCropSpec) {
        if (uiState.isAvatarUpdating) return
        uiState = uiState.copy(isAvatarUpdating = true, errorMessage = null, noticeMessage = null)
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) { avatarStorage.save(source, target, crop) } }
                .onSuccess { privateUri ->
                    val profile = when (target) {
                        AvatarTarget.USER -> uiState.userProfile.copy(avatarUri = privateUri)
                        AvatarTarget.HERMES -> uiState.userProfile.copy(hermesAvatarUri = privateUri)
                    }
                    configStore.saveUserProfile(profile)
                    uiState = uiState.copy(
                        userProfile = profile,
                        isAvatarUpdating = false,
                        noticeMessage = if (target == AvatarTarget.USER) "我的头像已更新" else "Hermes 头像已更新",
                    )
                }
                .onFailure { throwable ->
                    val message = when (unwrapFailure(throwable)) {
                        is SecurityException -> "照片读取授权已失效，请重新选择图片"
                        is OutOfMemoryError -> "图片尺寸过大，请选择较小的图片"
                        else -> throwable.message?.takeIf(String::isNotBlank) ?: "头像保存失败，请重新选择"
                    }
                    uiState = uiState.copy(isAvatarUpdating = false, errorMessage = message)
                }
        }
    }

    private fun resetAvatar(target: AvatarTarget) {
        if (uiState.isAvatarUpdating) return
        uiState = uiState.copy(isAvatarUpdating = true, errorMessage = null, noticeMessage = null)
        viewModelScope.launch {
            withContext(Dispatchers.IO) { avatarStorage.delete(target) }
            val profile = when (target) {
                AvatarTarget.USER -> uiState.userProfile.copy(avatarUri = "")
                AvatarTarget.HERMES -> uiState.userProfile.copy(hermesAvatarUri = "")
            }
            configStore.saveUserProfile(profile)
            uiState = uiState.copy(
                userProfile = profile,
                isAvatarUpdating = false,
                noticeMessage = if (target == AvatarTarget.USER) "已恢复默认用户头像" else "已恢复默认 Hermes 头像",
            )
        }
    }

    fun showSkillsAndTools() {
        val client = apiClient ?: return
        uiState = uiState.copy(
            route = AppRoute.SKILLS_TOOLS,
            isAdvancedSettingsLoading = true,
            errorMessage = null,
            noticeMessage = null,
        )
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    Triple(client.listSkills(), client.listToolsets(), client.listMcpServers())
                }
            }.onSuccess { (skills, tools, mcp) ->
                uiState = uiState.copy(
                    serverSkills = skills,
                    toolsets = tools,
                    mcpServers = mcp,
                    isAdvancedSettingsLoading = false,
                )
            }.onFailure(::handleFailure)
        }
    }

    fun toggleSkill(skill: ServerSkill) {
        val client = apiClient ?: return
        if (uiState.settingsActionKey != null) return
        uiState = uiState.copy(settingsActionKey = "skill:${skill.name}", errorMessage = null)
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) { client.setSkillEnabled(skill.name, !skill.enabled) } }
                .onSuccess {
                    uiState = uiState.copy(
                        serverSkills = uiState.serverSkills.map {
                            if (it.name == skill.name) it.copy(enabled = !skill.enabled) else it
                        },
                        selectedSkill = uiState.selectedSkill?.let {
                            if (it.name == skill.name) it.copy(enabled = !skill.enabled) else it
                        },
                        settingsActionKey = null,
                        noticeMessage = "技能设置已保存，下次会话生效",
                    )
                }.onFailure(::handleFailure)
        }
    }

    fun openSkill(skill: ServerSkill) {
        val client = apiClient ?: return
        uiState = uiState.copy(selectedSkill = skill, selectedSkillContent = "", settingsActionKey = "skill-content:${skill.name}")
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) { client.skillContent(skill.name) } }
                .onSuccess { content ->
                    uiState = uiState.copy(selectedSkillContent = content, settingsActionKey = null)
                }.onFailure(::handleFailure)
        }
    }

    fun closeSkill() {
        uiState = uiState.copy(selectedSkill = null, selectedSkillContent = "", settingsActionKey = null)
    }

    fun toggleToolset(toolset: ToolsetInfo) {
        val client = apiClient ?: return
        if (uiState.settingsActionKey != null) return
        uiState = uiState.copy(settingsActionKey = "toolset:${toolset.name}", errorMessage = null)
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) { client.setToolsetEnabled(toolset.name, !toolset.enabled) } }
                .onSuccess {
                    uiState = uiState.copy(
                        toolsets = uiState.toolsets.map {
                            if (it.name == toolset.name) it.copy(enabled = !toolset.enabled) else it
                        },
                        serverSettings = withContext(Dispatchers.IO) { client.serverSettings() },
                        settingsActionKey = null,
                        noticeMessage = "工具集设置已保存，下次会话生效",
                    )
                }.onFailure(::handleFailure)
        }
    }

    fun toggleMcpServer(server: McpServerInfo) {
        val client = apiClient ?: return
        if (uiState.settingsActionKey != null) return
        uiState = uiState.copy(settingsActionKey = "mcp:${server.name}", errorMessage = null)
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) { client.setMcpServerEnabled(server.name, !server.enabled) } }
                .onSuccess {
                    uiState = uiState.copy(
                        mcpServers = uiState.mcpServers.map {
                            if (it.name == server.name) it.copy(enabled = !server.enabled) else it
                        },
                        settingsActionKey = null,
                        noticeMessage = "MCP 设置已保存",
                    )
                }.onFailure(::handleFailure)
        }
    }

    fun showModelSettings() = loadServerSettings(AppRoute.MODEL_SETTINGS, loadModels = true)

    fun saveModelSettings(value: ServerModelSettings) {
        saveServerSettings("模型设置已保存，新会话将使用新的模型配置") { it.saveModelSettings(value) }
    }

    fun addCustomProvider(id: String, name: String, baseUrl: String, model: String, apiKey: String) {
        val client = apiClient ?: return
        if (id.isBlank() || baseUrl.isBlank() || model.isBlank()) {
            showError("请填写提供商标识、接口地址和默认模型")
            return
        }
        uiState = uiState.copy(isAdvancedSettingsLoading = true, errorMessage = null)
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val settings = client.addCustomProvider(id, name, baseUrl, model, apiKey)
                    settings to client.modelCatalog()
                }
            }.onSuccess { (settings, catalog) ->
                uiState = uiState.copy(
                    serverSettings = settings,
                    modelCatalog = catalog,
                    isAdvancedSettingsLoading = false,
                    noticeMessage = "模型提供商已添加",
                )
            }.onFailure(::handleFailure)
        }
    }

    fun showConversationStyleSettings() = loadServerSettings(AppRoute.CONVERSATION_STYLE)

    fun saveConversationStyle(value: ConversationStyleSettings) {
        saveServerSettings("对话风格已保存") { it.saveConversationStyle(value) }
    }

    fun showApprovalSettings() = loadServerSettings(AppRoute.APPROVAL_SETTINGS)

    fun saveApprovalSettings(value: ApprovalSettings) {
        saveServerSettings("审批模式已保存") { it.saveApprovalSettings(value) }
    }

    fun showMemoryContextSettings() = loadServerSettings(AppRoute.MEMORY_CONTEXT)

    fun saveMemorySettings(value: MemoryContextSettings) {
        saveServerSettings("记忆与上下文设置已保存") { it.saveMemorySettings(value) }
    }

    fun showArchivedSessions() {
        val client = apiClient ?: return
        uiState = uiState.copy(
            route = AppRoute.ARCHIVED_SESSIONS,
            isAdvancedSettingsLoading = true,
            errorMessage = null,
            noticeMessage = null,
        )
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) { client.listArchivedSessions() } }
                .onSuccess { sessions ->
                    uiState = uiState.copy(archivedSessions = sessions, isAdvancedSettingsLoading = false)
                }.onFailure(::handleFailure)
        }
    }

    fun restoreArchivedSession(session: HermesSession) {
        val client = apiClient ?: return
        if (uiState.settingsActionKey != null) return
        uiState = uiState.copy(settingsActionKey = "restore:${session.id}", errorMessage = null)
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) { client.restoreSession(session.id) } }
                .onSuccess {
                    uiState = uiState.copy(
                        archivedSessions = uiState.archivedSessions.filterNot { it.id == session.id },
                        settingsActionKey = null,
                        noticeMessage = "会话已恢复",
                    )
                    refreshSessions()
                }.onFailure(::handleFailure)
        }
    }

    fun deleteArchivedSession(session: HermesSession) {
        val client = apiClient ?: return
        if (uiState.settingsActionKey != null) return
        uiState = uiState.copy(settingsActionKey = "delete:${session.id}", errorMessage = null)
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) { client.deleteSession(session.id) } }
                .onSuccess {
                    uiState = uiState.copy(
                        archivedSessions = uiState.archivedSessions.filterNot { it.id == session.id },
                        settingsActionKey = null,
                        noticeMessage = "归档会话已删除",
                    )
                }.onFailure(::handleFailure)
        }
    }

    private fun loadServerSettings(route: AppRoute, loadModels: Boolean = false) {
        val client = apiClient ?: return
        uiState = uiState.copy(
            route = route,
            isAdvancedSettingsLoading = true,
            errorMessage = null,
            noticeMessage = null,
        )
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    client.serverSettings() to if (loadModels) client.modelCatalog() else uiState.modelCatalog
                }
            }.onSuccess { (settings, catalog) ->
                uiState = uiState.copy(
                    serverSettings = settings,
                    modelCatalog = catalog,
                    isAdvancedSettingsLoading = false,
                )
            }.onFailure(::handleFailure)
        }
    }

    private fun saveServerSettings(
        notice: String,
        save: (HermesApiClient) -> ServerSettings,
    ) {
        val client = apiClient ?: return
        if (uiState.isAdvancedSettingsLoading) return
        uiState = uiState.copy(isAdvancedSettingsLoading = true, errorMessage = null)
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) { save(client) } }
                .onSuccess { settings ->
                    uiState = uiState.copy(
                        serverSettings = settings,
                        isAdvancedSettingsLoading = false,
                        noticeMessage = notice,
                    )
                }.onFailure(::handleFailure)
        }
    }

    fun showNotificationSettings() {
        uiState = uiState.copy(route = AppRoute.NOTIFICATIONS, errorMessage = null, noticeMessage = null)
    }

    fun updateNotificationPreferences(value: NotificationPreferences) {
        configStore.saveNotificationPreferences(value)
        HermesNotifications.applyPreferences(getApplication(), value)
        HermesNotifications.scheduleCronPolling(getApplication(), value.enabled && value.taskAlerts)
        uiState = uiState.copy(notificationPreferences = value)
    }

    fun sendTestNotification() {
        HermesNotifications.showMessage(getApplication(), "Hermes 通知测试", "系统通知、提示音与角标已经可以正常工作。")
        showNotice("测试通知已发送；如果没有出现，请检查系统通知权限")
    }

    fun showVoiceSettings() = loadServerSettings(AppRoute.VOICE_SETTINGS)

    fun updateVoicePreferences(value: VoicePreferences) {
        configStore.saveVoicePreferences(value)
        uiState = uiState.copy(voicePreferences = value)
    }

    fun saveVoiceSettings(value: ServerVoiceSettings) {
        saveServerSettings("语音模型设置已保存") { it.saveVoiceSettings(value) }
    }

    fun acceptVoiceResult(text: String) {
        val transcript = normalizeVoiceTranscript(text, uiState.voicePreferences.transcriptScript).trim()
        if (transcript.isBlank()) return
        uiState = uiState.copy(
            voiceCapture = uiState.voiceCapture.copy(
                phase = VoicePhase.IDLE,
                target = VoiceCaptureTarget.CHAT_INPUT,
                transcript = transcript,
                message = "已识别到输入框",
                requiresAgentUpdate = false,
            ),
        )
        updateDraft(listOf(uiState.draft, transcript).filter(String::isNotBlank).joinToString(" "))
        if (uiState.voicePreferences.autoSend) sendMessage()
    }

    fun startSingleVoiceInput() = startVoiceCapture(VoiceCaptureTarget.CHAT_INPUT)

    fun startVoiceSettingsTest() = startVoiceCapture(VoiceCaptureTarget.SETTINGS_TEST)

    private fun startVoiceCapture(target: VoiceCaptureTarget) {
        if (!uiState.voicePreferences.enabled) return showNotice("请先启用语音功能")
        if (target == VoiceCaptureTarget.CHAT_INPUT && uiState.isStreaming) {
            return showNotice("请等待 Hermes 完成当前回复后再录音")
        }
        voicePlaybackJob?.cancel()
        voiceCaptureJob?.cancel()
        voicePlayback.stop()
        runCatching { voiceRecorder.start() }
            .onSuccess {
                uiState = uiState.copy(
                    voiceCapture = uiState.voiceCapture.copy(
                        phase = VoicePhase.LISTENING,
                        target = target,
                        transcript = "",
                        provider = "",
                        message = if (target == VoiceCaptureTarget.CHAT_INPUT) "正在录音，再点麦克风结束" else "请说一句中文测试语音",
                        requiresAgentUpdate = false,
                    ),
                    errorMessage = null,
                )
            }
            .onFailure { throwable ->
                uiState = uiState.copy(
                    voiceCapture = uiState.voiceCapture.copy(
                        phase = VoicePhase.ERROR,
                        target = target,
                        message = throwable.message ?: "无法启动麦克风",
                    ),
                )
            }
    }

    fun cancelSingleVoiceInput() {
        voiceCaptureJob?.cancel()
        voiceCaptureJob = null
        voiceRecorder.cancel()
        uiState = uiState.copy(voiceCapture = VoiceCaptureState())
    }

    fun stopSingleVoiceInput() {
        val client = apiClient ?: return
        val capture = uiState.voiceCapture
        if (capture.phase != VoicePhase.LISTENING) return
        uiState = uiState.copy(
            voiceCapture = capture.copy(phase = VoicePhase.TRANSCRIBING, message = "正在识别语音"),
        )
        voiceCaptureJob?.cancel()
        voiceCaptureJob = viewModelScope.launch {
            runCatching {
                val (bytes, mimeType) = withContext(Dispatchers.IO) { voiceRecorder.stop() }
                if (capture.target == VoiceCaptureTarget.CHAT_INPUT && uiState.voicePreferences.engine == "system") {
                    throw IllegalStateException("当前已选择手机系统语音识别")
                }
                withContext(Dispatchers.IO) { client.transcribeAudio(bytes, mimeType) }
            }.onSuccess { result ->
                voiceCaptureJob = null
                val target = capture.target
                val transcript = normalizeVoiceTranscript(result.transcript, uiState.voicePreferences.transcriptScript).trim()
                uiState = uiState.copy(
                    voiceCapture = uiState.voiceCapture.copy(
                        phase = VoicePhase.IDLE,
                        target = target,
                        transcript = transcript,
                        provider = result.provider,
                        message = if (target == VoiceCaptureTarget.SETTINGS_TEST) "识别成功" else "已识别到输入框",
                        agentSttAvailable = true,
                        requiresAgentUpdate = false,
                    ),
                )
                if (target == VoiceCaptureTarget.CHAT_INPUT) acceptVoiceResult(transcript)
            }.onFailure { throwable ->
                if (throwable is CancellationException) return@onFailure
                voiceCaptureJob = null
                val root = unwrapFailure(throwable)
                val unavailable = root is ApiException && root.statusCode in setOf(404, 405, 501)
                val incompatible = isAgentSttCompatibilityFailure(root)
                uiState = uiState.copy(
                    voiceCapture = uiState.voiceCapture.copy(
                        phase = VoicePhase.ERROR,
                        message = when {
                            incompatible -> agentSttCompatibilityMessage()
                            unavailable -> "Agent 语音识别不可用，可改用手机系统识别"
                            else -> diagnosticFailure(root)
                        },
                        agentSttAvailable = if (unavailable || incompatible) false else uiState.voiceCapture.agentSttAvailable,
                        requiresAgentUpdate = incompatible,
                    ),
                )
            }
        }
    }

    fun testAgentVoice() {
        val client = apiClient ?: return
        if (uiState.settingsActionKey != null) return
        voicePlaybackJob?.cancel()
        voicePlayback.stop()
        uiState = uiState.copy(settingsActionKey = "voice-tts-test", errorMessage = null)
        voicePlaybackJob = viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) { client.synthesizeSpeech("你好，我是 Hermes。语音合成测试成功。") }
            }.onSuccess { audio ->
                uiState = uiState.copy(settingsActionKey = null, noticeMessage = "正在播放 Agent 语音测试")
                voicePlayback.play(audio)
            }.onFailure { throwable ->
                uiState = uiState.copy(settingsActionKey = null)
                handleFailure(throwable)
            }
        }
    }

    fun openVoiceConversation() {
        if (!uiState.voicePreferences.enabled) return showNotice("请先在‘我的 → 语音’中启用语音功能")
        if (uiState.selectedSession == null) return showNotice("请先打开一个对话")
        voiceRecorder.cancel()
        voiceCaptureJob?.cancel()
        voiceCaptureJob = null
        voiceReturnRoute = uiState.route
        uiState = uiState.copy(
            route = AppRoute.VOICE_CHAT,
            voiceCapture = VoiceCaptureState(),
            voiceConversation = uiState.voiceConversation.copy(
                active = true,
                phase = if (uiState.isStreaming) VoicePhase.THINKING else VoicePhase.IDLE,
                message = if (uiState.isStreaming) "Hermes 正在处理当前问题" else "点按按钮开始说话",
            ),
            errorMessage = null,
        )
    }

    fun closeVoiceConversation() {
        voiceRecorder.cancel()
        voiceLevelJob?.cancel()
        voiceLevelJob = null
        voicePlaybackJob?.cancel()
        voicePlayback.stop()
        uiState = uiState.copy(
            route = voiceReturnRoute.takeIf { it == AppRoute.CHAT } ?: AppRoute.CHAT,
            voiceConversation = VoiceConversationState(),
        )
    }

    fun startVoiceListening() {
        if (uiState.isStreaming) return showNotice("请等待 Hermes 完成当前回复")
        voicePlaybackJob?.cancel()
        voicePlayback.stop()
        runCatching { voiceRecorder.start() }
            .onSuccess {
                uiState = uiState.copy(
                    voiceConversation = uiState.voiceConversation.copy(
                        active = true,
                        phase = VoicePhase.LISTENING,
                        transcript = "",
                        provider = "",
                        message = "正在聆听，再点一次即可发送",
                        requiresAgentUpdate = false,
                        inputLevel = 0f,
                    ),
                )
                voiceLevelJob?.cancel()
                voiceLevelJob = viewModelScope.launch {
                    while (uiState.voiceConversation.phase == VoicePhase.LISTENING) {
                        val level = voiceRecorder.inputLevel()
                        uiState = uiState.copy(
                            voiceConversation = uiState.voiceConversation.copy(inputLevel = level),
                        )
                        delay(72)
                    }
                }
            }
            .onFailure { throwable ->
                uiState = uiState.copy(
                    voiceConversation = uiState.voiceConversation.copy(
                        phase = VoicePhase.ERROR,
                        message = throwable.message ?: "无法启动麦克风",
                    ),
                )
            }
    }

    fun cancelVoiceListening() {
        voiceRecorder.cancel()
        voiceLevelJob?.cancel()
        voiceLevelJob = null
        uiState = uiState.copy(
            voiceConversation = uiState.voiceConversation.copy(
                phase = VoicePhase.IDLE,
                message = "已取消，点按重新说话",
                inputLevel = 0f,
            ),
        )
    }

    fun stopVoiceListening() {
        val client = apiClient ?: return
        if (uiState.voiceConversation.phase != VoicePhase.LISTENING) return
        voiceLevelJob?.cancel()
        voiceLevelJob = null
        uiState = uiState.copy(
            voiceConversation = uiState.voiceConversation.copy(
                phase = VoicePhase.TRANSCRIBING,
                message = "正在识别语音",
                inputLevel = 0f,
            ),
        )
        viewModelScope.launch {
            runCatching {
                val (bytes, mimeType) = withContext(Dispatchers.IO) { voiceRecorder.stop() }
                if (uiState.voicePreferences.engine == "system") {
                    throw IllegalStateException("请使用手机系统语音识别")
                }
                withContext(Dispatchers.IO) { client.transcribeAudio(bytes, mimeType) }
            }.onSuccess { result ->
                val transcript = normalizeVoiceTranscript(result.transcript, uiState.voicePreferences.transcriptScript).trim()
                uiState = uiState.copy(
                    voiceConversation = uiState.voiceConversation.copy(
                        phase = VoicePhase.THINKING,
                        transcript = transcript,
                        provider = result.provider,
                        message = "已发送，Hermes 正在思考",
                        agentSttAvailable = true,
                        requiresAgentUpdate = false,
                    ),
                )
                submitVoiceConversationText(transcript)
            }.onFailure { throwable ->
                val root = unwrapFailure(throwable)
                val unavailable = root is ApiException && root.statusCode in setOf(404, 405, 501)
                val incompatible = isAgentSttCompatibilityFailure(root)
                uiState = uiState.copy(
                    voiceConversation = uiState.voiceConversation.copy(
                        phase = VoicePhase.ERROR,
                        message = when {
                            incompatible -> agentSttCompatibilityMessage()
                            unavailable -> "当前 Agent 未启用语音识别，可切换为手机系统识别"
                            else -> diagnosticFailure(root)
                        },
                        agentSttAvailable = if (unavailable) false else uiState.voiceConversation.agentSttAvailable,
                        requiresAgentUpdate = incompatible,
                    ),
                )
            }
        }
    }

    fun submitVoiceConversationText(text: String) {
        val transcript = normalizeVoiceTranscript(text, uiState.voicePreferences.transcriptScript).trim()
        if (transcript.isBlank() || uiState.isStreaming) return
        uiState = uiState.copy(
            draft = transcript,
            voiceConversation = uiState.voiceConversation.copy(
                phase = VoicePhase.THINKING,
                transcript = transcript,
                message = "Hermes 正在思考",
            ),
        )
        sendMessage()
    }

    fun interruptVoicePlayback() {
        voicePlaybackJob?.cancel()
        voicePlayback.stop()
        uiState = uiState.copy(
            voiceConversation = uiState.voiceConversation.copy(phase = VoicePhase.IDLE, message = "已停止播放"),
        )
    }

    private fun speakVoiceReply(text: String) {
        if (!uiState.voiceConversation.active || text.isBlank()) return
        if (!uiState.voicePreferences.autoRead) {
            uiState = uiState.copy(
                voiceConversation = uiState.voiceConversation.copy(phase = VoicePhase.IDLE, message = "Hermes 已回复"),
            )
            return
        }
        val client = apiClient ?: return
        voicePlaybackJob?.cancel()
        voicePlaybackJob = viewModelScope.launch {
            uiState = uiState.copy(
                voiceConversation = uiState.voiceConversation.copy(phase = VoicePhase.SPEAKING, message = "Hermes 正在回答"),
            )
            val preferences = uiState.voicePreferences
            val agentResult = if (preferences.engine != "system" && uiState.voiceConversation.agentTtsAvailable != false) {
                runCatching { withContext(Dispatchers.IO) { client.synthesizeSpeech(text) } }
            } else null
            if (agentResult?.isSuccess == true) {
                val audio = agentResult.getOrThrow()
                uiState = uiState.copy(
                    voiceConversation = uiState.voiceConversation.copy(provider = audio.provider, agentTtsAvailable = true),
                )
                voicePlayback.play(audio)
            } else {
                if (agentResult?.exceptionOrNull() is ApiException) {
                    uiState = uiState.copy(
                        voiceConversation = uiState.voiceConversation.copy(agentTtsAvailable = false, provider = "Android TTS"),
                    )
                }
                voicePlayback.speakSystem(text, preferences.language, preferences.speechRate)
            }
            uiState = uiState.copy(
                voiceConversation = uiState.voiceConversation.copy(phase = VoicePhase.IDLE, message = "回答完毕，点按继续"),
            )
            if (preferences.continuous && uiState.route == AppRoute.VOICE_CHAT) {
                delay(450)
                startVoiceListening()
            }
        }
    }

    fun checkAgentUpdate(force: Boolean = true) {
        val client = apiClient ?: return
        if (uiState.isAgentUpdateChecking || uiState.agentUpdateProgress.running) return
        uiState = uiState.copy(isAgentUpdateChecking = true, errorMessage = null)
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) { client.checkAgentUpdate(force) } }
                .onSuccess { info ->
                    uiState = uiState.copy(
                        agentUpdateInfo = info,
                        gatewayInfo = uiState.gatewayInfo.copy(
                            agentVersion = info.currentVersion.ifBlank { uiState.gatewayInfo.agentVersion },
                        ),
                        isAgentUpdateChecking = false,
                        noticeMessage = if (info.updateAvailable) "发现 Hermes Agent 更新" else "当前已是最新版本",
                    )
                }
                .onFailure { throwable ->
                    uiState = uiState.copy(isAgentUpdateChecking = false)
                    val root = unwrapFailure(throwable)
                    if (root is ApiException && root.statusCode == 404) {
                        uiState = uiState.copy(
                            agentUpdateInfo = AgentUpdateInfo(message = "当前 Agent 版本尚未提供远程更新接口，请在服务器运行 hermes update"),
                            errorMessage = "当前 Agent 不支持应用内更新，请先在服务器手动升级一次",
                        )
                    } else handleFailure(throwable)
                }
        }
    }

    fun applyAgentUpdate() {
        val client = apiClient ?: return
        val info = uiState.agentUpdateInfo
        if (!info.updateAvailable || !info.canApply || uiState.isStreaming || uiState.pendingAgentRequests.isNotEmpty()) {
            return showNotice("请先完成当前任务和待处理请求，再更新 Hermes Agent")
        }
        if (agentUpdateJob?.isActive == true) return
        val previousVersion = info.currentVersion
        uiState = uiState.copy(
            agentUpdateProgress = AgentUpdateProgress(started = true, running = true, lines = "正在启动服务器更新"),
            errorMessage = null,
        )
        agentUpdateJob = viewModelScope.launch {
            val started = runCatching { withContext(Dispatchers.IO) { client.startAgentUpdate() } }
            if (started.isFailure) {
                uiState = uiState.copy(agentUpdateProgress = AgentUpdateProgress())
                handleFailure(started.exceptionOrNull()!!)
                return@launch
            }
            uiState = uiState.copy(agentUpdateProgress = started.getOrThrow(), noticeMessage = "更新已启动，Gateway 可能短暂离线")
            repeat(60) {
                delay(5_000)
                val status = runCatching { withContext(Dispatchers.IO) { client.agentUpdateStatus() } }.getOrNull()
                if (status != null) {
                    uiState = uiState.copy(agentUpdateProgress = status)
                    if (!status.running && status.exitCode != null && status.exitCode != 0) {
                        uiState = uiState.copy(errorMessage = "Hermes 更新失败（退出码 ${status.exitCode}）")
                        return@launch
                    }
                }
                val refreshed = runCatching {
                    withContext(Dispatchers.IO) {
                        runCatching { client.reconnectGateway() }
                        client.checkAgentUpdate(force = true)
                    }
                }.getOrNull()
                if (refreshed != null) {
                    val versionChanged = refreshed.currentVersion.isNotBlank() && refreshed.currentVersion != previousVersion
                    if (versionChanged || !refreshed.updateAvailable) {
                        uiState = uiState.copy(
                            agentUpdateInfo = refreshed,
                            agentUpdateProgress = AgentUpdateProgress(started = true, running = false, exitCode = 0, lines = status?.lines.orEmpty()),
                            gatewayInfo = uiState.gatewayInfo.copy(agentVersion = refreshed.currentVersion),
                            noticeMessage = "Hermes Agent 已更新并重新连接",
                        )
                        loadGatewayInfo(client)
                        return@launch
                    }
                }
            }
            uiState = uiState.copy(
                agentUpdateProgress = uiState.agentUpdateProgress.copy(running = false),
                noticeMessage = "服务器仍在更新或重启，请稍后重新检查版本",
            )
        }
    }

    fun showAbout() {
        uiState = uiState.copy(route = AppRoute.ABOUT, errorMessage = null, noticeMessage = null)
    }

    fun showChangeLog() {
        uiState = uiState.copy(route = AppRoute.CHANGELOG, errorMessage = null, noticeMessage = null)
    }

    fun closeSettingsPage() {
        if (uiState.route == AppRoute.VOICE_SETTINGS) {
            voiceRecorder.cancel()
            voiceCaptureJob?.cancel()
            voiceCaptureJob = null
            voicePlaybackJob?.cancel()
            voicePlayback.stop()
        }
        uiState = uiState.copy(
            route = AppRoute.SETTINGS,
            voiceCapture = VoiceCaptureState(),
            errorMessage = null,
            noticeMessage = null,
        )
    }

    fun openConnectionSettings() {
        if (uiState.isStreaming) stopGeneration()
        settingsReturnRoute = uiState.route.takeIf {
            it in setOf(AppRoute.SESSIONS, AppRoute.WORKSPACE, AppRoute.TASKS, AppRoute.PROFILE, AppRoute.SETTINGS, AppRoute.VOICE_CHAT)
        }
            ?: AppRoute.PROFILE
        uiState = uiState.copy(route = AppRoute.SETUP, errorMessage = null, noticeMessage = null)
        if (uiState.agentUpdateInfo.currentVersion.isBlank()) checkAgentUpdate(force = false)
    }

    fun closeConnectionSettings() {
        uiState = uiState.copy(
            route = settingsReturnRoute,
            connectionDiagnostics = emptyList(),
            isConnectionDiagnosing = false,
            errorMessage = null,
            noticeMessage = null,
        )
    }

    fun diagnoseConnection() {
        val client = apiClient ?: return showNotice("请先完成一次登录，再诊断已保存的连接")
        if (uiState.isConnectionDiagnosing) return
        val items = listOf(
            ConnectionDiagnosticItem("gateway", "网关接口", "正在访问 ${uiState.baseUrl}", DiagnosticStatus.CHECKING),
            ConnectionDiagnosticItem("version", "版本与兼容性", "正在读取 Agent 与网关版本", DiagnosticStatus.CHECKING),
            ConnectionDiagnosticItem("auth", "登录状态", "正在验证加密保存的登录会话", DiagnosticStatus.CHECKING),
            ConnectionDiagnosticItem("realtime", "实时连接", "正在检查 WebSocket 流式通道", DiagnosticStatus.CHECKING),
            ConnectionDiagnosticItem("capabilities", "功能接口", "正在检查 Profile 与会话接口", DiagnosticStatus.CHECKING),
        )
        uiState = uiState.copy(
            connectionDiagnostics = items,
            isConnectionDiagnosing = true,
            errorMessage = null,
            noticeMessage = null,
        )
        viewModelScope.launch {
            val gatewayStart = System.currentTimeMillis()
            val gateway = runCatching { withContext(Dispatchers.IO) { client.gatewayInfo() } }
            if (gateway.isFailure) {
                updateDiagnostic("gateway", "无法访问：${diagnosticFailure(gateway.exceptionOrNull())}", DiagnosticStatus.FAILED)
                listOf("version", "auth", "realtime", "capabilities").forEach { key ->
                    updateDiagnostic(key, "网关不可用，已跳过", DiagnosticStatus.WARNING)
                }
                uiState = uiState.copy(isConnectionDiagnosing = false)
                return@launch
            }
            val info = gateway.getOrThrow()
            uiState = uiState.copy(gatewayInfo = info)
            val latency = System.currentTimeMillis() - gatewayStart
            val insecure = uiState.baseUrl.startsWith("http://", ignoreCase = true)
            updateDiagnostic(
                "gateway",
                "接口响应 ${latency}ms${if (insecure) "；当前为未加密 HTTP" else "；HTTPS 正常"}",
                if (insecure) DiagnosticStatus.WARNING else DiagnosticStatus.PASSED,
            )
            val versionText = buildList {
                info.agentVersion.takeIf(String::isNotBlank)?.let { add("Agent $it") }
                info.gatewayVersion.takeIf(String::isNotBlank)?.let { add("Gateway $it") }
            }.joinToString(" · ")
            updateDiagnostic(
                "version",
                versionText.ifBlank { "网关未公布版本号；已改用接口探测判断兼容性" },
                if (versionText.isBlank()) DiagnosticStatus.WARNING else DiagnosticStatus.PASSED,
            )

            runCatching { withContext(Dispatchers.IO) { client.checkSavedSession() } }
                .onSuccess { user -> updateDiagnostic("auth", "登录有效：$user", DiagnosticStatus.PASSED) }
                .onFailure { updateDiagnostic("auth", diagnosticFailure(it), DiagnosticStatus.FAILED) }

            runCatching { withContext(Dispatchers.IO) { client.reconnectGateway() } }
                .onSuccess { updateDiagnostic("realtime", "WebSocket 已连接，可接收流式回复", DiagnosticStatus.PASSED) }
                .onFailure { updateDiagnostic("realtime", diagnosticFailure(it), DiagnosticStatus.FAILED) }

            runCatching {
                withContext(Dispatchers.IO) {
                    val profileCount = client.listProfiles().size
                    val sessionCount = client.listSessions().totalCount
                    profileCount to sessionCount
                }
            }.onSuccess { (profileCount, sessionCount) ->
                val advertised = info.capabilities.takeIf(List<String>::isNotEmpty)
                    ?.let { " · 服务端公布 ${it.size} 项能力" }
                    .orEmpty()
                updateDiagnostic(
                    "capabilities",
                    "$profileCount 个 Profile · $sessionCount 个会话，核心接口正常$advertised",
                    DiagnosticStatus.PASSED,
                )
            }.onFailure { updateDiagnostic("capabilities", diagnosticFailure(it), DiagnosticStatus.FAILED) }
            uiState = uiState.copy(isConnectionDiagnosing = false)
        }
    }

    fun disconnect() {
        if (uiState.isStreaming) stopGeneration()
        agentUpdateJob?.cancel()
        voicePlaybackJob?.cancel()
        voiceCaptureJob?.cancel()
        voiceLevelJob?.cancel()
        voiceRecorder.cancel()
        voicePlayback.stop()
        apiClient?.close()
        cookieJar.clear()
        configStore.clear()
        indexedArtifactProfiles.clear()
        commandCatalogCache.clear()
        messageCache.clear()
        oldestMessageOffsets.clear()
        apiClient = null
        uiState = AppUiState(
            route = AppRoute.SETUP,
            themeMode = uiState.themeMode,
            skinMode = uiState.skinMode,
        )
    }

    fun setThemeMode(mode: ThemeMode) {
        configStore.saveThemeMode(mode.name)
        uiState = uiState.copy(themeMode = mode)
    }

    fun setSkinMode(mode: SkinMode) {
        configStore.saveSkinMode(mode.name)
        uiState = uiState.copy(skinMode = mode)
    }

    fun clearTransientMessage() {
        uiState = uiState.copy(errorMessage = null, noticeMessage = null)
    }

    fun showVoiceRecognitionUnavailable() {
        showError("此手机没有系统语音识别服务；请在语音设置选择 Agent 自动识别，或安装并启用系统语音助手")
    }

    fun showNotice(message: String) {
        uiState = uiState.copy(noticeMessage = message, errorMessage = null)
    }

    private fun handleStreamEvent(event: StreamEvent) {
        when (event) {
            is StreamEvent.RunStarted -> uiState = uiState.copy(
                runStage = "Hermes 正在执行",
                runLastActivityAtMillis = System.currentTimeMillis(),
            )
            is StreamEvent.AssistantDelta -> {
                enqueueStreamingDelta(event.text)
            }
            is StreamEvent.AssistantCompleted -> {
                flushStreamingDelta()
                if (event.content.isNotBlank()) updateStreamingMessage { event.content }
                uiState = uiState.copy(runLastActivityAtMillis = System.currentTimeMillis())
            }
            is StreamEvent.ToolStarted -> {
                flushStreamingDelta()
                val toolName = councilToolName(event.name)
                uiState = uiState.copy(
                    runStage = "正在使用 $toolName",
                    runLastActivityAtMillis = System.currentTimeMillis(),
                )
                updateTool(toolName, event.preview, ToolStatus.RUNNING, event.todos)
            }
            is StreamEvent.ToolProgress -> {
                val toolName = councilToolName(event.name)
                uiState = uiState.copy(
                    runStage = event.preview.ifBlank { "正在使用 $toolName" }.take(80),
                    runLastActivityAtMillis = System.currentTimeMillis(),
                )
                updateTool(toolName, event.preview, ToolStatus.RUNNING)
            }
            is StreamEvent.ToolCompleted -> {
                flushStreamingDelta()
                val toolName = councilToolName(event.name)
                uiState = uiState.copy(
                    runStage = "$toolName 已完成",
                    runLastActivityAtMillis = System.currentTimeMillis(),
                )
                updateTool(toolName, event.preview, ToolStatus.COMPLETED, event.todos)
            }
            is StreamEvent.ToolFailed -> {
                uiState = uiState.copy(
                    runStage = "${councilToolName(event.name)} 执行失败",
                    runLastActivityAtMillis = System.currentTimeMillis(),
                )
                updateTool(councilToolName(event.name), event.preview, ToolStatus.FAILED)
            }
            is StreamEvent.AgentRequestPending -> {
                val request = event.request.copy(conversationId = activeStreamSession?.id.orEmpty())
                uiState = uiState.copy(
                    pendingAgentRequests = (uiState.pendingAgentRequests.filterNot { it.requestId == request.requestId } + request),
                    runStage = "等待你的处理",
                    runLastActivityAtMillis = System.currentTimeMillis(),
                )
                configStore.savePendingAgentRequests(uiState.pendingAgentRequests)
                val action = if (request.type == AgentRequestType.APPROVAL) "需要确认一项操作" else "需要你补充信息"
                HermesNotifications.showAgentRequest(
                    getApplication(),
                    "Hermes $action",
                    request.title,
                    profile = activeStreamSession?.profile,
                    sessionId = activeStreamSession?.id,
                )
            }
            is StreamEvent.AgentRequestExpired -> {
                uiState = uiState.copy(
                    pendingAgentRequests = uiState.pendingAgentRequests.filterNot { it.requestId == event.requestId },
                    runStage = "请求已过期，Hermes 正在继续",
                    runLastActivityAtMillis = System.currentTimeMillis(),
                )
                configStore.savePendingAgentRequests(uiState.pendingAgentRequests)
            }
            is StreamEvent.ConnectionInterrupted -> {
                flushStreamingDelta()
                recoverInterruptedStream(event.message)
            }
            is StreamEvent.Error -> {
                flushStreamingDelta()
                handleStreamFailure(IllegalStateException(event.message))
            }
            StreamEvent.Completed -> {
                flushStreamingDelta()
                finishStreaming()
            }
        }
    }

    private fun enqueueStreamingDelta(text: String) {
        if (text.isEmpty()) return
        streamingDeltaBuffer.append(text)
        if (streamingDeltaFlushJob?.isActive == true) return
        streamingDeltaFlushJob = viewModelScope.launch {
            delay(STREAM_DELTA_FRAME_MILLIS)
            streamingDeltaFlushJob = null
            flushStreamingDelta()
        }
    }

    private fun flushStreamingDelta() {
        if (streamingDeltaBuffer.isEmpty()) return
        val text = streamingDeltaBuffer.toString()
        streamingDeltaBuffer.clear()
        uiState = uiState.copy(
            runStage = "正在组织回复",
            runLastActivityAtMillis = System.currentTimeMillis(),
        )
        updateStreamingMessage { current -> current + text }
    }

    private fun updateStreamingMessage(transform: (String) -> String) {
        val updated = activeStreamMessages().map { message ->
            if (message.isStreaming) message.copy(content = transform(message.content)) else message
        }
        setActiveStreamMessages(updated)
    }

    private fun updateTool(
        name: String,
        preview: String,
        status: ToolStatus,
        todos: List<ChatTodo> = emptyList(),
    ) {
        val existing = activeStreamToolActivities.indexOfLast { it.name == name && it.status == ToolStatus.RUNNING }
        val updated = activeStreamToolActivities.toMutableList()
        if (existing >= 0) {
            updated[existing] = updated[existing].copy(
                preview = preview.ifBlank { updated[existing].preview },
                status = status,
            )
        } else {
            updated += ToolActivity(name = name, preview = preview, status = status)
        }
        val newArtifacts = ChatInsightParser.artifactsFromText(preview)
        activeStreamToolActivities = updated
        if (todos.isNotEmpty()) activeStreamTodos = todos
        activeStreamArtifacts = (activeStreamArtifacts + newArtifacts).distinctBy(ChatArtifact::path)
        if (uiState.selectedSession?.id == uiState.streamingSessionId) {
            uiState = uiState.copy(
                toolActivities = activeStreamToolActivities,
                chatTodos = activeStreamTodos,
                chatArtifacts = activeStreamArtifacts,
            )
        }
    }

    private fun finishStreaming() {
        val sessionId = uiState.streamingSessionId ?: activeStreamSession?.id
        val session = activeStreamSession ?: uiState.sessions.firstOrNull { it.id == sessionId }
        val completedMessages = activeStreamMessages().mapNotNull { message ->
            if (!message.isStreaming) return@mapNotNull message
            message.takeIf { it.content.isNotBlank() || it.images.isNotEmpty() }
                ?.copy(isStreaming = false)
        }
        if (sessionId != null) cacheMessages(sessionId, completedMessages)
        val completedMessage = completedMessages.lastOrNull { it.role == MessageRole.ASSISTANT }
        if (session != null) rememberRecentArtifacts(session, completedMessages, activeStreamArtifacts)
        val completedText = completedMessage?.content.orEmpty().replace(Regex("\\s+"), " ").trim()
        val hasReply = completedText.isNotBlank() || completedMessage?.images?.isNotEmpty() == true
        val needsAttention = sessionId != null && hasReply && replyNeedsAttention(
            route = uiState.route,
            selectedSessionId = uiState.selectedSession?.id,
            completedSessionId = sessionId,
            appInForeground = isAppInForeground(),
        )
        val unreadKey = session?.scopedId ?: "${uiState.activeProfile}::$sessionId"
        val unreadSessionIds = if (needsAttention) uiState.unreadSessionIds + unreadKey else uiState.unreadSessionIds
        val queued = uiState.queuedRunMessage?.takeIf { it.session.id == sessionId }
        val completion = sessionId?.let {
            RunCompletionSummary(
                sessionId = it,
                title = session?.title?.ifBlank { "Hermes 已完成" } ?: "Hermes 已完成",
                summary = completedText.take(240).ifBlank {
                    if (activeStreamArtifacts.isNotEmpty()) "已生成 ${activeStreamArtifacts.size} 个产物" else "本轮执行已完成"
                },
                artifacts = activeStreamArtifacts,
            )
        }
        val recentCompletions = completion?.let { item ->
            (listOf(item) + uiState.recentCompletions.filterNot {
                it.sessionId == item.sessionId && it.completedAtMillis == item.completedAtMillis
            }).take(29)
        } ?: uiState.recentCompletions
        if (needsAttention) configStore.saveUnreadSessionIds(unreadSessionIds)
        uiState = uiState.copy(
            isStreaming = false,
            streamingSessionId = null,
            runStage = "",
            activeCouncilMode = CouncilMode.OFF,
            isSteering = false,
            queuedRunMessage = if (queued != null) null else uiState.queuedRunMessage,
            pendingAgentRequests = uiState.pendingAgentRequests.filterNot { it.conversationId == sessionId },
            latestCompletion = completion ?: uiState.latestCompletion,
            recentCompletions = recentCompletions,
            isRecoveringConnection = false,
            messages = if (uiState.selectedSession?.id == sessionId) completedMessages else uiState.messages,
            toolActivities = if (uiState.selectedSession?.id == sessionId) activeStreamToolActivities else uiState.toolActivities,
            chatArtifacts = if (uiState.selectedSession?.id == sessionId) activeStreamArtifacts else uiState.chatArtifacts,
            chatTodos = if (uiState.selectedSession?.id == sessionId) activeStreamTodos else uiState.chatTodos,
            unreadSessionIds = unreadSessionIds,
            sessions = uiState.sessions.map { item ->
                if (item.id == sessionId && hasReply) {
                    item.copy(preview = completedText.ifBlank { "Hermes 已发送图片" })
                } else item
            },
        )
        configStore.savePendingAgentRequests(uiState.pendingAgentRequests)
        configStore.saveRecentCompletions(recentCompletions)
        if (hasReply) speakVoiceReply(completedText)
        if (needsAttention) {
            val hermesName = uiState.userProfile.hermesDisplayName.ifBlank { "Hermes" }
            HermesNotifications.showMessage(
                getApplication(),
                "$hermesName 已回复",
                listOfNotNull(
                    session?.title?.takeIf(String::isNotBlank),
                    completedText.take(120).ifBlank { "回复中包含图片" },
                ).joinToString(" · "),
                profile = session?.profile,
                sessionId = sessionId,
                route = "chat",
            )
        }
        if (sessionId != null) scheduleTitleRefresh(sessionId)
        clearActiveStreamState()
        if (queued != null) {
            viewModelScope.launch {
                delay(180)
                startMessage(queued.session, queued.prompt, queued.attachments)
            }
        } else {
            consumePendingDeepLink()
        }
    }

    private fun startStreamWatchdog(session: HermesSession) {
        streamWatchdogJob?.cancel()
        streamWatchdogJob = viewModelScope.launch {
            var observedRecoveredSignature = ""
            delay(STREAM_IDLE_POLL_AFTER_MILLIS)
            while (uiState.isStreaming && uiState.streamingSessionId == session.id) {
                val hasPendingRequest = uiState.pendingAgentRequests.any { it.conversationId == session.id }
                val idleFor = System.currentTimeMillis() - uiState.runLastActivityAtMillis
                if (!hasPendingRequest && !uiState.isRecoveringConnection && idleFor >= STREAM_IDLE_POLL_AFTER_MILLIS) {
                    val client = apiClient ?: return@launch
                    val result = runCatching { withContext(Dispatchers.IO) { client.loadLatestMessages(session) } }
                    val failure = result.exceptionOrNull()?.let(::unwrapFailure)
                    if (failure is ApiException && failure.statusCode in setOf(401, 403)) {
                        handleStreamFailure(failure)
                        return@launch
                    }
                    val messages = result.getOrNull()
                    val recovered = messages?.let {
                        findRecoveredAssistant(it, streamRecoveryPrompt, streamBaselineAssistantSignature)
                    }
                    if (messages != null && recovered != null) {
                        val signature = recovered.recoverySignature()
                        if (signature == observedRecoveredSignature) {
                            streamController?.stop()
                            completeRecoveredStream(session, messages, "回复已自动同步")
                            return@launch
                        }
                        observedRecoveredSignature = signature
                    } else {
                        observedRecoveredSignature = ""
                    }
                    if (failure != null && idleFor >= STREAM_CONNECTION_STALE_MILLIS) {
                        recoverInterruptedStream("实时连接暂时没有响应，正在自动取回结果")
                        return@launch
                    }
                } else observedRecoveredSignature = ""
                delay(STREAM_IDLE_POLL_INTERVAL_MILLIS)
            }
        }
    }

    private fun resumeRecoveryAfterAgentResponse(request: AgentRequest) {
        if (uiState.isStreaming && streamController != null && streamRecoveryJob?.isActive != true) return
        if (streamRecoveryJob?.isActive == true) return
        val session = activeStreamSession
            ?: uiState.sessions.firstOrNull { it.id == request.conversationId }
            ?: uiState.selectedSession?.takeIf { it.id == request.conversationId }
            ?: return
        val snapshot = configStore.readActiveRunSnapshot()
        activeStreamSession = session
        streamRecoveryPrompt = snapshot?.takeIf { it.sessionId == session.id }?.submittedPrompt.orEmpty()
        streamBaselineAssistantSignature = snapshot?.takeIf { it.sessionId == session.id }?.baselineAssistantSignature
            ?: activeStreamMessages().lastOrNull { it.role == MessageRole.ASSISTANT }?.recoverySignature().orEmpty()
        val startedAt = snapshot?.takeIf { it.sessionId == session.id }?.startedAtMillis ?: System.currentTimeMillis()
        uiState = uiState.copy(
            isStreaming = true,
            streamingSessionId = session.id,
            runStage = "已处理，Hermes 正在继续",
            runStartedAtMillis = startedAt,
            runLastActivityAtMillis = System.currentTimeMillis(),
            isRecoveringConnection = true,
        )
        recoverInterruptedStream("已提交处理结果，正在继续取回回复")
    }

    private fun completeRecoveredStream(session: HermesSession, messages: List<ChatMessage>, notice: String) {
        val visibleMessages = messages.visibleConversationMessages()
        cacheMessages(session.id, visibleMessages)
        val insights = ChatInsightParser.fromMessages(messages)
        activeStreamArtifacts = insights.artifacts
        activeStreamTodos = insights.todos
        uiState = uiState.copy(
            messages = if (uiState.selectedSession?.id == session.id) visibleMessages else uiState.messages,
            chatArtifacts = if (uiState.selectedSession?.id == session.id) insights.artifacts else uiState.chatArtifacts,
            chatTodos = if (uiState.selectedSession?.id == session.id) insights.todos else uiState.chatTodos,
            noticeMessage = notice,
            errorMessage = null,
        )
        finishStreaming()
    }

    private fun recoverInterruptedStream(message: String) {
        if (streamRecoveryJob?.isActive == true) return
        val client = apiClient ?: return finishInterruptedRecovery()
        val session = activeStreamSession
            ?: uiState.sessions.firstOrNull { it.id == uiState.streamingSessionId }
            ?: return finishInterruptedRecovery()
        val prompt = streamRecoveryPrompt
        val baselineSignature = streamBaselineAssistantSignature

        streamWatchdogJob?.cancel()
        streamWatchdogJob = null
        streamController?.stop()
        streamJob?.cancel()
        streamController = null
        streamJob = null
        uiState = uiState.copy(
            isStreaming = true,
            isRecoveringConnection = true,
            errorMessage = null,
            noticeMessage = message,
            toolActivities = activeStreamToolActivities.map { activity ->
                if (activity.status == ToolStatus.RUNNING) activity.copy(status = ToolStatus.FAILED) else activity
            },
        )
        activeStreamToolActivities = activeStreamToolActivities.map { activity ->
            if (activity.status == ToolStatus.RUNNING) activity.copy(status = ToolStatus.FAILED) else activity
        }

        streamRecoveryJob = viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) { client.reconnectGateway() } }

            val retryDelays = listOf(0L, 1_000L, 2_000L, 4_000L, 7_000L, 10_000L, 15_000L, 20_000L, 30_000L, 30_000L, 45_000L, 60_000L)
            for (waitMillis in retryDelays) {
                if (waitMillis > 0) delay(waitMillis)
                if (!uiState.isStreaming || uiState.streamingSessionId != session.id) return@launch
                if (uiState.pendingAgentRequests.any { it.conversationId == session.id }) {
                    streamRecoveryJob = null
                    uiState = uiState.copy(
                        isRecoveringConnection = false,
                        runStage = "等待你的处理",
                        noticeMessage = "处理下方请求后，Hermes 会继续运行",
                    )
                    return@launch
                }

                val result = runCatching { withContext(Dispatchers.IO) { client.loadLatestMessages(session) } }
                val failure = result.exceptionOrNull()?.let(::unwrapFailure)
                if (failure is ApiException && failure.statusCode in setOf(401, 403)) {
                    handleStreamFailure(failure)
                    return@launch
                }

                val messages = result.getOrNull() ?: continue
                val recoveredAssistant = findRecoveredAssistant(messages, prompt, baselineSignature)
                if (recoveredAssistant != null) {
                    completeRecoveredStream(session, messages, "连接已恢复，回复已同步")
                    return@launch
                }
            }
            finishInterruptedRecovery()
        }
    }

    private fun finishInterruptedRecovery() {
        val sessionId = uiState.streamingSessionId
        if (uiState.pendingAgentRequests.any { it.conversationId == sessionId }) {
            streamRecoveryJob = null
            uiState = uiState.copy(
                isRecoveringConnection = false,
                runStage = "等待你的处理",
                noticeMessage = "处理待确认请求后，Hermes 会继续运行",
            )
            return
        }
        val stoppedMessages = preserveFailedSend(sessionId)
        streamRecoveryJob = null
        streamController = null
        streamJob = null
        streamRecoveryPrompt = ""
        streamBaselineAssistantSignature = ""
        uiState = uiState.copy(
            isStreaming = false,
            streamingSessionId = null,
            runStage = "",
            activeCouncilMode = CouncilMode.OFF,
            isSteering = false,
            pendingAgentRequests = uiState.pendingAgentRequests.filterNot { it.conversationId == sessionId },
            isRecoveringConnection = false,
            messages = if (uiState.selectedSession?.id == sessionId) stoppedMessages else uiState.messages,
            toolActivities = uiState.toolActivities.map { activity ->
                if (activity.status == ToolStatus.RUNNING) activity.copy(status = ToolStatus.FAILED) else activity
            },
            noticeMessage = null,
            errorMessage = "网络仍不稳定，未能自动取回完整回复。请稍后重新打开当前对话确认结果。",
        )
        configStore.savePendingAgentRequests(uiState.pendingAgentRequests)
        clearActiveStreamState()
    }

    private fun scheduleTitleRefresh(sessionId: String) {
        val session = activeStreamSession?.takeIf { it.id == sessionId }
            ?: uiState.selectedSession?.takeIf { it.id == sessionId }
            ?: uiState.sessions.firstOrNull { it.id == sessionId }
            ?: return
        if (sessionId !in pendingTitleSessionIds) return
        val client = apiClient ?: return
        titleRefreshJob?.cancel()
        titleRefreshJob = viewModelScope.launch {
            val waits = listOf(700L, 1_400L, 2_800L)
            for (wait in waits) {
                delay(wait)
                val raw = runCatching { withContext(Dispatchers.IO) { client.sessionTitle(session.id) } }
                    .getOrNull()
                    .orEmpty()
                if (!isPlaceholderSessionTitle(raw)) {
                    val compact = compactSessionTitle(raw)
                    val saved = if (compact != raw.trim()) {
                        runCatching { withContext(Dispatchers.IO) { client.renameSession(session.id, compact) } }
                            .getOrDefault(compact)
                    } else compact
                    applySessionTitle(session.id, saved)
                    pendingTitleSessionIds -= session.id
                    return@launch
                }
            }
            val fallback = compactSessionTitle(
                messageCache[cacheKey(sessionId)].orEmpty().firstOrNull { it.role == MessageRole.USER }?.content.orEmpty(),
            )
            if (fallback != "新会话") {
                val saved = runCatching { withContext(Dispatchers.IO) { client.renameSession(session.id, fallback) } }
                    .getOrDefault(fallback)
                applySessionTitle(session.id, saved)
            }
            pendingTitleSessionIds -= session.id
        }
    }

    private fun applySessionTitle(sessionId: String, title: String) {
        uiState = uiState.copy(
            selectedSession = uiState.selectedSession?.takeIf { it.id == sessionId }?.copy(title = title)
                ?: uiState.selectedSession,
            sessions = uiState.sessions.map { if (it.id == sessionId) it.copy(title = title) else it },
        )
    }

    private fun updateSession(sessionId: String, transform: (HermesSession) -> HermesSession) {
        uiState = uiState.copy(
            sessions = uiState.sessions.map { if (it.id == sessionId) transform(it) else it },
            selectedSession = uiState.selectedSession?.let { if (it.id == sessionId) transform(it) else it },
        )
    }

    private fun activeStreamMessages(): List<ChatMessage> {
        val sessionId = uiState.streamingSessionId ?: activeStreamSession?.id ?: return emptyList()
        return if (uiState.selectedSession?.id == sessionId && uiState.messages.isNotEmpty()) {
            uiState.messages
        } else {
            messageCache[cacheKey(sessionId)].orEmpty()
        }
    }

    private fun setActiveStreamMessages(messages: List<ChatMessage>) {
        val sessionId = uiState.streamingSessionId ?: activeStreamSession?.id ?: return
        cacheMessages(sessionId, messages)
        if (uiState.selectedSession?.id == sessionId) {
            uiState = uiState.copy(messages = messages)
        }
    }

    private fun cacheMessages(sessionId: String, messages: List<ChatMessage>) {
        messageCache[cacheKey(sessionId)] = messages
        while (messageCache.size > 8) messageCache.remove(messageCache.keys.first())
    }

    private fun rememberRecentArtifacts(
        session: HermesSession,
        messages: List<ChatMessage>,
        additionalArtifacts: List<ChatArtifact> = emptyList(),
    ) {
        mergeRecentArtifacts(discoverRecentArtifacts(session, messages, additionalArtifacts))
    }

    private fun discoverRecentArtifacts(
        session: HermesSession,
        messages: List<ChatMessage>,
        additionalArtifacts: List<ChatArtifact> = emptyList(),
    ): List<RecentArtifact> {
        val visibleMessages = messages.filter { it.role == MessageRole.USER || it.role == MessageRole.ASSISTANT }
        val now = System.currentTimeMillis()
        val fromMessages = visibleMessages.asReversed().flatMap { message ->
            ChatInsightParser.artifactsFromText(message.content).map { artifact ->
                RecentArtifact(
                    profile = session.profile,
                    sessionId = session.id,
                    sessionTitle = session.title.ifBlank { "Hermes 对话" },
                    messageId = message.id,
                    path = artifact.path,
                    name = artifact.name,
                    kind = artifact.kind,
                    seenAtMillis = now,
                )
            }
        }
        val fallbackMessageId = visibleMessages.lastOrNull()?.id.orEmpty()
        val extras = additionalArtifacts.map { artifact ->
            RecentArtifact(
                profile = session.profile,
                sessionId = session.id,
                sessionTitle = session.title.ifBlank { "Hermes 对话" },
                messageId = fallbackMessageId,
                path = artifact.path,
                name = artifact.name,
                kind = artifact.kind,
                seenAtMillis = now,
            )
        }
        val discovered = (extras + fromMessages).distinctBy { it.path }
        return discovered
    }

    private fun mergeRecentArtifacts(discovered: List<RecentArtifact>) {
        if (discovered.isEmpty()) return
        val merged = (discovered + uiState.recentArtifacts)
            .distinctBy { "${it.profile}::${it.path}" }
            .take(60)
        uiState = uiState.copy(recentArtifacts = merged)
        configStore.saveRecentArtifacts(merged)
    }

    private fun recentArtifactSource(session: HermesSession, artifact: ChatArtifact): RecentArtifact {
        val sourceMessage = uiState.messages.asReversed().firstOrNull { message ->
            message.role in setOf(MessageRole.USER, MessageRole.ASSISTANT) &&
                ChatInsightParser.artifactsFromText(message.content).any { it.path == artifact.path }
        }
        return RecentArtifact(
            profile = session.profile,
            sessionId = session.id,
            sessionTitle = session.title.ifBlank { "Hermes 对话" },
            messageId = sourceMessage?.id.orEmpty(),
            path = artifact.path,
            name = artifact.name,
            kind = artifact.kind,
        )
    }

    private fun searchResult(
        session: HermesSession,
        messages: List<ChatMessage>?,
        keyword: String,
    ): SessionSearchResult? {
        val message = messages.orEmpty().asReversed().firstOrNull { item ->
            item.role in setOf(MessageRole.USER, MessageRole.ASSISTANT) &&
                item.content.contains(keyword, ignoreCase = true)
        } ?: return null
        return SessionSearchResult(
            session = session,
            snippet = searchSnippet(message.content, keyword),
            messageId = message.id,
            matchedMessage = true,
        )
    }

    private fun metadataSearchResult(session: HermesSession, keyword: String): SessionSearchResult? {
        val source = sequenceOf(session.title, session.preview, session.source)
            .firstOrNull { it.contains(keyword, ignoreCase = true) }
            ?: return null
        return SessionSearchResult(
            session = session,
            snippet = searchSnippet(source, keyword),
        )
    }

    private fun cacheKey(sessionId: String): String =
        "${activeStreamSession?.profile ?: uiState.activeProfile}::$sessionId"

    private fun markSessionRead(sessionId: String) {
        if (sessionId !in uiState.unreadSessionIds) return
        val unreadSessionIds = uiState.unreadSessionIds - sessionId
        configStore.saveUnreadSessionIds(unreadSessionIds)
        uiState = uiState.copy(unreadSessionIds = unreadSessionIds)
    }

    private fun clearActiveStreamState() {
        streamingDeltaFlushJob?.cancel()
        streamingDeltaFlushJob = null
        streamingDeltaBuffer.clear()
        configStore.clearActiveRunSnapshot()
        streamController = null
        streamJob = null
        streamRecoveryJob = null
        streamWatchdogJob?.cancel()
        streamWatchdogJob = null
        streamRecoveryPrompt = ""
        streamBaselineAssistantSignature = ""
        activeSubmittedPrompt = ""
        activeSubmittedAttachments = emptyList()
        activeUserMessageId = null
        activeStreamSession = null
        activeStreamToolActivities = emptyList()
        activeStreamArtifacts = emptyList()
        activeStreamTodos = emptyList()
    }

    private fun restoreSavedRunIfNeeded(sessions: List<HermesSession>) {
        if (savedRunRecoveryAttempted || uiState.isStreaming) return
        val snapshot = configStore.readActiveRunSnapshot() ?: return
        savedRunRecoveryAttempted = true
        if (System.currentTimeMillis() - snapshot.startedAtMillis > 24 * 60 * 60 * 1_000L || snapshot.profile != uiState.activeProfile) {
            configStore.clearActiveRunSnapshot()
            return
        }
        val session = sessions.firstOrNull { it.id == snapshot.sessionId }
            ?: HermesSession(
                id = snapshot.sessionId,
                title = snapshot.title.ifBlank { "Hermes 对话" },
                profile = snapshot.profile,
            )
        activeStreamSession = session
        streamRecoveryPrompt = snapshot.submittedPrompt
        streamBaselineAssistantSignature = snapshot.baselineAssistantSignature
        activeStreamToolActivities = emptyList()
        activeStreamArtifacts = emptyList()
        activeStreamTodos = emptyList()
        uiState = uiState.copy(
            isStreaming = true,
            streamingSessionId = session.id,
            runStage = "正在恢复上次运行",
            runStartedAtMillis = snapshot.startedAtMillis,
            runLastActivityAtMillis = System.currentTimeMillis(),
            isRecoveringConnection = true,
            noticeMessage = "检测到上次未完成的 Agent 运行，正在自动取回结果",
        )
        recoverInterruptedStream("正在重新连接 Hermes")
    }

    private fun handleStreamFailure(throwable: Throwable) {
        flushStreamingDelta()
        val sessionId = uiState.streamingSessionId ?: activeStreamSession?.id
        val stoppedMessages = preserveFailedSend(sessionId)
        uiState = uiState.copy(
            isStreaming = false,
            streamingSessionId = null,
            runStage = "",
            activeCouncilMode = CouncilMode.OFF,
            isSteering = false,
            pendingAgentRequests = uiState.pendingAgentRequests.filterNot { it.conversationId == sessionId },
            isRecoveringConnection = false,
            messages = if (uiState.selectedSession?.id == sessionId) stoppedMessages else uiState.messages,
        )
        configStore.savePendingAgentRequests(uiState.pendingAgentRequests)
        clearActiveStreamState()
        handleFailure(throwable)
    }

    private fun preserveFailedSend(sessionId: String?): List<ChatMessage> {
        val currentMessages = activeStreamMessages()
        val partialReply = currentMessages.lastOrNull { it.isStreaming }
            ?.let { it.content.isNotBlank() || it.images.isNotEmpty() }
            ?: false
        val stopped = currentMessages.mapNotNull { item ->
            if (!item.isStreaming) return@mapNotNull item
            item.takeIf { it.content.isNotBlank() || it.images.isNotEmpty() }
                ?.copy(isStreaming = false)
        }
        val cleaned = if (!partialReply && activeUserMessageId != null) {
            stopped.filterNot { it.id == activeUserMessageId }
        } else stopped
        if (sessionId != null) cacheMessages(sessionId, cleaned)

        val failure = FailedSend(activeSubmittedPrompt, activeSubmittedAttachments)
        val session = activeStreamSession ?: uiState.selectedSession?.takeIf { it.id == sessionId }
        if (session != null) configStore.saveDraft(session.profile, session.id, activeSubmittedPrompt)
        uiState = uiState.copy(
            draft = if (uiState.selectedSession?.id == sessionId) activeSubmittedPrompt else uiState.draft,
            attachments = if (uiState.selectedSession?.id == sessionId) activeSubmittedAttachments else uiState.attachments,
            failedSend = if (uiState.selectedSession?.id == sessionId) failure else uiState.failedSend,
        )
        return cleaned
    }

    private fun updateDiagnostic(key: String, detail: String, status: DiagnosticStatus) {
        uiState = uiState.copy(
            connectionDiagnostics = uiState.connectionDiagnostics.map { item ->
                if (item.key == key) item.copy(detail = detail, status = status) else item
            },
        )
    }

    private fun diagnosticFailure(throwable: Throwable?): String {
        val root = throwable?.let(::unwrapFailure)
        return when (root) {
            is ApiException -> when (root.statusCode) {
                401, 403 -> "登录会话已失效，请重新输入密码"
                404 -> "接口不存在，可能需要升级 Hermes Agent"
                408 -> "连接超时，请检查反向代理或网络"
                else -> root.message.ifBlank { "服务器请求失败" }
            }
            is IOException -> "网络不可达或连接被中断"
            null -> "未知错误"
            else -> root.message?.takeIf(String::isNotBlank) ?: "连接失败"
        }
    }

    private fun handleFailure(throwable: Throwable) {
        val root = unwrapFailure(throwable)
        val message = when (root) {
            is ApiException -> when (root.statusCode) {
                401, 403 -> "登录已失效，或 Hermes 用户名/密码不正确"
                404 -> "当前 Hermes 版本不支持所需接口，请先升级 Hermes Agent"
                429 -> "Hermes 正在处理过多任务，请稍后再试"
                else -> root.message.ifBlank { "服务器请求失败" }
            }
            is IOException -> "网络连接不稳定，请稍后重试"
            else -> root.message?.takeIf { it.isNotBlank() } ?: "连接失败，请检查远程网关地址和网络"
        }
        uiState = uiState.copy(
            isBusy = false,
            isWorkspaceLoading = false,
            isWorkspaceSaving = false,
            isImageLoading = false,
            isCronLoading = false,
            cronActionId = null,
            isModelsLoading = false,
            isModelSwitching = false,
            isProfileSwitching = false,
            isProjectsLoading = false,
            isProjectPickerLoading = false,
            isAdvancedSettingsLoading = false,
            settingsActionKey = null,
            sessionActionId = null,
            isBatchRenaming = false,
            errorMessage = message,
            route = if (root is ApiException && root.statusCode in setOf(401, 403)) AppRoute.SETUP else uiState.route,
            hasSavedConnection = if (root is ApiException && root.statusCode in setOf(401, 403)) {
                false
            } else {
                uiState.hasSavedConnection
            },
        )
    }

    private fun showError(message: String) {
        uiState = uiState.copy(errorMessage = message, noticeMessage = null, isBusy = false)
    }

    private fun normalizeBaseUrl(raw: String): String? {
        val value = raw.trim().trimEnd('/')
        val uri = runCatching { URI(value) }.getOrNull() ?: return null
        if (uri.scheme?.lowercase() !in setOf("http", "https") || uri.host.isNullOrBlank()) return null
        if (uri.userInfo != null || uri.query != null || uri.fragment != null) return null
        return value
    }

    private fun pathIsWithin(root: String, target: String): Boolean {
        val cleanRoot = root.trimEnd('/', '\\')
        if (target == cleanRoot || target == root) return true
        return target.startsWith("$cleanRoot/") || target.startsWith("$cleanRoot\\")
    }

    private fun isAppInForeground(): Boolean {
        val info = ActivityManager.RunningAppProcessInfo()
        ActivityManager.getMyMemoryState(info)
        return info.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND ||
            info.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE
    }

    private fun resumeSavedConnection(client: HermesApiClient) {
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) { client.checkSavedSession() } }
                .onSuccess { signedInAs ->
                    uiState = uiState.copy(
                        route = AppRoute.SESSIONS,
                        isBusy = false,
                        noticeMessage = "已恢复登录：$signedInAs",
                    )
                    loadGatewayInfo(client)
                    loadProfilesAndSessions(client)
                }
                .onFailure {
                    cookieJar.clear()
                    uiState = uiState.copy(
                        route = AppRoute.SETUP,
                        isBusy = false,
                        hasSavedConnection = false,
                        noticeMessage = "登录已过期，请重新输入密码",
                    )
                }
        }
    }

    private fun loadProfilesAndSessions(client: HermesApiClient) {
        uiState = uiState.copy(isProfilesLoading = true, isBusy = true)
        val saved = configStore.readActiveHermesProfile().ifBlank { "default" }
        client.setProfile(saved)
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    coroutineScope {
                        val profilesRequest = async { client.listProfiles() }
                        val sessionsRequest = async { runCatching { client.listSessions() } }
                        val profiles = profilesRequest.await()
                        val available = profiles.ifEmpty { listOf(HermesProfile(name = "default", isDefault = true)) }
                        val selected = available.firstOrNull { it.name == saved }
                            ?: available.firstOrNull { it.isDefault }
                            ?: available.first()
                        val page = if (selected.name == saved) {
                            sessionsRequest.await().getOrThrow()
                        } else {
                            sessionsRequest.await()
                            client.setProfile(selected.name)
                            client.listSessions()
                        }
                        Triple(available, selected, page)
                    }
                }
            }
                .onSuccess { (available, selected, page) ->
                    client.setProfile(selected.name)
                    configStore.saveActiveHermesProfile(selected.name)
                    uiState = uiState.copy(
                        profiles = available,
                        activeProfile = selected.name,
                        sessions = page.sessions.distinctBy(HermesSession::id),
                        sessionTotalCount = page.totalCount,
                        isProfilesLoading = false,
                        isBusy = false,
                        isProfileSwitching = false,
                    )
                    consumePendingDeepLink()
                    refreshProjects()
                    restoreSavedRunIfNeeded(page.sessions)
                }
                .onFailure {
                    uiState = uiState.copy(isProfilesLoading = false)
                    handleFailure(it)
                }
        }
    }

    private fun loadGatewayInfo(client: HermesApiClient) {
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) { client.gatewayInfo() } }
                .onSuccess { info -> if (apiClient === client) uiState = uiState.copy(gatewayInfo = info) }
        }
    }

    private fun unwrapFailure(throwable: Throwable): Throwable {
        var current = throwable
        while (current.cause != null && current.cause !== current) current = current.cause!!
        return current
    }

    override fun onCleared() {
        streamRecoveryJob?.cancel()
        streamWatchdogJob?.cancel()
        agentUpdateJob?.cancel()
        voicePlaybackJob?.cancel()
        voiceCaptureJob?.cancel()
        voiceLevelJob?.cancel()
        voiceRecorder.cancel()
        voicePlayback.stop()
        apiClient?.close()
        super.onCleared()
    }
}

internal fun searchSnippet(content: String, keyword: String): String {
    val compact = content.replace(Regex("\\s+"), " ").trim()
    if (compact.length <= 120) return compact
    val index = compact.indexOf(keyword, ignoreCase = true).coerceAtLeast(0)
    val start = (index - 36).coerceAtLeast(0)
    val end = (start + 120).coerceAtMost(compact.length)
    return buildString {
        if (start > 0) append('…')
        append(compact.substring(start, end))
        if (end < compact.length) append('…')
    }
}

private fun WorkspaceDocument.bytesForTransfer(): ByteArray =
    bytes.takeIf { it.isNotEmpty() } ?: content.toByteArray(Charsets.UTF_8)

private fun String.isPreviewableArtifact(): Boolean =
    substringAfterLast('.', "").lowercase() in setOf(
        "md", "markdown", "pdf", "html", "htm", "txt", "csv", "tsv", "json", "xml",
        "yaml", "yml", "log", "kt", "java", "py", "js", "ts", "css", "sh", "sql",
    )

private val DEFAULT_SLASH_COMMANDS = listOf(
    SlashCommand("/new", "开始一个新对话", "会话"),
    SlashCommand("/retry", "重新执行上一条消息", "会话"),
    SlashCommand("/undo", "移除上一轮用户与助手消息", "会话"),
    SlashCommand("/title", "设置当前对话标题", "会话", "[标题]"),
    SlashCommand("/compress", "压缩当前对话上下文", "会话"),
    SlashCommand("/model", "查看或切换当前模型", "模型", "[provider:model]"),
    SlashCommand("/reasoning", "调整推理强度或显示方式", "模型", "[级别]"),
    SlashCommand("/skills", "搜索、查看或管理技能", "技能"),
    SlashCommand("/status", "查看当前会话状态", "信息"),
    SlashCommand("/usage", "查看本会话用量", "信息"),
    SlashCommand("/help", "查看可用命令", "信息"),
    SlashCommand("/stop", "停止当前正在执行的任务", "会话"),
)

private const val CHAT_PAGE_SIZE = 60
private const val SEARCH_MESSAGE_LIMIT = 200
private const val RECENT_ARTIFACT_SESSION_LIMIT = 24
private const val RECENT_ARTIFACT_MESSAGE_LIMIT = 80
private const val STREAM_DELTA_FRAME_MILLIS = 50L
private const val STREAM_IDLE_POLL_AFTER_MILLIS = 45_000L
private const val STREAM_IDLE_POLL_INTERVAL_MILLIS = 30_000L
private const val STREAM_CONNECTION_STALE_MILLIS = 90_000L

internal fun artifactIndexFingerprint(session: HermesSession): String =
    listOf(session.updatedAt, session.messageCount.toString(), session.preview.hashCode().toString()).joinToString("|")

private fun String.isMoaProvider(): Boolean =
    equals("moa", ignoreCase = true) || contains("mixture-of-agents", ignoreCase = true)

private fun councilToolName(name: String): String =
    if (name.contains("delegate", ignoreCase = true)) "专家并行分析" else name

internal fun buildCouncilPrompt(prompt: String, mode: CouncilMode): String = when (mode) {
    CouncilMode.OFF -> prompt
    CouncilMode.QUICK -> """
        [Hermes Mobile · 快速会审]
        当前会话使用 MoA。请利用各参考模型已经独立生成的分析，由聚合模型做真正的比较与裁决；不要虚构角色对话，也不要输出参考模型的原始聊天记录。

        最终答复只保留对用户有用的内容，并使用以下结构：
        ## 会审结论
        ## 共识
        ## 关键分歧与裁决
        ## 证据与风险
        ## 相比单模型的增益
        ## 置信度与未决事项

        [原始问题]
        $prompt
    """.trimIndent()
    CouncilMode.DEEP -> """
        [Hermes Mobile · 深度专家会审协议]
        这不是角色扮演。三个子 Agent 必须给出真实、独立的返回结果；移动端会把异步批次中的三份结果分别显示为群聊成员，不得把子 Agent 回包伪装成用户消息。

        先判断该问题是否确实值得多 Agent 会审。若问题很简单，直接给出精炼答案并明确说明“本题无需会审”，避免浪费 Token。若值得会审：
        1. 使用 delegate_task，以一个并行批次启动 3 个隔离上下文的专家：证据分析员（事实、来源与假设）、反方审查员（反例、盲点与失败条件）、落地评审员（成本、步骤与可执行性）。三者必须独立首轮分析。
        2. 主 Agent 比较三份结论，识别真正影响决策的共识和冲突。只有存在高影响且未解决的分歧时，才允许追加至多 1 轮定向复核；禁止开放式互聊。
        3. 若 delegate_task 不可用，不得伪造专家意见；请明确标注“会审降级为单 Agent 审查”。
        4. 子 Agent 的独立结果由异步批次正常返回；主 Agent 的最终答复不要再次整段复制三份原文，只输出压缩后的决策信息。

        最终答复使用以下结构：
        ## 会审结论
        ## 共识
        ## 关键分歧与裁决
        ## 证据与风险
        ## 相比单 Agent 的增益
        ## 置信度与未决事项

        [原始问题]
        $prompt
    """.trimIndent()
}

private fun List<ChatMessage>?.visibleConversationMessages(): List<ChatMessage> =
    this.orEmpty().filter { it.role == MessageRole.USER || it.role == MessageRole.ASSISTANT }

internal fun replyNeedsAttention(
    route: AppRoute,
    selectedSessionId: String?,
    completedSessionId: String,
    appInForeground: Boolean,
): Boolean = !appInForeground || route !in setOf(AppRoute.CHAT, AppRoute.VOICE_CHAT) || selectedSessionId != completedSessionId

internal fun ChatMessage.recoverySignature(): String = buildString {
    append(createdAt)
    append('|')
    append(content.trim())
    images.forEach { append('|').append(it.source) }
}

internal fun findRecoveredAssistant(
    messages: List<ChatMessage>,
    submittedPrompt: String,
    baselineSignature: String,
): ChatMessage? {
    val visible = messages.visibleConversationMessages()
    val normalizedPrompt = submittedPrompt.trim()
    val submittedUserIndex = visible.indexOfLast { message ->
        message.role == MessageRole.USER &&
            normalizedPrompt.isNotBlank() &&
            message.content.trim().startsWith(normalizedPrompt)
    }
    val candidate = if (submittedUserIndex >= 0) {
        visible.drop(submittedUserIndex + 1).lastOrNull { it.role == MessageRole.ASSISTANT }
    } else {
        visible.lastOrNull { it.role == MessageRole.ASSISTANT }
    }
    return candidate?.takeIf {
        (it.content.isNotBlank() || it.images.isNotEmpty()) && it.recoverySignature() != baselineSignature
    }
}

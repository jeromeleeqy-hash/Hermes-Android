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
import com.qingyu.hermescompanion.data.ArtifactFileReader
import com.qingyu.hermescompanion.data.resolveRemoteArtifactPath
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

enum class ChatEntryAction { NONE, ATTACHMENTS, VOICE }

enum class AppRoute {
    HOME,
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
    val selectedProjectId: String? = null,
    val profiles: List<HermesProfile> = emptyList(),
    val activeProfile: String = "default",
    val isProfilesLoading: Boolean = false,
    val isProfileSwitching: Boolean = false,
    val selectedSession: HermesSession? = null,
    val chatEntryAction: ChatEntryAction = ChatEntryAction.NONE,
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
    val runningSessions: List<HermesSession> = emptyList(),
    val runningRuns: List<RunUiState> = emptyList(),
    val stoppingSessionKeys: Set<String> = emptySet(),
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
    val workspaceAttachmentTarget: HermesSession? = null,
    val isWorkspaceAttaching: Boolean = false,
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
    private val activeRuns = LinkedHashMap<String, SessionRun>()
    private val titleRefreshJobs = mutableMapOf<String, Job>()
    private val failedSends = mutableMapOf<String, FailedSend>()
    private val attachmentDrafts = mutableMapOf<String, List<PendingAttachment>>()
    private var slashCommandJob: Job? = null
    private var sessionSearchJob: Job? = null
    private var agentUpdateJob: Job? = null
    private var voicePlaybackJob: Job? = null
    private var voiceCaptureJob: Job? = null
    private var voiceLevelJob: Job? = null
    private var voiceEpoch = 0L
    private var voiceSessionKey: String? = null
    private var slashCommandQuery: String = ""
    private val commandCatalogCache = mutableMapOf<String, List<SlashCommand>>()
    private val messageCache = LinkedHashMap<String, List<ChatMessage>>()
    private val oldestMessageOffsets = mutableMapOf<String, Int>()
    private val indexedArtifactProfiles = mutableSetOf<String>()
    private val pendingTitleSessionIds = mutableSetOf<String>()
    private val chatScrollPositions = mutableMapOf<String, ChatScrollPosition>()
    private var searchReturnRoute = AppRoute.SESSIONS
    private var chatReturnRoute: AppRoute = AppRoute.SESSIONS
    private var settingsReturnRoute: AppRoute = AppRoute.PROFILE
    private var pendingDeepLink: HermesDeepLink? = null
    private val recoveredProfiles = mutableSetOf<String>()
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
            val client = HermesApiClient(saved, cookieJar, configStore)
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
                val client = HermesApiClient(config, cookieJar, configStore)
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
                    route = AppRoute.HOME,
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
                        invalidateWorkspace()
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
            selectedProjectId = null,
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
        if (uiState.isStreaming || uiState.stoppingSessionKeys.isNotEmpty()) {
            showNotice("当前回复仍在生成，请等待完成后再切换 Profile")
            return
        }
        if (profile.name == client.currentProfile()) return
        titleRefreshJobs.values.forEach { it.cancel() }
        titleRefreshJobs.clear()
        slashCommandJob?.cancel()
        sessionSearchJob?.cancel()
        invalidateWorkspace()
        client.setProfile(profile.name)
        configStore.saveActiveHermesProfile(profile.name)
        messageCache.clear()
        oldestMessageOffsets.clear()
        pendingTitleSessionIds.clear()
        uiState = uiState.copy(
            route = AppRoute.SESSIONS,
            activeProfile = profile.name,
            selectedProjectId = configStore.readSelectedProject(profile.name),
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

    fun selectProject(projectId: String?) {
        if (uiState.route == AppRoute.SESSIONS) saveCurrentChatDraft()
        configStore.saveSelectedProject(uiState.activeProfile,projectId)
        invalidateWorkspace()
        uiState = uiState.copy(
            selectedProjectId = projectId,
            selectedSession = if (uiState.route == AppRoute.SESSIONS) null else uiState.selectedSession,
        )
        publishRuns()
    }

    fun createSession() = createSessionWithDraft(null)

    fun startFromHome(text: String) = createSessionWithDraft(text)

    fun startWithAttachmentsFromHome(text: String) = createSessionWithDraft(text, ChatEntryAction.ATTACHMENTS)
    fun startWithVoiceFromHome(text: String) = createSessionWithDraft(text, ChatEntryAction.VOICE)
    fun consumeChatEntryAction() { uiState = uiState.copy(chatEntryAction = ChatEntryAction.NONE) }

    private fun createSessionWithDraft(initialDraft: String?, entryAction: ChatEntryAction = ChatEntryAction.NONE) {
        val client = apiClient ?: return
        if (uiState.isBusy || uiState.isProfileSwitching) return
        val project = uiState.projects.firstOrNull { it.id == uiState.selectedProjectId }
        if (uiState.selectedProjectId != null && project == null) {
            showNotice("项目列表正在刷新，请稍后再试")
            return
        }
        saveCurrentChatDraft()
        val profile = uiState.activeProfile
        chatReturnRoute = if (uiState.route == AppRoute.HOME) AppRoute.HOME else AppRoute.SESSIONS
        uiState = uiState.copy(isBusy = true, errorMessage = null)
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) { client.createSession(project?.primaryPath) } }
                .onSuccess { session ->
                    if (uiState.activeProfile != profile || apiClient !== client) return@onSuccess
                    uiState = uiState.copy(
                        sessions = (listOf(session) + uiState.sessions).distinctBy(HermesSession::scopedId),
                        isBusy = false,
                    )
                    cacheMessages(session.id, emptyList())
                    openSessionInternal(session, null)
                    if (initialDraft != null) updateDraft(initialDraft)
                    uiState = uiState.copy(chatEntryAction = entryAction)
                }.onFailure(::handleFailure)
        }
    }

    fun openSession(session: HermesSession) {
        chatReturnRoute = if (uiState.route == AppRoute.HOME) AppRoute.HOME else AppRoute.SESSIONS
        openSessionInternal(session, targetMessageId = null)
    }

    fun openTaskSession(session: HermesSession) {
        chatReturnRoute = AppRoute.TASKS
        openSessionInternal(session, targetMessageId = null)
    }

    private fun openSessionInternal(session: HermesSession, targetMessageId: String?) {
        invalidateWorkspaceAttachmentPicker()
        val client = apiClient ?: return
        uiState = uiState.copy(chatEntryAction = ChatEntryAction.NONE)
        saveCurrentChatDraft()
        markSessionRead(session.scopedId)
        val run = activeRuns[session.scopedId]
        val cached = run?.messages ?: messageCache[session.scopedId]
        val cachedInsights = cached?.let(ChatInsightParser::fromMessages)
        val isActiveStream = run != null
        val visibleCached = cached.visibleConversationMessages()
        val targetIndex = targetMessageId?.let { id -> visibleCached.indexOfFirst { it.id == id }.takeIf { it >= 0 } }
        val savedScroll = targetIndex?.let { ChatScrollPosition(it, 0) } ?: chatScrollPositions[session.scopedId]
        val cachedOffset = oldestMessageOffsets[session.scopedId]
            ?: (session.messageCount - cached.orEmpty().size).coerceAtLeast(0)
        uiState = uiState.copy(
            route = AppRoute.CHAT,
            selectedSession = run?.session ?: session,
            messages = visibleCached,
            hasOlderMessages = cached != null && cachedOffset > 0,
            isOlderMessagesLoading = false,
            toolActivities = run?.tools.orEmpty(),
            chatArtifacts = run?.artifacts ?: cachedInsights?.artifacts.orEmpty(),
            chatTodos = run?.todos ?: cachedInsights?.todos.orEmpty(),
            attachments = attachmentDrafts[session.scopedId].orEmpty(),
            draft = configStore.readDraft(session.profile, session.id),
            failedSend = failedSends[session.scopedId],
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
        publishRuns()
        // session.create returns a live runtime before an empty session is
        // necessarily stored. Its known-empty cache is authoritative until a
        // first message is sent; fetching HTTP history here can return 404.
        val isEmptyLiveSession = !session.runtimeId.isNullOrBlank() &&
            session.messageCount == 0 && cached?.isEmpty() == true
        if (isActiveStream || isEmptyLiveSession) return
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    client.loadRecentMessagePage(session, if (targetMessageId == null) CHAT_PAGE_SIZE else SEARCH_MESSAGE_LIMIT)
                }
            }
                .onSuccess { page ->
                    if (uiState.route != AppRoute.CHAT || uiState.selectedSession?.scopedId != session.scopedId || activeRuns.containsKey(session.scopedId)) {
                        return@onSuccess
                    }
                    val messages = page.messages
                    messageCache[session.scopedId] = messages
                    oldestMessageOffsets[session.scopedId] = page.offset
                    trimMessageCache()
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
                .onFailure { error ->
                    // A delayed history failure belongs only to the page that
                    // requested it, never a newly opened conversation.
                    if (apiClient === client && uiState.route == AppRoute.CHAT &&
                        uiState.selectedSession?.scopedId == session.scopedId &&
                        !activeRuns.containsKey(session.scopedId)
                    ) handleFailure(error)
                }
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
                val latest = activeRuns[session.scopedId]?.messages ?: messageCache[session.scopedId].orEmpty()
                val merged = (page.messages + latest).distinctBy(ChatMessage::id)
                activeRuns[session.scopedId]?.messages = merged.visibleConversationMessages()
                messageCache[session.scopedId] = merged
                oldestMessageOffsets[session.scopedId] = page.offset
                trimMessageCache()
                if (uiState.selectedSession?.scopedId == session.scopedId) {
                    val insights = ChatInsightParser.fromMessages(merged)
                    uiState = uiState.copy(
                        messages = merged.visibleConversationMessages(),
                        chatArtifacts = activeRuns[session.scopedId]?.artifacts ?: insights.artifacts,
                        chatTodos = activeRuns[session.scopedId]?.todos ?: insights.todos,
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
        if (activeRuns.containsKey(session.scopedId) || session.scopedId in uiState.stoppingSessionKeys) return showNotice("请先等待这段对话停止，再进行此操作")
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
                    uiState = uiState.copy(projects = projects, isProjectsLoading = false,
                        selectedProjectId = (uiState.selectedProjectId ?: configStore.readSelectedProject(expectedProfile))?.takeIf { id -> projects.any { it.id == id } })
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
                            selectedProjectId = project.id,
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
        val profile=uiState.activeProfile
        val version=++workspaceRequestVersion
        uiState = uiState.copy(isProjectPickerLoading = true, errorMessage = null)
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    client.listWorkspaceForProfile(path,profile)
                }
            }.onSuccess { listing ->
                if(!workspaceRequestIsCurrent(version,profile)) return@onSuccess
                uiState = uiState.copy(projectPickerListing = listing, isProjectPickerLoading = false)
            }.onFailure { if(workspaceRequestIsCurrent(version,profile)) handleFailure(it) }
        }
    }

    fun closeProjectDirectoryPicker() {
        workspaceRequestVersion++
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
        if (activeRuns.containsKey(session.scopedId) || session.scopedId in uiState.stoppingSessionKeys) return showNotice("请先等待这段对话停止，再进行此操作")
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
        if (activeRuns.containsKey(session.scopedId) || session.scopedId in uiState.stoppingSessionKeys) return showNotice("请先等待这段对话停止，再进行此操作")
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
        val session = uiState.selectedSession ?: return
        val resolver = getApplication<Application>().contentResolver
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) { uris.map { AttachmentReader.read(resolver, it) } } }
                .onSuccess { attachments ->
                    val visible = uiState.selectedSession?.scopedId == session.scopedId
                    val existing = if (visible) uiState.attachments else attachmentDrafts[session.scopedId].orEmpty()
                    val merged = (existing + attachments).take(MAX_ATTACHMENTS)
                    attachmentDrafts[session.scopedId] = merged
                    if (visible) uiState = uiState.copy(attachments = merged,
                        noticeMessage = if (existing.size + attachments.size > MAX_ATTACHMENTS) "单次最多添加 $MAX_ATTACHMENTS 个附件" else null)
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
        }.distinct().take(MAX_ATTACHMENTS)
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
        val target = sessionId?.let { id -> uiState.sessions.firstOrNull { it.id == id } }
        if (target != null && activeRuns.containsKey(target.scopedId)) return showNotice("这段对话正在运行，请选择其他对话")
        saveCurrentChatDraft()
        val project = uiState.projects.firstOrNull { it.id == uiState.selectedProjectId }
        if (uiState.isShareSending) return
        uiState = uiState.copy(isShareSending = true, errorMessage = null)
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val session = target ?: client.createSession(project?.primaryPath)
                    val history = if (session.messageCount > 0) client.loadMessages(session) else emptyList()
                    session to history
                }
            }.onSuccess { (session, history) ->
                if (activeRuns.containsKey(session.scopedId)) {
                    uiState = uiState.copy(isShareSending = false)
                    showNotice("这段对话已开始运行，请选择其他对话")
                    return@onSuccess
                }
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
        uiState.selectedSession?.let { attachmentDrafts[it.scopedId] = uiState.attachments }
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
        if (currentRun() != null || session.scopedId in uiState.stoppingSessionKeys || uiState.isModelSwitching) return
        uiState = uiState.copy(isModelSwitching = true, errorMessage = null)
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) { client.switchSessionModel(session, provider, model) }
            }.onSuccess { updated ->
                uiState = uiState.copy(
                    selectedSession = if (uiState.selectedSession?.scopedId == updated.scopedId) updated else uiState.selectedSession,
                    isModelSwitching = false,
                    noticeMessage = "当前会话已切换到 ${model.substringAfterLast('/')}",
                )
            }.onFailure(::handleFailure)
        }
    }

    fun setCouncilMode(mode: CouncilMode) {
        if (currentRun() != null) return showNotice("请在当前任务完成后开启专家会审")
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
                    selectedSession = if (uiState.selectedSession?.scopedId == updated.scopedId) updated else uiState.selectedSession,
                    councilMode = if (uiState.selectedSession?.scopedId == updated.scopedId) CouncilMode.QUICK else uiState.councilMode,
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
        val session = uiState.selectedSession
        val resolvedPath = resolveArtifactPath(artifact.path, session?.workspacePath.orEmpty())
            ?: return showNotice("无法确定 ${artifact.name} 的绝对路径，请回到来源会话后再试")
        val resolvedArtifact = artifact.copy(path = resolvedPath)
        val source = session?.let { recentArtifactSource(it, resolvedArtifact) }
        if (source != null) {
            val merged = (listOf(source) + uiState.recentArtifacts)
                .distinctBy { "${it.profile}::${it.path}" }
                .take(60)
            uiState = uiState.copy(recentArtifacts = merged)
            configStore.saveRecentArtifacts(merged)
        }
        if (resolvedPath.substringAfterLast('.', "").lowercase() in setOf("png", "jpg", "jpeg", "webp", "gif", "bmp")) {
            openImage(resolvedPath, artifact.name)
            return
        }
        if (!resolvedPath.isPreviewableArtifact()) {
            showNotice("${artifact.name} 已列入聊天产物；当前版本支持图片、Markdown、PDF、HTML 和常见文本预览")
            return
        }
        uiState = uiState.copy(isWorkspaceLoading = true, errorMessage = null)
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) { client.readWorkspaceDocument(resolvedPath) } }
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
                .onFailure(::handleFileFailure)
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

    private fun artifactLoader(item: RecentArtifact): (() -> WorkspaceDocument)? {
        val client = apiClient ?: return null
        val source = uiState.selectedSession?.takeIf { it.id == item.sessionId && it.profile == item.profile }
            ?: uiState.sessions.firstOrNull { it.id == item.sessionId && it.profile == item.profile }
        val messages = if (source?.scopedId == uiState.selectedSession?.scopedId && source != null) uiState.messages
            else messageCache["${item.profile}::${item.sessionId}"].orEmpty()
        return { ArtifactFileReader(client).read(item, source, messages) }
    }

    fun openRecentArtifact(item: RecentArtifact) {
        if (item.profile != uiState.activeProfile) return showNotice("请先切换到档案：${item.profile}")
        if (uiState.workspaceAttachmentTarget != null) { attachRecentArtifact(item); return }
        val load = artifactLoader(item) ?: return
        val version = ++workspaceRequestVersion
        val origin = uiState.route
        uiState = uiState.copy(isWorkspaceLoading = true, errorMessage = null)
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) { load() } }
                .onSuccess { document ->
                    if (!workspaceRequestIsCurrent(version, item.profile) || uiState.route != origin) return@onSuccess
                    val resolved = item.copy(path = document.path, name = document.name)
                    val merged = (listOf(resolved) + uiState.recentArtifacts.filterNot { it.profile == item.profile && it.path == item.path })
                        .distinctBy { "${it.profile}::${it.path}" }.take(60)
                    uiState = uiState.copy(recentArtifacts = merged, isWorkspaceLoading = false)
                    configStore.saveRecentArtifacts(merged)
                    when {
                        document.mimeType.startsWith("image/") -> uiState = uiState.copy(
                            imagePreview = ImagePreview(document.name, document.path, document.mimeType, document.bytes),
                        )
                        document.path.isPreviewableArtifact() -> uiState = uiState.copy(
                            route = AppRoute.WORKSPACE, workspaceDocument = document, workspaceDocumentOrigin = null,
                            workspaceSourceArtifact = resolved, workspaceDraft = document.content, isWorkspaceEditing = false,
                        )
                        else -> showNotice("${document.name} 暂不支持预览，可从对话的“+ → 空间”添加为附件")
                    }
                }.onFailure { if (workspaceRequestIsCurrent(version, item.profile) && uiState.route == origin) handleFileFailure(it) }
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
        if (currentRun() != null) return
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
        voiceTurn: Boolean = false,
    ) {
        val client = apiClient ?: return
        if (activeRuns.containsKey(session.scopedId)) return
        if (session.scopedId in uiState.stoppingSessionKeys) return showNotice("正在停止这段对话的上一轮，请稍后发送")
        if (uiState.sessionActionId == session.id || uiState.isProfileSwitching) {
            showNotice("会话正在更新，请稍后发送")
            return
        }
        val submittedPrompt = submittedPromptOverride ?: prompt.ifBlank { "请查看我发送的附件。" }
        val baseMessages = if (uiState.selectedSession?.scopedId == session.scopedId) uiState.messages
            else messageCache[session.scopedId].visibleConversationMessages()
        val userMessage = ChatMessage(
            role = MessageRole.USER,
            content = buildString {
                if (prompt.isNotBlank()) append(prompt)
                attachments.forEach {
                    if (isNotEmpty()) append('\n')
                    append("📎 ").append(it.name)
                }
            },
            images = attachments.mapNotNull { attachment ->
                attachment.dataUrl?.let { ChatImage(attachment.name, it, attachment.mimeType) }
            },
        )
        val run = SessionRun(
            session = session,
            submittedPrompt = submittedPrompt,
            originalPrompt = prompt,
            submittedAttachments = attachments,
            userMessageId = userMessage.id,
            baselineSignature = baseMessages.lastOrNull { it.role == MessageRole.ASSISTANT }?.recoverySignature().orEmpty(),
            councilMode = councilMode,
        )
        run.messages = baseMessages + userMessage + ChatMessage(role = MessageRole.ASSISTANT, content = "", isStreaming = true)
        activeRuns[session.scopedId] = run
        if (session.messageCount == 0 || session.title == "新会话") pendingTitleSessionIds += session.id
        configStore.saveActiveRunSnapshot(run.snapshot())
        val consumeDraft = !voiceTurn && configStore.readDraft(session.profile, session.id).trim() == prompt.trim()
        if (consumeDraft) configStore.clearDraft(session.profile, session.id)
        if (!voiceTurn && attachmentDrafts[session.scopedId] == attachments) attachmentDrafts.remove(session.scopedId)
        failedSends.remove(session.scopedId)
        val unread = uiState.unreadSessionIds - session.scopedId
        configStore.saveUnreadSessionIds(unread)
        val isVisible = isVisible(run)
        uiState = uiState.copy(
            draft = if (!voiceTurn && isVisible && uiState.draft.trim() == prompt.trim()) "" else uiState.draft,
            attachments = if (!voiceTurn && isVisible && uiState.attachments == attachments) emptyList() else uiState.attachments,
            failedSend = if (isVisible) null else uiState.failedSend,
            councilMode = if (!voiceTurn && isVisible) CouncilMode.OFF else uiState.councilMode,
            unreadSessionIds = unread,
            errorMessage = null,
            noticeMessage = null,
        )
        setRunMessages(run, run.messages)
        publishRuns()
        val controller = StreamController()
        run.controller = controller
        run.streamJob = viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val receive: (StreamEvent) -> Unit = { event -> viewModelScope.launch { handleStreamEvent(run, event) } }
                if (voiceTurn) client.streamVoiceMessage(controller, session, submittedPrompt, uiState.voicePreferences.fastReply,
                    onNotice = { message -> viewModelScope.launch { if (isVisible(run)) showNotice(message) } }, onEvent = receive)
                else client.streamMessage(controller, session, submittedPrompt, attachments, receive)
            }.onFailure { throwable ->
                if (!controller.isStopped() && !controller.wasDisconnected()) {
                    withContext(Dispatchers.Main) {
                        if (isActive(run)) handleStreamFailure(run, throwable)
                    }
                }
            }
        }
        startStreamWatchdog(run)
    }

    fun steerCurrentRun() {
        val client = apiClient ?: return
        val run = currentRun() ?: return
        val runtimeId = run.controller?.runtimeSessionId ?: return showNotice("Hermes 运行尚未就绪，请稍后再试")
        val text = uiState.draft.trim()
        if (text.isBlank() || run.isSteering) return
        if (uiState.attachments.isNotEmpty()) return showNotice("追加要求暂不支持附件；可改用排队发送")
        run.isSteering = true
        publishRuns()
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) { client.steerSession(runtimeId, text) } }
                .onSuccess {
                    // Do not clear another chat, or newer text typed while awaiting acknowledgement.
                    if (configStore.readDraft(run.session.profile, run.session.id) == text) {
                        configStore.clearDraft(run.session.profile, run.session.id)
                    }
                    if (isVisible(run) && uiState.draft.trim() == text) uiState = uiState.copy(draft = "")
                    run.touch("已收到追加要求")
                    uiState = uiState.copy(noticeMessage = "追加要求已送达 Hermes")
                }.onFailure(::handleFailure)
            run.isSteering = false
            publishRuns()
        }
    }

    fun queueCurrentMessage() {
        val run = currentRun() ?: return
        val prompt = uiState.draft.trim()
        val attachments = uiState.attachments
        if (prompt.isBlank() && attachments.isEmpty()) return
        val replacing = run.queued != null
        run.queued = QueuedRunMessage(run.session, prompt.ifBlank { "请查看我发送的附件。" }, attachments)
        configStore.clearDraft(run.session.profile, run.session.id)
        attachmentDrafts.remove(run.session.scopedId)
        uiState = uiState.copy(draft = "", attachments = emptyList(),
            noticeMessage = if (replacing) "已替换这段对话的排队消息" else "消息已排队，将在这段对话本轮完成后发送")
        publishRuns()
    }

    fun cancelQueuedMessage() {
        val run = currentRun() ?: return
        run.queued = null
        publishRuns()
        uiState = uiState.copy(noticeMessage = "已取消这段对话的排队消息")
    }

    fun respondToAgentRequest(request: AgentRequest, answer: String) {
        val client = apiClient ?: return
        if (answer.isBlank() || request.isResponding) return
        uiState = uiState.copy(
            pendingAgentRequests = uiState.pendingAgentRequests.map {
                if (it.requestId == request.requestId && it.runtimeSessionId == request.runtimeSessionId) it.copy(isResponding = true) else it
            },
            errorMessage = null,
        )
        configStore.savePendingAgentRequests(uiState.pendingAgentRequests)
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) { client.respondAgentRequest(request, answer) } }
                .onSuccess {
                    uiState = uiState.copy(
                        pendingAgentRequests = uiState.pendingAgentRequests.filterNot { it.requestId == request.requestId && it.runtimeSessionId == request.runtimeSessionId },
                        noticeMessage = "已提交给 Hermes",
                    )
                    configStore.savePendingAgentRequests(uiState.pendingAgentRequests)
                    resumeRecoveryAfterAgentResponse(request)
                }
                .onFailure { throwable ->
                    uiState = uiState.copy(
                        pendingAgentRequests = uiState.pendingAgentRequests.map {
                            if (it.requestId == request.requestId && it.runtimeSessionId == request.runtimeSessionId) it.copy(isResponding = false) else it
                        },
                    )
                    configStore.savePendingAgentRequests(uiState.pendingAgentRequests)
                    handleFailure(throwable)
                }
        }
    }

    fun retryFailedMessage() {
        if (currentRun() != null) return
        val failure = uiState.failedSend ?: return
        val session = uiState.selectedSession ?: return
        startMessage(session, failure.prompt, failure.attachments)
    }

    fun stopGeneration() {
        val run = currentRun() ?: focusedRun().takeIf { uiState.selectedSession == null } ?: return
        stopRun(run)
    }

    fun stopActiveRun() {
        focusedRun()?.let(::stopRun)
    }

    fun stopSessionRun(session: HermesSession) {
        activeRuns[session.scopedId]?.let(::stopRun)
    }

    private fun stopRun(run: SessionRun) {
        if (!isActive(run)) return
        flushStreamingDelta(run)
        val controller = run.controller
        controller?.stop()
        run.streamJob?.cancel()
        restoreQueuedDraft(run)
        val stopped = run.messages.filter { !it.isStreaming || it.content.isNotBlank() || it.images.isNotEmpty() }
            .map { it.copy(isStreaming = false) }
        setRunMessages(run, stopped)
        removeRunRequests(run)
        uiState = uiState.copy(stoppingSessionKeys = uiState.stoppingSessionKeys + run.session.scopedId)
        removeRun(run)
        uiState = uiState.copy(noticeMessage = "已请求停止这段对话")
        if (voiceIsCurrent() && isVisible(run)) interruptVoicePlayback()
        val client = apiClient
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val runtimeId = controller?.runtimeSessionId ?: run.session.runtimeId
                    if (runtimeId != null) client?.stopRun(runtimeId, run.session.profile)
                }
                // A submit already in flight also sends its interrupt after acknowledgement.
                // Wait for it before accepting the next turn in this same conversation.
                run.streamJob?.join()
            } finally {
                uiState = uiState.copy(stoppingSessionKeys = uiState.stoppingSessionKeys - run.session.scopedId)
            }
        }
    }

    private fun stopAllRuns() {
        activeRuns.values.toList().forEach(::stopRun)
    }

    fun backToSessions() {
        saveCurrentChatDraft()
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
        publishRuns()
        if (returnRoute == AppRoute.TASKS) refreshTasks() else refreshSessions()
    }

    fun showHome() {
        invalidateWorkspaceAttachmentPicker()
        saveCurrentChatDraft()
        uiState = uiState.copy(route = AppRoute.HOME, errorMessage = null, noticeMessage = null)
        refreshSessions()
    }

    fun showSessions() {
        invalidateWorkspaceAttachmentPicker()
        if (uiState.route == AppRoute.SESSIONS) return
        uiState = uiState.copy(route = AppRoute.SESSIONS, errorMessage = null, noticeMessage = null)
        refreshSessions()
    }

    fun openActiveRun() {
        val session = focusedRun()?.session ?: return
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
        searchReturnRoute = if (uiState.route == AppRoute.HOME) AppRoute.HOME else AppRoute.SESSIONS
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
            route = searchReturnRoute,
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

    private var workspaceRequestVersion = 0L
    private var workspaceContextKey: String? = null
    private var workspaceChatRoot: String? = null

    private fun invalidateWorkspace() {
        invalidateWorkspaceAttachmentPicker()
        workspaceRequestVersion++
        workspaceContextKey = null
        workspaceChatRoot = null
        uiState = uiState.copy(workspaceListing=null,workspaceRootPath=null,workspaceDocument=null,
            workspaceDocumentOrigin=null,workspaceSourceArtifact=null,workspaceDraft="",isWorkspaceEditing=false,
            isWorkspaceLoading=false,isWorkspaceSaving=false,projectPickerListing=null,isProjectPickerLoading=false)
    }

    fun showWorkspace() {
        invalidateWorkspaceAttachmentPicker()
        val fromChat = uiState.route == AppRoute.CHAT
        workspaceChatRoot = if(fromChat) uiState.selectedSession
            ?.takeIf { it.profile == uiState.activeProfile }?.workspacePath?.takeIf(String::isNotBlank) else null
        val desiredRoot = selectedWorkspaceRoot()
        val context = "${uiState.activeProfile}::${desiredRoot.orEmpty()}"
        val reset = workspaceContextKey != context || uiState.workspaceListing == null
        uiState = uiState.copy(route=AppRoute.WORKSPACE, workspaceDocument=null,
            workspaceDocumentOrigin=null,workspaceSourceArtifact=null,errorMessage=null,noticeMessage=null)
        if(reset) {
            workspaceContextKey = context
            uiState=uiState.copy(workspaceListing=null,workspaceRootPath=null)
            refreshWorkspace(resetToRoot=true)
        }
        indexRecentArtifacts(force=false)
    }

    private var workspaceAttachmentRequest = 0L

    private fun invalidateWorkspaceAttachmentPicker() {
        workspaceAttachmentRequest++
        uiState = uiState.copy(workspaceAttachmentTarget = null, isWorkspaceAttaching = false)
    }

    fun showWorkspaceAttachmentPicker() {
        val target = uiState.selectedSession ?: return
        if (uiState.route != AppRoute.CHAT || target.profile != uiState.activeProfile) return
        saveCurrentChatDraft()
        showWorkspace()
        uiState = uiState.copy(workspaceAttachmentTarget = target)
    }

    fun cancelWorkspaceAttachmentPicker() {
        val target = uiState.workspaceAttachmentTarget ?: return
        invalidateWorkspaceAttachmentPicker()
        workspaceRequestVersion++
        uiState = uiState.copy(isWorkspaceLoading = false, errorMessage = null, noticeMessage = null)
        if (uiState.selectedSession?.scopedId == target.scopedId && uiState.activeProfile == target.profile) {
            uiState = uiState.copy(route = AppRoute.CHAT)
        }
    }

    fun attachWorkspaceFile(entry: com.qingyu.hermescompanion.model.WorkspaceEntry) {
        val target = uiState.workspaceAttachmentTarget ?: return
        if (entry.isDirectory) return
        val client = apiClient ?: return
        prepareWorkspaceAttachment { client.readWorkspaceDocumentForProfile(entry.path, target.profile) }
    }

    fun attachRecentArtifact(item: RecentArtifact) {
        val target = uiState.workspaceAttachmentTarget ?: return
        if (item.profile != target.profile) return showNotice("请选择当前档案内的文件")
        val load = artifactLoader(item) ?: return
        prepareWorkspaceAttachment(load)
    }

    private fun prepareWorkspaceAttachment(load: () -> WorkspaceDocument) {
        val target = uiState.workspaceAttachmentTarget ?: return
        if (uiState.isWorkspaceAttaching) return
        if (uiState.attachments.size >= MAX_ATTACHMENTS) return showNotice("单次最多添加 $MAX_ATTACHMENTS 个附件")
        val version = ++workspaceAttachmentRequest
        uiState = uiState.copy(isWorkspaceAttaching = true, errorMessage = null, noticeMessage = null)
        fun isCurrent() = version == workspaceAttachmentRequest && uiState.route == AppRoute.WORKSPACE &&
            uiState.workspaceAttachmentTarget?.scopedId == target.scopedId &&
            uiState.selectedSession?.scopedId == target.scopedId && uiState.activeProfile == target.profile
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) { AttachmentReader.fromWorkspaceDocument(load()) } }
                .onSuccess { attachment ->
                    if (!isCurrent()) return@onSuccess
                    if (uiState.attachments.size >= MAX_ATTACHMENTS) {
                        uiState = uiState.copy(isWorkspaceAttaching = false)
                        showNotice("单次最多添加 $MAX_ATTACHMENTS 个附件")
                        return@onSuccess
                    }
                    val merged = uiState.attachments + attachment
                    attachmentDrafts[target.scopedId] = merged
                    workspaceRequestVersion++
                    invalidateWorkspaceAttachmentPicker()
                    uiState = uiState.copy(route = AppRoute.CHAT, attachments = merged, isWorkspaceLoading = false,
                        workspaceDocument = null, noticeMessage = "已添加 ${attachment.name}")
                }.onFailure {
                    if (isCurrent()) {
                        uiState = uiState.copy(isWorkspaceAttaching = false)
                        handleFileFailure(it)
                    }
                }
        }
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

    private fun selectedWorkspaceRoot(): String? = workspaceChatRoot
        ?: uiState.projects.firstOrNull { it.id == uiState.selectedProjectId }?.primaryPath?.takeIf(String::isNotBlank)

    private fun workspaceRequestIsCurrent(version: Long, profile: String): Boolean =
        version == workspaceRequestVersion && profile == uiState.activeProfile

    fun refreshWorkspace(resetToRoot: Boolean = false) {
        val client = apiClient ?: return
        val existing = uiState.workspaceListing
        val desiredRoot = selectedWorkspaceRoot()
        if (uiState.selectedProjectId != null && desiredRoot == null) {
            showError("所选项目已不可用，请重新选择项目")
            return
        }
        val profile=uiState.activeProfile
        val version=++workspaceRequestVersion
        val root=uiState.workspaceRootPath
        uiState=uiState.copy(isWorkspaceLoading=true,errorMessage=null)
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) {
                if(resetToRoot || existing==null) {
                    if(desiredRoot!=null) client.listWorkspaceForProfile(desiredRoot,profile)
                    else client.initialWorkspaceForProfile(profile)
                } else client.listWorkspaceForProfile(existing.path,profile).copy(projectName=existing.projectName)
            } }.onSuccess { listing ->
                if(!workspaceRequestIsCurrent(version,profile)) return@onSuccess
                val expected = if(resetToRoot || existing==null) desiredRoot else existing.path
                if(expected!=null && listing.path.trimEnd('/')!=expected.trimEnd('/')) {
                    uiState=uiState.copy(isWorkspaceLoading=false)
                    showError("服务器返回了其他目录，请检查当前 Profile 的工作目录设置")
                    return@onSuccess
                }
                uiState=uiState.copy(workspaceListing=listing,
                    workspaceRootPath=if(resetToRoot || root==null) listing.path else root,
                    workspaceDocument=null,workspaceDocumentOrigin=null,workspaceSourceArtifact=null,
                    workspaceDraft="",isWorkspaceEditing=false,isWorkspaceLoading=false)
            }.onFailure { if(workspaceRequestIsCurrent(version,profile)) handleFileFailure(it) }
        }
    }

    fun openWorkspaceDirectory(path: String) {
        val client=apiClient ?: return
        val current=uiState.workspaceListing ?: return
        val root=uiState.workspaceRootPath ?: current.path
        if(!pathIsWithin(root,path)) { showError("不能离开当前 Hermes 项目目录"); return }
        val profile=uiState.activeProfile
        val version=++workspaceRequestVersion
        uiState=uiState.copy(isWorkspaceLoading=true,errorMessage=null)
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) { client.listWorkspaceForProfile(path,profile) } }
                .onSuccess { listing ->
                    if(!workspaceRequestIsCurrent(version,profile)) return@onSuccess
                    if(listing.path.trimEnd('/')!=path.trimEnd('/') || !pathIsWithin(root,listing.path)) {
                        uiState=uiState.copy(isWorkspaceLoading=false)
                        showError("服务器返回了其他目录，已保留当前项目位置")
                    } else uiState=uiState.copy(workspaceListing=listing.copy(projectName=current.projectName),isWorkspaceLoading=false)
                }.onFailure { if(workspaceRequestIsCurrent(version,profile)) handleFileFailure(it) }
        }
    }

    fun openWorkspaceDocument(path: String) {
        val client=apiClient ?: return
        val profile=uiState.activeProfile
        val version=++workspaceRequestVersion
        uiState=uiState.copy(isWorkspaceLoading=true,errorMessage=null)
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) { client.readWorkspaceDocumentForProfile(path,profile) } }
                .onSuccess { document ->
                    if(!workspaceRequestIsCurrent(version,profile)) return@onSuccess
                    uiState=uiState.copy(workspaceDocument=document,workspaceDocumentOrigin=null,
                        workspaceSourceArtifact=null,workspaceDraft=document.content,isWorkspaceEditing=false,isWorkspaceLoading=false)
                }.onFailure { if(workspaceRequestIsCurrent(version,profile)) handleFileFailure(it) }
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
        workspaceRequestVersion++
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
        val profile=uiState.activeProfile
        val version=++workspaceRequestVersion
        val draft=uiState.workspaceDraft
        uiState = uiState.copy(isWorkspaceSaving = true, errorMessage = null)
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    client.saveWorkspaceDocumentForProfile(document.path, draft, profile)
                }
            }.onSuccess { saved ->
                if(!workspaceRequestIsCurrent(version,profile)) return@onSuccess
                uiState = uiState.copy(
                    workspaceDocument = saved,
                    workspaceDraft = saved.content,
                    isWorkspaceEditing = false,
                    isWorkspaceSaving = false,
                    noticeMessage = "文档已保存到 Hermes 工作区",
                )
            }.onFailure { if(workspaceRequestIsCurrent(version,profile)) handleFailure(it) }
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
        invalidateWorkspaceAttachmentPicker()
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
        invalidateWorkspaceAttachmentPicker()
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
                    val profile = updateAvatarUri(uiState.userProfile, target, privateUri)
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
            val profile = updateAvatarUri(uiState.userProfile, target, "")
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
        if (target == VoiceCaptureTarget.CHAT_INPUT && currentRun() != null) {
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

    private fun voiceIsCurrent(epoch: Long = voiceEpoch): Boolean = epoch == voiceEpoch &&
        uiState.route == AppRoute.VOICE_CHAT && uiState.voiceConversation.active &&
        uiState.selectedSession?.scopedId == voiceSessionKey

    fun openVoiceConversation() {
        if (!uiState.voicePreferences.enabled) return showNotice("先在‘我的 → 语音’中启用语音功能")
        val session = uiState.selectedSession ?: return showNotice("先打开一个对话")
        voiceEpoch++
        voiceSessionKey = session.scopedId
        voiceRecorder.cancel()
        voiceLevelJob?.cancel()
        voiceCaptureJob?.cancel()
        voicePlaybackJob?.cancel()
        voicePlayback.stop()
        voiceReturnRoute = uiState.route
        uiState = uiState.copy(route = AppRoute.VOICE_CHAT, voiceCapture = VoiceCaptureState(),
            voiceConversation = VoiceConversationState(active = true,
                phase = if (currentRun() != null) VoicePhase.THINKING else VoicePhase.IDLE,
                message = if (currentRun() != null) "Hermes 正在处理当前问题" else "点按开始，说完停顿后自动发送"), errorMessage = null)
    }

    fun closeVoiceConversation() {
        voiceEpoch++
        voiceSessionKey = null
        voiceCaptureJob?.cancel(); voiceCaptureJob = null
        voiceRecorder.cancel()
        voiceLevelJob?.cancel(); voiceLevelJob = null
        voicePlaybackJob?.cancel(); voicePlaybackJob = null
        voicePlayback.stop()
        uiState = uiState.copy(route = voiceReturnRoute.takeIf { it == AppRoute.CHAT } ?: AppRoute.CHAT,
            voiceConversation = VoiceConversationState())
    }

    fun startVoiceListening() {
        if (!voiceIsCurrent() || currentRun() != null || uiState.voiceConversation.phase == VoicePhase.LISTENING) return
        voicePlaybackJob?.cancel(); voicePlaybackJob = null
        voicePlayback.stop()
        runCatching { voiceRecorder.start() }.onSuccess {
            uiState = uiState.copy(voiceConversation = uiState.voiceConversation.copy(phase = VoicePhase.LISTENING,
                transcript = "", provider = "", message = "正在适应环境声音，可以直接说话", requiresAgentUpdate = false, inputLevel = 0f))
            voiceLevelJob?.cancel()
            val epoch = voiceEpoch
            val endpoint = com.qingyu.hermescompanion.data.VoiceSilenceDetector(sensitivity = uiState.voicePreferences.noiseSensitivity)
            val started = android.os.SystemClock.elapsedRealtime()
            voiceLevelJob = viewModelScope.launch {
                while (voiceIsCurrent(epoch) && uiState.voiceConversation.phase == VoicePhase.LISTENING) {
                    val sample = voiceRecorder.inputSample()
                    val now = android.os.SystemClock.elapsedRealtime()
                    endpoint.sensitivity = uiState.voicePreferences.noiseSensitivity
                    val shouldSend = endpoint.sampleDb(sample.dbFs, now)
                    uiState = uiState.copy(voiceConversation = uiState.voiceConversation.copy(inputLevel = sample.displayLevel,
                        message = when {
                            endpoint.isCalibrating -> "正在适应环境声音，可以直接说话"
                            endpoint.isSpeaking -> "正在听你说，说完停顿后自动发送"
                            endpoint.hasSpeech -> "停顿中，即将自动发送…"
                            else -> "正在听，靠近手机自然说话即可"
                        }))
                    if (shouldSend) {
                        voiceLevelJob = null // stopVoiceListening must not cancel its own caller.
                        stopVoiceListening()
                        break
                    }
                    if (now - started > 90_000) { voiceLevelJob = null; cancelVoiceListening(); break }
                    delay(72)
                }
            }
        }.onFailure { error ->
            uiState = uiState.copy(voiceConversation = uiState.voiceConversation.copy(phase = VoicePhase.ERROR,
                message = error.message ?: "无法启动麦克风"))
        }
    }

    fun cancelVoiceListening() {
        voiceRecorder.cancel()
        voiceLevelJob?.cancel(); voiceLevelJob = null
        voiceCaptureJob?.cancel(); voiceCaptureJob = null
        if (!voiceIsCurrent()) return
        uiState = uiState.copy(voiceConversation = uiState.voiceConversation.copy(phase = VoicePhase.IDLE,
            message = "已暂停，点按重新说话", inputLevel = 0f))
    }

    fun stopVoiceListening() {
        val client = apiClient ?: return
        if (!voiceIsCurrent() || uiState.voiceConversation.phase != VoicePhase.LISTENING) return
        voiceLevelJob?.cancel(); voiceLevelJob = null
        val epoch = voiceEpoch
        val profile = uiState.activeProfile
        uiState = uiState.copy(voiceConversation = uiState.voiceConversation.copy(phase = VoicePhase.TRANSCRIBING,
            message = "正在识别语音", inputLevel = 0f))
        voiceCaptureJob = viewModelScope.launch {
            try {
                // Release the recorder before suspension so close/reopen cannot stop a newer capture.
                val (bytes, mime) = voiceRecorder.stop()
                val result = withContext(Dispatchers.IO) { client.transcribeAudio(bytes, mime, profile) }
                if (!voiceIsCurrent(epoch)) return@launch
                uiState = uiState.copy(voiceConversation = uiState.voiceConversation.copy(provider = result.provider,
                    agentSttAvailable = true, requiresAgentUpdate = false))
                submitVoiceConversationText(result.transcript)
            } catch (error: kotlinx.coroutines.CancellationException) { throw error }
            catch (error: Exception) {
                if (!voiceIsCurrent(epoch)) return@launch
                val root = unwrapFailure(error)
                val unavailable = root is ApiException && root.statusCode in setOf(404, 405, 501)
                val incompatible = isAgentSttCompatibilityFailure(root)
                uiState = uiState.copy(voiceConversation = uiState.voiceConversation.copy(phase = VoicePhase.ERROR,
                    message = if (incompatible) agentSttCompatibilityMessage() else if (unavailable) "当前 Agent 未启用语音识别，可改用手机系统识别" else diagnosticFailure(root),
                    agentSttAvailable = if (unavailable) false else uiState.voiceConversation.agentSttAvailable,
                    requiresAgentUpdate = incompatible))
            }
        }
    }

    fun submitVoiceConversationText(text: String) {
        if (!voiceIsCurrent()) return
        val session = uiState.selectedSession ?: return
        val transcript = normalizeVoiceTranscript(text, uiState.voicePreferences.transcriptScript).trim()
        if (transcript.isBlank() || currentRun() != null) return
        if (uiState.isModelSwitching) return showNotice("模型正在切换，稍后再说")
        uiState = uiState.copy(voiceConversation = uiState.voiceConversation.copy(phase = VoicePhase.THINKING,
            transcript = transcript, message = "正在生成回答"))
        startMessage(session, transcript, emptyList(), voiceTurn = true)
    }

    fun interruptVoicePlayback() {
        voicePlaybackJob?.cancel(); voicePlaybackJob = null
        voicePlayback.stop()
        if (voiceIsCurrent()) uiState = uiState.copy(voiceConversation = uiState.voiceConversation.copy(
            phase = VoicePhase.IDLE, message = "已停止播放，点按继续说话"))
    }

    private fun requestNextVoiceTurn() {
        if (!voiceIsCurrent()) return
        val continuous = uiState.voicePreferences.continuous
        uiState = uiState.copy(voiceConversation = uiState.voiceConversation.copy(phase = VoicePhase.IDLE,
            message = if (continuous) "准备聆听" else "回答完毕，点按继续",
            listenRequest = uiState.voiceConversation.listenRequest + if (continuous) 1 else 0))
    }

    private fun speakVoiceReply(text: String) {
        if (!voiceIsCurrent()) return
        val spoken = com.qingyu.hermescompanion.data.spokenReply(text)
        if (spoken.isBlank() || !uiState.voicePreferences.autoRead) { requestNextVoiceTurn(); return }
        val client = apiClient ?: return
        val epoch = voiceEpoch
        val profile = uiState.activeProfile
        val preferences = uiState.voicePreferences
        voicePlaybackJob?.cancel()
        voicePlaybackJob = viewModelScope.launch {
            try {
                uiState = uiState.copy(voiceConversation = uiState.voiceConversation.copy(phase = VoicePhase.SPEAKING,
                    message = "Hermes 正在回答"))
                val chinese = com.qingyu.hermescompanion.data.containsChinese(spoken)
                suspend fun phone() {
                    if (!voiceIsCurrent(epoch)) throw kotlinx.coroutines.CancellationException()
                    uiState = uiState.copy(voiceConversation = uiState.voiceConversation.copy(provider = "手机中文语音".takeIf { chinese } ?: "Android TTS"))
                    voicePlayback.speakSystem(spoken, preferences.language, preferences.speechRate)
                }
                suspend fun agent() {
                    if (chinese) {
                        val config = withContext(Dispatchers.IO) { client.voiceSettings(profile).tts }
                        if (!com.qingyu.hermescompanion.data.agentVoiceSupportsChinese(config)) {
                            throw IllegalStateException("Agent 当前发音人不支持中文，在语音设置中选择中文发音人后重试")
                        }
                    }
                    for (chunk in com.qingyu.hermescompanion.data.speechChunks(spoken)) {
                        val audio = withContext(Dispatchers.IO) { client.synthesizeSpeech(chunk, profile) }
                        if (!voiceIsCurrent(epoch)) throw kotlinx.coroutines.CancellationException()
                        uiState = uiState.copy(voiceConversation = uiState.voiceConversation.copy(provider = audio.provider, agentTtsAvailable = true))
                        voicePlayback.play(audio)
                    }
                }
                // An English-only engine may return valid audio containing only digits/English.
                // For Chinese in automatic mode, first use a verified Chinese phone voice.
                if (preferences.engine == "system") phone()
                else if (chinese && preferences.engine == "automatic") {
                    try { phone() } catch (e: kotlinx.coroutines.CancellationException) { throw e }
                    catch (_: Exception) { agent() }
                } else {
                    try { agent() } catch (e: kotlinx.coroutines.CancellationException) { throw e }
                    catch (_: Exception) { phone() }
                }
                if (!voiceIsCurrent(epoch)) return@launch
                delay(350)
                voicePlaybackJob = null
                requestNextVoiceTurn()
            } catch (error: kotlinx.coroutines.CancellationException) { throw error }
            catch (error: Exception) {
                if (voiceIsCurrent(epoch)) uiState = uiState.copy(voiceConversation = uiState.voiceConversation.copy(
                    phase = VoicePhase.ERROR, message = error.message ?: "朗读失败，回答已保留在对话中"))
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
        if (uiState.isStreaming) stopAllRuns()
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
        if (uiState.isStreaming) stopAllRuns()
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
        attachmentDrafts.clear()
        failedSends.clear()
        recoveredProfiles.clear()
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

    private fun handleStreamEvent(run: SessionRun, event: StreamEvent) {
        // A stopped/completed turn may still have callbacks queued on the main thread.
        if (!isActive(run) || run.recovering) return
        when (event) {
            is StreamEvent.RunStarted -> {
                run.session = run.session.copy(runtimeId = event.runId)
                updateSession(run.session.id) { it.copy(runtimeId = event.runId) }
                run.touch("Hermes 正在执行")
            }
            is StreamEvent.ReasoningDelta -> {
                updateStreamingReasoning(run) { it + event.text }
                run.touch("Hermes 正在思考")
            }
            is StreamEvent.ReasoningAvailable -> {
                updateStreamingReasoning(run) { it.ifBlank { event.text } }
                run.touch()
            }
            is StreamEvent.AssistantDelta -> enqueueStreamingDelta(run, event.text)
            is StreamEvent.AssistantInterim -> {
                flushStreamingDelta(run)
                updateStreamingMessage(run) { mergeInterimAssistantText(it, event.content) }
                run.touch("Hermes 正在处理")
            }
            is StreamEvent.AssistantCompleted -> {
                flushStreamingDelta(run)
                if (event.content.isNotBlank()) {
                    updateStreamingMessage(run) { mergeCompletedAssistantText(it, event.content, event.responsePreviewed) }
                }
                run.touch()
            }
            is StreamEvent.ToolStarted -> {
                flushStreamingDelta(run)
                val name = councilToolName(event.name)
                run.touch("正在使用 $name")
                updateTool(run, name, event.preview, ToolStatus.RUNNING, event.todos)
            }
            is StreamEvent.ToolProgress -> {
                val name = councilToolName(event.name)
                run.touch(event.preview.ifBlank { "正在使用 $name" }.take(80))
                updateTool(run, name, event.preview, ToolStatus.RUNNING)
            }
            is StreamEvent.ToolCompleted -> {
                flushStreamingDelta(run)
                val name = councilToolName(event.name)
                run.touch("$name 已完成")
                updateTool(run, name, event.preview, ToolStatus.COMPLETED, event.todos)
            }
            is StreamEvent.ToolFailed -> {
                val name = councilToolName(event.name)
                run.touch("$name 执行失败")
                updateTool(run, name, event.preview, ToolStatus.FAILED)
            }
            is StreamEvent.AgentRequestPending -> {
                val request = event.request.copy(conversationId = run.session.id)
                uiState = uiState.copy(pendingAgentRequests = uiState.pendingAgentRequests.filterNot {
                    it.runtimeSessionId == request.runtimeSessionId && it.requestId == request.requestId
                } + request)
                configStore.savePendingAgentRequests(uiState.pendingAgentRequests)
                run.touch("等待你的处理")
                val action = if (request.type == AgentRequestType.APPROVAL) "需要确认一项操作" else "需要你补充信息"
                HermesNotifications.showAgentRequest(getApplication(), "Hermes $action", request.title,
                    profile = run.session.profile, sessionId = run.session.id)
            }
            is StreamEvent.AgentRequestExpired -> {
                uiState = uiState.copy(pendingAgentRequests = uiState.pendingAgentRequests.filterNot {
                    it.conversationId == run.session.id && it.requestId == event.requestId
                })
                configStore.savePendingAgentRequests(uiState.pendingAgentRequests)
                run.touch("请求已过期，Hermes 正在继续")
            }
            is StreamEvent.ConnectionInterrupted -> {
                flushStreamingDelta(run)
                recoverInterruptedStream(run, event.message)
            }
            is StreamEvent.Error -> handleStreamFailure(run, IllegalStateException(event.message))
            StreamEvent.Completed -> {
                flushStreamingDelta(run)
                finishStreaming(run)
            }
        }
        publishRuns()
    }

    private fun enqueueStreamingDelta(run: SessionRun, text: String) {
        if (text.isEmpty()) return
        run.touch("正在组织回复")
        run.deltaBuffer.append(text)
        if (run.deltaFlushJob?.isActive == true) return
        run.deltaFlushJob = viewModelScope.launch {
            delay(STREAM_DELTA_FRAME_MILLIS)
            run.deltaFlushJob = null
            if (isActive(run)) flushStreamingDelta(run)
        }
    }

    private fun flushStreamingDelta(run: SessionRun) {
        if (run.deltaBuffer.isEmpty()) return
        val text = run.deltaBuffer.toString()
        run.deltaBuffer.clear()
        updateStreamingMessage(run) { it + text }
    }

    private fun updateStreamingMessage(run: SessionRun, transform: (String) -> String) {
        setRunMessages(run, run.messages.map { if (it.isStreaming) it.copy(content = transform(it.content)) else it })
    }

    private fun updateStreamingReasoning(run: SessionRun, transform: (String) -> String) {
        setRunMessages(run, run.messages.map { if (it.isStreaming) it.copy(reasoning = transform(it.reasoning)) else it })
    }

    private fun updateTool(run: SessionRun, name: String, preview: String, status: ToolStatus, todos: List<ChatTodo> = emptyList()) {
        val existing = run.tools.indexOfLast { it.name == name && it.status == ToolStatus.RUNNING }
        val updated = run.tools.toMutableList()
        if (existing >= 0) updated[existing] = updated[existing].copy(preview = preview.ifBlank { updated[existing].preview }, status = status)
        else updated += ToolActivity(name = name, preview = preview, status = status)
        run.tools = updated
        if (todos.isNotEmpty()) run.todos = todos
        run.artifacts = (run.artifacts + ChatInsightParser.artifactsFromText(preview)).distinctBy(ChatArtifact::path)
    }

    private fun finishStreaming(run: SessionRun) {
        if (!isActive(run)) return
        val session = run.session
        val completed = run.messages.filter { !it.isStreaming || it.content.isNotBlank() || it.images.isNotEmpty() }
            .map { it.copy(isStreaming = false) }
        setRunMessages(run, completed)
        val reply = completed.lastOrNull { it.role == MessageRole.ASSISTANT }
        val text = reply?.content.orEmpty().replace(Regex("\\s+"), " ").trim()
        val hasReply = text.isNotBlank() || reply?.images?.isNotEmpty() == true
        rememberRecentArtifacts(session, completed, run.artifacts)
        val needsAttention = hasReply && replyNeedsAttention(uiState.route, uiState.selectedSession?.id, session.id, isAppInForeground())
        val unread = if (needsAttention) uiState.unreadSessionIds + session.scopedId else uiState.unreadSessionIds
        configStore.saveUnreadSessionIds(unread)
        val completion = RunCompletionSummary(sessionId = session.id, title = session.title.ifBlank { "Hermes 已完成" },
            summary = text.take(240).ifBlank { "本轮执行已完成" }, artifacts = run.artifacts)
        val recent = (listOf(completion) + uiState.recentCompletions).take(29)
        val queued = run.queued
        uiState = uiState.copy(
            latestCompletion = completion, recentCompletions = recent, unreadSessionIds = unread,
            toolActivities = if (isVisible(run)) run.tools else uiState.toolActivities,
            chatArtifacts = if (isVisible(run)) run.artifacts else uiState.chatArtifacts,
            chatTodos = if (isVisible(run)) run.todos else uiState.chatTodos,
            sessions = uiState.sessions.map {
                if (it.scopedId == session.scopedId) it.copy(preview = text.ifBlank { "Hermes 已发送图片" },
                    messageCount = maxOf(it.messageCount + 2, completed.size), runtimeId = session.runtimeId) else it
            },
        )
        configStore.saveRecentCompletions(recent)
        updateSession(session.id) { it.copy(messageCount = maxOf(it.messageCount, completed.size), runtimeId = session.runtimeId) }
        removeRunRequests(run)
        // Voice playback belongs to the conversation being viewed, never a background completion.
        if (hasReply && isVisible(run)) speakVoiceReply(reply?.content.orEmpty())
        if (needsAttention) {
            val name = uiState.userProfile.hermesDisplayName.ifBlank { "Hermes" }
            HermesNotifications.showMessage(getApplication(), "$name 已回复",
                "${session.title} · ${text.take(120).ifBlank { "回复中包含图片" }}",
                profile = session.profile, sessionId = session.id, route = "chat")
        }
        scheduleTitleRefresh(session)
        removeRun(run)
        if (queued != null) startMessage(run.session, queued.prompt, queued.attachments)
        else consumePendingDeepLink()
    }

    private fun startStreamWatchdog(run: SessionRun) {
        run.watchdogJob?.cancel()
        run.watchdogJob = viewModelScope.launch {
            delay(STREAM_IDLE_POLL_AFTER_MILLIS)
            while (isActive(run)) {
                val pending = uiState.pendingAgentRequests.any { it.conversationId == run.session.id }
                val idleFor = System.currentTimeMillis() - run.lastActivityAtMillis
                if (!pending && !run.recovering && idleFor >= STREAM_IDLE_POLL_AFTER_MILLIS) {
                    val client = apiClient ?: return@launch
                    val result = runCatching { withContext(Dispatchers.IO) { client.loadLatestMessages(run.session) } }
                    if (!isActive(run)) return@launch
                    val failure = result.exceptionOrNull()?.let(::unwrapFailure)
                    if (failure is ApiException && failure.statusCode in setOf(401, 403)) {
                        handleStreamFailure(run, failure)
                        return@launch
                    }
                    // Preserve 3.0.3a's completion boundary: saved text alone cannot end a live run.
                    if (failure != null && idleFor >= STREAM_CONNECTION_STALE_MILLIS) {
                        recoverInterruptedStream(run, "实时连接暂时没有响应，正在自动取回结果")
                        return@launch
                    }
                }
                delay(STREAM_IDLE_POLL_INTERVAL_MILLIS)
            }
        }
    }

    private fun resumeRecoveryAfterAgentResponse(request: AgentRequest) {
        val key = "${uiState.activeProfile}::${request.conversationId}"
        var run = activeRuns[key]
        if (run != null) {
            run.touch("已处理，Hermes 正在继续")
            publishRuns()
            if (run.controller != null && run.recoveryJob?.isActive != true) return
            if (run.recoveryJob?.isActive == true) return
        } else {
            val session = uiState.sessions.firstOrNull { it.id == request.conversationId }
                ?: uiState.selectedSession?.takeIf { it.id == request.conversationId } ?: return
            val snapshot = configStore.readActiveRunSnapshots().firstOrNull { it.profile == session.profile && it.sessionId == session.id }
            run = restoredRun(session, snapshot)
            activeRuns[session.scopedId] = run
        }
        recoverInterruptedStream(run, "已提交处理结果，正在继续取回回复")
    }

    private fun recoverInterruptedStream(run: SessionRun, message: String) {
        if (!isActive(run) || run.recoveryJob?.isActive == true) return
        val client = apiClient ?: return finishInterruptedRecovery(run)
        run.watchdogJob?.cancel()
        run.controller?.stop()
        run.streamJob?.cancel()
        run.controller = null
        run.recovering = true
        run.touch("正在取回回复")
        run.tools = run.tools.map { if (it.status == ToolStatus.RUNNING) it.copy(status = ToolStatus.FAILED) else it }
        publishRuns()
        uiState = uiState.copy(noticeMessage = message)
        run.recoveryJob = viewModelScope.launch {
            // Reuse a healthy socket. Reconnecting each run would interrupt all its peers.
            runCatching { withContext(Dispatchers.IO) { client.ensureConnected() } }
            val waits = listOf(0L, 1_000L, 2_000L, 4_000L, 7_000L, 10_000L, 15_000L, 20_000L, 30_000L, 30_000L, 45_000L, 60_000L)
            for (wait in waits) {
                if (wait > 0) delay(wait)
                if (!isActive(run)) return@launch
                if (uiState.pendingAgentRequests.any { it.conversationId == run.session.id }) {
                    run.recoveryJob = null
                    run.recovering = false
                    run.touch("等待你的处理")
                    publishRuns()
                    return@launch
                }
                val result = runCatching { withContext(Dispatchers.IO) { client.loadLatestMessages(run.session) } }
                if (!isActive(run)) return@launch
                val failure = result.exceptionOrNull()?.let(::unwrapFailure)
                if (failure is ApiException && failure.statusCode in setOf(401, 403)) {
                    handleStreamFailure(run, failure)
                    return@launch
                }
                val messages = result.getOrNull() ?: continue
                if (findRecoveredAssistant(messages, run.submittedPrompt, run.baselineSignature) != null) {
                    val insights = ChatInsightParser.fromMessages(messages)
                    run.artifacts = insights.artifacts
                    run.todos = insights.todos
                    setRunMessages(run, messages.visibleConversationMessages())
                    uiState = uiState.copy(noticeMessage = "${run.session.title}：回复已同步")
                    finishStreaming(run)
                    return@launch
                }
            }
            finishInterruptedRecovery(run)
        }
    }

    private fun finishInterruptedRecovery(run: SessionRun) {
        if (!isActive(run)) return
        if (uiState.pendingAgentRequests.any { it.conversationId == run.session.id }) {
            run.recoveryJob = null
            run.recovering = false
            run.touch("等待你的处理")
            publishRuns()
            return
        }
        preserveFailedSend(run)
        restoreQueuedDraft(run)
        removeRun(run)
        uiState = uiState.copy(errorMessage = "${run.session.title}：未能自动取回完整回复，请稍后重新打开这段对话确认结果。")
    }

    private fun scheduleTitleRefresh(session: HermesSession) {
        val sessionId = session.id
        if (sessionId !in pendingTitleSessionIds) return
        val client = apiClient ?: return
        titleRefreshJobs.remove(session.scopedId)?.cancel()
        titleRefreshJobs[session.scopedId] = viewModelScope.launch {
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

    private fun currentRun(): SessionRun? = uiState.selectedSession?.scopedId?.let(activeRuns::get)

    private fun focusedRun(): SessionRun? = currentRun() ?: activeRuns.values.firstOrNull()

    private fun isActive(run: SessionRun): Boolean = activeRuns[run.session.scopedId] === run

    private fun isVisible(run: SessionRun): Boolean = uiState.selectedSession?.scopedId == run.session.scopedId

    private fun publishRuns() {
        val focused = focusedRun()
        val current = currentRun()
        uiState = uiState.copy(
            isStreaming = activeRuns.isNotEmpty(),
            runningSessions = activeRuns.values.map { it.session },
            runningRuns = activeRuns.values.map { RunUiState(it.session, it.stage, it.startedAtMillis, it.recovering) },
            streamingSessionId = focused?.session?.id,
            runStage = focused?.stage.orEmpty(),
            runStartedAtMillis = focused?.startedAtMillis ?: 0L,
            runLastActivityAtMillis = focused?.lastActivityAtMillis ?: 0L,
            activeCouncilMode = current?.councilMode ?: CouncilMode.OFF,
            isSteering = current?.isSteering ?: false,
            queuedRunMessage = current?.queued,
            isRecoveringConnection = focused?.recovering ?: false,
            toolActivities = current?.tools ?: uiState.toolActivities,
            chatArtifacts = current?.artifacts ?: uiState.chatArtifacts,
            chatTodos = current?.todos ?: uiState.chatTodos,
        )
    }

    private fun setRunMessages(run: SessionRun, messages: List<ChatMessage>) {
        run.messages = messages
        messageCache[run.session.scopedId] = messages
        trimMessageCache()
        if (isVisible(run)) uiState = uiState.copy(messages = messages)
    }

    private fun cacheMessages(sessionId: String, messages: List<ChatMessage>) {
        messageCache[cacheKey(sessionId)] = messages
        trimMessageCache()
    }

    private fun trimMessageCache() {
        // Never evict an active transcript; inactive history can be reloaded from the server.
        while (messageCache.size > 8) {
            val key = messageCache.keys.firstOrNull {
                it !in activeRuns && it != uiState.selectedSession?.scopedId
            } ?: break
            messageCache.remove(key)
        }
    }

    private fun saveCurrentChatDraft() {
        val session = uiState.selectedSession ?: return
        configStore.saveDraft(session.profile, session.id, uiState.draft)
        attachmentDrafts[session.scopedId] = uiState.attachments
    }

    private fun removeRunRequests(run: SessionRun) {
        uiState = uiState.copy(pendingAgentRequests = uiState.pendingAgentRequests.filterNot {
            it.conversationId == run.session.id
        })
        configStore.savePendingAgentRequests(uiState.pendingAgentRequests)
    }

    private fun removeRun(run: SessionRun) {
        if (!isActive(run)) return
        activeRuns.remove(run.session.scopedId)
        run.deltaFlushJob?.cancel()
        run.watchdogJob?.cancel()
        run.recoveryJob?.cancel()
        run.deltaBuffer.clear()
        configStore.clearActiveRunSnapshot(run.session.profile, run.session.id)
        publishRuns()
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
                val resolvedPath = resolveArtifactPath(artifact.path, session.workspacePath) ?: artifact.path
                RecentArtifact(
                    profile = session.profile,
                    sessionId = session.id,
                    sessionTitle = session.title.ifBlank { "Hermes 对话" },
                    messageId = message.id,
                    path = resolvedPath,
                    name = artifact.name,
                    kind = artifact.kind,
                    workspacePath = session.workspacePath,
                    sourcePath = artifact.path,
                    seenAtMillis = now,
                )
            }
        }
        val fallbackMessageId = visibleMessages.lastOrNull()?.id.orEmpty()
        val extras = additionalArtifacts.map { artifact ->
            val resolvedPath = resolveArtifactPath(artifact.path, session.workspacePath) ?: artifact.path
            RecentArtifact(
                profile = session.profile,
                sessionId = session.id,
                sessionTitle = session.title.ifBlank { "Hermes 对话" },
                messageId = fallbackMessageId,
                path = resolvedPath,
                name = artifact.name,
                kind = artifact.kind,
                workspacePath = session.workspacePath,
                sourcePath = artifact.path,
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
                ChatInsightParser.artifactsFromText(message.content).any {
                    resolveArtifactPath(it.path, session.workspacePath) == artifact.path
                }
        }
        return RecentArtifact(
            profile = session.profile,
            sessionId = session.id,
            sessionTitle = session.title.ifBlank { "Hermes 对话" },
            messageId = sourceMessage?.id.orEmpty(),
            path = artifact.path,
            name = artifact.name,
            kind = artifact.kind,
            workspacePath = session.workspacePath,
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
        "${uiState.activeProfile}::$sessionId"

    private fun markSessionRead(sessionId: String) {
        if (sessionId !in uiState.unreadSessionIds) return
        val unreadSessionIds = uiState.unreadSessionIds - sessionId
        configStore.saveUnreadSessionIds(unreadSessionIds)
        uiState = uiState.copy(unreadSessionIds = unreadSessionIds)
    }

    private fun restoreSavedRunIfNeeded(sessions: List<HermesSession>) {
        val profile = uiState.activeProfile
        if (!recoveredProfiles.add(profile)) return
        val snapshots = configStore.readActiveRunSnapshots().filter { it.profile == profile }
        snapshots.forEach { snapshot ->
            if (System.currentTimeMillis() - snapshot.startedAtMillis > 24 * 60 * 60 * 1_000L) {
                configStore.clearActiveRunSnapshot(snapshot.profile, snapshot.sessionId)
                return@forEach
            }
            val session = sessions.firstOrNull { it.id == snapshot.sessionId }
                ?: HermesSession(id = snapshot.sessionId, title = snapshot.title.ifBlank { "Hermes 对话" },
                    profile = profile, workspacePath = snapshot.workspacePath)
            if (activeRuns.containsKey(session.scopedId)) return@forEach
            val run = restoredRun(session, snapshot)
            activeRuns[session.scopedId] = run
            publishRuns()
            recoverInterruptedStream(run, "正在恢复上次运行的对话")
        }
    }

    private fun restoredRun(session: HermesSession, snapshot: ActiveRunSnapshot?): SessionRun = SessionRun(
        session = session,
        submittedPrompt = snapshot?.submittedPrompt.orEmpty(),
        originalPrompt = snapshot?.submittedPrompt.orEmpty(),
        baselineSignature = snapshot?.baselineAssistantSignature.orEmpty(),
        startedAtMillis = snapshot?.startedAtMillis ?: System.currentTimeMillis(),
    ).also { it.messages = messageCache[session.scopedId].visibleConversationMessages() }

    private fun handleStreamFailure(run: SessionRun, throwable: Throwable) {
        if (!isActive(run)) return
        flushStreamingDelta(run)
        run.controller?.stop()
        run.streamJob?.cancel()
        preserveFailedSend(run)
        restoreQueuedDraft(run)
        removeRunRequests(run)
        removeRun(run)
        if (voiceIsCurrent() && isVisible(run)) uiState = uiState.copy(voiceConversation = uiState.voiceConversation.copy(
            phase = VoicePhase.ERROR, message = "回复暂时失败，内容已保留，可回到对话重试"))
        handleFailure(throwable)
    }

    private fun preserveFailedSend(run: SessionRun) {
        val partialReply = run.messages.any { it.isStreaming && (it.content.isNotBlank() || it.images.isNotEmpty()) }
        val stopped = run.messages.mapNotNull { message ->
            when {
                !partialReply && message.id == run.userMessageId -> null
                !message.isStreaming -> message
                message.content.isNotBlank() || message.images.isNotEmpty() -> message.copy(isStreaming = false)
                else -> null
            }
        }
        setRunMessages(run, stopped)
        if (run.originalPrompt.isBlank() && run.submittedAttachments.isEmpty()) return
        val failure = FailedSend(run.originalPrompt, run.submittedAttachments)
        failedSends[run.session.scopedId] = failure
        // Keep anything the user has already typed for the next turn.
        val draft = configStore.readDraft(run.session.profile, run.session.id)
        if (draft.isBlank()) {
            configStore.saveDraft(run.session.profile, run.session.id, run.originalPrompt)
            attachmentDrafts[run.session.scopedId] = run.submittedAttachments
            if (isVisible(run) && uiState.draft.isBlank() && uiState.attachments.isEmpty()) {
                uiState = uiState.copy(draft = run.originalPrompt, attachments = run.submittedAttachments)
            }
        }
        if (isVisible(run)) uiState = uiState.copy(failedSend = failure)
    }

    private fun restoreQueuedDraft(run: SessionRun) {
        val queued = run.queued ?: return
        val existing = configStore.readDraft(run.session.profile, run.session.id)
        val draft = listOf(existing, queued.prompt).filter(String::isNotBlank).distinct().joinToString("\n\n")
        val attachments = (attachmentDrafts[run.session.scopedId].orEmpty() + queued.attachments).distinctBy { it.id }
        configStore.saveDraft(run.session.profile, run.session.id, draft)
        attachmentDrafts[run.session.scopedId] = attachments
        if (isVisible(run)) uiState = uiState.copy(draft = draft, attachments = attachments)
        run.queued = null
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

    private fun handleFileFailure(throwable: Throwable) {
        val root = unwrapFailure(throwable)
        if (root is ApiException && root.statusCode == 403) {
            uiState = uiState.copy(isWorkspaceLoading = false, isWorkspaceSaving = false,
                isWorkspaceAttaching = false, isImageLoading = false)
            showError("没有读取或操作该文件的权限，请检查当前档案的文件权限。${root.message.takeIf { it.isNotBlank() }?.let { "\n$it" }.orEmpty()}")
        } else {
            if (root is ApiException && root.statusCode == 401) invalidateWorkspaceAttachmentPicker()
            handleFailure(throwable)
        }
    }

    private fun handleFailure(throwable: Throwable) {
        val root = unwrapFailure(throwable)
        val message = when (root) {
            is ApiException -> when (root.statusCode) {
                401, 403 -> "登录已失效，或 Hermes 用户名/密码不正确"
                404 -> root.message.ifBlank { "当前 Hermes 版本不支持所需接口，请先升级 Hermes Agent" }
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
        if (target.replace('\\', '/').split('/').any { it == ".." }) return false
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
                        route = AppRoute.HOME,
                        isBusy = false,
                        noticeMessage = "已恢复登录：$signedInAs",
                    )
                    loadGatewayInfo(client)
                    loadProfilesAndSessions(client)
                }
                .onFailure { error ->
                    val root = unwrapFailure(error)
                    if (root is ApiException && root.statusCode in setOf(401, 403)) {
                        cookieJar.clear()
                        uiState = uiState.copy(
                            route = AppRoute.SETUP,
                            isBusy = false,
                            hasSavedConnection = false,
                            noticeMessage = "登录已过期，请重新输入密码",
                        )
                    } else {
                        uiState = uiState.copy(
                            route = AppRoute.SETUP,
                            isBusy = false,
                            hasSavedConnection = true,
                            noticeMessage = null,
                            errorMessage = "暂时无法连接已保存的远程网关，请确认服务器已启动且手机网络可访问该地址",
                        )
                    }
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
        activeRuns.values.forEach { run ->
            run.controller?.stop()
            run.streamJob?.cancel()
            run.recoveryJob?.cancel()
            run.watchdogJob?.cancel()
            run.deltaFlushJob?.cancel()
        }
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
private const val MAX_ATTACHMENTS = 10
private const val SEARCH_MESSAGE_LIMIT = 200
private const val RECENT_ARTIFACT_SESSION_LIMIT = 24
private const val RECENT_ARTIFACT_MESSAGE_LIMIT = 80
private const val STREAM_DELTA_FRAME_MILLIS = 50L
private const val STREAM_IDLE_POLL_AFTER_MILLIS = 45_000L
private const val STREAM_IDLE_POLL_INTERVAL_MILLIS = 30_000L
private const val STREAM_CONNECTION_STALE_MILLIS = 90_000L

internal fun artifactIndexFingerprint(session: HermesSession): String =
    listOf("paths-v2", session.workspacePath, session.updatedAt, session.messageCount.toString(), session.preview.hashCode().toString()).joinToString("|")

internal fun updateAvatarUri(
    profile: UserProfilePreferences,
    target: AvatarTarget,
    uri: String,
): UserProfilePreferences = when (target) {
    AvatarTarget.USER -> profile.copy(avatarUri = uri)
    AvatarTarget.HERMES -> profile.copy(hermesAvatarUri = uri)
}

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
    append('|')
    append(reasoning.trim())
    images.forEach { append('|').append(it.source) }
}

internal fun mergeInterimAssistantText(streamed: String, interim: String): String {
    val live = streamed.trim()
    val preview = interim.trim()
    if (live.isBlank()) return preview
    if (preview.isBlank()) return live
    val normalizedLive = live.normalizeStreamText()
    val normalizedPreview = preview.normalizeStreamText()
    return when {
        normalizedLive == normalizedPreview -> live
        normalizedLive.endsWith(normalizedPreview) -> live
        normalizedPreview.startsWith(normalizedLive) -> preview
        else -> "$live\n\n$preview"
    }
}

internal fun mergeCompletedAssistantText(
    streamed: String,
    completed: String,
    responsePreviewed: Boolean = false,
): String {
    val live = streamed.trim()
    val final = completed.trim()
    val normalizedLive = live.normalizeStreamText()
    val normalizedFinal = final.normalizeStreamText()
    return when {
        live.isBlank() -> final
        final.isBlank() -> live
        normalizedFinal == normalizedLive -> live
        normalizedFinal.startsWith(normalizedLive) -> final
        normalizedLive.startsWith(normalizedFinal) || normalizedLive.endsWith(normalizedFinal) -> live
        responsePreviewed && normalizedLive.contains(normalizedFinal) -> live
        else -> "$live\n\n$final"
    }
}

private fun String.normalizeStreamText(): String = replace(Regex("\\s+"), " ").trim()

internal fun resolveArtifactPath(path: String, workspacePath: String): String? =
    resolveRemoteArtifactPath(path, workspacePath)


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

package com.qingyu.hermescompanion.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.qingyu.hermescompanion.ui.component.AmbientBackground
import com.qingyu.hermescompanion.ui.component.HermesBottomDock
import com.qingyu.hermescompanion.ui.screen.ChatScreen
import com.qingyu.hermescompanion.ui.screen.ConnectionScreen
import com.qingyu.hermescompanion.ui.screen.CronDetailScreen
import com.qingyu.hermescompanion.ui.screen.NotificationSettingsScreen
import com.qingyu.hermescompanion.ui.screen.VoiceSettingsScreen
import com.qingyu.hermescompanion.ui.screen.AboutScreen
import com.qingyu.hermescompanion.ui.screen.ApprovalSettingsScreen
import com.qingyu.hermescompanion.ui.screen.ArchivedSessionsScreen
import com.qingyu.hermescompanion.ui.screen.ConversationStyleScreen
import com.qingyu.hermescompanion.ui.screen.MemoryContextScreen
import com.qingyu.hermescompanion.ui.screen.ModelSettingsScreen
import com.qingyu.hermescompanion.ui.screen.ProfileScreen
import com.qingyu.hermescompanion.ui.screen.ProfileSettingsScreen
import com.qingyu.hermescompanion.ui.screen.SessionsScreen
import com.qingyu.hermescompanion.ui.screen.SessionSearchScreen
import com.qingyu.hermescompanion.ui.screen.SkillsToolsScreen
import com.qingyu.hermescompanion.ui.screen.TasksScreen
import com.qingyu.hermescompanion.ui.screen.WorkspaceScreen
import com.qingyu.hermescompanion.ui.component.ImageLoadingDialog
import com.qingyu.hermescompanion.ui.component.ImagePreviewDialog

private val DockRoutes = setOf(AppRoute.SESSIONS, AppRoute.WORKSPACE, AppRoute.TASKS, AppRoute.PROFILE)

@Composable
fun HermesApp(viewModel: HermesViewModel, state: AppUiState) {
    val snackbarHostState = remember { SnackbarHostState() }
    val message = state.errorMessage ?: state.noticeMessage

    LaunchedEffect(message) {
        if (!message.isNullOrBlank()) {
            snackbarHostState.showSnackbar(
                message = message,
                duration = if (state.errorMessage != null) SnackbarDuration.Long else SnackbarDuration.Short,
            )
            viewModel.clearTransientMessage()
        }
    }

    AmbientBackground {
        Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (state.route in DockRoutes) {
                HermesBottomDock(
                    selected = state.route,
                    hasUnreadConversations = state.unreadSessionIds.isNotEmpty(),
                    onSelect = { route ->
                        when (route) {
                            AppRoute.SESSIONS -> viewModel.showSessions()
                            AppRoute.WORKSPACE -> viewModel.showWorkspace()
                            AppRoute.TASKS -> viewModel.showTasks()
                            AppRoute.PROFILE -> viewModel.showProfile()
                            else -> Unit
                        }
                    },
                )
            }
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                snackbar = { data ->
                    androidx.compose.material3.Snackbar(
                        snackbarData = data,
                        containerColor = if (state.errorMessage != null) {
                            MaterialTheme.colorScheme.errorContainer
                        } else {
                            MaterialTheme.colorScheme.inverseSurface
                        },
                        contentColor = if (state.errorMessage != null) {
                            MaterialTheme.colorScheme.onErrorContainer
                        } else {
                            MaterialTheme.colorScheme.inverseOnSurface
                        },
                    )
                },
            )
        },
        contentColor = Color.Unspecified,
    ) { padding ->
        when (state.route) {
            AppRoute.SETUP -> ConnectionScreen(
                state = state,
                contentPadding = padding,
                onConnect = viewModel::connect,
                onBack = if (state.hasSavedConnection) viewModel::closeConnectionSettings else null,
                onDisconnect = if (state.hasSavedConnection) viewModel::disconnect else null,
            )

            AppRoute.SESSIONS -> SessionsScreen(
                state = state,
                contentPadding = padding,
                onRefresh = viewModel::refreshSessions,
                onNewSession = viewModel::createSession,
                onSearch = viewModel::showSessionSearch,
                onOpenSession = viewModel::openSession,
                onDeleteSession = viewModel::deleteSession,
                onAiRenameSession = viewModel::aiRenameSession,
                onTogglePinned = viewModel::toggleSessionPinned,
                onArchiveSession = viewModel::archiveSession,
                onMoveToProject = viewModel::moveSessionToProject,
                onLoadProjects = viewModel::refreshProjects,
                onLoadProjectDirectories = viewModel::loadProjectDirectoryPicker,
                onCloseProjectDirectoryPicker = viewModel::closeProjectDirectoryPicker,
                onCreateProject = viewModel::createProject,
                onBatchAiRename = viewModel::batchAiRenameSessions,
                onRefreshProfiles = viewModel::refreshProfiles,
                onSelectProfile = viewModel::selectProfile,
            )

            AppRoute.SEARCH -> SessionSearchScreen(
                sessions = state.sessions,
                contentPadding = padding,
                onBack = viewModel::closeSessionSearch,
                onOpenSession = viewModel::openSession,
            )

            AppRoute.CHAT -> ChatScreen(
                state = state,
                contentPadding = padding,
                onBack = viewModel::backToSessions,
                onDraftChange = viewModel::updateDraft,
                onAddAttachments = viewModel::addAttachments,
                onRemoveAttachment = viewModel::removeAttachment,
                onSend = viewModel::sendMessage,
                onRetryFailed = viewModel::retryFailedMessage,
                onStop = viewModel::stopGeneration,
                onVoiceUnavailable = viewModel::showVoiceRecognitionUnavailable,
                onVoiceResult = viewModel::acceptVoiceResult,
                onLoadModels = viewModel::loadModelCatalog,
                onSwitchModel = viewModel::switchModel,
                onOpenArtifact = viewModel::openChatArtifact,
                onOpenWorkspace = viewModel::showWorkspace,
                onOpenImage = viewModel::openImage,
                onOpenLink = viewModel::openChatLink,
                onLoadInlineImages = viewModel::loadInlineChatImages,
            )

            AppRoute.WORKSPACE -> WorkspaceScreen(
                state = state,
                contentPadding = padding,
                onRefresh = viewModel::refreshWorkspace,
                onOpenDirectory = viewModel::openWorkspaceDirectory,
                onOpenDocument = viewModel::openWorkspaceDocument,
                onOpenImage = viewModel::openImage,
                onCloseDocument = viewModel::closeWorkspaceDocument,
                onEditingChange = viewModel::setWorkspaceEditing,
                onDraftChange = viewModel::updateWorkspaceDraft,
                onSave = viewModel::saveWorkspaceDocument,
                onUnsupportedFile = { viewModel.showNotice("$it 暂不支持在 APP 内预览；当前版本优先支持 Markdown") },
            )

            AppRoute.TASKS -> TasksScreen(
                state = state,
                contentPadding = padding,
                onStartConversation = viewModel::createSession,
                onRefreshCron = viewModel::refreshCronJobs,
                onCreateCron = viewModel::createCronJob,
                onUpdateCron = viewModel::updateCronJob,
                onOpenCron = viewModel::openCronJob,
                onToggleCron = viewModel::toggleCronJob,
                onTriggerCron = viewModel::triggerCronJob,
                onDeleteCron = viewModel::deleteCronJob,
            )

            AppRoute.CRON_DETAIL -> CronDetailScreen(
                state = state,
                contentPadding = padding,
                onBack = viewModel::closeCronJob,
                onSave = viewModel::updateCronJob,
                onToggle = viewModel::toggleCronJob,
                onTrigger = viewModel::triggerCronJob,
                onDelete = viewModel::deleteCronJob,
            )

            AppRoute.PROFILE -> ProfileScreen(
                state = state,
                contentPadding = padding,
                onThemeChange = viewModel::setThemeMode,
                onSkinChange = viewModel::setSkinMode,
                onConnectionSettings = viewModel::openConnectionSettings,
                onNotificationSettings = viewModel::showNotificationSettings,
                onVoiceSettings = viewModel::showVoiceSettings,
                onSkillsTools = viewModel::showSkillsAndTools,
                onModelSettings = viewModel::showModelSettings,
                onConversationStyle = viewModel::showConversationStyleSettings,
                onApprovalSettings = viewModel::showApprovalSettings,
                onMemoryContext = viewModel::showMemoryContextSettings,
                onArchivedSessions = viewModel::showArchivedSessions,
                onProfileSettings = viewModel::showProfileSettings,
                onAbout = viewModel::showAbout,
            )

            AppRoute.PROFILE_SETTINGS -> ProfileSettingsScreen(
                state = state,
                contentPadding = padding,
                onBack = viewModel::closeSettingsPage,
                onSave = viewModel::updateUserProfile,
                onUserAvatarSelected = viewModel::updateUserAvatar,
                onHermesAvatarSelected = viewModel::updateHermesAvatar,
                onResetUserAvatar = viewModel::resetUserAvatar,
                onResetHermesAvatar = viewModel::resetHermesAvatar,
            )

            AppRoute.SKILLS_TOOLS -> SkillsToolsScreen(
                state = state,
                contentPadding = padding,
                onBack = viewModel::closeSettingsPage,
                onRefresh = viewModel::showSkillsAndTools,
                onSkillClick = viewModel::openSkill,
                onSkillToggle = viewModel::toggleSkill,
                onToolsetToggle = viewModel::toggleToolset,
                onMcpToggle = viewModel::toggleMcpServer,
                onCloseSkill = viewModel::closeSkill,
            )

            AppRoute.MODEL_SETTINGS -> ModelSettingsScreen(
                state = state,
                contentPadding = padding,
                onBack = viewModel::closeSettingsPage,
                onSave = viewModel::saveModelSettings,
                onAddProvider = viewModel::addCustomProvider,
            )

            AppRoute.CONVERSATION_STYLE -> ConversationStyleScreen(
                settings = state.serverSettings.conversation,
                loading = state.isAdvancedSettingsLoading,
                contentPadding = padding,
                onBack = viewModel::closeSettingsPage,
                onSave = viewModel::saveConversationStyle,
            )

            AppRoute.APPROVAL_SETTINGS -> ApprovalSettingsScreen(
                settings = state.serverSettings.approvals,
                loading = state.isAdvancedSettingsLoading,
                contentPadding = padding,
                onBack = viewModel::closeSettingsPage,
                onSave = viewModel::saveApprovalSettings,
            )

            AppRoute.MEMORY_CONTEXT -> MemoryContextScreen(
                settings = state.serverSettings.memory,
                loading = state.isAdvancedSettingsLoading,
                contentPadding = padding,
                onBack = viewModel::closeSettingsPage,
                onSave = viewModel::saveMemorySettings,
            )

            AppRoute.ARCHIVED_SESSIONS -> ArchivedSessionsScreen(
                state = state,
                contentPadding = padding,
                onBack = viewModel::closeSettingsPage,
                onRefresh = viewModel::showArchivedSessions,
                onRestore = viewModel::restoreArchivedSession,
                onDelete = viewModel::deleteArchivedSession,
            )

            AppRoute.NOTIFICATIONS -> NotificationSettingsScreen(
                preferences = state.notificationPreferences,
                contentPadding = padding,
                onBack = viewModel::closeSettingsPage,
                onChange = viewModel::updateNotificationPreferences,
                onTest = viewModel::sendTestNotification,
            )

            AppRoute.VOICE_SETTINGS -> VoiceSettingsScreen(
                preferences = state.voicePreferences,
                contentPadding = padding,
                onBack = viewModel::closeSettingsPage,
                onChange = viewModel::updateVoicePreferences,
            )

            AppRoute.ABOUT -> AboutScreen(
                contentPadding = padding,
                onBack = viewModel::closeSettingsPage,
            )
        }
        }
        if (state.isImageLoading) ImageLoadingDialog()
        state.imagePreview?.let { image ->
            ImagePreviewDialog(image = image, onDismiss = viewModel::closeImagePreview)
        }
        state.crashReport?.let { report ->
            CrashReportDialog(report = report, onDismiss = viewModel::dismissCrashReport)
        }
    }
}

@Composable
private fun CrashReportDialog(report: String, onDismiss: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    AlertDialog(
        onDismissRequest = {},
        title = { Text("检测到上次闪退") },
        text = {
            Column {
                Text(
                    "已暂停自动恢复登录，应用会停留在安全页面。请复制下面的诊断信息发给开发者。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SelectionContainer {
                    Text(
                        text = report,
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .heightIn(max = 320.dp)
                            .verticalScroll(rememberScrollState()),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    clipboard.setText(AnnotatedString(report))
                    onDismiss()
                },
            ) { Text("复制并关闭") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        },
    )
}

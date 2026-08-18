package com.qingyu.hermescompanion.ui.screen

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.qingyu.hermescompanion.model.ChatMessage
import com.qingyu.hermescompanion.model.AgentRequest
import com.qingyu.hermescompanion.model.AgentRequestType
import com.qingyu.hermescompanion.model.scopedId
import com.qingyu.hermescompanion.model.MessageRole
import com.qingyu.hermescompanion.model.PendingAttachment
import com.qingyu.hermescompanion.model.SlashCommand
import com.qingyu.hermescompanion.model.ToolStatus
import com.qingyu.hermescompanion.model.VoiceCaptureState
import com.qingyu.hermescompanion.model.VoicePhase
import com.qingyu.hermescompanion.ui.AppUiState
import com.qingyu.hermescompanion.ui.voiceRecognitionLanguage
import com.qingyu.hermescompanion.ui.CouncilMode
import com.qingyu.hermescompanion.ui.VoiceInputAction
import com.qingyu.hermescompanion.ui.findChatImageTargets
import com.qingyu.hermescompanion.ui.CitationSource
import com.qingyu.hermescompanion.ui.CouncilAgentMessage
import com.qingyu.hermescompanion.ui.findCitationSources
import com.qingyu.hermescompanion.ui.isSyntheticProcessingStatus
import com.qingyu.hermescompanion.ui.parseCouncilAgentMessages
import com.qingyu.hermescompanion.ui.resolveVoiceInputAction
import com.qingyu.hermescompanion.ui.component.HermesIconKind
import com.qingyu.hermescompanion.ui.component.HermesMark
import com.qingyu.hermescompanion.ui.component.HermesMulticolorIcon
import com.qingyu.hermescompanion.ui.component.HermesStatusIcon
import com.qingyu.hermescompanion.ui.component.HermesStatusKind
import com.qingyu.hermescompanion.ui.component.HermesWelcomeAnimation
import com.qingyu.hermescompanion.ui.component.UserAvatar
import com.qingyu.hermescompanion.ui.component.MarkdownContent
import com.qingyu.hermescompanion.ui.component.PreviewableImage
import com.qingyu.hermescompanion.ui.format.messageTimeLabel
import com.qingyu.hermescompanion.ui.format.shouldShowMessageTime
import com.qingyu.hermescompanion.ui.theme.HermesSkin
import com.qingyu.hermescompanion.ui.theme.HermesColors
import com.qingyu.hermescompanion.ui.theme.HermesSpacing
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.core.content.ContextCompat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    state: AppUiState,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onDraftChange: (String) -> Unit,
    onAddAttachments: (List<android.net.Uri>) -> Unit,
    onRemoveAttachment: (String) -> Unit,
    onSend: () -> Unit,
    onRetryFailed: () -> Unit,
    onStop: () -> Unit,
    onSteer: () -> Unit,
    onQueue: () -> Unit,
    onCancelQueued: () -> Unit,
    onRespondRequest: (AgentRequest, String) -> Unit,
    onVoiceConversation: () -> Unit,
    onStartVoiceInput: () -> Unit,
    onStopVoiceInput: () -> Unit,
    onCancelVoiceInput: () -> Unit,
    onVoiceSystemResult: (String) -> Unit,
    onVoiceUnavailable: () -> Unit,
    onLoadModels: () -> Unit,
    onSwitchModel: (String, String) -> Unit,
    onLoadCommandCatalog: () -> Unit,
    onSetCouncilMode: (CouncilMode) -> Unit,
    onOpenArtifact: (com.qingyu.hermescompanion.model.ChatArtifact) -> Unit,
    onOpenWorkspace: () -> Unit,
    onOpenImage: (String, String) -> Unit,
    onOpenLink: (String) -> Unit,
    onLoadInlineImages: (List<String>) -> Unit,
    onLoadOlderMessages: () -> Unit,
    onScrollPositionChange: (String, Int, Int) -> Unit,
) {
    val skin = HermesSkin.current
    val context = LocalContext.current
    val voicePermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) onStartVoiceInput() else onVoiceUnavailable()
    }
    val systemVoiceLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
                ?.takeIf(String::isNotBlank)
                ?.let(onVoiceSystemResult)
        }
    }
    val launchSystemVoice = {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                voiceRecognitionLanguage(state.voicePreferences.language, state.voicePreferences.transcriptScript),
            )
            putExtra(RecognizerIntent.EXTRA_PROMPT, "请说出要发送给 Hermes 的内容")
        }
        try {
            systemVoiceLauncher.launch(intent)
        } catch (_: ActivityNotFoundException) {
            onVoiceUnavailable()
        } catch (_: SecurityException) {
            onVoiceUnavailable()
        }
    }
    val startAgentVoice = {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            onStartVoiceInput()
        } else {
            voicePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
    val voiceInputAction = {
        when (resolveVoiceInputAction(state.voicePreferences.engine, state.voiceCapture.phase, state.voiceCapture.agentSttAvailable)) {
            VoiceInputAction.STOP_RECORDING -> onStopVoiceInput()
            VoiceInputAction.LAUNCH_SYSTEM -> launchSystemVoice()
            VoiceInputAction.START_AGENT -> startAgentVoice()
            VoiceInputAction.WAIT -> Unit
        }
    }
    val sessionKey = state.selectedSession?.scopedId.orEmpty()
    val listState = remember(sessionKey) { LazyListState(state.chatScrollIndex, state.chatScrollOffset) }
    val listScope = rememberCoroutineScope()
    val showJumpToBottom by remember {
        derivedStateOf { listState.canScrollForward }
    }
    var assistantSheetVisible by remember { mutableStateOf(false) }
    var composerToolsVisible by remember { mutableStateOf(false) }
    var commandPaletteVisible by remember { mutableStateOf(false) }
    var councilSheetVisible by remember { mutableStateOf(false) }
    var councilOpenedFromAssistant by remember { mutableStateOf(false) }
    var linkDialogVisible by remember { mutableStateOf(false) }
    var linkDraft by remember { mutableStateOf("") }
    val hermesName = state.userProfile.hermesDisplayName.ifBlank { "Hermes" }
    val isCurrentSessionStreaming = state.isStreaming &&
        state.streamingSessionId == state.selectedSession?.id
    val isCurrentSessionRecovering = state.isRecoveringConnection && isCurrentSessionStreaming
    val composerEnabled = (!state.isStreaming || isCurrentSessionStreaming) && !state.isModelSwitching
    val historyHeaderCount = if (state.hasOlderMessages || state.isOlderMessagesLoading) 1 else 0
    val modelLabel = state.selectedSession?.model.orEmpty()
        .substringAfterLast(':')
        .substringAfterLast('/')
        .takeIf(String::isNotBlank)
    val fileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
        onResult = onAddAttachments,
    )
    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
        onResult = onAddAttachments,
    )
    DisposableEffect(sessionKey) {
        onDispose {
            onScrollPositionChange(sessionKey, listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset)
        }
    }

    LaunchedEffect(sessionKey, state.isBusy) {
        if (!state.isBusy && state.messages.isNotEmpty() && (!state.hasSavedChatScroll || isCurrentSessionStreaming)) {
            listState.scrollToItem(state.messages.lastIndex + historyHeaderCount)
        }
    }
    LaunchedEffect(state.highlightedMessageId, state.messages.size) {
        val targetId = state.highlightedMessageId ?: return@LaunchedEffect
        val targetIndex = state.messages.indexOfFirst { it.id == targetId }
        if (targetIndex >= 0) {
            delay(60)
            listState.animateScrollToItem(targetIndex + historyHeaderCount)
        }
    }
    val inlineImageTargets = remember(state.messages) {
        state.messages.asSequence()
            .filterNot(ChatMessage::isStreaming)
            .flatMap { message -> findChatImageTargets(message.content).asSequence() }
            .distinct()
            .toList()
    }
    LaunchedEffect(inlineImageTargets) {
        if (inlineImageTargets.isNotEmpty()) onLoadInlineImages(inlineImageTargets)
    }
    val scrollKey = state.messages.lastOrNull()?.content?.length ?: 0
    LaunchedEffect(scrollKey) {
        if (isCurrentSessionStreaming && state.messages.isNotEmpty()) {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: Int.MAX_VALUE
            if (lastVisible >= listState.layoutInfo.totalItemsCount - 4) {
                delay(70)
                val target = (listState.layoutInfo.totalItemsCount - 1).coerceAtLeast(0)
                listState.scrollToItem(target)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .padding(contentPadding),
    ) {
        CenterAlignedTopAppBar(
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = state.selectedSession?.title ?: "新会话",
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        HermesStatusIcon(if (isCurrentSessionStreaming) HermesStatusKind.BUSY else HermesStatusKind.CONNECTED)
                        Text(
                            text = when {
                                isCurrentSessionRecovering && modelLabel != null -> "$modelLabel · 正在重连"
                                isCurrentSessionRecovering -> "$hermesName · 正在重连"
                                isCurrentSessionStreaming && state.runStage.isNotBlank() -> state.runStage
                                isCurrentSessionStreaming && modelLabel != null -> "$modelLabel · 正在处理"
                                isCurrentSessionStreaming -> "$hermesName · 正在处理"
                                modelLabel != null -> "$modelLabel · 在线"
                                else -> "$hermesName 在线"
                            },
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 5.dp),
                        )
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    HermesMulticolorIcon(HermesIconKind.BACK, contentDescription = "返回")
                }
            },
            actions = {
                if (state.voicePreferences.enabled) {
                    IconButton(onClick = onVoiceConversation) {
                        HermesMulticolorIcon(HermesIconKind.WAVEFORM, contentDescription = "语音对话", iconSize = 22.dp)
                    }
                }
                Box(
                    modifier = Modifier.padding(end = 12.dp).size(40.dp).clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer).clickable { assistantSheetVisible = true },
                    contentAlignment = Alignment.Center,
                ) {
                    HermesMulticolorIcon(HermesIconKind.SKILLS, contentDescription = "$hermesName 助理面板", iconSize = 20.dp)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = skin.chromeAlpha),
            ),
        )

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when {
                state.isBusy && state.messages.isEmpty() -> {
                    ChatLoadingState(modifier = Modifier.align(Alignment.Center))
                }

                state.messages.isEmpty() -> {
                    EmptyConversation(
                        onSuggestion = onDraftChange,
                        hermesName = hermesName,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }

                else -> {
                    Box(Modifier.fillMaxSize()) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = HermesSpacing.sm, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(11.dp),
                        ) {
                            if (state.hasOlderMessages || state.isOlderMessagesLoading) {
                                item(key = "older-messages") {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center,
                                    ) {
                                        TextButton(
                                            onClick = onLoadOlderMessages,
                                            enabled = !state.isOlderMessagesLoading,
                                        ) {
                                            if (state.isOlderMessagesLoading) {
                                                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                                                Spacer(Modifier.width(8.dp))
                                                Text("正在加载更早消息")
                                            } else {
                                                Text("加载更早消息")
                                            }
                                        }
                                    }
                                }
                            }
                            itemsIndexed(state.messages, key = { _, item -> item.id }) { index, message ->
                                if (shouldShowMessageTime(state.messages.getOrNull(index - 1)?.createdAt, message.createdAt)) {
                                    ConversationTime(messageTimeLabel(message.createdAt))
                                }
                                MessageItem(
                                    message = message,
                                    showIdentity = state.messages.getOrNull(index - 1)?.role != message.role,
                                    highlighted = message.id == state.highlightedMessageId,
                                    onOpenImage = onOpenImage,
                                    onOpenLink = onOpenLink,
                                    userName = state.userProfile.displayName.ifBlank { state.username.ifBlank { "我" } },
                                    userAvatarUri = state.userProfile.avatarUri,
                                    hermesName = hermesName,
                                    hermesAvatarUri = state.userProfile.hermesAvatarUri,
                                    inlineImagePreviews = state.inlineImagePreviews,
                                    runningToolCount = if (message.isStreaming) {
                                        state.toolActivities.count { it.status == ToolStatus.RUNNING }
                                    } else {
                                        0
                                    },
                                )
                            }
                            if (isCurrentSessionStreaming) {
                                state.pendingAgentRequests
                                    .filter { it.conversationId == state.selectedSession?.id }
                                    .forEach { request ->
                                        item(key = "agent-request-${request.requestId}") {
                                            AgentRequestCard(request, onRespondRequest)
                                        }
                                    }
                            }
                            state.latestCompletion
                                ?.takeIf { it.sessionId == state.selectedSession?.id && !isCurrentSessionStreaming }
                                ?.let { completion ->
                                    item(key = "completion-${completion.completedAtMillis}") {
                                        CompletionCard()
                                    }
                                }
                        }
                        if (showJumpToBottom) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shadowElevation = 3.dp,
                                modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp).size(40.dp).clickable {
                                    listScope.launch {
                                        val target = (listState.layoutInfo.totalItemsCount - 1).coerceAtLeast(0)
                                        listState.scrollToItem(target)
                                        delay(16)
                                        val layout = listState.layoutInfo
                                        val targetInfo = layout.visibleItemsInfo.firstOrNull { it.index == target }
                                        val viewportSize = layout.viewportEndOffset - layout.viewportStartOffset
                                        val offset = bottomScrollOffset(targetInfo?.size ?: 0, viewportSize)
                                        listState.animateScrollToItem(target, offset)
                                    }
                                },
                            ) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    HermesMulticolorIcon(
                                        HermesIconKind.EXPAND_DOWN,
                                        contentDescription = "跳到底部",
                                        iconSize = 19.dp,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Composer(
            draft = state.draft,
            attachments = state.attachments,
            assistantName = hermesName,
            isStreaming = isCurrentSessionStreaming,
            enabled = composerEnabled,
            slashCommands = state.slashCommands,
            isSlashCommandsLoading = state.isSlashCommandsLoading,
            onDraftChange = onDraftChange,
            onSlashSelect = { command ->
                onDraftChange(command.command + if (command.argsHint.isNotBlank()) " " else "")
            },
            onTools = { composerToolsVisible = true },
            onCommands = {
                commandPaletteVisible = true
                onLoadCommandCatalog()
            },
            councilMode = state.councilMode,
            onDisableCouncil = { onSetCouncilMode(CouncilMode.OFF) },
            voiceCapture = state.voiceCapture,
            onVoice = voiceInputAction,
            onCancelVoice = onCancelVoiceInput,
            onRemoveAttachment = onRemoveAttachment,
            onPreviewAttachment = { attachment ->
                attachment.dataUrl?.let { onOpenImage(it, attachment.name) }
            },
            onSend = onSend,
            failedSend = state.failedSend != null,
            onRetryFailed = onRetryFailed,
            onStop = onStop,
            isSteering = state.isSteering,
            hasQueuedMessage = state.queuedRunMessage?.session?.id == state.selectedSession?.id,
            onSteer = onSteer,
            onQueue = onQueue,
            onCancelQueued = onCancelQueued,
        )
    }

    if (assistantSheetVisible) {
        ChatAssistantSheet(
            state = state,
            onDismiss = { assistantSheetVisible = false },
            onLoadModels = onLoadModels,
            onSwitchModel = onSwitchModel,
            onOpenArtifact = {
                assistantSheetVisible = false
                onOpenArtifact(it)
            },
            onOpenCouncil = {
                assistantSheetVisible = false
                councilOpenedFromAssistant = true
                councilSheetVisible = true
                onLoadModels()
            },
        )
    }
    if (composerToolsVisible) {
        ComposerToolsSheet(
            onDismiss = { composerToolsVisible = false },
            onPickFiles = {
                fileLauncher.launch(arrayOf("text/*", "application/json", "application/xml", "application/x-yaml"))
            },
            onPickImages = { imageLauncher.launch(arrayOf("image/*")) },
            onAddLink = { linkDialogVisible = true },
            onOpenWorkspace = onOpenWorkspace,
            onInsertPrompt = { snippet ->
                onDraftChange(listOf(state.draft, snippet).filter(String::isNotBlank).joinToString("\n"))
            },
        )
    }
    if (commandPaletteVisible) {
        CommandPaletteSheet(
            commands = state.commandCatalog.ifEmpty { state.slashCommands },
            loading = state.isCommandCatalogLoading,
            onDismiss = { commandPaletteVisible = false },
            onSelect = { command ->
                onDraftChange(command.command + if (command.argsHint.isNotBlank()) " " else "")
                commandPaletteVisible = false
            },
        )
    }
    if (councilSheetVisible) {
        ExpertCouncilSheet(
            state = state,
            onDismiss = {
                councilSheetVisible = false
                councilOpenedFromAssistant = false
            },
            onBackToAssistant = if (councilOpenedFromAssistant) {
                {
                    councilSheetVisible = false
                    councilOpenedFromAssistant = false
                    assistantSheetVisible = true
                }
            } else null,
            onSelect = { mode ->
                onSetCouncilMode(mode)
                councilSheetVisible = false
                councilOpenedFromAssistant = false
            },
        )
    }
    if (linkDialogVisible) {
        AlertDialog(
            onDismissRequest = { linkDialogVisible = false },
            shape = RoundedCornerShape(22.dp),
            title = { Text("添加链接") },
            text = {
                Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f)) {
                    BasicTextField(
                        value = linkDraft,
                        onValueChange = { linkDraft = it },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 11.dp),
                        decorationBox = { inner -> Box { if (linkDraft.isBlank()) Text("https://…", color = MaterialTheme.colorScheme.onSurfaceVariant); inner() } },
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = linkDraft.isNotBlank(),
                    onClick = {
                        onDraftChange(listOf(state.draft, "请查看这个链接：${linkDraft.trim()}").filter(String::isNotBlank).joinToString("\n"))
                        linkDraft = ""
                        linkDialogVisible = false
                    },
                ) { Text("添加") }
            },
            dismissButton = { TextButton(onClick = { linkDialogVisible = false }) { Text("取消") } },
        )
    }
}

internal fun bottomScrollOffset(itemSize: Int, viewportSize: Int): Int =
    (itemSize - viewportSize).coerceAtLeast(0)

@Composable
private fun ChatLoadingState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.width(176.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
            HermesMulticolorIcon(HermesIconKind.AI, contentDescription = null, iconSize = 23.dp)
        }
        Text("正在加载最近消息", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 9.dp, bottom = 9.dp))
        LinearProgressIndicator(
            modifier = Modifier.fillMaxWidth().height(3.dp).clip(CircleShape),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
        )
    }
}

@Composable
private fun ConversationTime(label: String) {
    if (label.isBlank()) return
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
        )
    }
}

@Composable
private fun MessageItem(
    message: ChatMessage,
    showIdentity: Boolean,
    highlighted: Boolean,
    onOpenImage: (String, String) -> Unit,
    onOpenLink: (String) -> Unit,
    userName: String,
    userAvatarUri: String,
    hermesName: String,
    hermesAvatarUri: String,
    inlineImagePreviews: Map<String, com.qingyu.hermescompanion.model.ImagePreview>,
    runningToolCount: Int,
) {
    val skin = HermesSkin.current
    val councilMessages = remember(message.content) { parseCouncilAgentMessages(message.content) }
    val syntheticProcessing = message.role == MessageRole.ASSISTANT && message.isStreaming &&
        isSyntheticProcessingStatus(message.content)
    val visibleContent = if (syntheticProcessing) "" else message.content
    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth().then(
            if (highlighted) {
                Modifier.clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.68f))
                    .padding(7.dp)
            } else {
                Modifier
            },
        ),
    ) {
        val userBubbleMaxWidth = maxWidth * 0.80f
        if (councilMessages.isNotEmpty()) {
            CouncilGroupTranscript(
                messages = councilMessages,
                onOpenImage = onOpenImage,
                onOpenLink = onOpenLink,
                inlineImagePreviews = inlineImagePreviews,
            )
        } else when (message.role) {
        MessageRole.USER -> Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.Top,
        ) {
            Column(horizontalAlignment = Alignment.End) {
                if (showIdentity) Text(
                    userName,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 2.dp, bottom = 3.dp),
                )
                val bubbleShape = RoundedCornerShape(18.dp, 7.dp, 18.dp, 18.dp)
                Box(
                    modifier = Modifier.widthIn(max = userBubbleMaxWidth)
                        .clip(bubbleShape)
                        .background(
                            if (skin.glass) {
                                Brush.linearGradient(listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.secondaryContainer))
                            } else {
                                Brush.linearGradient(listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.primaryContainer))
                            },
                        )
                        .then(
                            if (skin.glass) Modifier.border(0.8.dp, MaterialTheme.colorScheme.outlineVariant, bubbleShape)
                            else Modifier,
                        ),
                ) {
                    Column {
                        if (message.content.isNotBlank()) {
                            MarkdownContent(
                                markdown = message.content,
                                modifier = Modifier.padding(horizontal = 13.dp, vertical = 10.dp),
                                onOpenImage = onOpenImage,
                                onOpenLink = onOpenLink,
                                inlineImagePreviews = inlineImagePreviews,
                            )
                        }
                        if (message.images.isNotEmpty()) {
                            Column(
                                modifier = Modifier.padding(start = 9.dp, end = 9.dp, bottom = 9.dp),
                                verticalArrangement = Arrangement.spacedBy(7.dp),
                            ) {
                                message.images.forEach { image ->
                                    PreviewableImage(
                                        source = image.source,
                                        name = image.name,
                                        onOpen = onOpenImage,
                                        modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp, max = 190.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Box(Modifier.padding(start = 8.dp, top = 1.dp).size(32.dp)) {
                if (showIdentity) UserAvatar(
                    uri = userAvatarUri,
                    displayName = userName,
                    size = 32.dp,
                )
            }
        }

        MessageRole.ASSISTANT -> Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            Box(Modifier.padding(top = 1.dp).size(32.dp)) {
                if (showIdentity && hermesAvatarUri.isNotBlank()) {
                    UserAvatar(
                        uri = hermesAvatarUri,
                        displayName = hermesName,
                        size = 32.dp,
                        hermesFallback = true,
                    )
                } else if (showIdentity) {
                    HermesMark(compact = true)
                }
            }
            Column(modifier = Modifier.weight(1f).padding(start = 10.dp, end = 4.dp, top = if (showIdentity) 3.dp else 0.dp)) {
                if (showIdentity) {
                    Text(
                        hermesName,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 3.dp),
                    )
                }
                if (visibleContent.isNotBlank()) {
                    if (message.isStreaming) {
                        Text(
                            text = visibleContent,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.1f,
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    } else {
                        MarkdownContent(
                            markdown = visibleContent,
                            onOpenImage = onOpenImage,
                            onOpenLink = onOpenLink,
                            inlineImagePreviews = inlineImagePreviews,
                        )
                        val sources = remember(visibleContent) { findCitationSources(visibleContent) }
                        if (sources.isNotEmpty()) {
                            CitationSourcesCard(sources = sources, onOpenLink = onOpenLink)
                        }
                    }
                }
                if (message.images.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        message.images.forEach { image ->
                            PreviewableImage(
                                source = image.source,
                                name = image.name,
                                onOpen = onOpenImage,
                                modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp, max = 210.dp),
                            )
                        }
                    }
                }
                if (message.isStreaming) {
                    Row(
                        modifier = Modifier.padding(top = if (visibleContent.isBlank()) 2.dp else 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(15.dp), strokeWidth = 2.dp)
                        Text(
                            text = when {
                                runningToolCount > 0 || syntheticProcessing -> "正在处理…"
                                visibleContent.isBlank() -> "正在思考…"
                                else -> "正在继续…"
                            },
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            }
        }

        MessageRole.TOOL, MessageRole.SYSTEM -> Unit
        }
    }
}

@Composable
private fun CouncilGroupTranscript(
    messages: List<CouncilAgentMessage>,
    onOpenImage: (String, String) -> Unit,
    onOpenLink: (String) -> Unit,
    inlineImagePreviews: Map<String, com.qingyu.hermescompanion.model.ImagePreview>,
) {
    Column(Modifier.fillMaxWidth().padding(end = 4.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.padding(start = 2.dp, bottom = 1.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HermesMulticolorIcon(HermesIconKind.COUNCIL, contentDescription = null, iconSize = 19.dp)
            Column(Modifier.padding(start = 8.dp)) {
                Text(
                    "专家会审",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    "${messages.size} 位专家已分别返回",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        messages.forEachIndexed { position, agent ->
            CouncilAgentBubble(
                agent = agent,
                position = position,
                onOpenImage = onOpenImage,
                onOpenLink = onOpenLink,
                inlineImagePreviews = inlineImagePreviews,
            )
        }
    }
}

@Composable
private fun CouncilAgentBubble(
    agent: CouncilAgentMessage,
    position: Int,
    onOpenImage: (String, String) -> Unit,
    onOpenLink: (String) -> Unit,
    inlineImagePreviews: Map<String, com.qingyu.hermescompanion.model.ImagePreview>,
) {
    val accent = when (position % 3) {
        0 -> MaterialTheme.colorScheme.primary
        1 -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.tertiary
    }
    val container = when (position % 3) {
        0 -> MaterialTheme.colorScheme.primaryContainer
        1 -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.tertiaryContainer
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Surface(
            modifier = Modifier.size(32.dp),
            shape = CircleShape,
            color = accent,
            contentColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(agent.badge, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
        Column(Modifier.weight(1f).padding(start = 9.dp)) {
            Text(
                agent.name,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = accent,
                modifier = Modifier.padding(start = 2.dp, bottom = 4.dp),
            )
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 7.dp, topEnd = 17.dp, bottomEnd = 17.dp, bottomStart = 17.dp),
                color = container.copy(alpha = 0.66f),
                border = BorderStroke(0.7.dp, accent.copy(alpha = 0.16f)),
                tonalElevation = 0.dp,
            ) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp)) {
                    if (agent.task.isNotBlank()) {
                        Text(
                            "分工 · ${agent.task}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(bottom = 7.dp),
                        )
                    }
                    MarkdownContent(
                        markdown = agent.content,
                        onOpenImage = onOpenImage,
                        onOpenLink = onOpenLink,
                        inlineImagePreviews = inlineImagePreviews,
                    )
                }
            }
        }
    }
}

@Composable
private fun CitationSourcesCard(
    sources: List<CitationSource>,
    onOpenLink: (String) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(0.7.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
        tonalElevation = 0.dp,
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                HermesMulticolorIcon(HermesIconKind.LINK, contentDescription = null, iconSize = 17.dp)
                Text(
                    "参考来源 · ${sources.size}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 7.dp),
                )
            }
            sources.forEachIndexed { index, source ->
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(9.dp))
                        .clickable { onOpenLink(source.url) }
                        .padding(horizontal = 6.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "${index + 1}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.width(22.dp),
                    )
                    Column(Modifier.weight(1f)) {
                        Text(
                            source.label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            source.host,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    HermesMulticolorIcon(HermesIconKind.OPEN_EXTERNAL, contentDescription = "打开来源", iconSize = 15.dp)
                }
            }
        }
    }
}

@Composable
private fun AgentRequestCard(
    request: AgentRequest,
    onRespond: (AgentRequest, String) -> Unit,
) {
    var answer by remember(request.requestId) { mutableStateOf("") }
    val actions = if (request.type == AgentRequestType.APPROVAL) {
        buildList {
            add("仅本次允许" to "once")
            if (request.allowSession) add("本次会话允许" to "session")
            if (request.allowPermanent) add("始终允许" to "always")
            add("拒绝" to "deny")
        }
    } else {
        request.choices.map { it.label to it.value }
    }
    Surface(
        modifier = Modifier.fillMaxWidth().padding(start = 43.dp, end = 4.dp),
        shape = RoundedCornerShape(16.dp),
        color = HermesColors.extended.warningContainer,
        border = BorderStroke(0.8.dp, HermesColors.extended.warning.copy(alpha = 0.24f)),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                HermesMulticolorIcon(
                    if (request.type == AgentRequestType.APPROVAL) HermesIconKind.LOCK else HermesIconKind.IDEA,
                    contentDescription = null,
                    iconSize = 20.dp,
                )
                Text(
                    if (request.type == AgentRequestType.APPROVAL) "Hermes 需要你的确认" else "Hermes 需要补充信息",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            Text(request.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            if (request.detail.isNotBlank()) {
                Text(request.detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            actions.forEach { (label, value) ->
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable(enabled = !request.isResponding) { onRespond(request, value) },
                    shape = RoundedCornerShape(11.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
                ) {
                    Text(label, modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp), style = MaterialTheme.typography.bodyMedium)
                }
            }
            if (request.type == AgentRequestType.CLARIFICATION && actions.isEmpty()) {
                Surface(shape = RoundedCornerShape(11.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f)) {
                    BasicTextField(
                        value = answer,
                        onValueChange = { answer = it },
                        enabled = !request.isResponding,
                        maxLines = 4,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp, max = 120.dp).padding(11.dp),
                        decorationBox = { inner ->
                            Box {
                                if (answer.isBlank()) Text("输入你的回答…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                inner()
                            }
                        },
                    )
                }
                TextButton(
                    enabled = answer.isNotBlank() && !request.isResponding,
                    onClick = { onRespond(request, answer.trim()) },
                    modifier = Modifier.align(Alignment.End),
                ) { Text(if (request.isResponding) "提交中…" else "提交回答") }
            }
            if (request.isResponding) LinearProgressIndicator(Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun CompletionCard() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(end = 8.dp, top = 2.dp, bottom = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) {
            HermesMulticolorIcon(
                HermesIconKind.CHECK_CIRCLE,
                contentDescription = null,
                iconSize = 14.dp,
            )
        }
        Text(
            "本轮执行完成",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 10.dp),
        )
    }
}

@Composable
private fun Composer(
    draft: String,
    attachments: List<PendingAttachment>,
    assistantName: String,
    isStreaming: Boolean,
    enabled: Boolean,
    slashCommands: List<SlashCommand>,
    isSlashCommandsLoading: Boolean,
    onDraftChange: (String) -> Unit,
    onSlashSelect: (SlashCommand) -> Unit,
    onTools: () -> Unit,
    onCommands: () -> Unit,
    councilMode: CouncilMode,
    onDisableCouncil: () -> Unit,
    voiceCapture: VoiceCaptureState,
    onVoice: () -> Unit,
    onCancelVoice: () -> Unit,
    onRemoveAttachment: (String) -> Unit,
    onPreviewAttachment: (PendingAttachment) -> Unit,
    onSend: () -> Unit,
    failedSend: Boolean,
    onRetryFailed: () -> Unit,
    onStop: () -> Unit,
    isSteering: Boolean,
    hasQueuedMessage: Boolean,
    onSteer: () -> Unit,
    onQueue: () -> Unit,
    onCancelQueued: () -> Unit,
) {
    val skin = HermesSkin.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent,
    ) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(start = HermesSpacing.sm, end = HermesSpacing.sm, top = 5.dp, bottom = 5.dp),
        ) {
            AnimatedVisibility(visible = councilMode != CouncilMode.OFF) {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 7.dp),
                    shape = RoundedCornerShape(13.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 5.dp, top = 5.dp, bottom = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        HermesMulticolorIcon(HermesIconKind.COUNCIL, contentDescription = null, iconSize = 18.dp)
                        Text(
                            text = if (councilMode == CouncilMode.DEEP) "专家会审 · 深度" else "专家会审 · 快速 MoA",
                            modifier = Modifier.weight(1f).padding(start = 8.dp),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        TextButton(onClick = onDisableCouncil) { Text("关闭") }
                    }
                }
            }
            AnimatedVisibility(visible = hasQueuedMessage) {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 7.dp),
                    shape = RoundedCornerShape(13.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("已有一条消息排队中", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        TextButton(onClick = onCancelQueued) { Text("取消") }
                    }
                }
            }
            AnimatedVisibility(visible = voiceCapture.phase != VoicePhase.IDLE) {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 7.dp),
                    shape = RoundedCornerShape(13.dp),
                    color = when (voiceCapture.phase) {
                        VoicePhase.ERROR -> MaterialTheme.colorScheme.errorContainer
                        VoicePhase.LISTENING -> MaterialTheme.colorScheme.tertiaryContainer
                        else -> MaterialTheme.colorScheme.primaryContainer
                    },
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 5.dp, top = 6.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (voiceCapture.phase == VoicePhase.TRANSCRIBING) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            HermesMulticolorIcon(HermesIconKind.MICROPHONE, contentDescription = null, iconSize = 18.dp)
                        }
                        Column(Modifier.weight(1f).padding(start = 8.dp)) {
                            Text(
                                when (voiceCapture.phase) {
                                    VoicePhase.LISTENING -> "正在录音"
                                    VoicePhase.TRANSCRIBING -> "正在识别"
                                    VoicePhase.ERROR -> "语音输入失败"
                                    else -> "语音输入"
                                },
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                voiceCapture.message.ifBlank { "再点麦克风结束录音" },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (voiceCapture.phase == VoicePhase.ERROR) {
                                    MaterialTheme.colorScheme.onErrorContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                maxLines = 2,
                            )
                        }
                        TextButton(onClick = onCancelVoice) {
                            Text(if (voiceCapture.phase == VoicePhase.ERROR) "关闭" else "取消")
                        }
                    }
                }
            }
            AnimatedVisibility(visible = failedSend) {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 7.dp),
                    shape = RoundedCornerShape(13.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("发送失败，内容已保留", modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onErrorContainer)
                        TextButton(onClick = onRetryFailed, enabled = enabled && !isStreaming) { Text("重新发送") }
                    }
                }
            }
            AnimatedVisibility(
                visible = draft.trimStart().startsWith('/') && !draft.trimStart().contains(Regex("\\s")) &&
                    (slashCommands.isNotEmpty() || isSlashCommandsLoading),
            ) {
                SlashCommandMenu(
                    commands = slashCommands,
                    loading = isSlashCommandsLoading,
                    onSelect = onSlashSelect,
                    modifier = Modifier.padding(bottom = 7.dp),
                )
            }
            AnimatedVisibility(attachments.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    attachments.take(2).forEach { attachment ->
                        AttachmentChip(
                            attachment,
                            onOpen = { onPreviewAttachment(attachment) },
                            onRemove = { onRemoveAttachment(attachment.id) },
                        )
                    }
                    if (attachments.size > 2) {
                        Text(
                            text = "+${attachments.size - 2}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(if (skin.glass) skin.shadowElevation.dp else 2.dp, RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = skin.chromeAlpha),
                tonalElevation = 0.dp,
                border = BorderStroke(
                    if (skin.glass) 0.9.dp else 0.7.dp,
                    if (skin.glass) Color.White.copy(alpha = 0.42f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.72f),
                ),
            ) {
                Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)) {
                    BasicTextField(
                        value = draft,
                        onValueChange = onDraftChange,
                        enabled = enabled,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                        maxLines = 5,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 34.dp, max = 96.dp).padding(horizontal = 4.dp, vertical = 3.dp),
                        decorationBox = { inner ->
                            Box(contentAlignment = Alignment.TopStart) {
                                if (draft.isBlank()) {
                                    Text(
                                        when {
                                            isStreaming -> "追加要求，或排队到下一轮…"
                                            enabled -> "给 $assistantName 发消息…"
                                            else -> "另一段对话正在后台生成…"
                                        },
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                inner()
                            }
                        },
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ComposerAction(
                            icon = HermesIconKind.ATTACHMENT,
                            description = "添加内容",
                            background = Color.Transparent,
                            enabled = enabled,
                            onClick = onTools,
                        )
                        Spacer(Modifier.width(3.dp))
                        ComposerAction(
                            icon = HermesIconKind.COMMAND,
                            description = "打开命令面板",
                            background = Color.Transparent,
                            enabled = enabled && !isStreaming,
                            onClick = onCommands,
                        )
                        Spacer(Modifier.weight(1f))
                        if (isStreaming && (draft.isNotBlank() || attachments.isNotEmpty())) {
                            TextButton(
                                enabled = !isSteering && draft.isNotBlank() && attachments.isEmpty(),
                                onClick = onSteer,
                            ) { Text(if (isSteering) "追加中" else "追加") }
                            TextButton(onClick = onQueue) { Text(if (hasQueuedMessage) "替换" else "排队") }
                        }
                        if (!isStreaming && draft.isBlank() && attachments.isEmpty()) {
                            ComposerAction(
                                icon = HermesIconKind.MICROPHONE,
                                description = when (voiceCapture.phase) {
                                    VoicePhase.LISTENING -> "结束录音"
                                    VoicePhase.TRANSCRIBING -> "正在识别"
                                    else -> "单次语音输入"
                                },
                                background = if (voiceCapture.phase == VoicePhase.LISTENING) {
                                    MaterialTheme.colorScheme.errorContainer
                                } else {
                                    Color.Transparent
                                },
                                enabled = enabled && voiceCapture.phase != VoicePhase.TRANSCRIBING,
                                onClick = onVoice,
                            )
                        } else {
                            Box(
                                modifier = Modifier.size(38.dp).clip(CircleShape)
                                    .background(
                                        if (isStreaming) {
                                            Brush.linearGradient(listOf(MaterialTheme.colorScheme.error, MaterialTheme.colorScheme.error))
                                        } else {
                                            Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary))
                                        },
                                    )
                                    .clickable(
                                        enabled = isStreaming || (enabled && (draft.isNotBlank() || attachments.isNotEmpty())),
                                        onClick = if (isStreaming) onStop else onSend,
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                HermesMulticolorIcon(
                                    kind = if (isStreaming) HermesIconKind.STOP else HermesIconKind.SEND,
                                    contentDescription = if (isStreaming) "停止" else "发送",
                                    iconSize = 19.dp,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SlashCommandMenu(
    commands: List<SlashCommand>,
    loading: Boolean,
    onSelect: (SlashCommand) -> Unit,
    modifier: Modifier = Modifier,
) {
    val visibleCommands = commands.take(10)
    Surface(
        modifier = modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.62f)),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().heightIn(max = 286.dp).verticalScroll(rememberScrollState()).padding(vertical = 5.dp),
        ) {
            if (loading && visibleCommands.isEmpty()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(17.dp), strokeWidth = 2.dp)
                    Text("正在读取 Hermes 命令…", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 9.dp))
                }
            }
            visibleCommands.forEachIndexed { index, command ->
                if (index == 0 || visibleCommands[index - 1].category != command.category) {
                    Text(
                        command.category.ifBlank { "Hermes 命令" },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = if (index == 0) 5.dp else 9.dp, bottom = 3.dp),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { onSelect(command) }.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(command.command, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                            if (command.argsHint.isNotBlank()) Text(" ${command.argsHint}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (command.description.isNotBlank()) {
                            Text(command.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CommandPaletteSheet(
    commands: List<SlashCommand>,
    loading: Boolean,
    onDismiss: () -> Unit,
    onSelect: (SlashCommand) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(commands, query) {
        val keyword = query.trim().removePrefix("/")
        if (keyword.isBlank()) commands else commands.filter { command ->
            command.command.contains(keyword, ignoreCase = true) ||
                command.description.contains(keyword, ignoreCase = true) ||
                command.category.contains(keyword, ignoreCase = true)
        }
    }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().heightIn(max = 650.dp)
                .navigationBarsPadding().padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
        ) {
            Text("Hermes 命令", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(
                "点选后插入输入框，你仍可补充参数再发送。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
            )
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f),
            ) {
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = 12.dp),
                    decorationBox = { inner ->
                        Box {
                            if (query.isBlank()) Text("搜索命令或用途", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            inner()
                        }
                    },
                )
            }
            when {
                loading && commands.isEmpty() -> Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    Text("正在读取服务器命令…", modifier = Modifier.padding(start = 9.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                filtered.isEmpty() -> Text(
                    "没有匹配的命令",
                    modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f, fill = false).padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    itemsIndexed(filtered, key = { _, item -> item.command }) { index, command ->
                        Column {
                            if (index == 0 || filtered[index - 1].category != command.category) {
                                Text(
                                    command.category.ifBlank { "Hermes 命令" },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(start = 8.dp, top = if (index == 0) 5.dp else 12.dp, bottom = 3.dp),
                                )
                            }
                            Surface(
                                modifier = Modifier.fillMaxWidth().clickable { onSelect(command) },
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceContainer,
                            ) {
                                Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(command.command, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                        if (command.argsHint.isNotBlank()) {
                                            Text(" ${command.argsHint}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    if (command.description.isNotBlank()) {
                                        Text(command.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpertCouncilSheet(
    state: AppUiState,
    onDismiss: () -> Unit,
    onBackToAssistant: (() -> Unit)?,
    onSelect: (CouncilMode) -> Unit,
) {
    val currentIsMoa = state.selectedSession?.provider.orEmpty().isMoaProviderName()
    val moaPreset = state.modelCatalog.providers.firstOrNull { it.slug.isMoaProviderName() }?.models?.firstOrNull()
    val quickAvailable = currentIsMoa || moaPreset != null
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, bottom = 20.dp),
        ) {
            if (onBackToAssistant != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBackToAssistant) {
                        HermesMulticolorIcon(HermesIconKind.BACK, contentDescription = "返回助理面板")
                    }
                    Text("专家会审", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                }
            } else {
                Text("专家会审", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            }
            Text(
                "深度会审会把三位专家作为独立群成员展示，最后由 Hermes 统一裁决；专家之间不会开放式互聊。每次开启仅作用于下一条消息。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 5.dp, bottom = 14.dp),
            )
            CouncilOptionCard(
                title = "深度会审",
                subtitle = "3 个独立专家并行首轮，主 Agent 裁决；仅在关键分歧未解决时追加 1 轮复核。",
                badge = "推荐 · 更严谨",
                selected = state.councilMode == CouncilMode.DEEP,
                enabled = !state.isModelSwitching,
                onClick = { onSelect(CouncilMode.DEEP) },
            )
            Spacer(Modifier.height(9.dp))
            CouncilOptionCard(
                title = "快速会审 · MoA",
                subtitle = when {
                    currentIsMoa -> "使用当前 MoA 的参考模型并行分析，由聚合模型一次裁决。"
                    moaPreset != null -> "将当前会话切换到 ${moaPreset.substringAfterLast('/')} 后启用。"
                    state.isModelsLoading -> "正在检查服务器上的 MoA 预设…"
                    else -> "服务器未提供 MoA 预设；可在 Hermes 中配置后使用。"
                },
                badge = "更快 · Token 可控",
                selected = state.councilMode == CouncilMode.QUICK,
                enabled = quickAvailable && !state.isModelSwitching,
                onClick = { onSelect(CouncilMode.QUICK) },
            )
            if (state.isModelSwitching) {
                Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(17.dp), strokeWidth = 2.dp)
                    Text("正在切换会审模型…", modifier = Modifier.padding(start = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            TextButton(
                onClick = { onSelect(CouncilMode.OFF) },
                modifier = Modifier.align(Alignment.End).padding(top = 8.dp),
            ) { Text("关闭会审") }
        }
    }
}

@Composable
private fun CouncilOptionCard(
    title: String,
    subtitle: String,
    badge: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().alpha(if (enabled) 1f else 0.55f)
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(
            if (selected) 1.2.dp else 0.7.dp,
            if (selected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text(badge, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 5.dp))
        }
    }
}

private fun String.isMoaProviderName(): Boolean =
    equals("moa", ignoreCase = true) || contains("mixture-of-agents", ignoreCase = true)

@Composable
private fun ComposerAction(
    icon: HermesIconKind,
    description: String,
    background: Color,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier.size(36.dp).clip(RoundedCornerShape(11.dp)).background(background)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        HermesMulticolorIcon(
            icon,
            contentDescription = description,
            iconSize = 19.dp,
            modifier = Modifier.alpha(if (enabled) 1f else 0.38f),
        )
    }
}

@Composable
private fun ComposerTextAction(
    text: String,
    description: String,
    background: Color,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier.size(36.dp).clip(RoundedCornerShape(11.dp)).background(background)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.alpha(if (enabled) 1f else 0.38f),
        )
    }
}

@Composable
private fun AttachmentChip(attachment: PendingAttachment, onOpen: () -> Unit, onRemove: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(start = 10.dp, end = 3.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HermesMulticolorIcon(HermesIconKind.ATTACHMENT, contentDescription = null, iconSize = 17.dp)
            Text(
                text = attachment.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .clickable(enabled = attachment.dataUrl != null, onClick = onOpen)
                    .padding(start = 5.dp)
                    .width(95.dp),
            )
            IconButton(onClick = onRemove, modifier = Modifier.size(40.dp)) {
                HermesMulticolorIcon(HermesIconKind.CLOSE, contentDescription = "移除附件", iconSize = 16.dp)
            }
        }
    }
}

@Composable
private fun EmptyConversation(
    onSuggestion: (String) -> Unit,
    hermesName: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        HermesWelcomeAnimation(
            modifier = Modifier.size(168.dp),
            contentDescription = "$hermesName 欢迎动画",
        )
        Text(text = "今天需要我做什么？", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Text(
            "可以提问，也可以直接交代任务",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 3.dp),
        )
        Spacer(Modifier.height(12.dp))
        listOf(
            Triple("帮我梳理今天最重要的三件事", HermesIconKind.IDEA, HermesColors.extended.warningContainer),
            Triple("总结一下最近工作的进展", HermesIconKind.SUMMARIZE, MaterialTheme.colorScheme.secondaryContainer),
            Triple("帮我安排接下来一周的计划", HermesIconKind.PLAN, HermesColors.extended.successContainer),
        ).forEach { (suggestion, icon, softColor) ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp)
                    .clickable { onSuggestion(suggestion) },
                shape = RoundedCornerShape(13.dp),
                color = if (HermesSkin.current.glass) MaterialTheme.colorScheme.surface.copy(alpha = 0.72f) else Color.Transparent,
                tonalElevation = 0.dp,
                border = if (HermesSkin.current.glass) BorderStroke(0.8.dp, Color.White.copy(alpha = 0.42f)) else null,
            ) {
                Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(36.dp).clip(RoundedCornerShape(11.dp)).background(softColor), contentAlignment = Alignment.Center) {
                        HermesMulticolorIcon(icon, contentDescription = null, iconSize = 18.dp)
                    }
                    Text(text = suggestion, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 9.dp))
                }
            }
        }
    }
}

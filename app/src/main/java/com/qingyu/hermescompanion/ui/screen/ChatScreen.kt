package com.qingyu.hermescompanion.ui.screen

import com.qingyu.hermescompanion.i18n.uiText
import com.qingyu.hermescompanion.R


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
import androidx.compose.foundation.horizontalScroll
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
import com.qingyu.hermescompanion.ui.component.HermesAlertDialog as AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LinearProgressIndicator
import com.qingyu.hermescompanion.ui.component.HermesModalBottomSheet as ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.testTag
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import com.qingyu.hermescompanion.ui.component.HermesDropdownMenu as DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import com.qingyu.hermescompanion.ui.ChatEntryAction
import com.qingyu.hermescompanion.ui.component.AssistantGlyph
import com.qingyu.hermescompanion.ui.component.AssistantIconWell
import com.qingyu.hermescompanion.ui.component.AssistantPanel
import com.qingyu.hermescompanion.ui.component.AssistantBlue
import com.qingyu.hermescompanion.ui.component.AssistantPurple
import com.qingyu.hermescompanion.ui.component.AssistantMint
import com.qingyu.hermescompanion.model.scopedId
import com.qingyu.hermescompanion.model.ChatMessage
import com.qingyu.hermescompanion.model.AgentRequest
import com.qingyu.hermescompanion.model.AgentRequestChoice
import com.qingyu.hermescompanion.model.AgentRequestType
import com.qingyu.hermescompanion.model.MessageRole
import com.qingyu.hermescompanion.model.PendingAttachment
import com.qingyu.hermescompanion.model.SlashCommand
import com.qingyu.hermescompanion.model.ToolStatus
import com.qingyu.hermescompanion.model.VoiceCaptureState
import com.qingyu.hermescompanion.model.VoicePhase
import com.qingyu.hermescompanion.ui.format.projectForWorkspace
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
import com.qingyu.hermescompanion.ui.component.HermesMulticolorIcon
import com.qingyu.hermescompanion.ui.component.HermesStatusIcon
import com.qingyu.hermescompanion.ui.component.HermesStatusKind
import com.qingyu.hermescompanion.ui.component.HermesWelcomeAnimation
import com.qingyu.hermescompanion.ui.component.HermesMascot
import com.qingyu.hermescompanion.ui.component.MascotMotion
import com.qingyu.hermescompanion.model.VoiceCaptureTarget
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
    onEntryHandled: () -> Unit,
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
    onSnippetsChange: (List<com.qingyu.hermescompanion.model.PromptSnippet>) -> Unit = {},
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
            putExtra(RecognizerIntent.EXTRA_PROMPT, uiText(R.string.ui_0654, "请说出要发送给 Hermes 的内容"))
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
    var showHistory by remember(sessionKey) { mutableStateOf(state.highlightedMessageId != null) }
    val composerFocusRequester = remember { FocusRequester() }
    val softwareKeyboard = LocalSoftwareKeyboardController.current
    var topMenuVisible by remember { mutableStateOf(false) }
    var fullTitleVisible by remember(sessionKey) { mutableStateOf(false) }
    var assistantSheetVisible by remember { mutableStateOf(false) }
    var composerToolsVisible by remember { mutableStateOf(false) }
    var commandPaletteVisible by remember { mutableStateOf(false) }
    var councilSheetVisible by remember { mutableStateOf(false) }
    var councilOpenedFromAssistant by remember { mutableStateOf(false) }
    LaunchedEffect(sessionKey, state.chatEntryAction) {
        if (sessionKey.isNotEmpty() && state.chatEntryAction != ChatEntryAction.NONE) {
            when (state.chatEntryAction) {
                ChatEntryAction.ATTACHMENTS -> composerToolsVisible = true
                ChatEntryAction.VOICE -> voiceInputAction()
                else -> Unit
            }
            onEntryHandled()
        }
    }
    val hermesName = state.userProfile.hermesDisplayName.ifBlank { "Hermes" }
    val isCurrentSessionStreaming = state.isStreaming &&
        state.streamingSessionId == state.selectedSession?.id
    val isCurrentSessionRecovering = state.isRecoveringConnection && isCurrentSessionStreaming
    var observedRun by remember(sessionKey) { mutableStateOf(false) }
    var celebrate by remember(sessionKey) { mutableStateOf(false) }
    var lastCompletion by remember(sessionKey) { mutableStateOf(state.latestCompletion?.completedAtMillis ?: 0L) }
    LaunchedEffect(sessionKey, isCurrentSessionStreaming, state.latestCompletion?.completedAtMillis) {
        if (isCurrentSessionStreaming) { observedRun = true; celebrate = false }
        val completion = state.latestCompletion
        if (!isCurrentSessionStreaming && observedRun && completion?.sessionId == state.selectedSession?.id && completion != null && completion.completedAtMillis > lastCompletion) {
            celebrate = true; observedRun = false
        }
        if (completion != null && !observedRun) lastCompletion = completion.completedAtMillis
    }
    val characterState = when {
        state.voiceCapture.target == VoiceCaptureTarget.CHAT_INPUT && state.voiceCapture.phase == VoicePhase.LISTENING -> MascotMotion.LISTENING
        state.voiceCapture.target == VoiceCaptureTarget.CHAT_INPUT && state.voiceCapture.phase == VoicePhase.TRANSCRIBING -> MascotMotion.WORKING
        state.pendingAgentRequests.any { it.conversationId == state.selectedSession?.id } -> MascotMotion.IDLE_HALF
        isCurrentSessionStreaming -> MascotMotion.WORKING
        celebrate -> MascotMotion.DONE
        else -> null
    }

    val composerEnabled = !state.isModelSwitching && !state.isBusy && state.sessionActionId != state.selectedSession?.id &&
        state.selectedSession?.scopedId !in state.stoppingSessionKeys
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
            if (showHistory) onScrollPositionChange(sessionKey, listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset)
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
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
            .padding(contentPadding),
    ) {
        CenterAlignedTopAppBar(
            title = {
                Column(Modifier.clickable { fullTitleVisible = true }, horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = state.selectedSession?.title ?: uiText(R.string.ui_0079, "新会话"),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium,
                    )

                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    HermesMulticolorIcon(HermesIconKind.BACK, contentDescription = uiText(R.string.ui_0554, "返回"))
                }
            },
            actions = {
                Box {
                    IconButton(onClick = { topMenuVisible = true }, modifier = Modifier.semantics { contentDescription=uiText(R.string.ui_0655, "对话菜单") }) { AssistantGlyph("more") }
                    ChatOverflowMenu(expanded = topMenuVisible, onDismiss = { topMenuVisible = false },
                        showHistory = showHistory, voiceEnabled = state.voicePreferences.enabled,
                        onHistory = { showHistory = !showHistory; topMenuVisible = false },
                        onSettings = { assistantSheetVisible = true; topMenuVisible = false },
                        onVoice = { onVoiceConversation(); topMenuVisible = false })
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
            ),
        )

        if (fullTitleVisible) {
            AlertDialog(onDismissRequest = { fullTitleVisible = false },
                title = { Text(uiText(R.string.ui_0660, "对话详情")) },
                text = { Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text(state.selectedSession?.title.orEmpty())
                    state.selectedSession?.workspacePath?.takeIf { it.isNotBlank() }?.let { Text(it, Modifier.padding(top = 12.dp)) }
                } },
                confirmButton = { TextButton(colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer), onClick = { fullTitleVisible = false }) { Text(uiText(R.string.ui_0196, "关闭")) } })
        }
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when {
                !showHistory && state.messages.isNotEmpty() -> {
                    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 17.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        item {
                            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                val project = state.selectedSession?.workspacePath?.let { projectForWorkspace(state.projects, it) }
                                Text(project?.name?.let { uiText(R.string.ui_0661, "%1\$s · 当前对话", it) } ?: uiText(R.string.ui_0661, "%1\$s · 当前对话", state.activeProfile), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(if(isCurrentSessionStreaming) state.chatTodos.firstOrNull { it.status == com.qingyu.hermescompanion.model.TodoStatus.IN_PROGRESS }?.content ?: uiText(R.string.ui_0662, "正在处理，\n你交给我的这件事") else uiText(R.string.ui_0663, "最近一次答复"), fontSize = 25.sp, lineHeight = 35.sp, fontWeight = FontWeight.Bold)
                                Row(Modifier.padding(bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    if (characterState != null) HermesMascot(characterState, Modifier.size(66.dp, 86.dp), onFinished = { celebrate = false })
                                    else UserAvatar(state.userProfile.hermesAvatarUri, hermesName, 42.dp, hermesFallback = true, shape = CircleShape)
                                    Text(if(isCurrentSessionRecovering) uiText(R.string.ui_0664, "%1\$s 正在重连", hermesName) else if(isCurrentSessionStreaming) uiText(R.string.ui_0665, "%1\$s 正在处理", hermesName) else uiText(R.string.ui_0666, "%1\$s 的回复", hermesName), Modifier.weight(1f).padding(start = 12.dp), fontSize = 15.sp)
                                    if(isCurrentSessionStreaming) CircularProgressIndicator(Modifier.size(15.dp), color = AssistantBlue, strokeWidth = 2.dp)
                                }
                            }
                        }
                        state.pendingAgentRequests.filter { it.conversationId == state.selectedSession?.id }.forEach { request ->
                            item(key = "focus-request-${request.requestId}") { AgentRequestCard(request, onRespondRequest) }
                        }
                        if(state.chatTodos.isNotEmpty() || state.toolActivities.isNotEmpty()) item {
                            WorkProgress(state.chatTodos, state.toolActivities)
                        }
                        val answer = state.messages.lastOrNull { it.role == MessageRole.ASSISTANT }
                        if (answer != null) {
                            item(key = "focus-answer-${answer.id}") {
                                Surface(shape = MaterialTheme.shapes.large, color = if(isCurrentSessionStreaming) AssistantBlue.copy(alpha = .06f) else MaterialTheme.colorScheme.surface) {
                                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                                        MessageItem(answer, true, false, onOpenImage, onOpenLink,
                                            state.userProfile.displayName, state.userProfile.avatarUri,
                                            hermesName, state.userProfile.hermesAvatarUri, state.inlineImagePreviews,
                                            state.toolActivities.count { it.status == ToolStatus.RUNNING }, readerMode = true)
                                    }
                                }
                            }
                        }
                        if(state.chatArtifacts.isNotEmpty()) item {
                            AssistantPanel(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(uiText(R.string.ui_0667, "本次资料"), fontSize = 16.sp, fontWeight = FontWeight.Medium)
                                    state.chatArtifacts.forEach { artifact ->
                                        Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.background,
                                            modifier = Modifier.fillMaxWidth().clickable { onOpenArtifact(artifact) }) {
                                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                                AssistantIconWell("file", AssistantMint, Modifier.size(38.dp))
                                                Text(artifact.name, Modifier.weight(1f).padding(horizontal = 12.dp), fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                                AssistantGlyph("chevron", Modifier.size(18.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        item { TextButton(colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer), onClick = { showHistory = true }) { Text(uiText(R.string.ui_0668, "查看完整对话与上下文 →")) } }
                    }
                }
                state.isBusy && state.messages.isEmpty() -> {
                    ChatLoadingState(modifier = Modifier.align(Alignment.Center))
                }

                state.messages.isEmpty() -> {
                    EmptyConversation(
                        onSuggestion = onDraftChange,
                        motion = characterState,
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
                                        TextButton(colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer), 
                                            onClick = onLoadOlderMessages,
                                            enabled = !state.isOlderMessagesLoading,
                                        ) {
                                            if (state.isOlderMessagesLoading) {
                                                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                                                Spacer(Modifier.width(8.dp))
                                                Text(uiText(R.string.ui_0669, "正在加载更早消息"))
                                            } else {
                                                Text(uiText(R.string.ui_0670, "加载更早消息"))
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
                                    userName = state.userProfile.displayName.ifBlank { state.username.ifBlank { uiText(R.string.ui_0671, "我") } },
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
                            state.recentCompletions.firstOrNull { it.sessionId == state.selectedSession?.id }
                                ?.takeIf { !isCurrentSessionStreaming }
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
                                        contentDescription = uiText(R.string.ui_0672, "跳到底部"),
                                        iconSize = 19.dp,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if(isCurrentSessionStreaming && !showHistory) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 17.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HomeChoice(uiText(R.string.ui_0673, "调整要求"), "tune", AssistantPurple, Modifier.weight(1f)) { composerFocusRequester.requestFocus(); softwareKeyboard?.show() }
                HomeChoice(uiText(R.string.ui_0674, "停止这项工作"), "stop", HermesColors.extended.warning, Modifier.weight(1f), onStop)
            }
        }
        Composer(
            inputFocusRequester = composerFocusRequester,
            inlineStop = showHistory,
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
            snippets = state.promptSnippets,
            onSnippetsChange = onSnippetsChange,
            onDismiss = { composerToolsVisible = false },
            onPickFiles = {
                fileLauncher.launch(arrayOf("text/*", "application/json", "application/xml", "application/x-yaml"))
            },
            onPickImages = { imageLauncher.launch(arrayOf("image/*")) },
            onOpenCommands = { commandPaletteVisible = true; onLoadCommandCatalog() },
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

}

internal fun bottomScrollOffset(itemSize: Int, viewportSize: Int): Int =
    (itemSize - viewportSize).coerceAtLeast(0)

@Composable
private fun ChatOverflowMenu(
    expanded: Boolean, onDismiss: () -> Unit, showHistory: Boolean, voiceEnabled: Boolean,
    onHistory: () -> Unit, onSettings: () -> Unit, onVoice: () -> Unit,
) {
    val actions = buildList {
        add((if (showHistory) uiText(R.string.ui_0656, "最新答复") else uiText(R.string.ui_0657, "完整对话")) to onHistory)
        add(uiText(R.string.ui_0658, "助理设置") to onSettings)
        if (voiceEnabled) add(uiText(R.string.ui_0659, "连续语音") to onVoice)
    }
    val labels = actions.map { it.first }
    val style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium)
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val screenWidth = LocalConfiguration.current.screenWidthDp
    // Size to the translated labels, retaining padding and a comfortable hit target.
    // An explicit width also stops fillMaxWidth labels expanding the menu to its maximum.
    val menuWidth = remember(labels, style, measurer, density, screenWidth) {
        with(density) {
            (labels.maxOf { measurer.measure(it, style, softWrap = false).size.width }.toDp() + 32.dp)
                .coerceIn(128.dp, minOf(240.dp, (screenWidth - 32).coerceAtLeast(128).dp))
        }
    }
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss,
        modifier = Modifier.width(menuWidth).testTag("chat_overflow_menu"),
        shape = RoundedCornerShape((HermesSkin.current.menuRadius - 4).dp)) {
        actions.forEach { (label, action) ->
            DropdownMenuItem(onClick = action, modifier = Modifier.heightIn(min = 48.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                text = { Text(label, Modifier.fillMaxWidth(), style = style,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis) })
        }
    }
}

@Composable
private fun ChatLoadingState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.width(176.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
            HermesMulticolorIcon(HermesIconKind.AI, contentDescription = null, iconSize = 23.dp)
        }
        Text(uiText(R.string.ui_0675, "正在加载最近消息"), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 9.dp, bottom = 9.dp))
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
    readerMode: Boolean = false,
) {
    val skin = HermesSkin.current
    val councilMessages = remember(message.content) { parseCouncilAgentMessages(message.content) }
    val syntheticProcessing = message.role == MessageRole.ASSISTANT && message.isStreaming &&
        isSyntheticProcessingStatus(message.content)
    val visibleContent = if (syntheticProcessing) "" else message.content
    val visibleReasoning = message.reasoning.takeUnless { it.trim().trimEnd('.', '…') == uiText(R.string.ui_0676, "正在思考") }.orEmpty()
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
            if (!readerMode) Box(Modifier.padding(top = 1.dp).size(32.dp)) {
                if (showIdentity) {
                    UserAvatar(
                        uri = hermesAvatarUri,
                        displayName = hermesName,
                        size = 32.dp,
                        hermesFallback = true,
                    )
                }
            }
            Column(modifier = Modifier.weight(1f).padding(start = if (readerMode) 0.dp else 10.dp, end = if (readerMode) 0.dp else 4.dp, top = if (showIdentity) 3.dp else 0.dp)) {
                if (showIdentity) {
                    Text(
                        hermesName,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 3.dp),
                    )
                }
                if (visibleReasoning.isNotBlank()) {
                    ReasoningDisclosure(
                        reasoning = visibleReasoning,
                        streaming = message.isStreaming && visibleContent.isBlank() && runningToolCount == 0 && !syntheticProcessing,
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
                if (message.isStreaming && !(visibleReasoning.isNotBlank() && visibleContent.isBlank() && runningToolCount == 0 && !syntheticProcessing)) {
                    Row(
                        modifier = Modifier.padding(top = if (visibleContent.isBlank()) 2.dp else 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(15.dp), strokeWidth = 2.dp)
                        Text(
                            text = when {
                                runningToolCount > 0 || syntheticProcessing -> uiText(R.string.ui_0677, "正在处理…")
                                visibleContent.isBlank() -> uiText(R.string.ui_0678, "正在思考…")
                                else -> uiText(R.string.ui_0679, "正在继续…")
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
private fun ReasoningDisclosure(reasoning: String, streaming: Boolean) {
    var expanded by remember { mutableStateOf(streaming) }
    LaunchedEffect(streaming) {
        if (streaming) expanded = true
    }
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable { expanded = !expanded }
                .padding(horizontal = 7.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (streaming) {
                CircularProgressIndicator(modifier = Modifier.size(13.dp), strokeWidth = 1.7.dp)
            }
            Text(
                text = if (streaming) uiText(R.string.ui_0676, "正在思考") else uiText(R.string.ui_0680, "思考过程"),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = if (streaming) 7.dp else 0.dp),
            )
            Spacer(Modifier.weight(1f))
            HermesMulticolorIcon(
                kind = if (expanded) HermesIconKind.EXPAND_UP else HermesIconKind.EXPAND_DOWN,
                contentDescription = if (expanded) uiText(R.string.ui_0681, "收起思考过程") else uiText(R.string.ui_0682, "展开思考过程"),
                iconSize = 15.dp,
            )
        }
        AnimatedVisibility(visible = expanded) {
            Text(
                text = reasoning,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 6.dp, top = 3.dp),
            )
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
                    uiText(R.string.ui_0632, "专家会审"),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    uiText(R.string.ui_0683, "%1\$s 位专家已分别返回", messages.size),
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
                            uiText(R.string.ui_0684, "分工 · %1\$s", agent.task),
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
                    uiText(R.string.ui_0685, "参考来源 · %1\$s", sources.size),
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
                    HermesMulticolorIcon(HermesIconKind.OPEN_EXTERNAL, contentDescription = uiText(R.string.ui_0686, "打开来源"), iconSize = 15.dp)
                }
            }
        }
    }
}

@Composable
private fun AgentRequestCard(request: AgentRequest, onRespond: (AgentRequest, String) -> Unit) {
    DecisionCard(request, onRespond)
}

internal fun buildAgentRequestAnswer(
    request: AgentRequest,
    selectedValues: Set<String>,
    customAnswer: String,
): String = buildList {
    request.choices.forEach { choice ->
        if (choice.value in selectedValues) add(choice.value)
    }
    customAnswer.trim().takeIf(String::isNotBlank)?.let(::add)
}.joinToString(", ")

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
            uiText(R.string.ui_0687, "本轮执行完成"),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 10.dp),
        )
    }
}

@Composable
private fun Composer(
    inputFocusRequester: FocusRequester,
    inlineStop: Boolean,
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
                .padding(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 8.dp),
        ) {
            com.qingyu.hermescompanion.ui.component.VoiceRecoveryBar()
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
                            text = if (councilMode == CouncilMode.DEEP) uiText(R.string.ui_0688, "专家会审 · 深度") else uiText(R.string.ui_0689, "专家会审 · 快速 MoA"),
                            modifier = Modifier.weight(1f).padding(start = 8.dp),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        TextButton(colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer), onClick = onDisableCouncil) { Text(uiText(R.string.ui_0196, "关闭")) }
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
                        Text(uiText(R.string.ui_0690, "已有一条消息排队中"), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        TextButton(colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer), onClick = onCancelQueued) { Text(uiText(R.string.ui_0553, "取消")) }
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
                                    VoicePhase.LISTENING -> uiText(R.string.ui_0691, "正在录音")
                                    VoicePhase.TRANSCRIBING -> uiText(R.string.ui_0692, "正在识别")
                                    VoicePhase.ERROR -> uiText(R.string.ui_0693, "语音输入失败")
                                    else -> uiText(R.string.ui_0694, "语音输入")
                                },
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                voiceCapture.message.ifBlank { uiText(R.string.ui_0695, "再点麦克风结束录音") },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (voiceCapture.phase == VoicePhase.ERROR) {
                                    MaterialTheme.colorScheme.onErrorContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                maxLines = 2,
                            )
                        }
                        TextButton(colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer), onClick = onCancelVoice) {
                            Text(if (voiceCapture.phase == VoicePhase.ERROR) uiText(R.string.ui_0196, "关闭") else uiText(R.string.ui_0553, "取消"))
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
                        Text(uiText(R.string.ui_0696, "发送失败，内容已保留"), modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onErrorContainer)
                        TextButton(colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer), onClick = onRetryFailed, enabled = enabled && !isStreaming) { Text(uiText(R.string.ui_0697, "重新发送")) }
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
                        .horizontalScroll(rememberScrollState())
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    attachments.forEach { attachment ->
                        AttachmentChip(
                            attachment,
                            onOpen = { onPreviewAttachment(attachment) },
                            onRemove = { onRemoveAttachment(attachment.id) },
                        )
                    }
                    Text(
                        text = "${attachments.size}/10",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 10.dp),
                    )
                }
            }

            AssistantPanel(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(horizontal = 6.dp, vertical = 4.dp)) {
                    Row(Modifier.heightIn(min = 50.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onTools, enabled = enabled, modifier=Modifier.semantics {contentDescription=uiText(R.string.ui_0698, "添加内容")}) { AssistantIconWell("plus", MaterialTheme.colorScheme.onSurfaceVariant, Modifier.size(30.dp)) }
                        BasicTextField(value = draft, onValueChange = onDraftChange, enabled = enabled,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface), maxLines = 5,
                            modifier = Modifier.weight(1f).focusRequester(inputFocusRequester).padding(horizontal = 4.dp, vertical = 8.dp),
                            decorationBox = { inner -> Box {
                                if(draft.isBlank()) Text(if(enabled) uiText(R.string.ui_0699, "补充一句，或直接说…") else uiText(R.string.ui_0700, "正在加载对话…"), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .75f), fontSize = 16.sp)
                                inner()
                            } })
                        if(isStreaming && inlineStop) IconButton(onClick = onStop) { AssistantGlyph("stop", tint = MaterialTheme.colorScheme.error) }
                        else if(!isStreaming && (draft.isNotBlank() || attachments.isNotEmpty())) IconButton(onClick = onSend, enabled = enabled, modifier=Modifier.semantics {contentDescription=uiText(R.string.ui_0701, "发送")}) { AssistantGlyph("arrow", tint = AssistantBlue) }
                        else IconButton(onClick = onVoice, enabled = enabled && voiceCapture.phase != VoicePhase.TRANSCRIBING) { AssistantGlyph("wave", tint = if(voiceCapture.phase == VoicePhase.LISTENING) MaterialTheme.colorScheme.error else AssistantBlue) }
                    }
                    if(isStreaming && (draft.isNotBlank() || attachments.isNotEmpty())) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer), enabled = !isSteering && draft.isNotBlank() && attachments.isEmpty(), onClick = onSteer) { Text(if(isSteering) uiText(R.string.ui_0702, "追加中") else uiText(R.string.ui_0703, "追加要求")) }
                        TextButton(colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer), onClick = onQueue) { Text(if(hasQueuedMessage) uiText(R.string.ui_0704, "替换排队消息") else uiText(R.string.ui_0705, "排队发送")) }
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
                    Text(uiText(R.string.ui_0706, "正在读取 Hermes 命令…"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 9.dp))
                }
            }
            visibleCommands.forEachIndexed { index, command ->
                if (index == 0 || visibleCommands[index - 1].category != command.category) {
                    Text(
                        command.category.ifBlank { uiText(R.string.ui_0707, "Hermes 命令") },
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
            Text(uiText(R.string.ui_0707, "Hermes 命令"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(
                uiText(R.string.ui_0708, "点选后插入输入框，你仍可补充参数再发送。"),
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
                            if (query.isBlank()) Text(uiText(R.string.ui_0709, "搜索命令或用途"), color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    Text(uiText(R.string.ui_0710, "正在读取服务器命令…"), modifier = Modifier.padding(start = 9.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                filtered.isEmpty() -> Text(
                    uiText(R.string.ui_0711, "没有匹配的命令"),
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
                                    command.category.ifBlank { uiText(R.string.ui_0707, "Hermes 命令") },
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
                        HermesMulticolorIcon(HermesIconKind.BACK, contentDescription = uiText(R.string.ui_0712, "返回助理面板"))
                    }
                    Text(uiText(R.string.ui_0632, "专家会审"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                }
            } else {
                Text(uiText(R.string.ui_0632, "专家会审"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            }
            Text(
                uiText(R.string.ui_0713, "深度会审会把三位专家作为独立群成员展示，最后由 Hermes 统一裁决；专家之间不会开放式互聊。每次开启仅作用于下一条消息。"),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 5.dp, bottom = 14.dp),
            )
            CouncilOptionCard(
                title = uiText(R.string.ui_0714, "深度会审"),
                subtitle = uiText(R.string.ui_0715, "3 个独立专家并行首轮，主 Agent 裁决；仅在关键分歧未解决时追加 1 轮复核。"),
                badge = uiText(R.string.ui_0716, "推荐 · 更严谨"),
                selected = state.councilMode == CouncilMode.DEEP,
                enabled = !state.isModelSwitching,
                onClick = { onSelect(CouncilMode.DEEP) },
            )
            Spacer(Modifier.height(9.dp))
            CouncilOptionCard(
                title = uiText(R.string.ui_0717, "快速会审 · MoA"),
                subtitle = when {
                    currentIsMoa -> uiText(R.string.ui_0718, "使用当前 MoA 的参考模型并行分析，由聚合模型一次裁决。")
                    moaPreset != null -> uiText(R.string.ui_0719, "将当前会话切换到 %1\$s 后启用。", moaPreset.substringAfterLast('/'))
                    state.isModelsLoading -> uiText(R.string.ui_0720, "正在检查服务器上的 MoA 预设…")
                    else -> uiText(R.string.ui_0721, "服务器未提供 MoA 预设；可在 Hermes 中配置后使用。")
                },
                badge = uiText(R.string.ui_0722, "更快 · Token 可控"),
                selected = state.councilMode == CouncilMode.QUICK,
                enabled = quickAvailable && !state.isModelSwitching,
                onClick = { onSelect(CouncilMode.QUICK) },
            )
            if (state.isModelSwitching) {
                Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(17.dp), strokeWidth = 2.dp)
                    Text(uiText(R.string.ui_0723, "正在切换会审模型…"), modifier = Modifier.padding(start = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            TextButton(colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer), 
                onClick = { onSelect(CouncilMode.OFF) },
                modifier = Modifier.align(Alignment.End).padding(top = 8.dp),
            ) { Text(uiText(R.string.ui_0724, "关闭会审")) }
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
                HermesMulticolorIcon(HermesIconKind.CLOSE, contentDescription = uiText(R.string.ui_0725, "移除附件"), iconSize = 16.dp)
            }
        }
    }
}

@Composable
private fun EmptyConversation(
    onSuggestion: (String) -> Unit,
    hermesName: String,
    modifier: Modifier = Modifier,
    motion: MascotMotion? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (motion == null) HermesWelcomeAnimation(Modifier.size(168.dp), uiText(R.string.ui_0726, "%1\$s 欢迎动画", hermesName))
        else HermesMascot(motion, Modifier.size(168.dp))
        Text(text = uiText(R.string.ui_0727, "今天需要我做什么？"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Text(
            uiText(R.string.ui_0728, "可以提问，也可以直接交代任务"),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 3.dp),
        )
        Spacer(Modifier.height(12.dp))
        listOf(
            Triple(uiText(R.string.ui_0729, "帮我梳理今天最重要的三件事"), HermesIconKind.IDEA, HermesColors.extended.warningContainer),
            Triple(uiText(R.string.ui_0730, "总结一下最近工作的进展"), HermesIconKind.SUMMARIZE, MaterialTheme.colorScheme.secondaryContainer),
            Triple(uiText(R.string.ui_0731, "帮我安排接下来一周的计划"), HermesIconKind.PLAN, HermesColors.extended.successContainer),
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

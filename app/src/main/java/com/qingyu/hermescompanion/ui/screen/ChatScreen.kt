package com.qingyu.hermescompanion.ui.screen

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import com.qingyu.hermescompanion.model.MessageRole
import com.qingyu.hermescompanion.model.PendingAttachment
import com.qingyu.hermescompanion.model.SlashCommand
import com.qingyu.hermescompanion.model.ToolActivity
import com.qingyu.hermescompanion.model.ToolStatus
import com.qingyu.hermescompanion.ui.AppUiState
import com.qingyu.hermescompanion.ui.findChatImageTargets
import com.qingyu.hermescompanion.ui.component.HermesIconKind
import com.qingyu.hermescompanion.ui.component.HermesMark
import com.qingyu.hermescompanion.ui.component.HermesMulticolorIcon
import com.qingyu.hermescompanion.ui.component.HermesStatusIcon
import com.qingyu.hermescompanion.ui.component.HermesStatusKind
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
    onVoiceUnavailable: () -> Unit,
    onVoiceResult: (String) -> Unit,
    onLoadModels: () -> Unit,
    onSwitchModel: (String, String) -> Unit,
    onOpenArtifact: (com.qingyu.hermescompanion.model.ChatArtifact) -> Unit,
    onOpenWorkspace: () -> Unit,
    onOpenImage: (String, String) -> Unit,
    onOpenLink: (String) -> Unit,
    onLoadInlineImages: (List<String>) -> Unit,
) {
    val context = LocalContext.current
    val skin = HermesSkin.current
    val listState = rememberLazyListState()
    val listScope = rememberCoroutineScope()
    val showJumpToBottom by remember {
        derivedStateOf { listState.canScrollForward }
    }
    var assistantSheetVisible by remember { mutableStateOf(false) }
    var composerToolsVisible by remember { mutableStateOf(false) }
    var linkDialogVisible by remember { mutableStateOf(false) }
    var linkDraft by remember { mutableStateOf("") }
    val hermesName = state.userProfile.hermesDisplayName.ifBlank { "Hermes" }
    val isCurrentSessionStreaming = state.isStreaming &&
        state.streamingSessionId == state.selectedSession?.id
    val isCurrentSessionRecovering = state.isRecoveringConnection && isCurrentSessionStreaming
    val composerEnabled = !state.isStreaming || isCurrentSessionStreaming
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
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val words = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!words.isNullOrBlank()) {
                onVoiceResult(words)
            }
        }
    }

    LaunchedEffect(state.isBusy, state.messages.size) {
        if (!state.isBusy && state.messages.isNotEmpty()) {
            listState.scrollToItem(state.messages.lastIndex)
        }
    }
    val inlineImageTargets = remember(state.messages) {
        state.messages.flatMap { message -> findChatImageTargets(message.content) }.distinct()
    }
    LaunchedEffect(inlineImageTargets) {
        if (inlineImageTargets.isNotEmpty()) onLoadInlineImages(inlineImageTargets)
    }
    val scrollKey = state.messages.lastOrNull()?.content?.length ?: 0
    LaunchedEffect(scrollKey) {
        if (isCurrentSessionStreaming && state.messages.isNotEmpty()) {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: Int.MAX_VALUE
            if (lastVisible >= state.messages.lastIndex - 2) {
                delay(30)
                listState.animateScrollToItem(state.messages.lastIndex + if (state.toolActivities.isEmpty()) 0 else 1)
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
                IconButton(
                    onClick = { assistantSheetVisible = true },
                    modifier = Modifier.padding(end = 12.dp).size(40.dp).clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                ) {
                    HermesMulticolorIcon(HermesIconKind.AI, contentDescription = "$hermesName 助理面板", iconSize = 20.dp)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
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
                        hermesAvatarUri = state.userProfile.hermesAvatarUri,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }

                else -> {
                    Box(Modifier.fillMaxSize()) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = HermesSpacing.sm, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(15.dp),
                        ) {
                            itemsIndexed(state.messages, key = { _, item -> item.id }) { index, message ->
                                if (shouldShowMessageTime(state.messages.getOrNull(index - 1)?.createdAt, message.createdAt)) {
                                    ConversationTime(messageTimeLabel(message.createdAt))
                                }
                                MessageItem(
                                    message = message,
                                    onOpenImage = onOpenImage,
                                    onOpenLink = onOpenLink,
                                    userName = state.userProfile.displayName.ifBlank { state.username.ifBlank { "我" } },
                                    userAvatarUri = state.userProfile.avatarUri,
                                    hermesName = hermesName,
                                    hermesAvatarUri = state.userProfile.hermesAvatarUri,
                                    inlineImagePreviews = state.inlineImagePreviews,
                                )
                            }
                            if (isCurrentSessionStreaming && state.toolActivities.isNotEmpty()) {
                                item(key = "tool-activities") {
                                    ToolActivityPanel(state.toolActivities, hermesName)
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
            onVoice = {
                if (!state.voicePreferences.enabled) {
                    onVoiceUnavailable()
                } else {
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, state.voicePreferences.language)
                        putExtra(RecognizerIntent.EXTRA_PROMPT, "请说话")
                    }
                    if (intent.resolveActivity(context.packageManager) == null) {
                        onVoiceUnavailable()
                    } else {
                        try {
                            speechLauncher.launch(intent)
                        } catch (_: ActivityNotFoundException) {
                            onVoiceUnavailable()
                        } catch (_: SecurityException) {
                            onVoiceUnavailable()
                        }
                    }
                }
            },
            onRemoveAttachment = onRemoveAttachment,
            onPreviewAttachment = { attachment ->
                attachment.dataUrl?.let { onOpenImage(it, attachment.name) }
            },
            onSend = onSend,
            failedSend = state.failedSend != null,
            onRetryFailed = onRetryFailed,
            onStop = onStop,
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
        )
    }
    if (composerToolsVisible) {
        ComposerToolsSheet(
            onDismiss = { composerToolsVisible = false },
            onPickFiles = {
                fileLauncher.launch(arrayOf("text/*", "application/json", "application/xml", "application/x-yaml", "application/pdf"))
            },
            onPickImages = { imageLauncher.launch(arrayOf("image/*")) },
            onAddLink = { linkDialogVisible = true },
            onOpenWorkspace = onOpenWorkspace,
            onInsertPrompt = { snippet ->
                onDraftChange(listOf(state.draft, snippet).filter(String::isNotBlank).joinToString("\n"))
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
            modifier = Modifier.clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f))
                .padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun MessageItem(
    message: ChatMessage,
    onOpenImage: (String, String) -> Unit,
    onOpenLink: (String) -> Unit,
    userName: String,
    userAvatarUri: String,
    hermesName: String,
    hermesAvatarUri: String,
    inlineImagePreviews: Map<String, com.qingyu.hermescompanion.model.ImagePreview>,
) {
    val skin = HermesSkin.current
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val userBubbleMaxWidth = maxWidth * 0.76f
        when (message.role) {
        MessageRole.USER -> Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.Top,
        ) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    userName,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 2.dp, bottom = 4.dp),
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
            Box(Modifier.padding(start = 8.dp, top = 1.dp)) {
                UserAvatar(
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
            Box(Modifier.padding(top = 1.dp)) {
                if (hermesAvatarUri.isNotBlank()) {
                    UserAvatar(
                        uri = hermesAvatarUri,
                        displayName = hermesName,
                        size = 32.dp,
                        hermesFallback = true,
                    )
                } else {
                    HermesMark(compact = true)
                }
            }
            Column(modifier = Modifier.weight(1f).padding(start = 11.dp, end = 4.dp, top = 3.dp)) {
                Text(
                    hermesName,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
                    if (message.content.isNotBlank()) {
                        MarkdownContent(
                            markdown = message.content,
                            onOpenImage = onOpenImage,
                            onOpenLink = onOpenLink,
                            inlineImagePreviews = inlineImagePreviews,
                        )
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
                            modifier = Modifier.padding(top = if (message.content.isBlank()) 2.dp else 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(15.dp), strokeWidth = 2.dp)
                            Text(
                                text = if (message.content.isBlank()) "正在思考…" else "正在继续…",
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
private fun ToolActivityPanel(activities: List<ToolActivity>, hermesName: String) {
    val running = activities.count { it.status == ToolStatus.RUNNING }
    val completed = activities.count { it.status == ToolStatus.COMPLETED }
    Surface(
        modifier = Modifier.fillMaxWidth().padding(start = 43.dp, end = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 0.dp,
    ) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            if (running > 0) {
                CircularProgressIndicator(Modifier.size(17.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.secondary)
            } else {
                HermesMulticolorIcon(
                    HermesIconKind.VERIFIED,
                    contentDescription = null,
                    iconSize = 18.dp,
                )
            }
            Text(
                text = when {
                    running > 0 -> "$hermesName 正在处理 · $running 项进行中"
                    completed > 0 -> "$hermesName 已完成 $completed 项，正在整理回复"
                    else -> "$hermesName 正在处理"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 9.dp),
            )
        }
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
    onVoice: () -> Unit,
    onRemoveAttachment: (String) -> Unit,
    onPreviewAttachment: (PendingAttachment) -> Unit,
    onSend: () -> Unit,
    failedSend: Boolean,
    onRetryFailed: () -> Unit,
    onStop: () -> Unit,
) {
    val skin = HermesSkin.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent,
    ) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(start = HermesSpacing.sm, end = HermesSpacing.sm, top = 7.dp, bottom = 7.dp),
        ) {
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
                    .shadow(if (skin.glass) skin.shadowElevation.dp else 0.dp, RoundedCornerShape(22.dp)),
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
                border = BorderStroke(
                    if (skin.glass) 0.9.dp else 0.7.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = skin.borderAlpha),
                ),
            ) {
                Column(modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp)) {
                    BasicTextField(
                        value = draft,
                        onValueChange = onDraftChange,
                        enabled = enabled,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                        maxLines = 5,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 40.dp, max = 112.dp).padding(horizontal = 4.dp, vertical = 5.dp),
                        decorationBox = { inner ->
                            Box(contentAlignment = Alignment.TopStart) {
                                if (draft.isBlank()) {
                                    Text(
                                        if (enabled) "给 $assistantName 发消息…" else "另一段对话正在后台生成…",
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
                            background = MaterialTheme.colorScheme.primaryContainer,
                            enabled = enabled,
                            onClick = onTools,
                        )
                        Spacer(Modifier.width(7.dp))
                        ComposerAction(
                            icon = HermesIconKind.MICROPHONE,
                            description = "语音输入",
                            background = HermesColors.extended.successContainer,
                            enabled = enabled,
                            onClick = onVoice,
                        )
                        Spacer(Modifier.weight(1f))
                        Box(
                            modifier = Modifier.size(42.dp).clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (isStreaming) {
                                        Brush.linearGradient(listOf(MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.errorContainer))
                                    } else {
                                        Brush.linearGradient(listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.secondaryContainer))
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
                                iconSize = 21.dp,
                            )
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

@Composable
private fun ComposerAction(
    icon: HermesIconKind,
    description: String,
    background: Color,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(background)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        HermesMulticolorIcon(
            icon,
            contentDescription = description,
            iconSize = 20.dp,
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
    hermesAvatarUri: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (hermesAvatarUri.isNotBlank()) {
                UserAvatar(
                    uri = hermesAvatarUri,
                    displayName = hermesName,
                    size = 48.dp,
                    hermesFallback = true,
                )
            } else {
                HermesMark()
            }
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(text = "今天需要我做什么？", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("可以提问，也可以直接交代任务", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(14.dp))
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
                border = if (HermesSkin.current.glass) BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.62f)) else null,
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

package com.qingyu.hermescompanion.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.qingyu.hermescompanion.model.ChatArtifact
import com.qingyu.hermescompanion.model.ModelProvider
import com.qingyu.hermescompanion.model.TodoStatus
import com.qingyu.hermescompanion.ui.AppUiState
import com.qingyu.hermescompanion.ui.component.HermesIconKind
import com.qingyu.hermescompanion.ui.component.HermesMark
import com.qingyu.hermescompanion.ui.component.HermesMulticolorIcon
import com.qingyu.hermescompanion.ui.component.UserAvatar
import com.qingyu.hermescompanion.ui.theme.HermesSkin
import com.qingyu.hermescompanion.ui.theme.HermesColors

private enum class AssistantPage { HOME, MODELS, ARTIFACTS, TODOS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatAssistantSheet(
    state: AppUiState,
    onDismiss: () -> Unit,
    onLoadModels: () -> Unit,
    onSwitchModel: (String, String) -> Unit,
    onOpenArtifact: (ChatArtifact) -> Unit,
) {
    val skin = HermesSkin.current
    var page by remember { mutableStateOf(AssistantPage.HOME) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetGesturesEnabled = false,
        shape = RoundedCornerShape(
            topStart = if (skin.glass) 24.dp else 16.dp,
            topEnd = if (skin.glass) 24.dp else 16.dp,
        ),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        dragHandle = {
            HermesMulticolorIcon(
                HermesIconKind.DRAG_HANDLE,
                contentDescription = null,
                modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
                iconSize = 30.dp,
            )
        },
    ) {
        when (page) {
            AssistantPage.HOME -> AssistantHome(
                state = state,
                onNavigate = { next ->
                    page = next
                    if (next == AssistantPage.MODELS) onLoadModels()
                },
            )
            AssistantPage.MODELS -> ModelPickerContent(
                state = state,
                onBack = { page = AssistantPage.HOME },
                onLoadModels = onLoadModels,
                onSwitchModel = onSwitchModel,
            )
            AssistantPage.ARTIFACTS -> ArtifactContent(
                state = state,
                onBack = { page = AssistantPage.HOME },
                onOpenArtifact = onOpenArtifact,
            )
            AssistantPage.TODOS -> TodoContent(state, onBack = { page = AssistantPage.HOME })
        }
    }
}

@Composable
private fun AssistantHome(state: AppUiState, onNavigate: (AssistantPage) -> Unit) {
    val hermesName = state.userProfile.hermesDisplayName.ifBlank { "Hermes" }
    val model = state.selectedSession?.model.orEmpty()
        .ifBlank { state.modelCatalog.currentModel }
        .ifBlank { "跟随 Hermes 默认模型" }
    Column(modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 28.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (state.userProfile.hermesAvatarUri.isNotBlank()) {
                UserAvatar(
                    state.userProfile.hermesAvatarUri,
                    hermesName,
                    48.dp,
                    hermesFallback = true,
                )
            } else {
                HermesMark()
            }
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text("$hermesName 助理面板", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("管理当前对话，不改变其他会话", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.size(18.dp))
        AssistantEntry(
            icon = HermesIconKind.MODEL,
            title = "切换模型",
            subtitle = model.substringAfterLast('/'),
            background = MaterialTheme.colorScheme.primaryContainer,
            onClick = { onNavigate(AssistantPage.MODELS) },
        )
        AssistantEntry(
            icon = HermesIconKind.ARTIFACT,
            title = "聊天产物",
            subtitle = if (state.chatArtifacts.isEmpty()) "暂未识别到文件" else "${state.chatArtifacts.size} 个文件或文档",
            background = HermesColors.extended.successContainer,
            onClick = { onNavigate(AssistantPage.ARTIFACTS) },
        )
        AssistantEntry(
            icon = HermesIconKind.TODO,
            title = "待办列表",
            subtitle = if (state.chatTodos.isEmpty()) "当前对话暂无待办" else "${state.chatTodos.count { it.status == TodoStatus.COMPLETED }}/${state.chatTodos.size} 已完成",
            background = HermesColors.extended.warningContainer,
            onClick = { onNavigate(AssistantPage.TODOS) },
        )
    }
}

@Composable
private fun AssistantEntry(
    icon: HermesIconKind,
    title: String,
    subtitle: String,
    background: Color,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.46f),
        tonalElevation = 0.dp,
    ) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(13.dp)).background(background), contentAlignment = Alignment.Center) {
                HermesMulticolorIcon(icon, contentDescription = null, iconSize = 22.dp)
            }
            Column(modifier = Modifier.weight(1f).padding(start = 11.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            HermesMulticolorIcon(HermesIconKind.CHEVRON_RIGHT, contentDescription = null)
        }
    }
}

@Composable
private fun SheetHeader(title: String, subtitle: String, onBack: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { HermesMulticolorIcon(HermesIconKind.BACK, contentDescription = "返回") }
        Column(modifier = Modifier.padding(start = 2.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ModelPickerContent(
    state: AppUiState,
    onBack: () -> Unit,
    onLoadModels: () -> Unit,
    onSwitchModel: (String, String) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var pendingModel by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { onLoadModels() }
    LaunchedEffect(state.isModelSwitching) {
        if (!state.isModelSwitching) pendingModel = null
    }
    val current = state.selectedSession?.model.orEmpty().ifBlank { state.modelCatalog.currentModel }
    val providers = remember(state.modelCatalog.providers, query) {
        if (query.isBlank()) state.modelCatalog.providers
        else state.modelCatalog.providers.mapNotNull { provider ->
            val models = provider.models.filter { it.contains(query, ignoreCase = true) || provider.name.contains(query, ignoreCase = true) }
            provider.copy(models = models).takeIf { models.isNotEmpty() }
        }
    }
    Column(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.84f).padding(bottom = 18.dp)) {
        SheetHeader("切换模型", "仅影响当前会话", onBack)
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(15.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f),
        ) {
            Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                HermesMulticolorIcon(HermesIconKind.SEARCH, contentDescription = null, iconSize = 20.dp)
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                    modifier = Modifier.weight(1f).padding(start = 9.dp),
                    decorationBox = { inner ->
                        Box { if (query.isBlank()) Text("搜索已配置模型", color = MaterialTheme.colorScheme.onSurfaceVariant); inner() }
                    },
                )
            }
        }
        when {
            state.isModelsLoading -> SheetLoading("正在读取可用模型…")
            providers.isEmpty() -> SheetEmpty("没有找到已配置的模型", "请先在 Hermes 服务器完成模型配置")
            else -> LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 12.dp),
            ) {
                providers.forEach { provider ->
                    item(key = "provider-${provider.slug}") { ProviderHeader(provider) }
                    items(provider.models, key = { "${provider.slug}-$it" }) { model ->
                        ModelRow(
                            model = model,
                            selected = model == current,
                            enabled = !state.isModelSwitching,
                            switching = state.isModelSwitching && model == pendingModel,
                            onClick = {
                                pendingModel = model
                                onSwitchModel(provider.slug, model)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProviderHeader(provider: ModelProvider) {
    Text(
        provider.name,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 4.dp),
    )
}

@Composable
private fun ModelRow(model: String, selected: Boolean, enabled: Boolean, switching: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(35.dp).clip(RoundedCornerShape(11.dp))
                .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
            contentAlignment = Alignment.Center,
        ) {
            HermesMulticolorIcon(HermesIconKind.MODEL, contentDescription = null, iconSize = 19.dp)
        }
        Text(model.substringAfterLast('/'), modifier = Modifier.weight(1f).padding(start = 11.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
        when {
            switching -> CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            selected -> HermesMulticolorIcon(HermesIconKind.CHECK, contentDescription = "当前模型")
        }
    }
    HorizontalDivider(modifier = Modifier.padding(start = 64.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
}

@Composable
private fun ArtifactContent(state: AppUiState, onBack: () -> Unit, onOpenArtifact: (ChatArtifact) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
        SheetHeader("聊天产物", "从当前会话的回复与工具结果中整理", onBack)
        if (state.chatArtifacts.isEmpty()) {
            SheetEmpty("还没有聊天产物", "Hermes 生成文档、图片或安装包后会显示在这里")
        } else {
            LazyColumn(modifier = Modifier.heightIn(max = 520.dp)) {
                items(state.chatArtifacts, key = ChatArtifact::path) { artifact ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onOpenArtifact(artifact) }.padding(horizontal = 18.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(HermesColors.extended.successContainer), contentAlignment = Alignment.Center) {
                            HermesMulticolorIcon(HermesIconKind.ARTIFACT, contentDescription = null, iconSize = 21.dp)
                        }
                        Column(modifier = Modifier.weight(1f).padding(start = 11.dp)) {
                            Text(artifact.name, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(artifact.kind, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        HermesMulticolorIcon(HermesIconKind.CHEVRON_RIGHT, contentDescription = null)
                    }
                }
            }
        }
    }
}

@Composable
private fun TodoContent(state: AppUiState, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
        SheetHeader("待办列表", "同步 Hermes 在当前会话中维护的计划", onBack)
        if (state.chatTodos.isEmpty()) {
            SheetEmpty("当前没有待办", "当 Hermes 使用待办工具规划任务时会自动出现")
        } else {
            LazyColumn(modifier = Modifier.heightIn(max = 520.dp)) {
                items(state.chatTodos, key = { it.id }) { todo ->
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp), verticalAlignment = Alignment.Top) {
                        val icon = when (todo.status) {
                            TodoStatus.COMPLETED -> HermesIconKind.CHECK_CIRCLE
                            TodoStatus.IN_PROGRESS -> HermesIconKind.PENDING
                            TodoStatus.PENDING -> HermesIconKind.UNCHECKED
                        }
                        HermesMulticolorIcon(icon, contentDescription = null, modifier = Modifier.padding(top = 2.dp), iconSize = 20.dp)
                        Text(todo.content, modifier = Modifier.weight(1f).padding(start = 11.dp), color = if (todo.status == TodoStatus.COMPLETED) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }
}

@Composable
private fun SheetLoading(label: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 34.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(Modifier.size(19.dp), strokeWidth = 2.dp)
        Spacer(Modifier.width(10.dp))
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SheetEmpty(title: String, subtitle: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 38.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        HermesMulticolorIcon(HermesIconKind.AI, contentDescription = null, iconSize = 31.dp)
        Text(title, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 10.dp))
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
    }
}

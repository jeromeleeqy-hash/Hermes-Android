package com.qingyu.hermescompanion.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.qingyu.hermescompanion.model.ApprovalSettings
import com.qingyu.hermescompanion.model.ConversationStyleSettings
import com.qingyu.hermescompanion.model.FallbackModel
import com.qingyu.hermescompanion.model.HermesSession
import com.qingyu.hermescompanion.model.McpServerInfo
import com.qingyu.hermescompanion.model.MemoryContextSettings
import com.qingyu.hermescompanion.model.ModelCatalog
import com.qingyu.hermescompanion.model.ModelChoice
import com.qingyu.hermescompanion.model.ModelProvider
import com.qingyu.hermescompanion.model.ServerModelSettings
import com.qingyu.hermescompanion.model.ServerSkill
import com.qingyu.hermescompanion.model.ToolsetInfo
import com.qingyu.hermescompanion.ui.AppUiState
import com.qingyu.hermescompanion.ui.component.GlassPanel
import com.qingyu.hermescompanion.ui.component.HermesIconKind
import com.qingyu.hermescompanion.ui.component.HermesMulticolorIcon
import com.qingyu.hermescompanion.ui.component.HermesSegmentedControl
import com.qingyu.hermescompanion.ui.format.ellipsizeSessionTitle
import com.qingyu.hermescompanion.ui.format.sessionTimeLabel
import com.qingyu.hermescompanion.ui.theme.HermesSpacing

private enum class CapabilityTab(val label: String) { SKILLS("技能"), TOOLS("工具集"), MCP("MCP") }

@Composable
fun SkillsToolsScreen(
    state: AppUiState,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onSkillClick: (ServerSkill) -> Unit,
    onSkillToggle: (ServerSkill) -> Unit,
    onToolsetToggle: (ToolsetInfo) -> Unit,
    onMcpToggle: (McpServerInfo) -> Unit,
    onCloseSkill: () -> Unit,
) {
    var tab by remember { mutableStateOf(CapabilityTab.SKILLS) }
    Column(Modifier.fillMaxSize().padding(contentPadding)) {
        SettingsHeader("技能与工具", "修改后通常从下次会话生效", onBack, onRefresh, state.isAdvancedSettingsLoading)
        HermesSegmentedControl(
            items = CapabilityTab.entries.map(CapabilityTab::label),
            selectedIndex = tab.ordinal,
            onSelect = { tab = CapabilityTab.entries[it] },
            modifier = Modifier.fillMaxWidth().padding(horizontal = HermesSpacing.page, vertical = 6.dp),
        )
        if (state.isAdvancedSettingsLoading && state.serverSkills.isEmpty() && state.toolsets.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 2.4.dp) }
        } else {
            LazyColumn(contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 24.dp)) {
                when (tab) {
                    CapabilityTab.SKILLS -> items(state.serverSkills, key = { it.name }) { skill ->
                        CapabilityRow(
                            title = skill.name,
                            subtitle = listOf(skill.category, skill.description).filter(String::isNotBlank).joinToString(" · "),
                            icon = HermesIconKind.AI,
                            checked = skill.enabled,
                            busy = state.settingsActionKey == "skill:${skill.name}",
                            onClick = { onSkillClick(skill) },
                            onToggle = { onSkillToggle(skill) },
                        )
                    }
                    CapabilityTab.TOOLS -> items(state.toolsets, key = { it.name }) { tool ->
                        CapabilityRow(
                            title = tool.label,
                            subtitle = tool.description.ifBlank { "${tool.tools.size} 个工具" },
                            icon = HermesIconKind.TODO,
                            checked = tool.enabled,
                            busy = state.settingsActionKey == "toolset:${tool.name}",
                            onClick = null,
                            onToggle = { onToolsetToggle(tool) },
                        )
                    }
                    CapabilityTab.MCP -> items(state.mcpServers, key = { it.name }) { server ->
                        CapabilityRow(
                            title = server.name,
                            subtitle = listOf(server.transport, server.status, "${server.toolCount} 个工具").filter(String::isNotBlank).joinToString(" · "),
                            icon = HermesIconKind.CONNECTION,
                            checked = server.enabled,
                            busy = state.settingsActionKey == "mcp:${server.name}",
                            onClick = null,
                            onToggle = { onMcpToggle(server) },
                        )
                    }
                }
            }
        }
    }

    state.selectedSkill?.let { skill ->
        AlertDialog(
            onDismissRequest = onCloseSkill,
            shape = RoundedCornerShape(16.dp),
            title = { Text(skill.name) },
            text = {
                Column(Modifier.fillMaxWidth().heightIn(max = 520.dp).verticalScroll(rememberScrollState())) {
                    if (state.settingsActionKey == "skill-content:${skill.name}") {
                        Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp) }
                    } else {
                        Text(skill.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (state.selectedSkillContent.isNotBlank()) {
                            Text(state.selectedSkillContent, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 12.dp))
                        }
                        HorizontalDivider(modifier = Modifier.padding(top = 10.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                        TextButton(onClick = { onSkillToggle(skill) }) {
                            Text(if (skill.enabled) "停用技能" else "启用技能")
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = onCloseSkill) { Text("关闭") } },
        )
    }
}

@Composable
private fun CapabilityRow(
    title: String,
    subtitle: String,
    icon: HermesIconKind,
    checked: Boolean,
    busy: Boolean,
    onClick: (() -> Unit)?,
    onToggle: () -> Unit,
) {
    GlassPanel(modifier = Modifier.fillMaxWidth().padding(bottom = 7.dp).then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
            HermesMulticolorIcon(icon, null, iconSize = 18.dp)
            Column(Modifier.weight(1f).padding(start = 8.dp)) {
                Text(title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (subtitle.isNotBlank()) Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (busy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            else Switch(checked = checked, onCheckedChange = { onToggle() }, modifier = Modifier.scale(0.78f))
        }
    }
}

private data class ModelPickerTarget(val kind: String, val key: String = "")

@Composable
fun ModelSettingsScreen(
    state: AppUiState,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onSave: (ServerModelSettings) -> Unit,
    onAddProvider: (String, String, String, String, String) -> Unit,
) {
    val initial = state.serverSettings.models
    val initialMain = remember(initial, state.modelCatalog.currentProvider, state.modelCatalog.currentModel) {
        ModelChoice(
            provider = initial.provider.ifBlank { state.modelCatalog.currentProvider },
            model = initial.model.ifBlank { state.modelCatalog.currentModel },
        )
    }
    var main by remember(initialMain) { mutableStateOf(initialMain) }
    var effort by remember(initial) { mutableStateOf(initial.reasoningEffort.ifBlank { "medium" }) }
    var contextLength by remember(initial) { mutableStateOf(initial.contextLength.takeIf { it > 0 }?.toString().orEmpty()) }
    var auxiliary by remember(initial) { mutableStateOf(initial.auxiliary) }
    val references = remember(initial) { mutableStateListOf<String>().apply { addAll(initial.moaReferenceModels) } }
    var aggregator by remember(initial) { mutableStateOf(initial.moaAggregatorModel) }
    val fallbacks = remember(initial) { mutableStateListOf<FallbackModel>().apply { addAll(initial.fallbackModels) } }
    var pickerTarget by remember { mutableStateOf<ModelPickerTarget?>(null) }
    var showEffort by remember { mutableStateOf(false) }
    var showProviderDialog by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(contentPadding)) {
        SettingsHeader("模型设置", "配置 Hermes 新会话使用的模型", onBack, busy = state.isAdvancedSettingsLoading)
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = HermesSpacing.page),
        ) {
        SettingsSection {
            SelectRow("模型", modelChoiceLabel(main)) { pickerTarget = ModelPickerTarget("main") }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.42f))
            SelectRow("推理强度", reasoningLabel(effort)) { showEffort = true }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.42f))
            NumberField("上下文窗口（tokens）", contextLength) { contextLength = it.filter(Char::isDigit) }
        }
        SettingsSection {
            AUXILIARY_LABELS.forEachIndexed { index, (key, label) ->
                SelectRow(label, modelChoiceLabel(auxiliary[key] ?: ModelChoice())) {
                    pickerTarget = ModelPickerTarget("aux", key)
                }
                if (index != AUXILIARY_LABELS.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.42f))
            }
        }
        SettingsSection {
            references.forEachIndexed { index, spec ->
                EditableModelRow("参考模型 ${index + 1}", spec, onEdit = { pickerTarget = ModelPickerTarget("reference", index.toString()) }) {
                    references.removeAt(index)
                }
            }
            TextButton(onClick = { pickerTarget = ModelPickerTarget("reference", "new") }) {
                HermesMulticolorIcon(HermesIconKind.ADD, null, iconSize = 18.dp); Text("添加参考模型", modifier = Modifier.padding(start = 6.dp))
            }
            SelectRow("聚合模型", aggregator.ifBlank { "未指定" }) { pickerTarget = ModelPickerTarget("aggregator") }
        }
        SettingsSection {
            fallbacks.forEachIndexed { index, fallback ->
                EditableModelRow("备用模型 ${index + 1}", "${fallback.provider}:${fallback.model}", onEdit = { pickerTarget = ModelPickerTarget("fallback", index.toString()) }) {
                    fallbacks.removeAt(index)
                }
            }
            TextButton(onClick = { pickerTarget = ModelPickerTarget("fallback", "new") }) {
                HermesMulticolorIcon(HermesIconKind.ADD, null, iconSize = 18.dp); Text("添加备用模型", modifier = Modifier.padding(start = 6.dp))
            }
        }
        OutlinedButton(onClick = { showProviderDialog = true }, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) { Text("增加模型提供商") }
        Button(
            onClick = {
                onSave(
                    ServerModelSettings(
                        provider = main.provider,
                        model = main.model,
                        reasoningEffort = effort,
                        contextLength = contextLength.toIntOrNull() ?: 0,
                        auxiliary = auxiliary,
                        fallbackModels = fallbacks.toList(),
                        moaReferenceModels = references.toList(),
                        moaAggregatorModel = aggregator,
                    ),
                )
            },
            enabled = !state.isAdvancedSettingsLoading && main.provider.isNotBlank() && main.model.isNotBlank(),
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 28.dp),
        ) { Text("保存模型设置") }
        }
    }

    pickerTarget?.let { target ->
        ModelPickerDialog(
            catalog = state.modelCatalog,
            allowAutomatic = target.kind == "aux",
            onDismiss = { pickerTarget = null },
            onSelect = { choice ->
                when (target.kind) {
                    "main" -> main = choice
                    "aux" -> auxiliary = auxiliary + (target.key to choice)
                    "aggregator" -> aggregator = choice.toSpec()
                    "reference" -> if (target.key == "new") references.add(choice.toSpec()) else references[target.key.toInt()] = choice.toSpec()
                    "fallback" -> {
                        val value = FallbackModel(choice.provider, choice.model)
                        if (target.key == "new") fallbacks.add(value) else fallbacks[target.key.toInt()] = value
                    }
                }
                pickerTarget = null
            },
        )
    }
    if (showEffort) ChoiceDialog("默认推理强度", REASONING_LEVELS, effort, { showEffort = false }) { effort = it; showEffort = false }
    if (showProviderDialog) AddProviderDialog(
        busy = state.isAdvancedSettingsLoading,
        onDismiss = { showProviderDialog = false },
        onAdd = { id, name, url, model, key -> onAddProvider(id, name, url, model, key); showProviderDialog = false },
    )
}

@Composable
fun ConversationStyleScreen(
    settings: ConversationStyleSettings,
    loading: Boolean,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onSave: (ConversationStyleSettings) -> Unit,
) {
    var personality by remember(settings) { mutableStateOf(settings.personality.ifBlank { "helpful" }) }
    var timezone by remember(settings) { mutableStateOf(settings.timezone.ifBlank { "Asia/Shanghai" }) }
    var showReasoning by remember(settings) { mutableStateOf(settings.showReasoning) }
    var choosePersonality by remember { mutableStateOf(false) }
    var chooseTimezone by remember { mutableStateOf(false) }
    SettingsForm("对话风格", "控制 Hermes 的表达方式与时间基准", contentPadding, onBack) {
        SettingsSection {
            SelectRow("人格", personalityLabel(personality)) { choosePersonality = true }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.42f))
            SelectRow("时区", timezone) { chooseTimezone = true }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.42f))
            ToggleSetting("显示推理过程", "在支持的模型上显示思考内容", showReasoning) { showReasoning = it }
        }
        SaveButton("保存对话风格", loading) { onSave(ConversationStyleSettings(personality, timezone, showReasoning)) }
    }
    if (choosePersonality) ChoiceDialog("选择人格", PERSONALITIES.map { it.first }, personality, { choosePersonality = false }) { personality = it; choosePersonality = false }
    if (chooseTimezone) ChoiceDialog("选择时区", TIMEZONES, timezone, { chooseTimezone = false }) { timezone = it; chooseTimezone = false }
}

@Composable
fun ApprovalSettingsScreen(
    settings: ApprovalSettings,
    loading: Boolean,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onSave: (ApprovalSettings) -> Unit,
) {
    var mode by remember(settings) { mutableStateOf(settings.mode) }
    var timeout by remember(settings) { mutableStateOf(settings.timeoutSeconds.toString()) }
    var chooseMode by remember { mutableStateOf(false) }
    SettingsForm("审批模式", "控制危险命令的人工确认方式", contentPadding, onBack) {
        SettingsSection {
            SelectRow("审批模式", approvalModeLabel(mode)) { chooseMode = true }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.42f))
            NumberField("审批超时（秒）", timeout) { timeout = it.filter(Char::isDigit) }
        }
        if (mode == "off") Text("关闭审批会跳过危险命令确认，仅建议在可信的隔离环境中使用。", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        SaveButton("保存审批设置", loading) { onSave(ApprovalSettings(mode, timeout.toIntOrNull() ?: 60)) }
    }
    if (chooseMode) ChoiceDialog("选择审批模式", APPROVAL_MODES.map { it.first }, mode, { chooseMode = false }) { mode = it; chooseMode = false }
}

@Composable
fun MemoryContextScreen(
    settings: MemoryContextSettings,
    loading: Boolean,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onSave: (MemoryContextSettings) -> Unit,
) {
    var memoryEnabled by remember(settings) { mutableStateOf(settings.memoryEnabled) }
    var profileEnabled by remember(settings) { mutableStateOf(settings.userProfileEnabled) }
    var memoryBudget by remember(settings) { mutableStateOf(settings.memoryCharLimit.toString()) }
    var profileBudget by remember(settings) { mutableStateOf(settings.userCharLimit.toString()) }
    var compressionEnabled by remember(settings) { mutableStateOf(settings.compressionEnabled) }
    var threshold by remember(settings) { mutableStateOf(settings.compressionThreshold.toString()) }
    var target by remember(settings) { mutableStateOf(settings.compressionTargetRatio.toString()) }
    var protect by remember(settings) { mutableStateOf(settings.protectLastMessages.toString()) }
    SettingsForm("记忆与上下文", "管理持久记忆、用户画像与自动压缩", contentPadding, onBack) {
        SettingsSection {
            ToggleSetting("持久记忆", "允许 Hermes 跨会话维护 MEMORY.md", memoryEnabled) { memoryEnabled = it }
            ToggleSetting("用户画像", "允许 Hermes 维护 USER.md 用户画像", profileEnabled) { profileEnabled = it }
            NumberField("记忆预算长度（字符）", memoryBudget) { memoryBudget = it.filter(Char::isDigit) }
            NumberField("画像预算长度（字符）", profileBudget) { profileBudget = it.filter(Char::isDigit) }
        }
        SettingsSection {
            ToggleSetting("自动压缩", "接近上下文窗口时自动总结旧消息", compressionEnabled) { compressionEnabled = it }
            DecimalField("压缩阈值（0.10–0.95）", threshold) { threshold = it.filter { ch -> ch.isDigit() || ch == '.' } }
            DecimalField("压缩目标（0.05–0.80）", target) { target = it.filter { ch -> ch.isDigit() || ch == '.' } }
            NumberField("保护最近消息数", protect) { protect = it.filter(Char::isDigit) }
        }
        SaveButton("保存记忆与上下文", loading) {
            onSave(
                MemoryContextSettings(
                    memoryEnabled,
                    profileEnabled,
                    memoryBudget.toIntOrNull() ?: 2200,
                    profileBudget.toIntOrNull() ?: 1375,
                    compressionEnabled,
                    threshold.toDoubleOrNull() ?: 0.50,
                    target.toDoubleOrNull() ?: 0.20,
                    protect.toIntOrNull() ?: 20,
                ),
            )
        }
    }
}

@Composable
fun ArchivedSessionsScreen(
    state: AppUiState,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onRestore: (HermesSession) -> Unit,
    onDelete: (HermesSession) -> Unit,
) {
    var deleteTarget by remember { mutableStateOf<HermesSession?>(null) }
    Column(Modifier.fillMaxSize().padding(contentPadding)) {
        SettingsHeader("已归档对话", "恢复后会重新出现在首页", onBack, onRefresh, state.isAdvancedSettingsLoading)
        when {
            state.isAdvancedSettingsLoading && state.archivedSessions.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            state.archivedSessions.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("暂无归档对话", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            else -> LazyColumn(contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 24.dp)) {
                items(state.archivedSessions, key = { it.id }) { session ->
                    GlassPanel(Modifier.fillMaxWidth().padding(bottom = 7.dp)) {
                        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            HermesMulticolorIcon(HermesIconKind.ARCHIVE, null, iconSize = 23.dp)
                            Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                                Text(ellipsizeSessionTitle(session.title), fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(sessionTimeLabel(session.updatedAt), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            TextButton(onClick = { onRestore(session) }, enabled = state.settingsActionKey == null) { Text("恢复") }
                            IconButton(onClick = { deleteTarget = session }, enabled = state.settingsActionKey == null) { HermesMulticolorIcon(HermesIconKind.DELETE, "删除") }
                        }
                    }
                }
            }
        }
    }
    deleteTarget?.let { session ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("永久删除归档会话？") },
            text = { Text("“${ellipsizeSessionTitle(session.title)}”及消息记录将无法恢复。") },
            confirmButton = { TextButton(onClick = { onDelete(session); deleteTarget = null }) { Text("删除", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("取消") } },
        )
    }
}

@Composable
private fun SettingsHeader(title: String, subtitle: String, onBack: () -> Unit, onRefresh: (() -> Unit)? = null, busy: Boolean = false) {
    Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { HermesMulticolorIcon(HermesIconKind.BACK, "返回") }
        Column(Modifier.weight(1f).padding(start = 3.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
        else if (onRefresh != null) IconButton(onClick = onRefresh) { HermesMulticolorIcon(HermesIconKind.REFRESH, "刷新") }
    }
}

@Composable
private fun SettingsForm(title: String, subtitle: String, padding: PaddingValues, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().padding(padding)) {
        SettingsHeader(title, subtitle, onBack)
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = HermesSpacing.page),
        ) {
            content()
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun SettingsSection(content: @Composable ColumnScope.() -> Unit) {
    GlassPanel(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 11.dp, vertical = 3.dp), content = content)
    }
}

@Composable
private fun SelectRow(label: String, value: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontWeight = FontWeight.Medium, modifier = Modifier.weight(0.42f), maxLines = 1)
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(0.52f).padding(start = 8.dp),
        )
        HermesMulticolorIcon(HermesIconKind.CHEVRON_RIGHT, null, modifier = Modifier.padding(start = 7.dp), iconSize = 13.dp)
    }
}

@Composable
private fun EditableModelRow(label: String, value: String, onEdit: () -> Unit, onDelete: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).clickable(onClick = onEdit)) {
            Text(label, fontWeight = FontWeight.Medium)
            Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = onDelete) { HermesMulticolorIcon(HermesIconKind.DELETE, "移除") }
    }
}

@Composable
private fun ToggleSetting(title: String, subtitle: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium)
            if (subtitle.isNotBlank()) Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Switch(checked = value, onCheckedChange = onChange, modifier = Modifier.scale(0.78f))
    }
}

@Composable
private fun NumberField(label: String, value: String, onChange: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f), maxLines = 1)
        CompactValueField(value, KeyboardType.Number, 118.dp, onChange)
    }
}

@Composable
private fun DecimalField(label: String, value: String, onChange: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f), maxLines = 1)
        CompactValueField(value, KeyboardType.Decimal, 104.dp, onChange)
    }
}

@Composable
private fun CompactValueField(
    value: String,
    keyboardType: KeyboardType,
    width: androidx.compose.ui.unit.Dp,
    onChange: (String) -> Unit,
) {
    Surface(
        modifier = Modifier.width(width).height(40.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
        tonalElevation = 0.dp,
    ) {
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.End),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            modifier = Modifier.fillMaxSize().padding(horizontal = 9.dp, vertical = 7.dp),
            decorationBox = { inner ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterEnd) {
                    if (value.isBlank()) Text("自动", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    inner()
                }
            },
        )
    }
}

@Composable
private fun SaveButton(label: String, loading: Boolean, onClick: () -> Unit) {
    Button(onClick = onClick, enabled = !loading, modifier = Modifier.fillMaxWidth().padding(top = 14.dp)) {
        if (loading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Text(label)
    }
}

@Composable
private fun ModelPickerDialog(catalog: ModelCatalog, allowAutomatic: Boolean, onDismiss: () -> Unit, onSelect: (ModelChoice) -> Unit) {
    var provider by remember { mutableStateOf<ModelProvider?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (provider == null) "选择模型供应商" else provider?.name.orEmpty()) },
        text = {
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 520.dp)) {
                if (provider == null) {
                    if (allowAutomatic) item("auto") { ChoiceRow("自动（使用主模型）") { onSelect(ModelChoice()) } }
                    items(catalog.providers, key = { it.slug }) { item -> ChoiceRow(item.name) { provider = item } }
                } else {
                    items(provider?.models.orEmpty(), key = { it }) { model -> ChoiceRow(model) { onSelect(ModelChoice(provider?.slug.orEmpty(), model)) } }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("取消") } },
        dismissButton = if (provider != null) ({ TextButton(onClick = { provider = null }) { Text("返回") } }) else null,
    )
}

@Composable
private fun ChoiceDialog(title: String, values: List<String>, selected: String, onDismiss: () -> Unit, onSelect: (String) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { LazyColumn(Modifier.heightIn(max = 500.dp)) { items(values) { value -> ChoiceRow(if (value == selected) "✓ ${choiceLabel(value)}" else choiceLabel(value)) { onSelect(value) } } } },
        confirmButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun ChoiceRow(label: String, onClick: () -> Unit) {
    Text(label, modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp), maxLines = 2, overflow = TextOverflow.Ellipsis)
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
}

@Composable
private fun AddProviderDialog(busy: Boolean, onDismiss: () -> Unit, onAdd: (String, String, String, String, String) -> Unit) {
    var id by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var key by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("增加模型提供商") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                DialogCompactField("提供商标识", id, "如 groq") { id = it }
                DialogCompactField("显示名称", name, "可选") { name = it }
                DialogCompactField("接口地址", url, "OpenAI 兼容接口地址", KeyboardType.Uri) { url = it }
                DialogCompactField("默认模型", model, "模型 ID") { model = it }
                DialogCompactField("API Key", key, "可留空") { key = it }
                Text("密钥会写入 Hermes .env，界面不会保存明文。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = { TextButton(onClick = { onAdd(id, name, url, model, key) }, enabled = !busy && id.isNotBlank() && url.isNotBlank() && model.isNotBlank()) { Text("添加") } },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text("取消") } },
    )
}

@Composable
private fun DialogCompactField(
    label: String,
    value: String,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    onChange: (String) -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = 2.dp, bottom = 3.dp))
        Surface(
            modifier = Modifier.fillMaxWidth().height(42.dp),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
            tonalElevation = 0.dp,
        ) {
            BasicTextField(
                value = value,
                onValueChange = onChange,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 9.dp),
                decorationBox = { inner ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
                        if (value.isBlank()) Text(placeholder, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        inner()
                    }
                },
            )
        }
    }
}

private fun modelChoiceLabel(choice: ModelChoice): String = when {
    choice.provider == "auto" && choice.model.isBlank() -> "自动（使用主模型）"
    choice.provider.isBlank() && choice.model.isNotBlank() -> choice.model
    choice.provider.isBlank() -> "未配置"
    choice.model.isBlank() -> choice.provider
    else -> "${choice.provider} · ${choice.model}"
}
private fun ModelChoice.toSpec(): String = if (provider.isBlank()) model else "$provider:$model"
private fun reasoningLabel(value: String): String = mapOf("none" to "不启用", "minimal" to "最少", "low" to "低", "medium" to "中", "high" to "高", "xhigh" to "很高", "max" to "最大", "ultra" to "极致")[value] ?: value
private fun choiceLabel(value: String): String = PERSONALITIES.firstOrNull { it.first == value }?.second
    ?: APPROVAL_MODES.firstOrNull { it.first == value }?.second
    ?: reasoningLabel(value)
private fun personalityLabel(value: String): String = PERSONALITIES.firstOrNull { it.first == value }?.second ?: value
private fun approvalModeLabel(value: String): String = APPROVAL_MODES.firstOrNull { it.first == value }?.second ?: value

private val AUXILIARY_LABELS = listOf(
    "vision" to "视觉模型", "web_extract" to "网页提取", "compression" to "上下文压缩", "skills_hub" to "技能中心",
    "approval" to "审批模型", "mcp" to "MCP 调度", "title_generation" to "标题生成", "curator" to "维护器",
)
private val REASONING_LEVELS = listOf("none", "minimal", "low", "medium", "high", "xhigh", "max", "ultra")
private val PERSONALITIES = listOf(
    "helpful" to "通用助理", "concise" to "简洁直接", "technical" to "技术专家", "creative" to "创意伙伴", "teacher" to "耐心教师",
    "philosopher" to "深度思考", "kawaii" to "可爱活泼", "noir" to "冷峻侦探", "hype" to "高能鼓励",
)
private val TIMEZONES = listOf("Asia/Shanghai", "Asia/Hong_Kong", "Asia/Tokyo", "Asia/Singapore", "UTC", "Europe/London", "America/New_York", "America/Los_Angeles")
private val APPROVAL_MODES = listOf(
    Triple("smart", "智能审批", "由辅助模型判断风险，不确定时再询问"),
    Triple("manual", "手动审批", "危险命令始终等待你的确认"),
    Triple("off", "关闭审批", "跳过危险命令确认（高风险）"),
)

package com.qingyu.hermescompanion.ui.screen

import com.qingyu.hermescompanion.i18n.uiText
import com.qingyu.hermescompanion.R


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import com.qingyu.hermescompanion.ui.component.HermesButton as Button
import com.qingyu.hermescompanion.ui.component.HermesAlertDialog as AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.qingyu.hermescompanion.ui.AppUiState
import com.qingyu.hermescompanion.model.ConnectionDiagnosticItem
import com.qingyu.hermescompanion.model.DiagnosticStatus
import com.qingyu.hermescompanion.ui.component.GlassPanel
import com.qingyu.hermescompanion.ui.component.HermesMark
import com.qingyu.hermescompanion.ui.component.HermesIconKind
import com.qingyu.hermescompanion.ui.component.HermesMulticolorIcon
import com.qingyu.hermescompanion.ui.component.HermesStatusIcon
import com.qingyu.hermescompanion.ui.component.HermesStatusKind
import com.qingyu.hermescompanion.ui.theme.HermesSpacing
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.platform.testTag
import com.qingyu.hermescompanion.ui.component.HermesOutlinedTextField

@Composable
fun ConnectionScreen(
    state: AppUiState,
    contentPadding: PaddingValues,
    onConnect: (String, String, String, Boolean) -> Unit,
    onDiagnose: () -> Unit,
    onCheckAgentUpdate: () -> Unit,
    onApplyAgentUpdate: () -> Unit,
    onBack: (() -> Unit)?,
    onDisconnect: (() -> Unit)?,
    onLanguageChange: (com.qingyu.hermescompanion.i18n.AppLanguageMode) -> Unit = {},
) {
    var baseUrl by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(state.baseUrl) }
    var username by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(state.username) }
    var password by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("") }
    var showLanguage by remember { mutableStateOf(false) }
    var showPassword by remember { mutableStateOf(false) }
    var allowInsecureHttp by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    var confirmAgentUpdate by remember { mutableStateOf(false) }

    if (showLanguage) com.qingyu.hermescompanion.ui.component.LanguagePicker(state.languageMode, onLanguageChange) { showLanguage = false }

    val firstRun = !state.hasSavedConnection
    var addressHelp by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    var diagnosticsOpen by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    androidx.activity.compose.BackHandler(enabled = firstRun && onBack != null) { onBack?.invoke() }
    LaunchedEffect(state.baseUrl) { if (baseUrl.isBlank()) baseUrl = state.baseUrl }
    LaunchedEffect(state.username) { if (username.isBlank()) username = state.username }
    LaunchedEffect(state.isConnectionDiagnosing, state.agentUpdateProgress.running) {
        if (state.isConnectionDiagnosing || state.agentUpdateProgress.running) diagnosticsOpen = true
    }
    Column(Modifier.fillMaxSize().padding(contentPadding).statusBarsPadding().navigationBarsPadding().imePadding()
        .testTag(if (firstRun) "gateway_onboarding" else "gateway_settings")) {
        Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (onBack != null) IconButton(onClick = onBack) { HermesMulticolorIcon(HermesIconKind.BACK, uiText(R.string.ui_0554, "返回")) }
            Text(if (firstRun) uiText(R.string.setup_step_gateway, "2 / 2 · 连接网关") else uiText(R.string.ui_0761, "远程网关"),
                Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            TextButton(onClick = { showLanguage = true }) { Text("中 / EN") }
        }
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp)
            .padding(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            SetupHero(if (firstRun) uiText(R.string.gateway_welcome, "连接你的 Agent") else uiText(R.string.gateway_manage, "让连接保持顺畅"),
                if (firstRun) uiText(R.string.gateway_intro, "形象准备好了。再连接电脑上的网关，就可以开始对话。")
                else uiText(R.string.gateway_manage_hint, "管理服务器地址、登录账号与连接状态。"))
            if (state.hasSavedConnection) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HermesMulticolorIcon(HermesIconKind.CHECK_CIRCLE, null, iconSize = 18.dp)
                Text(uiText(R.string.ui_0765, "已保存远程网关，可重新验证或更新"),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            SetupSection(uiText(R.string.gateway_server, "服务器")) {
                HermesOutlinedTextField(baseUrl, {
                    baseUrl = it
                    if (!it.trim().startsWith("http://")) allowInsecureHttp = false
                }, Modifier.fillMaxWidth().testTag("gateway_url"),
                    label = { Text(uiText(R.string.ui_0767, "远程网关地址")) },
                    placeholder = { Text("https://your-server.example") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri), singleLine = true)
                TextButton(onClick = { addressHelp = !addressHelp }, modifier = Modifier.testTag("gateway_address_help")) {
                    Text(uiText(R.string.gateway_find_address, "在哪里找到地址？"))
                }
                if (addressHelp) Text(uiText(R.string.gateway_address_steps, "在电脑端打开 Hermes Desktop 的远程访问设置，复制“远程 URL”。手机与服务器需要能够互相访问；不要在地址后追加 /api 或 /v1。"),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            SetupSection(uiText(R.string.gateway_account, "网关账号")) {
                HermesOutlinedTextField(username, { username = it }, Modifier.fillMaxWidth().testTag("gateway_username"),
                    label = { Text(uiText(R.string.gateway_username_label, "用户名")) },
                    placeholder = { Text(uiText(R.string.ui_0770, "与电脑端相同的用户名")) }, singleLine = true)
                HermesOutlinedTextField(password, { password = it }, Modifier.fillMaxWidth().testTag("gateway_password"),
                    label = { Text(uiText(R.string.gateway_password_label, "密码")) },
                    placeholder = { Text(uiText(R.string.ui_0772, "仅用于本次登录验证")) }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            HermesMulticolorIcon(if (showPassword) HermesIconKind.EYE_OFF else HermesIconKind.EYE,
                                if (showPassword) uiText(R.string.ui_0773, "隐藏密码") else uiText(R.string.ui_0774, "显示密码"), iconSize = 20.dp)
                        }
                    })
                Text(uiText(R.string.gateway_account_hint, "使用电脑端设置的登录账号，与助理昵称无关。密码不会保存在手机中。"),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (baseUrl.trim().startsWith("http://")) Surface(shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = .65f)) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(allowInsecureHttp, { allowInsecureHttp = it })
                    Column(Modifier.weight(1f).padding(start = 8.dp)) {
                        Text(uiText(R.string.ui_0775, "允许未加密 HTTP 连接"), style = MaterialTheme.typography.bodyMedium)
                        Text(uiText(R.string.ui_0776, "公网使用建议改为 HTTPS 或可信 VPN"), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            state.errorMessage?.takeIf { it.isNotBlank() }?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
            Button(onClick = { onConnect(baseUrl.trim(), username.trim(), password, allowInsecureHttp) },
                enabled = !state.isBusy, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("gateway_connect")) {
                if (state.isBusy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                else Text(if (state.hasSavedConnection) uiText(R.string.ui_0777, "验证并更新连接") else uiText(R.string.ui_0778, "验证并连接"))
            }
            if (state.hasSavedConnection) {
                TextButton(onClick = { diagnosticsOpen = !diagnosticsOpen }, modifier = Modifier.fillMaxWidth()) {
                    Text(uiText(R.string.gateway_diagnostics, "诊断与 Agent 版本"))
                    HermesMulticolorIcon(if (diagnosticsOpen) HermesIconKind.EXPAND_UP else HermesIconKind.EXPAND_DOWN, null, iconSize = 18.dp)
                }
                if (diagnosticsOpen) {
                    SetupSection(uiText(R.string.ui_0781, "连接诊断")) {
                        com.qingyu.hermescompanion.ui.component.HermesOutlinedButton(onClick = onDiagnose,
                            enabled = !state.isConnectionDiagnosing && !state.isBusy, modifier = Modifier.fillMaxWidth()) {
                            Text(if (state.isConnectionDiagnosing) uiText(R.string.ui_0779, "正在诊断连接") else uiText(R.string.ui_0780, "诊断当前连接"))
                        }
                        state.connectionDiagnostics.forEach { DiagnosticRow(it) }
                    }
                    AgentUpdateCard(state, onCheckAgentUpdate, { confirmAgentUpdate = true })
                }
            }
            if (onDisconnect != null) TextButton(onClick = onDisconnect, enabled = !state.agentUpdateProgress.running,
                modifier = Modifier.fillMaxWidth()) { Text(uiText(R.string.ui_0786, "清除连接并退出"), color = MaterialTheme.colorScheme.error) }
        }
    }

    if (confirmAgentUpdate) {
        AlertDialog(
            onDismissRequest = { confirmAgentUpdate = false },
            title = { Text(uiText(R.string.ui_0787, "更新 Hermes Agent？")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(uiText(R.string.ui_0788, "服务器会执行 Hermes 官方更新流程，通常需要 1–4 分钟。期间 Gateway 短暂断开属于正常重启。"))
                    state.agentUpdateInfo.commits.take(5).forEach { commit ->
                        Text("• ${commit.summary.ifBlank { commit.sha }}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(uiText(R.string.ui_0789, "更新时请不要关闭本页，也不要同时运行新的 Agent 任务。"), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer), onClick = { confirmAgentUpdate = false; onApplyAgentUpdate() }) { Text(uiText(R.string.ui_0790, "开始更新")) }
            },
            dismissButton = { TextButton(colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer), onClick = { confirmAgentUpdate = false }) { Text(uiText(R.string.ui_0553, "取消")) } },
        )
    }
}

@Composable
private fun AgentUpdateCard(state: AppUiState, onCheck: () -> Unit, onUpdate: () -> Unit) {
    val info = state.agentUpdateInfo
    val progress = state.agentUpdateProgress
    SetupSection(uiText(R.string.ui_0791, "Hermes Agent 版本")) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                HermesMulticolorIcon(HermesIconKind.SYNC, null, iconSize = 21.dp)
                Column(Modifier.weight(1f).padding(start = 9.dp)) {
                    Text(
                        info.currentVersion.ifBlank { state.gatewayInfo.agentVersion }.ifBlank { uiText(R.string.ui_0792, "等待读取") },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (state.isAgentUpdateChecking || progress.running) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            }
            if (info.installMethod.isNotBlank()) {
                Text(uiText(R.string.ui_0793, "安装方式：%1\$s", info.installMethod), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                when {
                    progress.running -> uiText(R.string.ui_0794, "正在服务器上更新；连接中断后 APP 会继续等待并自动重连。")
                    info.updateAvailable && info.canApply -> uiText(R.string.ui_0795, "发现新版本%1\$s", info.behind?.let { uiText(R.string.ui_0796, " · 落后 %1\$s 个提交", it) }.orEmpty())
                    info.updateAvailable -> info.message.ifBlank { uiText(R.string.ui_0797, "发现更新，但当前安装方式不支持远程应用") }
                    info.currentVersion.isNotBlank() -> info.message.ifBlank { uiText(R.string.ui_0342, "当前已是最新版本") }
                    else -> info.message.ifBlank { uiText(R.string.ui_0798, "检查服务器上的 Agent 版本和更新状态") }
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (info.updateAvailable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (progress.lines.isNotBlank()) {
                Surface(shape = RoundedCornerShape(9.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)) {
                    Text(progress.lines.takeLast(900), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth().padding(9.dp), maxLines = 8)
                }
            }
            if (info.updateAvailable && !info.canApply && info.updateCommand.isNotBlank()) {
                Text(uiText(R.string.ui_0799, "服务器命令：%1\$s", info.updateCommand), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(Modifier.align(Alignment.End)) {
                TextButton(colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer), onClick = onCheck, enabled = !state.isAgentUpdateChecking && !progress.running) { Text(uiText(R.string.ui_0800, "检查更新")) }
                if (info.updateAvailable && info.canApply) {
                    Button(onClick = onUpdate, enabled = !progress.running && !state.isStreaming && state.pendingAgentRequests.isEmpty()) { Text(uiText(R.string.ui_0801, "更新 Agent")) }
                }
            }
        }
    }
}

@Composable
private fun DiagnosticRow(item: ConnectionDiagnosticItem) {
    val icon = when (item.status) {
        DiagnosticStatus.CHECKING -> HermesIconKind.STATUS_BUSY
        DiagnosticStatus.PASSED -> HermesIconKind.CHECK_CIRCLE
        DiagnosticStatus.WARNING -> HermesIconKind.WARNING
        DiagnosticStatus.FAILED -> HermesIconKind.ERROR
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        HermesMulticolorIcon(icon, null, iconSize = 19.dp)
        Column(Modifier.weight(1f).padding(start = 9.dp)) {
            Text(item.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(item.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}


package com.qingyu.hermescompanion.ui.screen

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
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
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
) {
    var baseUrl by remember { mutableStateOf(state.baseUrl) }
    var username by remember { mutableStateOf(state.username) }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var allowInsecureHttp by remember { mutableStateOf(false) }
    var confirmAgentUpdate by remember { mutableStateOf(false) }

    LaunchedEffect(state.baseUrl) { if (baseUrl.isBlank()) baseUrl = state.baseUrl }
    LaunchedEffect(state.username) { if (username.isBlank()) username = state.username }
    Column(
        modifier = Modifier.fillMaxSize().navigationBarsPadding().padding(contentPadding),
    ) {
        if (onBack != null) {
            Row(
                modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(start = 8.dp, end = 8.dp, top = 6.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) { HermesMulticolorIcon(HermesIconKind.BACK, "返回") }
                Column(Modifier.padding(start = 4.dp)) {
                    Text("远程网关", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text("连接服务器上的 Hermes Agent", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(top = 22.dp, bottom = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                HermesMark()
                Text("Hermes", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 10.dp))
                Text("连接你的远程个人助理", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 3.dp))
            }
        }

        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = HermesSpacing.page),
        ) {

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.46f),
            tonalElevation = 0.dp,
        ) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                HermesStatusIcon(if (state.hasSavedConnection) HermesStatusKind.CONNECTED else HermesStatusKind.BUSY)
                Text(
                    if (state.hasSavedConnection) {
                        state.gatewayInfo.agentVersion.takeIf(String::isNotBlank)
                            ?.let { "已连接 Hermes Agent $it" }
                            ?: "已保存远程网关，可重新验证或更新"
                    } else {
                        "使用与 Hermes Desktop 相同的账号连接"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }

        GlassPanel(Modifier.fillMaxWidth().padding(top = 9.dp), shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                CompactConnectionField(
                    label = "远程网关地址",
                    value = baseUrl,
                    placeholder = "http://服务器IP:9119",
                    keyboardType = KeyboardType.Uri,
                    onValueChange = {
                        baseUrl = it
                        if (!it.trim().startsWith("http://")) allowInsecureHttp = false
                    },
                )
                CompactConnectionField("Hermes 用户名", username, "与电脑端相同的用户名", onValueChange = { username = it })
                CompactConnectionField(
                    label = "Hermes 密码",
                    value = password,
                    placeholder = "仅用于本次登录验证",
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailing = {
                        IconButton(onClick = { showPassword = !showPassword }, modifier = Modifier.size(40.dp)) {
                            HermesMulticolorIcon(
                                if (showPassword) HermesIconKind.EYE_OFF else HermesIconKind.EYE,
                                if (showPassword) "隐藏密码" else "显示密码",
                                iconSize = 19.dp,
                            )
                        }
                    },
                    onValueChange = { password = it },
                )

                if (baseUrl.trim().startsWith("http://")) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.62f),
                        tonalElevation = 0.dp,
                    ) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 7.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(allowInsecureHttp, { allowInsecureHttp = it }, modifier = Modifier.size(36.dp))
                            Column(Modifier.padding(start = 5.dp)) {
                                Text("允许未加密 HTTP 连接", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                Text("公网使用建议改为 HTTPS 或可信 VPN", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onErrorContainer)
                            }
                        }
                    }
                }

                Button(
                    onClick = { onConnect(baseUrl, username, password, allowInsecureHttp) },
                    enabled = !state.isBusy,
                    shape = RoundedCornerShape(11.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                ) {
                    if (state.isBusy) CircularProgressIndicator(Modifier.size(19.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    else {
                        Text(if (state.hasSavedConnection) "验证并更新连接" else "验证并连接")
                    }
                }
                if (state.hasSavedConnection) {
                    TextButton(
                        onClick = onDiagnose,
                        enabled = !state.isConnectionDiagnosing && !state.isBusy,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (state.isConnectionDiagnosing) {
                            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            HermesMulticolorIcon(HermesIconKind.STATUS_CONNECTED, null, iconSize = 18.dp)
                        }
                        Text(
                            if (state.isConnectionDiagnosing) "正在诊断连接" else "诊断当前连接",
                            modifier = Modifier.padding(start = 7.dp),
                        )
                    }
                }
            }
        }

        if (state.connectionDiagnostics.isNotEmpty()) {
            GlassPanel(Modifier.fillMaxWidth().padding(top = 9.dp), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp)) {
                    Text("连接诊断", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    state.connectionDiagnostics.forEach { item -> DiagnosticRow(item) }
                }
            }
        }

        if (state.hasSavedConnection) {
            AgentUpdateCard(
                state = state,
                onCheck = onCheckAgentUpdate,
                onUpdate = { confirmAgentUpdate = true },
            )
        }

        GatewayNote(HermesIconKind.LOCK, "本机凭据保护", "密码不会保存；登录 Cookie 使用 Android Keystore 加密。")
        GatewayNote(HermesIconKind.WARNING, "地址填写说明", "填写电脑端“远程 URL”的完整内容，不要额外添加 /api 或 /v1。")

        if (onDisconnect != null) {
            TextButton(
                onClick = onDisconnect,
                enabled = !state.agentUpdateProgress.running,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) {
                Text("清除连接并退出", color = MaterialTheme.colorScheme.error)
            }
        }
        Spacer(Modifier.height(24.dp))
        }
    }

    if (confirmAgentUpdate) {
        AlertDialog(
            onDismissRequest = { confirmAgentUpdate = false },
            title = { Text("更新 Hermes Agent？") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("服务器会执行 Hermes 官方更新流程，通常需要 1–4 分钟。期间 Gateway 短暂断开属于正常重启。")
                    state.agentUpdateInfo.commits.take(5).forEach { commit ->
                        Text("• ${commit.summary.ifBlank { commit.sha }}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("更新时请不要关闭本页，也不要同时运行新的 Agent 任务。", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = { confirmAgentUpdate = false; onApplyAgentUpdate() }) { Text("开始更新") }
            },
            dismissButton = { TextButton(onClick = { confirmAgentUpdate = false }) { Text("取消") } },
        )
    }
}

@Composable
private fun AgentUpdateCard(state: AppUiState, onCheck: () -> Unit, onUpdate: () -> Unit) {
    val info = state.agentUpdateInfo
    val progress = state.agentUpdateProgress
    GlassPanel(Modifier.fillMaxWidth().padding(top = 9.dp), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                HermesMulticolorIcon(HermesIconKind.SYNC, null, iconSize = 21.dp)
                Column(Modifier.weight(1f).padding(start = 9.dp)) {
                    Text("Hermes Agent 版本", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        info.currentVersion.ifBlank { state.gatewayInfo.agentVersion }.ifBlank { "等待读取" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (state.isAgentUpdateChecking || progress.running) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            }
            if (info.installMethod.isNotBlank()) {
                Text("安装方式：${info.installMethod}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                when {
                    progress.running -> "正在服务器上更新；连接中断后 APP 会继续等待并自动重连。"
                    info.updateAvailable && info.canApply -> "发现新版本${info.behind?.let { " · 落后 $it 个提交" }.orEmpty()}"
                    info.updateAvailable -> info.message.ifBlank { "发现更新，但当前安装方式不支持远程应用" }
                    info.currentVersion.isNotBlank() -> info.message.ifBlank { "当前已是最新版本" }
                    else -> info.message.ifBlank { "检查服务器上的 Agent 版本和更新状态" }
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
                Text("服务器命令：${info.updateCommand}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(Modifier.align(Alignment.End)) {
                TextButton(onClick = onCheck, enabled = !state.isAgentUpdateChecking && !progress.running) { Text("检查更新") }
                if (info.updateAvailable && info.canApply) {
                    Button(onClick = onUpdate, enabled = !progress.running && !state.isStreaming && state.pendingAgentRequests.isEmpty()) { Text("更新 Agent") }
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

@Composable
private fun CompactConnectionField(
    label: String,
    value: String,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailing: (@Composable () -> Unit)? = null,
    onValueChange: (String) -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = 2.dp, bottom = 4.dp))
        Surface(
            modifier = Modifier.fillMaxWidth().height(44.dp),
            shape = RoundedCornerShape(9.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
            tonalElevation = 0.dp,
        ) {
            Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    visualTransformation = visualTransformation,
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                    modifier = Modifier.weight(1f).padding(horizontal = 10.dp, vertical = 10.dp),
                    decorationBox = { inner ->
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
                            if (value.isBlank()) Text(placeholder, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            inner()
                        }
                    },
                )
                trailing?.invoke()
            }
        }
    }
}

@Composable
private fun GatewayNote(icon: HermesIconKind, title: String, text: String) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 5.dp, vertical = 8.dp), verticalAlignment = Alignment.Top) {
        HermesMulticolorIcon(icon, null, iconSize = 18.dp)
        Column(Modifier.padding(start = 9.dp)) {
            Text(title, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
            Text(text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

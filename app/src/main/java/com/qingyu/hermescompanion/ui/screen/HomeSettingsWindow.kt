package com.qingyu.hermescompanion.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.qingyu.hermescompanion.today.*
import com.qingyu.hermescompanion.ui.AppUiState
import com.qingyu.hermescompanion.ui.HomeMode
import com.qingyu.hermescompanion.ui.component.HermesContentAction
import java.time.format.DateTimeFormatter

@Composable
fun HomeSettingsWindow(
    state: AppUiState, onDismiss: () -> Unit, onMode: (HomeMode) -> Unit,
    onGenerate: () -> Unit, onRefresh: () -> Unit, onMigrate: () -> Unit, onCompact: () -> Unit,
    onSchedule: (String, String, String) -> Unit, onCheckSchedule: () -> Unit,
) {
    val today = state.today
    val board = today.board
    val canPrepare = today.rootVerified && !today.loading && !state.isBusy && !state.isProfileSwitching
    var maintenance by rememberSaveable { mutableStateOf(false) }
    var schedule by rememberSaveable { mutableStateOf(false) }
    var showSummary by rememberSaveable { mutableStateOf(false) }
    if (schedule) TodayScheduleSheet(check = state.todayScheduleCheck.takeIf { it.profile == today.profile && it.root == today.root && it.connectionScope == state.baseUrl.trimEnd('/') + "\n" + state.username } ?: TodayScheduleCheck(), onCheck = onCheckSchedule, onDismiss = onDismiss, onConfigure = { morning, evening, zone -> onDismiss(); onSchedule(morning, evening, zone) })
    else if (showSummary) TodaySyncDetailsWindow(state, onDismiss, onRefresh)
    else TodayDetailWindow(onDismiss = onDismiss, title = todayText("首页设置", "Home settings")) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(todayText("首页模式", "Home mode"), style = MaterialTheme.typography.titleLarge)
            Column(Modifier.selectableGroup()) {
                HomeModeOption(HomeMode.SIMPLE, state.homeMode, todayText("简洁首页", "Simple Home"),
                    todayText("人物、问候、日常对话和最近聊天。无需配置整理功能。", "Your companion, greeting, daily conversation, and recent chats. No briefing setup needed.")) { onMode(it); onDismiss() }
                HomeModeOption(HomeMode.DEEP, state.homeMode, todayText("深度助理", "Personal assistant"),
                    todayText("汇总待办、进展与提醒，可按需配置早晚整理。", "Bring tasks, progress, and reminders together, with optional daily briefings.")) { onMode(it); onDismiss() }
            }
            Text(todayText("选择会保存在这台设备上，三套外观都支持。切换不会删除资料或停止已有的早晚任务；需要暂停时，请到「任务」中操作。", "Your choice is saved on this device and works with every skin. Switching keeps your data and existing schedules; pause schedules in Tasks."), style = MaterialTheme.typography.bodySmall)
            if (state.homeMode == HomeMode.DEEP) {
                HorizontalDivider()
                Text(todayText("首页整理，可按需开启", "Make Home your own"), style = MaterialTheme.typography.titleLarge)
                Text(todayText("不配置也能正常使用聊天、文件和任务，作为通用的 Hermes 移动终端。以下设置只用于首页的信息整理。", "Chat, files, and tasks work without configuring Home. These optional settings add personal briefings to your Hermes mobile client."), modifier = Modifier.testTag("today-basic-client-note"))
                HorizontalDivider()
                Text(todayText("1 · 首次整理", "1 · Your first briefing"), style = MaterialTheme.typography.titleMedium)
                Text(if (board == null) todayText("连接服务器并确认工作目录后，点击下方按钮。Hermes 会从最近的对话与工作区文件生成首页卡片，结果保存在当前工作区。", "Connect to your server and confirm the workspace, then start below. Hermes creates Home cards from recent conversations and workspace files, saving results in that workspace.")
                    else todayText("首页已有整理内容。和 Hermes 聊过新进展后，点击首页刷新按钮即可更新。", "Your Home briefing is ready. After discussing new progress with Hermes, tap Refresh on Home to update it."))
                if (board == null) HermesContentAction(onClick = { onDismiss(); onGenerate() }, enabled = canPrepare,
                    icon = "spark", modifier = Modifier.fillMaxWidth().testTag("today-setup-start")) { Text(todayText("开始首次整理", "Create first briefing")) }
                if (!today.rootVerified) Text(todayText("连接服务器并确认工作目录后，即可配置整理功能。", "Connect to the server and confirm a workspace to configure briefings."), style = MaterialTheme.typography.bodySmall)
                HorizontalDivider()
                Text(todayText("2 · 早晚整理（可选）", "2 · Daily schedule (optional)"), style = MaterialTheme.typography.titleMedium)
                Text(todayText("只需设置一次时间和时区，服务器就会每天自动整理。以后仅在想改时间或暂停时调整，无需每次打开 App 重新配置。", "Set the times and timezone once. The server then prepares daily briefings automatically. Return only to change the times; pause jobs from Tasks."))
                HermesContentAction(onClick = { schedule = true; onCheckSchedule() }, enabled = canPrepare,
                    icon = "calendar", modifier = Modifier.fillMaxWidth().testTag("today-setup-schedule")) { Text(todayText("早晚整理", "Daily schedule")) }
                Text(todayText("自动整理的处理记录在「任务」中查看；暂停或恢复定时任务，也在「任务」中操作。", "View automatic briefing records in Tasks, where you can also pause or resume scheduled jobs."), style = MaterialTheme.typography.bodySmall)
                HorizontalDivider()
                Text(todayText("文件收纳 · 只需一次", "Storage · one-time setup"), style = MaterialTheme.typography.titleMedium)
                val organized = today.rootVerified && today.error == null && today.fileExists &&
                    com.qingyu.hermescompanion.data.remotePathsEqual(today.filePath, com.qingyu.hermescompanion.data.joinServerPath(today.root, TodayBoard.PATH))
                Text(if (organized) todayText("首页文件已收纳，无需重复操作。", "Overview files are organized; no need to repeat.")
                    else if (!today.fileExists) todayText("新用户无需收纳，首次整理会自动建立首页文件。", "New users can skip this; the first briefing creates its files automatically.")
                    else todayText("将旧版首页文件收纳到应用文件夹，完成后无需再点。业务文件仍留在原处。", "Move legacy overview files into the app folder once. Your source documents stay in place."))
                if (!organized && today.fileExists) com.qingyu.hermescompanion.ui.component.HermesButton(onClick = { onDismiss(); onMigrate() }, enabled = canPrepare, modifier = Modifier.fillMaxWidth()) {
                    Text(todayText("收纳首页文件", "Organize overview files"), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                HorizontalDivider()
                HermesContentAction(icon = "tune", onClick = { maintenance = !maintenance }, modifier = Modifier.testTag("today-maintenance")) {
                    Text(todayText(if (maintenance) "收起维护工具" else "维护工具", if (maintenance) "Hide maintenance tools" else "Maintenance tools"))
                }
                if (maintenance) {
                    Text(todayText("平时不用操作。只在核对同步情况，或需要重新应用整理规则时使用。", "Usually no action is needed here. Use these tools to inspect sync status or reapply the briefing rules."), style = MaterialTheme.typography.bodyMedium)
                    HermesContentAction(icon = "history", onClick = { showSummary = true }) { Text(todayText("同步与读取详情", "Sync and read details")) }
                    if (board != null) HermesContentAction(icon = "refresh", onClick = { onDismiss(); onCompact() }, enabled = canPrepare) { Text(todayText("更新整理规则", "Update briefing rules")) }
                }
            }
        }
    }
}

@Composable
private fun HomeModeOption(mode: HomeMode, selected: HomeMode, title: String, description: String, onSelect: (HomeMode) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).testTag("home-mode-${mode.name.lowercase()}")
        .selectable(selected == mode, role = Role.RadioButton, onClick = { onSelect(mode) })
        .padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = selected == mode, onClick = null)
        Column(Modifier.weight(1f).padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
internal fun TodaySyncDetailsWindow(state: AppUiState, onDismiss: () -> Unit, onRefresh: () -> Unit) {
    val today = state.today
    val board = today.board
    TodayDetailWindow(onDismiss = onDismiss, title = todayText("同步与读取详情", "Sync and read details")) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(board?.headline ?: todayText("读取详情", "Read details"), style = MaterialTheme.typography.headlineSmall)
            today.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            today.cacheError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            board?.let {
                Text("${it.date} · ${it.generatedAt}", style = MaterialTheme.typography.labelMedium)
                if (it.summary.isNotBlank()) Text(it.summary)
                if (it.coverageNote.isNotBlank()) Text(it.coverageNote, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            today.syncedAt?.let {
                val synced = java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("MM-dd HH:mm"))
                Text(todayText("上次检查服务器 · $synced", "Last server check · $synced"), style = MaterialTheme.typography.labelMedium)
            }
            if (today.localSaved) Text(todayText("手机上已保存加密副本", "Encrypted copy saved on this phone"), style = MaterialTheme.typography.labelMedium)
            HermesContentAction(icon = "refresh", onClick = { onDismiss(); onRefresh() }, enabled = !today.loading && !state.isProfileSwitching) { Text(todayText("重新同步", "Sync again")) }
        }
    }
}

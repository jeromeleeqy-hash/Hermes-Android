package com.qingyu.hermescompanion.ui.screen
import com.qingyu.hermescompanion.ui.component.HermesOutlinedTextField as OutlinedTextField

import com.qingyu.hermescompanion.i18n.uiText
import com.qingyu.hermescompanion.R


import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import com.qingyu.hermescompanion.ui.component.HermesAlertDialog as AlertDialog
import com.qingyu.hermescompanion.ui.component.HermesButton as Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.qingyu.hermescompanion.model.CronJob
import com.qingyu.hermescompanion.model.AgentRequest
import com.qingyu.hermescompanion.model.AgentRequestChoice
import com.qingyu.hermescompanion.model.AgentRequestType
import com.qingyu.hermescompanion.model.ChatArtifact
import com.qingyu.hermescompanion.model.HermesSession
import com.qingyu.hermescompanion.model.RunCompletionSummary
import com.qingyu.hermescompanion.model.ToolStatus
import com.qingyu.hermescompanion.ui.AppUiState
import com.qingyu.hermescompanion.ui.component.GlassPanel
import com.qingyu.hermescompanion.ui.component.HermesIconKind
import com.qingyu.hermescompanion.ui.component.HermesSegmentedControl
import com.qingyu.hermescompanion.ui.component.HermesSwitch
import com.qingyu.hermescompanion.ui.component.HermesMulticolorIcon
import com.qingyu.hermescompanion.ui.component.HermesStatusIcon
import com.qingyu.hermescompanion.ui.component.HermesStatusKind
import com.qingyu.hermescompanion.ui.theme.HermesSkin
import com.qingyu.hermescompanion.ui.theme.HermesSpacing
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private enum class TaskTab(val label: String) {
    PENDING(uiText(R.string.ui_1271, "待处理")),
    RUNNING(uiText(R.string.ui_0622, "进行中")),
    SCHEDULED(uiText(R.string.ui_0116, "定时任务")),
    COMPLETED(uiText(R.string.ui_1272, "执行记录")),
}

@Composable
fun TasksScreen(
    state: AppUiState,
    contentPadding: PaddingValues,
    onStartConversation: () -> Unit,
    onOpenActiveRun: (HermesSession) -> Unit,
    onStopActiveRun: (HermesSession) -> Unit,
    onRespondRequest: (AgentRequest, String) -> Unit,
    onOpenCompletion: (RunCompletionSummary) -> Unit,
    onOpenCronSession: (HermesSession) -> Unit,
    onOpenArtifact: (ChatArtifact) -> Unit,
    onRefreshCron: () -> Unit,
    onCreateCron: (String, String, String) -> Unit,
    onOpenCron: (CronJob) -> Unit,
    onUpdateCron: (CronJob, String, String, String) -> Unit,
    onToggleCron: (CronJob) -> Unit,
    onTriggerCron: (CronJob) -> Unit,
    onDeleteCron: (CronJob) -> Unit,
) {
    var selectedTab by remember {
        mutableStateOf(
            when {
                state.pendingAgentRequests.isNotEmpty() -> TaskTab.PENDING
                state.isStreaming -> TaskTab.RUNNING
                else -> TaskTab.SCHEDULED
            },
        )
    }
    var showCreate by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<CronJob?>(null) }
    val activeTools = state.toolActivities.filter { it.status == ToolStatus.RUNNING }
    val runningCronJobs = state.cronJobs.filter(CronJob::isRunning)
    val cronSessions = remember(state.sessions) {
        state.sessions.filter { it.source.equals("cron", ignoreCase = true) }
            .sortedByDescending(HermesSession::updatedAt)
    }
    val cronSessionIds = remember(cronSessions) { cronSessions.mapTo(mutableSetOf(), HermesSession::id) }
    val regularCompletions = remember(state.recentCompletions, cronSessionIds) {
        state.recentCompletions.filterNot { it.sessionId in cronSessionIds }
    }
    val runningCount = state.runningRuns.size + runningCronJobs.size
    val completedJobs = regularCompletions.size + cronSessions.size
    LaunchedEffect(state.pendingAgentRequests.size) {
        if (state.pendingAgentRequests.isNotEmpty()) selectedTab = TaskTab.PENDING
    }

    Column(modifier = Modifier.fillMaxSize().padding(top = contentPadding.calculateTopPadding())) {
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().statusBarsPadding().verticalScroll(rememberScrollState()).padding(bottom = contentPadding.calculateBottomPadding())
                .padding(horizontal = HermesSpacing.page, vertical = 8.dp),
        ) {
            TaskPageHeader(
                running = runningCount,
                refreshing = state.isCronLoading,
                onRefresh = onRefreshCron,
                onCreate = { showCreate = true },
            )
            TaskSummaryStrip(
                pending = state.pendingAgentRequests.size,
                running = runningCount,
                enabled = state.cronJobs.count(CronJob::enabled),
                completed = completedJobs,
            )

            HermesSegmentedControl(
                items = TaskTab.entries.map(TaskTab::label),
                selectedIndex = selectedTab.ordinal,
                onSelect = { selectedTab = TaskTab.entries[it] },
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                compact = true,
            )

        when (selectedTab) {
            TaskTab.PENDING -> {
                if (state.pendingAgentRequests.isEmpty()) {
                    TaskEmptyState(HermesIconKind.CHECK_CIRCLE, uiText(R.string.ui_1273, "没有待处理请求"), uiText(R.string.ui_1274, "Hermes 需要确认操作或补充信息时，会集中显示在这里。"))
                } else {
                    Text(uiText(R.string.ui_1275, "需要你的决定"), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 13.dp, bottom = 6.dp))
                    state.pendingAgentRequests.forEach { request ->
                        TaskAgentRequestCard(request, onRespondRequest)
                    }
                }
            }

            TaskTab.RUNNING -> {
                if (state.isStreaming || runningCronJobs.isNotEmpty()) {
                    Text(uiText(R.string.ui_1276, "当前运行"), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 13.dp, bottom = 6.dp))
                    state.runningRuns.forEach { run ->
                        ActiveRunCard(
                            title = run.session.title.ifBlank { uiText(R.string.ui_0159, "Hermes 任务") },
                            stage = run.stage,
                            recovering = run.recovering,
                            startedAtMillis = run.startedAtMillis,
                            onOpen = { onOpenActiveRun(run.session) },
                            onStop = { onStopActiveRun(run.session) },
                        )
                    }
                    activeTools.takeLast(8).forEach { activity -> ToolRunCard(activity.name, activity.preview, activity.status) }
                    runningCronJobs.forEach { job ->
                        CronJobCard(
                            job = job,
                            busy = state.cronActionId == job.id,
                            onOpen = { onOpenCron(job) },
                            onToggle = { onToggleCron(job) },
                            onTrigger = { onTriggerCron(job) },
                            onDelete = { deleteTarget = job },
                        )
                    }
                } else {
                    TaskEmptyState(
                        icon = HermesIconKind.TASK,
                        title = uiText(R.string.ui_1277, "没有正在运行的任务"),
                        description = uiText(R.string.ui_1278, "在对话中发起任务后，当前工具执行状态会显示在这里。"),
                        actionLabel = uiText(R.string.ui_1279, "发起新任务"),
                        onAction = onStartConversation,
                    )
                }
            }

            TaskTab.SCHEDULED -> {
                if (state.cronJobs.isEmpty() && !state.isCronLoading) {
                    TaskEmptyState(
                        icon = HermesIconKind.RECENT,
                        title = uiText(R.string.ui_1280, "还没有定时任务"),
                        description = uiText(R.string.ui_1281, "可以让 Hermes 按 Cron 计划自动执行日报、检查与提醒。"),
                        actionLabel = uiText(R.string.ui_1282, "新建定时任务"),
                        onAction = { showCreate = true },
                    )
                } else {
                    Text(uiText(R.string.ui_1283, "自动执行"), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 13.dp, bottom = 6.dp))
                    state.cronJobs.forEach { job ->
                        CronJobCard(
                            job = job,
                            busy = state.cronActionId == job.id,
                            onOpen = { onOpenCron(job) },
                            onToggle = { onToggleCron(job) },
                            onTrigger = { onTriggerCron(job) },
                            onDelete = { deleteTarget = job },
                        )
                    }
                }
            }

            TaskTab.COMPLETED -> {
                val history = state.cronJobs.filter { it.lastRunAt.isNotBlank() }
                if (history.isEmpty() && regularCompletions.isEmpty() && cronSessions.isEmpty()) {
                    TaskEmptyState(HermesIconKind.ARCHIVE, uiText(R.string.ui_1284, "暂无执行记录"), uiText(R.string.ui_1285, "对话任务或定时任务完成后，会在这里保留最近结果。"))
                } else {
                    Text(uiText(R.string.ui_1286, "最近执行"), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 13.dp, bottom = 6.dp))
                    regularCompletions.forEach { completion ->
                        RunCompletionCard(completion, { onOpenCompletion(completion) }, onOpenArtifact)
                    }
                    if (cronSessions.isNotEmpty()) {
                        Text(uiText(R.string.ui_1287, "Cron 会话"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 6.dp, bottom = 6.dp))
                        cronSessions.forEach { session ->
                            CronSessionCard(session, onClick = { onOpenCronSession(session) })
                        }
                    }
                    history.sortedByDescending(CronJob::lastRunAt).forEach { job ->
                        CronHistoryCard(job, onClick = { onOpenCron(job) })
                    }
                }
            }
        }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showCreate) {
        CreateCronDialog(
            busy = state.isCronLoading,
            onDismiss = { if (!state.isCronLoading) showCreate = false },
            onCreate = { name, prompt, schedule ->
                onCreateCron(name, prompt, schedule)
                showCreate = false
            },
        )
    }
    deleteTarget?.let { job ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(uiText(R.string.ui_0827, "删除定时任务？")) },
            text = { Text(uiText(R.string.ui_0828, "“%1\$s”将停止自动执行，已有会话记录不会删除。", job.name)) },
            confirmButton = { TextButton(colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer), onClick = { onDeleteCron(job); deleteTarget = null }) { Text(uiText(R.string.ui_0469, "删除"), color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer), onClick = { deleteTarget = null }) { Text(uiText(R.string.ui_0553, "取消")) } },
        )
    }
}

@Composable
private fun TaskPageHeader(
    running: Int,
    refreshing: Boolean,
    onRefresh: () -> Unit,
    onCreate: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(uiText(R.string.ui_1288, "执行中心"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                HermesStatusIcon(if (running > 0) HermesStatusKind.BUSY else HermesStatusKind.CONNECTED)
                Text(
                    if (running > 0) uiText(R.string.ui_1289, "%1\$s 个 Agent 正在工作", running) else uiText(R.string.ui_1290, "Agent 当前空闲"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
        }
        IconButton(onClick = onRefresh, enabled = !refreshing, modifier = Modifier.size(38.dp)) {
            if (refreshing) CircularProgressIndicator(Modifier.size(19.dp), strokeWidth = 2.dp)
            else HermesMulticolorIcon(
                HermesIconKind.REFRESH,
                contentDescription = uiText(R.string.ui_0555, "刷新"),
                iconSize = 21.dp,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        com.qingyu.hermescompanion.ui.component.AssistantCreateButton(uiText(R.string.ui_1291, "新建任务"),onCreate)

    }
}

@Composable
private fun ActiveRunCard(
    title: String,
    stage: String,
    recovering: Boolean,
    startedAtMillis: Long,
    onOpen: () -> Unit,
    onStop: () -> Unit,
) {
    val elapsedMinutes = ((System.currentTimeMillis() - startedAtMillis).coerceAtLeast(0) / 60_000L)
    GlassPanel(
        modifier = Modifier.fillMaxWidth().padding(bottom = 7.dp).clickable(onClick = onOpen),
        shape = RoundedCornerShape(14.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.5.dp)
                Column(Modifier.weight(1f).padding(start = 10.dp)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        if (recovering) uiText(R.string.ui_1292, "连接中断，正在自动取回结果") else stage.ifBlank { uiText(R.string.ui_0373, "Hermes 正在执行") },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(if (elapsedMinutes < 1) uiText(R.string.ui_0483, "刚刚") else uiText(R.string.ui_1293, "%1\$s 分钟", elapsedMinutes), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
            Row(Modifier.align(Alignment.End).padding(top = 4.dp)) {
                TextButton(colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer), onClick = onOpen) { Text(uiText(R.string.ui_0162, "打开会话")) }
                TextButton(colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer), onClick = onStop) { Text(uiText(R.string.ui_0192, "停止"), color = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

@Composable
private fun TaskAgentRequestCard(request: AgentRequest, onRespond: (AgentRequest, String) -> Unit) {
    DecisionCard(request, onRespond)
}

@Composable
private fun RunCompletionCard(
    completion: RunCompletionSummary,
    onOpen: () -> Unit,
    onOpenArtifact: (ChatArtifact) -> Unit,
) {
    GlassPanel(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).clickable(onClick = onOpen),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                HermesStatusIcon(HermesStatusKind.CONNECTED)
                Text(completion.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f).padding(start = 8.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(taskCompletionTime(completion.completedAtMillis), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(completion.summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3, overflow = TextOverflow.Ellipsis)
            completion.artifacts.take(3).forEach { artifact ->
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable { onOpenArtifact(artifact) },
                    shape = RoundedCornerShape(9.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                ) {
                    Text(uiText(R.string.ui_1294, "打开产物 · %1\$s", artifact.name), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.padding(9.dp))
                }
            }
        }
    }
}

private fun taskCompletionTime(millis: Long): String = runCatching {
    DateTimeFormatter.ofPattern("MM-dd HH:mm").withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(millis))
}.getOrDefault("")

@Composable
private fun CronJobCard(
    job: CronJob,
    busy: Boolean,
    onOpen: () -> Unit,
    onToggle: () -> Unit,
    onTrigger: () -> Unit,
    onDelete: () -> Unit,
) {
    GlassPanel(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).clickable(onClick = onOpen),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(34.dp).clip(RoundedCornerShape(10.dp))
                        .background(if (job.enabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) { HermesMulticolorIcon(HermesIconKind.RECENT, contentDescription = null, iconSize = 20.dp) }
                Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
                    Text(job.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        job.schedule.display.ifBlank { job.schedule.expression.ifBlank { uiText(R.string.ui_1295, "未设置计划") } },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                if (busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                else HermesSwitch(checked = job.enabled, onCheckedChange = { onToggle() })
            }
            if (job.prompt.isNotBlank()) {
                Text(job.prompt, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
            }
            Row(modifier = Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (job.nextRunAt.isBlank()) uiText(R.string.ui_1296, "等待服务器计算下次时间") else uiText(R.string.ui_1297, "下次 %1\$s", cronTimeLabel(job.nextRunAt)),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onTrigger, enabled = !busy, modifier = Modifier.size(36.dp)) {
                    HermesMulticolorIcon(HermesIconKind.PLAY, contentDescription = uiText(R.string.ui_0822, "立即运行"), iconSize = 20.dp)
                }
                IconButton(onClick = onDelete, enabled = !busy, modifier = Modifier.size(36.dp)) {
                    HermesMulticolorIcon(HermesIconKind.DELETE, contentDescription = uiText(R.string.ui_0826, "删除任务"), iconSize = 19.dp)
                }
            }
        }
    }
}

@Composable
private fun CronHistoryCard(job: CronJob, onClick: () -> Unit) {
    val failed = job.lastStatus.contains("fail", true) || job.lastStatus.contains("error", true)
    GlassPanel(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            HermesStatusIcon(if (failed) HermesStatusKind.ERROR else HermesStatusKind.CONNECTED)
            Column(modifier = Modifier.weight(1f).padding(start = 9.dp)) {
                Text(job.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(cronTimeLabel(job.lastRunAt), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(job.lastStatus.ifBlank { job.state }, style = MaterialTheme.typography.labelMedium, color = if (failed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun CronSessionCard(session: HermesSession, onClick: () -> Unit) {
    GlassPanel(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(36.dp).clip(RoundedCornerShape(11.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                HermesMulticolorIcon(HermesIconKind.RECENT, contentDescription = null, iconSize = 20.dp)
            }
            Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
                Text(
                    session.title.ifBlank { uiText(R.string.ui_1298, "定时任务会话") },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    com.qingyu.hermescompanion.ui.format.conversationPreview(session.preview).ifBlank { uiText(R.string.ui_1299, "点按查看完整执行内容") },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                cronTimeLabel(session.updatedAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

@Composable
private fun CronDetailDialog(
    job: CronJob,
    busy: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit,
    onToggle: () -> Unit,
    onTrigger: () -> Unit,
    onDelete: () -> Unit,
) {
    var editing by remember(job.id) { mutableStateOf(false) }
    var name by remember(job.id) { mutableStateOf(job.name) }
    var prompt by remember(job.id) { mutableStateOf(job.prompt) }
    var schedule by remember(job.id) { mutableStateOf(job.schedule.expression) }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(18.dp),
        title = { Text(if (editing) uiText(R.string.ui_0802, "编辑定时任务") else uiText(R.string.ui_0803, "定时任务详情")) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 510.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                if (editing) {
                    OutlinedTextField(name, { name = it }, label = { Text(uiText(R.string.ui_0806, "任务名称")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(prompt, { prompt = it }, label = { Text(uiText(R.string.ui_1300, "让 Hermes 做什么")) }, minLines = 3, maxLines = 6, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(
                        schedule,
                        { schedule = it },
                        label = { Text(uiText(R.string.ui_1301, "Cron 表达式")) },
                        supportingText = { Text(uiText(R.string.ui_1302, "示例：每天 9:00 = 0 9 * * *")) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    Text(job.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    DetailRow(uiText(R.string.ui_1303, "状态"), if (job.enabled) job.state.ifBlank { uiText(R.string.ui_1304, "已启用") } else uiText(R.string.ui_0805, "已暂停"))
                    DetailRow(uiText(R.string.ui_0808, "执行计划"), job.schedule.display.ifBlank { job.schedule.expression.ifBlank { uiText(R.string.ui_0115, "未设置") } })
                    DetailRow(uiText(R.string.ui_1305, "下次执行"), job.nextRunAt.takeIf(String::isNotBlank)?.let(::cronTimeLabel) ?: uiText(R.string.ui_0813, "等待服务器计算"))
                    DetailRow(uiText(R.string.ui_1306, "上次执行"), job.lastRunAt.takeIf(String::isNotBlank)?.let(::cronTimeLabel) ?: uiText(R.string.ui_1307, "暂无"))
                    job.lastStatus.takeIf(String::isNotBlank)?.let { DetailRow(uiText(R.string.ui_1308, "上次状态"), it) }
                    (job.model.ifBlank { job.provider }).takeIf(String::isNotBlank)?.let { DetailRow(uiText(R.string.ui_1309, "运行模型"), it) }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                    Text(uiText(R.string.ui_1310, "执行内容"), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(job.prompt.ifBlank { uiText(R.string.ui_1311, "未填写") }, style = MaterialTheme.typography.bodyMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 3.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        TextButton(colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer), onClick = onTrigger, enabled = !busy) { Text(uiText(R.string.ui_0822, "立即运行")) }
                        TextButton(colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer), onClick = onToggle, enabled = !busy) { Text(if (job.enabled) uiText(R.string.ui_1312, "暂停") else uiText(R.string.ui_1313, "启用")) }
                        Spacer(Modifier.weight(1f))
                        TextButton(colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer), onClick = onDelete, enabled = !busy) { Text(uiText(R.string.ui_0469, "删除"), color = MaterialTheme.colorScheme.error) }
                    }
                }
            }
        },
        confirmButton = {
            if (editing) {
                TextButton(colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer), 
                    enabled = !busy && name.isNotBlank() && prompt.isNotBlank() && schedule.isNotBlank(),
                    onClick = { onSave(name, prompt, schedule) },
                ) { Text(uiText(R.string.ui_0936, "保存")) }
            } else {
                TextButton(colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer), enabled = !busy, onClick = { editing = true }) { Text(uiText(R.string.ui_1314, "编辑")) }
            }
        },
        dismissButton = {
            TextButton(colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer), enabled = !busy, onClick = { if (editing) editing = false else onDismiss() }) {
                Text(if (editing) uiText(R.string.ui_0820, "取消编辑") else uiText(R.string.ui_0196, "关闭"))
            }
        },
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(0.34f))
        Text(value, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(0.66f))
    }
}

@Composable
private fun CreateCronDialog(busy: Boolean, onDismiss: () -> Unit, onCreate: (String, String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var prompt by remember { mutableStateOf("") }
    var schedule by remember { mutableStateOf("0 9 * * *") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(uiText(R.string.ui_1282, "新建定时任务")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text(uiText(R.string.ui_0806, "任务名称")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(prompt, { prompt = it }, label = { Text(uiText(R.string.ui_1300, "让 Hermes 做什么")) }, minLines = 3, maxLines = 5, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(schedule, { schedule = it }, label = { Text(uiText(R.string.ui_1301, "Cron 表达式")) }, supportingText = { Text(uiText(R.string.ui_1302, "示例：每天 9:00 = 0 9 * * *")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer), enabled = !busy && name.isNotBlank() && prompt.isNotBlank() && schedule.isNotBlank(), onClick = { onCreate(name, prompt, schedule) }) {
                Text(uiText(R.string.ui_0978, "创建"))
            }
        },
        dismissButton = { TextButton(colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer), enabled = !busy, onClick = onDismiss) { Text(uiText(R.string.ui_0553, "取消")) } },
    )
}

@Composable
private fun TaskSummaryStrip(
    pending: Int,
    running: Int,
    enabled: Int,
    completed: Int,
) {
    GlassPanel(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 7.dp),
    ) {
        Column(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TaskSummaryMetric(uiText(R.string.ui_1271, "待处理"), pending.toString(), Modifier.weight(1f))
                TaskSummaryMetric(uiText(R.string.ui_1315, "运行中"), running.toString(), Modifier.weight(1f))
                TaskSummaryMetric(uiText(R.string.ui_1316, "定时"), enabled.toString(), Modifier.weight(1f))
                TaskSummaryMetric(uiText(R.string.ui_1317, "记录"), completed.toString(), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun TaskSummaryMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 7.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ToolRunCard(name: String, preview: String, status: ToolStatus) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = HermesSkin.current.panelAlpha),
        tonalElevation = 0.dp,
    ) {
        Row(modifier = Modifier.padding(9.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(11.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                Box(Modifier.size(38.dp), contentAlignment = Alignment.Center) {
                    when (status) {
                        ToolStatus.RUNNING -> CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        ToolStatus.COMPLETED -> HermesMulticolorIcon(HermesIconKind.VERIFIED, contentDescription = null, iconSize = 23.dp)
                        ToolStatus.FAILED -> HermesMulticolorIcon(HermesIconKind.HISTORY, contentDescription = null)
                    }
                }
            }
            Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
                Text(name, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                if (preview.isNotBlank()) Text(preview, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
            }
            HermesStatusIcon(
                when (status) {
                    ToolStatus.RUNNING -> HermesStatusKind.BUSY
                    ToolStatus.COMPLETED -> HermesStatusKind.CONNECTED
                    ToolStatus.FAILED -> HermesStatusKind.ERROR
                },
            )
        }
    }
}

@Composable
private fun TaskEmptyState(icon: HermesIconKind, title: String, description: String, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = HermesSkin.current.panelAlpha),
        tonalElevation = 0.dp,
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                Box(Modifier.size(52.dp), contentAlignment = Alignment.Center) { HermesMulticolorIcon(icon, contentDescription = null, iconSize = 29.dp) }
            }
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 12.dp))
            Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
            if (actionLabel != null && onAction != null) {
                Button(onClick = onAction, modifier = Modifier.padding(top = 12.dp)) {
                    Text(actionLabel)
                }
            }
        }
    }
}

private fun cronTimeLabel(raw: String): String {
    if (raw.isBlank()) return ""
    val instant = com.qingyu.hermescompanion.ui.format.parseHermesInstant(raw)
        ?: runCatching { OffsetDateTime.parse(raw).toInstant() }.getOrNull()
        ?: return raw
    return DateTimeFormatter.ofPattern(uiText(R.string.ui_0489, "M月d日 HH:mm")).withZone(ZoneId.systemDefault()).format(instant)
}

private val CronJob.isRunning: Boolean
    get() = state.equals("running", ignoreCase = true) ||
        state.equals("in_progress", ignoreCase = true) ||
        lastStatus.equals("running", ignoreCase = true) ||
        lastStatus.equals("in_progress", ignoreCase = true)

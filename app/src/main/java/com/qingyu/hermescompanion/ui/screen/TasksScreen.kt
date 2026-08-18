package com.qingyu.hermescompanion.ui.screen

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
    PENDING("待处理"),
    RUNNING("进行中"),
    SCHEDULED("定时任务"),
    COMPLETED("执行记录"),
}

@Composable
fun TasksScreen(
    state: AppUiState,
    contentPadding: PaddingValues,
    onStartConversation: () -> Unit,
    onOpenActiveRun: () -> Unit,
    onStopActiveRun: () -> Unit,
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
    val runningCount = (if (state.isStreaming) 1 else 0) + runningCronJobs.size
    val completedJobs = regularCompletions.size + cronSessions.size
    LaunchedEffect(state.pendingAgentRequests.size) {
        if (state.pendingAgentRequests.isNotEmpty()) selectedTab = TaskTab.PENDING
    }

    Column(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().statusBarsPadding().verticalScroll(rememberScrollState())
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
                modifier = Modifier.fillMaxWidth().padding(top = 9.dp),
                compact = true,
            )

        when (selectedTab) {
            TaskTab.PENDING -> {
                if (state.pendingAgentRequests.isEmpty()) {
                    TaskEmptyState(HermesIconKind.CHECK_CIRCLE, "没有待处理请求", "Hermes 需要确认操作或补充信息时，会集中显示在这里。")
                } else {
                    Text("需要你的决定", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 13.dp, bottom = 6.dp))
                    state.pendingAgentRequests.forEach { request ->
                        TaskAgentRequestCard(request, onRespondRequest)
                    }
                }
            }

            TaskTab.RUNNING -> {
                if (state.isStreaming || runningCronJobs.isNotEmpty()) {
                    Text("当前运行", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 13.dp, bottom = 6.dp))
                    if (state.isStreaming) {
                        ActiveRunCard(
                            title = state.sessions.firstOrNull { it.id == state.streamingSessionId }?.title
                                ?: state.selectedSession?.title.orEmpty().ifBlank { "Hermes 任务" },
                            stage = state.runStage,
                            recovering = state.isRecoveringConnection,
                            startedAtMillis = state.runStartedAtMillis,
                            onOpen = onOpenActiveRun,
                            onStop = onStopActiveRun,
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
                        title = "没有正在运行的任务",
                        description = "在对话中发起任务后，当前工具执行状态会显示在这里。",
                        actionLabel = "发起新任务",
                        onAction = onStartConversation,
                    )
                }
            }

            TaskTab.SCHEDULED -> {
                if (state.cronJobs.isEmpty() && !state.isCronLoading) {
                    TaskEmptyState(
                        icon = HermesIconKind.RECENT,
                        title = "还没有定时任务",
                        description = "可以让 Hermes 按 Cron 计划自动执行日报、检查与提醒。",
                        actionLabel = "新建定时任务",
                        onAction = { showCreate = true },
                    )
                } else {
                    Text("自动执行", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 13.dp, bottom = 6.dp))
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
                    TaskEmptyState(HermesIconKind.ARCHIVE, "暂无执行记录", "对话任务或定时任务完成后，会在这里保留最近结果。")
                } else {
                    Text("最近执行", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 13.dp, bottom = 6.dp))
                    regularCompletions.forEach { completion ->
                        RunCompletionCard(completion, { onOpenCompletion(completion) }, onOpenArtifact)
                    }
                    if (cronSessions.isNotEmpty()) {
                        Text("Cron 会话", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 6.dp, bottom = 6.dp))
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
            title = { Text("删除定时任务？") },
            text = { Text("“${job.name}”将停止自动执行，已有会话记录不会删除。") },
            confirmButton = { TextButton(onClick = { onDeleteCron(job); deleteTarget = null }) { Text("删除", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("取消") } },
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
            Text("执行中心", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                HermesStatusIcon(if (running > 0) HermesStatusKind.BUSY else HermesStatusKind.CONNECTED)
                Text(
                    if (running > 0) "$running 个 Agent 正在工作" else "Agent 当前空闲",
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
                contentDescription = "刷新",
                iconSize = 21.dp,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Surface(
            onClick = onCreate,
            modifier = Modifier.size(32.dp).semantics { contentDescription = "新建任务" },
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shadowElevation = 3.dp,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Box(
                    Modifier.size(15.dp, 2.5.dp)
                        .background(MaterialTheme.colorScheme.onPrimary, RoundedCornerShape(2.dp)),
                )
                Box(
                    Modifier.size(2.5.dp, 15.dp)
                        .background(MaterialTheme.colorScheme.onPrimary, RoundedCornerShape(2.dp)),
                )
            }
        }
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
                        if (recovering) "连接中断，正在自动取回结果" else stage.ifBlank { "Hermes 正在执行" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(if (elapsedMinutes < 1) "刚刚" else "${elapsedMinutes} 分钟", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
            Row(Modifier.align(Alignment.End).padding(top = 4.dp)) {
                TextButton(onClick = onOpen) { Text("打开会话") }
                TextButton(onClick = onStop) { Text("停止", color = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

@Composable
private fun TaskAgentRequestCard(request: AgentRequest, onRespond: (AgentRequest, String) -> Unit) {
    var answer by remember(request.requestId) { mutableStateOf("") }
    val actions = if (request.type == AgentRequestType.APPROVAL) {
        buildList {
            add("仅本次允许" to "once")
            if (request.allowSession) add("本次会话允许" to "session")
            if (request.allowPermanent) add("始终允许" to "always")
            add("拒绝" to "deny")
        }
    } else request.choices.map { it.label to it.value }
    GlassPanel(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), shape = RoundedCornerShape(15.dp)) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                HermesMulticolorIcon(
                    if (request.type == AgentRequestType.APPROVAL) HermesIconKind.LOCK else HermesIconKind.IDEA,
                    null,
                    iconSize = 20.dp,
                )
                Text(
                    if (request.type == AgentRequestType.APPROVAL) "操作确认" else "补充信息",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 7.dp),
                )
            }
            Text(request.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            if (request.detail.isNotBlank()) Text(request.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            actions.forEach { (label, value) ->
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable(enabled = !request.isResponding) { onRespond(request, value) },
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                ) { Text(label, modifier = Modifier.padding(horizontal = 11.dp, vertical = 9.dp)) }
            }
            if (request.type == AgentRequestType.CLARIFICATION && actions.isEmpty()) {
                OutlinedTextField(
                    value = answer,
                    onValueChange = { answer = it },
                    enabled = !request.isResponding,
                    placeholder = { Text("输入回答") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                )
                TextButton(
                    enabled = answer.isNotBlank() && !request.isResponding,
                    onClick = { onRespond(request, answer.trim()) },
                    modifier = Modifier.align(Alignment.End),
                ) { Text(if (request.isResponding) "提交中…" else "提交回答") }
            }
            if (request.isResponding) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
        }
    }
}

@Composable
private fun RunCompletionCard(
    completion: RunCompletionSummary,
    onOpen: () -> Unit,
    onOpenArtifact: (ChatArtifact) -> Unit,
) {
    GlassPanel(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).clickable(onClick = onOpen),
        shape = RoundedCornerShape(15.dp),
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
                    Text("打开产物 · ${artifact.name}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(9.dp))
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
        shape = RoundedCornerShape(15.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(34.dp).clip(RoundedCornerShape(10.dp))
                        .background(if (job.enabled) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) { HermesMulticolorIcon(HermesIconKind.RECENT, contentDescription = null, iconSize = 20.dp) }
                Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
                    Text(job.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        job.schedule.display.ifBlank { job.schedule.expression.ifBlank { "未设置计划" } },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
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
                    if (job.nextRunAt.isBlank()) "等待服务器计算下次时间" else "下次 ${cronTimeLabel(job.nextRunAt)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onTrigger, enabled = !busy, modifier = Modifier.size(36.dp)) {
                    HermesMulticolorIcon(HermesIconKind.PLAY, contentDescription = "立即运行", iconSize = 20.dp)
                }
                IconButton(onClick = onDelete, enabled = !busy, modifier = Modifier.size(36.dp)) {
                    HermesMulticolorIcon(HermesIconKind.DELETE, contentDescription = "删除任务", iconSize = 19.dp)
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
                    session.title.ifBlank { "定时任务会话" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    session.preview.ifBlank { "点按查看完整执行内容" },
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
        title = { Text(if (editing) "编辑定时任务" else "定时任务详情") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 510.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                if (editing) {
                    OutlinedTextField(name, { name = it }, label = { Text("任务名称") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(prompt, { prompt = it }, label = { Text("让 Hermes 做什么") }, minLines = 3, maxLines = 6, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(
                        schedule,
                        { schedule = it },
                        label = { Text("Cron 表达式") },
                        supportingText = { Text("示例：每天 9:00 = 0 9 * * *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    Text(job.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    DetailRow("状态", if (job.enabled) job.state.ifBlank { "已启用" } else "已暂停")
                    DetailRow("执行计划", job.schedule.display.ifBlank { job.schedule.expression.ifBlank { "未设置" } })
                    DetailRow("下次执行", job.nextRunAt.takeIf(String::isNotBlank)?.let(::cronTimeLabel) ?: "等待服务器计算")
                    DetailRow("上次执行", job.lastRunAt.takeIf(String::isNotBlank)?.let(::cronTimeLabel) ?: "暂无")
                    job.lastStatus.takeIf(String::isNotBlank)?.let { DetailRow("上次状态", it) }
                    (job.model.ifBlank { job.provider }).takeIf(String::isNotBlank)?.let { DetailRow("运行模型", it) }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                    Text("执行内容", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(job.prompt.ifBlank { "未填写" }, style = MaterialTheme.typography.bodyMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 3.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        TextButton(onClick = onTrigger, enabled = !busy) { Text("立即运行") }
                        TextButton(onClick = onToggle, enabled = !busy) { Text(if (job.enabled) "暂停" else "启用") }
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = onDelete, enabled = !busy) { Text("删除", color = MaterialTheme.colorScheme.error) }
                    }
                }
            }
        },
        confirmButton = {
            if (editing) {
                TextButton(
                    enabled = !busy && name.isNotBlank() && prompt.isNotBlank() && schedule.isNotBlank(),
                    onClick = { onSave(name, prompt, schedule) },
                ) { Text("保存") }
            } else {
                TextButton(enabled = !busy, onClick = { editing = true }) { Text("编辑") }
            }
        },
        dismissButton = {
            TextButton(enabled = !busy, onClick = { if (editing) editing = false else onDismiss() }) {
                Text(if (editing) "取消编辑" else "关闭")
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
        title = { Text("新建定时任务") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("任务名称") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(prompt, { prompt = it }, label = { Text("让 Hermes 做什么") }, minLines = 3, maxLines = 5, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(schedule, { schedule = it }, label = { Text("Cron 表达式") }, supportingText = { Text("示例：每天 9:00 = 0 9 * * *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(enabled = !busy && name.isNotBlank() && prompt.isNotBlank() && schedule.isNotBlank(), onClick = { onCreate(name, prompt, schedule) }) {
                Text("创建")
            }
        },
        dismissButton = { TextButton(enabled = !busy, onClick = onDismiss) { Text("取消") } },
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
                TaskSummaryMetric("待处理", pending.toString(), Modifier.weight(1f))
                TaskSummaryMetric("运行中", running.toString(), Modifier.weight(1f))
                TaskSummaryMetric("定时", enabled.toString(), Modifier.weight(1f))
                TaskSummaryMetric("记录", completed.toString(), Modifier.weight(1f))
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
        shape = RoundedCornerShape(15.dp),
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
    val instant = runCatching { Instant.parse(raw) }.getOrNull()
        ?: runCatching { OffsetDateTime.parse(raw).toInstant() }.getOrNull()
        ?: return raw
    return DateTimeFormatter.ofPattern("M月d日 HH:mm").withZone(ZoneId.systemDefault()).format(instant)
}

private val CronJob.isRunning: Boolean
    get() = state.equals("running", ignoreCase = true) ||
        state.equals("in_progress", ignoreCase = true) ||
        lastStatus.equals("running", ignoreCase = true) ||
        lastStatus.equals("in_progress", ignoreCase = true)

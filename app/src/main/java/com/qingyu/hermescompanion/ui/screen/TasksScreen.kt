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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.sp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import com.qingyu.hermescompanion.today.todayText
import com.qingyu.hermescompanion.ui.component.AssistantGlyph
import com.qingyu.hermescompanion.ui.component.HermesAlertDialog as AlertDialog
import com.qingyu.hermescompanion.ui.component.HermesButton as Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.qingyu.hermescompanion.ui.component.HermesContentAction
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

private enum class TaskTab(val zh: String, val en: String) {
    CURRENT("当前", "Current"), SCHEDULED("定时", "Scheduled"), COMPLETED("记录", "History");
    val label get() = todayText(zh, en)
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
    var selectedTab by rememberSaveable(state.baseUrl, state.activeProfile) {
        mutableStateOf(if (state.pendingAgentRequests.isNotEmpty() || state.runningRuns.isNotEmpty()) TaskTab.CURRENT else TaskTab.SCHEDULED)
    }
    var showPaused by rememberSaveable(state.baseUrl, state.activeProfile) { mutableStateOf(false) }
    var showCreate by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<CronJob?>(null) }
    val runningCronJobs = state.cronJobs.filter(CronJob::isRunning)
    val taskSessions = remember(state.sessions, state.activeProfile, state.taskSessionKeys) {
        state.sessions.filter { it.profile == state.activeProfile && com.qingyu.hermescompanion.data.isTaskConversation(it, state.taskSessionKeys) }
            .sortedByDescending(HermesSession::updatedAt)
    }
    val taskSessionIds = taskSessions.mapTo(mutableSetOf(), HermesSession::id)
    val regularCompletions = state.recentCompletions.filterNot { it.sessionId in taskSessionIds }
    val runningCount = state.runningRuns.size + runningCronJobs.size
    val pendingCount = state.pendingAgentRequests.size
    LazyColumn(
        Modifier.fillMaxSize().testTag("tasks-page").padding(top = contentPadding.calculateTopPadding()).statusBarsPadding(),
        contentPadding = PaddingValues(start = HermesSpacing.page, end = HermesSpacing.page, top = 12.dp, bottom = contentPadding.calculateBottomPadding() + 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "header") { TaskPageHeader(runningCount, state.isCronLoading, onRefreshCron, { showCreate = true }) }
        item(key = "tabs") {
            HermesSegmentedControl(items = TaskTab.entries.map { it.label + if (it == TaskTab.CURRENT && pendingCount + runningCount > 0) " · ${pendingCount + runningCount}" else "" },
                selectedIndex = selectedTab.ordinal, onSelect = { selectedTab = TaskTab.entries[it] }, modifier = Modifier.fillMaxWidth(), compact = true)
        }
        if (pendingCount > 0 && selectedTab != TaskTab.CURRENT) item(key = "needs-you") {
            HermesContentAction(onClick = { selectedTab = TaskTab.CURRENT }) {
                Text(todayText("有 $pendingCount 件事需要你确认", "$pendingCount requests need your input"))
            }
        }
        when (selectedTab) {
            TaskTab.CURRENT -> {
                if (pendingCount + runningCount == 0) item {
                    TaskEmptyState(HermesIconKind.CHECK_CIRCLE, todayText("暂时没有需要处理的事", "Nothing waiting right now"),
                        todayText("交给 Hermes 的工作和需要你确认的事项，会出现在这里。", "Work in progress and requests for your input will appear here."),
                        todayText("聊点什么", "Start a conversation"), onStartConversation)
                }
                items(state.pendingAgentRequests, key = { "request:${com.qingyu.hermescompanion.data.agentRequestKey(it)}" }) { TaskAgentRequestCard(it, onRespondRequest) }
                items(state.runningRuns, key = { "run:${it.session.profile}:${it.session.id}" }) { run ->
                    ActiveRunCard(run.session.title.ifBlank { todayText("正在处理的事", "Work in progress") }, run.stage, run.recovering, run.startedAtMillis,
                        { onOpenActiveRun(run.session) }, { onStopActiveRun(run.session) })
                }
                items(runningCronJobs, key = { "running-cron:${it.id}" }) { job ->
                    CronJobCard(job, state.cronActionId == job.id, { onOpenCron(job) }, { onToggleCron(job) }, { onTriggerCron(job) }, { deleteTarget = job })
                }
            }
            TaskTab.SCHEDULED -> {
                if (state.cronJobs.isEmpty() && !state.isCronLoading) item {
                    TaskEmptyState(HermesIconKind.RECENT, todayText("还没有定时安排", "No scheduled work yet"),
                        todayText("把例行整理和提醒交给 Hermes，按约定的时间帮你完成。", "Let Hermes take care of regular briefings and reminders."),
                        todayText("添加安排", "Add a schedule"), { showCreate = true })
                }
                items(state.cronJobs.filter { it.enabled || it.isRunning || showPaused }.sortedWith(compareBy<CronJob> { !it.enabled }.thenBy { com.qingyu.hermescompanion.ui.format.parseHermesInstant(it.nextRunAt) ?: Instant.MAX }), key = { "cron:${it.id}" }) { job ->
                    CronJobCard(job, state.cronActionId == job.id, { onOpenCron(job) }, { onToggleCron(job) }, { onTriggerCron(job) }, { deleteTarget = job })
                }
                val paused = state.cronJobs.count { !it.enabled && !it.isRunning }
                if (paused > 0) item(key = "paused-toggle") {
                    HermesContentAction(onClick = { showPaused = !showPaused }, modifier = Modifier.testTag("paused-tasks-toggle")) {
                        Text(if (showPaused) todayText("收起已暂停安排", "Hide paused schedules") else todayText("已暂停 · $paused", "Paused · $paused"))
                    }
                }
            }
            TaskTab.COMPLETED -> {
                val history = state.cronJobs.filter { it.lastRunAt.isNotBlank() }
                if (history.isEmpty() && regularCompletions.isEmpty() && taskSessions.isEmpty()) item {
                    TaskEmptyState(HermesIconKind.ARCHIVE, todayText("这里会留下处理结果", "Your results will appear here"),
                        todayText("刷新、整理和自动执行的记录都在这里。点开可查看处理详情。", "Refreshes, briefings and automated work are recorded here. Open an item to see its details."))
                }
                items(regularCompletions, key = { "completion:${it.sessionId}:${it.completedAtMillis}" }) { completion ->
                    RunCompletionCard(completion.copy(title = state.sessions.firstOrNull { it.id == completion.sessionId && it.profile == state.activeProfile }?.title ?: completion.title), { onOpenCompletion(completion) }, onOpenArtifact)
                }
                items(taskSessions, key = { "history:${it.profile}:${it.id}" }) { session -> CronSessionCard(session) { onOpenCronSession(session) } }
                // Only use job summaries when conversation history is unavailable, avoiding two copies of every run.
                if (taskSessions.none { it.source.equals("cron", true) }) items(history.sortedByDescending(CronJob::lastRunAt), key = { "last-run:${it.id}" }) { job ->
                    CronHistoryCard(job) { onOpenCron(job) }
                }
            }
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
            Text(todayText("任务", "Tasks"), fontSize = 30.sp, fontWeight = FontWeight.SemiBold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (running > 0) todayText("正在帮你处理 $running 件事", "Working on $running things") else todayText("把约好的事，稳稳接住", "Your work, kept in view"),
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
        com.qingyu.hermescompanion.ui.component.AssistantCreateButton(todayText("添加定时安排", "Add a schedule"),onCreate)

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
        shape = RoundedCornerShape(22.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.5.dp)
                Column(Modifier.weight(1f).padding(start = 10.dp)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        if (recovering) uiText(R.string.ui_1292, "连接中断，正在自动取回结果") else todayText("正在为你处理", "Working on your request"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(if (elapsedMinutes < 1) uiText(R.string.ui_0483, "刚刚") else uiText(R.string.ui_1293, "%1\$s 分钟", elapsedMinutes), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
            Row(Modifier.align(Alignment.End).padding(top = 4.dp)) {
                TextButton(colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer), onClick = onOpen) { Text(todayText("查看处理详情", "View details")) }
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
                AssistantGlyph("chat", Modifier.size(18.dp), MaterialTheme.colorScheme.onSurfaceVariant)
                Text(com.qingyu.hermescompanion.data.readableConversationTitle(completion.title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f).padding(start = 8.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(taskCompletionTime(completion.completedAtMillis), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(com.qingyu.hermescompanion.data.replyExcerpt(completion.summary).ifBlank { todayText("点开核对这次回复", "Open to check this reply") }, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)

        }
    }
}

private fun taskCompletionTime(millis: Long): String = runCatching {
    DateTimeFormatter.ofPattern("MM-dd HH:mm").withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(millis))
}.getOrDefault("")

internal fun taskScheduleLabel(job: CronJob): String {
    val bits = job.schedule.expression.trim().split(Regex("\\s+"))
    if (job.schedule.kind == "cron" && bits.size == 5 && bits[2] == "*" && bits[3] == "*") {
        val minute = bits[0].toIntOrNull(); val hour = bits[1].toIntOrNull()
        if (minute != null && minute in 0..59 && hour != null && hour in 0..23) {
            val time = "%02d:%02d".format(hour, minute)
            if (bits[4] == "*") return todayText("每天 ", "Daily ") + time
            val rawDays = bits[4].split(',').flatMap { value ->
                val range = value.split('-').map { it.toIntOrNull() }
                when {
                    range.size == 1 && range[0] != null && range[0]!! in 0..7 -> listOf(range[0]!! % 7)
                    range.size == 2 && range[0] != null && range[1] != null && range[0]!! in 0..7 && range[1]!! in range[0]!!..7 -> (range[0]!!..range[1]!!).map { it % 7 }
                    else -> listOf(-1)
                }
            }.distinct().sorted()
            if (-1 !in rawDays && rawDays.isNotEmpty()) {
                if (rawDays.size == 7) return todayText("每天 ", "Daily ") + time
                if (rawDays == listOf(1, 2, 3, 4, 5)) return todayText("周一至周五 ", "Mon–Fri ") + time
                if (rawDays == listOf(0, 6)) return todayText("每周六、日 ", "Sat–Sun ") + time
                val names = rawDays.map { listOf(todayText("日", "Sun"), todayText("一", "Mon"), todayText("二", "Tue"), todayText("三", "Wed"), todayText("四", "Thu"), todayText("五", "Fri"), todayText("六", "Sat"))[it] }
                return todayText("每周", "Every ") + names.joinToString(todayText("、", ", ")) + " " + time
            }
        }
    }
    return job.schedule.display.takeIf { it.isNotBlank() && it != job.schedule.expression }
        ?: todayText("按约定时间", "On schedule")
}

@Composable
private fun CronJobCard(job: CronJob, busy: Boolean, onOpen: () -> Unit, onToggle: () -> Unit, onTrigger: () -> Unit, onDelete: () -> Unit) {
    var menu by remember(job.id) { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    GlassPanel(Modifier.fillMaxWidth().testTag("task-cron:${job.id}").clickable(onClick = onOpen), shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.padding(start = 18.dp, end = 12.dp, top = 14.dp, bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f).padding(end = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(job.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(taskScheduleLabel(job), style = MaterialTheme.typography.labelMedium, color = colors.primary)
                }
                if (busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                else HermesSwitch(checked = job.enabled, onCheckedChange = { onToggle() })
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(when {
                    job.isRunning -> todayText("正在处理", "Running")
                    !job.enabled -> todayText("已暂停", "Paused")
                    job.nextRunAt.isNotBlank() -> todayText("下次 ", "Next ") + cronTimeLabel(job.nextRunAt)
                    else -> todayText("已启用 · 时间待同步", "Enabled · Awaiting next run time")
                }, Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                Box {
                    IconButton(onClick = { menu = true }, enabled = !busy, modifier = Modifier.size(32.dp).semantics { contentDescription = todayText("任务选项", "Task options") }) {
                        AssistantGlyph("more", Modifier.size(18.dp), colors.onSurfaceVariant)
                    }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text(todayText("查看安排", "View schedule")) }, onClick = { menu = false; onOpen() })
                        DropdownMenuItem(text = { Text(todayText("现在运行一次", "Run once now")) }, onClick = { menu = false; onTrigger() }, enabled = !job.isRunning)
                        DropdownMenuItem(text = { Text(todayText("删除安排", "Delete schedule"), color = colors.error) }, onClick = { menu = false; onDelete() })
                    }
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
        shape = RoundedCornerShape(22.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            if (failed) HermesStatusIcon(HermesStatusKind.ERROR) else AssistantGlyph("chat", Modifier.size(20.dp), MaterialTheme.colorScheme.onSurfaceVariant)
            Column(modifier = Modifier.weight(1f).padding(start = 9.dp)) {
                Text(job.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(cronTimeLabel(job.lastRunAt), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(if (failed) todayText("需要查看", "Needs attention") else todayText("查看结果", "View result"),
                style = MaterialTheme.typography.labelMedium, color = if (failed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun CronSessionCard(session: HermesSession, onClick: () -> Unit) {
    GlassPanel(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
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
                    com.qingyu.hermescompanion.data.readableConversationTitle(session.title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    com.qingyu.hermescompanion.ui.format.conversationPreview(session.preview).ifBlank { todayText("点开查看结果", "Open to view results") },
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
        shape = RoundedCornerShape(22.dp),
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
                Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) { HermesMulticolorIcon(icon, contentDescription = null, iconSize = 22.dp) }
            }
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 12.dp))
            Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
            if (actionLabel != null && onAction != null) {
                HermesContentAction(onClick = onAction, modifier = Modifier.padding(top = 8.dp)) {
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

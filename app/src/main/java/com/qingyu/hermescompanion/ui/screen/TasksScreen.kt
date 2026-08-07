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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.qingyu.hermescompanion.model.CronJob
import com.qingyu.hermescompanion.model.ToolStatus
import com.qingyu.hermescompanion.ui.AppUiState
import com.qingyu.hermescompanion.ui.component.GlassPanel
import com.qingyu.hermescompanion.ui.component.HermesIconKind
import com.qingyu.hermescompanion.ui.component.HermesSegmentedControl
import com.qingyu.hermescompanion.ui.component.HermesMulticolorIcon
import com.qingyu.hermescompanion.ui.component.HermesStatusIcon
import com.qingyu.hermescompanion.ui.component.HermesStatusKind
import com.qingyu.hermescompanion.ui.theme.HermesSpacing
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private enum class TaskTab(val label: String) {
    RUNNING("进行中"),
    SCHEDULED("定时任务"),
    COMPLETED("执行记录"),
}

@Composable
fun TasksScreen(
    state: AppUiState,
    contentPadding: PaddingValues,
    onStartConversation: () -> Unit,
    onRefreshCron: () -> Unit,
    onCreateCron: (String, String, String) -> Unit,
    onOpenCron: (CronJob) -> Unit,
    onUpdateCron: (CronJob, String, String, String) -> Unit,
    onToggleCron: (CronJob) -> Unit,
    onTriggerCron: (CronJob) -> Unit,
    onDeleteCron: (CronJob) -> Unit,
) {
    var selectedTab by remember { mutableStateOf(TaskTab.SCHEDULED) }
    var showCreate by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<CronJob?>(null) }
    val activeTools = state.toolActivities.filter { it.status == ToolStatus.RUNNING }
    val runningCronJobs = state.cronJobs.filter(CronJob::isRunning)
    val runningCount = activeTools.size + runningCronJobs.size
    val completedJobs = state.cronJobs.count { it.lastRunAt.isNotBlank() }

    Column(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().statusBarsPadding().verticalScroll(rememberScrollState())
                .padding(horizontal = HermesSpacing.page, vertical = 8.dp),
        ) {
            TaskSummaryStrip(
                running = runningCount,
                enabled = state.cronJobs.count(CronJob::enabled),
                completed = completedJobs,
                refreshing = state.isCronLoading,
                onRefresh = onRefreshCron,
                onCreate = { showCreate = true },
            )

            HermesSegmentedControl(
                items = TaskTab.entries.map(TaskTab::label),
                selectedIndex = selectedTab.ordinal,
                onSelect = { selectedTab = TaskTab.entries[it] },
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            )

        when (selectedTab) {
            TaskTab.RUNNING -> {
                if (activeTools.isNotEmpty() || runningCronJobs.isNotEmpty()) {
                    Text("当前运行", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 13.dp, bottom = 6.dp))
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
                if (history.isEmpty()) {
                    TaskEmptyState(HermesIconKind.ARCHIVE, "暂无执行记录", "定时任务首次运行后，这里会显示最近状态和执行时间。")
                } else {
                    Text("最近执行", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 13.dp, bottom = 6.dp))
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
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 9.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(38.dp).clip(RoundedCornerShape(11.dp))
                        .background(if (job.enabled) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) { HermesMulticolorIcon(HermesIconKind.RECENT, contentDescription = null, iconSize = 22.dp) }
                Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
                    Text(job.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        job.schedule.display.ifBlank { job.schedule.expression.ifBlank { "未设置计划" } },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                if (busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                else Switch(checked = job.enabled, onCheckedChange = { onToggle() }, modifier = Modifier.scale(0.78f))
            }
            if (job.prompt.isNotBlank()) {
                Text(job.prompt, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 7.dp))
            }
            Row(modifier = Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (job.nextRunAt.isBlank()) "等待服务器计算下次时间" else "下次 ${cronTimeLabel(job.nextRunAt)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onTrigger, enabled = !busy, modifier = Modifier.size(40.dp)) {
                    HermesMulticolorIcon(HermesIconKind.PLAY, contentDescription = "立即运行")
                }
                IconButton(onClick = onDelete, enabled = !busy, modifier = Modifier.size(40.dp)) {
                    HermesMulticolorIcon(HermesIconKind.DELETE, contentDescription = "删除任务")
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
    running: Int,
    enabled: Int,
    completed: Int,
    refreshing: Boolean,
    onRefresh: () -> Unit,
    onCreate: () -> Unit,
) {
    GlassPanel(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
    ) {
        Column(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TaskSummaryMetric("运行中", running.toString(), Modifier.weight(1f))
                TaskSummaryMetric("已启用", enabled.toString(), Modifier.weight(1f))
                TaskSummaryMetric("有记录", completed.toString(), Modifier.weight(1f))
            }
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.58f),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TaskSummaryAction(
                    label = if (refreshing) "刷新中" else "刷新",
                    primary = false,
                    enabled = !refreshing,
                    onClick = onRefresh,
                    modifier = Modifier.weight(1f),
                ) {
                    if (refreshing) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    else HermesMulticolorIcon(HermesIconKind.REFRESH, contentDescription = null, iconSize = 18.dp)
                }
                TaskSummaryAction(
                    label = "新建任务",
                    primary = true,
                    enabled = true,
                    onClick = onCreate,
                    modifier = Modifier.weight(1f),
                ) {
                    HermesMulticolorIcon(HermesIconKind.ADD, contentDescription = null, iconSize = 18.dp)
                }
            }
        }
    }
}

@Composable
private fun TaskSummaryAction(
    label: String,
    primary: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.height(38.dp).clip(RoundedCornerShape(11.dp))
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(11.dp),
        color = if (primary) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = if (primary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icon()
            Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 6.dp))
        }
    }
}

@Composable
private fun TaskSummaryMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 7.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ToolRunCard(name: String, preview: String, status: ToolStatus) {
    GlassPanel(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        shape = RoundedCornerShape(15.dp),
        contentPadding = PaddingValues(9.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
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
    GlassPanel(
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 22.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
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

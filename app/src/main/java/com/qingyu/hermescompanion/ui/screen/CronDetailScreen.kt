package com.qingyu.hermescompanion.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.qingyu.hermescompanion.model.CronJob
import com.qingyu.hermescompanion.ui.AppUiState
import com.qingyu.hermescompanion.ui.component.GlassPanel
import com.qingyu.hermescompanion.ui.component.HermesIconKind
import com.qingyu.hermescompanion.ui.component.HermesMulticolorIcon
import com.qingyu.hermescompanion.ui.component.HermesStatusIcon
import com.qingyu.hermescompanion.ui.component.HermesStatusKind
import com.qingyu.hermescompanion.ui.theme.HermesSpacing

@Composable
fun CronDetailScreen(
    state: AppUiState,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onSave: (CronJob, String, String, String) -> Unit,
    onToggle: (CronJob) -> Unit,
    onTrigger: (CronJob) -> Unit,
    onDelete: (CronJob) -> Unit,
) {
    val selected = state.selectedCronJob ?: return
    val job = state.cronJobs.firstOrNull { it.id == selected.id } ?: selected
    val busy = state.cronActionId == job.id
    var editing by remember(job.id) { mutableStateOf(false) }
    var name by remember(job.id) { mutableStateOf(job.name) }
    var prompt by remember(job.id) { mutableStateOf(job.prompt) }
    var schedule by remember(job.id) { mutableStateOf(job.schedule.expression) }
    var confirmDelete by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
        Row(
            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(start = 8.dp, end = 8.dp, top = 6.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                HermesMulticolorIcon(HermesIconKind.BACK, contentDescription = "返回")
            }
            Column(modifier = Modifier.weight(1f).padding(start = 4.dp)) {
                Text(if (editing) "编辑定时任务" else "定时任务详情", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text(
                    if (job.enabled) "已启用 · ${job.schedule.display.ifBlank { job.schedule.expression }}" else "已暂停",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (busy) CircularProgressIndicator(Modifier.padding(end = 10.dp), strokeWidth = 2.dp)
        }

        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = HermesSpacing.page),
        ) {

        GlassPanel(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (editing) {
                    OutlinedTextField(name, { name = it }, label = { Text("任务名称") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(
                        prompt,
                        { prompt = it },
                        label = { Text("任务内容") },
                        minLines = 6,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        schedule,
                        { schedule = it },
                        label = { Text("执行计划") },
                        supportingText = { Text("支持 every 2h、0 9 * * * 或 ISO 时间") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        HermesMulticolorIcon(HermesIconKind.RECENT, contentDescription = null, iconSize = 28.dp)
                        Column(modifier = Modifier.padding(start = 10.dp)) {
                            Text(job.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                HermesStatusIcon(if (job.enabled) HermesStatusKind.CONNECTED else HermesStatusKind.BUSY)
                                Text(
                                    if (job.enabled) "自动执行中" else "当前已暂停",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(start = 6.dp),
                                )
                            }
                        }
                    }
                    DetailField("执行计划", job.schedule.display.ifBlank { job.schedule.expression })
                    DetailField("下次运行", job.nextRunAt.ifBlank { "等待服务器计算" })
                    DetailField("上次运行", job.lastRunAt.ifBlank { "尚未运行" })
                    DetailField("运行状态", job.lastStatus.ifBlank { job.state.ifBlank { "等待执行" } })
                    DetailField("模型", listOf(job.provider, job.model).filter(String::isNotBlank).joinToString(" · ").ifBlank { "使用默认模型" })
                    DetailField("任务内容", job.prompt.ifBlank { "未填写任务内容" })
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (editing) {
                CronActionIcon(
                    icon = HermesIconKind.CLOSE,
                    description = "取消编辑",
                    enabled = !busy,
                    onClick = {
                        name = job.name
                        prompt = job.prompt
                        schedule = job.schedule.expression
                        editing = false
                    },
                )
                CronActionIcon(
                    icon = HermesIconKind.CHECK,
                    description = "保存修改",
                    enabled = !busy && name.isNotBlank() && prompt.isNotBlank() && schedule.isNotBlank(),
                    emphasized = true,
                    onClick = {
                        onSave(job, name, prompt, schedule)
                        editing = false
                    },
                )
            } else {
                CronActionIcon(HermesIconKind.PLAY, "立即运行", !busy, onClick = { onTrigger(job) })
                CronActionIcon(
                    if (job.enabled) HermesIconKind.PAUSE else HermesIconKind.PLAY,
                    if (job.enabled) "暂停任务" else "恢复任务",
                    !busy,
                    onClick = { onToggle(job) },
                )
                CronActionIcon(HermesIconKind.EDIT, "编辑任务", !busy, onClick = { editing = true })
                CronActionIcon(HermesIconKind.DELETE, "删除任务", !busy, destructive = true, onClick = { confirmDelete = true })
            }
        }
        Spacer(Modifier.height(30.dp))
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("删除定时任务？") },
            text = { Text("“${job.name}”将停止自动执行，已有会话记录不会删除。") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete(job) }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("取消") } },
        )
    }
}

@Composable
private fun CronActionIcon(
    icon: HermesIconKind,
    description: String,
    enabled: Boolean,
    emphasized: Boolean = false,
    destructive: Boolean = false,
    onClick: () -> Unit,
) {
    val colors = when {
        destructive -> IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.72f),
            contentColor = MaterialTheme.colorScheme.error,
        )
        emphasized -> IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        )
        else -> IconButtonDefaults.filledTonalIconButtonColors()
    }
    FilledTonalIconButton(onClick = onClick, enabled = enabled, colors = colors) {
        HermesMulticolorIcon(
            icon,
            contentDescription = description,
            tint = when {
                emphasized -> MaterialTheme.colorScheme.onPrimary
                destructive -> MaterialTheme.colorScheme.error
                else -> null
            },
        )
    }
}

@Composable
private fun DetailField(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 3.dp))
    }
}

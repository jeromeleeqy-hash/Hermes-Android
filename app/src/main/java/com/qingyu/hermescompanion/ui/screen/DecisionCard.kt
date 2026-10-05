package com.qingyu.hermescompanion.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qingyu.hermescompanion.data.agentRequestKey
import com.qingyu.hermescompanion.data.approvalChoices
import com.qingyu.hermescompanion.model.*
import com.qingyu.hermescompanion.today.todayText
import com.qingyu.hermescompanion.ui.component.*
import com.qingyu.hermescompanion.ui.AgentRequestCheck
import com.qingyu.hermescompanion.ui.component.HermesButton as Button
import com.qingyu.hermescompanion.ui.component.HermesOutlinedTextField as OutlinedTextField
import com.qingyu.hermescompanion.ui.component.HermesRadioButton as RadioButton

private val LocalRequestPresenter = staticCompositionLocalOf<((AgentRequest) -> Unit)?> { null }
private val LocalRequestChecks = staticCompositionLocalOf { emptyMap<String, AgentRequestCheck>() }
private val LocalRequestDismiss = staticCompositionLocalOf<((AgentRequest) -> Unit)?> { null }
private val LocalRequestRecheck = staticCompositionLocalOf<(() -> Unit)?> { null }

/** A single presenter for every route; summary cards never stack a second authorization window. */
@Composable
internal fun AgentRequestHost(requests: List<AgentRequest>, onRespond: (AgentRequest, String) -> Unit,
    onRecheck: () -> Unit, checks: Map<String, AgentRequestCheck> = emptyMap(),
    onDismissReminder: ((AgentRequest) -> Unit)? = null, content: @Composable () -> Unit) {
    var seen by remember { mutableStateOf(emptySet<String>()) }
    var presented by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(requests.map(::agentRequestKey)) {
        val live = requests.map(::agentRequestKey).toSet()
        seen = seen.intersect(live)
        if (presented !in live) presented = null
        if (presented == null) requests.firstOrNull { agentRequestKey(it) !in seen }?.let {
            presented = agentRequestKey(it)
            seen = seen + agentRequestKey(it)
        }
    }
    CompositionLocalProvider(LocalRequestPresenter provides { request ->
        presented = agentRequestKey(request)
        seen = seen + agentRequestKey(request)
    }, LocalRequestRecheck provides onRecheck, LocalRequestChecks provides checks, LocalRequestDismiss provides onDismissReminder) { content() }
    requests.firstOrNull { agentRequestKey(it) == presented }?.let { request ->
        AgentDecisionDialog(request, onRespond, onDismiss = { presented = null }, onRecheck = onRecheck,
            check = checks[agentRequestKey(request)], onDismissReminder = onDismissReminder)
    }
}

@Composable
internal fun DecisionCard(request: AgentRequest, onRespond: (AgentRequest, String) -> Unit,
    openImmediately: Boolean = false, showSummary: Boolean = true, onDismiss: () -> Unit = {}) {
    val presenter = LocalRequestPresenter.current
    val recheck = LocalRequestRecheck.current
    var expanded by rememberSaveable(agentRequestKey(request)) { mutableStateOf(openImmediately) }
    fun open() { if (presenter != null) presenter(request) else expanded = true }
    if (showSummary) AssistantPanel(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            HomeCardLabel(todayText("等你决定", "Your decision"), "bulb", androidx.compose.ui.graphics.Color(0xFFC88E14))
            Text(request.title, fontSize = 17.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (request.detail.isNotBlank()) Text(request.detail, fontSize = 14.sp, lineHeight = 21.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Button(onClick = ::open, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("agent-request-open")) {
                Text(if (request.isResponding) todayText("正在提交…", "Submitting…") else todayText("查看并确认", "Review and confirm"), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
    if (expanded && presenter == null) AgentDecisionDialog(request, onRespond,
        onDismiss = { expanded = false; onDismiss() }, onRecheck = recheck,
        check = LocalRequestChecks.current[agentRequestKey(request)], onDismissReminder = LocalRequestDismiss.current)
}

@Composable
private fun AgentDecisionDialog(request: AgentRequest, onRespond: (AgentRequest, String) -> Unit,
    onDismiss: () -> Unit, onRecheck: (() -> Unit)?, check: AgentRequestCheck? = null,
    onDismissReminder: ((AgentRequest) -> Unit)? = null) {
    // Runtime IDs and the submitting flag can change during a poll without changing the question.
    val contentKey = request.copy(runtimeSessionId = "", isResponding = false)
    var selected by remember(contentKey) { mutableStateOf(emptySet<String>()) }
    var answer by remember(contentKey) { mutableStateOf("") }
    var confirmRemoval by remember(contentKey) { mutableStateOf(false) }
    val approval = request.type == AgentRequestType.APPROVAL
    val multiple = !approval && request.allowMultiple
    val choices = if (approval) approvalChoices(request) else request.choices
    val response = if (approval) selected.firstOrNull().orEmpty() else buildAgentRequestAnswer(request, selected, answer)
    TodayDetailWindow(onDismiss, title = if (approval) todayText("确认后，再继续", "Confirm to continue") else todayText("还需要你的想法", "Your input is needed")) {
        Column(Modifier.testTag("decision_panel").fillMaxSize()) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (request.profile.isNotBlank()) Text(request.profile, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(request.title, style = MaterialTheme.typography.titleMedium)
                if (request.detail.isNotBlank()) Text(request.detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                choices.forEach { choice ->
                    val checked = choice.value in selected
                    Surface(
                        modifier = Modifier.fillMaxWidth().testTag("agent-choice:${choice.value}").clickable(enabled = !request.isResponding) {
                            selected = if (multiple) { if (checked) selected - choice.value else selected + choice.value } else setOf(choice.value)
                        },
                        shape = RoundedCornerShape(18.dp),
                        color = if (checked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (multiple) Checkbox(checked, onCheckedChange = null) else RadioButton(checked, onClick = null)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(choice.label, style = MaterialTheme.typography.bodyLarge)
                                if (choice.description.isNotBlank()) Text(choice.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                if (!approval && (choices.isEmpty() || multiple)) OutlinedTextField(
                    value = answer, onValueChange = { answer = it }, enabled = !request.isResponding,
                    placeholder = { Text(if (multiple) todayText("补充回答（可选）", "Additional answer (optional)") else todayText("写下你的回答", "Your answer")) },
                    modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 5, shape = RoundedCornerShape(18.dp),
                )
            }
            Surface(color = MaterialTheme.colorScheme.surface) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (!check?.message.isNullOrBlank()) Text(check!!.message, modifier = Modifier.testTag("agent-request-check-result"), style = MaterialTheme.typography.bodySmall)
                    if (onRecheck != null) HermesContentAction(onClick = onRecheck, enabled = !request.isResponding && check?.checking != true,
                        icon = "refresh", modifier = Modifier.fillMaxWidth().testTag("agent-request-recheck")) {
                        if (check?.checking == true) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        Text(if (check?.checking == true) todayText("正在核对…", "Checking…") else todayText("已在其他端处理？核对状态", "Handled elsewhere? Check status"), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    if (check?.canDismiss == true && onDismissReminder != null) HermesContentAction(
                        onClick = { confirmRemoval = true }, enabled = !request.isResponding && !check.checking,
                        icon = "check", modifier = Modifier.fillMaxWidth().testTag("agent-request-remove")) {
                        Text(todayText("已处理，移除此提醒", "Handled — remove reminder"), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Button(onClick = { onRespond(request, response) }, enabled = response.isNotBlank() && !request.isResponding && check?.checking != true, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("agent-request-submit")) {
                        Text(if (request.isResponding) todayText("提交中…", "Submitting…") else todayText("确认并继续", "Confirm and continue"), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
    if (confirmRemoval && check?.canDismiss == true) AlertDialog(onDismissRequest = { confirmRemoval = false },
        title = { Text(todayText("移除这条提醒？", "Remove this reminder?")) },
        text = { Text(todayText("仅移除手机上的这条提醒，不会同意、拒绝或重新执行服务器操作。确认已在其他端处理后再移除。", "This only removes the reminder from this phone. It does not approve, reject, or rerun server work. Remove it after confirming it was handled elsewhere.")) },
        confirmButton = { TextButton(onClick = { confirmRemoval = false; onDismissReminder?.invoke(request) },
            modifier = Modifier.testTag("agent-request-remove-confirm")) { Text(todayText("移除提醒", "Remove reminder")) } },
        dismissButton = { TextButton(onClick = { confirmRemoval = false }) { Text(todayText("取消", "Cancel")) } })

}

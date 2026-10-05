package com.qingyu.hermescompanion.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qingyu.hermescompanion.today.*
import com.qingyu.hermescompanion.ui.component.HermesContentAction
import com.qingyu.hermescompanion.ui.component.AssistantGlyph
import org.json.JSONArray
import org.json.JSONObject

@Composable
private fun CardSmall(text: String, modifier: Modifier = Modifier, maxLines: Int = Int.MAX_VALUE) = Text(text, modifier,
    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
    maxLines = maxLines, overflow = TextOverflow.Ellipsis)

@Composable
private fun CardTile(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier, shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp), content = content)
    }
}

@Composable
private fun BeforeAfter(i: TodayInteraction, compact: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        CardTile(Modifier.weight(1f)) { CardSmall(todayText("之前", "Before")); Text(i.before, style = MaterialTheme.typography.titleMedium, maxLines = if (compact) 3 else Int.MAX_VALUE, overflow = TextOverflow.Ellipsis) }
        Text("→", color = MaterialTheme.colorScheme.onSurfaceVariant)
        CardTile(Modifier.weight(1f)) { CardSmall(todayText("新记录", "New record")); Text(i.after, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, maxLines = if (compact) 3 else Int.MAX_VALUE, overflow = TextOverflow.Ellipsis) }
    }
}

@Composable
private fun ItemLine(item: TodayInteractionItem, prefix: String = "", details: Boolean = true, compact: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        if (prefix.isNotBlank()) Text(prefix, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(item.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (item.value.isNotBlank()) CardSmall(item.value, maxLines = if (compact) 2 else Int.MAX_VALUE)
            if (details && item.detail.isNotBlank()) CardSmall(item.detail, maxLines = if (compact) 2 else Int.MAX_VALUE)
        }
    }
}

@Composable
private fun Trend(i: TodayInteraction) {
    val points = i.series
    if (points.size < 2) return
    val color = MaterialTheme.colorScheme.primary
    val grid = MaterialTheme.colorScheme.outlineVariant
    val minimum = points.minOf { it.value }; val maximum = points.maxOf { it.value }
    val range = (maximum - minimum).takeIf { it > 0 } ?: 1.0
    val description = points.joinToString("; ") { "${it.label}: ${it.value}" }
    Canvas(Modifier.fillMaxWidth().height(86.dp).semantics { contentDescription = description }) {
        val inset = 5.dp.toPx()
        fun position(index: Int): Offset = Offset(inset + (size.width - inset * 2) * index / (points.size - 1),
            inset + ((maximum - points[index].value) / range).toFloat() * (size.height - inset * 2))
        drawLine(grid, Offset(inset, size.height - inset), Offset(size.width - inset, size.height - inset), 1.dp.toPx())
        for (n in 1 until points.size) drawLine(color, position(n - 1), position(n), 2.5.dp.toPx(), StrokeCap.Round)
        points.indices.forEach { drawCircle(color, 3.dp.toPx(), position(it)) }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        CardSmall("${points.first().label} · ${points.first().value}")
        CardSmall("${points.last().label} · ${points.last().value}")
    }
}

/** Compact content in the actual feed. Length limits also apply to legacy fallback prose. */
@Composable
internal fun TodayInteractionSummary(card: TodayCard) {
    val i = card.presentation.interaction ?: return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        when (i.type) {
            TodayLayout.CLARIFICATION -> {
                card.presentation.facts.take(2).forEach { CardSmall(it, maxLines = 2) }
                Text(i.items.take(3).joinToString("  /  ") { it.title }, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            TodayLayout.COMPARISON -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                i.items.take(2).forEach { item -> CardTile(Modifier.weight(1f)) { Text(item.title, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis); CardSmall(item.value.ifBlank { item.detail }, Modifier.heightIn(min = 20.dp), maxLines = 2) } }
            }
            TodayLayout.PRIORITIES -> i.items.take(3).forEachIndexed { index, item -> ItemLine(item, "${index + 1}", false, compact = true) }
            TodayLayout.TIME_PICKER -> { Text(i.items.first().title, style = MaterialTheme.typography.headlineSmall, maxLines = 2, overflow = TextOverflow.Ellipsis); CardSmall(i.timezone); CardSmall(todayText("${i.items.size} 个候选时段", "${i.items.size} candidate times")) }
            TodayLayout.MEETING_ACTIONS -> i.items.take(3).forEach { item ->
                ItemLine(item.copy(detail = listOf(item.owner.ifBlank { todayText("待分配", "Unassigned") }, item.due.ifBlank { todayText("日期待确认", "Date unconfirmed") }).joinToString(" · ")), "·", compact = true)
            }
            TodayLayout.CHECKLIST -> {
                val count = i.items.count { it.status == "done" }
                Text("$count / ${i.items.size}", style = MaterialTheme.typography.headlineLarge)
                LinearProgressIndicator(progress = { count.toFloat() / i.items.size }, modifier = Modifier.fillMaxWidth())
                i.items.firstOrNull { it.status != "done" }?.let { CardSmall(todayText("下一步 · ", "Next · ") + it.title) }
            }
            TodayLayout.MILESTONES -> i.items.take(3).forEachIndexed { index, item -> ItemLine(item, if (item.status == "done") "✓" else "${index + 1}", false, compact = true) }
            TodayLayout.BLOCKER -> CardTile(Modifier.fillMaxWidth()) { CardSmall(todayText("等待补充", "Missing information")); Text(i.text.ifBlank { card.summary }, maxLines = 3, overflow = TextOverflow.Ellipsis) }
            TodayLayout.EXECUTION -> i.items.take(3).forEach { item -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(item.title, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                CardSmall(itemStatus(item.status))
            } }
            TodayLayout.DELIVERABLE -> CardTile(Modifier.fillMaxWidth()) { AssistantGlyph("file", Modifier.size(26.dp)); Text(i.items.first().title, maxLines = 2, overflow = TextOverflow.Ellipsis); CardSmall(i.items.first().value, maxLines = 2) }
            TodayLayout.METRICS -> {
                card.presentation.metrics.firstOrNull()?.let { metric ->
                    Text(metric.value + metric.unit, fontSize = 38.sp, fontWeight = FontWeight.Medium)
                    CardSmall(metric.label)
                }
                Trend(i)
            }
            TodayLayout.CHANGE, TodayLayout.MEMORY_CHANGE -> BeforeAfter(i, compact = true)
            TodayLayout.TIMELINE -> i.items.takeLast(3).forEach { item -> ItemLine(item.copy(value = ""), item.value, false, compact = true) }
            TodayLayout.EVIDENCE -> { Text(i.text.ifBlank { card.summary }, maxLines = 2, overflow = TextOverflow.Ellipsis); CardSmall(todayText("${i.items.size} 条依据或待验证项", "${i.items.size} evidence or open items")) }
            TodayLayout.REVISION -> i.items.firstOrNull()?.let { item ->
                Text(item.detail, style = MaterialTheme.typography.bodyMedium, textDecoration = TextDecoration.LineThrough, maxLines = 2, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(item.value, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.primary)
            }
            TodayLayout.TRIAGE -> i.items.take(3).forEach { item -> Row(verticalAlignment = Alignment.CenterVertically) {
                AssistantGlyph("file", Modifier.size(20.dp)); Text(item.title, Modifier.padding(start = 8.dp), maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
            } }
            TodayLayout.PERSON_FOLLOWUP -> { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(44.dp).background(MaterialTheme.colorScheme.secondaryContainer, CircleShape), contentAlignment = Alignment.Center) { AssistantGlyph("profile", Modifier.size(24.dp)) }
                Text(i.text.ifBlank { card.title }, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }; i.items.take(2).forEach { ItemLine(it, details = false, compact = true) } }
            TodayLayout.QUICK_LOG -> i.fields.firstOrNull()?.let { field -> CardTile(Modifier.fillMaxWidth()) { CardSmall(field.label); Text(field.value.ifBlank { todayText("记一笔", "Add entry") }, style = MaterialTheme.typography.headlineLarge, maxLines = 2, overflow = TextOverflow.Ellipsis) } }
            TodayLayout.REFLECTION -> Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CardTile(Modifier.weight(1f)) { Text("${i.items.count { it.status == "done" }}", style = MaterialTheme.typography.headlineLarge); CardSmall(todayText("已完成", "Completed")) }
                CardTile(Modifier.weight(1f)) { Text("${i.items.count { it.status != "done" }}", style = MaterialTheme.typography.headlineLarge); CardSmall(todayText("待延续", "To carry forward")) }
            }
        }
    }
}

private fun itemStatus(value: String) = when (value) {
    "done" -> todayText("已完成", "Done")
    "active" -> todayText("进行中", "In progress")
    "waiting" -> todayText("等待", "Waiting")
    else -> todayText("未完成", "Open")
}

@Composable
private fun ChoiceRow(label: String, detail: String = "", selected: Boolean, onSelect: () -> Unit, tag: String = "") {
    Surface(Modifier.fillMaxWidth().testTag(tag).selectable(selected, role = Role.RadioButton, onClick = onSelect),
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(14.dp)) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp).heightIn(min = 34.dp), verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected, onClick = null)
            Column(Modifier.padding(start = 10.dp).weight(1f)) { Text(label); if (detail.isNotBlank()) CardSmall(detail) }
        }
    }
}

@Composable
private fun CheckRow(label: String, selected: Boolean, onToggle: () -> Unit, tag: String = "") {
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag(tag).toggleable(selected, role = Role.Checkbox, onValueChange = { onToggle() }), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(selected, onCheckedChange = null)
        Text(label, Modifier.padding(start = 8.dp).weight(1f), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun CardSelect(label: String, value: String, choices: List<Pair<String, String>>, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier) {
        CardSmall(label)
        Box {
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Text(choices.firstOrNull { it.first == value }?.second ?: todayText("请选择", "Choose"), Modifier.weight(1f)); Text("⌄")
            }
            DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
                choices.forEach { (key, text) -> DropdownMenuItem(text = { Text(text) }, onClick = { onChange(key); expanded = false }) }
            }
        }
    }
}

@Composable
internal fun TodayInteractionEditor(card: TodayCard, enabled: Boolean = true, preview: Boolean = false,
    onSubmit: (String) -> Unit, onPath: (String) -> Unit = {}, action: TodayActionState? = null, onProcessing: () -> Unit = {}, onRecovery: () -> Unit = {}) {
    val i = card.presentation.interaction ?: return
    var draftRaw by rememberSaveable(card.id) { mutableStateOf(action?.takeIf { it.expectedCard == card.interactionFingerprint }?.let { runCatching { JSONObject(it.request).getJSONObject("input").toString() }.getOrNull() } ?: initialTodayInput(card)) }
    var draftFingerprint by rememberSaveable(card.id) { mutableStateOf(card.interactionFingerprint) }
    var operationId by rememberSaveable(card.id) { mutableStateOf(action?.takeIf { it.expectedCard == card.interactionFingerprint }?.operationId ?: java.util.UUID.randomUUID().toString()) }
    var error by rememberSaveable(card.id, card.interactionFingerprint) { mutableStateOf("") }
    var feedback by rememberSaveable(card.id, card.interactionFingerprint) { mutableStateOf("") }
    var expandedInfo by rememberSaveable(card.id) { mutableStateOf(false) }
    var expandedSources by rememberSaveable(card.id) { mutableStateOf(false) }
    var expandedData by rememberSaveable(card.id) { mutableStateOf(false) }
    var evidenceFilter by rememberSaveable(card.id) { mutableStateOf(false) }
    var eventId by rememberSaveable(card.id) { mutableStateOf(i.items.lastOrNull()?.id.orEmpty()) }
    val draft = remember(draftRaw) { JSONObject(draftRaw) }
    val changed = draftFingerprint != card.interactionFingerprint
    fun edit(block: (JSONObject) -> Unit) {
        if (!enabled || action?.pending == true || changed) return
        val next = JSONObject(draftRaw); block(next); draftRaw = next.toString()
        operationId = java.util.UUID.randomUUID().toString(); error = ""; feedback = ""
    }
    fun selected(id: String) = draft.getJSONArray("selected_ids").let { a -> (0 until a.length()).any { a.getString(it) == id } }
    fun toggle(id: String) = edit { d ->
        val a = d.getJSONArray("selected_ids"); val ids = (0 until a.length()).map { a.getString(it) }.toMutableList()
        if (!ids.remove(id)) ids.add(id); d.put("selected_ids", JSONArray(ids))
    }
    val note = draft.getString("note")
    val operation = draft.getString("operation")
    Column(Modifier.fillMaxWidth().testTag("interactive-editor")) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).testTag("interactive-scroll").padding(horizontal = 22.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(i.type.label + " · " + card.domain.label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(card.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Medium)
            if (card.readableContext.isNotBlank()) Text(card.readableContext, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (card.presentation.question.isNotBlank() && card.presentation.question != card.title && card.presentation.question != card.readableContext)
                Text(card.presentation.question, style = MaterialTheme.typography.titleMedium)
            if (card.whenLabel.isNotBlank()) CardSmall(card.whenLabel)
            if (!changed) {
            when (i.type) {
                TodayLayout.CLARIFICATION -> {
                    if (card.presentation.facts.isNotEmpty()) Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        card.presentation.facts.take(2).filter { !card.readableContext.contains(it) }.forEach { fact -> CardSmall(fact) }
                    }
                    i.items.forEach { item -> ChoiceRow(item.title, item.detail, draft.getString("selection") == item.id, { edit { it.put("selection", item.id) } }, "interaction-select:${item.id}") }
                }
                TodayLayout.COMPARISON -> {
                    i.items.chunked(2).forEach { pair -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        pair.forEach { item ->
                            val chosen = draft.getString("selection") == item.id
                            Surface(Modifier.weight(1f).testTag("interaction-select:${item.id}").selectable(chosen, role = Role.RadioButton, onClick = { edit { it.put("selection", item.id) } }),
                                color = if (chosen) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh, shape = RoundedCornerShape(18.dp)) {
                                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    RadioButton(chosen, null); Text(item.title, style = MaterialTheme.typography.titleMedium)
                                    if (item.value.isNotBlank()) Text(item.value, style = MaterialTheme.typography.headlineSmall)
                                    CardSmall(item.detail)
                                }
                            }
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    } }
                }
                TodayLayout.PRIORITIES -> {
                    val order = draft.getJSONArray("order").let { a -> (0 until a.length()).map { a.getString(it) } }
                    order.forEachIndexed { index, id ->
                        val item = i.items.first { it.id == id }
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("${index + 1}", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                            Column(Modifier.weight(1f)) { Text(item.title); CardSmall(item.detail) }
                            Column {
                                IconButton(enabled = index > 0, onClick = { edit { val a = order.toMutableList(); java.util.Collections.swap(a, index, index - 1); it.put("order", JSONArray(a)) } },
                                    modifier = Modifier.testTag("interaction-up:$id").semantics { contentDescription = todayText("上移${item.title}", "Move up ${item.title}") }) { Text("↑") }
                                IconButton(enabled = index < order.lastIndex, onClick = { edit { val a = order.toMutableList(); java.util.Collections.swap(a, index, index + 1); it.put("order", JSONArray(a)) } },
                                    modifier = Modifier.semantics { contentDescription = todayText("下移${item.title}", "Move down ${item.title}") }) { Text("↓") }
                            }
                        }
                        HorizontalDivider()
                    }
                }
                TodayLayout.TIME_PICKER -> {
                    CardSmall(i.timezone)
                    i.items.forEach { item -> ChoiceRow(item.title, item.value.ifBlank { item.detail }, draft.getString("selection") == item.id, { edit { it.put("selection", item.id) } }, "interaction-select:${item.id}") }
                    CardSmall(todayText("先形成安排草稿；冲突与日历授权由 Hermes 核对。", "Prepare a draft; Hermes checks conflicts and calendar access."))
                }
                TodayLayout.MEETING_ACTIONS -> i.items.forEach { item ->
                    val row = draft.getJSONObject("rows").getJSONObject(item.id)
                    CheckRow(item.title, selected(item.id), { toggle(item.id) }, "interaction-check:${item.id}")
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(value = row.getString("owner"), onValueChange = { value -> if (value.length <= 60) edit { it.getJSONObject("rows").getJSONObject(item.id).put("owner", value) } },
                            label = { Text(todayText("负责人", "Owner")) }, placeholder = { Text(todayText("待分配", "Unassigned")) }, singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = row.getString("due"), onValueChange = { value -> if (value.length <= 10) edit { it.getJSONObject("rows").getJSONObject(item.id).put("due", value) } },
                            label = { Text(todayText("截止日期", "Due date")) }, placeholder = { Text("YYYY-MM-DD") }, singleLine = true, modifier = Modifier.weight(1f))
                    }
                    HorizontalDivider()
                }
                TodayLayout.CHECKLIST -> {
                    val count = i.items.count { selected(it.id) }
                    Text("$count / ${i.items.size}", style = MaterialTheme.typography.headlineLarge)
                    LinearProgressIndicator(progress = { count.toFloat() / i.items.size }, modifier = Modifier.fillMaxWidth())
                    i.items.forEach { item -> CheckRow(item.title, selected(item.id), { toggle(item.id) }, "interaction-check:${item.id}") }
                    CardSmall(todayText("勾选是本次更新草稿，提交后才交给 Hermes 保存。", "Checks are your draft update until submitted to Hermes."))
                }
                TodayLayout.MILESTONES -> {
                    i.items.forEachIndexed { index, item -> ItemLine(item, if (item.status == "done") "✓" else "${index + 1}") }
                    if (i.text.isNotBlank()) CardTile(Modifier.fillMaxWidth()) { CardSmall(todayText("验收条件", "Acceptance criteria")); Text(i.text) }
                    CheckRow(todayText("我已核对交付物与验收条件", "I reviewed the deliverables and criteria"), draft.getBoolean("reviewed"), { edit { it.put("reviewed", !it.getBoolean("reviewed")) } }, "interaction-reviewed")
                }
                TodayLayout.BLOCKER -> { CardTile(Modifier.fillMaxWidth()) { CardSmall(todayText("还缺什么", "What is missing")); Text(i.text.ifBlank { card.summary }) }; i.items.forEach { ItemLine(it, if (it.status == "done") "✓" else "·") } }
                TodayLayout.EXECUTION -> {
                    i.items.forEach { item -> CardTile(Modifier.fillMaxWidth()) { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { Text(item.title, Modifier.weight(1f)); CardSmall(itemStatus(item.status)) }; if (item.detail.isNotBlank()) CardSmall(item.detail) } }
                    CardSmall(todayText("这是上次记录的执行阶段；可让 Hermes 核对最新状态。", "These are the last recorded stages; ask Hermes to check the current state."))
                }
                TodayLayout.DELIVERABLE -> {
                    i.items.forEach { item -> CardTile(Modifier.fillMaxWidth()) { AssistantGlyph("file", Modifier.size(28.dp)); Text(item.title, style = MaterialTheme.typography.titleMedium); CardSmall(item.detail); CardSmall(item.value) } }
                    ChoiceRow(todayText("采用这一版", "Adopt this version"), selected = operation == "adopt", onSelect = { edit { it.put("operation", "adopt") } }, tag = "interaction-operation:adopt")
                    ChoiceRow(todayText("需要修改", "Request changes"), selected = operation == "revise", onSelect = { edit { it.put("operation", "revise") } }, tag = "interaction-operation:revise")
                }
                TodayLayout.METRICS -> {
                    card.presentation.metrics.forEach { metric -> CardTile(Modifier.fillMaxWidth()) { CardSmall(metric.label); Text(metric.value + metric.unit, style = MaterialTheme.typography.displaySmall) } }
                    Trend(i)
                    if (i.series.isNotEmpty()) HermesContentAction(onClick = { expandedData = !expandedData }) { Text(if (expandedData) todayText("收起数据", "Hide values") else todayText("查看数据", "View values")) }
                    if (expandedData) i.series.forEach { CardSmall("${it.label} · ${it.value}") }
                    if (i.text.isNotBlank()) CardSmall(i.text)
                }
                TodayLayout.CHANGE -> {
                    BeforeAfter(i)
                    i.items.forEach { ItemLine(it, "·") }
                    ChoiceRow(todayText("接受新信息，调整内部计划", "Accept and adjust the plan"), selected = operation == "accept", onSelect = { edit { it.put("operation", "accept") } }, tag = "interaction-operation:accept")
                    ChoiceRow(todayText("保留原计划，继续核对冲突", "Keep the plan and check the conflict"), selected = operation == "keep", onSelect = { edit { it.put("operation", "keep") } }, tag = "interaction-operation:keep")
                }
                TodayLayout.TIMELINE -> {
                    i.items.forEach { item -> ChoiceRow(item.title, item.value, eventId == item.id, { eventId = item.id }, "interaction-event:${item.id}") }
                    i.items.firstOrNull { it.id == eventId }?.let { CardTile(Modifier.fillMaxWidth()) { CardSmall(it.detail) } }
                }
                TodayLayout.EVIDENCE -> {
                    Text(i.text.ifBlank { card.summary }, style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilterChip(!evidenceFilter, { evidenceFilter = false }, label = { Text(todayText("已有依据", "Evidence")) })
                        FilterChip(evidenceFilter, { evidenceFilter = true }, label = { Text(todayText("待验证", "Open questions")) })
                    }
                    val visible = i.items.filter { (it.status == "waiting") == evidenceFilter }
                    if (visible.isEmpty()) CardSmall(todayText("没有对应的记录", "No entries in this section"))
                    visible.forEach { ItemLine(it, "·") }
                }
                TodayLayout.REVISION -> {
                    i.items.forEach { item -> CardTile(Modifier.fillMaxWidth()) {
                        CheckRow(item.title, selected(item.id), { toggle(item.id) }, "interaction-check:${item.id}")
                        Text(item.detail, style = MaterialTheme.typography.bodyMedium, textDecoration = TextDecoration.LineThrough, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(item.value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                    } }
                    CardSmall(todayText("保留未选中的原文，采用的修改保存为新版本。", "Keep unselected original text and save accepted edits as a new version."))
                }
                TodayLayout.TRIAGE -> i.items.forEach { item ->
                    ItemLine(item.copy(value = ""))
                    CardSelect(todayText("存放方式", "Classification"), draft.getJSONObject("rows").getJSONObject(item.id).getString("classification"),
                        listOf("archive" to todayText("归档资料", "Archive"), "lead" to todayText("任务线索 · 待确认", "Task lead · unconfirmed"), "later" to todayText("暂不处理", "Later")),
                        { value -> edit { it.getJSONObject("rows").getJSONObject(item.id).put("classification", value) } })
                    HorizontalDivider()
                }
                TodayLayout.MEMORY_CHANGE -> {
                    BeforeAfter(i)
                    if (i.text.isNotBlank()) CardSmall(i.text)
                    ChoiceRow(todayText("纠正这项记忆", "Correct this memory"), selected = operation == "correct", onSelect = { edit { it.put("operation", "correct") } }, tag = "interaction-operation:correct")
                    ChoiceRow(todayText("撤销这次变更", "Undo this change"), selected = operation == "undo", onSelect = { edit { it.put("operation", "undo") } }, tag = "interaction-operation:undo")
                }
                TodayLayout.PERSON_FOLLOWUP -> {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.size(52.dp).background(MaterialTheme.colorScheme.secondaryContainer, CircleShape), contentAlignment = Alignment.Center) { AssistantGlyph("profile", Modifier.size(28.dp)) }
                        Text(i.text.ifBlank { card.title }, style = MaterialTheme.typography.headlineSmall)
                    }
                    i.items.forEach { ItemLine(it) }
                    CardSmall(todayText("只准备可编辑的跟进草稿，由你决定是否发送。", "Prepare an editable draft for you to review before sending."))
                }
                TodayLayout.QUICK_LOG -> if (i.text.isNotBlank()) Text(i.text)
                TodayLayout.REFLECTION -> {
                    TodayInteractionSummary(card)
                    i.items.filter { it.status == "done" }.forEach { ItemLine(it, "✓", false) }
                    i.items.filter { it.status != "done" }.forEach { item -> CheckRow(todayText("明日重点 · ", "Tomorrow · ") + item.title, selected(item.id), { toggle(item.id) }, "interaction-check:${item.id}") }
                }
            }
            i.fields.forEach { field ->
                val value = draft.getJSONObject("fields").optString(field.id)
                if (field.kind == "select") CardSelect(field.label, value, field.choices.map { it to it }, { v -> edit { it.getJSONObject("fields").put(field.id, v) } }, Modifier.testTag("interaction-field:${field.id}"))
                else OutlinedTextField(value, { v -> if (v.length <= 500) edit { it.getJSONObject("fields").put(field.id, v) } },
                    label = { Text(field.label + if (field.required) " *" else "") }, modifier = Modifier.fillMaxWidth().testTag("interaction-field:${field.id}"),
                    keyboardOptions = KeyboardOptions(keyboardType = if (field.kind == "number") KeyboardType.Decimal else KeyboardType.Text),
                    supportingText = { when (field.kind) { "date" -> Text("YYYY-MM-DD"); "time" -> Text("HH:mm"); "number" -> Text("${field.min} – ${field.max}"); else -> {} } }, singleLine = field.kind != "text")
            }
            val needsNote = i.type in setOf(TodayLayout.BLOCKER, TodayLayout.TIMELINE, TodayLayout.REFLECTION, TodayLayout.PERSON_FOLLOWUP, TodayLayout.EVIDENCE) ||
                (i.type == TodayLayout.DELIVERABLE && operation == "revise") || (i.type == TodayLayout.MEMORY_CHANGE && operation == "correct")
            if (needsNote) OutlinedTextField(note, { value -> if (value.length <= 1000) edit { it.put("note", value) } },
                label = { Text(when (i.type) { TodayLayout.REFLECTION -> todayText("一句心得 · 可不填", "Reflection · optional"); TodayLayout.PERSON_FOLLOWUP -> todayText("语气或补充要求 · 可不填", "Tone or instructions · optional"); TodayLayout.EVIDENCE -> todayText("重点核验什么 · 可不填", "Focus of investigation · optional"); else -> todayText("补充具体内容", "Add specific information") }) },
                modifier = Modifier.fillMaxWidth().testTag("interaction-note"), minLines = 2, maxLines = 5)
            }
            if (i.note.isNotBlank()) CardSmall(i.note)
            if (card.summary.isNotBlank() || card.backgroundText.isNotBlank() || i.text.length > 200) {
                HermesContentAction(onClick = { expandedInfo = !expandedInfo }) { Text(if (expandedInfo) todayText("收起背景", "Hide background") else todayText("查看背景", "View background")) }
                if (expandedInfo) Text(listOf(card.summary, card.backgroundText).filter(String::isNotBlank).distinct().joinToString("\n\n"), style = MaterialTheme.typography.bodyMedium)
            }
            if (card.sources.isNotEmpty() || card.unavailableSources.isNotEmpty()) {
                HermesContentAction(icon = "folder", onClick = { expandedSources = !expandedSources }) { Text(todayText("来源 · ${card.sources.size + card.unavailableSources.size}", "Sources · ${card.sources.size + card.unavailableSources.size}")) }
                if (expandedSources) {
                    card.sources.forEach { path -> HermesContentAction(icon = "file", onClick = { onPath(path) }, enabled = !preview) { Text(path.substringAfterLast('/').substringAfterLast('\\')) } }
                    card.unavailableSources.forEach { CardSmall(todayText("待核对 · $it", "Unverified · $it")) }
                }
            }
            if (feedback.isNotBlank()) CardTile(Modifier.fillMaxWidth().testTag("interaction-preview-result")) { Text(feedback) }
        }
        HorizontalDivider()
        Column(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            if (action != null) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (action.busy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    Text(action.message, Modifier.weight(1f).testTag("interaction-server-result"), style = MaterialTheme.typography.bodySmall)
                }
                if (action.sessionId.isNotBlank()) HermesContentAction(onClick = onProcessing) { Text(todayText("查看处理详情", "Processing details")) }
                if (action.status in setOf("uncertain", "failed")) HermesContentAction(onClick = onRecovery, enabled = enabled,
                    modifier = Modifier.testTag("interaction-recover")) { Text(todayText("继续未完成部分", "Continue unfinished work")) }
            }
            if (changed) {
                CardSmall(todayText("服务器上的卡片已更新，你的输入仍保留。载入新内容后再操作。", "The server card changed. Your input is preserved; load the update before editing."))
                HermesContentAction(onClick = { draftRaw = initialTodayInput(card); draftFingerprint = card.interactionFingerprint; operationId = java.util.UUID.randomUUID().toString(); error = "" }) {
                    Text(todayText("载入更新内容", "Load updated content"))
                }
            }
            if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.testTag("interaction-error"))
            Button(onClick = {
                runCatching { todayInteractionRequest(card, draftRaw, operationId) }.onSuccess { request ->
                    if (preview) feedback = todayText("体验操作已完成。这些选择仅用于演示，没有发送或修改真实记录。", "Demo only. Nothing was sent or changed in your records.")
                    else onSubmit(request)
                }.onFailure { error = it.message ?: todayText("请检查输入内容", "Check your input") }
            }, enabled = enabled && !card.isClosed && !changed && action?.busy != true && !(action?.status == "applied" && action.expectedCard == card.interactionFingerprint),
                modifier = Modifier.fillMaxWidth().testTag("interaction-submit")) {
                Text(if (action?.status == "uncertain") todayText("核对处理结果", "Check the result") else i.type.actionLabel)
            }
            CardSmall(if (preview) todayText("示例数据 · 操作仅供体验", "Sample data · local demo") else todayText("在这里等待结果，服务器确认后更新状态", "Stay here; the state updates after server confirmation"))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TodayInteractiveSheet(card: TodayCard, enabled: Boolean, onDismiss: () -> Unit, onSubmit: (String) -> Unit, onPath: (String) -> Unit,
    action: TodayActionState? = null, onProcessing: () -> Unit = {}, onRecovery: () -> Unit = {}) {
    TodayDetailWindow(onDismiss) {
        Box(Modifier.fillMaxSize()) { TodayInteractionEditor(card, enabled, onSubmit = onSubmit, onPath = onPath, action = action, onProcessing = onProcessing, onRecovery = onRecovery) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TodayCardGallerySheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val cards = remember { runCatching { TodayBoard.decode(context.assets.open("hermes-today-interactions.json").bufferedReader().use { it.readText() }, "/example").cards }.getOrDefault(emptyList()) }
    var index by rememberSaveable { mutableIntStateOf(0) }
    var menu by remember { mutableStateOf(false) }
    TodayDetailWindow(onDismiss, todayText("卡片体验", "Card preview")) {
        Column(Modifier.fillMaxSize().testTag("today-gallery")) {
            if (cards.isEmpty()) { Text(todayText("示例暂时无法读取", "Samples unavailable"), Modifier.padding(24.dp)); return@Column }
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { index = (index + cards.size - 1) % cards.size }, modifier = Modifier.testTag("gallery-previous")) { Text(todayText("上一种", "Previous")) }
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    TextButton(onClick = { menu = true }, modifier = Modifier.testTag("gallery-picker")) { Text("${index + 1} / ${cards.size} · ${cards[index].presentation.interaction?.type?.label.orEmpty()}") }
                    DropdownMenu(menu, { menu = false }) { cards.forEachIndexed { n, card -> DropdownMenuItem(text = { Text(card.presentation.interaction!!.type.label) }, onClick = { index = n; menu = false }) } }
                }
                TextButton(onClick = { index = (index + 1) % cards.size }, modifier = Modifier.testTag("gallery-next")) { Text(todayText("下一种", "Next")) }
            }
            key(index) { TodayInteractionEditor(cards[index], preview = true, onSubmit = {}) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TodayScheduleSheet(onDismiss: () -> Unit, onConfigure: (String, String, String) -> Unit,
    check: TodayScheduleCheck = TodayScheduleCheck(), onCheck: () -> Unit = {}) {
    var morning by rememberSaveable { mutableStateOf("09:00") }
    var evening by rememberSaveable { mutableStateOf("21:00") }
    var zone by rememberSaveable { mutableStateOf(java.time.ZoneId.systemDefault().id) }
    var edited by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(check.morning, check.evening, check.timezone) {
        if (!edited && check.morning.isNotBlank() && check.evening.isNotBlank() && check.timezone.isNotBlank()) {
            morning = check.morning; evening = check.evening; zone = check.timezone
        }
    }
    var error by rememberSaveable { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState()).padding(24.dp).testTag("today-schedule"), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(todayText("让 Hermes 早晚整理一次", "Morning and evening briefings"), style = MaterialTheme.typography.headlineSmall)
            CardSmall(todayText("只需配置一次，以后每天自动运行。已有设置会从服务器读取；仅在需要改时间时重新保存。暂停或恢复请到「任务」。", "Configure once for daily automatic briefings. Saved times load from your server; save again only to change them. Pause or resume jobs in Tasks."))
            OutlinedTextField(morning, { morning = it; error = ""; edited = true }, label = { Text(todayText("早间时间", "Morning")) }, placeholder = { Text("09:00") }, modifier = Modifier.fillMaxWidth().testTag("schedule-morning"), singleLine = true)
            OutlinedTextField(evening, { evening = it; error = ""; edited = true }, label = { Text(todayText("晚间时间", "Evening")) }, placeholder = { Text("21:00") }, modifier = Modifier.fillMaxWidth().testTag("schedule-evening"), singleLine = true)
            OutlinedTextField(zone, { zone = it; error = ""; edited = true }, label = { Text(todayText("时区", "Timezone")) }, supportingText = { Text("Asia/Shanghai · Europe/London") }, modifier = Modifier.fillMaxWidth().testTag("schedule-zone"), singleLine = true)
            CardSmall(todayText("Hermes 将保存卡片规范并创建或更新对应的定时任务。以处理对话中的实际核验结果为准。", "Hermes will save the card contract and create or update the matching jobs. Check the verified result in the processing conversation."))
            if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("schedule-error"))
            Button(onClick = {
                runCatching { todayScheduleConfig(morning.trim(), evening.trim(), zone.trim()) }.onSuccess { onConfigure(morning.trim(), evening.trim(), zone.trim()) }
                    .onFailure { error = it.message.orEmpty() }
            }, enabled = !check.loading, modifier = Modifier.fillMaxWidth().testTag("schedule-submit")) { Text(todayText("交给 Hermes 配置", "Configure with Hermes")) }
            HermesContentAction(icon = "refresh", onClick = onCheck, enabled = !check.loading, modifier = Modifier.testTag("schedule-check")) {
                Text(if (check.loading) todayText("正在核对服务器…", "Checking server…") else todayText("核对已配置的任务", "Verify configured jobs"))
            }
            if (check.message.isNotBlank()) CardSmall(check.message)
            check.jobs.forEach { CardTile(Modifier.fillMaxWidth()) { Text(it, style = MaterialTheme.typography.bodySmall) } }
            Spacer(Modifier.height(12.dp))
        }
    }
}

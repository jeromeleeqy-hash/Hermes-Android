package com.qingyu.hermescompanion.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.qingyu.hermescompanion.today.*
import com.qingyu.hermescompanion.ui.component.HermesContentAction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TodayCardDetailSheet(card: TodayCard, onDismiss: () -> Unit, onPath: (String) -> Unit, onAction: (String) -> Unit,
    onQuickResponse: (() -> Unit)? = null, action: TodayActionState? = null) {
    var background by rememberSaveable(card.id) { mutableStateOf(false) }
    var sources by rememberSaveable(card.id) { mutableStateOf(false) }
    var choices by rememberSaveable(card.id) { mutableStateOf(false) }
    val presentation = card.presentation
    val compact = remember(card.id) { card.title.length + card.readableContext.length < 150 &&
        presentation.steps.size <= 2 && presentation.interaction == null }
    TodayDetailWindow(onDismiss, compact = compact) {
        Column(Modifier.fillMaxWidth().fillMaxHeight().testTag("today-detail")) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).testTag("today-detail-scroll")
                .padding(horizontal = 24.dp).padding(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(card.domain.label + " · " + card.attentionGroup.label, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                Text(card.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Medium)
                if (card.readableContext.isNotBlank()) Text(card.readableContext, style = MaterialTheme.typography.bodyMedium)
                TodayReadableContent(card, expanded = true)
                if (presentation.question.isNotBlank() && presentation.question != card.title) {
                    Text(presentation.question, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
                if (onQuickResponse != null || presentation.options.isNotEmpty()) HermesContentAction(onClick = {
                    if (onQuickResponse != null) onQuickResponse() else choices = !choices
                }, modifier = Modifier.testTag("today-quick-response")) { Text(todayText("快捷回应", "Quick response")) }
                if (choices) presentation.options.forEachIndexed { index, option ->
                    OutlinedButton(onClick = { onAction("option:$index") }, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium,
                        contentPadding = PaddingValues(14.dp)) {
                        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(option.title, style = MaterialTheme.typography.titleSmall)
                            if (option.detail.isNotBlank()) Text(option.detail, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                if (card.attention?.reason?.isNotBlank() == true || card.backgroundText.isNotBlank() || presentation.facts.isNotEmpty() || (card.summary.isNotBlank() && card.summary != card.readableContext)) {
                    HermesContentAction(onClick = { background = !background }, modifier = Modifier.testTag("today-background-toggle")) {
                        Text(if (background) todayText("收起背景", "Hide background") else todayText("查看背景", "View background"))
                    }
                    if (background) Text(listOf(card.summary.takeUnless { it == card.readableContext }.orEmpty(), presentation.facts.joinToString("\n"), presentation.background, card.attention?.reason.orEmpty()).filter(String::isNotBlank).distinct().joinToString("\n\n"),
                        Modifier.testTag("today-background"), style = MaterialTheme.typography.bodyMedium)
                }
                val sourceCount = card.sources.size + card.unavailableSources.size
                if (sourceCount > 0 || presentation.issue != null) {
                    HermesContentAction(icon = "folder", onClick = { sources = !sources }, modifier = Modifier.testTag("today-sources-toggle")) {
                        Text(if (sources) todayText("收起来源", "Hide sources") else todayText("查看来源 · $sourceCount", "Sources · $sourceCount"))
                    }
                    if (sources) {
                        card.sources.forEach { source -> HermesContentAction(icon = "file", onClick = { onPath(source) }) { Text(source.substringAfterLast('/').substringAfterLast('\\')) } }
                        card.unavailableSources.forEach { Text(todayText("待核对 · $it", "Needs checking · $it"), style = MaterialTheme.typography.bodySmall) }
                        presentation.issue?.let { Text(todayText("展示字段需修复：$it", "Presentation needs repair: $it"), style = MaterialTheme.typography.bodySmall) }
                    }
                }
            }
            HorizontalDivider()
            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (action != null) Column(Modifier.fillMaxWidth()) {
                    HermesContentAction(onClick = { onAction("processing") }) { Text(todayActionLabel(action)) }
                    if (action.status in setOf("uncertain", "failed")) HermesContentAction(onClick = { onAction("recover") }) {
                        Text(todayText("继续未完成部分", "Continue remaining work"))
                    }
                }
                Button(onClick = { onAction("discuss") }, modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp).testTag("today-detail-discuss")) {
                    Text(todayText("聊聊这件事", "Talk about this"))
                }
            }
        }
    }
}

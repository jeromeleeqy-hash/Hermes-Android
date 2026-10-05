package com.qingyu.hermescompanion.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qingyu.hermescompanion.today.*
import com.qingyu.hermescompanion.ui.component.AssistantGlyph

/** Read-only visual summaries. Opening or scrolling a card never submits a decision. */
@Composable
internal fun TodayReadableContent(card: TodayCard, expanded: Boolean = false) {
    val p = card.presentation
    val interaction = p.interaction
    val metrics = p.metrics.ifEmpty {
        if (interaction?.type == TodayLayout.METRICS) interaction.items.filter { it.value.isNotBlank() }
            .take(3).map { TodayMetric(it.title, it.value) } else emptyList()
    }
    val steps = p.steps.ifEmpty {
        if (interaction?.type == TodayLayout.CHECKLIST && interaction.items.any { it.status == "done" }) interaction.items.map { TodayStep(it.title, it.status == "done") } else emptyList()
    }
    val colors = MaterialTheme.colorScheme
    when {
        metrics.isNotEmpty() -> {
            Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                metrics.take(3).forEach { metric ->
                    Column(Modifier.weight(1f).background(colors.primary.copy(alpha = .045f), RoundedCornerShape(14.dp)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(metric.value + metric.unit, fontSize = if (metrics.size > 2) 22.sp else 27.sp, lineHeight = 32.sp,
                            color = colors.primary, fontWeight = FontWeight.SemiBold)
                        Text(metric.label, style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                    }
                }
            }
        }
        steps.isNotEmpty() -> {
            val done = steps.count { it.done }
            Column(Modifier.fillMaxWidth().padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(todayText("已确认的进展", "Recorded progress"), style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                    Text("$done / ${steps.size}", style = MaterialTheme.typography.labelMedium, color = colors.primary)
                }
                LinearProgressIndicator(progress = { done.toFloat() / steps.size }, modifier = Modifier.fillMaxWidth().height(5.dp),
                    color = colors.primary, trackColor = colors.primary.copy(alpha = .08f))
                (if (expanded) steps else steps.filterNot { it.done }.take(2)).forEach { step ->
                    Row(verticalAlignment = Alignment.Top) {
                        Text(if (step.done) "✓" else "•", color = colors.primary, modifier = Modifier.width(20.dp))
                        Text(step.title, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                    }
                }
            }
        }
        card.whenLabel.isNotBlank() && (card.kind == TodayKind.SCHEDULE || p.layout == "schedule") -> {
            Surface(Modifier.padding(top = 12.dp), shape = RoundedCornerShape(12.dp), color = colors.primary.copy(alpha = .07f)) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    AssistantGlyph("calendar", Modifier.size(18.dp), colors.primary)
                    Text(card.whenLabel, Modifier.padding(start = 8.dp), style = MaterialTheme.typography.labelLarge, color = colors.primary)
                }
            }
        }
        interaction?.type == TodayLayout.TIMELINE -> {
            Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                interaction.items.take(if (expanded) 12 else 3).forEach { item ->
                    Row(verticalAlignment = Alignment.Top) {
                        Text("•", Modifier.width(18.dp), color = colors.primary)
                        Column(Modifier.weight(1f)) {
                            Text(item.title, style = MaterialTheme.typography.bodyMedium)
                            if (item.detail.isNotBlank()) Text(item.detail, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant,
                                maxLines = if (expanded) Int.MAX_VALUE else 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
        interaction?.type in setOf(TodayLayout.MEMORY_CHANGE, TodayLayout.CHANGE) -> {
            Column(Modifier.padding(top = 12.dp).background(colors.surfaceContainerLow, RoundedCornerShape(12.dp)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(todayText("记录中的变化", "Change to review"), style = MaterialTheme.typography.labelMedium, color = colors.primary)
                Text(interaction!!.before, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant,
                    maxLines = if (expanded) Int.MAX_VALUE else 2, overflow = TextOverflow.Ellipsis)
                Text("→ " + interaction.after, style = MaterialTheme.typography.bodyMedium,
                    maxLines = if (expanded) Int.MAX_VALUE else 3, overflow = TextOverflow.Ellipsis)
            }
        }
    }
    if (expanded && card.whenLabel.isNotBlank() && card.kind != TodayKind.SCHEDULE && p.layout != "schedule") {
        Text(card.whenLabel, Modifier.padding(top = 8.dp), style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
    }

}

internal fun todayActionLabel(action: TodayActionState): String = when (action.status) {
    "applied" -> todayText("已记录", "Recorded")
    "awaiting_input" -> todayText("需要你确认", "Needs your confirmation")
    "failed" -> todayText("这次未完成，点开查看", "Not completed · View details")
    "uncertain" -> todayText("结果待核对", "Result needs checking")
    else -> todayText("正在处理", "Working on it")
}

package com.qingyu.hermescompanion.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.qingyu.hermescompanion.model.*
import com.qingyu.hermescompanion.today.todayText
import com.qingyu.hermescompanion.ui.component.*

@Composable
internal fun ConversationMaterials(materials: List<ChatArtifact>, turnKey: String, onOpen: (ChatArtifact) -> Unit) {
    if (materials.isEmpty()) return
    var expanded by rememberSaveable(turnKey) { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth()) {
        TextButton(onClick = { expanded = !expanded }, contentPadding = PaddingValues(0.dp), modifier = Modifier.testTag("conversation-materials-toggle")) {
            AssistantGlyph("file", Modifier.size(16.dp), MaterialTheme.colorScheme.onSurfaceVariant)
            Text(todayText("参考资料 · ${materials.size}", "References · ${materials.size}"), Modifier.padding(horizontal = 7.dp),
                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(if (expanded) "−" else "+", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (expanded) GlassPanel(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 5.dp)) {
                materials.forEachIndexed { index, file ->
                    if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .3f))
                    Row(Modifier.fillMaxWidth().clickable { onOpen(file) }.padding(vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
                        AssistantGlyph("file", Modifier.size(18.dp), MaterialTheme.colorScheme.primary)
                        Text(file.name, Modifier.weight(1f).padding(horizontal = 10.dp), style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2, overflow = TextOverflow.Ellipsis)
                        AssistantGlyph("chevron", Modifier.size(16.dp), MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
internal fun ConversationProcessDetails(todos: List<ChatTodo>, tools: List<ToolActivity>, turnKey: String) {
    if (todos.isEmpty() && tools.isEmpty()) return
    var expanded by rememberSaveable(turnKey) { mutableStateOf(false) }
    Column {
        TextButton(onClick = { expanded = !expanded }, contentPadding = PaddingValues(0.dp), modifier = Modifier.testTag("conversation-process-toggle")) {
            Text(if (expanded) todayText("收起处理详情", "Hide activity") else todayText("处理详情", "Activity details"),
                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (expanded) WorkProgress(todos, tools)
    }
}

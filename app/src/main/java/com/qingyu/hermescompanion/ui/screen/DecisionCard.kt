package com.qingyu.hermescompanion.ui.screen
import com.qingyu.hermescompanion.ui.component.HermesOutlinedTextField as OutlinedTextField
import com.qingyu.hermescompanion.ui.component.HermesRadioButton as RadioButton


import com.qingyu.hermescompanion.i18n.uiText
import com.qingyu.hermescompanion.R


import com.qingyu.hermescompanion.ui.component.HermesButton as Button

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
import com.qingyu.hermescompanion.ui.component.*
import com.qingyu.hermescompanion.model.*
import com.qingyu.hermescompanion.ui.theme.HermesColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DecisionCard(request: AgentRequest, onRespond: (AgentRequest, String) -> Unit) {
    var expanded by rememberSaveable(request.requestId) { mutableStateOf(false) }
    var selected by remember(request.requestId) { mutableStateOf(emptySet<String>()) }
    var answer by rememberSaveable(request.requestId) { mutableStateOf("") }
    val approval = request.type == AgentRequestType.APPROVAL
    val multiple = !approval && request.allowMultiple
    val choices = if (approval) buildList {
        add(AgentRequestChoice(uiText(R.string.ui_0829, "仅本次允许"), "once"))
        if (request.allowSession) add(AgentRequestChoice(uiText(R.string.ui_0830, "本次会话允许"), "session"))
        if (request.allowPermanent) add(AgentRequestChoice(uiText(R.string.ui_0831, "始终允许"), "always"))
        add(AgentRequestChoice(uiText(R.string.ui_0832, "拒绝"), "deny"))
    } else request.choices
    val response = if (approval) selected.firstOrNull().orEmpty() else buildAgentRequestAnswer(request, selected, answer)
    AssistantPanel(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            HomeCardLabel(uiText(R.string.ui_0833, "等你决定"), "bulb", androidx.compose.ui.graphics.Color(0xFFC88E14))
            Text(request.title, fontSize = 17.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (request.detail.isNotBlank()) Text(request.detail, fontSize = 14.sp, lineHeight = 21.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (choices.isNotEmpty()) Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                choices.take(2).forEachIndexed { i,choice ->
                    HomeChoice(choice.label, if(i == 0) "bulb" else "file", if(i == 0) AssistantMint else AssistantPurple, Modifier.weight(1f)) { expanded = true }
                }
            }
            TextButton(colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer), onClick = { expanded = true }, contentPadding = PaddingValues(0.dp)) {
                Text(if(request.isResponding) uiText(R.string.ui_0834, "正在提交…") else uiText(R.string.ui_0835, "看看建议"), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = AssistantBlue)
                Spacer(Modifier.width(8.dp)); AssistantGlyph("arrow", Modifier.size(21.dp), AssistantBlue)
            }
        }
    }
    if (expanded) ModalBottomSheet(
        onDismissRequest = { expanded = false },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        Column(Modifier.testTag("decision_panel").fillMaxWidth().fillMaxHeight(0.92f).imePadding().navigationBarsPadding()) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(if (approval) uiText(R.string.ui_0836, "确认后，再继续") else uiText(R.string.ui_0837, "还需要你的想法"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                Text(request.title, style = MaterialTheme.typography.titleMedium)
                if (request.detail.isNotBlank()) Text(request.detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                choices.forEach { choice ->
                    val checked = choice.value in selected
                    Surface(
                        modifier = Modifier.fillMaxWidth().clickable(enabled = !request.isResponding) {
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
                    placeholder = { Text(if (multiple) uiText(R.string.ui_0838, "补充回答（可选）") else uiText(R.string.ui_0839, "写下你的回答")) },
                    modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 5, shape = RoundedCornerShape(18.dp),
                )
                Spacer(Modifier.height(8.dp))
            }
            Surface(color = MaterialTheme.colorScheme.surface) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (selected.isNotEmpty()) Text(uiText(R.string.ui_0840, "已选 %1\$s 项", selected.size), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = { onRespond(request, response) }, enabled = response.isNotBlank() && !request.isResponding, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                        Text(if (request.isResponding) uiText(R.string.ui_0841, "提交中…") else uiText(R.string.ui_0842, "确认并继续"))
                    }
                }
            }
        }
    }
}

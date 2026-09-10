package com.qingyu.hermescompanion.ui.screen
import com.qingyu.hermescompanion.ui.component.HermesRadioButton as RadioButton


import com.qingyu.hermescompanion.i18n.uiText
import com.qingyu.hermescompanion.R


import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import com.qingyu.hermescompanion.ui.component.HermesAlertDialog as AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.qingyu.hermescompanion.model.HermesProfile
import com.qingyu.hermescompanion.ui.AppUiState
import com.qingyu.hermescompanion.ui.component.HermesIconKind
import com.qingyu.hermescompanion.ui.component.HermesMulticolorIcon
import com.qingyu.hermescompanion.ui.format.ellipsizeSessionTitle
import com.qingyu.hermescompanion.assistant.DailyConversation

@Composable
fun ShareToHermesDialog(
    state: AppUiState,
    onSelectProfile: (HermesProfile) -> Unit,
    onInstructionChange: (String) -> Unit,
    onSend: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    val payload = state.incomingShare ?: return
    var selectedSessionId by remember(state.activeProfile, payload.sharedText, payload.attachments) {
        mutableStateOf<String?>(DailyConversation.SHARE_TARGET)
    }
    var chooseConversation by remember(state.activeProfile, payload.sharedText, payload.attachments) { mutableStateOf(false) }
    val canSend = payload.sharedText.isNotBlank() || payload.attachments.isNotEmpty()
    AlertDialog(
        onDismissRequest = { if (!state.isShareSending) onDismiss() },
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                HermesMulticolorIcon(HermesIconKind.SEND, contentDescription = null, iconSize = 22.dp)
                Text(uiText(R.string.ui_1263, "分享到 Hermes"), modifier = Modifier.padding(start = 9.dp), color = MaterialTheme.colorScheme.onSurface)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 440.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (state.profiles.size > 1) {
                Text(uiText(R.string.ui_1264, "发送给"), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    state.profiles.forEach { profile ->
                        val selected = profile.name == state.activeProfile
                        Text(
                            profile.name,
                            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                            modifier = Modifier.clip(RoundedCornerShape(10.dp))
                                .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh)
                                .clickable(enabled = !state.isProfileSwitching && !state.isShareSending) { onSelectProfile(profile) }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                        )
                    }
                }
                }

                Text(uiText(R.string.ui_1265, "想跟我说什么？（可选）"), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                BasicTextField(
                    value = payload.instruction,
                    onValueChange = onInstructionChange,
                    enabled = !state.isShareSending,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .padding(horizontal = 11.dp, vertical = 10.dp),
                    decorationBox = { inner ->
                        if (payload.instruction.isBlank()) {
                            Text(uiText(R.string.ui_1266, "例如：这个帮我记一下，下次做内容参考"), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        inner()
                    },
                )

                if (payload.sharedText.isNotBlank()) {
                    Text(
                        payload.sharedText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(11.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                            .padding(10.dp),
                    )
                }
                if (payload.attachments.isNotEmpty()) {
                    Text(
                        payload.attachments.joinToString(" · ") { it.name },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Text(uiText(R.string.ui_1267, "发送到：") + when(selectedSessionId) {
                    DailyConversation.SHARE_TARGET -> uiText(R.string.ui_0045, "日常助理")
                    null -> uiText(R.string.ui_1268, "新建对话")
                    else -> state.sessions.firstOrNull { it.id == selectedSessionId }?.title.orEmpty()
                }, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = { chooseConversation = !chooseConversation }, enabled = !state.isShareSending && !state.isProfileSwitching) {
                    Text(if (chooseConversation) uiText(R.string.ui_1269, "收起选择") else uiText(R.string.ui_1270, "换个对话"))
                }
                if (chooseConversation) {
                ShareSessionRow(uiText(R.string.ui_0045, "日常助理"), selectedSessionId == DailyConversation.SHARE_TARGET, !state.isShareSending) { selectedSessionId = DailyConversation.SHARE_TARGET }
                ShareSessionRow(uiText(R.string.ui_1268, "新建对话"), selectedSessionId == null, !state.isShareSending) { selectedSessionId = null }
                state.sessions.filter { it.profile == state.activeProfile && !it.source.equals("cron", true) && !DailyConversation.isDailyTitle(it.title) }.take(5).forEach { session ->
                    ShareSessionRow(
                        ellipsizeSessionTitle(session.title),
                        selectedSessionId == session.id,
                        !state.isShareSending && !state.isProfileSwitching,
                    ) { selectedSessionId = session.id }
                }
                }
                if (state.isProfileSwitching) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        CircularProgressIndicator(modifier = Modifier.padding(8.dp), strokeWidth = 2.dp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer), 
                enabled = canSend && !state.isShareSending && !state.isProfileSwitching,
                onClick = { onSend(selectedSessionId) },
            ) {
                if (state.isShareSending) CircularProgressIndicator(strokeWidth = 2.dp)
                else Text(uiText(R.string.ui_0701, "发送"))
            }
        },
        dismissButton = { TextButton(colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer), enabled = !state.isShareSending, onClick = onDismiss) { Text(uiText(R.string.ui_0553, "取消")) } },
    )
}

@Composable
private fun ShareSessionRow(title: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick, enabled = enabled)
        Text(
            title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

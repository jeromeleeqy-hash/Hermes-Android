package com.qingyu.hermescompanion.ui.screen

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
        mutableStateOf<String?>(null)
    }
    val canSend = payload.sharedText.isNotBlank() || payload.attachments.isNotEmpty()
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                HermesMulticolorIcon(HermesIconKind.SEND, contentDescription = null, iconSize = 22.dp)
                Text("分享到 Hermes", modifier = Modifier.padding(start = 9.dp), color = MaterialTheme.colorScheme.onSurface)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("发送到 Profile", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

                Text("补充要求（可选）", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                BasicTextField(
                    value = payload.instruction,
                    onValueChange = onInstructionChange,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .padding(horizontal = 11.dp, vertical = 10.dp),
                    decorationBox = { inner ->
                        if (payload.instruction.isBlank()) {
                            Text("例如：总结重点并给出行动建议", color = MaterialTheme.colorScheme.onSurfaceVariant)
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

                Text("选择会话", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                ShareSessionRow("新建对话", selectedSessionId == null) { selectedSessionId = null }
                state.sessions.filterNot { it.source.equals("cron", true) }.take(5).forEach { session ->
                    ShareSessionRow(
                        ellipsizeSessionTitle(session.title),
                        selectedSessionId == session.id,
                    ) { selectedSessionId = session.id }
                }
                if (state.isProfileSwitching) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        CircularProgressIndicator(modifier = Modifier.padding(8.dp), strokeWidth = 2.dp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = canSend && !state.isShareSending && !state.isProfileSwitching,
                onClick = { onSend(selectedSessionId) },
            ) {
                if (state.isShareSending) CircularProgressIndicator(strokeWidth = 2.dp)
                else Text("发送")
            }
        },
        dismissButton = { TextButton(enabled = !state.isShareSending, onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun ShareSessionRow(title: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
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

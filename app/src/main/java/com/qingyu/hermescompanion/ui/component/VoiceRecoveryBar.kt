package com.qingyu.hermescompanion.ui.component

import com.qingyu.hermescompanion.i18n.uiText
import com.qingyu.hermescompanion.R


import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.qingyu.hermescompanion.model.VoiceCaptureState
import com.qingyu.hermescompanion.model.VoicePhase

data class VoiceRecoveryActions(val state: VoiceCaptureState, val retry: () -> Unit, val discard: () -> Unit)
val LocalVoiceRecovery = staticCompositionLocalOf<VoiceRecoveryActions?> { null }

@Composable fun VoiceRecoveryBar() {
    val actions = LocalVoiceRecovery.current ?: return
    if (!actions.state.canRetry || actions.state.phase == VoicePhase.TRANSCRIBING) return
    var discard by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(uiText(R.string.ui_0467, "上次录音已保留"), Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
        TextButton(onClick = actions.retry) { Text(uiText(R.string.ui_0468, "重新识别")) }
        TextButton(onClick = { discard = true }) { Text(uiText(R.string.ui_0469, "删除")) }
    }
    if (discard) AlertDialog(onDismissRequest = { discard = false }, title = { Text(uiText(R.string.ui_0470, "删除这份录音？")) },
        text = { Text(uiText(R.string.ui_0471, "录音删除后无法恢复，已识别的文字不受影响。")) },
        confirmButton = { TextButton(onClick = { discard = false; actions.discard() }) { Text(uiText(R.string.ui_0472, "删除录音")) } },
        dismissButton = { TextButton(onClick = { discard = false }) { Text(uiText(R.string.ui_0473, "保留")) } })
}

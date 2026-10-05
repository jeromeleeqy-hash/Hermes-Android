package com.qingyu.hermescompanion.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.qingyu.hermescompanion.model.AgentRequest
import com.qingyu.hermescompanion.today.*

@Composable
internal fun TodayOperationDialog(action: TodayActionState, title: String, requests: List<AgentRequest>,
    onDismiss: () -> Unit, onCheck: () -> Unit, onRecover: () -> Unit, onConversation: () -> Unit,
    onRespond: (AgentRequest, String) -> Unit) {
    TodayDetailWindow(onDismiss, todayText("处理进展", "Processing progress"), compact = true) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp).testTag("today-operation-details"),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(todayActionLabel(action), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            if (action.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            Text(action.message.ifBlank { todayText("先核对服务器记录，再决定是否需要继续", "Check server records before continuing") },
                modifier = Modifier.testTag("today-operation-message"), style = MaterialTheme.typography.bodyLarge)
            requests.forEach { DecisionCard(it, onRespond) }
            OutlinedButton(onClick = onCheck, enabled = action.status != "checking", modifier = Modifier.fillMaxWidth()) {
                Text(todayText("重新核对结果", "Check result again"))
            }
            if (action.status in setOf("uncertain", "failed")) Button(onClick = onRecover,
                modifier = Modifier.fillMaxWidth().testTag("operation-continue")) {
                Text(todayText("继续未完成部分", "Continue unfinished work"))
            }
            if (action.sessionId.isNotBlank()) TextButton(onClick = onConversation, modifier = Modifier.fillMaxWidth()) {
                Text(todayText("查看原处理对话", "Open processing conversation"))
            }
        }
    }
}

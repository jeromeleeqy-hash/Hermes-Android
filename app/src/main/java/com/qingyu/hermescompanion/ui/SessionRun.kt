package com.qingyu.hermescompanion.ui

import com.qingyu.hermescompanion.i18n.uiText
import com.qingyu.hermescompanion.R


import com.qingyu.hermescompanion.data.StreamController
import com.qingyu.hermescompanion.model.*
import kotlinx.coroutines.Job

/** Owned by one stored conversation and one turn. Mutated only on the main thread. */
internal class SessionRun(
    var session: HermesSession,
    val submittedPrompt: String,
    val originalPrompt: String,
    val submittedAttachments: List<PendingAttachment> = emptyList(),
    val userMessageId: String? = null,
    val baselineSignature: String = "",
    val councilMode: CouncilMode = CouncilMode.OFF,
    val startedAtMillis: Long = System.currentTimeMillis(),
) {
    var controller: StreamController? = null
    var streamJob: Job? = null
    var watchdogJob: Job? = null
    var recoveryJob: Job? = null
    var deltaFlushJob: Job? = null
    val deltaBuffer = StringBuilder()
    var messages: List<ChatMessage> = emptyList()
    var tools: List<ToolActivity> = emptyList()
    var artifacts: List<ChatArtifact> = emptyList()
    var todos: List<ChatTodo> = emptyList()
    var queued: QueuedRunMessage? = null
    var stage = uiText(R.string.ui_0435, "正在连接 Hermes")
    var lastActivityAtMillis = startedAtMillis
    var recovering = false
    var isSteering = false

    fun touch(value: String = stage) {
        stage = value
        lastActivityAtMillis = System.currentTimeMillis()
    }

    fun snapshot() = ActiveRunSnapshot(session.profile, session.id, session.title, submittedPrompt,
        baselineSignature, startedAtMillis, session.workspacePath)
}

data class RunUiState(
    val session: HermesSession,
    val stage: String,
    val startedAtMillis: Long,
    val recovering: Boolean,
)

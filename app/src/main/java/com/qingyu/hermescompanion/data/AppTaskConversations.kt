package com.qingyu.hermescompanion.data

import com.qingyu.hermescompanion.model.*
import org.json.JSONObject

internal val appTaskModes = setOf("refresh_overview_only", "repair_format_only", "compact_existing_only",
    "apply_card_action", "configure_card_schedule", "improve_overview_and_schedule", "migrate_today_storage")

fun isTaskConversation(session: HermesSession, taskKeys: Set<String>): Boolean =
    session.source.equals("cron", true) || session.scopedId in taskKeys

/** Exact App-owned attachment envelope, never a title/keyword guess about an ordinary chat. */
internal fun isLegacyAppTask(session: HermesSession, messages: List<ChatMessage>): Boolean {
    val text = messages.firstOrNull { it.role == MessageRole.USER }?.content ?: return false
    val header = Regex("\\n\\n<!-- hermes-mobile-context-v1:(\\d+) -->\\n")
    return header.findAll(text).take(12).any { match ->
        val length = match.groupValues[1].toIntOrNull() ?: return@any false
        val start = match.range.last + 1
        val end = start + length
        if (length !in 1..1_000_000 || end > text.length || !text.startsWith("\n<!-- /hermes-mobile-context-v1 -->", end)) return@any false
        val context = text.substring(start, end)
        val prefix = "附件：hermes-today-request.json\n"
        if (!context.startsWith(prefix)) return@any false
        runCatching {
            val request = JSONObject(context.removePrefix(prefix))
            request.optString("mode") in appTaskModes && request.optString("profile") == session.profile &&
                isAbsoluteRemotePath(request.optString("workspace")) &&
                (session.workspacePath.isBlank() || remotePathsEqual(session.workspacePath, request.optString("workspace")))
        }.getOrDefault(false)
    }
}

internal fun agentRequestKey(request: AgentRequest): String =
    "${request.profile}\n${request.conversationId.ifBlank { request.runtimeSessionId }}\n${request.requestId}"

internal fun sameAgentRequestContent(first: AgentRequest, second: AgentRequest): Boolean =
    agentRequestKey(first) == agentRequestKey(second) && first.type == second.type &&
        first.serverRequestId == second.serverRequestId && first.questionId == second.questionId &&
        first.title == second.title && first.detail == second.detail && first.choices == second.choices &&
        first.allowMultiple == second.allowMultiple && first.allowSession == second.allowSession && first.allowPermanent == second.allowPermanent

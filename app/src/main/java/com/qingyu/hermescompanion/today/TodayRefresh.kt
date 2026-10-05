package com.qingyu.hermescompanion.today

import com.qingyu.hermescompanion.data.remotePathsEqual
import com.qingyu.hermescompanion.model.*
import org.json.JSONArray
import org.json.JSONObject

/** The receipt belongs to this refresh, never merely to a newer file timestamp. */
data class TodayRefreshState(
    val requestId: String = "", val profile: String = "", val root: String = "",
    val sessionId: String = "", val busy: Boolean = false, val message: String = "",
    val incompleteSources: Boolean = false,
)

fun confirmedTodayRefresh(raw: String?, requestId: String): Boolean = runCatching {
    if (requestId.isBlank()) return false
    val receipts = JSONObject(raw ?: return false).optJSONArray("refresh_receipts") ?: return false
    (0 until receipts.length()).any { receipts.optJSONObject(it)?.let { value ->
        value.optString("request_id") == requestId && value.optString("status") == "applied"
    } == true }
}.getOrDefault(false)

internal fun overviewConversationCandidates(sessions: List<HermesSession>, profile: String, root: String,
    linked: Set<String>, excluded: Set<String>): List<HermesSession> = sessions.filter {
    it.profile == profile && it.id !in excluded &&
        (remotePathsEqual(it.workspacePath, root) || (it.workspacePath.isBlank() && it.id in linked))
}.distinctBy { it.id }.take(6)

internal fun overviewConversationEvidence(session: HermesSession, messages: List<ChatMessage>): JSONObject = JSONObject()
    .put("session_id", session.id).put("profile", session.profile).put("workspace", session.workspacePath)
    .put("title", session.title).put("updated_at", session.updatedAt)
    .put("messages", JSONArray(messages.filter { !it.isStreaming && it.role in setOf(MessageRole.USER, MessageRole.ASSISTANT) }
        .takeLast(16).map { JSONObject().put("role", it.role.name.lowercase()).put("message_id", it.id)
            .put("created_at", it.createdAt).put("text", it.content.take(6000)).put("truncated", it.content.length > 6000) }))

package com.qingyu.hermescompanion.today

import org.json.JSONArray
import org.json.JSONObject

data class TodayActionState(
    val operationId: String, val cardId: String, val profile: String, val root: String,
    val request: String, val sessionId: String = "", val status: String = "checking", val message: String = "",
) {
    val key get() = "$profile\n$root\n$cardId"
    val pending get() = status in setOf("checking", "running", "awaiting_input", "uncertain")
    val busy get() = status in setOf("checking", "running", "awaiting_input")
    val expectedCard get() = runCatching { JSONObject(request).getString("expected_card") }.getOrDefault("")
}

fun encodeTodayActions(values: Collection<TodayActionState>): String = JSONArray((values.filterNot { it.pending }.takeLast((80 - values.count { it.pending }).coerceAtLeast(0)) + values.filter { it.pending }).also { require(it.size <= 80) { "Too many unconfirmed operations; resolve pending submissions first" } }.map {
    JSONObject().put("operation_id", it.operationId).put("card_id", it.cardId).put("profile", it.profile)
        .put("root", it.root).put("request", it.request).put("session_id", it.sessionId)
        .put("status", it.status).put("message", it.message)
}).toString()

fun decodeTodayActions(raw: String): Map<String, TodayActionState> = runCatching {
    val rows = JSONArray(raw)
    require(rows.length() <= 80 && raw.length < 3_000_000)
    (0 until rows.length()).map { index -> rows.getJSONObject(index).let {
        TodayActionState(it.getString("operation_id"), it.getString("card_id"), it.getString("profile"),
            it.getString("root"), it.getString("request"), it.optString("session_id"),
            if (it.getString("status") in setOf("checking", "running", "awaiting_input")) "uncertain" else it.getString("status"),
            it.optString("message"))
    } }.associateBy { it.key }
}.getOrDefault(emptyMap())

/** An Agent's chat reply or a changed timestamp is not an operation receipt. */
fun confirmedTodayAction(raw: String?, action: TodayActionState): String? = runCatching {
    val rows = JSONObject(raw ?: return null).optJSONArray("action_receipts") ?: return null
    (0 until rows.length()).mapNotNull { rows.optJSONObject(it) }.firstOrNull {
        it.optString("operation_id") == action.operationId && it.optString("card_id") == action.cardId && it.optString("status") == "applied"
    }?.optString("message")?.take(200)?.ifBlank { todayText("服务器已确认这次更新", "The server confirmed this update") }
}.getOrNull()

/** Only an explicit server not-found response permits a reconciliation conversation. */
internal fun isMissingTodaySession(error: Throwable): Boolean = error is com.qingyu.hermescompanion.data.ApiException &&
    (error.statusCode == 404 || (error.statusCode in setOf(-32000, -32602) && error.message.orEmpty().trim().equals("Session not found", ignoreCase = true)))

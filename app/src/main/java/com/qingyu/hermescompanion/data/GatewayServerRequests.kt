package com.qingyu.hermescompanion.data

import com.qingyu.hermescompanion.model.*
import com.qingyu.hermescompanion.i18n.uiText
import com.qingyu.hermescompanion.R
import org.json.JSONObject

/** Contract v7: the JSON-RPC envelope id is distinct from the approval queue id. */
internal fun parseServerRequests(frame: JSONObject, session: HermesSession): List<AgentRequest> {
    val id = frame.getString("id")
    val params = frame.getJSONObject("params")
    require(id.isNotBlank() && params.getString("session_id") == session.runtimeId)
    fun request(type: AgentRequestType, title: String, key: String = id, qid: String = "", choices: List<AgentRequestChoice> = emptyList(), multiple: Boolean = false) =
        AgentRequest(key, session.runtimeId!!, session.id, type, title,
            detail = params.optString("description"), choices = choices, allowMultiple = multiple,
            allowSession = "session" in choices.map { it.value }, allowPermanent = "always" in choices.map { it.value },
            profile = session.profile, serverRequestId = id, questionId = qid)
    return when (frame.getString("method")) {
        "approval" -> {
            val choices = params.optJSONArray("choices")
            val allowed = if (choices != null) (0 until choices.length()).map { choices.getString(it) }
                else buildList {
                    add("once")
                    if (!params.optBoolean("smart_denied") && params.optBoolean("allow_session", true)) add("session")
                    if (!params.optBoolean("smart_denied") && params.optBoolean("allow_permanent", false)) add("always")
                    add("deny")
                }
            require(allowed.isNotEmpty() && allowed.all { it in setOf("once", "session", "always", "deny") })
            listOf(request(AgentRequestType.APPROVAL,
                params.optString("command").ifBlank { params.optString("tool_name").ifBlank { uiText(R.string.ui_0131, "需要确认操作") } },
                choices = allowed.distinct().map { AgentRequestChoice(it, it) }))
        }
        "clarify" -> {
            val questions = params.getJSONArray("questions")
            require(questions.length() in 1..5)
            val answered = params.optJSONObject("answers")
            val ids = mutableSetOf<String>()
            buildList {
                for (n in 0 until questions.length()) {
                    val q = questions.getJSONObject(n)
                    val qid = q.getString("qid")
                    require(qid.isNotBlank() && ids.add(qid))
                    if (answered?.has(qid) == true) continue
                    val values = q.optJSONArray("choices")
                    add(request(AgentRequestType.CLARIFICATION, q.getString("question"), "$id:$qid", qid,
                        choices = values?.let { (0 until it.length()).map { n -> AgentRequestChoice(it.getString(n)) } }.orEmpty(),
                        multiple = q.optBoolean("multi_select")))
                }
            }
        }
        else -> throw UnsupportedOperationException("Unsupported server request")
    }
}

internal fun approvalChoices(request: AgentRequest): List<AgentRequestChoice> {
    val allowed = if (request.choices.isNotEmpty()) request.choices.map { it.value } else buildList {
        add("once"); if (request.allowSession) add("session"); if (request.allowPermanent) add("always"); add("deny")
    }
    return allowed.mapNotNull { value ->
        val label = when (value) {
            "once" -> uiText(R.string.ui_0829, "仅本次允许")
            "session" -> uiText(R.string.ui_0830, "本次会话允许")
            "always" -> uiText(R.string.ui_0831, "始终允许")
            "deny" -> uiText(R.string.ui_0832, "拒绝")
            else -> return@mapNotNull null
        }
        AgentRequestChoice(label, value)
    }
}

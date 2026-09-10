package com.qingyu.hermescompanion.data

import com.qingyu.hermescompanion.i18n.uiText
import com.qingyu.hermescompanion.R


import java.io.File
import java.util.UUID
import org.json.JSONObject

data class VoiceDraft(val id: String = UUID.randomUUID().toString(), val server: String, val account: String,
    val profile: String, val session: String, val settingsTest: Boolean = false, val transcript: String = "") {
    fun encode(): String = JSONObject().put("id", id).put("server", server.trimEnd('/')).put("account", account)
        .put("profile", profile).put("session", session).put("settings", settingsTest).put("transcript", transcript).toString()
    fun matches(server: String, account: String, profile: String, session: String, settings: Boolean): Boolean =
        this.server.trimEnd('/') == server.trimEnd('/') && this.account == account && this.profile == profile && this.session == session && settingsTest == settings
}
class VoiceDraftStore(private val root: File) {
    fun file(draft: VoiceDraft): File { root.mkdirs(); return File(root, "${draft.id}.m4a") }
    @Synchronized fun save(draft: VoiceDraft) {
        root.mkdirs()
        val temporary = File(root, "${draft.id}.tmp")
        temporary.outputStream().use { it.write(draft.encode().toByteArray()); it.fd.sync() }
        check(temporary.renameTo(File(root, "${draft.id}.json"))) { uiText(R.string.ui_0139, "无法保留录音，请检查存储空间") }
    }
    @Synchronized fun all(): List<VoiceDraft> = root.listFiles().orEmpty().filter { it.extension == "json" }.mapNotNull { file ->
        runCatching {
            val j = JSONObject(file.readText()); val id = j.getString("id"); UUID.fromString(id)
            VoiceDraft(id, j.getString("server"), j.getString("account"), j.getString("profile"), j.getString("session"), j.optBoolean("settings"), j.optString("transcript"))
        }.getOrNull()
    }
    @Synchronized fun delete(draft: VoiceDraft) { file(draft).delete(); File(root, "${draft.id}.json").delete() }
}

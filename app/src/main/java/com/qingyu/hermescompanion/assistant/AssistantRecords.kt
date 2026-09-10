package com.qingyu.hermescompanion.assistant

import com.qingyu.hermescompanion.i18n.uiText
import com.qingyu.hermescompanion.R


import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.time.LocalDate
import java.util.UUID

/** Immutable revisions. The server workspace is authoritative; this is a disposable cache/outbox. */
data class AssistantScope(val server: String, val account: String, val profile: String, val root: String) {
    val key: String get() = sha256(listOf(server.trimEnd('/'), account, profile, com.qingyu.hermescompanion.data.remotePathKey(root)).joinToString("\u0000"))
    val directory: String get() = com.qingyu.hermescompanion.data.normalizeWorkspacePath(root)
    val prefix: String get() = "hermes-assistant-${sha256(profile).take(12)}-"
    fun path(record: AssistantRecord): String = com.qingyu.hermescompanion.data.joinServerPath(directory, "$prefix${record.revision}.md")
    init { require(com.qingyu.hermescompanion.data.isAbsoluteRemotePath(root) && directory != "/" &&
        !directory.matches(Regex("^[A-Za-z]:[/\\\\]$")) && root.replace('\\', '/').split('/').none { it == ".." }) { uiText(R.string.ui_0024, "请先选择一个明确的项目工作目录") } }
}

enum class RecordKind(private val labelProvider: () -> String) {
    MATERIAL({ uiText(R.string.ui_0025, "收下的资料") }), FOCUS({ uiText(R.string.ui_0026, "近期重点") }), PLAN({ uiText(R.string.ui_0027, "计划") }), REVIEW({ uiText(R.string.ui_0028, "复盘") }), FOLLOWUP({ uiText(R.string.ui_0029, "跟进") }), RESULT({ uiText(R.string.ui_0030, "交付成果") }), PREFERENCES({ uiText(R.string.ui_0031, "相处方式") });
    val label: String get() = labelProvider()
}

data class AssistantRecord(
    val revision: String = UUID.randomUUID().toString(),
    val entity: String = UUID.randomUUID().toString(),
    val parents: List<String> = emptyList(),
    val kind: RecordKind,
    val title: String,
    val content: String,
    val intent: String = "",
    val topic: String = "",
    val sourceSession: String = "",
    val sourceMessage: String = "",
    val files: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val confirmed: Boolean = false,
    val archived: Boolean = false,
    val dueAt: Long = 0,
    val zone: String = "",
    val cronId: String = "",
    val reminderAttempted: Boolean = false,
) {
    val filename: String get() = "hermes-assistant-$revision.md"
    fun metadata(): JSONObject = JSONObject().put("schema", 1).put("revision", revision).put("entity", entity)
        .put("parents", JSONArray(parents)).put("kind", kind.name).put("title", title).put("intent", intent)
        .put("topic", topic).put("source_session", sourceSession).put("source_message", sourceMessage)
        .put("files", JSONArray(files)).put("created_at", createdAt).put("confirmed", confirmed)
        .put("archived", archived).put("due_at", dueAt).put("zone", zone).put("cron_id", cronId)
        .put("reminder_attempted", reminderAttempted)
    fun encode(): String = "<!-- hermes-assistant-v1 " + metadata().toString().replace("-->", "--\\u003e") + " -->\n" + content
    fun next(content: String = this.content, confirmed: Boolean = this.confirmed, archived: Boolean = this.archived,
        parents: List<String> = listOf(revision)): AssistantRecord = copy(revision = UUID.randomUUID().toString(), parents = parents,
        content = content, confirmed = confirmed, archived = archived, createdAt = System.currentTimeMillis())
    companion object {
        fun decode(raw: String): AssistantRecord {
            val first = raw.substringBefore('\n')
            require(first.startsWith("<!-- hermes-assistant-v1 ") && first.endsWith(" -->")) { uiText(R.string.ui_0032, "资料格式不兼容") }
            val j = JSONObject(first.removePrefix("<!-- hermes-assistant-v1 ").removeSuffix(" -->"))
            require(j.getInt("schema") == 1)
            val revision = j.getString("revision"); UUID.fromString(revision)
            val entity = j.getString("entity"); require(entity.matches(Regex("[A-Za-z0-9_-]{1,100}")))
            fun strings(key: String) = j.optJSONArray(key)?.let { a -> (0 until a.length()).map { a.getString(it) } }.orEmpty()
            strings("parents").forEach { UUID.fromString(it) }
            return AssistantRecord(revision, entity, strings("parents"), RecordKind.valueOf(j.getString("kind")),
                j.getString("title"), raw.substringAfter('\n', ""), j.optString("intent"), j.optString("topic"),
                j.optString("source_session"), j.optString("source_message"), strings("files"), j.getLong("created_at"),
                j.optBoolean("confirmed"), j.optBoolean("archived"), j.optLong("due_at"), j.optString("zone"),
                j.optString("cron_id"), j.optBoolean("reminder_attempted"))
        }
    }
}

fun currentRevisions(records: List<AssistantRecord>): List<AssistantRecord> {
    // A revision can only supersede its own entity. Keep concurrent branches visible.
    val byId = records.associateBy { it.revision }
    val superseded = records.flatMap { child -> child.parents.filter { byId[it]?.entity == child.entity } }.toSet()
    return records.filter { it.revision !in superseded }.distinctBy { it.revision }.sortedByDescending { it.createdAt }
}
fun sha256(value: String): String = MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }

class AssistantLocalStore(private val base: File) {
    private fun directory(scope: AssistantScope, pending: Boolean): File = File(base, "${scope.key}/${if (pending) "outbox" else "cache"}").apply { mkdirs() }
    @Synchronized fun read(scope: AssistantScope, pending: Boolean = false): List<AssistantRecord> = directory(scope, pending).listFiles().orEmpty()
        .filter { it.extension == "md" }.mapNotNull { runCatching { AssistantRecord.decode(it.readText()) }.getOrNull() }
    @Synchronized fun write(scope: AssistantScope, record: AssistantRecord, pending: Boolean = false) {
        val dir = directory(scope, pending)
        val target = File(dir, record.filename)
        val temp = File(dir, record.filename + ".tmp")
        temp.outputStream().use { out -> out.write(record.encode().toByteArray()); out.fd.sync() }
        check(temp.renameTo(target)) { uiText(R.string.ui_0033, "无法保存本机草稿，请检查存储空间") }
    }
    @Synchronized fun acknowledge(scope: AssistantScope, record: AssistantRecord) {
        write(scope, record); File(directory(scope, true), record.filename).delete()
    }
    @Synchronized fun removePending(scope: AssistantScope, record: AssistantRecord) { File(directory(scope, true), record.filename).delete() }
    fun attachmentDirectory(scope: AssistantScope, revision: String): File = File(base, "${scope.key}/attachments/$revision").apply { mkdirs() }
    fun saveAttachment(scope: AssistantScope, revision: String, index: Int, data: String) {
        val file = File(attachmentDirectory(scope, revision), "$index.data")
        file.outputStream().use { out -> out.write(data.toByteArray()); out.fd.sync() }
    }
    fun readAttachment(scope: AssistantScope, revision: String, index: Int): String? = File(attachmentDirectory(scope, revision), "$index.data").takeIf { it.exists() }?.readText()
    fun clearAttachments(scope: AssistantScope, revision: String) { attachmentDirectory(scope, revision).deleteRecursively() }
}

fun planningEntity(kind: RecordKind, date: LocalDate = LocalDate.now()): String? = when (kind) {
    RecordKind.FOCUS -> "focus"
    RecordKind.PREFERENCES -> "preferences"
    RecordKind.PLAN -> "plan-$date"
    else -> null
}

/** A new proposal does not replace an accepted agreement until the user adopts it. */
fun acceptedRevisions(records: List<AssistantRecord>): List<AssistantRecord> {
    val byId = records.associateBy { it.revision }
    return currentRevisions(records).groupBy { it.entity }.values.mapNotNull { leaves ->
        if (leaves.size != 1 || leaves.single().archived) return@mapNotNull null
        var level = leaves
        val seen = mutableSetOf<String>()
        while (level.isNotEmpty()) {
            val accepted = level.filter { it.confirmed && !it.archived }
            if (accepted.isNotEmpty()) return@mapNotNull accepted.singleOrNull()
            level = level.flatMap { child -> child.parents.mapNotNull { byId[it]?.takeIf { p -> p.entity == child.entity && !p.archived } } }
                .filter { seen.add(it.revision) }.distinctBy { it.revision }
        }
        null
    }.sortedByDescending { it.createdAt }
}

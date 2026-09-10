package com.qingyu.hermescompanion.assistant

import com.qingyu.hermescompanion.i18n.uiText
import com.qingyu.hermescompanion.R


import com.qingyu.hermescompanion.data.ApiException
import com.qingyu.hermescompanion.data.HermesApiClient
import com.qingyu.hermescompanion.model.PendingAttachment
import java.util.Base64

class AssistantRepository(private val local: AssistantLocalStore) {
    fun cached(scope: AssistantScope): List<AssistantRecord> = local.read(scope)
    fun pending(scope: AssistantScope): List<AssistantRecord> = local.read(scope, true)
    fun enqueue(scope: AssistantScope, record: AssistantRecord, attachments: List<PendingAttachment> = emptyList()) {
        require(record.title.isNotBlank() && record.content.isNotBlank()) { uiText(R.string.ui_0034, "请填写标题和内容") }
        require(record.content.length <= 200_000) { uiText(R.string.ui_0035, "内容过长，请以附件形式收下") }
        // Write attachment bytes first: a crash must never leave an outbox record referring to missing bytes.
        attachments.forEachIndexed { i, attachment ->
            val data = attachment.dataUrl ?: attachment.textContent?.let { dataUrl(it) }
            if (data != null) local.saveAttachment(scope, record.revision, i, data)
        }
        local.write(scope, record, true)
    }
    fun sync(client: HermesApiClient, scope: AssistantScope): List<AssistantRecord> {
        val listing = client.listWorkspaceForProfile(scope.directory, scope.profile)
        val names = listing.entries.filter { !it.isDirectory && it.name.matches(Regex("${scope.prefix}[0-9a-f-]{36}\\.md")) }
        val cached = local.read(scope).associateBy { scope.path(it).substringAfterLast('/') }
        val live = names.map { entry ->
            cached[entry.name] ?: AssistantRecord.decode(client.readWorkspaceDocumentForProfile(entry.path, scope.profile).content)
                .also { require(scope.path(it).substringAfterLast('/') == entry.name); local.write(scope, it) }
        }
        // Return only server-listed records. A deleted server file must not remain authoritative through cache.
        return live
    }
    fun replacePending(scope: AssistantScope, previous: AssistantRecord, next: AssistantRecord) {
        require(pending(scope).any { it.revision == previous.revision }) { uiText(R.string.ui_0036, "这份草稿已同步，请刷新后再编辑") }
        next.files.forEachIndexed { index, path ->
            val originalIndex = previous.files.indexOf(path)
            if (originalIndex >= 0) local.readAttachment(scope, previous.revision, originalIndex)?.let { data ->
                local.saveAttachment(scope, next.revision, index, data)
            }
        }
        enqueue(scope, next)
        local.removePending(scope, previous)
        local.clearAttachments(scope, previous.revision)
    }
    fun flush(client: HermesApiClient, scope: AssistantScope, onlyRevision: String? = null): List<AssistantRecord> {
        val sent = mutableListOf<AssistantRecord>()
        var failure: Exception? = null
        for (record in pending(scope).filter { onlyRevision == null || it.revision == onlyRevision }.sortedBy { it.createdAt }) {
            try {
            record.files.forEachIndexed { index, path ->
                local.readAttachment(scope, record.revision, index)?.let { data ->
                    uploadVerified(client, scope, path, data)
                }
            }
            uploadVerified(client, scope, scope.path(record), dataUrl(record.encode()))
            local.acknowledge(scope, record)
            local.clearAttachments(scope, record.revision)
            sent += record
            } catch (e: Exception) { if (failure == null) failure = e }
        }
        failure?.let { throw it }
        return sent
    }
    private fun uploadVerified(client: HermesApiClient, scope: AssistantScope, path: String, data: String) {
        val expected = Base64.getDecoder().decode(data.substringAfter("base64,"))
        try { client.uploadAssistantFile(path, data, scope.profile) }
        catch (error: Exception) {
            // Covers a conflict and a lost POST response without issuing a duplicate write.
            val actual = runCatching { client.readWorkspaceDocumentForProfile(path, scope.profile).bytes }.getOrNull()
            if (actual?.contentEquals(expected) != true) throw error
            return
        }
        val actual = client.readWorkspaceDocumentForProfile(path, scope.profile).bytes
        require(actual.contentEquals(expected)) { uiText(R.string.ui_0037, "服务器回读内容不一致，草稿仍保留在本机") }
    }
    fun armReminder(client: HermesApiClient, scope: AssistantScope, entity: String): AssistantRecord {
        val records = sync(client, scope) + pending(scope)
        val versions = currentRevisions(records).filter { it.entity == entity }
        require(versions.size == 1) { uiText(R.string.ui_0038, "跟进内容有并发版本，请先合并") }
        val record = versions.single()
        require(record.kind == RecordKind.FOLLOWUP && !record.archived) { uiText(R.string.ui_0039, "这份内容不再是待跟进事项") }
        val marker = "[hermes-assistant:${record.entity}]"
        val existing = client.listCronJobs(scope.profile).filter { marker in it.prompt }
        require(existing.size <= 1) { uiText(R.string.ui_0040, "发现多个同源提醒，请在任务页核对") }
        var base = record
        val job = existing.singleOrNull() ?: run {
            require(!record.reminderAttempted) { uiText(R.string.ui_0041, "上次创建结果仍未确认，目前未查到提醒。请到任务页核对；为避免重复，本次不会再次创建。") }
            require(record.dueAt > System.currentTimeMillis()) { uiText(R.string.ui_0042, "跟进时间已过，请重新约定时间") }
            base = record.next().copy(reminderAttempted = true)
            enqueue(scope, base); flush(client, scope, base.revision)
            client.createCronJob(record.title,
                uiText(R.string.ui_0043, "%1\$s\n到达用户约定的跟进时间。请根据以下事项准备简短摘要，区分已知结果和待核实事项，不要擅自联系团队。\n%2\$s\n资料：%3\$s\n原对话：%4\$s", marker, record.content, scope.path(record), record.sourceSession.ifBlank { uiText(R.string.ui_0044, "无") }),
                java.time.Instant.ofEpochMilli(record.dueAt).toString(), scope.profile)
        }
        val result = base.next(confirmed = true).copy(cronId = job.id, reminderAttempted = true)
        enqueue(scope, result); flush(client, scope, result.revision)
        return result
    }
    companion object {
        fun dataUrl(value: String): String = "data:text/markdown;charset=utf-8;base64," + Base64.getEncoder().encodeToString(value.toByteArray())
        fun attachmentPath(scope: AssistantScope, revision: String, index: Int, name: String): String {
            val clean = name.substringAfterLast('/').substringAfterLast('\\').replace(Regex("[^\\p{L}\\p{N}._-]"), "_").takeLast(90).ifBlank { "attachment" }
            return "${scope.directory}/hermes-material-$revision-$index-$clean"
        }
    }
}

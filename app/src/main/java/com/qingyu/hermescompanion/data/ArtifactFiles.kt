package com.qingyu.hermescompanion.data

import com.qingyu.hermescompanion.model.ChatMessage
import com.qingyu.hermescompanion.model.HermesSession
import com.qingyu.hermescompanion.model.RecentArtifact
import com.qingyu.hermescompanion.model.WorkspaceDocument
import java.net.URLDecoder

/** Decode link syntax once, without turning literal '+' characters into spaces. */
internal fun normalizeArtifactTarget(raw: String, markdownLink: Boolean = false): String {
    var value = raw.trim().trim('`', '"', '\'', '<', '>').trim()
    if (value.startsWith("MEDIA:", true)) value = value.substring(6).trim().trim('`', '"', '\'')
    val fileUri = value.startsWith("file://", true)
    val sandboxUri = value.startsWith("sandbox:", true)
    if (fileUri) value = value.substring(7).removePrefix("localhost")
    if (sandboxUri) value = value.substring(8)
    if (markdownLink || fileUri || sandboxUri) {
        value = value.substringBefore('#').substringBefore('?')
        value = runCatching { URLDecoder.decode(value.replace("+", "%2B"), "UTF-8") }.getOrDefault(value)
    }
    return value
}

internal fun resolveRemoteArtifactPath(path: String, workspace: String): String? {
    val clean = normalizeArtifactTarget(path)
    if (clean.isBlank()) return null
    if (clean.startsWith('/') || clean.startsWith("~/") || clean.startsWith("\\\\") ||
        Regex("^[A-Za-z]:[\\\\/]").containsMatchIn(clean)) return clean
    // Public URLs are links, never remote filesystem paths.
    if (Regex("^[A-Za-z][A-Za-z0-9+.-]*:").containsMatchIn(clean)) return null
    val root = workspace.trim()
    if (root.isBlank()) return null
    val separator = if ('\\' in root && '/' !in root) '\\' else '/'
    return root.trimEnd('/', '\\') + separator + clean.removePrefix("./").removePrefix(".\\")
}

/** Recover a stale index only from the original conversation, never another project. */
internal class ArtifactFileReader(private val client: HermesApiClient) {
    fun read(item: RecentArtifact, sourceSession: HermesSession?, cachedMessages: List<ChatMessage>): WorkspaceDocument {
        require(sourceSession == null || sourceSession.profile == item.profile) { "文件与来源档案不一致" }
        var workspace = item.workspacePath.ifBlank { sourceSession?.workspacePath.orEmpty() }
        val tried = linkedSetOf<String>()
        fun readPath(raw: String, base: String = workspace): WorkspaceDocument? {
            var path = resolveRemoteArtifactPath(raw, base)
            if (path == null && base.isBlank() && !raw.contains("://")) {
                workspace = client.initialWorkspaceForProfile(item.profile).path
                path = resolveRemoteArtifactPath(raw, workspace)
            }
            if (path == null || !tried.add(path)) return null
            return try {
                client.readWorkspaceDocumentForProfile(path, item.profile)
            } catch (error: ApiException) {
                if (error.statusCode != 404) throw error
                null
            }
        }
        readPath(item.sourcePath.ifBlank { item.path })?.let { return it }
        if (item.sourcePath.isNotBlank()) readPath(item.path)?.let { return it }

        fun recover(messages: List<ChatMessage>): WorkspaceDocument? {
            val source = if (item.messageId.isBlank()) messages else messages.filter { it.id == item.messageId }
            val candidates = source.flatMap { ChatInsightParser.artifactsFromText(it.content) }
                .filter { it.name == normalizeArtifactTarget(item.name, markdownLink = true) || truncatedExtensionMatch(item.name, it.name) }
                .map { it.path }.distinct()
            // Two same-name files are ambiguous; leave the choice to the user.
            if (candidates.size != 1) return null
            val raw = candidates.single()
            readPath(raw)?.let { return it }
            val currentSourceRoot = sourceSession?.workspacePath.orEmpty()
            // Only after the recorded location is missing, consider the same source
            // conversation's updated directory and its exact relative link.
            if (currentSourceRoot.isNotBlank() && currentSourceRoot != workspace && resolveRemoteArtifactPath(raw, "") == null) {
                return readPath(raw, currentSourceRoot)
            }
            return null
        }
        recover(cachedMessages)?.let { return it }
        if (item.sessionId.isNotBlank()) {
            val messages = try {
                client.loadArtifactSourceMessages(item.sessionId, item.profile, item.messageId)
            } catch (error: ApiException) {
                if (error.statusCode != 404) throw error
                emptyList()
            }
            recover(messages)?.let { return it }
        }
        throw ApiException(404, "找不到「${item.name}」。文件可能已移动或删除，请在项目文件中重新选择，或回到来源对话确认位置。")
    }

    private fun truncatedExtensionMatch(old: String, current: String): Boolean =
        (old.endsWith(".doc", true) || old.endsWith(".xls", true) || old.endsWith(".ppt", true)) && current == old + "x" ||
            old.endsWith(".htm", true) && current == old + "l"
}

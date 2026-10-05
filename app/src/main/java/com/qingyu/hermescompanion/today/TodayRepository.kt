package com.qingyu.hermescompanion.today

import com.qingyu.hermescompanion.data.*
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction

class TodayRepository(private val client: HermesApiClient, private val clock: () -> Long = System::currentTimeMillis) {
    /** Use the same configured workspace and strict byte decoding as normal overview reads. */
    fun verifySubmission(profile: String, root: String, card: TodayCard): String {
        val fresh = load(profile)
        require(fresh.error == null && fresh.rootVerified && remotePathsEqual(fresh.root, root)) {
            fresh.error ?: todayText("工作区已变化，请重新同步", "Workspace changed; sync again")
        }
        val target = fresh.board?.cards?.firstOrNull { it.id == card.id }
        require(target != null && !target.isClosed && target.interactionFingerprint == card.interactionFingerprint) {
            todayText("这张卡已更新，请同步后重新选择；本次还未提交", "This card changed. Sync and review it; nothing was submitted")
        }
        return todayFingerprint(requireNotNull(fresh.rawJson))
    }

    fun load(profile: String, previous: TodayState? = null, forceRead: Boolean = true): TodayState {
        var result = TodayState(profile = profile, loaded = true)
        return try {
            val listing = client.initialWorkspaceForProfile(profile)
            val root = listing.path
            require(isAbsoluteRemotePath(root)) { "Invalid workspace root" }
            val library = listing.entries.filter { entry ->
                safeTodayPath(root, entry.path) != null && !entry.name.startsWith('.') &&
                    (entry.isDirectory || entry.name.endsWith(".md", true))
            }.map { TodayLibraryEntry(it.name, it.path, it.isDirectory) }
                .sortedWith(compareByDescending<TodayLibraryEntry> { it.isDirectory }.thenBy { it.name })
            result = result.copy(root = root, library = library, rootVerified = true)
            val legacy = listing.entries.firstOrNull { it.name == TodayBoard.FILE && !it.isDirectory }
            val directory = joinServerPath(root, TodayBoard.DIRECTORY)
            val modern = try {
                client.listWorkspaceForProfile(directory, profile).also {
                    require(remotePathsEqual(it.path, directory)) { "Unexpected overview directory" }
                }.entries.firstOrNull { it.name == TodayBoard.FILE }
            } catch (error: ApiException) {
                if (error.statusCode != 404) throw error
                null // Only a confirmed missing directory permits legacy fallback.
            }
            val entry = modern ?: legacy ?: return result
            val path = joinServerPath(root, if (modern != null) TodayBoard.PATH else TodayBoard.FILE)
            result = result.copy(fileExists = true, filePath = path)
            require(!entry.isDirectory && remotePathsEqual(entry.path, path)) { "Unexpected overview path" }
            require((entry.size ?: 0) <= TodayBoard.MAX_BYTES) { "Overview exceeds 1 MiB" }
            val now = clock()
            val modified = entry.modifiedAt.takeIf { it.isFinite() && it > 0 }
            if (!forceRead && previous != null && previous.profile == profile && previous.rootVerified &&
                remotePathsEqual(previous.root, root) && previous.filePath == path && !(modern != null && legacy != null) && previous.error == null && previous.board != null && previous.rawJson != null &&
                modified != null && entry.size != null && modified == previous.fileModifiedAt && entry.size == previous.fileSize &&
                previous.lastFullReadAt?.let { now - it in 0 until 300_000 } == true) {
                return result.copy(board = previous.board, rawJson = previous.rawJson, syncedAt = now,
                    fileSize = entry.size, fileModifiedAt = modified, lastFullReadAt = previous.lastFullReadAt)
            }
            val doc = client.readWorkspaceDocumentForProfile(path, profile)
            require(remotePathsEqual(doc.path, path)) { "Unexpected response path" }
            require(doc.bytes.size <= TodayBoard.MAX_BYTES) { "Overview exceeds 1 MiB" }
            val text = Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(doc.bytes)).toString()
            val board = if (previous?.profile == profile && remotePathsEqual(previous.root, root) && previous.rawJson == text && previous.board != null)
                previous.board else TodayBoard.decode(text, root)
            if (modern != null && legacy != null) {
                val oldPath = joinServerPath(root, TodayBoard.FILE)
                require(remotePathsEqual(legacy.path, oldPath)) { "Unexpected legacy overview path" }
                require((legacy.size ?: 0) <= TodayBoard.MAX_BYTES) { "Legacy overview exceeds 1 MiB" }
                val old = client.readWorkspaceDocumentForProfile(oldPath, profile)
                require(remotePathsEqual(old.path, oldPath) && old.bytes.contentEquals(doc.bytes)) {
                    todayText("新旧位置存在不同的概览，请先核对迁移冲突；两份文件均已保留", "The old and new locations contain different overviews. Resolve the migration conflict; both files are preserved")
                }
            }
            result.copy(board = board, rawJson = text, syncedAt = now, fileSize = entry.size, fileModifiedAt = modified, lastFullReadAt = now)
        } catch (error: Exception) {
            result.copy(error = error.message ?: "Unable to read overview")
        }
    }

    fun countEntries(state: TodayState): List<TodayLibraryEntry> = state.library.mapIndexed { index, entry ->
        if (!entry.isDirectory || index >= 48) entry else entry.copy(count = runCatching {
            require(safeTodayPath(state.root, entry.path) != null)
            val listing = client.listWorkspaceForProfile(entry.path, state.profile)
            require(remotePathsEqual(listing.path, entry.path))
            // Count direct children only; no content reads, recursive indexing or inferred facts.
            listing.entries.count { !it.name.startsWith('.') && safeTodayPath(entry.path, it.path) != null }
        }.getOrNull())
    }
}

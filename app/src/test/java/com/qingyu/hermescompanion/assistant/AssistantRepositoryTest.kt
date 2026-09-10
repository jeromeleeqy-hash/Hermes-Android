package com.qingyu.hermescompanion.assistant

import com.qingyu.hermescompanion.data.HermesApiClient
import com.qingyu.hermescompanion.model.ConnectionConfig
import com.qingyu.hermescompanion.model.PendingAttachment
import com.qingyu.hermescompanion.storage.SecureCookieJar
import okhttp3.mockwebserver.*
import org.json.JSONObject
import org.json.JSONArray
import org.junit.*
import org.junit.Assert.*
import org.mockito.Mockito.mock
import java.nio.file.Files
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap

class AssistantRepositoryTest {
    private val root = Files.createTempDirectory("assistant-repository").toFile()
    private val files = ConcurrentHashMap<String, String>()
    private val jobs = ConcurrentHashMap<String, JSONObject>()
    private lateinit var server: MockWebServer
    private lateinit var client: HermesApiClient
    private lateinit var target: AssistantScope
    private lateinit var repository: AssistantRepository
    private var offline = false
    private var lostUploadAck = false
    private var lostCronAck = false
    private var cronPosts = 0
    private var blockedUploadSuffix = ""
    private val requestedProfiles = mutableListOf<String?>()
    @Before fun setup() {
        server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                requestedProfiles.add(request.requestUrl!!.queryParameter("profile"))
                if (offline) return MockResponse().setResponseCode(503).setBody("{\"error\":\"offline\"}")
                val path = request.requestUrl!!.encodedPath
                if (path == "/api/files" && request.method == "GET") return MockResponse().setBody(JSONObject().put("path", "/work").put("entries", JSONArray(files.keys.map { key ->
                    JSONObject().put("name", key.substringAfterLast('/')).put("path", key).put("is_dir", false)
                })).toString())
                if (path == "/api/files/read") {
                    val data = files[request.requestUrl!!.queryParameter("path")] ?: return MockResponse().setResponseCode(404)
                    return MockResponse().setBody(JSONObject().put("data_url", data).put("mime_type", "text/markdown").toString())
                }
                if (path == "/api/files/upload") {
                    val body = JSONObject(request.body.readUtf8())
                    assertEquals("work", body.getString("profile")); assertFalse(body.getBoolean("overwrite"))
                    if (blockedUploadSuffix.isNotBlank() && body.getString("path").endsWith(blockedUploadSuffix)) return MockResponse().setResponseCode(413)
                    val previous = files.putIfAbsent(body.getString("path"), body.getString("data_url"))
                    if (previous != null) return MockResponse().setResponseCode(409)
                    if (lostUploadAck) return MockResponse().setResponseCode(502)
                    return MockResponse().setBody("{\"ok\":true}")
                }
                if (path == "/api/cron/jobs" && request.method == "GET") return MockResponse().setBody(JSONObject().put("jobs", JSONArray(jobs.values.toList())).toString())
                if (path == "/api/cron/jobs" && request.method == "POST") {
                    cronPosts++
                    val body = JSONObject(request.body.readUtf8())
                    assertEquals("work", body.getString("profile"))
                    body.put("id", "cron-$cronPosts"); jobs[body.getString("id")] = body
                    return if (lostCronAck) MockResponse().setResponseCode(503) else MockResponse().setBody(JSONObject().put("job", body).toString())
                }
                return MockResponse().setResponseCode(404)
            }
        }
        server.start()
        client = HermesApiClient(ConnectionConfig(server.url("/").toString(), "ceo"), mock(SecureCookieJar::class.java))
        client.setProfile("personal") // Every operation must retain its captured work profile.
        target = AssistantScope(server.url("/").toString(), "ceo", "work", "/work")
        repository = AssistantRepository(AssistantLocalStore(root))
    }
    @After fun close() { client.close(); server.shutdown(); root.deleteRecursively() }
    @Test fun offlineCollectionSurvivesRestartAndUploadsAttachmentBeforeRecord() {
        val original = AssistantRecord(kind = RecordKind.MATERIAL, title = "爆款样本", content = "原始链接", intent = "研究开头三秒")
        val bytes = byteArrayOf(1, 5, 7, 9)
        val data = "data:application/octet-stream;base64," + Base64.getEncoder().encodeToString(bytes)
        val record = original.copy(files = listOf(AssistantRepository.attachmentPath(target, original.revision, 0, "sample.bin")))
        repository.enqueue(target, record, listOf(PendingAttachment(name = "sample.bin", mimeType = "application/octet-stream", dataUrl = data)))
        offline = true
        assertThrows(Exception::class.java) { repository.flush(client, target) }
        repository = AssistantRepository(AssistantLocalStore(root))
        assertEquals(1, repository.pending(target).size)
        offline = false
        repository.flush(client, target)
        assertEquals(data, files[record.files.single()])
        assertEquals(listOf(record), repository.sync(client, target))
        assertTrue(repository.pending(target).isEmpty())
        assertTrue(requestedProfiles.all { it == "work" })
    }
    @Test fun oneFailedItemDoesNotBlockOtherRecordsAndPendingAttachmentCanBeRemoved() {
        val bad = AssistantRecord(kind = RecordKind.MATERIAL, title = "失败资料", content = "需重新整理", files = listOf("/work/blocked.bin"))
        val good = AssistantRecord(kind = RecordKind.RESULT, title = "正常成果", content = "已确认")
        blockedUploadSuffix = "blocked.bin"
        repository.enqueue(target, bad, listOf(PendingAttachment(name="blocked.bin", mimeType="application/octet-stream", dataUrl="data:application/octet-stream;base64,AQID")))
        repository.enqueue(target, good)
        assertThrows(Exception::class.java) { repository.flush(client, target) }
        assertTrue(files.containsKey(target.path(good)))
        assertEquals(listOf(bad), repository.pending(target))
        val fixed = bad.next(content = "修正后的内容").copy(files = emptyList())
        repository.replacePending(target, bad, fixed)
        assertEquals(listOf(fixed), repository.pending(target))
        repository.flush(client, target)
        assertTrue(repository.pending(target).isEmpty())
        assertTrue(files.containsKey(target.path(fixed)))
        assertEquals(2, repository.sync(client, target).size)
    }
    @Test fun lostUploadAcknowledgementIsResolvedByReadback() {
        lostUploadAck = true
        val record = AssistantRecord(kind = RecordKind.RESULT, title = "团队说明", content = "已确认的要求")
        repository.enqueue(target, record)
        repository.flush(client, target)
        assertEquals(1, files.size)
        assertTrue(repository.pending(target).isEmpty())
    }
    @Test fun differentExistingContentIsNeverOverwrittenOrAcknowledged() {
        val record = AssistantRecord(kind = RecordKind.MATERIAL, title = "资料", content = "新内容")
        files[target.path(record)] = AssistantRepository.dataUrl("different")
        repository.enqueue(target, record)
        assertThrows(Exception::class.java) { repository.flush(client, target) }
        assertEquals(1, repository.pending(target).size)
        assertEquals(AssistantRepository.dataUrl("different"), files[target.path(record)])
    }
    @Test fun sameWorkspaceDoesNotMixProfilesAndServerDeletionIsRespected() {
        val a = AssistantRecord(kind = RecordKind.PLAN, title = "工作", content = "工作计划")
        val b = AssistantRecord(kind = RecordKind.PLAN, title = "生活", content = "生活计划")
        files[target.path(a)] = AssistantRepository.dataUrl(a.encode())
        files[target.copy(profile = "personal").path(b)] = AssistantRepository.dataUrl(b.encode())
        assertEquals(listOf(a), repository.sync(client, target))
        files.remove(target.path(a))
        assertTrue(repository.sync(client, target).isEmpty())
    }
    @Test fun lostCronAcknowledgementReconcilesWithoutCreatingDuplicateEvenWithStaleUiRecord() {
        val record = AssistantRecord(kind = RecordKind.FOLLOWUP, title = "检查测试结果", content = "确认获客样本表现", dueAt = System.currentTimeMillis() + 86400000)
        repository.enqueue(target, record); repository.flush(client, target)
        lostCronAck = true
        assertThrows(Exception::class.java) { repository.armReminder(client, target, record.entity) }
        assertEquals(1, cronPosts)
        val acknowledged = repository.armReminder(client, target, record.entity)
        assertTrue(acknowledged.confirmed); assertEquals("cron-1", acknowledged.cronId)
        assertEquals(1, cronPosts)
    }
    @Test fun ambiguousAttemptNeverBlindlyPostsAgainWhenServerCannotFindIt() {
        val record = AssistantRecord(kind = RecordKind.FOLLOWUP, title = "跟进", content = "看结果", dueAt = System.currentTimeMillis() + 86400000, reminderAttempted = true)
        repository.enqueue(target, record); repository.flush(client, target)
        assertThrows(IllegalArgumentException::class.java) { repository.armReminder(client, target, record.entity) }
        assertEquals(0, cronPosts)
    }
}

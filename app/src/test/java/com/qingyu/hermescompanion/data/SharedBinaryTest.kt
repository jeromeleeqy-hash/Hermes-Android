package com.qingyu.hermescompanion.data

import com.qingyu.hermescompanion.model.*
import com.qingyu.hermescompanion.storage.SecureCookieJar
import okhttp3.mockwebserver.*
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import org.mockito.Mockito.mock
import java.util.Base64

class SharedBinaryTest {
    private lateinit var server: MockWebServer
    private lateinit var client: HermesApiClient
    private val bytes = byteArrayOf(0, 5, 12, -3, 88)
    private val file = PendingAttachment(id = "share-1", name = "chat-record.dat", mimeType = "application/octet-stream", dataUrl = "data:application/octet-stream;base64," + Base64.getEncoder().encodeToString(bytes))
    private val session = HermesSession("work-session", "Work", profile = "work", workspacePath = "/work")
    private var wrongRead = false
    private var exists = false
    private val requests = mutableListOf<String>()
    @Before fun setup() {
        server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                requests.add(request.path.orEmpty())
                assertEquals("work", request.requestUrl!!.queryParameter("profile"))
                if (request.requestUrl!!.encodedPath == "/api/files/upload") {
                    val body = JSONObject(request.body.readUtf8())
                    assertFalse(body.getBoolean("overwrite")); assertEquals("work", body.getString("profile"))
                    assertEquals("/work/hermes-share-share-1-chat-record.dat", body.getString("path"))
                    if (exists) return MockResponse().setResponseCode(409).setBody("{}")
                    exists = true; return MockResponse().setBody("{}")
                }
                return MockResponse().setBody(JSONObject().put("path", "/work/hermes-share-share-1-chat-record.dat")
                    .put("name", "chat-record.dat").put("mime_type", file.mimeType).put("data_url", if (wrongRead) "data:application/octet-stream;base64,QQ==" else file.dataUrl).toString())
            }
        }
        server.start(); client = HermesApiClient(ConnectionConfig(server.url("/").toString().trimEnd('/'), "test"), mock(SecureCookieJar::class.java))
        client.setProfile("personal")
    }
    @After fun close() { client.close(); server.shutdown() }
    @Test fun binaryBecomesVerifiedServerPathForTheSelectedConversation() {
        val result = client.prepareBinaryAttachments(session, listOf(file)).single()
        assertNull(result.dataUrl); assertEquals("/work/hermes-share-share-1-chat-record.dat", result.remotePath)
        assertEquals(2, requests.size)
    }
    @Test fun retryUsesTheSameImmutablePathAndReadsBackBeforeProceeding() {
        client.prepareBinaryAttachments(session, listOf(file))
        assertNotNull(client.prepareBinaryAttachments(session, listOf(file)).single().remotePath)
        assertEquals(4, requests.size)
    }
    @Test fun differentRemoteBytesPreventSending() {
        wrongRead = true
        assertThrows(IllegalArgumentException::class.java) { client.prepareBinaryAttachments(session, listOf(file)) }
    }
    @Test fun stopBeforeUploadDoesNotWrite() {
        assertThrows(IllegalStateException::class.java) { client.prepareBinaryAttachments(session, listOf(file)) { true } }
        assertTrue(requests.isEmpty())
    }
}

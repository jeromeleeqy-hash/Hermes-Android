package com.qingyu.hermescompanion.data

import com.qingyu.hermescompanion.model.ConnectionConfig
import com.qingyu.hermescompanion.storage.SecureCookieJar
import okhttp3.mockwebserver.*
import org.json.JSONArray
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import org.mockito.Mockito.mock

class DailyConversationApiTest {
    private lateinit var server: MockWebServer
    private lateinit var client: HermesApiClient
    @Before fun setup() {
        server = MockWebServer(); server.start()
        client = HermesApiClient(ConnectionConfig(server.url("/").toString(), "user"), mock(SecureCookieJar::class.java))
        client.setProfile("other")
    }
    @After fun close() { client.close(); server.shutdown() }
    private fun page() = JSONObject().put("sessions", JSONArray((0 until 60).map { JSONObject().put("id", "s$it").put("title", "旧话题 $it") })).toString()
    @Test fun searchesBeyondFirstPageAndKeepsExplicitProfile() {
        server.enqueue(MockResponse().setBody(page()))
        server.enqueue(MockResponse().setBody("""{"sessions":[{"id":"daily","title":"日常助理","cwd":"/my-work","message_count":8}]}"""))
        val found = client.findSessionByTitleForProfile("日常助理", "work")!!
        assertEquals("daily", found.id); assertEquals("work", found.profile); assertEquals("/my-work", found.workspacePath)
        val first = server.takeRequest().requestUrl!!; val second = server.takeRequest().requestUrl!!
        assertEquals("work", first.queryParameter("profile")); assertEquals("work", second.queryParameter("profile"))
        assertEquals("60", second.queryParameter("offset"))
    }
    @Test fun paginationThatRepeatsCannotPretendNoConversationExists() {
        server.enqueue(MockResponse().setBody(page())); server.enqueue(MockResponse().setBody(page()))
        assertThrows(ApiException::class.java) { client.findSessionByTitleForProfile("日常助理", "work") }
        assertEquals(2, server.requestCount)
    }
    @Test fun malformedHistoryAndWrongSessionAreErrors() {
        server.enqueue(MockResponse().setBody("{}"))
        assertThrows(ApiException::class.java) { client.findSessionByTitleForProfile("日常助理", "work") }
        server.enqueue(MockResponse().setBody("""{"id":"someone-else","title":"日常助理"}"""))
        assertThrows(ApiException::class.java) { client.sessionForProfile("daily", "work") }
    }
    @Test fun metadataAndRenameUseCapturedProfileAndSkipArchivedSessions() {
        server.enqueue(MockResponse().setBody("""{"session":{"id":"daily","title":"日常助理","archived":true}}"""))
        assertNull(client.sessionForProfile("daily", "work"))
        server.enqueue(MockResponse().setBody("""{"title":"日常助理"}"""))
        assertEquals("日常助理", client.renameSessionForProfile("daily", "日常助理", "work"))
        assertEquals("work", server.takeRequest().requestUrl!!.queryParameter("profile"))
        val patch = server.takeRequest()
        assertEquals("PATCH", patch.method); assertEquals("work", patch.requestUrl!!.queryParameter("profile"))
    }
}

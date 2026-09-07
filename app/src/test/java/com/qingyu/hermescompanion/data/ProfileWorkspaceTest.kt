package com.qingyu.hermescompanion.data

import com.qingyu.hermescompanion.model.ConnectionConfig
import com.qingyu.hermescompanion.storage.SecureCookieJar
import okhttp3.mockwebserver.*
import org.junit.*
import org.junit.Assert.*
import org.mockito.Mockito.*
import java.util.concurrent.CopyOnWriteArrayList

class ProfileWorkspaceTest {
    private lateinit var server:MockWebServer
    private lateinit var client:HermesApiClient
    private val requests=CopyOnWriteArrayList<String>()
    private var failDirectory=false
    @Before fun setup() {
        server=MockWebServer()
        server.dispatcher=object:Dispatcher() {
            override fun dispatch(request:RecordedRequest):MockResponse {
                requests.add(request.path.orEmpty())
                val profile=request.requestUrl!!.queryParameter("profile")
                val path=request.requestUrl!!.queryParameter("path")
                if(request.requestUrl!!.encodedPath=="/api/sessions/archive") return MockResponse().setBody("""{"message_count":201}""")
                if(request.requestUrl!!.encodedPath=="/api/sessions/archive/messages") {
                    val id=if(request.requestUrl!!.queryParameter("offset")=="0") "old" else "new"
                    return MockResponse().setBody("""{"messages":[{"id":"$id","role":"assistant","content":"report"}]}""")
                }
                if(request.requestUrl!!.encodedPath=="/api/files/read") return MockResponse().setBody("""{"name":"empty.md","path":"/work/sales/empty.md","mime_type":"text/markdown","data_url":"data:text/markdown;base64,"}""")
                if(request.requestUrl!!.encodedPath=="/api/config") return MockResponse().setBody("""{"config":{"terminal":{"cwd":"/work/$profile"}}}""")
                if(request.requestUrl!!.encodedPath=="/api/files") {
                    if(failDirectory) return MockResponse().setResponseCode(403).setBody("""{"error":"denied"}""")
                    return MockResponse().setBody("""{"path":"${path ?: "/"}","entries":[]}""")
                }
                return MockResponse().setResponseCode(404)
            }
        }
        server.start()
        client=HermesApiClient(ConnectionConfig(server.url("/").toString().trimEnd('/'),"test"),mock(SecureCookieJar::class.java))
    }
    @After fun close() { client.close();server.shutdown() }
    @Test fun eachProfileUsesItsOwnConfiguredDirectory() {
        client.setProfile("sales")
        assertEquals("/work/sales",client.initialWorkspace().path)
        client.setProfile("personal")
        assertEquals("/work/personal",client.initialWorkspace().path)
        assertEquals(4,requests.size)
        assertTrue(requests.none { it=="/api/files" })
    }
    @Test fun failedConfiguredDirectoryNeverFallsBackToServerRoot() {
        failDirectory=true
        assertThrows(ApiException::class.java) { client.initialWorkspaceForProfile("sales") }
        assertEquals(2,requests.size)
        assertTrue(requests.last().contains("path="))
    }
    @Test fun explicitProfileSurvivesClientProfileChange() {
        client.setProfile("personal")
        assertEquals("/work/sales",client.initialWorkspaceForProfile("sales").path)
        assertTrue(requests.all { it.contains("profile=sales") })
    }
    @Test fun emptyFileReadPreservesBytesAndExplicitProfile() {
        client.setProfile("personal")
        val document=client.readWorkspaceDocumentForProfile("/work/sales/empty.md","sales")
        assertEquals("",document.content);assertTrue(document.bytes.isEmpty())
        assertTrue(requests.single().contains("profile=sales"))
    }

    @Test fun artifactRecoveryCanFindOlderSourceMessageWithinSameProfile() {
        client.setProfile("personal")
        assertEquals("old",client.loadArtifactSourceMessages("archive","sales","old").single().id)
        assertEquals(3,requests.size)
        assertTrue(requests.all { it.contains("profile=sales") })
    }

}

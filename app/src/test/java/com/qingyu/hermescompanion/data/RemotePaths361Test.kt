package com.qingyu.hermescompanion.data

import com.qingyu.hermescompanion.model.*
import com.qingyu.hermescompanion.storage.SecureCookieJar
import okhttp3.*
import okhttp3.mockwebserver.*
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import org.mockito.Mockito.mock
import java.util.concurrent.CopyOnWriteArrayList

class RemotePaths361Test {
    @Test fun acceptsPosixDriveRootsUnicodeAndUncWithoutGuessingRelativePaths() {
        listOf("/", "/home/用户/work", "C:\\", "C:\\Users\\Name\\My work", "d:/Hermes/work", "\\\\server\\share\\work",
            "\\\\?\\C:\\long\\path", "\\\\?\\UNC\\server\\share\\work", "\"D:\\My project\"").forEach { assertTrue(it, isAbsoluteRemotePath(it)) }
        listOf("", "workspace", "./work", "../work", "C:work", "C:", "\\work", "\\\\server", "\\\\.\\COM1", "C:\\bad\npath").forEach { assertFalse(it, isAbsoluteRemotePath(it)) }
        assertEquals("C:\\", normalizeWorkspacePath(" C:\\ "))
        assertEquals("/", normalizeWorkspacePath(" / "))
        assertEquals("C:\\My work", normalizeWorkspacePath("\"C:\\My work\\\""))
        assertEquals("\\\\?\\C:\\", normalizeWorkspacePath("\\\\?\\C:\\"))
    }
    @Test fun projectContainmentKeepsVolumeAndSegmentBoundaries() {
        assertTrue(isRemotePathWithin("C:\\Work", "c:/work/Docs/a.md"))
        assertTrue(isRemotePathWithin("C:\\", "C:\\Work\\a.md"))
        assertTrue(isRemotePathWithin("\\\\server\\share", "//SERVER/share/folder/a.md"))
        assertFalse(isRemotePathWithin("C:\\Work", "D:\\Work\\a.md"))
        assertFalse(isRemotePathWithin("C:\\Work", "C:\\Workspace\\a.md"))
        assertFalse(isRemotePathWithin("C:\\Work", "C:\\Work\\..\\private.md"))
        assertFalse(isRemotePathWithin("/Work", "/work/a.md"))
        assertFalse(isRemotePathWithin("/", "C:\\a.md"))
        assertEquals("C:\\Work\\a.png", joinServerPath("C:\\Work", "a.png"))
        assertEquals("C:\\", remoteParentPath("C:\\a.md"))
        assertEquals("\\\\server\\share", remoteParentPath("\\\\server\\share\\a.md"))
    }
    @Test fun windowsFileLinksAndRelativeImagesUseServerPathSyntax() {
        assertEquals("C:/Work/report.md", normalizeArtifactTarget("file:///C:/Work/report.md"))
        assertEquals("//server/share/report.md", normalizeArtifactTarget("file://server/share/report.md"))
        assertEquals("C:\\Work\\images\\a.png", resolveRemoteArtifactPath("images/a.png", "C:\\Work"))
    }
    @Test fun windowsCwdSurvivesJsonRpcHttpBrowsingAndProfileRouting() {
        val root = "D:\\Hermes\\我的项目"
        val calls = CopyOnWriteArrayList<JSONObject>()
        val paths = CopyOnWriteArrayList<Pair<String?,String?>>()
        val server = MockWebServer()
        server.dispatcher = object : okhttp3.mockwebserver.Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val url = request.requestUrl!!
                when (url.encodedPath) {
                    "/api/auth/ws-ticket" -> return MockResponse().setBody("{\"ticket\":\"test\"}")
                    "/api/config" -> return MockResponse().setBody(JSONObject().put("config",JSONObject().put("terminal",JSONObject().put("cwd",root))).toString())
                    "/api/files" -> {
                        paths += url.queryParameter("profile") to url.queryParameter("path")
                        return MockResponse().setBody(JSONObject().put("path",root).put("entries",org.json.JSONArray()).toString())
                    }
                }
                return MockResponse().withWebSocketUpgrade(object : WebSocketListener() {
                    override fun onOpen(ws: WebSocket, response: Response) {
                        ws.send("""{"method":"event","params":{"type":"gateway.ready","payload":{}}}""")
                    }
                    override fun onClosing(ws: WebSocket, code: Int, reason: String) { ws.close(code,reason) }
                    override fun onMessage(ws: WebSocket, text: String) {
                        val frame=JSONObject(text);calls+=frame
                        val result = when(frame.getString("method")) {
                            "projects.create" -> JSONObject().put("project",JSONObject().put("id","work").put("name","Work").put("primary_path",root))
                            else -> JSONObject().put("ok",true)
                        }
                        ws.send(JSONObject().put("jsonrpc","2.0").put("id",frame.get("id")).put("result",result).toString())
                    }
                })
            }
        }
        server.start()
        val client = HermesApiClient(ConnectionConfig(server.url("/").toString(),"test"),mock(SecureCookieJar::class.java))
        try {
            client.setProfile("windows")
            val project = client.createProject("Work", root)
            assertEquals(root, project.primaryPath)
            val session = HermesSession("s", "Title", profile="windows",runtimeId="runtime-s")
            assertEquals(root, client.moveSessionToProject(session,project).workspacePath)
            assertEquals(root, client.initialWorkspaceForProfile("windows").path)
            assertEquals(root,calls.first { it.getString("method")=="projects.create" }.getJSONObject("params").getString("primary_path"))
            val cwd = calls.first { it.getString("method")=="session.cwd.set" }.getJSONObject("params")
            assertEquals(root,cwd.getString("cwd"));assertEquals("windows",cwd.getString("profile"))
            assertEquals(listOf("windows" to root), paths)
        } finally { client.close();server.shutdown() }
    }
}

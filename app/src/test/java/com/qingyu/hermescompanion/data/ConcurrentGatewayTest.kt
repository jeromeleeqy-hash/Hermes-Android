package com.qingyu.hermescompanion.data

import com.qingyu.hermescompanion.model.*
import com.qingyu.hermescompanion.storage.SecureCookieJar
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okhttp3.Response
import okhttp3.mockwebserver.*
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import org.mockito.Mockito.*
import java.util.concurrent.*

/** Exercises real WebSocket JSON-RPC with interleaved runtimes, without a live Hermes server. */
class ConcurrentGatewayTest {
    private lateinit var server: MockWebServer
    private lateinit var client: HermesApiClient
    private lateinit var socket: WebSocket
    private val calls = CopyOnWriteArrayList<JSONObject>()
    private val submitted = LinkedBlockingQueue<String>()
    private val workers = Executors.newCachedThreadPool()
    private var denyDirectory = false
    private var reasoning: String? = null

    @Before fun setUp() {
        server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                if (request.path.orEmpty().startsWith("/api/auth/ws-ticket")) return MockResponse().setBody("{\"ticket\":\"test\"}")
                return MockResponse().withWebSocketUpgrade(object : WebSocketListener() {
                    override fun onOpen(webSocket: WebSocket, response: Response) {
                        socket = webSocket
                        event(null, "gateway.ready")
                    }
                    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                        webSocket.close(code, reason)
                    }
                    override fun onMessage(webSocket: WebSocket, text: String) {
                        val frame = JSONObject(text)
                        calls += frame
                        val params = frame.getJSONObject("params")
                        val method = frame.getString("method")
                        val id = params.optString("session_id")
                        val result = when (method) {
                            "session.create" -> JSONObject().put("session_id", "runtime-created").put("stored_session_id", "created")
                            "session.resume" -> JSONObject().put("session_id", "runtime-$id").put("info", JSONObject().apply { reasoning?.let { put("reasoning_effort", it) } })
                            "session.cwd.set" -> JSONObject().put("ok", !denyDirectory)
                            else -> JSONObject().put("ok", true)
                        }
                        webSocket.send(JSONObject().put("jsonrpc", "2.0").put("id", frame.get("id")).put("result", result).toString())
                        if (method == "prompt.submit") {
                            event(id, "message.start")
                            submitted.put(id)
                        }
                    }
                })
            }
        }
        server.start()
        val jar = mock(SecureCookieJar::class.java)
        client = HermesApiClient(ConnectionConfig(server.url("/").toString().trimEnd('/'), "test"), jar)
    }

    @After fun close() {
        if (this::client.isInitialized) client.close()
        workers.shutdownNow()
        server.shutdown()
    }

    private fun event(runtime: String?, type: String, text: String = "") {
        val params = JSONObject().put("type", type).put("payload", JSONObject().put("text", text))
        if (runtime != null) params.put("session_id", runtime)
        socket.send(JSONObject().put("method", "event").put("params", params).toString())
    }

    private fun start(id: String, controller: StreamController, events: MutableList<StreamEvent>): Future<*> = workers.submit {
        client.streamMessage(controller, HermesSession(id = id, title = id, workspacePath = "/projects/$id"), "prompt-$id", emptyList(), events::add)
    }

    @Test fun interleavedRepliesStayInTheirOwnConversation() {
        val a = CopyOnWriteArrayList<StreamEvent>(); val b = CopyOnWriteArrayList<StreamEvent>()
        val fa = start("a", StreamController(), a)
        val fb = start("b", StreamController(), b)
        assertEquals(setOf("runtime-a", "runtime-b"), setOf(submitted.poll(15, TimeUnit.SECONDS), submitted.poll(15, TimeUnit.SECONDS)))
        event("runtime-b", "message.delta", "B1")
        event("runtime-a", "message.delta", "A1")
        event(null, "message.delta", "ambiguous")
        event("unknown", "message.complete", "wrong")
        event("runtime-a", "message.complete", "A done")
        event("runtime-b", "message.delta", "B2")
        event("runtime-b", "message.complete", "B done")
        fa.get(10, TimeUnit.SECONDS); fb.get(10, TimeUnit.SECONDS)
        assertEquals(listOf("A1"), a.filterIsInstance<StreamEvent.AssistantDelta>().map { it.text })
        assertEquals(listOf("B1", "B2"), b.filterIsInstance<StreamEvent.AssistantDelta>().map { it.text })
        assertEquals(1, a.count { it == StreamEvent.Completed })
        assertEquals(1, b.count { it == StreamEvent.Completed })
        listOf("a", "b").forEach { id ->
            val ownCalls = calls.filter { it.getJSONObject("params").optString("session_id") == "runtime-$id" }
            assertEquals(listOf("session.cwd.set", "prompt.submit"), ownCalls.map { it.getString("method") })
            assertEquals("/projects/$id", ownCalls.first().getJSONObject("params").getString("cwd"))
        }
    }

    @Test fun stoppingOneRuntimeDoesNotStopItsPeer() {
        val ca = StreamController(); val cb = StreamController()
        val a = CopyOnWriteArrayList<StreamEvent>(); val b = CopyOnWriteArrayList<StreamEvent>()
        val fa = start("a", ca, a); val fb = start("b", cb, b)
        repeat(2) { assertNotNull(submitted.poll(15, TimeUnit.SECONDS)) }
        ca.stop(); client.stopRun("runtime-a")
        fa.get(10, TimeUnit.SECONDS)
        event("runtime-a", "message.delta", "late A")
        event("runtime-b", "message.delta", "still B")
        event("runtime-b", "message.complete", "done B")
        fb.get(10, TimeUnit.SECONDS)
        assertFalse(cb.isStopped())
        assertEquals(listOf("still B"), b.filterIsInstance<StreamEvent.AssistantDelta>().map { it.text })
        assertTrue(a.none { it is StreamEvent.AssistantDelta })
        assertEquals(setOf("runtime-a"), calls.filter { it.getString("method") == "session.interrupt" }.map { it.getJSONObject("params").getString("session_id") }.toSet())
    }

    @Test fun creatingInProjectSetsRuntimeDirectoryAndPreservesRoot() {
        val session = client.createSession("/")
        assertEquals("/", session.workspacePath)
        assertEquals("runtime-created", session.runtimeId)
        assertEquals(listOf("session.create", "session.cwd.set"), calls.map { it.getString("method") })
        assertEquals("/", calls.last().getJSONObject("params").getString("cwd"))
    }

    @Test fun invalidDirectoryFailsBeforeSubmittingAnyPrompt() {
        denyDirectory = true
        val future = start("a", StreamController(), CopyOnWriteArrayList())
        assertThrows(ExecutionException::class.java) { future.get(15, TimeUnit.SECONDS) }
        assertFalse(calls.any { it.getString("method") == "prompt.submit" })
    }

    @Test fun stoppedBeforeSubmissionDoesNotCreateRuntimeOrSendMessage() {
        val controller = StreamController().apply { stop() }
        start("a", controller, CopyOnWriteArrayList()).get(5, TimeUnit.SECONDS)
        assertTrue(calls.isEmpty())
    }

    @Test fun disconnectNotifiesAllLiveConversations() {
        val ca = StreamController(); val cb = StreamController()
        val a = CopyOnWriteArrayList<StreamEvent>(); val b = CopyOnWriteArrayList<StreamEvent>()
        val fa = start("a", ca, a); val fb = start("b", cb, b)
        repeat(2) { assertNotNull(submitted.poll(15, TimeUnit.SECONDS)) }
        socket.close(1001, "test disconnect")
        fa.get(10, TimeUnit.SECONDS); fb.get(10, TimeUnit.SECONDS)
        assertTrue(ca.wasDisconnected()); assertTrue(cb.wasDisconnected())
        assertEquals(1, a.filterIsInstance<StreamEvent.ConnectionInterrupted>().size)
        assertEquals(1, b.filterIsInstance<StreamEvent.ConnectionInterrupted>().size)
    }
    @Test fun remotePdfIsSubmittedAsServerFileNotAsAnImage() {
        val attachment=AttachmentReader.fromWorkspaceDocument(WorkspaceDocument("报告.pdf","/projects/source/报告.pdf","application/pdf","",byteArrayOf(1,2)))
        val future=workers.submit {
            client.streamMessage(StreamController(),HermesSession("a","A",profile="sales",runtimeId="runtime-a",workspacePath="/projects/a"),"请总结",listOf(attachment)) {}
        }
        assertEquals("runtime-a",submitted.poll(15,TimeUnit.SECONDS))
        val prompt=calls.single { it.getString("method")=="prompt.submit" }.getJSONObject("params")
        assertEquals("sales",prompt.getString("profile"))
        assertTrue(prompt.getString("text").contains("/projects/source/报告.pdf"))
        assertTrue(calls.none { it.getString("method")=="image.attach_bytes" })
        event("runtime-a","message.complete","已处理")
        future.get(10,TimeUnit.SECONDS)
    }

    @Test fun voiceFastReplyIsSessionScopedAndRestoredBeforeCompletion() {
        reasoning = "high"
        val events = CopyOnWriteArrayList<StreamEvent>()
        val task = workers.submit {
            client.streamVoiceMessage(StreamController(), HermesSession("voice", "语音", profile="work"), "今天的安排", true, {}, events::add)
        }
        assertEquals("runtime-voice", submitted.poll(15, TimeUnit.SECONDS))
        event("runtime-voice", "message.complete", "今天有三件事。")
        task.get(15, TimeUnit.SECONDS)
        val changes = calls.filter { it.optString("method") == "config.set" }.map { it.getJSONObject("params") }
        assertEquals(listOf("none", "high"), changes.map { it.getString("value") })
        assertTrue(changes.all { it.getString("scope") == "session" && it.getString("profile") == "work" && it.getString("session_id") == "runtime-voice" })
        assertEquals(1, events.count { it == StreamEvent.Completed })
    }

    @Test fun unsupportedVoiceReasoningStillSendsWithoutChangingConfig() {
        reasoning = null
        val notices = CopyOnWriteArrayList<String>()
        val task = workers.submit {
            client.streamVoiceMessage(StreamController(), HermesSession("old", "旧网关"), "你好", true, notices::add, {})
        }
        assertEquals("runtime-old", submitted.poll(15, TimeUnit.SECONDS))
        event("runtime-old", "message.complete", "你好。")
        task.get(15, TimeUnit.SECONDS)
        assertTrue(calls.none { it.optString("method") == "config.set" })
        assertEquals(1, notices.size)
    }

}

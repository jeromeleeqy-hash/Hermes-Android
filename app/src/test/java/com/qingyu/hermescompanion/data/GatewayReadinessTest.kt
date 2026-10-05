package com.qingyu.hermescompanion.data

import com.qingyu.hermescompanion.model.ConnectionConfig
import com.qingyu.hermescompanion.storage.SecureCookieJar
import okhttp3.*
import okhttp3.mockwebserver.*
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import org.mockito.Mockito.mock
import java.util.concurrent.*

/** Delay the protocol-ready edge independently of the WebSocket handshake. */
class GatewayReadinessTest {
    private val server = MockWebServer()
    private val workers = Executors.newCachedThreadPool()
    private val opened = LinkedBlockingQueue<WebSocket>()
    private val requests = LinkedBlockingQueue<Pair<WebSocket, JSONObject>>()
    private lateinit var client: HermesApiClient
    @Before fun setup() {
        server.dispatcher = object : okhttp3.mockwebserver.Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                if (request.path!!.startsWith("/api/auth/ws-ticket")) return MockResponse().setBody("{\"ticket\":\"test\"}")
                return MockResponse().withWebSocketUpgrade(object : WebSocketListener() {
                    override fun onOpen(webSocket: WebSocket, response: Response) { opened.put(webSocket) }
                    override fun onMessage(webSocket: WebSocket, text: String) {
                        val frame = JSONObject(text)
                        if (frame.optString("method") == "client.capabilities") webSocket.send(JSONObject().put("id", frame.get("id")).put("result", JSONObject()).toString())
                        else requests.put(webSocket to frame)
                    }
                    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) { webSocket.close(code, reason) }
                })
            }
        }
        server.start()
        client = HermesApiClient(ConnectionConfig(server.url("/").toString().trimEnd('/'), "test"), mock(SecureCookieJar::class.java))
    }
    @After fun close() { client.close(); workers.shutdownNow(); server.shutdown() }
    private fun ready(ws: WebSocket) { ws.send("{\"method\":\"event\",\"params\":{\"type\":\"gateway.ready\"}}") }
    private fun reply(call: Pair<WebSocket, JSONObject>) {
        call.first.send(JSONObject().put("id", call.second.get("id")).put("result", JSONObject().put("session_id", "runtime").put("stored_session_id", "stored")).toString())
    }
    @Test fun concurrentCallWaitsForProtocolReadyAndUsesOneConnection() {
        val first = workers.submit(Callable { client.createSession() })
        val ws = opened.poll(5, TimeUnit.SECONDS)!!
        // Ensure the client has consumed onOpen before a second caller arrives.
        val socketField = HermesApiClient::class.java.getDeclaredField("socketOpenFuture").apply { isAccessible = true }
        (socketField.get(client) as CompletableFuture<*>).get(5, TimeUnit.SECONDS)
        val secondStarted = CountDownLatch(1)
        val second = workers.submit(Callable { secondStarted.countDown(); client.createSession() })
        assertTrue(secondStarted.await(5, TimeUnit.SECONDS))
        assertNull("No RPC before gateway.ready", requests.poll(350, TimeUnit.MILLISECONDS))
        assertFalse(first.isDone); assertFalse(second.isDone)
        ready(ws)
        repeat(2) { reply(requests.poll(5, TimeUnit.SECONDS)!!) }
        assertEquals("stored", first.get(5, TimeUnit.SECONDS).id)
        assertEquals("stored", second.get(5, TimeUnit.SECONDS).id)
        assertNull(opened.poll(100, TimeUnit.MILLISECONDS))
    }
    @Test fun connectionCheckDoesNotInterruptPendingRpc() {
        val pending = workers.submit(Callable { client.createSession() })
        ready(opened.poll(5, TimeUnit.SECONDS)!!)
        val call = requests.poll(5, TimeUnit.SECONDS)!!
        val diagnostic = workers.submit { client.reconnectGateway() }
        diagnostic.get(5, TimeUnit.SECONDS)
        assertNull(opened.poll(100, TimeUnit.MILLISECONDS))
        reply(call)
        assertEquals("stored", pending.get(5, TimeUnit.SECONDS).id)
    }
    @Test fun disconnectedRequestIsNotReplayedAndNextRequestReconnects() {
        val first = workers.submit(Callable { client.createSession() })
        val old = opened.poll(5, TimeUnit.SECONDS)!!; ready(old)
        requests.poll(5, TimeUnit.SECONDS)!!
        old.close(1001, "offline")
        try { first.get(5, TimeUnit.SECONDS); fail("Disconnected RPC must fail") } catch (_: ExecutionException) { }
        val next = workers.submit(Callable { client.createSession() })
        val replacement = opened.poll(5, TimeUnit.SECONDS)!!; ready(replacement)
        reply(requests.poll(5, TimeUnit.SECONDS)!!)
        assertEquals("stored", next.get(5, TimeUnit.SECONDS).id)
        assertNull("Do not replay an uncertain write", requests.poll(100, TimeUnit.MILLISECONDS))
    }
}

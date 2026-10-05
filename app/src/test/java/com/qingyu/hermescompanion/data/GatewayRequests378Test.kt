package com.qingyu.hermescompanion.data

import com.qingyu.hermescompanion.model.*
import com.qingyu.hermescompanion.storage.SecureCookieJar
import okhttp3.*
import okhttp3.mockwebserver.*
import org.json.*
import org.junit.*
import org.junit.Assert.*
import org.mockito.Mockito.mock
import java.util.concurrent.*

class GatewayRequests378Test {
    private lateinit var server: MockWebServer
    private lateinit var client: HermesApiClient
    private lateinit var socket: WebSocket
    private val received = LinkedBlockingQueue<StreamEvent>()
    private val replies = LinkedBlockingQueue<JSONObject>()
    private val calls = CopyOnWriteArrayList<JSONObject>()
    private val workers = Executors.newCachedThreadPool()
    private var replay = JSONArray()
    private var legacy = false
    @Volatile private var includesSnapshot = true
    @Volatile private var eventSnapshot: JSONArray? = null
    @Volatile private var eventError: Int? = null
    private val session = HermesSession("stored", "Card", profile = "work", workspacePath = "/work")

    @Before fun setup() {
        server = MockWebServer()
        server.dispatcher = object : okhttp3.mockwebserver.Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                if (request.path!!.startsWith("/api/auth/ws-ticket")) return MockResponse().setBody("{\"ticket\":\"test\"}")
                return MockResponse().withWebSocketUpgrade(object : WebSocketListener() {
                    override fun onOpen(ws: WebSocket, response: Response) { socket = ws; event("gateway.ready") }
                    override fun onMessage(ws: WebSocket, text: String) {
                        val frame = JSONObject(text)
                        if (!frame.has("method")) { replies.put(frame); return }
                        calls += frame
                        val result = when (frame.getString("method")) {
                            "session.resume" -> JSONObject().put("session_id", "runtime").put("running", false).also { if (includesSnapshot) it.put("open_requests", replay) }
                            "session.events.since" -> JSONObject().put("events", JSONArray()).put("latest_seq", 3).put("count", 0)
                                .also { eventSnapshot?.let { requests -> it.put("open_requests", requests) } }
                            "clarify.lock" -> JSONObject().put("status", "ok")
                            else -> JSONObject().put("ok", true)
                        }
                        val response = JSONObject().put("jsonrpc", "2.0").put("id", frame.get("id"))
                        if (frame.getString("method") == "session.events.since" && eventError != null) response.put("error", JSONObject().put("code", eventError).put("message", "Cannot read snapshot"))
                        else if (legacy && frame.getString("method") == "client.capabilities") response.put("error", JSONObject().put("code", -32601).put("message", "unknown method"))
                        else response.put("result", result)
                        ws.send(response.toString())
                    }
                    override fun onClosing(ws: WebSocket, code: Int, reason: String) { ws.close(code, reason) }
                })
            }
        }
        server.start()
        client = HermesApiClient(ConnectionConfig(server.url("/").toString().trimEnd('/'), "tester"), mock(SecureCookieJar::class.java))
        client.setAgentRequestListener { _, event -> received.put(event) }
    }
    @After fun close() { client.close(); server.shutdown(); workers.shutdownNow() }
    private fun event(type: String, data: JSONObject = JSONObject()) {
        socket.send(JSONObject().put("method", "event").put("params", JSONObject().put("session_id", "runtime").put("type", type).put("payload", data)).toString())
    }
    private fun approval(id: String = "srq-one") = JSONObject().put("id", id).put("method", "approval").put("params", JSONObject()
        .put("session_id", "runtime").put("request_id", "queue-different-id").put("command", "python writer.py --action-id original")
        .put("description", "Update the selected card").put("choices", JSONArray(listOf("once", "deny"))))
    private fun pending() = (received.poll(5, TimeUnit.SECONDS) as StreamEvent.AgentRequestPending).request

    @Test fun advertisesBeforeResumeAndRepliesWithEnvelopeIdOnlyAfterExplicitChoice() {
        client.resumeSession(session)
        assertEquals("client.capabilities", calls.first().getString("method"))
        assertTrue(calls.first().getJSONObject("params").getBoolean("server_requests"))
        socket.send(approval().toString())
        val request = pending()
        assertEquals("work", request.profile)
        assertEquals("stored", request.conversationId)
        assertEquals(listOf("once", "deny"), approvalChoices(request).map { it.value })
        assertTrue(replies.isEmpty())
        assertThrows(IllegalArgumentException::class.java) { client.respondAgentRequest(request, "always") }
        client.setProfile("personal")
        client.respondAgentRequest(request, "once")
        val reply = replies.poll(5, TimeUnit.SECONDS)!!
        assertEquals("srq-one", reply.getString("id"))
        assertEquals("once", reply.getJSONObject("result").getString("choice"))
        assertFalse(reply.has("method"))
        assertFalse(calls.any { it.optString("method") == "approval.respond" })
        assertThrows(IllegalArgumentException::class.java) { client.respondAgentRequest(request, "once") }
    }

    @Test fun cancellationRemovesOnlyMatchingRequestAndRejectsStaleApproval() {
        client.resumeSession(session)
        socket.send(approval("srq-one").toString()); val first = pending()
        socket.send(approval("srq-two").toString()); val second = pending()
        event("request.cancel", JSONObject().put("id", "srq-one").put("reason", "timeout"))
        val expired = received.poll(5, TimeUnit.SECONDS) as StreamEvent.AgentRequestExpired
        assertEquals(first.requestId, expired.requestId); assertEquals("timeout", expired.reason)
        assertThrows(IllegalArgumentException::class.java) { client.respondAgentRequest(first, "once") }
        client.respondAgentRequest(second, "deny")
        assertEquals("srq-two", replies.poll(5, TimeUnit.SECONDS)!!.getString("id"))
    }

    @Test fun reconnectReplaysOpenRequestsAndAdvertisesAgainWithoutResubmittingWork() {
        client.resumeSession(session)
        socket.send(approval().toString()); val old = pending()
        socket.close(1001, "reconnect")
        // A bounded poll is for the socket callback, not a timer-dependent approval.
        val limit = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
        while (client.recentTransportIssues().lines().none { it.contains("1001") } && System.nanoTime() < limit) Thread.sleep(5)
        replay = JSONArray().put(approval())
        client.refreshAgentRequests(session)
        val restored = pending()
        assertEquals(old.serverRequestId, restored.serverRequestId)
        assertEquals(2, calls.count { it.optString("method") == "client.capabilities" })
        client.respondAgentRequest(restored, "once")
        assertEquals("srq-one", replies.poll(5, TimeUnit.SECONDS)!!.getString("id"))
        assertFalse(calls.any { it.optString("method") == "prompt.submit" })
    }

    @Test fun batchClarificationSkipsLockedAnswerAndUsesCapturedProfile() {
        client.resumeSession(session)
        val params = JSONObject().put("session_id", "runtime").put("answers", JSONObject().put("q1", "Done"))
            .put("questions", JSONArray().put(JSONObject().put("qid", "q1").put("question", "First?"))
                .put(JSONObject().put("qid", "q2").put("question", "Next?").put("choices", JSONArray(listOf("A", "B"))).put("multi_select", true)))
        socket.send(JSONObject().put("id", "srq-clarify").put("method", "clarify").put("params", params).toString())
        val request = pending()
        assertEquals("q2", request.questionId); assertTrue(request.allowMultiple); assertTrue(received.isEmpty())
        client.setProfile("personal"); client.respondAgentRequest(request, "A")
        val lock = calls.single { it.optString("method") == "clarify.lock" }.getJSONObject("params")
        assertEquals("work", lock.getString("profile")); assertEquals("q2", lock.getString("question_id"))
        assertEquals("srq-clarify", lock.getString("request_id"))
    }

    @Test fun unknownServerMethodsGetExplicitUnsupportedError() {
        client.resumeSession(session)
        socket.send(JSONObject().put("id", "srq-unknown").put("method", "sudo").put("params", JSONObject().put("session_id", "runtime")).toString())
        assertEquals(-32601, replies.poll(5, TimeUnit.SECONDS)!!.getJSONObject("error").getInt("code"))
        assertTrue(received.isEmpty())
    }

    @Test fun oldGatewayStillUsesApprovalRespondWithOriginalProfile() {
        legacy = true
        client.resumeSession(session)
        val controller = StreamController()
        val run = workers.submit { client.streamMessage(controller, session.copy(runtimeId = "runtime"), "test", emptyList(), received::put) }
        val limit = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
        while (calls.none { it.optString("method") == "prompt.submit" } && System.nanoTime() < limit) Thread.sleep(5)
        event("message.start"); assertTrue(received.poll(5, TimeUnit.SECONDS) is StreamEvent.RunStarted)
        event("approval.request", approval().getJSONObject("params"))
        val request = pending().copy(profile = "work")
        client.setProfile("personal"); client.respondAgentRequest(request, "once")
        val reply = calls.single { it.optString("method") == "approval.respond" }.getJSONObject("params")
        assertEquals("queue-different-id", reply.getString("request_id")); assertEquals("work", reply.getString("profile"))
        event("message.complete"); run.get(5, TimeUnit.SECONDS)
    }
    @Test fun authoritativeEmptySnapshotClearsPcHandledRequestsWithoutSubmittingAnything() {
        replay = JSONArray().put(approval())
        assertEquals(1, client.inspectAgentRequests(session)!!.size)
        pending()
        replay = JSONArray()
        assertTrue(client.inspectAgentRequests(session)!!.isEmpty())
        assertTrue(received.poll(5, TimeUnit.SECONDS) is StreamEvent.AgentRequestExpired)
        assertTrue(replies.isEmpty())
        assertFalse(calls.any { it.optString("method") in setOf("prompt.submit", "approval.respond") })
    }

    @Test fun missingSnapshotMeansUnknownRatherThanNoPendingRequest() {
        includesSnapshot = false
        assertNull(client.inspectAgentRequests(session))
        socket.send(approval().toString()); pending()
        assertNull(client.inspectAgentRequests(session))
        assertTrue(replies.isEmpty())
    }

    @Test fun realGatewayOmittedEmptyResumeListIsReconciledUsingEventsSnapshot() {
        // Official _live_session_payload omits empty lists; events.since includes open_requests: [].
        replay = JSONArray().put(approval())
        assertEquals(1, client.inspectAgentRequests(session)!!.size)
        pending()
        includesSnapshot = false
        eventSnapshot = JSONArray()
        assertTrue(client.inspectAgentRequests(session)!!.isEmpty())
        assertTrue(received.poll(5, TimeUnit.SECONDS) is StreamEvent.AgentRequestExpired)
        val params = calls.single { it.optString("method") == "session.events.since" }.getJSONObject("params")
        assertEquals("runtime", params.getString("session_id"))
        assertEquals("work", params.getString("profile"))
        assertTrue(replies.isEmpty())
        assertFalse(calls.any { it.optString("method") in setOf("prompt.submit", "approval.respond", "session.delete") })
    }

    @Test fun fallbackRetainsLiveRequestsAndDoesNotReplyToThem() {
        includesSnapshot = false
        eventSnapshot = JSONArray().put(approval())
        assertEquals("srq-one", client.inspectAgentRequests(session)!!.single().serverRequestId)
        pending()
        assertEquals(1, client.inspectAgentRequests(session)!!.size)
        assertTrue(replies.isEmpty())
    }

    @Test fun unsupportedOrMalformedFallbackDoesNotClaimRequestIsResolved() {
        includesSnapshot = false
        eventError = -32601
        assertNull(client.inspectAgentRequests(session))
        eventError = 403
        assertThrows(RpcException::class.java) { client.inspectAgentRequests(session) }
        eventError = null
        eventSnapshot = JSONArray().put(JSONObject().put("id", "bad"))
        assertThrows(IllegalArgumentException::class.java) { client.inspectAgentRequests(session) }
        assertTrue(replies.isEmpty())
    }

    @Test fun expirationForRestoredLegacyRequestIsDeliveredWithoutAnActiveStream() {
        client.resumeSession(session)
        event("approval.expired", JSONObject().put("request_id", "restored-legacy"))
        val expired = received.poll(5, TimeUnit.SECONDS) as StreamEvent.AgentRequestExpired
        assertEquals("restored-legacy", expired.requestId)
    }

}

package com.qingyu.hermescompanion.data

import com.qingyu.hermescompanion.model.ConnectionConfig
import com.qingyu.hermescompanion.storage.SecureCookieJar
import okhttp3.mockwebserver.*
import org.junit.*
import org.junit.Assert.*
import org.mockito.Mockito.mock

class GatewayTransportTest {
    private val server = MockWebServer()
    private lateinit var client: HermesApiClient
    @Before fun setup() {
        server.start()
        client = HermesApiClient(ConnectionConfig(server.url("/").toString().trimEnd('/'), "test"), mock(SecureCookieJar::class.java))
    }
    @After fun close() { client.close(); server.shutdown() }
    private fun interrupted() = MockResponse().setBody("{\"version\":\"" + "x".repeat(1000) + "\"}").setSocketPolicy(SocketPolicy.DISCONNECT_DURING_RESPONSE_BODY)
    @Test fun interruptedReadBodyIsRecoveredUsingOneFreshRead() {
        server.enqueue(interrupted()); server.enqueue(MockResponse().setBody("{\"auth_required\":true}"))
        try { client.checkGatewayAccess() } catch (error: GatewayTransportException) {
            throw AssertionError("Read recovery failed after ${error.attempts} attempts, server received ${server.requestCount}", error.original)
        }
        assertEquals(2, server.requestCount)
        assertTrue(client.recentTransportIssues().isNotEmpty())
    }
    @Test fun retryIsBoundedAndDoesNotDiscardTheFailureCategory() {
        server.enqueue(interrupted()); server.enqueue(interrupted())
        try { client.checkGatewayAccess(); fail("Must report two failed reads") } catch (error: GatewayTransportException) {
            assertEquals("${error.original.javaClass.name}; read=${error.readOnly}; retry=${canRetryRead(error.original)}", 2, error.attempts); assertTrue(error.readOnly)
            assertFalse(error.message.orEmpty().contains(server.hostName))
        }
        assertEquals(2, server.requestCount)
    }
    @Test fun uncertainWriteIsNeverAutomaticallyReplayed() {
        server.enqueue(interrupted())
        try { client.createCronJob("private-job", "private-prompt", "0 8 * * *"); fail("Write must fail without acknowledgement") }
        catch (error: GatewayTransportException) { assertEquals(1, error.attempts); assertFalse(error.readOnly) }
        assertEquals(1, server.requestCount)
        assertFalse(client.recentTransportIssues().contains("private-job"))
        assertFalse(client.recentTransportIssues().contains("private-prompt"))
    }
    @Test fun authenticationFailureIsNotRetried() {
        server.enqueue(MockResponse().setResponseCode(401).setBody("{}"))
        try { client.gatewayInfo(); fail("Expected authentication error") } catch (error: ApiException) { assertEquals(401, error.statusCode) }
        assertEquals(1, server.requestCount)
    }
    @Test fun tlsAndCancellationFailuresAreNotRetried() {
        assertFalse(canRetryRead(javax.net.ssl.SSLHandshakeException("certificate")))
        assertFalse(canRetryRead(java.io.InterruptedIOException("cancelled")))
        assertTrue(canRetryRead(java.net.SocketTimeoutException()))
    }
}

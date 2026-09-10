package com.qingyu.hermescompanion.data

import com.qingyu.hermescompanion.model.ConnectionConfig
import com.qingyu.hermescompanion.storage.SecureCookieJar
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.MockResponse
import org.json.JSONObject
import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mockito.mock
import okio.Buffer
import java.nio.file.Files
import java.util.Base64

class AudioFileRequestBodyTest {
    @Test fun streamedJsonHasExactLengthAndRoundTripsAtEveryBase64BlockBoundary() {
        val file = Files.createTempFile("audio", ".m4a").toFile()
        try {
            listOf(128, 24575, 24576, 24577, 49153).forEach { size ->
                val bytes = ByteArray(size) { (it % 251).toByte() }; file.writeBytes(bytes)
                val body = AudioFileRequestBody(file); val sink = Buffer(); body.writeTo(sink)
                assertEquals(body.contentLength(), sink.size)
                val json = JSONObject(sink.readUtf8())
                assertEquals("audio/mp4", json.getString("mime_type"))
                assertArrayEquals(bytes, Base64.getDecoder().decode(json.getString("data_url").substringAfter("base64,")))
            }
        } finally { file.delete() }
    }
    @Test fun transcriptionPreservesRecordingOnBothFailureAndSuccessAndKeepsExplicitProfile() {
        val server = MockWebServer(); server.start()
        val client = HermesApiClient(ConnectionConfig(server.url("/").toString(), "ceo"), mock(SecureCookieJar::class.java))
        val file = Files.createTempFile("audio", ".m4a").toFile(); file.writeBytes(ByteArray(900) { 3 })
        try {
            client.setProfile("other")
            server.enqueue(MockResponse().setResponseCode(413).setBody("{\"error\":\"too large\"}"))
            assertThrows(ApiException::class.java) { client.transcribeAudioFile(file, "work") }
            assertEquals(900L, file.length())
            server.enqueue(MockResponse().setBody("{\"ok\":true,\"transcript\":\"明天确认选题\",\"provider\":\"test\"}"))
            assertEquals("明天确认选题", client.transcribeAudioFile(file, "work").transcript)
            assertEquals(900L, file.length())
            repeat(2) { assertEquals("work", server.takeRequest().requestUrl!!.queryParameter("profile")) }
        } finally { client.close(); server.shutdown(); file.delete() }
    }
    @Test fun emptyOrChangedRecordingIsRejectedBeforeUpload() {
        val file = Files.createTempFile("audio", ".m4a").toFile()
        try {
            assertThrows(IllegalArgumentException::class.java) { AudioFileRequestBody(file) }
            file.writeBytes(ByteArray(512))
            val body = AudioFileRequestBody(file); file.writeBytes(ByteArray(128))
            assertThrows(IllegalArgumentException::class.java) { body.writeTo(Buffer()) }
        } finally { file.delete() }
    }
}

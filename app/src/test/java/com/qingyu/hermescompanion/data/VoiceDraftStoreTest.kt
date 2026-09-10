package com.qingyu.hermescompanion.data

import org.junit.Test
import org.junit.Assert.*
import java.nio.file.Files

class VoiceDraftStoreTest {
    @Test fun recordingAndTranscriptSurviveRestartAndStayBoundToTheirOriginalAccountAndConversation() {
        val root = Files.createTempDirectory("voice-recovery").toFile()
        try {
            val draft = VoiceDraft(server = "https://hermes/", account = "ceo", profile = "work", session = "session-a")
            val store = VoiceDraftStore(root)
            store.save(draft); store.file(draft).writeBytes(ByteArray(512) { 1 })
            val restored = VoiceDraftStore(root).all().single()
            assertTrue(restored.matches("https://hermes", "ceo", "work", "session-a", false))
            assertFalse(restored.matches("https://other", "ceo", "work", "session-a", false))
            assertFalse(restored.matches("https://hermes", "another", "work", "session-a", false))
            assertFalse(restored.matches("https://hermes", "ceo", "personal", "session-a", false))
            assertFalse(restored.matches("https://hermes", "ceo", "work", "session-b", false))
            store.save(restored.copy(transcript = "先确认数据"))
            assertEquals("先确认数据", VoiceDraftStore(root).all().single().transcript)
            assertEquals(512L, store.file(draft).length())
            store.delete(draft)
            assertTrue(store.all().isEmpty()); assertFalse(store.file(draft).exists())
        } finally { root.deleteRecursively() }
    }
}

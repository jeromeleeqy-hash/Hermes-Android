package com.qingyu.hermescompanion.storage

import android.app.Application
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class VoiceTranscriptCommitTest {
    @Test fun restartAfterDraftCommitDoesNotAppendTheSameTranscriptionTwice() {
        val context = RuntimeEnvironment.getApplication()
        val store = SecureConfigStore(context)
        store.saveDraft("work", "a", "已有想法")
        assertEquals("已有想法 先确认数据", store.applyVoiceTranscript("work", "a", "recording-1", "先确认数据"))
        val restarted = SecureConfigStore(context)
        assertEquals("已有想法 先确认数据", restarted.applyVoiceTranscript("work", "a", "recording-1", "先确认数据"))
        assertEquals("", restarted.readDraft("work", "b"))
        assertEquals("已有想法 先确认数据 再跟团队讨论", restarted.applyVoiceTranscript("work", "a", "recording-2", "再跟团队讨论"))
    }
    @Test fun tooLongDraftIsNotSilentlyTruncatedByTranscription() {
        val store = SecureConfigStore(RuntimeEnvironment.getApplication())
        store.saveDraft("work", "a", "x".repeat(50000))
        assertThrows(IllegalArgumentException::class.java) { store.applyVoiceTranscript("work", "a", "recording-1", "新文字") }
        assertEquals(50000, store.readDraft("work", "a").length)
    }
}

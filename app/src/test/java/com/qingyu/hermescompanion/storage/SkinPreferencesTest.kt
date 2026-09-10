package com.qingyu.hermescompanion.storage

import com.qingyu.hermescompanion.ui.SkinMode
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SkinPreferencesTest {
    @Test fun allThreeSkinsSurviveReopeningTheStoreWithoutChangingDailyBinding() {
        val app = RuntimeEnvironment.getApplication()
        val store = SecureConfigStore(app)
        store.saveDailyConversation("https://example.com", "alice", "work", "daily-1")
        SkinMode.entries.forEach { mode ->
            store.saveSkinMode(mode.name)
            val reopened = SecureConfigStore(app)
            assertEquals(mode.name,reopened.readSkinMode())
            assertEquals("daily-1",reopened.readDailyConversation("https://example.com", "alice", "work"))
        }
    }
}

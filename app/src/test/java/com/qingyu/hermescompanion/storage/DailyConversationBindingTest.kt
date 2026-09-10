package com.qingyu.hermescompanion.storage

import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DailyConversationBindingTest {
    @Test fun bindingSurvivesRestartAndIsIsolatedByServerAccountAndProfile() {
        val context = RuntimeEnvironment.getApplication()
        val first = SecureConfigStore(context)
        first.saveDailyConversation("https://one/", "alice", "work", "daily-a")
        val restarted = SecureConfigStore(context)
        assertEquals("daily-a", restarted.readDailyConversation("https://one", "alice", "work"))
        assertNull(restarted.readDailyConversation("https://two", "alice", "work"))
        assertNull(restarted.readDailyConversation("https://one", "bob", "work"))
        assertNull(restarted.readDailyConversation("https://one", "alice", "personal"))
        restarted.saveDailyConversation("https://one", "alice", "work", null)
        assertNull(SecureConfigStore(context).readDailyConversation("https://one", "alice", "work"))
    }
}

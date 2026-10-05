package com.qingyu.hermescompanion.storage

import com.qingyu.hermescompanion.ui.*
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class HomeModePreferencesTest {
    @Test fun migrationPreservesDeepUsersAndAnExplicitChoiceAlwaysWins() {
        assertEquals(HomeMode.SIMPLE, resolveHomeMode(null, false))
        assertEquals(HomeMode.DEEP, resolveHomeMode(null, true))
        assertEquals(HomeMode.SIMPLE, resolveHomeMode("SIMPLE", true))
        assertEquals(HomeMode.DEEP, resolveHomeMode("DEEP", false))
        assertEquals(HomeMode.SIMPLE, resolveHomeMode("unknown", false))
        assertEquals(HomeMode.DEEP, resolveHomeMode("unknown", true))
    }

    @Test fun modeSurvivesReopeningAndSignOutWithoutChangingSkinOrDailyConversation() {
        val app = RuntimeEnvironment.getApplication()
        val store = SecureConfigStore(app)
        store.saveSkinMode("GLASS")
        store.saveDailyConversation("https://example.com", "alice", "work", "daily-1")
        HomeMode.entries.forEach { mode ->
            store.saveHomeMode(mode.name)
            val reopened = SecureConfigStore(app)
            assertEquals(mode.name, reopened.readHomeMode())
            assertEquals("GLASS", reopened.readSkinMode())
            assertEquals("daily-1", reopened.readDailyConversation("https://example.com", "alice", "work"))
        }
        store.clear()
        assertEquals("DEEP", SecureConfigStore(app).readHomeMode())
    }

    @Test fun onlyActualTaskEvidenceMigratesOldUsers() {
        val store = SecureConfigStore(RuntimeEnvironment.getApplication())
        assertFalse(store.hasUsedDeepHome())
        store.saveInspectedTaskSessions("scope", setOf("default::human-chat"))
        store.saveTaskSessionKeys("scope", emptySet())
        assertFalse(store.hasUsedDeepHome())
        store.saveTaskSessionKeys("scope", setOf("default::overview-task"))
        assertTrue(store.hasUsedDeepHome())
    }
}

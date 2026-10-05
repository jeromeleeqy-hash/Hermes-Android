package com.qingyu.hermescompanion.today

import org.junit.*
import org.junit.Assert.*
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import javax.crypto.KeyGenerator

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class TodayCacheTest {
    @get:Rule val temporary = TemporaryFolder()
    private val secret = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
    private val cipher = AesTodayCipher { secret }
    private val scope = TodayCacheScope("https://one.example", "alice", "default")
    private val raw = """{"schema_version":1,"date":"2026-10-03","generated_at":"2026-10-03T09:00:00+08:00","headline":"Confidential overview","summary":"","cards":[]}"""
    private fun state(root: String = "/work") = TodayState(profile = scope.profile, root = root, rawJson = raw,
        board = TodayBoard.decode(raw, root), rootVerified = true, loaded = true, fileExists = true, syncedAt = 12345)

    @Test fun processRestartRestoresAnEncryptedReadOnlyCopy() {
        val folder = temporary.newFolder()
        val cache = TodayCache(folder, cipher)
        assertTrue(cache.save(scope, state(), cache.ticket()))
        assertFalse(File(folder, scope.key).readText().contains("Confidential overview"))
        val restarted = TodayCache(folder, cipher).read(scope)!!
        assertEquals(raw, restarted.rawJson)
        assertEquals(12345L, restarted.syncedAt)
        assertTrue(restarted.fromCache); assertTrue(restarted.localSaved)
        assertFalse(restarted.rootVerified)
    }

    @Test fun scopeSeparatesServerAccountAndProfileAndRejectsCopiedCiphertext() {
        val folder = temporary.newFolder()
        val cache = TodayCache(folder, cipher)
        cache.save(scope, state(), cache.ticket())
        listOf(scope.copy(server = "https://two.example"), scope.copy(account = "bob"), scope.copy(profile = "personal")).forEach {
            assertNull(cache.read(it))
            File(folder, scope.key).copyTo(File(folder, it.key))
            assertThrows(Exception::class.java) { cache.read(it) }
        }
    }

    @Test fun invalidNewFileDoesNotReplaceLastGoodCopyAndRootChangeDoesNotReuseIt() {
        val cache = TodayCache(temporary.newFolder(), cipher)
        cache.save(scope, state(), cache.ticket())
        assertThrows(Exception::class.java) { cache.save(scope, state().copy(rawJson = "{broken"), cache.ticket()) }
        val previous = cache.read(scope)!!
        assertEquals(raw, previous.rawJson)
        val offline = TodayState(profile = scope.profile, loaded = true, error = "offline")
        assertEquals(raw, retainTodayOnFailure(offline, previous).rawJson)
        val moved = offline.copy(root = "/new-work", rootVerified = true)
        assertNull(retainTodayOnFailure(moved, previous).board)
        cache.remove(scope, cache.ticket())
        assertNull(cache.read(scope))
    }

    @Test fun tamperingFailsClosedAndLogoutRejectsLateWrites() {
        val folder = temporary.newFolder()
        val cache = TodayCache(folder, cipher)
        val ticket = cache.ticket()
        cache.save(scope, state(), ticket)
        val file = File(folder, scope.key)
        val bytes = file.readBytes(); bytes[bytes.lastIndex] = (bytes.last().toInt() xor 1).toByte(); file.writeBytes(bytes)
        assertThrows(Exception::class.java) { cache.read(scope) }
        cache.clear()
        assertFalse(cache.save(scope, state(), ticket))
        assertNull(cache.read(scope))
    }

    @Test fun newerSyncInvalidatesAnOlderPendingWrite() {
        val cache = TodayCache(temporary.newFolder(), cipher)
        val earlier = cache.ticket()
        val later = cache.ticket()
        assertTrue(cache.save(scope, state("/new-work"), later))
        assertFalse(cache.save(scope, state(), earlier))
        assertEquals("/new-work", cache.read(scope)!!.root)
    }
}

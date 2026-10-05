package com.qingyu.hermescompanion.update

import android.app.DownloadManager
import android.content.Context
import androidx.core.content.FileProvider
import com.qingyu.hermescompanion.BuildConfig
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [35])
class AppUpdateRepositoryTest {
    private val context get() = RuntimeEnvironment.getApplication()
    private fun release() = AppRelease(context.packageName, BuildConfig.VERSION_CODE.toLong() + 1, "3.8.3",
        "https://download.leaier.com/releases/Hermes-Android-3.8.3-release.apk", "a".repeat(64), 200, 26, emptyList())
    @Before fun clean() { context.getSharedPreferences("app_updates", Context.MODE_PRIVATE).edit().clear().commit() }
    @Test fun recoverSystemDownloadAndOptionsAfterRepositoryRecreation() {
        val first = AppUpdateRepository(context)
        first.wifiOnly = false
        val pending = first.start(release())
        val restored = AppUpdateRepository(context)
        assertEquals(pending, restored.pending())
        assertFalse(restored.wifiOnly)
        val cursor = context.getSystemService(DownloadManager::class.java).query(DownloadManager.Query().setFilterById(pending.id))
        cursor.use { assertTrue(it.moveToFirst()) }
        restored.clearDownload()
        assertNull(AppUpdateRepository(context).pending())
    }
    @Test fun startsWithWifiOnlyAndThrottlesDailyChecksWithoutBlockingManualChecks() {
        val repo = AppUpdateRepository(context)
        assertTrue(repo.wifiOnly)
        assertTrue(repo.shouldCheck(100_000_000))
        repo.markAttempt(100_000_000)
        assertFalse(repo.shouldCheck(100_000_001))
        assertTrue(repo.shouldCheck(186_400_000))
        repo.automatic = false
        assertFalse(repo.shouldCheck(300_000_000))
        assertFalse(AppUpdateRepository(context).automatic)
    }
    @Test fun providerExposesOnlyUpdateSubdirectory() {
        val base = context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS)!!
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", File(base, "app-updates/a.apk"))
        assertEquals("content", uri.scheme)
        assertTrue(uri.path!!.startsWith("/app_updates/"))
        try {
            FileProvider.getUriForFile(context, "${context.packageName}.files", File(base, "unrelated.txt"))
            fail("Other external files must not be shared")
        } catch (_: IllegalArgumentException) { }
    }
    @Test fun downloadRefusesDowngradesBeforeEnqueue() {
        try {
            AppUpdateRepository(context).start(release().copy(versionCode = BuildConfig.VERSION_CODE.toLong()))
            fail("Refuse same or older version")
        } catch (e: UpdateFailure) { assertEquals(UpdateError.INCOMPATIBLE, e.reason) }
    }
}

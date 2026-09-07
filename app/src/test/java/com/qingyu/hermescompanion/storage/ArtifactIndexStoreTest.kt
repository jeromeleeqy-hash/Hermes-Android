package com.qingyu.hermescompanion.storage

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.qingyu.hermescompanion.model.RecentArtifact
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
class ArtifactIndexStoreTest {
    @Test fun originalPathRoundTripsAndOldRecordsRemainReadable() {
        val context=ApplicationProvider.getApplicationContext<Context>()
        val store=SecureConfigStore(context)
        val item=RecentArtifact("sales","source","日报","m","/work/out/日报.md","日报.md","Markdown","/work",123L,"out/日报.md")
        store.saveRecentArtifacts(listOf(item))
        assertEquals(item,store.readRecentArtifacts().single())
        val prefs=context.getSharedPreferences("hermes_secure_connection",Context.MODE_PRIVATE)
        val fields=prefs.getString("recent_artifacts","")!!.split('\t')
        prefs.edit().putString("recent_artifacts",(fields.take(8)+fields.last()).joinToString("\t")).commit()
        assertEquals(item.copy(sourcePath=""),store.readRecentArtifacts().single())
        prefs.edit().putString("recent_artifacts",(fields.take(7)+fields.last()).joinToString("\t")).commit()
        assertEquals(item.copy(sourcePath="",workspacePath=""),store.readRecentArtifacts().single())
    }
}

package com.qingyu.hermescompanion.storage

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.qingyu.hermescompanion.model.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class UiPreferences360Test {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    @Test fun editsAndOrderSurviveRecreationAndAnEmptyListStaysEmpty() {
        val store = SecureConfigStore(context)
        val edited = listOf(PromptSnippet("mine", "自定义", "多行\n包含中文、引号\"与变量 ${'$'}{topic}")) + DefaultPromptSnippets.reversed()
        store.savePromptSnippets(edited); store.saveReduceMotion(true)
        val reopened = SecureConfigStore(context)
        assertEquals(edited, reopened.readPromptSnippets()); assertTrue(reopened.readReduceMotion())
        reopened.savePromptSnippets(emptyList())
        assertEquals(emptyList<PromptSnippet>(), SecureConfigStore(context).readPromptSnippets())
        reopened.savePromptSnippets(DefaultPromptSnippets)
        assertEquals(DefaultPromptSnippets, SecureConfigStore(context).readPromptSnippets())
    }
    @Test fun newPreferencesDefaultWithoutChangingTheExistingSkin() {
        val store = SecureConfigStore(context)
        store.saveSkinMode("PAPER"); store.saveThemeMode("DARK")
        assertEquals(DefaultPromptSnippets, store.readPromptSnippets())
        assertFalse(store.readReduceMotion())
        store.saveReduceMotion(true)
        assertEquals("PAPER", store.readSkinMode()); assertEquals("DARK", store.readThemeMode())
    }
}

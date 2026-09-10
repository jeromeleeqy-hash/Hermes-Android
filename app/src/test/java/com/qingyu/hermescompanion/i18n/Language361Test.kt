package com.qingyu.hermescompanion.i18n

import android.app.Application
import android.app.LocaleManager
import android.content.Context
import android.os.LocaleList
import androidx.test.core.app.ApplicationProvider
import com.qingyu.hermescompanion.R
import com.qingyu.hermescompanion.assistant.DailyConversation
import com.qingyu.hermescompanion.model.*
import com.qingyu.hermescompanion.storage.SecureConfigStore
import com.qingyu.hermescompanion.ui.*
import com.qingyu.hermescompanion.ui.format.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35],qualifiers="en-rUS")
class Language361Test {
    private val app = ApplicationProvider.getApplicationContext<Application>()
    @After fun reset() { AppLanguage.setMode(app,AppLanguageMode.SYSTEM) }
    @Test fun explicitLanguageOverridesSystemAndDefaultListsReevaluate() {
        AppLanguage.setMode(app,AppLanguageMode.CHINESE)
        assertEquals("全部时间",SessionTimeFilter.ALL.label)
        assertEquals("梳理目标",DefaultPromptSnippets.first().title)
        AppLanguage.setMode(app,AppLanguageMode.ENGLISH)
        assertEquals("All time",SessionTimeFilter.ALL.label)
        assertFalse(DefaultPromptSnippets.first().title.any { it in '\u4e00'..'\u9fff' })
        assertEquals("en",AppLanguage.localizedContext(app).resources.configuration.locales[0].language)
        assertEquals("en", app.getSystemService(LocaleManager::class.java).applicationLocales[0].language)
        assertEquals("ENGLISH",SecureConfigStore(app).readLanguageMode())
        AppLanguage.setMode(app,AppLanguageMode.SYSTEM)
        assertTrue(app.getSystemService(LocaleManager::class.java).applicationLocales.isEmpty)
        assertEquals("en",AppLanguage.locale.language)
    }
    @Test @Config(sdk=[26],qualifiers="zh-rCN") fun olderAndroidPersistsChoiceAndWrapsResources() {
        AppLanguage.setMode(app,AppLanguageMode.ENGLISH)
        AppLanguage.refresh(app)
        assertEquals(AppLanguageMode.ENGLISH,AppLanguage.savedMode(app))
        assertEquals("Reduce motion",AppLanguage.localizedContext(app).getString(R.string.ui_0854))
    }
    @Test fun allEnglishResourcesFormatAndHaveNoChineseFallback() {
        AppLanguage.setMode(app,AppLanguageMode.ENGLISH)
        val res = AppLanguage.localizedContext(app).resources
        val han=Regex("[\\u4e00-\\u9fff]")
        val args=Array<Any?>(12) { "VALUE%$it" }
        var count=0
        R.string::class.java.declaredFields.filter { it.name.startsWith("ui_") }.forEach {
            val id=it.getInt(null);val text=res.getString(id)
            assertFalse("English missing: ${it.name}",han.containsMatchIn(text))
            if(Regex("%\\d+[${'$'}]s").containsMatchIn(text)) String.format(AppLanguage.locale,text,*args)
            count++
        }
        assertTrue(count>1300)
    }
    @Test fun languageChangeKeepsDraftConversationAndCustomSnippets() {
        AppLanguage.setMode(app,AppLanguageMode.CHINESE)
        val snippet=PromptSnippet("custom","我的模板","保留原文 %s / C:\\工作")
        SecureConfigStore(app).savePromptSnippets(listOf(snippet))
        val vm=HermesViewModel(app)
        val session=HermesSession("current","用户自己的标题",profile="work",workspacePath="C:\\Work")
        val before=vm.uiState.copy(route=AppRoute.CHAT,selectedSession=session,draft="尚未发送的中英 draft",workspaceDraft="# 原文",activeProfile="work")
        HermesViewModel::class.java.getDeclaredMethod("setUiState",AppUiState::class.java).apply { isAccessible=true }.invoke(vm,before)
        vm.setLanguageMode(AppLanguageMode.ENGLISH)
        assertEquals(before.draft,vm.uiState.draft)
        assertEquals(session,vm.uiState.selectedSession)
        assertEquals(before.workspaceDraft,vm.uiState.workspaceDraft)
        assertEquals(before.activeProfile,vm.uiState.activeProfile)
        assertEquals(listOf(snippet),vm.uiState.promptSnippets)
        assertEquals(listOf(snippet),SecureConfigStore(app).readPromptSnippets())
        assertEquals(AppRoute.CHAT,vm.uiState.route)
        assertTrue(DailyConversation.isDailyTitle("日常助理"))
        assertTrue(DailyConversation.isDailyTitle("Daily assistant"))
        assertEquals("日常助理",DailyConversation.stableTitle(session.copy(title="日常助理")))
    }
    @Test fun datesAndPlaceholdersUseUiLanguageWithoutTranslatingUserTitles() {
        AppLanguage.setMode(app,AppLanguageMode.ENGLISH)
        assertTrue(sessionTimeLabel("2026-09-01T10:00:00Z",Instant.parse("2026-09-09T12:00:00Z")).contains("Sep"))
        assertEquals("这是用户标题",resolvedSessionTitle("这是用户标题"))
        assertTrue(isPlaceholderSessionTitle("新会话"));assertTrue(isPlaceholderSessionTitle("New conversation"))
        assertEquals("New conversation",resolvedSessionTitle("新会话"))
    }
    @Test fun localeRecreationDoesNotReplayTheInitialShare() {
        val vm=HermesViewModel(app)
        val intent=android.content.Intent(android.content.Intent.ACTION_SEND).putExtra(android.content.Intent.EXTRA_TEXT,"Shared original")
        vm.handleInitialIntent(intent)
        assertEquals("Shared original",vm.uiState.incomingShare?.sharedText)
        val afterDismiss=vm.uiState.copy(incomingShare=null)
        HermesViewModel::class.java.getDeclaredMethod("setUiState",AppUiState::class.java).apply { isAccessible=true }.invoke(vm,afterDismiss)
        vm.setLanguageMode(AppLanguageMode.ENGLISH)
        vm.handleInitialIntent(intent)
        assertNull(vm.uiState.incomingShare)
    }

}

package com.qingyu.hermescompanion.ui.screen

import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.*
import com.qingyu.hermescompanion.model.*
import com.qingyu.hermescompanion.ui.*
import com.qingyu.hermescompanion.ui.component.*
import com.qingyu.hermescompanion.ui.theme.HermesCompanionTheme
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.Instant

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "zh-rCN-w390dp-h844dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class UiUpgrade360Test {
    @get:Rule val compose = createComposeRule()
    private val sample = AppUiState(route = AppRoute.HOME, username = "Jerome", reduceMotion = true, homeWelcomed = true,
        sessions = listOf(HermesSession("video", "AI 新媒体战略研讨", "继续梳理账号运营策略", Instant.now().minusSeconds(86400).toString()),
            HermesSession("daily", "Looki 日报", "查看今天的工作记录", Instant.now().toString())))
    private fun capture(name: String, node: SemanticsNodeInteraction = compose.onRoot()): Bitmap = compose.runOnIdle {
        val view = (node.fetchSemanticsNode().root as ViewRootForTest).view
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(android.graphics.Canvas(bitmap))
        File("build/ui-validation/360-$name.png").apply { parentFile.mkdirs() }.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap
    }
    private fun home(mode: SkinMode, dark: Boolean = false, scale: Float = 1f, captureSuffix: String = "") {
        var opened = false
        compose.setContent { CompositionLocalProvider(LocalDensity provides Density(1f, scale), LocalReduceMotion provides true) {
            HermesCompanionTheme(if (dark) ThemeMode.DARK else ThemeMode.LIGHT, mode) { AmbientBackground {
                Scaffold(containerColor = Color.Transparent, contentWindowInsets = WindowInsets(0,0,0,0), bottomBar = { ReferenceBottomDock(AppRoute.HOME, false, {}) }) { padding ->
                    HermesScene { AssistantHomeScreen(sample.copy(skinMode = mode), padding, {}, {}, {}, {}, {}, { _, _ -> }, {}, onDaily = { opened = true }) }
                }
            } }
        } }
        compose.onNodeWithTag("daily_conversation_entry").assertIsDisplayed()
        val person = compose.onNodeWithTag("home_hermes_portrait").fetchSemanticsNode().boundsInRoot
        val button = compose.onNodeWithTag("daily_conversation_entry").fetchSemanticsNode().boundsInRoot
        assertTrue("Character and action must not intersect", person.right <= button.left || person.left >= button.right || person.bottom <= button.top || person.top >= button.bottom)
        if (mode == SkinMode.GLASS) assertTrue("Glass must have a clear gap below the boots", button.top - person.bottom >= 15f)
        compose.onNodeWithTag("home_menu_anchor").assertDoesNotExist()
        capture("home-${mode.name}-${if(dark) "dark" else "light"}-${scale}${captureSuffix}")
        compose.onNodeWithTag("daily_conversation_entry").performClick(); assertTrue(opened)
        compose.onNodeWithText("Looki 日报").performScrollTo().assertIsDisplayed()
    }
    @Test fun warmHome() = home(SkinMode.CLEAN)
    @Test fun glassHome() = home(SkinMode.GLASS)
    @Test fun quietHome() = home(SkinMode.PAPER)
    @Test fun glassDarkHome() = home(SkinMode.GLASS, true)
    @Test fun warmDarkHome() = home(SkinMode.CLEAN, true)
    @Test fun quietDarkHome() = home(SkinMode.PAPER, true)
    @Test @Config(qualifiers="zh-rCN-w320dp-h640dp-mdpi") fun smallWarmWithLargeText() = home(SkinMode.CLEAN, false, 1.3f)
    @Test @Config(qualifiers="zh-rCN-w320dp-h640dp-mdpi") fun smallGlassWithLargeText() = home(SkinMode.GLASS, false, 1.3f)
    @Test @Config(qualifiers="zh-rCN-w320dp-h640dp-mdpi") fun smallQuietWithLargeText() = home(SkinMode.PAPER, false, 1.3f)

    @Test fun promptCrudReordersAndInsertsWithoutSending() {
        var items by mutableStateOf(DefaultPromptSnippets)
        var visible by mutableStateOf(true)
        var inserted = ""
        compose.setContent { HermesCompanionTheme(ThemeMode.LIGHT, SkinMode.GLASS) { AmbientBackground {
            HermesScene { Text("日常助理") }
            if (visible) ComposerToolsSheet({ visible = false }, {}, {}, {}, {}, { inserted = it }, items, { items = it })
        } } }
        capture("composer-glass", compose.onNodeWithText("添加到对话"))
        compose.onNodeWithText("管理").performClick()
        compose.onNodeWithText("新建片段").performClick()
        compose.onNodeWithTag("snippet_title").performTextInput("晨间安排")
        compose.onNodeWithTag("snippet_body").performTextInput("按照优先级整理今天的事项")
        compose.onNodeWithText("保存片段").performScrollTo().performClick()
        assertEquals(4, items.size)
        val id = items.last().id
        compose.onNodeWithContentDescription("上移晨间安排").performScrollTo().performClick()
        assertEquals(id, items[2].id)
        compose.onNodeWithContentDescription("编辑晨间安排").performScrollTo().performClick()
        compose.onNodeWithTag("snippet_body").performTextReplacement("先列三件重要的事，再安排时间")
        compose.onNodeWithText("保存片段").performScrollTo().performClick()
        assertEquals("先列三件重要的事，再安排时间", items[2].text)
        capture("snippets-manage", compose.onNodeWithText("管理提示词"))
        compose.onNodeWithText("返回").performScrollTo().performClick()
        compose.onNodeWithText("晨间安排").performScrollTo().performClick()
        assertFalse(visible)
        assertEquals("先列三件重要的事，再安排时间", inserted)
    }
    @Test fun deletingAndRestoringSnippetsRequireConfirmation() {
        var items by mutableStateOf(DefaultPromptSnippets)
        compose.setContent { HermesCompanionTheme(ThemeMode.LIGHT, SkinMode.CLEAN) { AmbientBackground {
            ComposerToolsSheet({}, {}, {}, {}, {}, {}, items, { items = it })
        } } }
        compose.onNodeWithText("管理").performClick()
        compose.onNodeWithContentDescription("删除梳理目标").performClick()
        compose.onNodeWithText("取消").performClick(); assertEquals(3, items.size)
        compose.onNodeWithContentDescription("删除梳理目标").performClick()
        compose.onNodeWithText("删除", substring=false).performClick(); assertEquals(2, items.size)
        compose.onNodeWithText("恢复默认片段").performScrollTo().performClick()
        compose.onNodeWithText("取消").performClick(); assertEquals(2, items.size)
        compose.onNodeWithText("恢复默认片段").performScrollTo().performClick()
        compose.onNodeWithText("恢复默认", substring=false).performClick(); assertEquals(DefaultPromptSnippets, items)
    }
    @Test fun unfinishedSnippetCanBeKeptOnCancel() {
        var items = DefaultPromptSnippets
        compose.setContent { HermesCompanionTheme(ThemeMode.LIGHT, SkinMode.CLEAN) { AmbientBackground {
            ComposerToolsSheet({}, {}, {}, {}, {}, {}, items, { items = it })
        } } }
        compose.onNodeWithText("管理").performClick(); compose.onNodeWithText("新建片段").performClick()
        compose.onNodeWithTag("snippet_title").performTextInput("未完成")
        compose.onNodeWithText("返回").performClick()
        compose.onNodeWithText("继续编辑").performClick()
        compose.onNodeWithTag("snippet_title").assertTextContains("未完成")
        assertEquals(DefaultPromptSnippets, items)
    }
    @Test fun appearanceHasExclusiveRadioChoicesAndReducedMotion() {
        var current by mutableStateOf(sample)
        compose.setContent { CompositionLocalProvider(LocalReduceMotion provides true) {
            HermesCompanionTheme(current.themeMode, current.skinMode) { AmbientBackground {
                HermesScene { ProfileScreen(current, PaddingValues(), true, {}, {}, { current = current.copy(themeMode = it) },
                    { current = current.copy(skinMode = it) }, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, { _, _ -> }, {}, {},
                    onReduceMotionChange = { current = current.copy(reduceMotion = it) }) }
            } }
        } }
        compose.onNodeWithText("外观").performClick()
        compose.onNodeWithText("液态玻璃").performClick()
        compose.onNodeWithText("深色", substring=false).performScrollTo().performClick()
        assertEquals(ThemeMode.DARK, current.themeMode)
        compose.onNodeWithText("深色", substring=false).assertIsSelected()
        compose.onNodeWithText("浅色", substring=false).assertIsNotSelected()
        capture("appearance-dark", compose.onNodeWithText("颜色模式"))
        compose.onNodeWithText("减少动态效果").performScrollTo().performClick()
        assertFalse(current.reduceMotion)
    }
    @Test fun sheetMaterialCoversTheWholeGestureInset() {
        var inset by mutableStateOf(24)
        var skin by mutableStateOf(SkinMode.GLASS)
        compose.setContent { HermesCompanionTheme(ThemeMode.LIGHT, skin) { AmbientBackground {
            HermesScene { Text("背景内容") }
            HermesModalBottomSheet(onDismissRequest = {}, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                navigationInsets = WindowInsets(0,0,0,inset)) {
                Column(Modifier.fillMaxWidth().padding(20.dp)) { Text("安全区检查"); Button(onClick = {}) { Text("底部操作") } }
            }
        } } }
        for (mode in SkinMode.entries) for (bottom in listOf(24,48)) {
            compose.runOnIdle { skin = mode; inset = bottom }
            compose.onNodeWithText("底部操作").assertIsDisplayed()
            val sheet = compose.onNodeWithTag("sheet_surface").fetchSemanticsNode().boundsInRoot
            val action = compose.onNodeWithText("底部操作").fetchSemanticsNode().boundsInRoot
            val bitmap = capture("sheet-inset-${mode.name}-$bottom", compose.onNodeWithTag("sheet_surface"))
            assertEquals("Surface must reach the physical window bottom", bitmap.height.toFloat(), sheet.bottom, 1f)
            assertTrue("Content stays clear of system gestures", sheet.bottom - action.bottom >= bottom)
        }
    }

    @Test @Config(sdk=[26]) fun oldAndroidRendersStaticCharacterAndGlassFallback() = home(SkinMode.GLASS, captureSuffix = "-api26")

    private fun completionOrder(beforeStop: Boolean) {
        val session = sample.sessions.first()
        var current by mutableStateOf(sample.copy(route = AppRoute.CHAT, selectedSession = session, reduceMotion = false,
            isStreaming = true, streamingSessionId = session.id,
            messages = listOf(ChatMessage(role=MessageRole.USER, content="梳理工作"), ChatMessage(role=MessageRole.ASSISTANT, content="正在整理…", isStreaming=true))))
        compose.setContent { HermesCompanionTheme(ThemeMode.LIGHT, SkinMode.CLEAN) { AmbientBackground {
            HermesScene { ChatScreen(state=current,onEntryHandled={},contentPadding=PaddingValues(),onBack={},onDraftChange={},onAddAttachments={},onRemoveAttachment={},
                onSend={},onRetryFailed={},onStop={},onSteer={},onQueue={},onCancelQueued={},onRespondRequest={_,_->},onVoiceConversation={},
                onStartVoiceInput={},onStopVoiceInput={},onCancelVoiceInput={},onVoiceSystemResult={},onVoiceUnavailable={},onLoadModels={},onSwitchModel={_,_->},
                onLoadCommandCatalog={},onSetCouncilMode={},onOpenArtifact={},onOpenWorkspace={},onOpenImage={_,_->},onOpenLink={},onLoadInlineImages={},onLoadOlderMessages={},onScrollPositionChange={_,_,_->}) }
        } } }
        compose.onNodeWithTag("mascot_WORKING").assertExists()
        val completion = RunCompletionSummary(session.id, session.title, "已完成", completedAtMillis = 1000)
        if (beforeStop) {
            compose.runOnIdle { current = current.copy(latestCompletion = completion) }
            compose.onNodeWithTag("mascot_DONE").assertDoesNotExist()
            compose.runOnIdle { current = current.copy(isStreaming = false) }
        } else {
            compose.runOnIdle { current = current.copy(isStreaming = false) }
            compose.onNodeWithTag("mascot_DONE").assertDoesNotExist()
            compose.runOnIdle { current = current.copy(latestCompletion = completion) }
        }
        compose.onNodeWithTag("mascot_DONE").assertExists()
    }
    @Test fun completionEventBeforeStreamStopStillCelebrates() = completionOrder(true)
    @Test fun completionEventAfterStreamStopCelebratesOnlyWhenConfirmed() = completionOrder(false)

}

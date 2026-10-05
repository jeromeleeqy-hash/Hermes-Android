package com.qingyu.hermescompanion.ui.screen

import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.*
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.qingyu.hermescompanion.i18n.*
import com.qingyu.hermescompanion.model.*
import com.qingyu.hermescompanion.today.*
import com.qingyu.hermescompanion.ui.*
import com.qingyu.hermescompanion.ui.component.AmbientBackground
import com.qingyu.hermescompanion.ui.component.HermesScene
import com.qingyu.hermescompanion.ui.theme.HermesCompanionTheme
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.*
import org.robolectric.annotation.*
import java.io.File
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "zh-rCN-w390dp-h844dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class Hermes387UiTest {
    @get:Rule val compose = createComposeRule()
    @Before fun language() { AppLanguage.setMode(RuntimeEnvironment.getApplication(), AppLanguageMode.CHINESE) }
    private fun state(): AppUiState {
        val raw = File("src/main/assets/hermes-today-examples.json").readText()
        return AppUiState(reduceMotion = true, homeMode = HomeMode.DEEP,
            today = TodayState(profile = "default", root = "/work", rootVerified = true, loaded = true,
                fileExists = true, rawJson = raw, board = TodayBoard.decode(raw, "/work")))
    }

    @OptIn(ExperimentalComposeUiApi::class)
    private fun screenshot(name: String, tag: String) = compose.runOnIdle {
        val view = (compose.onNodeWithTag(tag).fetchSemanticsNode().root as ViewRootForTest).view
        val output = File("build/387-previews/$name.png"); output.parentFile.mkdirs()
        Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888).let { bitmap ->
            view.draw(android.graphics.Canvas(bitmap))
            output.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    @Test fun renameOpensPrefilledInputAndSavesExactlyWhatTheUserEnters() {
        val session = HermesSession("rename", "原对话名称", messageCount = 4)
        var saved: Pair<HermesSession, String>? = null
        compose.setContent { HermesCompanionTheme(ThemeMode.LIGHT, SkinMode.GLASS) { AmbientBackground { HermesScene {
            SessionsScreen(AppUiState(sessions = listOf(session), reduceMotion = true), PaddingValues(),
                {}, {}, {}, {}, {}, {}, { target, name -> saved = target to name }, {}, {}, { _, _ -> }, {}, { _, _ -> }, {}, {}, {}, {})
        } } } }
        compose.onNodeWithTag("session_row_rename").performTouchInput { longClick() }
        compose.onNodeWithText("AI 重命名").assertDoesNotExist()
        compose.onNodeWithText("重命名").performClick()
        compose.onNodeWithTag("session-name-input").assertTextContains(session.title)
        assertNull(saved)
        compose.onNodeWithTag("session-name-input").performTextReplacement("   ")
        compose.onNodeWithTag("session-name-save").assertIsNotEnabled()
        val title = "十月份复盘：第二阶段计划与后续安排。"
        compose.onNodeWithTag("session-name-input").performTextReplacement(title)
        screenshot("manual-rename", "session-name-input")
        compose.onNodeWithTag("session-name-save").performClick()
        assertEquals(session to title, saved)
        compose.onNodeWithTag("session-name-input").assertDoesNotExist()
    }

    @Test fun homeHeaderUsesTheThreeSkinsAndRemovesSuccessfulProcessingStatus() {
        var skin by mutableStateOf(SkinMode.GLASS)
        var current by mutableStateOf(state())
        var opened = 0
        compose.setContent { HermesCompanionTheme(ThemeMode.LIGHT, skin) { AmbientBackground { HermesScene {
            TodayOverviewScreen(current.copy(skinMode = skin), PaddingValues(), {}, {}, { _, _ -> }, { _, _ -> }, {}, {}, {}, { _, _ -> }, {},
                onOpenRefresh = { opened++ })
        } } } }
        for (value in SkinMode.entries) {
            compose.runOnIdle { skin = value }
            compose.onNodeWithTag("today-briefing-header").performScrollTo().assertIsDisplayed()
            compose.onNodeWithText("已结合最近进展核对首页").assertDoesNotExist()
            compose.onNodeWithTag("overview-update-status").assertDoesNotExist()
            screenshot("home-${value.name.lowercase()}", "today_home")
        }
        compose.runOnIdle { current = current.copy(todayRefresh = TodayRefreshState("refresh", "default", "/work", "task", true, "正在读取最新记录")) }
        compose.onNodeWithTag("overview-update-status").performScrollTo().performClick()
        assertEquals(1, opened)
        compose.onNodeWithTag("today-refresh-button").assertIsNotEnabled()
        compose.runOnIdle { current = current.copy(todayRefresh = current.todayRefresh.copy(busy = false, message = "")) }
        compose.onNodeWithTag("overview-update-status").assertDoesNotExist()
        compose.onNodeWithText("查看处理").assertDoesNotExist()
        compose.onNodeWithTag("today-refresh-button").assertIsEnabled()
    }

    @Test @Config(qualifiers = "zh-rCN-w320dp-h640dp-mdpi")
    fun headerActionsStaySingleLineWithLargeTextAndKeepConfirmationAccessible() {
        val request = AgentRequest("approve", "runtime", "task", AgentRequestType.APPROVAL, "更新首页", profile = "default")
        var current by mutableStateOf(state().copy(todayRefresh = TodayRefreshState("r", "default", "/work", "task", true, "需要确认"),
            pendingAgentRequests = listOf(request)))
        compose.setContent { CompositionLocalProvider(LocalDensity provides Density(1f, 1.5f)) {
            HermesCompanionTheme(ThemeMode.DARK, SkinMode.CLEAN) {
                Surface(color = MaterialTheme.colorScheme.background) { Column(Modifier.fillMaxSize().padding(18.dp)) {
                    TodayBriefingHeader(current, 12, LocalDate.now(), {}, {}, {})
                } }
            }
        } }
        screenshot("header-small-dark-large-text", "today-briefing-header")
        listOf("更新中", "查看处理").forEach { label ->
            val layouts = mutableListOf<TextLayoutResult>()
            compose.onNodeWithText(label, useUnmergedTree = true).assertIsDisplayed()
                .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
            assertEquals(1, layouts.single().lineCount)
            val layout = layouts.single()
            assertFalse(layout.isLineEllipsized(0))
            // Intrinsic text widths round to physical pixels; subpixel rounding is not truncation.
            assertTrue("$label clipped: ${layout.size}", layout.getLineRight(0) <= layout.size.width + 1f)
        }
        compose.onNodeWithText("需要你确认", useUnmergedTree = true).assertIsDisplayed()
        compose.runOnIdle { current = current.copy(pendingAgentRequests = emptyList(), todayRefresh = current.todayRefresh.copy(
            busy = false, message = "部分对话未能读取，首页可能缺少最新进展")) }
        compose.onNodeWithTag("overview-update-status").assertIsDisplayed()
        compose.onNodeWithText("查看处理", useUnmergedTree = true).assertIsDisplayed()
    }
}

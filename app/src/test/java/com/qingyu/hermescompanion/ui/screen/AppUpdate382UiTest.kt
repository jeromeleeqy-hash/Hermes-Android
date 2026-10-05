package com.qingyu.hermescompanion.ui.screen

import android.graphics.Bitmap
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.qingyu.hermescompanion.BuildConfig
import com.qingyu.hermescompanion.i18n.*
import com.qingyu.hermescompanion.today.*
import com.qingyu.hermescompanion.ui.*
import com.qingyu.hermescompanion.ui.component.*
import com.qingyu.hermescompanion.ui.theme.HermesCompanionTheme
import com.qingyu.hermescompanion.update.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "zh-rCN-w390dp-h844dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AppUpdate382UiTest {
    @get:Rule val compose = createComposeRule()
    @Before fun language() { AppLanguage.setMode(RuntimeEnvironment.getApplication(), AppLanguageMode.CHINESE) }
    private fun show(theme: () -> ThemeMode = { ThemeMode.LIGHT }, scale: () -> Float = { 1f }, content: @Composable () -> Unit) {
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, scale()), LocalReduceMotion provides true) {
                HermesCompanionTheme(theme(), SkinMode.CLEAN) { AmbientBackground { HermesScene { content() } } }
            }
        }
    }
    @OptIn(ExperimentalComposeUiApi::class)
    private fun screenshot(name: String, tag: String) = compose.runOnIdle {
        val view = (compose.onNodeWithTag(tag).fetchSemanticsNode().root as ViewRootForTest).view
        val file = File("build/382-previews/$name.png"); file.parentFile.mkdirs()
        Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888).let { b ->
            view.draw(android.graphics.Canvas(b)); file.outputStream().use { b.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }
    @Test fun choosingASceneDoesNotSendAnythingUntilExplicitStart() {
        var sent: String? = null
        var theme by mutableStateOf(ThemeMode.LIGHT)
        var scale by mutableFloatStateOf(1f)
        show(theme = { theme }, scale = { scale }) { TodayScenesSheet({}, { sent = it }) }
        compose.onNodeWithTag("help-start").assertIsNotEnabled()
        compose.onNodeWithTag("help-scene:meeting").performClick()
        assertNull(sent)
        screenshot("help-scenes", "help-scenes")
        compose.onNodeWithTag("help-start").performClick()
        assertTrue(sent!!.contains("先问清是哪次会议"))
        compose.runOnIdle { theme = ThemeMode.DARK; scale = 1.5f }
        compose.onNodeWithTag("help-start").assertIsDisplayed()
        compose.onNodeWithTag("help-scenes").performScrollToNode(hasTestTag("help-context"))
        screenshot("help-large-dark", "help-scenes")
    }
    @Test fun freeTextCanStartWithoutChoosingACategory() {
        var sent: String? = null
        show { TodayScenesSheet({}, { sent = it }) }
        compose.onNodeWithTag("help-scenes").performScrollToNode(hasTestTag("help-context"))
        compose.onNodeWithTag("help-context").performTextInput("想一起梳理本周安排")
        compose.onNodeWithTag("help-start").performClick()
        assertEquals("想一起梳理本周安排", sent)
    }
    @Test fun homeUsesTheHalfPortraitAndOpensHelp() {
        val board = TodayBoard.decode(File("src/main/assets/hermes-today-examples.json").readText(), "/work")
        val state = AppUiState(username = "Jerome", reduceMotion = true,
            today = TodayState(profile = "default", root = "/work", loaded = true, fileExists = true, board = board))
        show { TodayOverviewScreen(state, PaddingValues(bottom = 20.dp), {}, {}, { _, _ -> }, { _, _ -> }, {}, {}, {}, { _, _ -> }, {}) }
        compose.onNodeWithTag("mascot_IDLE_HALF").assertIsDisplayed()
        screenshot("home-half-portrait", "today_home")
        compose.onNodeWithTag("home-help").performClick()
        compose.onNodeWithText("想一起完成什么？").assertIsDisplayed()
    }
    @Test fun updateRequiresExplicitDownloadAndInstallationIsSeparate() {
        val release = AppRelease(BuildConfig.APPLICATION_ID, 999, "9.9.9", "https://download.leaier.com/releases/demo.apk",
            "a".repeat(64), 65_000_000, 26, listOf("示例：改善阅读体验", "示例：优化首页布局"))
        var state by mutableStateOf(AppUpdateState(visible = true, phase = AppUpdatePhase.AVAILABLE, release = release))
        var downloaded = false; var installed = false; var closed = false
        show { AppUpdateContent(state, { closed = true }, {}, { downloaded = true }, {}, { installed = true }, {}, {}) }
        assertFalse(downloaded)
        screenshot("update-available-demo", "app-update-content")
        compose.onNodeWithText("下载更新").performClick()
        assertTrue(downloaded); assertFalse(installed)
        compose.runOnIdle { state = state.copy(phase = AppUpdatePhase.READY) }
        compose.onNodeWithText("安装更新").performClick()
        assertTrue(installed)
        compose.onNodeWithText("关闭", useUnmergedTree = true).performClick()
        assertTrue(closed)
    }
    @Test fun downloadAndVerificationNeverOfferPrematureInstall() {
        var state by mutableStateOf(AppUpdateState(visible = true, phase = AppUpdatePhase.VERIFYING))
        var rechecks = 0
        show { AppUpdateContent(state, {}, { rechecks++ }, {}, {}, {}, {}, {}) }
        compose.onNodeWithText("安装更新").assertDoesNotExist()
        compose.onNodeWithText("关闭").assertIsDisplayed()
        compose.runOnIdle { state = state.copy(phase = AppUpdatePhase.FAILED, error = UpdateError.SIGNATURE,
            release = AppRelease(BuildConfig.APPLICATION_ID, 999, "9.9.9", "https://download.leaier.com/releases/demo.apk",
                "a".repeat(64), 65_000_000, 26, emptyList())) }
        compose.onNodeWithText("安装更新").assertDoesNotExist()
        compose.onNodeWithText("安装包签名与当前 App 不一致，已阻止安装。").assertIsDisplayed()
        compose.onNodeWithText("重新检查版本").performClick()
        assertEquals(1, rechecks)
    }
}

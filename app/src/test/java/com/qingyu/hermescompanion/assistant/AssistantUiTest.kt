package com.qingyu.hermescompanion.assistant

import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.*
import com.qingyu.hermescompanion.model.*
import com.qingyu.hermescompanion.ui.*
import com.qingyu.hermescompanion.ui.component.*
import com.qingyu.hermescompanion.ui.screen.*
import com.qingyu.hermescompanion.ui.theme.HermesCompanionTheme
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@OptIn(ExperimentalComposeUiApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "zh-rCN-w320dp-h740dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AssistantUiTest {
    @get:Rule val compose = createComposeRule()
    private val state = AppUiState(route = AppRoute.HOME, username = "Jerome", sessions = listOf(HermesSession("project", "本周内容复盘", preview = "下一批视频的三个调整方向")))
    private fun capture(name: String, node: SemanticsNodeInteraction = compose.onRoot()) {
        val view = (node.fetchSemanticsNode().root as ViewRootForTest).view
        compose.runOnIdle {
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(android.graphics.Canvas(bitmap))
            val out = File("build/ui-validation/$name.png"); out.parentFile.mkdirs()
            out.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }
    private fun home(dark: Boolean = false, scale: Float = 1f, opening: Boolean = false, onDaily: () -> Unit = {}) {
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, scale)) {
                HermesCompanionTheme(if (dark) ThemeMode.DARK else ThemeMode.LIGHT, SkinMode.CLEAN) {
                    Scaffold(contentWindowInsets = WindowInsets(0,0,0,0), bottomBar = { ReferenceBottomDock(AppRoute.HOME, false, {}) }) { padding ->
                        AssistantHomeScreen(state.copy(isDailyOpening = opening), padding, {}, {}, {}, {}, {}, { _, _ -> }, {}, onDaily)
                    }
                }
            }
        }
    }
    @Test fun narrowHomeHasOneDailyEntryAndPreservesPortrait() {
        var calls = 0
        home(onDaily = { calls++ })
        compose.onNodeWithText("跟我说").assertIsDisplayed().performClick()
        assertEquals(1, calls)
        compose.onNodeWithTag("home_hermes_portrait").assertIsDisplayed()
        compose.onNodeWithText("安排今天").assertDoesNotExist()
        compose.onNodeWithText("收下灵感").assertDoesNotExist()
        capture("daily-home-320")
    }
    @Test fun largeTextDarkHomeKeepsDailyEntryAndRecentConversationReachable() {
        home(dark = true, scale = 1.3f)
        compose.onNodeWithText("跟我说").assertIsDisplayed()
        capture("daily-home-dark-large-320")
        compose.onNodeWithText("本周内容复盘").performScrollTo().assertIsDisplayed()
    }
    @Test fun openingStatePreventsDoubleTap() {
        var calls = 0
        home(opening = true, onDaily = { calls++ })
        compose.onNodeWithTag("daily_conversation_entry").assertIsNotEnabled().performClick()
        assertEquals(0, calls)
    }
    @Test fun sharingDefaultsToDailyWithoutClassificationAndCanChooseExistingChat() {
        var target: String? = null
        compose.setContent { HermesCompanionTheme(ThemeMode.LIGHT, SkinMode.CLEAN) {
            ShareToHermesDialog(state.copy(incomingShare = IncomingShare(sharedText = "https://example.com/video")), {}, {}, { target = it }, {})
        } }
        compose.onNodeWithText("发送到：日常助理").assertIsDisplayed()
        compose.onNodeWithText("先收下").assertDoesNotExist()
        compose.onNodeWithText("本周内容复盘").assertDoesNotExist()
        capture("daily-share-320", compose.onNodeWithText("分享到 Hermes"))
        compose.onNodeWithText("发送").performClick(); assertEquals(DailyConversation.SHARE_TARGET, target)
        compose.onNodeWithText("换个对话").performClick()
        compose.onNodeWithText("本周内容复盘").performScrollTo().performClick()
        compose.onNodeWithText("发送").performClick(); assertEquals("project", target)
    }
}

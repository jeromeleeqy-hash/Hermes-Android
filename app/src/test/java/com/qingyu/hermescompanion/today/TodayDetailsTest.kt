package com.qingyu.hermescompanion.today

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import com.qingyu.hermescompanion.i18n.*
import com.qingyu.hermescompanion.ui.*
import com.qingyu.hermescompanion.ui.screen.TodayCardDetailSheet
import com.qingyu.hermescompanion.ui.theme.HermesCompanionTheme
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
class TodayDetailsTest {
    @get:Rule val compose = createComposeRule()
    private val longBackground = "9/28 的看场地记录和 9/29 的确定场地记录，是否属于同一事项尚未确认。旧清单仍保留先前状态，应等用户补充后再更新。".repeat(8)
    private val legacy = TodayCard("venue", "活动场地已定，旧的看场地事项是否可以结束", longBackground, TodayKind.DECISION, TodayDomain.WORK,
        sources = listOf("/work/notes.md"))
    private val structured = legacy.copy(title = "核对场地事项", summary = "两条场地记录需要确认是否为同一件事。",
        presentation = TodayPresentation(intent = "clarify", question = "这两条记录是同一件事吗？",
            facts = listOf("9/28：记录了看场地", "9/29：记录了确定场地", "旧清单尚未确认是否可结束"),
            options = listOf(TodayOption("是同一件事"), TodayOption("不是，继续保留"), TodayOption("我补充一下")), background = longBackground))
    @Before fun language() { AppLanguage.setMode(RuntimeEnvironment.getApplication(), AppLanguageMode.CHINESE) }

    private fun show(card: TodayCard, fontScale: Float = 1f, action: (String) -> Unit = {}) {
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale)) {
                HermesCompanionTheme(ThemeMode.LIGHT, SkinMode.CLEAN) { TodayCardDetailSheet(card, {}, {}, action) }
            }
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    private fun screenshot(name: String) = compose.runOnIdle {
        val view = (compose.onNodeWithTag("today-detail").fetchSemanticsNode().root as ViewRootForTest).view
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        val file = File("build/today-previews/$name.png"); file.parentFile.mkdirs()
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun oldLongProseIsCollapsedVerbatimAndActionsStayVisible() {
        show(legacy)
        compose.onNodeWithTag("today-background").assertDoesNotExist()
        compose.onNodeWithText("notes.md").assertDoesNotExist()
        compose.onNodeWithTag("today-detail-discuss").assertIsDisplayed()
        screenshot("detail-legacy-collapsed")
        compose.onNodeWithTag("today-background-toggle").performScrollTo().performClick()
        compose.onNodeWithTag("today-background").assertTextEquals(longBackground)
        compose.onNodeWithTag("today-detail-discuss").assertIsDisplayed()
        compose.onNodeWithTag("today-sources-toggle").performScrollTo().performClick()
        compose.onNodeWithText("notes.md").performScrollTo().assertIsDisplayed()
    }

    @Test fun aClarificationHasFactsAndOptionsAndDoesNotMarkAnythingDone() {
        var selected = ""
        show(structured, action = { selected = it })
        compose.onNodeWithText("工作 · 待收尾").assertIsDisplayed()
        compose.onNodeWithText("这两条记录是同一件事吗？").assertIsDisplayed()
        compose.onNodeWithText(structured.summary).assertIsDisplayed()
        compose.onNodeWithText("9/28：记录了看场地").assertDoesNotExist()
        compose.onNodeWithTag("today-background").assertDoesNotExist()
        screenshot("detail-structured")
        compose.onNodeWithTag("today-quick-response").performScrollTo().performClick()
        compose.onNodeWithText("是同一件事").performScrollTo().performClick()
        assertEquals("option:0", selected)
        assertEquals("open", structured.status)
    }

    @Test @Config(qualifiers = "zh-rCN-w360dp-h640dp-mdpi")
    fun compactPhoneAndLargeTypeKeepActionsReachable() {
        var selected = ""
        show(structured, fontScale = 1.4f, action = { selected = it })
        compose.onNodeWithTag("today-detail-discuss").assertIsDisplayed()
        screenshot("detail-large-font")
        compose.onNodeWithTag("today-quick-response").performScrollTo().performClick()
        compose.onNodeWithText("我补充一下").performScrollTo().performClick()
        assertEquals("option:2", selected)
        compose.onNodeWithTag("today-detail-discuss").performClick()
        assertEquals("discuss", selected)
    }
    @Test fun repeatedEdgeFlingsDoNotMoveTheFixedActionsOrViewport() {
        show(structured)
        val actions = compose.onNodeWithTag("today-detail-discuss").getUnclippedBoundsInRoot()
        val viewport = compose.onNodeWithTag("today-detail-window").getUnclippedBoundsInRoot()
        repeat(4) {
            compose.onNodeWithTag("today-detail-scroll").performTouchInput { swipeUp(durationMillis = 100) }
            compose.onNodeWithTag("today-detail-scroll").performTouchInput { swipeDown(durationMillis = 100) }
        }
        compose.mainClock.autoAdvance = false
        repeat(60) {
            compose.mainClock.advanceTimeByFrame()
            assertEquals(actions, compose.onNodeWithTag("today-detail-discuss").getUnclippedBoundsInRoot())
            assertEquals(viewport, compose.onNodeWithTag("today-detail-window").getUnclippedBoundsInRoot())
        }
        compose.mainClock.autoAdvance = true
        compose.onNodeWithTag("today-detail-close").assertIsDisplayed()
    }
}

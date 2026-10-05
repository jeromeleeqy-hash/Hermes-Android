package com.qingyu.hermescompanion.today

import android.graphics.Bitmap
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.qingyu.hermescompanion.i18n.*
import com.qingyu.hermescompanion.ui.*
import com.qingyu.hermescompanion.ui.component.*
import com.qingyu.hermescompanion.ui.screen.TodayOverviewScreen
import com.qingyu.hermescompanion.ui.theme.HermesCompanionTheme
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35], qualifiers="zh-rCN-w390dp-h844dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TodayUiTest {
    @get:Rule val compose = createComposeRule()
    private val cards = listOf(
        TodayCard("decide", "下周方案评审，先读材料还是现场梳理？", "这次评审要比较两套方案。参与者是否能提前阅读尚未确认，需要先确定讨论方式，才能安排准备工作。", TodayKind.DECISION,
            presentation = TodayPresentation(options = listOf(TodayOption("方案 A", "提前阅读，集中讨论"), TodayOption("方案 B", "现场梳理，共同定稿")))),
        TodayCard("follow", "研究资料整理", "只统计已明确记录的步骤。", TodayKind.FOLLOWUP, TodayDomain.LEARNING,
            presentation = TodayPresentation(steps = listOf(TodayStep("收集材料", true), TodayStep("标注来源", true), TodayStep("比较证据", false)))),
        TodayCard("plan", "周末的生活安排", "核对日历中的预约。", TodayKind.SCHEDULE, TodayDomain.LIFE, whenLabel = "周六 · 10:30"),
        TodayCard("update", "新整理的资料", "这些是演示记录。", TodayKind.UPDATE),
        TodayCard("more", "第五件事", "只在展开后显示。", TodayKind.UPDATE),
    )
    private val fixture = AppUiState(route = AppRoute.HOME, username = "Jerome", reduceMotion = true,
        today = TodayState(profile = "default", root = "/work", loaded = true, fileExists = true,
            board = TodayBoard(LocalDate.parse("2026-10-03"), OffsetDateTime.parse("2026-10-03T09:00:00+08:00"), "今天，先关注这几件事", "完整摘要只出现在详情里。", cards),
            library = listOf(TodayLibraryEntry("工作", "/work/work", true, 12), TodayLibraryEntry("学习", "/work/learning", true, 4),
                TodayLibraryEntry("人物", "/work/people", true, 3), TodayLibraryEntry("生活", "/work/life", true, null),
                TodayLibraryEntry("偏好", "/work/preferences", true, 2), TodayLibraryEntry("时间线", "/work/history", true, 8))))
    @Before fun language() { AppLanguage.setMode(RuntimeEnvironment.getApplication(), AppLanguageMode.CHINESE) }
    @After fun reset() { AppLanguage.setMode(RuntimeEnvironment.getApplication(), AppLanguageMode.CHINESE) }
    private fun content(skin: () -> SkinMode = { SkinMode.CLEAN }, action: (String, String) -> Unit = { _, _ -> }, path: (String, Boolean) -> Unit = { _, _ -> }, theme: () -> ThemeMode = { ThemeMode.LIGHT }, scale: Float = 1f) {
        compose.setContent {
            CompositionLocalProvider(LocalHomeClock provides { ZonedDateTime.parse("2026-10-03T10:00:00+08:00") }, LocalDensity provides Density(LocalDensity.current.density, scale)) {
                HermesCompanionTheme(theme(), skin()) { AmbientBackground { HermesScene {
                    TodayOverviewScreen(fixture, PaddingValues(), {}, {}, path, action, {}, {}, {}, { _, _ -> }, {})
                } } }
            }
        }
    }
    @OptIn(ExperimentalComposeUiApi::class)
    private fun screenshot(name: String) = compose.runOnIdle {
        val file = File("build/today-previews/$name.png"); file.parentFile.mkdirs()
        val view = (compose.onRoot().fetchSemanticsNode().root as ViewRootForTest).view
        Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888).let { bitmap ->
            view.draw(android.graphics.Canvas(bitmap))
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }
    @Test fun optionOpensContextDraftWithoutMarkingComplete() {
        var action: Pair<String, String>? = null
        content(action = { id, value -> action = id to value })
        compose.onNodeWithTag("today_home").performScrollToNode(hasTestTag("today-card:decide"))
        compose.onNodeWithText("这次评审要比较两套方案。参与者是否能提前阅读尚未确认，需要先确定讨论方式，才能安排准备工作。").assertIsDisplayed()
        compose.onNodeWithText("方案 A").assertDoesNotExist()
        compose.onNodeWithTag("today-card:decide").performClick()
        compose.onNodeWithTag("today-quick-response").performScrollTo().performClick()
        compose.onNodeWithText("方案 A").performScrollTo().performClick()
        assertEquals("decide" to "option:0", action)
        assertEquals("open", fixture.today.board!!.cards.first().status)
        compose.onNodeWithText("完整摘要只出现在详情里。").assertDoesNotExist()
        compose.onNodeWithTag("today-card:more").assertDoesNotExist()
    }
    @Test fun homeHasNoDuplicateNavigationAndDetailsLiveInMenu() {
        content()
        compose.onNodeWithTag("today-memory-tab").assertDoesNotExist()
        compose.onNodeWithText("资料与记忆").assertDoesNotExist()
        compose.onNodeWithText("接着上次的事").assertDoesNotExist()
        compose.onNodeWithText("今天，先关注这几件事").assertDoesNotExist()
        compose.onNodeWithTag("today-gallery-open").assertDoesNotExist()
        compose.onNodeWithTag("today-settings").performClick()
        compose.onNodeWithTag("today-maintenance").performScrollTo().performClick()
        compose.onNodeWithText("同步与读取详情").performScrollTo().performClick()
        compose.onNodeWithText("完整摘要只出现在详情里。").assertIsDisplayed()
    }
    @Test fun screensRenderAndScrollAcrossAllSkins() {
        var skin by mutableStateOf(SkinMode.CLEAN)
        var theme by mutableStateOf(ThemeMode.LIGHT)
        content(skin = { skin }, theme = { theme })
        SkinMode.entries.forEach { selected ->
            compose.runOnIdle { skin = selected }
            compose.onNodeWithTag("today_home").performScrollToIndex(0)
            screenshot("today-${selected.name.lowercase()}")
            compose.runOnIdle { theme = ThemeMode.DARK }
            screenshot("today-dark-${selected.name.lowercase()}")
            compose.runOnIdle { theme = ThemeMode.LIGHT }
            compose.onNodeWithTag("today_home").performScrollToNode(hasText("查看其余 1 件"))
            compose.onNodeWithText("查看其余 1 件").performClick()
            compose.onNodeWithTag("today_home").performScrollToNode(hasTestTag("today-card:more"))
            compose.onNodeWithTag("today-card:more").assertIsDisplayed()
            compose.onNodeWithTag("today_home").performScrollToNode(hasText("收起"))
            compose.onNodeWithText("收起").performClick()
            compose.onNodeWithTag("today_home").performScrollToNode(hasTestTag("today-card:follow"))
            screenshot("steps-${selected.name.lowercase()}")
            compose.runOnIdle { AppLanguage.setMode(RuntimeEnvironment.getApplication(), AppLanguageMode.ENGLISH) }
            compose.onNodeWithTag("today_home").performScrollToIndex(0)
            compose.runOnIdle { AppLanguage.setMode(RuntimeEnvironment.getApplication(), AppLanguageMode.CHINESE) }
        }
    }
    @Test fun smallScreenWithLargeTextKeepsMenuAndCardActionsReachable() {
        RuntimeEnvironment.setQualifiers("zh-rCN-w360dp-h640dp-mdpi")
        content(skin = { SkinMode.GLASS }, theme = { ThemeMode.DARK }, scale = 1.4f)
        compose.onNodeWithTag("today_home").performScrollToNode(hasTestTag("today-card:decide"))
        screenshot("today-large-glass")
        compose.onNodeWithTag("today-card:decide").performClick()
        compose.onNodeWithTag("today-quick-response").performScrollTo().performClick()
        compose.onNodeWithText("方案 A").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("today-detail-close").performClick()
        compose.onNodeWithTag("today_home").performScrollToIndex(0)
        compose.onNodeWithTag("today-settings").performClick()
        compose.onNodeWithTag("today-maintenance").performScrollTo().performClick()
        compose.onNodeWithText("同步与读取详情").performScrollTo().performClick()
        compose.onNodeWithText("完整摘要只出现在详情里。").assertIsDisplayed()
    }
}

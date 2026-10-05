package com.qingyu.hermescompanion.ui.screen

import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import com.qingyu.hermescompanion.i18n.*
import com.qingyu.hermescompanion.today.*
import com.qingyu.hermescompanion.ui.*
import com.qingyu.hermescompanion.ui.component.*
import com.qingyu.hermescompanion.ui.theme.HermesCompanionTheme
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.*
import org.robolectric.annotation.*
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w390dp-h844dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class Today383UiTest {
    @get:Rule val compose = createComposeRule()
    @Before fun language() { AppLanguage.setMode(RuntimeEnvironment.getApplication(), AppLanguageMode.CHINESE) }
    @OptIn(ExperimentalComposeUiApi::class)
    private fun screenshot(name: String, tag: String) = compose.runOnIdle {
        val view = (compose.onNodeWithTag(tag).fetchSemanticsNode().root as ViewRootForTest).view
        val file = File("build/383-previews/$name.png"); file.parentFile.mkdirs()
        Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888).let { bitmap ->
            view.draw(android.graphics.Canvas(bitmap)); file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) }
        }
    }
    @Test fun noticeAndGlassDockHaveIdenticalVisibleWidth() {
        compose.setContent { HermesCompanionTheme(ThemeMode.LIGHT, SkinMode.GLASS) {
            Column { AppNotice("原处理对话已不可用，操作记录仍保留", true)
                ReferenceBottomDock(AppRoute.HOME, false, {}, WindowInsets(0,0,0,0)) }
        } }
        val notice = compose.onNodeWithTag("app_notice").fetchSemanticsNode().boundsInRoot
        val dock = compose.onNodeWithTag("floating_bottom_dock").fetchSemanticsNode().boundsInRoot
        assertEquals(dock.left, notice.left, 0.5f); assertEquals(dock.right, notice.right, 0.5f)
        screenshot("notice-width", "app_notice")
    }
    @Test fun uncertainResultOpensOperationDetailsAndContinueIsVisible() {
        val board = TodayBoard.decode(File("src/main/assets/hermes-today-examples.json").readText(), "/work")
        val card = board.cards.first()
        val action = TodayActionState("original", card.id, "default", "/work", "{}", "missing", "uncertain", "原处理对话已不可用，原操作记录仍保留。")
        val state = AppUiState(reduceMotion=true, today=TodayState(profile="default",root="/work",rootVerified=true,board=board), todayActions=mapOf(action.key to action))
        var called = ""
        compose.setContent { HermesCompanionTheme(ThemeMode.LIGHT, SkinMode.CLEAN) {
            TodayOverviewScreen(state, PaddingValues(), {}, {}, {_,_->}, {_,value->called=value}, {}, {}, {}, {_,_->}, {})
        } }
        compose.onNodeWithText("结果待核对").performClick()
        compose.onNodeWithTag("today-operation-details").assertIsDisplayed()
        assertEquals("check", called)
        compose.onNodeWithTag("operation-continue").performScrollTo().performClick()
        assertEquals("recover", called)
        screenshot("operation-details", "today-operation-details")
    }
    @Test fun refreshButtonStartsProgressReconciliationNotReadOnlySync() {
        var synced=0; var updated=0
        val state=AppUiState(reduceMotion=true,today=TodayState(profile="default",root="/work",rootVerified=true))
        compose.setContent { HermesCompanionTheme(ThemeMode.LIGHT, SkinMode.CLEAN) {
            TodayOverviewScreen(state, PaddingValues(), {synced++}, {}, {_,_->}, {_,_->}, {}, {}, {}, {_,_->}, {}, onUpdate={updated++})
        } }
        compose.onNodeWithContentDescription("核对最新进展并更新首页").performClick()
        assertEquals(1,updated); assertEquals(0,synced)
    }
    @Test fun recoveryMessageAndButtonsRemainReachableWithLargeDarkText() {
        val action=TodayActionState("original","card","default","/work","{}","missing","uncertain", "服务器暂未确认原任务是否结束，操作已保留。请稍后核对，不会重复执行。")
        var continued=false
        compose.setContent { CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 1.5f)) {
            HermesCompanionTheme(ThemeMode.DARK,SkinMode.GLASS) { TodayOperationDialog(action,"跟进两项工作安排",emptyList(),{},{},{continued=true},{},{_,_->}) }
        } }
        compose.onNodeWithTag("operation-continue").performScrollTo().performClick()
        assertTrue(continued)
        screenshot("operation-dark-large", "today-operation-details")
    }
}

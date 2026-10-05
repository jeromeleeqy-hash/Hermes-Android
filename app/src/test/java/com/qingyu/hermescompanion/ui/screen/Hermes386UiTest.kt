package com.qingyu.hermescompanion.ui.screen

import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import com.qingyu.hermescompanion.i18n.*
import com.qingyu.hermescompanion.model.*
import com.qingyu.hermescompanion.today.*
import com.qingyu.hermescompanion.ui.*
import com.qingyu.hermescompanion.ui.theme.HermesCompanionTheme
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.*
import org.robolectric.annotation.*
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "zh-rCN-w390dp-h844dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class Hermes386UiTest {
    @get:Rule val compose = createComposeRule()
    @Before fun language() { AppLanguage.setMode(RuntimeEnvironment.getApplication(), AppLanguageMode.CHINESE) }

    @OptIn(ExperimentalComposeUiApi::class)
    private fun screenshot(name: String, tag: String) = compose.runOnIdle {
        val view = (compose.onNodeWithTag(tag).fetchSemanticsNode().root as ViewRootForTest).view
        val file = File("build/386-previews/$name.png"); file.parentFile.mkdirs()
        Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888).let { bitmap ->
            view.draw(android.graphics.Canvas(bitmap)); file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    @Test fun bothDirectionsWorkInEverySkinWithoutStartingAgentWork() {
        var state by mutableStateOf(AppUiState(reduceMotion = true))
        var generated = 0
        var scheduled = 0
        var daily = 0
        compose.setContent { HermesCompanionTheme(ThemeMode.LIGHT, state.skinMode) { Surface {
            HermesHomeScreen(state, PaddingValues(), { state = state.copy(homeMode = it) }, {}, {}, {}, {},
                {}, { generated++ }, { _, _ -> }, { _, _ -> }, { daily++ }, {}, {}, { _, _ -> }, {}, {}, { _, _ -> },
                { _, _, _ -> scheduled++ }, {}, {}, {}, {})
        } } }
        val heroes = listOf("home_hero_warm", "home_hero_glass", "home_hero_quiet")
        SkinMode.entries.forEachIndexed { i, skin ->
            compose.runOnIdle { state = state.copy(skinMode = skin) }
            compose.onNodeWithTag("home_root").assertIsDisplayed()
            compose.onNodeWithTag(heroes[i]).assertIsDisplayed()
            compose.onNodeWithTag("daily_conversation_entry").performClick()
            screenshot("simple-${skin.name.lowercase()}", "home_root")
            compose.onNodeWithTag("today-settings").performClick()
            compose.onNodeWithTag("home-mode-simple").assertIsSelected()
            compose.onNodeWithTag("today-setup-start").assertDoesNotExist()
            if (i == 0) screenshot("home-modes", "today-detail-window")
            compose.onNodeWithTag("home-mode-deep").performScrollTo().performClick()
            compose.onNodeWithTag("today_home").assertIsDisplayed()
            compose.onNodeWithTag("home_root").assertDoesNotExist()
            compose.onNodeWithTag("today-settings").performClick()
            compose.onNodeWithTag("home-mode-deep").assertIsSelected()
            compose.onNodeWithTag("home-mode-simple").performClick()
            compose.onNodeWithTag("home_root").assertIsDisplayed()
            assertEquals(skin, state.skinMode)
        }
        assertEquals(3, daily)
        assertEquals(0, generated); assertEquals(0, scheduled)
    }

    @Test fun simpleRecentChatsHideAppTasksAndCronButKeepHumanChatsAndRequests() {
        val task = HermesSession(id = "task", title = "自动整理", profile = "default")
        val cron = HermesSession(id = "cron", title = "早间任务", profile = "default", source = "cron")
        val human = HermesSession(id = "human", title = "我的主动聊天", profile = "default")
        val request = AgentRequest("r", "runtime", "task", AgentRequestType.APPROVAL, "等待确认操作", profile = "default")
        var opened = ""
        compose.setContent { HermesCompanionTheme(ThemeMode.LIGHT, SkinMode.PAPER) { Surface {
            AssistantHomeScreen(AppUiState(reduceMotion = true, sessions = listOf(task, cron, human),
                taskSessionKeys = setOf(task.scopedId), pendingAgentRequests = listOf(request)), PaddingValues(),
                {}, { opened = it.id }, {}, {}, {}, { _, _ -> }, {})
        } } }
        compose.onNodeWithText("自动整理").assertDoesNotExist()
        compose.onNodeWithText("早间任务").assertDoesNotExist()
        compose.onNodeWithText("等待确认操作").assertExists()
        compose.onNodeWithText("我的主动聊天").performScrollTo().performClick()
        assertEquals("human", opened)
    }

    @Test @Config(qualifiers = "zh-rCN-w320dp-h640dp-mdpi")
    fun modeChoicesStayReachableWithLargeTextAndDeepSetupRemainsAvailable() {
        var selected: HomeMode? = null
        var generations = 0
        compose.setContent { CompositionLocalProvider(LocalDensity provides Density(1f, 1.5f)) {
            HermesCompanionTheme(ThemeMode.DARK, SkinMode.GLASS) {
                HomeSettingsWindow(AppUiState(homeMode = HomeMode.DEEP, today = TodayState(root = "/work", rootVerified = true)),
                    {}, { selected = it }, { generations++ }, {}, {}, {}, { _, _, _ -> }, {})
            }
        } }
        compose.onNodeWithTag("home-mode-deep").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("today-setup-start").performScrollTo().assertIsEnabled()
        compose.onNodeWithTag("home-mode-simple").performScrollTo().performClick()
        assertEquals(HomeMode.SIMPLE, selected)
        assertEquals(0, generations)
        screenshot("home-modes-small-dark", "today-detail-window")
    }
}

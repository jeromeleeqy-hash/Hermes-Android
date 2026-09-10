package com.qingyu.hermescompanion.ui.screen

import android.content.Intent
import android.content.res.Configuration
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.*
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.qingyu.hermescompanion.R
import com.qingyu.hermescompanion.i18n.*
import com.qingyu.hermescompanion.ui.*
import com.qingyu.hermescompanion.ui.component.*
import com.qingyu.hermescompanion.ui.theme.HermesCompanionTheme
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.ZonedDateTime
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35],qualifiers="zh-rCN-w390dp-h844dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class UiRefinement365Test {
    @get:Rule val compose=createComposeRule()
    private val context get()=RuntimeEnvironment.getApplication()
    @Before fun language() { AppLanguage.setMode(context,AppLanguageMode.CHINESE) }
    @After fun resetLanguage() { AppLanguage.setMode(context,AppLanguageMode.CHINESE) }
    private fun quotes(locale:Locale)=context.createConfigurationContext(
        Configuration(context.resources.configuration).apply { setLocale(locale) }
    ).resources.getStringArray(R.array.home_quotes).toList()
    private fun caption()=compose.onNodeWithTag("home_caption").fetchSemanticsNode()
        .config[SemanticsProperties.Text].single().text

    @Test fun bilingualQuotesStayWithinTheChineseLengthBudget() {
        val chinese=quotes(Locale.SIMPLIFIED_CHINESE)
        val english=quotes(Locale.ENGLISH)
        assertTrue(chinese.isNotEmpty())
        assertEquals(chinese.size,english.size)
        assertEquals(chinese.size,chinese.distinct().size)
        assertTrue(chinese.all { it.isNotBlank() && it.codePointCount(0,it.length)<=15 })
        assertTrue(english.all { it.isNotBlank() && it.length<=32 })
    }

    @Test fun dateRollsOverAndQuotesStayStableUntilNextDayOrHomeVisit() {
        var time=ZonedDateTime.parse("2026-09-10T23:58:00+08:00")
        val source:()->ZonedDateTime={time}
        var skin by mutableStateOf(SkinMode.CLEAN)
        var visible by mutableStateOf(true)
        compose.setContent { CompositionLocalProvider(LocalHomeClock provides source) {
            HermesCompanionTheme(ThemeMode.LIGHT,skin) { AmbientBackground { HermesScene {
                if(visible) AssistantHomeScreen(AppUiState(route=AppRoute.HOME,username="admin",homeWelcomed=true,reduceMotion=true),
                    PaddingValues(),{},{},{},{},{},{_,_->},{})
            } } }
        } }
        compose.onNodeWithTag("home_date").assertTextEquals("9月10日 · 星期四")
        val first=caption()
        assertTrue(first in quotes(Locale.SIMPLIFIED_CHINESE))
        val button=compose.onNodeWithTag("daily_conversation_entry").fetchSemanticsNode().boundsInRoot
        compose.runOnIdle { time=time.plusMinutes(1);context.sendBroadcast(Intent(Intent.ACTION_TIME_TICK)) }
        assertEquals(first,caption())
        assertEquals(button,compose.onNodeWithTag("daily_conversation_entry").fetchSemanticsNode().boundsInRoot)
        compose.runOnIdle { skin=SkinMode.GLASS }
        assertEquals(first,caption())
        compose.runOnIdle { time=time.plusMinutes(1);context.sendBroadcast(Intent(Intent.ACTION_TIME_TICK)) }
        compose.onNodeWithTag("home_date").assertTextEquals("9月11日 · 星期五")
        val second=caption()
        assertNotEquals(first,second)
        compose.runOnIdle { AppLanguage.setMode(context,AppLanguageMode.ENGLISH) }
        compose.onNodeWithTag("home_date").assertTextEquals("Fri, Sep 11")
        val english=caption()
        assertEquals(quotes(Locale.SIMPLIFIED_CHINESE).indexOf(second),quotes(Locale.ENGLISH).indexOf(english))
        compose.runOnIdle { visible=false }
        compose.onNodeWithTag("home_caption").assertDoesNotExist()
        compose.runOnIdle { visible=true }
        assertNotEquals(english,caption())
    }
}

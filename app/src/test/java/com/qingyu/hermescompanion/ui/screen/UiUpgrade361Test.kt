package com.qingyu.hermescompanion.ui.screen

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.*
import androidx.test.core.app.ApplicationProvider
import com.qingyu.hermescompanion.i18n.*
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

@OptIn(ExperimentalComposeUiApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35],qualifiers="zh-rCN-w390dp-h844dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class UiUpgrade361Test {
    @get:Rule val compose=createComposeRule()
    private val context=ApplicationProvider.getApplicationContext<Context>()
    @After fun resetLanguage() { AppLanguage.setMode(context,AppLanguageMode.SYSTEM) }
    private fun capture(name:String,node:SemanticsNodeInteraction=compose.onRoot()):Bitmap=compose.runOnIdle {
        val view=(node.fetchSemanticsNode().root as ViewRootForTest).view
        val bmp=Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888)
        view.draw(android.graphics.Canvas(bmp))
        File("build/ui-validation/361-$name.png").apply { parentFile.mkdirs() }.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG,100,it) }
        bmp
    }
    private fun home(english:Boolean,scale:Float=1f,compact:Boolean=false) {
        AppLanguage.setMode(context,if(english) AppLanguageMode.ENGLISH else AppLanguageMode.CHINESE)
        val titles=if(english) listOf("Content strategy and weekly review","Yesterday's work report") else listOf("AI 短视频充值记录与本周运营复盘","Looki 日报")
        val state=AppUiState(route=AppRoute.HOME,skinMode=SkinMode.GLASS,username="admin",reduceMotion=true,homeWelcomed=true,
            sessions=titles.mapIndexed { index,title -> HermesSession("item$index",title,if(english) "Review yesterday's results and plan the next steps for the team" else "最近我在用 AI 做短视频运营，花费比较多，每天有几件事需要梳理。",Instant.now().minusSeconds((index+1)*60L).toString()) })
        compose.setContent { CompositionLocalProvider(LocalDensity provides Density(1f,scale),LocalReduceMotion provides true) {
            HermesCompanionTheme(ThemeMode.LIGHT,SkinMode.GLASS) { AmbientBackground {
                Scaffold(containerColor=Color.Transparent,contentWindowInsets=WindowInsets(0,0,0,0),bottomBar={ ReferenceBottomDock(AppRoute.HOME,false,{},WindowInsets(0,0,0,24)) }) { padding ->
                    HermesScene { AssistantHomeScreen(state,padding,{},{},{},{},{},{_,_->},{}) }
                }
            } }
        } }
        compose.onNodeWithTag("daily_conversation_entry").assertIsDisplayed()
        val dock=compose.onNodeWithTag("floating_bottom_dock").fetchSemanticsNode().boundsInRoot
        if(!compact) {
            compose.onNodeWithText(titles.last()).assertIsDisplayed()
            val recent=compose.onNodeWithTag("home_recent").fetchSemanticsNode().boundsInRoot
            assertTrue("Two complete recent rows must fit above dock: $recent / $dock",recent.bottom<=dock.top)
        } else compose.onNodeWithText(titles.last()).performScrollTo().assertIsDisplayed()
        capture("home-${if(english) "en" else "zh"}${if(compact) "-small-large-text" else ""}")
    }
    @Test fun glassHomeFitsTwoChineseConversations()=home(false)
    @Test fun glassHomeFitsTwoEnglishConversations()=home(true)
    @Test @Config(qualifiers="zh-rCN-w320dp-h640dp-mdpi") fun largeTextRemainsScrollableWithoutHidingActions()=home(true,1.3f,true)

    @Test fun footerCoversScrollingContentThroughBothNavigationInsets() {
        var route by mutableStateOf(AppRoute.HOME)
        var inset by mutableStateOf(24)
        var fill by mutableStateOf(Color.Red)
        compose.setContent { HermesCompanionTheme(ThemeMode.LIGHT,SkinMode.GLASS) { AmbientBackground {
            Box(Modifier.fillMaxSize()) {
                HermesScene(contentBottomClip = (inset+8).dp) { Box(Modifier.fillMaxSize().background(fill)) }
                Box(Modifier.align(Alignment.BottomCenter)) { ReferenceBottomDock(route,false,{},WindowInsets(0,0,0,inset)) }
            }
        } } }
        for (next in listOf(AppRoute.HOME,AppRoute.SESSIONS,AppRoute.TASKS,AppRoute.WORKSPACE,AppRoute.SETTINGS)) {
            compose.runOnIdle { route=next;inset=if(next==AppRoute.TASKS)48 else 24;fill=if(next==AppRoute.SESSIONS)Color.Blue else Color.Red }
            val footer=compose.onNodeWithTag("dock_safe_area").fetchSemanticsNode().boundsInRoot
            val bitmap=capture("footer-${next.name}")
            compose.runOnIdle { fill = Color.Transparent }
            val baseline=capture("footer-${next.name}-background")
            assertEquals((inset+8).toFloat(),footer.height,.5f)
            for (x in listOf(2,bitmap.width/2,bitmap.width-3)) {
                val y = bitmap.height - 3
                assertEquals("Footer reveals the original gradient, never route content",baseline.getPixel(x,y),bitmap.getPixel(x,y))
            }
            assertNotEquals("Route content is still visible above the footer", baseline.getPixel(3,200), bitmap.getPixel(3,200))
        }
    }
    private fun voice(mode:SkinMode,english:Boolean,dark:Boolean=false) {
        AppLanguage.setMode(context,if(english)AppLanguageMode.ENGLISH else AppLanguageMode.CHINESE)
        var preferences by mutableStateOf(VoicePreferences())
        compose.setContent { CompositionLocalProvider(LocalReduceMotion provides true) {
            HermesCompanionTheme(if(dark)ThemeMode.DARK else ThemeMode.LIGHT,mode) { AmbientBackground { HermesScene {
                VoiceSettingsScreen(AppUiState(skinMode=mode,voicePreferences=preferences),PaddingValues(),{}, {preferences=it},{},{},{},{},{},{})
            } } }
        } }
        val label=if(english) "Enable voice" else "启用语音功能"
        compose.onNodeWithText(label).assertIsOn().performClick().assertIsOff()
        assertFalse(preferences.enabled)
        compose.onNodeWithText(label).performClick().assertIsOn()
        val caption=if(english) "Listen again after read-aloud ends; tap to interrupt at any time" else "朗读结束后自动重新聆听；可随时点按打断"
        compose.onNodeWithText(caption).performScrollTo()
        val result=mutableListOf<TextLayoutResult>()
        compose.onNodeWithText(caption,useUnmergedTree=true).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(result) }
        capture("voice-${mode.name}-${if(english) "en" else "zh"}-${if(dark)"dark" else "light"}")
        assertTrue(result.joinToString { "${it.size} paragraph=${it.multiParagraph.width}x${it.multiParagraph.height} lines=${it.lineCount} widthOverflow=${it.didOverflowWidth} heightOverflow=${it.didOverflowHeight}" }, result.none { it.hasVisualOverflow })
    }
    @Test fun glassSettingsEnglish()=voice(SkinMode.GLASS,true)
    @Test fun glassSettingsDarkChinese()=voice(SkinMode.GLASS,false,true)
    @Test fun warmSettingsChinese()=voice(SkinMode.CLEAN,false)
    @Test fun quietSettingsEnglish()=voice(SkinMode.PAPER,true)

    @Test fun languageSheetChangesSelectionWithRadioSemantics() {
        var selected by mutableStateOf(AppLanguageMode.CHINESE)
        var shown by mutableStateOf(true)
        AppLanguage.setMode(context,selected)
        compose.setContent { HermesCompanionTheme(ThemeMode.LIGHT,SkinMode.GLASS) { AmbientBackground {
            if(shown) LanguagePicker(selected,{selected=it;AppLanguage.setMode(context,it)},{shown=false})
        } } }
        compose.onNodeWithTag("language_chinese").assertIsSelected()
        compose.onNodeWithTag("language_english").assertIsNotSelected()
        capture("language-picker",compose.onNodeWithText("English"))
        compose.onNodeWithTag("language_english").performClick()
        assertEquals(AppLanguageMode.ENGLISH,selected);assertFalse(shown)
    }
    @Test fun englishProjectDialogAcceptsDrivePathsFromTheKeyboard() = projectDialog(SkinMode.GLASS)
    @Test fun warmProjectDialogAcceptsDrivePathsFromTheKeyboard() = projectDialog(SkinMode.CLEAN)
    private fun projectDialog(skin: SkinMode) {
        AppLanguage.setMode(context,AppLanguageMode.ENGLISH)
        var created: Pair<String,String>? = null
        val state=AppUiState(route=AppRoute.SESSIONS,skinMode=skin,
            projectPickerListing=WorkspaceListing(path="C:\\Work",parent=null,entries=emptyList()))
        compose.setContent { HermesCompanionTheme(ThemeMode.LIGHT,skin) { AmbientBackground { HermesScene {
            SessionsScreen(state,PaddingValues(),{},{},{},{},{},{},{},{},{},{_,_->},{},
                {name,path->created=name to path},{},{},{},{})
        } } } }
        compose.onNodeWithText("Recent",substring=true).performClick()
        compose.onNodeWithText("Projects",substring=false).performClick()
        compose.onNodeWithText("New project",substring=false).performClick()
        compose.onNodeWithText("Project name").performTextInput("Windows team")
        compose.onNodeWithTag("project_absolute_path").performTextReplacement("D:\\Hermes\\Team workspace")
        capture(if(skin==SkinMode.GLASS) "windows-project-en" else "windows-project-warm-en",compose.onNodeWithText("Create",substring=false))
        compose.onNodeWithText("Create",substring=false).performClick()
        assertEquals("Windows team" to "D:\\Hermes\\Team workspace",created)

    }

}

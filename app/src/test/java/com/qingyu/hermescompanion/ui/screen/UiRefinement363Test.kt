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
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.qingyu.hermescompanion.i18n.*
import com.qingyu.hermescompanion.model.*
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
import java.io.File
import java.time.Instant

@OptIn(ExperimentalComposeUiApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35],qualifiers="zh-rCN-w390dp-h844dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class UiRefinement363Test {
    @get:Rule val compose=createComposeRule()
    private val context get()=RuntimeEnvironment.getApplication()
    @Before fun language() { AppLanguage.setMode(context,AppLanguageMode.CHINESE) }
    @After fun resetLanguage() { AppLanguage.setMode(context,AppLanguageMode.CHINESE) }
    private fun capture(name:String,node:SemanticsNodeInteraction=compose.onRoot())=compose.runOnIdle {
        val view=(node.fetchSemanticsNode().root as ViewRootForTest).view
        val bitmap=Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888)
        view.draw(android.graphics.Canvas(bitmap))
        File("build/ui-validation/363-$name.png").apply { parentFile.mkdirs() }.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) }
    }
    private fun homes(english:Boolean) {
        AppLanguage.setMode(context,if(english)AppLanguageMode.ENGLISH else AppLanguageMode.CHINESE)
        var skin by mutableStateOf(SkinMode.CLEAN)
        val sessions=listOf("AI 新媒体战略研讨","日常助理").mapIndexed { index,title -> HermesSession("item$index",if(english)listOf("Content strategy review","Daily assistant")[index] else title,
            if(english)"Review this week's progress and prepare our next meeting" else "今天晚上我们要开个非常重要的会议，确定接下来的安排。",Instant.now().minusSeconds((index+1)*3600L).toString()) }
        compose.setContent { CompositionLocalProvider(LocalDensity provides Density(1f),LocalReduceMotion provides true) {
            HermesCompanionTheme(ThemeMode.LIGHT,skin) { AmbientBackground {
                Scaffold(containerColor=Color.Transparent,contentWindowInsets=WindowInsets(0,0,0,0),bottomBar={ReferenceBottomDock(AppRoute.HOME,false,{},WindowInsets(0,0,0,24))}) { padding ->
                    HermesScene(contentBottomClip=if(skin==SkinMode.GLASS)32.dp else 0.dp) {
                        AssistantHomeScreen(AppUiState(route=AppRoute.HOME,skinMode=skin,username="admin",sessions=sessions,reduceMotion=true,homeWelcomed=true),padding,{},{},{},{},{},{_,_->},{})
                    }
                }
            } }
        } }
        for(mode in SkinMode.entries) {
            compose.runOnIdle { skin=mode }
            compose.onNodeWithTag("daily_conversation_entry").assertIsDisplayed()
            compose.onNode(hasText(sessions.last().title) and hasAnyAncestor(hasTestTag("home_recent"))).assertIsDisplayed()
            val recent=compose.onNodeWithTag("home_recent").fetchSemanticsNode().boundsInRoot
            val dock=compose.onNodeWithTag("floating_bottom_dock").fetchSemanticsNode().boundsInRoot
            capture("home-${mode.name}-${if(english)"en" else "zh"}-${context.resources.configuration.screenHeightDp}")
            assertTrue("$mode must show both recent entries above dock without scrolling: $recent / $dock",recent.bottom<=dock.top)
            compose.onNodeWithTag("home_date").assertTextEquals(java.time.LocalDate.now().format(
                java.time.format.DateTimeFormatter.ofPattern(if(english)"EEE, MMM d" else "M月d日 · EEEE",AppLanguage.locale)))
            val caption=mutableListOf<TextLayoutResult>()
            compose.onNodeWithTag("home_caption").performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(caption) }
            // A cached paragraph may retain the available width after Text shrinks
            // to its content; verify the rendered line extents rather than that width.
            assertTrue("$mode caption must fit without truncating: " + caption.map { it.layoutInput.text },caption.all { result ->
                result.lineCount in 1..2 && (0 until result.lineCount).all { line ->
                    !result.isLineEllipsized(line) && result.getLineRight(line)-result.getLineLeft(line)<=result.size.width+1f &&
                        result.getLineBottom(line)<=result.size.height+1f
                }
            })
            if(mode==SkinMode.CLEAN) {
                val greeting=compose.onNodeWithTag("warm_greeting").fetchSemanticsNode().boundsInRoot
                val portrait=compose.onNodeWithTag("home_hermes_portrait").fetchSemanticsNode().boundsInRoot
                val action=compose.onNodeWithTag("daily_conversation_entry").fetchSemanticsNode().boundsInRoot
                assertEquals(greeting.top+4f,portrait.top,1f)
                assertEquals(action.bottom,portrait.bottom,1f)
                assertTrue("Character must remain beside the action",portrait.left>=action.right)
            }
        }
    }
    @Test fun threeHomeSkinsFitChinese()=homes(false)
    @Test fun threeHomeSkinsFitEnglish()=homes(true)
    @Test @Config(qualifiers="zh-rCN-w360dp-h780dp-mdpi") fun smallerHomeFitsChinese()=homes(false)
    @Test @Config(qualifiers="zh-rCN-w360dp-h780dp-mdpi") fun smallerHomeFitsEnglish()=homes(true)

    @Test fun gatewayFieldsAndActionsSurviveEverySkinAndLanguage() {
        var skin by mutableStateOf(SkinMode.GLASS)
        var saved by mutableStateOf(false)
        var theme by mutableStateOf(ThemeMode.LIGHT)
        var values:List<Any>?=null
        var backed=false
        compose.setContent { HermesCompanionTheme(theme,skin) { AmbientBackground { HermesScene {
            ConnectionScreen(AppUiState(skinMode=skin,hasSavedConnection=saved),PaddingValues(),{a,b,c,d->values=listOf(a,b,c,d)}, {},{},{},{backed=true},if(saved) ({}) else null)
        } } } }
        compose.onNodeWithTag("gateway_url").performTextReplacement("https://gateway.example")
        compose.onNodeWithTag("gateway_username").performScrollTo().performTextReplacement("team-user")
        compose.onNodeWithTag("gateway_password").performScrollTo().performTextReplacement("test-only")
        for(mode in SkinMode.entries) {
            compose.runOnIdle { skin=mode }
            compose.onNodeWithTag("gateway_url").performScrollTo().assertTextContains("https://gateway.example")
            capture("gateway-${mode.name}")
        }
        compose.runOnIdle { saved=true;AppLanguage.setMode(context,AppLanguageMode.ENGLISH) }
        compose.onNodeWithTag("gateway_connect").performScrollTo().performClick()
        assertEquals(listOf("https://gateway.example","team-user","test-only",false),values)
        compose.onNodeWithTag("gateway_url").performScrollTo()
        capture("gateway-settings-en")
        compose.runOnIdle { skin=SkinMode.GLASS;theme=ThemeMode.DARK }
        compose.onNodeWithTag("gateway_url").assertTextContains("https://gateway.example")
        capture("gateway-settings-glass-dark-en")
        compose.onNodeWithContentDescription("Back").performClick();assertTrue(backed)
    }

    @Test fun identityEditorsKeepDraftsAcrossSkinsAndSaveOnlyLocalProfile() {
        var skin by mutableStateOf(SkinMode.GLASS)
        var first by mutableStateOf(true)
        var profile by mutableStateOf(UserProfilePreferences())
        var result:UserProfilePreferences?=null
        compose.setContent { HermesCompanionTheme(ThemeMode.LIGHT,skin) { AmbientBackground { HermesScene {
            IdentityEditorScreen(AppUiState(skinMode=skin,userProfile=profile,username="agent-account"),firstRun=first,
                onSave={result=it;profile=it},onUpdateHermesAvatar={_,_->},onResetHermesAvatar={},onSkinChange={skin=it})
        } } } }
        compose.onNodeWithTag("assistant_name").performTextReplacement("小月")
        compose.onNodeWithTag("user_display_name").performScrollTo().performTextReplacement("Alex")
        for(mode in SkinMode.entries) {
            compose.runOnIdle { skin=mode }
            compose.onNodeWithTag("assistant_name").performScrollTo().assertTextContains("小月")
            capture("identity-${mode.name}")
        }
        compose.onNodeWithTag("identity_continue").performScrollTo().performClick()
        assertEquals("小月",result?.hermesDisplayName);assertEquals("Alex",result?.displayName)
        compose.runOnIdle { first=false }
        for(mode in SkinMode.entries) {
            compose.runOnIdle { skin=mode }
            compose.onNodeWithTag("assistant_name").performScrollTo()
            capture("edit-identity-${mode.name}")
        }
        compose.onNodeWithTag("user_bio").performScrollTo().performTextReplacement("Enjoy small details")
        compose.onNodeWithTag("identity_continue").performScrollTo().performClick()
        assertEquals("Enjoy small details",result?.bio)
        compose.onNodeWithText("当前网关账号：agent-account").assertIsDisplayed()
    }

    @Test fun guideNavigatesByTaskAndSearchAndOffersReplay() {
        var exited=false;var replay=false
        compose.setContent { HermesCompanionTheme(ThemeMode.LIGHT,SkinMode.GLASS) { AmbientBackground { HermesScene {
            OperationGuideScreen(PaddingValues(),{exited=true},{replay=true})
        } } } }
        capture("guide-index")
        compose.onNodeWithTag("guide_category_connect").performClick()
        capture("guide-category")
        compose.onNodeWithTag("guide_task_first-connect").performClick()
        compose.onNodeWithTag("guide_article_title").assertTextEquals("第一次连接 Agent")
        capture("guide-article")
        compose.onNodeWithTag("guide_back").performClick()
        compose.onNodeWithTag("guide_task_first-connect").assertIsDisplayed()
        compose.onNodeWithTag("guide_back").performClick()
        compose.onNodeWithTag("guide_search").performTextInput("息屏")
        compose.onNodeWithTag("guide_task_continuous-voice").performClick()
        compose.runOnIdle { AppLanguage.setMode(context,AppLanguageMode.ENGLISH) }
        compose.onNodeWithTag("guide_article_title").assertTextEquals("Start and interrupt continuous voice")
        capture("guide-voice-en")
        compose.onNodeWithTag("guide_back").performClick();compose.onNodeWithTag("guide_back").performClick()
        compose.onNodeWithTag("guide_search").performTextClearance()
        compose.onNodeWithTag("guide_list").performScrollToNode(hasTestTag("guide_replay_intro"))
        compose.onNodeWithTag("guide_replay_intro").performClick();assertTrue(replay)
        compose.onNodeWithTag("guide_back").performClick();assertTrue(exited)
    }

    @Test fun glassSessionsHaveEighteenDpInsideRoundedGroups() {
        val sessions=(1..7).map { HermesSession("s$it", "会话 $it · 本周进展与内容安排", "讨论团队这周的目标与执行安排。",Instant.now().minusSeconds(it*3600L).toString()) }
        compose.setContent { HermesCompanionTheme(ThemeMode.LIGHT,SkinMode.GLASS) { AmbientBackground {
            Scaffold(containerColor=Color.Transparent,contentWindowInsets=WindowInsets(0,0,0,0),bottomBar={ReferenceBottomDock(AppRoute.SESSIONS,false,{},WindowInsets(0,0,0,24))}) { padding ->
                HermesScene(contentBottomClip=32.dp) { SessionsScreen(AppUiState(sessions=sessions,skinMode=SkinMode.GLASS),padding,{},{},{},{},{},{},{},{},{},{_,_->},{},{_,_->},{},{},{},{}) }
            }
        } } }
        val outer=compose.onNodeWithTag("session_row_s1").fetchSemanticsNode().boundsInRoot
        val inner=compose.onNodeWithTag("session_row_content_s1",useUnmergedTree=true).fetchSemanticsNode().boundsInRoot
        assertEquals(18f,inner.left-outer.left,.5f);assertEquals(18f,outer.right-inner.right,.5f)
        assertEquals(18f,inner.top-outer.top,.5f)
        capture("sessions-glass-padding")
    }

    @Test fun chatMenuCentersLabelsAndUsesThemedPopup() {
        var skin by mutableStateOf(SkinMode.GLASS)
        var theme by mutableStateOf(ThemeMode.LIGHT)
        var voiceStarts=0
        val state=AppUiState(route=AppRoute.CHAT,selectedSession=HermesSession("test","日常助理"),skinMode=SkinMode.GLASS,
            messages=listOf(ChatMessage(role=MessageRole.ASSISTANT,content="收到，我们继续。")))
        compose.setContent { HermesCompanionTheme(theme,skin) { AmbientBackground { HermesScene {
            ChatScreen(state=state.copy(skinMode=skin),onEntryHandled={},contentPadding=PaddingValues(),onBack={},onDraftChange={},onAddAttachments={},onRemoveAttachment={},
                onSend={},onRetryFailed={},onStop={},onSteer={},onQueue={},onCancelQueued={},onRespondRequest={_,_->},onVoiceConversation={voiceStarts++},
                onStartVoiceInput={},onStopVoiceInput={},onCancelVoiceInput={},onVoiceSystemResult={},onVoiceUnavailable={},onLoadModels={},onSwitchModel={_,_->},
                onLoadCommandCatalog={},onSetCouncilMode={},onOpenArtifact={},onOpenWorkspace={},onOpenImage={_,_->},onOpenLink={},onLoadInlineImages={},onLoadOlderMessages={},onScrollPositionChange={_,_,_->})
        } } } }
        for(english in listOf(false,true)) for(mode in SkinMode.entries) for(dark in listOf(false,true)) {
            compose.runOnIdle { skin=mode;theme=if(dark)ThemeMode.DARK else ThemeMode.LIGHT
                AppLanguage.setMode(context,if(english)AppLanguageMode.ENGLISH else AppLanguageMode.CHINESE) }
            compose.onNodeWithContentDescription(if(english)"Conversation menu" else "对话菜单").performClick()
            val bounds=compose.onNodeWithTag("chat_overflow_menu").fetchSemanticsNode().boundsInRoot
            assertTrue("Compact menu width: $bounds",bounds.width<=if(english)200f else 130f)
            assertTrue("Compact menu height: $bounds",bounds.height<=168f)
            for(label in if(english)listOf("Full conversation","Assistant settings","Voice chat") else listOf("完整对话","助理设置","连续语音")) {
                val layout=mutableListOf<TextLayoutResult>()
                compose.onNodeWithText(label,useUnmergedTree=true).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layout) }
                assertTrue(layout.isNotEmpty())
                assertTrue(layout.all { it.layoutInput.style.textAlign==TextAlign.Center && it.lineCount==1 && !it.isLineEllipsized(0) })
            }
            capture("menu-fix-${mode.name}-${if(english)"en" else "zh"}-${if(dark)"dark" else "light"}",compose.onNodeWithTag("chat_overflow_menu"))
            compose.onNodeWithText(if(english)"Voice chat" else "连续语音").performClick()
            compose.onNodeWithTag("chat_overflow_menu").assertDoesNotExist()
        }
        assertEquals(12,voiceStarts)
    }
    @Test fun reducedMotionIntroCanBeSkippedAndCompletesOnlyOnce() {
        compose.mainClock.autoAdvance=false
        var count=0
        compose.setContent { HermesCompanionTheme(ThemeMode.LIGHT,SkinMode.GLASS) {
            LaunchIntroScreen(true) { count++ }
        } }
        compose.onNodeWithTag("intro_skip").performClick()
        compose.mainClock.advanceTimeBy(500)
        compose.runOnIdle { assertEquals(1,count) }
        capture("intro-poster")
    }

}

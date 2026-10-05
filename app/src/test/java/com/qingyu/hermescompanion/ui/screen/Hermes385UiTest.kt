package com.qingyu.hermescompanion.ui.screen

import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import com.qingyu.hermescompanion.data.agentRequestKey
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
@Config(sdk=[35], qualifiers="zh-rCN-w390dp-h844dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class Hermes385UiTest {
    @get:Rule val compose = createComposeRule()
    @Before fun language() { AppLanguage.setMode(RuntimeEnvironment.getApplication(), AppLanguageMode.CHINESE) }
    @OptIn(ExperimentalComposeUiApi::class)
    private fun screenshot(name: String, tag: String) = compose.runOnIdle {
        val view=(compose.onNodeWithTag(tag).fetchSemanticsNode().root as ViewRootForTest).view
        val file=File("build/385-previews/$name.png"); file.parentFile.mkdirs()
        Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888).let { bitmap ->
            view.draw(android.graphics.Canvas(bitmap)); file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) }
        }
    }

    @Test @Config(qualifiers="zh-rCN-w320dp-h640dp-mdpi")
    fun checkFeedbackIsVisibleInTheDialogAndRemovalRequiresExplicitConfirmation() {
        val request=AgentRequest("r","runtime","conversation",AgentRequestType.APPROVAL,"核对并更新已选择的文件",profile="default")
        var pending by mutableStateOf(listOf(request))
        var check by mutableStateOf(AgentRequestCheck())
        var submissions=0
        var removals=0
        compose.setContent { CompositionLocalProvider(LocalDensity provides Density(1f,1.5f)) {
            HermesCompanionTheme(ThemeMode.DARK,SkinMode.CLEAN) {
                AgentRequestHost(pending, {_,_->submissions++}, { check=AgentRequestCheck(true,"正在核对服务器状态…") },
                    checks=mapOf(agentRequestKey(request) to check), onDismissReminder={removals++;pending=emptyList()}) {}
            }
        } }
        compose.onNodeWithTag("agent-request-recheck").performClick().assertIsNotEnabled()
        compose.onNodeWithText("正在核对服务器状态…").assertIsDisplayed()
        compose.runOnIdle { check=AgentRequestCheck(message="服务器未提供可核对的状态。若已在其他端处理，可移除此提醒。",canDismiss=true) }
        compose.onNodeWithTag("agent-request-check-result").assertIsDisplayed()
        compose.onNodeWithTag("agent-request-remove").assertIsDisplayed()
        compose.onNodeWithTag("agent-request-submit").assertIsDisplayed()
        screenshot("approval-status-small-dark", "decision_panel")
        compose.onNodeWithTag("agent-request-remove").performClick()
        assertEquals(0,removals)
        compose.onNodeWithTag("agent-request-remove-confirm").performClick()
        assertEquals(1,removals)
        assertEquals(0,submissions)
        compose.onNodeWithTag("decision_panel").assertDoesNotExist()
    }

    @Test fun contentActionsShareTheBodyGutterAndSourcesStillOpen() {
        val card=TodayCard("card","下周评审安排","先确认参会时间，再整理材料。",TodayKind.DECISION,
            sources=listOf("/work/plan.md"),presentation=TodayPresentation(background="上次讨论留下了两项需要核对的安排。"))
        var opened=""
        compose.setContent { HermesCompanionTheme(ThemeMode.LIGHT,SkinMode.GLASS) {
            TodayCardDetailSheet(card,{}, {opened=it},{})
        } }
        val body=compose.onNodeWithText(card.title).fetchSemanticsNode().boundsInRoot
        for (tag in listOf("today-background-toggle","today-sources-toggle")) {
            val action=compose.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot
            assertEquals(body.left,action.left,0.5f)
            assertTrue(action.height>=48f)
        }
        screenshot("aligned-card-actions", "today-detail-window")
        compose.onNodeWithTag("today-sources-toggle").performClick()
        compose.onNodeWithText("plan.md").performScrollTo().performClick()
        assertEquals("/work/plan.md",opened)
    }

    @Test fun savedScheduleLoadsWithoutOverwritingAnEditAndOnlyExplicitSaveConfiguresIt() {
        var check by mutableStateOf(TodayScheduleCheck())
        var saved: List<String>? = null
        compose.setContent { HermesCompanionTheme(ThemeMode.LIGHT,SkinMode.CLEAN) {
            TodayScheduleSheet({}, {m,e,z-> saved=listOf(m,e,z)}, check)
        } }
        compose.runOnIdle { check=TodayScheduleCheck(morning="08:15",evening="20:30",timezone="Asia/Shanghai") }
        compose.onNodeWithTag("schedule-morning").assertTextContains("08:15")
        compose.onNodeWithTag("schedule-evening").assertTextContains("20:30")
        assertNull(saved)
        compose.onNodeWithTag("schedule-morning").performTextReplacement("07:45")
        compose.runOnIdle { check=check.copy(morning="09:00") }
        compose.onNodeWithTag("schedule-morning").assertTextContains("07:45")
        compose.onNodeWithTag("schedule-submit").performScrollTo().performClick()
        assertEquals(listOf("07:45","20:30","Asia/Shanghai"),saved)
    }

    @Test fun gearOpensOptionalSetupAndScheduleLivesInsideIt() {
        val state=AppUiState(reduceMotion=true,today=TodayState(profile="default",root="/work",rootVerified=true))
        var scheduleChecks=0
        var generations=0
        compose.setContent { HermesCompanionTheme(ThemeMode.LIGHT,SkinMode.CLEAN) {
            androidx.compose.material3.Surface {
                TodayOverviewScreen(state,PaddingValues(),{},{generations++},{_,_->},{_,_->},{},{},{},{_,_->},{},onCheckSchedule={scheduleChecks++})
            }
        } }
        screenshot("home-gear", "today_home")
        compose.onNodeWithTag("today-settings").performClick()
        compose.onNodeWithTag("today-basic-client-note").performScrollTo().assertIsDisplayed()
        assertEquals(0,generations)
        screenshot("home-setup", "today-detail-window")
        compose.onNodeWithTag("today-setup-schedule").performScrollTo().performClick()
        compose.onNodeWithTag("today-schedule").assertIsDisplayed()
        assertEquals(1,scheduleChecks)
        assertEquals(0,generations)
    }
}

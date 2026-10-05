package com.qingyu.hermescompanion.ui.screen

import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.*
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
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
@Config(sdk=[35], qualifiers="zh-rCN-w390dp-h844dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class Hermes384UiTest {
    @get:Rule val compose = createComposeRule()
    @Before fun language() { AppLanguage.setMode(RuntimeEnvironment.getApplication(), AppLanguageMode.CHINESE) }
    private val request = AgentRequest("approval-1", "runtime", "task", AgentRequestType.APPROVAL,
        "python3 .hermes-app/today/writer.py --action-id original\n".repeat(12), "请确认写回对应文件并更新首页，操作范围仅限已选择事项。", profile="default")
    @OptIn(ExperimentalComposeUiApi::class)
    private fun screenshot(name: String, tag: String) = compose.runOnIdle {
        val view = (compose.onNodeWithTag(tag).fetchSemanticsNode().root as ViewRootForTest).view
        val output=File("build/384-previews/$name.png"); output.parentFile.mkdirs()
        Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888).let { bitmap ->
            view.draw(android.graphics.Canvas(bitmap)); output.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) }
        }
    }

    @Test fun onlyOneFixedApprovalWindowPreservesChoiceAcrossPollsAndClosesWhenResolved() {
        var pending by mutableStateOf(listOf(request))
        compose.setContent { HermesCompanionTheme(ThemeMode.LIGHT,SkinMode.GLASS) {
            AgentRequestHost(pending, {_,_->}, {}) { pending.firstOrNull()?.let { DecisionCard(it,{_,_->}) } }
        } }
        compose.onAllNodesWithTag("decision_panel").assertCountEquals(1)
        val before=compose.onNodeWithTag("decision_panel").fetchSemanticsNode().boundsInRoot
        val footer=compose.onNodeWithTag("agent-request-submit").fetchSemanticsNode().boundsInRoot
        compose.onNodeWithTag("agent-choice:once").performScrollTo().performClick()
        compose.onNodeWithTag("agent-request-submit").assertIsEnabled()
        compose.runOnIdle { pending=listOf(request.copy(runtimeSessionId="resumed", isResponding=true)) }
        assertEquals(before,compose.onNodeWithTag("decision_panel").fetchSemanticsNode().boundsInRoot)
        assertEquals(footer,compose.onNodeWithTag("agent-request-submit").fetchSemanticsNode().boundsInRoot)
        compose.runOnIdle { pending=listOf(request.copy(runtimeSessionId="resumed")) }
        compose.onNodeWithTag("agent-request-submit").assertIsEnabled()
        screenshot("approval-fixed", "decision_panel")
        compose.onNodeWithTag("today-detail-close").performClick()
        compose.onNodeWithTag("decision_panel").assertDoesNotExist()
        compose.onNodeWithTag("agent-request-open").performClick()
        compose.onAllNodesWithTag("decision_panel").assertCountEquals(1)
        compose.runOnIdle { pending=emptyList() }
        compose.onNodeWithTag("decision_panel").assertDoesNotExist()
    }

    @Test @Config(qualifiers="zh-rCN-w320dp-h640dp-mdpi")
    fun approvalButtonsStaySingleLineOnSmallScreensWithLargeText() {
        compose.setContent { CompositionLocalProvider(LocalDensity provides Density(1f,1.5f)) {
            HermesCompanionTheme(ThemeMode.DARK,SkinMode.CLEAN) { DecisionCard(request,{_,_->}) }
        } }
        val layouts = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText("查看并确认", useUnmergedTree=true).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertEquals(1, layouts.single().lineCount)
        screenshot("approval-card-small", "agent-request-open")
        compose.onNodeWithTag("agent-request-open").performClick()
        compose.onNodeWithTag("agent-choice:session").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("agent-request-submit").assertIsDisplayed()
        screenshot("approval-large-dark", "decision_panel")
    }

    @Test fun homeSettingsExplainOptionalSetupAndShowCompletedStorage() {
        var state by mutableStateOf(AppUiState(reduceMotion=true, today=TodayState(profile="default",root="/work",rootVerified=true,fileExists=true,filePath="/work/hermes-today.json")))
        compose.setContent { HermesCompanionTheme(ThemeMode.LIGHT,SkinMode.CLEAN) {
            TodayOverviewScreen(state,PaddingValues(),{},{},{_,_->},{_,_->},{},{},{},{_,_->},{})
        } }
        compose.onNodeWithTag("today-settings").performClick()
        compose.onNodeWithText("首页设置").assertIsDisplayed()
        compose.onNodeWithTag("today-basic-client-note").assertIsDisplayed()
        compose.onNodeWithText("早晚整理").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("文件收纳 · 只需一次").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("收纳首页文件").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("同步与读取详情").assertDoesNotExist()
        screenshot("home-settings", "today-detail-window")
        compose.runOnIdle { state=state.copy(today=state.today.copy(filePath="/work/${TodayBoard.PATH}")) }
        compose.onNodeWithText("首页文件已收纳，无需重复操作。").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("收纳首页文件").assertDoesNotExist()
        compose.onNodeWithTag("today-maintenance").performScrollTo().performClick()
        compose.onNodeWithText("同步与读取详情").performScrollTo().assertIsDisplayed()
    }

    @Test fun automaticTasksStayOutOfConversationListButRemainAccessibleInTaskHistory() {
        val chat=HermesSession("chat","主动聊天",messageCount=2)
        val task=HermesSession("task","自动整理首页",source="android",messageCount=2)
        val cron=HermesSession("cron","早间整理记录",source="cron",messageCount=2)
        val state=AppUiState(sessions=listOf(chat,task,cron),taskSessionKeys=setOf(task.scopedId),reduceMotion=true)
        var tasks by mutableStateOf(false)
        var opened=""
        compose.setContent { HermesCompanionTheme(ThemeMode.LIGHT,SkinMode.CLEAN) {
            if (tasks) TasksScreen(state,PaddingValues(),{},{},{},{_,_->},{},{opened=it.id},{},{},{_,_,_->},{},{_,_,_,_->},{},{},{})
            else SessionsScreen(state,PaddingValues(),{},{},{},{},{},{},{},{},{},{_,_->},{},{_,_->},{},{},{},{})
        } }
        compose.onNodeWithText("主动聊天").assertIsDisplayed()
        compose.onNodeWithText("自动整理首页").assertDoesNotExist()
        compose.onNodeWithText("早间整理记录").assertDoesNotExist()
        compose.runOnIdle { tasks=true }
        compose.onNodeWithText("记录").performClick()
        compose.onNodeWithText("自动整理首页").assertIsDisplayed().performClick()
        assertEquals(task.id,opened)
        compose.onNodeWithText("早间整理记录").assertIsDisplayed()
        screenshot("task-history", "tasks-page")
    }
}

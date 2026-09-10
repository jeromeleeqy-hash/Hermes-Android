package com.qingyu.hermescompanion.ui.screen

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.*
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

@OptIn(ExperimentalComposeUiApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "zh-rCN-w390dp-h844dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AssistantLayoutTest {
    @get:Rule val compose = createComposeRule()
    private val session = HermesSession("completed", "本周运营复盘", preview="报告已完成 · 3 个关键发现")
    private val running = HermesSession("running", "内容转化分析")
    private val request = AgentRequest("request","runtime","running",AgentRequestType.CLARIFICATION,"这周的内容，先优化哪个方向？","互动不错，但咨询偏少。我整理了两个方向。",listOf(AgentRequestChoice("增加咨询量"),AgentRequestChoice("提高咨询质量")))
    private val state = AppUiState(route=AppRoute.HOME,username="Jerome",sessions=listOf(session,running),pendingAgentRequests=listOf(request),runningRuns=listOf(RunUiState(running,"已看完 12 篇内容，正在整理建议",0,false)))
    private fun render(fontScale:Float=1f,dark:Boolean=false,preview:AppUiState=state,onStart:(String)->Unit={}) {
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f,fontScale)) {
                HermesCompanionTheme(if(dark) ThemeMode.DARK else ThemeMode.LIGHT,SkinMode.CLEAN) {
                    Scaffold(modifier=Modifier.fillMaxSize().testTag("test_viewport"),contentWindowInsets=WindowInsets(0,0,0,0),
                        bottomBar={ReferenceBottomDock(AppRoute.HOME,false,{})}) { padding ->
                        AssistantHomeScreen(preview,padding,onStart,{},{},{},{},{_,_->},{})
                    }
                }
            }
        }
        compose.waitForIdle()
    }
    private fun capture(name:String, selector:SemanticsNodeInteraction=compose.onRoot()): Bitmap {
        val file=File("build/ui-validation/$name.png");file.parentFile.mkdirs()
        val view=(selector.fetchSemanticsNode().root as ViewRootForTest).view
        return compose.runOnIdle {
            val bitmap=Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888)
            val canvas=android.graphics.Canvas(bitmap)
            canvas.drawColor(android.graphics.Color.rgb(245,246,250))
            view.draw(canvas)
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) }
            bitmap
        }
    }
    @Test fun homeKeepsRealContentWithoutComposer() {
        render()
        compose.onNodeWithText("这周的内容，先优化哪个方向？").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("本周运营复盘").assertExists()
        compose.onNodeWithTag("home_input").assertDoesNotExist()
        compose.onNodeWithTag("home_composer").assertDoesNotExist()
        compose.onNodeWithTag("fixed_region_divider").assertDoesNotExist()
        val root=compose.onNodeWithTag("home_root").fetchSemanticsNode().boundsInRoot
        val dock=compose.onNodeWithTag("floating_bottom_dock").fetchSemanticsNode().boundsInRoot
        assertTrue("Scrollable home extends behind the floating dock",root.bottom >= dock.bottom)
        capture("home-light-320")
    }
    @Test @Config(qualifiers="zh-rCN-w360dp-h800dp-mdpi") fun homeLargeTextStillReachesItsLastRow() {
        render(fontScale=1.3f)
        compose.onNodeWithText("本周运营复盘").performScrollTo().assertIsDisplayed()
        capture("home-large-320")
    }
    @Test fun homeNewTopicOpensChatWithoutRedundantLink() {
        var started=false
        render(preview=state.copy(pendingAgentRequests=emptyList()),onStart={started=true})
        compose.onNodeWithText("接着聊聊").assertDoesNotExist()
        compose.onNodeWithText("跟我说").performClick()
        assertTrue(started)
        compose.onNodeWithText("1 件事正在处理").assertDoesNotExist()
        compose.onNodeWithText("暂时没有待确认事项").assertDoesNotExist()
        val portrait = compose.onNodeWithTag("home_hermes_portrait").fetchSemanticsNode().boundsInRoot
        val button = compose.onNodeWithTag("daily_conversation_entry").fetchSemanticsNode().boundsInRoot
        assertTrue("The character must not cover the daily action", portrait.left >= button.right || portrait.top >= button.bottom || portrait.bottom <= button.top)
        capture("home-simplified-320")
    }

    @Test fun darkHomeUsesTheSamePortraitWithoutWhiteRectangle() {
        render(dark=true)
        compose.onNodeWithTag("home_hermes_portrait").assertIsDisplayed()
        capture("home-dark-320")
    }
    @Test fun twelveChoicesKeepConfirmationAccessible() {
        var reply=""
        compose.setContent { HermesCompanionTheme(ThemeMode.LIGHT, SkinMode.CLEAN) {
            DecisionCard(request.copy(choices=(1..12).map {AgentRequestChoice("选项 $it：保留完整的说明和阅读空间","$it")}),{_,value->reply=value})
        } }
        compose.onNodeWithText("看看建议").performClick()
        compose.onNodeWithText("确认并继续").assertIsDisplayed().assertIsNotEnabled()
        compose.onNodeWithText("选项 12：保留完整的说明和阅读空间").performScrollTo().performClick()
        compose.onNodeWithText("确认并继续").assertIsDisplayed().assertIsEnabled()
        capture("decision-long-options",compose.onNodeWithTag("decision_panel"))
        compose.onNodeWithText("确认并继续").performClick();assertEquals("12",reply)
    }
    @Test fun workTimelineShowsOnlySuppliedSteps() {
        compose.setContent { HermesCompanionTheme(ThemeMode.LIGHT, SkinMode.CLEAN) { Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(18.dp)) {
            WorkProgress(listOf(ChatTodo("1","已读完 12 篇内容",TodoStatus.COMPLETED),ChatTodo("2","已完成内容对比",TodoStatus.COMPLETED),ChatTodo("3","正在核对咨询入口",TodoStatus.IN_PROGRESS),ChatTodo("4","整理改进建议",TodoStatus.PENDING)),emptyList())
        } } }
        compose.onNodeWithText("正在核对咨询入口").assertIsDisplayed()
        compose.onNodeWithText("整理改进建议").assertIsDisplayed()
        capture("work-timeline")
    }
    @Test fun workDetailRendersReferenceStructure() {
        val work = state.copy(route=AppRoute.CHAT,selectedSession=running,isStreaming=true,streamingSessionId=running.id,
            pendingAgentRequests=emptyList(),runStage="正在核对咨询入口",
            messages=listOf(ChatMessage(role=MessageRole.USER,content="分析一下本周内容的咨询转化"),ChatMessage(role=MessageRole.ASSISTANT,content="部分内容有讨论点，但缺少明确的咨询理由。",isStreaming=true)),
            chatTodos=listOf(ChatTodo("1","已读完 12 篇内容",TodoStatus.COMPLETED),ChatTodo("2","已完成内容对比",TodoStatus.COMPLETED),ChatTodo("3","正在核对咨询入口",TodoStatus.IN_PROGRESS),ChatTodo("4","整理改进建议",TodoStatus.PENDING)),
            chatArtifacts=listOf(ChatArtifact("/workspace/内容数据.xlsx","内容数据.xlsx","xlsx")))
        compose.setContent { HermesCompanionTheme(ThemeMode.LIGHT,SkinMode.CLEAN) {
            ChatScreen(state=work,onEntryHandled={},contentPadding=PaddingValues(),onBack={},onDraftChange={},onAddAttachments={},onRemoveAttachment={},
                onSend={},onRetryFailed={},onStop={},onSteer={},onQueue={},onCancelQueued={},onRespondRequest={_,_->},onVoiceConversation={},
                onStartVoiceInput={},onStopVoiceInput={},onCancelVoiceInput={},onVoiceSystemResult={},onVoiceUnavailable={},onLoadModels={},onSwitchModel={_,_->},
                onLoadCommandCatalog={},onSetCouncilMode={},onOpenArtifact={},onOpenWorkspace={},onOpenImage={_,_->},onOpenLink={},onLoadInlineImages={},onLoadOlderMessages={},onScrollPositionChange={_,_,_->})
        } }
        compose.onNodeWithText("调整要求").assertIsDisplayed()
        compose.onNodeWithText("停止这项工作").assertIsDisplayed()
        capture("work-detail")
    }

    @Test fun thinkingHasOneIndicator() {
        val work = state.copy(route=AppRoute.CHAT,selectedSession=running,isStreaming=true,streamingSessionId=running.id,
            pendingAgentRequests=emptyList(),runStage="准备回答",
            messages=listOf(ChatMessage(role=MessageRole.USER,content="分析一下本周内容的咨询转化"),ChatMessage(role=MessageRole.ASSISTANT,content="",reasoning="正在核对上下文。",isStreaming=true)),
            chatTodos=listOf(ChatTodo("1","已读完 12 篇内容",TodoStatus.COMPLETED),ChatTodo("2","已完成内容对比",TodoStatus.COMPLETED),ChatTodo("3","正在核对咨询入口",TodoStatus.IN_PROGRESS),ChatTodo("4","整理改进建议",TodoStatus.PENDING)),
            chatArtifacts=listOf(ChatArtifact("/workspace/内容数据.xlsx","内容数据.xlsx","xlsx")))
        compose.setContent { HermesCompanionTheme(ThemeMode.LIGHT,SkinMode.CLEAN) {
            ChatScreen(state=work,onEntryHandled={},contentPadding=PaddingValues(),onBack={},onDraftChange={},onAddAttachments={},onRemoveAttachment={},
                onSend={},onRetryFailed={},onStop={},onSteer={},onQueue={},onCancelQueued={},onRespondRequest={_,_->},onVoiceConversation={},
                onStartVoiceInput={},onStopVoiceInput={},onCancelVoiceInput={},onVoiceSystemResult={},onVoiceUnavailable={},onLoadModels={},onSwitchModel={_,_->},
                onLoadCommandCatalog={},onSetCouncilMode={},onOpenArtifact={},onOpenWorkspace={},onOpenImage={_,_->},onOpenLink={},onLoadInlineImages={},onLoadOlderMessages={},onScrollPositionChange={_,_,_->})
        } }
        compose.onNodeWithText("调整要求").assertIsDisplayed()
        compose.onNodeWithText("停止这项工作").assertIsDisplayed()
        compose.onAllNodesWithText("正在思考", substring=true).assertCountEquals(1)
        capture("thinking-single-320")
    }

    @Test fun modernPaletteAndLauncherPreview() {
        compose.setContent { HermesCompanionTheme(ThemeMode.LIGHT, SkinMode.CLEAN) {
            Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(24.dp), verticalArrangement=Arrangement.spacedBy(24.dp)) {
                Text("Hermes · 晴空蓝", style=MaterialTheme.typography.headlineLarge)
                Row(horizontalArrangement=Arrangement.spacedBy(24.dp)) {
                    listOf(false, true).forEach { round ->
                        androidx.compose.ui.viewinterop.AndroidView(factory={ context ->
                            android.widget.ImageView(context).apply {
                                val source=context.getDrawable(com.qingyu.hermescompanion.R.mipmap.ic_launcher) as android.graphics.drawable.AdaptiveIconDrawable
                                setImageDrawable(object:android.graphics.drawable.Drawable() {
                                    override fun draw(canvas:android.graphics.Canvas) {
                                        val checkpoint=canvas.save()
                                        val path=android.graphics.Path()
                                        val box=android.graphics.RectF(bounds)
                                        if(round) path.addOval(box,android.graphics.Path.Direction.CW)
                                        else path.addRoundRect(box,24f,24f,android.graphics.Path.Direction.CW)
                                        canvas.clipPath(path)
                                        source.background.bounds=bounds;source.background.draw(canvas)
                                        // Adaptive icon layers extend beyond the visible viewport by 25%.
                                        val inset=bounds.width()/4
                                        source.foreground.setBounds(-inset,-inset,bounds.right+inset,bounds.bottom+inset)
                                        source.foreground.draw(canvas);canvas.restoreToCount(checkpoint)
                                    }
                                    override fun setAlpha(alpha:Int) {}
                                    override fun setColorFilter(filter:android.graphics.ColorFilter?) {}
                                    @Deprecated("Deprecated in Java") override fun getOpacity()=android.graphics.PixelFormat.TRANSLUCENT
                                })
                            }
                        },modifier=Modifier.size(108.dp))
                    }
                }
                AssistantPanel(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
                        Text("清晰、轻盈的操作层级",style=MaterialTheme.typography.titleMedium)
                        Button(onClick={}) { Text("确认并继续") }
                        OutlinedButton(onClick={},colors=ButtonDefaults.outlinedButtonColors(contentColor=MaterialTheme.colorScheme.onPrimaryContainer)) { Text("稍后处理") }
                        TextButton(onClick={},colors=ButtonDefaults.textButtonColors(contentColor=MaterialTheme.colorScheme.onPrimaryContainer)) { Text("查看详情 →") }
                    }
                }
                ReferenceBottomDock(AppRoute.HOME,false,{})
            }
        } }
        compose.onNodeWithText("确认并继续").assertIsDisplayed()
        capture("modern-palette")
    }

    @Test fun sessions_unified() {
        val preview=state.copy(route=AppRoute.SESSIONS,sessions=listOf(session,running),pendingAgentRequests=emptyList(),runningRuns=emptyList())
        compose.setContent { HermesCompanionTheme(ThemeMode.LIGHT,SkinMode.CLEAN) {
            Scaffold(bottomBar={ReferenceBottomDock(AppRoute.SESSIONS,false,{})},contentWindowInsets=WindowInsets(0,0,0,0)) { padding ->
                SessionsScreen(state=preview,contentPadding=padding,onRefresh={},onNewSession={},onSelectProject={_ -> },onSearch={},onOpenSession={_ -> },onDeleteSession={_ -> },onAiRenameSession={_ -> },onTogglePinned={_ -> },onArchiveSession={_ -> },onMoveToProject={_,_ -> },onLoadProjects={},onCreateProject={_,_ -> },onLoadProjectDirectories={_ -> },onCloseProjectDirectoryPicker={},onRefreshProfiles={},onSelectProfile={_ -> })
            }
        } }
        compose.onNodeWithTag("new_conversation").assertIsDisplayed()
        val touch=compose.onNodeWithTag("new_conversation").fetchSemanticsNode().boundsInRoot
        assertTrue(touch.width>=48f); assertTrue(touch.top < 130f)
        capture("sessions-unified")
    }

    @Test fun files_unified() {
        val preview=state.copy(route=AppRoute.WORKSPACE,sessions=emptyList(),pendingAgentRequests=emptyList(),runningRuns=emptyList(),workspaceListing=WorkspaceListing(path="/work/运营资料",projectName="运营资料",entries=listOf(WorkspaceEntry("客户反馈","/work/运营资料/客户反馈",true),WorkspaceEntry("本周运营复盘.md","/work/运营资料/本周运营复盘.md",false,4096))),workspaceRootPath="/work/运营资料")
        compose.setContent { HermesCompanionTheme(ThemeMode.LIGHT,SkinMode.CLEAN) {
            Scaffold(bottomBar={ReferenceBottomDock(AppRoute.WORKSPACE,false,{})},contentWindowInsets=WindowInsets(0,0,0,0)) { padding ->
                WorkspaceScreen(state=preview,contentPadding=padding,onRefresh={},onOpenDirectory={_ -> },onOpenDocument={_ -> },onOpenImage={_,_ -> },onOpenRecentArtifact={_ -> },onOpenArtifactSource={_ -> },onRefreshRecentArtifacts={},onCloseDocument={},onEditingChange={_ -> },onDraftChange={_ -> },onSave={},onExportDocument={_ -> },onShareDocument={},onUnsupportedFile={_ -> })
            }
        } }
        compose.onNodeWithText("本周运营复盘.md").assertIsDisplayed()
        capture("files-unified")
    }

    @Test fun tasks_unified() {
        val preview=state.copy(route=AppRoute.TASKS,sessions=emptyList(),pendingAgentRequests=emptyList(),runningRuns=emptyList(),cronJobs=listOf(CronJob("daily","每日运营简报","汇总昨天的数据和待办",CronSchedule(expression="0 9 * * *",display="每天 09:00")),CronJob("weekly","每周复盘","整理本周进展",CronSchedule(expression="0 18 * * 5",display="每周五 18:00"),enabled=false)))
        compose.setContent { HermesCompanionTheme(ThemeMode.LIGHT,SkinMode.CLEAN) {
            Scaffold(bottomBar={ReferenceBottomDock(AppRoute.TASKS,false,{})},contentWindowInsets=WindowInsets(0,0,0,0)) { padding ->
                TasksScreen(state=preview,contentPadding=padding,onStartConversation={},onOpenActiveRun={_ -> },onStopActiveRun={_ -> },onRespondRequest={_,_ -> },onOpenCompletion={_ -> },onOpenCronSession={_ -> },onOpenArtifact={_ -> },onRefreshCron={},onCreateCron={_,_,_ -> },onOpenCron={_ -> },onUpdateCron={_,_,_,_ -> },onToggleCron={_ -> },onTriggerCron={_ -> },onDeleteCron={_ -> })
            }
        } }
        compose.onNodeWithText("每日运营简报").assertIsDisplayed()
        capture("tasks-unified")
    }

    @Test fun settings_unified() {
        val preview=state.copy(route=AppRoute.PROFILE,sessions=emptyList(),pendingAgentRequests=emptyList(),runningRuns=emptyList())
        compose.setContent { HermesCompanionTheme(ThemeMode.LIGHT,SkinMode.CLEAN) {
            Scaffold(bottomBar={ReferenceBottomDock(AppRoute.PROFILE,false,{})},contentWindowInsets=WindowInsets(0,0,0,0)) { padding ->
                ProfileScreen(state=preview,contentPadding=padding,onOpenSettings={},onBackToProfile={},onThemeChange={_ -> },onSkinChange={_ -> },onConnectionSettings={},onNotificationSettings={},onVoiceSettings={},onSkillsTools={},onModelSettings={},onConversationStyle={},onApprovalSettings={},onMemoryContext={},onOpenMemoryFile={},onOpenSoulFile={},onArchivedSessions={},onProfileSettings={},onUpdateUserAvatar={_,_ -> },onAbout={},onChangeLog={},showSettings=true)
            }
        } }
        compose.onNodeWithText("我的").assertExists()
        capture("settings-unified")
    }

    @Test fun navigationSelectsFilesAndUsesFilledState() {
        compose.setContent { HermesCompanionTheme(ThemeMode.LIGHT,SkinMode.CLEAN) {
            var route by remember { mutableStateOf(AppRoute.HOME) }
            ReferenceBottomDock(route,false,{route=it})
        } }
        compose.onNodeWithTag("nav_HOME").assertIsSelected()
        compose.onNodeWithTag("nav_WORKSPACE").performClick()
        compose.onNodeWithTag("nav_WORKSPACE").assertIsSelected()
        compose.onNodeWithTag("nav_HOME").assertIsNotSelected()
        compose.onNodeWithTag("nav_TASKS").performClick()
        compose.onNodeWithTag("nav_TASKS").assertIsSelected()
        compose.onNodeWithTag("nav_WORKSPACE").assertIsNotSelected()
        compose.onNodeWithText("任务").assertIsDisplayed()
    }

    @Test fun navigationBlocksAllContentBelowItsTopIncludingSystemGestureInset() {
        compose.setContent { HermesCompanionTheme(ThemeMode.LIGHT,SkinMode.CLEAN) {
            Scaffold(modifier=Modifier.fillMaxSize().testTag("occlusion_root"),
                bottomBar={ReferenceBottomDock(AppRoute.TASKS,false,{},WindowInsets(0,0,0,24))},
                contentWindowInsets=WindowInsets(0,0,0,0)) { _ ->
                Box(Modifier.fillMaxSize().background(Color.Magenta))
            }
        } }
        val dock = compose.onNodeWithTag("floating_bottom_dock").fetchSemanticsNode().boundsInRoot
        val image = capture("dock-occlusion-320", compose.onNodeWithTag("occlusion_root"))
        assertEquals(android.graphics.Color.MAGENTA, image.getPixel(0, dock.top.toInt() - 20))
        for (y in dock.top.toInt() + 1 until image.height step 3) {
            for (x in 0 until image.width step 3) assertNotEquals("Content leaked at $x,$y", android.graphics.Color.MAGENTA, image.getPixel(x,y))
        }
    }

    @Test fun voiceNoiseModeCanChangeWhileListening() {
        var chosen = ""
        compose.setContent { HermesCompanionTheme(ThemeMode.LIGHT,SkinMode.CLEAN) {
            var preview by remember { mutableStateOf(state.copy(route=AppRoute.VOICE_CHAT,
                voiceConversation=VoiceConversationState(active=true,phase=VoicePhase.LISTENING,message="停顿中，即将自动发送…"))) }
            VoiceConversationScreen(preview,PaddingValues(),{},{},{},{},{},{},{},{},
                onNoiseSensitivityChange={ value -> chosen=value; preview=preview.copy(voicePreferences=preview.voicePreferences.copy(noiseSensitivity=value)) })
        } }
        compose.onNodeWithText("收音：日常").performClick()
        compose.onNodeWithText("嘈杂 · 减少背景声误触发，靠近手机说话").performClick()
        assertEquals("noisy",chosen)
        compose.onNodeWithText("收音：嘈杂").assertIsDisplayed()
        compose.onNodeWithText("停顿中，即将自动发送…").assertIsDisplayed()
        capture("voice-listening-320")
    }

    @Test fun homeBrandHasNoRedundantMenuAndKeepsSearch() {
        render()
        compose.onNodeWithTag("home_menu_anchor").assertDoesNotExist()
        compose.onNodeWithTag("home_menu").assertDoesNotExist()
        compose.onNodeWithContentDescription("搜索对话").assertIsDisplayed()
        compose.onNodeWithTag("nav_TASKS").assertIsDisplayed()
        compose.onNodeWithTag("nav_WORKSPACE").assertIsDisplayed()
    }

    @Test fun composerCommandsOpenPaletteAndChatMenuIsSimplified() {
        var draft="";var loads=0
        compose.setContent { HermesCompanionTheme(ThemeMode.LIGHT,SkinMode.CLEAN) {
            ChatScreen(state=state.copy(route=AppRoute.CHAT,selectedSession=session,commandCatalog=listOf(SlashCommand("/help","查看帮助"))),onEntryHandled={},contentPadding=PaddingValues(),onBack={},onDraftChange={draft=it},onAddAttachments={},onRemoveAttachment={},
                onSend={},onRetryFailed={},onStop={},onSteer={},onQueue={},onCancelQueued={},onRespondRequest={_,_->},onVoiceConversation={},
                onStartVoiceInput={},onStopVoiceInput={},onCancelVoiceInput={},onVoiceSystemResult={},onVoiceUnavailable={},onLoadModels={},onSwitchModel={_,_->},
                onLoadCommandCatalog={loads++},onSetCouncilMode={},onOpenArtifact={},onOpenWorkspace={},onOpenImage={_,_->},onOpenLink={},onLoadInlineImages={},onLoadOlderMessages={},onScrollPositionChange={_,_,_->})
        } }
        compose.onNodeWithContentDescription("对话菜单").performClick()
        compose.onNodeWithText("对话详情").assertDoesNotExist()
        compose.onNodeWithText("快捷命令").assertDoesNotExist()
        compose.onNodeWithText("完整对话").performClick()
        compose.onNodeWithContentDescription("添加内容").performClick()
        compose.onNodeWithText("链接").assertDoesNotExist()
        compose.onNodeWithText("命令").assertIsDisplayed()
        capture("composer-tools",compose.onNodeWithText("添加到对话"))
        compose.onNodeWithText("命令").performClick()
        compose.onNodeWithText("/help").assertIsDisplayed().performClick()
        assertEquals(1,loads);assertEquals("/help",draft)
    }
    @Test fun workspacePickerRoutesRecentAndProjectFilesToAttachmentActions() {
        var recent=0;var files=0;var previews=0;var cancelled=false
        val artifact=RecentArtifact("default","source","九月运营汇报","m","/work/日报.md","日报.md","Markdown")
        val preview=state.copy(route=AppRoute.WORKSPACE,workspaceAttachmentTarget=session,selectedSession=session,
            recentArtifacts=listOf(artifact),workspaceRootPath="/work",workspaceListing=WorkspaceListing(path="/work",entries=listOf(WorkspaceEntry("运营数据.xlsx","/work/运营数据.xlsx",false,2048))))
        compose.setContent { HermesCompanionTheme(ThemeMode.LIGHT,SkinMode.CLEAN) {
            WorkspaceScreen(state=preview,contentPadding=PaddingValues(),onRefresh={},onOpenDirectory={},onOpenDocument={previews++},onOpenImage={_,_->previews++},onOpenRecentArtifact={previews++},onOpenArtifactSource={previews++},onRefreshRecentArtifacts={},onCloseDocument={},onEditingChange={},onDraftChange={},onSave={},onExportDocument={},onShareDocument={},onUnsupportedFile={previews++},
                onCancelAttachmentPicker={cancelled=true},onSelectAttachment={files++},onSelectRecentAttachment={recent++})
        } }
        compose.onNodeWithText("选择附件").assertIsDisplayed()
        compose.onNodeWithText("切换项目").assertDoesNotExist()
        compose.onNodeWithContentDescription("返回来源对话").assertDoesNotExist()
        capture("workspace-picker")
        compose.onNodeWithText("日报.md").performClick();assertEquals(1,recent)
        compose.onNodeWithText("项目文件").performClick()
        compose.onNodeWithText("运营数据.xlsx").performClick();assertEquals(1,files)
        assertEquals(0,previews)
        compose.onNodeWithText("取消").performClick();assertTrue(cancelled)
    }

}

package com.qingyu.hermescompanion.ui.screen

import android.graphics.Bitmap
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.qingyu.hermescompanion.i18n.*
import com.qingyu.hermescompanion.model.*
import com.qingyu.hermescompanion.today.*
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
import java.time.ZonedDateTime
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "zh-rCN-w390dp-h844dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AssistantRefocus380UiTest {
    @get:Rule val compose = createComposeRule()
    private val originalTimeZone = TimeZone.getDefault()
    @Before fun language() {
        AppLanguage.setMode(RuntimeEnvironment.getApplication(), AppLanguageMode.CHINESE)
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"))
    }
    @After fun restoreTimeZone() { TimeZone.setDefault(originalTimeZone) }
    private fun show(theme: () -> ThemeMode = { ThemeMode.LIGHT }, skin: () -> SkinMode = { SkinMode.CLEAN }, scale: () -> Float = { 1f }, content: @Composable () -> Unit) {
        compose.setContent {
            CompositionLocalProvider(LocalHomeClock provides { ZonedDateTime.parse("2026-10-04T14:00:00+08:00") }, LocalDensity provides Density(LocalDensity.current.density, scale())) {
                HermesCompanionTheme(theme(), skin()) { AmbientBackground { HermesScene { content() } } }
            }
        }
    }
    @OptIn(ExperimentalComposeUiApi::class)
    private fun screenshot(name: String, tag: String? = null) = compose.runOnIdle {
        val node = if (tag == null) compose.onRoot() else compose.onNodeWithTag(tag)
        val view = (node.fetchSemanticsNode().root as ViewRootForTest).view
        val file = File("build/381-previews/$name.png"); file.parentFile.mkdirs()
        Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888).let { b ->
            view.draw(android.graphics.Canvas(b)); file.outputStream().use { b.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }
    @Test fun homeLeadsWithReadableContextAndDiscussionWithoutSubmittingAnAction() {
        val board = TodayBoard.decode(File("src/main/assets/hermes-today-examples.json").readText(), "/work")
        val state = AppUiState(route = AppRoute.HOME, username = "Jerome", reduceMotion = true,
            today = TodayState(profile = "default", root = "/work", loaded = true, fileExists = true, board = board))
        var selected: Pair<String, String>? = null
        var submitted = false
        var theme by mutableStateOf(ThemeMode.LIGHT)
        show(theme = { theme }) {
            TodayOverviewScreen(state, PaddingValues(bottom = 20.dp), {}, {}, { _, _ -> }, { id, action -> selected = id to action },
                {}, {}, {}, { _, _ -> }, {}, onInteraction = { _, _ -> submitted = true })
        }
        screenshot("home")
        compose.onNodeWithTag("today_home").performScrollToNode(hasTestTag("today-card:reading-plan"))
        compose.onNodeWithTag("today-card:reading-plan").performClick()
        compose.onNodeWithTag("today-detail-discuss").assertIsDisplayed()
        compose.onNodeWithTag("today-quick-response").assertDoesNotExist()
        screenshot("home-detail", "today-detail")
        compose.onNodeWithTag("today-detail-discuss").performClick()
        assertEquals("reading-plan" to "discuss", selected)
        assertFalse(submitted)
        compose.runOnIdle { theme = ThemeMode.DARK }
        compose.onNodeWithTag("today_home").performScrollToNode(hasTestTag("today-card:reading-progress"))
        screenshot("home-progress-dark")
    }
    @Test fun chatKeepsTheAnswerAndFoldsManyReferencesAndToolStepsByDefault() {
        val session = HermesSession("chat", "一起推进方案", workspacePath = "/work")
        val materials = (1..12).joinToString("\n") { "/work/资料$it.md" }
        val state = AppUiState(route = AppRoute.CHAT, selectedSession = session, reduceMotion = true, username = "Jerome",
            messages = listOf(ChatMessage(role = MessageRole.USER, content = "帮我看看方案下一步怎么推进。"),
                ChatMessage(role = MessageRole.ASSISTANT, content = "方案方向已经明确，下一步先确认执行分工。\n\n**建议先做两件事**\n\n- 确认每项工作的负责人。\n- 和负责人一起约定完成时间。\n\n我可以先帮你整理一份分工草稿。")),
            toolActivities = listOf(ToolActivity(name = "读取文件并校验哈希", preview = materials, status = ToolStatus.COMPLETED)))
        var opened = ""
        show {
            ChatScreen(state, {}, PaddingValues(), {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, { _, _ -> },
                {}, {}, {}, {}, {}, {}, {}, { _, _ -> }, {}, {}, { opened = it.name }, {}, { _, _ -> }, {}, {}, {}, { _, _, _ -> })
        }
        compose.onNodeWithText("本次资料").assertDoesNotExist()
        compose.onNodeWithText("资料1.md").assertDoesNotExist()
        compose.onNodeWithText("读取文件并校验哈希").assertDoesNotExist()
        screenshot("chat")
        compose.onNodeWithTag("chat-focus-feed").performScrollToNode(hasTestTag("conversation-materials-toggle"))
        compose.onNodeWithTag("conversation-materials-toggle").performClick()
        compose.onNodeWithTag("chat-focus-feed").performScrollToNode(hasText("资料1.md"))
        compose.onNodeWithText("资料1.md").performClick()
        assertEquals("资料1.md", opened)
    }
    @Test fun scheduledWorkIsCompactAndSecondaryActionsRemainAvailable() {
        val jobs = listOf(
            CronJob("m", "早间整理", "不应显示的内部提示词", CronSchedule(expression = "0 9 * * *"), nextRunAt = "2026-10-05T09:00:00+08:00"),
            CronJob("e", "晚间回顾", "内部脚本路径 /work/writer.py", CronSchedule(expression = "0 21 * * *"), nextRunAt = "2026-10-04T21:00:00+08:00"),
            CronJob("w", "每周研究资料整理", "内部处理步骤", CronSchedule(expression = "0 18 * * 5"), enabled = false))
        val state = AppUiState(route = AppRoute.TASKS, cronJobs = jobs, reduceMotion = true)
        var triggered = ""
        var theme by mutableStateOf(ThemeMode.LIGHT)
        var skin by mutableStateOf(SkinMode.CLEAN)
        var scale by mutableStateOf(1f)
        show(theme = { theme }, skin = { skin }, scale = { scale }) {
            TasksScreen(state, PaddingValues(), {}, {}, {}, { _, _ -> }, {}, {}, {}, {}, { _, _, _ -> }, {}, { _, _, _, _ -> }, {}, { triggered = it.id }, {})
        }
        compose.onNodeWithText("执行中心").assertDoesNotExist()
        compose.onNodeWithText("不应显示的内部提示词").assertDoesNotExist()
        compose.onNodeWithText("早间整理").assertIsDisplayed()
        screenshot("tasks")
        compose.onAllNodesWithContentDescription("任务选项")[0].performClick()
        compose.onNodeWithText("现在运行一次").performClick()
        assertEquals("e", triggered)
        compose.runOnIdle { theme = ThemeMode.DARK; skin = SkinMode.GLASS; scale = 1.3f }
        screenshot("tasks-glass-large")
        compose.onNodeWithText("早间整理").assertIsDisplayed()
    }
    @Test fun filesStartAtTheWorkspaceEvenWhenRecentArtifactsExist() {
        val state = AppUiState(route = AppRoute.WORKSPACE, reduceMotion = true, workspaceRootPath = "/work",
            workspaceListing = WorkspaceListing(projectName = "个人工作区", path = "/work", root = "/work", entries = listOf(
                WorkspaceEntry("工作", "/work/work", true), WorkspaceEntry("学习", "/work/learning", true),
                WorkspaceEntry("生活", "/work/life", true), WorkspaceEntry(".hermes-app", "/work/.hermes-app", true), WorkspaceEntry("我的偏好.md", "/work/preferences.md", false, 1200))),
            recentArtifacts = listOf(RecentArtifact("default", "old", "旧对话", path = "/work/旧产物.md", name = "旧产物.md", kind = "Markdown")))
        var opened = ""
        show {
            WorkspaceScreen(state, PaddingValues(), {}, { opened = it }, {}, { _, _ -> }, {}, {}, {}, {}, {}, {}, {}, {}, {}, {})
        }
        compose.onNodeWithText("最近产物").assertDoesNotExist()
        compose.onNodeWithText("旧产物.md").assertDoesNotExist()
        compose.onNodeWithText("工作").assertIsDisplayed()
        screenshot("files")
        compose.onNodeWithText(".hermes-app").assertDoesNotExist()
        compose.onNodeWithTag("workspace-menu").performClick()
        compose.onNodeWithText("显示隐藏与系统文件").performClick()
        compose.onNodeWithText(".hermes-app").assertIsDisplayed()
        compose.onNodeWithText("工作").performClick()
        assertEquals("/work/work", opened)
    }
    @Test fun focusedReplyCollapsesExplicitLogsAndKeepsTheOriginalAccessible() {
        val answer = "方案已整理，下一步请确认负责人。\n\n**处理日志（详情）**\n- SHA256: diagnostic-only-marker\n- 检查了两份来源。\n\n**结论：先确认分工，再约定评审时间。**"
        val state = AppUiState(route = AppRoute.CHAT, selectedSession = HermesSession("reply", "安排项目评审"), reduceMotion = true,
            messages = listOf(ChatMessage(role = MessageRole.USER, content = "帮我梳理下一步。"), ChatMessage(role = MessageRole.ASSISTANT, content = answer)))
        show {
            ChatScreen(state, {}, PaddingValues(), {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, { _, _ -> },
                {}, {}, {}, {}, {}, {}, {}, { _, _ -> }, {}, {}, {}, {}, { _, _ -> }, {}, {}, {}, { _, _, _ -> })
        }
        compose.onNodeWithText("最近一次答复").assertDoesNotExist()
        compose.onNodeWithText("Hermes 的回复").assertDoesNotExist()
        compose.onNodeWithText("diagnostic-only-marker", substring = true).assertDoesNotExist()
        compose.onNodeWithText("帮我梳理下一步。").assertIsDisplayed()
        screenshot("chat-clean-reply")
        compose.onNodeWithTag("reply-log-toggle").performClick()
        compose.onNodeWithTag("reply-log").assertExists()
        compose.onNodeWithText("diagnostic-only-marker", substring = true).assertExists()
    }
    @Test fun profileKeepsEditingMemoryAndSettingsInACompactLayout() {
        val state = AppUiState(route = AppRoute.PROFILE, username = "Jerome", reduceMotion = true,
            baseUrl = "https://example.invalid", hasSavedConnection = true)
        var openedMemory = false
        show {
            ProfileScreen(state, PaddingValues(bottom = 20.dp), false, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {},
                { openedMemory = true }, {}, {}, {}, { _, _ -> }, {}, {})
        }
        compose.onNodeWithText("编辑资料").assertIsDisplayed()
        compose.onNodeWithText("设置照片").assertDoesNotExist()
        screenshot("profile")
        compose.onNodeWithText("我的记忆").performClick()
        assertTrue(openedMemory)
    }

}

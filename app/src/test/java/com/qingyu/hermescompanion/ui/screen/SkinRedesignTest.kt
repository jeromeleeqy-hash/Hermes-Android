package com.qingyu.hermescompanion.ui.screen

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import com.qingyu.hermescompanion.ui.component.HermesButton as Button
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
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
import java.time.Instant

@OptIn(ExperimentalComposeUiApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "zh-rCN-w390dp-h844dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SkinRedesignTest {
    @get:Rule val compose = createComposeRule()
    private val longModel = "claude-sonnet-4-20250514-extended-thinking"
    private val sample = AppUiState(username = "Jerome", serverSettings = ServerSettings(models = ServerModelSettings(
        provider = "custom:team", model = longModel, reasoningEffort = "high",
        auxiliary = mapOf("vision" to ModelChoice("custom:team", "claude-sonnet-4"), "compression" to ModelChoice("deepseek", "deepseek-chat")),
    )), sessions = listOf(
        HermesSession("daily", "日常助理", updatedAt = Instant.now().toString(), preview = "今天先把三件重要的事情理清楚。<!-- hermes-mobile-context-v1:309 hidden"),
        HermesSession("video", "AI 短视频内容与转化复盘", updatedAt = Instant.now().minusSeconds(600).toString(), preview = "内容表现与私域咨询，有两个改进方向。"),
        HermesSession("meeting", "团队会议纪要", updatedAt = Instant.now().minusSeconds(86400).toString(), preview = "@file:/root/.hermes/attachments/团队会议纪要.md"),
        HermesSession("research", "新的商业模式研究", updatedAt = Instant.now().minusSeconds(90000).toString(), preview = "下一步先验证渠道与获客成本。"),
    ))

    private fun render(mode: SkinMode, fontScale: Float = 1f, content: @Composable () -> Unit) {
        compose.setContent { CompositionLocalProvider(LocalDensity provides Density(1f, fontScale)) {
            HermesCompanionTheme(ThemeMode.LIGHT, mode) { AmbientBackground { content() } }
        } }
        compose.waitForIdle()
    }
    private fun capture(name: String, node: SemanticsNodeInteraction = compose.onRoot()): Bitmap = compose.runOnIdle {
        val view = (node.fetchSemanticsNode().root as ViewRootForTest).view
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(android.graphics.Canvas(bitmap))
        File("build/ui-validation/351-$name.png").apply { parentFile.mkdirs() }.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) }
        bitmap
    }
    private fun sessions(mode: SkinMode) {
        var opened = ""
        var created = false
        render(mode) {
            Scaffold(containerColor = Color.Transparent, contentWindowInsets = WindowInsets(0,0,0,0), bottomBar = { ReferenceBottomDock(AppRoute.SESSIONS,false,{}) }) { padding ->
                HermesScene { SessionsScreen(sample,padding,{}, {created=true},{},{},{opened=it.id},{},{},{},{},{_,_->},{},{_,_->},{},{},{},{}) }
            }
        }
        compose.onNodeWithText("今天").assertIsDisplayed()
        compose.onNodeWithText("昨天").assertIsDisplayed()
        compose.onNodeWithText("附件 · 团队会议纪要.md").assertIsDisplayed()
        compose.onAllNodesWithText("hermes-mobile-context", substring=true).assertCountEquals(0)
        capture("sessions-${mode.name}")
        compose.onNodeWithTag("new_conversation").performClick()
        assertTrue(created)
        compose.onNodeWithText("日常助理").performClick()
        assertEquals("daily",opened)
    }
    @Test fun sessionsGlass() = sessions(SkinMode.GLASS)
    @Test fun sessionsWarm() = sessions(SkinMode.CLEAN)
    @Test fun sessionsPaper() = sessions(SkinMode.PAPER)

    private fun models(mode: SkinMode, large: Boolean = false) {
        render(mode, if(large) 1.3f else 1f) { HermesScene { ModelSettingsScreen(sample,PaddingValues(),{}, {}, {_,_,_,_,_->}) } }
        val result = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText(longModel,useUnmergedTree=true).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(result) }
        assertTrue("Long identifier must wrap without losing content",result.isNotEmpty() && result.none { it.hasVisualOverflow })
        capture("models-${mode.name}-${if(large) "large" else "normal"}")
        compose.onNodeWithText("备用模型").performScrollTo().assertIsDisplayed()
    }
    @Test fun modelsGlass() = models(SkinMode.GLASS)
    @Test fun modelsWarm() = models(SkinMode.CLEAN)
    @Test fun modelsPaper() = models(SkinMode.PAPER)
    @Test @Config(qualifiers="zh-rCN-w320dp-h640dp-mdpi") fun modelsSmallLargeText() = models(SkinMode.CLEAN,true)

    private fun voice(mode: SkinMode) {
        render(mode) { HermesScene { VoiceSettingsScreen(sample,PaddingValues(),{}, {}, {}, {}, {}, {}, {}, {}) } }
        compose.onNodeWithText("语音输入").assertIsDisplayed()
        capture("voice-${mode.name}")
        compose.onNodeWithText("语音识别服务").performScrollTo().assertIsDisplayed()
        capture("voice-server-${mode.name}")
        compose.onNodeWithText("保存语音服务设置").performScrollTo().assertIsDisplayed()
    }
    @Test fun voiceGlass() = voice(SkinMode.GLASS)
    @Test fun voiceWarm() = voice(SkinMode.CLEAN)
    @Test fun voicePaper() = voice(SkinMode.PAPER)

    private fun dialog(mode: SkinMode, large: Boolean = false) {
        var selected = 0
        render(mode,if(large) 1.3f else 1f) {
            var showing by remember { mutableStateOf(true) }
            HermesScene { Column(Modifier.padding(24.dp)) { repeat(14) { Text("日常工作与项目记录 $it",Modifier.padding(8.dp)) } } }
            if(showing) HermesAlertDialog(onDismissRequest={showing=false},title={Text("选择语音模型")},text={
                Column { Text("当前服务商 · OpenAI"); SettingsChoice("识别模型", "gpt-4o-mini-transcribe",model=true) {} }
            }, confirmButton={Button(onClick={selected++;showing=false}){Text("使用此模型")}},dismissButton={TextButton(onClick={showing=false}){Text("取消")}})
        }
        compose.onNodeWithText("使用此模型").assertIsDisplayed()
        capture("dialog-${mode.name}-${if(large) "large" else "normal"}",compose.onNode(isDialog()))
        compose.onNodeWithText("使用此模型").performClick()
        assertEquals(1,selected)
        compose.onNode(isDialog()).assertDoesNotExist()
    }
    @Test fun dialogGlass() = dialog(SkinMode.GLASS)
    @Test fun dialogWarm() = dialog(SkinMode.CLEAN)
    @Test fun dialogPaper() = dialog(SkinMode.PAPER)
    @Test @Config(qualifiers="zh-rCN-w320dp-h640dp-mdpi") fun dialogSmallLargeText() = dialog(SkinMode.GLASS,true)

    @Test fun modelSelectionCanBeCancelledAndOnlySavingCommitsTheDraft() {
        var saved: ServerModelSettings? = null
        val state = sample.copy(modelCatalog = ModelCatalog(providers = listOf(ModelProvider("custom:team", "团队模型", listOf(longModel,"qwen-3")))))
        render(SkinMode.CLEAN) { HermesScene { ModelSettingsScreen(state,PaddingValues(),{}, {saved=it}, {_,_,_,_,_->}) } }
        compose.onNodeWithText(longModel).performClick()
        compose.onNodeWithText("团队模型  ›").performClick()
        compose.onNodeWithText("使用此模型").assertIsNotEnabled()
        compose.onNodeWithText("qwen-3").performClick()
        compose.onNodeWithText("取消").performClick()
        compose.onNodeWithText(longModel).assertIsDisplayed()
        assertNull(saved)
        compose.onNodeWithText(longModel).performClick()
        compose.onNodeWithText("团队模型  ›").performClick()
        compose.onNodeWithText("qwen-3").performClick()
        compose.onNodeWithText("使用此模型").performClick()
        compose.onNodeWithText("qwen-3").assertIsDisplayed()
        assertNull(saved)
        compose.onNodeWithText("保存模型设置").performScrollTo().performClick()
        assertEquals("qwen-3",saved?.model)
        assertEquals(sample.serverSettings.models.auxiliary,saved?.auxiliary)
        assertEquals("high",saved?.reasoningEffort)
    }

    @Test fun voicePickerRequiresConfirmationAndCancellationKeepsPreferences() {
        var chosen: VoicePreferences? = null
        render(SkinMode.GLASS) { HermesScene { VoiceSettingsScreen(sample,PaddingValues(),{}, {chosen=it}, {}, {}, {}, {}, {}, {}) } }
        compose.onNodeWithText("语音引擎").performClick()
        compose.onNodeWithText("手机系统").performClick()
        assertNull(chosen)
        compose.onNodeWithText("取消").performClick()
        assertNull(chosen)
        compose.onNodeWithText("语音引擎").performClick()
        compose.onNodeWithText("手机系统").performClick()
        compose.onNodeWithText("确定").performClick()
        assertEquals("system",chosen?.engine)
    }

    @Test fun darkSkinsCanSwitchWhileReadingTheSameSettings() {
        var skin by mutableStateOf(SkinMode.GLASS)
        compose.setContent { HermesCompanionTheme(ThemeMode.DARK,skin) { AmbientBackground {
            HermesScene { ModelSettingsScreen(sample,PaddingValues(),{}, {}, {_,_,_,_,_->}) }
        } } }
        SkinMode.entries.forEach { mode ->
            compose.runOnIdle { skin = mode }
            compose.onNodeWithText(longModel).assertIsDisplayed()
            capture("models-dark-${mode.name}")
        }
    }

    @Test @Config(sdk=[26]) fun olderAndroidUsesSolidFallbackForGlassNavigation() {
        render(SkinMode.GLASS) {
            Scaffold(containerColor=Color.Transparent,bottomBar={ReferenceBottomDock(AppRoute.SESSIONS,false,{})}) { padding ->
                HermesScene { Text("旧系统兼容",Modifier.padding(padding)) }
            }
        }
        compose.onNodeWithTag("nav_SESSIONS").assertIsSelected()
        capture("glass-fallback-api26")
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test fun changingSkinWithASheetOpenKeepsItsActionsVisible() {
        var skin by mutableStateOf(SkinMode.GLASS)
        compose.setContent { HermesCompanionTheme(ThemeMode.LIGHT,skin) { AmbientBackground {
            HermesScene { Text("日常工作记录",Modifier.padding(24.dp)) }
            HermesModalBottomSheet(onDismissRequest={},sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true)) {
                Column(Modifier.fillMaxWidth().padding(24.dp)) {
                    Text("添加到对话",style=MaterialTheme.typography.titleLarge)
                    Text("选择文件或图片",Modifier.padding(vertical=16.dp))
                    Button(onClick={},modifier=Modifier.fillMaxWidth()) { Text("选择文件") }
                }
            }
        } } }
        SkinMode.entries.forEach { mode ->
            compose.runOnIdle { skin=mode }
            compose.onNodeWithText("选择文件").assertIsDisplayed()
            capture("sheet-${mode.name}",compose.onNodeWithText("添加到对话"))
        }
    }
}

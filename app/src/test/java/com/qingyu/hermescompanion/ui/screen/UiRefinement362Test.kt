package com.qingyu.hermescompanion.ui.screen

import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
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

@OptIn(ExperimentalComposeUiApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "zh-rCN-w390dp-h844dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class UiRefinement362Test {
    @get:Rule val compose = createComposeRule()
    @After fun resetLanguage() { AppLanguage.setMode(RuntimeEnvironment.getApplication(), AppLanguageMode.CHINESE) }
    private fun capture(name: String, node: SemanticsNodeInteraction = compose.onRoot()) = compose.runOnIdle {
        val view = (node.fetchSemanticsNode().root as ViewRootForTest).view
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(android.graphics.Canvas(bitmap))
        File("build/ui-validation/362-$name.png").apply { parentFile.mkdirs() }.outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    @Test fun glassSettingsFieldsStayEditableAndLegibleAcrossThemes() {
        var theme by mutableStateOf(ThemeMode.LIGHT)
        val state = AppUiState(serverSettings = ServerSettings(models = ServerModelSettings(provider = "openai", model = "gpt-5")))
        AppLanguage.setMode(RuntimeEnvironment.getApplication(), AppLanguageMode.CHINESE)
        compose.setContent { CompositionLocalProvider(LocalDensity provides Density(1f)) {
            HermesCompanionTheme(theme, SkinMode.GLASS) { AmbientBackground {
                HermesScene { ModelSettingsScreen(state, PaddingValues(), {}, {}, {_,_,_,_,_->}) }
            } }
        } }
        capture("models-light")
        compose.onNodeWithText("增加模型提供商").performScrollTo().assertIsDisplayed()
        capture("model-actions-light")
        compose.onNodeWithText("增加模型提供商").performClick()
        compose.onAllNodes(hasSetTextAction() and hasAnyAncestor(hasTestTag("sheet_surface"))).assertCountEquals(5)
        compose.onAllNodes(hasSetTextAction() and hasAnyAncestor(hasTestTag("sheet_surface")))[0].performTextInput("team")
        capture("provider-light", compose.onNodeWithTag("sheet_surface"))
        compose.runOnIdle { theme = ThemeMode.DARK }
        compose.onNodeWithText("team").assertIsDisplayed()
        capture("provider-dark", compose.onNodeWithTag("sheet_surface"))
        compose.onNodeWithText("取消").performClick()
        compose.onNodeWithTag("sheet_surface").assertDoesNotExist()
        capture("model-actions-dark")
    }

    @Test fun glassControlsMeetContrastAndPreserveInputAcrossThemeChanges() {
        var theme by mutableStateOf(ThemeMode.LIGHT)
        var value by mutableStateOf("")
        var saved = ""
        compose.setContent { HermesCompanionTheme(theme, SkinMode.GLASS) { AmbientBackground { HermesScene {
            val colors = glassControlColors()
            fun contrast(a: androidx.compose.ui.graphics.Color, b: androidx.compose.ui.graphics.Color): Float {
                val x = a.luminance(); val y = b.luminance()
                return (maxOf(x,y)+.05f)/(minOf(x,y)+.05f)
            }
            assertTrue("Field border must remain identifiable", contrast(colors.edge, colors.field) >= 3f)
            assertTrue("Action text must be readable over both gradient stops", listOf(colors.actionTop,colors.actionBottom).all { contrast(colors.onAction,it) >= 4.5f })
            Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Text("Hermes 3.6.2",style = MaterialTheme.typography.headlineSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    HermesMark(requestedSize = 88.dp)
                    UserAvatar("", "Hermes", 88.dp, hermesFallback = true)
                    UserAvatar("", "User", 88.dp)
                }
                HermesOutlinedTextField(value, { value = it }, Modifier.fillMaxWidth(), label = { Text("模型 / Model") })
                HermesButton(onClick = { saved = value }, modifier = Modifier.fillMaxWidth()) { Text("保存设置 / Save settings") }
                HermesOutlinedButton(onClick = {}, modifier = Modifier.fillMaxWidth()) { Text("测试连接 / Test connection") }
                HermesButton(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) { Text("正在保存 / Saving") }
                SettingsToggle("自动朗读 / Read aloud", "朗读完整回复 / Read the complete reply", true) {}
            }
        } } } }
        compose.onNode(hasSetTextAction()).performTextInput("team-model")
        capture("brand-controls-light")
        compose.runOnIdle { theme = ThemeMode.DARK }
        capture("brand-controls-dark")
        compose.onNodeWithText("保存设置 / Save settings").performClick()
        assertEquals("team-model", saved)
    }

    @Test fun allHistoricalVersionsRemainReachableAfterChangingLanguage() {
        val context = RuntimeEnvironment.getApplication()
        AppLanguage.setMode(context, AppLanguageMode.CHINESE)
        compose.setContent { HermesCompanionTheme(ThemeMode.LIGHT, SkinMode.GLASS) { AmbientBackground {
            HermesScene { ChangeLogScreen(PaddingValues(), {}) }
        } } }
        capture("history-zh")
        compose.runOnIdle { AppLanguage.setMode(context, AppLanguageMode.ENGLISH) }
        compose.onNodeWithText("Changelog").assertIsDisplayed()
        capture("history-en")
        compose.onNodeWithTag("release_history").performScrollToNode(hasTestTag("release_3.3.0-preview"))
        compose.onNodeWithTag("release_3.3.0-preview").assertIsDisplayed()
        compose.onNodeWithText("Version 3.3.0-preview").performClick()
        compose.onNodeWithText("Collaboration used a separate service.", substring = true).assertIsDisplayed()
        compose.onNodeWithTag("release_history").performScrollToNode(hasTestTag("release_0.5.1"))
        compose.onNodeWithTag("release_0.5.1").assertIsDisplayed()
        capture("history-oldest")
    }
}

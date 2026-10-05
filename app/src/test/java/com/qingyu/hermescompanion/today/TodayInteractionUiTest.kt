package com.qingyu.hermescompanion.today

import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import com.qingyu.hermescompanion.i18n.*
import com.qingyu.hermescompanion.ui.*
import com.qingyu.hermescompanion.ui.component.*
import com.qingyu.hermescompanion.ui.screen.*
import com.qingyu.hermescompanion.ui.theme.HermesCompanionTheme
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "zh-rCN-w390dp-h844dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TodayInteractionUiTest {
    @get:Rule val compose = createComposeRule()
    private val fixtures get() = TodayBoard.decode(File("src/main/assets/hermes-today-interactions.json").readText(), "/work").cards
    @Before fun language() { AppLanguage.setMode(RuntimeEnvironment.getApplication(), AppLanguageMode.CHINESE) }

    @OptIn(ExperimentalComposeUiApi::class)
    private fun screenshot(name: String, tag: String = "interactive-editor") = compose.runOnIdle {
        val view = (compose.onNodeWithTag(tag).fetchSemanticsNode().root as ViewRootForTest).view
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(android.graphics.Canvas(bitmap))
        val output = File("build/today-previews/377-$name.png"); output.parentFile.mkdirs()
        output.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun allTwentyNativeLayoutsRenderAndPrimaryActionStaysReachable() {
        var index by mutableIntStateOf(0)
        val cards = fixtures
        compose.setContent {
            HermesCompanionTheme(ThemeMode.LIGHT, SkinMode.CLEAN) { Surface(Modifier.fillMaxSize()) {
                TodayInteractionEditor(cards[index], preview = true, onSubmit = { error("Demo must not submit") })
            } }
        }
        cards.forEachIndexed { n, card ->
            compose.runOnIdle { index = n }
            compose.onNodeWithText(card.title).assertIsDisplayed()
            compose.onNodeWithTag("interaction-submit").assertIsDisplayed()
            screenshot(card.presentation.interaction!!.type.key)
        }
    }

    @Test fun choosingAndCheckingRemainDraftsUntilExplicitSubmission() {
        val card = fixtures.first()
        var submitted: String? = null
        compose.setContent { HermesCompanionTheme(ThemeMode.LIGHT, SkinMode.CLEAN) { Surface(Modifier.fillMaxSize()) {
            TodayInteractionEditor(card, onSubmit = { submitted = it })
        } } }
        compose.onNodeWithTag("interaction-submit").performClick()
        compose.onNodeWithTag("interaction-error").assertIsDisplayed()
        assertNull(submitted)
        compose.onNodeWithTag("interaction-select:same").performScrollTo().performClick()
        assertNull(submitted)
        assertEquals("open", card.status)
        compose.onNodeWithTag("interaction-submit").performClick()
        assertEquals("same", JSONObject(submitted!!).getJSONObject("input").getString("selection"))
    }

    @Test fun approvalAppearsAboveCardDetailsWithoutLosingTheSelection() {
        val card = fixtures.first()
        var requests by mutableStateOf(emptyList<com.qingyu.hermescompanion.model.AgentRequest>())
        var approved = ""
        var submitted = ""
        compose.setContent { HermesCompanionTheme(ThemeMode.LIGHT, SkinMode.CLEAN) {
            AgentRequestHost(requests, { _, answer -> approved = answer; requests = emptyList() }, {}) {
                TodayInteractiveSheet(card, enabled = true, onDismiss = {}, onSubmit = { submitted = it }, onPath = {})
            }
        } }
        compose.onNodeWithTag("interaction-select:same").performScrollTo().performClick()
        compose.runOnIdle { requests = listOf(com.qingyu.hermescompanion.model.AgentRequest("srq-example", "runtime", "session",
            com.qingyu.hermescompanion.model.AgentRequestType.APPROVAL, "写回已选择的事项", "核对后同步卡片", profile = "default",
            choices = listOf(com.qingyu.hermescompanion.model.AgentRequestChoice("once"), com.qingyu.hermescompanion.model.AgentRequestChoice("deny")), serverRequestId = "srq-example")) }
        compose.onNodeWithTag("decision_panel").assertIsDisplayed()
        compose.onNodeWithTag("agent-request-submit").assertIsNotEnabled()
        assertEquals("", approved)
        compose.onNodeWithTag("agent-choice:once").performScrollTo().performClick()
        screenshot("approval-overlay", "decision_panel")
        compose.onNodeWithTag("agent-request-submit").performClick()
        assertEquals("once", approved)
        compose.onNodeWithTag("interaction-submit").performClick()
        assertEquals("same", JSONObject(submitted).getJSONObject("input").getString("selection"))
    }

    @Test @Config(qualifiers = "zh-rCN-w360dp-h640dp-mdpi")
    fun largeFontsAndDarkThemeKeepInputAndActionAccessible() {
        val card = fixtures.single { it.presentation.interaction!!.type == TodayLayout.MEETING_ACTIONS }
        var skin by mutableStateOf(SkinMode.CLEAN)
        compose.setContent { CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 1.4f)) {
            HermesCompanionTheme(ThemeMode.DARK, skin) { Surface(Modifier.fillMaxSize()) { TodayInteractionEditor(card, preview = true, onSubmit = {}) } }
        } }
        SkinMode.entries.forEach { value ->
            compose.runOnIdle { skin = value }
            compose.onNodeWithTag("interaction-submit").assertIsDisplayed()
            compose.onNodeWithTag("interaction-check:training").performScrollTo().performClick()
            compose.onNodeWithTag("interaction-submit").performClick()
            compose.onNodeWithTag("interaction-preview-result").performScrollTo().assertIsDisplayed()
            screenshot("large-${value.name.lowercase()}")
        }
    }

    @Test fun galleryIsSeparateFromRealDataAndLetsUserChooseAllTypes() {
        compose.setContent { HermesCompanionTheme(ThemeMode.LIGHT, SkinMode.CLEAN) { AmbientBackground { HermesScene {
            // The optional component catalogue is tested in isolation; it is no longer a Home tool.
            TodayCardGallerySheet(onDismiss = {})
        } } } }
        compose.onNodeWithTag("today-gallery").assertIsDisplayed()
        screenshot("gallery")
        compose.onNodeWithTag("interaction-select:same").performScrollTo().performClick()
        compose.onNodeWithTag("interaction-submit").performClick()
        compose.onNodeWithTag("interaction-preview-result").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("gallery-next").performClick()
        compose.onNode(hasText("新流程，怎样开始？") and hasAnyAncestor(hasTestTag("today-gallery"))).assertIsDisplayed()
        compose.onNodeWithTag("gallery-picker").performClick()
        compose.onNodeWithText("快速记录").performScrollTo().performClick()
        compose.onNode(hasText("今天读了多久？") and hasAnyAncestor(hasTestTag("today-gallery"))).assertIsDisplayed()
    }

    @Test fun scheduleRequiresValidWallTimeAndTimezoneBeforeConfiguration() {
        var submitted: Triple<String, String, String>? = null
        compose.setContent { HermesCompanionTheme(ThemeMode.LIGHT, SkinMode.CLEAN) { TodayScheduleSheet({}, { a,b,c -> submitted = Triple(a,b,c) }) } }
        compose.onNodeWithTag("schedule-morning").performTextReplacement("25:00")
        compose.onNodeWithTag("schedule-submit").performScrollTo().performClick()
        assertNull(submitted)
        compose.onNodeWithTag("schedule-error").assertExists()
        compose.onNodeWithTag("schedule-morning").performScrollTo().performTextReplacement("08:30")
        compose.onNodeWithTag("schedule-zone").performScrollTo().performTextReplacement("Asia/Shanghai")
        compose.onNodeWithTag("schedule-submit").performScrollTo().performClick()
        assertEquals(Triple("08:30", "21:00", "Asia/Shanghai"), submitted)
    }
    @Test fun refreshPreservesDraftAndNeverSubmitsAgainstChangedRows() {
        var card by mutableStateOf(fixtures.single { it.presentation.interaction!!.type == TodayLayout.TRIAGE })
        compose.setContent { HermesCompanionTheme(ThemeMode.LIGHT, SkinMode.CLEAN) { Surface(Modifier.fillMaxSize()) {
            TodayInteractionEditor(card, onSubmit = { error("A stale draft must not be submitted") })
        } } }
        compose.runOnIdle {
            val source = JSONObject(File("src/main/assets/hermes-today-interactions.json").readText())
            val rows = source.getJSONArray("cards")
            for (n in 0 until rows.length()) if (rows.getJSONObject(n).getString("id") == card.id) {
                rows.getJSONObject(n).getJSONObject("presentation").getJSONObject("interaction").getJSONArray("items").getJSONObject(0).put("id", "new-row")
            }
            card = TodayBoard.decode(source.toString(), "/work").cards.single { it.id == card.id }
        }
        compose.onNodeWithTag("interaction-submit").assertIsNotEnabled()
        compose.onNodeWithText("载入更新内容").assertIsDisplayed().performClick()
        compose.onNodeWithTag("interaction-submit").assertIsEnabled()
    }

}

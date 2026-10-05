package com.qingyu.hermescompanion.ui.screen

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.qingyu.hermescompanion.i18n.*
import com.qingyu.hermescompanion.today.TodayBoard
import com.qingyu.hermescompanion.today.TodayState
import com.qingyu.hermescompanion.ui.*
import com.qingyu.hermescompanion.ui.component.*
import com.qingyu.hermescompanion.ui.theme.HermesCompanionTheme
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.*
import org.robolectric.annotation.*
import java.io.File
import kotlin.random.Random

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "zh-rCN-w390dp-h844dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class Hermes388UiTest {
    @get:Rule val compose = createComposeRule()
    @Before fun language() { AppLanguage.setMode(RuntimeEnvironment.getApplication(), AppLanguageMode.CHINESE) }

    @OptIn(ExperimentalComposeUiApi::class)
    private fun screenshot(name: String, tag: String) = compose.runOnIdle {
        val view = (compose.onNodeWithTag(tag).fetchSemanticsNode().root as ViewRootForTest).view
        val output = File("build/388-previews/$name.png"); output.parentFile.mkdirs()
        Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888).let { bitmap ->
            view.draw(android.graphics.Canvas(bitmap))
            output.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    @Test fun portraitFitsEveryHomeSkinInLightAndDarkWithoutOldBodyCropping() {
        var skin by mutableStateOf(SkinMode.CLEAN)
        var dark by mutableStateOf(false)
        val raw = File("src/main/assets/hermes-today-examples.json").readText()
        val state = AppUiState(reduceMotion = true, homeMode = HomeMode.DEEP,
            today = TodayState(profile = "default", root = "/work", rootVerified = true, loaded = true,
                fileExists = true, rawJson = raw, board = TodayBoard.decode(raw, "/work")))
        compose.setContent { HermesCompanionTheme(if (dark) ThemeMode.DARK else ThemeMode.LIGHT, skin) {
            AmbientBackground { HermesScene {
                TodayOverviewScreen(state.copy(skinMode = skin), PaddingValues(), {}, {}, { _, _ -> }, { _, _ -> }, {}, {}, {}, { _, _ -> }, {})
            } }
        } }
        for (night in listOf(false, true)) for (value in SkinMode.entries) {
            compose.runOnIdle { skin = value; dark = night }
            compose.onNodeWithTag("home-portrait").assertIsDisplayed().assertWidthIsEqualTo(76.dp).assertHeightIsEqualTo(82.dp)
            compose.onNodeWithTag("home-portrait-static", useUnmergedTree = true).assertExists()
            compose.onNodeWithTag("home-portrait").assertIsNotEnabled()
            screenshot("home-${value.name.lowercase()}-${if (night) "dark" else "light"}", "today_home")
        }
    }

    @Test fun packagedClipsAreNativeTransparentAnimationsAndDecodeAtTheSameSize() {
        val resources = RuntimeEnvironment.getApplication().resources
        HomePortraitClip.entries.forEach { clip ->
            val drawable = ImageDecoder.decodeDrawable(ImageDecoder.createSource(resources, clip.asset))
            assertTrue("${clip.name} must animate", drawable is AnimatedImageDrawable)
            assertEquals(320, drawable.intrinsicWidth)
            assertEquals(320, drawable.intrinsicHeight)
        }
    }

    @Test fun rotationWaitsBetweenClipsAndStopsWhenHiddenBackgroundedOrReduced() {
        val owner = object : LifecycleOwner {
            val registry = LifecycleRegistry(this)
            override val lifecycle: Lifecycle get() = registry
        }
        var visible by mutableStateOf(true)
        var reduce by mutableStateOf(false)
        var taps = 0
        val playback = HomePortraitPlayback(HomePortraitPlaylist(Random(388)))
        compose.runOnIdle { owner.registry.currentState = Lifecycle.State.RESUMED }
        compose.mainClock.autoAdvance = false
        compose.setContent { CompositionLocalProvider(LocalLifecycleOwner provides owner) {
            HermesCompanionTheme(ThemeMode.LIGHT, SkinMode.CLEAN) { Surface(color = MaterialTheme.colorScheme.background) {
                HomePortraitCarousel(Modifier.size(76.dp,82.dp), visible, reduce, { taps++ }, playback)
            } }
        } }
        fun awaitPhase(expected: HomePortraitPhase) {
            // AndroidView posts work on the Android main queue as well as the
            // Compose clock. Drain both between frames when resuming it.
            repeat(80) {
                compose.mainClock.advanceTimeByFrame()
                if (compose.runOnIdle { playback.phase == expected }) return
            }
            fail("Portrait did not reach $expected after visibility/lifecycle changed")
        }
        awaitPhase(HomePortraitPhase.PLAYING)
        compose.onNodeWithTag("home-portrait").performClick()
        compose.mainClock.advanceTimeBy(200)
        compose.runOnIdle { assertEquals(HomePortraitClip.WINK, playback.clip); assertEquals(1,taps) }
        compose.onNodeWithTag("home-portrait").performClick()
        compose.runOnIdle { assertEquals(1,taps); playback.complete(playback.generation) }
        compose.mainClock.advanceTimeBy(500)
        compose.runOnIdle { assertEquals(HomePortraitPhase.RESTING,playback.phase) }
        compose.mainClock.advanceTimeBy(3100)
        compose.runOnIdle { assertEquals(HomePortraitPhase.PLAYING,playback.phase); assertNotEquals(HomePortraitClip.WINK,playback.clip) }
        compose.runOnIdle { visible = false }
        awaitPhase(HomePortraitPhase.STATIC)
        val hiddenToken = compose.runOnIdle { playback.generation }
        compose.mainClock.advanceTimeBy(10000)
        compose.runOnIdle { assertEquals(HomePortraitPhase.STATIC,playback.phase); assertEquals(hiddenToken,playback.generation); visible = true }
        awaitPhase(HomePortraitPhase.PLAYING)
        compose.runOnIdle { owner.registry.currentState = Lifecycle.State.CREATED }
        awaitPhase(HomePortraitPhase.STATIC)
        val backgroundToken = compose.runOnIdle { playback.generation }
        compose.mainClock.advanceTimeBy(10000)
        compose.runOnIdle { assertEquals(HomePortraitPhase.STATIC,playback.phase); assertEquals(backgroundToken,playback.generation); owner.registry.currentState = Lifecycle.State.RESUMED }
        awaitPhase(HomePortraitPhase.PLAYING)
        compose.runOnIdle { reduce = true }
        awaitPhase(HomePortraitPhase.STATIC)
        compose.mainClock.advanceTimeBy(10000)
        compose.onNodeWithTag("home-portrait").assertIsNotEnabled()
        compose.runOnIdle { assertEquals(HomePortraitPhase.STATIC,playback.phase) }
    }
}

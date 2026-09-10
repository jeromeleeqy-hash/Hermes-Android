package com.qingyu.hermescompanion.ui.screen

import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.*
import com.qingyu.hermescompanion.appearance.LauncherIcon
import com.qingyu.hermescompanion.i18n.*
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
import java.time.ZonedDateTime
import java.io.File

@OptIn(ExperimentalComposeUiApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35],qualifiers="zh-rCN-w390dp-h844dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class UiRefinement364Test {
    @get:Rule val compose=createComposeRule()
    private val context get()=RuntimeEnvironment.getApplication()
    @Before fun language() { AppLanguage.setMode(context,AppLanguageMode.CHINESE) }
    @After fun resetLanguage() { AppLanguage.setMode(context,AppLanguageMode.CHINESE) }
    private fun capture(name:String): Bitmap = compose.runOnIdle {
        val view=(compose.onRoot().fetchSemanticsNode().root as ViewRootForTest).view
        Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888).also {
            view.draw(android.graphics.Canvas(it))
            File("build/ui-validation/364-$name.png").apply { parentFile.mkdirs() }.outputStream().use { out -> it.compress(Bitmap.CompressFormat.PNG,100,out) }
        }
    }

    @Test fun greetingUpdatesAtMinuteTickAndAfterClockOrTimezoneChanges() {
        var time=ZonedDateTime.parse("2026-09-09T23:59:00+08:00")
        val source:()->ZonedDateTime={time}
        compose.setContent { CompositionLocalProvider(LocalHomeClock provides source) {
            HermesCompanionTheme(ThemeMode.LIGHT,SkinMode.GLASS) { AmbientBackground { HermesScene {
                AssistantHomeScreen(AppUiState(route=AppRoute.HOME,username="admin",homeWelcomed=true,reduceMotion=true),PaddingValues(),{},{},{},{},{},{_,_->},{})
            } } }
        } }
        compose.onNodeWithText("晚上好，admin").assertExists()
        compose.runOnIdle { time=ZonedDateTime.parse("2026-09-10T08:08:00+08:00");context.sendBroadcast(Intent(Intent.ACTION_TIME_TICK)) }
        compose.onNodeWithText("早上好，admin").assertExists()
        compose.runOnIdle { time=ZonedDateTime.parse("2026-09-10T12:01:00+08:00");context.sendBroadcast(Intent(Intent.ACTION_TIME_CHANGED)) }
        compose.onNodeWithText("中午好，admin").assertExists()
        compose.runOnIdle { time=ZonedDateTime.parse("2026-09-10T16:01:00+12:00");context.sendBroadcast(Intent(Intent.ACTION_TIMEZONE_CHANGED)) }
        compose.onNodeWithText("下午好，admin").assertExists()
        capture("greeting-afternoon")
    }

    @Test fun gesturesNeverRestartOrQueueAndAlternateAfterCompletion() {
        val interaction=MascotInteraction()
        interaction.tap(false);assertNull(interaction.motion)
        interaction.tap(true);assertEquals(MascotMotion.CONFIDENT,interaction.motion)
        repeat(5){interaction.tap(true)};assertEquals(MascotMotion.CONFIDENT,interaction.motion)
        interaction.finish();interaction.tap(true);assertEquals(MascotMotion.STRETCH,interaction.motion)
        interaction.finish();interaction.tap(true);assertEquals(MascotMotion.CONFIDENT,interaction.motion)
    }

    @Test fun foregroundResumeRefreshesClockAndBackgroundRemovesMinuteListener() {
        val owner=object:androidx.lifecycle.LifecycleOwner {
            val registry=androidx.lifecycle.LifecycleRegistry(this)
            override val lifecycle:androidx.lifecycle.Lifecycle get()=registry
        }
        var time=ZonedDateTime.parse("2026-09-09T23:00:00+08:00")
        val source:()->ZonedDateTime={time}
        compose.runOnIdle { owner.registry.currentState=androidx.lifecycle.Lifecycle.State.RESUMED }
        compose.setContent { CompositionLocalProvider(LocalHomeClock provides source,androidx.lifecycle.compose.LocalLifecycleOwner provides owner) {
            Text(rememberHomeTime().hour.toString())
        } }
        compose.onNodeWithText("23").assertExists()
        compose.runOnIdle {
            owner.registry.currentState=androidx.lifecycle.Lifecycle.State.CREATED
            time=ZonedDateTime.parse("2026-09-10T08:00:00+08:00")
            context.sendBroadcast(Intent(Intent.ACTION_TIME_TICK))
        }
        compose.onNodeWithText("23").assertExists()
        compose.runOnIdle { owner.registry.currentState=androidx.lifecycle.Lifecycle.State.RESUMED }
        compose.onNodeWithText("8").assertExists()
    }

    @Test fun bothIconsRemainSelectableInEverySkinAndLanguage() {
        var skin by mutableStateOf(SkinMode.CLEAN)
        var selected by mutableStateOf(LauncherIcon.PARTNER)
        var busy by mutableStateOf(false)
        compose.setContent { HermesCompanionTheme(ThemeMode.LIGHT,skin) { Surface { Column(Modifier.padding(24.dp)) {
            LauncherIconChoices(selected,busy){selected=it}
        } } } }
        for(mode in SkinMode.entries) {
            compose.runOnIdle { skin=mode }
            compose.onNodeWithTag("launcher_icon_SPRITE").performClick().assertIsSelected()
            compose.onNodeWithTag("launcher_icon_PARTNER").performClick().assertIsSelected()
            capture("icons-${mode.name}")
        }
        compose.runOnIdle { AppLanguage.setMode(context,AppLanguageMode.ENGLISH);busy=true }
        compose.onNodeWithText("Partner",useUnmergedTree=true).assertExists()
        compose.onNodeWithTag("launcher_icon_SPRITE").assertIsNotEnabled()
    }

    @Test fun lowerDockCornersExposeTheSameSceneWithOpaqueContentBehindThem() {
        var dark by mutableStateOf(false)
        var whiteContent by mutableStateOf(true)
        var inset by mutableStateOf(24)
        var dockState:DockOcclusionState?=null
        compose.setContent { CompositionLocalProvider(LocalDensity provides Density(1f)) {
            HermesCompanionTheme(if(dark)ThemeMode.DARK else ThemeMode.LIGHT,SkinMode.GLASS) { AmbientBackground {
                val dock=LocalDockOcclusion.current
                SideEffect { dockState=dock }
                Scaffold(containerColor=Color.Transparent,contentWindowInsets=WindowInsets(0,0,0,0),bottomBar={
                    ReferenceBottomDock(AppRoute.SESSIONS,false,{},WindowInsets(0,0,0,inset))
                }) { _ -> HermesScene {
                    // Extreme regression case: a solid list card behind the entire dock.
                    Box(Modifier.fillMaxSize().background(if(whiteContent)Color.White else Color.Transparent))
                } }
            } }
        } }
        for(isDark in listOf(false,true)) for(bottom in listOf(0,24,48)) {
            compose.runOnIdle { dark=isDark;inset=bottom;whiteContent=true }
            compose.waitForIdle()
            val full=capture("dock-${if(isDark)"dark" else "light"}-$bottom")
            val geometry=requireNotNull(dockState?.geometry)
            val origin=IntArray(2)
            compose.runOnIdle { (compose.onRoot().fetchSemanticsNode().root as ViewRootForTest).view.getLocationInWindow(origin) }
            val b=geometry.bounds.translate(Offset(-origin[0].toFloat(),-origin[1].toFloat()))
            compose.runOnIdle { whiteContent=false }
            val ambient=capture("dock-background-${if(isDark)"dark" else "light"}-$bottom")
            val samples=listOf(Offset(b.left+2,b.bottom-2),Offset(b.right-2,b.bottom-2),Offset(b.left+8,b.bottom-3),Offset(b.center.x,b.bottom+4))
            samples.forEach { p ->
                if(p.y<full.height) {
                    val a=full.getPixel(p.x.toInt(),p.y.toInt());val z=ambient.getPixel(p.x.toInt(),p.y.toInt())
                    for(shift in listOf(0,8,16)) assertTrue("Corner must expose same gradient: $p",kotlin.math.abs((a shr shift and 255)-(z shr shift and 255))<=3)
                }
            }
            // Route content remains present inside the lower rounded part.
            val center=Offset(b.center.x,b.bottom-12)
            assertNotEquals(full.getPixel(center.x.toInt(),center.y.toInt()),ambient.getPixel(center.x.toInt(),center.y.toInt()))
        }
    }
}

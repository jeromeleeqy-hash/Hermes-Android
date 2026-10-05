package com.qingyu.hermescompanion.ui.component

import android.animation.ValueAnimator
import android.graphics.ImageDecoder
import android.graphics.drawable.Animatable2
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.widget.ImageView
import androidx.annotation.RequiresApi
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.qingyu.hermescompanion.R
import com.qingyu.hermescompanion.today.todayText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private data class PortraitFrame(val clip: HomePortraitClip, val generation: Int)

/** A separate compact portrait keeps the original full-body Simple Home intact. */
@Composable
internal fun HomePortraitCarousel(modifier: Modifier = Modifier, active: Boolean = true,
    reduceMotion: Boolean = false, onTap: () -> Unit = {},
    playback: HomePortraitPlayback = remember { HomePortraitPlayback() }) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var resumed by remember(lifecycle) { mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, _ ->
            resumed = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer); playback.pause() }
    }
    val running = active && resumed && !reduceMotion && !LocalReduceMotion.current &&
        ValueAnimator.areAnimatorsEnabled() && Build.VERSION.SDK_INT >= 28
    LaunchedEffect(running, playback.generation, playback.phase) {
        if (!running) { playback.pause(); return@LaunchedEffect }
        when (playback.phase) {
            HomePortraitPhase.STATIC -> playback.advance()
            HomePortraitPhase.RESTING -> { delay(playback.restMs()); playback.advance() }
            HomePortraitPhase.PLAYING -> {
                val token = playback.generation
                // End callbacks normally drive playback. A failed decoder or an
                // OEM missing a callback must not freeze the carousel forever.
                delay(playback.clip.durationMs + 1800)
                playback.complete(token)
            }
        }
    }
    val frame = if (running && playback.phase == HomePortraitPhase.PLAYING)
        PortraitFrame(playback.clip, playback.generation) else null
    val label = todayText("点一下，和她打个招呼", "Tap to say hello")
    Crossfade(frame, modifier.testTag("home-portrait")
        .semantics { contentDescription = label }
        .clickable(enabled = running, role = Role.Button) { if (playback.tap(running)) onTap() },
        animationSpec = tween(if (running) 160 else 0), label = "homePortrait") { target ->
        if (target != null && Build.VERSION.SDK_INT >= 28) {
            key(target) {
                AnimatedHomePortrait(target.clip, running && target == frame) {
                    playback.complete(target.generation)
                }
            }
        } else PortraitPoster()
    }
}

@Composable
private fun PortraitPoster() {
    Image(painterResource(R.drawable.home_portrait_poster), null, contentScale = ContentScale.Fit,
        modifier = Modifier.fillMaxSize().testTag("home-portrait-static"))
}

@RequiresApi(28)
@Composable
private fun AnimatedHomePortrait(clip: HomePortraitClip, playing: Boolean, onFinished: () -> Unit) {
    val context = LocalContext.current
    val finished by rememberUpdatedState(onFinished)
    val decoded by produceState<Pair<Boolean, Drawable?>>(false to null, clip, context) {
        val drawable = withContext(Dispatchers.IO) {
            runCatching { ImageDecoder.decodeDrawable(ImageDecoder.createSource(context.resources, clip.asset)) }.getOrNull()
        }
        value = true to drawable
    }
    val drawable = decoded.second
    LaunchedEffect(decoded, playing) {
        if (playing && decoded.first && drawable !is AnimatedImageDrawable) finished()
    }
    DisposableEffect(drawable, playing) {
        val animation = drawable as? AnimatedImageDrawable
        var disposed = false
        val callback = object : Animatable2.AnimationCallback() {
            override fun onAnimationEnd(drawable: Drawable?) { if (!disposed && playing) finished() }
        }
        if (playing) {
            animation?.repeatCount = 0
            animation?.registerAnimationCallback(callback)
            animation?.start()
        } else animation?.stop()
        // Keep the guarded callback attached until GC: Android can have an end
        // event queued that still dereferences its callback list after stop().
        onDispose { disposed = true; animation?.stop() }
    }
    if (drawable == null) PortraitPoster()
    else AndroidView(factory = { ImageView(it).apply { scaleType = ImageView.ScaleType.FIT_CENTER } },
        modifier = Modifier.fillMaxSize().testTag("home-portrait-${clip.name.lowercase()}"),
        update = { if (it.drawable !== drawable) it.setImageDrawable(drawable) },
        onRelease = { view -> (view.drawable as? AnimatedImageDrawable)?.stop(); view.setImageDrawable(null) })
}

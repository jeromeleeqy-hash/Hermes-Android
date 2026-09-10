package com.qingyu.hermescompanion.ui.component

import android.animation.ValueAnimator
import android.graphics.ImageDecoder
import android.graphics.drawable.Animatable2
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.qingyu.hermescompanion.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

val LocalReduceMotion = staticCompositionLocalOf { false }

enum class MascotMotion(@param:DrawableRes val asset: Int, @param:DrawableRes val poster: Int, val loop: Boolean, val half: Boolean) {
    IDLE_FULL(R.drawable.mascot_01b_idle_full, R.drawable.mascot_01b_idle_full_poster, true, false),
    IDLE_HALF(R.drawable.mascot_01a_idle_half, R.drawable.mascot_01a_idle_half_poster, true, true),
    WELCOME(R.drawable.mascot_02_welcome, R.drawable.mascot_02_welcome_poster, false, false),
    LISTENING(R.drawable.mascot_03_listening, R.drawable.mascot_03_listening_poster, true, true),
    WORKING(R.drawable.mascot_04_working, R.drawable.mascot_04_working_poster, true, true),
    DONE(R.drawable.mascot_05_done, R.drawable.mascot_05_done_poster, false, true),
    CONFIDENT(R.drawable.mascot_06_confident, R.drawable.mascot_06_confident_poster, false, false),
    STRETCH(R.drawable.mascot_08_stretch, R.drawable.mascot_08_stretch_poster, false, false),
}

/** Each visible instance owns its decoder; pause in background and dispose offscreen. */
@Composable
fun HermesMascot(motion: MascotMotion, modifier: Modifier = Modifier, contentDescription: String? = null,
    active: Boolean = true, waistUp: Boolean = false, widePose: Boolean = false,
    alignStandingBody: Boolean = false, onFinished: () -> Unit = {}) {
    val reduce = LocalReduceMotion.current || !ValueAnimator.areAnimatorsEnabled()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var resumed by remember(lifecycle) { mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, _ -> resumed = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    val finished by rememberUpdatedState(onFinished)
    LaunchedEffect(motion, reduce) { if (!motion.loop && (reduce || Build.VERSION.SDK_INT < 28)) finished() }
    Crossfade(motion, modifier = modifier.testTag("mascot_${motion.name}"), animationSpec = tween(if (reduce) 0 else 180), label = "mascotState") { artwork ->
        // Only crop the transparent margin, using the union of ALL animation frames.
        Layout(content = {
            if (Build.VERSION.SDK_INT >= 28 && !reduce && active && resumed) AnimatedMascot(artwork, contentDescription) {
                if (artwork == motion) finished()
            }
            else Image(painterResource(artwork.poster), contentDescription, contentScale = ContentScale.Fit)
        }, modifier = Modifier.clipToBounds()) { measurables, constraints ->
            val width = constraints.maxWidth
            val height = constraints.maxHeight
            val interaction = artwork == MascotMotion.CONFIDENT || artwork == MascotMotion.STRETCH
            val cropWidth = if (waistUp) if (interaction) 464f else 420f else if (artwork.half) 450f else if (widePose || interaction) 464f else 400f
            // Stable visual anchors measured from the supplied frames. Using
            // the canvas centre (or the old 270px crop centre) shifts the body.
            // Never follow each moving frame: natural leaning must remain motion.
            val anchor = if (interaction) 310f else if (artwork.half) 315f else 310f
            val cropLeft = anchor - cropWidth / 2f
            // In the warm home layout, the body spans the greeting-to-button
            // interval. Exclude transparent head/foot margins, retaining the
            // union of the standing animation frames rather than following motion.
            val standing = alignStandingBody && !artwork.half && !waistUp
            val cropTop = if (standing) 24f else 0f
            val cropHeight = if (standing) 605f else if (waistUp) if (interaction) 390f else 520f else 640f
            val scale = minOf(width / cropWidth, height / cropHeight)
            val side = (640 * scale).roundToInt().coerceAtLeast(1)
            val child = measurables.single().measure(Constraints.fixed(side, side))
            layout(width, height) { child.place(((width - cropWidth * scale) / 2 - cropLeft * scale).roundToInt(), (height - (cropHeight + cropTop) * scale).roundToInt()) }
        }
    }
}

@RequiresApi(28)
@Composable
private fun AnimatedMascot(motion: MascotMotion, description: String?, onFinished: () -> Unit) {
    val context = LocalContext.current
    val finished by rememberUpdatedState(onFinished)
    val decoded by produceState<Pair<Boolean, Drawable?>>(false to null, motion.asset, context) {
        val result = withContext(Dispatchers.IO) {
            runCatching { ImageDecoder.decodeDrawable(ImageDecoder.createSource(context.resources, motion.asset)) }.getOrNull()
        }
        value = true to result
    }
    val drawable = decoded.second
    LaunchedEffect(decoded) { if (decoded.first && drawable !is AnimatedImageDrawable && !motion.loop) finished() }
    DisposableEffect(drawable) {
        val animation = drawable as? AnimatedImageDrawable
        var disposed = false
        val callback = object : Animatable2.AnimationCallback() {
            override fun onAnimationEnd(drawable: Drawable?) { if (!disposed && !motion.loop) finished() }
        }
        animation?.repeatCount = if (motion.loop) AnimatedImageDrawable.REPEAT_INFINITE else 0
        if (!motion.loop) animation?.registerAnimationCallback(callback)
        animation?.start()
        // Android may already have queued an end callback that dereferences its
        // callback list. Keep the guarded callback with this owned drawable until
        // GC instead of clearing the list while that platform message is pending.
        onDispose { disposed = true; animation?.stop() }
    }
    if (drawable == null) Image(painterResource(motion.poster), description, contentScale = ContentScale.Fit)
    else AndroidView(factory = { ImageView(it).apply { scaleType = ImageView.ScaleType.FIT_CENTER } },
        update = { view ->
            if (view.drawable !== drawable) view.setImageDrawable(drawable)
            view.contentDescription = description
        }, onRelease = { view -> (view.drawable as? AnimatedImageDrawable)?.stop(); view.setImageDrawable(null) })
}

@Composable
fun HermesWelcomeAnimation(modifier: Modifier = Modifier, contentDescription: String? = null) {
    var greeted by rememberSaveable { mutableStateOf(false) }
    HermesMascot(if (greeted) MascotMotion.IDLE_FULL else MascotMotion.WELCOME, modifier, contentDescription,
        onFinished = { greeted = true })
}

package com.qingyu.hermescompanion.ui.screen

import android.content.Context
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.view.Surface
import android.view.TextureView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.qingyu.hermescompanion.R
import com.qingyu.hermescompanion.i18n.uiText
import com.qingyu.hermescompanion.ui.component.KeepScreenAwake
import kotlinx.coroutines.delay

/** One local, muted welcome clip; every exit funnels through the same completion guard. */
@Composable
fun LaunchIntroScreen(reduceMotion: Boolean, onFinished: () -> Unit) {
    val latestFinish by rememberUpdatedState(onFinished)
    var finished by remember { mutableStateOf(false) }
    val finish = { if (!finished) { finished = true; latestFinish() } }
    var position by rememberSaveable { mutableIntStateOf(0) }
    var rendering by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var resumed by remember { mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
    val player = remember(context) { IntroPlayer(context, position, { rendering = true }, finish) }
    KeepScreenAwake()
    BackHandler(onBack = finish)
    DisposableEffect(lifecycle, player) {
        val observer = LifecycleEventObserver { _, _ ->
            resumed = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
            player.setForeground(resumed)
            position = player.position
        }
        lifecycle.addObserver(observer)
        player.setForeground(resumed)
        onDispose { lifecycle.removeObserver(observer); position = player.position; player.close() }
    }
    LaunchedEffect(resumed, reduceMotion) {
        if (resumed) {
            // Missing codec / interrupted preparation must never block initial setup.
            delay(if (reduceMotion) 250 else 9_000)
            finish()
        }
    }
    Box(Modifier.fillMaxSize().background(Color(0xFF101829)).testTag("launch_intro")) {
        if (!reduceMotion) AndroidView(
            factory = { TextureView(it).also(player::attach) },
            modifier = Modifier.fillMaxSize(),
        )
        if (!rendering || reduceMotion) Image(painterResource(R.drawable.hermes_intro_poster), null,
            Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        TextButton(onClick = finish, modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding()
            .padding(16.dp).background(Color.Black.copy(alpha = .42f), androidx.compose.foundation.shape.CircleShape)
            .heightIn(min = 48.dp).testTag("intro_skip")) {
            Text(uiText(R.string.intro_skip, "跳过"), color = Color.White)
        }
    }
}

private class IntroPlayer(
    private val context: Context, initialPosition: Int,
    private val onRendering: () -> Unit, private val onFinished: () -> Unit,
) : TextureView.SurfaceTextureListener, AutoCloseable {
    private var view: TextureView? = null
    private var player: MediaPlayer? = null
    private var surface: Surface? = null
    private var prepared = false
    private var foreground = false
    private var closed = false
    private var savedPosition = initialPosition
    val position: Int get() = if (prepared) runCatching { player?.currentPosition ?: savedPosition }.getOrDefault(savedPosition) else savedPosition
    fun attach(target: TextureView) { view = target; target.surfaceTextureListener = this }
    fun setForeground(value: Boolean) {
        foreground = value
        if (prepared) runCatching {
            if (value) player?.start() else { savedPosition = position; player?.pause() }
        }.onFailure { if (!closed) onFinished() }
    }
    override fun onSurfaceTextureAvailable(texture: SurfaceTexture, width: Int, height: Int) {
        if (closed) return
        transform(width, height)
        runCatching {
            surface = Surface(texture)
            val media = MediaPlayer()
            player = media
            media.setSurface(surface)
            context.resources.openRawResourceFd(R.raw.hermes_intro).use { fd ->
                media.setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
            }
            media.setVolume(0f, 0f)
            media.isLooping = false
            media.setOnPreparedListener {
                if (!closed && player === it) {
                    prepared = true
                    if (savedPosition > 0) it.seekTo(savedPosition)
                    if (foreground) it.start()
                }
            }
            media.setOnInfoListener { _, what, _ ->
                if (!closed && what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) onRendering()
                false
            }
            media.setOnCompletionListener { if (!closed) onFinished() }
            media.setOnErrorListener { _, _, _ -> if (!closed) onFinished(); true }
            media.prepareAsync()
        }.onFailure { if (!closed) onFinished() }
    }
    private fun transform(width: Int, height: Int) {
        if (width <= 0 || height <= 0) return
        val videoAspect = 1080f / 1920f
        val viewAspect = width.toFloat() / height
        val sx = if (viewAspect < videoAspect) videoAspect / viewAspect else 1f
        val sy = if (viewAspect > videoAspect) viewAspect / videoAspect else 1f
        view?.setTransform(Matrix().apply { setScale(sx, sy, width / 2f, height / 2f) })
    }
    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) = transform(width, height)
    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) = Unit
    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean { releaseMedia(); return true }
    private fun releaseMedia() {
        savedPosition = position
        prepared = false
        player?.setOnCompletionListener(null)
        player?.setOnErrorListener(null)
        player?.setOnPreparedListener(null)
        player?.setOnInfoListener(null)
        player?.release(); player = null
        surface?.release(); surface = null
    }
    override fun close() { closed = true; releaseMedia(); view?.surfaceTextureListener = null; view = null }
}

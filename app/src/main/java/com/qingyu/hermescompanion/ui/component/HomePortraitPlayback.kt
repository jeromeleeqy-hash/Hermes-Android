package com.qingyu.hermescompanion.ui.component

import androidx.annotation.DrawableRes
import androidx.compose.runtime.*
import com.qingyu.hermescompanion.R
import kotlin.random.Random

internal enum class HomePortraitClip(@param:DrawableRes val asset: Int, val durationMs: Long) {
    BREATHE(R.drawable.home_portrait_breathe, 4800),
    GREET(R.drawable.home_portrait_greet, 5550),
    LISTEN(R.drawable.home_portrait_listen, 5500),
    THINK(R.drawable.home_portrait_think, 5550),
    NOD(R.drawable.home_portrait_nod, 5050),
    WINK(R.drawable.home_portrait_wink, 5450),
}

/** Four calm clips per round, plus alternating accents; never repeat a boundary. */
internal class HomePortraitPlaylist(private val random: Random = Random.Default) {
    private val queue = mutableListOf<HomePortraitClip>()
    private var last: HomePortraitClip? = null
    private var nextAccent = HomePortraitClip.THINK

    fun record(clip: HomePortraitClip) { last = clip }

    fun next(): HomePortraitClip {
        if (queue.isEmpty()) {
            queue += listOf(HomePortraitClip.BREATHE, HomePortraitClip.GREET,
                HomePortraitClip.LISTEN, HomePortraitClip.NOD, nextAccent).shuffled(random)
            nextAccent = if (nextAccent == HomePortraitClip.THINK) HomePortraitClip.WINK else HomePortraitClip.THINK
        }
        if (queue.first() == last) {
            val alternate = queue.indexOfFirst { it != last }
            // A manual wink can consume the only remaining accent. Start a new
            // round instead of immediately showing that same wink again.
            if (alternate < 0) { queue.clear(); return next() }
            val first = queue[0]; queue[0] = queue[alternate]; queue[alternate] = first
        }
        return queue.removeAt(0).also(::record)
    }

    fun restMs(): Long = random.nextLong(1000, 3001)
}

internal enum class HomePortraitPhase { STATIC, PLAYING, RESTING }

/** Generation tokens reject callbacks from interrupted or disposed drawables. */
@Stable
internal class HomePortraitPlayback(private val playlist: HomePortraitPlaylist = HomePortraitPlaylist()) {
    var clip by mutableStateOf(HomePortraitClip.BREATHE)
        private set
    var phase by mutableStateOf(HomePortraitPhase.STATIC)
        private set
    var generation by mutableIntStateOf(0)
        private set

    private fun play(next: HomePortraitClip) {
        clip = next
        generation++
        phase = HomePortraitPhase.PLAYING
        playlist.record(next)
    }

    fun advance() { if (phase != HomePortraitPhase.PLAYING) play(playlist.next()) }
    fun complete(token: Int) {
        if (token == generation && phase == HomePortraitPhase.PLAYING) phase = HomePortraitPhase.RESTING
    }
    fun tap(enabled: Boolean): Boolean {
        if (!enabled || (phase == HomePortraitPhase.PLAYING && clip == HomePortraitClip.WINK)) return false
        play(HomePortraitClip.WINK)
        return true
    }
    fun pause() {
        if (phase != HomePortraitPhase.STATIC) { generation++; phase = HomePortraitPhase.STATIC }
    }
    fun restMs(): Long = playlist.restMs()
}

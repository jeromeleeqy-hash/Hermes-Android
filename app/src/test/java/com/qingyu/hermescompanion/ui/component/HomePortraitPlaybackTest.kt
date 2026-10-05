package com.qingyu.hermescompanion.ui.component

import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class HomePortraitPlaybackTest {
    @Test fun roundsIncludeEachCalmMotionOnceAndAlternateOccasionalAccents() {
        val playlist = HomePortraitPlaylist(Random(388))
        val calm = setOf(HomePortraitClip.BREATHE, HomePortraitClip.GREET, HomePortraitClip.LISTEN, HomePortraitClip.NOD)
        var previous: HomePortraitClip? = null
        repeat(20) { round ->
            val clips = List(5) { playlist.next() }
            assertEquals(5, clips.toSet().size)
            assertTrue(clips.containsAll(calm))
            assertTrue(clips.contains(if (round % 2 == 0) HomePortraitClip.THINK else HomePortraitClip.WINK))
            assertNotEquals(previous, clips.first())
            previous = clips.last()
            assertTrue(playlist.restMs() in 1000L..3000L)
        }
    }

    @Test fun tapInterruptsOnceAndRejectsOldCompletionCallbacks() {
        val playback = HomePortraitPlayback(HomePortraitPlaylist(Random(3)))
        playback.advance()
        val oldToken = playback.generation
        assertTrue(playback.tap(true))
        val winkToken = playback.generation
        assertEquals(HomePortraitClip.WINK, playback.clip)
        repeat(10) { assertFalse(playback.tap(true)) }
        assertEquals(winkToken, playback.generation)
        playback.complete(oldToken)
        assertEquals(HomePortraitPhase.PLAYING, playback.phase)
        playback.complete(winkToken)
        assertEquals(HomePortraitPhase.RESTING, playback.phase)
        playback.advance()
        assertNotEquals(HomePortraitClip.WINK, playback.clip)
    }

    @Test fun pausingInvalidatesCallbacksAndDisabledTapsDoNothing() {
        val playback = HomePortraitPlayback()
        assertFalse(playback.tap(false))
        assertEquals(HomePortraitPhase.STATIC, playback.phase)
        playback.advance()
        val token = playback.generation
        playback.pause()
        playback.complete(token)
        assertEquals(HomePortraitPhase.STATIC, playback.phase)
        val paused = playback.generation
        playback.pause()
        assertEquals(paused, playback.generation)
        playback.advance()
        assertEquals(HomePortraitPhase.PLAYING, playback.phase)
        assertTrue(playback.generation > token)
    }

    @Test fun aManualWinkNeverMakesTheNextAutomaticClipRepeatIt() {
        val playlist = HomePortraitPlaylist(Random(7))
        repeat(80) {
            playlist.next()
            playlist.record(HomePortraitClip.WINK)
            assertNotEquals(HomePortraitClip.WINK, playlist.next())
        }
    }
}

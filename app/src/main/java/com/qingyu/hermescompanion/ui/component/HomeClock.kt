package com.qingyu.hermescompanion.ui.component

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import android.os.Build
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.time.ZonedDateTime

internal val LocalHomeClock = staticCompositionLocalOf<() -> ZonedDateTime> { { ZonedDateTime.now() } }
internal enum class DayPeriod { MORNING, NOON, AFTERNOON, EVENING }
internal fun dayPeriod(hour: Int) = when (hour) {
    in 5..10 -> DayPeriod.MORNING
    in 11..13 -> DayPeriod.NOON
    in 14..18 -> DayPeriod.AFTERNOON
    else -> DayPeriod.EVENING
}

/** Android's minute tick while visible, plus immediate clock/timezone/resume updates. */
@Composable
internal fun rememberHomeTime(): ZonedDateTime {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val clock = LocalHomeClock.current
    var now by remember(clock) { mutableStateOf(clock()) }
    DisposableEffect(context, lifecycle, clock) {
        var registered = false
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) { now = clock() }
        }
        fun sync() {
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                now = clock()
                if (!registered) {
                    val filter = IntentFilter().apply {
                        addAction(Intent.ACTION_TIME_TICK); addAction(Intent.ACTION_TIME_CHANGED); addAction(Intent.ACTION_TIMEZONE_CHANGED)
                    }
                    if (Build.VERSION.SDK_INT >= 33) {
                        context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
                    } else {
                        // These are protected system broadcasts. The platform
                        // registration avoids a synthetic permission on older Android.
                        @Suppress("DEPRECATION")
                        context.registerReceiver(receiver, filter)
                    }
                    registered = true
                }
            } else if (registered) { context.unregisterReceiver(receiver); registered = false }
        }
        val observer = LifecycleEventObserver { _, _ -> sync() }
        lifecycle.addObserver(observer); sync()
        onDispose { lifecycle.removeObserver(observer); if (registered) context.unregisterReceiver(receiver) }
    }
    return now
}

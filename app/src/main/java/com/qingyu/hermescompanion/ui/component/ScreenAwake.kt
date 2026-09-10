package com.qingyu.hermescompanion.ui.component

import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/** Scoped to the visible screen; never takes a background wake lock. */
internal class ScreenAwakeLease(private val view: View, private val lifecycle: Lifecycle) : AutoCloseable {
    private val previous = view.keepScreenOn
    private val observer = LifecycleEventObserver { _, _ -> update() }
    init { lifecycle.addObserver(observer); update() }
    private fun update() {
        view.keepScreenOn = previous || lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
    }
    override fun close() { lifecycle.removeObserver(observer); view.keepScreenOn = previous }
}

@Composable
fun KeepScreenAwake() {
    val view = LocalView.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(view, lifecycle) {
        val lease = ScreenAwakeLease(view, lifecycle)
        onDispose { lease.close() }
    }
}

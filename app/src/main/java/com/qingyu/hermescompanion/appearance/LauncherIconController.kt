package com.qingyu.hermescompanion.appearance

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build

enum class LauncherIcon(val alias: String) {
    PARTNER("com.qingyu.hermescompanion.MainActivity"),
    SPRITE("com.qingyu.hermescompanion.launcher.SpriteIcon");
}

/** PackageManager owns the persisted launcher choice; preferences repair interrupted OEM updates. */
class LauncherIconController(context: Context) {
    private val app = context.applicationContext ?: context
    private val pm = app.packageManager
    private val prefs = app.getSharedPreferences("launcher_appearance", Context.MODE_PRIVATE)
    private fun component(icon: LauncherIcon) = ComponentName(app.packageName, icon.alias)
    private fun enabled(icon: LauncherIcon): Boolean = when (pm.getComponentEnabledSetting(component(icon))) {
        PackageManager.COMPONENT_ENABLED_STATE_DEFAULT -> icon == LauncherIcon.PARTNER
        PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
        else -> false
    }
    fun current(): LauncherIcon = synchronized(lock) {
        val active = LauncherIcon.entries.filter(::enabled)
        active.singleOrNull() ?: saved().takeIf { it in active } ?: active.firstOrNull() ?: saved()
    }
    private fun saved() = LauncherIcon.entries.firstOrNull { it.name == prefs.getString("choice", null) } ?: LauncherIcon.PARTNER

    fun reconcile(): LauncherIcon = synchronized(lock) {
        val active = LauncherIcon.entries.filter(::enabled)
        val choice = active.singleOrNull() ?: saved()
        if (active.size != 1) apply(choice)
        prefs.edit().putString("choice", choice.name).apply()
        choice
    }

    fun select(icon: LauncherIcon): Result<LauncherIcon> = synchronized(lock) {
        val old = current()
        runCatching {
            try {
                apply(icon)
                check(LauncherIcon.entries.filter(::enabled) == listOf(icon))
            } catch (failure: Exception) {
                // The legacy path always enables the destination first. Restore
                // the previous alias before reporting an OEM/package-manager error.
                runCatching { apply(old) }
                throw failure
            }
            prefs.edit().putString("choice", icon.name).apply()
            icon
        }
    }

    private fun apply(icon: LauncherIcon) {
        val ordered = listOf(icon) + LauncherIcon.entries.filter { it != icon }
        if (Build.VERSION.SDK_INT >= 33) {
            pm.setComponentEnabledSettings(ordered.map {
                PackageManager.ComponentEnabledSetting(component(it), if (it == icon) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                    else PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
            })
        } else ordered.forEach {
            pm.setComponentEnabledSetting(component(it), if (it == icon) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                else PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
        }
    }
    private companion object { val lock = Any() }
}

class LauncherIconRestoreReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) runCatching { LauncherIconController(context).reconcile() }
    }
}

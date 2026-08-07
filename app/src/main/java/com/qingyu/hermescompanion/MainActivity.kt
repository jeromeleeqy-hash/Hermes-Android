package com.qingyu.hermescompanion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.qingyu.hermescompanion.ui.AppRoute
import com.qingyu.hermescompanion.ui.HermesApp
import com.qingyu.hermescompanion.ui.HermesViewModel
import com.qingyu.hermescompanion.ui.ThemeMode
import com.qingyu.hermescompanion.ui.theme.HermesCompanionTheme

private val SettingsDetailRoutes = setOf(
    AppRoute.NOTIFICATIONS,
    AppRoute.VOICE_SETTINGS,
    AppRoute.PROFILE_SETTINGS,
    AppRoute.ABOUT,
    AppRoute.SKILLS_TOOLS,
    AppRoute.MODEL_SETTINGS,
    AppRoute.CONVERSATION_STYLE,
    AppRoute.APPROVAL_SETTINGS,
    AppRoute.MEMORY_CONTEXT,
    AppRoute.ARCHIVED_SESSIONS,
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: HermesViewModel = viewModel()
            val state = viewModel.uiState
            val systemDark = isSystemInDarkTheme()
            val darkSystemBars = when (state.themeMode) {
                ThemeMode.SYSTEM -> systemDark
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            SideEffect {
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !darkSystemBars
                    isAppearanceLightNavigationBars = !darkSystemBars
                }
            }

            HermesCompanionTheme(themeMode = state.themeMode, skinMode = state.skinMode) {
                BackHandler(
                    enabled = state.route == AppRoute.CHAT ||
                        state.route == AppRoute.CRON_DETAIL ||
                        state.route in SettingsDetailRoutes ||
                        (state.route == AppRoute.SETUP && state.hasSavedConnection),
                ) {
                    when {
                        state.route == AppRoute.CHAT -> viewModel.backToSessions()
                        state.route == AppRoute.CRON_DETAIL -> viewModel.closeCronJob()
                        state.route in SettingsDetailRoutes -> viewModel.closeSettingsPage()
                        else -> viewModel.closeConnectionSettings()
                    }
                }
                HermesApp(viewModel = viewModel, state = state)
            }
        }
    }
}

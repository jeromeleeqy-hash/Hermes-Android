package com.qingyu.hermescompanion

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.core.view.WindowCompat
import com.qingyu.hermescompanion.ui.AppRoute
import com.qingyu.hermescompanion.ui.HermesApp
import com.qingyu.hermescompanion.ui.HermesViewModel
import com.qingyu.hermescompanion.ui.ThemeMode
import com.qingyu.hermescompanion.ui.theme.HermesCompanionTheme

class MainActivity : ComponentActivity() {
    private val hermesViewModel: HermesViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        hermesViewModel.handleDeepLink(intent)
        hermesViewModel.handleShareIntent(intent)
        intent.action = null
        setContent {
            val viewModel = hermesViewModel
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
                        state.route == AppRoute.SEARCH ||
                        state.route == AppRoute.VOICE_CHAT ||
                        state.route == AppRoute.SETTINGS ||
                        state.route == AppRoute.CRON_DETAIL ||
                        state.route in setOf(
                            AppRoute.NOTIFICATIONS,
                            AppRoute.VOICE_SETTINGS,
                            AppRoute.PROFILE_SETTINGS,
                            AppRoute.ABOUT,
                            AppRoute.CHANGELOG,
                            AppRoute.SKILLS_TOOLS,
                            AppRoute.MODEL_SETTINGS,
                            AppRoute.CONVERSATION_STYLE,
                            AppRoute.APPROVAL_SETTINGS,
                            AppRoute.MEMORY_CONTEXT,
                            AppRoute.ARCHIVED_SESSIONS,
                        ) ||
                        (state.route == AppRoute.SETUP && state.hasSavedConnection),
                ) {
                    when {
                        state.route == AppRoute.SEARCH -> viewModel.closeSessionSearch()
                        state.route == AppRoute.VOICE_CHAT -> viewModel.closeVoiceConversation()
                        state.route == AppRoute.SETTINGS -> viewModel.showProfile()
                        state.route == AppRoute.CHAT -> viewModel.backToSessions()
                        state.route == AppRoute.CRON_DETAIL -> viewModel.closeCronJob()
                        state.route in setOf(
                            AppRoute.NOTIFICATIONS,
                            AppRoute.VOICE_SETTINGS,
                            AppRoute.PROFILE_SETTINGS,
                            AppRoute.ABOUT,
                            AppRoute.CHANGELOG,
                            AppRoute.SKILLS_TOOLS,
                            AppRoute.MODEL_SETTINGS,
                            AppRoute.CONVERSATION_STYLE,
                            AppRoute.APPROVAL_SETTINGS,
                            AppRoute.MEMORY_CONTEXT,
                            AppRoute.ARCHIVED_SESSIONS,
                        ) -> viewModel.closeSettingsPage()
                        else -> viewModel.closeConnectionSettings()
                    }
                }
                HermesApp(viewModel = viewModel, state = state)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        hermesViewModel.handleDeepLink(intent)
        hermesViewModel.handleShareIntent(intent)
        intent.action = null
    }
}

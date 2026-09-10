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

open class MainActivity : ComponentActivity() {
    private var createdLanguage = com.qingyu.hermescompanion.i18n.AppLanguageMode.SYSTEM

    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(com.qingyu.hermescompanion.i18n.AppLanguage.localizedContext(newBase))
    }

    private val hermesViewModel: HermesViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.qingyu.hermescompanion.i18n.AppLanguage.refresh(this)
        createdLanguage = com.qingyu.hermescompanion.i18n.AppLanguage.mode
        hermesViewModel.refreshUiLanguage()
        enableEdgeToEdge()
        // Keep the app's own continuous background visible in three-button mode too.
        if (android.os.Build.VERSION.SDK_INT >= 29) window.isNavigationBarContrastEnforced = false
        @Suppress("DEPRECATION")
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        hermesViewModel.handleInitialIntent(intent)
        intent.action = null
        setContent {
            val viewModel = hermesViewModel
            val state = viewModel.uiState
            androidx.compose.runtime.LaunchedEffect(state.languageMode) {
                if (android.os.Build.VERSION.SDK_INT < 33 && state.languageMode != createdLanguage) recreate()
            }
            val systemDark = isSystemInDarkTheme()
            val darkSystemBars = if (state.showLaunchIntro) true else when (state.themeMode) {
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
                    enabled = state.route in setOf(AppRoute.SESSIONS, AppRoute.WORKSPACE, AppRoute.TASKS, AppRoute.PROFILE) || state.route == AppRoute.CHAT ||
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
                        state.route == AppRoute.WORKSPACE && state.workspaceAttachmentTarget != null -> viewModel.cancelWorkspaceAttachmentPicker()
                        state.route in setOf(AppRoute.SESSIONS, AppRoute.WORKSPACE, AppRoute.TASKS, AppRoute.PROFILE) -> viewModel.showHome()
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
                androidx.compose.runtime.CompositionLocalProvider(com.qingyu.hermescompanion.ui.component.LocalLauncherIcon provides state.launcherIcon) {
                    HermesApp(viewModel = viewModel, state = state)
                }
            }
        }
    }

    override fun onStop() {
        if (!isChangingConfigurations) hermesViewModel.onAppBackgrounded()
        super.onStop()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        hermesViewModel.handleDeepLink(intent)
        hermesViewModel.handleShareIntent(intent)
        intent.action = null
    }
}

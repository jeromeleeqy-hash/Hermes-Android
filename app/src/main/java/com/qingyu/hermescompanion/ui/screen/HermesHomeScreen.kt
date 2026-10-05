package com.qingyu.hermescompanion.ui.screen

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import com.qingyu.hermescompanion.model.AgentRequest
import com.qingyu.hermescompanion.model.HermesSession
import com.qingyu.hermescompanion.ui.AppUiState
import com.qingyu.hermescompanion.ui.HomeMode

/** Both Home layouts share one settings entry; changing content never changes the skin. */
@Composable
fun HermesHomeScreen(
    state: AppUiState, contentPadding: PaddingValues,
    onHomeMode: (HomeMode) -> Unit,
    onOpen: (HermesSession) -> Unit, onHistory: () -> Unit, onTasks: () -> Unit, onFiles: () -> Unit,
    onRefresh: () -> Unit, onGenerate: () -> Unit,
    onPath: (String, Boolean) -> Unit, onCardAction: (String, String) -> Unit,
    onDaily: () -> Unit, onStart: (String) -> Unit, onSearch: () -> Unit,
    onRespond: (AgentRequest, String) -> Unit, onWelcomed: () -> Unit,
    onCompact: () -> Unit, onInteraction: (String, String) -> Unit,
    onSchedule: (String, String, String) -> Unit, onCheckSchedule: () -> Unit,
    onUpdate: () -> Unit, onMigrate: () -> Unit, onOpenRefresh: () -> Unit,
) {
    var settings by rememberSaveable { mutableStateOf(false) }
    if (state.homeMode == HomeMode.SIMPLE) {
        AssistantHomeScreen(state, contentPadding, onStart, onOpen, onHistory, onTasks, onFiles,
            onRespond, onSearch, onDaily, onWelcomed, onSettings = { settings = true })
    } else {
        TodayOverviewScreen(state, contentPadding, onRefresh, onGenerate, onPath, onCardAction,
            onDaily, onStart, onSearch, onRespond, onWelcomed, onCompact, onInteraction,
            onSchedule, onCheckSchedule, onUpdate, onMigrate, onOpenRefresh, onSettings = { settings = true })
    }
    if (settings) HomeSettingsWindow(state, { settings = false }, onHomeMode,
        onGenerate, onRefresh, onMigrate, onCompact, onSchedule, onCheckSchedule)
}

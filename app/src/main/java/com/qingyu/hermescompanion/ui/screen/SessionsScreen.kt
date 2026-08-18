package com.qingyu.hermescompanion.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import com.qingyu.hermescompanion.model.HermesProject
import com.qingyu.hermescompanion.model.HermesProfile
import com.qingyu.hermescompanion.model.HermesSession
import com.qingyu.hermescompanion.model.scopedId
import com.qingyu.hermescompanion.model.WorkspaceListing
import com.qingyu.hermescompanion.ui.AppUiState
import com.qingyu.hermescompanion.ui.component.HermesIconKind
import com.qingyu.hermescompanion.ui.component.HermesMark
import com.qingyu.hermescompanion.ui.component.HermesMulticolorIcon
import com.qingyu.hermescompanion.ui.component.HermesPinnedMarker
import com.qingyu.hermescompanion.ui.component.HermesStatusIcon
import com.qingyu.hermescompanion.ui.component.HermesStatusKind
import com.qingyu.hermescompanion.ui.component.HermesWelcomeAnimation
import com.qingyu.hermescompanion.ui.component.UserAvatar
import com.qingyu.hermescompanion.ui.format.ellipsizeSessionTitle
import com.qingyu.hermescompanion.ui.format.sessionTimeLabel
import com.qingyu.hermescompanion.ui.format.SessionTimeFilter
import com.qingyu.hermescompanion.ui.format.sessionMatchesTime
import com.qingyu.hermescompanion.ui.theme.HermesSkin
import com.qingyu.hermescompanion.ui.theme.HermesColors
import com.qingyu.hermescompanion.ui.theme.HermesSpacing
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlin.math.roundToInt

private enum class SessionListMode { RECENT, PROJECTS }

private data class AvatarPalette(val tone: Int)

private val AvatarPalettes = List(6, ::AvatarPalette)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionsScreen(
    state: AppUiState,
    contentPadding: PaddingValues,
    onRefresh: () -> Unit,
    onNewSession: () -> Unit,
    onSearch: () -> Unit,
    onOpenSession: (HermesSession) -> Unit,
    onDeleteSession: (HermesSession) -> Unit,
    onAiRenameSession: (HermesSession) -> Unit,
    onTogglePinned: (HermesSession) -> Unit,
    onArchiveSession: (HermesSession) -> Unit,
    onMoveToProject: (HermesSession, HermesProject) -> Unit,
    onLoadProjects: () -> Unit,
    onCreateProject: (String, String) -> Unit,
    onLoadProjectDirectories: (String?) -> Unit,
    onCloseProjectDirectoryPicker: () -> Unit,
    onRefreshProfiles: () -> Unit,
    onSelectProfile: (HermesProfile) -> Unit,
) {
    var listMode by remember { mutableStateOf(SessionListMode.RECENT) }
    var actionTarget by remember { mutableStateOf<HermesSession?>(null) }
    var showProjectPicker by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<HermesSession?>(null) }
    var archiveTarget by remember { mutableStateOf<HermesSession?>(null) }
    var showCreateProject by remember { mutableStateOf(false) }
    var selectedProjectId by remember(state.activeProfile) { mutableStateOf<String?>(null) }
    var timeFilter by remember(state.activeProfile) { mutableStateOf(SessionTimeFilter.ALL) }

    LaunchedEffect(state.projects, selectedProjectId) {
        if (selectedProjectId != null && state.projects.none { it.id == selectedProjectId }) {
            selectedProjectId = null
        }
    }

    val regularSessions = remember(state.sessions) {
        val regular = state.sessions.filterNot(HermesSession::isCron)
        regular.filter(HermesSession::isPinned) + regular.filterNot(HermesSession::isPinned)
    }
    val filteredRegularSessions = remember(regularSessions, state.projects, selectedProjectId, timeFilter) {
        regularSessions.filter { session ->
            val projectMatches = selectedProjectId == null ||
                state.projects.firstOrNull { it.owns(session.workspacePath) }?.id == selectedProjectId
            projectMatches && sessionMatchesTime(session.updatedAt, timeFilter)
        }
    }
    val projectGroups = remember(filteredRegularSessions, state.projects, selectedProjectId) {
        state.projects.map { project ->
            project to filteredRegularSessions.filter { session ->
                state.projects.firstOrNull { it.owns(session.workspacePath) }?.id == project.id
            }
        }.filter { (project, _) -> selectedProjectId == null || project.id == selectedProjectId }
    }
    val assignedIds = remember(projectGroups) { projectGroups.flatMap { it.second }.mapTo(mutableSetOf(), HermesSession::id) }
    val ungroupedSessions = remember(filteredRegularSessions, assignedIds, selectedProjectId) {
        if (selectedProjectId == null) filteredRegularSessions.filterNot { it.id in assignedIds } else emptyList()
    }

    val skin = HermesSkin.current
    Box(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(0.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 0.dp,
                tonalElevation = 0.dp,
            ) {
                Column(modifier = Modifier.padding(horizontal = HermesSpacing.page)) {
                    Header(
                        assistantName = state.userProfile.hermesDisplayName.ifBlank { "Hermes" },
                        assistantAvatarUri = state.userProfile.hermesAvatarUri,
                        profiles = state.profiles,
                        activeProfile = state.activeProfile,
                        isProfilesLoading = state.isProfilesLoading || state.isProfileSwitching,
                        isProfileSwitching = state.isProfileSwitching,
                        onSearch = onSearch,
                        onRefreshProfiles = onRefreshProfiles,
                        onSelectProfile = onSelectProfile,
                    )
                    SessionFilterBar(
                        mode = listMode,
                        projects = state.projects,
                        selectedProjectId = selectedProjectId,
                        timeFilter = timeFilter,
                        onLoadProjects = onLoadProjects,
                        onModeChange = {
                            listMode = it
                            if (it == SessionListMode.PROJECTS) onLoadProjects()
                        },
                        onProjectChange = { selectedProjectId = it },
                        onTimeChange = { timeFilter = it },
                    )
                }
            }
            PullToRefreshBox(
                isRefreshing = state.isBusy,
                onRefresh = onRefresh,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = HermesSpacing.page, top = 4.dp, end = HermesSpacing.page, bottom = 86.dp),
                ) {
                when {
                    state.isBusy && regularSessions.isEmpty() -> item("loading") {
                        Box(Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(Modifier.size(25.dp), strokeWidth = 2.2.dp)
                        }
                    }

                    regularSessions.isEmpty() && listMode == SessionListMode.RECENT -> item("empty") { EmptySessions(onNewSession) }

                    filteredRegularSessions.isEmpty() && listMode == SessionListMode.RECENT -> item("filtered-empty") {
                        FilteredSessionsEmpty()
                    }

                    listMode == SessionListMode.RECENT -> {
                        items(filteredRegularSessions, key = { it.id }) { session ->
                            SessionRow(
                                session = session,
                                unread = session.scopedId in state.unreadSessionIds,
                                onClick = { onOpenSession(session) },
                                onLongClick = { actionTarget = session },
                                onArchive = { archiveTarget = session },
                                onDelete = { deleteTarget = session },
                            )
                        }
                    }

                    else -> {
                        item("project-create") {
                            ProjectCreateBar(onClick = {
                                showCreateProject = true
                                onLoadProjectDirectories(null)
                            })
                        }
                        if (state.isProjectsLoading && state.projects.isEmpty()) {
                            item("projects-loading") {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    CircularProgressIndicator(Modifier.size(19.dp), strokeWidth = 2.dp)
                                    Text("正在读取 Hermes 项目", modifier = Modifier.padding(start = 9.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        } else {
                            projectGroups.forEach { (project, sessions) ->
                                item("project-${project.id}") { ProjectSectionHeader(project.name, sessions.size) }
                                items(sessions, key = { "${project.id}-${it.id}" }) { session ->
                                    SessionRow(
                                        session = session,
                                        unread = session.scopedId in state.unreadSessionIds,
                                        onClick = { onOpenSession(session) },
                                        onLongClick = { actionTarget = session },
                                        onArchive = { archiveTarget = session },
                                        onDelete = { deleteTarget = session },
                                    )
                                }
                            }
                            if (ungroupedSessions.isNotEmpty()) {
                                item("project-ungrouped") { ProjectSectionHeader("未分项目", ungroupedSessions.size, muted = true) }
                                items(ungroupedSessions, key = { "ungrouped-${it.id}" }) { session ->
                                    SessionRow(
                                        session = session,
                                        unread = session.scopedId in state.unreadSessionIds,
                                        onClick = { onOpenSession(session) },
                                        onLongClick = { actionTarget = session },
                                        onArchive = { archiveTarget = session },
                                        onDelete = { deleteTarget = session },
                                    )
                                }
                            }
                            if (projectGroups.isEmpty() && ungroupedSessions.isEmpty()) {
                                item("projects-empty") { ProjectModeEmpty() }
                            }
                        }
                    }
                }
                }
            }
        }
        FloatingActionButton(
            onClick = onNewSession,
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 18.dp, bottom = 16.dp).size(48.dp),
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ) {
            HermesMulticolorIcon(
                HermesIconKind.NEW_CHAT,
                contentDescription = "新建对话",
                iconSize = 20.dp,
                tint = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }

    actionTarget?.let { session ->
        ModalBottomSheet(
            onDismissRequest = {
                actionTarget = null
                showProjectPicker = false
            },
            shape = RoundedCornerShape(
                topStart = if (skin.glass) 20.dp else 14.dp,
                topEnd = if (skin.glass) 20.dp else 14.dp,
            ),
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = skin.chromeAlpha),
            tonalElevation = 0.dp,
        ) {
            if (showProjectPicker) {
                ProjectPicker(
                    projects = state.projects,
                    loading = state.isProjectsLoading,
                    onBack = { showProjectPicker = false },
                    onSelect = { project ->
                        actionTarget = null
                        showProjectPicker = false
                        onMoveToProject(session, project)
                    },
                )
            } else {
                SessionActions(
                    session = session,
                    busy = state.sessionActionId == session.id,
                    onAiRename = { actionTarget = null; onAiRenameSession(session) },
                    onTogglePinned = { actionTarget = null; onTogglePinned(session) },
                    onArchive = { actionTarget = null; archiveTarget = session },
                    onMove = {
                        showProjectPicker = true
                        onLoadProjects()
                    },
                    onDelete = { actionTarget = null; deleteTarget = session },
                )
            }
        }
    }

    deleteTarget?.let { session ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            shape = MaterialTheme.shapes.extraLarge,
            title = { Text("删除这段会话？") },
            text = { Text("“${ellipsizeSessionTitle(session.title)}”将从 Hermes 会话记录中永久删除。") },
            confirmButton = {
                TextButton(onClick = { onDeleteSession(session); deleteTarget = null }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("取消") } },
        )
    }

    archiveTarget?.let { session ->
        AlertDialog(
            onDismissRequest = { archiveTarget = null },
            shape = MaterialTheme.shapes.extraLarge,
            title = { Text("归档这段会话？") },
            text = { Text("归档后会从手机首页移除，仍可在 Hermes 电脑端恢复。") },
            confirmButton = {
                TextButton(onClick = { onArchiveSession(session); archiveTarget = null }) { Text("归档") }
            },
            dismissButton = { TextButton(onClick = { archiveTarget = null }) { Text("取消") } },
        )
    }

    if (showCreateProject) {
        CreateProjectDialog(
            listing = state.projectPickerListing,
            busy = state.isProjectsLoading || state.isProjectPickerLoading,
            onBrowse = onLoadProjectDirectories,
            onDismiss = {
                if (!state.isProjectsLoading) {
                    showCreateProject = false
                    onCloseProjectDirectoryPicker()
                }
            },
            onCreate = { name, path ->
                onCreateProject(name, path)
                showCreateProject = false
                onCloseProjectDirectoryPicker()
            },
        )
    }
}

@Composable
private fun Header(
    assistantName: String,
    assistantAvatarUri: String,
    profiles: List<HermesProfile>,
    activeProfile: String,
    isProfilesLoading: Boolean,
    isProfileSwitching: Boolean,
    onSearch: () -> Unit,
    onRefreshProfiles: () -> Unit,
    onSelectProfile: (HermesProfile) -> Unit,
) {
    val skin = HermesSkin.current
    var profileMenuExpanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(top = 6.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (assistantAvatarUri.isNotBlank()) {
            UserAvatar(
                uri = assistantAvatarUri,
                displayName = assistantName,
                size = 38.dp,
                hermesFallback = true,
            )
        } else {
            HermesMark(compact = true, requestedSize = 38.dp)
        }
        Column(
            modifier = Modifier.padding(start = 9.dp).height(38.dp).weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                assistantName,
                style = MaterialTheme.typography.titleLarge.copy(lineHeight = 22.sp),
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Box {
                Row(
                    modifier = Modifier.offset(y = (-1).dp).clip(RoundedCornerShape(6.dp)).clickable {
                        profileMenuExpanded = true
                    }.padding(end = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    HermesStatusIcon(if (isProfileSwitching) HermesStatusKind.BUSY else HermesStatusKind.CONNECTED)
                    Text(
                        if (isProfileSwitching) "正在切换 · $activeProfile" else "Profile · $activeProfile  ▾",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 5.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                DropdownMenu(
                    expanded = profileMenuExpanded,
                    onDismissRequest = { profileMenuExpanded = false },
                    offset = DpOffset(0.dp, 4.dp),
                    shape = RoundedCornerShape(skin.menuRadius.dp),
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = skin.chromeAlpha),
                    tonalElevation = 0.dp,
                    shadowElevation = if (skin.glass) 6.dp else 2.dp,
                ) {
                    if (isProfilesLoading && profiles.isEmpty()) {
                        CompactMenuItem("正在读取 Profile…", enabled = false) {}
                    }
                    profiles.forEach { profile ->
                        CompactMenuItem(
                            label = (if (profile.name == activeProfile) "✓  " else "") + profile.name,
                            detail = profileDetailText(profile),
                            selected = profile.name == activeProfile,
                            enabled = !isProfilesLoading,
                            onClick = {
                                profileMenuExpanded = false
                                onSelectProfile(profile)
                            },
                        )
                    }
                    if (profiles.isNotEmpty()) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                        CompactMenuItem(if (isProfilesLoading) "正在刷新…" else "刷新 Profile 列表", enabled = !isProfilesLoading, onClick = onRefreshProfiles)
                    }
                }
            }
        }
        IconButton(onClick = onSearch, modifier = Modifier.size(40.dp)) {
            HermesMulticolorIcon(
                HermesIconKind.SEARCH,
                contentDescription = "搜索对话",
                iconSize = 22.dp,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

internal fun profileDetailText(profile: HermesProfile): String {
    val description = profile.description.trim().takeUnless {
        it.equals("false", ignoreCase = true) ||
            it.equals("true", ignoreCase = true) ||
            it.equals("null", ignoreCase = true)
    }.orEmpty()
    return description.ifBlank {
        listOf(profile.provider, profile.model).map(String::trim).filter(String::isNotBlank).joinToString(" · ")
    }
}

@Composable
private fun SessionFilterBar(
    mode: SessionListMode,
    projects: List<HermesProject>,
    selectedProjectId: String?,
    timeFilter: SessionTimeFilter,
    onLoadProjects: () -> Unit,
    onModeChange: (SessionListMode) -> Unit,
    onProjectChange: (String?) -> Unit,
    onTimeChange: (SessionTimeFilter) -> Unit,
) {
    val skin = HermesSkin.current
    var modeMenu by remember { mutableStateOf(false) }
    var projectMenu by remember { mutableStateOf(false) }
    var timeMenu by remember { mutableStateOf(false) }
    val projectLabel = projects.firstOrNull { it.id == selectedProjectId }?.name ?: "全部项目"
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.weight(0.72f)) {
            FilterChipLabel(if (mode == SessionListMode.RECENT) "最近" else "项目", Modifier.fillMaxWidth(), active = mode == SessionListMode.PROJECTS, onClick = { modeMenu = true })
            DropdownMenu(
                expanded = modeMenu,
                onDismissRequest = { modeMenu = false },
                offset = DpOffset(0.dp, 4.dp),
                shape = RoundedCornerShape(skin.menuRadius.dp),
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = skin.chromeAlpha),
            ) {
                SessionListMode.entries.forEach { option ->
                    val label = if (option == SessionListMode.RECENT) "最近" else "项目"
                    CompactMenuItem(label, selected = option == mode) { modeMenu = false; onModeChange(option) }
                }
            }
        }
        Box(Modifier.weight(1.28f)) {
            FilterChipLabel(projectLabel, Modifier.fillMaxWidth(), active = selectedProjectId != null, onClick = {
                onLoadProjects()
                projectMenu = true
            })
            DropdownMenu(
                expanded = projectMenu,
                onDismissRequest = { projectMenu = false },
                offset = DpOffset(0.dp, 4.dp),
                shape = RoundedCornerShape(skin.menuRadius.dp),
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = skin.chromeAlpha),
            ) {
                CompactMenuItem("全部项目", selected = selectedProjectId == null) { projectMenu = false; onProjectChange(null) }
                projects.forEach { project ->
                    CompactMenuItem(project.name, selected = project.id == selectedProjectId) { projectMenu = false; onProjectChange(project.id) }
                }
            }
        }
        Box(Modifier.weight(0.9f)) {
            FilterChipLabel(timeFilter.label, Modifier.fillMaxWidth(), active = timeFilter != SessionTimeFilter.ALL, onClick = { timeMenu = true })
            DropdownMenu(
                expanded = timeMenu,
                onDismissRequest = { timeMenu = false },
                offset = DpOffset(0.dp, 4.dp),
                shape = RoundedCornerShape(skin.menuRadius.dp),
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = skin.chromeAlpha),
            ) {
                SessionTimeFilter.entries.forEach { filter ->
                    CompactMenuItem(filter.label, selected = filter == timeFilter) { timeMenu = false; onTimeChange(filter) }
                }
            }
        }
    }
}

@Composable
private fun FilterChipLabel(
    text: String,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val skin = HermesSkin.current
    val shape = RoundedCornerShape(skin.controlRadius.dp)
    Surface(
        modifier = modifier.clip(shape).then(if (onClick == null) Modifier else Modifier.clickable(onClick = onClick)),
        shape = shape,
        color = when {
            active -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = skin.selectedFillAlpha)
            skin.glass -> MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.55f)
            else -> MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.92f)
        },
        tonalElevation = 0.dp,
    ) {
        Text(
            text = text + if (onClick == null) "" else "  ▾",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
        )
    }
}

@Composable
private fun CompactMenuItem(
    label: String,
    detail: String = "",
    selected: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = if (detail.isBlank()) 9.dp else 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.45f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (detail.isNotBlank()) Text(
                detail,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (enabled) 1f else 0.45f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            if (selected) "✓" else "",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.End,
            modifier = Modifier.size(width = 20.dp, height = 18.dp),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SessionRow(
    session: HermesSession,
    isCron: Boolean = false,
    unread: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onArchive: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
) {
    val haptics = LocalHapticFeedback.current
    val skin = HermesSkin.current
    val palette = paletteFor(session)
    val rowContent: @Composable () -> Unit = {
        Column(
            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                },
            ),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 3.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SessionAvatar(palette, isCron, session.title)
                Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (session.isPinned) {
                            HermesPinnedMarker(
                                modifier = Modifier.padding(end = 5.dp),
                                iconSize = 16.dp,
                            )
                        }
                        Text(
                            ellipsizeSessionTitle(session.title),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Clip,
                            modifier = Modifier.weight(1f),
                        )
                        if (unread) {
                            Box(
                                Modifier.padding(start = 7.dp).size(7.dp).clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.error),
                            )
                        }
                        sessionTimeLabel(session.updatedAt).takeIf(String::isNotBlank)?.let { time ->
                            Text(time, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(start = 9.dp))
                        }
                    }
                    Text(
                        session.preview.ifBlank { if (isCron) "定时任务运行记录" else "暂无内容摘要" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
            HorizontalDivider(
                modifier = Modifier.padding(start = if (skin.glass) 60.dp else 48.dp),
                thickness = 0.6.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.52f),
            )
        }
    }
    if (isCron || onArchive == null || onDelete == null) {
        rowContent()
    } else {
        val revealWidthPx = with(LocalDensity.current) { 132.dp.toPx() }
        var dragOffsetPx by remember(session.id) { mutableFloatStateOf(0f) }
        var settleJob by remember(session.id) { mutableStateOf<Job?>(null) }
        val scope = rememberCoroutineScope()
        val dragState = rememberDraggableState { delta ->
            dragOffsetPx = (dragOffsetPx + delta).coerceIn(-revealWidthPx, 0f)
        }
        Box(Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.matchParentSize(),
                horizontalArrangement = Arrangement.End,
            ) {
                SessionSwipeAction(
                    label = "归档",
                    icon = HermesIconKind.ARCHIVE,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.primary,
                    onClick = {
                        settleJob?.cancel()
                        dragOffsetPx = 0f
                        onArchive()
                    },
                )
                SessionSwipeAction(
                    label = "删除",
                    icon = HermesIconKind.DELETE,
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.error,
                    onClick = {
                        settleJob?.cancel()
                        dragOffsetPx = 0f
                        onDelete()
                    },
                )
            }
            Box(
                modifier = Modifier.offset { IntOffset(dragOffsetPx.roundToInt(), 0) }.draggable(
                    state = dragState,
                    orientation = Orientation.Horizontal,
                    onDragStarted = { settleJob?.cancel() },
                    onDragStopped = { velocity ->
                        val target = if (dragOffsetPx <= -revealWidthPx * 0.28f || velocity < -520f) {
                            -revealWidthPx
                        } else {
                            0f
                        }
                        settleJob = scope.launch {
                            Animatable(dragOffsetPx).animateTo(
                                targetValue = target,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessMedium,
                                ),
                            ) {
                                dragOffsetPx = value
                            }
                        }
                    },
                ),
            ) {
                rowContent()
            }
        }
    }
}

@Composable
private fun SessionSwipeAction(
    label: String,
    icon: HermesIconKind,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier.width(66.dp).fillMaxHeight().background(containerColor).clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        HermesMulticolorIcon(icon, contentDescription = label, iconSize = 18.dp, tint = contentColor)
        Text(label, style = MaterialTheme.typography.labelSmall, color = contentColor, modifier = Modifier.padding(top = 3.dp))
    }
}

@Composable
private fun SessionAvatar(palette: AvatarPalette, isCron: Boolean, title: String) {
    val skin = HermesSkin.current
    val background = if (isCron) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        when (palette.tone) {
            0 -> MaterialTheme.colorScheme.primaryContainer
            1 -> HermesColors.extended.successContainer
            2 -> MaterialTheme.colorScheme.secondaryContainer
            3 -> MaterialTheme.colorScheme.tertiaryContainer
            4 -> MaterialTheme.colorScheme.errorContainer
            else -> HermesColors.extended.warningContainer
        }
    }
    Box(
        modifier = Modifier
            .size(if (skin.glass) 40.dp else 36.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(background.copy(alpha = skin.iconWellAlpha)),
        contentAlignment = Alignment.Center,
    ) {
        if (isCron) {
            HermesMulticolorIcon(HermesIconKind.RECENT, contentDescription = null, iconSize = 19.dp)
        } else {
            val foreground = when (palette.tone) {
                0 -> MaterialTheme.colorScheme.primary
                1 -> HermesColors.extended.success
                2 -> MaterialTheme.colorScheme.secondary
                3 -> MaterialTheme.colorScheme.tertiary
                4 -> MaterialTheme.colorScheme.error
                else -> HermesColors.extended.warning
            }
            Text(
                title.trim().firstOrNull()?.uppercase() ?: "H",
                style = MaterialTheme.typography.titleSmall,
                color = foreground,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun ProjectSectionHeader(name: String, count: Int, muted: Boolean = false) {
    val skin = HermesSkin.current
    Row(modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 10.dp, bottom = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(if (skin.glass) 28.dp else 24.dp).clip(RoundedCornerShape(if (skin.glass) 9.dp else 6.dp))
                .background(
                    if (!skin.glass) Color.Transparent
                    else if (muted) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = skin.iconWellAlpha)
                    else HermesColors.extended.successContainer.copy(alpha = skin.iconWellAlpha),
                ),
            contentAlignment = Alignment.Center,
        ) {
            HermesMulticolorIcon(HermesIconKind.PROJECT, contentDescription = null, iconSize = 17.dp)
        }
        Text(name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 8.dp))
        Text("$count", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 6.dp))
    }
}

@Composable
private fun ProjectCreateBar(onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 7.dp, bottom = 3.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.48f))
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HermesMulticolorIcon(HermesIconKind.NEW_CHAT, contentDescription = null, iconSize = 20.dp)
        Text("新建项目", modifier = Modifier.padding(start = 8.dp), fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.weight(1f))
        Text("手机端创建", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun CreateProjectDialog(
    listing: WorkspaceListing?,
    busy: Boolean,
    onBrowse: (String?) -> Unit,
    onDismiss: () -> Unit,
    onCreate: (String, String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        title = { Text("新建项目") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("项目名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("选择服务器目录", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                ) {
                    Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                        HermesMulticolorIcon(HermesIconKind.FOLDER, contentDescription = null, iconSize = 21.dp)
                        Text(
                            listing?.path ?: "正在读取工作区…",
                            modifier = Modifier.weight(1f).padding(start = 8.dp),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                if (busy && listing == null) {
                    Box(Modifier.fillMaxWidth().height(96.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 250.dp)) {
                        listing?.parent?.let { parent ->
                            item("parent") {
                                DirectoryChoice("返回上级目录", parent, onClick = { onBrowse(parent) })
                            }
                        }
                        items(listing?.entries?.filter { it.isDirectory }.orEmpty(), key = { it.path }) { entry ->
                            DirectoryChoice(entry.name, entry.path, onClick = { onBrowse(entry.path) })
                        }
                        if (listing?.entries?.none { it.isDirectory } == true) {
                            item("empty") {
                                Text(
                                    "当前目录下没有子目录，可直接选择这里。",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 16.dp),
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !busy && name.isNotBlank() && !listing?.path.isNullOrBlank(),
                onClick = { onCreate(name, listing?.path.orEmpty()) },
            ) { Text("创建") }
        },
        dismissButton = { TextButton(enabled = !busy, onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun DirectoryChoice(name: String, path: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 4.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HermesMulticolorIcon(HermesIconKind.FOLDER, contentDescription = null, iconSize = 20.dp)
        Column(modifier = Modifier.weight(1f).padding(start = 9.dp)) {
            Text(name, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(path, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        HermesMulticolorIcon(HermesIconKind.CHEVRON_RIGHT, contentDescription = null, iconSize = 16.dp)
    }
}

@Composable
private fun SessionActions(
    session: HermesSession,
    busy: Boolean,
    onAiRename: () -> Unit,
    onTogglePinned: () -> Unit,
    onArchive: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 24.dp)) {
        Text(ellipsizeSessionTitle(session.title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp))
        if (busy) {
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 26.dp), horizontalArrangement = Arrangement.Center) {
                CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
            }
        } else {
            SessionActionRow(HermesIconKind.RENAME, "AI 重命名", onAiRename)
            SessionPinActionRow(if (session.isPinned) "取消置顶" else "置顶", onTogglePinned)
            SessionActionRow(HermesIconKind.ARCHIVE, "归档", onArchive)
            SessionActionRow(HermesIconKind.MOVE, "移至项目", onMove)
            SessionActionRow(HermesIconKind.DELETE, "删除", onDelete)
        }
    }
}

@Composable
private fun SessionPinActionRow(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable(onClick = onClick).padding(horizontal = 9.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HermesPinnedMarker(contentDescription = null, iconSize = 20.dp)
        Text(label, modifier = Modifier.padding(start = 12.dp), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun SessionActionRow(icon: HermesIconKind, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable(onClick = onClick).padding(horizontal = 9.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HermesMulticolorIcon(icon, contentDescription = null, iconSize = 21.dp)
        Text(label, modifier = Modifier.padding(start = 12.dp), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ProjectPicker(projects: List<HermesProject>, loading: Boolean, onBack: () -> Unit, onSelect: (HermesProject) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { HermesMulticolorIcon(HermesIconKind.BACK, contentDescription = "返回") }
            Text("移至项目", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        }
        when {
            loading && projects.isEmpty() -> Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
            }
            projects.isEmpty() -> Text(
                "暂无可用项目，可在首页切换到“项目”后新建。",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 28.dp),
            )
            else -> LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 460.dp)) {
                items(projects, key = HermesProject::id) { project ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onSelect(project) }.padding(horizontal = 18.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(36.dp).clip(RoundedCornerShape(11.dp)).background(HermesColors.extended.successContainer), contentAlignment = Alignment.Center) {
                            HermesMulticolorIcon(HermesIconKind.PROJECT, contentDescription = null, iconSize = 20.dp)
                        }
                        Text(project.name, modifier = Modifier.weight(1f).padding(start = 11.dp), fontWeight = FontWeight.Medium)
                    }
                    HorizontalDivider(modifier = Modifier.padding(start = 65.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                }
            }
        }
    }
}

@Composable
private fun EmptySessions(onNewSession: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp, horizontal = 22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        HermesWelcomeAnimation(
            modifier = Modifier.size(176.dp),
            contentDescription = "Hermes 欢迎动画",
        )
        Text("还没有会话", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 2.dp))
        Text(
            "和 Hermes 开始一段新对话吧",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
        Button(onClick = onNewSession, modifier = Modifier.padding(top = 12.dp)) { Text("开始新对话") }
    }
}

@Composable
private fun FilteredSessionsEmpty() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 52.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("没有符合筛选条件的会话", fontWeight = FontWeight.SemiBold)
        Text("可以调整项目或时间范围", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 5.dp))
    }
}

@Composable
private fun ProjectModeEmpty() {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 34.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        HermesMulticolorIcon(HermesIconKind.PROJECT, contentDescription = null, iconSize = 32.dp)
        Text("暂无项目对话", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 9.dp))
        Text("可长按对话并选择“移至项目”", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 3.dp))
    }
}

private val HermesSession.isCron: Boolean
    get() = source.equals("cron", ignoreCase = true)

private fun paletteFor(session: HermesSession): AvatarPalette {
    val conversationLength = session.messageCount + session.preview.length / 48
    return AvatarPalettes[conversationLength.coerceAtLeast(0) % AvatarPalettes.size]
}

private fun HermesProject.owns(rawPath: String): Boolean {
    if (rawPath.isBlank()) return false
    val path = rawPath.replace('\\', '/').trimEnd('/')
    return (paths + primaryPath).asSequence().filter(String::isNotBlank).map { it.replace('\\', '/').trimEnd('/') }
        .any { root -> path == root || path.startsWith("$root/") }
}

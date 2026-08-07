package com.qingyu.hermescompanion.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.DpOffset
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
import com.qingyu.hermescompanion.ui.component.UserAvatar
import com.qingyu.hermescompanion.ui.format.ellipsizeSessionTitle
import com.qingyu.hermescompanion.ui.format.sessionTimeLabel
import com.qingyu.hermescompanion.ui.format.SessionTimeFilter
import com.qingyu.hermescompanion.ui.format.sessionMatchesTime
import com.qingyu.hermescompanion.ui.theme.HermesSkin
import com.qingyu.hermescompanion.ui.theme.HermesColors
import com.qingyu.hermescompanion.ui.theme.HermesSpacing

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
    onBatchAiRename: () -> Unit,
    onRefreshProfiles: () -> Unit,
    onSelectProfile: (HermesProfile) -> Unit,
) {
    var listMode by remember { mutableStateOf(SessionListMode.RECENT) }
    var actionTarget by remember { mutableStateOf<HermesSession?>(null) }
    var showProjectPicker by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<HermesSession?>(null) }
    var archiveTarget by remember { mutableStateOf<HermesSession?>(null) }
    var showBatchConfirm by remember { mutableStateOf(false) }
    var showCreateProject by remember { mutableStateOf(false) }
    var cronExpanded by remember { mutableStateOf(false) }
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
    val cronSessions = remember(state.sessions, timeFilter) {
        state.sessions.filter(HermesSession::isCron).filter { sessionMatchesTime(it.updatedAt, timeFilter) }
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
    Column(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(
                bottomStart = if (skin.glass) 16.dp else 0.dp,
                bottomEnd = if (skin.glass) 16.dp else 0.dp,
            ),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = skin.shadowElevation.dp,
            tonalElevation = 0.dp,
            border = if (skin.glass) BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant) else null,
        ) {
            Column(modifier = Modifier.padding(horizontal = HermesSpacing.page)) {
                Header(
                    assistantName = state.userProfile.hermesDisplayName.ifBlank { "Hermes" },
                    assistantAvatarUri = state.userProfile.hermesAvatarUri,
                    isBatchRenaming = state.isBatchRenaming,
                    profiles = state.profiles,
                    activeProfile = state.activeProfile,
                    isProfilesLoading = state.isProfilesLoading || state.isProfileSwitching,
                    isProfileSwitching = state.isProfileSwitching,
                    onNewSession = onNewSession,
                    onSearch = onSearch,
                    onBatchAiRename = { showBatchConfirm = true },
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
                contentPadding = PaddingValues(start = HermesSpacing.page, top = 4.dp, end = HermesSpacing.page, bottom = 16.dp),
            ) {
                when {
                    state.isBusy && state.sessions.isEmpty() -> item("loading") {
                        Box(Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(Modifier.size(25.dp), strokeWidth = 2.2.dp)
                        }
                    }

                    state.sessions.isEmpty() && listMode == SessionListMode.RECENT -> item("empty") { EmptySessions(onNewSession) }

                    filteredRegularSessions.isEmpty() && listMode == SessionListMode.RECENT -> item("filtered-empty") {
                        FilteredSessionsEmpty()
                    }

                    listMode == SessionListMode.RECENT -> {
                        items(filteredRegularSessions, key = { it.id }) { session ->
                            SessionRow(session, unread = session.scopedId in state.unreadSessionIds, onClick = { onOpenSession(session) }, onLongClick = { actionTarget = session })
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
                                    SessionRow(session, unread = session.scopedId in state.unreadSessionIds, onClick = { onOpenSession(session) }, onLongClick = { actionTarget = session })
                                }
                            }
                            if (ungroupedSessions.isNotEmpty()) {
                                item("project-ungrouped") { ProjectSectionHeader("未分项目", ungroupedSessions.size, muted = true) }
                                items(ungroupedSessions, key = { "ungrouped-${it.id}" }) { session ->
                                    SessionRow(session, unread = session.scopedId in state.unreadSessionIds, onClick = { onOpenSession(session) }, onLongClick = { actionTarget = session })
                                }
                            }
                            if (projectGroups.isEmpty() && ungroupedSessions.isEmpty()) {
                                item("projects-empty") { ProjectModeEmpty() }
                            }
                        }
                    }
                }

                if (cronSessions.isNotEmpty()) {
                    item("cron-header") {
                        CronGroupHeader(
                            count = cronSessions.size,
                            expanded = cronExpanded,
                            onClick = { cronExpanded = !cronExpanded },
                        )
                    }
                    if (cronExpanded) {
                        items(cronSessions, key = { "cron-${it.id}" }) { session ->
                            SessionRow(
                                session = session,
                                isCron = true,
                                unread = session.scopedId in state.unreadSessionIds,
                                onClick = { onOpenSession(session) },
                                onLongClick = { actionTarget = session },
                            )
                        }
                    }
                }
            }
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
            containerColor = MaterialTheme.colorScheme.surface,
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
            shape = RoundedCornerShape(18.dp),
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
            shape = RoundedCornerShape(18.dp),
            title = { Text("归档这段会话？") },
            text = { Text("归档后会从手机首页移除，仍可在 Hermes 电脑端恢复。") },
            confirmButton = {
                TextButton(onClick = { onArchiveSession(session); archiveTarget = null }) { Text("归档") }
            },
            dismissButton = { TextButton(onClick = { archiveTarget = null }) { Text("取消") } },
        )
    }

    if (showBatchConfirm) {
        val count = regularSessions.count { it.messageCount > 0 }
        AlertDialog(
            onDismissRequest = { showBatchConfirm = false },
            shape = RoundedCornerShape(18.dp),
            title = { Text("批量改名？") },
            text = { Text("Hermes 将根据对话内容重新生成 $count 个标题，每个标题最多 15 个字符。") },
            confirmButton = {
                TextButton(onClick = { showBatchConfirm = false; onBatchAiRename() }) { Text("开始改名") }
            },
            dismissButton = { TextButton(onClick = { showBatchConfirm = false }) { Text("取消") } },
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
    isBatchRenaming: Boolean,
    profiles: List<HermesProfile>,
    activeProfile: String,
    isProfilesLoading: Boolean,
    isProfileSwitching: Boolean,
    onNewSession: () -> Unit,
    onSearch: () -> Unit,
    onBatchAiRename: () -> Unit,
    onRefreshProfiles: () -> Unit,
    onSelectProfile: (HermesProfile) -> Unit,
) {
    val skin = HermesSkin.current
    var menuExpanded by remember { mutableStateOf(false) }
    var profileMenuExpanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(top = 8.dp, bottom = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (assistantAvatarUri.isNotBlank()) {
            UserAvatar(
                uri = assistantAvatarUri,
                displayName = assistantName,
                size = 42.dp,
                hermesFallback = true,
            )
        } else {
            HermesMark(compact = true, requestedSize = 42.dp)
        }
        Column(
            modifier = Modifier.padding(start = 10.dp).height(42.dp).weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                assistantName,
                style = MaterialTheme.typography.titleMedium.copy(lineHeight = 19.sp),
                fontWeight = FontWeight.Bold,
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
                    shape = RoundedCornerShape(10.dp),
                    containerColor = MaterialTheme.colorScheme.surface,
                ) {
                    if (isProfilesLoading && profiles.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("正在读取 Profile…") },
                            onClick = {},
                            enabled = false,
                        )
                    }
                    profiles.forEach { profile ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(
                                        (if (profile.name == activeProfile) "✓  " else "    ") + profile.name,
                                        fontWeight = if (profile.name == activeProfile) FontWeight.Bold else FontWeight.Medium,
                                    )
                                    val detail = profileDetailText(profile)
                                    if (detail.isNotBlank()) Text(
                                        detail,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            },
                            onClick = {
                                profileMenuExpanded = false
                                onSelectProfile(profile)
                            },
                            enabled = !isProfilesLoading,
                        )
                    }
                    if (profiles.isNotEmpty()) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                        DropdownMenuItem(
                            text = { Text(if (isProfilesLoading) "正在刷新…" else "刷新 Profile 列表") },
                            onClick = onRefreshProfiles,
                            enabled = !isProfilesLoading,
                        )
                    }
                }
            }
        }
        Box {
            IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(40.dp)) {
                if (isBatchRenaming) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                else HermesMulticolorIcon(HermesIconKind.MORE, contentDescription = "更多操作")
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                shape = RoundedCornerShape(10.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
                shadowElevation = if (skin.glass) 6.dp else 2.dp,
                border = BorderStroke(0.7.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
            ) {
                CompactMenuItem("新建对话", HermesIconKind.NEW_CHAT) { menuExpanded = false; onNewSession() }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                CompactMenuItem("搜索对话", HermesIconKind.SEARCH) { menuExpanded = false; onSearch() }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                CompactMenuItem("批量改名", HermesIconKind.AI, enabled = !isBatchRenaming) {
                    menuExpanded = false
                    onBatchAiRename()
                }
            }
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
private fun CompactMenuItem(label: String, icon: HermesIconKind, enabled: Boolean = true, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(label, fontWeight = FontWeight.Medium) },
        leadingIcon = { HermesMulticolorIcon(icon, contentDescription = null, iconSize = 21.dp) },
        enabled = enabled,
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
    )
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
    var modeMenu by remember { mutableStateOf(false) }
    var projectMenu by remember { mutableStateOf(false) }
    var timeMenu by remember { mutableStateOf(false) }
    val projectLabel = projects.firstOrNull { it.id == selectedProjectId }?.name ?: "全部项目"
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 5.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.weight(0.72f)) {
            FilterChipLabel(if (mode == SessionListMode.RECENT) "最近" else "项目", Modifier.fillMaxWidth(), onClick = { modeMenu = true })
            FilterDropdownMenu(
                expanded = modeMenu,
                onDismissRequest = { modeMenu = false },
                modifier = Modifier.widthIn(min = 124.dp),
            ) {
                SessionListMode.entries.forEach { option ->
                    val label = if (option == SessionListMode.RECENT) "最近" else "项目"
                    FilterDropdownItem(
                        label = label,
                        selected = option == mode,
                        onClick = { modeMenu = false; onModeChange(option) },
                    )
                }
            }
        }
        Box(Modifier.weight(1.28f)) {
            FilterChipLabel(projectLabel, Modifier.fillMaxWidth(), onClick = {
                onLoadProjects()
                projectMenu = true
            })
            FilterDropdownMenu(
                expanded = projectMenu,
                onDismissRequest = { projectMenu = false },
                modifier = Modifier.widthIn(min = 188.dp, max = 286.dp),
            ) {
                FilterDropdownItem(
                    label = "全部项目",
                    selected = selectedProjectId == null,
                    onClick = { projectMenu = false; onProjectChange(null) },
                )
                projects.forEach { project ->
                    FilterDropdownItem(
                        label = project.name,
                        selected = project.id == selectedProjectId,
                        onClick = { projectMenu = false; onProjectChange(project.id) },
                    )
                }
            }
        }
        Box(Modifier.weight(0.9f)) {
            FilterChipLabel(timeFilter.label, Modifier.fillMaxWidth(), onClick = { timeMenu = true })
            FilterDropdownMenu(
                expanded = timeMenu,
                onDismissRequest = { timeMenu = false },
                modifier = Modifier.widthIn(min = 152.dp),
            ) {
                SessionTimeFilter.entries.forEach { filter ->
                    FilterDropdownItem(
                        label = filter.label,
                        selected = filter == timeFilter,
                        onClick = { timeMenu = false; onTimeChange(filter) },
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        offset = DpOffset(x = 0.dp, y = 6.dp),
        shape = RoundedCornerShape(14.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 0.dp,
        shadowElevation = 7.dp,
        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.72f)),
        content = content,
    )
}

@Composable
private fun FilterDropdownItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        leadingIcon = {
            Box(Modifier.size(18.dp), contentAlignment = Alignment.Center) {
                if (selected) {
                    Text(
                        text = "✓",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        },
        onClick = onClick,
        modifier = Modifier
            .padding(horizontal = 6.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                else Color.Transparent,
            ),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
    )
}

@Composable
private fun FilterChipLabel(text: String, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    Surface(
        modifier = modifier.then(if (onClick == null) Modifier else Modifier.clickable(onClick = onClick)),
        shape = RoundedCornerShape(9.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.72f),
        tonalElevation = 0.dp,
    ) {
        Text(
            text = text + if (onClick == null) "" else "  ▾",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp),
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
) {
    val haptics = LocalHapticFeedback.current
    val palette = paletteFor(session)
    Column(
        modifier = Modifier.fillMaxWidth().combinedClickable(
            onClick = onClick,
            onLongClick = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onLongClick()
            },
        ),
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
            SessionAvatar(palette, isCron)
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (session.isPinned) {
                        HermesPinnedMarker(
                            modifier = Modifier.padding(end = 5.dp).size(17.dp),
                        )
                    }
                    Text(
                        ellipsizeSessionTitle(session.title),
                        style = MaterialTheme.typography.titleSmall,
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
        HorizontalDivider(modifier = Modifier.padding(start = 60.dp), thickness = 0.6.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.58f))
    }
}

@Composable
private fun SessionAvatar(palette: AvatarPalette, isCron: Boolean) {
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
        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(background.copy(alpha = 0.82f)),
        contentAlignment = Alignment.Center,
    ) {
        HermesMulticolorIcon(
            if (isCron) HermesIconKind.RECENT else HermesIconKind.CHAT,
            contentDescription = null,
            iconSize = if (isCron) 21.dp else 20.dp,
        )
    }
}

@Composable
private fun ProjectSectionHeader(name: String, count: Int, muted: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 10.dp, bottom = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(28.dp).clip(RoundedCornerShape(9.dp))
                .background(if (muted) MaterialTheme.colorScheme.surfaceVariant else HermesColors.extended.successContainer),
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
        shape = RoundedCornerShape(18.dp),
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
        Text(ellipsizeSessionTitle(session.title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
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
        HermesPinnedMarker(modifier = Modifier.size(21.dp), contentDescription = null)
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
            Text("移至项目", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CronGroupHeader(count: Int, expanded: Boolean, onClick: () -> Unit) {
    val purple = MaterialTheme.colorScheme.secondary
    Surface(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp).combinedClickable(onClick = onClick),
        shape = RoundedCornerShape(13.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.82f),
        tonalElevation = 0.dp,
    ) {
        Row(modifier = Modifier.padding(horizontal = 11.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(34.dp).clip(RoundedCornerShape(11.dp)).background(purple.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
                HermesMulticolorIcon(HermesIconKind.RECENT, contentDescription = null, iconSize = 19.dp)
            }
            Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
                Text("定时任务对话", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("$count 个 Cron 会话，默认折叠", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            HermesMulticolorIcon(
                if (expanded) HermesIconKind.EXPAND_UP else HermesIconKind.EXPAND_DOWN,
                contentDescription = if (expanded) "收起" else "展开",
            )
        }
    }
}

@Composable
private fun EmptySessions(onNewSession: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 36.dp, horizontal = 22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(54.dp).clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
            HermesMulticolorIcon(HermesIconKind.AI, contentDescription = null, iconSize = 27.dp)
        }
        Text("还没有会话", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 10.dp))
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

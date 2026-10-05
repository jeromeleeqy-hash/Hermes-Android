package com.qingyu.hermescompanion.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qingyu.hermescompanion.model.*
import com.qingyu.hermescompanion.today.*
import com.qingyu.hermescompanion.ui.AppUiState
import com.qingyu.hermescompanion.ui.SkinMode
import com.qingyu.hermescompanion.ui.component.*
import com.qingyu.hermescompanion.ui.theme.HermesSkin
import com.qingyu.hermescompanion.ui.theme.HermesSpacing
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayOverviewScreen(
    state: AppUiState, contentPadding: PaddingValues,
    onRefresh: () -> Unit, onGenerate: () -> Unit,
    onPath: (String, Boolean) -> Unit, onCardAction: (String, String) -> Unit,
    onDaily: () -> Unit, onStart: (String) -> Unit, onSearch: () -> Unit,
    onRespond: (AgentRequest, String) -> Unit, onWelcomed: () -> Unit,
    onCompact: () -> Unit = onGenerate,
    onInteraction: (String, String) -> Unit = { _, _ -> },
    onSchedule: (String, String, String) -> Unit = { _, _, _ -> },
    onCheckSchedule: () -> Unit = {},
    onUpdate: () -> Unit = onGenerate,
    onMigrate: () -> Unit = {},
    onOpenRefresh: () -> Unit = {},
    onSettings: (() -> Unit)? = null,
) {
    val today = state.today
    val board = today.board
    val now = rememberHomeTime()
    val localDate = now.toLocalDate()
    var groupKey by rememberSaveable(state.baseUrl, state.activeProfile) { mutableStateOf("") }
    var expanded by rememberSaveable(state.baseUrl, state.activeProfile) { mutableStateOf(false) }
    var showClosed by rememberSaveable(state.baseUrl, state.activeProfile) { mutableStateOf(false) }
    var detailId by rememberSaveable(state.baseUrl, state.activeProfile) { mutableStateOf<String?>(null) }
    var operationId by rememberSaveable(state.baseUrl, state.activeProfile) { mutableStateOf<String?>(null) }
    var quickId by rememberSaveable(state.baseUrl, state.activeProfile) { mutableStateOf<String?>(null) }
    var showSummary by rememberSaveable { mutableStateOf(false) }
    var settings by rememberSaveable { mutableStateOf(false) }
    var scenes by rememberSaveable { mutableStateOf(false) }
    val list = rememberLazyListState()
    val group = TodayAttentionGroup.entries.firstOrNull { it.key == groupKey }
    val filtered = focusCards(board?.cards.orEmpty(), showClosed).filter { group == null || it.attentionGroup == group }
    val visible = if (expanded) filtered else filtered.take(4)
    val searchLabel = todayText("搜索对话", "Search conversations")
    val canPrepare = today.rootVerified && !today.loading && !state.isBusy && !state.isProfileSwitching

    fun handleCardAction(id: String, value: String) {
        if (value == "processing" || value == "recover") {
            quickId = null
            operationId = id
            onCardAction(id, if (value == "processing") "check" else value)
        } else onCardAction(id, value)
    }

    LazyColumn(Modifier.fillMaxSize().testTag("today_home").padding(top = contentPadding.calculateTopPadding()).statusBarsPadding(),
        state = list, contentPadding = PaddingValues(start = HermesSpacing.page, end = HermesSpacing.page, top = 8.dp, bottom = contentPadding.calculateBottomPadding() + 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item(key = "header") {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(now.format(DateTimeFormatter.ofPattern(todayText("M月d日 · EEEE", "EEE, MMM d"), com.qingyu.hermescompanion.i18n.AppLanguage.locale)),
                    Modifier.weight(1f), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                IconButton(onClick = onSearch, modifier = Modifier.semantics { contentDescription = searchLabel }) { AssistantGlyph("search", Modifier.size(23.dp)) }
                IconButton(onClick = { onSettings?.invoke() ?: run { settings = true } }, modifier = Modifier.testTag("today-settings").semantics { contentDescription = todayText("首页设置", "Home settings") }) {
                    HermesMulticolorIcon(HermesIconKind.SETTINGS, null, iconSize = 23.dp)
                }
            }
        }
        item(key = "hero") {
            val active by remember { derivedStateOf { list.layoutInfo.visibleItemsInfo.any { it.key == "hero" } } }
            CompactTodayHero(state, active, now.hour, onDaily, { scenes = true }, onWelcomed)
        }
        if (state.pendingAgentRequests.isNotEmpty()) item(key = "approval") { DecisionCard(state.pendingAgentRequests.first(), onRespond) }
        item(key = "briefing") {
            TodayBriefingHeader(state, filtered.size, localDate, onUpdate, onOpenRefresh, { showSummary = true })
        }
        if (board == null && !today.loading) item(key = "empty") {
            TodaySurface {
                Text(todayText("让重要的事，在这里汇合", "Bring important things together"), style = MaterialTheme.typography.titleMedium)
                Text(todayText("请 Hermes 整理最近的安排和想法，或设置早晚自动整理。", "Ask Hermes to organize your plans and ideas, or set a daily schedule."),
                    Modifier.padding(top = 8.dp, bottom = 12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                FilledTonalButton(onClick = onGenerate, enabled = canPrepare) { Text(todayText("开始整理", "Get started")) }
            }
        }
        if (board != null && board.cards.filterNot { it.isClosed }.map { it.attentionGroup }.distinct().size > 1) item(key = "filters") {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                TodayCategory(todayText("全部", "All"), group == null) { groupKey = ""; expanded = false }
                TodayAttentionGroup.entries.filter { candidate -> board.cards.any { !it.isClosed && it.attentionGroup == candidate } }.forEach { candidate ->
                    TodayCategory(candidate.label, candidate == group) { groupKey = candidate.key; expanded = false }
                }
            }
        }
        items(visible, key = { "card:${it.id}" }) { card ->
            val action = state.todayActions["${today.profile}\n${today.root}\n${card.id}"]
            TodayFocusCard(card, onDetails = { detailId = card.id }, onAction = { value -> handleCardAction(card.id, value) }, action = action)
        }
        if (board != null && (filtered.size > 4 || board.cards.any { it.isClosed } || filtered.isEmpty())) item(key = "more") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                if (filtered.size > 4) HermesContentAction(onClick = { expanded = !expanded }) {
                    Text(if (expanded) todayText("收起", "Show fewer") else todayText("查看其余 ${filtered.size - 4} 件", "Show ${filtered.size - 4} more"))
                }
                if (board.cards.any { it.isClosed }) HermesContentAction(icon = "history", onClick = { showClosed = !showClosed }) {
                    Text(if (showClosed) todayText("隐藏已结束", "Hide closed") else todayText("查看已结束", "Show closed"))
                }
            }
            if (filtered.isEmpty()) Text(todayText("这里暂时没有需要留意的事", "Nothing to highlight here right now"), style = MaterialTheme.typography.bodyMedium)
        }
    }
    val detail = board?.cards?.firstOrNull { it.id == detailId }
    if (detail != null) TodayCardDetailSheet(detail, onDismiss = { detailId = null },
        onPath = { source -> detailId = null; onPath(source, false) },
        onAction = { action -> detailId = null; handleCardAction(detail.id, action) },
        onQuickResponse = if (detail.presentation.interaction != null) ({ detailId = null; quickId = detail.id }) else null,
        action = state.todayActions["${today.profile}\n${today.root}\n${detail.id}"])
    val quick = board?.cards?.firstOrNull { it.id == quickId }
    if (quick?.presentation?.interaction != null) TodayInteractiveSheet(quick,
        enabled = !state.isBusy && !state.isProfileSwitching && today.rootVerified && !today.loading && today.error == null,
        onDismiss = { quickId = null; detailId = quick.id }, onSubmit = { request -> onInteraction(quick.id, request) },
        onPath = { source -> quickId = null; onPath(source, false) },
        action = state.todayActions["${today.profile}\n${today.root}\n${quick.id}"],
        onProcessing = { quickId = null; handleCardAction(quick.id, "processing") },
        onRecovery = { handleCardAction(quick.id, "recover") })
    val operation = operationId?.let { state.todayActions["${today.profile}\n${today.root}\n$it"] }
    if (operation != null) TodayOperationDialog(operation,
        title = board?.cards?.firstOrNull { it.id == operation.cardId }?.title ?: todayText("这次卡片操作", "Card operation"),
        requests = state.pendingAgentRequests.filter { it.conversationId == operation.sessionId && (it.profile.isBlank() || it.profile == operation.profile) },
        onDismiss = { operationId = null }, onCheck = { onCardAction(operation.cardId, "check") },
        onRecover = { onCardAction(operation.cardId, "recover") },
        onConversation = { onCardAction(operation.cardId, "conversation") }, onRespond = onRespond)
    if (settings) HomeSettingsWindow(state.copy(homeMode = com.qingyu.hermescompanion.ui.HomeMode.DEEP),
        onDismiss = { settings = false }, onMode = {}, onGenerate = onGenerate, onRefresh = onRefresh,
        onMigrate = onMigrate, onCompact = onCompact, onSchedule = onSchedule, onCheckSchedule = onCheckSchedule)
    if (showSummary) TodaySyncDetailsWindow(state, { showSummary = false }, onRefresh)
    if (scenes) TodayScenesSheet(onDismiss = { scenes = false }, onStart = { scenes = false; onStart(it) })
}

@Composable
internal fun TodayBriefingHeader(state: AppUiState, count: Int, localDate: LocalDate,
    onUpdate: () -> Unit, onOpenRefresh: () -> Unit, onDetails: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val board = state.today.board
    val refresh = state.todayRefresh
    val updating = state.today.loading || refresh.busy
    val needsConfirmation = state.pendingAgentRequests.any { it.conversationId == refresh.sessionId &&
        (it.profile.isBlank() || it.profile == refresh.profile) }
    Column(Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 2.dp).testTag("today-briefing-header"),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(todayText("为你留意", "On your radar"), Modifier.weight(1f, fill = false),
                        style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (board != null) Surface(shape = RoundedCornerShape(8.dp), color = colors.onSurface.copy(alpha = .055f)) {
                        Text(count.toString(), Modifier.padding(horizontal = 7.dp, vertical = 2.dp), style = MaterialTheme.typography.labelMedium,
                            color = colors.onSurfaceVariant, maxLines = 1)
                    }
                }
                if (board != null) {
                    val updated = board.generatedAt.atZoneSameInstant(java.time.ZoneId.systemDefault())
                    val time = updated.format(DateTimeFormatter.ofPattern(if (updated.toLocalDate() == localDate) "HH:mm" else "M/d HH:mm"))
                    Text(todayText("更新于 $time", "Updated $time"), style = MaterialTheme.typography.labelMedium,
                        color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Row(Modifier.heightIn(min = 48.dp).testTag("today-refresh-button")
                .clip(RoundedCornerShape(14.dp)).background(colors.primaryContainer.copy(alpha = .65f))
                .clickable(enabled = !updating && !state.isProfileSwitching && !state.isBusy, role = Role.Button, onClick = onUpdate)
                .semantics { contentDescription = todayText("核对最新进展并更新首页", "Check recent progress and update overview") }
                .padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                if (updating) CircularProgressIndicator(Modifier.size(16.dp).testTag("today-loading"), strokeWidth = 1.7.dp, color = colors.primary)
                else AssistantGlyph("refresh", Modifier.size(17.dp), colors.primary)
                Spacer(Modifier.width(6.dp))
                Text(if (updating) todayText("更新中", "Updating") else todayText("刷新", "Refresh"),
                    style = MaterialTheme.typography.labelLarge, color = colors.primary, maxLines = 1, softWrap = false)
            }
        }
        if (refresh.busy || refresh.message.isNotBlank()) {
            val canOpen = refresh.sessionId.isNotBlank()
            val status = when {
                needsConfirmation -> todayText("需要你确认", "Your confirmation is needed")
                refresh.busy -> todayText("正在更新首页", "Updating your overview")
                else -> refresh.message
            }
            Surface(shape = RoundedCornerShape(14.dp), color = colors.onSurface.copy(alpha = .04f),
                modifier = Modifier.fillMaxWidth().testTag("overview-update-status")
                    .then(if (canOpen) Modifier.clip(RoundedCornerShape(14.dp)).clickable(onClick = onOpenRefresh) else Modifier)) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    AssistantGlyph(if (needsConfirmation) "check" else "history", Modifier.size(17.dp), colors.onSurfaceVariant)
                    Text(status, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant,
                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                    if (canOpen) {
                        Text(todayText("查看处理", "Details"), style = MaterialTheme.typography.labelMedium, color = colors.primary,
                            maxLines = 1, softWrap = false)
                        AssistantGlyph("chevron", Modifier.size(14.dp), colors.primary)
                    }
                }
            }
        }
        if (state.today.error != null || state.today.cacheError != null) HermesContentAction(onClick = onDetails) {
            Text(if (board == null) todayText("暂时无法同步 · 查看详情", "Sync unavailable · Details")
                else todayText("显示已保存内容 · 查看详情", "Showing saved content · Details"), style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun TodayCategory(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(Modifier.heightIn(min = 44.dp).selectable(selected, role = Role.Tab, onClick = onClick)
        .padding(horizontal = 14.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = if (selected) colors.primary else colors.onSurfaceVariant,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal)
        Box(Modifier.width(24.dp).height(2.dp).background(if (selected) colors.primary else colors.primary.copy(alpha = 0f), RoundedCornerShape(2.dp)))
    }
}

@Composable
private fun TodaySurface(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    // The same flat, borderless surface and corner token used by the conversation list.
    GlassPanel(modifier.fillMaxWidth().then(if (onClick == null) Modifier else Modifier.clickable(onClick = onClick))) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 18.dp), content = content)
    }
}

@Composable
private fun TodayFocusCard(card: TodayCard, onDetails: () -> Unit, onAction: (String) -> Unit, action: TodayActionState? = null) {
    TodaySurface(Modifier.testTag("today-card:${card.id}"), onDetails) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            AssistantGlyph(card.domain.glyph, Modifier.size(16.dp), MaterialTheme.colorScheme.primary)
            Text(card.domain.label, Modifier.weight(1f).padding(start = 7.dp), style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = .065f)) {
                Text(if (card.isClosed) todayText("已结束", "Closed") else card.attentionGroup.label,
                    Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary)
            }
        }
        Text(card.title.removePrefix(card.kind.zh + "：").removePrefix(card.kind.zh + ":").trim(),
            Modifier.padding(top = 10.dp, bottom = 8.dp), fontSize = 19.sp, lineHeight = 27.sp, fontWeight = FontWeight.SemiBold)
        val context = card.readableContext.ifBlank { card.presentation.facts.firstOrNull().orEmpty() }
        if (context.isNotBlank()) Text(context, Modifier.testTag("today-context:${card.id}"),
            style = MaterialTheme.typography.bodyMedium, lineHeight = 23.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        TodayReadableContent(card)
        if (action != null && action.status in setOf("awaiting_input", "uncertain", "failed")) HermesContentAction(
            onClick = { onAction("processing") }) {
            Text(todayActionLabel(action), Modifier.testTag("today-action:${card.id}"), style = MaterialTheme.typography.labelMedium)
        }
        Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            HermesContentAction(icon = "chat", onClick = { onAction("discuss") }, modifier = Modifier.testTag("today-discuss:${card.id}")) {
                Text(todayText("聊聊这件事", "Talk about this"), style = MaterialTheme.typography.labelLarge)
            }
            TextButton(onClick = onDetails, contentPadding = PaddingValues(0.dp)) {
                Text(todayText("详情", "Details"), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                AssistantGlyph("chevron", Modifier.padding(start = 3.dp).size(15.dp), MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun CompactTodayHero(state: AppUiState, active: Boolean, hour: Int, onDaily: () -> Unit, onScenes: () -> Unit, onWelcomed: () -> Unit) {
    val greeting = when (hour) { in 5..10 -> todayText("早上好", "Good morning"); in 11..16 -> todayText("下午好", "Good afternoon"); else -> todayText("晚上好", "Good evening") }
    Column {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).padding(end = 10.dp)) {
                Text(greeting + "，" + state.userProfile.displayName.ifBlank { state.username }.ifBlank { todayText("朋友", "friend") },
                    fontSize = 25.sp, lineHeight = 32.sp, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            HomePortraitCarousel(Modifier.size(76.dp, 82.dp), active = active,
                reduceMotion = state.reduceMotion, onTap = onWelcomed)
        }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = onDaily, modifier = Modifier.weight(1.35f).heightIn(min = 48.dp).testTag("daily_conversation_entry"), enabled = !state.isDailyOpening && !state.isProfileSwitching) {
                AssistantGlyph("wave", Modifier.padding(end = 8.dp).size(21.dp), LocalContentColor.current); Text(todayText("跟我说", "Talk to me"))
            }
            OutlinedButton(onClick = onScenes, modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("home-help"), enabled = !state.isProfileSwitching) { Text(todayText("帮我做…", "Help me…")) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TodayScenesSheet(onDismiss: () -> Unit, onStart: (String) -> Unit) {
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    var details by rememberSaveable { mutableStateOf("") }
    val scenes = helpScenes()
    val choice = scenes.firstOrNull { it.id == selected }
    val columns = if (androidx.compose.ui.platform.LocalDensity.current.fontScale > 1.25f) 1 else 2
    TodayDetailWindow(onDismiss, title = todayText("帮我做", "Help me")) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(HermesSpacing.page)
            .testTag("help-scenes"), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(todayText("想一起完成什么？", "What shall we work on?"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                Text(todayText("选个方向，或直接说说你的想法。", "Choose a starting point, or describe your idea."), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                scenes.chunked(columns).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        row.forEach { item ->
                            val chosen = item.id == selected
                            Surface(modifier = Modifier.weight(1f).testTag("help-scene:${item.id}")
                                .clip(RoundedCornerShape(20.dp)).selectable(chosen, role = Role.RadioButton, onClick = { selected = if (chosen) null else item.id }),
                                shape = RoundedCornerShape(20.dp),
                                color = if (chosen) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                border = if (chosen) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .65f)) else null) {
                                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        AssistantGlyph(item.glyph, Modifier.size(23.dp), MaterialTheme.colorScheme.primary)
                                        if (chosen) AssistantGlyph("check", Modifier.size(18.dp), MaterialTheme.colorScheme.primary)
                                    }
                                    Text(item.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                    Text(item.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
            OutlinedTextField(details, { details = it }, modifier = Modifier.fillMaxWidth().testTag("help-context"),
                label = { Text(todayText("说说具体的事", "Tell me more")) },
                placeholder = { Text(choice?.hint ?: todayText("想做什么、卡在哪里，随便说说就好…", "What would you like to do? Where are you stuck?")) },
                shape = RoundedCornerShape(18.dp), minLines = 3, maxLines = 6)
            Text(todayText("资料可以进入对话后再添加。", "You can add files once the conversation opens."), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = HermesSpacing.page).padding(top = 10.dp, bottom = 16.dp)) {
            com.qingyu.hermescompanion.ui.component.HermesButton(onClick = {
                onStart(listOfNotNull(choice?.prompt, details.trim().takeIf { it.isNotEmpty() }).joinToString("\n\n"))
            }, modifier = Modifier.fillMaxWidth().testTag("help-start"), enabled = choice != null || details.isNotBlank()) {
                Text(todayText("开始聊聊", "Let's talk"))
            }
        }
    }
}

private data class HelpScene(val id: String, val glyph: String, val title: String, val subtitle: String, val hint: String, val prompt: String)
private fun helpScenes() = listOf(
    HelpScene("meeting", "chat", todayText("整理会议", "Meeting notes"), todayText("结论、分工与跟进", "Decisions and follow-ups"),
        todayText("哪次会议？有哪些资料或你记得的重点？", "Which meeting? What notes or key points do you have?"),
        todayText("帮我整理一次会议或讨论的结论、分工和未确认事项。先问清是哪次会议、有哪些资料，不把建议当作已作出的决定。", "Help organize a meeting's decisions, responsibilities and open questions. First ask which meeting and what materials I have. Keep suggestions separate from decisions.")),
    HelpScene("information", "folder", todayText("梳理资料", "Organize information"), todayText("提炼重点，理清线索", "Key points and connections"),
        todayText("想整理哪些资料？准备拿来做什么？", "What material, and what will you use it for?"),
        todayText("帮我梳理资料、提炼重点和关联。先确认资料范围和用途，不把历史归档当成当前任务。", "Help organize information and its connections. First confirm the scope and purpose; do not treat archives as current tasks.")),
    HelpScene("plan", "sparkles", todayText("一起做方案", "Work on a plan"), todayText("比较选择，明确下一步", "Options and next steps"),
        todayText("要解决什么问题？有什么限制？", "What problem are we solving? Any constraints?"),
        todayText("和我一起分析问题、比较方案并明确下一步。先确认目标和限制，缺少的信息向我询问。", "Help analyze a problem, compare options and plan next steps. Ask about my goal, constraints and missing information first.")),
    HelpScene("research", "search", todayText("学习研究", "Learn and research"), todayText("弄懂问题，整理认识", "Understand and reflect"),
        todayText("想弄懂什么？目前了解多少？", "What would you like to understand? What do you already know?"),
        todayText("帮我学习研究一个问题。先了解我想弄懂什么、已有基础和希望的深度，再一起整理结论和依据。", "Help me research a question. First understand the topic, my background and desired depth, then organize findings and evidence.")),
    HelpScene("writing", "file", todayText("协助写作", "Write together"), todayText("从想法到清楚的表达", "Ideas into clear words"),
        todayText("写给谁看？想表达什么？", "Who is it for, and what would you like to say?"),
        todayText("帮我起草或修改一份文字。先确认用途、读者、想表达的重点和已有素材。", "Help draft or revise a piece of writing. First ask about its purpose, audience, key points and existing material.")),
    HelpScene("life", "calendar", todayText("安排生活", "Everyday plans"), todayText("行程、备忘与小计划", "Plans and reminders"),
        todayText("想安排哪件事？有没有时间要求？", "What would you like to plan? Any dates in mind?"),
        todayText("帮我整理一件生活安排或备忘。先问清具体事情和时间要求；新增提醒前和我确认。", "Help organize an everyday plan or reminder. Ask about the details and timing, and confirm before creating reminders.")),
)

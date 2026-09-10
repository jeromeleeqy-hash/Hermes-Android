package com.qingyu.hermescompanion.ui.screen

import com.qingyu.hermescompanion.i18n.uiText
import com.qingyu.hermescompanion.i18n.AppLanguage
import java.time.format.DateTimeFormatter
import com.qingyu.hermescompanion.R


import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import com.qingyu.hermescompanion.ui.component.HermesButton as Button
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.HorizontalAlignmentLine
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qingyu.hermescompanion.model.*
import com.qingyu.hermescompanion.ui.AppUiState
import com.qingyu.hermescompanion.ui.SkinMode
import com.qingyu.hermescompanion.ui.component.*
import com.qingyu.hermescompanion.ui.theme.HermesSkin
import com.qingyu.hermescompanion.ui.format.conversationPreview
import com.qingyu.hermescompanion.ui.format.sessionTimeLabel
import androidx.compose.ui.semantics.Role
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
fun AssistantHomeScreen(
    state: AppUiState, contentPadding: PaddingValues,
    onStart: (String) -> Unit, onOpen: (HermesSession) -> Unit,
    onHistory: () -> Unit, onTasks: () -> Unit, onFiles: () -> Unit,
    onRespond: (AgentRequest, String) -> Unit, onSearch: () -> Unit,
    onDaily: () -> Unit = { onStart("") },
    onWelcomed: () -> Unit = {},
) {
    val skin = HermesSkin.current.mode
    val user = state.userProfile.displayName.ifBlank { state.username }.ifBlank { uiText(R.string.ui_0605, "朋友") }
    val runs = state.runningRuns.filter { it.session.profile == state.activeProfile }
    val recent = (runs.map { it.session } + state.sessions.filter { it.profile == state.activeProfile })
        .distinctBy { it.scopedId }.take(2)
    val now = rememberHomeTime()
    val locale = AppLanguage.locale
    val dateLabel = remember(now.toLocalDate(), locale) {
        now.format(DateTimeFormatter.ofPattern(if (locale.language == "zh") "M月d日 · EEEE" else "EEE, MMM d", locale))
    }
    val quote = rememberHomeQuote(now.toLocalDate())
    val greeting = when (dayPeriod(now.hour)) { DayPeriod.MORNING -> uiText(R.string.ui_0606, "早上好"); DayPeriod.NOON -> uiText(R.string.ui_0607, "中午好"); DayPeriod.AFTERNOON -> uiText(R.string.ui_0608, "下午好"); else -> uiText(R.string.ui_0609, "晚上好") }
    val listState = rememberLazyListState()
    val heroVisible by remember { derivedStateOf { listState.layoutInfo.visibleItemsInfo.any { it.key == "hero" } } }
    var greeted by rememberSaveable { mutableStateOf(state.homeWelcomed) }
    val interaction = remember { MascotInteraction() }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val motionEnabled = !state.reduceMotion && !LocalReduceMotion.current && android.animation.ValueAnimator.areAnimatorsEnabled() && android.os.Build.VERSION.SDK_INT >= 28
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_PAUSE) interaction.finish() }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer); interaction.finish() }
    }
    LaunchedEffect(heroVisible, motionEnabled, skin) { if (!heroVisible || !motionEnabled) interaction.finish() }
    val onTap = { if (heroVisible && motionEnabled) { greeted = true; onWelcomed(); interaction.tap(true) } }
    val motion = interaction.motion ?: if (skin == SkinMode.PAPER) MascotMotion.IDLE_HALF else if (greeted) MascotMotion.IDLE_FULL else MascotMotion.WELCOME
    val onFinished = { if (interaction.motion != null) interaction.finish() else { greeted = true; onWelcomed() } }
    val caption = when {
        state.pendingAgentRequests.isNotEmpty() -> uiText(R.string.ui_0610, "有 %1\$s 件事，等你决定。", state.pendingAgentRequests.size)
        runs.isNotEmpty() -> uiText(R.string.ui_0611, "有 %1\$s 件事正在进行，我在这里。", runs.size)
        else -> quote
    }
    CompositionLocalProvider(LocalReduceMotion provides (state.reduceMotion || LocalReduceMotion.current)) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
        val statusHeight = with(LocalDensity.current) { WindowInsets.statusBars.getTop(this).toDp() }
        val visibleHeight = maxHeight - contentPadding.calculateTopPadding() - statusHeight - contentPadding.calculateBottomPadding()
        // Budget the real viewport, including a complete pair of recent rows.
        // Large accessibility fonts retain scrolling instead of shrinking text.
        val glassPortraitHeight = (visibleHeight - 444.dp).coerceIn(124.dp, 220.dp)
        val warmPortraitHeight = (visibleHeight - 330.dp).coerceIn(224.dp, 300.dp)
        val compactHome = visibleHeight < 740.dp
        LazyColumn(Modifier.fillMaxSize().testTag("home_root").padding(top = contentPadding.calculateTopPadding()).statusBarsPadding(),
            state = listState, contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 8.dp, bottom = contentPadding.calculateBottomPadding() + 12.dp)) {
            item(key = "header") {
                Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("home_header"), verticalAlignment = Alignment.CenterVertically) {
                    Text(dateLabel, Modifier.weight(1f).testTag("home_date"),
                        fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    IconButton(onClick = onSearch, modifier = Modifier.size(48.dp)
                        .then(if (skin == SkinMode.GLASS) Modifier.hermesChrome(CircleShape, .30f, backgroundOnly = true)
                        else if (skin == SkinMode.CLEAN) Modifier.background(MaterialTheme.colorScheme.surfaceContainer, CircleShape) else Modifier)
                        .semantics { contentDescription = uiText(R.string.ui_0613, "搜索对话") }) { AssistantGlyph("search", Modifier.size(23.dp)) }
                }
            }
            item(key = "hero") {
                when (skin) {
                    SkinMode.CLEAN -> WarmHomeHero(greeting, user, caption, state, motion, heroVisible, onFinished, onDaily, warmPortraitHeight, onTap, motionEnabled)
                    SkinMode.GLASS -> GlassHomeHero(greeting, user, caption, state, motion, heroVisible, onFinished, onDaily, glassPortraitHeight, onTap, motionEnabled)
                    SkinMode.PAPER -> QuietHomeHero(greeting, user, caption, state, heroVisible, onDaily, motion, onFinished, onTap, motionEnabled)
                }
            }
            if (state.pendingAgentRequests.isNotEmpty()) item(key = "decision") {
                Box(Modifier.padding(top = 20.dp)) { DecisionCard(state.pendingAgentRequests.first(), onRespond) }
            }
            item(key = "recent") {
                val panel = if (skin == SkinMode.GLASS) Modifier.padding(top = 18.dp)
                    .clip(MaterialTheme.shapes.large).background(MaterialTheme.colorScheme.surface.copy(alpha = .94f)).padding(horizontal = 14.dp, vertical = 10.dp)
                    else Modifier.padding(top = 14.dp)
                Column(panel.testTag("home_recent")) {
                    Row(Modifier.fillMaxWidth().padding(bottom = if (skin == SkinMode.GLASS) 0.dp else 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(uiText(R.string.ui_0614, "接着上次的事"), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        TextButton(onClick = onHistory) { Text(uiText(R.string.ui_0615, "全部")); AssistantGlyph("chevron", Modifier.padding(start = 4.dp).size(14.dp), MaterialTheme.colorScheme.primary) }
                    }
                    val entries = if (recent.isEmpty()) listOf(
                        HomeEntry(uiText(R.string.ui_0616, "任务与定时"), uiText(R.string.ui_0617, "查看任务、提醒和执行记录"), "", false, onTasks),
                        HomeEntry(uiText(R.string.ui_0618, "文件与成果"), uiText(R.string.ui_0619, "整理过的资料，都在这里"), "", false, onFiles))
                    else recent.map { session ->
                        val run = runs.firstOrNull { it.session.scopedId == session.scopedId }
                        HomeEntry(session.title, run?.let { if (it.recovering) uiText(R.string.ui_0620, "正在恢复连接") else it.stage }
                            ?: if (session.source.equals("cron", ignoreCase = true)) uiText(R.string.ui_1299, "点按查看完整执行内容") else conversationPreview(session.preview).ifBlank { uiText(R.string.ui_0621, "点击继续这段对话") },
                            if (run != null) uiText(R.string.ui_0622, "进行中") else sessionTimeLabel(session.updatedAt), session.scopedId in state.unreadSessionIds) { onOpen(session) }
                    }
                    BoxWithConstraints {
                        // Keep the pair only when each card has enough room for large type.
                        if (skin == SkinMode.CLEAN && maxWidth >= 300.dp && LocalDensity.current.fontScale <= 1.2f) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                entries.forEach { entry -> RecentCard(entry, Modifier.weight(1f), compactHome) }
                            }
                        } else Column {
                            if (skin == SkinMode.PAPER) HorizontalDivider(thickness = .5.dp)
                            entries.forEachIndexed { i, entry ->
                                RecentRow(entry, skin, compactHome)
                                if (skin == SkinMode.PAPER || i != entries.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = .5.dp)
                            }
                        }
                    }
                }
            }
        }
        }
    }
}

@Composable private fun DailyAction(state: AppUiState, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val glass = HermesSkin.current.glass
    val shape = MaterialTheme.shapes.small
    val enabled = !state.isDailyOpening && !state.isProfileSwitching
    Button(onClick, modifier = modifier.testTag("daily_conversation_entry")
        .then(if (glass) Modifier.hermesChrome(shape, .30f, backgroundOnly = true) else Modifier), enabled = enabled,
        shape = shape, elevation = null,
        colors = if (glass) ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = MaterialTheme.colorScheme.primary,
            disabledContainerColor = Color.Transparent) else ButtonDefaults.buttonColors(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp)) {
        if (glass) AssistantGlyph("wave", Modifier.padding(end = 12.dp).size(23.dp), MaterialTheme.colorScheme.primary)
        Text(if (state.isDailyOpening) uiText(R.string.ui_0623, "正在打开…") else uiText(R.string.ui_0624, "跟我说"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        if (!glass) AssistantGlyph("arrow", Modifier.padding(start = 14.dp).size(20.dp), MaterialTheme.colorScheme.onPrimary)
    }
}

@Composable private fun WarmHomeHero(greeting: String, user: String, caption: String, state: AppUiState,
    motion: MascotMotion, active: Boolean, onFinished: () -> Unit, onDaily: () -> Unit, h: androidx.compose.ui.unit.Dp, onTap: () -> Unit, motionEnabled: Boolean) {
    BoxWithConstraints(Modifier.fillMaxWidth().testTag("home_hero_warm").padding(top = 4.dp)) {
        val compact = maxWidth < 320.dp || LocalDensity.current.fontScale > 1.2f
        Layout(modifier = Modifier.fillMaxWidth().heightIn(min = h), content = {
            Column(Modifier.padding(top = 12.dp, bottom = 12.dp, end = 4.dp)) {
                Text(uiText(R.string.ui_0045, "日常助理"), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Box(Modifier.padding(top = 8.dp, bottom = 12.dp).size(24.dp, 2.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = .45f)))
                WarmPortraitAnchor(WarmPortraitTop) {
                    Text("$greeting，", Modifier.testTag("warm_greeting"), fontSize = if (compact) 25.sp else 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold)
                }
                Text(user, fontSize = if (compact) 25.sp else 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(caption, Modifier.padding(top = 12.dp, bottom = 16.dp).testTag("home_caption"),
                    style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    minLines = 2, maxLines = 2, overflow = TextOverflow.Ellipsis)
                WarmPortraitAnchor(WarmPortraitBottom, bottom = true) { DailyAction(state, onDaily) }
            }
            HermesMascot(motion, Modifier.testTag("home_hermes_portrait").mascotTap(motionEnabled, onTap),
                active = active, widePose = true, alignStandingBody = true, onFinished = onFinished)
        }) { measurables, constraints ->
            val width = constraints.maxWidth
            val textWidth = (width * .52f).toInt()
            val text = measurables[0].measure(constraints.copy(minWidth = textWidth, maxWidth = textWidth, minHeight = 0))
            val height = constraints.constrainHeight(text.height)
            val textY = (height - text.height) / 2
            val top = text[WarmPortraitTop]
            val bottom = text[WarmPortraitBottom]
            val portrait = measurables[1].measure(Constraints.fixed(width - textWidth, (bottom - top).coerceAtLeast(1)))
            layout(width, height) {
                text.placeRelative(0, textY)
                portrait.placeRelative(textWidth, textY + top)
            }
        }
    }
}

private val WarmPortraitTop = HorizontalAlignmentLine { a, b -> minOf(a, b) }
private val WarmPortraitBottom = HorizontalAlignmentLine { a, b -> maxOf(a, b) }

/** Publish the text/action anchors during measurement, avoiding a visible second-frame jump. */
@Composable private fun WarmPortraitAnchor(line: HorizontalAlignmentLine, bottom: Boolean = false, content: @Composable () -> Unit) {
    Layout(content = content) { measurables, constraints ->
        val child = measurables.single().measure(constraints)
        val anchor = if (bottom) child.height else 4.sp.roundToPx()
        layout(child.width, child.height, mapOf(line to anchor)) { child.placeRelative(0, 0) }
    }
}

@Composable private fun GlassHomeHero(greeting: String, user: String, caption: String, state: AppUiState,
    motion: MascotMotion, active: Boolean, onFinished: () -> Unit, onDaily: () -> Unit, h: androidx.compose.ui.unit.Dp, onTap: () -> Unit, motionEnabled: Boolean) {
    Column(Modifier.fillMaxWidth().testTag("home_hero_glass").padding(top = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(uiText(R.string.home_greeting, "%1\$s，%2\$s", greeting, user), fontSize = 26.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(caption, Modifier.padding(top = 6.dp, bottom = 10.dp).testTag("home_caption"), style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
        HermesMascot(motion, Modifier.width(h * .74f).height(h).testTag("home_hermes_portrait").mascotTap(motionEnabled, onTap), active = active, widePose = true, onFinished = onFinished)
        // A real layout gap remains even at the largest frame: decoration never covers an action.
        Spacer(Modifier.height(16.dp))
        DailyAction(state, onDaily, Modifier.fillMaxWidth().heightIn(min = 52.dp))
    }
}

@Composable private fun QuietHomeHero(greeting: String, user: String, caption: String, state: AppUiState, active: Boolean, onDaily: () -> Unit, motion: MascotMotion, onFinished: () -> Unit, onTap: () -> Unit, motionEnabled: Boolean) {
    Column(Modifier.fillMaxWidth().testTag("home_hero_quiet").padding(top = 8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).padding(end = 12.dp)) {
                Text(uiText(R.string.ui_0045, "日常助理"), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(uiText(R.string.home_greeting, "%1\$s，%2\$s", greeting, user), Modifier.padding(top = 10.dp), fontSize = 26.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
            // Reserve arm room for both gestures without resizing the portrait on a tap.
            HermesMascot(motion, Modifier.width(156.dp).height(132.dp).testTag("home_hermes_portrait").mascotTap(motionEnabled, onTap), active = active, waistUp = true, onFinished = onFinished)
        }
        HorizontalDivider(thickness = .5.dp)
        Text(caption, Modifier.padding(top = 16.dp, bottom = 12.dp).testTag("home_caption"),
            style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        DailyAction(state, onDaily)
    }
}

private data class HomeEntry(val title: String, val detail: String, val time: String, val unread: Boolean, val open: () -> Unit)

@Composable private fun RecentCard(entry: HomeEntry, modifier: Modifier, compact: Boolean) {
    val shape = RoundedCornerShape(22.dp)
    Column(modifier.shadow(8.dp, shape, ambientColor = Color.Black.copy(alpha = .025f), spotColor = Color.Black.copy(alpha = .035f))
        .clip(shape).background(MaterialTheme.colorScheme.surface).clickable(onClick = entry.open).heightIn(min = 144.dp).padding(14.dp)
        .testTag("home_recent_card"), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            AssistantIconWell("file", AssistantAccent, Modifier.size(32.dp))
            Spacer(Modifier.weight(1f))
            Text(entry.time, Modifier.padding(start = 4.dp).weight(1.5f), style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.End, maxLines = 2)
        }
        Text(entry.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 2, minLines = 2, overflow = TextOverflow.Ellipsis)
        Text(entry.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = if (compact) 1 else 2, overflow = TextOverflow.Ellipsis)
        if (entry.unread) Text(uiText(R.string.ui_0627, "新回复"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable private fun RecentRow(entry: HomeEntry, skin: SkinMode, compact: Boolean) {
    Row(Modifier.fillMaxWidth().clickable(onClick = entry.open).padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        if (skin != SkinMode.PAPER) { AssistantIconWell("file", AssistantAccent, Modifier.size(38.dp)); Spacer(Modifier.width(12.dp)) }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            if (skin == SkinMode.GLASS) Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(entry.title, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (entry.time.isNotBlank()) Text(entry.time, Modifier.padding(start = 8.dp).widthIn(max = 74.dp),
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            } else {
                if (entry.time.isNotBlank()) Text(entry.time, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(entry.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Text(entry.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = if (skin == SkinMode.GLASS || compact) 1 else 2, overflow = TextOverflow.Ellipsis)
        }
        if (entry.unread) Box(Modifier.padding(start = 8.dp).size(5.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
        AssistantGlyph("chevron", Modifier.padding(start = 10.dp).size(16.dp))
    }
}

@Composable internal fun HomeCardLabel(text: String,icon: String,color: Color,modifier: Modifier=Modifier) {
    Row(modifier,verticalAlignment=Alignment.CenterVertically) {AssistantIconWell(icon,color,Modifier.size(32.dp));Text(text,Modifier.padding(start=11.dp),fontSize=14.sp,color=MaterialTheme.colorScheme.onSurface)}
}
@Composable internal fun HomeChoice(text: String,icon: String,color: Color,modifier: Modifier=Modifier,onClick:()->Unit) {
    Surface(modifier.clickable(onClick=onClick),shape=RoundedCornerShape(30.dp),color=color.copy(alpha=.09f)) {
        Row(Modifier.heightIn(min=46.dp).padding(horizontal=13.dp,vertical=10.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(9.dp)) {
            AssistantGlyph(icon,Modifier.size(20.dp),color);Text(text,Modifier.weight(1f),fontSize=14.sp,lineHeight=20.sp,maxLines=2,overflow=TextOverflow.Ellipsis)
        }
    }
}

@Composable private fun Modifier.mascotTap(enabled: Boolean, onTap: () -> Unit): Modifier =
    if (!enabled) this else this.semantics { contentDescription = uiText(R.string.mascot_tap, "和我互动") }
        .clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
            indication = null, role = Role.Button, onClick = onTap)

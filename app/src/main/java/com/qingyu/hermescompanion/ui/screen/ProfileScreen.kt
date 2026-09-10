package com.qingyu.hermescompanion.ui.screen
import com.qingyu.hermescompanion.ui.component.HermesRadioButton as RadioButton


import com.qingyu.hermescompanion.i18n.uiText
import com.qingyu.hermescompanion.R


import com.qingyu.hermescompanion.ui.component.hermesWell
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.ui.semantics.Role

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.material3.ExperimentalMaterial3Api
import com.qingyu.hermescompanion.ui.component.HermesButton as Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import com.qingyu.hermescompanion.ui.component.HermesModalBottomSheet as ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Slider
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.qingyu.hermescompanion.ui.AppUiState
import com.qingyu.hermescompanion.ui.ThemeMode
import com.qingyu.hermescompanion.ui.SkinMode
import com.qingyu.hermescompanion.model.UserProfilePreferences
import com.qingyu.hermescompanion.storage.AvatarCropSpec
import com.qingyu.hermescompanion.storage.AvatarTarget
import com.qingyu.hermescompanion.ui.component.GlassPanel
import com.qingyu.hermescompanion.ui.component.HermesIconKind
import com.qingyu.hermescompanion.ui.component.HermesMulticolorIcon
import com.qingyu.hermescompanion.ui.component.HermesWelcomeAnimation
import com.qingyu.hermescompanion.ui.component.UserAvatar
import com.qingyu.hermescompanion.ui.component.UserPhoto
import com.qingyu.hermescompanion.ui.theme.HermesSpacing
import com.qingyu.hermescompanion.ui.theme.HermesColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val LocalProfileDockInset = androidx.compose.runtime.staticCompositionLocalOf { 0.dp }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    state: AppUiState,
    contentPadding: PaddingValues,
    showSettings: Boolean,
    onOpenSettings: () -> Unit,
    onBackToProfile: () -> Unit,
    onThemeChange: (ThemeMode) -> Unit,
    onSkinChange: (SkinMode) -> Unit,
    onConnectionSettings: () -> Unit,
    onNotificationSettings: () -> Unit,
    onVoiceSettings: () -> Unit,
    onSkillsTools: () -> Unit,
    onModelSettings: () -> Unit,
    onConversationStyle: () -> Unit,
    onApprovalSettings: () -> Unit,
    onMemoryContext: () -> Unit,
    onOpenMemoryFile: () -> Unit,
    onOpenSoulFile: () -> Unit,
    onArchivedSessions: () -> Unit,
    onProfileSettings: () -> Unit,
    onUpdateUserAvatar: (Uri, AvatarCropSpec) -> Unit,
    onAbout: () -> Unit,
    onChangeLog: () -> Unit,
    onReduceMotionChange: (Boolean) -> Unit = {},
    onLanguageChange: (com.qingyu.hermescompanion.i18n.AppLanguageMode) -> Unit = {},
    onReplayIntro: () -> Unit = {},
    onLauncherIconChange: (com.qingyu.hermescompanion.appearance.LauncherIcon) -> Unit = {},
) {
    var showLanguagePicker by remember { mutableStateOf(false) }
    var showThemePicker by remember { mutableStateOf(false) }
    var pane by remember(showSettings) {
        mutableStateOf(if (showSettings) ProfilePane.SETTINGS else ProfilePane.HOME)
    }
    var showProfilePhoto by remember { mutableStateOf(false) }
    var profilePhotoExpanded by remember { mutableStateOf(false) }
    var avatarCropRequest by remember { mutableStateOf<PendingAvatarCrop?>(null) }
    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) avatarCropRequest = PendingAvatarCrop(uri, AvatarTarget.USER)
    }
    val openPhotoPicker = { avatarPicker.launch(arrayOf("image/*")) }
    val animationScope = rememberCoroutineScope()
    val closeProfilePhoto: () -> Unit = {
        profilePhotoExpanded = false
        animationScope.launch {
            delay(340)
            showProfilePhoto = false
        }
        Unit
    }

    BackHandler(enabled = showProfilePhoto || pane != ProfilePane.HOME) {
        if (showProfilePhoto) closeProfilePhoto()
        else if (pane == ProfilePane.GUIDE) pane = if (showSettings) ProfilePane.SETTINGS else ProfilePane.HOME
        else onBackToProfile()
    }

    androidx.compose.runtime.CompositionLocalProvider(LocalProfileDockInset provides contentPadding.calculateBottomPadding()) {
    Box(modifier = Modifier.fillMaxSize().padding(top = contentPadding.calculateTopPadding())) {
        AnimatedContent(
            targetState = pane,
            modifier = Modifier.fillMaxSize(),
            transitionSpec = {
                if (targetState == ProfilePane.HOME) {
                    (fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) + slideInHorizontally { -it / 5 })
                        .togetherWith(fadeOut(spring(stiffness = Spring.StiffnessMediumLow)) + slideOutHorizontally { it / 5 })
                } else {
                    (fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) + slideInHorizontally { it / 5 })
                        .togetherWith(fadeOut(spring(stiffness = Spring.StiffnessMediumLow)) + slideOutHorizontally { -it / 5 })
                }
            },
            label = "profile-pane",
        ) { target ->
            when (target) {
                ProfilePane.HOME -> ProfileHomeContent(
                    state = state,
                    onAvatarClick = {
                        showProfilePhoto = true
                        animationScope.launch {
                            delay(24)
                            profilePhotoExpanded = true
                        }
                    },
                    onSetPhoto = openPhotoPicker,
                    onEdit = onProfileSettings,
                    onSettings = onOpenSettings,
                    onConnectionSettings = onConnectionSettings,
                    onMemory = onOpenMemoryFile,
                    onSoul = onOpenSoulFile,
                    onGuide = { pane = ProfilePane.GUIDE },
                )
                ProfilePane.SETTINGS -> ProfileSettingsListContent(
                    onBack = onBackToProfile,
                    onTheme = { showThemePicker = true },
                    onLanguage = { showLanguagePicker = true },
                    onConnectionSettings = onConnectionSettings,
                    onNotificationSettings = onNotificationSettings,
                    onVoiceSettings = onVoiceSettings,
                    onSkillsTools = onSkillsTools,
                    onModelSettings = onModelSettings,
                    onConversationStyle = onConversationStyle,
                    onApprovalSettings = onApprovalSettings,
                    onMemoryContext = onMemoryContext,
                    onArchivedSessions = onArchivedSessions,
                    onChangeLog = onChangeLog,
                    onAbout = onAbout,
                )
                ProfilePane.GUIDE -> OperationGuideScreen(
                    contentPadding = PaddingValues(bottom = LocalProfileDockInset.current),
                    onReplayIntro = onReplayIntro,
                    onBack = { pane = if (showSettings) ProfilePane.SETTINGS else ProfilePane.HOME },
                )
            }
        }

        AnimatedVisibility(
            visible = showProfilePhoto,
            modifier = Modifier.fillMaxSize(),
            enter = fadeIn(tween(90)),
            exit = fadeOut(tween(100)),
        ) {
            ProfilePhotoContent(
                state = state,
                expanded = profilePhotoExpanded,
                onBack = closeProfilePhoto,
                onSetPhoto = openPhotoPicker,
                onEdit = onProfileSettings,
                onSettings = {
                    profilePhotoExpanded = false
                    animationScope.launch {
                        delay(340)
                        showProfilePhoto = false
                        onOpenSettings()
                    }
                },
            )
        }
    }

    avatarCropRequest?.let { request ->
        AvatarCropSheet(
            request = request,
            onDismiss = { avatarCropRequest = null },
            onConfirm = { uri, crop ->
                avatarCropRequest = null
                onUpdateUserAvatar(uri, crop)
            },
        )
    }

    if (showLanguagePicker) {
        com.qingyu.hermescompanion.ui.component.LanguagePicker(state.languageMode, onLanguageChange) { showLanguagePicker = false }
    }
    if (showThemePicker) {
        ModalBottomSheet(
            onDismissRequest = { showThemePicker = false },
            shape = RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 20.dp,
            ),
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = if (state.skinMode == SkinMode.GLASS) .92f else 1f),
            tonalElevation = 0.dp,
        ) {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, bottom = 28.dp)) {
                Text(uiText(R.string.ui_0843, "外观"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text(
                    uiText(R.string.ui_0844, "选择界面材质与颜色模式"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 3.dp, bottom = 12.dp),
                )
                Text(uiText(R.string.ui_0845, "界面皮肤"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 7.dp))
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    SkinMode.entries.forEach { skin ->
                        val (name, description) = when (skin) {
                            SkinMode.GLASS -> uiText(R.string.ui_0846, "液态玻璃") to uiText(R.string.ui_0847, "清晰内容 · 透光导航与弹层")
                            SkinMode.CLEAN -> uiText(R.string.ui_0848, "温暖灵动") to uiText(R.string.ui_0849, "柔和卡片 · 鼠尾草绿")
                            SkinMode.PAPER -> uiText(R.string.ui_0850, "安静耐看") to uiText(R.string.ui_0851, "留白与细线 · 专注阅读")
                        }
                        Surface(
                            onClick = { onSkinChange(skin) },
                            shape = MaterialTheme.shapes.large,
                            color = if (state.skinMode == skin) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
                            border = BorderStroke(1.dp, if (state.skinMode == skin) MaterialTheme.colorScheme.primary.copy(alpha = .45f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = .45f)),
                        ) {
                            Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                com.qingyu.hermescompanion.ui.component.SkinPreview(skin)
                                Column(Modifier.weight(1f).padding(start = 14.dp)) {
                                    Text(name, fontWeight = FontWeight.SemiBold)
                                    Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (state.skinMode == skin) HermesMulticolorIcon(HermesIconKind.CHECK, uiText(R.string.ui_0852, "已选择"), iconSize = 20.dp)
                            }
                        }
                    }
                }
                Text(uiText(R.string.ui_0853, "颜色模式"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 18.dp, bottom = 7.dp))
                Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ThemeMode.entries.forEach { mode ->
                        ThemeChoice(
                            mode = mode,
                            selected = state.themeMode == mode,
                            onClick = { onThemeChange(mode) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                com.qingyu.hermescompanion.ui.component.LauncherIconChoices(state.launcherIcon, state.isIconChanging, onLauncherIconChange)
                Spacer(Modifier.height(20.dp))
                com.qingyu.hermescompanion.ui.component.SettingsToggle(
                    uiText(R.string.ui_0854, "减少动态效果"), uiText(R.string.ui_0855, "使用静态人物，减少页面过渡"), state.reduceMotion,
                    onCheckedChange = onReduceMotionChange, horizontalPadding = 0,
                )
            }
        }
    }

}

}

private enum class ProfilePane { HOME, SETTINGS, GUIDE }

@Composable
private fun ProfileHomeContent(
    state: AppUiState,
    onAvatarClick: () -> Unit,
    onSetPhoto: () -> Unit,
    onEdit: () -> Unit,
    onSettings: () -> Unit,
    onConnectionSettings: () -> Unit,
    onMemory: () -> Unit,
    onSoul: () -> Unit,
    onGuide: () -> Unit,
) {
    val displayName = state.userProfile.displayName.ifBlank { state.username.ifBlank { uiText(R.string.ui_0856, "Hermes 用户") } }
    val bio = state.userProfile.bio.ifBlank { uiText(R.string.ui_0150, "个人工作助理") }
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = LocalProfileDockInset.current).statusBarsPadding()
            .padding(horizontal = HermesSpacing.page),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(16.dp))
        Box(contentAlignment = Alignment.Center, modifier = Modifier.clickable(onClick = onAvatarClick)) {
            Surface(shape = CircleShape, shadowElevation = 8.dp, border = androidx.compose.foundation.BorderStroke(2.dp, Color.White)) {
                UserAvatar(state.userProfile.avatarUri, displayName, 122.dp, shape = CircleShape)
            }
            if (state.isAvatarUpdating) CircularProgressIndicator(Modifier.size(34.dp), strokeWidth = 2.5.dp)
        }
        Text(displayName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 13.dp))
        Text(
            bio,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ProfileActionButton(uiText(R.string.ui_0857, "设置照片"), HermesIconKind.CAMERA_ADD, onSetPhoto, Modifier.weight(1f))
            ProfileActionButton(uiText(R.string.ui_0858, "编辑信息"), HermesIconKind.EDIT, onEdit, Modifier.weight(1f))
            ProfileActionButton(uiText(R.string.ui_0859, "设置"), HermesIconKind.SETTINGS, onSettings, Modifier.weight(1f))
        }

        GlassPanel(
            modifier = Modifier.fillMaxWidth().padding(top = 20.dp).clickable(onClick = onConnectionSettings),
            shape = RoundedCornerShape(20.dp),
        ) {
            ProfileInfoRow(
                icon = HermesIconKind.CONNECTION,
                title = uiText(R.string.ui_0761, "远程网关"),
                value = uiText(R.string.ui_0860, "连接正常 · %1\$s", maskAddress(state.baseUrl)),
                showChevron = true,
            )
        }
        GlassPanel(modifier = Modifier.fillMaxWidth().padding(top = 10.dp), shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().clickable(onClick = onMemory)) {
                    ProfileInfoRow(
                        HermesIconKind.MEMORY,
                        uiText(R.string.ui_0861, "我的记忆"),
                        uiText(R.string.ui_0862, "长期事实、偏好与经验 · %1\$s/MEMORY.md", state.activeProfile),
                        showChevron = true,
                    )
                }
                HorizontalDivider(
                    modifier = Modifier.padding(start = 56.dp),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                )
                Row(Modifier.fillMaxWidth().clickable(onClick = onSoul)) {
                    ProfileInfoRow(
                        HermesIconKind.SOUL,
                        uiText(R.string.ui_0863, "我的心智"),
                        uiText(R.string.ui_0864, "人格、原则与行为边界 · %1\$s/SOUL.md", state.activeProfile),
                        showChevron = true,
                    )
                }
            }
        }
        GlassPanel(
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp).clickable(onClick = onGuide),
            shape = RoundedCornerShape(20.dp),
        ) {
            ProfileInfoRow(
                HermesIconKind.GUIDE,
                uiText(R.string.ui_0865, "使用说明"),
                uiText(R.string.ui_0866, "连接、对话、专家会审、任务、语音与故障排查"),
                showChevron = true,
            )
        }
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun ProfileSettingsListContent(
    onBack: () -> Unit,
    onTheme: () -> Unit,
    onLanguage: () -> Unit,
    onConnectionSettings: () -> Unit,
    onNotificationSettings: () -> Unit,
    onVoiceSettings: () -> Unit,
    onSkillsTools: () -> Unit,
    onModelSettings: () -> Unit,
    onConversationStyle: () -> Unit,
    onApprovalSettings: () -> Unit,
    onMemoryContext: () -> Unit,
    onArchivedSessions: () -> Unit,
    onChangeLog: () -> Unit,
    onAbout: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        ProfilePageHeader(uiText(R.string.ui_0859, "设置"), onBack)
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = LocalProfileDockInset.current)
                .padding(horizontal = HermesSpacing.page),
        ) {
            com.qingyu.hermescompanion.ui.component.SettingsBlock(uiText(R.string.ui_0867, "应用偏好")) {
                Column(Modifier.fillMaxWidth()) {
                    SettingRow(HermesIconKind.CONNECTION, uiText(R.string.ui_0761, "远程网关"), onConnectionSettings)
                    SettingRow(HermesIconKind.APPEARANCE, uiText(R.string.ui_0843, "外观"), onTheme)
                    SettingRow(HermesIconKind.CONVERSATION_STYLE, uiText(R.string.language_title, "语言"), onLanguage)
                    SettingRow(HermesIconKind.NOTIFICATION, uiText(R.string.ui_0868, "通知设置"), onNotificationSettings)
                    SettingRow(HermesIconKind.MICROPHONE, uiText(R.string.ui_0869, "语音设置"), onVoiceSettings)
                }
            }
            com.qingyu.hermescompanion.ui.component.SettingsBlock(uiText(R.string.ui_0870, "Hermes 助理")) {
                Column(Modifier.fillMaxWidth()) {
                    SettingRow(HermesIconKind.SKILLS, uiText(R.string.ui_0493, "技能与工具"), onSkillsTools)
                    SettingRow(HermesIconKind.MODEL, uiText(R.string.ui_0498, "模型设置"), onModelSettings)
                    SettingRow(HermesIconKind.CONVERSATION_STYLE, uiText(R.string.ui_0518, "对话风格"), onConversationStyle)
                    SettingRow(HermesIconKind.VERIFIED, uiText(R.string.ui_0527, "审批模式"), onApprovalSettings)
                    SettingRow(HermesIconKind.MEMORY, uiText(R.string.ui_0533, "记忆与上下文"), onMemoryContext)
                    SettingRow(HermesIconKind.ARCHIVE, uiText(R.string.ui_0547, "已归档对话"), onArchivedSessions)
                }
            }
            com.qingyu.hermescompanion.ui.component.SettingsBlock(uiText(R.string.ui_0871, "关于")) {
                Column(Modifier.fillMaxWidth()) {
                    SettingRow(HermesIconKind.CHANGELOG, uiText(R.string.ui_0872, "更新日志"), onChangeLog)
                    SettingRow(HermesIconKind.INFORMATION, uiText(R.string.ui_0873, "关于 Hermes"), onAbout)
                }
            }
            Spacer(Modifier.height(26.dp))
        }
    }
}

@Composable
private fun ProfilePhotoContent(
    state: AppUiState,
    expanded: Boolean,
    onBack: () -> Unit,
    onSetPhoto: () -> Unit,
    onEdit: () -> Unit,
    onSettings: () -> Unit,
) {
    val displayName = state.userProfile.displayName.ifBlank { state.username.ifBlank { uiText(R.string.ui_0856, "Hermes 用户") } }
    val bio = state.userProfile.bio.ifBlank { uiText(R.string.ui_0150, "个人工作助理") }
    val photoHeight by animateDpAsState(
        targetValue = if (expanded) 470.dp else 122.dp,
        animationSpec = tween(360, easing = FastOutSlowInEasing),
        label = "profile-photo-height",
    )
    val corner by animateDpAsState(
        targetValue = if (expanded) 0.dp else 61.dp,
        animationSpec = tween(360, easing = FastOutSlowInEasing),
        label = "profile-photo-corner",
    )
    val topInset by animateDpAsState(
        targetValue = if (expanded) 0.dp else 16.dp,
        animationSpec = tween(360, easing = FastOutSlowInEasing),
        label = "profile-photo-top",
    )
    val detailsAlpha by animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = tween(if (expanded) 280 else 160, delayMillis = if (expanded) 90 else 0),
        label = "profile-photo-details",
    )
    val swipeDismissThreshold = with(LocalDensity.current) { 72.dp.toPx() }
    var upwardDragDistance by remember(expanded) { mutableStateOf(0f) }
    BoxWithConstraints(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        val collapsedInset = ((maxWidth - 122.dp) / 2).coerceAtLeast(0.dp)
        val horizontalInset by animateDpAsState(
            targetValue = if (expanded) 0.dp else collapsedInset,
            animationSpec = tween(360, easing = FastOutSlowInEasing),
            label = "profile-photo-horizontal",
        )
        Column(Modifier.fillMaxSize().padding(top = topInset)) {
            Box(
                Modifier.fillMaxWidth().padding(horizontal = horizontalInset).height(photoHeight)
                    .clip(RoundedCornerShape(corner))
                    .pointerInput(expanded) {
                        if (expanded) {
                            detectVerticalDragGestures(
                                onDragStart = { upwardDragDistance = 0f },
                                onDragCancel = { upwardDragDistance = 0f },
                                onDragEnd = {
                                    if (upwardDragDistance >= swipeDismissThreshold) onBack()
                                    upwardDragDistance = 0f
                                },
                                onVerticalDrag = { _, dragAmount ->
                                    upwardDragDistance = (upwardDragDistance - dragAmount).coerceAtLeast(0f)
                                },
                            )
                        }
                    },
            ) {
                UserPhoto(
                    uri = state.userProfile.avatarUri,
                    displayName = displayName,
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(corner),
                )
                Box(
                    Modifier.fillMaxSize().alpha(detailsAlpha).background(
                        Brush.verticalGradient(
                            0f to Color.Black.copy(alpha = 0.12f),
                            0.42f to Color.Transparent,
                            0.68f to Color.Black.copy(alpha = 0.34f),
                            1f to Color.Black.copy(alpha = 0.76f),
                        ),
                    ),
                )
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.alpha(detailsAlpha).statusBarsPadding().padding(6.dp).align(Alignment.TopStart),
                ) {
                    HermesMulticolorIcon(HermesIconKind.BACK, contentDescription = uiText(R.string.ui_0554, "返回"), tint = Color.White, iconSize = 25.dp)
                }
                Column(Modifier.alpha(detailsAlpha).align(Alignment.BottomStart).padding(horizontal = 16.dp, vertical = 18.dp)) {
                    Text(displayName, style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.SemiBold)
                    Text(bio, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.82f), modifier = Modifier.padding(top = 3.dp))
                    FrostedProfileActions(
                        onSetPhoto = onSetPhoto,
                        onEdit = onEdit,
                        onSettings = onSettings,
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    )
                }
            }
            GlassPanel(
                modifier = Modifier.alpha(detailsAlpha).fillMaxWidth().padding(horizontal = HermesSpacing.page, vertical = 14.dp),
                shape = RoundedCornerShape(20.dp),
            ) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 17.dp, vertical = 15.dp)) {
                    Text(state.username.ifBlank { uiText(R.string.ui_0922, "未显示网关账号") }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                    Text(uiText(R.string.ui_0923, "网关账号"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 3.dp))
                    HorizontalDivider(Modifier.padding(vertical = 13.dp), thickness = 0.5.dp)
                    Text(maskAddress(state.baseUrl), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                    Text(uiText(R.string.ui_0924, "远程网关 · 连接正常"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 3.dp))
                }
            }
        }
    }
}

@Composable
private fun ProfileActionButton(
    label: String,
    icon: HermesIconKind,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    dark: Boolean = false,
) {
    Surface(
        modifier = modifier.height(72.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = if (dark) Color.White.copy(alpha = 0.10f) else MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
    ) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            HermesMulticolorIcon(
                icon,
                contentDescription = null,
                iconSize = 20.dp,
                tint = if (dark) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(label, style = MaterialTheme.typography.labelSmall, color = if (dark) Color.White else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 6.dp))
        }
    }
}

@Composable
private fun ProfileInfoRow(
    icon: HermesIconKind,
    title: String,
    value: String,
    showChevron: Boolean = false,
) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
        IconWell(icon)
        Column(Modifier.weight(1f).padding(start = 11.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
            Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
        }
        if (showChevron) HermesMulticolorIcon(HermesIconKind.CHEVRON_RIGHT, contentDescription = null, tint = MaterialTheme.colorScheme.outline, iconSize = 15.dp)
    }
}

@Composable
private fun ProfilePageHeader(title: String, onBack: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 7.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) { HermesMulticolorIcon(HermesIconKind.BACK, contentDescription = uiText(R.string.ui_0554, "返回"), iconSize = 24.dp) }
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 4.dp))
    }
}

@Composable
private fun GuideRow(body: String, divider: Boolean = true) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 12.dp)) {
        Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    if (divider) HorizontalDivider(Modifier.padding(start = 15.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
}

@Composable
private fun GuideSection(title: String, rows: List<String>) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 8.dp, bottom = 7.dp, start = 3.dp),
    )
    GlassPanel(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.fillMaxWidth()) {
            rows.forEachIndexed { index, row ->
                GuideRow(row, divider = index < rows.lastIndex)
            }
        }
    }
}

@Composable
private fun FrostedProfileActions(
    onSetPhoto: () -> Unit,
    onEdit: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier.height(72.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ProfileActionButton(uiText(R.string.ui_0857, "设置照片"), HermesIconKind.CAMERA_ADD, onSetPhoto, Modifier.weight(1f), dark = true)
        ProfileActionButton(uiText(R.string.ui_0858, "编辑信息"), HermesIconKind.EDIT, onEdit, Modifier.weight(1f), dark = true)
        ProfileActionButton(uiText(R.string.ui_0859, "设置"), HermesIconKind.SETTINGS, onSettings, Modifier.weight(1f), dark = true)
    }
}

@Composable
fun ProfileSettingsScreen(
    state: AppUiState,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onSave: (UserProfilePreferences) -> Unit,
    onUpdateHermesAvatar: (Uri, AvatarCropSpec) -> Unit,
    onResetHermesAvatar: () -> Unit,
    onUpdateUserAvatar: (Uri, AvatarCropSpec) -> Unit = { _, _ -> },
    onResetUserAvatar: () -> Unit = {},
) {
    IdentityEditorScreen(state, contentPadding, firstRun = false, onBack = onBack,
        onSave = { onSave(it); onBack() }, onUpdateHermesAvatar = onUpdateHermesAvatar,
        onResetHermesAvatar = onResetHermesAvatar, onUpdateUserAvatar = onUpdateUserAvatar,
        onResetUserAvatar = onResetUserAvatar)
}

internal data class PendingAvatarCrop(val uri: Uri, val target: AvatarTarget)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AvatarCropSheet(
    request: PendingAvatarCrop,
    onDismiss: () -> Unit,
    onConfirm: (Uri, AvatarCropSpec) -> Unit,
) {
    val context = LocalContext.current
    var zoom by remember(request.uri) { mutableStateOf(1f) }
    var horizontal by remember(request.uri) { mutableStateOf(0f) }
    var vertical by remember(request.uri) { mutableStateOf(0f) }
    val bitmap by produceState<Bitmap?>(initialValue = null, request.uri) {
        value = withContext(Dispatchers.IO) { loadAvatarCropPreview(context, request.uri) }
    }
    ModalBottomSheet(onDismissRequest = onDismiss, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)) {
        Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                if (request.target == AvatarTarget.USER) uiText(R.string.ui_0937, "裁剪我的头像") else uiText(R.string.ui_0938, "裁剪 Hermes 头像"),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start),
            )
            Text(
                uiText(R.string.ui_0939, "调整缩放和位置，方框内就是最终头像"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Start).padding(top = 3.dp, bottom = 14.dp),
            )
            Box(
                modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center,
            ) {
                if (bitmap == null) {
                    CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 2.4.dp)
                } else {
                    Image(
                        bitmap = bitmap!!.asImageBitmap(),
                        contentDescription = uiText(R.string.ui_0940, "头像裁剪预览"),
                        contentScale = ContentScale.Crop,
                        alignment = BiasAlignment(horizontal, vertical),
                        modifier = Modifier.fillMaxSize().graphicsLayer {
                            scaleX = zoom
                            scaleY = zoom
                            translationX = avatarPreviewTranslation(horizontal, zoom, size.width)
                            translationY = avatarPreviewTranslation(vertical, zoom, size.height)
                        },
                    )
                    Box(Modifier.fillMaxSize().border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.82f), RoundedCornerShape(24.dp)))
                }
            }
            CropSlider(uiText(R.string.ui_0941, "缩放"), zoom, 1f..3f) { zoom = it }
            CropSlider(uiText(R.string.ui_0942, "左右"), horizontal, -1f..1f) { horizontal = it }
            CropSlider(uiText(R.string.ui_0943, "上下"), vertical, -1f..1f) { vertical = it }
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TextButton(colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer), onClick = onDismiss, modifier = Modifier.weight(1f)) { Text(uiText(R.string.ui_0553, "取消")) }
                Button(
                    onClick = { onConfirm(request.uri, AvatarCropSpec(zoom, horizontal, vertical)) },
                    enabled = bitmap != null,
                    modifier = Modifier.weight(1f),
                ) { Text(uiText(R.string.ui_0944, "使用头像")) }
            }
        }
    }
}

@Composable
private fun CropSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(0.18f))
        Slider(value = value, onValueChange = onChange, valueRange = range, modifier = Modifier.weight(0.82f))
    }
}

internal fun avatarPreviewTranslation(bias: Float, zoom: Float, axisSize: Float): Float {
    val travel = (zoom.coerceIn(1f, 4f) - 1f) * axisSize.coerceAtLeast(0f) / 2f
    return if (travel == 0f) 0f else -bias.coerceIn(-1f, 1f) * travel
}

private fun loadAvatarCropPreview(context: Context, uri: Uri): Bitmap? = runCatching {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri).use { input -> BitmapFactory.decodeStream(input, null, bounds) }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null
    var sample = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 1600) sample *= 2
    context.contentResolver.openInputStream(uri).use { input ->
        BitmapFactory.decodeStream(input, null, BitmapFactory.Options().apply { inSampleSize = sample })
    }
}.getOrNull()

@Composable
private fun IconWell(icon: HermesIconKind) {
    Box(Modifier.size(36.dp).hermesWell(), contentAlignment = Alignment.Center) {
        HermesMulticolorIcon(icon, null, iconSize = 20.dp, tint = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun ThemeChoice(mode: ThemeMode, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val icon = when (mode) {
        ThemeMode.SYSTEM -> HermesIconKind.SYSTEM_MODE
        ThemeMode.LIGHT -> HermesIconKind.LIGHT_MODE
        ThemeMode.DARK -> HermesIconKind.DARK_MODE
    }
    val label = when (mode) {
        ThemeMode.SYSTEM -> uiText(R.string.ui_0945, "跟随系统")
        ThemeMode.LIGHT -> uiText(R.string.ui_0946, "浅色")
        ThemeMode.DARK -> uiText(R.string.ui_0947, "深色")
    }
    Row(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HermesMulticolorIcon(icon, contentDescription = null, iconSize = 22.dp)
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            modifier = Modifier.weight(1f).padding(start = 12.dp),
            maxLines = 1,
        )
        RadioButton(selected = selected, onClick = null)
    }
}

@Composable
private fun SettingRow(icon: HermesIconKind, title: String, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 11.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconWell(icon)
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f).padding(start = 11.dp),
            )
            HermesMulticolorIcon(
                HermesIconKind.CHEVRON_RIGHT,
                contentDescription = null,
                iconSize = 15.dp,
                tint = MaterialTheme.colorScheme.outline,
            )
        }
        HorizontalDivider(
            modifier = Modifier.padding(start = 53.dp, end = 11.dp),
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.46f),
        )
    }
}

private fun maskAddress(url: String): String {
    if (url.isBlank()) return uiText(R.string.ui_0948, "尚未配置")
    return url.replace(Regex(":\\d+(?=/|$)"), ":••••")
}

package com.qingyu.hermescompanion.ui.screen

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    state: AppUiState,
    contentPadding: PaddingValues,
    showSettings: Boolean,
    onOpenSettings: () -> Unit,
    onBackToProfile: () -> Unit,
    onThemeChange: (ThemeMode) -> Unit,
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
) {
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

    Box(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
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
                ProfilePane.GUIDE -> ProfileGuideContent(
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

    if (showThemePicker) {
        ModalBottomSheet(
            onDismissRequest = { showThemePicker = false },
            shape = RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 20.dp,
            ),
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
        ) {
            Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 28.dp)) {
                Text("外观", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text(
                    "选择浅色、深色或跟随系统",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 3.dp, bottom = 12.dp),
                )
                Text("颜色模式", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 7.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ThemeMode.entries.forEach { mode ->
                        ThemeChoice(
                            mode = mode,
                            selected = state.themeMode == mode,
                            onClick = { onThemeChange(mode) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
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
    val displayName = state.userProfile.displayName.ifBlank { state.username.ifBlank { "Hermes 用户" } }
    val bio = state.userProfile.bio.ifBlank { "个人工作助理" }
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding()
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
            ProfileActionButton("设置照片", HermesIconKind.CAMERA_ADD, onSetPhoto, Modifier.weight(1f))
            ProfileActionButton("编辑信息", HermesIconKind.EDIT, onEdit, Modifier.weight(1f))
            ProfileActionButton("设置", HermesIconKind.SETTINGS, onSettings, Modifier.weight(1f))
        }

        GlassPanel(
            modifier = Modifier.fillMaxWidth().padding(top = 20.dp).clickable(onClick = onConnectionSettings),
            shape = RoundedCornerShape(20.dp),
        ) {
            ProfileInfoRow(
                icon = HermesIconKind.CONNECTION,
                title = "远程网关",
                value = "连接正常 · ${maskAddress(state.baseUrl)}",
                showChevron = true,
            )
        }
        GlassPanel(modifier = Modifier.fillMaxWidth().padding(top = 10.dp), shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().clickable(onClick = onMemory)) {
                    ProfileInfoRow(
                        HermesIconKind.MEMORY,
                        "我的记忆",
                        "长期事实、偏好与经验 · ${state.activeProfile}/MEMORY.md",
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
                        "我的心智",
                        "人格、原则与行为边界 · ${state.activeProfile}/SOUL.md",
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
                "使用说明",
                "连接、对话、专家会审、任务、语音与故障排查",
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
        ProfilePageHeader("设置", onBack)
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = HermesSpacing.page),
        ) {
            GlassPanel(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.fillMaxWidth()) {
                    SettingRow(HermesIconKind.CONNECTION, "远程网关", onConnectionSettings)
                    SettingRow(HermesIconKind.APPEARANCE, "外观", onTheme)
                    SettingRow(HermesIconKind.NOTIFICATION, "通知设置", onNotificationSettings)
                    SettingRow(HermesIconKind.MICROPHONE, "语音设置", onVoiceSettings)
                }
            }
            GlassPanel(Modifier.fillMaxWidth().padding(top = 10.dp), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.fillMaxWidth()) {
                    SettingRow(HermesIconKind.SKILLS, "技能与工具", onSkillsTools)
                    SettingRow(HermesIconKind.MODEL, "模型设置", onModelSettings)
                    SettingRow(HermesIconKind.CONVERSATION_STYLE, "对话风格", onConversationStyle)
                    SettingRow(HermesIconKind.VERIFIED, "审批模式", onApprovalSettings)
                    SettingRow(HermesIconKind.MEMORY, "记忆与上下文", onMemoryContext)
                    SettingRow(HermesIconKind.ARCHIVE, "已归档对话", onArchivedSessions)
                }
            }
            GlassPanel(Modifier.fillMaxWidth().padding(top = 10.dp), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.fillMaxWidth()) {
                    SettingRow(HermesIconKind.CHANGELOG, "更新日志", onChangeLog)
                    SettingRow(HermesIconKind.INFORMATION, "关于 Hermes", onAbout)
                }
            }
            Spacer(Modifier.height(26.dp))
        }
    }
}

@Composable
private fun ProfileGuideContent(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        ProfilePageHeader("使用说明", onBack)
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = HermesSpacing.page),
        ) {
            HermesWelcomeAnimation(
                modifier = Modifier.size(154.dp).align(Alignment.CenterHorizontally),
                contentDescription = "Hermes 使用说明引导动画",
            )
            Text("Hermes 移动端", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Text(
                "从连接远程网关到对话、会审、文件、自动任务和连续语音，这里按实际使用顺序说明。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
            )
            GuideSection("内容导航", listOf(
                "远程连接与 Profile",
                "对话、运行控制与消息状态",
                "专家会审与助理面板",
                "文件、空间与内容查找",
                "执行中心、定时任务与通知",
                "单次语音、连续语音与模型",
                "记忆、心智、安全与故障排查",
            ))
            GuideSection("远程连接与 Profile", listOf(
                "首次连接：在“我的 → 远程网关”填写服务器地址、账号和密码，先点“验证并更新连接”。连接成功后，APP 会读取该账号可用的 Profile、会话和服务器能力。",
                "地址与安全：同一局域网可使用内网地址；跨网络访问优先使用 HTTPS、可信 VPN 或安全反向代理，不建议把未加密 HTTP 网关直接暴露到公网。",
                "Profile 隔离：work、default 等 Profile 的会话、项目、MEMORY.md 与 SOUL.md 相互独立。顶部切换 Profile 后，列表和搜索都会跟随切换。",
                "连接异常：先确认服务器在线、端口放行、账号密码正确，再进入远程网关页运行诊断；更新 Agent 时不要同时发起新的任务。",
            ))
            GuideSection("对话与运行控制", listOf(
                "新建与查找：点对话页右下角蓝色按钮创建会话；顶部搜索可匹配标题、摘要和近期消息。项目与时间筛选只影响当前列表，不会删除内容。",
                "发送内容：输入框支持文字、图片、文档、链接、空间文件、单次语音和命令。输入“/”或点命令按钮可查看当前服务器支持的 Hermes 命令。",
                "运行中追加：Hermes 执行时，可以选择立即追加要求，或排队到下一轮；停止按钮会终止当前生成，但已经完成的工具操作不会自动撤销。",
                "状态说明：“正在思考”表示尚未输出正文；“正在处理”表示正在调用工具或执行 Agent 步骤；断线后 APP 会尝试重新连接并取回服务器上的完整结果。",
                "长对话阅读：向上加载更早消息；APP 会保存最近阅读位置。回复完成后可用右侧向下按钮快速回到最新内容。",
            ))
            GuideSection("专家会审与助理面板", listOf(
                "入口：点聊天页右上角助理按钮进入专家会审。会审开关只作用于下一条消息，发送后会自动关闭，避免后续普通问题继续消耗额外 Token。",
                "深度会审：三位隔离上下文的专家并行分析——证据分析员查事实与假设，反方审查员找盲点和失败条件，落地评审员评估成本与执行。",
                "群聊展示：三位专家的返回内容会以不同群成员身份分别显示，随后由 Hermes 对共识、关键分歧、证据风险和最终方案做统一裁决。",
                "快速会审：使用服务器 MoA 参考模型并行分析，速度更快；如果服务器没有配置 MoA 预设，该选项会保持不可用。",
                "适用场景：高影响决策、方案评审和证据冲突适合会审；简单查询直接用普通对话更快，也更节省 Token。",
            ))
            GuideSection("文件、空间与查找", listOf(
                "发送附件：曲别针面板可选择图片、文本、JSON、XML、YAML、网页链接或空间文件。大文件是否可读取取决于网关和模型的限制。",
                "空间：集中查看服务器目录和最近产物。Markdown 支持预览、编辑、保存、导出与分享；图片可以点开查看和缩放。",
                "来源关系：最近产物会保留来源会话，点来源可回到生成该文件的聊天位置；聊天中的网页引用会在回复下方整理成可点击来源。",
                "敏感信息：密码、API Key、Cookie 和私钥不要作为普通聊天附件发送；应保存在服务器环境变量或系统安全存储中。",
            ))
            GuideSection("执行中心、定时任务与通知", listOf(
                "执行中心：待处理页集中展示审批与澄清请求；进行中页显示当前 Agent 和工具状态；执行记录用于回看最近完成结果与产物。",
                "定时任务：点右上角蓝色加号填写名称、执行内容和 Cron 表达式。创建后可暂停、恢复、手动执行、编辑或删除。",
                "审批原则：允许前先核对目标、文件范围、外部收件人和是否会产生不可逆操作；不确定时优先选择“仅本次允许”。",
                "后台执行：离开聊天后，运行状态会以底部悬浮提示显示；点提示可返回对应会话，也可以直接停止当前任务。",
                "通知：建议开启对话完成、审批和定时任务提醒。系统省电或后台限制可能延迟通知，可在安卓系统设置中允许 Hermes 后台运行。",
            ))
            GuideSection("语音与模型", listOf(
                "单次语音：聊天输入框的麦克风用于录一段话并转写到输入框，可在语音设置中选择识别后自动发送或先手动确认。",
                "连续语音：聊天页顶部波形按钮进入连续对话；说完后自动识别、发送并朗读回复，回答过程中点中间按钮可立即打断。",
                "识别服务：可选择 Agent STT/TTS、手机系统或自动兜底。中文建议使用 zh-CN，并按习惯选择简体、繁体或保留原文。",
                "模型设置：可分别指定主模型、视觉、网页提取、上下文压缩、技能、审批和维护模型。日常问题不必全部使用最高推理强度。",
                "语音失败：检查录音权限、STT/TTS 服务、语言和网络；Agent 语音不可用时，可临时切换手机系统识别。",
            ))
            GuideSection("记忆、心智与排查", listOf(
                "“我的记忆”对应当前 Profile 的 MEMORY.md，用于保存长期事实、偏好和经验；“我的心智”对应 SOUL.md，用于定义人格、原则与行为边界。",
                "修改建议：先阅读原内容再编辑，避免一次覆盖过多；重要文件在服务器保留版本或备份，便于出现偏差时恢复。",
                "性能排查：会话多时使用项目与时间筛选；超长聊天按需加载历史。若页面卡顿，先退出大图预览或长文编辑再重试。",
                "连接排查：依次检查服务器进程、网关健康、端口、HTTPS 证书、账号认证和 Agent 版本；不要只反复刷新 APP。",
                "安全建议：高风险工具开启审批；公网连接使用加密通道；安装更新包前核对来源和签名，不要将网关凭证转发给他人。",
            ))
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
    val displayName = state.userProfile.displayName.ifBlank { state.username.ifBlank { "Hermes 用户" } }
    val bio = state.userProfile.bio.ifBlank { "个人工作助理" }
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
                    HermesMulticolorIcon(HermesIconKind.BACK, contentDescription = "返回", tint = Color.White, iconSize = 25.dp)
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
                    Text(state.username.ifBlank { "未显示网关账号" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                    Text("网关账号", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 3.dp))
                    HorizontalDivider(Modifier.padding(vertical = 13.dp), thickness = 0.5.dp)
                    Text(maskAddress(state.baseUrl), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                    Text("远程网关 · 连接正常", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 3.dp))
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
        IconButton(onClick = onBack) { HermesMulticolorIcon(HermesIconKind.BACK, contentDescription = "返回", iconSize = 24.dp) }
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
        ProfileActionButton("设置照片", HermesIconKind.CAMERA_ADD, onSetPhoto, Modifier.weight(1f), dark = true)
        ProfileActionButton("编辑信息", HermesIconKind.EDIT, onEdit, Modifier.weight(1f), dark = true)
        ProfileActionButton("设置", HermesIconKind.SETTINGS, onSettings, Modifier.weight(1f), dark = true)
    }
}

@Composable
fun ProfileSettingsScreen(
    state: AppUiState,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onSave: (UserProfilePreferences) -> Unit,
) {
    var draftName by remember(state.userProfile.displayName, state.username) {
        mutableStateOf(state.userProfile.displayName.ifBlank { state.username })
    }
    var draftBio by remember(state.userProfile.bio) { mutableStateOf(state.userProfile.bio) }
    var draftHermesName by remember(state.userProfile.hermesDisplayName) {
        mutableStateOf(state.userProfile.hermesDisplayName.ifBlank { "Hermes" })
    }

    Column(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
        Row(
            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            androidx.compose.material3.IconButton(onClick = onBack) {
                HermesMulticolorIcon(HermesIconKind.BACK, contentDescription = "返回")
            }
            Column(Modifier.padding(start = 4.dp)) {
                Text("编辑信息", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text("设置你与 Hermes 在会话中的显示信息", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = HermesSpacing.page),
        ) {
        GlassPanel(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 11.dp, vertical = 4.dp)) {
                ProfileInputRow("昵称", draftName, "Hermes 用户") { draftName = it.take(24) }
                ProfileInputRow("个人签名", draftBio, "个人工作助理") { draftBio = it.take(50) }
                Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("网关账号", fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                    Text(state.username.ifBlank { "未登录" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        GlassPanel(Modifier.fillMaxWidth().padding(top = 9.dp)) {
            Column(Modifier.fillMaxWidth().padding(12.dp)) {
                Text("Hermes 聊天资料", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Row(Modifier.fillMaxWidth().padding(top = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                    UserAvatar(
                        uri = "",
                        displayName = draftHermesName.ifBlank { "Hermes" },
                        size = 68.dp,
                        hermesFallback = true,
                    )
                    Column(Modifier.weight(1f).padding(start = 13.dp)) {
                        Text(
                            "固定 Hermes 头像",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            "会同步显示在会话列表和聊天界面",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
                ProfileInputRow("Hermes 昵称", draftHermesName, "Hermes") { draftHermesName = it.take(24) }
            }
        }
        Text(
            "照片请在“我的”主页单独设置；昵称和签名只用于这台手机，不会修改网关账号。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 10.dp),
        )
        Button(
            onClick = {
                onSave(
                    state.userProfile.copy(
                        displayName = draftName.trim(),
                        bio = draftBio.trim(),
                        hermesDisplayName = draftHermesName.trim().ifBlank { "Hermes" },
                    ),
                )
                onBack()
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("保存") }
        Spacer(Modifier.height(26.dp))
        }
    }

}

private data class PendingAvatarCrop(val uri: Uri, val target: AvatarTarget)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AvatarCropSheet(
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
                if (request.target == AvatarTarget.USER) "裁剪我的头像" else "裁剪 Hermes 头像",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start),
            )
            Text(
                "调整缩放和位置，方框内就是最终头像",
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
                        contentDescription = "头像裁剪预览",
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
            CropSlider("缩放", zoom, 1f..3f) { zoom = it }
            CropSlider("左右", horizontal, -1f..1f) { horizontal = it }
            CropSlider("上下", vertical, -1f..1f) { vertical = it }
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("取消") }
                Button(
                    onClick = { onConfirm(request.uri, AvatarCropSpec(zoom, horizontal, vertical)) },
                    enabled = bitmap != null,
                    modifier = Modifier.weight(1f),
                ) { Text("使用头像") }
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
private fun ProfileInputRow(label: String, value: String, placeholder: String, onChange: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontWeight = FontWeight.Medium, modifier = Modifier.weight(0.35f), maxLines = 1)
        Surface(
            modifier = Modifier.weight(0.65f).height(40.dp),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
            tonalElevation = 0.dp,
        ) {
            androidx.compose.foundation.text.BasicTextField(
                value = value,
                onValueChange = onChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 8.dp),
                decorationBox = { inner ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
                        if (value.isBlank()) Text(placeholder, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        inner()
                    }
                },
            )
        }
    }
}

@Composable
private fun IconWell(icon: HermesIconKind) {
    val container = when (icon) {
        HermesIconKind.CONNECTION -> Color(0xFF2AABEE)
        HermesIconKind.APPEARANCE -> Color(0xFF8B5CF6)
        HermesIconKind.NOTIFICATION -> Color(0xFFF04455)
        HermesIconKind.MICROPHONE -> Color(0xFF20B8C9)
        HermesIconKind.SKILLS -> Color(0xFF9B5DE5)
        HermesIconKind.MODEL -> Color(0xFF477EF5)
        HermesIconKind.CONVERSATION_STYLE -> Color(0xFF22A699)
        HermesIconKind.VERIFIED -> Color(0xFF31B95E)
        HermesIconKind.MEMORY -> Color(0xFF477EF5)
        HermesIconKind.SOUL -> Color(0xFFB45AF2)
        HermesIconKind.ARCHIVE -> Color(0xFFF59E0B)
        HermesIconKind.CHANGELOG -> Color(0xFF20A4E8)
        HermesIconKind.INFORMATION -> Color(0xFF2AABEE)
        HermesIconKind.AI -> Color(0xFF8B5CF6)
        HermesIconKind.STORAGE -> Color(0xFF31B95E)
        else -> Color(0xFF6B7280)
    }
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = container,
        tonalElevation = 0.dp,
    ) {
        Box(Modifier.size(34.dp), contentAlignment = Alignment.Center) {
            HermesMulticolorIcon(icon, contentDescription = null, iconSize = 19.dp, tint = Color.White)
        }
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
        ThemeMode.SYSTEM -> "跟随系统"
        ThemeMode.LIGHT -> "浅色"
        ThemeMode.DARK -> "深色"
    }
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow)
            .clickable(onClick = onClick)
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
        HermesMulticolorIcon(
            if (selected) HermesIconKind.RADIO_SELECTED else HermesIconKind.UNCHECKED,
            contentDescription = if (selected) "已选择" else null,
            iconSize = 20.dp,
            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        )
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
    if (url.isBlank()) return "尚未配置"
    return url.replace(Regex(":\\d+(?=/|$)"), ":••••")
}

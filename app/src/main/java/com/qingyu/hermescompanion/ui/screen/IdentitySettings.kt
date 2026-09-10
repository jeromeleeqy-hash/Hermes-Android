package com.qingyu.hermescompanion.ui.screen

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.qingyu.hermescompanion.R
import com.qingyu.hermescompanion.i18n.*
import com.qingyu.hermescompanion.model.UserProfilePreferences
import com.qingyu.hermescompanion.storage.AvatarCropSpec
import com.qingyu.hermescompanion.storage.AvatarTarget
import com.qingyu.hermescompanion.ui.*
import com.qingyu.hermescompanion.ui.component.*
import com.qingyu.hermescompanion.ui.theme.HermesSkin

/** Shared page hierarchy with genuinely different warm / glass / paper treatments. */
@Composable
internal fun SetupHero(title: String, subtitle: String, profile: UserProfilePreferences? = null) {
    val skin = HermesSkin.current
    val emblem: @Composable (Int) -> Unit = { size ->
        if (profile != null) UserAvatar(profile.hermesAvatarUri, profile.hermesDisplayName, size.dp,
            hermesFallback = true, shape = CircleShape)
        else Box(Modifier.size(size.dp).hermesWell(MaterialTheme.shapes.large), contentAlignment = Alignment.Center) {
            HermesMulticolorIcon(HermesIconKind.CONNECTION, null, iconSize = (size * .48f).dp)
        }
    }
    if (skin.glass) Column(Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        emblem(68)
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    } else {
        Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            emblem(if (skin.mode == SkinMode.CLEAN) 76 else 56)
        }
        if (skin.mode == SkinMode.PAPER) HorizontalDivider(Modifier.padding(top = 8.dp), thickness = .5.dp)
    }
}

@Composable
internal fun SetupSection(title: String, modifier: Modifier = Modifier, trailing: (@Composable () -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val skin = HermesSkin.current
    val paper = skin.mode == SkinMode.PAPER
    val shape = MaterialTheme.shapes.large
    Column(modifier.fillMaxWidth()
        .then(if (paper) Modifier else Modifier.background(MaterialTheme.colorScheme.surface, shape))
        .then(if (skin.glass) Modifier.border(.7.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .6f), shape) else Modifier)
        .padding(horizontal = if (paper) 0.dp else 20.dp, vertical = if (paper) 8.dp else 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, modifier = if (trailing != null) Modifier.weight(1f) else Modifier, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            if (paper && trailing == null) HorizontalDivider(Modifier.weight(1f).padding(start = 16.dp), thickness = .5.dp)
            trailing?.invoke()
        }
        content()
    }
}

@Composable
internal fun SetupSkinSelector(mode: SkinMode, onSelect: (SkinMode) -> Unit) {
    HermesSegmentedControl(
        listOf(uiText(R.string.style_warm_short, "温暖"), uiText(R.string.style_glass_short, "玻璃"), uiText(R.string.style_quiet_short, "安静")),
        SkinMode.entries.indexOf(mode), { onSelect(SkinMode.entries[it]) }, compact = true)
}

@Composable
fun IdentityEditorScreen(
    state: AppUiState, contentPadding: PaddingValues = PaddingValues(), firstRun: Boolean,
    onBack: () -> Unit = {}, onSave: (UserProfilePreferences) -> Unit,
    onUpdateHermesAvatar: (Uri, AvatarCropSpec) -> Unit, onResetHermesAvatar: () -> Unit,
    onUpdateUserAvatar: (Uri, AvatarCropSpec) -> Unit = { _, _ -> }, onResetUserAvatar: () -> Unit = {},
    onSkinChange: (SkinMode) -> Unit = {}, onLanguageChange: (AppLanguageMode) -> Unit = {},
) {
    var name by rememberSaveable { mutableStateOf(state.userProfile.hermesDisplayName.ifBlank { "Hermes" }) }
    var userName by rememberSaveable { mutableStateOf(state.userProfile.displayName) }
    var bio by rememberSaveable { mutableStateOf(state.userProfile.bio) }
    var crop by remember { mutableStateOf<PendingAvatarCrop?>(null) }
    var showLanguage by remember { mutableStateOf(false) }
    val hermesPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) crop = PendingAvatarCrop(uri, AvatarTarget.HERMES)
    }
    val userPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) crop = PendingAvatarCrop(uri, AvatarTarget.USER)
    }
    val displayProfile = state.userProfile.copy(hermesDisplayName = name.ifBlank { "Hermes" })
    Column(Modifier.fillMaxSize().padding(contentPadding).statusBarsPadding().navigationBarsPadding().imePadding()
        .testTag(if (firstRun) "identity_onboarding" else "identity_settings")) {
        Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = if (firstRun) 22.dp else 8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            if (!firstRun) IconButton(onClick = onBack) { HermesMulticolorIcon(HermesIconKind.BACK, uiText(R.string.ui_0554, "返回")) }
            Text(if (firstRun) uiText(R.string.setup_step_identity, "1 / 2 · 认识彼此") else uiText(R.string.ui_0858, "编辑信息"),
                Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (firstRun) TextButton(onClick = { showLanguage = true }) { Text("中 / EN") }
        }
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp)
            .padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            SetupHero(
                if (firstRun) uiText(R.string.identity_welcome, "先认识一下") else uiText(R.string.identity_title, "属于你的名字与形象"),
                uiText(R.string.identity_local_hint, "这些资料只用于手机里的显示，不会修改 Agent 或网关账号。"),
                displayProfile)
            if (firstRun) SetupSkinSelector(state.skinMode, onSkinChange)
            SetupSection(uiText(R.string.identity_assistant, "你的助理")) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    UserAvatar(state.userProfile.hermesAvatarUri, displayProfile.hermesDisplayName, 64.dp, hermesFallback = true)
                    Column(Modifier.weight(1f)) {
                        Text(uiText(R.string.identity_avatar, "助理头像"), style = MaterialTheme.typography.bodyMedium)
                        Row {
                            TextButton(onClick = { hermesPicker.launch(arrayOf("image/*")) }, enabled = !state.isAvatarUpdating) {
                                Text(uiText(R.string.ui_0933, "更换头像"))
                            }
                            if (state.userProfile.hermesAvatarUri.isNotBlank()) TextButton(onClick = onResetHermesAvatar, enabled = !state.isAvatarUpdating) {
                                Text(uiText(R.string.ui_0760, "恢复默认"))
                            }
                        }
                    }
                }
                HermesOutlinedTextField(name, { name = it.take(24) }, Modifier.fillMaxWidth().testTag("assistant_name"),
                    label = { Text(uiText(R.string.identity_assistant_name, "助理昵称")) }, placeholder = { Text("Hermes") }, singleLine = true)
            }
            SetupSection(if (firstRun) uiText(R.string.identity_your_name, "怎么称呼你") else uiText(R.string.identity_you, "你的信息")) {
                if (!firstRun) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    UserAvatar(state.userProfile.avatarUri, userName.ifBlank { uiText(R.string.ui_0605, "朋友") }, 64.dp)
                    Column(Modifier.weight(1f)) {
                        Text(uiText(R.string.identity_user_avatar, "我的头像"), style = MaterialTheme.typography.bodyMedium)
                        Row {
                            TextButton(onClick = { userPicker.launch(arrayOf("image/*")) }, enabled = !state.isAvatarUpdating) {
                                Text(uiText(R.string.ui_0933, "更换头像"))
                            }
                            if (state.userProfile.avatarUri.isNotBlank()) TextButton(onClick = onResetUserAvatar, enabled = !state.isAvatarUpdating) {
                                Text(uiText(R.string.ui_0760, "恢复默认"))
                            }
                        }
                    }
                }
                HermesOutlinedTextField(userName, { userName = it.take(24) }, Modifier.fillMaxWidth().testTag("user_display_name"),
                    label = { Text(uiText(R.string.identity_user_name, "你的昵称（可选）")) },
                    placeholder = { Text(uiText(R.string.identity_user_placeholder, "填写你喜欢的称呼")) }, singleLine = true)
                if (!firstRun) HermesOutlinedTextField(bio, { bio = it.take(50) }, Modifier.fillMaxWidth().testTag("user_bio"),
                    label = { Text(uiText(R.string.ui_0927, "个人签名")) }, minLines = 2, maxLines = 3)
            }
            if (!firstRun && state.username.isNotBlank()) Text(
                uiText(R.string.identity_gateway_account, "当前网关账号：%1\$s", state.username),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            state.errorMessage?.takeIf { it.isNotBlank() }?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
            HermesButton(onClick = {
                onSave(state.userProfile.copy(hermesDisplayName = name.trim().ifBlank { "Hermes" },
                    displayName = userName.trim(), bio = bio.trim()))
            }, modifier = Modifier.fillMaxWidth().testTag("identity_continue"), enabled = !state.isAvatarUpdating) {
                Text(if (firstRun) uiText(R.string.identity_next, "下一步 · 连接网关") else uiText(R.string.ui_0936, "保存"))
            }
            if (firstRun) Text(uiText(R.string.identity_change_later, "头像和昵称以后都可以在“我的 → 编辑信息”中修改。"),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    if (showLanguage) LanguagePicker(state.languageMode, onLanguageChange) { showLanguage = false }
    crop?.let { request ->
        AvatarCropSheet(request, { crop = null }) { uri, spec ->
            crop = null
            if (request.target == AvatarTarget.HERMES) onUpdateHermesAvatar(uri, spec) else onUpdateUserAvatar(uri, spec)
        }
    }
}

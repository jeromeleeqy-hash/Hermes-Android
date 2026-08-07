package com.qingyu.hermescompanion.ui.screen

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.qingyu.hermescompanion.BuildConfig
import com.qingyu.hermescompanion.model.NotificationPreferences
import com.qingyu.hermescompanion.model.VoicePreferences
import com.qingyu.hermescompanion.ui.component.GlassPanel
import com.qingyu.hermescompanion.ui.component.HermesIconKind
import com.qingyu.hermescompanion.ui.component.HermesMark
import com.qingyu.hermescompanion.ui.component.HermesMulticolorIcon
import com.qingyu.hermescompanion.ui.theme.HermesSpacing

@Composable
fun NotificationSettingsScreen(
    preferences: NotificationPreferences,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onChange: (NotificationPreferences) -> Unit,
    onTest: () -> Unit,
) {
    val context = LocalContext.current
    var permissionGranted by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permissionGranted = granted
        if (granted && !preferences.enabled) onChange(preferences.copy(enabled = true))
    }
    SettingsPage("通知设置", "安卓系统通知与后台任务提醒", contentPadding, onBack) {
        GlassPanel(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.fillMaxWidth()) {
                SwitchSettingRow("允许通知", if (permissionGranted) "已获得系统通知权限" else "需要先授予安卓通知权限", preferences.enabled && permissionGranted) { enabled ->
                    if (enabled && !permissionGranted && Build.VERSION.SDK_INT >= 33) permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    else onChange(preferences.copy(enabled = enabled))
                }
                SwitchSettingRow("对话完成提醒", "Hermes 在后台完成回复时通知", preferences.messageAlerts, preferences.enabled && permissionGranted) {
                    onChange(preferences.copy(messageAlerts = it))
                }
                SwitchSettingRow("定时任务提醒", "后台检查任务完成、失败与异常", preferences.taskAlerts, preferences.enabled && permissionGranted) {
                    onChange(preferences.copy(taskAlerts = it))
                }
                SwitchSettingRow("提示音", "使用系统通知提示音", preferences.sound, preferences.enabled && permissionGranted) {
                    onChange(preferences.copy(sound = it))
                }
                SwitchSettingRow("振动", "收到任务结果时振动", preferences.vibration, preferences.enabled && permissionGranted) {
                    onChange(preferences.copy(vibration = it))
                }
                SwitchSettingRow("桌面角标", "由手机桌面决定显示圆点或数字", preferences.badge, preferences.enabled && permissionGranted) {
                    onChange(preferences.copy(badge = it))
                }
            }
        }
        Text(
            "消息弹窗、锁屏显示和最终提示音仍受安卓系统频道设置控制。后台定时任务每 15 分钟补偿检查一次。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 10.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            Button(onClick = onTest, enabled = preferences.enabled && permissionGranted, modifier = Modifier.weight(1f)) {
                Text("发送测试通知")
            }
            TextButton(
                onClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
                    )
                },
            ) {
                HermesMulticolorIcon(HermesIconKind.OPEN_EXTERNAL, contentDescription = null)
                Text("系统设置", modifier = Modifier.padding(start = 4.dp))
            }
        }
    }
}

@Composable
fun VoiceSettingsScreen(
    preferences: VoicePreferences,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onChange: (VoicePreferences) -> Unit,
) {
    val context = LocalContext.current
    val recognitionIntent = remember {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        }
    }
    var available by remember {
        mutableStateOf(
            SpeechRecognizer.isRecognitionAvailable(context) ||
                recognitionIntent.resolveActivity(context.packageManager) != null,
        )
    }
    var languagePicker by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf("") }
    val testLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            testResult = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull().orEmpty()
            available = testResult.isNotBlank() || available
        }
    }
    val launchTest = {
        val intent = Intent(recognitionIntent).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, preferences.language)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "请说一句话测试语音输入")
        }
        try {
            testLauncher.launch(intent)
        } catch (_: ActivityNotFoundException) {
            available = false
            testResult = "未找到可用的系统语音识别服务"
        } catch (_: SecurityException) {
            available = false
            testResult = "系统阻止了语音识别服务启动"
        }
    }
    SettingsPage("语音输入", "使用手机的系统语音识别服务", contentPadding, onBack) {
        GlassPanel(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.fillMaxWidth()) {
                SwitchSettingRow(
                    "启用语音输入",
                    if (available) "已检测到系统语音识别服务" else "可先启用，再通过下方测试检查服务",
                    preferences.enabled,
                ) { onChange(preferences.copy(enabled = it)) }
                SwitchSettingRow("识别后自动发送", "关闭时先把识别文字放入输入框", preferences.autoSend, preferences.enabled) {
                    onChange(preferences.copy(autoSend = it))
                }
                SettingsChoiceRow("识别语言", voiceLanguageLabel(preferences.language), preferences.enabled) { languagePicker = true }
                SettingsActionRow("测试语音识别", if (testResult.isBlank()) "打开系统识别窗口" else testResult, preferences.enabled, launchTest)
            }
        }
        Text(
            "语音开关现在只控制 Hermes 是否显示和调用语音入口，不再因为部分手机检测不到服务而无法开启。实际识别由手机系统服务完成。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 10.dp),
        )
    }

    if (languagePicker) {
        AlertDialog(
            onDismissRequest = { languagePicker = false },
            title = { Text("识别语言") },
            text = {
                Column {
                    VOICE_LANGUAGES.forEach { (code, label) ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                onChange(preferences.copy(language = code))
                                languagePicker = false
                            }.padding(vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(label, modifier = Modifier.weight(1f))
                            if (preferences.language == code) {
                                HermesMulticolorIcon(
                                    HermesIconKind.CHECK,
                                    contentDescription = "已选择",
                                    iconSize = 19.dp,
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { languagePicker = false }) { Text("取消") } },
            containerColor = MaterialTheme.colorScheme.surface,
        )
    }
}

@Composable
fun AboutScreen(contentPadding: PaddingValues, onBack: () -> Unit) {
    SettingsPage("关于 Hermes", "版本与客户端能力", contentPadding, onBack) {
        GlassPanel(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
            Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                HermesMark()
                Column(modifier = Modifier.padding(start = 13.dp)) {
                    Text("Hermes", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("版本 ${BuildConfig.VERSION_NAME}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        GlassPanel(modifier = Modifier.fillMaxWidth().padding(top = 9.dp), shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
                CapabilityRow(HermesIconKind.CHAT, "远程对话", "通过登录网关连接自部署 Hermes Agent")
                CapabilityRow(HermesIconKind.SPACE, "项目空间", "浏览图片与文件，预览和编辑 Markdown")
                CapabilityRow(HermesIconKind.RECENT, "定时任务", "创建、暂停、恢复和手动执行 Cron Job")
                CapabilityRow(HermesIconKind.NOTIFICATION, "系统通知", "任务结果、弹窗、声音、振动与桌面角标")
                CapabilityRow(HermesIconKind.MICROPHONE, "语音输入", "调用安卓系统语音识别并写入对话")
            }
        }
        Text(
            "连接信息和登录会话保存在本机；认证 Cookie 使用 Android Keystore 加密。客户端不会把网关密码另行上传到第三方服务。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 12.dp),
        )
    }
}

@Composable
private fun SettingsPage(
    title: String,
    subtitle: String,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
        Row(modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(start = 8.dp, end = 8.dp, top = 6.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { HermesMulticolorIcon(HermesIconKind.BACK, contentDescription = "返回") }
            Column(modifier = Modifier.padding(start = 4.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = HermesSpacing.page),
        ) {
            content()
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SwitchSettingRow(title: String, subtitle: String, checked: Boolean, enabled: Boolean = true, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled, modifier = Modifier.scale(0.78f))
    }
}

@Composable
private fun SettingsChoiceRow(title: String, value: String, enabled: Boolean = true, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick).padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        HermesMulticolorIcon(HermesIconKind.CHEVRON_RIGHT, null, modifier = Modifier.padding(start = 7.dp), iconSize = 13.dp)
    }
}

@Composable
private fun SettingsActionRow(title: String, subtitle: String, enabled: Boolean = true, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick).padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
        HermesMulticolorIcon(HermesIconKind.CHEVRON_RIGHT, null, iconSize = 13.dp)
    }
}

private fun voiceLanguageLabel(code: String): String = VOICE_LANGUAGES.firstOrNull { it.first == code }?.second ?: code

private val VOICE_LANGUAGES = listOf(
    "zh-CN" to "中文（普通话）",
    "zh-HK" to "中文（粤语）",
    "en-US" to "English (US)",
)

@Composable
private fun CapabilityRow(icon: HermesIconKind, title: String, subtitle: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        HermesMulticolorIcon(icon, contentDescription = null, iconSize = 20.dp)
        Column(modifier = Modifier.padding(start = 8.dp)) {
            Text(title, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

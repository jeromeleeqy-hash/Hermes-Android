package com.qingyu.hermescompanion.ui.screen
import com.qingyu.hermescompanion.ui.component.HermesOutlinedTextField as OutlinedTextField
import com.qingyu.hermescompanion.ui.component.HermesRadioButton as RadioButton


import com.qingyu.hermescompanion.i18n.uiText
import com.qingyu.hermescompanion.R


import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.imePadding

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import com.qingyu.hermescompanion.ui.component.HermesButton as Button
import com.qingyu.hermescompanion.ui.component.HermesAlertDialog as AlertDialog
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.qingyu.hermescompanion.BuildConfig
import com.qingyu.hermescompanion.model.NotificationPreferences
import com.qingyu.hermescompanion.model.ServerVoiceSettings
import com.qingyu.hermescompanion.model.VoiceCaptureTarget
import com.qingyu.hermescompanion.model.VoicePhase
import com.qingyu.hermescompanion.model.VoicePreferences
import com.qingyu.hermescompanion.ui.AppUiState
import com.qingyu.hermescompanion.ui.normalizeVoiceTranscript
import com.qingyu.hermescompanion.ui.voiceRecognitionLanguage
import com.qingyu.hermescompanion.ui.component.GlassPanel
import com.qingyu.hermescompanion.ui.component.HermesIconKind
import com.qingyu.hermescompanion.ui.component.HermesMark
import com.qingyu.hermescompanion.ui.component.HermesMulticolorIcon
import com.qingyu.hermescompanion.ui.component.HermesSwitch
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
    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    androidx.compose.runtime.DisposableEffect(lifecycle) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) permissionGranted =
                Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permissionGranted = granted
        if (granted && !preferences.enabled) onChange(preferences.copy(enabled = true))
    }
    SettingsPage(uiText(R.string.ui_0868, "通知设置"), uiText(R.string.ui_0991, "安卓系统通知与后台任务提醒"), contentPadding, onBack) {
        SettingsGroup(uiText(R.string.ui_0992, "通知权限"), "") {
            Column(Modifier.fillMaxWidth()) {
                SwitchSettingRow(uiText(R.string.ui_0993, "允许通知"), if (permissionGranted) uiText(R.string.ui_0994, "已获得系统通知权限") else uiText(R.string.ui_0995, "需要先授予安卓通知权限"), preferences.enabled && permissionGranted) { enabled ->
                    if (enabled && !permissionGranted && Build.VERSION.SDK_INT >= 33) permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    else onChange(preferences.copy(enabled = enabled))
                }
            }
        }
        SettingsGroup(uiText(R.string.ui_0996, "提醒内容"), "") {
            Column(Modifier.fillMaxWidth()) {
                SwitchSettingRow(uiText(R.string.ui_0997, "对话完成提醒"), uiText(R.string.ui_0998, "Hermes 在后台完成回复时通知"), preferences.messageAlerts, preferences.enabled && permissionGranted) {
                    onChange(preferences.copy(messageAlerts = it))
                }
                SwitchSettingRow(uiText(R.string.ui_0999, "定时任务提醒"), uiText(R.string.ui_1000, "后台检查任务完成、失败与异常"), preferences.taskAlerts, preferences.enabled && permissionGranted) {
                    onChange(preferences.copy(taskAlerts = it))
                }
            }
        }
        SettingsGroup(uiText(R.string.ui_1001, "提示方式"), "") {
            Column(Modifier.fillMaxWidth()) {
                SwitchSettingRow(uiText(R.string.ui_1002, "提示音"), uiText(R.string.ui_1003, "使用系统通知提示音"), preferences.sound, preferences.enabled && permissionGranted) {
                    onChange(preferences.copy(sound = it))
                }
                SwitchSettingRow(uiText(R.string.ui_1004, "振动"), uiText(R.string.ui_1005, "收到任务结果时振动"), preferences.vibration, preferences.enabled && permissionGranted) {
                    onChange(preferences.copy(vibration = it))
                }
                SwitchSettingRow(uiText(R.string.ui_1006, "桌面角标"), uiText(R.string.ui_1007, "由手机桌面决定显示圆点或数字"), preferences.badge, preferences.enabled && permissionGranted) {
                    onChange(preferences.copy(badge = it))
                }
            }
        }
        Text(
            uiText(R.string.ui_1008, "锁屏显示和提示声音还受手机系统设置控制。更改后，可发送一条测试通知检查效果。"),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 10.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            Button(onClick = onTest, enabled = preferences.enabled && permissionGranted, modifier = Modifier.weight(1f)) {
                Text(uiText(R.string.ui_1009, "发送测试通知"))
            }
            TextButton(colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer), 
                onClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
                    )
                },
            ) {
                HermesMulticolorIcon(HermesIconKind.OPEN_EXTERNAL, contentDescription = null)
                Text(uiText(R.string.ui_1010, "系统设置"), modifier = Modifier.padding(start = 4.dp))
            }
        }
    }
}

@Composable
fun VoiceSettingsScreen(
    state: AppUiState,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onChange: (VoicePreferences) -> Unit,
    onSaveAgentVoice: (ServerVoiceSettings) -> Unit,
    onStartAgentSttTest: () -> Unit,
    onStopAgentSttTest: () -> Unit,
    onCancelAgentSttTest: () -> Unit,
    onTestAgentTts: () -> Unit,
    onUnavailable: () -> Unit,
) {
    val preferences = state.voicePreferences
    val context = LocalContext.current
    val recognitionIntent = remember {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        }
    }
    var picker by remember { mutableStateOf<String?>(null) }
    var customTarget by remember { mutableStateOf<String?>(null) }
    var customValue by remember { mutableStateOf("") }
    var serverDraft by remember(state.serverSettings.voice) { mutableStateOf(state.serverSettings.voice) }
    var testResult by remember { mutableStateOf("") }
    val testLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            testResult = normalizeVoiceTranscript(
                result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull().orEmpty(),
                preferences.transcriptScript,
            )
        }
    }
    val launchTest = {
        val intent = Intent(recognitionIntent).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                voiceRecognitionLanguage(preferences.language, preferences.transcriptScript),
            )
            putExtra(RecognizerIntent.EXTRA_PROMPT, uiText(R.string.ui_1011, "请说一句话测试语音输入"))
        }
        try {
            testLauncher.launch(intent)
        } catch (_: ActivityNotFoundException) {
            testResult = uiText(R.string.ui_1012, "未找到可用的系统语音识别服务")
        } catch (_: SecurityException) {
            testResult = uiText(R.string.ui_1013, "系统阻止了语音识别服务启动")
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) onStartAgentSttTest() else onUnavailable()
    }
    val startAgentTest = {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            onStartAgentSttTest()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
    val capture = state.voiceCapture
    val settingsTestActive = capture.target == VoiceCaptureTarget.SETTINGS_TEST && capture.phase != VoicePhase.IDLE

    SettingsPage(uiText(R.string.ui_0869, "语音设置"), uiText(R.string.ui_1014, "输入、朗读与语音服务"), contentPadding, onBack) {
        SettingsGroup(uiText(R.string.ui_0694, "语音输入"), "") {
            Column(Modifier.fillMaxWidth()) {
                SwitchSettingRow(
                    uiText(R.string.ui_1015, "启用语音功能"),
                    uiText(R.string.ui_1016, "显示语音输入和连续对话入口"),
                    preferences.enabled,
                ) { onChange(preferences.copy(enabled = it)) }
                SettingsChoiceRow(uiText(R.string.ui_1017, "语音引擎"), voiceEngineLabel(preferences.engine), preferences.enabled) { picker = "engine" }
                SettingsChoiceRow(uiText(R.string.ui_1018, "收音环境"), voiceSensitivityLabel(preferences.noiseSensitivity), preferences.enabled && preferences.engine != "system") { picker = "noiseSensitivity" }
                SettingsChoiceRow(uiText(R.string.ui_1019, "中文转写文字"), transcriptScriptLabel(preferences.transcriptScript), preferences.enabled) { picker = "transcriptScript" }
            }
        }
        SettingsGroup(uiText(R.string.ui_1020, "对话与朗读"), "") {
            Column(Modifier.fillMaxWidth()) {
                SwitchSettingRow(uiText(R.string.ui_1021, "自动朗读回复"), uiText(R.string.ui_1022, "完整朗读回答，中文优先匹配中文发音人"), preferences.autoRead, preferences.enabled) {
                    onChange(preferences.copy(autoRead = it))
                }
                SwitchSettingRow(uiText(R.string.ui_1023, "连续对话"), uiText(R.string.ui_1024, "朗读结束后自动重新聆听；可随时点按打断"), preferences.continuous, preferences.enabled && preferences.autoRead) {
                    onChange(preferences.copy(continuous = it))
                }
                SwitchSettingRow(uiText(R.string.ui_1025, "语音快速回答"), uiText(R.string.ui_1026, "语音回复更快，文字对话设置不受影响"), preferences.fastReply, preferences.enabled) {
                    onChange(preferences.copy(fastReply = it))
                }
            }
        }
        SettingsGroup(uiText(R.string.ui_1027, "手机语音"), uiText(R.string.ui_1028, "服务器不可用时使用手机能力")) {
            Column(Modifier.fillMaxWidth()) {
                SettingsChoiceRow(uiText(R.string.ui_1029, "手机兜底语言"), voiceLanguageLabel(preferences.language), preferences.enabled) { picker = "phoneLanguage" }
                SettingsActionRow(uiText(R.string.ui_1030, "手机朗读设置"), uiText(R.string.ui_1031, "安装中文语音包或选择手机发音引擎"), preferences.enabled) {
                    try { context.startActivity(Intent("com.android.settings.TTS_SETTINGS").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                    catch (_: ActivityNotFoundException) { testResult = uiText(R.string.ui_1032, "在手机系统设置中搜索“文字转语音”安装中文语音包") }
                    catch (_: SecurityException) { testResult = uiText(R.string.ui_1033, "在手机系统设置中打开“文字转语音”") }
                }
                SettingsActionRow(uiText(R.string.ui_1034, "测试手机语音识别"), if (testResult.isBlank()) uiText(R.string.ui_1035, "仅测试 Android 系统服务") else testResult, preferences.enabled, launchTest)
            }
        }

        SettingsGroup(uiText(R.string.ui_1036, "语音识别服务"), uiText(R.string.ui_1037, "当前 Profile：%1\$s", state.activeProfile)) {
            Column(Modifier.fillMaxWidth()) {
                SwitchSettingRow(uiText(R.string.ui_1038, "使用服务器识别"), uiText(R.string.ui_1039, "语音上传到当前 Hermes Profile 识别"), serverDraft.stt.enabled, preferences.enabled) {
                    serverDraft = serverDraft.copy(stt = serverDraft.stt.copy(enabled = it))
                }
                SettingsChoiceRow(uiText(R.string.ui_1040, "服务商"), sttProviderLabel(serverDraft.stt.provider), preferences.enabled) { picker = "sttProvider" }
                SettingsChoiceRow(uiText(R.string.ui_1041, "STT 模型"), serverDraft.stt.model.ifBlank { uiText(R.string.ui_1042, "服务器默认") }, preferences.enabled && serverDraft.stt.enabled) { picker = "sttModel" }
                SettingsChoiceRow(uiText(R.string.ui_1043, "Agent 识别语言"), sttLanguageLabel(serverDraft.stt.language), preferences.enabled && serverDraft.stt.enabled) { picker = "sttLanguage" }
            }
        }
        SettingsGroup(uiText(R.string.ui_1044, "语音朗读服务"), uiText(R.string.ui_1045, "设置合成模型与发音人")) {
            Column(Modifier.fillMaxWidth()) {
                SettingsChoiceRow(uiText(R.string.ui_1040, "服务商"), ttsProviderLabel(serverDraft.tts.provider), preferences.enabled) { picker = "ttsProvider" }
                SettingsChoiceRow(uiText(R.string.ui_1046, "TTS 模型"), serverDraft.tts.model.ifBlank { uiText(R.string.ui_1042, "服务器默认") }, preferences.enabled) { picker = "ttsModel" }
                SettingsChoiceRow(uiText(R.string.ui_1047, "声音"), ttsVoiceLabel(serverDraft.tts.voice), preferences.enabled) { picker = "ttsVoice" }
            }
        }
        Button(
            onClick = { onSaveAgentVoice(serverDraft) },
            enabled = preferences.enabled && !state.isAdvancedSettingsLoading,
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp).heightIn(min = 48.dp),
        ) { Text(if (state.isAdvancedSettingsLoading) uiText(R.string.ui_1048, "正在同步配置…") else uiText(R.string.ui_1049, "保存语音服务设置")) }

        SettingsGroup(uiText(R.string.ui_1050, "测试语音服务"), uiText(R.string.ui_1051, "保存后测试服务器识别与朗读")) {
            Column(Modifier.fillMaxWidth().padding(12.dp)) {
                Text(uiText(R.string.ui_1052, "语音服务测试"), fontWeight = FontWeight.SemiBold)
                com.qingyu.hermescompanion.ui.component.VoiceRecoveryBar()
                Text(
                    when {
                        capture.target == VoiceCaptureTarget.SETTINGS_TEST && capture.transcript.isNotBlank() -> uiText(R.string.ui_1053, "识别结果：%1\$s", capture.transcript)
                        settingsTestActive -> capture.message
                        else -> uiText(R.string.ui_1054, "保存配置后，可直接测试服务器识别与合成")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (capture.phase == VoicePhase.ERROR) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            if (capture.phase == VoicePhase.LISTENING && capture.target == VoiceCaptureTarget.SETTINGS_TEST) onStopAgentSttTest()
                            else startAgentTest()
                        },
                        enabled = !state.isAdvancedSettingsLoading && capture.phase != VoicePhase.TRANSCRIBING,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(if (capture.phase == VoicePhase.LISTENING && capture.target == VoiceCaptureTarget.SETTINGS_TEST) uiText(R.string.ui_1055, "结束并识别") else uiText(R.string.ui_1056, "测试 STT"))
                    }
                    Button(
                        onClick = onTestAgentTts,
                        enabled = !state.isAdvancedSettingsLoading && state.settingsActionKey == null && !settingsTestActive,
                        modifier = Modifier.weight(1f),
                    ) { Text(if (state.settingsActionKey == "voice-tts-test") uiText(R.string.ui_1057, "合成中…") else uiText(R.string.ui_1058, "试听 TTS")) }
                }
                if (settingsTestActive || (capture.target == VoiceCaptureTarget.SETTINGS_TEST && capture.phase == VoicePhase.ERROR)) {
                    TextButton(colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer), onClick = onCancelAgentSttTest, modifier = Modifier.align(Alignment.End)) { Text(uiText(R.string.ui_1059, "取消测试")) }
                }
            }
        }
        Text(
            uiText(R.string.ui_1060, "中文转写文字会统一作用于 Agent 与手机识别结果。自动模式会先调用服务器上的 Hermes STT/TTS，失败时回退手机能力。模型设置仅作用于当前 Profile；API Key 请在受保护的服务器或 HTTPS WebUI 中配置。"),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 10.dp),
        )
    }

    picker?.let { target ->
        val options = voicePickerOptions(target, serverDraft)
        VoiceOptionDialog(
            title = voicePickerTitle(target),
            options = options,
            selected = voicePickerSelected(target, preferences, serverDraft),
            onDismiss = { picker = null },
            onSelect = { value ->
                if (value == VOICE_CUSTOM_VALUE) {
                    customTarget = target
                    customValue = voicePickerSelected(target, preferences, serverDraft)
                } else {
                    when (target) {
                        "engine" -> onChange(preferences.copy(engine = value))
                        "noiseSensitivity" -> onChange(preferences.copy(noiseSensitivity = value))
                        "transcriptScript" -> onChange(preferences.copy(transcriptScript = value))
                        "phoneLanguage" -> onChange(preferences.copy(language = value))
                        "sttProvider" -> serverDraft = serverDraft.copy(stt = serverDraft.stt.copy(provider = value, model = defaultSttModel(value)))
                        "sttModel" -> serverDraft = serverDraft.copy(stt = serverDraft.stt.copy(model = value))
                        "sttLanguage" -> serverDraft = serverDraft.copy(stt = serverDraft.stt.copy(language = value))
                        "ttsProvider" -> serverDraft = serverDraft.copy(tts = serverDraft.tts.copy(provider = value, model = defaultTtsModel(value), voice = defaultTtsVoice(value)))
                        "ttsModel" -> serverDraft = serverDraft.copy(tts = serverDraft.tts.copy(model = value))
                        "ttsVoice" -> serverDraft = serverDraft.copy(tts = serverDraft.tts.copy(voice = value))
                    }
                }
                picker = null
            },
        )
    }
    customTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { customTarget = null },
            title = { Text(uiText(R.string.ui_1061, "自定义%1\$s", voicePickerTitle(target))) },
            text = {
                OutlinedTextField(
                    value = customValue,
                    onValueChange = { customValue = it },
                    singleLine = true,
                    label = { Text(uiText(R.string.ui_1062, "配置值")) },
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer), onClick = {
                    val value = customValue.trim()
                    if (value.isNotBlank()) {
                        when (target) {
                            "sttModel" -> serverDraft = serverDraft.copy(stt = serverDraft.stt.copy(model = value))
                            "ttsModel" -> serverDraft = serverDraft.copy(tts = serverDraft.tts.copy(model = value))
                            "ttsVoice" -> serverDraft = serverDraft.copy(tts = serverDraft.tts.copy(voice = value))
                        }
                    }
                    customTarget = null
                }) { Text(uiText(R.string.ui_1063, "应用")) }
            },
            dismissButton = { TextButton(colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer), onClick = { customTarget = null }) { Text(uiText(R.string.ui_0553, "取消")) } },
        )
    }
}

private fun voiceEngineLabel(value: String): String = when (value) {
    "agent" -> "Hermes Agent"
    "system" -> uiText(R.string.ui_1064, "手机系统")
    else -> uiText(R.string.ui_1065, "自动（推荐）")
}

private const val VOICE_CUSTOM_VALUE = "__custom__"

private fun sttProviderLabel(value: String): String = mapOf(
    "local" to uiText(R.string.ui_1066, "本地 Whisper"),
    "groq" to "Groq",
    "openai" to "OpenAI",
    "mistral" to "Mistral",
    "xai" to "xAI",
)[value] ?: value

private fun ttsProviderLabel(value: String): String = mapOf(
    "edge" to uiText(R.string.ui_1067, "Edge TTS（免费）"),
    "openai" to "OpenAI",
    "elevenlabs" to "ElevenLabs",
    "neutts" to uiText(R.string.ui_1068, "NeuTTS（本地）"),
    "minimax" to "MiniMax",
    "mistral" to "Mistral",
    "gemini" to "Gemini",
    "xai" to "xAI",
    "kittentts" to "KittenTTS",
    "piper" to uiText(R.string.ui_1069, "Piper（本地）"),
)[value] ?: value

private fun sttLanguageLabel(value: String): String = when (value) {
    "zh" -> uiText(R.string.ui_1070, "中文")
    "en" -> "English"
    "ja" -> uiText(R.string.ui_1071, "日本語")
    "ko" -> "한국어"
    else -> if (value.isBlank()) uiText(R.string.ui_1072, "自动检测") else value
}

private fun transcriptScriptLabel(value: String): String = when (value) {
    "simplified" -> uiText(R.string.ui_1073, "简体中文（推荐）")
    "traditional" -> uiText(R.string.ui_1074, "繁体中文")
    else -> uiText(R.string.ui_1075, "保持识别原文")
}

private fun ttsVoiceLabel(value: String): String = mapOf(
    "zh-CN-XiaoxiaoNeural" to uiText(R.string.ui_1076, "晓晓（女声）"),
    "zh-CN-XiaoyiNeural" to uiText(R.string.ui_1077, "晓伊（女声）"),
    "zh-CN-YunxiNeural" to uiText(R.string.ui_1078, "云希（男声）"),
    "zh-CN-YunjianNeural" to uiText(R.string.ui_1079, "云健（男声）"),
)[value] ?: value.ifBlank { uiText(R.string.ui_1042, "服务器默认") }

private fun defaultSttModel(provider: String): String = when (provider) {
    "groq" -> "whisper-large-v3-turbo"
    "openai" -> "gpt-4o-mini-transcribe"
    "mistral" -> "voxtral-mini-latest"
    "xai" -> "grok-stt"
    else -> "base"
}

private fun defaultTtsModel(provider: String): String = when (provider) {
    "openai" -> "gpt-4o-mini-tts"
    "elevenlabs" -> "eleven_multilingual_v2"
    "neutts" -> "neuphonic/neutts-air-q4-gguf"
    else -> ""
}

private fun defaultTtsVoice(provider: String): String = when (provider) {
    "edge" -> "zh-CN-XiaoxiaoNeural"
    "openai" -> "alloy"
    else -> ""
}

internal fun voiceSensitivityLabel(value: String): String = when (value) {
    "quiet" -> uiText(R.string.ui_1080, "轻声")
    "noisy" -> uiText(R.string.ui_1081, "嘈杂")
    else -> uiText(R.string.ui_1082, "日常")
}

internal val voiceSensitivityOptions get() = listOf(
    "balanced" to uiText(R.string.ui_1083, "日常 · 自动适应环境声音"),
    "noisy" to uiText(R.string.ui_1084, "嘈杂 · 减少背景声误触发，靠近手机说话"),
    "quiet" to uiText(R.string.ui_1085, "轻声 · 提高收音灵敏度，适合安静环境"),
)

private fun voicePickerTitle(target: String): String = when (target) {
    "engine" -> uiText(R.string.ui_1017, "语音引擎")
    "noiseSensitivity" -> uiText(R.string.ui_1018, "收音环境")
    "transcriptScript" -> uiText(R.string.ui_1019, "中文转写文字")
    "phoneLanguage" -> uiText(R.string.ui_1029, "手机兜底语言")
    "sttProvider" -> uiText(R.string.ui_1040, "服务商")
    "sttModel" -> uiText(R.string.ui_1041, "STT 模型")
    "sttLanguage" -> uiText(R.string.ui_1043, "Agent 识别语言")
    "ttsProvider" -> uiText(R.string.ui_1040, "服务商")
    "ttsModel" -> uiText(R.string.ui_1046, "TTS 模型")
    "ttsVoice" -> uiText(R.string.ui_1047, "声音")
    else -> uiText(R.string.ui_0869, "语音设置")
}

private fun voicePickerSelected(target: String, preferences: VoicePreferences, voice: ServerVoiceSettings): String = when (target) {
    "engine" -> preferences.engine
    "noiseSensitivity" -> preferences.noiseSensitivity
    "transcriptScript" -> preferences.transcriptScript
    "phoneLanguage" -> preferences.language
    "sttProvider" -> voice.stt.provider
    "sttModel" -> voice.stt.model
    "sttLanguage" -> voice.stt.language
    "ttsProvider" -> voice.tts.provider
    "ttsModel" -> voice.tts.model
    "ttsVoice" -> voice.tts.voice
    else -> ""
}

private fun voicePickerOptions(target: String, voice: ServerVoiceSettings): List<Pair<String, String>> {
    val base = when (target) {
        "noiseSensitivity" -> voiceSensitivityOptions
        "engine" -> listOf(
            "automatic" to uiText(R.string.ui_1065, "自动（推荐）"),
            "agent" to "Hermes Agent",
            "system" to uiText(R.string.ui_1064, "手机系统"),
        )
        "transcriptScript" -> listOf(
            "simplified" to uiText(R.string.ui_1073, "简体中文（推荐）"),
            "traditional" to uiText(R.string.ui_1074, "繁体中文"),
            "original" to uiText(R.string.ui_1075, "保持识别原文"),
        )
        "phoneLanguage" -> VOICE_LANGUAGES
        "sttProvider" -> listOf("local", "groq", "openai", "mistral", "xai").map { it to sttProviderLabel(it) }
        "sttModel" -> when (voice.stt.provider) {
            "local" -> listOf("tiny", "base", "small", "medium", "large-v3")
            "groq" -> listOf("whisper-large-v3-turbo", "whisper-large-v3")
            "openai" -> listOf("whisper-1", "gpt-4o-mini-transcribe", "gpt-4o-transcribe")
            "mistral" -> listOf("voxtral-mini-latest")
            "xai" -> listOf("grok-stt")
            else -> emptyList()
        }.map { it to it }
        "sttLanguage" -> listOf("" to uiText(R.string.ui_1072, "自动检测"), "zh" to uiText(R.string.ui_1086, "中文优先"), "en" to "English", "ja" to uiText(R.string.ui_1071, "日本語"), "ko" to "한국어")
        "ttsProvider" -> listOf("edge", "openai", "elevenlabs", "neutts", "minimax", "mistral", "gemini", "xai", "kittentts", "piper")
            .map { it to ttsProviderLabel(it) }
        "ttsModel" -> when (voice.tts.provider) {
            "openai" -> listOf("gpt-4o-mini-tts")
            "elevenlabs" -> listOf("eleven_multilingual_v2", "eleven_turbo_v2_5")
            "neutts" -> listOf("neuphonic/neutts-air-q4-gguf")
            else -> emptyList()
        }.map { it to it }
        "ttsVoice" -> when (voice.tts.provider) {
            "edge" -> listOf(
                "zh-CN-XiaoxiaoNeural" to uiText(R.string.ui_1076, "晓晓（女声）"),
                "zh-CN-XiaoyiNeural" to uiText(R.string.ui_1077, "晓伊（女声）"),
                "zh-CN-YunxiNeural" to uiText(R.string.ui_1078, "云希（男声）"),
                "zh-CN-YunjianNeural" to uiText(R.string.ui_1079, "云健（男声）"),
            )
            "openai" -> listOf("alloy", "echo", "fable", "onyx", "nova", "shimmer").map { it to it }
            else -> emptyList()
        }
        else -> emptyList()
    }.toMutableList()
    val selected = voicePickerSelected(target, VoicePreferences(), voice)
    if (target !in setOf("engine", "transcriptScript", "phoneLanguage") && selected.isNotBlank() && base.none { it.first == selected }) {
        base.add(selected to uiText(R.string.ui_1087, "当前：%1\$s", selected))
    }
    if (target in setOf("sttModel", "ttsModel", "ttsVoice")) base.add(VOICE_CUSTOM_VALUE to uiText(R.string.ui_1088, "自定义…"))
    return base
}

@Composable
private fun VoiceOptionDialog(
    title: String,
    options: List<Pair<String, String>>,
    selected: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
    var draft by remember(selected) { mutableStateOf(selected) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 420.dp).verticalScroll(rememberScrollState()).selectableGroup()) {
                options.forEach { (value, label) ->
                    Row(
                        modifier = Modifier.fillMaxWidth().selectable(value == draft, role = Role.RadioButton, onClick = { draft = value }).padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(label, modifier = Modifier.weight(1f))
                        RadioButton(selected = value == draft, onClick = null)
                    }
                }
            }
        },
        confirmButton = { Button(onClick = { onSelect(draft) }) { Text(uiText(R.string.ui_1089, "确定")) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(uiText(R.string.ui_0553, "取消")) } },
        containerColor = MaterialTheme.colorScheme.surface,
    )
}

@Composable
fun AboutScreen(contentPadding: PaddingValues, onBack: () -> Unit) {
    SettingsPage(uiText(R.string.ui_0873, "关于 Hermes"), uiText(R.string.ui_1090, "版本与客户端能力"), contentPadding, onBack) {
        GlassPanel(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
            Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                HermesMark()
                Column(modifier = Modifier.padding(start = 13.dp)) {
                    Text("Hermes", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text(uiText(R.string.ui_1091, "版本 %1\$s", BuildConfig.VERSION_NAME), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        GlassPanel(modifier = Modifier.fillMaxWidth().padding(top = 9.dp), shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
                CapabilityRow(HermesIconKind.CHAT, uiText(R.string.ui_1092, "远程对话"), uiText(R.string.ui_1093, "通过登录网关连接自部署 Hermes Agent"))
                CapabilityRow(HermesIconKind.STATUS_BUSY, uiText(R.string.ui_1094, "Agent 控制"), uiText(R.string.ui_1095, "运行状态、追加要求、排队发送与交互请求"))
                CapabilityRow(HermesIconKind.SPACE, uiText(R.string.ui_1096, "项目空间"), uiText(R.string.ui_1097, "浏览图片与文件，预览和编辑 Markdown"))
                CapabilityRow(HermesIconKind.RECENT, uiText(R.string.ui_0116, "定时任务"), uiText(R.string.ui_1098, "创建、暂停、恢复和手动执行 Cron Job"))
                CapabilityRow(HermesIconKind.NOTIFICATION, uiText(R.string.ui_1099, "系统通知"), uiText(R.string.ui_1100, "任务结果、弹窗、声音、振动与桌面角标"))
                CapabilityRow(HermesIconKind.MICROPHONE, uiText(R.string.ui_1101, "语音对话"), uiText(R.string.ui_1102, "Hermes STT/TTS 与安卓系统能力自动回退"))
            }
        }
        Text(
            uiText(R.string.ui_1103, "连接信息和登录会话保存在本机；认证 Cookie 使用 Android Keystore 加密。客户端不会把网关密码另行上传到第三方服务。"),
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
    Column(modifier = Modifier.fillMaxSize().padding(contentPadding).navigationBarsPadding().imePadding()) {
        Row(modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(start = 8.dp, end = 8.dp, top = 5.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { HermesMulticolorIcon(HermesIconKind.BACK, contentDescription = uiText(R.string.ui_0554, "返回")) }
            Column(modifier = Modifier.weight(1f).padding(start = 4.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
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
private fun SettingsGroup(
    title: String,
    subtitle: String,
    content: @Composable BoxScope.() -> Unit,
) {
    com.qingyu.hermescompanion.ui.component.SettingsBlock(title, subtitle, content)

}

@Composable
private fun SwitchSettingRow(title: String, subtitle: String, checked: Boolean, enabled: Boolean = true, onCheckedChange: (Boolean) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        com.qingyu.hermescompanion.ui.component.SettingsToggle(title, subtitle, checked, enabled, onCheckedChange = onCheckedChange)
        SettingsRowDivider()
    }
}

@Composable
private fun SettingsChoiceRow(title: String, value: String, enabled: Boolean = true, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        com.qingyu.hermescompanion.ui.component.SettingsChoice(title, value, enabled, model = title.endsWith(uiText(R.string.ui_0422, "模型"), ignoreCase = true), onClick = onClick)
        SettingsRowDivider()
    }

}

@Composable
private fun SettingsActionRow(title: String, subtitle: String, enabled: Boolean = true, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small)
                .clickable(enabled = enabled, onClick = onClick).padding(horizontal = 16.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
            }
            HermesMulticolorIcon(HermesIconKind.CHEVRON_RIGHT, null, iconSize = 13.dp)
        }
        SettingsRowDivider()
    }
}

@Composable
private fun SettingsRowDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 12.dp, end = 12.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.46f),
    )
}

private fun voiceLanguageLabel(code: String): String = VOICE_LANGUAGES.firstOrNull { it.first == code }?.second ?: code

private val VOICE_LANGUAGES get() = listOf(
    "zh-CN" to uiText(R.string.ui_1261, "中文（普通话）"),
    "zh-HK" to uiText(R.string.ui_1262, "中文（粤语）"),
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

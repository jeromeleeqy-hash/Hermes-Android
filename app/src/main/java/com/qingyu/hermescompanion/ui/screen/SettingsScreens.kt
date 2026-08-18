package com.qingyu.hermescompanion.ui.screen

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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
            putExtra(RecognizerIntent.EXTRA_PROMPT, "请说一句话测试语音输入")
        }
        try {
            testLauncher.launch(intent)
        } catch (_: ActivityNotFoundException) {
            testResult = "未找到可用的系统语音识别服务"
        } catch (_: SecurityException) {
            testResult = "系统阻止了语音识别服务启动"
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

    SettingsPage("语音设置", "单次输入、连续对话与 Agent 语音模型", contentPadding, onBack) {
        GlassPanel(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.fillMaxWidth()) {
                SwitchSettingRow(
                    "启用语音功能",
                    "在聊天页显示语音输入与语音对话入口",
                    preferences.enabled,
                ) { onChange(preferences.copy(enabled = it)) }
                SettingsChoiceRow("语音引擎", voiceEngineLabel(preferences.engine), preferences.enabled) { picker = "engine" }
                SwitchSettingRow("自动朗读回复", "优先使用 Agent TTS，失败时回退到 Android TTS", preferences.autoRead, preferences.enabled) {
                    onChange(preferences.copy(autoRead = it))
                }
                SwitchSettingRow("连续对话", "朗读结束后自动重新聆听；可随时点按打断", preferences.continuous, preferences.enabled && preferences.autoRead) {
                    onChange(preferences.copy(continuous = it))
                }
                SwitchSettingRow("识别后自动发送", "关闭时先把识别文字放入输入框", preferences.autoSend, preferences.enabled) {
                    onChange(preferences.copy(autoSend = it))
                }
                SettingsChoiceRow("中文转写文字", transcriptScriptLabel(preferences.transcriptScript), preferences.enabled) { picker = "transcriptScript" }
                SettingsChoiceRow("手机兜底语言", voiceLanguageLabel(preferences.language), preferences.enabled) { picker = "phoneLanguage" }
                SettingsActionRow("测试手机语音识别", if (testResult.isBlank()) "仅测试 Android 系统服务" else testResult, preferences.enabled, launchTest)
            }
        }

        Text(
            "Hermes Agent 语音服务 · Profile：${state.activeProfile}",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 4.dp, top = 18.dp, bottom = 8.dp),
        )
        GlassPanel(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.fillMaxWidth()) {
                SwitchSettingRow("启用服务器 STT", "语音上传到当前 Hermes Profile 识别", serverDraft.stt.enabled, preferences.enabled) {
                    serverDraft = serverDraft.copy(stt = serverDraft.stt.copy(enabled = it))
                }
                SettingsChoiceRow("STT 服务商", sttProviderLabel(serverDraft.stt.provider), preferences.enabled) { picker = "sttProvider" }
                SettingsChoiceRow("STT 模型", serverDraft.stt.model.ifBlank { "服务器默认" }, preferences.enabled && serverDraft.stt.enabled) { picker = "sttModel" }
                SettingsChoiceRow("Agent 识别语言", sttLanguageLabel(serverDraft.stt.language), preferences.enabled && serverDraft.stt.enabled) { picker = "sttLanguage" }
            }
        }
        GlassPanel(modifier = Modifier.fillMaxWidth().padding(top = 10.dp), shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.fillMaxWidth()) {
                SettingsChoiceRow("TTS 服务商", ttsProviderLabel(serverDraft.tts.provider), preferences.enabled) { picker = "ttsProvider" }
                SettingsChoiceRow("TTS 模型", serverDraft.tts.model.ifBlank { "服务器默认" }, preferences.enabled) { picker = "ttsModel" }
                SettingsChoiceRow("声音", ttsVoiceLabel(serverDraft.tts.voice), preferences.enabled) { picker = "ttsVoice" }
            }
        }
        Button(
            onClick = { onSaveAgentVoice(serverDraft) },
            enabled = preferences.enabled && !state.isAdvancedSettingsLoading,
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
        ) { Text(if (state.isAdvancedSettingsLoading) "正在同步配置…" else "保存 Agent 语音模型") }

        GlassPanel(modifier = Modifier.fillMaxWidth().padding(top = 10.dp), shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.fillMaxWidth().padding(12.dp)) {
                Text("语音服务测试", fontWeight = FontWeight.SemiBold)
                Text(
                    when {
                        capture.target == VoiceCaptureTarget.SETTINGS_TEST && capture.transcript.isNotBlank() -> "识别结果：${capture.transcript}"
                        settingsTestActive -> capture.message
                        else -> "保存配置后，可直接测试服务器识别与合成"
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
                        Text(if (capture.phase == VoicePhase.LISTENING && capture.target == VoiceCaptureTarget.SETTINGS_TEST) "结束并识别" else "测试 STT")
                    }
                    Button(
                        onClick = onTestAgentTts,
                        enabled = !state.isAdvancedSettingsLoading && state.settingsActionKey == null && !settingsTestActive,
                        modifier = Modifier.weight(1f),
                    ) { Text(if (state.settingsActionKey == "voice-tts-test") "合成中…" else "试听 TTS") }
                }
                if (settingsTestActive || (capture.target == VoiceCaptureTarget.SETTINGS_TEST && capture.phase == VoicePhase.ERROR)) {
                    TextButton(onClick = onCancelAgentSttTest, modifier = Modifier.align(Alignment.End)) { Text("取消测试") }
                }
            }
        }
        Text(
            "中文转写文字会统一作用于 Agent 与手机识别结果。自动模式会先调用服务器上的 Hermes STT/TTS，失败时回退手机能力。模型设置仅作用于当前 Profile；API Key 请在受保护的服务器或 HTTPS WebUI 中配置。",
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
            title = { Text("自定义${voicePickerTitle(target)}") },
            text = {
                OutlinedTextField(
                    value = customValue,
                    onValueChange = { customValue = it },
                    singleLine = true,
                    label = { Text("配置值") },
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val value = customValue.trim()
                    if (value.isNotBlank()) {
                        when (target) {
                            "sttModel" -> serverDraft = serverDraft.copy(stt = serverDraft.stt.copy(model = value))
                            "ttsModel" -> serverDraft = serverDraft.copy(tts = serverDraft.tts.copy(model = value))
                            "ttsVoice" -> serverDraft = serverDraft.copy(tts = serverDraft.tts.copy(voice = value))
                        }
                    }
                    customTarget = null
                }) { Text("应用") }
            },
            dismissButton = { TextButton(onClick = { customTarget = null }) { Text("取消") } },
        )
    }
}

private fun voiceEngineLabel(value: String): String = when (value) {
    "agent" -> "Hermes Agent"
    "system" -> "手机系统"
    else -> "自动（推荐）"
}

private const val VOICE_CUSTOM_VALUE = "__custom__"

private fun sttProviderLabel(value: String): String = mapOf(
    "local" to "本地 Whisper",
    "groq" to "Groq",
    "openai" to "OpenAI",
    "mistral" to "Mistral",
    "xai" to "xAI",
)[value] ?: value

private fun ttsProviderLabel(value: String): String = mapOf(
    "edge" to "Edge TTS（免费）",
    "openai" to "OpenAI",
    "elevenlabs" to "ElevenLabs",
    "neutts" to "NeuTTS（本地）",
    "minimax" to "MiniMax",
    "mistral" to "Mistral",
    "gemini" to "Gemini",
    "xai" to "xAI",
    "kittentts" to "KittenTTS",
    "piper" to "Piper（本地）",
)[value] ?: value

private fun sttLanguageLabel(value: String): String = when (value) {
    "zh" -> "中文"
    "en" -> "English"
    "ja" -> "日本語"
    "ko" -> "한국어"
    else -> if (value.isBlank()) "自动检测" else value
}

private fun transcriptScriptLabel(value: String): String = when (value) {
    "simplified" -> "简体中文（推荐）"
    "traditional" -> "繁体中文"
    else -> "保持识别原文"
}

private fun ttsVoiceLabel(value: String): String = mapOf(
    "zh-CN-XiaoxiaoNeural" to "晓晓（女声）",
    "zh-CN-XiaoyiNeural" to "晓伊（女声）",
    "zh-CN-YunxiNeural" to "云希（男声）",
    "zh-CN-YunjianNeural" to "云健（男声）",
)[value] ?: value.ifBlank { "服务器默认" }

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

private fun voicePickerTitle(target: String): String = when (target) {
    "engine" -> "语音引擎"
    "transcriptScript" -> "中文转写文字"
    "phoneLanguage" -> "手机兜底语言"
    "sttProvider" -> "STT 服务商"
    "sttModel" -> "STT 模型"
    "sttLanguage" -> "Agent 识别语言"
    "ttsProvider" -> "TTS 服务商"
    "ttsModel" -> "TTS 模型"
    "ttsVoice" -> "声音"
    else -> "语音设置"
}

private fun voicePickerSelected(target: String, preferences: VoicePreferences, voice: ServerVoiceSettings): String = when (target) {
    "engine" -> preferences.engine
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
        "engine" -> listOf(
            "automatic" to "自动（推荐）",
            "agent" to "Hermes Agent",
            "system" to "手机系统",
        )
        "transcriptScript" -> listOf(
            "simplified" to "简体中文（推荐）",
            "traditional" to "繁体中文",
            "original" to "保持识别原文",
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
        "sttLanguage" -> listOf("" to "自动检测", "zh" to "中文优先", "en" to "English", "ja" to "日本語", "ko" to "한국어")
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
                "zh-CN-XiaoxiaoNeural" to "晓晓（女声）",
                "zh-CN-XiaoyiNeural" to "晓伊（女声）",
                "zh-CN-YunxiNeural" to "云希（男声）",
                "zh-CN-YunjianNeural" to "云健（男声）",
            )
            "openai" -> listOf("alloy", "echo", "fable", "onyx", "nova", "shimmer").map { it to it }
            else -> emptyList()
        }
        else -> emptyList()
    }.toMutableList()
    val selected = voicePickerSelected(target, VoicePreferences(), voice)
    if (target !in setOf("engine", "transcriptScript", "phoneLanguage") && selected.isNotBlank() && base.none { it.first == selected }) {
        base.add(selected to "当前：$selected")
    }
    if (target in setOf("sttModel", "ttsModel", "ttsVoice")) base.add(VOICE_CUSTOM_VALUE to "自定义…")
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
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                options.forEach { (value, label) ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onSelect(value) }.padding(vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(label, modifier = Modifier.weight(1f))
                        if (value == selected) HermesMulticolorIcon(HermesIconKind.CHECK, "已选择", iconSize = 19.dp)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("取消") } },
        containerColor = MaterialTheme.colorScheme.surface,
    )
}

@Composable
fun AboutScreen(contentPadding: PaddingValues, onBack: () -> Unit) {
    SettingsPage("关于 Hermes", "版本与客户端能力", contentPadding, onBack) {
        GlassPanel(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
            Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                HermesMark()
                Column(modifier = Modifier.padding(start = 13.dp)) {
                    Text("Hermes", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text("版本 ${BuildConfig.VERSION_NAME}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        GlassPanel(modifier = Modifier.fillMaxWidth().padding(top = 9.dp), shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
                CapabilityRow(HermesIconKind.CHAT, "远程对话", "通过登录网关连接自部署 Hermes Agent")
                CapabilityRow(HermesIconKind.STATUS_BUSY, "Agent 控制", "运行状态、追加要求、排队发送与交互请求")
                CapabilityRow(HermesIconKind.SPACE, "项目空间", "浏览图片与文件，预览和编辑 Markdown")
                CapabilityRow(HermesIconKind.RECENT, "定时任务", "创建、暂停、恢复和手动执行 Cron Job")
                CapabilityRow(HermesIconKind.NOTIFICATION, "系统通知", "任务结果、弹窗、声音、振动与桌面角标")
                CapabilityRow(HermesIconKind.MICROPHONE, "语音对话", "Hermes STT/TTS 与安卓系统能力自动回退")
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
fun ChangeLogScreen(contentPadding: PaddingValues, onBack: () -> Unit) {
    SettingsPage("更新日志", "Hermes 移动端版本记录", contentPadding, onBack) {
        if (BuildConfig.VERSION_CODE >= 300) {
            ChangeLogEntry(
                version = "3.0.0-release",
                date = "2026-08-18",
                current = true,
                items = listOf(
                    "界面全面收敛为轻量 Telegram 式设计，完成导航、会话、聊天、空间、任务、设置与状态反馈的统一图标和交互动效",
                    "重建个人主页与沉浸式头像，加入我的记忆、我的心智和更完整的使用说明，并在关键空白页加入 Hermes 形象动画",
                    "优化会话筛选、全文搜索、左滑归档与删除、命令面板、聊天排版和任务执行中心；Cron 会话统一归入任务页",
                    "专家会审升级为更易理解的多成员协作展示，并完善 Agent 运行状态、追加要求、排队发送、审批与澄清闭环",
                    "完善单次语音和连续语音、Agent STT/TTS、中文转写及系统能力回退；连续语音采用全新的声能核心交互",
                    "升级空间与产物中心，支持 Markdown、PDF、HTML、图片和常见文本的预览、保存、分享及来源定位",
                    "增强长对话性能、后台执行、断线恢复、静默流自检和参考来源解析，减少卡顿、假等待与重复信息",
                ),
            )
            ChangeLogEntry(
                version = "2.0.0",
                date = "1.0–2.0 重要更新汇总",
                items = listOf(
                    "完成远程网关登录、Cookie 加密保存、多 Profile 切换，以及会话、项目、设置和缓存的 Profile 隔离",
                    "建立稳定的移动聊天体验：草稿恢复、失败重试、流式回复、未读通知、全文搜索、Markdown 与图片文件预览",
                    "接入 Hermes Agent 实时运行能力，支持查看阶段、停止执行、追加要求、排队发送，并直接处理审批和澄清请求",
                    "建立空间与产物中心，支持系统分享进入 Hermes、生成文件汇总、应用内预览和返回来源消息",
                    "将任务页升级为 Agent 执行中心，集中管理待处理请求、运行任务、Cron 定时任务和执行记录",
                    "接入单次与连续语音对话，支持 Agent STT/TTS、自动朗读、打断、连续聆听和 Android 系统能力回退",
                    "完善模型、Skills、工具集、MCP、记忆、审批和远程 Agent 更新，并通过性能优化与 UI 2.0 奠定统一体验基础",
                ),
            )
        } else {
        ChangeLogEntry(
            version = "2.8.1-release",
            date = "2026-08-18",
            current = true,
            items = listOf(
                "重绘修长版置顶图钉，任务新建入口改为小型圆形；移除资料签名和网关主按钮中的绿色对号",
                "对话列表不再展示 Cron 会话，运行内容集中到任务页执行记录；将长忆录 / 心智章程改名为我的记忆 / 我的心智",
                "头像大图取消局部模糊并支持照片区域上滑返回；使用说明移至我的页面并取消数字编号",
                "新会话、空对话列表和使用说明加入耳机女孩循环动画，并为旧版 Android 提供静态兜底",
                "专家会审结果按独立专家成员分组展示，避免子 Agent 内容被误认为用户消息",
                "统一正在处理与正在思考的排版，后台执行改为底部悬浮状态；连续语音升级为动态科技感控制球",
                "移除聊天中的正在整理结果面板，新增静默流自检、延长断线取回并完善审批 / 澄清续接",
                "来源解析支持引用式链接和带括号 URL，忽略代码块、行内代码与图片地址",
            ),
        )
        ChangeLogEntry(
            version = "2.8.0-release",
            date = "2026-08-17",
            items = listOf(
                "修复搜索页系统侧滑返回、设置子页面返回栈和专家会审返回助理面板的导航逻辑",
                "助理与附件面板改用横向拖动条并默认完整展开；聊天完成状态重新对齐，长回复行距提升约 10%",
                "重构我的主页信息层级，加入我的记忆与我的心智文件入口，并完善沉浸式头像与资料操作区",
                "连续语音界面上移视觉中心，加入由真实麦克风音量驱动的声波纹；无声音时保持安静",
                "缩减 Markdown 编辑器工具栏、任务新增入口，优化会话列表首屏与后台刷新速度",
                "重绘 Hermes 助理图标并统一细节尺寸，完善分层使用说明与配置、故障排查建议",
            ),
        )
        ChangeLogEntry(
            version = "2.7.0",
            date = "2026-08-17",
            items = listOf(
                "完整接入八轮精细图标：236 个 SVG 转为 Android 原生 VectorDrawable，统一覆盖导航、会话、聊天、空间、任务、设置和状态图标",
                "移除业务代码中的旧位图与旧矢量引用，所有页面统一通过语义图标目录调用，浅色与深色模式自动着色",
                "底栏采用 22dp 线性/实体双态图标；列表、输入器、文档操作与设置入口按 17–20dp 分级，保留 40–48dp 易点击区域",
                "补齐 Markdown、网页、文件夹、来源会话、保存、分享与下载等专用图标，减少含义模糊的通用图形",
                "语义色收敛为蓝、青、紫、绿、琥珀和红，仅用于操作、状态与文件类型提示",
            ),
        )
        ChangeLogEntry(
            version = "2.6.0",
            date = "2026-08-17",
            items = listOf(
                "启动 Telegram 风格图标系统第一轮，统一底栏、首页、聊天、助理面板、个人主页和设置入口的矢量语言",
                "底栏使用未选中线性、选中实体的双态图标，并与滑动底板同步淡入、缩放和换色",
                "首页搜索与新建对话、聊天输入器、助理面板、Memory / Soul 和个人主页操作改用圆润轻量图标",
                "设置入口使用白色图形配高辨识度语义色圆角底板，同时适配浅色与深色模式",
            ),
        )
        ChangeLogEntry(
            version = "2.5.1",
            date = "2026-08-16",
            items = listOf(
                "修正我的主页 Memory / Soul 入口：不再跳到配置页，而是打开当前 Hermes Profile 的真实文件",
                "Memory 读取当前 Profile 的 memories/MEMORY.md，Soul 读取当前 Profile 根目录的 SOUL.md",
                "新增独立只读 Markdown 预览，支持复制路径、系统分享和保存到手机，返回后仍停留在我的主页",
                "兼容普通主机目录与 HERMES_HOME 直接作为文件根目录的服务器部署方式",
            ),
        )
        ChangeLogEntry(
            version = "2.5.0",
            date = "2026-08-16",
            items = listOf(
                "移除全局多余的灰色半透明按压遮罩，保留底栏、分段控件和开关自身的滑动与弹性动画",
                "首页下拉菜单与筛选选框之间增加轻微间距，避免菜单紧贴触发器",
                "会话列表支持左滑露出归档和删除按钮，并保留二次确认防误触",
                "我的主页新增 Memory 和 Soul 两个 Agent 核心入口",
                "头像由圆形连续铺展为顶部大图，返回时反向收回，替代整页缩放淡入",
            ),
        )
        ChangeLogEntry(
            version = "2.4.0",
            date = "2026-08-16",
            items = listOf(
                "聊天执行结束改为与正文左对齐的轻量状态行，不再重复显示完整 AI 回复和绿色结果框",
                "首页下拉选单字号与筛选框统一，保留文字左对齐、对勾右对齐的清晰选择结构",
                "我的页面重建为个人主页，只保留头像、设置照片、编辑信息和设置三个核心操作",
                "设置迁移到独立纯列表页面，不再重复展示头像与个人资料；新增版本、网关和使用说明入口",
                "恢复照片选择、裁剪和应用私有存储，点击头像可平滑放大为顶部沉浸式照片视图",
            ),
        )
        ChangeLogEntry(
            version = "2.3.0",
            date = "2026-08-16",
            items = listOf(
                "专家会审移入右上角助理面板，聊天输入区只保留附件、命令及语音/发送状态切换",
                "首页菜单改为文字左对齐、对勾右对齐，新建对话按钮缩小为圆形",
                "点击我的页面头像进入沉浸式个人资料页，增加圆形大头像与顺滑放大过渡",
                "底栏与空间、任务页分段加入滑动选中动画，图标颜色和尺度同步过渡",
                "通知、任务及高级设置开关统一弹性动效与主题色状态",
            ),
        )
        ChangeLogEntry(
            version = "2.2.0",
            date = "2026-08-16",
            items = listOf(
                "首页移除重复标题并压缩头部、筛选栏、会话行和底部导航",
                "下拉菜单缩小字号与行高，修复按压态越过圆角形成灰色方块",
                "搜索页改为单行返回与搜索工具栏，聊天输入器减少色块和高度",
                "连续助手消息合并头像与名称，最近产物以小图标进入来源会话",
                "修复新对话与新建任务图标，压缩执行中心、我的和设置页密度",
            ),
        )
        ChangeLogEntry(
            version = "2.1.0",
            date = "2026-08-16",
            items = listOf(
                "统一为 Telegram 式清爽办公界面，取消清爽办公与液态玻璃双皮肤分叉",
                "首页搜索固定到右上角，新对话改为右下角悬浮按钮，并移除批量改名菜单",
                "我的页面所有功能入口移除说明副标题，改为更紧凑的白色分组列表",
                "会话列表使用低饱和字母头像，页面、弹层与底部导航统一表面和交互蓝",
                "任务页补充执行中心标题和 Agent 状态，保留浅色、深色与跟随系统",
            ),
        )
        ChangeLogEntry(
            version = "2.0.0",
            date = "2026-08-16",
            items = listOf(
                "全量套用 UI 2.0，统一会话、聊天、空间、任务、我的、网关、语音与全部设置页",
                "升级浅色与深色配色，中性色承载内容，蓝、青、紫、绿、琥珀与红只表达明确语义",
                "清爽办公使用纯净表面与轻边界；液态玻璃使用可读的半透明面板、细描边和受控光晕",
                "聊天输入器收敛为中性工具键与唯一高亮发送键，助手正文改为内容优先文档流",
                "统一悬浮底部导航、紧凑菜单、弹层字号、设置分隔线、图标尺寸和低饱和底板",
            ),
        )
        ChangeLogEntry(
            version = "1.7.0",
            date = "2026-08-15",
            items = listOf(
                "启动 UI 2.0：统一字体、字号、间距、圆角、菜单与表面层级",
                "清爽办公减少色块、胶囊、阴影和图标底板，提升信息密度与内容主次",
                "圆润卡片升级为液态玻璃，以柔和透光、细描边和轻阴影构建层次",
                "重做首页筛选器、下拉菜单、会话图标、底部导航与我的页面设置列表",
                "优化深色模式语义颜色，确保两套皮肤下标题、正文和辅助文字清晰可读",
            ),
        )
        ChangeLogEntry(
            version = "1.6.1",
            date = "2026-08-15",
            items = listOf(
                "语音设置新增中文转写文字，可选择简体、繁体或保持识别原文",
                "默认统一输出简体中文，旧用户升级后无需重新设置",
                "单条语音、连续语音、Agent STT 与手机系统识别共用繁简设置",
            ),
        )
        ChangeLogEntry(
            version = "1.6.0",
            date = "2026-08-15",
            items = listOf(
                "聊天输入框麦克风恢复为单次语音输入，识别后写入输入框或按设置自动发送",
                "顶部波形按钮独立进入连续语音，连续语音页不再重复显示文字对话卡",
                "语音设置新增当前 Profile 的 STT/TTS 服务商、模型、中文优先和声音管理",
                "新增 Agent STT 录音测试、TTS 试听与明确的手机系统兜底测试",
            ),
        )
        ChangeLogEntry(
            version = "1.5.1",
            date = "2026-08-15",
            items = listOf(
                "输入框麦克风统一进入 Hermes Agent 语音对话，不再依赖手机预装语音助手",
                "识别 Agent STT 版本不匹配，隐藏底层英文异常并给出明确修复说明",
                "语音错误页可直达远程网关检查并更新 Hermes Agent，返回后直接重试",
                "优化无系统语音服务时的回退提示",
            ),
        )
        ChangeLogEntry(
            version = "1.5.0",
            date = "2026-08-15",
            items = listOf(
                "长对话首屏改为最近 60 条，支持向上按页加载更早消息",
                "流式回复按帧合并刷新，生成结束后再渲染完整 Markdown，减少长回复卡顿",
                "最近产物使用会话指纹增量索引，只扫描发生变化的会话",
                "输入区新增斜杠按钮和可搜索、按类别展示的 Hermes 命令面板",
                "新增专家会审：深度模式调用真实子 Agent，快速模式使用服务器 MoA 预设",
                "会审只展示进度、共识、关键分歧、证据风险与最终裁决，不展示原始 AI 互聊",
            ),
        )
        ChangeLogEntry(
            version = "1.4.0",
            date = "2026-08-15",
            items = listOf(
                "任务页升级为 Agent 执行中心，集中处理审批、澄清、运行进度与最近结果",
                "审批与澄清通知可快速进入处理页或来源会话",
                "新增 Agent STT/TTS、安卓回退、自动朗读、打断与连续模式的语音对话",
                "网关页可检查 Agent 版本、安装方式和待更新提交",
                "支持通过官方 Gateway 安全更新 Agent，并在服务重启后自动重连验证",
            ),
        )
        ChangeLogEntry(
            version = "1.3.0",
            date = "2026-08-15",
            items = listOf(
                "支持从安卓系统分享网页、文字、图片和常见文本到 Hermes",
                "产物中心支持 Markdown、PDF、隔离 HTML 与常见文本预览",
                "产物可复制路径、保存到手机、系统分享或返回来源消息",
                "自动检测 Agent / Gateway 版本及服务端公布的能力",
                "助手回复中的网页链接自动整理为参考来源卡片",
                "修复暗色模式下聊天标题等文字显示为黑色的问题",
            ),
        )
        ChangeLogEntry(
            version = "1.2.0",
            date = "2026-08-15",
            items = listOf(
                "空间新增最近产物，汇总当前 Profile 对话中生成的文件",
                "文件与来源会话可互相跳转，并定位到对应消息",
                "对话搜索升级为消息正文搜索，展示命中上下文",
                "搜索结果可直接进入会话并高亮命中的消息",
                "远程网关新增接口、登录、实时通道与能力诊断",
            ),
        )
        ChangeLogEntry(
            version = "1.1.0",
            date = "2026-08-14",
            items = listOf(
                "新增全局运行状态条，离开对话后也能查看进度并快速返回",
                "支持执行中追加要求，以及把下一条消息排队发送",
                "支持在对话内处理 Hermes 的操作审批与澄清问题",
                "新增完成结果卡，集中展示本轮摘要与生成产物",
                "系统通知可直达对应 Profile、对话或任务页面",
                "修复附件选择器显示暂不支持的 PDF 类型，并恢复固定头像体验",
            ),
        )
        ChangeLogEntry(
            version = "1.0.0",
            date = "首个正式版本",
            items = listOf(
                "远程连接自部署 Hermes Agent，管理多 Profile 与会话",
                "支持项目空间、Markdown 编辑、图片预览与聊天产物",
                "支持 Cron 定时任务、系统通知、语音输入和深色模式",
                "提供模型、技能工具、审批、记忆与上下文设置",
            ),
        )
        }
    }
}

@Composable
private fun ChangeLogEntry(
    version: String,
    date: String,
    items: List<String>,
    current: Boolean = false,
) {
    GlassPanel(
        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
        shape = RoundedCornerShape(17.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                HermesMulticolorIcon(
                    if (current) HermesIconKind.CHECK_CIRCLE else HermesIconKind.HISTORY,
                    contentDescription = null,
                    iconSize = 21.dp,
                )
                Column(Modifier.padding(start = 9.dp)) {
                    Text("版本 $version${if (current) " · 当前版本" else ""}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(date, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            items.forEach { item ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    Text("•", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Text(item, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 8.dp).weight(1f))
                }
            }
        }
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
        Row(modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(start = 8.dp, end = 8.dp, top = 5.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { HermesMulticolorIcon(HermesIconKind.BACK, contentDescription = "返回") }
            Column(modifier = Modifier.padding(start = 4.dp)) {
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
private fun SwitchSettingRow(title: String, subtitle: String, checked: Boolean, enabled: Boolean = true, onCheckedChange: (Boolean) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(9.dp))
                .clickable(enabled = enabled) { onCheckedChange(!checked) }.padding(horizontal = 11.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
            HermesSwitch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
        }
        SettingsRowDivider()
    }
}

@Composable
private fun SettingsChoiceRow(title: String, value: String, enabled: Boolean = true, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(9.dp))
                .clickable(enabled = enabled, onClick = onClick).padding(horizontal = 11.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            HermesMulticolorIcon(HermesIconKind.CHEVRON_RIGHT, null, modifier = Modifier.padding(start = 7.dp), iconSize = 13.dp)
        }
        SettingsRowDivider()
    }
}

@Composable
private fun SettingsActionRow(title: String, subtitle: String, enabled: Boolean = true, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(9.dp))
                .clickable(enabled = enabled, onClick = onClick).padding(horizontal = 11.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
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

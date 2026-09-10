package com.qingyu.hermescompanion.ui.screen

import com.qingyu.hermescompanion.i18n.uiText
import com.qingyu.hermescompanion.R


import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.imePadding

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import com.qingyu.hermescompanion.ui.component.HermesMascot
import com.qingyu.hermescompanion.ui.component.MascotMotion
import com.qingyu.hermescompanion.ui.component.HermesButton
import com.qingyu.hermescompanion.ui.component.LocalReduceMotion
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.qingyu.hermescompanion.ui.component.HermesAlertDialog as AlertDialog
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.qingyu.hermescompanion.model.VoicePhase
import com.qingyu.hermescompanion.ui.AppUiState
import com.qingyu.hermescompanion.ui.voiceRecognitionLanguage
import com.qingyu.hermescompanion.ui.component.HermesIconKind
import com.qingyu.hermescompanion.ui.component.HermesMulticolorIcon
import com.qingyu.hermescompanion.ui.component.HermesStatusIcon
import com.qingyu.hermescompanion.ui.component.HermesStatusKind
import com.qingyu.hermescompanion.ui.theme.HermesSpacing
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun VoiceConversationScreen(
    state: AppUiState,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onCancelListening: () -> Unit,
    onInterruptPlayback: () -> Unit,
    onSystemResult: (String) -> Unit,
    onUnavailable: () -> Unit,
    onOpenGatewaySettings: () -> Unit,
    onFastReplyChange: (Boolean) -> Unit = {},
    onNoiseSensitivityChange: (String) -> Unit = {},
) {
    com.qingyu.hermescompanion.ui.component.KeepScreenAwake()
    val context = LocalContext.current
    val voice = state.voiceConversation
    var showNoiseOptions by remember { mutableStateOf(false) }
    val usesSystemRecognition = state.voicePreferences.engine == "system" ||
        (state.voicePreferences.engine == "automatic" && voice.agentSttAvailable == false)
    val animatedLevel by animateFloatAsState(
        targetValue = if (voice.phase == VoicePhase.LISTENING) voice.inputLevel else 0f,
        label = "voice-input-level",
    )
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) onStartListening() else onUnavailable()
    }
    val systemLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
                ?.takeIf(String::isNotBlank)
                ?.let(onSystemResult)
        } else onCancelListening()
    }
    val useSystemRecognition = {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                voiceRecognitionLanguage(state.voicePreferences.language, state.voicePreferences.transcriptScript),
            )
            putExtra(RecognizerIntent.EXTRA_PROMPT, uiText(R.string.ui_1318, "对 Hermes 说话，说完自动发送"))
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1_250L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1_250L)
        }
        try {
            systemLauncher.launch(intent)
        } catch (_: ActivityNotFoundException) {
            onUnavailable()
        } catch (_: SecurityException) {
            onUnavailable()
        }
    }
    val startAgentRecognition = {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            onStartListening()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
    val latestCancel by rememberUpdatedState(onCancelListening)
    DisposableEffect(Unit) { onDispose { latestCancel() } }
    LaunchedEffect(voice.listenRequest) {
        if (voice.listenRequest > 0 && voice.active && voice.phase == VoicePhase.IDLE) {
            if (state.voicePreferences.engine == "system" ||
                (state.voicePreferences.engine == "automatic" && voice.agentSttAvailable == false)) useSystemRecognition()
            else startAgentRecognition()
        }
    }
    Column(Modifier.fillMaxSize().padding(contentPadding).navigationBarsPadding().imePadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) { HermesMulticolorIcon(HermesIconKind.BACK, uiText(R.string.ui_0554, "返回")) }
            Column(Modifier.weight(1f).padding(start = 4.dp)) {
                Text(uiText(R.string.ui_0659, "连续语音"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HermesStatusIcon(if (voice.phase in setOf(VoicePhase.THINKING, VoicePhase.TRANSCRIBING)) HermesStatusKind.BUSY else HermesStatusKind.CONNECTED)
                    Text(
                        voice.provider.takeIf(String::isNotBlank)?.let { uiText(R.string.ui_1319, "语音服务 · %1\$s", it) } ?: uiText(R.string.ui_1320, "Agent 语音优先，手机服务兜底"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 5.dp),
                    )
                }
            }
        }

        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = HermesSpacing.page, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                when (voice.phase) {
                    VoicePhase.LISTENING -> uiText(R.string.ui_1321, "我在听")
                    VoicePhase.TRANSCRIBING -> uiText(R.string.ui_0692, "正在识别")
                    VoicePhase.THINKING -> uiText(R.string.ui_0374, "Hermes 正在思考")
                    VoicePhase.SPEAKING -> uiText(R.string.ui_0337, "Hermes 正在回答")
                    VoicePhase.ERROR -> uiText(R.string.ui_1322, "需要你的处理")
                    VoicePhase.IDLE -> uiText(R.string.ui_1323, "准备好了")
                },
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Text(
                when (voice.phase) {
                    VoicePhase.LISTENING -> voice.message.ifBlank { uiText(R.string.ui_1324, "说完停顿后自动发送") }
                    VoicePhase.TRANSCRIBING -> uiText(R.string.ui_1325, "正在把语音转换为文字")
                    VoicePhase.THINKING -> uiText(R.string.ui_1326, "已发送，正在生成回答")
                    VoicePhase.SPEAKING -> uiText(R.string.ui_1327, "回答正在播放，可随时打断")
                    VoicePhase.ERROR -> voice.message.ifBlank { uiText(R.string.ui_1328, "语音服务暂时不可用") }
                    VoicePhase.IDLE -> voice.message.ifBlank { uiText(R.string.ui_1329, "点按下方按钮开始说话") }
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (voice.phase == VoicePhase.ERROR) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { onFastReplyChange(!state.voicePreferences.fastReply) },
                    enabled = voice.phase in setOf(VoicePhase.IDLE, VoicePhase.ERROR)) {
                    Text(if (state.voicePreferences.fastReply) uiText(R.string.ui_1330, "快速回答 ✓") else uiText(R.string.ui_1331, "深入思考"))
                }
                if (!usesSystemRecognition) TextButton(onClick = { showNoiseOptions = true },
                    enabled = voice.phase in setOf(VoicePhase.IDLE, VoicePhase.ERROR, VoicePhase.LISTENING)) {
                    Text(uiText(R.string.ui_1332, "收音：%1\$s", voiceSensitivityLabel(state.voicePreferences.noiseSensitivity)))
                }
            }
            HermesMascot(when (voice.phase) {
                VoicePhase.LISTENING -> MascotMotion.LISTENING
                VoicePhase.TRANSCRIBING, VoicePhase.THINKING -> MascotMotion.WORKING
                else -> MascotMotion.IDLE_HALF
            }, Modifier.padding(top = 12.dp).size(152.dp, 210.dp))
            VoiceControlOrb(
                phase = voice.phase,
                inputLevel = animatedLevel,
                modifier = Modifier.padding(top = 18.dp),
                onClick = {
                    when (voice.phase) {
                        VoicePhase.LISTENING -> onStopListening()
                        VoicePhase.SPEAKING -> onInterruptPlayback()
                        else -> if (
                            state.voicePreferences.engine == "system" ||
                            (state.voicePreferences.engine == "automatic" && voice.agentSttAvailable == false)
                        ) {
                            useSystemRecognition()
                        } else startAgentRecognition()
                    }
                },
            )
            VoiceCoreStatus(
                phase = voice.phase,
                continuous = state.voicePreferences.continuous,
                modifier = Modifier.padding(top = 17.dp),
            )

            if (voice.phase == VoicePhase.LISTENING) {
                TextButton(colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer), onClick = onCancelListening, modifier = Modifier.padding(top = 4.dp)) { Text(uiText(R.string.ui_1333, "取消录音")) }
            }
            if (voice.agentSttAvailable == false && state.voicePreferences.engine != "system") {
                TextButton(colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer), onClick = useSystemRecognition, modifier = Modifier.padding(top = 4.dp)) { Text(uiText(R.string.ui_1334, "改用手机系统识别")) }
            }
            if (voice.requiresAgentUpdate) {
                TextButton(colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer), onClick = onOpenGatewaySettings, modifier = Modifier.padding(top = 4.dp)) {
                    Text(uiText(R.string.ui_1335, "检查并更新 Hermes Agent"))
                }
            }
        }

    }
    if (showNoiseOptions) AlertDialog(
        onDismissRequest = { showNoiseOptions = false },
        title = { Text(uiText(R.string.ui_1018, "收音环境")) },
        text = { Column {
            voiceSensitivityOptions.forEach { (value, label) ->
                TextButton(onClick = { onNoiseSensitivityChange(value); showNoiseOptions = false }, modifier = Modifier.fillMaxWidth()) {
                    Text(label + if (state.voicePreferences.noiseSensitivity == value) " ✓" else "")
                }
            }
        } },
        confirmButton = { TextButton(onClick = { showNoiseOptions = false }) { Text(uiText(R.string.ui_0196, "关闭")) } },
    )
}

@Composable
private fun VoiceControlOrb(
    phase: VoicePhase,
    inputLevel: Float,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val enabled = phase !in setOf(VoicePhase.TRANSCRIBING, VoicePhase.THINKING)
    val label = when (phase) {
        VoicePhase.LISTENING -> uiText(R.string.ui_1336, "结束录音")
        VoicePhase.SPEAKING -> uiText(R.string.ui_1337, "打断回答")
        VoicePhase.ERROR -> uiText(R.string.ui_1338, "重新开始")
        VoicePhase.THINKING -> uiText(R.string.ui_0676, "正在思考")
        VoicePhase.TRANSCRIBING -> uiText(R.string.ui_0692, "正在识别")
        VoicePhase.IDLE -> uiText(R.string.ui_1339, "开始说话")
    }
    val reduce = LocalReduceMotion.current
    HermesButton(onClick, modifier = modifier.widthIn(min = 192.dp).height(56.dp), enabled = enabled) {
        val ink = MaterialTheme.colorScheme.onPrimary
        Canvas(Modifier.size(24.dp).padding(end = 4.dp)) {
            val level = if (phase == VoicePhase.LISTENING && !reduce) inputLevel.coerceIn(0f, 1f) else 0f
            repeat(5) { i ->
                val middle = 1f - kotlin.math.abs(i - 2) / 3f
                val h = size.height * (.22f + middle * (.28f + .45f * level))
                val x = size.width * (.12f + i * .19f)
                drawLine(ink, Offset(x, (size.height - h) / 2), Offset(x, (size.height + h) / 2), 2.dp.toPx(), androidx.compose.ui.graphics.StrokeCap.Round)
            }
        }
        Text(label, Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun VoiceEnergyGlyph(
    phase: VoicePhase,
    inputLevel: Float,
    motion: Float,
) {
    Canvas(Modifier.size(72.dp)) {
        val base = floatArrayOf(0.34f, 0.52f, 0.72f, 0.94f, 0.72f, 0.52f, 0.34f)
        val gains = floatArrayOf(0.55f, 0.76f, 0.92f, 1f, 0.88f, 0.7f, 0.5f)
        val barWidth = 6.dp.toPx()
        val gap = 3.3.dp.toPx()
        val totalWidth = barWidth * base.size + gap * (base.size - 1)
        val maxHeight = 49.dp.toPx()
        val minHeight = 7.dp.toPx()
        val startX = (size.width - totalWidth) / 2f

        base.indices.forEach { index ->
            val phaseWave = ((sin((motion + index * 0.13f) * PI.toFloat() * 2f) + 1f) / 2f)
            val heightFactor = when (phase) {
                VoicePhase.LISTENING -> 0.1f + inputLevel * (0.34f + gains[index] * 0.58f)
                VoicePhase.TRANSCRIBING -> 0.3f + phaseWave * 0.48f * gains[index]
                VoicePhase.THINKING -> 0.28f + phaseWave * 0.4f * gains[base.lastIndex - index]
                VoicePhase.SPEAKING -> 0.32f + phaseWave * 0.58f * gains[index]
                VoicePhase.ERROR -> 0.2f + base[index] * 0.2f
                VoicePhase.IDLE -> base[index] * 0.64f
            }.coerceIn(0.1f, 1f)
            val barHeight = (maxHeight * heightFactor).coerceAtLeast(minHeight)
            val left = startX + index * (barWidth + gap)
            val top = center.y - barHeight / 2f
            val radius = barWidth / 2f

            drawRoundRect(
                color = Color.White.copy(alpha = 0.16f),
                topLeft = Offset(left - 1.dp.toPx(), top - 1.dp.toPx()),
                size = Size(barWidth + 2.dp.toPx(), barHeight + 2.dp.toPx()),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius + 1.dp.toPx()),
            )
            drawRoundRect(
                brush = Brush.verticalGradient(
                    listOf(Color.White, Color(0xFFD8FBFF), Color.White.copy(alpha = 0.9f)),
                    startY = top,
                    endY = top + barHeight,
                ),
                topLeft = Offset(left, top),
                size = Size(barWidth, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius),
            )
        }
    }
}

@Composable
private fun VoiceCoreStatus(
    phase: VoicePhase,
    continuous: Boolean,
    modifier: Modifier = Modifier,
) {
    val accent = when (phase) {
        VoicePhase.LISTENING -> MaterialTheme.colorScheme.tertiary
        VoicePhase.SPEAKING -> MaterialTheme.colorScheme.secondary
        VoicePhase.ERROR -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.primary
    }
    val status = when (phase) {
        VoicePhase.LISTENING -> uiText(R.string.ui_1340, "聆听中 · 声音实时响应")
        VoicePhase.TRANSCRIBING -> uiText(R.string.ui_0330, "正在识别语音")
        VoicePhase.THINKING -> uiText(R.string.ui_1341, "思考中 · 正在生成回答")
        VoicePhase.SPEAKING -> uiText(R.string.ui_1342, "正在播放回复")
        VoicePhase.ERROR -> uiText(R.string.ui_1343, "语音服务需要处理")
        VoicePhase.IDLE -> if (continuous) uiText(R.string.ui_1344, "连续对话已开启") else uiText(R.string.ui_1345, "准备就绪")
    }
    val action = when (phase) {
        VoicePhase.LISTENING -> uiText(R.string.ui_1346, "停顿自动发送 · 也可点按提前结束")
        VoicePhase.SPEAKING -> uiText(R.string.ui_1347, "点按语音核心可打断")
        VoicePhase.TRANSCRIBING, VoicePhase.THINKING -> uiText(R.string.ui_1348, "请稍候")
        VoicePhase.ERROR -> uiText(R.string.ui_1349, "根据上方提示处理后重试")
        VoicePhase.IDLE -> uiText(R.string.ui_1329, "点按下方按钮开始说话")
    }
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(7.dp).clip(CircleShape).background(accent))
            Text(
                status,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        Text(
            action,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 10.dp),
        )
    }
}

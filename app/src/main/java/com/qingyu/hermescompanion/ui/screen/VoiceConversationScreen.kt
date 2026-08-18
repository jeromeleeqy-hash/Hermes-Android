package com.qingyu.hermescompanion.ui.screen

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
) {
    val context = LocalContext.current
    val voice = state.voiceConversation
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
        }
    }
    val useSystemRecognition = {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                voiceRecognitionLanguage(state.voicePreferences.language, state.voicePreferences.transcriptScript),
            )
            putExtra(RecognizerIntent.EXTRA_PROMPT, "请对 Hermes 说话")
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
    Column(Modifier.fillMaxSize().padding(contentPadding)) {
        Row(
            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) { HermesMulticolorIcon(HermesIconKind.BACK, "返回") }
            Column(Modifier.weight(1f).padding(start = 4.dp)) {
                Text("连续语音", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HermesStatusIcon(if (voice.phase in setOf(VoicePhase.THINKING, VoicePhase.TRANSCRIBING)) HermesStatusKind.BUSY else HermesStatusKind.CONNECTED)
                    Text(
                        voice.provider.takeIf(String::isNotBlank)?.let { "语音服务 · $it" } ?: "Agent 语音优先，手机服务兜底",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 5.dp),
                    )
                }
            }
        }

        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().offset(y = (-30).dp)
                .padding(horizontal = HermesSpacing.page),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                when (voice.phase) {
                    VoicePhase.LISTENING -> "我在听"
                    VoicePhase.TRANSCRIBING -> "正在识别"
                    VoicePhase.THINKING -> "Hermes 正在思考"
                    VoicePhase.SPEAKING -> "Hermes 正在回答"
                    VoicePhase.ERROR -> "需要你的处理"
                    VoicePhase.IDLE -> "准备好了"
                },
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Text(
                when (voice.phase) {
                    VoicePhase.LISTENING -> "自然说话即可，声音会实时响应"
                    VoicePhase.TRANSCRIBING -> "正在把语音转换为文字"
                    VoicePhase.THINKING -> "已发送，正在生成回答"
                    VoicePhase.SPEAKING -> "回答正在播放，可随时打断"
                    VoicePhase.ERROR -> voice.message.ifBlank { "语音服务暂时不可用" }
                    VoicePhase.IDLE -> voice.message.ifBlank { "点按语音核心开始说话" }
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (voice.phase == VoicePhase.ERROR) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )

            VoiceControlOrb(
                phase = voice.phase,
                inputLevel = animatedLevel,
                modifier = Modifier.padding(top = 26.dp),
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
                TextButton(onClick = onCancelListening, modifier = Modifier.padding(top = 4.dp)) { Text("取消录音") }
            }
            if (voice.agentSttAvailable == false && state.voicePreferences.engine != "system") {
                TextButton(onClick = useSystemRecognition, modifier = Modifier.padding(top = 4.dp)) { Text("改用手机系统识别") }
            }
            if (voice.requiresAgentUpdate) {
                TextButton(onClick = onOpenGatewaySettings, modifier = Modifier.padding(top = 4.dp)) {
                    Text("检查并更新 Hermes Agent")
                }
            }
        }

    }
}

@Composable
private fun VoiceControlOrb(
    phase: VoicePhase,
    inputLevel: Float,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val enabled = phase !in setOf(VoicePhase.TRANSCRIBING, VoicePhase.THINKING)
    val leading = when (phase) {
        VoicePhase.LISTENING -> tertiary
        VoicePhase.SPEAKING -> secondary
        VoicePhase.ERROR -> MaterialTheme.colorScheme.error
        else -> primary
    }
    val trailing = when (phase) {
        VoicePhase.LISTENING -> primary
        VoicePhase.SPEAKING -> tertiary
        VoicePhase.ERROR -> MaterialTheme.colorScheme.errorContainer
        else -> tertiary
    }
    val label = when (phase) {
        VoicePhase.LISTENING -> "结束录音并发送"
        VoicePhase.SPEAKING -> "停止播放"
        VoicePhase.TRANSCRIBING -> "正在识别"
        VoicePhase.THINKING -> "Hermes 正在思考"
        else -> "开始说话"
    }
    val motionTransition = rememberInfiniteTransition(label = "voice-core-motion")
    val breathing by motionTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "voice-core-breathing",
    )
    val waveformMotion by motionTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500),
            repeatMode = RepeatMode.Restart,
        ),
        label = "voice-core-waveform",
    )
    val reactive = if (phase == VoicePhase.LISTENING) inputLevel.coerceIn(0f, 1f) else 0f

    Box(modifier.size(196.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val haloRadius = 91.dp.toPx() + reactive * 3.5.dp.toPx()
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        leading.copy(alpha = 0.12f + reactive * 0.08f),
                        trailing.copy(alpha = 0.06f + breathing * 0.035f),
                        Color.Transparent,
                    ),
                    radius = 101.dp.toPx(),
                ),
                radius = 98.dp.toPx(),
            )
            drawCircle(
                brush = Brush.sweepGradient(
                    listOf(
                        leading.copy(alpha = 0.72f),
                        trailing.copy(alpha = 0.88f),
                        leading.copy(alpha = 0.5f),
                        leading.copy(alpha = 0.72f),
                    ),
                ),
                radius = haloRadius,
                style = Stroke(width = (1.45f + reactive * 0.65f).dp.toPx()),
            )
        }
        Box(
            Modifier.size(172.dp)
                .shadow(20.dp, CircleShape)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                            leading.copy(alpha = 0.12f + reactive * 0.08f),
                            trailing.copy(alpha = 0.08f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                        ),
                    ),
                ),
        )
        Canvas(Modifier.fillMaxSize()) {
            listOf(64.dp to 0.34f, 74.dp to 0.23f, 82.dp to 0.14f).forEach { (radius, alpha) ->
                drawCircle(
                    color = Color.White.copy(alpha = alpha + reactive * 0.12f),
                    radius = radius.toPx(),
                    style = Stroke(width = 1.dp.toPx()),
                )
            }
        }
        Box(
            modifier = Modifier.size(122.dp)
                .shadow(12.dp, CircleShape)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            trailing.copy(alpha = 0.94f),
                            leading,
                            leading.copy(alpha = 0.96f),
                            trailing.copy(alpha = 0.9f),
                        ),
                    ),
                )
                .alpha(if (enabled) 1f else 0.84f)
                .semantics { contentDescription = label }
                .clickable(enabled = enabled, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            VoiceEnergyGlyph(
                phase = phase,
                inputLevel = reactive,
                motion = waveformMotion,
            )
        }
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
        VoicePhase.LISTENING -> "聆听中 · 声音实时响应"
        VoicePhase.TRANSCRIBING -> "正在识别语音"
        VoicePhase.THINKING -> "思考中 · 正在生成回答"
        VoicePhase.SPEAKING -> "正在播放回复"
        VoicePhase.ERROR -> "语音服务需要处理"
        VoicePhase.IDLE -> if (continuous) "连续对话已开启" else "准备就绪"
    }
    val action = when (phase) {
        VoicePhase.LISTENING -> "点按结束并发送"
        VoicePhase.SPEAKING -> "点按语音核心可打断"
        VoicePhase.TRANSCRIBING, VoicePhase.THINKING -> "请稍候"
        VoicePhase.ERROR -> "根据上方提示处理后重试"
        VoicePhase.IDLE -> "点按语音核心开始说话"
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

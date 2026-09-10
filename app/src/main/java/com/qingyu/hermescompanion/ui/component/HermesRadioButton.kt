package com.qingyu.hermescompanion.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.qingyu.hermescompanion.ui.theme.HermesSkin

@Composable
fun HermesRadioButton(selected: Boolean, onClick: (() -> Unit)?, modifier: Modifier = Modifier, enabled: Boolean = true) {
    if (!HermesSkin.current.glass) {
        androidx.compose.material3.RadioButton(selected, onClick, modifier, enabled)
        return
    }
    val colors = MaterialTheme.colorScheme
    val hit = if (onClick == null) modifier else modifier.selectable(selected, enabled = enabled, role = Role.RadioButton, onClick = onClick)
    Box(hit.size(48.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(22.dp)) {
            val alpha = if (enabled) 1f else .38f
            drawCircle(Brush.linearGradient(listOf(colors.surface.copy(alpha = alpha), colors.primaryContainer.copy(alpha = .6f * alpha))))
            drawCircle((if (selected) colors.primary else colors.onSurfaceVariant).copy(alpha = alpha), style = Stroke(1.6.dp.toPx()))
            drawArc(Color.White.copy(alpha = .6f * alpha), 205f, 80f, false, topLeft = Offset(1.dp.toPx(), 1.dp.toPx()),
                size = size.copy(width = size.width - 2.dp.toPx(), height = size.height - 2.dp.toPx()), style = Stroke(.6.dp.toPx()))
            if (selected) drawCircle(colors.primary.copy(alpha = alpha), radius = 5.5.dp.toPx())
        }
    }
}

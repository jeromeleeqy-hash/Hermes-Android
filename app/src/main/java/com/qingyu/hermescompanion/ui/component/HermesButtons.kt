package com.qingyu.hermescompanion.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.qingyu.hermescompanion.ui.SkinMode
import com.qingyu.hermescompanion.ui.theme.HermesSkin

@Composable
private fun actionShape(): Shape = RoundedCornerShape(HermesSkin.current.controlRadius.dp)

@Composable
fun HermesButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    shape: Shape = actionShape(), colors: ButtonColors? = null,
    elevation: ButtonElevation? = ButtonDefaults.buttonElevation(), border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null, content: @Composable RowScope.() -> Unit) {
    val glass = HermesSkin.current.glass && colors == null
    val tones = glassControlColors()
    val resolved = colors ?: if (glass) ButtonDefaults.buttonColors(containerColor = Color.Transparent,
        contentColor = tones.onAction, disabledContainerColor = tones.field,
        disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = .38f)) else ButtonDefaults.buttonColors()
    androidx.compose.material3.Button(onClick,modifier.heightIn(min=48.dp)
        .then(if (glass && enabled) Modifier.background(Brush.verticalGradient(listOf(tones.actionTop, tones.actionBottom)), shape)
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .55f), shape) else Modifier),enabled,shape,resolved,
        if (glass) null else elevation,border,contentPadding,interactionSource,content)
}

@Composable
fun HermesOutlinedButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    shape: Shape = actionShape(), colors: ButtonColors? = null,
    elevation: ButtonElevation? = null, border: BorderStroke? = ButtonDefaults.outlinedButtonBorder(enabled),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null, content: @Composable RowScope.() -> Unit) {
    val glass = HermesSkin.current.glass && (colors == null || colors.containerColor == Color.Transparent)
    val tones = glassControlColors()
    val resolved = colors ?: ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
    androidx.compose.material3.OutlinedButton(onClick,modifier.heightIn(min=48.dp)
        .then(if (glass && enabled) Modifier.background(tones.secondary, shape) else Modifier),enabled,shape,resolved,elevation,
        if (glass) BorderStroke(1.dp, tones.edge.copy(alpha = if (enabled) 1f else .45f)) else border,
        contentPadding,interactionSource,content)
}

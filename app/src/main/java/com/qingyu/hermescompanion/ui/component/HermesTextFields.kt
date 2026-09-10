package com.qingyu.hermescompanion.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.qingyu.hermescompanion.ui.theme.HermesSkin

/** Filled input surfaces remain identifiable inside translucent sheets and white cards. */
internal data class GlassControlColors(val field: Color, val edge: Color, val secondary: Color,
    val actionTop: Color, val actionBottom: Color, val onAction: Color)

@Composable
internal fun glassControlColors(): GlassControlColors =
    if (MaterialTheme.colorScheme.background.luminance() < .5f) GlassControlColors(
        Color(0xFF27394D), Color(0xFF8DA5BF), Color(0xFF30445B),
        Color(0xFFAED0F8), Color(0xFF80AFE3), Color(0xFF102C4D),
    ) else GlassControlColors(
        Color(0xFFE6EDF6), Color(0xFF70859E), Color(0xFFDEE9F5),
        Color(0xFF426F9F), Color(0xFF2C5687), Color.White,
    )

@Composable
fun Modifier.hermesInputWell(shape: Shape = MaterialTheme.shapes.small, focused: Boolean = false): Modifier {
    if (!HermesSkin.current.glass) return this.hermesWell(shape)
    val tones = glassControlColors()
    return this.background(tones.field, shape)
        .border(if (focused) 2.dp else 1.dp, if (focused) MaterialTheme.colorScheme.primary else tones.edge, shape)
        .clip(shape)
}

/** One input contract for settings, task forms, workspace paths and prompt editing. */
@Composable
fun HermesOutlinedTextField(
    value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier,
    enabled: Boolean = true, readOnly: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
    label: (@Composable () -> Unit)? = null, placeholder: (@Composable () -> Unit)? = null,
    leadingIcon: (@Composable () -> Unit)? = null, trailingIcon: (@Composable () -> Unit)? = null,
    supportingText: (@Composable () -> Unit)? = null, isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false, maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE, minLines: Int = 1,
    interactionSource: MutableInteractionSource? = null, shape: Shape = MaterialTheme.shapes.small,
) {
    val palette = MaterialTheme.colorScheme
    val glass = HermesSkin.current.glass
    val tones = glassControlColors()
    val colors = if (glass) OutlinedTextFieldDefaults.colors(
        focusedContainerColor = tones.field, unfocusedContainerColor = tones.field,
        disabledContainerColor = palette.surfaceContainer, errorContainerColor = palette.errorContainer.copy(alpha = .25f),
        focusedBorderColor = palette.primary, unfocusedBorderColor = tones.edge,
        disabledBorderColor = tones.edge.copy(alpha = .45f),
        focusedTextColor = palette.onSurface, unfocusedTextColor = palette.onSurface,
        focusedPlaceholderColor = palette.onSurfaceVariant, unfocusedPlaceholderColor = palette.onSurfaceVariant,
        focusedLabelColor = palette.primary, unfocusedLabelColor = palette.onSurfaceVariant,
        cursorColor = palette.primary,
    ) else OutlinedTextFieldDefaults.colors()
    androidx.compose.material3.OutlinedTextField(
        value = value, onValueChange = onValueChange, modifier = modifier, enabled = enabled, readOnly = readOnly,
        textStyle = textStyle, label = label, placeholder = placeholder, leadingIcon = leadingIcon,
        trailingIcon = trailingIcon, supportingText = supportingText, isError = isError,
        visualTransformation = visualTransformation, keyboardOptions = keyboardOptions, keyboardActions = keyboardActions,
        singleLine = singleLine, maxLines = maxLines, minLines = minLines, interactionSource = interactionSource,
        shape = shape, colors = colors,
    )
}

package com.qingyu.hermescompanion.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.qingyu.hermescompanion.ui.SkinMode
import com.qingyu.hermescompanion.ui.theme.hermesColorScheme

/** Reflect the three actual home layouts, including the five-destination dock. */
@Composable
fun SkinPreview(mode: SkinMode) {
    val colors = hermesColorScheme(mode, false)
    Canvas(Modifier.size(52.dp, 68.dp)) {
        val w = size.width; val h = size.height
        fun block(x: Float, y: Float, width: Float, height: Float, color: Color, radius: Float = 2f) =
            drawRoundRect(color, Offset(w*x,h*y), Size(w*width,h*height), CornerRadius(radius.dp.toPx()))
        block(0f,0f,1f,1f,colors.background,5f)
        block(.12f,.1f,.32f,.025f,colors.onSurface)
        when(mode) {
            SkinMode.CLEAN -> {
                block(.12f,.27f,.34f,.04f,colors.onSurface); block(.12f,.35f,.30f,.025f,colors.outline)
                block(.12f,.46f,.34f,.09f,colors.primary,4f); block(.60f,.22f,.24f,.33f,colors.primaryContainer,8f)
                block(.10f,.62f,.37f,.20f,colors.surface,4f); block(.53f,.62f,.37f,.20f,colors.surface,4f)
            }
            SkinMode.GLASS -> {
                block(.25f,.23f,.5f,.025f,colors.onSurface); block(.39f,.30f,.22f,.26f,colors.primaryContainer,8f)
                block(.13f,.59f,.74f,.08f,colors.primaryContainer.copy(alpha=.65f),4f)
                block(.10f,.71f,.80f,.13f,colors.surface,3f)
            }
            SkinMode.PAPER -> {
                block(.12f,.28f,.43f,.03f,colors.onSurface); block(.71f,.22f,.16f,.17f,colors.outlineVariant,5f)
                block(.12f,.43f,.34f,.08f,colors.primary)
                repeat(3) { block(.12f,.6f+it*.09f,.76f,.009f,colors.outlineVariant,0f) }
            }
        }
        block(.10f,.88f,.80f,.07f,if(mode==SkinMode.GLASS) colors.primaryContainer else colors.surface,4f)
        repeat(5) { i -> drawCircle(if(i==0) colors.primary else colors.outlineVariant,1.1.dp.toPx(),Offset(w*(.19f+i*.155f),h*.915f)) }
    }
}

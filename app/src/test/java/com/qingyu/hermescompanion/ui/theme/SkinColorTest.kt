package com.qingyu.hermescompanion.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.qingyu.hermescompanion.ui.SkinMode
import org.junit.Test
import org.junit.Assert.*

class SkinColorTest {
    private fun contrast(a: Color, b: Color): Float =
        (maxOf(a.luminance(),b.luminance())+.05f)/(minOf(a.luminance(),b.luminance())+.05f)
    @Test fun normalTextAndSelectedControlsStayReadableAcrossAllSixPalettes() {
        for (mode in SkinMode.entries) for (dark in listOf(false,true)) {
            val c = hermesColorScheme(mode,dark)
            for ((fg,bg) in listOf(c.onSurface to c.surface, c.onSurfaceVariant to c.background,
                c.onPrimary to c.primary, c.onPrimaryContainer to c.primaryContainer)) {
                assertTrue("$mode dark=$dark contrast=${contrast(fg,bg)}",contrast(fg,bg)>=4.5f)
            }
            assertEquals(1f,c.surface.alpha,0f)
            assertEquals(1f,c.background.alpha,0f)
        }
    }
}

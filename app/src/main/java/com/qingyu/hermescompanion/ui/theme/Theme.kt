package com.qingyu.hermescompanion.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.qingyu.hermescompanion.ui.SkinMode
import com.qingyu.hermescompanion.ui.ThemeMode

@Immutable
data class HermesSkinTokens(
    val mode: SkinMode,
    val glass: Boolean,
    val panelAlpha: Float,
    val chromeAlpha: Float,
    val shadowElevation: Int,
    val borderAlpha: Float,
    val ambientStrength: Float,
    val panelRadius: Int,
    val controlRadius: Int,
    val menuRadius: Int,
    val selectedFillAlpha: Float,
    val iconWellAlpha: Float,
    val sheetRadius: Int = 24,
    val dockRadius: Int = 28,
)

private val WarmSkin = HermesSkinTokens(
    mode = SkinMode.CLEAN, glass = false, panelAlpha = 1f, chromeAlpha = 1f,
    shadowElevation = 0, borderAlpha = 0f, ambientStrength = 0f,
    panelRadius = 22, controlRadius = 16, menuRadius = 20,
    selectedFillAlpha = 1f, iconWellAlpha = 1f,
)
private val LiquidGlassSkin = HermesSkinTokens(
    mode = SkinMode.GLASS, glass = true, panelAlpha = 1f, chromeAlpha = .72f,
    shadowElevation = 0, borderAlpha = .32f, ambientStrength = 0f,
    panelRadius = 20, controlRadius = 12, menuRadius = 16,
    selectedFillAlpha = .64f, iconWellAlpha = .72f,
)
private val PaperSkin = HermesSkinTokens(
    mode = SkinMode.PAPER, glass = false, panelAlpha = 1f, chromeAlpha = 1f,
    shadowElevation = 0, borderAlpha = .20f, ambientStrength = 0f,
    panelRadius = 2, controlRadius = 8, menuRadius = 14,
    selectedFillAlpha = .86f, iconWellAlpha = .72f,
)

private fun skinTokens(mode: SkinMode) = when (mode) {
    SkinMode.CLEAN -> WarmSkin
    SkinMode.GLASS -> LiquidGlassSkin
    SkinMode.PAPER -> PaperSkin
}

val LocalHermesSkin = staticCompositionLocalOf { WarmSkin }

object HermesSkin {
    val current: HermesSkinTokens
        @Composable get() = LocalHermesSkin.current
}

@Immutable
data class HermesExtendedColors(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val warning: Color,
    val onWarning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val infoContainer: Color,
    val onInfoContainer: Color,
    val cyan: Color,
    val cyanContainer: Color,
    val purple: Color,
    val purpleContainer: Color,
)

private val LightExtendedColors = HermesExtendedColors(
    success = Color(0xFF20A67A),
    onSuccess = Color.White,
    successContainer = Color(0xFFE6FAF4),
    onSuccessContainer = Color(0xFF0B5D4D),
    warning = Color(0xFFEDA93A),
    onWarning = Color.White,
    warningContainer = Color(0xFFFFF4DC),
    onWarningContainer = Color(0xFF694300),
    infoContainer = Color(0xFFE6F3FF),
    onInfoContainer = Color(0xFF0078AD),
    cyan = Color(0xFF16A6B6),
    cyanContainer = Color(0xFFE1F6F8),
    purple = Color(0xFF7567E8),
    purpleContainer = Color(0xFFF0EBFF),
)

private val DarkExtendedColors = HermesExtendedColors(
    success = Color(0xFF70D7BE),
    onSuccess = Color(0xFF00382E),
    successContainer = Color(0xFF164E43),
    onSuccessContainer = Color(0xFFB4F1E1),
    warning = Color(0xFFFFC66B),
    onWarning = Color(0xFF4D2D00),
    warningContainer = Color(0xFF5A421D),
    onWarningContainer = Color(0xFFFFE2AF),
    infoContainer = Color(0xFF293B68),
    onInfoContainer = Color(0xFFDCE5FF),
    cyan = Color(0xFF39C6BC),
    cyanContainer = Color(0xFF173D3D),
    purple = Color(0xFFA995FF),
    purpleContainer = Color(0xFF342D5A),
)

val LocalHermesExtendedColors = staticCompositionLocalOf { LightExtendedColors }

object HermesColors {
    val extended: HermesExtendedColors
        @Composable get() = LocalHermesExtendedColors.current
}

object HermesSpacing {
    val xxs = 4.dp
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
    val page = 16.dp
    val densePage = 12.dp
    val minTouchTarget = 48.dp
}

/** Every role is paired with its foreground; no palette inherits blue/purple controls. */
internal fun hermesColorScheme(mode: SkinMode, dark: Boolean): androidx.compose.material3.ColorScheme {
    val paper = mode == SkinMode.PAPER
    val warm = mode == SkinMode.CLEAN
    val accent = when {
        paper && dark -> Color(0xFFE2E0DA)
        paper -> Color(0xFF30332F)
        warm && dark -> Color(0xFFA7CDB7)
        warm -> Color(0xFF3F6B57)
        dark -> Color(0xFFA3C7FF)
        else -> Color(0xFF275FA8)
    }
    val bg = when {
        paper && dark -> Color(0xFF171817)
        paper -> Color(0xFFFAF9F5)
        warm && dark -> Color(0xFF161C19)
        warm -> Color(0xFFF4F6F2)
        dark -> Color(0xFF141820)
        else -> Color(0xFFF4F6FA)
    }
    val surface = when { paper -> bg; dark && warm -> Color(0xFF212B25); dark -> Color(0xFF222933); else -> Color.White }
    val ink = if (dark) Color(0xFFF1F2F0) else Color(0xFF202725)
    val secondaryInk = if (dark) Color(0xFFB8C1BD) else Color(0xFF626E69)
    val line = if (dark) Color(0xFF3C4641) else Color(0xFFDDE3DE)
    val well = when { dark && !paper && !warm -> Color(0xFF2C3949); dark -> Color(0xFF303A35); paper -> Color(0xFFF0F0E9); warm -> Color(0xFFEBF0EA); else -> Color(0xFFECF0F6) }
    val active = when { dark && paper -> Color(0xFF3C403A); dark && warm -> Color(0xFF304C3D); dark -> Color(0xFF284262); paper -> Color(0xFFE9ECE5); warm -> Color(0xFFE1EDE2); else -> Color(0xFFE1ECFB) }
    val scheme = if (dark) darkColorScheme() else lightColorScheme()
    return scheme.copy(
        primary = accent, onPrimary = if (dark) Color(0xFF17261C) else Color.White,
        primaryContainer = active, onPrimaryContainer = accent,
        secondary = accent, onSecondary = if (dark) Color(0xFF17261C) else Color.White,
        secondaryContainer = well, onSecondaryContainer = accent,
        tertiary = accent, onTertiary = if (dark) Color(0xFF17261C) else Color.White,
        tertiaryContainer = active, onTertiaryContainer = accent,
        background = bg, onBackground = ink, surface = surface, onSurface = ink,
        surfaceDim = bg, surfaceBright = surface, surfaceTint = Color.Transparent,
        surfaceContainerLowest = surface, surfaceContainerLow = bg,
        surfaceContainer = well, surfaceContainerHigh = well, surfaceContainerHighest = active,
        surfaceVariant = well, onSurfaceVariant = secondaryInk,
        outline = secondaryInk, outlineVariant = line,
        inverseSurface = ink, inverseOnSurface = bg, inversePrimary = if (dark) accent else active,
        error = if (dark) Color(0xFFFFB4AB) else Color(0xFFAE3F42),
        onError = if (dark) Color(0xFF601410) else Color.White,
        errorContainer = if (dark) Color(0xFF542B2D) else Color(0xFFFBEAEC),
        onErrorContainer = if (dark) Color(0xFFFFDAD6) else Color(0xFF772A2D),
        scrim = Color(0xFF101923),
    )
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun HermesCompanionTheme(
    themeMode: ThemeMode,
    @Suppress("UNUSED_PARAMETER")
    skinMode: SkinMode,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colors = hermesColorScheme(skinMode, darkTheme)
    val skin = skinTokens(skinMode)
    val extended = (if (darkTheme) DarkExtendedColors else LightExtendedColors).copy(
        infoContainer = colors.primaryContainer, onInfoContainer = colors.primary,
        cyan = colors.primary, cyanContainer = colors.primaryContainer,
        purple = colors.primary, purpleContainer = colors.secondaryContainer,
    )
    androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.foundation.LocalIndication provides HermesPressIndication,
        androidx.compose.material3.LocalRippleConfiguration provides null,
        LocalHermesSkin provides skin,
        LocalHermesExtendedColors provides extended,
    ) {
        MaterialTheme(
            colorScheme = colors,
            typography = HermesTypography,
            shapes = Shapes(
                extraSmall = RoundedCornerShape(skin.controlRadius.dp),
                small = RoundedCornerShape(skin.controlRadius.dp),
                medium = RoundedCornerShape(skin.panelRadius.dp),
                large = RoundedCornerShape(skin.panelRadius.dp),
                extraLarge = RoundedCornerShape(skin.menuRadius.dp),
            ),
            content = { androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.material3.LocalContentColor provides colors.onSurface,
            ) { content() } },
        )
    }
}

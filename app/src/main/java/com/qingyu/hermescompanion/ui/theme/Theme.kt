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
    val shadowElevation: Int,
    val borderAlpha: Float,
    val ambientStrength: Float,
)

private val CleanSkin = HermesSkinTokens(
    mode = SkinMode.CLEAN,
    glass = false,
    shadowElevation = 0,
    borderAlpha = 0f,
    ambientStrength = 0f,
)

private val GlassSkin = HermesSkinTokens(
    mode = SkinMode.GLASS,
    glass = true,
    shadowElevation = 4,
    borderAlpha = 0.82f,
    ambientStrength = 0.45f,
)

val LocalHermesSkin = staticCompositionLocalOf { CleanSkin }

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
)

private val LightExtendedColors = HermesExtendedColors(
    success = Color(0xFF168A74),
    onSuccess = Color.White,
    successContainer = Color(0xFFD9F3EC),
    onSuccessContainer = Color(0xFF0B5D4D),
    warning = Color(0xFF9A6411),
    onWarning = Color.White,
    warningContainer = Color(0xFFFFEBC8),
    onWarningContainer = Color(0xFF694300),
    infoContainer = Color(0xFFE3EAFF),
    onInfoContainer = Color(0xFF294684),
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

private val LightColors = lightColorScheme(
    primary = Color(0xFF4B67D1),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE4EBFF),
    onPrimaryContainer = Color(0xFF263E8B),
    secondary = Color(0xFF8063CF),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEFE9FF),
    onSecondaryContainer = Color(0xFF49317F),
    tertiary = Color(0xFFE88A5E),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE8DE),
    onTertiaryContainer = Color(0xFF7B4433),
    background = Color(0xFFF6F7FA),
    onBackground = Color(0xFF20242D),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF20242D),
    surfaceDim = Color(0xFFEEF0F5),
    surfaceBright = Color(0xFFFFFFFF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFAFBFC),
    surfaceContainer = Color(0xFFF4F5F8),
    surfaceContainerHigh = Color(0xFFEFF1F5),
    surfaceContainerHighest = Color(0xFFE9ECF2),
    surfaceVariant = Color(0xFFEEF0F4),
    onSurfaceVariant = Color(0xFF666D7A),
    outline = Color(0xFF9299A5),
    outlineVariant = Color(0xFFDCE0E8),
    error = Color(0xFFBD4D57),
    errorContainer = Color(0xFFFFE4E6),
    onErrorContainer = Color(0xFF6D2630),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFAFC3FF),
    onPrimary = Color(0xFF17306F),
    primaryContainer = Color(0xFF2D437E),
    onPrimaryContainer = Color(0xFFE0E7FF),
    secondary = Color(0xFFD3BFFF),
    onSecondary = Color(0xFF3B276D),
    secondaryContainer = Color(0xFF493A6B),
    onSecondaryContainer = Color(0xFFEDE5FF),
    tertiary = Color(0xFFFFB9A2),
    onTertiary = Color(0xFF5A291C),
    tertiaryContainer = Color(0xFF663B30),
    onTertiaryContainer = Color(0xFFFFDBCF),
    background = Color(0xFF0A0D13),
    onBackground = Color(0xFFF1F3F8),
    surface = Color(0xFF171B24),
    onSurface = Color(0xFFF1F3F8),
    surfaceDim = Color(0xFF0D1017),
    surfaceBright = Color(0xFF303744),
    surfaceContainerLowest = Color(0xFF080B10),
    surfaceContainerLow = Color(0xFF131720),
    surfaceContainer = Color(0xFF1C212C),
    surfaceContainerHigh = Color(0xFF252B37),
    surfaceContainerHighest = Color(0xFF303744),
    surfaceVariant = Color(0xFF282E3A),
    onSurfaceVariant = Color(0xFFD4D9E2),
    outline = Color(0xFFA8B0BE),
    outlineVariant = Color(0xFF4A5362),
    error = Color(0xFFFFB2B8),
    errorContainer = Color(0xFF68282E),
    onErrorContainer = Color(0xFFFFDADC),
)

private val HermesShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

@Composable
fun HermesCompanionTheme(
    themeMode: ThemeMode,
    skinMode: SkinMode,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    androidx.compose.runtime.CompositionLocalProvider(
        LocalHermesSkin provides if (skinMode == SkinMode.GLASS) GlassSkin else CleanSkin,
        LocalHermesExtendedColors provides if (darkTheme) DarkExtendedColors else LightExtendedColors,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = HermesTypography,
            shapes = HermesShapes,
            content = content,
        )
    }
}

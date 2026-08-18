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
)

/**
 * Hermes now ships one focused interface language.  The token name is kept
 * compatible with the old preference model, but every screen receives these
 * Telegram-inspired office tokens regardless of the legacy skin value.
 */
private val OfficeSkin = HermesSkinTokens(
    mode = SkinMode.CLEAN,
    glass = false,
    panelAlpha = 1f,
    chromeAlpha = 1f,
    shadowElevation = 0,
    borderAlpha = 0f,
    ambientStrength = 0f,
    panelRadius = 18,
    controlRadius = 12,
    menuRadius = 16,
    selectedFillAlpha = 1f,
    iconWellAlpha = 1f,
)

val LocalHermesSkin = staticCompositionLocalOf { OfficeSkin }

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
    successContainer = Color(0xFFE3F5EE),
    onSuccessContainer = Color(0xFF0B5D4D),
    warning = Color(0xFFD58C22),
    onWarning = Color.White,
    warningContainer = Color(0xFFFFF4DC),
    onWarningContainer = Color(0xFF694300),
    infoContainer = Color(0xFFE6F3FF),
    onInfoContainer = Color(0xFF17689B),
    cyan = Color(0xFF16A6B6),
    cyanContainer = Color(0xFFE1F6F8),
    purple = Color(0xFF7567E8),
    purpleContainer = Color(0xFFF0EDFF),
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

private val LightColors = lightColorScheme(
    primary = Color(0xFF2A9FE8),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE4F3FF),
    onPrimaryContainer = Color(0xFF17689B),
    secondary = Color(0xFF7567E8),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF0ECFF),
    onSecondaryContainer = Color(0xFF47359A),
    tertiary = Color(0xFF16A7A0),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE4F6F5),
    onTertiaryContainer = Color(0xFF08645F),
    background = Color(0xFFF1F2F6),
    onBackground = Color(0xFF1D222B),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1D222B),
    surfaceDim = Color(0xFFEDEFF3),
    surfaceBright = Color(0xFFFFFFFF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFAFBFC),
    surfaceContainer = Color(0xFFF5F6F9),
    surfaceContainerHigh = Color(0xFFEEF0F4),
    surfaceContainerHighest = Color(0xFFE7E9EE),
    surfaceVariant = Color(0xFFF1F3F6),
    onSurfaceVariant = Color(0xFF6D7480),
    outline = Color(0xFF9AA1AC),
    outlineVariant = Color(0xFFE1E4E9),
    error = Color(0xFFE25464),
    errorContainer = Color(0xFFFFE8EB),
    onErrorContainer = Color(0xFF6D2630),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF5AB3EE),
    onPrimary = Color(0xFF07324C),
    primaryContainer = Color(0xFF234B67),
    onPrimaryContainer = Color(0xFFD8F0FF),
    secondary = Color(0xFFA995FF),
    onSecondary = Color(0xFF2E1D69),
    secondaryContainer = Color(0xFF342D5A),
    onSecondaryContainer = Color(0xFFEAE4FF),
    tertiary = Color(0xFF39C6BC),
    onTertiary = Color(0xFF003735),
    tertiaryContainer = Color(0xFF173D3D),
    onTertiaryContainer = Color(0xFFB5F0EB),
    background = Color(0xFF0E1621),
    onBackground = Color(0xFFF3F5F9),
    surface = Color(0xFF17212B),
    onSurface = Color(0xFFF3F5F9),
    surfaceDim = Color(0xFF101923),
    surfaceBright = Color(0xFF2B3946),
    surfaceContainerLowest = Color(0xFF0E1621),
    surfaceContainerLow = Color(0xFF111B25),
    surfaceContainer = Color(0xFF17212B),
    surfaceContainerHigh = Color(0xFF1E2A35),
    surfaceContainerHighest = Color(0xFF273440),
    surfaceVariant = Color(0xFF1E2A35),
    onSurfaceVariant = Color(0xFFB7C0CF),
    outline = Color(0xFF7E899A),
    outlineVariant = Color(0xFF343C4A),
    error = Color(0xFFFFB2B8),
    errorContainer = Color(0xFF68282E),
    onErrorContainer = Color(0xFFFFDADC),
)

private val OfficeShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

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
    androidx.compose.runtime.CompositionLocalProvider(
        LocalHermesSkin provides OfficeSkin,
        LocalHermesExtendedColors provides if (darkTheme) DarkExtendedColors else LightExtendedColors,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = HermesTypography,
            shapes = OfficeShapes,
            content = content,
        )
    }
}

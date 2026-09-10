package com.qingyu.hermescompanion.ui.component

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import com.kyant.backdrop.Backdrop
import androidx.compose.ui.unit.*
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.qingyu.hermescompanion.ui.theme.HermesSkin

private val LocalSceneBackdrop = staticCompositionLocalOf<LayerBackdrop?> { null }
private val LocalAmbientBackdrop = staticCompositionLocalOf<LayerBackdrop?> { null }
private val LocalSceneView = staticCompositionLocalOf<android.view.View?> { null }

internal data class DockGeometry(val bounds: Rect, val radius: Float)
internal class DockOcclusionState { var geometry by mutableStateOf<DockGeometry?>(null) }
internal val LocalDockOcclusion = staticCompositionLocalOf<DockOcclusionState?> { null }

/** Keep live content inside the dock, reveal the original scene outside its lower corners. */
internal fun dockContentPath(width: Float, height: Float, dock: DockGeometry): Path {
    val b = dock.bounds
    val radius = dock.radius.coerceIn(0f, minOf(b.width, b.height) / 2f)
    return Path.combine(PathOperation.Union,
        Path().apply { addRect(Rect(0f, 0f, width, (b.bottom - radius).coerceIn(0f, height))) },
        Path().apply { addRoundRect(RoundRect(b, CornerRadius(radius))) })
}

@Composable
internal fun Modifier.trackFloatingDock(): Modifier {
    val state = LocalDockOcclusion.current
    val skin = HermesSkin.current
    val radius = with(androidx.compose.ui.platform.LocalDensity.current) { skin.dockRadius.dp.toPx() }
    DisposableEffect(state, skin.glass) { onDispose { state?.geometry = null } }
    return if (!skin.glass || state == null) this else onGloballyPositioned {
        state.geometry = DockGeometry(it.boundsInWindow(), radius)
    }
}

/** Only route content is recorded; dock and popup windows must never record themselves. */
@Composable
fun HermesMaterialHost(content: @Composable () -> Unit) {
    val dock = remember { DockOcclusionState() }
    val backdrop = if (HermesSkin.current.glass && Build.VERSION.SDK_INT >= 31) rememberLayerBackdrop() else null
    val ambient = if (HermesSkin.current.glass && Build.VERSION.SDK_INT >= 31) rememberLayerBackdrop() else null
    CompositionLocalProvider(LocalSceneBackdrop provides backdrop, LocalAmbientBackdrop provides ambient,
        LocalSceneView provides LocalView.current, LocalDockOcclusion provides dock, content = content)
}

/** Popup and dialog coordinates are local to their own Window, unlike the route layer. */
private class WindowBackdrop(val source: Backdrop, val page: android.view.View, val popup: android.view.View) : Backdrop {
    override val isCoordinatesDependent = true
    private val pageOrigin = IntArray(2)
    private val popupOrigin = IntArray(2)
    override fun DrawScope.drawBackdrop(density: Density, coordinates: LayoutCoordinates?, layerBlock: (GraphicsLayerScope.() -> Unit)?) {
        page.rootView.getLocationOnScreen(pageOrigin)
        popup.rootView.getLocationOnScreen(popupOrigin)
        withTransform({ translate((pageOrigin[0]-popupOrigin[0]).toFloat(), (pageOrigin[1]-popupOrigin[1]).toFloat()) }) {
            with(source) { drawBackdrop(density, coordinates, layerBlock) }
        }
    }
}

@Composable
fun HermesScene(contentBottomClip: Dp = 0.dp, content: @Composable () -> Unit) {
    val backdrop = LocalSceneBackdrop.current
    val glass = HermesSkin.current.glass
    val dock = LocalDockOcclusion.current
    var sceneOrigin by remember { mutableStateOf(Offset.Zero) }
    Box(Modifier.fillMaxSize()
        .onGloballyPositioned { sceneOrigin = it.positionInWindow() }
        .then(if (glass && backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier)
        .background(MaterialTheme.colorScheme.background)) {
        val ambient = LocalAmbientBackdrop.current
        val colors = MaterialTheme.colorScheme
        val dark = colors.background.luminance() < .5f
        Box(Modifier.matchParentSize()
            .then(if (ambient != null) Modifier.layerBackdrop(ambient) else Modifier)
            .background(if (glass) Brush.linearGradient(listOf(colors.background, colors.background,
                if (dark) Color(0xFF1A263A) else Color(0xFFE7EFFA),
                if (dark) Color(0xFF24253A) else Color(0xFFE8E7F6))) else SolidColor(colors.background)))
        // Use measured dock bounds, including variable font/inset heights. A
        // horizontal footer clip alone leaves white cards in the corner cutouts.
        Box(Modifier.fillMaxSize().drawWithContent {
            val geometry = dock?.geometry?.takeIf { glass }
            if (geometry != null) {
                val local = geometry.copy(bounds = geometry.bounds.translate(-sceneOrigin))
                clipPath(dockContentPath(size.width, size.height, local)) { this@drawWithContent.drawContent() }
            } else clipRect(bottom = (size.height - contentBottomClip.toPx()).coerceAtLeast(0f)) { this@drawWithContent.drawContent() }
        }) { content() }
    }
}

/** Samples actual page pixels. API 26–30 use an opaque, readable fallback. */
@Composable
fun Modifier.hermesChrome(shape: Shape = RoundedCornerShape(HermesSkin.current.menuRadius.dp), tintAlpha: Float = .64f,
    backgroundOnly: Boolean = false, elevation: Dp? = null, refract: Boolean = true): Modifier {
    val skin = HermesSkin.current
    val colors = MaterialTheme.colorScheme
    val source = if (backgroundOnly) LocalAmbientBackdrop.current else LocalSceneBackdrop.current
    val pageView = LocalSceneView.current
    val currentView = LocalView.current
    val positionedSource = remember(source, pageView, currentView) {
        if (source != null && pageView != null) WindowBackdrop(source, pageView, currentView) else source
    }
    val optical = skin.glass && source != null && Build.VERSION.SDK_INT >= 31
    val dark = colors.background.luminance() < .5f
    return this
        .shadow(elevation ?: if (skin.glass) 5.dp else if (skin.mode == com.qingyu.hermescompanion.ui.SkinMode.PAPER) 1.dp else 3.dp,
            shape, ambientColor = Color.Black.copy(alpha=.08f), spotColor = Color.Black.copy(alpha=.12f))
        .then(if (optical) Modifier.drawBackdrop(
            backdrop = positionedSource!!, shape = { shape },
            effects = { blur(14.dp.toPx()); if (refract) lens(3.dp.toPx(), 1.5.dp.toPx(), depthEffect = false) },
            highlight = null, shadow = null,
            onDrawSurface = { drawRect(colors.surface.copy(alpha = if (dark) maxOf(.42f, tintAlpha) else tintAlpha)) },
        ) else Modifier.background(colors.surface, shape))
        .border(.7.dp, if (skin.glass) Brush.linearGradient(listOf(
            Color.White.copy(alpha=if (dark) .23f else .92f), colors.outlineVariant.copy(alpha=.38f),
            Color.White.copy(alpha=if (dark) .12f else .68f))) else SolidColor(colors.outlineVariant.copy(alpha=.7f)), shape)
        .clip(shape)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HermesAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: (@Composable () -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
    shape: Shape = MaterialTheme.shapes.extraLarge,
    containerColor: Color = MaterialTheme.colorScheme.surface,
) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    // Shape/container are intentionally owned by the current skin across all dialogs.
    val surfaceShape = RoundedCornerShape(HermesSkin.current.sheetRadius.dp)
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = { CompositionLocalProvider(androidx.compose.ui.platform.LocalDensity provides density) { confirmButton() } },
        modifier = modifier.hermesChrome(surfaceShape),
        dismissButton = dismissButton?.let { slot -> { CompositionLocalProvider(androidx.compose.ui.platform.LocalDensity provides density) { slot() } } },
        icon = icon, title = title?.let { slot -> { CompositionLocalProvider(androidx.compose.ui.platform.LocalDensity provides density) { slot() } } },
        text = text?.let { slot -> { CompositionLocalProvider(androidx.compose.ui.platform.LocalDensity provides density) { slot() } } }, shape = surfaceShape,
        containerColor = Color.Transparent, tonalElevation = 0.dp,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HermesModalBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(),
    shape: Shape = MaterialTheme.shapes.extraLarge,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    tonalElevation: Dp = 0.dp,
    tintAlpha: Float = .64f,
    dragHandle: (@Composable () -> Unit)? = { BottomSheetDefaults.DragHandle() },
    navigationInsets: WindowInsets = WindowInsets.navigationBars.only(WindowInsetsSides.Bottom),
    content: @Composable ColumnScope.() -> Unit,
) {
    val skin = HermesSkin.current
    val surfaceShape = RoundedCornerShape(topStart = skin.sheetRadius.dp, topEnd = skin.sheetRadius.dp)
    val density = androidx.compose.ui.platform.LocalDensity.current
    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismissRequest, sheetState = sheetState,
        // The native sheet applies its drag offset AFTER this modifier. Optical
        // clipping must be inside the content, or it clips the translated sheet.
        modifier = modifier, shape = surfaceShape,
        containerColor = if (skin.glass) Color.Transparent else MaterialTheme.colorScheme.surface, tonalElevation = 0.dp,
        contentColor = MaterialTheme.colorScheme.onSurface,
        scrimColor = MaterialTheme.colorScheme.scrim.copy(alpha = .24f),
        // Material's default inset pads OUTSIDE our translucent panel. Draw the
        // surface through the gesture area, then inset only the content within it.
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
        dragHandle = null,
    ) {
        val panel = if (skin.glass) Modifier.fillMaxWidth().hermesChrome(surfaceShape, tintAlpha = tintAlpha) else Modifier.fillMaxWidth()
        Box(panel.testTag("sheet_surface")) {
            SheetSystemBars()
            CompositionLocalProvider(androidx.compose.ui.platform.LocalDensity provides density) {
                Column(Modifier.fillMaxWidth().windowInsetsPadding(navigationInsets),
                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                    dragHandle?.invoke()
                    content()
                }
            }
        }
    }
}

@Suppress("DEPRECATION")
@Composable
private fun SheetSystemBars() {
    val view = LocalView.current
    val window = (view.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window
    val light = MaterialTheme.colorScheme.surface.luminance() > .5f
    DisposableEffect(window, light) {
        val original = window?.navigationBarColor
        val contrast = if (Build.VERSION.SDK_INT >= 29) window?.isNavigationBarContrastEnforced else null
        if (window != null) {
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            if (Build.VERSION.SDK_INT >= 29) window.isNavigationBarContrastEnforced = false
            androidx.core.view.WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = light
        }
        onDispose {
            if (window != null && original != null) window.navigationBarColor = original
            if (Build.VERSION.SDK_INT >= 29 && contrast != null) window?.isNavigationBarContrastEnforced = contrast
        }
    }
}

/** Small material well: restrained highlights on glass, soft fill on warm, bare on paper. */
@Composable
fun Modifier.hermesWell(shape: Shape = MaterialTheme.shapes.small, selected: Boolean = false): Modifier {
    val skin = HermesSkin.current
    val colors = MaterialTheme.colorScheme
    if (skin.mode == com.qingyu.hermescompanion.ui.SkinMode.PAPER)
        return this.background(if (selected) colors.primaryContainer else Color.Transparent, shape).clip(shape)
    val dark = colors.background.luminance() < .5f
    val fill = if (selected) colors.primaryContainer else colors.surfaceContainer
    return this.background(if (skin.glass) Brush.linearGradient(listOf(
        colors.surface.copy(alpha = if (dark) .65f else .85f), fill.copy(alpha = .65f))) else SolidColor(fill), shape)
        .then(if (skin.glass) Modifier.border(.65.dp, Brush.linearGradient(listOf(
            Color.White.copy(alpha = if (dark) .26f else .92f), colors.primary.copy(alpha = .14f))), shape) else Modifier)
        .clip(shape)
}

@Composable
fun HermesDropdownMenu(
    expanded: Boolean, onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier, offset: DpOffset = DpOffset.Zero,
    scrollState: ScrollState = rememberScrollState(),
    shape: Shape = MaterialTheme.shapes.extraLarge,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    tonalElevation: Dp = 0.dp, shadowElevation: Dp = 0.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val surfaceShape = shape
    androidx.compose.material3.DropdownMenu(
        expanded = expanded, onDismissRequest = onDismissRequest,
        modifier = modifier.hermesChrome(surfaceShape, tintAlpha = .84f, backgroundOnly = true,
            elevation = if (HermesSkin.current.glass) 1.dp else 2.dp, refract = false), offset = offset, scrollState = scrollState,
        shape = surfaceShape, containerColor = Color.Transparent, tonalElevation = 0.dp,
        shadowElevation = 0.dp, content = content,
    )
}

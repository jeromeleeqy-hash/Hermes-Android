package com.qingyu.hermescompanion.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.qingyu.hermescompanion.R
import com.qingyu.hermescompanion.ui.AppRoute
import com.qingyu.hermescompanion.ui.theme.HermesSkin
import com.qingyu.hermescompanion.ui.theme.HermesSpacing
import com.qingyu.hermescompanion.ui.theme.HermesColors

val HermesGradient: Brush
    @Composable get() = Brush.linearGradient(
        colors = listOf(MaterialTheme.colorScheme.primary, HermesColors.extended.success),
        start = Offset.Zero,
        end = Offset(420f, 420f),
    )

@Composable
fun AmbientBackground(content: @Composable () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val skin = HermesSkin.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                if (skin.glass) Brush.verticalGradient(
                    listOf(
                        colors.background,
                        colors.primaryContainer.copy(alpha = 0.18f),
                        colors.background,
                        colors.secondaryContainer.copy(alpha = 0.14f),
                    ),
                ) else SolidColor(colors.background),
            ),
    ) {
        if (skin.glass) {
            Box(
                Modifier
                    .size(300.dp)
                    .offset(x = (-130).dp, y = (-105).dp)
                    .background(
                        Brush.radialGradient(listOf(colors.primary.copy(alpha = 0.10f), Color.Transparent)),
                        CircleShape,
                    ),
            )
            Box(
                Modifier
                    .align(Alignment.CenterEnd)
                    .size(270.dp)
                    .offset(x = 135.dp, y = (-80).dp)
                    .background(
                        Brush.radialGradient(listOf(colors.secondary.copy(alpha = 0.09f), Color.Transparent)),
                        CircleShape,
                    ),
            )
            Box(
                Modifier
                    .align(Alignment.BottomStart)
                    .size(250.dp)
                    .offset(x = (-120).dp, y = 120.dp)
                    .background(
                        Brush.radialGradient(listOf(colors.tertiary.copy(alpha = 0.07f), Color.Transparent)),
                        CircleShape,
                    ),
            )
        }
        content()
    }
}

@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    shape: Shape? = null,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val skin = HermesSkin.current
    val resolvedShape = shape ?: RoundedCornerShape(skin.panelRadius.dp)
    val panelBrush: Brush = if (skin.glass) {
        Brush.linearGradient(
            listOf(
                colors.surface.copy(alpha = skin.panelAlpha + 0.08f),
                colors.surfaceContainerLow.copy(alpha = skin.panelAlpha),
                colors.primaryContainer.copy(alpha = 0.12f),
            ),
        )
    } else {
        SolidColor(colors.surface.copy(alpha = skin.panelAlpha))
    }
    val panelModifier = modifier
        .then(
            if (skin.shadowElevation > 0) {
                Modifier.shadow(
                    skin.shadowElevation.dp,
                    resolvedShape,
                    ambientColor = colors.primary.copy(alpha = if (skin.glass) 0.08f else 0.025f),
                    spotColor = Color.Black.copy(alpha = if (skin.glass) 0.16f else 0.06f),
                )
            } else {
                Modifier
            },
        )
        .clip(resolvedShape)
        .background(panelBrush)
        .then(
            if (skin.borderAlpha > 0f) {
                Modifier.border(
                    width = if (skin.glass) 0.8.dp else 0.6.dp,
                    brush = Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = if (skin.glass) skin.borderAlpha else 0.3f),
                            colors.outlineVariant.copy(alpha = skin.borderAlpha * 0.7f),
                        ),
                    ),
                    shape = resolvedShape,
                )
            } else {
                Modifier
            },
        )
        .padding(contentPadding)
    Box(modifier = panelModifier, content = content)
}

@Composable
fun HermesSegmentedControl(
    items: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    compact: Boolean = false,
) {
    val skin = HermesSkin.current
    val safeIndex = selectedIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0))
    val innerPadding = if (compact) 3.dp else 5.dp
    val itemSpacing = if (compact) 3.dp else 4.dp
    val controlHeight = if (compact) 31.dp else 38.dp
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(if (compact) skin.controlRadius.dp else (skin.controlRadius + 2).dp),
        color = if (skin.glass) MaterialTheme.colorScheme.surface.copy(alpha = skin.chromeAlpha)
        else MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 0.dp,
        border = androidx.compose.foundation.BorderStroke(
            0.7.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (skin.glass) skin.borderAlpha else 0.58f),
        ),
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth().padding(innerPadding).height(controlHeight),
        ) {
            if (items.isNotEmpty()) {
                val itemWidth = (maxWidth - itemSpacing * (items.size - 1)) / items.size
                val indicatorOffset by animateDpAsState(
                    targetValue = (itemWidth + itemSpacing) * safeIndex,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = 0.86f),
                    label = "segmentIndicator",
                )
                Box(
                    modifier = Modifier
                        .offset(x = indicatorOffset)
                        .width(itemWidth)
                        .height(controlHeight)
                        .clip(RoundedCornerShape(if (compact) skin.controlRadius.dp else (skin.controlRadius + 1).dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = skin.selectedFillAlpha)),
                )
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(itemSpacing),
                ) {
                    items.forEachIndexed { index, label ->
                        val selected = safeIndex == index
                        val textColor by animateColorAsState(
                            targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                            label = "segmentTextColor",
                        )
                        Box(
                            modifier = Modifier.width(itemWidth).height(controlHeight).clickable { onSelect(index) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                label,
                                color = textColor,
                                style = if (compact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge,
                                fontWeight = if (selected) androidx.compose.ui.text.font.FontWeight.SemiBold else androidx.compose.ui.text.font.FontWeight.Medium,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HermesSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val animatedScale by animateFloatAsState(
        targetValue = if (checked) 0.82f else 0.78f,
        animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = 0.72f),
        label = "switchScale",
    )
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        modifier = modifier.graphicsLayer { scaleX = animatedScale; scaleY = animatedScale },
        colors = SwitchDefaults.colors(
            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
            checkedTrackColor = MaterialTheme.colorScheme.primary,
            uncheckedThumbColor = MaterialTheme.colorScheme.surface,
            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            uncheckedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f),
        ),
    )
}

@Composable
fun HermesMark(
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    requestedSize: Dp? = null,
) {
    val skin = HermesSkin.current
    val size = requestedSize ?: if (compact) 32.dp else 48.dp
    val shape = RoundedCornerShape(size / 3.2f)
    Image(
        // Compose painterResource does not support LayerDrawable. Keep the in-app
        // Hermes mark on a raster resource so the setup screen can always compose.
        painter = painterResource(R.drawable.hermes_app_icon_art),
        contentDescription = "Hermes",
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(size)
            .shadow(if (skin.glass) (if (compact) 3.dp else 5.dp) else 0.dp, shape)
            .clip(shape)
            .border(0.8.dp, Color.White.copy(alpha = 0.75f), shape),
    )
}

@Composable
fun HermesBottomDock(
    selected: AppRoute,
    hasUnreadConversations: Boolean,
    onSelect: (AppRoute) -> Unit,
) {
    val skin = HermesSkin.current
    GlassPanel(
        modifier = Modifier.fillMaxWidth().navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(22.dp),
                ambientColor = Color.Black.copy(alpha = 0.06f),
                spotColor = Color.Black.copy(alpha = 0.10f),
            ),
        shape = RoundedCornerShape(22.dp),
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
    ) {
        DockItems(selected, hasUnreadConversations, onSelect)
    }
}

@Composable
private fun DockItems(selected: AppRoute, hasUnreadConversations: Boolean, onSelect: (AppRoute) -> Unit) {
    val routes = listOf(AppRoute.SESSIONS, AppRoute.WORKSPACE, AppRoute.TASKS, AppRoute.PROFILE)
    val selectedIndex = routes.indexOf(selected).coerceAtLeast(0)
    BoxWithConstraints(modifier = Modifier.fillMaxWidth().height(50.dp)) {
        val itemWidth = maxWidth / routes.size
        val indicatorOffset by animateDpAsState(
            targetValue = itemWidth * selectedIndex,
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = 0.84f),
            label = "dockIndicator",
        )
        Box(
            modifier = Modifier.align(Alignment.CenterStart).offset(x = indicatorOffset)
                .width(itemWidth).height(44.dp).padding(horizontal = 3.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = HermesSkin.current.selectedFillAlpha)),
        )
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DockItem("对话", HermesIconKind.NAV_CHAT_OUTLINE, HermesIconKind.NAV_CHAT_FILLED, selected == AppRoute.SESSIONS, hasUnreadConversations, Modifier.weight(1f)) { onSelect(AppRoute.SESSIONS) }
            DockItem("空间", HermesIconKind.NAV_SPACE_OUTLINE, HermesIconKind.NAV_SPACE_FILLED, selected == AppRoute.WORKSPACE, false, Modifier.weight(1f)) { onSelect(AppRoute.WORKSPACE) }
            DockItem("任务", HermesIconKind.NAV_TASK_OUTLINE, HermesIconKind.NAV_TASK_FILLED, selected == AppRoute.TASKS, false, Modifier.weight(1f)) { onSelect(AppRoute.TASKS) }
            DockItem("我的", HermesIconKind.NAV_PROFILE_OUTLINE, HermesIconKind.NAV_PROFILE_FILLED, selected == AppRoute.PROFILE, false, Modifier.weight(1f)) { onSelect(AppRoute.PROFILE) }
        }
    }
}

@Composable
private fun DockItem(
    label: String,
    outlineIcon: HermesIconKind,
    filledIcon: HermesIconKind,
    selected: Boolean,
    showBadge: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val selectionProgress by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = 0.86f),
        label = "dockIconFill",
    )
    val tint by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "dockTint",
    )
    val itemScale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.96f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = 0.82f),
        label = "dockScale",
    )
    Column(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(vertical = 2.dp)
            .graphicsLayer { scaleX = itemScale; scaleY = itemScale },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        Box(modifier = Modifier.size(width = 28.dp, height = 25.dp), contentAlignment = Alignment.Center) {
            HermesMulticolorIcon(
                kind = outlineIcon,
                contentDescription = label,
                iconSize = 22.dp,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.graphicsLayer {
                    alpha = 1f - selectionProgress
                    val scale = 1f - selectionProgress * 0.08f
                    scaleX = scale
                    scaleY = scale
                },
            )
            HermesMulticolorIcon(
                kind = filledIcon,
                contentDescription = null,
                iconSize = 22.dp,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.graphicsLayer {
                    alpha = selectionProgress
                    val scale = 0.9f + selectionProgress * 0.1f
                    scaleX = scale
                    scaleY = scale
                },
            )
            if (showBadge) {
                Box(
                    Modifier.align(Alignment.TopEnd).size(7.dp).clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error),
                )
            }
        }
        Text(
            label,
            color = tint,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) androidx.compose.ui.text.font.FontWeight.SemiBold else androidx.compose.ui.text.font.FontWeight.Normal,
        )
    }
}

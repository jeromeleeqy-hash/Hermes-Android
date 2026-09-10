package com.qingyu.hermescompanion.ui.component

import com.qingyu.hermescompanion.i18n.uiText


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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy

val HermesGradient: Brush
    @Composable get() = Brush.linearGradient(
        colors = listOf(MaterialTheme.colorScheme.primary, HermesColors.extended.success),
        start = Offset.Zero,
        end = Offset(420f, 420f),
    )

@Composable
fun AmbientBackground(content: @Composable () -> Unit) {
    HermesMaterialHost { Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) { content() } }
}

/** Content surfaces remain readable. Optical materials belong to the chrome layer. */
@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    shape: Shape? = null,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable BoxScope.() -> Unit,
) {
    val skin = HermesSkin.current
    val paper = skin.mode == com.qingyu.hermescompanion.ui.SkinMode.PAPER
    val resolved = shape ?: RoundedCornerShape(skin.panelRadius.dp)
    Box(modifier.clip(resolved)
        .background(if (paper) Color.Transparent else MaterialTheme.colorScheme.surface)
        .padding(contentPadding), content = content)
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
    val controlHeight = ((if (compact) 40f else 44f) * androidx.compose.ui.platform.LocalDensity.current.fontScale.coerceAtLeast(1f)).dp
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(if (compact) skin.controlRadius.dp else (skin.controlRadius + 2).dp),
        color = Color.Transparent,
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
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = .55f)),
                )
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(itemSpacing),
                ) {
                    items.forEachIndexed { index, label ->
                        val selected = safeIndex == index
                        val textColor by animateColorAsState(
                            targetValue = if (selected) AssistantBlue else MaterialTheme.colorScheme.onSurfaceVariant,
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
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    if (HermesSkin.current.glass) {
        val colors = MaterialTheme.colorScheme
        val offset by animateDpAsState(if (checked) 24.dp else 4.dp,
            animationSpec = if (LocalReduceMotion.current) androidx.compose.animation.core.snap() else spring(stiffness = Spring.StiffnessMedium), label = "glassSwitchThumb")
        val track by animateColorAsState(if (checked) colors.primary.copy(alpha = .65f) else colors.surfaceContainerHighest,
            label = "glassSwitchTrack")
        Box(modifier.minimumInteractiveComponentSize()
            .then(if (onCheckedChange != null) Modifier.toggleable(checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange) else Modifier)
            .size(52.dp, 32.dp).alpha(if (enabled) 1f else .40f)
            .background(Brush.linearGradient(listOf(track, track.copy(alpha = .65f))), CircleShape)
            .border(.8.dp, Brush.linearGradient(listOf(colors.surface.copy(alpha = .90f), colors.outlineVariant.copy(alpha = .50f))), CircleShape)) {
            Box(Modifier.offset(x = offset, y = 4.dp).size(24.dp)
                .shadow(2.dp, CircleShape, ambientColor = Color.Black.copy(alpha = .10f), spotColor = Color.Black.copy(alpha = .12f))
                .background(Brush.linearGradient(listOf(Color.White, Color(0xFFDFEAF4))), CircleShape)
                .border(.7.dp, Color.White.copy(alpha = .95f), CircleShape), contentAlignment = Alignment.Center) {
                if (checked) AssistantGlyph("check", Modifier.size(12.dp), colors.primary)
            }
        }
        return
    }
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        modifier = modifier,
        thumbContent = if (HermesSkin.current.glass) ({ Box(Modifier.size(18.dp).hermesWell(CircleShape, selected = checked)) }) else null,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = AssistantAccent,
            checkedBorderColor = Color.Transparent,
            disabledCheckedThumbColor = MaterialTheme.colorScheme.surface,
            disabledCheckedTrackColor = AssistantAccent.copy(alpha = .35f),
            uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainer,
            uncheckedBorderColor = MaterialTheme.colorScheme.outlineVariant,
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
        painter = painterResource(if (LocalLauncherIcon.current == com.qingyu.hermescompanion.appearance.LauncherIcon.PARTNER)
            R.drawable.launcher_partner_preview else R.drawable.launcher_sprite_preview),
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
    ReferenceBottomDock(selected, hasUnreadConversations, onSelect)
}

@Composable
private fun DockItems(selected: AppRoute, hasUnreadConversations: Boolean, onSelect: (AppRoute) -> Unit) {
    val routes = listOf(AppRoute.HOME, AppRoute.SESSIONS, AppRoute.PROFILE)
    val selectedIndex = routes.indexOf(selected).coerceAtLeast(0)
    BoxWithConstraints(modifier = Modifier.fillMaxWidth().height(64.dp)) {
        val itemWidth = maxWidth / routes.size
        val indicatorOffset by animateDpAsState(
            targetValue = itemWidth * selectedIndex,
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = 0.84f),
            label = "dockIndicator",
        )
        Box(
            modifier = Modifier.align(Alignment.CenterStart).offset(x = indicatorOffset)
                .width(itemWidth).heightIn(min = 48.dp).padding(horizontal = 3.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = HermesSkin.current.selectedFillAlpha)),
        )
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DockItem(uiText(R.string.ui_0437, "助理"), HermesIconKind.NAV_SPACE_OUTLINE, HermesIconKind.NAV_SPACE_FILLED, selected == AppRoute.HOME, false, Modifier.weight(1f)) { onSelect(AppRoute.HOME) }
            DockItem(uiText(R.string.ui_0438, "回看"), HermesIconKind.NAV_CHAT_OUTLINE, HermesIconKind.NAV_CHAT_FILLED, selected == AppRoute.SESSIONS, hasUnreadConversations, Modifier.weight(1f)) { onSelect(AppRoute.SESSIONS) }
            DockItem(uiText(R.string.ui_0439, "我的"), HermesIconKind.NAV_PROFILE_OUTLINE, HermesIconKind.NAV_PROFILE_FILLED, selected == AppRoute.PROFILE, false, Modifier.weight(1f)) { onSelect(AppRoute.PROFILE) }
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
        targetValue = if (selected) AssistantBlue else MaterialTheme.colorScheme.onSurfaceVariant,
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
            .heightIn(min = 48.dp)
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

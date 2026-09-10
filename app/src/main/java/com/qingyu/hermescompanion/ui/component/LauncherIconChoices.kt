package com.qingyu.hermescompanion.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.qingyu.hermescompanion.R
import com.qingyu.hermescompanion.appearance.LauncherIcon
import com.qingyu.hermescompanion.i18n.uiText

val LocalLauncherIcon = staticCompositionLocalOf { LauncherIcon.PARTNER }

@Composable
fun LauncherIconChoices(selected: LauncherIcon, busy: Boolean = false, onSelect: (LauncherIcon) -> Unit) {
    Text(uiText(R.string.launcher_icon_title, "桌面图标"), style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 18.dp, bottom = 8.dp))
    Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        LauncherIcon.entries.forEach { icon ->
            val active = selected == icon
            val title = if (icon == LauncherIcon.PARTNER) uiText(R.string.icon_partner, "白发伙伴") else uiText(R.string.icon_sprite, "耳机精灵")
            val shape = MaterialTheme.shapes.medium
            Surface(Modifier.weight(1f).clip(shape).selectable(active, enabled = !busy, role = Role.RadioButton, onClick = { onSelect(icon) })
                .testTag("launcher_icon_${icon.name}"), shape = shape,
                color = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                border = BorderStroke(1.dp, if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)) {
                Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Image(painterResource(if (icon == LauncherIcon.PARTNER) R.drawable.launcher_partner_preview else R.drawable.launcher_sprite_preview),
                        null, Modifier.size(64.dp).clip(RoundedCornerShape(16.dp)))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        HermesRadioButton(selected = active, onClick = null, enabled = !busy)
                        Text(title, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

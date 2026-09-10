package com.qingyu.hermescompanion.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.qingyu.hermescompanion.ui.SkinMode
import com.qingyu.hermescompanion.ui.theme.HermesSkin

/** Label and control share a row; explanation gets the full width below them. */
@Composable
fun SettingsToggle(title: String, subtitle: String, checked: Boolean, enabled: Boolean = true,
    horizontalPadding: Int = 16, onCheckedChange: (Boolean) -> Unit) {
    Column(Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small)
        .toggleable(checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
        .alpha(if (enabled) 1f else .50f).padding(horizontal = horizontalPadding.dp, vertical = 10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(title, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            HermesSwitch(checked, onCheckedChange = null, enabled = enabled)
        }
        if (subtitle.isNotBlank()) Text(subtitle, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth().padding(bottom = 3.dp))
    }
}

@Composable
fun SettingsBlock(title: String, subtitle: String = "", content: @Composable BoxScope.() -> Unit) {
    val paper = HermesSkin.current.mode == SkinMode.PAPER
    Column(Modifier.fillMaxWidth().padding(top = 24.dp)) {
        if (title.isNotBlank()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                if (paper) HorizontalDivider(Modifier.weight(1f).padding(start = 14.dp), color = MaterialTheme.colorScheme.outlineVariant)
            }
            if (subtitle.isNotBlank()) Text(subtitle, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp, top = 3.dp))
            Spacer(Modifier.height(10.dp))
        }
        GlassPanel(Modifier.fillMaxWidth(), content = content)
    }
}

/** Short choices stay inline; model identifiers keep their full name and provider. */
@Composable
fun SettingsChoice(label: String, value: String, enabled: Boolean = true,
    model: Boolean = false, horizontalPadding: Int = 16, onClick: () -> Unit) {
    val provider = if (model && value.contains(" · ")) value.substringBefore(" · ") else ""
    val display = if (provider.isNotBlank()) value.substringAfter(" · ") else value
    val stacked = model || value.length > 12 || LocalDensity.current.fontScale >= 1.2f
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).clip(MaterialTheme.shapes.small)
        .clickable(enabled = enabled, onClick = onClick).alpha(if (enabled) 1f else .5f)
        .padding(horizontal = horizontalPadding.dp, vertical = if (stacked) 16.dp else 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        if (stacked) Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(display, style = MaterialTheme.typography.bodyLarge.copy(lineBreak = androidx.compose.ui.text.style.LineBreak.Paragraph), fontWeight = FontWeight.Medium)
            if (provider.isNotBlank()) Text(provider, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(display, Modifier.weight(.85f), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.End)
        }
        HermesMulticolorIcon(HermesIconKind.CHEVRON_RIGHT, null, iconSize = 14.dp)
    }
}

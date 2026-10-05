package com.qingyu.hermescompanion.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/** Content links share the text gutter; the entire row remains an accessible 48dp target. */
@Composable
fun HermesContentAction(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    icon: String = "chevron", content: @Composable RowScope.() -> Unit) {
    val color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = .38f)
    Row(modifier.heightIn(min = 48.dp).clickable(enabled = enabled, role = Role.Button, onClick = onClick)
        .padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AssistantGlyph(icon, Modifier.size(20.dp), color)
        CompositionLocalProvider(LocalContentColor provides color) {
            ProvideTextStyle(MaterialTheme.typography.labelLarge, content = { content() })
        }
    }
}

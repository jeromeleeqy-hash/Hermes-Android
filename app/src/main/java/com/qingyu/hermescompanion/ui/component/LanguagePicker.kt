package com.qingyu.hermescompanion.ui.component
import com.qingyu.hermescompanion.ui.component.HermesRadioButton as RadioButton


import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.qingyu.hermescompanion.R
import com.qingyu.hermescompanion.i18n.AppLanguageMode
import com.qingyu.hermescompanion.i18n.uiText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguagePicker(selected: AppLanguageMode, onSelect: (AppLanguageMode) -> Unit, onDismiss: () -> Unit) {
    HermesModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp).selectableGroup()) {
            Text(uiText(R.string.language_title, "语言"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(uiText(R.string.language_description, "选择应用界面的语言"), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp, bottom = 18.dp))
            AppLanguageMode.entries.forEach { mode ->
                val label = when (mode) {
                    AppLanguageMode.SYSTEM -> uiText(R.string.ui_0945, "跟随系统")
                    AppLanguageMode.CHINESE -> "简体中文"
                    AppLanguageMode.ENGLISH -> "English"
                }
                Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).clip(MaterialTheme.shapes.small)
                    .hermesWell(MaterialTheme.shapes.small, selected == mode)
                    .selectable(selected == mode, role = Role.RadioButton, onClick = { onDismiss(); onSelect(mode) })
                    .testTag("language_${mode.name.lowercase()}").padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    RadioButton(selected = selected == mode, onClick = null)
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

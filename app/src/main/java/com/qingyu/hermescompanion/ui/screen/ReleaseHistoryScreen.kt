package com.qingyu.hermescompanion.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.qingyu.hermescompanion.BuildConfig
import com.qingyu.hermescompanion.R
import com.qingyu.hermescompanion.data.ReleaseHistoryItem
import com.qingyu.hermescompanion.data.parseReleaseHistory
import com.qingyu.hermescompanion.i18n.AppLanguage
import com.qingyu.hermescompanion.i18n.uiText
import com.qingyu.hermescompanion.ui.component.*

@Composable
fun ChangeLogScreen(contentPadding: PaddingValues, onBack: () -> Unit) {
    val context = LocalContext.current
    val releases = remember(context) {
        context.assets.open("release-history.json").bufferedReader().use { parseReleaseHistory(it.readText()) }
    }
    val english = AppLanguage.locale.language == "en"
    Column(Modifier.fillMaxSize().padding(contentPadding).navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { HermesMulticolorIcon(HermesIconKind.BACK, uiText(R.string.ui_0554, "返回")) }
            Column(Modifier.weight(1f).padding(start = 4.dp)) {
                Text(uiText(R.string.ui_0872, "更新日志"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text(uiText(R.string.release_history_count, "%1\$d 个版本 · 点按展开", releases.size),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        LazyColumn(Modifier.weight(1f).fillMaxWidth().testTag("release_history"),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(releases, key = { it.version }) { item ->
                HistoryEntry(item, english, item.version == BuildConfig.VERSION_NAME.substringBefore('-'))
            }
        }
    }
}

@Composable
private fun HistoryEntry(item: ReleaseHistoryItem, english: Boolean, current: Boolean) {
    var expanded by rememberSaveable(item.version) { mutableStateOf(current) }
    val lines = if (english) item.english else item.chinese
    val expandLabel = uiText(R.string.release_history_expand, "展开更新内容")
    val collapseLabel = uiText(R.string.release_history_collapse, "收起更新内容")
    GlassPanel(Modifier.fillMaxWidth().testTag("release_${item.version}")) {
        Column(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth()
                .semantics { stateDescription = if (expanded) collapseLabel else expandLabel }
                .clickable(role = Role.Button, onClickLabel = if (expanded) collapseLabel else expandLabel) { expanded = !expanded }
                .padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    HermesMulticolorIcon(if (current) HermesIconKind.CHECK_CIRCLE else HermesIconKind.HISTORY, null, iconSize = 20.dp)
                    Column(Modifier.weight(1f)) {
                        Text(uiText(R.string.ui_1259, "版本 %1\$s%2\$s", item.version,
                            if (current) uiText(R.string.ui_1260, " · 当前版本") else ""),
                            style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        if (item.date.isNotBlank()) Text(item.date, style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(if (expanded) "−" else "+", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                }
                if (!expanded) Text(lines.first(), Modifier.padding(top = 10.dp), maxLines = 2,
                    overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (expanded) Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                lines.forEach { line -> Row(Modifier.fillMaxWidth()) {
                    Text("•", color = MaterialTheme.colorScheme.primary)
                    Text(line, Modifier.weight(1f).padding(start = 8.dp), style = MaterialTheme.typography.bodyMedium)
                } }
            }
        }
    }
}

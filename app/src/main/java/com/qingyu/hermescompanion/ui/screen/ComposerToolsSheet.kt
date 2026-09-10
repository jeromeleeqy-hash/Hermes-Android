package com.qingyu.hermescompanion.ui.screen
import com.qingyu.hermescompanion.ui.component.HermesOutlinedTextField as OutlinedTextField

import com.qingyu.hermescompanion.i18n.uiText
import com.qingyu.hermescompanion.R


import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.qingyu.hermescompanion.model.DefaultPromptSnippets
import com.qingyu.hermescompanion.model.PromptSnippet
import com.qingyu.hermescompanion.ui.component.*
import com.qingyu.hermescompanion.ui.component.HermesButton as Button
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComposerToolsSheet(
    onDismiss: () -> Unit,
    onPickFiles: () -> Unit,
    onPickImages: () -> Unit,
    onOpenCommands: () -> Unit,
    onOpenWorkspace: () -> Unit,
    onInsertPrompt: (String) -> Unit,
    snippets: List<PromptSnippet> = DefaultPromptSnippets,
    onSnippetsChange: (List<PromptSnippet>) -> Unit = {},
) {
    var managing by rememberSaveable { mutableStateOf(false) }
    var editId by rememberSaveable { mutableStateOf<String?>(null) }
    var title by rememberSaveable { mutableStateOf("") }
    var body by rememberSaveable { mutableStateOf("") }
    var originalTitle by rememberSaveable { mutableStateOf("") }
    var originalBody by rememberSaveable { mutableStateOf("") }
    var discardClose by remember { mutableStateOf<Boolean?>(null) }
    var deleting by remember { mutableStateOf<PromptSnippet?>(null) }
    var restoring by remember { mutableStateOf(false) }
    val dirty = editId != null && (title != originalTitle || body != originalBody)
    val leaveEditor: (Boolean) -> Unit = { close ->
        if (dirty) discardClose = close else if (close) onDismiss() else editId = null
    }
    val edit: (PromptSnippet?) -> Unit = { item ->
        editId = item?.id ?: UUID.randomUUID().toString()
        title = item?.title.orEmpty(); body = item?.text.orEmpty()
        originalTitle = title; originalBody = body
    }
    HermesModalBottomSheet(onDismissRequest = { leaveEditor(true) }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        BackHandler(enabled = editId != null || managing) {
            if (editId != null) leaveEditor(false) else managing = false
        }
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, bottom = 24.dp).testTag("composer_tools_content")) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(if (editId != null) uiText(R.string.ui_0732, "编辑片段") else if (managing) uiText(R.string.ui_0733, "管理提示词") else uiText(R.string.ui_0734, "添加到对话"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text(if (editId != null) uiText(R.string.ui_0735, "保存后即可在对话中复用") else if (managing) uiText(R.string.ui_0736, "按常用顺序排列，点选后可继续编辑") else uiText(R.string.ui_0737, "选择资料或使用常用提示词"),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                }
                if (editId != null || managing) TextButton(onClick = { if (editId != null) leaveEditor(false) else managing = false }) { Text(uiText(R.string.ui_0554, "返回")) }
            }
            when {
                editId != null -> {
                    OutlinedTextField(title, { if (it.length <= 40) title = it }, Modifier.fillMaxWidth().padding(top = 22.dp).testTag("snippet_title"),
                        label = { Text(uiText(R.string.ui_0738, "名称")) }, singleLine = true, shape = MaterialTheme.shapes.small,
                        supportingText = { Text(uiText(R.string.ui_0739, "简短的名称，方便查找")) })
                    OutlinedTextField(body, { if (it.length <= 8000) body = it }, Modifier.fillMaxWidth().padding(top = 10.dp).testTag("snippet_body"),
                        label = { Text(uiText(R.string.ui_0740, "提示词内容")) }, minLines = 4, maxLines = 10, shape = MaterialTheme.shapes.small,
                        supportingText = { Text("${body.length} / 8000") })
                    Button(onClick = {
                        val value = PromptSnippet(editId!!, title.trim(), body.trim())
                        val updated = if (snippets.any { it.id == value.id }) snippets.map { if (it.id == value.id) value else it } else snippets + value
                        onSnippetsChange(updated); editId = null
                    }, modifier = Modifier.fillMaxWidth().padding(top = 18.dp), enabled = title.isNotBlank() && body.isNotBlank()) { Text(uiText(R.string.ui_0741, "保存片段")) }
                }
                managing -> {
                    Button(onClick = { edit(null) }, modifier = Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 14.dp)) { Text(uiText(R.string.ui_0742, "新建片段")) }
                    if (snippets.isEmpty()) Text(uiText(R.string.ui_0743, "还没有片段，添加一条常用指令吧。"), Modifier.padding(vertical = 16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    snippets.forEachIndexed { index, item ->
                        Column(Modifier.fillMaxWidth().padding(bottom = 12.dp).hermesWell().padding(14.dp).testTag("snippet_manage_${item.id}")) {
                            Text(item.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(item.text, Modifier.padding(top = 6.dp), style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3, overflow = TextOverflow.Ellipsis)
                            Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { onSnippetsChange(snippets.toMutableList().apply { add(index - 1, removeAt(index)) }) }, enabled = index > 0) {
                                    HermesMulticolorIcon(HermesIconKind.EXPAND_UP, uiText(R.string.ui_0744, "上移%1\$s", item.title), iconSize = 19.dp)
                                }
                                IconButton(onClick = { onSnippetsChange(snippets.toMutableList().apply { add(index + 1, removeAt(index)) }) }, enabled = index < snippets.lastIndex) {
                                    HermesMulticolorIcon(HermesIconKind.EXPAND_DOWN, uiText(R.string.ui_0745, "下移%1\$s", item.title), iconSize = 19.dp)
                                }
                                Spacer(Modifier.weight(1f))
                                IconButton(onClick = { edit(item) }) { HermesMulticolorIcon(HermesIconKind.EDIT, uiText(R.string.ui_0746, "编辑%1\$s", item.title), iconSize = 19.dp) }
                                IconButton(onClick = { deleting = item }) { HermesMulticolorIcon(HermesIconKind.DELETE, uiText(R.string.ui_0747, "删除%1\$s", item.title), iconSize = 19.dp) }
                            }
                        }
                    }
                    TextButton(onClick = { restoring = true }, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text(uiText(R.string.ui_0748, "恢复默认片段")) }
                }
                else -> {
                    Row(Modifier.fillMaxWidth().padding(top = 22.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf(uiText(R.string.ui_0064, "文件") to HermesIconKind.FILE, uiText(R.string.ui_0057, "图片") to HermesIconKind.PHOTO,
                            uiText(R.string.ui_0749, "命令") to HermesIconKind.COMMAND, uiText(R.string.ui_0750, "空间") to HermesIconKind.SPACE).forEachIndexed { index, (label, icon) ->
                            Column(Modifier.weight(1f).hermesWell().clickable {
                                onDismiss(); listOf(onPickFiles, onPickImages, onOpenCommands, onOpenWorkspace)[index]()
                            }.padding(vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                HermesMulticolorIcon(icon, null, iconSize = 25.dp, tint = MaterialTheme.colorScheme.primary)
                                Text(label, Modifier.padding(top = 8.dp), style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth().padding(top = 22.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(uiText(R.string.ui_0751, "提示词片段"), Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        TextButton(onClick = { managing = true }) { Text(uiText(R.string.ui_0752, "管理")) }
                    }
                    if (snippets.isEmpty()) Text(uiText(R.string.ui_0753, "把常用指令存成片段，下次一键填入。"), Modifier.padding(bottom = 12.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    snippets.forEach { item ->
                        Row(Modifier.fillMaxWidth().padding(bottom = 10.dp).hermesWell()
                            .clickable { onDismiss(); onInsertPrompt(item.text) }.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            HermesMulticolorIcon(HermesIconKind.ARTIFACT, null, iconSize = 22.dp, tint = MaterialTheme.colorScheme.primary)
                            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                Text(item.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                                Text(item.text, Modifier.padding(top = 3.dp), style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
    }
    discardClose?.let { close -> HermesAlertDialog(onDismissRequest = { discardClose = null }, title = { Text(uiText(R.string.ui_0754, "放弃未保存的修改？")) },
        confirmButton = { TextButton(onClick = { discardClose = null; editId = null; if (close) onDismiss() }) { Text(uiText(R.string.ui_0755, "放弃修改")) } },
        dismissButton = { TextButton(onClick = { discardClose = null }) { Text(uiText(R.string.ui_0756, "继续编辑")) } }) }
    deleting?.let { item -> HermesAlertDialog(onDismissRequest = { deleting = null }, title = { Text(uiText(R.string.ui_0757, "删除“%1\$s”？", item.title)) },
        confirmButton = { TextButton(onClick = { onSnippetsChange(snippets.filterNot { it.id == item.id }); deleting = null }) { Text(uiText(R.string.ui_0469, "删除")) } },
        dismissButton = { TextButton(onClick = { deleting = null }) { Text(uiText(R.string.ui_0553, "取消")) } }) }
    if (restoring) HermesAlertDialog(onDismissRequest = { restoring = false }, title = { Text(uiText(R.string.ui_0758, "恢复默认片段？")) },
        text = { Text(uiText(R.string.ui_0759, "当前片段及排序将被三条默认片段替换。")) },
        confirmButton = { TextButton(onClick = { onSnippetsChange(DefaultPromptSnippets); restoring = false }) { Text(uiText(R.string.ui_0760, "恢复默认")) } },
        dismissButton = { TextButton(onClick = { restoring = false }) { Text(uiText(R.string.ui_0553, "取消")) } })
}

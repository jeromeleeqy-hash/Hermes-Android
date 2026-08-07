package com.qingyu.hermescompanion.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.qingyu.hermescompanion.ui.component.HermesIconKind
import com.qingyu.hermescompanion.ui.component.HermesMulticolorIcon
import com.qingyu.hermescompanion.ui.theme.HermesSkin
import com.qingyu.hermescompanion.ui.theme.HermesColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComposerToolsSheet(
    onDismiss: () -> Unit,
    onPickFiles: () -> Unit,
    onPickImages: () -> Unit,
    onAddLink: () -> Unit,
    onOpenWorkspace: () -> Unit,
    onInsertPrompt: (String) -> Unit,
) {
    val skin = HermesSkin.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(
            topStart = if (skin.glass) 24.dp else 16.dp,
            topEnd = if (skin.glass) 24.dp else 16.dp,
        ),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        dragHandle = {
            HermesMulticolorIcon(
                HermesIconKind.DRAG_HANDLE,
                contentDescription = null,
                modifier = Modifier.padding(top = 8.dp, bottom = 3.dp),
                iconSize = 30.dp,
            )
        },
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 28.dp)) {
            Text("添加到对话", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("选择资料或快速组织提示词", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ToolTile("文件", HermesIconKind.FILE, MaterialTheme.colorScheme.primaryContainer, Modifier.weight(1f)) {
                    onDismiss(); onPickFiles()
                }
                ToolTile("图片", HermesIconKind.PHOTO, MaterialTheme.colorScheme.secondaryContainer, Modifier.weight(1f)) {
                    onDismiss(); onPickImages()
                }
                ToolTile("链接", HermesIconKind.LINK, HermesColors.extended.warningContainer, Modifier.weight(1f)) {
                    onDismiss(); onAddLink()
                }
                ToolTile("空间", HermesIconKind.SPACE, HermesColors.extended.successContainer, Modifier.weight(1f)) {
                    onDismiss(); onOpenWorkspace()
                }
            }
            Text("提示词片段", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 20.dp, bottom = 6.dp))
            listOf(
                "请先梳理目标，再给出可执行步骤",
                "请总结重点，并列出需要我确认的事项",
                "请检查现有内容，直接给出改进后的版本",
            ).forEachIndexed { index, prompt ->
                val background = listOf(
                    MaterialTheme.colorScheme.primaryContainer,
                    MaterialTheme.colorScheme.secondaryContainer,
                    HermesColors.extended.successContainer,
                )[index]
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.46f))
                        .clickable { onDismiss(); onInsertPrompt(prompt) }
                        .padding(horizontal = 11.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(background), contentAlignment = Alignment.Center) {
                        HermesMulticolorIcon(if (index == 2) HermesIconKind.ARTIFACT else HermesIconKind.AI, contentDescription = null, iconSize = 19.dp)
                    }
                    Text(prompt, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 10.dp))
                }
            }
        }
    }
}

@Composable
private fun ToolTile(
    label: String,
    icon: HermesIconKind,
    background: Color,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.46f))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(background), contentAlignment = Alignment.Center) {
            HermesMulticolorIcon(icon, contentDescription = null, iconSize = 21.dp)
        }
        Text(label, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 6.dp))
    }
}

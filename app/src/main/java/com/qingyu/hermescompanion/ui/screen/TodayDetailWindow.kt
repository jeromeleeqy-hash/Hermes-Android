package com.qingyu.hermescompanion.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.qingyu.hermescompanion.today.todayText

/** One fixed viewport: scrolling never changes a draggable sheet's height or anchors. */
@Composable
internal fun TodayDetailWindow(onDismiss: () -> Unit, title: String = todayText("事项详情", "Item details"), compact: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        BoxWithConstraints(Modifier.fillMaxSize().statusBarsPadding(), contentAlignment = Alignment.BottomCenter) {
        val height = maxHeight * if (compact && LocalDensity.current.fontScale <= 1.15f && maxHeight > 600.dp) .62f else 1f
        if (height < maxHeight) Spacer(Modifier.align(Alignment.TopCenter).fillMaxWidth().height(maxHeight - height).clickable(onClick = onDismiss))
        Surface(Modifier.fillMaxWidth().height(height), color = MaterialTheme.colorScheme.background, shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)) {
            Column(Modifier.fillMaxSize().navigationBarsPadding().imePadding().testTag("today-detail-window")) {
                Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(start = 22.dp, end = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = onDismiss, modifier = Modifier.testTag("today-detail-close")) { Text(todayText("关闭", "Close")) }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .4f))
                Column(Modifier.fillMaxWidth().weight(1f), content = content)
            }
        }
        }
    }
}

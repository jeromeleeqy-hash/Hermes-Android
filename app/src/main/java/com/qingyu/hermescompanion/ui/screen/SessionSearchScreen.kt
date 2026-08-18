package com.qingyu.hermescompanion.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.qingyu.hermescompanion.model.SessionSearchResult
import com.qingyu.hermescompanion.model.scopedId
import com.qingyu.hermescompanion.ui.AppUiState
import com.qingyu.hermescompanion.ui.component.GlassPanel
import com.qingyu.hermescompanion.ui.component.HermesIconKind
import com.qingyu.hermescompanion.ui.component.HermesMulticolorIcon
import com.qingyu.hermescompanion.ui.format.sessionTimeLabel
import com.qingyu.hermescompanion.ui.theme.HermesSpacing
import com.qingyu.hermescompanion.ui.format.ellipsizeSessionTitle
import kotlinx.coroutines.delay

@Composable
fun SessionSearchScreen(
    state: AppUiState,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onSearch: (String) -> Unit,
    onOpenResult: (SessionSearchResult) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    LaunchedEffect(query) {
        delay(650)
        onSearch(query)
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(contentPadding).statusBarsPadding()
            .padding(horizontal = HermesSpacing.page, vertical = 6.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                HermesMulticolorIcon(HermesIconKind.BACK, contentDescription = "返回")
            }
            GlassPanel(
                modifier = Modifier.weight(1f).padding(start = 4.dp),
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HermesMulticolorIcon(
                        HermesIconKind.SEARCH,
                        contentDescription = null,
                        iconSize = 18.dp,
                    )
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp).focusRequester(focusRequester),
                        decorationBox = { inner ->
                            if (query.isBlank()) {
                                Text("搜索对话", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            inner()
                        },
                    )
                    if (query.isNotBlank()) {
                        IconButton(onClick = { query = "" }, modifier = Modifier.size(32.dp)) {
                            HermesMulticolorIcon(HermesIconKind.CLOSE, contentDescription = "清空", iconSize = 15.dp)
                        }
                    }
                }
            }
        }

        when {
            query.trim().length < 2 -> SearchHint("输入至少两个字符，可搜索最近 100 个对话的消息内容")
            state.searchResults.isEmpty() && state.isSearchLoading -> SearchHint("正在搜索消息内容…")
            state.searchResults.isEmpty() -> SearchHint("没有匹配的对话或消息")
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(top = 5.dp),
                contentPadding = PaddingValues(bottom = 20.dp),
            ) {
                if (state.isSearchLoading) {
                    item(key = "searching") {
                        Text(
                            "正在继续搜索完整消息…",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 9.dp, vertical = 8.dp),
                        )
                    }
                }
                items(state.searchResults, key = { "${it.session.scopedId}:${it.messageId.orEmpty()}" }) { result ->
                    SearchResultRow(result, onOpenResult)
                }
            }
        }
    }
}

@Composable
private fun SearchResultRow(result: SessionSearchResult, onClick: (SessionSearchResult) -> Unit) {
    val session = result.session
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .clickable { onClick(result) }.padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(34.dp).clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f)),
            contentAlignment = Alignment.Center,
        ) {
            HermesMulticolorIcon(HermesIconKind.CHAT, contentDescription = null, iconSize = 18.dp)
        }
        Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(ellipsizeSessionTitle(session.title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Clip, modifier = Modifier.weight(1f))
                Text(sessionTimeLabel(session.updatedAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(start = 8.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (result.matchedMessage) {
                    Text(
                        "消息",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 6.dp),
                    )
                }
                Text(
                    result.snippet.ifBlank { session.preview.ifBlank { "暂无内容摘要" } },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun SearchHint(text: String) {
    Box(Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) {
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

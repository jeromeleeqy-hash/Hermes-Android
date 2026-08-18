package com.qingyu.hermescompanion.ui.screen

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.qingyu.hermescompanion.model.WorkspaceDocument
import com.qingyu.hermescompanion.model.WorkspaceEntry
import com.qingyu.hermescompanion.model.RecentArtifact
import com.qingyu.hermescompanion.ui.AppUiState
import com.qingyu.hermescompanion.ui.component.GlassPanel
import com.qingyu.hermescompanion.ui.component.HermesIconKind
import com.qingyu.hermescompanion.ui.component.HermesMulticolorIcon
import com.qingyu.hermescompanion.ui.component.HermesSegmentedControl
import com.qingyu.hermescompanion.ui.component.MarkdownContent
import com.qingyu.hermescompanion.ui.component.HtmlDocumentPreview
import com.qingyu.hermescompanion.ui.component.PdfDocumentPreview
import com.qingyu.hermescompanion.ui.component.PlainTextDocumentPreview
import com.qingyu.hermescompanion.ui.component.VisualMarkdownEditor
import com.qingyu.hermescompanion.ui.theme.HermesSpacing
import java.util.Locale

private enum class WorkspaceTab(val label: String) {
    RECENT("最近产物"),
    FILES("项目文件"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceScreen(
    state: AppUiState,
    contentPadding: PaddingValues,
    onRefresh: () -> Unit,
    onOpenDirectory: (String) -> Unit,
    onOpenDocument: (String) -> Unit,
    onOpenImage: (String, String) -> Unit,
    onOpenRecentArtifact: (RecentArtifact) -> Unit,
    onOpenArtifactSource: (RecentArtifact) -> Unit,
    onRefreshRecentArtifacts: () -> Unit,
    onCloseDocument: () -> Unit,
    onEditingChange: (Boolean) -> Unit,
    onDraftChange: (String) -> Unit,
    onSave: () -> Unit,
    onExportDocument: (android.net.Uri) -> Unit,
    onShareDocument: () -> Unit,
    onUnsupportedFile: (String) -> Unit,
) {
    val document = state.workspaceDocument
    if (document != null) {
        WorkspaceDocumentScreen(
            state = state,
            document = document,
            contentPadding = contentPadding,
            onClose = onCloseDocument,
            onEditingChange = onEditingChange,
            onDraftChange = onDraftChange,
            onSave = onSave,
            onExport = onExportDocument,
            onShare = onShareDocument,
            onOpenImage = onOpenImage,
            sourceArtifact = state.workspaceSourceArtifact,
            onOpenSource = onOpenArtifactSource,
        )
        return
    }

    val listing = state.workspaceListing
    val recentArtifacts = state.recentArtifacts.filter { it.profile == state.activeProfile }
    var selectedTab by remember { mutableStateOf(if (recentArtifacts.isEmpty()) WorkspaceTab.FILES else WorkspaceTab.RECENT) }
    val canGoUp = listing?.parent != null && listing.path != state.workspaceRootPath
    BackHandler(enabled = selectedTab == WorkspaceTab.FILES && canGoUp) {
        listing?.parent?.let(onOpenDirectory)
    }
    Column(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
        Column(modifier = Modifier.statusBarsPadding().padding(start = HermesSpacing.page, end = HermesSpacing.page, top = 4.dp, bottom = 4.dp)) {
            HermesSegmentedControl(
                items = WorkspaceTab.entries.map(WorkspaceTab::label),
                selectedIndex = selectedTab.ordinal,
                onSelect = { selectedTab = WorkspaceTab.entries[it] },
                modifier = Modifier.fillMaxWidth().padding(bottom = 7.dp),
                compact = true,
            )
            if (selectedTab == WorkspaceTab.FILES) GlassPanel(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(13.dp),
                contentPadding = PaddingValues(horizontal = 5.dp, vertical = 3.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { listing?.parent?.let(onOpenDirectory) },
                        enabled = canGoUp,
                        modifier = Modifier.size(40.dp),
                    ) {
                        HermesMulticolorIcon(HermesIconKind.FOLDER_UP, contentDescription = "返回上级", iconSize = 18.dp)
                    }
                    Text(
                        listing?.path ?: "正在读取工作区…",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(horizontal = 5.dp),
                    )
                }
            }
        }

        if (selectedTab == WorkspaceTab.RECENT) {
            PullToRefreshBox(
                isRefreshing = state.isRecentArtifactsLoading,
                onRefresh = onRefreshRecentArtifacts,
                modifier = Modifier.fillMaxSize(),
            ) {
                RecentArtifactsList(
                    items = recentArtifacts,
                    isLoading = state.isRecentArtifactsLoading,
                    onOpen = onOpenRecentArtifact,
                    onOpenSource = onOpenArtifactSource,
                )
            }
        } else PullToRefreshBox(
            isRefreshing = state.isWorkspaceLoading,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize(),
        ) {
            when {
                listing == null && state.isWorkspaceLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(25.dp), strokeWidth = 2.2.dp)
                }

                listing == null -> WorkspaceEmpty("无法读取工作区")
                listing.entries.isEmpty() -> WorkspaceEmpty("这个文件夹是空的")
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = HermesSpacing.page, end = HermesSpacing.page, top = 2.dp, bottom = 12.dp),
                ) {
                    items(listing.entries, key = { it.path }) { entry ->
                        WorkspaceEntryRow(entry) {
                            when {
                                entry.isDirectory -> onOpenDirectory(entry.path)
                                entry.isImage -> onOpenImage(entry.path, entry.name)
                                entry.isPreviewable -> onOpenDocument(entry.path)
                                else -> onUnsupportedFile(entry.name)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileFileScreen(
    state: AppUiState,
    contentPadding: PaddingValues,
    onClose: () -> Unit,
    onExportDocument: (android.net.Uri) -> Unit,
    onShareDocument: () -> Unit,
    onOpenImage: (String, String) -> Unit,
) {
    val document = state.workspaceDocument
    if (document == null) {
        BackHandler(onBack = onClose)
        Column(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
            Row(
                modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onClose) {
                    HermesMulticolorIcon(HermesIconKind.BACK, contentDescription = "返回")
                }
                Column(Modifier.padding(horizontal = 6.dp)) {
                    Text("Hermes 文件", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "正在读取 ${state.activeProfile} Profile",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(Modifier.size(25.dp), strokeWidth = 2.2.dp)
            }
        }
        return
    }
    WorkspaceDocumentScreen(
        state = state,
        document = document,
        contentPadding = contentPadding,
        onClose = onClose,
        onEditingChange = {},
        onDraftChange = {},
        onSave = {},
        onExport = onExportDocument,
        onShare = onShareDocument,
        onOpenImage = onOpenImage,
        sourceArtifact = null,
        onOpenSource = {},
        allowEditing = false,
        subtitle = "${state.activeProfile} Profile · Hermes 原始文件",
    )
}

@Composable
private fun RecentArtifactsList(
    items: List<RecentArtifact>,
    isLoading: Boolean,
    onOpen: (RecentArtifact) -> Unit,
    onOpenSource: (RecentArtifact) -> Unit,
) {
    if (items.isEmpty()) {
        WorkspaceEmpty(if (isLoading) "正在整理最近对话产物…" else "对话中生成的文件会出现在这里")
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = HermesSpacing.page, end = HermesSpacing.page, top = 4.dp, bottom = 14.dp),
    ) {
        items(items, key = { "${it.profile}:${it.path}" }) { item ->
            Column(Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                        .clickable { onOpen(item) }.padding(horizontal = 2.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                            .background(
                                (if (item.kind == "图片") MaterialTheme.colorScheme.secondaryContainer
                                else MaterialTheme.colorScheme.primaryContainer).copy(alpha = 0.82f),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        HermesMulticolorIcon(
                            recentArtifactIcon(item),
                            contentDescription = null,
                            iconSize = 19.dp,
                        )
                    }
                    Column(Modifier.weight(1f).padding(start = 10.dp)) {
                        Text(item.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            "${item.kind} · 来自 ${item.sessionTitle}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    IconButton(onClick = { onOpenSource(item) }, modifier = Modifier.padding(start = 2.dp).size(36.dp)) {
                        HermesMulticolorIcon(
                            HermesIconKind.SOURCE_CHAT,
                            contentDescription = "返回来源对话",
                            iconSize = 17.dp,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                HorizontalDivider(
                    modifier = Modifier.padding(start = 48.dp),
                    thickness = 0.6.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.58f),
                )
            }
        }
    }
}

@Composable
private fun WorkspaceEntryRow(entry: WorkspaceEntry, onClick: () -> Unit) {
    val icon = when {
        entry.isDirectory -> HermesIconKind.FOLDER_OPEN
        entry.isMarkdown -> HermesIconKind.MARKDOWN
        entry.isImage -> HermesIconKind.PHOTO
        entry.name.substringAfterLast('.', "").lowercase() in setOf("html", "htm") -> HermesIconKind.WEB
        else -> HermesIconKind.FILE
    }
    val wellColor = when {
        entry.isDirectory -> MaterialTheme.colorScheme.secondaryContainer
        entry.isMarkdown -> MaterialTheme.colorScheme.primaryContainer
        entry.isImage -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                .clickable(onClick = onClick).padding(horizontal = 2.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(wellColor.copy(alpha = 0.78f)),
                contentAlignment = Alignment.Center,
            ) {
                HermesMulticolorIcon(icon, contentDescription = null, iconSize = 19.dp)
            }
            Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
                Text(entry.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    if (entry.isDirectory) "文件夹" else formatFileSize(entry.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(start = 46.dp),
            thickness = 0.6.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.58f),
        )
    }
}

@Composable
private fun WorkspaceDocumentScreen(
    state: AppUiState,
    document: WorkspaceDocument,
    contentPadding: PaddingValues,
    onClose: () -> Unit,
    onEditingChange: (Boolean) -> Unit,
    onDraftChange: (String) -> Unit,
    onSave: () -> Unit,
    onExport: (android.net.Uri) -> Unit,
    onShare: () -> Unit,
    onOpenImage: (String, String) -> Unit,
    sourceArtifact: RecentArtifact?,
    onOpenSource: (RecentArtifact) -> Unit,
    allowEditing: Boolean = true,
    subtitle: String? = null,
) {
    var showDiscard by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current
    val exportLauncher = rememberLauncherForActivityResult(
        contract = remember(document.mimeType) {
            ActivityResultContracts.CreateDocument(document.mimeType.ifBlank { "application/octet-stream" })
        },
        onResult = { uri -> if (uri != null) onExport(uri) },
    )
    val isMarkdown = document.isMarkdown
    val dirty = state.workspaceDraft != document.content
    val closeSafely = {
        if (state.isWorkspaceEditing && dirty) showDiscard = true else onClose()
    }
    BackHandler { closeSafely() }
    Column(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
        Row(
            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = closeSafely) {
                HermesMulticolorIcon(HermesIconKind.BACK, contentDescription = "返回")
            }
            Column(modifier = Modifier.weight(1f).padding(horizontal = 6.dp)) {
                Text(document.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    if (state.isWorkspaceEditing) "可视化编辑" else subtitle ?: documentPreviewLabel(document),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (state.isWorkspaceEditing) {
                TextButton(onClick = { onEditingChange(false) }, enabled = !state.isWorkspaceSaving) { Text("取消") }
                TextButton(onClick = onSave, enabled = !state.isWorkspaceSaving) {
                    if (state.isWorkspaceSaving) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        HermesMulticolorIcon(HermesIconKind.SAVE, contentDescription = null, iconSize = 17.dp)
                        Text("保存", modifier = Modifier.padding(start = 3.dp))
                    }
                }
            } else {
                IconButton(onClick = { clipboard.setText(AnnotatedString(document.path)) }, modifier = Modifier.size(38.dp)) {
                    HermesMulticolorIcon(HermesIconKind.COPY, contentDescription = "复制路径", iconSize = 17.dp)
                }
                IconButton(onClick = onShare, modifier = Modifier.size(38.dp)) {
                    HermesMulticolorIcon(HermesIconKind.SHARE, contentDescription = "系统分享", iconSize = 17.dp)
                }
                IconButton(onClick = { exportLauncher.launch(document.name) }, modifier = Modifier.size(38.dp)) {
                    HermesMulticolorIcon(HermesIconKind.DOWNLOAD, contentDescription = "保存到手机", iconSize = 17.dp)
                }
                if (isMarkdown && allowEditing) IconButton(onClick = { onEditingChange(true) }, modifier = Modifier.size(38.dp)) {
                    HermesMulticolorIcon(HermesIconKind.DOCUMENT_EDIT, contentDescription = "编辑", iconSize = 17.dp)
                }
            }
        }

        if (sourceArtifact != null) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = HermesSpacing.page, vertical = 3.dp)
                    .clickable { onOpenSource(sourceArtifact) },
                shape = RoundedCornerShape(11.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f),
            ) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 11.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    HermesMulticolorIcon(HermesIconKind.SOURCE_CHAT, contentDescription = null, iconSize = 17.dp)
                    Column(Modifier.weight(1f).padding(start = 8.dp)) {
                        Text("来自 ${sourceArtifact.sessionTitle}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("点击返回生成这份文件的消息", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    HermesMulticolorIcon(HermesIconKind.CHEVRON_RIGHT, contentDescription = "查看来源会话", iconSize = 17.dp)
                }
            }
        }

        if (state.isWorkspaceEditing && isMarkdown) {
            GlassPanel(
                modifier = Modifier.fillMaxSize().padding(start = HermesSpacing.page, end = HermesSpacing.page, bottom = 6.dp),
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(8.dp),
            ) {
                VisualMarkdownEditor(
                    initialMarkdown = state.workspaceDraft,
                    documentKey = document.path,
                    onMarkdownChange = onDraftChange,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        } else if (isMarkdown) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 11.dp, end = 11.dp, bottom = 10.dp),
            ) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 5.dp, vertical = 10.dp),
                    ) {
                        MarkdownContent(
                            markdown = document.content,
                            modifier = Modifier.fillMaxWidth(),
                            onOpenImage = { source, name ->
                                onOpenImage(resolveMarkdownImageSource(document.path, source), name)
                            },
                        )
                    }
                }
            }
        } else if (document.isHtml) {
            HtmlDocumentPreview(document, modifier = Modifier.fillMaxSize().padding(top = 4.dp))
        } else if (document.isPdf) {
            PdfDocumentPreview(document, modifier = Modifier.fillMaxSize().padding(top = 4.dp))
        } else {
            PlainTextDocumentPreview(document, modifier = Modifier.fillMaxSize().padding(top = 4.dp))
        }
    }

    if (showDiscard) {
        AlertDialog(
            onDismissRequest = { showDiscard = false },
            title = { Text("放弃未保存的修改？") },
            text = { Text("返回后，本次对文档的修改不会保存。") },
            confirmButton = { TextButton(onClick = { showDiscard = false; onClose() }) { Text("放弃") } },
            dismissButton = { TextButton(onClick = { showDiscard = false }) { Text("继续编辑") } },
        )
    }
}

@Composable
private fun WorkspaceEmpty(text: String) {
    Box(Modifier.fillMaxSize().padding(bottom = 70.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            HermesMulticolorIcon(HermesIconKind.SPACE, contentDescription = null, iconSize = 32.dp)
            Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

private fun recentArtifactIcon(item: RecentArtifact): HermesIconKind {
    if (item.kind == "图片") return HermesIconKind.PHOTO
    return when (item.name.substringAfterLast('.', "").lowercase()) {
        "md", "markdown" -> HermesIconKind.MARKDOWN
        "html", "htm" -> HermesIconKind.WEB
        else -> HermesIconKind.FILE
    }
}

private val WorkspaceEntry.isMarkdown: Boolean
    get() = name.endsWith(".md", ignoreCase = true) || name.endsWith(".markdown", ignoreCase = true)

private val WorkspaceEntry.isImage: Boolean
    get() = mimeType?.startsWith("image/") == true ||
        name.substringAfterLast('.', "").lowercase() in setOf("png", "jpg", "jpeg", "webp", "gif", "bmp")

private val WorkspaceEntry.isPreviewable: Boolean
    get() = isMarkdown || name.substringAfterLast('.', "").lowercase() in setOf(
        "pdf", "html", "htm", "txt", "csv", "tsv", "json", "xml", "yaml", "yml", "log",
        "kt", "java", "py", "js", "ts", "css", "sh", "sql",
    )

private val WorkspaceDocument.isMarkdown: Boolean
    get() = mimeType.substringBefore(';').equals("text/markdown", true) ||
        path.substringAfterLast('.', "").lowercase() in setOf("md", "markdown")

private val WorkspaceDocument.isHtml: Boolean
    get() = mimeType.substringBefore(';').equals("text/html", true) ||
        path.substringAfterLast('.', "").lowercase() in setOf("html", "htm")

private val WorkspaceDocument.isPdf: Boolean
    get() = mimeType.substringBefore(';').equals("application/pdf", true) ||
        path.endsWith(".pdf", true)

private fun documentPreviewLabel(document: WorkspaceDocument): String = when {
    document.isMarkdown -> "Markdown 预览"
    document.isHtml -> "安全 HTML 预览"
    document.isPdf -> "PDF 预览"
    else -> "文本预览"
}

private fun resolveMarkdownImageSource(documentPath: String, source: String): String {
    val clean = source.trim()
    if (clean.startsWith("data:", true) || clean.startsWith("http://", true) || clean.startsWith("https://", true) || clean.startsWith('/')) {
        return clean
    }
    val parent = documentPath.substringBeforeLast('/', "")
    return if (parent.isBlank()) clean.removePrefix("./") else "$parent/${clean.removePrefix("./")}" 
}

private fun formatFileSize(bytes: Long?): String {
    if (bytes == null) return "文件"
    if (bytes < 1024) return "$bytes B"
    val kib = bytes / 1024.0
    if (kib < 1024) return String.format(Locale.getDefault(), "%.1f KB", kib)
    return String.format(Locale.getDefault(), "%.1f MB", kib / 1024.0)
}

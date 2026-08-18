package com.qingyu.hermescompanion.ui.component

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.qingyu.hermescompanion.model.WorkspaceDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.File

@Composable
fun HtmlDocumentPreview(document: WorkspaceDocument, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val dark = colors.background.luminance() < 0.5f
    val themedHtml = remember(document.content, dark) {
        htmlWithTheme(
            document.content,
            background = if (dark) "#171B24" else "#FFFFFF",
            foreground = if (dark) "#F1F3F8" else "#20242D",
            link = if (dark) "#AFC3FF" else "#4B67D1",
        )
    }
    AndroidView(
        modifier = modifier.fillMaxSize().background(colors.surface),
        factory = {
            WebView(it).apply {
                setBackgroundColor(colors.surface.toArgb())
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                settings.javaScriptCanOpenWindowsAutomatically = false
                settings.setSupportMultipleWindows(false)
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        val uri = request?.url ?: return true
                        if (uri.scheme in setOf("http", "https")) {
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                            }
                        }
                        return true
                    }

                    override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                        val uri = request?.url ?: return null
                        if (uri.scheme == "data") return null
                        return WebResourceResponse("text/plain", "UTF-8", ByteArrayInputStream(ByteArray(0)))
                    }
                }
                loadDataWithBaseURL("https://hermes.local/artifact/", themedHtml, "text/html", "UTF-8", null)
            }
        },
        update = { webView ->
            val key = themedHtml.hashCode()
            if (webView.tag != key) {
                webView.tag = key
                webView.setBackgroundColor(colors.surface.toArgb())
                webView.loadDataWithBaseURL("https://hermes.local/artifact/", themedHtml, "text/html", "UTF-8", null)
            }
        },
    )
}

@Composable
fun PlainTextDocumentPreview(document: WorkspaceDocument, modifier: Modifier = Modifier) {
    SelectionContainer(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        ) {
            item {
                Text(
                    document.content,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
fun PdfDocumentPreview(document: WorkspaceDocument, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val state by produceState<PdfPreviewState>(PdfPreviewState.Loading, document.path, document.bytes.contentHashCode()) {
        value = withContext(Dispatchers.IO) { renderPdf(context.cacheDir, document.bytes) }
    }
    when (val current = state) {
        PdfPreviewState.Loading -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        is PdfPreviewState.Failed -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(current.message, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        is PdfPreviewState.Ready -> LazyColumn(
            modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainer),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (current.totalPages > current.pages.size) {
                item {
                    Text(
                        "文件共 ${current.totalPages} 页，为控制内存仅预览前 ${current.pages.size} 页；可下载后查看完整内容。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(6.dp),
                    )
                }
            }
            itemsIndexed(current.pages) { index, bitmap ->
                Column(Modifier.fillMaxWidth()) {
                    Text(
                        "第 ${index + 1} 页",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                    Image(
                        bitmap = bitmap,
                        contentDescription = "PDF 第 ${index + 1} 页",
                        modifier = Modifier.fillMaxWidth().background(androidx.compose.ui.graphics.Color.White),
                        contentScale = ContentScale.FillWidth,
                    )
                }
            }
        }
    }
}

private sealed interface PdfPreviewState {
    data object Loading : PdfPreviewState
    data class Ready(val pages: List<ImageBitmap>, val totalPages: Int) : PdfPreviewState
    data class Failed(val message: String) : PdfPreviewState
}

private fun renderPdf(cacheDir: File, bytes: ByteArray): PdfPreviewState {
    if (bytes.isEmpty()) return PdfPreviewState.Failed("PDF 内容为空")
    val file = File.createTempFile("hermes-preview-", ".pdf", cacheDir)
    return runCatching {
        file.writeBytes(bytes)
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
            PdfRenderer(descriptor).use { renderer ->
                val count = renderer.pageCount.coerceAtMost(12)
                val pages = buildList {
                    for (index in 0 until count) {
                        renderer.openPage(index).use { page ->
                            val scale = (1200f / page.width).coerceAtMost(2f).coerceAtLeast(1f)
                            val width = (page.width * scale).toInt()
                            val height = (page.height * scale).toInt()
                            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                            bitmap.eraseColor(android.graphics.Color.WHITE)
                            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            add(bitmap.asImageBitmap())
                        }
                    }
                }
                PdfPreviewState.Ready(pages, renderer.pageCount)
            }
        }
    }.getOrElse { PdfPreviewState.Failed("无法预览这份 PDF，可下载后使用系统应用打开") }
        .also { file.delete() }
}

private fun htmlWithTheme(raw: String, background: String, foreground: String, link: String): String {
    val style = """
        <meta name="viewport" content="width=device-width, initial-scale=1" />
        <style>
          :root { color-scheme: light dark; }
          html, body { background: $background; color: $foreground; font-family: sans-serif; line-height: 1.55; margin: 0; padding: 10px; }
          img, video, canvas, svg { max-width: 100%; height: auto; }
          table { max-width: 100%; border-collapse: collapse; overflow-x: auto; display: block; }
          th, td { border: 1px solid ${if (background == "#171B24") "#4A5362" else "#DCE0E8"}; padding: 6px 8px; }
          a { color: $link; }
          pre, code { white-space: pre-wrap; overflow-wrap: anywhere; }
        </style>
    """.trimIndent()
    return when {
        raw.contains("</head>", ignoreCase = true) -> raw.replaceFirst(Regex("</head>", RegexOption.IGNORE_CASE), "$style</head>")
        raw.contains("<html", ignoreCase = true) -> {
            val tag = Regex("<html[^>]*>", RegexOption.IGNORE_CASE).find(raw)
            if (tag == null) raw else raw.replaceRange(tag.range, "${tag.value}<head>$style</head>")
        }
        else -> "<html><head>$style</head><body>$raw</body></html>"
    }
}

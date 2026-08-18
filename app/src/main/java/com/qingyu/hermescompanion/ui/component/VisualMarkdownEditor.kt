package com.qingyu.hermescompanion.ui.component

import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.Keep
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun VisualMarkdownEditor(
    initialMarkdown: String,
    documentKey: String,
    onMarkdownChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    val html = remember(
        documentKey,
        colorScheme.surface,
        colorScheme.onSurface,
        colorScheme.onSurfaceVariant,
        colorScheme.primary,
        colorScheme.surfaceVariant,
        colorScheme.outlineVariant,
    ) {
        editableDocumentHtml(
            markdown = initialMarkdown,
            background = colorScheme.surface.toCssColor(),
            foreground = colorScheme.onSurface.toCssColor(),
            secondary = colorScheme.onSurfaceVariant.toCssColor(),
            primary = colorScheme.primary.toCssColor(),
            soft = colorScheme.surfaceVariant.toCssColor(),
            outline = colorScheme.outlineVariant.toCssColor(),
        )
    }
    var webView by remember(documentKey) { mutableStateOf<WebView?>(null) }

    Column(modifier) {
        VisualEditorToolbar { command ->
            webView?.evaluateJavascript("window.hermesCommand('${command}')", null)
        }
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    setBackgroundColor(AndroidColor.TRANSPARENT)
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = false
                    settings.allowContentAccess = false
                    settings.allowFileAccess = false
                    settings.builtInZoomControls = false
                    settings.displayZoomControls = false
                    addJavascriptInterface(MarkdownEditorBridge(onMarkdownChange), "HermesEditor")
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean = true
                    }
                    loadDataWithBaseURL("https://hermes.local/editor/", html, "text/html", "UTF-8", null)
                    webView = this
                }
            },
            update = { webView = it },
        )
    }

    DisposableEffect(documentKey) {
        onDispose {
            webView?.removeJavascriptInterface("HermesEditor")
            webView?.destroy()
            webView = null
        }
    }
}

@Composable
private fun VisualEditorToolbar(onCommand: (String) -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f),
        shape = RoundedCornerShape(11.dp),
        tonalElevation = 0.dp,
        modifier = Modifier.padding(bottom = 7.dp),
    ) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 5.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            EditorTextAction("正文") { onCommand("p") }
            EditorTextAction("标题 1") { onCommand("h1") }
            EditorTextAction("标题 2") { onCommand("h2") }
            EditorTextAction("标题 3") { onCommand("h3") }
            EditorIconAction(HermesIconKind.BOLD, "粗体") { onCommand("bold") }
            EditorIconAction(HermesIconKind.ITALIC, "斜体") { onCommand("italic") }
            EditorIconAction(HermesIconKind.BULLET_LIST, "项目列表") { onCommand("ul") }
            EditorIconAction(HermesIconKind.NUMBERED_LIST, "编号列表") { onCommand("ol") }
            EditorIconAction(HermesIconKind.QUOTE, "引用") { onCommand("quote") }
            EditorIconAction(HermesIconKind.HORIZONTAL_RULE, "分割线") { onCommand("hr") }
        }
    }
}

@Composable
private fun EditorTextAction(label: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color.Transparent,
        modifier = Modifier.sizeIn(minHeight = 36.dp).clickable(onClick = onClick),
    ) {
        Box(Modifier.padding(horizontal = 9.dp).sizeIn(minHeight = 36.dp), contentAlignment = Alignment.Center) {
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun EditorIconAction(icon: HermesIconKind, description: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color.Transparent,
        modifier = Modifier.size(36.dp).clickable(onClick = onClick),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            HermesMulticolorIcon(icon, description, iconSize = 15.dp)
        }
    }
}

@Keep
private class MarkdownEditorBridge(private val onMarkdownChanged: (String) -> Unit) {
    private val mainHandler = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun onChanged(markdown: String) {
        mainHandler.post { onMarkdownChanged(markdown) }
    }
}

private fun Color.toCssColor(): String = String.format("#%06X", 0xFFFFFF and toArgb())

private fun editableDocumentHtml(
    markdown: String,
    background: String,
    foreground: String,
    secondary: String,
    primary: String,
    soft: String,
    outline: String,
): String {
    val content = markdownToEditableHtml(markdown)
    return """
        <!doctype html>
        <html><head><meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no">
        <style>
        :root{color-scheme:light dark} *{box-sizing:border-box}
        html,body{margin:0;padding:0;background:$background;color:$foreground;font-family:-apple-system,BlinkMacSystemFont,'Noto Sans SC','Segoe UI',sans-serif}
        #editor{min-height:100vh;padding:10px 10px 44px;outline:none;font-size:15px;line-height:1.62;caret-color:$primary;word-break:break-word}
        p{margin:6px 0} h1{font-size:23px;line-height:1.35;margin:17px 0 8px;font-weight:750;border-bottom:1px solid $outline;padding-bottom:7px}
        h2{font-size:19px;line-height:1.4;margin:15px 0 7px;font-weight:720} h3{font-size:17px;margin:13px 0 6px;font-weight:700}
        h4,h5,h6{font-size:15px;margin:10px 0 5px;font-weight:680} blockquote{margin:9px 0;padding:7px 10px;border-left:3px solid $primary;background:$soft;border-radius:0 9px 9px 0;color:$secondary}
        ul,ol{margin:8px 0;padding-left:27px} li{margin:4px 0} hr{border:0;border-top:1px solid $outline;margin:18px 0}
        code{background:$soft;border-radius:5px;padding:2px 5px;font-family:ui-monospace,SFMono-Regular,monospace;font-size:.9em}
        pre{overflow:auto;background:$soft;border-radius:11px;padding:11px;line-height:1.55} pre code{padding:0;background:transparent}
        .table-wrap{width:100%;overflow-x:auto;margin:10px 0;border-radius:9px} table{border-collapse:separate;border-spacing:0;width:max-content;min-width:100%;border:1px solid $outline;border-radius:9px;overflow:hidden}
        th,td{border-right:1px solid $outline;border-bottom:1px solid $outline;padding:7px 9px;min-width:128px;max-width:220px;text-align:left;white-space:normal;overflow-wrap:anywhere;vertical-align:top} th{background:$soft} tr:last-child td{border-bottom:0} th:last-child,td:last-child{border-right:0}
        img{display:block;max-width:100%;height:auto;border-radius:10px;margin:10px auto} a{color:$primary} .task{list-style:none;margin-left:-24px}
        </style></head>
        <body><div id="editor" contenteditable="true" spellcheck="true">$content</div>
        <script>
        const editor=document.getElementById('editor'); let changeTimer=null;
        function escText(s){return s.replace(/\\/g,'\\\\').replace(/([*_`~[\]])/g,'\\$1');}
        function inlineNode(node){
          if(node.nodeType===3)return escText(node.nodeValue||'');
          if(node.nodeType!==1)return '';
          const tag=node.tagName.toLowerCase(); const body=Array.from(node.childNodes).map(inlineNode).join('');
          if(tag==='strong'||tag==='b')return '**'+body+'**'; if(tag==='em'||tag==='i')return '*'+body+'*';
          if(tag==='s'||tag==='del'||tag==='strike')return '~~'+body+'~~'; if(tag==='code'&&node.parentElement.tagName.toLowerCase()!=='pre')return '`'+(node.textContent||'')+'`';
          if(tag==='a')return '['+body+']('+(node.getAttribute('href')||'')+')'; if(tag==='img')return '!['+(node.getAttribute('alt')||'图片')+']('+(node.getAttribute('src')||'')+')';
          if(tag==='br')return '\n'; return body;
        }
        function blockNode(el){
          const tag=el.tagName.toLowerCase();
          if(/^h[1-6]$/.test(tag))return '#'.repeat(Number(tag[1]))+' '+inlineNode(el);
          if(tag==='div'&&el.classList.contains('table-wrap'))return blockNode(el.querySelector('table'));
          if(tag==='p'||tag==='div')return inlineNode(el);
          if(tag==='blockquote')return inlineNode(el).split('\n').map(x=>'> '+x).join('\n');
          if(tag==='hr')return '---';
          if(tag==='pre'){const code=el.textContent||'';const lang=el.getAttribute('data-language')||'';return '```'+lang+'\n'+code.replace(/\n$/,'')+'\n```';}
          if(tag==='ul'||tag==='ol')return Array.from(el.children).map((li,i)=>{const body=inlineNode(li);if(li.classList.contains('task')){const checked=/^☑/.test(body);return '- ['+(checked?'x':' ')+'] '+body.replace(/^[☑☐]\s*/, '');}return (tag==='ol'?(i+1)+'. ':'- ')+body;}).join('\n');
          if(tag==='table'){
            const rows=Array.from(el.rows).map(r=>Array.from(r.cells).map(c=>inlineNode(c).replace(/\|/g,'\\|'))); if(!rows.length)return '';
            const out=['| '+rows[0].join(' | ')+' |','| '+rows[0].map(()=> '---').join(' | ')+' |']; rows.slice(1).forEach(r=>out.push('| '+r.join(' | ')+' |')); return out.join('\n');
          }
          if(tag==='img')return inlineNode(el); return inlineNode(el);
        }
        function toMarkdown(){return Array.from(editor.children).map(blockNode).filter(x=>x.trim().length).join('\n\n').replace(/\n{3,}/g,'\n\n').trim();}
        function notify(){clearTimeout(changeTimer);changeTimer=setTimeout(()=>HermesEditor.onChanged(toMarkdown()),80);}
        editor.addEventListener('input',notify);
        window.hermesCommand=function(cmd){editor.focus();if(cmd==='bold')document.execCommand('bold');else if(cmd==='italic')document.execCommand('italic');else if(cmd==='ul')document.execCommand('insertUnorderedList');else if(cmd==='ol')document.execCommand('insertOrderedList');else if(cmd==='quote')document.execCommand('formatBlock',false,'blockquote');else if(cmd==='hr')document.execCommand('insertHorizontalRule');else document.execCommand('formatBlock',false,cmd);notify();};
        </script></body></html>
    """.trimIndent()
}

private fun markdownToEditableHtml(markdown: String): String {
    if (markdown.isBlank()) return "<p><br></p>"
    val lines = markdown.replace("\r\n", "\n").lines()
    val out = StringBuilder()
    var index = 0
    var listType: String? = null
    fun closeList() {
        listType?.let { out.append("</").append(it).append('>') }
        listType = null
    }
    while (index < lines.size) {
        val line = lines[index]
        if (line.isBlank()) { closeList(); index++; continue }
        if (line.trimStart().startsWith("```")) {
            closeList()
            val language = line.trim().removePrefix("```").trim()
            val code = StringBuilder(); index++
            while (index < lines.size && !lines[index].trimStart().startsWith("```")) {
                if (code.isNotEmpty()) code.append('\n'); code.append(lines[index]); index++
            }
            if (index < lines.size) index++
            out.append("<pre data-language=\"").append(escapeHtml(language)).append("\"><code>").append(escapeHtml(code.toString())).append("</code></pre>")
            continue
        }
        val heading = Regex("^(#{1,6})\\s+(.+?)\\s*#*\\s*$").find(line)
        if (heading != null) { closeList(); val level=heading.groupValues[1].length; out.append("<h$level>").append(inlineToHtml(heading.groupValues[2])).append("</h$level>"); index++; continue }
        if (index + 1 < lines.size && lines[index + 1].trim().matches(Regex("^(=+|-+)$"))) {
            closeList(); val level=if(lines[index+1].trim().startsWith('='))1 else 2; out.append("<h$level>").append(inlineToHtml(line.trim())).append("</h$level>"); index+=2; continue
        }
        if (Regex("^\\s{0,3}((\\*\\s*){3,}|(-\\s*){3,}|(_\\s*){3,})\\s*$").matches(line)) { closeList(); out.append("<hr>"); index++; continue }
        val image = Regex("^\\s*!\\[([^]]*)]\\((.+)\\)\\s*$").find(line)
        if (image != null) { closeList(); out.append("<p><img alt=\"").append(escapeHtml(image.groupValues[1])).append("\" src=\"").append(escapeHtml(image.groupValues[2])).append("\"></p>"); index++; continue }
        if (isEditableTableStart(lines, index)) {
            closeList(); val rows=mutableListOf(splitEditableTableRow(line)); index+=2
            while(index<lines.size&&lines[index].contains('|')&&lines[index].isNotBlank()){rows+=splitEditableTableRow(lines[index]);index++}
            val columns=rows.maxOfOrNull(List<String>::size)?:1
            out.append("<div class=\"table-wrap\"><table><thead><tr>"); repeat(columns){cell->out.append("<th>").append(inlineToHtml(rows.first().getOrNull(cell).orEmpty())).append("</th>")}; out.append("</tr></thead><tbody>")
            rows.drop(1).forEach{row->out.append("<tr>");repeat(columns){cell->out.append("<td>").append(inlineToHtml(row.getOrNull(cell).orEmpty())).append("</td>")};out.append("</tr>")};out.append("</tbody></table></div>");continue
        }
        val task = Regex("^\\s*[-*+]\\s+\\[([ xX])]\\s+(.+)$").find(line)
        val bullet = Regex("^\\s*[-*+]\\s+(.+)$").find(line)
        val ordered = Regex("^\\s*\\d+\\.\\s+(.+)$").find(line)
        if (task != null || bullet != null || ordered != null) {
            val wanted = if (ordered != null) "ol" else "ul"; if(listType!=wanted){closeList();listType=wanted;out.append("<$wanted>")}
            val text=task?.groupValues?.get(2)?:bullet?.groupValues?.get(1)?:ordered!!.groupValues[1]
            out.append("<li");if(task!=null)out.append(" class=\"task\"");out.append('>');if(task!=null)out.append(if(task.groupValues[1].equals("x",true))"☑ " else "☐ ");out.append(inlineToHtml(text)).append("</li>");index++;continue
        }
        closeList()
        if (line.trimStart().startsWith('>')) { out.append("<blockquote>").append(inlineToHtml(line.trimStart().removePrefix(">").trimStart())).append("</blockquote>"); index++; continue }
        val paragraphLines=mutableListOf(line.trim());index++
        while(index<lines.size&&lines[index].isNotBlank()&&!startsEditableSpecial(lines,index)){paragraphLines+=lines[index].trim();index++}
        out.append("<p>").append(paragraphLines.joinToString("<br>"){inlineToHtml(it)}).append("</p>")
    }
    closeList()
    return out.toString().ifBlank { "<p><br></p>" }
}

private fun startsEditableSpecial(lines: List<String>, index: Int): Boolean {
    val line=lines[index]
    return line.trimStart().startsWith("```")||line.trimStart().startsWith('#')||line.trimStart().startsWith('>')||
        Regex("^\\s*[-*+]\\s+").containsMatchIn(line)||Regex("^\\s*\\d+\\.\\s+").containsMatchIn(line)||
        Regex("^\\s*!\\[[^]]*]\\(.+\\)\\s*$").matches(line)||isEditableTableStart(lines,index)||
        Regex("^\\s{0,3}((\\*\\s*){3,}|(-\\s*){3,}|(_\\s*){3,})\\s*$").matches(line)
}

private fun isEditableTableStart(lines: List<String>, index: Int): Boolean = index+1<lines.size&&lines[index].contains('|')&&Regex("^\\s*\\|?\\s*:?-{3,}:?\\s*(\\|\\s*:?-{3,}:?\\s*)+\\|?\\s*$").matches(lines[index+1])
private fun splitEditableTableRow(line: String): List<String> = line.trim().trim('|').split('|').map(String::trim)
private fun escapeHtml(value: String): String = value.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;")
private fun inlineToHtml(value: String): String {
    var text=escapeHtml(value)
    text=Regex("!\\[([^]]*)]\\(([^)]+)\\)").replace(text,"<img alt=\"$1\" src=\"$2\">")
    text=Regex("\\[([^]]+)]\\(([^)]+)\\)").replace(text,"<a href=\"$2\">$1</a>")
    text=Regex("\\*\\*(.+?)\\*\\*").replace(text,"<strong>$1</strong>")
    text=Regex("~~(.+?)~~").replace(text,"<s>$1</s>")
    text=Regex("`([^`]+)`").replace(text,"<code>$1</code>")
    text=Regex("(?<!\\*)\\*([^*]+)\\*(?!\\*)").replace(text,"<em>$1</em>")
    return text
}

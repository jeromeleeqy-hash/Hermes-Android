package com.qingyu.hermescompanion.ui.component

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.qingyu.hermescompanion.model.ImagePreview
import com.qingyu.hermescompanion.ui.parseChatFileLinkLine
import com.qingyu.hermescompanion.ui.parseChatImageLinkLine

private sealed interface MarkdownBlock {
    data class Paragraph(val text: String) : MarkdownBlock
    data class Heading(val level: Int, val text: String) : MarkdownBlock
    data class Bullet(val text: String, val ordered: Boolean, val number: Int = 0) : MarkdownBlock
    data class Task(val text: String, val checked: Boolean) : MarkdownBlock
    data class Quote(val text: String) : MarkdownBlock
    data class Code(val language: String, val code: String) : MarkdownBlock
    data class Table(val rows: List<List<String>>) : MarkdownBlock
    data class Image(val alt: String, val source: String) : MarkdownBlock
    data class DocumentLink(val label: String, val target: String) : MarkdownBlock
    data object HorizontalRule : MarkdownBlock
}

@Composable
fun MarkdownContent(
    markdown: String,
    modifier: Modifier = Modifier,
    onOpenImage: (String, String) -> Unit = { _, _ -> },
    onOpenLink: (String) -> Unit = {},
    inlineImagePreviews: Map<String, ImagePreview> = emptyMap(),
) {
    val blocks = remember(markdown) { parseMarkdown(markdown) }
    val relaxedBody = MaterialTheme.typography.bodyMedium.copy(
        color = MaterialTheme.colorScheme.onSurface,
        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.1f,
    )
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.Paragraph -> InlineMarkdownText(
                    text = block.text,
                    style = relaxedBody,
                    onOpenLink = onOpenLink,
                )

                is MarkdownBlock.Heading -> Column(Modifier.fillMaxWidth().padding(top = if (block.level <= 2) 8.dp else 4.dp, bottom = 2.dp)) {
                    InlineMarkdownText(
                        text = block.text,
                        style = when (block.level) {
                            1 -> MaterialTheme.typography.titleLarge
                            2 -> MaterialTheme.typography.titleMedium
                            3 -> MaterialTheme.typography.titleSmall
                            else -> relaxedBody
                        }.copy(
                            fontWeight = if (block.level <= 3) FontWeight.Bold else FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        ),
                        onOpenLink = onOpenLink,
                    )
                    if (block.level == 1) {
                        HorizontalDivider(Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f))
                    }
                }

                is MarkdownBlock.Bullet -> Row {
                    Text(
                        text = if (block.ordered) "${block.number}." else "•",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    InlineMarkdownText(
                        text = block.text,
                        style = relaxedBody,
                        onOpenLink = onOpenLink,
                    )
                }

                is MarkdownBlock.Task -> Row(verticalAlignment = androidx.compose.ui.Alignment.Top) {
                    HermesMulticolorIcon(
                        if (block.checked) HermesIconKind.CHECKBOX_CHECKED else HermesIconKind.CHECKBOX_EMPTY,
                        contentDescription = null,
                        modifier = Modifier.padding(top = 2.dp, end = 7.dp),
                        iconSize = 21.dp,
                    )
                    InlineMarkdownText(
                        text = block.text,
                        style = relaxedBody.copy(
                            color = if (block.checked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                            textDecoration = if (block.checked) TextDecoration.LineThrough else null,
                        ),
                        onOpenLink = onOpenLink,
                    )
                }

                is MarkdownBlock.Quote -> Row {
                    Box(
                        modifier = Modifier
                            .padding(end = 10.dp)
                            .widthIn(min = 3.dp, max = 3.dp)
                            .background(
                                MaterialTheme.colorScheme.primary,
                                RoundedCornerShape(2.dp),
                            )
                            .padding(vertical = 11.dp),
                    )
                    InlineMarkdownText(
                        text = block.text,
                        style = relaxedBody.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                        onOpenLink = onOpenLink,
                    )
                }

                is MarkdownBlock.Code -> CodeBlock(block)
                is MarkdownBlock.Table -> MarkdownTable(block.rows, onOpenLink)
                is MarkdownBlock.DocumentLink -> DocumentLinkCard(block, onOpenLink)
                is MarkdownBlock.Image -> PreviewableImage(
                    source = block.source,
                    name = block.alt.ifBlank { "图片" },
                    onOpen = onOpenImage,
                    preview = inlineImagePreviews[block.source],
                    modifier = Modifier.fillMaxWidth().heightIn(min = 92.dp, max = 220.dp),
                )
                MarkdownBlock.HorizontalRule -> HorizontalDivider(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 9.dp),
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
            }
        }
    }
}

@Composable
private fun DocumentLinkCard(
    block: MarkdownBlock.DocumentLink,
    onOpenLink: (String) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onOpenLink(block.target) },
        shape = RoundedCornerShape(13.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.58f),
        border = androidx.compose.foundation.BorderStroke(
            0.8.dp,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 11.dp, vertical = 10.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(36.dp)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp)),
                contentAlignment = androidx.compose.ui.Alignment.Center,
            ) {
                HermesMulticolorIcon(
                    HermesIconKind.ARTIFACT,
                    contentDescription = null,
                    iconSize = 21.dp,
                )
            }
            Column(Modifier.weight(1f).padding(start = 10.dp)) {
                Text(
                    block.label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    "点击打开 Markdown 预览",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            HermesMulticolorIcon(
                HermesIconKind.CHEVRON_RIGHT,
                contentDescription = "打开文档",
                iconSize = 16.dp,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CodeBlock(block: MarkdownBlock.Code) {
    val context = LocalContext.current
    val clipboard = remember(context) {
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 11.dp, end = 3.dp, top = 3.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = block.language.ifBlank { "代码" },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 7.dp),
                )
                IconButton(
                    onClick = {
                        clipboard.setPrimaryClip(ClipData.newPlainText("Hermes code", block.code))
                    },
                    modifier = Modifier.size(32.dp),
                ) {
                    HermesMulticolorIcon(HermesIconKind.COPY, contentDescription = "复制代码", iconSize = 15.dp)
                }
            }
            Text(
                text = block.code,
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(start = 11.dp, end = 11.dp, bottom = 10.dp),
            )
        }
    }
}

@Composable
private fun MarkdownTable(rows: List<List<String>>, onOpenLink: (String) -> Unit) {
    val columnCount = rows.maxOfOrNull(List<String>::size)?.coerceAtLeast(1) ?: 1
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val fittedCellWidth = maxWidth / columnCount.toFloat()
        val cellWidth = if (columnCount <= 2) fittedCellWidth.coerceAtLeast(116.dp) else 128.dp
        Box(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
            Surface(
                modifier = Modifier.width(cellWidth * columnCount),
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column {
                    rows.forEachIndexed { rowIndex, row ->
                        Row(
                            modifier = Modifier.height(IntrinsicSize.Min).background(
                                if (rowIndex == 0) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceContainerLow,
                            ),
                        ) {
                            repeat(columnCount) { columnIndex ->
                                InlineMarkdownText(
                                    text = row.getOrNull(columnIndex).orEmpty(),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = if (rowIndex == 0) FontWeight.SemiBold else FontWeight.Normal,
                                    ),
                                    onOpenLink = onOpenLink,
                                    modifier = Modifier
                                        .width(cellWidth)
                                        .fillMaxHeight()
                                        .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                                        .padding(horizontal = 9.dp, vertical = 8.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private const val LINK_ANNOTATION = "HERMES_LINK"

@Composable
private fun InlineMarkdownText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    onOpenLink: (String) -> Unit,
) {
    val annotated = inlineMarkdown(text)
    val hasLink = remember(annotated) {
        annotated.getStringAnnotations(LINK_ANNOTATION, 0, annotated.length).isNotEmpty()
    }
    if (hasLink) {
        @Suppress("DEPRECATION")
        ClickableText(
            text = annotated,
            modifier = modifier,
            style = style,
            onClick = { offset ->
                annotated.getStringAnnotations(LINK_ANNOTATION, offset, offset)
                    .firstOrNull()
                    ?.let { onOpenLink(it.item) }
            },
        )
    } else {
        Text(text = annotated, modifier = modifier, style = style)
    }
}

@Composable
private fun inlineMarkdown(text: String): AnnotatedString {
    val primary = MaterialTheme.colorScheme.primary
    val codeBackground = MaterialTheme.colorScheme.surfaceVariant
    return remember(text, primary, codeBackground) {
        buildAnnotatedString {
            var index = 0
            while (index < text.length) {
                when {
                    text.regionMatches(index, "MEDIA:", 0, 6, ignoreCase = true) -> {
                        val lineEnd = text.indexOf('\n', index).takeIf { it >= 0 } ?: text.length
                        val visible = text.substring(index, lineEnd).trimEnd()
                        val target = visible.substringAfter(':').trim().trim('`')
                        if (target.isNotBlank()) {
                            pushStringAnnotation(LINK_ANNOTATION, target)
                            pushStyle(SpanStyle(color = primary, textDecoration = TextDecoration.Underline))
                            append(visible)
                            pop()
                            pop()
                            index = lineEnd
                        } else {
                            append(text[index++])
                        }
                    }

                    text.startsWith("**", index) -> {
                        val end = text.indexOf("**", index + 2)
                        if (end > index) {
                            pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                            append(text.substring(index + 2, end))
                            pop()
                            index = end + 2
                        } else {
                            append(text[index++])
                        }
                    }

                    text.startsWith("~~", index) -> {
                        val end = text.indexOf("~~", index + 2)
                        if (end > index) {
                            pushStyle(SpanStyle(textDecoration = TextDecoration.LineThrough, color = primary.copy(alpha = 0.72f)))
                            append(text.substring(index + 2, end))
                            pop()
                            index = end + 2
                        } else append(text[index++])
                    }

                    (text[index] == '*' || text[index] == '_') -> {
                        val marker = text[index]
                        val end = text.indexOf(marker, index + 1)
                        if (end > index + 1) {
                            pushStyle(SpanStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic))
                            append(text.substring(index + 1, end))
                            pop()
                            index = end + 1
                        } else append(text[index++])
                    }

                    text[index] == '`' -> {
                        val end = text.indexOf('`', index + 1)
                        if (end > index) {
                            pushStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = codeBackground))
                            append(text.substring(index + 1, end))
                            pop()
                            index = end + 1
                        } else {
                            append(text[index++])
                        }
                    }

                    text[index] == '[' -> {
                        val labelEnd = text.indexOf(']', index + 1)
                        val urlStart = if (labelEnd >= 0) text.indexOf('(', labelEnd) else -1
                        val urlEnd = if (urlStart >= 0) text.indexOf(')', urlStart) else -1
                        if (labelEnd > index && urlStart == labelEnd + 1 && urlEnd > urlStart) {
                            val target = text.substring(urlStart + 1, urlEnd).trim()
                            pushStringAnnotation(LINK_ANNOTATION, target)
                            pushStyle(SpanStyle(color = primary, textDecoration = TextDecoration.Underline))
                            append(text.substring(index + 1, labelEnd))
                            pop()
                            pop()
                            index = urlEnd + 1
                        } else {
                            append(text[index++])
                        }
                    }

                    else -> append(text[index++])
                }
            }
        }
    }
}

private fun parseMarkdown(markdown: String): List<MarkdownBlock> {
    if (markdown.isBlank()) return emptyList()
    val lines = markdown.replace("\r\n", "\n").lines()
    val blocks = mutableListOf<MarkdownBlock>()
    var index = 0

    while (index < lines.size) {
        val line = lines[index]
        if (line.isBlank()) {
            index++
            continue
        }

        if (line.trimStart().startsWith("```")) {
            val language = line.trim().removePrefix("```").trim()
            val code = StringBuilder()
            index++
            while (index < lines.size && !lines[index].trimStart().startsWith("```")) {
                if (code.isNotEmpty()) code.append('\n')
                code.append(lines[index])
                index++
            }
            if (index < lines.size) index++
            blocks += MarkdownBlock.Code(language, code.toString())
            continue
        }

        parseChatFileLinkLine(line)?.let { link ->
            blocks += MarkdownBlock.DocumentLink(link.label, link.target)
            index++
            continue
        }

        parseChatImageLinkLine(line)?.let { image ->
            blocks += MarkdownBlock.Image(image.label, image.target)
            index++
            continue
        }

        if (isHorizontalRule(line)) {
            blocks += MarkdownBlock.HorizontalRule
            index++
            continue
        }

        if (index + 1 < lines.size && line.isNotBlank()) {
            val setext = lines[index + 1].trim()
            if (setext.matches(Regex("^=+$")) || setext.matches(Regex("^-+$"))) {
                blocks += MarkdownBlock.Heading(if (setext.startsWith('=')) 1 else 2, line.trim())
                index += 2
                continue
            }
        }

        val imageMatch = Regex("^\\s*!\\[([^]]*)]\\((.+)\\)\\s*$").find(line)
        if (imageMatch != null) {
            blocks += MarkdownBlock.Image(
                alt = imageMatch.groupValues[1],
                source = imageMatch.groupValues[2].trim(),
            )
            index++
            continue
        }

        if (isTableStart(lines, index)) {
            val rows = mutableListOf(splitTableRow(line))
            index += 2
            while (index < lines.size && lines[index].contains('|') && lines[index].isNotBlank()) {
                rows += splitTableRow(lines[index])
                index++
            }
            blocks += MarkdownBlock.Table(rows)
            continue
        }

        val headingMatch = Regex("^(#{1,6})\\s+(.+?)\\s*#*\\s*$").find(line)
        if (headingMatch != null) {
            blocks += MarkdownBlock.Heading(headingMatch.groupValues[1].length, headingMatch.groupValues[2])
            index++
            continue
        }

        val taskMatch = Regex("^\\s*[-*+]\\s+\\[([ xX])]\\s+(.+)$").find(line)
        if (taskMatch != null) {
            blocks += MarkdownBlock.Task(taskMatch.groupValues[2], taskMatch.groupValues[1].equals("x", true))
            index++
            continue
        }

        val orderedMatch = Regex("^\\s*(\\d+)\\.\\s+(.+)$").find(line)
        if (orderedMatch != null) {
            blocks += MarkdownBlock.Bullet(
                text = orderedMatch.groupValues[2],
                ordered = true,
                number = orderedMatch.groupValues[1].toIntOrNull() ?: 1,
            )
            index++
            continue
        }

        val bulletMatch = Regex("^\\s*[-*+]\\s+(.+)$").find(line)
        if (bulletMatch != null) {
            blocks += MarkdownBlock.Bullet(bulletMatch.groupValues[1], ordered = false)
            index++
            continue
        }

        if (line.trimStart().startsWith('>')) {
            blocks += MarkdownBlock.Quote(line.trimStart().removePrefix(">").trimStart())
            index++
            continue
        }

        val paragraph = StringBuilder(line.trim())
        index++
        while (index < lines.size && lines[index].isNotBlank() && !startsSpecialBlock(lines, index)) {
            paragraph.append('\n').append(lines[index].trim())
            index++
        }
        blocks += MarkdownBlock.Paragraph(paragraph.toString())
    }
    return blocks
}

private fun startsSpecialBlock(lines: List<String>, index: Int): Boolean {
    val line = lines[index]
    return line.trimStart().startsWith("```") ||
        line.trimStart().startsWith('#') ||
        line.trimStart().startsWith('>') ||
        Regex("^\\s*[-*+]\\s+").containsMatchIn(line) ||
        Regex("^\\s*\\d+\\.\\s+").containsMatchIn(line) ||
        Regex("^\\s*!\\[[^]]*]\\(.+\\)\\s*$").matches(line) ||
        parseChatFileLinkLine(line) != null ||
        parseChatImageLinkLine(line) != null ||
        isTableStart(lines, index) ||
        isHorizontalRule(line) ||
        (index + 1 < lines.size && lines[index + 1].trim().matches(Regex("^(=+|-+)$")))
}

private fun isHorizontalRule(line: String): Boolean = Regex("^\\s{0,3}((\\*\\s*){3,}|(-\\s*){3,}|(_\\s*){3,})\\s*$").matches(line)

private fun isTableStart(lines: List<String>, index: Int): Boolean {
    if (index + 1 >= lines.size || !lines[index].contains('|')) return false
    return Regex("^\\s*\\|?\\s*:?-{3,}:?\\s*(\\|\\s*:?-{3,}:?\\s*)+\\|?\\s*$")
        .matches(lines[index + 1])
}

private fun splitTableRow(line: String): List<String> {
    return line.trim().trim('|').split('|').map(String::trim)
}

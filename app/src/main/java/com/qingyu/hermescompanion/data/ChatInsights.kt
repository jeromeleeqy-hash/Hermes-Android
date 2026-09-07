package com.qingyu.hermescompanion.data

import com.qingyu.hermescompanion.model.ChatArtifact
import com.qingyu.hermescompanion.model.ChatMessage
import com.qingyu.hermescompanion.model.ChatTodo
import com.qingyu.hermescompanion.model.MessageRole
import com.qingyu.hermescompanion.model.TodoStatus
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener

data class ChatInsights(
    val artifacts: List<ChatArtifact> = emptyList(),
    val todos: List<ChatTodo> = emptyList(),
)

object ChatInsightParser {
    private const val extensions = "markdown|docx|xlsx|pptx|html|jpeg|md|txt|pdf|doc|xls|csv|ppt|htm|png|jpg|webp|gif|zip|apk"
    private val artifactPattern = Regex(
        """(?:~/|\./|/)[^\"'`\n\r{}<>|()，；]+?\.(?:$extensions)(?![\p{L}\p{N}_.])""",
        RegexOption.IGNORE_CASE,
    )
    private val markdownLinkPattern = Regex("""!?\[([^]]*)]\((<(?:[^>]+)>|(?:[^()\n]|\([^()\n]*\))+)\)""")
    private val urlPattern = Regex("""https?://[^\s<>\"'`]+""", RegexOption.IGNORE_CASE)

    fun fromMessages(messages: List<ChatMessage>): ChatInsights {
        val toolMessages = messages.filter { it.role == MessageRole.TOOL || it.role == MessageRole.SYSTEM }
        val todos = toolMessages.asReversed().firstNotNullOfOrNull { parseTodos(it.content).takeIf(List<ChatTodo>::isNotEmpty) }
            .orEmpty()
        val artifacts = messages.asSequence()
            .flatMap { artifactsFromText(it.content).asSequence() }
            .distinctBy(ChatArtifact::path)
            .take(24)
            .toList()
        return ChatInsights(artifacts = artifacts, todos = todos)
    }

    fun artifactsFromText(text: String): List<ChatArtifact> {
        if (text.isBlank()) return emptyList()
        val links = markdownLinkPattern.findAll(text).toList()
        val protectedRanges = links.map { it.range } + urlPattern.findAll(text).map { it.range }.toList()
        val paths = buildList<Pair<Int, String>> {
            links.forEach { match ->
                val target = normalizeArtifactTarget(match.groupValues[2], markdownLink = true)
                if (!target.contains("://") && target.hasArtifactExtension()) add(match.range.first to target)
            }
            artifactPattern.findAll(text).forEach { match ->
                if (protectedRanges.none { match.range.first in it }) {
                    add(match.range.first to normalizeArtifactTarget(match.value.trim().trimEnd('.', ',', ';', ':', '。', '，')))
                }
            }
        }
        return paths.sortedBy(Pair<Int, String>::first).map(Pair<Int, String>::second).distinct().map { path ->
            ChatArtifact(path = path, name = path.substringAfterLast('/').ifBlank { path }, kind = artifactKind(path))
        }
    }

    fun parseTodos(text: String): List<ChatTodo> {
        if (text.isBlank() || !text.contains("todos", ignoreCase = true)) return emptyList()
        val value = runCatching { JSONTokener(text).nextValue() }.getOrNull()
        val array = when (value) {
            is JSONObject -> value.optJSONArray("todos")
            is JSONArray -> value
            else -> null
        }
        parseTodos(array).takeIf(List<ChatTodo>::isNotEmpty)?.let { return it }

        // Android's org.json implementation is unavailable in local JVM tests, and some
        // gateways wrap the todo payload in log text. Keep a narrow fallback for the flat
        // todo objects Hermes emits instead of exposing the raw tool block in the chat.
        val todoArray = Regex(""""todos"\s*:\s*\[([\s\S]*?)]""", RegexOption.IGNORE_CASE)
            .find(text)
            ?.groupValues
            ?.getOrNull(1)
            ?: return emptyList()
        return Regex("""\{([^{}]*)}""").findAll(todoArray).mapIndexedNotNull { index, match ->
            val body = match.groupValues[1]
            val content = sequenceOf("content", "text", "title")
                .mapNotNull { body.jsonStringField(it) }
                .firstOrNull(String::isNotBlank)
                ?: return@mapIndexedNotNull null
            val rawStatus = body.jsonStringField("status").orEmpty().lowercase()
            ChatTodo(
                id = body.jsonStringField("id").orEmpty().ifBlank { "todo-$index-${content.hashCode()}" },
                content = content,
                status = rawStatus.toTodoStatus(),
            )
        }.toList()
    }

    fun parseTodos(array: JSONArray?): List<ChatTodo> {
        if (array == null) return emptyList()
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val content = sequenceOf("content", "text", "title")
                    .map { item.optString(it).trim() }
                    .firstOrNull(String::isNotBlank)
                    ?: continue
                val rawStatus = item.optString("status").lowercase()
                add(
                    ChatTodo(
                        id = item.optString("id").ifBlank { "todo-$index-${content.hashCode()}" },
                        content = content,
                        status = rawStatus.toTodoStatus(),
                    ),
                )
            }
        }
    }

    private fun String.hasArtifactExtension(): Boolean =
        substringAfterLast('.', missingDelimiterValue = "").lowercase() in setOf(
            "md", "markdown", "txt", "pdf", "doc", "docx", "xls", "xlsx", "csv",
            "ppt", "pptx", "html", "htm", "png", "jpg", "jpeg", "webp", "gif", "zip", "apk",
        )

    private fun artifactKind(path: String): String = when (path.substringAfterLast('.', "").lowercase()) {
        "md", "markdown" -> "Markdown"
        "png", "jpg", "jpeg", "webp", "gif" -> "图片"
        "zip" -> "压缩包"
        "apk" -> "Android 安装包"
        "pdf" -> "PDF"
        "doc", "docx" -> "Word 文档"
        "xls", "xlsx", "csv" -> "表格"
        "ppt", "pptx" -> "演示文稿"
        "html", "htm" -> "网页"
        else -> "文件"
    }

    private fun String.jsonStringField(key: String): String? {
        val encoded = Regex(""""${Regex.escape(key)}"\s*:\s*"((?:\\.|[^"\\])*)"""")
            .find(this)
            ?.groupValues
            ?.getOrNull(1)
            ?: return null
        return encoded
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
            .replace("\\\"", "\"")
            .replace("\\\\", "\\")
            .trim()
    }

    private fun String.toTodoStatus(): TodoStatus = when (this) {
        "completed", "complete", "done" -> TodoStatus.COMPLETED
        "in_progress", "in-progress", "running", "doing" -> TodoStatus.IN_PROGRESS
        else -> TodoStatus.PENDING
    }
}

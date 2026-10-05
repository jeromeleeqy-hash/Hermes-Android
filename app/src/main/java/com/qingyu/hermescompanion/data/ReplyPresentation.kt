package com.qingyu.hermescompanion.data

import com.qingyu.hermescompanion.assistant.AssistantPrompts
import com.qingyu.hermescompanion.today.todayText

/** A reading view only. Original messages, copied text and history are never rewritten. */
internal data class ReplyPresentation(val body: String, val details: List<String> = emptyList())

internal fun replyPresentation(raw: String): ReplyPresentation {
    val lines = raw.lines()
    val body = mutableListOf<String>()
    val details = mutableListOf<String>()
    var section = mutableListOf<String>()
    var logLevel: Int? = null
    var fence: String? = null
    fun flush() {
        if (section.isEmpty()) return
        val text = section.joinToString("\n").trim()
        // Keep an entire section visible when it may contain a material blocker or required input.
        if (Regex("失败|未完成|未成功|未保存|未写入|未同步|未核实|无法|拒绝|超时|取消|中断|受阻|冲突|需.{0,6}(确认|审批|授权)|等待.{0,6}(确认|审批)|不确定|待核|\\b(failed|failure|blocked|conflict|approval|permission|unverified|rejected|cancelled|unable|unsuccessful|timeout)\\b", RegexOption.IGNORE_CASE).containsMatchIn(text)) {
            body.addAll(section)
        } else details.add(text)
        section = mutableListOf()
        logLevel = null
    }
    for (line in lines) {
        val trimmed = line.trimStart()
        val marker = Regex("^(`{3,}|~{3,})").find(trimmed)?.value
        val insideFence = fence != null
        if (marker != null) {
            if (fence == null) fence = marker
            else if (marker.first() == fence!!.first() && marker.length >= fence!!.length) fence = null
        }
        val heading = if (insideFence || marker != null) null else replyHeading(line)
        val plain = heading?.second?.trim()?.trimEnd('：', ':')?.lowercase()
        val log = plain in setOf("处理日志", "处理日志（详情）", "处理日志(详情)", "执行日志", "技术详情", "processing log", "execution log", "technical details")
        val resultBoundary = plain?.let { it.startsWith("结论") || it.startsWith("需要你") || it.startsWith("结果") || it.startsWith("下一步") || it.startsWith("result") || it.startsWith("next steps") } == true
        if (logLevel != null && heading != null && (heading.first <= logLevel!! || resultBoundary)) flush()
        if (log) logLevel = heading!!.first
        if (logLevel != null) section.add(line) else body.add(line)
    }
    flush()
    // Never present an empty answer as success merely because the Agent returned only a log.
    return ReplyPresentation(body.joinToString("\n").trim(), details)
}

private fun replyHeading(line: String): Pair<Int, String>? {
    val text = line.trim()
    Regex("^(#{1,6})\\s+(.+)$").matchEntire(text)?.let { return it.groupValues[1].length to it.groupValues[2].trim('*', ' ') }
    if (text.startsWith("**") && text.endsWith("**") && text.length > 4) return 2 to text.removePrefix("**").removeSuffix("**").trim()
    // Old App templates asked for these unadorned section labels.
    if (Regex("^(处理日志(?:（详情）|\\(详情\\))?|执行日志|技术详情)[：:]?$").matches(text)) return 2 to text.trimEnd('：', ':')
    if (Regex("^(结论|需要你补充|需要你补的|下一步|结果)[：:].+").matches(text)) return 2 to text
    return null
}

/** Conservative excerpt, not semantic classification or an assertion of business success. */
internal fun replyExcerpt(raw: String): String {
    val visible = AssistantPrompts.visibleText(raw)
    val text = replyPresentation(visible).body
    val paragraphs = text.split(Regex("\\n\\s*\\n"))
    val result = paragraphs.firstOrNull { Regex("^(?:#{1,6}\\s*)?(?:\\*\\*)?(结论|结果|本次结果|需要你|Result|Outcome)[：: ]", RegexOption.IGNORE_CASE).containsMatchIn(it.trim()) }
    val selected = result ?: paragraphs.firstOrNull { block ->
        block.isNotBlank() && !isOpeningNarration(block.trim()) && !block.trim().matches(Regex("#{1,6}[^\\n]+"))
    }.orEmpty()
    return selected.replace(Regex("\\[([^]]+)]\\([^)]*\\)"), "$1")
        .replace(Regex("(?m)^\\s*(?:#{1,6} |[-*] )"), "")
        .replace("**", "").replace("`", "").replace(Regex("\\s+"), " ").trim().take(240)
}

private fun isOpeningNarration(text: String): Boolean = Regex(
    "^(I['’]ll (?:resume|process|start|read|check|first|now)|Let me (?:start|read|check|first)|我(?:先|会先)(?:读取|检查|核对|查看))",
    RegexOption.IGNORE_CASE,
).containsMatchIn(text)

/** Only known App-maintenance vocabulary is relabelled. User titles remain unchanged. */
internal fun readableConversationTitle(title: String): String = when {
    Regex("presentation_version|editorial_version|hermes-today\\.json|refresh_overview_only").containsMatchIn(title) -> todayText("首页整理与更新", "Overview maintenance")
    Regex("^(?:处理|提交|确认).{0,12}(?:definition|operation_id|apply_card_action)", RegexOption.IGNORE_CASE).containsMatchIn(title) -> todayText("事项处理记录", "Item discussion")
    title.isBlank() || title.trim() in setOf("新会话", "New chat", "New conversation") -> todayText("待命名对话", "Untitled conversation")
    else -> title
}

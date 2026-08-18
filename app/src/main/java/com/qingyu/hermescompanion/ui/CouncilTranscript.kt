package com.qingyu.hermescompanion.ui

internal data class CouncilAgentMessage(
    val index: Int,
    val total: Int,
    val name: String,
    val badge: String,
    val task: String,
    val content: String,
)

private val councilBatchMarker = Regex(
    "ASYNC\\s+DELEGATION\\s+BATCH\\s+COMPLETE|background\\s+fan-out\\s+of\\s+\\d+\\s+subagent",
    RegexOption.IGNORE_CASE,
)
private val councilTaskHeader = Regex(
    "(?im)^\\s*(?:-{2,}\\s*)?(?:[✓✔✅]\\s*)?/?\\s*TASK\\s+(\\d+)\\s*/\\s*(\\d+)\\s*:\\s*(.*)$",
)
private val councilExpertName = Regex("(?:你是\\s*)?[【\\[]\\s*([^】\\]\\n]{2,24})\\s*[】\\]]")
private val councilResultPrefix = Regex("(?i)^\\s*(?:result|response|output|结果|返回内容|专家结论)\\s*[:：]\\s*")
private val syntheticProcessingStatus = Regex(
    "^\\s*(?:Hermes\\s*)?正在处理(?:中|当前任务|当前问题)?(?:\\s*[·•]\\s*\\d+\\s*项进行中)?[。.!！…]*\\s*$",
    RegexOption.IGNORE_CASE,
)
private val fallbackExpertNames = listOf("证据分析员", "反方审查员", "落地评审员")

internal fun parseCouncilAgentMessages(text: String): List<CouncilAgentMessage> {
    if (!councilBatchMarker.containsMatchIn(text)) return emptyList()
    val headers = councilTaskHeader.findAll(text).toList()
    if (headers.isEmpty()) return emptyList()

    return headers.mapIndexedNotNull { position, header ->
        val index = header.groupValues[1].toIntOrNull() ?: (position + 1)
        val total = header.groupValues[2].toIntOrNull() ?: headers.size
        val heading = header.groupValues[3].trim()
        val bodyStart = header.range.last + 1
        val bodyEnd = headers.getOrNull(position + 1)?.range?.first ?: text.length
        val rawBody = text.substring(bodyStart.coerceAtMost(text.length), bodyEnd.coerceAtMost(text.length))
            .trim()
            .trimEnd('-', '—')
            .trim()
        val identitySource = "$heading\n${rawBody.take(240)}"
        val name = councilExpertName.find(identitySource)?.groupValues?.get(1)?.trim()
            ?.takeIf(String::isNotBlank)
            ?: fallbackExpertNames.getOrElse(position) { "专家 ${position + 1}" }
        val task = heading
            .replace(councilExpertName, "")
            .replace(Regex("^\\s*[。:：·—-]+\\s*"), "")
            .removePrefix("任务：")
            .removePrefix("任务:")
            .trim()
        val content = rawBody.replace(councilResultPrefix, "").trim().ifBlank { heading }
        if (content.isBlank()) null else CouncilAgentMessage(
            index = index,
            total = total,
            name = name,
            badge = name.firstOrNull()?.toString() ?: (position + 1).toString(),
            task = task,
            content = content,
        )
    }
}

internal fun isSyntheticProcessingStatus(text: String): Boolean =
    text.isNotBlank() && syntheticProcessingStatus.matches(text)

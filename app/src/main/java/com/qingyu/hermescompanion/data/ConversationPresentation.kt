package com.qingyu.hermescompanion.data

import com.qingyu.hermescompanion.model.*

/** Only known App protocol files are internal; arbitrary JSON, code, and user files remain accessible. */
internal fun isAssistantSupportFile(path: String): Boolean {
    val normalized = path.replace('\\', '/').substringBefore('?').substringBefore('#')
    val name = normalized.substringAfterLast('/')
    return normalized.contains("/.hermes-app/today/") || normalized.startsWith(".hermes-app/today/") || name in setOf(
        "hermes-today-contract.md", "hermes-today-writer.py", "hermes-today-examples.json", "hermes-today-cron-template.txt",
        "today-recent-conversations.json", "hermes-today-request.json", "today-card-context.json.txt", "today-card-action.json", "today-conversation-context.txt",
    )
}

/** This is a reference list, not a claim that every referenced file was created by the Agent. */
internal fun currentConversationMaterials(messages: List<ChatMessage>, tools: List<ToolActivity> = emptyList()): List<ChatArtifact> {
    val start = messages.indexOfLast { it.role == MessageRole.USER }.coerceAtLeast(0)
    val turn = messages.drop(start)
    return (turn.flatMap { ChatInsightParser.artifactsFromText(it.content) } +
        tools.flatMap { ChatInsightParser.artifactsFromText(it.preview) })
        .filterNot { isAssistantSupportFile(it.path) }
        .distinctBy { normalizeArtifactTarget(it.path).replace('\\', '/') }
}

internal fun assistantConversationPrompt(prompt: String): String {
    if (prompt.trimStart().startsWith('/')) return prompt
    return com.qingyu.hermescompanion.assistant.AssistantPrompts.envelope(prompt, "Hermes App 回复偏好：以用户本次要求为准。沿用用户当前使用的语言。日常回复先说事情的进展、结果和需要用户决定的内容，简洁但保留理解所需的背景。除非用户要求排查或执行详情，不逐项汇报读取文件、脚本、JSON、哈希、回执或校验日志。处理详情留在工具记录，不在最终答复另写长篇“处理日志（详情）”。不能隐藏实质性失败或省略需要用户审批的具体动作。明确区分事实、推测和建议；用户的详细分析要求优先于简洁偏好。")
}

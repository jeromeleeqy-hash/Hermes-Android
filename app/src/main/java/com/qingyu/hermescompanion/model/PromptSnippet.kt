package com.qingyu.hermescompanion.model

import com.qingyu.hermescompanion.i18n.uiText
import com.qingyu.hermescompanion.R


/** Stable IDs keep edits and ordering independent of display text. */
data class PromptSnippet(val id: String, val title: String, val text: String)

val DefaultPromptSnippets get() = listOf(
    PromptSnippet("plan", uiText(R.string.ui_0151, "梳理目标"), uiText(R.string.ui_0152, "先梳理目标，再给出可执行步骤")),
    PromptSnippet("summarize", uiText(R.string.ui_0153, "总结重点"), uiText(R.string.ui_0154, "总结重点，并列出需要我确认的事项")),
    PromptSnippet("improve", uiText(R.string.ui_0155, "改进内容"), uiText(R.string.ui_0156, "检查现有内容，直接给出改进后的版本")),
)

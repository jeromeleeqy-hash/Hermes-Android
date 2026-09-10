package com.qingyu.hermescompanion.assistant

import com.qingyu.hermescompanion.i18n.uiText
import com.qingyu.hermescompanion.R


import com.qingyu.hermescompanion.data.ApiException
import com.qingyu.hermescompanion.data.HermesApiClient
import com.qingyu.hermescompanion.model.HermesSession

/** An ordinary Hermes session. No workspace, record database or scheduler is created. */
object DailyConversation {
    val TITLE: String get() = uiText(R.string.ui_0045, "日常助理")
    private val titles = setOf("日常助理", "Daily assistant")
    fun isDailyTitle(title: String): Boolean = title in titles
    fun stableTitle(session: HermesSession): String = session.title.takeIf(::isDailyTitle) ?: TITLE
    const val SHARE_TARGET = "__hermes_daily_conversation__"

    fun resolve(
        client: HermesApiClient,
        profile: String,
        savedId: String?,
        remember: (HermesSession) -> Unit,
    ): HermesSession {
        val saved = savedId?.takeIf { it.isNotBlank() }?.let { id ->
            try { client.sessionForProfile(id, profile) }
            catch (error: ApiException) { if (error.statusCode == 404) null else throw error }
        }
        val existing = saved ?: client.findSessionByTitleForProfile(TITLE, profile)
            ?: titles.filterNot { it == TITLE }.firstNotNullOfOrNull { client.findSessionByTitleForProfile(it, profile) }
        val session = existing ?: client.createSessionForProfile(null, profile)
        require(session.profile == profile) { uiText(R.string.ui_0046, "服务器返回了其他档案的会话") }
        // Remember immediately: a lost rename acknowledgement must not create another session.
        remember(session)
        val targetTitle = stableTitle(session)
        if (session.title != targetTitle) {
            try { client.renameSessionForProfile(session.id, targetTitle, profile) }
            catch (error: ApiException) {
                // Some gateways persist empty live sessions only after their first message.
                // The first completed turn will save the title again.
                if (existing != null || error.statusCode != 404) throw error
            }
        }
        return session.copy(title = targetTitle)
    }

    fun prompt(text: String): String = AssistantPrompts.envelope(text, uiText(R.string.ui_0047, "\n        这是用户与你的日常对话。沿用你已有的身份、性格、记忆和工具。\n        用户手机当前时间：%1\$s。约定提醒时使用明确日期、时间及时区。\n        简单问题简洁自然地回答，需要深入时再展开。不要把普通聊天自动变成待办。\n        用户明确要求记住、保存、提醒或执行时，使用你现有且可用的记忆、文件或定时任务工具处理。\n        只有实际操作成功后才能说已保存或已安排；缺少工具、权限或执行失败时如实说明。\n        时间或对象不明确时，只追问必要的信息。修改或取消已有事项时先找到原事项，避免重复创建。\n        不要求用户学习分类、草案、采用等流程，不承诺未经配置的手机推送或持续后台跟进。\n    ", java.time.ZonedDateTime.now()).trimIndent())
}

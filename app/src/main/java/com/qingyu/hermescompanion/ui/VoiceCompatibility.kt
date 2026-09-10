package com.qingyu.hermescompanion.ui

import com.qingyu.hermescompanion.i18n.uiText
import com.qingyu.hermescompanion.R


internal fun isAgentSttCompatibilityFailure(throwable: Throwable?): Boolean {
    val message = throwable?.message.orEmpty().lowercase()
    return "transcribe_audio" in message &&
        "unexpected keyword argument" in message &&
        "source" in message
}

internal fun agentSttCompatibilityMessage(): String =
    uiText(R.string.ui_0436, "服务器语音组件与 Hermes Agent 版本不匹配。请更新 Hermes Agent，待网关重启后再试。")

package com.qingyu.hermescompanion.ui.screen

import com.qingyu.hermescompanion.model.AgentRequest
import com.qingyu.hermescompanion.model.AgentRequestChoice
import com.qingyu.hermescompanion.model.AgentRequestType
import org.junit.Assert.assertEquals
import org.junit.Test

class AgentRequestUiTest {
    private val request = AgentRequest(
        requestId = "request-1",
        runtimeSessionId = "runtime-1",
        type = AgentRequestType.CLARIFICATION,
        title = "选择常用工具",
        choices = listOf(
            AgentRequestChoice("Git", "git"),
            AgentRequestChoice("Docker", "docker"),
            AgentRequestChoice("Hermes Agent", "hermes"),
        ),
        allowMultiple = true,
    )

    @Test
    fun buildsMultiSelectAnswerInDisplayedOrder() {
        assertEquals(
            "git, hermes",
            buildAgentRequestAnswer(request, setOf("hermes", "git"), ""),
        )
    }

    @Test
    fun appendsCustomAnswerAfterSelectedChoices() {
        assertEquals(
            "docker, VS Code",
            buildAgentRequestAnswer(request, setOf("docker"), "  VS Code  "),
        )
    }

    @Test
    fun supportsFreeTextOnlyAnswer() {
        assertEquals("其他工具", buildAgentRequestAnswer(request, emptySet(), "其他工具"))
    }
}

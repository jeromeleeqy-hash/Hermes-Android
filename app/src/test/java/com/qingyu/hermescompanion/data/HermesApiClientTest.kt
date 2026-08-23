package com.qingyu.hermescompanion.data

import com.qingyu.hermescompanion.model.HermesProfileFile
import com.qingyu.hermescompanion.model.AgentRequestType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class HermesApiClientTest {
    @Test
    fun parsesLegacySingleSelectClarification() {
        val request = parseAgentRequestPayload(
            payload = mapOf(
                "request_id" to "clarify-1",
                "question" to "选择一个工具",
                "choices" to listOf("Git", "Docker"),
            ),
            fallbackSessionId = "runtime-1",
            type = AgentRequestType.CLARIFICATION,
        )

        assertEquals("选择一个工具", request.title)
        assertEquals(listOf("Git", "Docker"), request.choices.map { it.label })
        assertEquals(false, request.allowMultiple)
    }

    @Test
    fun parsesStructuredMultiSelectClarification() {
        val request = parseAgentRequestPayload(
            payload = mapOf(
                "request_id" to "clarify-2",
                "questions" to listOf(
                    mapOf(
                        "question" to "以下哪些工具你平时会用到？",
                        "options" to listOf(
                            mapOf("label" to "Git", "description" to "版本管理"),
                            mapOf("label" to "Docker", "description" to "容器"),
                            mapOf("label" to "Hermes Agent"),
                        ),
                        "multiSelect" to true,
                    ),
                ),
            ),
            fallbackSessionId = "runtime-2",
            type = AgentRequestType.CLARIFICATION,
        )

        assertEquals("以下哪些工具你平时会用到？", request.title)
        assertEquals(true, request.allowMultiple)
        assertEquals(listOf("Git", "Docker", "Hermes Agent"), request.choices.map { it.value })
        assertEquals("版本管理", request.choices.first().description)
    }

    @Test
    fun parsesNestedRequestAndSnakeCaseMultiSelect() {
        val request = parseAgentRequestPayload(
            payload = mapOf(
                "session_id" to "runtime-3",
                "request" to mapOf(
                    "id" to "clarify-3",
                    "question" to mapOf(
                        "title" to "选择需要保留的模块",
                        "choices" to listOf(
                            mapOf("text" to "会话", "value" to "sessions"),
                            mapOf("text" to "任务", "value" to "tasks"),
                        ),
                        "multi_select" to true,
                    ),
                ),
            ),
            fallbackSessionId = "fallback",
            type = AgentRequestType.CLARIFICATION,
        )

        assertEquals("clarify-3", request.requestId)
        assertEquals("runtime-3", request.runtimeSessionId)
        assertEquals("选择需要保留的模块", request.title)
        assertEquals(listOf("sessions", "tasks"), request.choices.map { it.value })
        assertEquals(true, request.allowMultiple)
    }

    @Test
    fun convertsHttpUrlToWebSocketUrl() {
        assertEquals(
            "ws://203.0.113.10:9119/api/ws?ticket=a%20b",
            toWebSocketUrl("http://203.0.113.10:9119/api/ws?ticket=a%20b"),
        )
    }

    @Test
    fun convertsHttpsUrlToSecureWebSocketUrl() {
        assertEquals(
            "wss://hermes.example.com/prefix/api/ws?ticket=abc",
            toWebSocketUrl("https://hermes.example.com/prefix/api/ws?ticket=abc"),
        )
    }

    @Test
    fun rejectsUnsupportedScheme() {
        assertThrows(IllegalArgumentException::class.java) {
            toWebSocketUrl("ftp://hermes.example.com/api/ws")
        }
    }

    @Test
    fun hidesLowLevelSocketFailureFromUser() {
        assertEquals("网络连接发生波动，正在尝试恢复", webSocketFailureMessage(null))
        assertEquals("登录状态已失效，请重新登录", webSocketFailureMessage(401))
    }

    @Test
    fun appendsEncodedProfileToRestPath() {
        assertEquals(
            "/api/sessions?limit=20&profile=work%20bench",
            appendProfileQuery("/api/sessions?limit=20", "work bench"),
        )
        assertEquals(
            "/api/config?profile=default",
            appendProfileQuery("/api/config", "default"),
        )
    }

    @Test
    fun keepsExplicitProfileQuery() {
        assertEquals(
            "/api/sessions?profile=research",
            appendProfileQuery("/api/sessions?profile=research", "default"),
        )
    }

    @Test
    fun parsesProfileCatalog() {
        val profiles = parseHermesProfiles(
            """{"profiles":[{"name":"default","is_default":true,"model":"gpt-5"},{"name":"research","description":"研究环境","skill_count":4}]}""",
        )
        assertEquals(listOf("default", "research"), profiles.map { it.name })
        assertEquals(true, profiles.first().isDefault)
        assertEquals("研究环境", profiles.last().description)
        assertEquals(4, profiles.last().skillCount)
    }

    @Test
    fun ignoresBooleanProfileDescription() {
        val profiles = parseHermesProfiles(
            """{"profiles":[{"name":"default","is_default":false,"description":false,"description_auto":false}]}""",
        )
        assertEquals("", profiles.single().description)
        assertEquals(false, profiles.single().isDefault)
        assertEquals("", profileTextValue(false))
        assertEquals("研究环境", profileTextValue(" 研究环境 "))
    }

    @Test
    fun resolvesNamedProfileMemoryAndSoulFilesFromUserHome() {
        assertEquals(
            listOf(
                "/root/.hermes/profiles/work/memories/MEMORY.md",
                "/root/profiles/work/memories/MEMORY.md",
            ),
            hermesProfileFileCandidates("/root", "work", HermesProfileFile.MEMORY),
        )
        assertEquals(
            listOf(
                "/root/.hermes/profiles/work/SOUL.md",
                "/root/profiles/work/SOUL.md",
            ),
            hermesProfileFileCandidates("/root", "work", HermesProfileFile.SOUL),
        )
    }

    @Test
    fun resolvesDefaultProfileDirectlyUnderHermesHome() {
        assertEquals(
            listOf("/root/.hermes/SOUL.md"),
            hermesProfileFileCandidates("/root/.hermes", "default", HermesProfileFile.SOUL),
        )
        assertEquals(
            listOf("/root/.hermes/memories/MEMORY.md"),
            hermesProfileFileCandidates("/root/.hermes/", "default", HermesProfileFile.MEMORY),
        )
    }

    @Test
    fun supportsHostedRootThatIsHermesHome() {
        assertEquals(
            listOf(
                "/opt/data/.hermes/profiles/personal/SOUL.md",
                "/opt/data/profiles/personal/SOUL.md",
            ),
            hermesProfileFileCandidates("/opt/data", "personal", HermesProfileFile.SOUL),
        )
        assertEquals(
            listOf("/opt/data/profiles/personal/memories/MEMORY.md"),
            hermesProfileFileCandidates(
                "/opt/data/profiles/personal",
                "personal",
                HermesProfileFile.MEMORY,
            ),
        )
    }

    @Test
    fun gatewayTurnRejectsCompletionBeforeItsStartEdge() {
        val turn = GatewayTurnTracker()

        assertEquals(false, turn.hasStarted())
        assertEquals(true, turn.markStarted())
        assertEquals(true, turn.hasStarted())
        assertEquals(false, turn.markStarted())
    }

    @Test
    fun normalizesReasoningEffortForGatewayCompatibility() {
        assertEquals("low", normalizeReasoningEffort("minimal"))
        assertEquals("max", normalizeReasoningEffort("ultra"))
        assertEquals("max", normalizeReasoningEffort("xhigh"))
        assertEquals("medium", normalizeReasoningEffort("unexpected"))
        assertEquals("high", normalizeReasoningEffort("HIGH"))
    }

}

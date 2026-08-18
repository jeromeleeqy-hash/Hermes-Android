package com.qingyu.hermescompanion.data

import com.qingyu.hermescompanion.model.HermesProfileFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class HermesApiClientTest {
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

}

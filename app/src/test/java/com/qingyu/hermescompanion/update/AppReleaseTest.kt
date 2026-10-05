package com.qingyu.hermescompanion.update

import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Files
import java.security.MessageDigest

class AppReleaseTest {
    private val pkg = "com.qingyu.hermescompanion.preview"
    private val bytes = "a signed package fixture".toByteArray()
    private val hash = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
    private fun metadata() = JSONObject().apply {
        put("schemaVersion", 1); put("packageName", pkg); put("versionCode", 383); put("versionName", "3.8.3")
        put("apkUrl", "https://download.leaier.com/releases/Hermes-Android-3.8.3-release.apk")
        put("sha256", hash); put("sizeBytes", bytes.size); put("minSdk", 26)
        put("notes", org.json.JSONArray(listOf("更清晰的首页")))
    }
    private fun release() = AppRelease.parse(metadata().toString(), pkg)
    private fun failure(reason: UpdateError, block: () -> Unit) {
        try { block(); fail("Expected $reason") } catch (e: UpdateFailure) { assertEquals(reason, e.reason) }
    }
    @Test fun validMetadataSurvivesDurableRoundTrip() {
        val release = release()
        assertEquals(383L, release.versionCode)
        assertEquals(release, AppRelease.parse(release.toJson(), pkg))
    }
    @Test fun rejectsHostConfusionTraversalQueriesCleartextAndCredentials() {
        listOf("http://download.leaier.com/releases/a.apk", "https://download.leaier.com.evil.invalid/releases/a.apk",
            "https://evil.invalid/releases/a.apk", "https://user@download.leaier.com/releases/a.apk",
            "https://download.leaier.com:444/releases/a.apk", "https://download.leaier.com/releases/../a.apk",
            "https://download.leaier.com/releases/a.apk?token=secret", "https://download.leaier.com/releases/a.apk#frag",
            "https://download.leaier.com/releases/%2Ffile.apk", "https://download.leaier.com/a.apk").forEach {
            failure(UpdateError.INVALID_MANIFEST) { AppRelease.parse(metadata().put("apkUrl", it).toString(), pkg) }
        }
    }
    @Test fun rejectsWrongBuildChannel() {
        failure(UpdateError.WRONG_PACKAGE) { AppRelease.parse(metadata().put("packageName", "another.app").toString(), pkg) }
    }
    @Test fun refusesCoercedMissingAndOutOfRangeSecurityFields() {
        listOf("versionCode" to "383", "versionCode" to 383.5, "versionCode" to -1, "schemaVersion" to 2,
            "sizeBytes" to 0, "sizeBytes" to 600_000_000L, "minSdk" to 25, "sha256" to "abc",
            "versionName" to "../../bad", "notes" to org.json.JSONArray(listOf("x".repeat(501)))).forEach { (k, v) ->
            failure(UpdateError.INVALID_MANIFEST) { AppRelease.parse(metadata().put(k, v).toString(), pkg) }
        }
        failure(UpdateError.INVALID_MANIFEST) { AppRelease.parse(metadata().apply { remove("sha256") }.toString(), pkg) }
    }
    @Test fun boundsInputSizeBeforeParsing() {
        failure(UpdateError.INVALID_MANIFEST) { AppRelease.parse(" ".repeat(65537), pkg) }
    }
    @Test fun validatesEveryDownloadedByteAndFileLength() {
        val file = Files.createTempFile("hermes-test-", ".apk").toFile()
        try {
            file.writeBytes(bytes); verifyReleaseBytes(file, release())
            file.writeBytes(bytes.copyOf().also { it[0] = 0 })
            failure(UpdateError.INTEGRITY) { verifyReleaseBytes(file, release()) }
            file.writeBytes(bytes + byteArrayOf(0))
            failure(UpdateError.INTEGRITY) { verifyReleaseBytes(file, release()) }
            file.delete()
            failure(UpdateError.MISSING_FILE) { verifyReleaseBytes(file, release()) }
        } finally { file.delete() }
    }
    @Test fun onlyNewerExactlyMatchingPackagesWithSameNonemptySignerPass() {
        val release = release()
        verifyReleaseIdentity(release, pkg, 383, "3.8.3", 382, setOf("trusted"), setOf("trusted"))
        failure(UpdateError.WRONG_PACKAGE) { verifyReleaseIdentity(release, "wrong", 383, "3.8.3", 382, setOf("a"), setOf("a")) }
        failure(UpdateError.INTEGRITY) { verifyReleaseIdentity(release, pkg, 382, "3.8.3", 381, setOf("a"), setOf("a")) }
        failure(UpdateError.INTEGRITY) { verifyReleaseIdentity(release, pkg, 383, "fake", 382, setOf("a"), setOf("a")) }
        failure(UpdateError.INTEGRITY) { verifyReleaseIdentity(release, pkg, 383, "3.8.3", 383, setOf("a"), setOf("a")) }
        failure(UpdateError.SIGNATURE) { verifyReleaseIdentity(release, pkg, 383, "3.8.3", 382, setOf("a"), setOf("b")) }
        failure(UpdateError.SIGNATURE) { verifyReleaseIdentity(release, pkg, 383, "3.8.3", 382, emptySet(), emptySet()) }
    }
    @Test fun checksPublicSourceWithoutAgentAuthorizationAndBypassesStaleCaches() {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody(metadata().toString()))
            assertEquals(release(), ReleaseClient().fetch(pkg, server.url("/latest.json").toString()))
            val req = server.takeRequest()
            assertEquals("no-cache", req.getHeader("Cache-Control"))
            assertNull(req.getHeader("Authorization")); assertNull(req.getHeader("Cookie"))
        }
    }
    @Test fun blockedUnpublishedAndServerErrorsStayDistinct() {
        MockWebServer().use { server ->
            for ((code, reason) in listOf(403 to UpdateError.FORBIDDEN, 404 to UpdateError.NOT_PUBLISHED, 500 to UpdateError.SERVER)) {
                server.enqueue(MockResponse().setResponseCode(code))
                failure(reason) { ReleaseClient().fetch(pkg, server.url("/latest.json").toString()) }
            }
        }
    }
    @Test fun refusesRedirectInsteadOfFollowingAnUntrustedUpdateEndpoint() {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(302).setHeader("Location", "http://localhost/other"))
            failure(UpdateError.SERVER) { ReleaseClient().fetch(pkg, server.url("/latest.json").toString()) }
            assertEquals(1, server.requestCount)
        }
    }
    @Test fun rejectsHtmlAndOversizedChunkedResponses() {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody("<html>Access denied</html>"))
            failure(UpdateError.INVALID_MANIFEST) { ReleaseClient().fetch(pkg, server.url("/").toString()) }
            server.enqueue(MockResponse().setChunkedBody(" ".repeat(65537), 1000))
            failure(UpdateError.INVALID_MANIFEST) { ReleaseClient().fetch(pkg, server.url("/").toString()) }
        }
    }
}

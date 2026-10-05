package com.qingyu.hermescompanion.storage

import okhttp3.Cookie
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.*
import org.junit.Test
import org.mockito.Mockito.*

class SecureCookieJarTest {
    @Test fun readingCookiesDoesNotRewriteEncryptedStorageOnEveryRequest() {
        val store = mock(SecureConfigStore::class.java)
        val jar = SecureCookieJar(store)
        val url = "https://example.com/api/status".toHttpUrl()
        val cookie = Cookie.Builder().name("session").value("private").hostOnlyDomain("example.com").path("/").build()
        jar.saveFromResponse(url, listOf(cookie))
        verify(store, times(1)).saveCookies(anyString())
        repeat(100) { assertEquals(listOf(cookie), jar.loadForRequest(url)); assertTrue(jar.hasCookies()) }
        verify(store, times(1)).saveCookies(anyString())
        assertTrue(jar.loadForRequest("https://other.example/api/status".toHttpUrl()).isEmpty())
        jar.clear(); assertTrue(jar.loadForRequest(url).isEmpty())
        verify(store, times(1)).clearCookies()
    }
}

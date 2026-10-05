package com.qingyu.hermescompanion.storage

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import org.json.JSONArray
import org.json.JSONObject

/**
 * A small persistent CookieJar for the Hermes dashboard session cookies.
 * The serialized payload is encrypted by [SecureConfigStore] with Android Keystore.
 */
class SecureCookieJar(private val store: SecureConfigStore) : CookieJar {
    private val storedCookies = mutableListOf<Cookie>()

    init {
        restore(store.readCookies())
    }

    @Synchronized
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val now = System.currentTimeMillis()
        cookies.forEach { incoming ->
            storedCookies.removeAll { current ->
                current.name == incoming.name &&
                    current.domain == incoming.domain &&
                    current.path == incoming.path
            }
            if (incoming.expiresAt > now) storedCookies += incoming
        }
        storedCookies.removeAll { it.expiresAt <= now }
        if (cookies.isNotEmpty()) persist()
    }

    @Synchronized
    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val now = System.currentTimeMillis()
        pruneAndPersist(now)
        return storedCookies.filter { it.matches(url) }
    }

    @Synchronized
    fun clear() {
        storedCookies.clear()
        store.clearCookies()
    }

    @Synchronized
    fun hasCookies(): Boolean {
        pruneAndPersist(System.currentTimeMillis())
        return storedCookies.isNotEmpty()
    }

    private fun restore(raw: String?) {
        if (raw.isNullOrBlank()) return
        runCatching {
            val array = JSONArray(raw)
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val builder = Cookie.Builder()
                    .name(item.getString("name"))
                    .value(item.getString("value"))
                    .path(item.optString("path", "/"))
                    .expiresAt(item.optLong("expiresAt", Long.MAX_VALUE))
                val domain = item.getString("domain")
                if (item.optBoolean("hostOnly", true)) builder.hostOnlyDomain(domain)
                else builder.domain(domain)
                if (item.optBoolean("secure")) builder.secure()
                if (item.optBoolean("httpOnly")) builder.httpOnly()
                storedCookies += builder.build()
            }
        }.onFailure {
            storedCookies.clear()
            store.clearCookies()
        }
        pruneAndPersist(System.currentTimeMillis())
    }

    private fun pruneAndPersist(now: Long) {
        val changed = storedCookies.removeAll { it.expiresAt <= now }
        if (changed) {
            if (storedCookies.isEmpty()) store.clearCookies() else persist()
        }
    }

    private fun persist() {
        val array = JSONArray()
        storedCookies.forEach { cookie ->
            array.put(
                JSONObject()
                    .put("name", cookie.name)
                    .put("value", cookie.value)
                    .put("domain", cookie.domain)
                    .put("path", cookie.path)
                    .put("expiresAt", cookie.expiresAt)
                    .put("secure", cookie.secure)
                    .put("httpOnly", cookie.httpOnly)
                    .put("hostOnly", cookie.hostOnly),
            )
        }
        store.saveCookies(array.toString())
    }
}

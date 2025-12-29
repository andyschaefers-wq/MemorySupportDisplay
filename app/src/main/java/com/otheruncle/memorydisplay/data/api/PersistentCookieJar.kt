package com.otheruncle.memorydisplay.data.api

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import javax.inject.Inject
import javax.inject.Singleton

private val Context.cookieDataStore: DataStore<Preferences> by preferencesDataStore(name = "cookies")

/**
 * Persistent cookie jar that stores session cookies across app restarts.
 * This is essential for maintaining authentication with the CodeIgniter backend
 * which uses session-based authentication via cookies.
 */
@Singleton
class PersistentCookieJar @Inject constructor(
    @ApplicationContext private val context: Context
) : CookieJar {

    private val cookiesKey = stringSetPreferencesKey("session_cookies")

    // In-memory cache for current session
    private val cookieStore = mutableMapOf<String, MutableList<Cookie>>()

    init {
        // Load persisted cookies on initialization
        runBlocking {
            loadPersistedCookies()
        }
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val host = url.host
        if (cookieStore[host] == null) {
            cookieStore[host] = mutableListOf()
        }

        cookies.forEach { cookie ->
            // Remove old cookie with same name
            cookieStore[host]?.removeAll { it.name == cookie.name }
            // Add new cookie if not expired
            if (cookie.expiresAt > System.currentTimeMillis()) {
                cookieStore[host]?.add(cookie)
            }
        }

        // Persist session cookies
        runBlocking {
            persistCookies()
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val host = url.host
        val cookies = cookieStore[host] ?: return emptyList()

        // Filter out expired cookies
        val validCookies = cookies.filter { it.expiresAt > System.currentTimeMillis() }

        // Update store if we removed expired cookies
        if (validCookies.size != cookies.size) {
            cookieStore[host] = validCookies.toMutableList()
            runBlocking {
                persistCookies()
            }
        }

        return validCookies
    }

    /**
     * Clear all cookies (used on logout)
     */
    suspend fun clearCookies() {
        cookieStore.clear()
        context.cookieDataStore.edit { prefs ->
            prefs.remove(cookiesKey)
        }
    }

    /**
     * Check if we have a valid session cookie
     */
    fun hasSessionCookie(): Boolean {
        return cookieStore.values.flatten().any { cookie ->
            cookie.name.contains("ci_session", ignoreCase = true) &&
                    cookie.expiresAt > System.currentTimeMillis()
        }
    }

    private suspend fun persistCookies() {
        val cookieStrings = cookieStore.flatMap { (host, cookies) ->
            cookies.map { cookie ->
                serializeCookie(host, cookie)
            }
        }.toSet()

        context.cookieDataStore.edit { prefs ->
            prefs[cookiesKey] = cookieStrings
        }
    }

    private suspend fun loadPersistedCookies() {
        try {
            val prefs = context.cookieDataStore.data.first()
            val cookieStrings = prefs[cookiesKey] ?: return

            cookieStrings.forEach { cookieString ->
                val cookie = deserializeCookie(cookieString)
                if (cookie != null && cookie.second.expiresAt > System.currentTimeMillis()) {
                    val host = cookie.first
                    if (cookieStore[host] == null) {
                        cookieStore[host] = mutableListOf()
                    }
                    cookieStore[host]?.add(cookie.second)
                }
            }
        } catch (e: Exception) {
            // If we fail to load cookies, just start fresh
            cookieStore.clear()
        }
    }

    private fun serializeCookie(host: String, cookie: Cookie): String {
        return "$host|${cookie.name}|${cookie.value}|${cookie.expiresAt}|${cookie.domain}|${cookie.path}|${cookie.secure}|${cookie.httpOnly}"
    }

    private fun deserializeCookie(cookieString: String): Pair<String, Cookie>? {
        return try {
            val parts = cookieString.split("|")
            if (parts.size < 8) return null

            val host = parts[0]
            val cookie = Cookie.Builder()
                .name(parts[1])
                .value(parts[2])
                .expiresAt(parts[3].toLong())
                .domain(parts[4])
                .path(parts[5])
                .apply {
                    if (parts[6].toBoolean()) secure()
                    if (parts[7].toBoolean()) httpOnly()
                }
                .build()

            Pair(host, cookie)
        } catch (e: Exception) {
            null
        }
    }
}

package com.otheruncle.memorydisplay.data.api

import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OkHttp interceptor that detects 401 Unauthorized responses
 * and notifies the SessionManager to trigger a global logout.
 * 
 * This ensures that expired sessions are handled consistently
 * regardless of which screen the user is on.
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val sessionManager: SessionManager
) : Interceptor {
    
    // Track if we've already emitted a session expired event to avoid duplicates
    @Volatile
    private var hasEmittedExpiry = false
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)
        
        // Check for 401 Unauthorized
        if (response.code == 401) {
            // Don't emit for auth endpoints (login, forgot-password, etc.)
            val path = request.url.encodedPath
            val isAuthEndpoint = path.contains("/auth/login") || 
                                 path.contains("/auth/forgot-password") ||
                                 path.contains("/auth/reset-password") ||
                                 path.contains("/auth/setup")
            
            if (!isAuthEndpoint && !hasEmittedExpiry) {
                hasEmittedExpiry = true
                runBlocking {
                    sessionManager.onSessionExpired()
                }
            }
        }
        
        return response
    }
    
    /**
     * Reset the expiry flag (called after successful login)
     */
    fun resetExpiryFlag() {
        hasEmittedExpiry = false
    }
}

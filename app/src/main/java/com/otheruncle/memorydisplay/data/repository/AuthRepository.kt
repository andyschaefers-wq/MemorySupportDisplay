package com.otheruncle.memorydisplay.data.repository

import com.otheruncle.memorydisplay.data.api.ApiContext
import com.otheruncle.memorydisplay.data.api.ApiService
import com.otheruncle.memorydisplay.data.api.NetworkResult
import com.otheruncle.memorydisplay.data.api.PersistentCookieJar
import com.otheruncle.memorydisplay.data.api.safeApiCall
import com.otheruncle.memorydisplay.data.model.*
import com.otheruncle.memorydisplay.data.push.PushTokenManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val apiService: ApiService,
    private val cookieJar: PersistentCookieJar,
    private val userPreferences: UserPreferences,
    private val pushTokenManager: PushTokenManager
) {

    /**
     * Check if user has an active session
     */
    fun hasActiveSession(): Boolean {
        return cookieJar.hasSessionCookie()
    }

    /**
     * Login with email and password
     * If saveCredentials is true, stores credentials for biometric auto-login
     */
    suspend fun login(email: String, password: String, saveCredentials: Boolean = false): NetworkResult<User> {
        val result = safeApiCall(ApiContext.LOGIN) {
            apiService.login(LoginRequest(email, password))
        }

        return when (result) {
            is NetworkResult.Success -> {
                val response = result.data
                if (response.success && response.user != null) {
                    // Save user data locally
                    userPreferences.saveUser(response.user)
                    // Save credentials if biometric is enabled
                    if (saveCredentials) {
                        userPreferences.saveCredentials(email, password)
                    }
                    // Register FCM token with backend
                    pushTokenManager.registerCurrentToken()
                    NetworkResult.Success(response.user)
                } else {
                    NetworkResult.Error(response.error ?: "Login failed")
                }
            }
            is NetworkResult.Error -> result
            is NetworkResult.Loading -> NetworkResult.Loading
        }
    }

    /**
     * Login using stored credentials (for biometric auto-login)
     */
    suspend fun loginWithStoredCredentials(): NetworkResult<User> {
        val credentials = userPreferences.getStoredCredentials()
            ?: return NetworkResult.Error("No stored credentials")
        
        return login(credentials.first, credentials.second, saveCredentials = true)
    }

    /**
     * Check if we have stored credentials for biometric login
     */
    suspend fun hasStoredCredentials(): Boolean {
        return userPreferences.hasStoredCredentials()
    }

    /**
     * Logout - clears session and local data
     */
    suspend fun logout(): NetworkResult<Unit> {
        // Unregister FCM token first (while we still have auth)
        pushTokenManager.unregisterToken()
        
        // Call API to invalidate server session
        val result = safeApiCall { apiService.logout() }

        // Clear local data regardless of API result
        cookieJar.clearCookies()
        userPreferences.clearUser()

        return when (result) {
            is NetworkResult.Success -> NetworkResult.Success(Unit)
            is NetworkResult.Error -> {
                // Still consider logout successful even if API call fails
                NetworkResult.Success(Unit)
            }
            is NetworkResult.Loading -> NetworkResult.Loading
        }
    }

    /**
     * Request password reset email
     */
    suspend fun forgotPassword(email: String): NetworkResult<String> {
        val result = safeApiCall(ApiContext.AUTH) {
            apiService.forgotPassword(ForgotPasswordRequest(email))
        }

        return when (result) {
            is NetworkResult.Success -> {
                NetworkResult.Success(result.data.message ?: "If an account exists, a reset link has been sent")
            }
            is NetworkResult.Error -> result
            is NetworkResult.Loading -> NetworkResult.Loading
        }
    }

    /**
     * Complete account setup from invitation
     */
    suspend fun setup(token: String, password: String): NetworkResult<User> {
        val result = safeApiCall(ApiContext.AUTH) {
            apiService.setup(SetupRequest(token, password))
        }

        return when (result) {
            is NetworkResult.Success -> {
                val response = result.data
                if (response.success && response.user != null) {
                    userPreferences.saveUser(response.user)
                    // Register FCM token with backend
                    pushTokenManager.registerCurrentToken()
                    NetworkResult.Success(response.user)
                } else {
                    NetworkResult.Error(response.error ?: "Setup failed")
                }
            }
            is NetworkResult.Error -> result
            is NetworkResult.Loading -> NetworkResult.Loading
        }
    }

    /**
     * Get current user from local storage
     */
    suspend fun getCurrentUser(): User? {
        return userPreferences.getUser()
    }

    /**
     * Refresh user data from server
     */
    suspend fun refreshUser(): NetworkResult<User> {
        val result = safeApiCall { apiService.getProfile() }

        return when (result) {
            is NetworkResult.Success -> {
                val response = result.data
                if (response.success && response.user != null) {
                    userPreferences.saveUser(response.user)
                    NetworkResult.Success(response.user)
                } else {
                    NetworkResult.Error(response.error ?: "Failed to refresh user")
                }
            }
            is NetworkResult.Error -> result
            is NetworkResult.Loading -> NetworkResult.Loading
        }
    }
}

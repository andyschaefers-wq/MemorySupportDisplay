package com.otheruncle.memorydisplay.data.push

import android.content.Context
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.otheruncle.memorydisplay.data.api.ApiService
import com.otheruncle.memorydisplay.data.model.PushRegisterRequest
import com.otheruncle.memorydisplay.data.model.PushUnregisterRequest
import com.otheruncle.memorydisplay.data.repository.UserPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages FCM token registration with the backend server
 */
@Singleton
class PushTokenManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apiService: ApiService,
    private val userPreferences: UserPreferences
) {
    companion object {
        private const val TAG = "PushTokenManager"
        private const val PREFS_NAME = "push_prefs"
        private const val KEY_DEVICE_ID = "device_id"
    }

    /**
     * Get or create a unique device ID for this installation
     * Uses SharedPreferences to persist across app restarts
     */
    private fun getDeviceId(): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var deviceId = prefs.getString(KEY_DEVICE_ID, null)
        
        if (deviceId == null) {
            deviceId = UUID.randomUUID().toString()
            prefs.edit().putString(KEY_DEVICE_ID, deviceId).apply()
        }
        
        return deviceId
    }

    /**
     * Called when a new FCM token is generated
     * Registers the token with the backend if user is logged in
     */
    suspend fun onNewToken(token: String) {
        Log.d(TAG, "Processing new FCM token")
        
        // Check if user is logged in by seeing if we have user data
        val user = userPreferences.getUser()
        if (user == null) {
            Log.d(TAG, "User not logged in, skipping token registration")
            return
        }

        registerToken(token)
    }

    /**
     * Register the current FCM token with the backend
     * Should be called after successful login
     */
    suspend fun registerCurrentToken() {
        try {
            val token = FirebaseMessaging.getInstance().token.await()
            Log.d(TAG, "Got FCM token, registering with backend")
            registerToken(token)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get FCM token", e)
        }
    }

    /**
     * Register a specific token with the backend
     */
    private suspend fun registerToken(token: String) {
        try {
            val deviceId = getDeviceId()
            val request = PushRegisterRequest(token = token, deviceId = deviceId)
            
            val response = apiService.registerPushToken(request)
            
            if (response.isSuccessful) {
                Log.d(TAG, "FCM token registered successfully")
            } else {
                Log.e(TAG, "Failed to register FCM token: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error registering FCM token", e)
        }
    }

    /**
     * Unregister the FCM token from the backend
     * Should be called on logout
     */
    suspend fun unregisterToken() {
        try {
            val deviceId = getDeviceId()
            val request = PushUnregisterRequest(deviceId = deviceId)
            
            val response = apiService.unregisterPushToken(request)
            
            if (response.isSuccessful) {
                Log.d(TAG, "FCM token unregistered successfully")
            } else {
                Log.e(TAG, "Failed to unregister FCM token: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering FCM token", e)
        }
    }
}

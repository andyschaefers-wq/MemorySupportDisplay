package com.otheruncle.memorydisplay.data.auth

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Result of a biometric authentication attempt
 */
sealed class BiometricResult {
    data object Success : BiometricResult()
    data object Cancelled : BiometricResult()
    data class Failed(val attemptsRemaining: Int) : BiometricResult()
    data object TooManyAttempts : BiometricResult()
    data class Error(val message: String) : BiometricResult()
}

/**
 * Helper class to manage biometric authentication.
 * Handles checking device capabilities and showing the biometric prompt.
 */
@Singleton
class BiometricHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val biometricManager = BiometricManager.from(context)
    
    /**
     * Check if the device supports biometric authentication
     */
    fun isBiometricAvailable(): Boolean {
        return when (biometricManager.canAuthenticate(BIOMETRIC_STRONG or BIOMETRIC_WEAK)) {
            BiometricManager.BIOMETRIC_SUCCESS -> true
            else -> false
        }
    }
    
    /**
     * Get a descriptive message about biometric availability
     */
    fun getBiometricStatusMessage(): String? {
        return when (biometricManager.canAuthenticate(BIOMETRIC_STRONG or BIOMETRIC_WEAK)) {
            BiometricManager.BIOMETRIC_SUCCESS -> null
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> "No biometric hardware available"
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> "Biometric hardware unavailable"
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> "No biometric credentials enrolled"
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> "Security update required"
            else -> "Biometric authentication unavailable"
        }
    }
    
    /**
     * Show the biometric prompt for authentication.
     * 
     * @param activity The FragmentActivity to show the prompt in
     * @param title Title for the biometric prompt
     * @param subtitle Subtitle for the biometric prompt
     * @param negativeButtonText Text for the cancel/password fallback button
     * @param onResult Callback with the result of the authentication attempt
     */
    fun showBiometricPrompt(
        activity: FragmentActivity,
        title: String = "Biometric Login",
        subtitle: String = "Use your fingerprint to unlock",
        negativeButtonText: String = "Use Password",
        maxAttempts: Int = 5,
        onResult: (BiometricResult) -> Unit
    ) {
        var failedAttempts = 0
        
        val executor = ContextCompat.getMainExecutor(activity)
        
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onResult(BiometricResult.Success)
            }
            
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                when (errorCode) {
                    BiometricPrompt.ERROR_USER_CANCELED,
                    BiometricPrompt.ERROR_NEGATIVE_BUTTON -> {
                        onResult(BiometricResult.Cancelled)
                    }
                    BiometricPrompt.ERROR_LOCKOUT,
                    BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> {
                        onResult(BiometricResult.TooManyAttempts)
                    }
                    else -> {
                        onResult(BiometricResult.Error(errString.toString()))
                    }
                }
            }
            
            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                failedAttempts++
                val remaining = maxAttempts - failedAttempts
                if (remaining <= 0) {
                    onResult(BiometricResult.TooManyAttempts)
                } else {
                    onResult(BiometricResult.Failed(remaining))
                }
            }
        }
        
        val biometricPrompt = BiometricPrompt(activity, executor, callback)
        
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText(negativeButtonText)
            .setAllowedAuthenticators(BIOMETRIC_STRONG or BIOMETRIC_WEAK)
            .build()
        
        biometricPrompt.authenticate(promptInfo)
    }
}

package com.otheruncle.memorydisplay

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.otheruncle.memorydisplay.data.api.AuthInterceptor
import com.otheruncle.memorydisplay.data.api.NetworkResult
import com.otheruncle.memorydisplay.data.api.SessionManager
import com.otheruncle.memorydisplay.data.repository.AuthRepository
import com.otheruncle.memorydisplay.data.repository.CalendarReviewRepository
import com.otheruncle.memorydisplay.data.repository.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MainUiState(
    val isLoggedIn: Boolean = false,
    val isCalendarReviewer: Boolean = false,
    val pendingReviewCount: Int = 0,
    val isLoading: Boolean = true,
    val showSessionExpiredDialog: Boolean = false,
    val requiresBiometric: Boolean = false,
    val biometricAutoLoginInProgress: Boolean = false,
    val biometricLoginError: String? = null
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userPreferences: UserPreferences,
    private val calendarReviewRepository: CalendarReviewRepository,
    private val sessionManager: SessionManager,
    private val authInterceptor: AuthInterceptor
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        checkAuthState()
        observeUser()
        observeSessionExpiry()
    }

    private fun checkAuthState() {
        viewModelScope.launch {
            val hasSession = authRepository.hasActiveSession()
            val user = authRepository.getCurrentUser()
            val biometricEnabled = userPreferences.isBiometricEnabled()
            val hasStoredCredentials = authRepository.hasStoredCredentials()

            // Determine if we need biometric authentication
            val requiresBiometric = biometricEnabled && hasStoredCredentials && (
                // Case 1: Has active session - biometric unlocks the app
                (hasSession && user != null) ||
                // Case 2: No session but has stored credentials - biometric triggers auto-login
                (!hasSession && hasStoredCredentials)
            )

            _uiState.update {
                it.copy(
                    isLoggedIn = hasSession && user != null,
                    isCalendarReviewer = user?.calendarReviewer == true,
                    isLoading = false,
                    requiresBiometric = requiresBiometric
                )
            }

            // If logged in as reviewer, fetch pending count
            if (hasSession && user?.calendarReviewer == true) {
                refreshPendingCount()
            }
        }
    }

    private fun observeUser() {
        viewModelScope.launch {
            userPreferences.observeUser().collect { user ->
                _uiState.update {
                    it.copy(
                        isLoggedIn = user != null,
                        isCalendarReviewer = user?.calendarReviewer == true
                    )
                }

                // Refresh pending count when user becomes reviewer
                if (user?.calendarReviewer == true) {
                    refreshPendingCount()
                }
            }
        }
    }

    private fun observeSessionExpiry() {
        viewModelScope.launch {
            sessionManager.sessionExpired.collect {
                // Show dialog then clear session
                _uiState.update { it.copy(showSessionExpiredDialog = true) }
            }
        }
    }

    fun refreshPendingCount() {
        viewModelScope.launch {
            if (_uiState.value.isCalendarReviewer) {
                when (val result = calendarReviewRepository.getPendingCount()) {
                    is NetworkResult.Success -> {
                        _uiState.update { it.copy(pendingReviewCount = result.data) }
                    }
                    else -> {
                        // Silently fail - badge just won't update
                    }
                }
            }
        }
    }

    /**
     * Called when user dismisses the session expired dialog
     */
    fun onSessionExpiredDialogDismissed() {
        viewModelScope.launch {
            // Clear session data
            authRepository.logout()
            _uiState.update {
                MainUiState(isLoading = false, showSessionExpiredDialog = false)
            }
        }
    }

    /**
     * Called when biometric authentication succeeds
     */
    fun onBiometricSuccess() {
        viewModelScope.launch {
            // Check if we have an active session or need to auto-login
            val hasSession = authRepository.hasActiveSession()
            
            if (hasSession) {
                // Just unlock the app
                _uiState.update { it.copy(requiresBiometric = false) }
            } else {
                // Need to auto-login with stored credentials
                _uiState.update { 
                    it.copy(
                        requiresBiometric = false, 
                        biometricAutoLoginInProgress = true 
                    ) 
                }
                
                when (val result = authRepository.loginWithStoredCredentials()) {
                    is NetworkResult.Success -> {
                        authInterceptor.resetExpiryFlag()
                        _uiState.update { 
                            it.copy(
                                isLoggedIn = true,
                                isCalendarReviewer = result.data.calendarReviewer,
                                biometricAutoLoginInProgress = false
                            ) 
                        }
                        if (result.data.calendarReviewer) {
                            refreshPendingCount()
                        }
                    }
                    is NetworkResult.Error -> {
                        // Auto-login failed - show login screen with error
                        _uiState.update { 
                            it.copy(
                                biometricAutoLoginInProgress = false,
                                biometricLoginError = result.message
                            ) 
                        }
                    }
                    is NetworkResult.Loading -> { /* no-op */ }
                }
            }
        }
    }

    /**
     * Called when biometric authentication fails (too many attempts or user cancels)
     */
    fun onBiometricFailed() {
        viewModelScope.launch {
            // Don't clear credentials - just require manual login this time
            _uiState.update {
                it.copy(requiresBiometric = false)
            }
        }
    }
    
    /**
     * Clear biometric login error after showing
     */
    fun clearBiometricLoginError() {
        _uiState.update { it.copy(biometricLoginError = null) }
    }

    fun onLogout() {
        viewModelScope.launch {
            authRepository.logout()
            _uiState.update {
                MainUiState(isLoading = false)
            }
        }
    }

    /**
     * Called after successful login to reset the auth interceptor flag
     */
    fun onLoginSuccess() {
        authInterceptor.resetExpiryFlag()
    }
}

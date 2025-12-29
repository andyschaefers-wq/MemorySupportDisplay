package com.otheruncle.memorydisplay.ui.profile

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.otheruncle.memorydisplay.data.api.NetworkResult
import com.otheruncle.memorydisplay.data.auth.BiometricHelper
import com.otheruncle.memorydisplay.data.model.User
import com.otheruncle.memorydisplay.data.repository.AuthRepository
import com.otheruncle.memorydisplay.data.repository.ProfileRepository
import com.otheruncle.memorydisplay.data.repository.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val user: User? = null,
    val name: String = "",
    val email: String = "",
    val birthday: String = "",  // MM-DD format for display
    val city: String = "",
    val state: String = "",
    val timezone: String = "",
    val calendarUrl: String = "",
    val eventReminders: Boolean = false,
    val biometricEnabled: Boolean = false,
    val biometricAvailable: Boolean = false,
    val biometricStatusMessage: String? = null,
    val showBiometricPasswordDialog: Boolean = false,
    val biometricPasswordError: String? = null,
    val isVerifyingPassword: Boolean = false,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    
    // Profile photo
    val selectedPhotoUri: Uri? = null,
    val isUploadingPhoto: Boolean = false,
    
    // Flag to indicate if we have pending edits that should be preserved
    val hasRestoredState: Boolean = false
) {
    // Convert user's stored birthday (YYYY-MM-DD) to display format (MM-DD) for comparison
    private val userBirthdayDisplay: String
        get() = user?.birthday?.let { bday ->
            if (bday.length >= 10 && bday.contains("-")) bday.substring(5) else bday
        } ?: ""
    
    val hasChanges: Boolean
        get() = user != null && (
            birthday != userBirthdayDisplay ||
            city != (user.city ?: "") ||
            state != (user.state ?: "") ||
            timezone != (user.timezone ?: "") ||
            calendarUrl != (user.calendarUrl ?: "") ||
            eventReminders != user.eventReminders
        )
    
    // Convert MM-DD to YYYY-MM-DD for API (using placeholder year 0000)
    val birthdayForApi: String?
        get() = birthday.ifBlank { null }?.let { bday ->
            if (bday.matches(Regex("\\d{2}-\\d{2}"))) {
                "0000-$bday"
            } else {
                bday
            }
        }
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val authRepository: AuthRepository,
    private val userPreferences: UserPreferences,
    private val biometricHelper: BiometricHelper,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    // Keys for SavedStateHandle
    private companion object {
        const val KEY_BIRTHDAY = "birthday"
        const val KEY_CITY = "city"
        const val KEY_STATE = "state"
        const val KEY_TIMEZONE = "timezone"
        const val KEY_CALENDAR_URL = "calendarUrl"
        const val KEY_EVENT_REMINDERS = "eventReminders"
        const val KEY_HAS_PENDING_EDITS = "hasPendingEdits"
    }
    
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()
    
    init {
        refreshProfile()
        checkBiometricAvailability()
    }
    
    private fun checkBiometricAvailability() {
        viewModelScope.launch {
            val available = biometricHelper.isBiometricAvailable()
            val statusMessage = biometricHelper.getBiometricStatusMessage()
            val enabled = userPreferences.isBiometricEnabled()
            
            _uiState.update {
                it.copy(
                    biometricAvailable = available,
                    biometricStatusMessage = statusMessage,
                    biometricEnabled = enabled && available
                )
            }
        }
    }
    
    /**
     * Public function to refresh profile (launches coroutine)
     */
    fun refreshProfile() {
        viewModelScope.launch {
            loadProfile()
        }
    }
    
    /**
     * Private suspend function to actually load profile
     * Preserves any pending edits from SavedStateHandle if activity was recreated
     */
    private suspend fun loadProfile() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        
        when (val result = profileRepository.getProfile()) {
            is NetworkResult.Success -> {
                val user = result.data
                
                // Check if we have pending edits that need to be restored
                val hasPendingEdits = savedStateHandle.get<Boolean>(KEY_HAS_PENDING_EDITS) ?: false
                
                if (hasPendingEdits) {
                    // Restore form values from SavedStateHandle
                    val restoredBirthday = savedStateHandle.get<String>(KEY_BIRTHDAY) ?: ""
                    val restoredCity = savedStateHandle.get<String>(KEY_CITY) ?: ""
                    val restoredState = savedStateHandle.get<String>(KEY_STATE) ?: ""
                    val restoredTimezone = savedStateHandle.get<String>(KEY_TIMEZONE) ?: ""
                    val restoredCalendarUrl = savedStateHandle.get<String>(KEY_CALENDAR_URL) ?: ""
                    val restoredEventReminders = savedStateHandle.get<Boolean>(KEY_EVENT_REMINDERS) ?: false
                    
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            user = user,
                            name = user.name,
                            email = user.email,
                            birthday = restoredBirthday,
                            city = restoredCity,
                            state = restoredState,
                            timezone = restoredTimezone,
                            calendarUrl = restoredCalendarUrl,
                            eventReminders = restoredEventReminders,
                            hasRestoredState = true
                        )
                    }
                } else {
                    // No pending edits - load from server
                    // Convert YYYY-MM-DD to MM-DD for display
                    val displayBirthday = user.birthday?.let { bday ->
                        if (bday.length >= 10 && bday.contains("-")) {
                            bday.substring(5)
                        } else {
                            bday
                        }
                    } ?: ""
                    
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            user = user,
                            name = user.name,
                            email = user.email,
                            birthday = displayBirthday,
                            city = user.city ?: "",
                            state = user.state ?: "",
                            timezone = user.timezone ?: "",
                            calendarUrl = user.calendarUrl ?: "",
                            eventReminders = user.eventReminders
                        )
                    }
                }
            }
            is NetworkResult.Error -> {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = result.message
                    )
                }
            }
            is NetworkResult.Loading -> { /* no-op */ }
        }
    }
    
    /**
     * Save current form state to SavedStateHandle
     * Called when form values change to preserve them across process death
     */
    private fun saveFormState() {
        val state = _uiState.value
        savedStateHandle[KEY_BIRTHDAY] = state.birthday
        savedStateHandle[KEY_CITY] = state.city
        savedStateHandle[KEY_STATE] = state.state
        savedStateHandle[KEY_TIMEZONE] = state.timezone
        savedStateHandle[KEY_CALENDAR_URL] = state.calendarUrl
        savedStateHandle[KEY_EVENT_REMINDERS] = state.eventReminders
        savedStateHandle[KEY_HAS_PENDING_EDITS] = state.hasChanges
    }
    
    /**
     * Clear saved form state (called after successful save)
     */
    private fun clearSavedFormState() {
        savedStateHandle[KEY_HAS_PENDING_EDITS] = false
    }
    
    fun updateBirthday(value: String) {
        _uiState.update { it.copy(birthday = value) }
        saveFormState()
    }
    
    fun updateCity(value: String) {
        _uiState.update { it.copy(city = value) }
        saveFormState()
    }
    
    fun updateState(value: String) {
        _uiState.update { it.copy(state = value) }
        saveFormState()
    }
    
    fun updateTimezone(value: String) {
        _uiState.update { it.copy(timezone = value) }
        saveFormState()
    }
    
    fun updateCalendarUrl(value: String) {
        _uiState.update { it.copy(calendarUrl = value) }
        saveFormState()
    }
    
    fun updateEventReminders(value: Boolean) {
        _uiState.update { it.copy(eventReminders = value) }
        saveFormState()
    }
    
    fun updateBiometricEnabled(value: Boolean) {
        viewModelScope.launch {
            if (value) {
                // When enabling biometric, show password dialog to save credentials
                _uiState.update { 
                    it.copy(
                        showBiometricPasswordDialog = true,
                        biometricPasswordError = null
                    ) 
                }
            } else {
                // When disabling, clear credentials and update preference
                userPreferences.setBiometricEnabled(false)
                userPreferences.clearCredentials()
                _uiState.update { it.copy(biometricEnabled = false) }
            }
        }
    }
    
    fun dismissBiometricPasswordDialog() {
        _uiState.update { 
            it.copy(
                showBiometricPasswordDialog = false,
                biometricPasswordError = null
            ) 
        }
    }
    
    fun confirmBiometricWithPassword(password: String) {
        val email = _uiState.value.email
        if (email.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(biometricPasswordError = "Please enter your password") }
            return
        }
        
        viewModelScope.launch {
            _uiState.update { it.copy(isVerifyingPassword = true, biometricPasswordError = null) }
            
            // Verify password by attempting login
            when (val result = authRepository.login(email, password, saveCredentials = true)) {
                is NetworkResult.Success -> {
                    // Password verified, enable biometric
                    userPreferences.setBiometricEnabled(true)
                    _uiState.update { 
                        it.copy(
                            isVerifyingPassword = false,
                            showBiometricPasswordDialog = false,
                            biometricEnabled = true,
                            successMessage = "Biometric unlock enabled"
                        ) 
                    }
                }
                is NetworkResult.Error -> {
                    _uiState.update { 
                        it.copy(
                            isVerifyingPassword = false,
                            biometricPasswordError = "Incorrect password"
                        ) 
                    }
                }
                is NetworkResult.Loading -> { /* no-op */ }
            }
        }
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    fun clearSuccessMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }
    
    fun uploadProfilePhoto(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(selectedPhotoUri = uri, isUploadingPhoto = true) }
            
            when (val result = profileRepository.uploadProfilePhoto(uri)) {
                is NetworkResult.Success -> {
                    // Reload profile and wait for it to complete
                    loadProfile()
                    _uiState.update { 
                        it.copy(
                            isUploadingPhoto = false, 
                            selectedPhotoUri = null,
                            successMessage = "Profile photo updated"
                        ) 
                    }
                }
                is NetworkResult.Error -> {
                    _uiState.update { 
                        it.copy(
                            isUploadingPhoto = false, 
                            selectedPhotoUri = null,
                            error = result.message
                        ) 
                    }
                }
                is NetworkResult.Loading -> { /* no-op */ }
            }
        }
    }
    
    fun saveProfile() {
        val state = _uiState.value
        if (!state.hasChanges) return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            
            val result = profileRepository.updateProfile(
                birthday = state.birthdayForApi,
                city = state.city.ifBlank { null },
                state = state.state.ifBlank { null },
                timezone = state.timezone.ifBlank { null },
                calendarUrl = state.calendarUrl.ifBlank { null },
                eventReminders = state.eventReminders
            )
            
            when (result) {
                is NetworkResult.Success -> {
                    val user = result.data
                    clearSavedFormState()  // Clear saved state after successful save
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            user = user,
                            successMessage = "Profile updated successfully"
                        )
                    }
                }
                is NetworkResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            error = result.message
                        )
                    }
                }
                is NetworkResult.Loading -> { /* no-op */ }
            }
        }
    }
    
    fun logout() {
        viewModelScope.launch {
            clearSavedFormState()  // Clear saved state on logout
            authRepository.logout()
        }
    }
}

data class ChangePasswordUiState(
    val currentPassword: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null
) {
    val isValid: Boolean
        get() = currentPassword.isNotBlank() && 
                newPassword.length >= 8 && 
                newPassword == confirmPassword
    
    val passwordsMatch: Boolean
        get() = confirmPassword.isEmpty() || newPassword == confirmPassword
}

@HiltViewModel
class ChangePasswordViewModel @Inject constructor(
    private val profileRepository: ProfileRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ChangePasswordUiState())
    val uiState: StateFlow<ChangePasswordUiState> = _uiState.asStateFlow()
    
    fun updateCurrentPassword(value: String) {
        _uiState.update { it.copy(currentPassword = value) }
    }
    
    fun updateNewPassword(value: String) {
        _uiState.update { it.copy(newPassword = value) }
    }
    
    fun updateConfirmPassword(value: String) {
        _uiState.update { it.copy(confirmPassword = value) }
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    fun changePassword() {
        val state = _uiState.value
        if (!state.isValid) return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            
            when (val result = profileRepository.changePassword(
                currentPassword = state.currentPassword,
                newPassword = state.newPassword
            )) {
                is NetworkResult.Success -> {
                    _uiState.update { it.copy(isSaving = false, isSaved = true) }
                }
                is NetworkResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            error = result.message
                        )
                    }
                }
                is NetworkResult.Loading -> { /* no-op */ }
            }
        }
    }
}

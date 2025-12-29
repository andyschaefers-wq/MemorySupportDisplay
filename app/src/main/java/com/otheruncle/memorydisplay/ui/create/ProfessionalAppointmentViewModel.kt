package com.otheruncle.memorydisplay.ui.create

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.otheruncle.memorydisplay.data.api.NetworkResult
import com.otheruncle.memorydisplay.data.model.CardDataRequest
import com.otheruncle.memorydisplay.data.model.CardType
import com.otheruncle.memorydisplay.data.api.ApiService
import com.otheruncle.memorydisplay.data.api.safeApiCall
import com.otheruncle.memorydisplay.data.repository.CardsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Icon options for professional appointments
 */
data class IconOption(
    val id: String,
    val displayName: String
)

val professionalIcons = listOf(
    IconOption("stethoscope", "Doctor"),
    IconOption("smile", "Dentist"),
    IconOption("eye", "Eye Doctor"),
    IconOption("heart-pulse", "Nurse"),
    IconOption("calculator", "Accountant"),
    IconOption("scale", "Lawyer"),
    IconOption("landmark", "Financial Advisor"),
    IconOption("paw-print", "Veterinarian"),
    IconOption("dog", "Dog Groomer"),
    IconOption("wrench", "Car Mechanic"),
    IconOption("plus", "Medical (Other)"),
    IconOption("briefcase", "Professional (Other)")
)

/**
 * Driver option for dropdown
 */
data class DriverOption(
    val id: Int?,
    val name: String,
    val profilePhoto: String? = null
)

/**
 * Form state for professional appointment
 */
data class ProfessionalAppointmentFormState(
    // Edit mode
    val cardId: Int? = null,
    val isEditMode: Boolean = false,
    
    // Form fields
    val icon: String = "stethoscope",
    val professionalName: String = "",
    val professionalType: String = "",
    val eventDate: String = "",
    val eventTime: String = "",
    val purpose: String = "",
    val location: String = "",
    val driverId: Int? = null,
    val driverName: String = "",
    val transportationNotes: String = "",
    val preparationNotes: String = "",
    val narrative: String = "",
    val allowOthersEdit: Boolean = false,
    
    // Image fields
    val selectedImageUri: Uri? = null,
    val existingImagePath: String? = null,
    val isUploadingImage: Boolean = false,
    
    // UI state
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val isSaved: Boolean = false,
    
    // Family members for driver dropdown
    val familyMembers: List<DriverOption> = emptyList(),
    val isLoadingFamily: Boolean = true
) {
    val isValid: Boolean
        get() = professionalName.isNotBlank() &&
                eventDate.isNotBlank() &&
                eventTime.isNotBlank() &&
                purpose.isNotBlank()
}

@HiltViewModel
class ProfessionalAppointmentViewModel @Inject constructor(
    private val cardsRepository: CardsRepository,
    private val apiService: ApiService,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val cardId: Int? = savedStateHandle.get<Int>("cardId")
    
    private val _uiState = MutableStateFlow(ProfessionalAppointmentFormState(
        cardId = cardId,
        isEditMode = cardId != null,
        isLoading = cardId != null
    ))
    val uiState: StateFlow<ProfessionalAppointmentFormState> = _uiState.asStateFlow()

    init {
        loadFamilyMembers()
        if (cardId != null) {
            loadCard(cardId)
        }
    }

    private fun loadCard(id: Int) {
        viewModelScope.launch {
            val result = cardsRepository.getCard(id)
            
            when (result) {
                is NetworkResult.Success -> {
                    val card = result.data
                    val data = card.data
                    
                    // Find driver name from family members if we have a driver_id
                    val driverName = if (data?.driverId != null) {
                        _uiState.value.familyMembers.find { it.id == data.driverId }?.name ?: ""
                    } else ""
                    
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            icon = data?.icon ?: "stethoscope",
                            professionalName = data?.professionalName ?: "",
                            professionalType = data?.professionalType ?: "",
                            eventDate = data?.eventDate ?: "",
                            eventTime = data?.eventTime?.take(5) ?: "", // HH:mm format
                            purpose = data?.purpose ?: "",
                            location = data?.location ?: "",
                            driverId = data?.driverId,
                            driverName = data?.driverName ?: driverName,
                            transportationNotes = data?.transportationNotes ?: "",
                            preparationNotes = data?.preparationNotes ?: "",
                            narrative = data?.narrative ?: "",
                            allowOthersEdit = card.allowOthersEdit,
                            existingImagePath = data?.imagePath
                        )
                    }
                }
                is NetworkResult.Error -> {
                    _uiState.update { it.copy(
                        isLoading = false,
                        error = result.message
                    )}
                }
                is NetworkResult.Loading -> {}
            }
        }
    }

    private fun loadFamilyMembers() {
        viewModelScope.launch {
            // Use family status endpoint to get family members
            val result = safeApiCall { apiService.getFamilyStatus() }
            
            when (result) {
                is NetworkResult.Success -> {
                    val drivers = listOf(DriverOption(null, "No driver needed")) +
                        result.data.statuses.map { status ->
                            DriverOption(
                                id = status.userId,
                                name = status.name,
                                profilePhoto = status.profilePhoto
                            )
                        }
                    _uiState.update { state ->
                        // If we're in edit mode and have a driverId, update the driver name
                        val updatedDriverName = if (state.driverId != null && state.driverName.isBlank()) {
                            drivers.find { it.id == state.driverId }?.name ?: ""
                        } else {
                            state.driverName
                        }
                        
                        state.copy(
                            familyMembers = drivers,
                            isLoadingFamily = false,
                            driverName = updatedDriverName
                        )
                    }
                }
                is NetworkResult.Error -> {
                    _uiState.update { it.copy(
                        familyMembers = listOf(DriverOption(null, "No driver needed")),
                        isLoadingFamily = false
                    )}
                }
                is NetworkResult.Loading -> {}
            }
        }
    }

    fun updateIcon(icon: String) {
        _uiState.update { it.copy(icon = icon) }
    }

    fun updateProfessionalName(name: String) {
        _uiState.update { it.copy(professionalName = name) }
    }

    fun updateProfessionalType(type: String) {
        _uiState.update { it.copy(professionalType = type) }
    }

    fun updateEventDate(date: String) {
        _uiState.update { it.copy(eventDate = date) }
    }

    fun updateEventTime(time: String) {
        _uiState.update { it.copy(eventTime = time) }
    }

    fun updatePurpose(purpose: String) {
        _uiState.update { it.copy(purpose = purpose) }
    }

    fun updateLocation(location: String) {
        _uiState.update { it.copy(location = location) }
    }

    fun updateDriver(driverId: Int?, driverName: String) {
        _uiState.update { it.copy(driverId = driverId, driverName = driverName) }
    }

    fun updateTransportationNotes(notes: String) {
        _uiState.update { it.copy(transportationNotes = notes) }
    }

    fun updatePreparationNotes(notes: String) {
        _uiState.update { it.copy(preparationNotes = notes) }
    }

    fun updateNarrative(narrative: String) {
        _uiState.update { it.copy(narrative = narrative) }
    }

    fun updateAllowOthersEdit(allow: Boolean) {
        _uiState.update { it.copy(allowOthersEdit = allow) }
    }

    fun updateSelectedImage(uri: Uri?) {
        _uiState.update { it.copy(selectedImageUri = uri) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun save() {
        val state = _uiState.value
        if (!state.isValid) {
            _uiState.update { it.copy(error = "Please fill in all required fields") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }

            val cardData = CardDataRequest(
                icon = state.icon,
                professionalName = state.professionalName,
                professionalType = state.professionalType.ifBlank { null },
                eventDate = state.eventDate,
                eventTime = state.eventTime,
                purpose = state.purpose,
                location = state.location.ifBlank { null },
                driverId = state.driverId,
                transportationNotes = state.transportationNotes.ifBlank { null },
                preparationNotes = state.preparationNotes.ifBlank { null },
                narrative = state.narrative.ifBlank { null }
            )

            val result = if (state.isEditMode && state.cardId != null) {
                // Update existing card
                cardsRepository.updateCard(
                    id = state.cardId,
                    data = cardData,
                    allowOthersEdit = state.allowOthersEdit
                )
            } else {
                // Create new card
                cardsRepository.createCard(
                    cardType = CardType.PROFESSIONAL_APPOINTMENT,
                    data = cardData,
                    allowOthersEdit = state.allowOthersEdit
                )
            }

            when (result) {
                is NetworkResult.Success -> {
                    val cardId = result.data.id
                    
                    // Upload image if one was selected
                    if (state.selectedImageUri != null) {
                        _uiState.update { it.copy(isSaving = false, isUploadingImage = true) }
                        
                        val uploadResult = cardsRepository.uploadCardImage(cardId, state.selectedImageUri)
                        
                        when (uploadResult) {
                            is NetworkResult.Success -> {
                                _uiState.update { it.copy(isUploadingImage = false, isSaved = true) }
                            }
                            is NetworkResult.Error -> {
                                // Card saved but image upload failed
                                _uiState.update { it.copy(
                                    isUploadingImage = false,
                                    isSaved = true,
                                    error = "Card saved but image upload failed: ${uploadResult.message}"
                                ) }
                            }
                            is NetworkResult.Loading -> {}
                        }
                    } else {
                        _uiState.update { it.copy(isSaving = false, isSaved = true) }
                    }
                }
                is NetworkResult.Error -> {
                    _uiState.update { it.copy(isSaving = false, error = result.message) }
                }
                is NetworkResult.Loading -> {}
            }
        }
    }
}

package com.otheruncle.memorydisplay.ui.create

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.otheruncle.memorydisplay.data.api.NetworkResult
import com.otheruncle.memorydisplay.data.model.CardDataRequest
import com.otheruncle.memorydisplay.data.model.CardType
import com.otheruncle.memorydisplay.data.repository.CardsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Icon options for other events
 */
val otherEventIcons = listOf(
    IconOption("church", "Church/Mass"),
    IconOption("music", "Concert/Music"),
    IconOption("film", "Movie"),
    IconOption("book", "Book Club"),
    IconOption("users", "Social Group"),
    IconOption("shopping-cart", "Shopping"),
    IconOption("scissors", "Hair Appointment"),
    IconOption("calendar", "General Event")
)

/**
 * Form state for other event
 */
data class OtherEventFormState(
    val cardId: Int? = null,
    val isEditMode: Boolean = false,
    
    val icon: String = "calendar",
    val summary: String = "",
    val eventDate: String = "",
    val eventTime: String = "",
    val location: String = "",
    val attendeesText: String = "",
    val transportation: String = "",
    val narrative: String = "",
    val allowOthersEdit: Boolean = false,
    
    // Image fields
    val selectedImageUri: Uri? = null,
    val existingImagePath: String? = null,
    val isUploadingImage: Boolean = false,
    
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val isSaved: Boolean = false
) {
    val isValid: Boolean get() = summary.isNotBlank() && eventDate.isNotBlank()
}

@HiltViewModel
class OtherEventViewModel @Inject constructor(
    private val cardsRepository: CardsRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val cardId: Int? = savedStateHandle.get<Int>("cardId")
    
    private val _uiState = MutableStateFlow(OtherEventFormState(
        cardId = cardId,
        isEditMode = cardId != null,
        isLoading = cardId != null
    ))
    val uiState: StateFlow<OtherEventFormState> = _uiState.asStateFlow()

    init {
        if (cardId != null) loadCard(cardId)
    }

    private fun loadCard(id: Int) {
        viewModelScope.launch {
            when (val result = cardsRepository.getCard(id)) {
                is NetworkResult.Success -> {
                    val card = result.data
                    val data = card.data
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            icon = data?.icon ?: "calendar",
                            summary = data?.summary ?: "",
                            eventDate = data?.eventDate ?: "",
                            eventTime = data?.eventTime?.take(5) ?: "",
                            location = data?.location ?: "",
                            attendeesText = data?.attendees ?: "",
                            transportation = data?.transportation ?: "",
                            narrative = data?.narrative ?: "",
                            allowOthersEdit = card.allowOthersEdit,
                            existingImagePath = data?.imagePath
                        )
                    }
                }
                is NetworkResult.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                is NetworkResult.Loading -> {}
            }
        }
    }

    fun updateIcon(icon: String) = _uiState.update { it.copy(icon = icon) }
    fun updateSummary(summary: String) = _uiState.update { it.copy(summary = summary) }
    fun updateEventDate(date: String) = _uiState.update { it.copy(eventDate = date) }
    fun updateEventTime(time: String) = _uiState.update { it.copy(eventTime = time) }
    fun updateLocation(location: String) = _uiState.update { it.copy(location = location) }
    fun updateAttendeesText(text: String) = _uiState.update { it.copy(attendeesText = text) }
    fun updateTransportation(text: String) = _uiState.update { it.copy(transportation = text) }
    fun updateNarrative(narrative: String) = _uiState.update { it.copy(narrative = narrative) }
    fun updateAllowOthersEdit(allow: Boolean) = _uiState.update { it.copy(allowOthersEdit = allow) }
    fun updateSelectedImage(uri: Uri?) = _uiState.update { it.copy(selectedImageUri = uri) }
    fun clearError() = _uiState.update { it.copy(error = null) }

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
                summary = state.summary,
                eventDate = state.eventDate,
                eventTime = state.eventTime.ifBlank { null },
                location = state.location.ifBlank { null },
                attendees = state.attendeesText.ifBlank { null },
                transportation = state.transportation.ifBlank { null },
                narrative = state.narrative.ifBlank { null }
            )

            val result = if (state.isEditMode && state.cardId != null) {
                cardsRepository.updateCard(state.cardId, cardData, state.allowOthersEdit)
            } else {
                cardsRepository.createCard(CardType.OTHER_EVENT, cardData, state.allowOthersEdit)
            }

            when (result) {
                is NetworkResult.Success -> {
                    val cardId = result.data.id
                    
                    if (state.selectedImageUri != null) {
                        _uiState.update { it.copy(isSaving = false, isUploadingImage = true) }
                        
                        when (val uploadResult = cardsRepository.uploadCardImage(cardId, state.selectedImageUri)) {
                            is NetworkResult.Success -> {
                                _uiState.update { it.copy(isUploadingImage = false, isSaved = true) }
                            }
                            is NetworkResult.Error -> {
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
                is NetworkResult.Error -> _uiState.update { it.copy(isSaving = false, error = result.message) }
                is NetworkResult.Loading -> {}
            }
        }
    }
}

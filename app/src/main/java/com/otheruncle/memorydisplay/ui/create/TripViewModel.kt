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
 * Form state for family trip
 */
data class TripFormState(
    val cardId: Int? = null,
    val isEditMode: Boolean = false,
    
    val destination: String = "",
    val departureDate: String = "",
    val departureAction: String = "",
    val returnDate: String = "",
    val returnAction: String = "",
    val whatDoing: String = "",
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
    val isValid: Boolean get() = destination.isNotBlank() && departureDate.isNotBlank()
}

@HiltViewModel
class TripViewModel @Inject constructor(
    private val cardsRepository: CardsRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val cardId: Int? = savedStateHandle.get<Int>("cardId")
    
    private val _uiState = MutableStateFlow(TripFormState(
        cardId = cardId,
        isEditMode = cardId != null,
        isLoading = cardId != null
    ))
    val uiState: StateFlow<TripFormState> = _uiState.asStateFlow()

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
                            destination = data?.destination ?: "",
                            departureDate = data?.departureDate ?: "",
                            departureAction = data?.departureAction ?: "",
                            returnDate = data?.returnDate ?: "",
                            returnAction = data?.returnAction ?: "",
                            whatDoing = data?.whatDoing ?: "",
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

    fun updateDestination(destination: String) = _uiState.update { it.copy(destination = destination) }
    fun updateDepartureDate(date: String) = _uiState.update { it.copy(departureDate = date) }
    fun updateDepartureAction(action: String) = _uiState.update { it.copy(departureAction = action) }
    fun updateReturnDate(date: String) = _uiState.update { it.copy(returnDate = date) }
    fun updateReturnAction(action: String) = _uiState.update { it.copy(returnAction = action) }
    fun updateWhatDoing(text: String) = _uiState.update { it.copy(whatDoing = text) }
    fun updateNarrative(narrative: String) = _uiState.update { it.copy(narrative = narrative) }
    fun updateAllowOthersEdit(allow: Boolean) = _uiState.update { it.copy(allowOthersEdit = allow) }
    fun updateSelectedImage(uri: Uri?) = _uiState.update { it.copy(selectedImageUri = uri) }
    fun clearError() = _uiState.update { it.copy(error = null) }

    fun save() {
        val state = _uiState.value
        if (!state.isValid) {
            _uiState.update { it.copy(error = "Please fill in destination and departure date") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }

            val cardData = CardDataRequest(
                destination = state.destination,
                departureDate = state.departureDate,
                departureAction = state.departureAction.ifBlank { null },
                returnDate = state.returnDate.ifBlank { null },
                returnAction = state.returnAction.ifBlank { null },
                whatDoing = state.whatDoing.ifBlank { null },
                narrative = state.narrative.ifBlank { null }
            )

            val result = if (state.isEditMode && state.cardId != null) {
                cardsRepository.updateCard(state.cardId, cardData, state.allowOthersEdit)
            } else {
                cardsRepository.createCard(CardType.FAMILY_TRIP, cardData, state.allowOthersEdit)
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

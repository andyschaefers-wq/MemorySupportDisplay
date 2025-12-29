package com.otheruncle.memorydisplay.ui.messages

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

data class MessagesUiState(
    val messagePreview: String = "",
    val fullMessage: String = "",
    val expirationDays: Int = 3,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val isEditMode: Boolean = false,
    val editCardId: Int? = null,
    val error: String? = null,
    val successMessage: String? = null,
    
    // Image fields
    val selectedImageUri: Uri? = null,
    val existingImagePath: String? = null,
    val isUploadingImage: Boolean = false
) {
    val isValid: Boolean
        get() = messagePreview.isNotBlank()  // fullMessage is now optional
}

@HiltViewModel
class MessagesViewModel @Inject constructor(
    private val cardsRepository: CardsRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(MessagesUiState())
    val uiState: StateFlow<MessagesUiState> = _uiState.asStateFlow()
    
    // Check if we're in edit mode (cardId passed in navigation)
    private val cardId: Int? = savedStateHandle.get<Int>("cardId")
    
    init {
        cardId?.let { id ->
            loadExistingMessage(id)
        }
    }
    
    private fun loadExistingMessage(id: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, isEditMode = true, editCardId = id) }
            
            when (val result = cardsRepository.getCard(id)) {
                is NetworkResult.Success -> {
                    val card = result.data
                    val data = card.data
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            messagePreview = data?.messagePreview ?: "",
                            fullMessage = data?.fullMessage ?: "",
                            expirationDays = data?.expirationDays ?: 3,
                            existingImagePath = data?.imagePath
                        )
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
    }
    
    fun updateMessagePreview(value: String) {
        // Limit to 80 characters
        if (value.length <= 80) {
            _uiState.update { it.copy(messagePreview = value) }
        }
    }
    
    fun updateFullMessage(value: String) {
        _uiState.update { it.copy(fullMessage = value) }
    }
    
    fun updateExpirationDays(days: Int) {
        _uiState.update { it.copy(expirationDays = days) }
    }
    
    fun updateSelectedImage(uri: Uri?) {
        _uiState.update { it.copy(selectedImageUri = uri) }
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    fun sendMessage() {
        val state = _uiState.value
        if (!state.isValid) {
            _uiState.update { it.copy(error = "Please fill in all required fields") }
            return
        }
        
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            
            val data = CardDataRequest(
                messagePreview = state.messagePreview,
                fullMessage = state.fullMessage,
                expirationDays = state.expirationDays
            )
            
            val result = if (state.isEditMode && state.editCardId != null) {
                cardsRepository.updateCard(state.editCardId, data)
            } else {
                cardsRepository.createCard(
                    cardType = CardType.FAMILY_MESSAGE,
                    data = data,
                    allowOthersEdit = false
                )
            }
            
            when (result) {
                is NetworkResult.Success -> {
                    val cardId = result.data.id
                    
                    // Upload image if one was selected
                    if (state.selectedImageUri != null) {
                        _uiState.update { it.copy(isSaving = false, isUploadingImage = true) }
                        
                        when (val uploadResult = cardsRepository.uploadCardImage(cardId, state.selectedImageUri)) {
                            is NetworkResult.Success -> {
                                _uiState.update { 
                                    it.copy(
                                        isUploadingImage = false, 
                                        isSaved = true,
                                        successMessage = "Message sent!"
                                    ) 
                                }
                            }
                            is NetworkResult.Error -> {
                                _uiState.update { 
                                    it.copy(
                                        isUploadingImage = false, 
                                        isSaved = true,
                                        error = "Message sent but image upload failed: ${uploadResult.message}"
                                    ) 
                                }
                            }
                            is NetworkResult.Loading -> {}
                        }
                    } else {
                        _uiState.update { 
                            it.copy(
                                isSaving = false, 
                                isSaved = true,
                                successMessage = "Message sent!"
                            ) 
                        }
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
}

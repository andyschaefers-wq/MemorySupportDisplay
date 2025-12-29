package com.otheruncle.memorydisplay.ui.create

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.otheruncle.memorydisplay.data.api.NetworkResult
import com.otheruncle.memorydisplay.data.model.CardDataRequest
import com.otheruncle.memorydisplay.data.model.CardType
import com.otheruncle.memorydisplay.data.model.RecurrenceType
import com.otheruncle.memorydisplay.data.repository.CardsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Icon options for reminders
 */
val reminderIcons = listOf(
    IconOption("bell", "General Reminder"),
    IconOption("pill", "Medication"),
    IconOption("trash-2", "Trash Day"),
    IconOption("droplet", "Water Plants"),
    IconOption("mail", "Check Mail"),
    IconOption("phone", "Make Phone Call"),
    IconOption("calendar-check", "Appointment Reminder"),
    IconOption("alert-circle", "Important")
)

/**
 * Recurrence type options
 */
data class RecurrenceOption(
    val type: String,
    val displayName: String
)

val recurrenceOptions = listOf(
    RecurrenceOption("one-time", "One-time"),
    RecurrenceOption("weekly", "Weekly"),
    RecurrenceOption("monthly", "Monthly")
)

/**
 * Day of week options for weekly recurrence
 */
val daysOfWeek = listOf(
    "Sunday" to 0,
    "Monday" to 1,
    "Tuesday" to 2,
    "Wednesday" to 3,
    "Thursday" to 4,
    "Friday" to 5,
    "Saturday" to 6
)

/**
 * Form state for reminder
 */
data class ReminderFormState(
    val cardId: Int? = null,
    val isEditMode: Boolean = false,
    
    val icon: String = "bell",
    val text: String = "",
    val recurrenceType: String = "one-time",
    
    // For weekly: day of week (0-6)
    val appearDay: Int? = null,
    val disappearDay: Int? = null,
    
    // For monthly: day of month (1-31)
    val appearDate: Int? = null,
    val disappearDate: Int? = null,
    
    // For one-time: specific date
    val appearSpecific: String = "",
    val disappearSpecific: String = "",
    
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
    val isValid: Boolean get() = text.isNotBlank() && when (recurrenceType) {
        "one-time" -> appearSpecific.isNotBlank()
        "weekly" -> appearDay != null
        "monthly" -> appearDate != null
        else -> false
    }
}

@HiltViewModel
class ReminderViewModel @Inject constructor(
    private val cardsRepository: CardsRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val cardId: Int? = savedStateHandle.get<Int>("cardId")
    
    private val _uiState = MutableStateFlow(ReminderFormState(
        cardId = cardId,
        isEditMode = cardId != null,
        isLoading = cardId != null
    ))
    val uiState: StateFlow<ReminderFormState> = _uiState.asStateFlow()

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
                            icon = data?.icon ?: "bell",
                            text = data?.text ?: "",
                            recurrenceType = data?.recurrenceType?.name?.lowercase()?.replace("_", "-") ?: "one-time",
                            appearDay = data?.appearDay,
                            disappearDay = data?.disappearDay,
                            appearDate = data?.appearDate,
                            disappearDate = data?.disappearDate,
                            appearSpecific = data?.appearSpecific ?: "",
                            disappearSpecific = data?.disappearSpecific ?: "",
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
    fun updateText(text: String) = _uiState.update { it.copy(text = text) }
    fun updateRecurrenceType(type: String) = _uiState.update { 
        it.copy(
            recurrenceType = type,
            // Clear other recurrence fields when type changes
            appearDay = null,
            disappearDay = null,
            appearDate = null,
            disappearDate = null,
            appearSpecific = "",
            disappearSpecific = ""
        )
    }
    fun updateAppearDay(day: Int?) = _uiState.update { it.copy(appearDay = day) }
    fun updateDisappearDay(day: Int?) = _uiState.update { it.copy(disappearDay = day) }
    fun updateAppearDate(date: Int?) = _uiState.update { it.copy(appearDate = date) }
    fun updateDisappearDate(date: Int?) = _uiState.update { it.copy(disappearDate = date) }
    fun updateAppearSpecific(date: String) = _uiState.update { it.copy(appearSpecific = date) }
    fun updateDisappearSpecific(date: String) = _uiState.update { it.copy(disappearSpecific = date) }
    fun updateNarrative(narrative: String) = _uiState.update { it.copy(narrative = narrative) }
    fun updateAllowOthersEdit(allow: Boolean) = _uiState.update { it.copy(allowOthersEdit = allow) }
    fun updateSelectedImage(uri: Uri?) = _uiState.update { it.copy(selectedImageUri = uri) }
    fun clearError() = _uiState.update { it.copy(error = null) }

    fun save() {
        val state = _uiState.value
        if (!state.isValid) {
            _uiState.update { it.copy(error = "Please fill in reminder text and when it appears") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }

            val cardData = CardDataRequest(
                icon = state.icon,
                text = state.text,
                recurrenceType = state.recurrenceType,
                appearDay = state.appearDay,
                disappearDay = state.disappearDay,
                appearDate = state.appearDate,
                disappearDate = state.disappearDate,
                appearSpecific = state.appearSpecific.ifBlank { null },
                disappearSpecific = state.disappearSpecific.ifBlank { null },
                narrative = state.narrative.ifBlank { null }
            )

            val result = if (state.isEditMode && state.cardId != null) {
                cardsRepository.updateCard(state.cardId, cardData, state.allowOthersEdit)
            } else {
                cardsRepository.createCard(CardType.REMINDER, cardData, state.allowOthersEdit)
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

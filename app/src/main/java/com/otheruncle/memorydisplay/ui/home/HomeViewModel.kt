package com.otheruncle.memorydisplay.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.otheruncle.memorydisplay.data.api.NetworkResult
import com.otheruncle.memorydisplay.data.model.Card
import com.otheruncle.memorydisplay.data.model.User
import com.otheruncle.memorydisplay.data.repository.CardsRepository
import com.otheruncle.memorydisplay.data.repository.ProfileRepository
import com.otheruncle.memorydisplay.data.repository.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val user: User? = null,
    val myCards: List<Card> = emptyList(),
    val editableCards: List<Card> = emptyList(),
    val otherDisplayedCards: List<Card> = emptyList(),
    // Status update
    val statusLocation: String = "",
    val statusNote: String = "",
    val isUpdatingStatus: Boolean = false,
    val statusUpdateSuccess: Boolean = false,
    val statusUpdateError: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val cardsRepository: CardsRepository,
    private val profileRepository: ProfileRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            // Load user
            val user = userPreferences.getUser()
            _uiState.update { it.copy(user = user) }

            val userId = user?.id ?: -1

            // Load display cards (only cards currently shown on the panel - excludes expired)
            when (val displayResult = cardsRepository.getDisplayCards()) {
                is NetworkResult.Success -> {
                    val displayCards = displayResult.data

                    // Split display cards into three categories:
                    // 1. My cards - cards I created
                    val myCards = displayCards.filter { it.creatorId == userId }
                    
                    // 2. Editable cards - cards created by others that I can edit
                    val editableCards = displayCards.filter { 
                        it.creatorId != userId && it.allowOthersEdit 
                    }
                    
                    // 3. Other displayed cards - cards created by others that I cannot edit
                    val otherDisplayedCards = displayCards.filter { 
                        it.creatorId != userId && !it.allowOthersEdit 
                    }

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            myCards = myCards,
                            editableCards = editableCards,
                            otherDisplayedCards = otherDisplayedCards
                        )
                    }
                }
                is NetworkResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            error = displayResult.message
                        )
                    }
                }
                is NetworkResult.Loading -> {
                    // Already handled
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            loadData()
        }
    }

    fun updateStatusLocation(location: String) {
        _uiState.update { it.copy(statusLocation = location, statusUpdateError = null) }
    }

    fun updateStatusNote(note: String) {
        _uiState.update { it.copy(statusNote = note, statusUpdateError = null) }
    }

    fun submitStatus() {
        val location = _uiState.value.statusLocation.trim()
        if (location.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isUpdatingStatus = true, statusUpdateError = null) }

            val note = _uiState.value.statusNote.trim().ifBlank { null }

            when (val result = profileRepository.updateStatus(location, note)) {
                is NetworkResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isUpdatingStatus = false,
                            statusUpdateSuccess = true,
                            statusLocation = "",
                            statusNote = ""
                        )
                    }
                    // Reset success flag after a delay
                    kotlinx.coroutines.delay(2000)
                    _uiState.update { it.copy(statusUpdateSuccess = false) }
                }
                is NetworkResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isUpdatingStatus = false,
                            statusUpdateError = result.message
                        )
                    }
                }
                is NetworkResult.Loading -> {
                    // Already handled
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun deleteCard(cardId: Int) {
        viewModelScope.launch {
            when (val result = cardsRepository.deleteCard(cardId)) {
                is NetworkResult.Success -> {
                    // Remove from local state
                    _uiState.update { state ->
                        state.copy(
                            myCards = state.myCards.filter { it.id != cardId },
                            editableCards = state.editableCards.filter { it.id != cardId },
                            otherDisplayedCards = state.otherDisplayedCards.filter { it.id != cardId }
                        )
                    }
                }
                is NetworkResult.Error -> {
                    _uiState.update { it.copy(error = result.message) }
                }
                is NetworkResult.Loading -> {}
            }
        }
    }
}

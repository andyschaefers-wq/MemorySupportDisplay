package com.otheruncle.memorydisplay.ui.create

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.otheruncle.memorydisplay.data.api.NetworkResult
import com.otheruncle.memorydisplay.data.model.Card
import com.otheruncle.memorydisplay.data.repository.CardsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CardDetailUiState(
    val isLoading: Boolean = true,
    val card: Card? = null,
    val error: String? = null,
    val isDeleting: Boolean = false,
    val isDeleted: Boolean = false
)

@HiltViewModel
class CardDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val cardsRepository: CardsRepository
) : ViewModel() {

    private val cardId: Int = savedStateHandle.get<Int>("cardId") ?: -1

    private val _uiState = MutableStateFlow(CardDetailUiState())
    val uiState: StateFlow<CardDetailUiState> = _uiState.asStateFlow()

    init {
        loadCard()
    }

    private fun loadCard() {
        if (cardId == -1) {
            _uiState.update { it.copy(isLoading = false, error = "Invalid card ID") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            when (val result = cardsRepository.getCard(cardId)) {
                is NetworkResult.Success -> {
                    _uiState.update { it.copy(isLoading = false, card = result.data) }
                }
                is NetworkResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
                is NetworkResult.Loading -> {}
            }
        }
    }

    fun deleteCard() {
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true) }

            when (val result = cardsRepository.deleteCard(cardId)) {
                is NetworkResult.Success -> {
                    _uiState.update { it.copy(isDeleting = false, isDeleted = true) }
                }
                is NetworkResult.Error -> {
                    _uiState.update { it.copy(isDeleting = false, error = result.message) }
                }
                is NetworkResult.Loading -> {}
            }
        }
    }

    fun refresh() {
        loadCard()
    }
}

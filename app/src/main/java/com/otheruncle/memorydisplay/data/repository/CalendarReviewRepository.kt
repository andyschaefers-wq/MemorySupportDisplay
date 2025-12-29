package com.otheruncle.memorydisplay.data.repository

import com.otheruncle.memorydisplay.data.api.ApiService
import com.otheruncle.memorydisplay.data.api.NetworkResult
import com.otheruncle.memorydisplay.data.api.safeApiCall
import com.otheruncle.memorydisplay.data.model.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CalendarReviewRepository @Inject constructor(
    private val apiService: ApiService
) {

    /**
     * Get pending calendar events for review
     */
    suspend fun getPendingEvents(): NetworkResult<List<PendingEvent>> {
        val result = safeApiCall { apiService.getPendingEvents() }

        return when (result) {
            is NetworkResult.Success -> NetworkResult.Success(result.data.pending)
            is NetworkResult.Error -> result
            is NetworkResult.Loading -> NetworkResult.Loading
        }
    }

    /**
     * Get count of pending events
     */
    suspend fun getPendingCount(): NetworkResult<Int> {
        val result = safeApiCall { apiService.getPendingEvents() }

        return when (result) {
            is NetworkResult.Success -> NetworkResult.Success(result.data.count)
            is NetworkResult.Error -> result
            is NetworkResult.Loading -> NetworkResult.Loading
        }
    }

    /**
     * Review a pending event and convert to proper card type
     */
    suspend fun reviewEvent(
        pendingId: Int,
        cardType: CardType,
        data: CardData
    ): NetworkResult<Card> {
        val typeString = when (cardType) {
            CardType.PROFESSIONAL_APPOINTMENT -> "professional-appointment"
            CardType.FAMILY_EVENT -> "family-event"
            CardType.OTHER_EVENT -> "other-event"
            else -> return NetworkResult.Error("Invalid card type for review")
        }

        val result = safeApiCall {
            apiService.reviewEvent(
                pendingId,
                ReviewRequest(
                    cardType = typeString,
                    data = data
                )
            )
        }

        return when (result) {
            is NetworkResult.Success -> {
                val response = result.data
                if (response.success && response.card != null) {
                    NetworkResult.Success(response.card)
                } else {
                    NetworkResult.Error(response.error ?: "Failed to review event")
                }
            }
            is NetworkResult.Error -> result
            is NetworkResult.Loading -> NetworkResult.Loading
        }
    }
}

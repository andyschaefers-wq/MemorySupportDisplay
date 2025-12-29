package com.otheruncle.memorydisplay.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Family member status for Family & Events area
 */
@Serializable
data class FamilyStatus(
    @SerialName("user_id")
    val userId: Int,
    val name: String,
    @SerialName("profile_photo")
    val profilePhoto: String? = null,
    val location: String? = null,
    val note: String? = null,
    val weather: Weather? = null,
    @SerialName("is_trip")
    val isTrip: Boolean = false
)

@Serializable
data class Weather(
    val temp: Int,
    val condition: String,
    val icon: String
)

/**
 * Family status list response
 */
@Serializable
data class FamilyStatusResponse(
    val statuses: List<FamilyStatus>
)

/**
 * Family status update request
 */
@Serializable
data class FamilyStatusUpdateRequest(
    val location: String,
    val note: String? = null
)

/**
 * Family status update response
 */
@Serializable
data class FamilyStatusUpdateResponse(
    val success: Boolean,
    @SerialName("expires_at")
    val expiresAt: String? = null,
    val error: String? = null
)

/**
 * Pending calendar event for review
 */
@Serializable
data class PendingEvent(
    val id: Int,
    @SerialName("card_id")
    val cardId: Int,
    val title: String,
    @SerialName("event_date")
    val eventDate: String,
    @SerialName("event_time")
    val eventTime: String? = null,
    val location: String? = null,
    @SerialName("source_user_id")
    val sourceUserId: Int,
    @SerialName("source_user_name")
    val sourceUserName: String,
    @SerialName("created_at")
    val createdAt: String
)

/**
 * Pending events list response
 */
@Serializable
data class PendingEventsResponse(
    val pending: List<PendingEvent>,
    val count: Int
)

/**
 * Review request - convert pending to proper card type
 */
@Serializable
data class ReviewRequest(
    @SerialName("card_type")
    val cardType: String,
    val data: CardData
)

package com.otheruncle.memorydisplay.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Base card create request
 */
@Serializable
data class CardCreateRequest(
    @SerialName("card_type")
    val cardType: String,
    @SerialName("allow_others_edit")
    val allowOthersEdit: Boolean = false,
    val data: CardDataRequest
)

/**
 * Card update request
 */
@Serializable
data class CardUpdateRequest(
    @SerialName("allow_others_edit")
    val allowOthersEdit: Boolean? = null,
    val data: CardDataRequest
)

/**
 * Card data for create/update requests
 * All fields optional - only include what's relevant for the card type
 */
@Serializable
data class CardDataRequest(
    // Common fields
    val icon: String? = null,
    @SerialName("event_date")
    val eventDate: String? = null,
    @SerialName("event_time")
    val eventTime: String? = null,
    val location: String? = null,
    val narrative: String? = null,
    
    // Professional Appointment
    @SerialName("professional_name")
    val professionalName: String? = null,
    @SerialName("professional_type")
    val professionalType: String? = null,
    val purpose: String? = null,
    @SerialName("driver_id")
    val driverId: Int? = null,
    @SerialName("transportation_notes")
    val transportationNotes: String? = null,
    @SerialName("preparation_notes")
    val preparationNotes: String? = null,
    
    // Family Event / Other Event
    val summary: String? = null,
    val attendees: String? = null, // Simple comma-separated text (e.g., "Mom, Dad, Sarah")
    @SerialName("what_to_bring")
    val whatToBring: String? = null,
    val transportation: String? = null,
    
    // Family Trip
    val destination: String? = null,
    @SerialName("departure_date")
    val departureDate: String? = null,
    @SerialName("departure_action")
    val departureAction: String? = null,
    @SerialName("return_date")
    val returnDate: String? = null,
    @SerialName("return_action")
    val returnAction: String? = null,
    @SerialName("what_doing")
    val whatDoing: String? = null,
    
    // Reminder
    val text: String? = null,
    @SerialName("recurrence_type")
    val recurrenceType: String? = null,
    @SerialName("appear_day")
    val appearDay: Int? = null,
    @SerialName("appear_date")
    val appearDate: Int? = null,
    @SerialName("appear_specific")
    val appearSpecific: String? = null,
    @SerialName("disappear_day")
    val disappearDay: Int? = null,
    @SerialName("disappear_date")
    val disappearDate: Int? = null,
    @SerialName("disappear_specific")
    val disappearSpecific: String? = null,
    
    // Family Message
    @SerialName("message_preview")
    val messagePreview: String? = null,
    @SerialName("full_message")
    val fullMessage: String? = null,
    @SerialName("expiration_days")
    val expirationDays: Int? = null
)

/**
 * Family member for driver/attendee selection
 */
@Serializable
data class FamilyMember(
    val id: Int,
    val name: String,
    @SerialName("profile_photo")
    val profilePhoto: String? = null
)

/**
 * Family members list response
 */
@Serializable
data class FamilyMembersResponse(
    val members: List<FamilyMember>
)

package com.otheruncle.memorydisplay.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Card model matching the API response from /cards
 */
@Serializable
data class Card(
    @Serializable(with = StringIntSerializer::class)
    val id: Int,
    @SerialName("card_type")
    val cardType: CardType,
    @SerialName("creator_id")
    @Serializable(with = StringIntSerializer::class)
    val creatorId: Int,
    @SerialName("creator_name")
    val creatorName: String? = null,
    @SerialName("allow_others_edit")
    @Serializable(with = StringBooleanSerializer::class)
    val allowOthersEdit: Boolean = false,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("temporal_badge")
    val temporalBadge: TemporalBadge? = null,
    val data: CardData? = null
)

@Serializable
enum class CardType {
    @SerialName("professional-appointment")
    PROFESSIONAL_APPOINTMENT,
    @SerialName("family-event")
    FAMILY_EVENT,
    @SerialName("other-event")
    OTHER_EVENT,
    @SerialName("family-trip")
    FAMILY_TRIP,
    @SerialName("reminder")
    REMINDER,
    @SerialName("family-message")
    FAMILY_MESSAGE,
    @SerialName("pending")
    PENDING
}

@Serializable
enum class TemporalBadge {
    @SerialName("today")
    TODAY,
    @SerialName("tomorrow")
    TOMORROW,
    @SerialName("upcoming")
    UPCOMING
}

/**
 * Polymorphic card data - use specific types for type-safe access
 */
@Serializable
data class CardData(
    // Common fields
    val icon: String? = null,
    @SerialName("event_date")
    val eventDate: String? = null,
    @SerialName("event_time")
    val eventTime: String? = null,
    val location: String? = null,
    val narrative: String? = null,
    @SerialName("image_path")
    val imagePath: String? = null,
    
    // Professional Appointment fields
    @SerialName("professional_name")
    val professionalName: String? = null,
    @SerialName("professional_type")
    val professionalType: String? = null,
    val purpose: String? = null,
    @SerialName("driver_id")
    @Serializable(with = NullableStringIntSerializer::class)
    val driverId: Int? = null,
    @SerialName("driver_name")
    val driverName: String? = null,
    @SerialName("driver_photo")
    val driverPhoto: String? = null,
    @SerialName("transportation_notes")
    val transportationNotes: String? = null,
    @SerialName("preparation_notes")
    val preparationNotes: String? = null,
    
    // Family Event / Other Event fields
    val summary: String? = null,
    @Serializable(with = AttendeesSerializer::class)
    val attendees: String? = null, // Normalized to comma-separated string
    @SerialName("what_to_bring")
    val whatToBring: String? = null,
    val transportation: String? = null,
    
    // Family Trip fields
    @SerialName("traveler_id")
    @Serializable(with = NullableStringIntSerializer::class)
    val travelerId: Int? = null,
    @SerialName("traveler_name")
    val travelerName: String? = null,
    @SerialName("traveler_photo")
    val travelerPhoto: String? = null,
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
    
    // Reminder fields
    val text: String? = null,
    @SerialName("recurrence_type")
    val recurrenceType: RecurrenceType? = null,
    @SerialName("appear_day")
    @Serializable(with = NullableStringIntSerializer::class)
    val appearDay: Int? = null,
    @SerialName("appear_date")
    @Serializable(with = NullableStringIntSerializer::class)
    val appearDate: Int? = null,
    @SerialName("appear_specific")
    val appearSpecific: String? = null,
    @SerialName("disappear_day")
    @Serializable(with = NullableStringIntSerializer::class)
    val disappearDay: Int? = null,
    @SerialName("disappear_date")
    @Serializable(with = NullableStringIntSerializer::class)
    val disappearDate: Int? = null,
    @SerialName("disappear_specific")
    val disappearSpecific: String? = null,
    
    // Family Message fields
    @SerialName("message_preview")
    val messagePreview: String? = null,
    @SerialName("full_message")
    val fullMessage: String? = null,
    @SerialName("expiration_days")
    @Serializable(with = NullableStringIntSerializer::class)
    val expirationDays: Int? = null,
    
    // Pending card fields
    val title: String? = null,
    val description: String? = null,
    @SerialName("source_user_id")
    @Serializable(with = NullableStringIntSerializer::class)
    val sourceUserId: Int? = null,
    @SerialName("source_user_name")
    val sourceUserName: String? = null
)

@Serializable
enum class RecurrenceType {
    @SerialName("weekly")
    WEEKLY,
    @SerialName("monthly")
    MONTHLY,
    @SerialName("one-time")
    ONE_TIME
}

/**
 * Response wrapper for cards list
 */
@Serializable
data class CardsResponse(
    val cards: List<Card>,
    @SerialName("no_events")
    val noEvents: NoEvents? = null
)

@Serializable
data class NoEvents(
    val today: Boolean = false,
    val tomorrow: Boolean = false
)

/**
 * Response wrapper for single card
 */
@Serializable
data class CardResponse(
    val success: Boolean,
    val card: Card? = null,
    val error: String? = null
)

package com.otheruncle.memorydisplay.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * User model matching the API response from /auth/login and /profile
 */
@Serializable
data class User(
    val id: Int,
    val name: String,
    val email: String,
    val role: UserRole,
    @SerialName("profile_photo")
    val profilePhoto: String? = null,
    val birthday: String? = null,
    val city: String? = null,
    val state: String? = null,
    val timezone: String? = null,
    @SerialName("calendar_url")
    val calendarUrl: String? = null,
    @SerialName("calendar_reviewer")
    val calendarReviewer: Boolean = false,
    @SerialName("event_reminders")
    val eventReminders: Boolean = false
)

@Serializable
enum class UserRole {
    @SerialName("sys-admin")
    SYS_ADMIN,
    @SerialName("client-admin")
    CLIENT_ADMIN,
    @SerialName("family-member")
    FAMILY_MEMBER
}

/**
 * User status in admin list
 */
@Serializable
enum class UserStatus {
    @SerialName("not-invited")
    NOT_INVITED,
    @SerialName("invited")
    INVITED,
    @SerialName("active")
    ACTIVE
}

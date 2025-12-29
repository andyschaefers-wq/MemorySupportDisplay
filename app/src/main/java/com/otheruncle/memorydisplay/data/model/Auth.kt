package com.otheruncle.memorydisplay.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Login request body
 */
@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

/**
 * Login response
 */
@Serializable
data class LoginResponse(
    val success: Boolean,
    val user: User? = null,
    val error: String? = null
)

/**
 * Forgot password request
 */
@Serializable
data class ForgotPasswordRequest(
    val email: String
)

/**
 * Reset password request
 */
@Serializable
data class ResetPasswordRequest(
    val token: String,
    val password: String
)

/**
 * Account setup request (from invitation)
 */
@Serializable
data class SetupRequest(
    val token: String,
    val password: String
)

/**
 * Generic success response
 */
@Serializable
data class SuccessResponse(
    val success: Boolean,
    val message: String? = null,
    val error: String? = null
)

/**
 * Profile update request
 */
@Serializable
data class ProfileUpdateRequest(
    val birthday: String? = null,
    val city: String? = null,
    val state: String? = null,
    val timezone: String? = null,
    @SerialName("calendar_url")
    val calendarUrl: String? = null,
    @SerialName("event_reminders")
    val eventReminders: Boolean? = null
)

/**
 * Profile response
 */
@Serializable
data class ProfileResponse(
    val success: Boolean,
    val user: User? = null,
    val error: String? = null
)

/**
 * Password change request
 */
@Serializable
data class PasswordChangeRequest(
    @SerialName("current_password")
    val currentPassword: String,
    @SerialName("new_password")
    val newPassword: String
)

/**
 * Photo upload response
 */
@Serializable
data class PhotoUploadResponse(
    val success: Boolean,
    @SerialName("profile_photo")
    val profilePhoto: String? = null,
    @SerialName("image_path")
    val imagePath: String? = null,
    val error: String? = null
)

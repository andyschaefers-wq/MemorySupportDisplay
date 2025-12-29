package com.otheruncle.memorydisplay.data.model

import kotlinx.serialization.Serializable

/**
 * Holiday model
 */
@Serializable
data class Holiday(
    val id: Int,
    val name: String,
    val date: String,
    val icon: String? = null
)

/**
 * Holidays list response
 */
@Serializable
data class HolidaysResponse(
    val holidays: List<Holiday>
)

/**
 * Holiday create/update request
 */
@Serializable
data class HolidayRequest(
    val name: String,
    val date: String,
    val icon: String? = null
)

/**
 * Holiday response
 */
@Serializable
data class HolidayResponse(
    val success: Boolean,
    val holiday: Holiday? = null,
    val error: String? = null
)

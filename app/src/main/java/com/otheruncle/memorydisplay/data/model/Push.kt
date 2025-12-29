package com.otheruncle.memorydisplay.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Push token registration request
 */
@Serializable
data class PushRegisterRequest(
    val token: String,
    @SerialName("device_id")
    val deviceId: String
)

/**
 * Push token unregister request
 */
@Serializable
data class PushUnregisterRequest(
    @SerialName("device_id")
    val deviceId: String
)

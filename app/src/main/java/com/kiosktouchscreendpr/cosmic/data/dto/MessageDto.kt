package com.kiosktouchscreendpr.cosmic.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Code author  : Anugrah Surya Putra.
 * Project      : Cosmic
 */

@Serializable
data class MessageDto(
    @SerialName("token") val token: String,
    @SerialName("isActive")val isActive: Boolean,
    @SerialName("message")val message: String,
)
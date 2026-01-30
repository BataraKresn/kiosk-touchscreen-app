package com.kiosktouchscreendpr.cosmic.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response dari register device
 * Endpoint: POST /api/displays/register
 */
@Serializable
data class RegisterDeviceResponse(
    @SerialName("message")
    val message: String,
    
    @SerialName("display")
    val display: DisplayInfo
)

@Serializable
data class DisplayInfo(
    @SerialName("id")
    val id: Int,
    
    @SerialName("token")
    val token: String,
    
    @SerialName("name")
    val name: String,
    
    @SerialName("device_info")
    val deviceInfo: Map<String, String>? = null,
    
    @SerialName("created_at")
    val createdAt: String? = null,
    
    @SerialName("updated_at")
    val updatedAt: String? = null
)

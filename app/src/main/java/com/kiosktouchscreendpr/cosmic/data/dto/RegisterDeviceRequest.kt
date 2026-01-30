package com.kiosktouchscreendpr.cosmic.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request untuk register device ke backend
 * Endpoint: POST /api/displays/register
 */
@Serializable
data class RegisterDeviceRequest(
    @SerialName("token")
    val token: String,
    
    @SerialName("name")
    val name: String,
    
    @SerialName("device_info")
    val deviceInfo: DeviceMetadata
)

@Serializable
data class DeviceMetadata(
    @SerialName("manufacturer")
    val manufacturer: String,
    
    @SerialName("model")
    val model: String,
    
    @SerialName("android_version")
    val androidVersion: String,
    
    @SerialName("app_version")
    val appVersion: String
)

package com.kiosktouchscreendpr.cosmic.data.api

import android.content.Context
import android.provider.Settings
import android.os.Build
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import android.util.Log
import kotlinx.coroutines.delay
import com.kiosktouchscreendpr.cosmic.data.cache.ResponseCache

/**
 * Service for device registration and heartbeat with CMS
 */
class DeviceRegistrationService(
    private val context: Context,
    private val baseUrl: String, // e.g., "https://kiosk.mugshot.dev"
    private val responseCache: ResponseCache? = null
) {
    private val tag = "DeviceRegistration"
    
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
        
        // Add timeout to prevent hanging requests
        install(io.ktor.client.plugins.HttpTimeout) {
            requestTimeoutMillis = 15000
            connectTimeoutMillis = 10000
            socketTimeoutMillis = 15000
        }
    }

    @Serializable
    data class RegistrationRequest(
        val device_name: String,
        val device_id: String,
        val mac_address: String? = null,
        val android_version: String,
        val app_version: String,
        val ip_address: String? = null
    )

    @Serializable
    data class RegistrationResponse(
        val success: Boolean,
        val message: String? = null,
        val data: RegistrationData? = null
    )

    @Serializable
    data class RegistrationData(
        val remote_id: Int,
        val token: String,
        val remote_control_enabled: Boolean,
        val websocket_url: String,
        val instructions: String? = null
    )

    @Serializable
    data class HeartbeatRequest(
        val status: String = "online",
        val battery_level: Int? = null,
        val wifi_strength: Int? = null,
        val screen_on: Boolean? = null,
        val storage_available_mb: Long? = null,
        val storage_total_mb: Long? = null,
        val ram_usage_mb: Int? = null,
        val ram_total_mb: Int? = null,
        val cpu_temp: Float? = null,
        val network_type: String? = null,
        val current_url: String? = null
    )

    @Serializable
    data class HeartbeatResponse(
        val success: Boolean,
        val data: HeartbeatData? = null
    )

    @Serializable
    data class HeartbeatData(
        val remote_control_enabled: Boolean,
        val should_reconnect: Boolean
    )

    /**
     * Register device with CMS on first launch
     * Returns token for future authentication
     */
    suspend fun registerDevice(deviceName: String): Result<RegistrationData> {
        return try {
            val deviceId = getDeviceId()
            val androidVersion = Build.VERSION.RELEASE
            val appVersion = getAppVersion()

            val request = RegistrationRequest(
                device_name = deviceName,
                device_id = deviceId,
                android_version = androidVersion,
                app_version = appVersion
            )

            Log.d(tag, "Registering device: $deviceName (ID: $deviceId)")

            val response: HttpResponse = client.post("$baseUrl/api/devices/register") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            if (response.status == HttpStatusCode.Created || response.status == HttpStatusCode.OK) {
                val body = response.body<RegistrationResponse>()
                if (body.success && body.data != null) {
                    Log.i(tag, "Device registered successfully. Remote ID: ${body.data.remote_id}")
                    Result.success(body.data)
                } else {
                    Log.e(tag, "Registration failed: ${body.message}")
                    Result.failure(Exception(body.message ?: "Registration failed"))
                }
            } else {
                Log.e(tag, "Registration HTTP error: ${response.status}")
                Result.failure(Exception("HTTP ${response.status.value}"))
            }
        } catch (e: Exception) {
            Log.e(tag, "Registration exception", e)
            Result.failure(e)
        }
    }

    /**
     * Send heartbeat to CMS to maintain online status
     * Should be called every 30 seconds with full device health metrics
     */
    suspend fun sendHeartbeat(
        token: String,
        batteryLevel: Int? = null,
        wifiStrength: Int? = null,
        screenOn: Boolean? = null,
        storageAvailableMB: Long? = null,
        storageTotalMB: Long? = null,
        ramUsageMB: Int? = null,
        ramTotalMB: Int? = null,
        cpuTemp: Float? = null,
        networkType: String? = null,
        currentUrl: String? = null
    ): Result<HeartbeatData> {
        return try {
            val request = HeartbeatRequest(
                status = "online",
                battery_level = batteryLevel,
                wifi_strength = wifiStrength,
                screen_on = screenOn,
                storage_available_mb = storageAvailableMB,
                storage_total_mb = storageTotalMB,
                ram_usage_mb = ramUsageMB,
                ram_total_mb = ramTotalMB,
                cpu_temp = cpuTemp,
                network_type = networkType,
                current_url = currentUrl
            )

            val response: HttpResponse = client.post("$baseUrl/api/devices/heartbeat") {
                contentType(ContentType.Application.Json)
                bearerAuth(token)
                setBody(request)
            }

            if (response.status == HttpStatusCode.OK) {
                val body = response.body<HeartbeatResponse>()
                if (body.success && body.data != null) {
                    // Cache response to reduce network load
                    responseCache?.cacheHeartbeatResponse(body.data.remote_control_enabled)
                    
                    Log.d(tag, "Heartbeat sent. Remote control: ${body.data.remote_control_enabled}")
                    Result.success(body.data)
                } else {
                    Result.failure(Exception("Heartbeat failed"))
                }
            } else {
                Result.failure(Exception("HTTP ${response.status.value}"))
            }
        } catch (e: Exception) {
            Log.w(tag, "Heartbeat exception", e)
            Result.failure(e)
        }
    }

    /**
     * Unregister device (on app uninstall or reset)
     */
    suspend fun unregisterDevice(token: String): Result<Boolean> {
        return try {
            val response: HttpResponse = client.delete("$baseUrl/api/devices/unregister") {
                bearerAuth(token)
            }

            if (response.status == HttpStatusCode.OK) {
                Log.i(tag, "Device unregistered successfully")
                Result.success(true)
            } else {
                Result.failure(Exception("HTTP ${response.status.value}"))
            }
        } catch (e: Exception) {
            Log.e(tag, "Unregister exception", e)
            Result.failure(e)
        }
    }

    /**
     * Start periodic heartbeat
     * Call this after registration or app start
     */
    suspend fun startHeartbeatLoop(token: String, intervalMs: Long = 30_000L) {
        while (true) {
            sendHeartbeat(token)
            delay(intervalMs)
        }
    }

    /**
     * Get unique Android device ID
     */
    private fun getDeviceId(): String {
        return Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: "unknown-${System.currentTimeMillis()}"
    }

    /**
     * Get app version from BuildConfig
     */
    private fun getAppVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }

    fun close() {
        client.close()
    }
}

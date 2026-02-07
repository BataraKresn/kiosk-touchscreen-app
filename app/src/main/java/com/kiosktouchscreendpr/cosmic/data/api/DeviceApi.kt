package com.kiosktouchscreendpr.cosmic.data.api

import android.os.Build
import android.util.Log
import com.kiosktouchscreendpr.cosmic.data.dto.DisplayListResponse
import com.kiosktouchscreendpr.cosmic.data.dto.RegisterDeviceRequest
import com.kiosktouchscreendpr.cosmic.data.dto.RegisterDeviceResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import javax.inject.Inject
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.net.NetworkInterface
import java.net.Inet4Address

/**
 * API untuk register Remote device ke backend CMS
 */
class DeviceApi @Inject constructor(
    private val client: HttpClient
) {
    /**
     * Check if device exists in CMS database
     * Endpoint: GET /api/devices/check
     * 
     * Mengirim: device_id
     * Menerima: remote_id, token jika device sudah terdaftar
     */
    suspend fun checkExistingDevice(
        baseUrl: String,
        deviceId: String
    ): RegisterRemoteResponse? {
        return try {
            Log.d("DeviceApi", "🔍 START: checkExistingDevice()")
            Log.d("DeviceApi", "📍 Target: $baseUrl/api/devices/check")
            Log.d("DeviceApi", "📦 Device ID: $deviceId")
            
            Log.d("DeviceApi", "🌐 Sending HTTP GET request...")
            val response = client.get("$baseUrl/api/devices/check") {
                parameter("device_id", deviceId)
            }.body<RegisterRemoteResponse>()

            Log.d("DeviceApi", "✅ Device found in database")
            Log.d("DeviceApi", "  - success: ${response.success}")
            Log.d("DeviceApi", "  - message: ${response.message}")
            Log.d("DeviceApi", "  - remote_id: ${response.data.remoteId}")
            Log.d("DeviceApi", "  - token: ${response.data.token.take(10)}...")
            
            response
        } catch (e: Exception) {
            Log.d("DeviceApi", "📱 Device not found in database - will need registration")
            Log.d("DeviceApi", "   Exception: ${e.message}")
            null
        }
    }

    /**
     * Register device ke Remote
     * Endpoint: POST /api/devices/register
     * 
     * Mengirim: device_name, device_id, mac_address, android_version, app_version, ip_address
     * Menerima: remote_id, token, remote_control_enabled, websocket_url
     */
    suspend fun registerRemoteDevice(
        baseUrl: String,
        deviceId: String,
        deviceName: String,
        appVersion: String
    ): RegisterRemoteResponse? {
        return try {
            Log.d("DeviceApi", "🔵 START: registerRemoteDevice()")
            Log.d("DeviceApi", "📍 Target: $baseUrl/api/devices/register")
            
            val macAddress = getMacAddress()
            val androidVersion = Build.VERSION.RELEASE
            val ipAddress = getLocalIpAddress()
            
            Log.d("DeviceApi", "📦 Request payload:")
            Log.d("DeviceApi", "  - deviceName: $deviceName")
            Log.d("DeviceApi", "  - deviceId: $deviceId")
            Log.d("DeviceApi", "  - macAddress: $macAddress")
            Log.d("DeviceApi", "  - androidVersion: $androidVersion")
            Log.d("DeviceApi", "  - appVersion: $appVersion")
            Log.d("DeviceApi", "  - ipAddress: $ipAddress")
            
            val request = RemoteRegisterRequest(
                deviceName = deviceName,
                deviceId = deviceId,
                macAddress = macAddress,
                androidVersion = androidVersion,
                appVersion = appVersion,
                ipAddress = ipAddress
            )

            Log.d("DeviceApi", "🌐 Sending HTTP POST request...")
            val response = client.post("$baseUrl/api/devices/register") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body<RegisterRemoteResponse>()

            Log.d("DeviceApi", "✅ HTTP Response received successfully")
            Log.d("DeviceApi", "  - success: ${response.success}")
            Log.d("DeviceApi", "  - message: ${response.message}")
            Log.d("DeviceApi", "  - remote_id: ${response.data.remoteId}")
            Log.d("DeviceApi", "  - token: ${response.data.token.take(10)}...")
            
            response
        } catch (e: Exception) {
            Log.e("DeviceApi", "❌ EXCEPTION in registerRemoteDevice(): ${e.message}", e)
            Log.e("DeviceApi", "   Exception type: ${e::class.simpleName}")
            Log.e("DeviceApi", "   Cause: ${e.cause?.message}")
            e.printStackTrace()
            null
        }
    }

    /**
     * Ambil daftar display dari CMS
     * Endpoint: GET /api/displays?search=...&per_page=...
     */
    suspend fun getDisplays(
        baseUrl: String,
        search: String? = null,
        perPage: Int = 50
    ): DisplayListResponse? {
        return try {
            client.get("$baseUrl/api/displays") {
                if (!search.isNullOrBlank()) {
                    parameter("search", search)
                }
                parameter("per_page", perPage)
            }.body<DisplayListResponse>()
        } catch (e: Exception) {
            Log.w("DeviceApi", "Failed to fetch displays: ${e.message}", e)
            null
        }
    }

    private fun getMacAddress(): String? {
        return try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                val mac = networkInterface.hardwareAddress ?: continue

                val macAddress = mac.joinToString(":") {
                    "%02X".format(it)
                }
                if (macAddress != "00:00:00:00:00:00") {
                    return macAddress
                }
            }
            null
        } catch (e: Exception) {
            Log.w("DeviceApi", "Error getting MAC: ${e.message}")
            null
        }
    }

    private fun getLocalIpAddress(): String? {
        return try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                val addresses = networkInterface.inetAddresses

                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address is Inet4Address) {
                        return address.hostAddress
                    }
                }
            }
            null
        } catch (e: Exception) {
            Log.w("DeviceApi", "Error getting IP: ${e.message}")
            null
        }
    }

    /**
     * Register display token ke backend
     * Endpoint: POST /api/displays/register
     * Untuk mengirim info device saat display token sudah ada
     */
    suspend fun registerDisplayToken(
        baseUrl: String,
        request: RegisterDeviceRequest
    ): RegisterDeviceResponse? {
        return try {
            val response = client.post("$baseUrl/api/displays/register") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body<RegisterDeviceResponse>()
            
            Log.d("DeviceApi", "✅ Display token registered: ${request.token}")
            response
        } catch (e: Exception) {
            Log.w("DeviceApi", "Failed to register display: ${e.message}", e)
            null
        }
    }
}

@Serializable
data class RemoteRegisterRequest(
    @SerialName("device_name")
    val deviceName: String,

    @SerialName("device_id")
    val deviceId: String,

    @SerialName("mac_address")
    val macAddress: String? = null,

    @SerialName("android_version")
    val androidVersion: String? = null,

    @SerialName("app_version")
    val appVersion: String? = null,

    @SerialName("ip_address")
    val ipAddress: String? = null
)

@Serializable
data class RegisterRemoteResponse(
    @SerialName("success")
    val success: Boolean,

    @SerialName("message")
    val message: String,

    @SerialName("data")
    val data: RemoteData
)

@Serializable
data class RemoteData(
    @SerialName("remote_id")
    val remoteId: Int,

    @SerialName("token")
    val token: String,

    @SerialName("remote_control_enabled")
    val remoteControlEnabled: Boolean = true,

    @SerialName("websocket_url")
    val websocketUrl: String? = null
)

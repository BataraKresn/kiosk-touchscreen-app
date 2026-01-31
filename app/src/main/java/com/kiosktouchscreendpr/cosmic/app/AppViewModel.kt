package com.kiosktouchscreendpr.cosmic.app

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kiosktouchscreendpr.cosmic.BuildConfig
import com.kiosktouchscreendpr.cosmic.app.AppState.Status
import com.kiosktouchscreendpr.cosmic.core.constant.AppConstant
import com.kiosktouchscreendpr.cosmic.core.utils.ConnectivityObserver
import com.kiosktouchscreendpr.cosmic.core.utils.DeviceHealthMonitor
import com.kiosktouchscreendpr.cosmic.core.utils.Preference
import com.kiosktouchscreendpr.cosmic.core.utils.formatLink
import com.kiosktouchscreendpr.cosmic.core.utils.getDeviceIP
import com.kiosktouchscreendpr.cosmic.data.api.DeviceApi
import com.kiosktouchscreendpr.cosmic.data.api.DeviceRegistrationService
import com.kiosktouchscreendpr.cosmic.data.cache.ResponseCache
import com.kiosktouchscreendpr.cosmic.data.datasource.heartbeat.Message
import com.kiosktouchscreendpr.cosmic.domain.usecase.WebSocketUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * Code author  : Anugrah Surya Putra.
 * Project      : Cosmic
 */

@HiltViewModel
class AppViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preference: Preference,
    private val heartBeat: WebSocketUseCase,
    private val connectivityObserver: ConnectivityObserver,
    private val deviceApi: DeviceApi,
    private val deviceHealthMonitor: DeviceHealthMonitor,
    private val responseCache: ResponseCache
) : ViewModel() {

    private val ipAddress: String? = getDeviceIP()
    private val formatLink: String = formatLink(ipAddress ?: "")

    private val websocketUrl = BuildConfig.WS_URL
    private val wsUrl = "$websocketUrl/ws_status_device?url=$formatLink"

    private val _state = MutableStateFlow(AppState())
    val state = _state
        .onStart {
            registerDeviceOnFirstLaunch()
            startPeriodicHealthHeartbeat()
            observeNetwork()
            observeWsMessages()
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = _state.value
        )


    val isLoggedIn: String?
        get() = preference.get(AppConstant.AUTH, null)

    val token: String?
        get() = preference.get(AppConstant.TOKEN, null)

    private fun connectWs() = viewModelScope.launch {
        try {
            heartBeat.connect(wsUrl)
            _state.update { it.copy(status = Status.CONNECTED, error = null) }
        } catch (e: Exception) {
            _state.update {
                it.copy(
                    status = Status.DISCONNECTED,
                    error = e.message ?: "Unknown error"
                )
            }
        }
    }

    private fun disconnectWs() = viewModelScope.launch {
        heartBeat.disconnect()
        _state.update { it.copy(status = Status.DISCONNECTED, error = null) }
    }

    private fun observeNetwork() = viewModelScope.launch {
        connectivityObserver.isConnected.collect { connected ->
            if (connected) {
                println("🟢 Network available, trying to connect WebSocket")
                connectWs()
            } else {
                println("🔴 Network lost, disconnecting WebSocket")
                disconnectWs()
            }
        }
    }

    private fun observeWsMessages() = viewModelScope.launch {
        heartBeat.observeMessages().collect { message ->
            when (message) {

                is Message.Error -> {
                    _state.update { it.copy(status = Status.DISCONNECTED, error = message.message) }
                }

                is Message.Text -> {
                    // handle messages from server
                    /*println("Received text: ${message.content}")*/
                }

                else -> Unit
            }
        }
    }

    /**
     * Register device pada first launch (untuk Remote menu)
     * Call POST /api/devices/register dengan device_id dan device info
     * Save remote_id & remote_token untuk keperluan remote control
     */
    private fun registerDeviceOnFirstLaunch() = viewModelScope.launch {
        try {
            // Generate atau ambil device_id yang sudah ada
            val deviceId = getOrCreateDeviceId()
            
            val baseUrl = BuildConfig.WEBVIEW_BASEURL
            val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}".uppercase()
            
            val response = deviceApi.registerRemoteDevice(
                baseUrl = baseUrl,
                deviceId = deviceId,
                deviceName = deviceName,
                appVersion = BuildConfig.VERSION_NAME
            )
            
            if (response != null && response.success) {
                // Save remote_id & remote_token untuk remote control
                preference.set(AppConstant.REMOTE_ID, response.data.remoteId.toString())
                preference.set(AppConstant.REMOTE_TOKEN, response.data.token)
                
                Log.d(TAG, "✅ Remote registered: remote_id=${response.data.remoteId}, device_id=$deviceId")
            } else {
                Log.w(TAG, "⚠️ Failed to register remote, but continuing anyway")
            }
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Error registering remote on first launch: ${e.message}")
        }
    }

    /**
     * Generate device_id unik atau ambil dari SharedPreferences jika sudah ada
     */
    private fun getOrCreateDeviceId(): String {
        val savedDeviceId = preference.get(AppConstant.DEVICE_ID, null)
        
        return if (savedDeviceId.isNullOrBlank()) {
            // Generate device_id baru dari Build.SERIAL atau UUID
            val newDeviceId = Build.SERIAL.takeIf { it != "unknown" }
                ?: UUID.randomUUID().toString().take(12)
            
            // Simpan ke SharedPreferences
            preference.set(AppConstant.DEVICE_ID, newDeviceId)
            
            Log.d(TAG, "Generated new device_id: $newDeviceId")
            newDeviceId
        } else {
            savedDeviceId
        }
    }

    /**
     * Start periodic health heartbeat every 30 seconds
     * Sends real device metrics to backend
     */
    private fun startPeriodicHealthHeartbeat() = viewModelScope.launch {
        val registrationService = DeviceRegistrationService(
            context = context,
            baseUrl = BuildConfig.WEBVIEW_BASEURL,
            responseCache = responseCache
        )
        
        while (true) {
            try {
                val token = preference.get(AppConstant.REMOTE_TOKEN, null)

                if (!token.isNullOrBlank()) {
                    // Get all health metrics
                    val metrics = deviceHealthMonitor.getAllMetrics()
                    
                    // Get current URL from preference
                    val currentUrl = preference.get(AppConstant.TOKEN, null)?.let { displayToken ->
                        "${BuildConfig.WEBVIEW_BASEURL}/display/$displayToken"
                    }
                    
                    // Send heartbeat with full metrics
                    val result = registrationService.sendHeartbeat(
                        token = token,
                        batteryLevel = metrics.batteryLevel,
                        wifiStrength = metrics.wifiStrength,
                        screenOn = metrics.screenOn,
                        storageAvailableMB = metrics.storageAvailableMB,
                        storageTotalMB = metrics.storageTotalMB,
                        ramUsageMB = metrics.ramUsageMB,
                        ramTotalMB = metrics.ramTotalMB,
                        cpuTemp = metrics.cpuTemp,
                        networkType = metrics.networkType,
                        currentUrl = currentUrl
                    )
                    
                    // Disabled for production performance
                    // if (result.isSuccess) {
                    //     Log.v(TAG, "✅ Heartbeat sent: ${metrics}")
                    // } else {
                    //     Log.w(TAG, "⚠️ Heartbeat failed: ${result.exceptionOrNull()?.message}")
                    // }
                }
            } catch (e: Exception) {
                // Log.e(TAG, "❌ Error in health heartbeat", e) // Disabled for performance
            }

            // Wait 30 seconds before next heartbeat
            delay(30_000L)
        }
    }

    companion object {
        private const val TAG = "AppViewModel"
    }
}

data class AppState(
    val status: Status = Status.CONNECTING,
    val error: String? = null
) {
    enum class Status {
        DISCONNECTED, CONNECTING, CONNECTED
    }
}
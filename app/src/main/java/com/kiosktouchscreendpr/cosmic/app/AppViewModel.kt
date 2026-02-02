package com.kiosktouchscreendpr.cosmic.app

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kiosktouchscreendpr.cosmic.BuildConfig
import com.kiosktouchscreendpr.cosmic.app.AppState.Status
import com.kiosktouchscreendpr.cosmic.core.connection.ConnectionManager
import com.kiosktouchscreendpr.cosmic.core.connection.NetworkObserver
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
 * 
 * UPDATED: Integrated with ConnectionManager for anti-flapping
 * - ConnectionManager is now the SINGLE SOURCE OF TRUTH
 * - Network events are signals, not commands
 * - All reconnect logic delegated to ConnectionManager
 */

@HiltViewModel
class AppViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preference: Preference,
    private val heartBeat: WebSocketUseCase,
    private val connectivityObserver: ConnectivityObserver,
    private val deviceApi: DeviceApi,
    private val deviceHealthMonitor: DeviceHealthMonitor,
    private val responseCache: ResponseCache,
    private val connectionManager: ConnectionManager,
    private val networkObserver: NetworkObserver
) : ViewModel() {

    private val ipAddress: String? = getDeviceIP()
    private val formatLink: String = formatLink(ipAddress ?: "")

    private val websocketUrl = BuildConfig.WS_URL
    private val wsUrl = "$websocketUrl/ws_status_device?url=$formatLink"

    private val _state = MutableStateFlow(AppState())
    val state = _state
        .onStart {
            registerDeviceOnFirstLaunch()
            observeConnectionManager()
            observeNetworkForConnectionManager()
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

    /**
     * FIX #1: Delegate all connection logic to ConnectionManager
     * No more direct WebSocket connection from AppViewModel
     */
    private fun connectWs() = viewModelScope.launch {
        val remoteToken = preference.get(AppConstant.REMOTE_TOKEN, null)
        if (!remoteToken.isNullOrBlank()) {
            connectionManager.connect(remoteToken)
        } else {
            Log.w(TAG, "No remote token available, skipping connection")
        }
    }

    private fun disconnectWs() = viewModelScope.launch {
        connectionManager.disconnect("User-initiated disconnect")
    }

    /**
     * FIX #4: Network events are SIGNALS to ConnectionManager
     * NetworkObserver reports stability, ConnectionManager decides action
     */
    private fun observeNetworkForConnectionManager() = viewModelScope.launch {
        networkObserver.isStable.collect { stable ->
            if (stable) {
                Log.d(TAG, "🟢 Network stable, notifying ConnectionManager")
                connectionManager.onNetworkAvailable()
                connectWs() // Initiate connection via manager
            } else {
                Log.d(TAG, "🔴 Network unstable, notifying ConnectionManager")
                connectionManager.onNetworkLost()
            }
        }
    }

    /**
     * FIX #1: Observe ConnectionManager state for UI updates
     */
    private fun observeConnectionManager() = viewModelScope.launch {
        connectionManager.connectionState.collect { connState ->
            when (connState) {
                is ConnectionManager.ConnectionState.Connected -> {
                    _state.update { it.copy(status = Status.CONNECTED, error = null) }
                    Log.i(TAG, "✅ Connected via ConnectionManager")
                }
                is ConnectionManager.ConnectionState.Connecting -> {
                    _state.update { it.copy(status = Status.CONNECTING, error = null) }
                }
                is ConnectionManager.ConnectionState.Reconnecting -> {
                    _state.update { 
                        it.copy(
                            status = Status.CONNECTING, 
                            error = "Reconnecting (attempt ${connState.attempt})..."
                        ) 
                    }
                }
                is ConnectionManager.ConnectionState.Disconnected -> {
                    _state.update { it.copy(status = Status.DISCONNECTED, error = null) }
                }
                is ConnectionManager.ConnectionState.Error -> {
                    _state.update { 
                        it.copy(
                            status = Status.DISCONNECTED, 
                            error = connState.message
                        ) 
                    }
                }
                is ConnectionManager.ConnectionState.ServerBlocked -> {
                    val until = connState.until
                    val message = if (until != null) {
                        "Server blocked reconnect until ${(until - System.currentTimeMillis()) / 1000}s"
                    } else {
                        "Server blocked reconnect indefinitely"
                    }
                    _state.update { 
                        it.copy(
                            status = Status.DISCONNECTED, 
                            error = message
                        ) 
                    }
                    Log.w(TAG, "⛔ $message")
                }
                is ConnectionManager.ConnectionState.CircuitOpen -> {
                    _state.update { 
                        it.copy(
                            status = Status.DISCONNECTED, 
                            error = "Circuit breaker open (too many failures)"
                        ) 
                    }
                    Log.e(TAG, "🚨 Circuit breaker open")
                }
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
     * 
     * UPDATED: Start heartbeat after successful registration
     */
    private fun registerDeviceOnFirstLaunch() = viewModelScope.launch {
        try {
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
                preference.set(AppConstant.REMOTE_ID, response.data.remoteId.toString())
                preference.set(AppConstant.REMOTE_TOKEN, response.data.token)
                
                // START HEARTBEAT via ConnectionManager
                connectionManager.connect(response.data.token)
            }
        } catch (e: Exception) {
            // Continue on error
        }
    }

    /**
     * Generate device_id unik atau ambil dari SharedPreferences jika sudah ada
     */
    private fun getOrCreateDeviceId(): String {
        val savedDeviceId = preference.get(AppConstant.DEVICE_ID, null)
        
        return if (savedDeviceId.isNullOrBlank()) {
            val newDeviceId = Build.SERIAL.takeIf { it != "unknown" }
                ?: UUID.randomUUID().toString().take(12)
            
            preference.set(AppConstant.DEVICE_ID, newDeviceId)
            newDeviceId
        } else {
            savedDeviceId
        }
    }

    /**
     * NOTE: Health heartbeat is now handled by ConnectionManager
     * ConnectionManager sends heartbeats with device metrics automatically
     * This method is kept for backward compatibility but does nothing
     */

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
package com.kiosktouchscreendpr.cosmic.presentation.remotecontrol

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kiosktouchscreendpr.cosmic.data.services.InputInjectionService
import com.kiosktouchscreendpr.cosmic.data.services.RemoteControlWebSocketClient
import com.kiosktouchscreendpr.cosmic.data.services.ScreenCaptureService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for managing remote control functionality
 */
@HiltViewModel
class RemoteControlViewModel @Inject constructor(
    private val webSocketClient: RemoteControlWebSocketClient
) : ViewModel() {

    private val _remoteControlState = MutableStateFlow<RemoteControlState>(RemoteControlState.Idle)
    val remoteControlState: StateFlow<RemoteControlState> = _remoteControlState.asStateFlow()

    private val _connectionStatus = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Disconnected)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    /**
     * Start remote control session
     */
    fun startRemoteControl(
        context: Context,
        deviceId: String,
        authToken: String,
        relayServerUrl: String
    ) {
        viewModelScope.launch {
            try {
                Log.e("RemoteControlVM", "🚀🚀🚀 Starting remote control - deviceId: $deviceId, serverUrl: $relayServerUrl 🚀🚀🚀")
                _remoteControlState.value = RemoteControlState.Starting
                _connectionStatus.value = ConnectionStatus.Connecting
                
                // Initialize WebSocket connection
                webSocketClient.connect(
                    wsUrl = relayServerUrl,
                    token = authToken,
                    devId = deviceId
                )
                
                // Wait for actual connection (with timeout of 10 seconds)
                var attempts = 0
                val maxAttempts = 20 // 20 * 500ms = 10 seconds
                while (webSocketClient.connectionState.value != RemoteControlWebSocketClient.ConnectionState.CONNECTED && attempts < maxAttempts) {
                    delay(500) // Check every 500ms
                    attempts++
                    Log.d("RemoteControlVM", "Waiting for connection... attempt $attempts/${maxAttempts}, state: ${webSocketClient.connectionState.value}")
                }
                
                if (webSocketClient.connectionState.value == RemoteControlWebSocketClient.ConnectionState.CONNECTED) {
                    Log.d("RemoteControlVM", "WebSocket connected successfully")
                    _connectionStatus.value = ConnectionStatus.Connected
                    _remoteControlState.value = RemoteControlState.Active
                } else {
                    Log.e("RemoteControlVM", "Failed to connect. State: ${webSocketClient.connectionState.value}")
                    _remoteControlState.value = RemoteControlState.Error("Failed to connect to relay server after ${attempts * 500}ms")
                    _connectionStatus.value = ConnectionStatus.Disconnected
                }
                
            } catch (e: Exception) {
                Log.e("RemoteControlVM", "Error in startRemoteControl", e)
                _remoteControlState.value = RemoteControlState.Error(e.message ?: "Unknown error")
                _connectionStatus.value = ConnectionStatus.Error(e.message ?: "Connection failed")
            }
        }
    }

    /**
     * Request screen capture permission
     * Call this from Activity with startActivityForResult
     */
    fun requestScreenCapturePermission(activity: Activity, requestCode: Int) {
        val mediaProjectionManager = activity.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val intent = mediaProjectionManager.createScreenCaptureIntent()
        activity.startActivityForResult(intent, requestCode)
    }

    /**
     * Handle screen capture permission result
     */
    fun onScreenCapturePermissionGranted(context: Context, resultCode: Int, data: Intent?) {
        Log.e("RemoteControlVM", "📹📹📹 onScreenCapturePermissionGranted called - resultCode: $resultCode, data: $data, RESULT_OK: ${Activity.RESULT_OK}")
        viewModelScope.launch {
            if (resultCode == Activity.RESULT_OK) {
                if (data == null) {
                    Log.e("RemoteControlVM", "⚠️⚠️⚠️ WARNING: data is NULL but resultCode is OK! This should not happen!")
                    _remoteControlState.value = RemoteControlState.Error("Screen capture data is null")
                    return@launch
                }
                
                try {
                    Log.e("RemoteControlVM", "⏳ Waiting 500ms for WebSocket to establish...")
                    // Wait a bit for WebSocket to establish
                    delay(500)
                    
                    Log.e("RemoteControlVM", "🎬🎬🎬 Starting ScreenCaptureService NOW!")
                    
                    // Store MediaProjection data directly in ScreenCaptureService companion object
                    // This MUST be done BEFORE starting the service
                    Log.e("RemoteControlVM", "💾 Storing data directly in ScreenCaptureService static properties")
                    ScreenCaptureService.mediaProjectionResultCode = resultCode
                    ScreenCaptureService.mediaProjectionData = data
                    Log.e("RemoteControlVM", "✅ Data stored: resultCode=$resultCode, data=$data")
                    
                    // Start ScreenCaptureService
                    val intent = Intent(context, ScreenCaptureService::class.java)
                    context.startForegroundService(intent)
                    Log.e("RemoteControlVM", "✅✅✅ ScreenCaptureService.startForegroundService() called successfully!")
                } catch (e: Exception) {
                    Log.e("RemoteControlVM", "❌❌❌ Failed to start ScreenCaptureService", e)
                    _remoteControlState.value = RemoteControlState.Error("Failed to start screen capture: ${e.message}")
                }
            } else {
                Log.e("RemoteControlVM", "❌ Screen capture permission DENIED - resultCode: $resultCode")
                _remoteControlState.value = RemoteControlState.Error("Screen capture permission denied")
                _connectionStatus.value = ConnectionStatus.Disconnected
            }
        }
    }

    /**
     * Stop remote control session
     */
    fun stopRemoteControl(context: Context) {
        viewModelScope.launch {
            try {
                // Stop WebSocket
                webSocketClient.disconnect()
                
                // Stop ScreenCaptureService
                context.stopService(Intent(context, ScreenCaptureService::class.java))
                
                _connectionStatus.value = ConnectionStatus.Disconnected
                _remoteControlState.value = RemoteControlState.Idle
                
            } catch (e: Exception) {
                _remoteControlState.value = RemoteControlState.Error(e.message ?: "Failed to stop")
            }
        }
    }

    /**
     * Check if accessibility service is enabled
     */
    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val serviceName = "${context.packageName}/${InputInjectionService::class.java.name}"
        val enabledServices = android.provider.Settings.Secure.getString(
            context.contentResolver,
            android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        return enabledServices?.contains(serviceName) == true
    }

    /**
     * Open accessibility settings
     */
    fun openAccessibilitySettings(context: Context) {
        val intent = Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }

    override fun onCleared() {
        super.onCleared()
        // Connection is managed by RemoteControlService; do not disconnect here
    }
}

/**
 * Remote control states
 */
sealed class RemoteControlState {
    object Idle : RemoteControlState()
    object Starting : RemoteControlState()
    object Active : RemoteControlState()
    data class Error(val message: String) : RemoteControlState()
}

/**
 * Connection status
 */
sealed class ConnectionStatus {
    object Disconnected : ConnectionStatus()
    object Connecting : ConnectionStatus()
    object Connected : ConnectionStatus()
    data class Error(val message: String) : ConnectionStatus()
}

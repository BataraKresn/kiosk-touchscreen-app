package com.kiosktouchscreendpr.cosmic.presentation.remotecontrol

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kiosktouchscreendpr.cosmic.data.services.InputInjectionService
import com.kiosktouchscreendpr.cosmic.data.services.RemoteControlWebSocketClient
import com.kiosktouchscreendpr.cosmic.data.services.ScreenCaptureService
import dagger.hilt.android.lifecycle.HiltViewModel
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
                _remoteControlState.value = RemoteControlState.Starting
                
                // Initialize WebSocket connection
                webSocketClient.connect(
                    wsUrl = relayServerUrl,
                    token = authToken,
                    devId = deviceId
                )
                
                _connectionStatus.value = ConnectionStatus.Connected
                _remoteControlState.value = RemoteControlState.Active
                
            } catch (e: Exception) {
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
        if (resultCode == Activity.RESULT_OK && data != null) {
            // Start ScreenCaptureService
            val intent = Intent(context, ScreenCaptureService::class.java).apply {
                putExtra("resultCode", resultCode)
                putExtra("data", data)
            }
            context.startForegroundService(intent)
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
        webSocketClient.disconnect()
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

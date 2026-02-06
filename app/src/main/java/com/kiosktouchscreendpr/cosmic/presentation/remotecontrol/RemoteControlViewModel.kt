package com.kiosktouchscreendpr.cosmic.presentation.remotecontrol

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.provider.Settings
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kiosktouchscreendpr.cosmic.BuildConfig
import com.kiosktouchscreendpr.cosmic.core.constant.AppConstant
import com.kiosktouchscreendpr.cosmic.data.api.DeviceApi
import com.kiosktouchscreendpr.cosmic.data.services.AdaptiveQualityController
import com.kiosktouchscreendpr.cosmic.data.services.ConnectionHealthMonitor
import com.kiosktouchscreendpr.cosmic.data.services.InputInjectionService
import com.kiosktouchscreendpr.cosmic.data.services.MetricsReporter
import com.kiosktouchscreendpr.cosmic.data.services.RemoteControlWebSocketClient
import com.kiosktouchscreendpr.cosmic.data.services.ScreenCaptureService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import android.content.SharedPreferences
import javax.inject.Inject

/**
 * ViewModel for managing remote control functionality
 * 
 * Integrates:
 * - WebSocket connection management
 * - Health monitoring
 * - Adaptive quality control
 * - Screen capture management
 * - Input injection
 */
@HiltViewModel
class RemoteControlViewModel @Inject constructor(
    private val webSocketClient: RemoteControlWebSocketClient,
    private val adaptiveQuality: AdaptiveQualityController,
    private val healthMonitor: ConnectionHealthMonitor,
    private val metricsReporter: MetricsReporter,
    private val deviceApi: DeviceApi,
    private val preferences: SharedPreferences,
    private val appScope: kotlinx.coroutines.CoroutineScope,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _remoteControlState = MutableStateFlow<RemoteControlState>(RemoteControlState.Idle)
    val remoteControlState: StateFlow<RemoteControlState> = _remoteControlState.asStateFlow()

    private val _connectionStatus = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Disconnected)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()
    
    // Expose health metrics to UI
    val connectionHealth = healthMonitor.connectionHealth
    val healthMetrics = healthMonitor.healthStatus
    val currentQuality = adaptiveQuality.currentQuality
    
    // Active duration tracking
    private val _activeDuration = MutableStateFlow(0L)
    val activeDuration: StateFlow<Long> = _activeDuration.asStateFlow()
    private var sessionStartTime = 0L
    
    // Metrics reporting
    private var currentDeviceId: String = ""
    private var currentSessionId: String = ""
    private var metricsReportingJob: kotlinx.coroutines.Job? = null
    private var lastRelayServerUrl: String? = null
    private var lastAuthToken: String? = null
    private var tokenRefreshInProgress = false
    private var tokenRefreshAttempts = 0
    private val maxTokenRefreshAttempts = 3

    init {
        viewModelScope.launch {
            webSocketClient.authEvents.collect { event ->
                when (event) {
                    is RemoteControlWebSocketClient.AuthEvent.AuthFailed -> {
                        Log.e("RemoteControlVM", "⚠️ Auth failed from relay: ${event.reason}")
                        refreshRemoteTokenAndReconnect()
                    }
                }
            }
        }
    }

    private fun refreshRemoteTokenAndReconnect() {
        if (tokenRefreshInProgress) return
        if (tokenRefreshAttempts >= maxTokenRefreshAttempts) {
            Log.e("RemoteControlVM", "❌ Max token refresh attempts reached. Skipping refresh.")
            return
        }

        val relayUrl = lastRelayServerUrl ?: buildRelayUrlFallback()
        if (relayUrl.isNullOrBlank()) {
            Log.w("RemoteControlVM", "⚠️ Missing relay URL, cannot refresh token")
            return
        }

        tokenRefreshInProgress = true
        tokenRefreshAttempts += 1

        appScope.launch {
            try {
                Log.e("RemoteControlVM", "🔄 Refreshing remote token from CMS (attempt $tokenRefreshAttempts/$maxTokenRefreshAttempts)")

                val androidId = preferences.getString(AppConstant.DEVICE_ID, null)
                    ?: Settings.Secure.getString(appContext.contentResolver, Settings.Secure.ANDROID_ID)
                    ?: ""

                val deviceName = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}"
                val response = deviceApi.registerRemoteDevice(
                    baseUrl = BuildConfig.WEBVIEW_BASEURL,
                    deviceId = androidId,
                    deviceName = deviceName,
                    appVersion = BuildConfig.VERSION_NAME
                )

                if (response?.success == true) {
                    val newRemoteId = response.data.remoteId.toString()
                    val newToken = response.data.token

                    preferences.edit().apply {
                        putString(AppConstant.DEVICE_ID, androidId)
                        putString(AppConstant.REMOTE_ID, newRemoteId)
                        putString(AppConstant.REMOTE_TOKEN, newToken)
                        apply()
                    }

                    currentDeviceId = newRemoteId
                    lastAuthToken = newToken

                    Log.e("RemoteControlVM", "✅ Token refreshed. Reconnecting with new token...")
                    webSocketClient.connect(
                        wsUrl = relayUrl,
                        token = newToken,
                        devId = newRemoteId
                    )
                } else {
                    Log.e("RemoteControlVM", "❌ Token refresh failed (null/unsuccessful response)")
                }
            } catch (e: Exception) {
                Log.e("RemoteControlVM", "❌ Token refresh error: ${e.message}", e)
            } finally {
                tokenRefreshInProgress = false
            }
        }
    }

    private fun buildRelayUrlFallback(): String {
        val baseUrl = BuildConfig.WEBVIEW_BASEURL.takeIf { it.isNotBlank() } ?: "https://kiosk.mugshot.dev"
        return baseUrl.replace("https://", "wss://").replace("http://", "ws://") + "/remote-control-ws"
    }

    /**
     * Start remote control session with adaptive quality monitoring
     */
    fun startRemoteControl(
        context: Context,
        deviceId: String,
        authToken: String,
        relayServerUrl: String,
        backendApiUrl: String? = null
    ) {
        viewModelScope.launch {
            try {
                Log.e("RemoteControlVM", "🚀🚀🚀 Starting remote control - deviceId: $deviceId, serverUrl: $relayServerUrl 🚀🚀🚀")
                _remoteControlState.value = RemoteControlState.Starting
                _connectionStatus.value = ConnectionStatus.Connecting
                sessionStartTime = System.currentTimeMillis()
                
                // Store device info for metrics
                currentDeviceId = deviceId
                currentSessionId = java.util.UUID.randomUUID().toString()
                lastRelayServerUrl = relayServerUrl
                lastAuthToken = authToken
                
                // Initialize metrics reporter if backend API provided
                if (backendApiUrl != null) {
                    metricsReporter.initialize(backendApiUrl)
                    Log.d("RemoteControlVM", "📊 Metrics reporter initialized - $backendApiUrl")
                    
                    // Send session start event
                    metricsReporter.startSession(
                        deviceId = deviceId,
                        timestamp = sessionStartTime,
                        sessionId = currentSessionId
                    )
                }
                
                // Start tracking active duration
                trackActiveDuration()
                
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
                    Log.d("RemoteControlVM", "✅ WebSocket connected successfully")
                    _connectionStatus.value = ConnectionStatus.Connected
                    _remoteControlState.value = RemoteControlState.Active
                    
                    // Health monitoring automatically started in healthMonitor
                    Log.d("RemoteControlVM", "📊 Health monitoring active")
                    
                    // Start metrics reporting background task
                    if (backendApiUrl != null) {
                        startMetricsReporting()
                    }
                } else {
                    Log.e("RemoteControlVM", "❌ Failed to connect. State: ${webSocketClient.connectionState.value}")
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
     * Track active session duration
     */
    private fun trackActiveDuration() {
        viewModelScope.launch {
            while (_remoteControlState.value == RemoteControlState.Active) {
                val duration = System.currentTimeMillis() - sessionStartTime
                _activeDuration.value = duration
                delay(1000) // Update every second
            }
        }
    }
    
    /**
     * Stop remote control session
     */
    fun stopRemoteControl() {
        Log.d("RemoteControlVM", "Stopping remote control session")
        
        // Stop metrics reporting
        metricsReportingJob?.cancel()
        metricsReportingJob = null
        
        // Send session end event
        viewModelScope.launch {
            val durationMs = System.currentTimeMillis() - sessionStartTime
            val framesSent = healthMonitor.healthStatus.value.totalFramesSent.toInt()
            val framesDropped = healthMonitor.healthStatus.value.droppedFrames.toInt()
            val avgLatency = calculateAverageLatency()
            
            metricsReporter.endSession(
                deviceId = currentDeviceId,
                sessionId = currentSessionId,
                durationMs = durationMs,
                framesSent = framesSent,
                framesDropped = framesDropped,
                avgLatency = avgLatency
            )
            
            Log.d("RemoteControlVM", "✅ Session ended - Duration: ${durationMs}ms")
        }
        
        _remoteControlState.value = RemoteControlState.Idle
        _connectionStatus.value = ConnectionStatus.Disconnected
        _activeDuration.value = 0L
        sessionStartTime = 0L
        
        // Clean up
        healthMonitor.stopMonitoring()
        webSocketClient.disconnect()
    }
    
    /**
     * Start background task to send metrics every 5 seconds
     */
    private fun startMetricsReporting() {
        metricsReportingJob?.cancel()
        
        metricsReportingJob = viewModelScope.launch {
            Log.d("RemoteControlVM", "📊 Starting metrics reporting task (every 5 seconds)")
            
            while (isActive && _remoteControlState.value == RemoteControlState.Active) {
                try {
                    val health = healthMonitor.healthStatus.value
                    val quality = adaptiveQuality.currentQuality.value
                    val connectionHealth = healthMonitor.connectionHealth.value
                    
                    metricsReporter.sendHealthReport(
                        deviceId = currentDeviceId,
                        timestamp = System.currentTimeMillis(),
                        latency = health.lastLatency,
                        avgLatency = calculateAverageLatency(),
                        throughput = (calculateCurrentThroughput() / 1_000_000), // bps to Mbps
                        avgThroughput = (health.estimatedThroughput / 1_000_000), // bps to Mbps
                        jitter = health.jitter,
                        frameDropRate = calculateFrameDropRate(),
                        droppedFrames = health.droppedFrames.toInt(),
                        totalFrames = health.totalFramesSent.toInt(),
                        qualityLevel = quality.name,
                        fps = quality.fps,
                        resolution = getScreenResolution(),
                        connectionHealth = connectionHealth.name
                    )
                    
                    // Wait 5 seconds before next report
                    delay(5000)
                    
                } catch (e: Exception) {
                    Log.e("RemoteControlVM", "Error in metrics reporting task", e)
                    delay(5000)  // Continue even on error
                }
            }
            
            Log.d("RemoteControlVM", "📊 Metrics reporting task stopped")
        }
    }
    
    /**
     * Calculate average latency from health monitor
     */
    private fun calculateAverageLatency(): Long {
        // This could be enhanced to maintain rolling average
        // For now, return last measured latency
        return healthMonitor.healthStatus.value.lastLatency
    }
    
    /**
     * Calculate current throughput
     */
    private fun calculateCurrentThroughput(): Long {
        return healthMonitor.healthStatus.value.estimatedThroughput
    }
    
    /**
     * Calculate frame drop rate
     */
    private fun calculateFrameDropRate(): Double {
        val health = healthMonitor.healthStatus.value
        val totalFrames = health.totalFramesSent
        if (totalFrames == 0L) return 0.0
        
        return health.droppedFrames.toDouble() / totalFrames
    }
    
    /**
     * Get current screen resolution
     */
    private fun getScreenResolution(): String {
        // This should be retrieved from ScreenCaptureService
        // For now, return placeholder - update with actual implementation
        return "720x1280"
    }
    
    /**
     * Get detailed diagnostics report
     */
    fun getDiagnosticsReport(): String {
        val wsReport = webSocketClient.getMetricsReport()
        val healthReport = healthMonitor.getDiagnosticsReport()
        val qualityReport = adaptiveQuality.getStatusReport()
        
        return buildString {
            append("=== Remote Control Diagnostics ===\n\n")
            append(wsReport).append("\n\n")
            append(healthReport).append("\n\n")
            append(qualityReport)
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

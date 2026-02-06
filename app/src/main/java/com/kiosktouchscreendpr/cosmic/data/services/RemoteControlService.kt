package com.kiosktouchscreendpr.cosmic.data.services

import android.app.Service
import android.content.Intent
import android.content.SharedPreferences
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.kiosktouchscreendpr.cosmic.BuildConfig
import com.kiosktouchscreendpr.cosmic.core.constant.AppConstant
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import javax.inject.Inject

/**
 * Background Service for maintaining WebSocket connection
 * Independent from UI lifecycle - survives screen navigation and activity rotation
 * 
 * Replaces ViewModel-based connection management which was getting cancelled on navigation.
 */
@AndroidEntryPoint
class RemoteControlService : Service() {

    companion object {
        private const val TAG = "RemoteControlService"
    }

    @Inject
    lateinit var webSocketClient: RemoteControlWebSocketClient

    @Inject
    lateinit var preferences: SharedPreferences
    
    @Inject
    lateinit var metricsReporter: MetricsReporter

    // Service scope - survives UI lifecycle
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Metrics reporting job
    private var metricsReportingJob: Job? = null

    // Binder for local communication
    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): RemoteControlService = this@RemoteControlService
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "🟢 RemoteControlService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "🟢 RemoteControlService started")

        // Start connection if not already active
        if (webSocketClient.connectionState.value != RemoteControlWebSocketClient.ConnectionState.CONNECTED &&
            webSocketClient.connectionState.value != RemoteControlWebSocketClient.ConnectionState.CONNECTING) {
            startConnection()
        }
        
        // Initialize metrics reporter with API URL
        val apiUrl = BuildConfig.WEBVIEW_BASEURL
        metricsReporter.initialize(apiUrl)
        
        // Start sending health metrics every 5 seconds
        startHealthReporting()

        // Keep service running even after process restart
        return START_STICKY
    }

    /**
     * Start WebSocket connection
     */
    private fun startConnection() {
        Log.d(TAG, "🔌 Starting WebSocket connection from Service...")

        serviceScope.launch {
            try {
                val remoteId = preferences.getString(AppConstant.REMOTE_ID, null)
                val remoteToken = preferences.getString(AppConstant.REMOTE_TOKEN, null)

                if (remoteId.isNullOrEmpty() || remoteToken.isNullOrEmpty()) {
                    Log.w(TAG, "⚠️ Cannot start remote control: credentials missing")
                    return@launch
                }

                val baseUrl = BuildConfig.WEBVIEW_BASEURL
                val wsUrl = baseUrl.replace("http://", "ws://")
                    .replace("https://", "wss://") + "/remote-control-ws"

                Log.d(TAG, "📡 Connecting to: $wsUrl")
                webSocketClient.connect(wsUrl, remoteToken, remoteId)

                Log.d(TAG, "✅ WebSocket connection initiated from Service")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to start connection", e)
            }
        }
    }
    
    /**
     * Start reporting health metrics to backend
     * Sends data every 5 seconds during remote control session
     */
    private fun startHealthReporting() {
        Log.d(TAG, "📊 Starting health metrics reporting...")
        
        metricsReportingJob?.cancel()
        metricsReportingJob = serviceScope.launch {
            while (isActive) {
                try {
                    val remoteId = preferences.getString(AppConstant.REMOTE_ID, "") ?: ""
                    
                    if (remoteId.isNotEmpty()) {
                        metricsReporter.sendHealthReport(
                            deviceId = remoteId,
                            timestamp = System.currentTimeMillis(),
                            latency = 45L,                    // TODO: from ConnectionHealthMonitor
                            avgLatency = 42L,
                            throughput = 5200000L,            // TODO: from ConnectionHealthMonitor
                            avgThroughput = 5000000L,
                            jitter = 8L,
                            frameDropRate = 0.005,
                            droppedFrames = 5,
                            totalFrames = 1000,
                            qualityLevel = "HIGH",            // TODO: from AdaptiveQualityController
                            fps = 30,
                            resolution = "1920x1080",         // TODO: from ScreenCaptureService
                            connectionHealth = "HEALTHY"      // TODO: from ConnectionHealthMonitor
                        )
                    }
                    
                    delay(5000)  // Wait 5 seconds
                } catch (e: Exception) {
                    Log.e(TAG, "Error reporting metrics: ${e.message}")
                    delay(5000)
                }
            }
        }
    }

    /**
     * Stop WebSocket connection (graceful shutdown)
     */
    fun stopConnection() {
        Log.d(TAG, "🔌 Stopping WebSocket connection...")
        serviceScope.launch {
            webSocketClient.disconnect()
            Log.d(TAG, "✅ WebSocket disconnected")
        }
    }

    override fun onBind(intent: Intent?): IBinder {
        Log.d(TAG, "🔗 Service bound")
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "🔴 RemoteControlService destroyed")

        // Cleanup
        metricsReportingJob?.cancel()
        stopConnection()
        serviceScope.cancel()
    }
}

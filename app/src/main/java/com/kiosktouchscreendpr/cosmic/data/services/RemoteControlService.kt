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

    // Service scope - survives UI lifecycle
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

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
        stopConnection()
        serviceScope.cancel()
    }
}

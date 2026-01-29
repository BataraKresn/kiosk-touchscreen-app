package com.kiosktouchscreendpr.cosmic.data.services

import android.util.Log
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.json.JSONObject
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * WebSocket Client for Remote Control
 * 
 * Features:
 * - Maintains persistent connection with relay server
 * - Sends video frames from ScreenCaptureService
 * - Receives input commands and routes to InputInjectionService
 * - Auto-reconnection with exponential backoff
 * - Heartbeat mechanism to detect connection health
 * 
 * Architecture:
 * ScreenCaptureService → frameCallback → WebSocket → Relay Server → CMS Viewer
 * CMS Viewer → WebSocket → Relay Server → WebSocket → InputInjectionService
 * 
 * @author Cosmic Development Team
 * @version 1.0.0 (POC)
 */
@Singleton
class RemoteControlWebSocketClient @Inject constructor(
    private val httpClient: HttpClient
) {

    companion object {
        private const val TAG = "RemoteControlWS"
        
        // Reconnection settings
        private const val INITIAL_RECONNECT_DELAY_MS = 1000L
        private const val MAX_RECONNECT_DELAY_MS = 30000L
        private const val RECONNECT_BACKOFF_MULTIPLIER = 2.0
        
        // Heartbeat settings
        private const val HEARTBEAT_INTERVAL_MS = 15000L // 15 seconds
        private const val HEARTBEAT_TIMEOUT_MS = 45000L  // 45 seconds
        
        // Frame queue settings
        private const val MAX_QUEUED_FRAMES = 5 // Drop frames if queue is full
    }

    // Connection state
    private var session: WebSocketSession? = null
    private var isConnected = false
    private var reconnectDelay = INITIAL_RECONNECT_DELAY_MS
    
    // Coroutine scope
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Jobs
    private var connectionJob: Job? = null
    private var heartbeatJob: Job? = null
    private var frameProcessingJob: Job? = null
    
    // Frame queue (buffered channel)
    private val frameQueue = MutableSharedFlow<ByteArray>(
        replay = 0,
        extraBufferCapacity = MAX_QUEUED_FRAMES,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    
    // Connection state flow
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
    
    // Last heartbeat response time
    private var lastHeartbeatResponse = 0L
    
    // Device info
    private var deviceToken: String? = null
    private var deviceId: String? = null

    enum class ConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        RECONNECTING,
        ERROR
    }

    /**
     * Connect to relay server
     * 
     * @param wsUrl WebSocket URL (e.g., "wss://kiosk.example.com/remote-control")
     * @param token Device token for authentication
     * @param devId Device ID
     */
    fun connect(wsUrl: String, token: String, devId: String) {
        deviceToken = token
        deviceId = devId
        
        Log.d(TAG, "Connecting to relay server: $wsUrl")
        _connectionState.value = ConnectionState.CONNECTING
        
        connectionJob?.cancel()
        connectionJob = scope.launch {
            while (isActive) {
                try {
                    connectInternal(wsUrl)
                } catch (e: Exception) {
                    Log.e(TAG, "Connection error: ${e.message}", e)
                    _connectionState.value = ConnectionState.ERROR
                    
                    // Exponential backoff
                    delay(reconnectDelay)
                    reconnectDelay = (reconnectDelay * RECONNECT_BACKOFF_MULTIPLIER)
                        .toLong()
                        .coerceAtMost(MAX_RECONNECT_DELAY_MS)
                    
                    _connectionState.value = ConnectionState.RECONNECTING
                    Log.d(TAG, "Reconnecting in ${reconnectDelay}ms...")
                }
            }
        }
        
        // Start frame processing
        startFrameProcessing()
    }

    /**
     * Internal connection logic
     */
    private suspend fun connectInternal(wsUrl: String) {
        httpClient.webSocket(urlString = wsUrl) {
            session = this
            isConnected = true
            reconnectDelay = INITIAL_RECONNECT_DELAY_MS // Reset backoff
            _connectionState.value = ConnectionState.CONNECTED
            
            Log.d(TAG, "WebSocket connected")
            
            // Send authentication message
            sendAuthenticationMessage()
            
            // Start heartbeat
            startHeartbeat()
            
            // Handle incoming messages
            try {
                for (frame in incoming) {
                    when (frame) {
                        is Frame.Text -> {
                            val message = frame.readText()
                            handleIncomingMessage(message)
                        }
                        
                        is Frame.Binary -> {
                            // Binary frames not expected in this protocol
                            Log.w(TAG, "Received unexpected binary frame")
                        }
                        
                        is Frame.Close -> {
                            Log.d(TAG, "WebSocket closed by server")
                            break
                        }
                        
                        else -> {
                            // Other frame types (Ping, Pong handled automatically)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error reading incoming frames", e)
            } finally {
                cleanup()
            }
        }
    }

    /**
     * Send authentication message on connection
     */
    private suspend fun sendAuthenticationMessage() {
        try {
            val authMessage = JSONObject().apply {
                put("type", "auth")
                put("role", "device")
                put("deviceId", deviceId)
                put("token", deviceToken)
                put("deviceName", android.os.Build.MODEL)
                put("androidVersion", android.os.Build.VERSION.RELEASE)
            }
            
            session?.send(Frame.Text(authMessage.toString()))
            Log.d(TAG, "Authentication message sent")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send authentication", e)
        }
    }

    /**
     * Handle incoming message from server
     */
    private fun handleIncomingMessage(message: String) {
        try {
            if (message == "pong") {
                // Heartbeat response
                lastHeartbeatResponse = System.currentTimeMillis()
                Log.v(TAG, "Heartbeat pong received")
                return
            }
            
            val json = JSONObject(message)
            val type = json.getString("type")
            
            when (type) {
                "auth_success" -> {
                    Log.d(TAG, "Authentication successful")
                }
                
                "auth_failed" -> {
                    val reason = json.optString("reason", "Unknown")
                    Log.e(TAG, "Authentication failed: $reason")
                    disconnect()
                }
                
                "input_command" -> {
                    // Route to InputInjectionService
                    val command = json.getJSONObject("command")
                    InputInjectionService.getInstance()?.processInputCommand(command.toString())
                }
                
                "control_command" -> {
                    // System-level commands (screenshot, restart service, etc.)
                    handleControlCommand(json)
                }
                
                else -> {
                    Log.w(TAG, "Unknown message type: $type")
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error handling message", e)
        }
    }

    /**
     * Handle control commands from server
     */
    private fun handleControlCommand(json: JSONObject) {
        try {
            val action = json.getString("action")
            
            when (action) {
                "stop_capture" -> {
                    Log.d(TAG, "Stop capture command received")
                    // Signal to stop ScreenCaptureService
                    // Implementation depends on your app architecture
                }
                
                "adjust_quality" -> {
                    val quality = json.getInt("quality")
                    Log.d(TAG, "Adjust quality command: $quality")
                    // Signal ScreenCaptureService to adjust JPEG quality
                }
                
                "adjust_fps" -> {
                    val fps = json.getInt("fps")
                    Log.d(TAG, "Adjust FPS command: $fps")
                    // Signal ScreenCaptureService to adjust frame rate
                }
                
                else -> {
                    Log.w(TAG, "Unknown control action: $action")
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error handling control command", e)
        }
    }

    /**
     * Start heartbeat mechanism
     */
    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        lastHeartbeatResponse = System.currentTimeMillis()
        
        heartbeatJob = scope.launch {
            while (isActive && isConnected) {
                try {
                    // Send heartbeat
                    session?.send(Frame.Text("ping"))
                    Log.v(TAG, "Heartbeat ping sent")
                    
                    // Check for timeout
                    val timeSinceLastResponse = System.currentTimeMillis() - lastHeartbeatResponse
                    if (timeSinceLastResponse > HEARTBEAT_TIMEOUT_MS) {
                        Log.w(TAG, "Heartbeat timeout, reconnecting...")
                        disconnect()
                        break
                    }
                    
                    delay(HEARTBEAT_INTERVAL_MS)
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Heartbeat error", e)
                    break
                }
            }
        }
    }

    /**
     * Start frame processing job
     */
    private fun startFrameProcessing() {
        frameProcessingJob?.cancel()
        
        frameProcessingJob = scope.launch {
            frameQueue.collect { frameBytes ->
                try {
                    if (isConnected && session != null) {
                        // Encode frame as base64 for JSON transmission
                        // Note: For production, use binary frames or dedicated protocol
                        val base64Frame = Base64.getEncoder().encodeToString(frameBytes)
                        
                        val frameMessage = JSONObject().apply {
                            put("type", "frame")
                            put("format", "jpeg")
                            put("data", base64Frame)
                            put("timestamp", System.currentTimeMillis())
                        }
                        
                        session?.send(Frame.Text(frameMessage.toString()))
                        
                        // Log stats (remove in production or use verbose logging)
                        Log.v(TAG, "Frame sent: ${frameBytes.size / 1024}KB")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error sending frame", e)
                }
            }
        }
    }

    /**
     * Queue a frame for transmission
     * Called by ScreenCaptureService callback
     */
    fun queueFrame(frameBytes: ByteArray) {
        val emitted = frameQueue.tryEmit(frameBytes)
        if (!emitted) {
            Log.v(TAG, "Frame queue full, frame dropped")
        }
    }

    /**
     * Disconnect from server
     */
    fun disconnect() {
        Log.d(TAG, "Disconnecting from relay server")
        
        connectionJob?.cancel()
        cleanup()
    }

    /**
     * Cleanup resources
     */
    private fun cleanup() {
        heartbeatJob?.cancel()
        session = null
        isConnected = false
        _connectionState.value = ConnectionState.DISCONNECTED
        
        Log.d(TAG, "WebSocket connection cleaned up")
    }

    /**
     * Shutdown client completely
     */
    fun shutdown() {
        Log.d(TAG, "Shutting down RemoteControlWebSocketClient")
        
        disconnect()
        frameProcessingJob?.cancel()
        scope.cancel()
    }
}

/*
 * INTEGRATION EXAMPLE
 * 
 * In your MainActivity or RemoteControlViewModel:
 * 
 * @Inject lateinit var wsClient: RemoteControlWebSocketClient
 * @Inject lateinit var screenCaptureService: ScreenCaptureService
 * 
 * fun startRemoteControl() {
 *     // 1. Start WebSocket connection
 *     wsClient.connect(
 *         wsUrl = "wss://kiosk.mugshot.dev/remote-control",
 *         token = deviceToken,
 *         devId = deviceId
 *     )
 *     
 *     // 2. Start screen capture
 *     val intent = Intent(context, ScreenCaptureService::class.java)
 *     intent.putExtra("resultCode", mediaProjectionResultCode)
 *     intent.putExtra("data", mediaProjectionData)
 *     startForegroundService(intent)
 *     
 *     // 3. Set frame callback
 *     screenCaptureService.setFrameCallback { frameBytes ->
 *         wsClient.queueFrame(frameBytes)
 *     }
 *     
 *     // 4. Observe connection state
 *     lifecycleScope.launch {
 *         wsClient.connectionState.collect { state ->
 *             when (state) {
 *                 ConnectionState.CONNECTED -> {
 *                     // Update UI
 *                 }
 *                 ConnectionState.ERROR -> {
 *                     // Show error
 *                 }
 *                 else -> {}
 *             }
 *         }
 *     }
 * }
 */

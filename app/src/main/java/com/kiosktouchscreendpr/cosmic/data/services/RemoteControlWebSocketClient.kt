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
    private val httpClient: HttpClient,
    private val adaptiveQuality: AdaptiveQualityController,
    private val healthMonitor: ConnectionHealthMonitor
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
    private var isAuthenticated = false
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
        extraBufferCapacity = MAX_QUEUED_FRAMES
    )
    
    // Connection state flow
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
    
    // Last heartbeat response time
    private var lastHeartbeatResponse = 0L
    private var lastPingTime = 0L
    
    // Device info
    private var deviceToken: String? = null
    private var deviceId: String? = null
    
    // Frame tracking for intelligent dropping
    private var consecutiveDrops = 0
    private var totalFramesSent = 0L

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
        
        Log.e(TAG, "🌐🌐🌐 Connecting to relay server: $wsUrl 🌐🌐🌐")
        Log.e(TAG, "📱 DeviceID: $devId, Token: $token")
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
            Log.d(TAG, "Sending auth - deviceId: $deviceId, token: $deviceToken")
            val authMessage = JSONObject().apply {
                put("type", "authenticate")
                put("role", "device")
                put("deviceId", deviceId)
                put("token", deviceToken)
                put("deviceName", android.os.Build.MODEL)
                put("androidVersion", android.os.Build.VERSION.RELEASE)
            }
            
            session?.send(Frame.Text(authMessage.toString()))
            Log.d(TAG, "Authentication message sent: ${authMessage.toString()}")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send authentication", e)
        }
    }

    /**
     * Handle incoming message from server with health monitoring
     */
    private fun handleIncomingMessage(message: String) {
        try {
            // Handle plain text pong (backward compatibility)
            if (message == "pong") {
                handlePongResponse()
                return
            }
            
            val json = JSONObject(message)
            val type = json.getString("type")
            
            when (type) {
                "pong" -> {
                    // Heartbeat response (JSON format)
                    handlePongResponse()
                }
                
                "authenticated", "auth_success" -> {
                    Log.d(TAG, "✅ Authentication successful")
                    isAuthenticated = true
                    startHeartbeat()
                    healthMonitor.startMonitoring()
                }
                
                "error", "auth_failed" -> {
                    val messageText = json.optString("message", json.optString("reason", "Unknown error"))
                    Log.e(TAG, "❌ Authentication failed: $messageText")
                    isAuthenticated = false
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
     * Handle pong response - measure latency and update quality
     */
    private fun handlePongResponse() {
        lastHeartbeatResponse = System.currentTimeMillis()
        
        if (lastPingTime > 0) {
            val latency = lastHeartbeatResponse - lastPingTime
            Log.v(TAG, "📊 Latency measurement: ${latency}ms")
            
            // Update health monitor
            healthMonitor.handlePong()
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
     * Start heartbeat mechanism with health monitoring
     */
    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        lastHeartbeatResponse = System.currentTimeMillis()
        
        heartbeatJob = scope.launch {
            while (isActive && isConnected) {
                try {
                    // Send heartbeat (ping) and measure response time
                    lastPingTime = System.currentTimeMillis()
                    val heartbeatMessage = JSONObject().apply {
                        put("type", "ping")
                        put("timestamp", lastPingTime)
                    }.toString()
                    session?.send(Frame.Text(heartbeatMessage))
                    Log.v(TAG, "Heartbeat ping sent at ${lastPingTime}")
                    
                    // Check for timeout
                    val timeSinceLastResponse = System.currentTimeMillis() - lastHeartbeatResponse
                    if (timeSinceLastResponse > HEARTBEAT_TIMEOUT_MS) {
                        Log.w(TAG, "❌ Heartbeat timeout (${timeSinceLastResponse}ms), reconnecting...")
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
     * Start frame processing job with intelligent frame dropping
     * Supports both JPEG (Phase 1) and H.264 (Phase 2) encoded frames
     */
    private fun startFrameProcessing() {
        frameProcessingJob?.cancel()
        
        frameProcessingJob = scope.launch {
            frameQueue.collect { frameBytes ->
                try {
                    if (isConnected && isAuthenticated && session != null) {
                        // Intelligent frame dropping based on queue status
                        val shouldDrop = shouldDropFrame()
                        
                        if (shouldDrop) {
                            Log.v(TAG, "⏭️  Dropping frame - network congestion")
                            healthMonitor.recordFrameDrop()
                            consecutiveDrops++
                            return@collect
                        }
                        
                        // Encode frame as base64 for JSON transmission
                        // Supports both JPEG and H.264 encoded data transparently
                        val base64Frame = Base64.getEncoder().encodeToString(frameBytes)
                        
                        // Determine frame format
                        val frameFormat = if (isH264Frame(frameBytes)) "h264" else "jpeg"
                        
                        val frameMessage = JSONObject().apply {
                            put("type", "frame")
                            put("format", frameFormat)
                            put("data", base64Frame)
                            put("timestamp", System.currentTimeMillis())
                            // Optional: keyframe indicator for H.264
                            if (frameFormat == "h264") {
                                put("is_keyframe", isH264Keyframe(frameBytes))
                            }
                        }
                        
                        session?.send(Frame.Text(frameMessage.toString()))
                        
                        // Update metrics
                        totalFramesSent++
                        consecutiveDrops = 0  // Reset drop counter on successful send
                        healthMonitor.recordFrameSent(frameBytes.size)
                        
                        // Log stats
                        Log.v(TAG, "📤 Frame sent ($frameFormat): ${frameBytes.size / 1024}KB (Total: $totalFramesSent)")
                    } else {
                        Log.v(TAG, "⏭️  Frame dropped - not connected (isConnected=$isConnected, isAuthenticated=$isAuthenticated)")
                        healthMonitor.recordFrameDrop()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error sending frame: ${e.message}", e)
                    healthMonitor.recordFrameDrop()
                }
            }
        }
    }
    
    /**
     * Detect if frame data is H.264 encoded
     * H.264 frames typically start with specific NAL unit bytes
     */
    private fun isH264Frame(frameData: ByteArray): Boolean {
        if (frameData.isEmpty()) return false
        
        // Check for H.264 NAL unit start code or AVCC format
        // H.264 frames often start with 00 00 00 01 (start code) or specific patterns
        // For simplicity, we check if data is small (H.264 is typically more compressed)
        // This is a heuristic - ideally we'd have metadata
        
        // In production, you should add a frame type indicator in the encoding layer
        return frameData.size < 50000  // H.264 frames are usually < 50KB for 720p
    }
    
    /**
     * Detect if H.264 frame is a keyframe (I-frame)
     * Keyframes start with NAL unit type 5
     */
    private fun isH264Keyframe(frameData: ByteArray): Boolean {
        if (frameData.size < 4) return false
        
        // Look for NAL unit start code
        if (frameData[0] == 0.toByte() && frameData[1] == 0.toByte() && 
            frameData[2] == 0.toByte() && frameData[3] == 1.toByte()) {
            // Check NAL unit type (bits 4-0 of next byte)
            val nalUnitType = frameData[4].toInt() and 0x1F
            return nalUnitType == 5  // NAL type 5 = IDR picture (keyframe)
        }
        
        return false
    }
    
    /**
     * Determine if current frame should be dropped based on network condition
     * Implements intelligent frame dropping to maintain smooth playback
     */
    private fun shouldDropFrame(): Boolean {
        // Check queue status
        if (frameQueue.replayCache.size > RemoteControlConfig.FrameBufferingConfig.MAX_QUEUED_FRAMES / 2) {
            Log.v(TAG, "Frame drop: Queue depth high (${frameQueue.replayCache.size})")
            return true
        }
        
        // Check consecutive drops (allow recovery)
        if (consecutiveDrops > 5) {
            return false  // Stop dropping after 5 consecutive, let sender catch up
        }
        
        // Check connection health (more aggressive dropping on poor connection)
        val health = healthMonitor.connectionHealth.value
        return health == ConnectionHealth.CRITICAL || health == ConnectionHealth.UNHEALTHY
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
        isAuthenticated = false
        _connectionState.value = ConnectionState.DISCONNECTED
        
        Log.d(TAG, "WebSocket connection cleaned up")
    }

    /**
     * Shutdown client completely
     */
    fun shutdown() {
        Log.d(TAG, "Shutting down RemoteControlWebSocketClient")
        Log.d(TAG, "📊 Final stats - Total frames sent: $totalFramesSent")
        
        healthMonitor.stopMonitoring()
        disconnect()
        frameProcessingJob?.cancel()
        scope.cancel()
    }
    
    /**
     * Get current metrics (for UI display)
     */
    fun getMetricsReport(): String {
        val health = healthMonitor.healthStatus.value
        val connectionHealth = healthMonitor.connectionHealth.value
        val quality = adaptiveQuality.currentQuality.value
        
        return buildString {
            append("=== Remote Control Metrics ===\n")
            append("Connection: ${_connectionState.value}\n")
            append("Health: $connectionHealth\n")
            append("Latency: ${health.lastLatency}ms\n")
            append("Throughput: ${health.estimatedThroughput / 1000}Kbps\n")
            append("Quality: ${quality.label}\n")
            append("Frames sent: $totalFramesSent\n")
            append("Frames dropped: ${health.droppedFrames}")
        }
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

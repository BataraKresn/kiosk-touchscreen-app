package com.kiosktouchscreendpr.cosmic.data.services

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Connection Health Monitor for Remote Control
 * 
 * Monitors WebSocket connection health and triggers adaptive quality adjustments
 * 
 * Features:
 * - Periodic health checks with ping/pong
 * - Real-time latency measurement
 * - Throughput estimation
 * - Automatic quality adjustment based on network condition
 * - Graceful degradation on poor network
 * - Automatic recovery on improved network
 * 
 * @author Cosmic Development Team
 * @version 1.0.0
 */
@Singleton
class ConnectionHealthMonitor @Inject constructor(
    private val adaptiveQuality: AdaptiveQualityController
) {
    
    companion object {
        private const val TAG = "HealthMonitor"
    }
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Health status
    private val _healthStatus = MutableStateFlow(HealthStatus())
    val healthStatus: StateFlow<HealthStatus> = _healthStatus.asStateFlow()
    
    // Connection state
    private val _connectionHealth = MutableStateFlow<ConnectionHealth>(ConnectionHealth.HEALTHY)
    val connectionHealth: StateFlow<ConnectionHealth> = _connectionHealth.asStateFlow()
    
    // Monitoring active flag
    private var isMonitoring = false
    private var lastPingTime = 0L
    private var lastPongTime = 0L
    
    // Frame transmission tracking
    private var lastFrameTime = 0L
    private var frameCount = 0L
    private var lastFrameCountReset = System.currentTimeMillis()
    
    // Throughput estimation
    private var totalBytesSent = 0L
    private var totalBytesReceived = 0L
    private var lastThroughputCheck = System.currentTimeMillis()
    
    // Jitter calculation (latency variance tracking)
    private val latencyHistory = mutableListOf<Long>()
    private val jitterWindowSize = 10  // Keep last 10 latency measurements
    
    /**
     * Start health monitoring
     */
    fun startMonitoring() {
        if (isMonitoring) {
            Log.d(TAG, "Health monitoring already running")
            return
        }
        
        isMonitoring = true
        Log.d(TAG, "Starting health monitoring")
        
        // Start periodic health checks
        scope.launch {
            while (isActive && isMonitoring) {
                try {
                    performHealthCheck()
                    delay(RemoteControlConfig.MonitoringConfig.HEALTH_CHECK_INTERVAL)
                } catch (e: Exception) {
                    Log.e(TAG, "Error in health check", e)
                }
            }
        }
    }
    
    /**
     * Stop health monitoring
     */
    fun stopMonitoring() {
        isMonitoring = false
        Log.d(TAG, "Health monitoring stopped")
    }
    
    /**
     * Send ping message for latency measurement
     */
    suspend fun sendPing(): Long {
        lastPingTime = System.currentTimeMillis()
        return lastPingTime
    }
    
    /**
     * Handle pong response from server
     */
    fun handlePong() {
        val now = System.currentTimeMillis()
        lastPongTime = now
        
        if (lastPingTime > 0) {
            val latency = now - lastPingTime
            
            // Track latency for jitter calculation
            latencyHistory.add(latency)
            if (latencyHistory.size > jitterWindowSize) {
                latencyHistory.removeAt(0)  // Keep only last 10 measurements
            }
            
            val jitter = calculateJitter()
            Log.v(TAG, "Pong received - Latency: ${latency}ms, Jitter: ${jitter}ms")
            
            // Update adaptive quality based on latency
            adaptiveQuality.updateQualityBasedOnLatency(latency)
            
            // Update health status with jitter
            val currentStatus = _healthStatus.value
            _healthStatus.value = currentStatus.copy(
                lastLatency = latency,
                jitter = jitter,
                consecutiveHealthyChecks = currentStatus.consecutiveHealthyChecks + 1,
                lastHealthCheckTime = now
            )
            
            // Update connection health
            updateConnectionHealth(latency)
        }
    }
    
    /**
     * Record frame sent
     */
    fun recordFrameSent(frameSize: Int) {
        val now = System.currentTimeMillis()
        lastFrameTime = now
        frameCount++
        totalBytesSent += frameSize
        
        adaptiveQuality.recordFrameSent()
        
        // Estimate throughput every second
        val timeSinceLastCheck = now - lastThroughputCheck
        if (timeSinceLastCheck >= 1000) {
            val throughput = (totalBytesSent * 8) / (timeSinceLastCheck / 1000) // bits per second
            _healthStatus.value = _healthStatus.value.copy(
                estimatedThroughput = throughput
            )
            lastThroughputCheck = now
            totalBytesSent = 0
            
            Log.v(TAG, "Throughput: ${throughput / 1000}Kbps, Frame rate: ${frameCount}fps")
            frameCount = 0
        }
    }
    
    /**
     * Record frame dropped
     */
    fun recordFrameDrop() {
        adaptiveQuality.recordFrameDrop()
        
        val currentStatus = _healthStatus.value
        _healthStatus.value = currentStatus.copy(
            droppedFrames = currentStatus.droppedFrames + 1
        )
    }
    
    /**
     * Perform periodic health check
     */
    private suspend fun performHealthCheck() {
        val now = System.currentTimeMillis()
        val timeSinceLastPong = now - lastPongTime
        
        Log.v(TAG, "📊 Health check - Time since last pong: ${timeSinceLastPong}ms")
        
        // Check if connection is stalled
        if (lastPongTime > 0 && timeSinceLastPong > RemoteControlConfig.MonitoringConfig.HEARTBEAT_TIMEOUT) {
            Log.e(TAG, "❌ CONNECTION STALLED - No pong received for ${timeSinceLastPong}ms")
            
            val currentStatus = _healthStatus.value
            _healthStatus.value = currentStatus.copy(
                consecutiveHealthyChecks = 0,
                isStalled = true
            )
            
            _connectionHealth.value = ConnectionHealth.UNHEALTHY
        } else if (lastPongTime > 0) {
            // Calculate frame drop rate
            val totalFrames = _healthStatus.value.totalFramesSent
            val droppedFrames = _healthStatus.value.droppedFrames
            val frameDropRate = if (totalFrames > 0) {
                droppedFrames.toFloat() / totalFrames
            } else {
                0f
            }
            
            // Trigger quality update with all metrics
            adaptiveQuality.updateQualityBasedOnNetworkCondition(
                latency = _healthStatus.value.lastLatency,
                throughput = _healthStatus.value.estimatedThroughput,
                frameDropRate = frameDropRate
            )
        }
    }
    
    /**
     * Update connection health status
     */
    private fun updateConnectionHealth(latency: Long) {
        val health = when {
            latency > RemoteControlConfig.NetworkThresholds.LATENCY_CRITICAL -> {
                ConnectionHealth.CRITICAL
            }
            latency > RemoteControlConfig.NetworkThresholds.LATENCY_POOR -> {
                ConnectionHealth.UNHEALTHY
            }
            latency > RemoteControlConfig.NetworkThresholds.LATENCY_ACCEPTABLE -> {
                ConnectionHealth.DEGRADED
            }
            latency > RemoteControlConfig.NetworkThresholds.LATENCY_GOOD -> {
                ConnectionHealth.HEALTHY
            }
            else -> {
                ConnectionHealth.EXCELLENT
            }
        }
        
        if (_connectionHealth.value != health) {
            Log.d(TAG, "Connection health changed: ${_connectionHealth.value} → $health")
            _connectionHealth.value = health
        }
    }
    
    /**
     * Calculate jitter (latency standard deviation)
     * Jitter measures the variation in latency - indicates connection stability
     */
    fun calculateJitter(): Long {
        if (latencyHistory.size < 2) return 0
        
        val avg = latencyHistory.average()
        val variance = latencyHistory.map { (it - avg) * (it - avg) }.average()
        val stdDev = Math.sqrt(variance)
        
        return stdDev.toLong()  // Return as milliseconds
    }
    
    /**
     * Get detailed diagnostics report
     */
    fun getDiagnosticsReport(): String {
        val status = _healthStatus.value
        val quality = adaptiveQuality.currentQuality.value
        
        return buildString {
            append("=== Connection Health Report ===\n")
            append("Health: ${_connectionHealth.value}\n")
            append("Latency: ${status.lastLatency}ms\n")
            append("Throughput: ${status.estimatedThroughput / 1000}Kbps\n")
            append("Quality: ${quality.label}\n")
            append("FPS: ${quality.fps}\n")
            append("Dropped Frames: ${status.droppedFrames}/${status.totalFramesSent}\n")
            append("Stalled: ${status.isStalled}\n")
            append("Healthy Checks: ${status.consecutiveHealthyChecks}\n")
            append("Last Check: ${System.currentTimeMillis() - status.lastHealthCheckTime}ms ago")
        }
    }
}

/**
 * Connection health status
 */
data class HealthStatus(
    val lastLatency: Long = 0,                  // Last measured latency (ms)
    val jitter: Long = 0,                      // Latency standard deviation (ms) - indicates stability
    val estimatedThroughput: Long = 0,         // Estimated throughput (bps)
    val droppedFrames: Long = 0,               // Total dropped frames
    val totalFramesSent: Long = 0,             // Total frames sent
    val consecutiveHealthyChecks: Int = 0,     // Consecutive successful health checks
    val isStalled: Boolean = false,            // Connection is stalled
    val lastHealthCheckTime: Long = System.currentTimeMillis()
)

/**
 * Connection health enum
 */
enum class ConnectionHealth {
    EXCELLENT,    // < 50ms latency
    HEALTHY,      // 50-100ms latency
    DEGRADED,     // 100-200ms latency
    UNHEALTHY,    // 200-500ms latency
    CRITICAL      // > 500ms latency
}

package com.kiosktouchscreendpr.cosmic.data.services

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Adaptive Quality Controller for Remote Control
 * 
 * Dynamically adjusts video quality based on network conditions
 * - Monitors network latency and throughput
 * - Adjusts JPEG quality, H.264 bitrate, FPS, and resolution
 * - Implements intelligent frame dropping when network is congested
 * 
 * Features:
 * - Real-time quality level selection
 * - Smooth quality transitions
 * - Hysteresis to prevent rapid oscillation
 * - Network condition tracking
 * 
 * @author Cosmic Development Team
 * @version 1.0.0
 */
@Singleton
class AdaptiveQualityController @Inject constructor() {
    
    companion object {
        private const val TAG = "AdaptiveQuality"
        
        // Hysteresis settings - prevent rapid oscillation between quality levels
        private const val UPGRADE_THRESHOLD_MS = 3000L   // Need 3 seconds of good connection to upgrade
        private const val DOWNGRADE_THRESHOLD_MS = 1000L // Quick downgrade on poor connection
    }
    
    // Current quality level
    private val _currentQuality = MutableStateFlow(RemoteControlConfig.QualityLevel.HIGH)
    val currentQuality: StateFlow<RemoteControlConfig.QualityLevel> = _currentQuality.asStateFlow()
    
    // Network metrics
    private val _networkMetrics = MutableStateFlow(NetworkMetrics())
    val networkMetrics: StateFlow<NetworkMetrics> = _networkMetrics.asStateFlow()
    
    // Quality adjustment history (for hysteresis)
    private var lastQualityChangeTime = 0L
    private var lastQualityLevel = RemoteControlConfig.QualityLevel.HIGH
    
    // Metrics rolling average
    private val latencyHistory = mutableListOf<Long>()
    private val throughputHistory = mutableListOf<Long>()
    
    /**
     * Update quality based on measured network latency
     */
    fun updateQualityBasedOnLatency(latency: Long) {
        updateQualityBasedOnNetworkCondition(
            latency = latency,
            throughput = _networkMetrics.value.throughput,
            frameDropRate = _networkMetrics.value.frameDropRate
        )
    }
    
    /**
     * Update quality based on full network condition
     */
    fun updateQualityBasedOnNetworkCondition(
        latency: Long,
        throughput: Long,
        frameDropRate: Float = 0f
    ) {
        // Update history for averaging
        latencyHistory.add(latency)
        throughputHistory.add(throughput)
        
        // Keep only last N measurements
        if (latencyHistory.size > RemoteControlConfig.MonitoringConfig.METRICS_WINDOW_SIZE) {
            latencyHistory.removeAt(0)
        }
        if (throughputHistory.size > RemoteControlConfig.MonitoringConfig.METRICS_WINDOW_SIZE) {
            throughputHistory.removeAt(0)
        }
        
        // Calculate averages
        val avgLatency = latencyHistory.average().toLong()
        val avgThroughput = throughputHistory.average().toLong()
        
        // Add jitter estimation
        val jitter = if (latencyHistory.size > 1) {
            val mean = avgLatency
            val variance = latencyHistory.map { (it - mean) * (it - mean) }.average()
            kotlin.math.sqrt(variance).toLong()
        } else {
            0L
        }
        
        // Update metrics
        _networkMetrics.value = NetworkMetrics(
            latency = latency,
            averageLatency = avgLatency,
            throughput = throughput,
            averageThroughput = avgThroughput,
            frameDropRate = frameDropRate,
            jitter = jitter,
            timestamp = System.currentTimeMillis()
        )
        
        // Determine new quality level
        val newQuality = RemoteControlConfig.AdaptiveRules.getQualityLevel(
            latency = avgLatency,
            throughput = avgThroughput,
            frameDropRate = frameDropRate
        )
        
        // Apply hysteresis to prevent rapid quality switching
        val shouldChangeQuality = when {
            newQuality.ordinal < lastQualityLevel.ordinal -> {
                // Quality degradation - be aggressive
                val timeSinceLastChange = System.currentTimeMillis() - lastQualityChangeTime
                timeSinceLastChange > RemoteControlConfig.AdaptiveRules.getQualityLevel(
                    latency, throughput, frameDropRate
                ).let { 500L } // 500ms for degradation
            }
            newQuality.ordinal > lastQualityLevel.ordinal -> {
                // Quality upgrade - be conservative
                val timeSinceLastChange = System.currentTimeMillis() - lastQualityChangeTime
                timeSinceLastChange > UPGRADE_THRESHOLD_MS
            }
            else -> false
        }
        
        if (shouldChangeQuality) {
            setQualityLevel(newQuality)
            lastQualityChangeTime = System.currentTimeMillis()
            lastQualityLevel = newQuality
            
            Log.i(TAG, "🔄 Quality changed to: ${newQuality.label}")
            Log.d(TAG, "   Latency: ${avgLatency}ms, Throughput: ${avgThroughput / 1000}Kbps")
        }
    }
    
    /**
     * Manually set quality level
     */
    fun setQualityLevel(level: RemoteControlConfig.QualityLevel) {
        if (_currentQuality.value != level) {
            Log.d(TAG, "📊 Quality level changed: ${_currentQuality.value.label} → ${level.label}")
            _currentQuality.value = level
        }
    }
    
    /**
     * Get current FPS based on network condition
     */
    fun getAdaptiveFPS(): Int {
        val metrics = _networkMetrics.value
        return RemoteControlConfig.AdaptiveRules.getAdaptiveFPS(
            latency = metrics.averageLatency,
            throughput = metrics.averageThroughput
        )
    }
    
    /**
     * Get current JPEG quality (0-100)
     */
    fun getCurrentJPEGQuality(): Int {
        return _currentQuality.value.jpegQuality
    }
    
    /**
     * Get current H.264 bitrate (bits per second)
     */
    fun getCurrentH264Bitrate(): Int {
        return _currentQuality.value.h264Bitrate
    }
    
    /**
     * Get current resolution width
     */
    fun getCurrentResolutionWidth(): Int {
        return _currentQuality.value.resolutionWidth
    }
    
    /**
     * Get current resolution height
     */
    fun getCurrentResolutionHeight(): Int {
        return _currentQuality.value.resolutionHeight
    }
    
    /**
     * Get bitrate multiplier for adjustments
     */
    fun getBitrateMultiplier(): Float {
        return _currentQuality.value.bitrateMultiplier
    }
    
    /**
     * Record frame drop event
     */
    fun recordFrameDrop() {
        val metrics = _networkMetrics.value
        val newDropRate = if (metrics.totalFrames > 0) {
            (metrics.droppedFrames + 1).toFloat() / (metrics.totalFrames + 1)
        } else {
            0f
        }
        
        _networkMetrics.value = metrics.copy(
            droppedFrames = metrics.droppedFrames + 1,
            frameDropRate = newDropRate
        )
        
        // Trigger quality adjustment if drop rate exceeds threshold
        if (newDropRate > RemoteControlConfig.NetworkThresholds.FRAME_DROP_RATE_WARNING) {
            updateQualityBasedOnNetworkCondition(
                latency = metrics.averageLatency,
                throughput = metrics.averageThroughput,
                frameDropRate = newDropRate
            )
        }
    }
    
    /**
     * Record successful frame transmission
     */
    fun recordFrameSent() {
        val metrics = _networkMetrics.value
        _networkMetrics.value = metrics.copy(
            totalFrames = metrics.totalFrames + 1
        )
    }
    
    /**
     * Reset statistics
     */
    fun resetStatistics() {
        latencyHistory.clear()
        throughputHistory.clear()
        _networkMetrics.value = NetworkMetrics()
        Log.d(TAG, "Statistics reset")
    }
    
    /**
     * Get detailed status for UI display
     */
    fun getStatusReport(): String {
        val quality = _currentQuality.value
        val metrics = _networkMetrics.value
        val condition = RemoteControlConfig.getNetworkConditionLabel(
            metrics.averageLatency,
            metrics.averageThroughput
        )
        
        return buildString {
            append("Quality: ${quality.label}\n")
            append("Condition: $condition\n")
            append("Latency: ${metrics.averageLatency}ms\n")
            append("Throughput: ${metrics.averageThroughput / 1000}Kbps\n")
            append("FPS: ${quality.fps}\n")
            append("Bitrate: ${quality.h264Bitrate / 1000}Kbps\n")
            append("Resolution: ${quality.resolutionWidth}x${quality.resolutionHeight}\n")
            append("Frame Drop: ${(metrics.frameDropRate * 100).toInt()}%")
        }
    }
}

/**
 * Network metrics data class
 */
data class NetworkMetrics(
    val latency: Long = 0,                      // Current latency (ms)
    val averageLatency: Long = 0,               // Average latency (ms)
    val throughput: Long = 0,                   // Current throughput (bps)
    val averageThroughput: Long = 0,            // Average throughput (bps)
    val jitter: Long = 0,                       // Network jitter (ms)
    val frameDropRate: Float = 0f,              // Dropped frames ratio (0-1)
    val droppedFrames: Long = 0,                // Total dropped frames
    val totalFrames: Long = 0,                  // Total frames sent
    val timestamp: Long = System.currentTimeMillis()
)

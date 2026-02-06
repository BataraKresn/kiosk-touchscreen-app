package com.kiosktouchscreendpr.cosmic.data.services

import android.util.Log

/**
 * Configuration for Remote Control - Adaptive Quality Management
 * 
 * Tunable parameters for quality levels, bitrates, FPS ranges, and thresholds
 * 
 * @author Cosmic Development Team
 * @version 1.0.0
 */
object RemoteControlConfig {
    
    private const val TAG = "RemoteControlConfig"
    
    // ==================== Quality Presets ====================
    
    enum class QualityLevel(
        val label: String,
        val jpegQuality: Int,
        val h264Bitrate: Int,      // bits per second
        val fps: Int,
        val resolutionWidth: Int,
        val resolutionHeight: Int,
        val bitrateMultiplier: Float
    ) {
        LOW(
            label = "Low",
            jpegQuality = 40,
            h264Bitrate = 300_000,      // 300 Kbps
            fps = 10,
            resolutionWidth = 480,
            resolutionHeight = 854,
            bitrateMultiplier = 0.6f
        ),
        MEDIUM(
            label = "Medium",
            jpegQuality = 60,
            h264Bitrate = 500_000,      // 500 Kbps
            fps = 15,
            resolutionWidth = 640,
            resolutionHeight = 1136,
            bitrateMultiplier = 1.0f
        ),
        HIGH(
            label = "High",
            jpegQuality = 75,
            h264Bitrate = 1_000_000,    // 1 Mbps
            fps = 20,
            resolutionWidth = 720,
            resolutionHeight = 1280,
            bitrateMultiplier = 1.5f
        ),
        ULTRA(
            label = "Ultra",
            jpegQuality = 85,
            h264Bitrate = 2_000_000,    // 2 Mbps
            fps = 25,
            resolutionWidth = 1080,
            resolutionHeight = 1920,
            bitrateMultiplier = 2.0f
        )
    }
    
    // ==================== Network Thresholds ====================
    
    object NetworkThresholds {
        // Latency thresholds (milliseconds)
        const val LATENCY_EXCELLENT = 50       // < 50ms
        const val LATENCY_GOOD = 100           // 50-100ms
        const val LATENCY_ACCEPTABLE = 200     // 100-200ms
        const val LATENCY_POOR = 500           // 200-500ms
        const val LATENCY_CRITICAL = 1000      // > 500ms
        
        // Throughput thresholds (bits per second)
        const val THROUGHPUT_EXCELLENT = 10_000_000L    // > 10 Mbps
        const val THROUGHPUT_GOOD = 5_000_000L          // 5-10 Mbps
        const val THROUGHPUT_ACCEPTABLE = 2_000_000L    // 2-5 Mbps
        const val THROUGHPUT_POOR = 1_000_000L          // 1-2 Mbps
        const val THROUGHPUT_CRITICAL = 500_000L        // < 1 Mbps
        
        // Frame drop thresholds
        const val FRAME_DROP_RATE_ACCEPTABLE = 0.05f    // 5%
        const val FRAME_DROP_RATE_WARNING = 0.1f        // 10%
        const val FRAME_DROP_RATE_CRITICAL = 0.2f       // 20%
        
        // Jitter thresholds (milliseconds)
        const val JITTER_ACCEPTABLE = 50
        const val JITTER_WARNING = 100
        const val JITTER_CRITICAL = 200
    }
    
    // ==================== Adaptive Quality Rules ====================
    
    object AdaptiveRules {
        // Determine quality level based on network condition
        fun getQualityLevel(latency: Long, throughput: Long, frameDropRate: Float): QualityLevel {
            return when {
                // Critical conditions - use minimum quality
                latency > NetworkThresholds.LATENCY_CRITICAL ||
                throughput < NetworkThresholds.THROUGHPUT_CRITICAL ||
                frameDropRate > NetworkThresholds.FRAME_DROP_RATE_CRITICAL -> {
                    Log.w(TAG, "🔴 CRITICAL network condition detected - using LOW quality")
                    Log.w(TAG, "   Latency: ${latency}ms, Throughput: ${throughput} bps, Drop rate: ${frameDropRate}%")
                    QualityLevel.LOW
                }
                
                // Poor conditions
                latency > NetworkThresholds.LATENCY_POOR ||
                throughput < NetworkThresholds.THROUGHPUT_POOR ||
                frameDropRate > NetworkThresholds.FRAME_DROP_RATE_WARNING -> {
                    Log.w(TAG, "⚠️ POOR network condition - using LOW quality")
                    QualityLevel.LOW
                }
                
                // Acceptable conditions
                latency > NetworkThresholds.LATENCY_ACCEPTABLE ||
                throughput < NetworkThresholds.THROUGHPUT_ACCEPTABLE -> {
                    Log.w(TAG, "⚠️ ACCEPTABLE network condition - using MEDIUM quality")
                    QualityLevel.MEDIUM
                }
                
                // Good conditions
                latency > NetworkThresholds.LATENCY_GOOD ||
                throughput < NetworkThresholds.THROUGHPUT_GOOD -> {
                    Log.d(TAG, "✅ GOOD network condition - using HIGH quality")
                    QualityLevel.HIGH
                }
                
                // Excellent conditions
                else -> {
                    Log.d(TAG, "✅✅ EXCELLENT network condition - using ULTRA quality")
                    QualityLevel.ULTRA
                }
            }
        }
        
        // Determine FPS based on network condition
        fun getAdaptiveFPS(latency: Long, throughput: Long): Int {
            return when {
                latency > NetworkThresholds.LATENCY_CRITICAL -> 5
                latency > NetworkThresholds.LATENCY_POOR -> 10
                latency > NetworkThresholds.LATENCY_ACCEPTABLE -> 15
                latency > NetworkThresholds.LATENCY_GOOD -> 20
                else -> 25
            }
        }
    }
    
    // ==================== Connection Monitoring ====================
    
    object MonitoringConfig {
        // Health check interval (milliseconds)
        const val HEALTH_CHECK_INTERVAL = 5000L         // Every 5 seconds
        
        // Heartbeat settings
        const val HEARTBEAT_INTERVAL = 15000L           // Every 15 seconds
        const val HEARTBEAT_TIMEOUT = 45000L            // 45 seconds timeout
        
        // Watchdog settings
        const val FRAME_TIMEOUT = 5000L                 // 5 seconds without frames
        const val PROACTIVE_RESTART_INTERVAL = 45000L   // Restart every 45 seconds
        
        // Metrics window (for averaging)
        const val METRICS_WINDOW_SIZE = 100             // Keep last 100 measurements
        const val METRICS_REPORTING_INTERVAL = 10000L   // Report every 10 seconds
    }
    
    // ==================== Frame Buffering ====================
    
    object FrameBufferingConfig {
        const val MAX_QUEUED_FRAMES = 5
        const val KEYFRAME_PRIORITY = true              // Keep I-frames, drop P-frames
        const val AUTO_RESTART_ON_STALL = true
        const val STALL_DETECTION_TIMEOUT = 3000L       // 3 seconds
    }
    
    // ==================== H.264 Encoding ====================
    
    object H264Config {
        const val COLOR_FORMAT = 21                     // COLOR_FormatYUV420SemiPlanar
        const val I_FRAME_INTERVAL = 2                  // 2 seconds between keyframes
        const val BITRATE_MODE = 0                      // VBR (Variable Bitrate)
        const val PROFILE = 1                           // Baseline profile
        const val LEVEL = 13                            // Level 3.1
    }
    
    // ==================== Debug Settings ====================
    
    object DebugSettings {
        val ENABLE_DETAILED_LOGGING = true
        val ENABLE_METRICS_REPORTING = true
        val ENABLE_ADAPTIVE_QUALITY = true
        val ENABLE_FRAME_DROP_OPTIMIZATION = true
        val SIMULATE_POOR_NETWORK = false               // For testing
        val SIMULATED_LATENCY = 0L                      // Additional latency to simulate
        val SIMULATED_PACKET_LOSS = 0.0f                // 0-1.0 packet loss rate
    }
    
    // ==================== Utility Functions ====================
    
    fun getQualityLabel(level: QualityLevel): String {
        return "Quality: ${level.label} | FPS: ${level.fps} | Bitrate: ${level.h264Bitrate / 1000}Kbps"
    }
    
    fun getNetworkConditionLabel(latency: Long, throughput: Long): String {
        return when {
            latency > NetworkThresholds.LATENCY_CRITICAL ||
            throughput < NetworkThresholds.THROUGHPUT_CRITICAL -> "CRITICAL ⛔"
            latency > NetworkThresholds.LATENCY_POOR ||
            throughput < NetworkThresholds.THROUGHPUT_POOR -> "POOR ⚠️"
            latency > NetworkThresholds.LATENCY_ACCEPTABLE ||
            throughput < NetworkThresholds.THROUGHPUT_ACCEPTABLE -> "ACCEPTABLE ⚠️"
            latency > NetworkThresholds.LATENCY_GOOD ||
            throughput < NetworkThresholds.THROUGHPUT_GOOD -> "GOOD ✅"
            else -> "EXCELLENT ✅✅"
        }
    }
}

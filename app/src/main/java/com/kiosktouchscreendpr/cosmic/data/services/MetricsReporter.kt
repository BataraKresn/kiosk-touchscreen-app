package com.kiosktouchscreendpr.cosmic.data.services

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Metrics Reporter for Backend Integration
 * 
 * Sends health metrics and session data to backend monitoring dashboard
 * 
 * Features:
 * - Send health reports every 5 seconds
 * - Session start/end tracking
 * - Automatic error handling with silent failures
 * - Thread-safe async execution
 * 
 * @author Cosmic Development Team
 * @version 1.0.0
 */
@Singleton
class MetricsReporter @Inject constructor() {
    
    companion object {
        private const val TAG = "MetricsReporter"
        private const val TIMEOUT_SECONDS = 10L
        private const val CONTENT_TYPE = "application/json"
    }
    
    private var apiBaseUrl: String = ""
    
    /**
     * Initialize the reporter with API base URL
     */
    fun initialize(baseUrl: String) {
        apiBaseUrl = baseUrl
        Log.d(TAG, "Metrics reporter initialized with URL: $baseUrl")
    }
    
    /**
     * Send health report to backend API
     * 
     * Endpoint: POST /api/metrics/health-report
     * Sent every 5 seconds during active sessions
     */
    suspend fun sendHealthReport(
        deviceId: String,
        timestamp: Long,
        latency: Long,
        avgLatency: Long,
        throughput: Long,
        avgThroughput: Long,
        jitter: Long,
        frameDropRate: Double,
        droppedFrames: Int,
        totalFrames: Int,
        qualityLevel: String,
        fps: Int,
        resolution: String,
        connectionHealth: String
    ) {
        if (apiBaseUrl.isEmpty()) {
            Log.w(TAG, "⚠️ API base URL not initialized")
            return
        }
        
        try {
            val httpClient = OkHttpClient.Builder()
                .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .build()
            
            // Backend API exact format - all fields at top level
            val json = JSONObject().apply {
                put("device_id", deviceId)
                put("timestamp", timestamp)
                put("latency", latency)
                put("average_latency", avgLatency)
                put("throughput", throughput)
                put("average_throughput", avgThroughput)
                put("jitter", jitter)
                put("frame_drop_rate", frameDropRate)
                put("dropped_frames", droppedFrames)
                put("total_frames", totalFrames)
                put("quality_level", qualityLevel)
                put("fps", fps)
                put("resolution", resolution)
                put("connection_health", connectionHealth)
            }.toString()
            
            val body = json.toRequestBody(CONTENT_TYPE.toMediaType())
            
            val request = Request.Builder()
                .url("$apiBaseUrl/api/metrics/health-report")
                .post(body)
                .build()
            
            withContext(Dispatchers.IO) {
                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        Log.v(TAG, "✅ Health report sent successfully")
                    } else {
                        Log.w(TAG, "⚠️ Health report failed: ${response.code}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error sending health report: ${e.message}")
        }
    }
    
    /**
     * Send session start event to backend API
     * 
     * Endpoint: POST /api/metrics/session-start
     * Called when remote control session begins
     */
    suspend fun startSession(
        deviceId: String,
        timestamp: Long,
        sessionId: String
    ) {
        if (apiBaseUrl.isEmpty()) {
            Log.w(TAG, "⚠️ API base URL not initialized")
            return
        }
        
        try {
            val httpClient = OkHttpClient.Builder()
                .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .build()
            
            val json = JSONObject().apply {
                put("device_id", deviceId)
                put("timestamp", timestamp)
                put("session_id", sessionId)
            }.toString()
            
            val body = json.toRequestBody(CONTENT_TYPE.toMediaType())
            
            val request = Request.Builder()
                .url("$apiBaseUrl/api/metrics/session-start")
                .post(body)
                .build()
            
            withContext(Dispatchers.IO) {
                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        Log.d(TAG, "✅ Session started - ID: $sessionId")
                    } else {
                        Log.w(TAG, "⚠️ Session start failed: ${response.code}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error starting session: ${e.message}")
        }
    }
    
    /**
     * Send session end event to backend API
     * 
     * Endpoint: POST /api/metrics/session-end
     * Called when remote control session ends
     */
    suspend fun endSession(
        deviceId: String,
        sessionId: String,
        durationMs: Long,
        framesSent: Int,
        framesDropped: Int,
        avgLatency: Long
    ) {
        if (apiBaseUrl.isEmpty()) {
            Log.w(TAG, "⚠️ API base URL not initialized")
            return
        }
        
        try {
            val httpClient = OkHttpClient.Builder()
                .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .build()
            
            val json = JSONObject().apply {
                put("device_id", deviceId)
                put("session_id", sessionId)
                put("duration_ms", durationMs)
                put("frames_sent", framesSent)
                put("frames_dropped", framesDropped)
                put("average_latency", avgLatency)
            }.toString()
            
            val body = json.toRequestBody(CONTENT_TYPE.toMediaType())
            
            val request = Request.Builder()
                .url("$apiBaseUrl/api/metrics/session-end")
                .post(body)
                .build()
            
            withContext(Dispatchers.IO) {
                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        Log.d(TAG, "✅ Session ended - Duration: ${durationMs}ms, Frames: $framesSent (dropped: $framesDropped)")
                    } else {
                        Log.w(TAG, "⚠️ Session end failed: ${response.code}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error ending session: ${e.message}")
        }
    }
}

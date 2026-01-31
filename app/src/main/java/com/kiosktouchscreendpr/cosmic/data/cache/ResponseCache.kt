package com.kiosktouchscreendpr.cosmic.data.cache

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local cache untuk response backend menggunakan SharedPreferences
 * Mengurangi beban ke server dengan cache data yang jarang berubah
 */
@Singleton
class ResponseCache @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "response_cache",
        Context.MODE_PRIVATE
    )
    private val gson = Gson()

    companion object {
        private const val CACHE_EXPIRY_MS = 60_000L // 1 minute
    }

    /**
     * Cache heartbeat response (remote_control_enabled status)
     */
    fun cacheHeartbeatResponse(remoteControlEnabled: Boolean) {
        prefs.edit()
            .putBoolean("remote_control_enabled", remoteControlEnabled)
            .putLong("remote_control_enabled_time", System.currentTimeMillis())
            .apply()
    }

    /**
     * Get cached remote_control_enabled jika masih valid (< 1 menit)
     */
    fun getCachedRemoteControlEnabled(): Boolean? {
        val timestamp = prefs.getLong("remote_control_enabled_time", 0)
        val age = System.currentTimeMillis() - timestamp
        
        return if (age < CACHE_EXPIRY_MS) {
            prefs.getBoolean("remote_control_enabled", false)
        } else {
            null // Cache expired
        }
    }

    /**
     * Cache device metrics untuk prevent duplicate reads
     */
    fun cacheDeviceMetrics(metrics: Map<String, Any>) {
        val json = gson.toJson(metrics)
        prefs.edit()
            .putString("device_metrics", json)
            .putLong("device_metrics_time", System.currentTimeMillis())
            .apply()
    }

    /**
     * Get cached metrics jika masih valid (< 30 detik)
     */
    fun getCachedDeviceMetrics(): Map<String, Any>? {
        val timestamp = prefs.getLong("device_metrics_time", 0)
        val age = System.currentTimeMillis() - timestamp
        
        return if (age < 30_000L) { // 30 seconds
            val json = prefs.getString("device_metrics", null)
            if (json != null) {
                val type = object : TypeToken<Map<String, Any>>() {}.type
                gson.fromJson(json, type)
            } else null
        } else {
            null
        }
    }

    /**
     * Clear all cache
     */
    fun clearAll() {
        prefs.edit().clear().apply()
    }
}

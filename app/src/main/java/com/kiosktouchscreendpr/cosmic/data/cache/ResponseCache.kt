package com.kiosktouchscreendpr.cosmic.data.cache

import android.content.Context
import android.content.SharedPreferences
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
     * Clear all cache
     */
    fun clearAll() {
        prefs.edit().clear().apply()
    }
}

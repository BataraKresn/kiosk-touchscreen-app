package com.kiosktouchscreendpr.cosmic.core.utils

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.PowerManager
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.RandomAccessFile
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Device Health Monitoring Service
 * 
 * Collects real-time device health metrics:
 * - Battery level & charging status
 * - WiFi signal strength
 * - Storage space (available & total)
 * - RAM usage (used & total)
 * - CPU temperature
 * - Network type (WiFi/Mobile/None)
 * - Screen on/off status
 * 
 * @author Cosmic Development Team
 * @version 1.0.0
 */
@Singleton
class DeviceHealthMonitor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "DeviceHealthMonitor"
    }

    /**
     * Get current battery level (0-100)
     */
    fun getBatteryLevel(): Int {
        return try {
            val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get battery level", e)
            -1
        }
    }

    /**
     * Check if device is charging
     */
    fun isCharging(): Boolean {
        return try {
            val batteryStatus = context.registerReceiver(
                null,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            )
            val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL
        } catch (e: Exception) {
            Log.w(TAG, "Failed to check charging status", e)
            false
        }
    }

    /**
     * Get WiFi signal strength in dBm
     * Returns value between -100 (weak) to -30 (excellent)
     * Returns null if not connected to WiFi
     */
    fun getWifiStrength(): Int? {
        return try {
            // Use WifiManager for all Android versions - it provides accurate RSSI
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? android.net.wifi.WifiManager
                ?: return null
            
            @Suppress("DEPRECATION")
            val wifiInfo = wifiManager.connectionInfo ?: return null
            
            // Verify we're actually connected to WiFi
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return null
            val network = connectivityManager.activeNetwork ?: return null
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return null
            
            if (!capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                return null // Not on WiFi
            }
            
            val rssi = wifiInfo.rssi
            // RSSI should be between -100 and 0, with -30 being excellent
            // If RSSI is invalid (e.g., -127 or 0), return null
            if (rssi == -127 || rssi == 0 || rssi < -100 || rssi > 0) {
                return null
            }
            
            return rssi
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Get available storage space in MB
     */
    fun getAvailableStorageMB(): Long {
        return try {
            val path = Environment.getDataDirectory()
            val stat = StatFs(path.path)
            val blockSize = stat.blockSizeLong
            val availableBlocks = stat.availableBlocksLong
            (availableBlocks * blockSize) / (1024 * 1024) // Convert to MB
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get available storage", e)
            -1
        }
    }

    /**
     * Get total storage space in MB
     */
    fun getTotalStorageMB(): Long {
        return try {
            val path = Environment.getDataDirectory()
            val stat = StatFs(path.path)
            val blockSize = stat.blockSizeLong
            val totalBlocks = stat.blockCountLong
            (totalBlocks * blockSize) / (1024 * 1024) // Convert to MB
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get total storage", e)
            -1
        }
    }

    /**
     * Get current RAM usage in MB
     */
    fun getRamUsageMB(): Int {
        return try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memoryInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memoryInfo)
            
            val usedMemory = memoryInfo.totalMem - memoryInfo.availMem
            (usedMemory / (1024 * 1024)).toInt() // Convert to MB
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get RAM usage", e)
            -1
        }
    }

    /**
     * Get total RAM in MB
     */
    fun getTotalRamMB(): Int {
        return try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memoryInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memoryInfo)
            
            (memoryInfo.totalMem / (1024 * 1024)).toInt() // Convert to MB
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get total RAM", e)
            -1
        }
    }

    /**
     * Get CPU temperature in Celsius
     * Returns null if unable to read temperature
     */
    fun getCpuTemperature(): Float? {
        return try {
            // Try reading from thermal zones
            val thermalFiles = listOf(
                "/sys/class/thermal/thermal_zone0/temp",
                "/sys/class/thermal/thermal_zone1/temp",
                "/sys/devices/virtual/thermal/thermal_zone0/temp"
            )

            for (path in thermalFiles) {
                val file = File(path)
                if (file.exists()) {
                    val temp = RandomAccessFile(file, "r").use { raf ->
                        raf.readLine()?.toFloatOrNull()
                    }
                    
                    if (temp != null) {
                        // Temperature is usually in millidegrees, convert to Celsius
                        val celsius = if (temp > 1000) temp / 1000f else temp
                        
                        // Validate temperature is in reasonable range (-50°C to 150°C)
                        // Ignore invalid sensor readings
                        if (celsius in -50f..150f) {
                            return celsius
                        }
                    }
                }
            }
            
            null
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get CPU temperature", e)
            null
        }
    }

    /**
     * Get current network type: "WiFi", "Mobile", or "None"
     */
    fun getNetworkType(): String {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val network = connectivityManager.activeNetwork ?: return "None"
                val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return "None"
                
                when {
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WiFi"
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Mobile"
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
                    else -> "Unknown"
                }
            } else {
                @Suppress("DEPRECATION")
                val networkInfo = connectivityManager.activeNetworkInfo ?: return "None"
                
                @Suppress("DEPRECATION")
                when (networkInfo.type) {
                    ConnectivityManager.TYPE_WIFI -> "WiFi"
                    ConnectivityManager.TYPE_MOBILE -> "Mobile"
                    ConnectivityManager.TYPE_ETHERNET -> "Ethernet"
                    else -> "Unknown"
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get network type", e)
            "Unknown"
        }
    }

    /**
     * Check if screen is currently on
     */
    fun isScreenOn(): Boolean {
        return try {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            powerManager.isInteractive
        } catch (e: Exception) {
            Log.w(TAG, "Failed to check screen status", e)
            true // Assume on if can't determine
        }
    }

    /**
     * Get all health metrics at once
     */
    fun getAllMetrics(): DeviceHealthMetrics {
        return DeviceHealthMetrics(
            batteryLevel = getBatteryLevel(),
            isCharging = isCharging(),
            wifiStrength = getWifiStrength(),
            storageAvailableMB = getAvailableStorageMB(),
            storageTotalMB = getTotalStorageMB(),
            ramUsageMB = getRamUsageMB(),
            ramTotalMB = getTotalRamMB(),
            cpuTemp = getCpuTemperature(),
            networkType = getNetworkType(),
            screenOn = isScreenOn()
        )
    }
}

/**
 * Data class for device health metrics
 */
data class DeviceHealthMetrics(
    val batteryLevel: Int,
    val isCharging: Boolean,
    val wifiStrength: Int?,
    val storageAvailableMB: Long,
    val storageTotalMB: Long,
    val ramUsageMB: Int,
    val ramTotalMB: Int,
    val cpuTemp: Float?,
    val networkType: String,
    val screenOn: Boolean
) {
    override fun toString(): String {
        return """
            DeviceHealthMetrics(
                battery=$batteryLevel% ${if (isCharging) "(charging)" else ""},
                wifi=${wifiStrength?.let { "$it dBm" } ?: "N/A"},
                storage=${storageAvailableMB}MB / ${storageTotalMB}MB,
                ram=${ramUsageMB}MB / ${ramTotalMB}MB,
                cpu=${cpuTemp?.let { "%.1f°C".format(it) } ?: "N/A"},
                network=$networkType,
                screen=${if (screenOn) "ON" else "OFF"}
            )
        """.trimIndent()
    }
}

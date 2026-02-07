package com.kiosktouchscreendpr.cosmic

import android.app.Application
import android.content.Intent
import android.provider.Settings
import com.kiosktouchscreendpr.cosmic.BuildConfig
import com.kiosktouchscreendpr.cosmic.core.utils.Preference
import com.kiosktouchscreendpr.cosmic.data.api.DeviceApi
import com.kiosktouchscreendpr.cosmic.data.services.RemoteControlService
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Code author  : Anugrah Surya Putra.
 * Project      : Cosmic
 */
@HiltAndroidApp
class CosmicApp : Application() {

    @Inject
    lateinit var deviceApi: DeviceApi

    @Inject
    lateinit var preference: Preference

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        checkExistingDeviceAndAutoStart()
    }

    /**
     * Check if device exists in CMS and auto-start remote control if device found
     */
    private fun checkExistingDeviceAndAutoStart() {
        applicationScope.launch {
            try {
                // Ensure device_id exists (auto-generate if not)
                val deviceId = ensureDeviceIdExists()
                
                android.util.Log.e("CosmicApp", "🔍 DEBUG: Starting checkExistingDeviceAndAutoStart")
                android.util.Log.e("CosmicApp", "📱 Device ID: $deviceId")
                
                println("CosmicApp: Checking existing device: $deviceId")
                
                // Try to check existing device via API
                val existingDevice = try {
                    deviceApi.checkExistingDevice(
                        baseUrl = BuildConfig.WEBVIEW_BASEURL,
                        deviceId = deviceId
                    )
                } catch (e: Exception) {
                    println("CosmicApp: API checkExistingDevice failed: ${e.message}")
                    null
                }
                
                if (existingDevice != null && 
                    existingDevice.success &&
                    existingDevice.data.remoteId > 0 && 
                    existingDevice.data.token.isNotEmpty()) {
                    
                    println("CosmicApp: Device exists via API, storing credentials and auto-starting")
                    preference.set("remote_id", existingDevice.data.remoteId.toString())
                    preference.set("remote_token", existingDevice.data.token)
                    startRemoteControlService()
                    
                } else {
                    // Fallback: Check if we already have stored credentials
                    val storedRemoteId = preference.get("remote_id", null)
                    val storedRemoteToken = preference.get("remote_token", null)
                    
                    android.util.Log.e("CosmicApp", "🔍 DEBUG: API check failed, checking stored credentials")
                    android.util.Log.e("CosmicApp", "📊 Stored remote_id: $storedRemoteId")
                    android.util.Log.e("CosmicApp", "🔑 Stored remote_token: ${storedRemoteToken?.take(10)}...")
                    
                    if (!storedRemoteId.isNullOrEmpty() && !storedRemoteToken.isNullOrEmpty()) {
                        println("CosmicApp: Using stored credentials for auto-start")
                        println("CosmicApp: Remote ID: $storedRemoteId, Token: ${storedRemoteToken.take(20)}...")
                        startRemoteControlService()
                    } else {
                        println("CosmicApp: No stored credentials found")
                        println("CosmicApp: Device is new - checking for auto-setup...")
                        
                        // Check if we have a token for auto-setup
                        val displayToken = preference.get("token", null)
                        
                        android.util.Log.e("CosmicApp", "🔍 DEBUG: No stored credentials found")
                        android.util.Log.e("CosmicApp", "🎫 Display token: $displayToken")
                        
                        if (!displayToken.isNullOrEmpty()) {
                            println("CosmicApp: Found display token - but waiting for user to submit settings")
                            println("CosmicApp: Will register on explicit user action, not auto-register to prevent duplicates")
                        } else {
                            android.util.Log.e("CosmicApp", "✅ No display token found - manual setup required.")
                            android.util.Log.e("CosmicApp", "⚠️ Device is new, user must configure settings first.")
                            println("CosmicApp: No display token found - manual setup required.")
                            println("CosmicApp: Device is new, user must configure settings first.")
                        }
                    }
                }
            } catch (e: Exception) {
                println("CosmicApp: Error in auto-start check: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    /**
     * Ensure device_id exists in preferences, auto-generate if not found
     */
    private fun ensureDeviceIdExists(): String {
        val existingDeviceId = preference.get("device_id", null)
        
        if (!existingDeviceId.isNullOrEmpty()) {
            println("CosmicApp: Using existing device_id: $existingDeviceId")
            return existingDeviceId
        }
        
        // Generate new device_id for new device
        val newDeviceId = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: "unknown_device_${System.currentTimeMillis()}"
        
        preference.set("device_id", newDeviceId)
        println("CosmicApp: Generated new device_id: $newDeviceId")
        
        return newDeviceId
    }

    private fun startRemoteControlService() {
        try {
            val serviceIntent = Intent(this, RemoteControlService::class.java).apply {
                putExtra("auto_start", true)
            }
            startService(serviceIntent)
            println("CosmicApp: RemoteControlService started with auto_start=true")
        } catch (e: Exception) {
            println("CosmicApp: Error starting RemoteControlService: ${e.message}")
            e.printStackTrace()
        }
    }
}
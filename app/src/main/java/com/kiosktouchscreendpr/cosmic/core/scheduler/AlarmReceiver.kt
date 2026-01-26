package com.kiosktouchscreendpr.cosmic.core.scheduler

import android.app.admin.DevicePolicyManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import android.widget.Toast
import com.kiosktouchscreendpr.cosmic.WakeUpActivity
import com.kiosktouchscreendpr.cosmic.core.receiver.CosmicAdminReceiver

/**
 * Code author  : Anugrah Surya Putra.
 * Project      : Cosmic
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val alarmType = intent.getStringExtra(EXTRA_ALARM_TYPE) ?: ALARM_TYPE_UNKNOWN

        when (alarmType) {
            AlarmType.LOCK.name -> handleLockScreen(context)
            AlarmType.WAKE.name -> handleWakeScreen(context)
            else -> Log.e(TAG, "Unknown alarm type: $alarmType")
        }
    }

    @Suppress("DEPRECATION")
    private fun handleLockScreen(context: Context) {
        val powerManager = context.getSystemService(PowerManager::class.java)
        val wakeLock = powerManager.newWakeLock(
            PowerManager.SCREEN_DIM_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "CosmicApp::WakeLock"
        )

        try {
            Log.d(TAG, "Locking screen...")
            wakeLock.acquire(10 * 1000L)
            attemptDeviceLock(context)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to acquire wake lock", e)
        } finally {
            wakeLock.release()
            Log.d(TAG, "WakeLock released")
        }
    }

    @Suppress("DEPRECATION")
    private fun handleWakeScreen(context: Context) {
        val pm = context.getSystemService(PowerManager::class.java)!!
        val wakeLock = pm.newWakeLock(
            PowerManager.FULL_WAKE_LOCK
                    or PowerManager.ACQUIRE_CAUSES_WAKEUP
                    or PowerManager.ON_AFTER_RELEASE,
            "CosmicApp::ScreenWakeLock"
        )

        wakeLock.acquire(3_000L)


        val wakeIntent = Intent(context, WakeUpActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        context.startActivity(wakeIntent)
    }

    private fun attemptDeviceLock(context: Context) {
        val devicePolicyManager = context.getSystemService(DevicePolicyManager::class.java)
        val componentName = ComponentName(context, CosmicAdminReceiver::class.java)

        if (devicePolicyManager.isAdminActive(componentName)) {
            devicePolicyManager.lockNow()
        } else {
            Toast.makeText(context, "Device Admin permission not granted", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val TAG = "AlarmReceiver"
        private const val EXTRA_ALARM_TYPE = "ALARM_TYPE"
        private const val ALARM_TYPE_UNKNOWN = "UNKNOWN"
    }
}
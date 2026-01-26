package com.kiosktouchscreendpr.cosmic.core.apprequest

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlarmManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.net.toUri
import com.kiosktouchscreendpr.cosmic.core.receiver.CosmicAdminReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Code author  : Anugrah Surya Putra.
 * Project      : Cosmic
 */
interface AppRequest {
    fun needsAdminPermission(): Boolean
    fun requestAdminPermission(activity: Activity)

    fun needsBatteryOptimizationExemption(): Boolean
    fun requestBatteryOptimizationExemption(activity: Activity)


    fun needsExactAlarmPermission(): Boolean
    fun requestExactAlarmPermission(activity: Activity)
}

class AppRequestImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : AppRequest {

    override fun needsAdminPermission(): Boolean {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val component = ComponentName(context, CosmicAdminReceiver::class.java)
        return !dpm.isAdminActive(component)
    }


    /**
     * device admin permission
     */
    override fun requestAdminPermission(activity: Activity) {
        val componentName = ComponentName(context, CosmicAdminReceiver::class.java)
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName)
            putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "We use this to lock screen.")
        }
        activity.startActivity(intent)
    }

    override fun needsBatteryOptimizationExemption(): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return !powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    /**
     * exempt battery optimization to avoid doze mode
     * since this is for kiosk
     *
     * also its always plugged so shouldn't matter
     */
    @SuppressLint("BatteryLife")
    override fun requestBatteryOptimizationExemption(activity: Activity) {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = "package:${context.packageName}".toUri()
        }
        activity.startActivity(intent)
    }

    override fun needsExactAlarmPermission(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                !(context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).canScheduleExactAlarms()
    }

    override fun requestExactAlarmPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = "package:${context.packageName}".toUri()
            }
            activity.startActivity(intent)
        }
    }
}
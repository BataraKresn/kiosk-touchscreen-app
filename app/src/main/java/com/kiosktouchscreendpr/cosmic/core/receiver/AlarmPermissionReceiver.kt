package com.kiosktouchscreendpr.cosmic.core.receiver

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.kiosktouchscreendpr.cosmic.core.utils.WakeService

/**
 * Code author  : Anugrah Surya Putra.
 * Project      : Cosmic
 */
class AlarmPermissionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED") {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val canSchedule = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                alarmManager.canScheduleExactAlarms()
            } else {
                true
            }
            if (canSchedule) {
                Log.d("ExactAlarmReceiver", "Permission granted. Rescheduling alarms.")
                context.startForegroundService(Intent(context, WakeService::class.java))
            } else {
                Log.d("ExactAlarmReceiver", "Permission revoked or not granted.")
                // Optional: Cancel alarms or notify user
            }
        }
    }
}
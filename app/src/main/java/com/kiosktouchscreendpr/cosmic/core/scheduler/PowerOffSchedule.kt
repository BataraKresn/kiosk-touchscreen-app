package com.kiosktouchscreendpr.cosmic.core.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject

/**
 * Code author  : Anugrah Surya Putra.
 * Project      : Cosmic
 */


class PowerOffSchedule @Inject constructor(
    @ApplicationContext private val context: Context,
    private val alarmManager: AlarmManager
) : Schedule {
    override fun schedule(item: AlarmItem) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("ALARM_TYPE", item.type.name)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            item.type.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmTimeMillis = calculateNextAlarmTime(item.hour, item.minute)

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            alarmTimeMillis,
            pendingIntent
        )
    }

    override fun cancel(item: AlarmItem) {
        alarmManager.cancel(
            PendingIntent.getBroadcast(
                context,
                item.type.hashCode(),
                Intent(context, AlarmReceiver::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
    }

    private fun calculateNextAlarmTime(hour: Int, minute: Int): Long {
        val now = LocalDateTime.now()
        var targetTime = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)

        if (targetTime.isBefore(now)) {
            targetTime = targetTime.plusDays(1)
        }

        return targetTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
}
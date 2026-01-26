package com.kiosktouchscreendpr.cosmic.core.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.kiosktouchscreendpr.cosmic.R

class WakeService : Service() {

    private lateinit var wakeLock: PowerManager.WakeLock

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            "WAKE_CHANNEL",
            "Wake Lock Channel",
            NotificationManager.IMPORTANCE_HIGH
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        val powerManager = getSystemService(PowerManager::class.java)
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK, "CosmicApp:WakeService"
        )
        wakeLock.acquire(1000L)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = NotificationCompat.Builder(this, "WAKE_CHANNEL")
            .setContentTitle("Selamat Pagi")
            .setContentText("Selamat Beraktifitas...")
            .setSmallIcon(R.mipmap.ic_launcher)
            .build()

        startForeground(1, notification)
        return START_STICKY
    }

    override fun onDestroy() {
        Log.d("WakeService", "WakeService destroyed")
        wakeLock.release()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
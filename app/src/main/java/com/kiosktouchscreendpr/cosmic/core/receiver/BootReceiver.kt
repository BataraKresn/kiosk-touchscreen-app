package com.kiosktouchscreendpr.cosmic.core.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.kiosktouchscreendpr.cosmic.core.utils.WakeService
import dagger.hilt.android.AndroidEntryPoint

/**
 * Code author  : Anugrah Surya Putra.
 * Project      : Cosmic
 */

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "Device reboot detected!")
            context.startForegroundService(Intent(context, WakeService::class.java))
        }
    }
}
package com.kiosktouchscreendpr.cosmic.core.utils

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.Toast

/**
 * Code author  : Anugrah Surya Putra.
 * Project      : Cosmic
 */
object VNCController {
     fun startVNCServer(context: Context, accessKey: String) {
        try {
            val intent = Intent().apply {
                component = ComponentName(
                    "net.christianbeier.droidvnc_ng",
                    "net.christianbeier.droidvnc_ng.MainService"
                )
                action = "net.christianbeier.droidvnc_ng.ACTION_START"
                putExtra("net.christianbeier.droidvnc_ng.EXTRA_ACCESS_KEY", accessKey)
                putExtra("net.christianbeier.droidvnc_ng.EXTRA_FILE_TRANSFER", true)
                putExtra("net.christianbeier.droidvnc_ng.EXTRA_FALLBACK_SCREEN_CAPTURE", true)
                putExtra("net.christianbeier.droidvnc_ng.EXTRA_VIEW_ONLY", false)

            }

            context.startForegroundService(intent)
            Toast.makeText(context, "VNC Server starting...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to start VNC server: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
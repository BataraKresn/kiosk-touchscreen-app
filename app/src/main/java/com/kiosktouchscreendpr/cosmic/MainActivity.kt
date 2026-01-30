package com.kiosktouchscreendpr.cosmic

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.kiosktouchscreendpr.cosmic.app.App
import com.kiosktouchscreendpr.cosmic.core.apprequest.AppRequest
import com.kiosktouchscreendpr.cosmic.core.utils.NetworkObserver
import com.kiosktouchscreendpr.cosmic.core.utils.WakeService
import com.kiosktouchscreendpr.cosmic.core.utils.setupLegacySystemUI
import com.kiosktouchscreendpr.cosmic.core.utils.setupModernSystemUI
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    companion object {
        var permissionStep = 0
        const val PWD = BuildConfig.APP_PASSWORD
    }

    @Inject
    lateinit var networkObserver: NetworkObserver

    @Inject
    lateinit var appRequest: AppRequest

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        networkObserver.register()
        setupSystemUi()
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.Transparent.toArgb(), Color.White.toArgb()),
            navigationBarStyle = SystemBarStyle.light(
                Color.Transparent.toArgb(),
                Color.White.toArgb()
            )
        )
        setContent {
            App()
        }
    }

    override fun onResume() {
        super.onResume()
        setupSystemUi()

        when (permissionStep) {
            0 -> {
                if (appRequest.needsAdminPermission()) {
                    appRequest.requestAdminPermission(this)
                    permissionStep = 1
                    return
                }
                permissionStep = 1
            }

            1 -> {
                if (appRequest.needsBatteryOptimizationExemption()) {
                    appRequest.requestBatteryOptimizationExemption(this)
                    permissionStep = 2
                    return
                }
                permissionStep = 2
            }

            2 -> {
                if (appRequest.needsExactAlarmPermission()) {
                    appRequest.requestExactAlarmPermission(this)
                    permissionStep = 3
                    return
                }
                permissionStep = 3
            }

            3 -> {
                // All permissions handled — proceed to start service or initialize app
                startForegroundService(Intent(this, WakeService::class.java))
                permissionStep = 4
            }
        }
    }

    override fun onStart() {
        super.onStart()
        setupSystemUi()
    }

    override fun onDestroy() {
        super.onDestroy()
        networkObserver.unregister()
    }


    private fun setupSystemUi() {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                setupModernSystemUI(window)
                startLockTask()
            }

            else -> setupLegacySystemUI(window)
        }
    }
}
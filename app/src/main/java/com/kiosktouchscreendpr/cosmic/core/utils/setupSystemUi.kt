package com.kiosktouchscreendpr.cosmic.core.utils

import android.os.Build
import android.view.View
import android.view.Window
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Code author  : Anugrah Surya Putra.
 * Project      : Cosmic
 */


@RequiresApi(Build.VERSION_CODES.R)
fun setupModernSystemUI(
    window: Window
) {
    WindowCompat.setDecorFitsSystemWindows(window, false)

    val controller = window.insetsController
    controller?.hide(WindowInsets.Type.systemBars())
    controller?.systemBarsBehavior =
        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
}


@Suppress("DEPRECATION")
fun setupLegacySystemUI(
    window: Window
) {
    window.statusBarColor = Color.Transparent.value.toInt()
    window.decorView.systemUiVisibility =
        (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN)
}

@Suppress("DEPRECATION")
@Composable
fun HideSystemBars() {
    val view = LocalView.current
    val controller = ViewCompat.getWindowInsetsController(view)

    controller?.apply {
        hide(WindowInsetsCompat.Type.systemBars())
        systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
}
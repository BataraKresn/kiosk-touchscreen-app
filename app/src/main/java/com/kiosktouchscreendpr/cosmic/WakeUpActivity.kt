package com.kiosktouchscreendpr.cosmic

import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp

@Suppress("DEPRECATION")
class WakeUpActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        )

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
            setTurnScreenOn(true)
            setShowWhenLocked(true)
        }

        setContent {
            WakeUpScreen()
        }

        Handler(mainLooper).postDelayed(
            {
                Log.d("WakeUpActivity", "Finishing activity after delay")
                finish()
            }, 2000
        )
    }

    override fun onResume() {
        super.onResume()
        Log.d("WakeUpActivity", "onResume called")
    }

    override fun onPause() {
        super.onPause()
        Log.d("WakeUpActivity", "onPause called")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("WakeUpActivity", "onDestroy called")
    }
}

@Composable
fun WakeUpScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Text(
            text = "Waking up...",
            color = Color.White,
            style = TextStyle(fontSize = 24.sp),
            modifier = Modifier.align(Alignment.Center)
        )
    }
}
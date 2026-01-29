package com.kiosktouchscreendpr.cosmic.data.services

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.*
import org.json.JSONObject

/**
 * Input Injection Service using AccessibilityService API
 * 
 * Features:
 * - Touch event injection (tap, long press)
 * - Swipe gesture simulation
 * - Keyboard input injection
 * - Multi-touch support (future)
 * 
 * Permissions Required:
 * - BIND_ACCESSIBILITY_SERVICE
 * - User must manually enable in Settings > Accessibility
 * 
 * Usage:
 * ```kotlin
 * // From RemoteControlWebSocketClient:
 * val command = JSONObject(message)
 * InputInjectionService.getInstance()?.injectTouch(x, y)
 * ```
 * 
 * @author Cosmic Development Team
 * @version 1.0.0 (POC)
 */
class InputInjectionService : AccessibilityService() {

    companion object {
        private const val TAG = "InputInjectionService"
        
        // Gesture durations (milliseconds)
        private const val TAP_DURATION = 50L
        private const val LONG_PRESS_DURATION = 500L
        private const val SWIPE_DURATION = 300L
        
        // Singleton instance for external access
        @Volatile
        private var instance: InputInjectionService? = null
        
        fun getInstance(): InputInjectionService? = instance
    }

    // Coroutine scope for async operations
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        instance = this
        Log.d(TAG, "InputInjectionService created")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Required override - we don't need to observe events
        // We only use this service to dispatch gestures
    }

    override fun onInterrupt() {
        Log.w(TAG, "InputInjectionService interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        serviceScope.cancel()
        Log.d(TAG, "InputInjectionService destroyed")
    }

    /**
     * Inject a touch event (tap) at coordinates
     * 
     * @param x X coordinate (0.0 to 1.0 or absolute pixels)
     * @param y Y coordinate (0.0 to 1.0 or absolute pixels)
     * @param isNormalized If true, coordinates are 0.0-1.0, else absolute pixels
     */
    fun injectTouch(x: Float, y: Float, isNormalized: Boolean = false) {
        serviceScope.launch {
            try {
                val (absX, absY) = if (isNormalized) {
                    convertNormalizedToAbsolute(x, y)
                } else {
                    Pair(x, y)
                }
                
                Log.d(TAG, "Injecting touch at ($absX, $absY)")
                
                val path = Path().apply {
                    moveTo(absX, absY)
                }
                
                val gestureBuilder = GestureDescription.Builder()
                val stroke = GestureDescription.StrokeDescription(
                    path,
                    0,
                    TAP_DURATION
                )
                gestureBuilder.addStroke(stroke)
                
                val gesture = gestureBuilder.build()
                val callback = object : GestureResultCallback() {
                    override fun onCompleted(gestureDescription: GestureDescription?) {
                        Log.v(TAG, "Touch injection completed")
                    }
                    
                    override fun onCancelled(gestureDescription: GestureDescription?) {
                        Log.w(TAG, "Touch injection cancelled")
                    }
                }
                
                dispatchGesture(gesture, callback, null)
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to inject touch", e)
            }
        }
    }

    /**
     * Inject a long press event
     * 
     * @param x X coordinate
     * @param y Y coordinate
     * @param isNormalized If true, coordinates are 0.0-1.0
     */
    fun injectLongPress(x: Float, y: Float, isNormalized: Boolean = false) {
        serviceScope.launch {
            try {
                val (absX, absY) = if (isNormalized) {
                    convertNormalizedToAbsolute(x, y)
                } else {
                    Pair(x, y)
                }
                
                Log.d(TAG, "Injecting long press at ($absX, $absY)")
                
                val path = Path().apply {
                    moveTo(absX, absY)
                }
                
                val gestureBuilder = GestureDescription.Builder()
                val stroke = GestureDescription.StrokeDescription(
                    path,
                    0,
                    LONG_PRESS_DURATION
                )
                gestureBuilder.addStroke(stroke)
                
                dispatchGesture(gestureBuilder.build(), null, null)
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to inject long press", e)
            }
        }
    }

    /**
     * Inject a swipe gesture
     * 
     * @param startX Start X coordinate
     * @param startY Start Y coordinate
     * @param endX End X coordinate
     * @param endY End Y coordinate
     * @param isNormalized If true, coordinates are 0.0-1.0
     * @param durationMs Swipe duration in milliseconds
     */
    fun injectSwipe(
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        isNormalized: Boolean = false,
        durationMs: Long = SWIPE_DURATION
    ) {
        serviceScope.launch {
            try {
                val (absStartX, absStartY) = if (isNormalized) {
                    convertNormalizedToAbsolute(startX, startY)
                } else {
                    Pair(startX, startY)
                }
                
                val (absEndX, absEndY) = if (isNormalized) {
                    convertNormalizedToAbsolute(endX, endY)
                } else {
                    Pair(endX, endY)
                }
                
                Log.d(TAG, "Injecting swipe from ($absStartX,$absStartY) to ($absEndX,$absEndY)")
                
                val path = Path().apply {
                    moveTo(absStartX, absStartY)
                    lineTo(absEndX, absEndY)
                }
                
                val gestureBuilder = GestureDescription.Builder()
                val stroke = GestureDescription.StrokeDescription(
                    path,
                    0,
                    durationMs
                )
                gestureBuilder.addStroke(stroke)
                
                dispatchGesture(gestureBuilder.build(), null, null)
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to inject swipe", e)
            }
        }
    }

    /**
     * Inject keyboard input (text)
     * Note: This is a simplified version. For full keyboard support,
     * consider using AccessibilityNodeInfo actions or system IME.
     * 
     * @param text Text to inject
     */
    fun injectText(text: String) {
        serviceScope.launch {
            try {
                Log.d(TAG, "Injecting text: $text")
                
                // For POC, we'll simulate typing by focusing on input field
                // and pasting text. Full implementation requires IME integration.
                
                val rootNode = rootInActiveWindow
                if (rootNode != null) {
                    // Find focused EditText and paste
                    val focusedNode = rootNode.findFocus(AccessibilityEvent.FOCUS_INPUT)
                    if (focusedNode != null) {
                        val arguments = android.os.Bundle()
                        arguments.putCharSequence(
                            android.view.accessibility.AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                            text
                        )
                        focusedNode.performAction(
                            android.view.accessibility.AccessibilityNodeInfo.ACTION_SET_TEXT,
                            arguments
                        )
                        Log.d(TAG, "Text injected successfully")
                    } else {
                        Log.w(TAG, "No input field focused")
                    }
                } else {
                    Log.w(TAG, "Cannot access root window")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to inject text", e)
            }
        }
    }

    /**
     * Simulate hardware key press (Back, Home, etc.)
     * 
     * @param keyCode Android KeyEvent code
     */
    fun injectKeyPress(keyCode: Int) {
        serviceScope.launch {
            try {
                Log.d(TAG, "Injecting key press: $keyCode")
                
                when (keyCode) {
                    android.view.KeyEvent.KEYCODE_BACK -> {
                        performGlobalAction(GLOBAL_ACTION_BACK)
                    }
                    android.view.KeyEvent.KEYCODE_HOME -> {
                        performGlobalAction(GLOBAL_ACTION_HOME)
                    }
                    android.view.KeyEvent.KEYCODE_APP_SWITCH -> {
                        performGlobalAction(GLOBAL_ACTION_RECENTS)
                    }
                    else -> {
                        Log.w(TAG, "Unsupported key code: $keyCode")
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to inject key press", e)
            }
        }
    }

    /**
     * Convert normalized coordinates (0.0-1.0) to absolute screen pixels
     */
    private fun convertNormalizedToAbsolute(normalizedX: Float, normalizedY: Float): Pair<Float, Float> {
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels.toFloat()
        val screenHeight = displayMetrics.heightPixels.toFloat()
        
        val absoluteX = normalizedX * screenWidth
        val absoluteY = normalizedY * screenHeight
        
        return Pair(absoluteX, absoluteY)
    }

    /**
     * Process input command from WebSocket
     * 
     * Expected JSON format:
     * ```json
     * {
     *   "type": "touch|swipe|longpress|text|key",
     *   "x": 0.5,
     *   "y": 0.5,
     *   "endX": 0.8,  // For swipe
     *   "endY": 0.3,  // For swipe
     *   "text": "Hello",  // For text
     *   "keyCode": 4,  // For key (BACK=4, HOME=3)
     *   "normalized": true
     * }
     * ```
     */
    fun processInputCommand(jsonCommand: String) {
        try {
            val command = JSONObject(jsonCommand)
            val type = command.getString("type")
            
            when (type) {
                "touch" -> {
                    val x = command.getDouble("x").toFloat()
                    val y = command.getDouble("y").toFloat()
                    val normalized = command.optBoolean("normalized", true)
                    injectTouch(x, y, normalized)
                }
                
                "longpress" -> {
                    val x = command.getDouble("x").toFloat()
                    val y = command.getDouble("y").toFloat()
                    val normalized = command.optBoolean("normalized", true)
                    injectLongPress(x, y, normalized)
                }
                
                "swipe" -> {
                    val startX = command.getDouble("x").toFloat()
                    val startY = command.getDouble("y").toFloat()
                    val endX = command.getDouble("endX").toFloat()
                    val endY = command.getDouble("endY").toFloat()
                    val normalized = command.optBoolean("normalized", true)
                    val duration = command.optLong("duration", SWIPE_DURATION)
                    injectSwipe(startX, startY, endX, endY, normalized, duration)
                }
                
                "text" -> {
                    val text = command.getString("text")
                    injectText(text)
                }
                
                "key" -> {
                    val keyCode = command.getInt("keyCode")
                    injectKeyPress(keyCode)
                }
                
                else -> {
                    Log.w(TAG, "Unknown input command type: $type")
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to process input command", e)
        }
    }
}

/*
 * ACCESSIBILITY SERVICE CONFIGURATION
 * 
 * Create: res/xml/accessibility_service_config.xml
 * 
 * <?xml version="1.0" encoding="utf-8"?>
 * <accessibility-service xmlns:android="http://schemas.android.com/apk/res/android"
 *     android:accessibilityEventTypes="typeAllMask"
 *     android:accessibilityFeedbackType="feedbackGeneric"
 *     android:accessibilityFlags="flagDefault|flagRequestTouchExplorationMode|flagRequestFilterKeyEvents"
 *     android:canPerformGestures="true"
 *     android:canRetrieveWindowContent="true"
 *     android:description="@string/accessibility_service_description"
 *     android:notificationTimeout="100"
 *     android:packageNames="com.kiosktouchscreendpr.cosmic"
 *     android:settingsActivity="com.kiosktouchscreendpr.cosmic.presentation.settings.SettingsView" />
 * 
 * Add to strings.xml:
 * <string name="accessibility_service_description">
 *     Allows remote control of this device via Cosmic CMS. 
 *     Required for touch event injection.
 * </string>
 */

package com.kiosktouchscreendpr.cosmic.data.services

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

/**
 * Screen Capture Service using MediaProjection API
 * 
 * Features:
 * - Real-time screen capture (configurable FPS)
 * - MJPEG encoding (Phase 1) or H.264 (Phase 2)
 * - Frame callback for WebSocket transmission
 * - Resolution scaling for bandwidth optimization
 * 
 * Usage:
 * ```kotlin
 * val intent = Intent(context, ScreenCaptureService::class.java)
 * intent.putExtra("resultCode", resultCode)
 * intent.putExtra("data", mediaProjectionData)
 * startForegroundService(intent)
 * ```
 * 
 * @author Cosmic Development Team
 * @version 1.0.0 (POC)
 */
@AndroidEntryPoint
class ScreenCaptureService : Service() {

    companion object {
        private const val TAG = "ScreenCaptureService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "screen_capture_channel"
        
        // Configuration
        private const val TARGET_FPS = 30
        private const val FRAME_INTERVAL_MS = 1000L / TARGET_FPS
        
        // Screen resolution (will be scaled down for bandwidth)
        private const val CAPTURE_WIDTH = 1080
        private const val CAPTURE_HEIGHT = 1920
        private const val SCREEN_DENSITY = 320
        
        // MJPEG Quality (0-100)
        private const val JPEG_QUALITY = 75
        
        // Frame watchdog
        private const val FRAME_TIMEOUT_MS = 5000L // 5 seconds without frames = problem
        
        // Action for callback
        const val ACTION_FRAME_AVAILABLE = "com.kiosktouchscreendpr.cosmic.FRAME_AVAILABLE"
        
        // Static holder for MediaProjection data (cannot be parceled through Intent extras)
        // These are set by RemoteControlViewModel BEFORE the service is started
        var mediaProjectionResultCode: Int = -1
        var mediaProjectionData: Intent? = null
    }

    // MediaProjection components
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    
    // Handler for background processing
    private var handlerThread: HandlerThread? = null
    private var handler: Handler? = null
    
    // Frame callback listener
    private var frameCallback: ((ByteArray) -> Unit)? = null

    @Inject
    lateinit var webSocketClient: RemoteControlWebSocketClient
    
    // Frame rate control and watchdog
    private var lastFrameTime = 0L
    private var lastFrameReceivedTime = 0L
    private var frameCount = 0L
    private var watchdogRunnable: Runnable? = null
    
    override fun onCreate() {
        super.onCreate()
        Log.e(TAG, "📹📹📹 ScreenCaptureService onCreate() called!")
        Log.e(TAG, "🔧 Creating notification channel...")
        
        // Create notification channel
        createNotificationChannel()
        
        Log.e(TAG, "🎯 Starting foreground service...")
        // Start foreground service
        startForeground(NOTIFICATION_ID, createNotification())
        Log.e(TAG, "✅ ScreenCaptureService onCreate() completed!")
        
        // Setup background handler
        handlerThread = HandlerThread("ScreenCaptureThread").apply {
            start()
            handler = Handler(looper)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.e(TAG, "📹📹📹 onStartCommand called!")
        
        // Try to get MediaProjection data from companion object
        var resultCode: Int? = null
        var data: Intent? = null
        
        Log.e(TAG, "📦 Checking companion object - mediaProjectionResultCode: $mediaProjectionResultCode, mediaProjectionData: $mediaProjectionData")
        
        // RESULT_OK is -1, so check explicitly for that value
        if (mediaProjectionResultCode == Activity.RESULT_OK && mediaProjectionData != null) {
            resultCode = mediaProjectionResultCode
            data = mediaProjectionData
            Log.e(TAG, "✅✅✅ Retrieved MediaProjection data from companion object!")
            Log.e(TAG, "   resultCode: $resultCode (RESULT_OK)")
            Log.e(TAG, "   data: $data")
            // Clear after retrieval
            mediaProjectionResultCode = 0
            mediaProjectionData = null
        } else {
            Log.e(TAG, "⚠️ No stored MediaProjection data in companion object - trying Intent extras as fallback")
            intent?.let {
                resultCode = it.getIntExtra("resultCode", -1)
                Log.e(TAG, "   resultCode from Intent: $resultCode")
            }
        }
        
        Log.e(TAG, "Final check - resultCode: $resultCode, data: $data")
        
        // Activity.RESULT_OK = -1
        if (resultCode == Activity.RESULT_OK && data != null) {
            Log.e(TAG, "🎬🎬🎬 Starting screen capture!")
            startCapture(resultCode, data)
        } else {
            Log.e(TAG, "❌ Cannot start capture - resultCode: $resultCode (expected: ${Activity.RESULT_OK}), data: $data")
        }
        
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * Start screen capture with MediaProjection
     */
    private fun startCapture(resultCode: Int, data: Intent) {
        Log.e(TAG, "🚀🚀🚀 startCapture() called - resultCode: $resultCode, data: $data")
        
        try {
            val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = projectionManager.getMediaProjection(resultCode, data)
            
            // Android 14+ requires callback registration before createVirtualDisplay
            mediaProjection?.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() {
                    Log.e(TAG, "⚠️ MediaProjection stopped")
                    stopSelf()
                }
            }, handler)
            
            Log.e(TAG, "✅ MediaProjection callback registered")
            
            // Setup ImageReader for frame capture
            imageReader = ImageReader.newInstance(
                CAPTURE_WIDTH,
                CAPTURE_HEIGHT,
                PixelFormat.RGBA_8888,
                2 // Max images
            ).apply {
                setOnImageAvailableListener({ reader ->
                    processFrame(reader)
                }, handler)
            }
            
            Log.e(TAG, "✅ ImageReader created")
            
            // Create virtual display
            virtualDisplay = mediaProjection?.createVirtualDisplay(
                "KioskScreenCapture",
                CAPTURE_WIDTH,
                CAPTURE_HEIGHT,
                SCREEN_DENSITY,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader?.surface,
                null,
                handler
            )
            
            Log.e(TAG, "✅✅✅ Screen capture started successfully!")
            
            // Start watchdog timer to detect ImageReader stalls
            startFrameWatchdog()
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to start screen capture", e)
            stopSelf()
        }
    }
    
    /**
     * Start watchdog timer to detect when ImageReader stops producing frames
     */
    private fun startFrameWatchdog() {
        lastFrameReceivedTime = System.currentTimeMillis()
        watchdogRunnable = object : Runnable {
            override fun run() {
                val timeSinceLastFrame = System.currentTimeMillis() - lastFrameReceivedTime
                if (timeSinceLastFrame > FRAME_TIMEOUT_MS && frameCount > 0) {
                    Log.e(TAG, "🚨🚨🚨 FRAME TIMEOUT! No frames for ${timeSinceLastFrame}ms (Total frames: $frameCount)")
                    Log.e(TAG, "🔍 MediaProjection: ${mediaProjection != null}, VirtualDisplay: ${virtualDisplay != null}, ImageReader: ${imageReader != null}")
                    
                    // Try to restart capture
                    Log.e(TAG, "🔄 Attempting to restart capture...")
                    restartCapture()
                } else if (frameCount > 0) {
                    Log.d(TAG, "🐕 Watchdog: ${timeSinceLastFrame}ms since last frame (Total: $frameCount frames)")
                }
                
                // Schedule next check
                handler?.postDelayed(this, FRAME_TIMEOUT_MS)
            }
        }
        handler?.postDelayed(watchdogRunnable!!, FRAME_TIMEOUT_MS)
        Log.e(TAG, "🐕 Frame watchdog started")
    }
    
    /**
     * Attempt to restart capture when ImageReader stalls
     */
    private fun restartCapture() {
        try {
            Log.e(TAG, "🔄 Releasing old resources...")
            virtualDisplay?.release()
            imageReader?.close()
            
            // Recreate ImageReader
            imageReader = ImageReader.newInstance(
                CAPTURE_WIDTH,
                CAPTURE_HEIGHT,
                PixelFormat.RGBA_8888,
                2
            ).apply {
                setOnImageAvailableListener({ reader ->
                    processFrame(reader)
                }, handler)
            }
            
            // Recreate VirtualDisplay
            virtualDisplay = mediaProjection?.createVirtualDisplay(
                "KioskScreenCapture",
                CAPTURE_WIDTH,
                CAPTURE_HEIGHT,
                SCREEN_DENSITY,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader?.surface,
                null,
                handler
            )
            
            frameCount = 0
            lastFrameReceivedTime = System.currentTimeMillis()
            Log.e(TAG, "✅ Capture restarted successfully")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to restart capture: ${e.message}", e)
        }
    }

    /**
     * Process captured frame
     */
    private fun processFrame(reader: ImageReader) {
        // Update watchdog
        lastFrameReceivedTime = System.currentTimeMillis()
        frameCount++
        
        // Frame rate control
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastFrameTime < FRAME_INTERVAL_MS) {
            return // Skip frame to maintain target FPS
        }
        lastFrameTime = currentTime
        
        var image: Image? = null
        try {
            image = reader.acquireLatestImage()
            if (image == null) {
                Log.w(TAG, "⚠️ acquireLatestImage() returned null - ImageReader may be stalled")
                return
            }
            
            // Convert to JPEG
            val jpegBytes = try {
                encodeToJPEG(image)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error encoding JPEG: ${e.message}", e)
                return
            }
            
            // Send to callback (if set)
            try {
                frameCallback?.invoke(jpegBytes)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error in frameCallback: ${e.message}", e)
            }

            // Send to WebSocket client (primary path)
            try {
                if (::webSocketClient.isInitialized) {
                    webSocketClient.queueFrame(jpegBytes)
                    Log.d(TAG, "📤 Frame queued to WebSocket: ${jpegBytes.size / 1024}KB")
                } else {
                    Log.e(TAG, "❌ webSocketClient NOT initialized!")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error queueing frame to WebSocket: ${e.message}", e)
            }
            
            // Log frame stats (remove in production)
            Log.v(TAG, "📸 Frame captured: ${jpegBytes.size / 1024}KB")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error processing frame: ${e.message}", e)
            e.printStackTrace()
        } finally {
            try {
                image?.close()
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error closing image: ${e.message}", e)
            }
        }
    }

    /**
     * Encode Image to JPEG (MJPEG)
     * Phase 1: Simple JPEG encoding
     * Phase 2: Migrate to H.264 for better compression
     */
    private fun encodeToJPEG(image: Image): ByteArray {
        val planes = image.planes
        val buffer = planes[0].buffer
        val pixelStride = planes[0].pixelStride
        val rowStride = planes[0].rowStride
        val rowPadding = rowStride - pixelStride * CAPTURE_WIDTH
        
        // Create bitmap from buffer
        val bitmap = Bitmap.createBitmap(
            CAPTURE_WIDTH + rowPadding / pixelStride,
            CAPTURE_HEIGHT,
            Bitmap.Config.ARGB_8888
        )
        bitmap.copyPixelsFromBuffer(buffer)
        
        // Crop if needed (due to row padding)
        val croppedBitmap = if (rowPadding != 0) {
            Bitmap.createBitmap(bitmap, 0, 0, CAPTURE_WIDTH, CAPTURE_HEIGHT)
        } else {
            bitmap
        }
        
        // Compress to JPEG
        val outputStream = ByteArrayOutputStream()
        croppedBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, outputStream)
        
        // Cleanup
        bitmap.recycle()
        if (croppedBitmap != bitmap) {
            croppedBitmap.recycle()
        }
        
        return outputStream.toByteArray()
    }

    /**
     * Set frame callback for WebSocket transmission
     */
    fun setFrameCallback(callback: (ByteArray) -> Unit) {
        this.frameCallback = callback
    }

    /**
     * Create notification for foreground service
     */
    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Remote Control Active")
            .setContentText("Screen is being captured for remote viewing")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    /**
     * Create notification channel (Android 8.0+)
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Screen Capture Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notification for screen capture service"
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }

    /**
     * Stop capturing and cleanup resources
     */
    private fun stopCapture() {
        Log.d(TAG, "Stopping screen capture")
        
        // Stop watchdog
        watchdogRunnable?.let { handler?.removeCallbacks(it) }
        watchdogRunnable = null
        
        imageReader?.close()
        virtualDisplay?.release()
        mediaProjection?.stop()
        
        imageReader = null
        virtualDisplay = null
        mediaProjection = null
        frameCallback = null
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "ScreenCaptureService destroyed")
        
        stopCapture()
        handlerThread?.quitSafely()
    }
}

/* 
 * PHASE 2 UPGRADE: H.264 Encoding (Better Compression)
 * 
 * Replace encodeToJPEG() with:
 * 
 * private fun encodeToH264(image: Image): ByteArray {
 *     // Use MediaCodec for H.264 encoding
 *     val codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
 *     val format = MediaFormat.createVideoFormat(
 *         MediaFormat.MIMETYPE_VIDEO_AVC,
 *         CAPTURE_WIDTH,
 *         CAPTURE_HEIGHT
 *     ).apply {
 *         setInteger(MediaFormat.KEY_BIT_RATE, 2_000_000) // 2 Mbps
 *         setInteger(MediaFormat.KEY_FRAME_RATE, TARGET_FPS)
 *         setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
 *         setInteger(
 *             MediaFormat.KEY_COLOR_FORMAT,
 *             MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
 *         )
 *     }
 *     codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
 *     // ... implement encoding logic
 * }
 */

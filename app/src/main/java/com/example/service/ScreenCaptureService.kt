package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.ByteArrayOutputStream

class ScreenCaptureService : Service() {

    companion object {
        private const val TAG = "ScreenCaptureService"
        private const val NOTIFICATION_CHANNEL_ID = "game_ai_capture_channel"
        private const val NOTIFICATION_ID = 8801

        const val ACTION_START = "com.example.service.START_CAPTURE"
        const val ACTION_STOP = "com.example.service.STOP_CAPTURE"
        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_RESULT_DATA = "extra_result_data"

        private val _isCapturing = MutableStateFlow(false)
        val isCapturing: StateFlow<Boolean> = _isCapturing.asStateFlow()

        private val _latestFrame = MutableStateFlow<Bitmap?>(null)
        val latestFrame: StateFlow<Bitmap?> = _latestFrame.asStateFlow()

        private var instance: ScreenCaptureService? = null

        fun captureCurrentScreen(): Bitmap? {
            return instance?.captureBitmapNow() ?: _latestFrame.value
        }

        fun updateCustomFrame(bitmap: Bitmap) {
            _latestFrame.value = bitmap
        }
    }

    private var mediaProjectionManager: MediaProjectionManager? = null
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null

    private var screenWidth = 720
    private var screenHeight = 1280
    private var screenDensity = 320

    override fun onCreate() {
        super.onCreate()
        instance = this
        mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
                val resultData: Intent? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(EXTRA_RESULT_DATA)
                }

                if (resultCode != 0 && resultData != null) {
                    startForegroundServiceWithNotification()
                    initMediaProjection(resultCode, resultData)
                } else {
                    Log.e(TAG, "Invalid MediaProjection result credentials")
                    stopSelf()
                }
            }
            ACTION_STOP -> {
                stopScreenCapture()
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun startForegroundServiceWithNotification() {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, ScreenCaptureService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("GameAI Agent Active")
            .setContentText("Watching screen for game state reasoning and automation")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_media_pause, "Stop Agent", stopIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun initMediaProjection(resultCode: Int, data: Intent) {
        try {
            val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealMetrics(metrics)

            // Scaled dimensions to optimize image processing and token efficiency
            val maxWidth = 720
            val scale = if (metrics.widthPixels > maxWidth) maxWidth.toFloat() / metrics.widthPixels else 1f
            screenWidth = (metrics.widthPixels * scale).toInt()
            screenHeight = (metrics.heightPixels * scale).toInt()
            screenDensity = metrics.densityDpi

            mediaProjection = mediaProjectionManager?.getMediaProjection(resultCode, data)
            mediaProjection?.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() {
                    super.onStop()
                    Log.i(TAG, "MediaProjection callback onStop received")
                    stopScreenCapture()
                }
            }, null)

            imageReader = ImageReader.newInstance(
                screenWidth,
                screenHeight,
                PixelFormat.RGBA_8888,
                2
            )

            virtualDisplay = mediaProjection?.createVirtualDisplay(
                "GameAIScreenCapture",
                screenWidth,
                screenHeight,
                screenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader?.surface,
                null,
                null
            )

            imageReader?.setOnImageAvailableListener({ reader ->
                val image = reader.acquireLatestImage()
                if (image != null) {
                    processImage(image)
                }
            }, null)

            _isCapturing.value = true
            Log.i(TAG, "VirtualDisplay created: ${screenWidth}x${screenHeight} @ $screenDensity dpi")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing MediaProjection: ${e.message}", e)
            stopScreenCapture()
        }
    }

    private fun processImage(image: Image) {
        try {
            val planes = image.planes
            val buffer = planes[0].buffer
            val pixelStride = planes[0].pixelStride
            val rowStride = planes[0].rowStride
            val rowPadding = rowStride - pixelStride * screenWidth

            val rawBitmap = Bitmap.createBitmap(
                screenWidth + rowPadding / pixelStride,
                screenHeight,
                Bitmap.Config.ARGB_8888
            )
            rawBitmap.copyPixelsFromBuffer(buffer)

            val cleanBitmap = if (rowPadding == 0) {
                rawBitmap
            } else {
                Bitmap.createBitmap(rawBitmap, 0, 0, screenWidth, screenHeight).also {
                    rawBitmap.recycle()
                }
            }

            _latestFrame.value = cleanBitmap
        } catch (e: Exception) {
            Log.e(TAG, "Error processing frame: ${e.message}")
        } finally {
            image.close()
        }
    }

    fun captureBitmapNow(): Bitmap? {
        val reader = imageReader ?: return _latestFrame.value
        return try {
            val image = reader.acquireLatestImage()
            if (image != null) {
                processImage(image)
            }
            _latestFrame.value
        } catch (e: Exception) {
            Log.e(TAG, "Error in captureBitmapNow: ${e.message}")
            _latestFrame.value
        }
    }

    private fun stopScreenCapture() {
        _isCapturing.value = false
        try {
            virtualDisplay?.release()
            virtualDisplay = null
            imageReader?.close()
            imageReader = null
            mediaProjection?.stop()
            mediaProjection = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping capture resources: ${e.message}")
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "GameAI Screen Capture Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Foreground notification while GameAI is analyzing game screens"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        stopScreenCapture()
        if (instance == this) instance = null
    }
}

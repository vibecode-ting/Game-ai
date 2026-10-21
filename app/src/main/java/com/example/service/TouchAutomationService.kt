package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.util.DisplayMetrics
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.example.data.model.ActionType
import com.example.data.model.GameAction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class TouchAutomationService : AccessibilityService() {

    companion object {
        private const val TAG = "TouchAutomationService"

        private var instance: TouchAutomationService? = null

        private val _isServiceActive = MutableStateFlow(false)
        val isServiceActive: StateFlow<Boolean> = _isServiceActive.asStateFlow()

        private val _lastExecutedAction = MutableStateFlow<GameAction?>(null)
        val lastExecutedAction: StateFlow<GameAction?> = _lastExecutedAction.asStateFlow()

        fun isConnected(): Boolean = instance != null

        suspend fun executeAction(action: GameAction): Boolean {
            val service = instance ?: run {
                Log.w(TAG, "TouchAutomationService is not enabled or bound")
                return false
            }
            return service.performGestureAction(action)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        _isServiceActive.value = true
        Log.i(TAG, "TouchAutomationService connected and ready for gesture dispatching")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Can optionally inspect window content or ad dialog titles
    }

    override fun onInterrupt() {
        Log.w(TAG, "TouchAutomationService interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
            _isServiceActive.value = false
        }
        Log.i(TAG, "TouchAutomationService destroyed")
    }

    suspend fun performGestureAction(action: GameAction): Boolean {
        _lastExecutedAction.value = action
        return when (action.action) {
            ActionType.TAP -> {
                val normX = action.x ?: 500
                val normY = action.y ?: 500
                val (px, py) = translateCoordinates(normX, normY)
                dispatchTap(px, py, action.durationMs.coerceIn(40L, 300L))
            }
            ActionType.SWIPE -> {
                val sX = action.startX ?: 500
                val sY = action.startY ?: 500
                val eX = action.endX ?: 500
                val eY = action.endY ?: 500
                val (startPx, startPy) = translateCoordinates(sX, sY)
                val (endPx, endPy) = translateCoordinates(eX, eY)
                dispatchSwipe(startPx, startPy, endPx, endPy, action.durationMs.coerceIn(100L, 2000L))
            }
            ActionType.HOLD -> {
                val normX = action.x ?: 500
                val normY = action.y ?: 500
                val (px, py) = translateCoordinates(normX, normY)
                dispatchHold(px, py, action.durationMs.coerceIn(300L, 5000L))
            }
            ActionType.WAIT -> {
                kotlinx.coroutines.delay(action.durationMs.coerceAtLeast(100L))
                true
            }
        }
    }

    private fun translateCoordinates(normalizedX: Int, normalizedY: Int): Pair<Float, Float> {
        val metrics: DisplayMetrics = resources.displayMetrics
        val screenWidth = metrics.widthPixels.toFloat()
        val screenHeight = metrics.heightPixels.toFloat()

        // Normalized scale is 0 to 1000
        val clampedX = normalizedX.coerceIn(0, 1000)
        val clampedY = normalizedY.coerceIn(0, 1000)

        val px = (clampedX / 1000f) * screenWidth
        val py = (clampedY / 1000f) * screenHeight
        return Pair(px.coerceIn(1f, screenWidth - 1f), py.coerceIn(1f, screenHeight - 1f))
    }

    private suspend fun dispatchTap(x: Float, y: Float, durationMs: Long): Boolean =
        suspendCancellableCoroutine { continuation ->
            val path = Path().apply {
                moveTo(x, y)
            }
            val stroke = GestureDescription.StrokeDescription(path, 0L, durationMs)
            val gesture = GestureDescription.Builder().addStroke(stroke).build()

            val success = dispatchGesture(gesture, object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    Log.d(TAG, "Tap completed at ($x, $y)")
                    if (continuation.isActive) continuation.resume(true)
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    Log.w(TAG, "Tap cancelled at ($x, $y)")
                    if (continuation.isActive) continuation.resume(false)
                }
            }, null)

            if (!success && continuation.isActive) {
                Log.e(TAG, "dispatchGesture failed to enqueue tap")
                continuation.resume(false)
            }
        }

    private suspend fun dispatchSwipe(
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        durationMs: Long
    ): Boolean = suspendCancellableCoroutine { continuation ->
        val path = Path().apply {
            moveTo(startX, startY)
            lineTo(endX, endY)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0L, durationMs)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()

        val success = dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                Log.d(TAG, "Swipe completed from ($startX, $startY) to ($endX, $endY)")
                if (continuation.isActive) continuation.resume(true)
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                Log.w(TAG, "Swipe cancelled from ($startX, $startY) to ($endX, $endY)")
                if (continuation.isActive) continuation.resume(false)
            }
        }, null)

        if (!success && continuation.isActive) {
            Log.e(TAG, "dispatchGesture failed to enqueue swipe")
            continuation.resume(false)
        }
    }

    private suspend fun dispatchHold(x: Float, y: Float, durationMs: Long): Boolean =
        suspendCancellableCoroutine { continuation ->
            val path = Path().apply {
                moveTo(x, y)
            }
            val stroke = GestureDescription.StrokeDescription(path, 0L, durationMs)
            val gesture = GestureDescription.Builder().addStroke(stroke).build()

            val success = dispatchGesture(gesture, object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    Log.d(TAG, "Hold completed at ($x, $y) for ${durationMs}ms")
                    if (continuation.isActive) continuation.resume(true)
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    Log.w(TAG, "Hold cancelled at ($x, $y)")
                    if (continuation.isActive) continuation.resume(false)
                }
            }, null)

            if (!success && continuation.isActive) {
                Log.e(TAG, "dispatchGesture failed to enqueue hold")
                continuation.resume(false)
            }
        }
}

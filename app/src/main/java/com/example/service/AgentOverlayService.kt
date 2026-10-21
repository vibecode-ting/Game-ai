package com.example.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.data.model.AgentState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AgentOverlayService : Service(), LifecycleOwner, SavedStateRegistryOwner {

    companion object {
        const val ACTION_SHOW = "com.example.service.SHOW_OVERLAY"
        const val ACTION_HIDE = "com.example.service.HIDE_OVERLAY"

        private val _isOverlayActive = MutableStateFlow(false)
        val isOverlayActive: StateFlow<Boolean> = _isOverlayActive.asStateFlow()

        private val _overlayStatusText = MutableStateFlow("Agent Ready")
        val overlayStatusText: StateFlow<String> = _overlayStatusText.asStateFlow()

        private val _overlayState = MutableStateFlow(AgentState.IDLE)
        val overlayState: StateFlow<AgentState> = _overlayState.asStateFlow()

        var onTogglePauseClicked: (() -> Unit)? = null
        var onStopClicked: (() -> Unit)? = null

        fun updateStatus(state: AgentState, text: String) {
            _overlayState.value = state
            _overlayStatusText.value = text
        }
    }

    private var windowManager: WindowManager? = null
    private var overlayView: FrameLayout? = null
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SHOW -> showOverlay()
            ACTION_HIDE -> hideOverlay()
        }
        return START_NOT_STICKY
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun showOverlay() {
        if (overlayView != null) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            return
        }

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 40
            y = 120
        }

        val frame = FrameLayout(this)
        frame.setViewTreeLifecycleOwner(this)
        frame.setViewTreeSavedStateRegistryOwner(this)

        val composeView = ComposeView(this).apply {
            setContent {
                OverlayPillContent(
                    onTogglePause = { onTogglePauseClicked?.invoke() },
                    onClose = {
                        onStopClicked?.invoke()
                        hideOverlay()
                    }
                )
            }
        }

        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        frame.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager?.updateViewLayout(frame, params)
                    true
                }
                else -> false
            }
        }

        frame.addView(composeView)
        overlayView = frame

        try {
            windowManager?.addView(frame, params)
            _isOverlayActive.value = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun hideOverlay() {
        overlayView?.let {
            try {
                windowManager?.removeView(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            overlayView = null
        }
        _isOverlayActive.value = false
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        hideOverlay()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

@Composable
fun OverlayPillContent(
    onTogglePause: () -> Unit,
    onClose: () -> Unit
) {
    val state by AgentOverlayService.overlayState.collectAsState()
    val statusText by AgentOverlayService.overlayStatusText.collectAsState()

    val stateColor = when (state) {
        AgentState.ANALYZING, AgentState.EXECUTING -> Color(0xFF00E676)
        AgentState.CAPTURING -> Color(0xFF00E5FF)
        AgentState.PAUSED -> Color(0xFFFFB300)
        AgentState.ERROR -> Color(0xFFFF1744)
        else -> Color(0xFFB0BEC5)
    }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color(0xEE111827),
        shadowElevation = 8.dp,
        modifier = Modifier
            .border(1.dp, stateColor.copy(alpha = 0.6f), RoundedCornerShape(24.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(stateColor, CircleShape)
            )

            Column(modifier = Modifier.width(160.dp)) {
                Text(
                    text = "GameAI: ${state.name}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = statusText,
                    fontSize = 9.sp,
                    color = Color(0xFF94A3B8),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(Color(0xFF1E293B), CircleShape)
                    .clickable { onTogglePause() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (state == AgentState.PAUSED) Icons.Default.PlayArrow else Icons.Default.Pause,
                    contentDescription = "Toggle Agent Pause",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(Color(0xFF334155), CircleShape)
                    .clickable { onClose() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Stop Agent Overlay",
                    tint = Color(0xFFFF5252),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

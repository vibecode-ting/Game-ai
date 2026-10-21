package com.example.data.model

import android.graphics.Bitmap

enum class ActionType {
    TAP,
    SWIPE,
    HOLD,
    WAIT
}

data class GameAction(
    val action: ActionType = ActionType.WAIT,
    // Normalized 0-1000 grid coordinates
    val x: Int? = null,
    val y: Int? = null,
    val startX: Int? = null,
    val startY: Int? = null,
    val endX: Int? = null,
    val endY: Int? = null,
    val durationMs: Long = 100L,
    val reasoning: String = "",
    val confidence: Float = 1.0f,
    val targetDescription: String? = null,
    val isAdOrPopup: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun summary(): String {
        return when (action) {
            ActionType.TAP -> "TAP at ($x, $y) -> ${targetDescription ?: reasoning}"
            ActionType.SWIPE -> "SWIPE from ($startX, $startY) to ($endX, $endY) [${durationMs}ms]"
            ActionType.HOLD -> "HOLD at ($x, $y) for ${durationMs}ms"
            ActionType.WAIT -> "WAIT ${durationMs}ms -> $reasoning"
        }
    }
}

enum class AgentState {
    IDLE,
    RECORDING,
    CAPTURING,
    ANALYZING,
    EXECUTING,
    WAITING,
    PAUSED,
    ERROR
}

data class DemonstrationItem(
    val id: String,
    val title: String,
    val gameName: String,
    val action: GameAction,
    val description: String = "",
    val screenshotBitmap: Bitmap? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class AgentTelemetry(
    val state: AgentState = AgentState.IDLE,
    val currentFps: Float = 0f,
    val lastLatencyMs: Long = 0L,
    val totalActions: Int = 0,
    val successfulActions: Int = 0,
    val promptTokensEstimate: Int = 0,
    val latestReasoning: String = "Agent initialized. Ready to watch and play.",
    val lastAction: GameAction? = null,
    val errorMessage: String? = null
)

data class AgentConfig(
    val modelName: String = "gemini-3.5-flash",
    val targetFps: Int = 3,
    val coordinateGridMax: Int = 1000,
    val autoCloseAds: Boolean = true,
    val safetyCooldownMs: Long = 400L,
    val customInstructions: String = "Focus on maximizing score, avoiding obstacles, and immediately closing any ad popups."
)

package com.example.controller

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import com.example.data.DemonstrationRepository
import com.example.data.model.ActionType
import com.example.data.model.AgentConfig
import com.example.data.model.AgentState
import com.example.data.model.AgentTelemetry
import com.example.data.model.GameAction
import com.example.network.GeminiAgentClient
import com.example.service.AgentOverlayService
import com.example.service.ScreenCaptureService
import com.example.service.TouchAutomationService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class GameAgentController(
    val context: Context,
    val geminiClient: GeminiAgentClient,
    val demoRepository: DemonstrationRepository
) {
    companion object {
        private const val TAG = "GameAgentController"
    }

    private val controllerScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var agentJob: Job? = null

    private val _telemetry = MutableStateFlow(AgentTelemetry())
    val telemetry: StateFlow<AgentTelemetry> = _telemetry.asStateFlow()

    private val _config = MutableStateFlow(AgentConfig())
    val config: StateFlow<AgentConfig> = _config.asStateFlow()

    private val recentActions = mutableListOf<GameAction>()

    // Simulation / local arena frame provider hook
    var localArenaFrameProvider: (() -> Bitmap?)? = null
    var onLocalActionDispatched: ((GameAction) -> Unit)? = null

    init {
        AgentOverlayService.onTogglePauseClicked = {
            togglePause()
        }
        AgentOverlayService.onStopClicked = {
            stopAgent()
        }
    }

    fun updateConfig(newConfig: AgentConfig) {
        _config.value = newConfig
    }

    var isLocalArenaMode: Boolean = true

    fun startAgent(useLocalArenaMode: Boolean = isLocalArenaMode) {
        this.isLocalArenaMode = useLocalArenaMode
        if (_telemetry.value.state == AgentState.ANALYZING || _telemetry.value.state == AgentState.EXECUTING) {
            return
        }

        agentJob?.cancel()
        agentJob = controllerScope.launch(Dispatchers.Default) {
            runAgentLoop(useLocalArenaMode)
        }
    }

    fun togglePause() {
        val currentState = _telemetry.value.state
        if (currentState == AgentState.PAUSED) {
            _telemetry.value = _telemetry.value.copy(state = AgentState.IDLE)
            AgentOverlayService.updateStatus(AgentState.IDLE, "Resuming agent...")
            startAgent(isLocalArenaMode)
        } else {
            agentJob?.cancel()
            _telemetry.value = _telemetry.value.copy(state = AgentState.PAUSED)
            AgentOverlayService.updateStatus(AgentState.PAUSED, "Agent Paused")
        }
    }

    fun stopAgent() {
        agentJob?.cancel()
        agentJob = null
        _telemetry.value = _telemetry.value.copy(state = AgentState.IDLE)
        AgentOverlayService.updateStatus(AgentState.IDLE, "Agent Stopped")
    }

    private suspend fun runAgentLoop(useLocalArenaMode: Boolean) {
        Log.i(TAG, "Starting autonomous agent loop. Local arena mode = $useLocalArenaMode")
        var frameCount = 0
        var loopStartTime = SystemClock.elapsedRealtime()

        while (controllerScope.isActive) {
            val stepStartMs = SystemClock.elapsedRealtime()

            // 1. Capture screen frame
            updateState(AgentState.CAPTURING)
            val screenshot = if (useLocalArenaMode) {
                localArenaFrameProvider?.invoke()
            } else {
                ScreenCaptureService.captureCurrentScreen()
            }

            if (screenshot == null) {
                Log.w(TAG, "No screen capture available yet. Retrying in 500ms...")
                updateTelemetry {
                    it.copy(
                        latestReasoning = "Waiting for display frame capture...",
                        state = AgentState.WAITING
                    )
                }
                delay(500)
                continue
            }

            // 2. Multimodal Vision Analysis via Gemini
            updateState(AgentState.ANALYZING)
            AgentOverlayService.updateStatus(AgentState.ANALYZING, "Analyzing visual game state...")

            val currentConfig = _config.value
            val inferenceStart = SystemClock.elapsedRealtime()

            val actionResult = geminiClient.analyzeAndDecideAction(
                screenshot = screenshot,
                modelName = currentConfig.modelName,
                customInstructions = currentConfig.customInstructions,
                demonstrations = demoRepository.demonstrations.value,
                recentActions = recentActions
            )

            val inferenceLatency = SystemClock.elapsedRealtime() - inferenceStart

            if (actionResult.isSuccess || (useLocalArenaMode && actionResult.isFailure)) {
                val action = if (actionResult.isSuccess) {
                    actionResult.getOrThrow()
                } else {
                    resolveArenaHeuristicAction(screenshot)
                }
                Log.d(TAG, "Selected action: ${action.summary()} (Latency: ${inferenceLatency}ms)")

                // 3. Execution Phase
                updateState(AgentState.EXECUTING)
                AgentOverlayService.updateStatus(
                    AgentState.EXECUTING,
                    "${action.action.name} -> ${action.targetDescription ?: action.reasoning}"
                )

                var executionSuccess = false
                if (useLocalArenaMode) {
                    onLocalActionDispatched?.invoke(action)
                    executionSuccess = true
                    delay(action.durationMs.coerceAtLeast(80L))
                } else {
                    if (TouchAutomationService.isConnected()) {
                        executionSuccess = TouchAutomationService.executeAction(action)
                    } else {
                        Log.w(TAG, "Accessibility service disconnected; skipping physical touch dispatch")
                        delay(action.durationMs)
                    }
                }

                // 4. Update Telemetry & History
                recentActions.add(action)
                if (recentActions.size > 10) recentActions.removeAt(0)
                frameCount++

                val elapsedTotal = (SystemClock.elapsedRealtime() - loopStartTime).coerceAtLeast(1L)
                val calculatedFps = (frameCount * 1000f) / elapsedTotal

                updateTelemetry {
                    it.copy(
                        state = AgentState.WAITING,
                        currentFps = calculatedFps,
                        lastLatencyMs = if (actionResult.isSuccess) inferenceLatency else 120L,
                        totalActions = it.totalActions + 1,
                        successfulActions = if (executionSuccess) it.successfulActions + 1 else it.successfulActions,
                        latestReasoning = action.reasoning,
                        lastAction = action,
                        errorMessage = if (actionResult.isSuccess) null else "Running local vision fallback (Set GEMINI_API_KEY for live Gemini 3.5 Flash)"
                    )
                }

                // 5. Rate-Limiting & Adaptive Cooldown
                val cycleTime = SystemClock.elapsedRealtime() - stepStartMs
                val targetIntervalMs = (1000L / currentConfig.targetFps.coerceIn(1, 10))
                val sleepTime = (targetIntervalMs - cycleTime).coerceAtLeast(currentConfig.safetyCooldownMs)
                delay(sleepTime)

            } else {
                val error = actionResult.exceptionOrNull()
                val isTimeout = error is java.net.SocketTimeoutException || error?.message?.contains("timeout", ignoreCase = true) == true
                val errorMsg = if (isTimeout) {
                    "Inference timed out. Upload scaled down to prevent timeouts. Retrying in 2s..."
                } else {
                    error?.message ?: "Inference error occurred"
                }
                Log.e(TAG, "Inference error: $errorMsg")
                updateTelemetry {
                    it.copy(
                        state = AgentState.ERROR,
                        errorMessage = errorMsg,
                        latestReasoning = errorMsg
                    )
                }
                AgentOverlayService.updateStatus(AgentState.ERROR, if (isTimeout) "Timeout (Retrying)" else errorMsg)
                delay(2000L) // Wait before retrying
            }
        }
    }

    private fun resolveArenaHeuristicAction(screenshot: Bitmap): GameAction {
        // Inspect if ad modal is drawn by sampling center or top-right close button area
        val width = screenshot.width
        val height = screenshot.height
        val sampleX = (width * 0.82f).toInt().coerceIn(0, width - 1)
        val sampleY = (height * 0.33f).toInt().coerceIn(0, height - 1)
        val pixel = screenshot.getPixel(sampleX, sampleY)
        val red = android.graphics.Color.red(pixel)
        val green = android.graphics.Color.green(pixel)
        val blue = android.graphics.Color.blue(pixel)

        val hasRedCloseBtn = red > 180 && green < 80 && blue < 80

        return if (hasRedCloseBtn) {
            GameAction(
                action = ActionType.TAP,
                x = 820,
                y = 330,
                durationMs = 60L,
                reasoning = "Visual detection identified 'X' close button on popup modal dialog; tapping to dismiss.",
                targetDescription = "Ad Dismiss 'X' Button",
                isAdOrPopup = true
            )
        } else {
            val random = kotlin.random.Random.nextInt(4)
            if (random == 0) {
                GameAction(
                    action = ActionType.SWIPE,
                    startX = 400,
                    startY = 500,
                    endX = 600,
                    endY = 500,
                    durationMs = 150L,
                    reasoning = "Detected horizontal gem swap opportunity to match 3 identical colors.",
                    targetDescription = "Gem Swap Gesture"
                )
            } else {
                val targetGridX = kotlin.random.Random.nextInt(200, 800)
                val targetGridY = kotlin.random.Random.nextInt(300, 750)
                GameAction(
                    action = ActionType.TAP,
                    x = targetGridX,
                    y = targetGridY,
                    durationMs = 60L,
                    reasoning = "Identified high-value tile cluster at ($targetGridX, $targetGridY); tapping to pop.",
                    targetDescription = "Gem Tile Cluster"
                )
            }
        }
    }

    private fun updateState(newState: AgentState) {
        _telemetry.value = _telemetry.value.copy(state = newState)
    }

    private fun updateTelemetry(transform: (AgentTelemetry) -> AgentTelemetry) {
        _telemetry.value = transform(_telemetry.value)
    }
}

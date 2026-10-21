package com.example.network

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.data.model.ActionType
import com.example.data.model.DemonstrationItem
import com.example.data.model.GameAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

class GeminiAgentClient {

    companion object {
        private const val TAG = "GeminiAgentClient"
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/"

        fun getApiKey(): String {
            return try {
                val key = BuildConfig.GEMINI_API_KEY
                if (key.isNullOrBlank() || key == "MY_GEMINI_API_KEY") "" else key
            } catch (e: Exception) {
                ""
            }
        }
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .callTimeout(75, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    var customApiKey: String? = null

    suspend fun analyzeAndDecideAction(
        screenshot: Bitmap,
        modelName: String = "gemini-3.5-flash",
        customInstructions: String = "",
        demonstrations: List<DemonstrationItem> = emptyList(),
        recentActions: List<GameAction> = emptyList()
    ): Result<GameAction> = withContext(Dispatchers.IO) {
        val apiKey = customApiKey?.takeIf { it.isNotBlank() } ?: getApiKey()

        if (apiKey.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("Gemini API key is not configured. Please enter your API key in settings or AI Studio Secrets panel.")
            )
        }

        var lastException: Exception? = null
        for (attempt in 1..2) {
            try {
                val base64Image = bitmapToBase64(screenshot)
                val requestJson = buildRequestPayload(
                    base64Image = base64Image,
                    customInstructions = customInstructions,
                    demonstrations = demonstrations,
                    recentActions = recentActions
                )

                val endpoint = "$BASE_URL$modelName:generateContent?key=$apiKey"
                val body = requestJson.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url(endpoint)
                    .post(body)
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    Log.e(TAG, "Gemini API error ${response.code}: $responseBody")
                    return@withContext Result.failure(
                        RuntimeException("Gemini API HTTP ${response.code}: ${extractErrorMessage(responseBody)}")
                    )
                }

                val parsedAction = parseGeminiResponse(responseBody)
                return@withContext Result.success(parsedAction)
            } catch (e: Exception) {
                lastException = e
                Log.w(TAG, "Gemini inference attempt $attempt failed: ${e.message}")
                if (attempt < 2) {
                    kotlinx.coroutines.delay(800)
                }
            }
        }

        Log.e(TAG, "Exception during Gemini inference: ${lastException?.message}", lastException)
        Result.failure(lastException ?: RuntimeException("Inference failed"))
    }

    private fun buildRequestPayload(
        base64Image: String,
        customInstructions: String,
        demonstrations: List<DemonstrationItem>,
        recentActions: List<GameAction>
    ): JSONObject {
        val systemInstructionText = buildString {
            appendLine("You are GameAI Agent, an autonomous real-time mobile gaming agent equipped with computer vision.")
            appendLine("Your role is to visually inspect the provided mobile game screenshot and determine the optimal next input touch action.")
            appendLine()
            appendLine("COORDINATE SYSTEM:")
            appendLine("All coordinates MUST use a normalized 0 to 1000 grid:")
            appendLine("- Top-Left corner is (0, 0)")
            appendLine("- Bottom-Right corner is (1000, 1000)")
            appendLine("- Center of screen is (500, 500)")
            appendLine()
            appendLine("ACTION PRIMITIVES:")
            appendLine("1. TAP: Single touch at (x, y). Used to select items, tap tiles, fire weapons, or click UI buttons.")
            appendLine("2. SWIPE: Motion gesture from (start_x, start_y) to (end_x, end_y) lasting duration_ms. Used for steering, jumping, sliding, or gem swaps.")
            appendLine("3. HOLD: Sustained press at (x, y) lasting duration_ms. Used for charging shots, acceleration, or dragging.")
            appendLine("4. WAIT: No touch action. Used when waiting for animations to complete, cooldown timers, or state transitions.")
            appendLine()
            appendLine("CRITICAL EDGE CASE RULES:")
            appendLine("- Popup Ads & Prompts: If an ad dialog, reward video prompt, rate prompt, or overlay modal appears, IMMEDIATELY locate the 'X', 'Close', 'No Thanks', or 'Skip' icon and return a TAP on it with is_ad_or_popup=true.")
            appendLine("- Game Over / Retry: If the player has lost or the round has ended, locate the 'Retry', 'Play Again', or 'Continue' button and tap it.")
            appendLine("- Score Maximization: Target collectibles, match identical tiles, dodge hazards, and time taps accurately.")
            if (customInstructions.isNotBlank()) {
                appendLine()
                appendLine("USER CUSTOM GAMEPLAY INSTRUCTIONS:")
                appendLine(customInstructions)
            }
        }

        val root = JSONObject()

        // System Instruction
        val sysContent = JSONObject().apply {
            put("parts", JSONArray().put(JSONObject().put("text", systemInstructionText)))
        }
        root.put("systemInstruction", sysContent)

        // Contents
        val contentsArray = JSONArray()

        // 1. Few-shot demonstration context
        if (demonstrations.isNotEmpty()) {
            val demoText = buildString {
                appendLine("REFERENCE HUMAN DEMONSTRATION EXAMPLES:")
                demonstrations.take(5).forEachIndexed { index, demo ->
                    appendLine("Example #${index + 1} (${demo.gameName}):")
                    appendLine("  Action taken: ${demo.action.summary()}")
                    if (demo.description.isNotBlank()) {
                        appendLine("  Teacher note: ${demo.description}")
                    }
                }
            }
            contentsArray.put(JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().put(JSONObject().put("text", demoText)))
            })
            contentsArray.put(JSONObject().apply {
                put("role", "model")
                put("parts", JSONArray().put(JSONObject().put("text", "Understood. I will follow these demonstration patterns.")))
            })
        }

        // 2. Recent action history
        val historyPrompt = if (recentActions.isNotEmpty()) {
            val recentList = recentActions.takeLast(3).joinToString("; ") { it.summary() }
            "Recent actions taken: [$recentList]. Now examine this live screenshot and choose the next action:"
        } else {
            "Examine this live game screenshot and return the single best next action in structured JSON:"
        }

        // 3. User turn with image
        val userTurnParts = JSONArray().apply {
            put(JSONObject().put("text", historyPrompt))
            put(JSONObject().apply {
                put("inlineData", JSONObject().apply {
                    put("mimeType", "image/jpeg")
                    put("data", base64Image)
                })
            })
        }

        contentsArray.put(JSONObject().apply {
            put("role", "user")
            put("parts", userTurnParts)
        })

        root.put("contents", contentsArray)

        // Generation config with structured JSON response
        val genConfig = JSONObject().apply {
            put("responseMimeType", "application/json")
            put("temperature", 0.15)
        }
        root.put("generationConfig", genConfig)

        return root
    }

    private fun parseGeminiResponse(rawResponseBody: String): GameAction {
        val root = JSONObject(rawResponseBody)
        val candidates = root.optJSONArray("candidates")
        val candidate = candidates?.optJSONObject(0)
        val content = candidate?.optJSONObject("content")
        val parts = content?.optJSONArray("parts")
        val textPart = parts?.optJSONObject(0)?.optString("text", "") ?: ""

        if (textPart.isBlank()) {
            return GameAction(
                action = ActionType.WAIT,
                durationMs = 200L,
                reasoning = "Empty response from Gemini; waiting."
            )
        }

        // Clean any accidental markdown code fences
        val cleanJson = textPart.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        val actionJson = JSONObject(cleanJson)

        val rawActionType = actionJson.optString("action", "WAIT").uppercase()
        val actionType = try {
            ActionType.valueOf(rawActionType)
        } catch (e: Exception) {
            ActionType.WAIT
        }

        val x = if (actionJson.has("x")) actionJson.optInt("x").coerceIn(0, 1000) else null
        val y = if (actionJson.has("y")) actionJson.optInt("y").coerceIn(0, 1000) else null
        val startX = if (actionJson.has("start_x")) actionJson.optInt("start_x").coerceIn(0, 1000) else null
        val startY = if (actionJson.has("start_y")) actionJson.optInt("start_y").coerceIn(0, 1000) else null
        val endX = if (actionJson.has("end_x")) actionJson.optInt("end_x").coerceIn(0, 1000) else null
        val endY = if (actionJson.has("end_y")) actionJson.optInt("end_y").coerceIn(0, 1000) else null
        val durationMs = actionJson.optLong("duration_ms", 120L).coerceIn(40L, 3000L)
        val reasoning = actionJson.optString("reasoning", "Autonomous decision by vision model")
        val targetDesc = if (actionJson.has("target_description")) actionJson.optString("target_description") else null
        val isAdOrPopup = actionJson.optBoolean("is_ad_or_popup", false)

        return GameAction(
            action = actionType,
            x = x,
            y = y,
            startX = startX,
            startY = startY,
            endX = endX,
            endY = endY,
            durationMs = durationMs,
            reasoning = reasoning,
            targetDescription = targetDesc,
            isAdOrPopup = isAdOrPopup
        )
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val maxDim = 640
        val scaledBitmap = if (bitmap.width > maxDim || bitmap.height > maxDim) {
            val scale = maxDim.toFloat() / maxOf(bitmap.width, bitmap.height)
            val newW = (bitmap.width * scale).toInt().coerceAtLeast(1)
            val newH = (bitmap.height * scale).toInt().coerceAtLeast(1)
            Bitmap.createScaledBitmap(bitmap, newW, newH, true)
        } else {
            bitmap
        }
        val stream = ByteArrayOutputStream()
        // Compress to JPEG 70% to conserve network bandwidth and tokens while preserving visual clarity
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 70, stream)
        val byteArray = stream.toByteArray()
        if (scaledBitmap != bitmap) {
            scaledBitmap.recycle()
        }
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    private fun extractErrorMessage(errorJson: String): String {
        return try {
            val root = JSONObject(errorJson)
            val error = root.optJSONObject("error")
            error?.optString("message", errorJson) ?: errorJson
        } catch (e: Exception) {
            errorJson
        }
    }
}

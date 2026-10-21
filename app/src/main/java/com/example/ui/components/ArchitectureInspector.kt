package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ArchitectureInspector(
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Pipeline Flow", "JSON Schema", "Core Services", "Safety & Limits")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0B0F19))
    ) {
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color(0xFF131B2E),
            contentColor = Color(0xFF00E5FF),
            edgePadding = 16.dp,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = Color(0xFF00E5FF)
                )
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp
                        )
                    },
                    modifier = Modifier.testTag("arch_tab_$index")
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            when (selectedTab) {
                0 -> {
                    item { ArchitectureOverviewCard() }
                    item { LoopStepCard(1, "VirtualDisplay Capture", "ScreenCaptureService grabs RGBA_8888 bitmap with rowStride unpadding.", Icons.Default.Videocam, Color(0xFF00E5FF)) }
                    item { LoopStepCard(2, "Multimodal Inference", "JPEG base64 image + few-shot memory context sent to gemini-3.5-flash via REST.", Icons.Default.Psychology, Color(0xFFD500F9)) }
                    item { LoopStepCard(3, "Structured Action Parse", "Strict JSON schema enforces TAP, SWIPE, HOLD, WAIT on a 0-1000 coordinate grid.", Icons.Default.DataObject, Color(0xFFFFEA00)) }
                    item { LoopStepCard(4, "Touch Gesture Dispatch", "TouchAutomationService AccessibilityService dispatches precise paths via dispatchGesture.", Icons.Default.TouchApp, Color(0xFF76FF03)) }
                    item { LoopStepCard(5, "Rate Limit & Safety Delay", "Dynamic FPS throttling (2-5 FPS) + kill-switch protection.", Icons.Default.Security, Color(0xFFFF1744)) }
                }
                1 -> {
                    item { JsonSchemaCard() }
                    item { SchemaExplanationCard() }
                }
                2 -> {
                    item { ServiceCodeSnippetCard("1. ScreenCaptureService (MediaProjection)", ScreenCaptureSnippet) }
                    item { ServiceCodeSnippetCard("2. TouchAutomationService (Accessibility)", TouchAutomationSnippet) }
                    item { ServiceCodeSnippetCard("3. GeminiAgentClient (Multimodal REST)", GeminiClientSnippet) }
                    item { ServiceCodeSnippetCard("4. Autonomous Control Loop (Coroutine)", ControlLoopSnippet) }
                }
                3 -> {
                    item { SafetyAndConstraintsCard() }
                }
            }
        }
    }
}

@Composable
fun ArchitectureOverviewCard() {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131B2E)),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "SYSTEM ARCHITECTURE TOPOLOGY",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF00E5FF),
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "End-to-End Multimodal Game Playback Loop",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "The GameAI Agent operates through an autonomous pipeline that decouples capture, AI reasoning, and actuation across distinct Android system layers to achieve sub-second reaction latencies without root permissions.",
                fontSize = 12.sp,
                color = Color(0xFF94A3B8),
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
fun LoopStepCard(step: Int, title: String, description: String, icon: ImageVector, color: Color) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131B2E)),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f))
                    .border(1.dp, color, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = "STEP $step: $title", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color)
                Text(text = description, fontSize = 12.sp, color = Color.White, lineHeight = 16.sp)
            }
        }
    }
}

@Composable
fun JsonSchemaCard() {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131B2E)),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.4f), RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "STRUCTURED OUTPUT JSON SCHEMA",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF00E5FF),
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF070B13))
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = """
{
  "action": "TAP" | "SWIPE" | "HOLD" | "WAIT",
  "x": 0..1000,
  "y": 0..1000,
  "start_x": 0..1000,
  "start_y": 0..1000,
  "end_x": 0..1000,
  "end_y": 0..1000,
  "duration_ms": 50..2000,
  "reasoning": "string explaining decision",
  "target_description": "label of object",
  "is_ad_or_popup": boolean
}
                    """.trimIndent(),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = Color(0xFF76FF03),
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
fun SchemaExplanationCard() {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131B2E)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Coordinate Normalization (0 - 1000)", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Because mobile devices have diverse screen resolutions and aspect ratios (e.g. 1080x2400, 1440x3120, 720x1600), Gemini outputs relative coordinates in a 0 to 1000 bounding grid. TouchAutomationService maps (x / 1000f) * displayWidth to guarantee pixel-perfect touches on any hardware.",
                fontSize = 11.sp,
                color = Color(0xFF94A3B8),
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
fun ServiceCodeSnippetCard(title: String, snippet: String) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131B2E)),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(imageVector = Icons.Default.Code, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(18.dp))
                Text(text = title, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF070B13))
                    .padding(12.dp)
            ) {
                Text(
                    text = snippet,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = Color(0xFFE2E8F0),
                    lineHeight = 15.sp
                )
            }
        }
    }
}

@Composable
fun SafetyAndConstraintsCard() {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131B2E)),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFFF5252).copy(alpha = 0.3f), RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "SAFETY, CONSTRAINTS & EDGE CASES",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFF5252),
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            SafetyBullet("1. Ad Popups & Modal Interstitials", "Gemini vision is explicitly instructed with high priority to detect 'X', 'Close', or 'Skip' icons on interstitial overlays, returning is_ad_or_popup=true to dismiss them immediately.")
            Spacer(modifier = Modifier.height(8.dp))
            SafetyBullet("2. API Rate-Limiting & Token Cost", "Frames are compressed to 720p JPEG (80% quality). The agent defaults to 2-3 FPS with a configurable safety cooldown (400ms) to stay within Google AI Studio quota limits.")
            Spacer(modifier = Modifier.height(8.dp))
            SafetyBullet("3. Network Latency & Transient Failures", "All HTTP calls to Gemini have 30s timeouts with supervisor coroutine isolation. If an error occurs, the agent pauses for 2000ms before retrying, preventing device thrashing.")
            Spacer(modifier = Modifier.height(8.dp))
            SafetyBullet("4. Kill Switch & Hardware Safety", "Floating overlay pill and in-app pause toggles immediately cancel background coroutines, aborting ongoing gestures and freeing MediaProjection buffers.")
        }
    }
}

@Composable
fun SafetyBullet(title: String, text: String) {
    Column {
        Text(text = title, fontWeight = FontWeight.Bold, color = Color(0xFF00E5FF), fontSize = 12.sp)
        Text(text = text, fontSize = 11.sp, color = Color(0xFF94A3B8), lineHeight = 16.sp)
    }
}

private const val ScreenCaptureSnippet = """
// VirtualDisplay bitmap capture with stride alignment
val imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
val virtualDisplay = mediaProjection.createVirtualDisplay(
    "GameCapture", width, height, densityDpi,
    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
    imageReader.surface, null, null
)

imageReader.setOnImageAvailableListener({ reader ->
    val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
    val plane = image.planes[0]
    val rowPadding = plane.rowStride - plane.pixelStride * width
    val rawBitmap = Bitmap.createBitmap(
        width + rowPadding / plane.pixelStride, height, Bitmap.Config.ARGB_8888
    )
    rawBitmap.copyPixelsFromBuffer(plane.buffer)
    val finalBitmap = Bitmap.createBitmap(rawBitmap, 0, 0, width, height)
    image.close()
}, null)
"""

private const val TouchAutomationSnippet = """
class TouchAutomationService : AccessibilityService() {
    suspend fun dispatchTap(normX: Int, normY: Int): Boolean = 
        suspendCancellableCoroutine { cont ->
            val px = (normX / 1000f) * resources.displayMetrics.widthPixels
            val py = (normY / 1000f) * resources.displayMetrics.heightPixels
            val path = Path().apply { moveTo(px, py) }
            val stroke = GestureDescription.StrokeDescription(path, 0L, 80L)
            val gesture = GestureDescription.Builder().addStroke(stroke).build()
            
            dispatchGesture(gesture, object : GestureResultCallback() {
                override fun onCompleted(desc: GestureDescription?) { cont.resume(true) }
                override fun onCancelled(desc: GestureDescription?) { cont.resume(false) }
            }, null)
        }
}
"""

private const val GeminiClientSnippet = """
// Direct REST call to Gemini 3.5 Flash with structured schema
val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=" + apiKey

val payload = JSONObject().apply {
    put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", systemRules))))
    put("contents", JSONArray().put(JSONObject().apply {
        put("role", "user")
        put("parts", JSONArray().apply {
            put(JSONObject().put("text", "Analyze screen & output next move JSON"))
            put(JSONObject().put("inlineData", JSONObject().apply {
                put("mimeType", "image/jpeg")
                put("data", base64Screenshot)
            }))
        })
    }))
    put("generationConfig", JSONObject().apply {
        put("responseMimeType", "application/json")
        put("temperature", 0.15)
    })
}
"""

private const val ControlLoopSnippet = """
// Autonomous loop orchestrator
while (isActive) {
    // 1. Capture Frame
    val frame = ScreenCaptureService.captureCurrentScreen() ?: continue
    
    // 2. Multimodal Vision Analysis
    val action = geminiClient.analyzeAndDecideAction(
        screenshot = frame,
        modelName = "gemini-3.5-flash",
        demonstrations = demoBuffer.value
    ).getOrNull() ?: continue
    
    // 3. Actuate Gesture
    TouchAutomationService.executeAction(action)
    
    // 4. Rate-Limiting Delay (e.g. 333ms for 3 FPS)
    delay(calculateInterval(targetFps = 3))
}
"""

package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DemonstrationRepository
import com.example.data.model.ActionType
import com.example.data.model.GameAction
import kotlinx.coroutines.launch
import kotlin.random.Random

data class GemTile(val id: Int, val row: Int, val col: Int, val type: Int)

val GemColors = listOf(
    Color(0xFFFF1744), // Ruby Red
    Color(0xFF00E5FF), // Cyan Diamond
    Color(0xFF76FF03), // Emerald Green
    Color(0xFFFFEA00), // Topaz Yellow
    Color(0xFFD500F9)  // Amethyst Violet
)

val GemNames = listOf("Ruby", "Diamond", "Emerald", "Topaz", "Amethyst")

@Composable
fun GameArena(
    demonstrationRepository: DemonstrationRepository,
    onBitmapGenerated: (Bitmap) -> Unit,
    modifier: Modifier = Modifier,
    dispatchedAction: GameAction? = null
) {
    var score by remember { mutableIntStateOf(1450) }
    var combos by remember { mutableIntStateOf(3) }
    var showPopupAd by remember { mutableStateOf(false) }
    var isDemonstrationRecording by remember { mutableStateOf(false) }

    val gridSize = 5
    val tiles = remember {
        mutableStateListOf<GemTile>().apply {
            var idCounter = 0
            for (r in 0 until gridSize) {
                for (c in 0 until gridSize) {
                    add(GemTile(id = idCounter++, row = r, col = c, type = (r + c) % GemColors.size))
                }
            }
        }
    }

    // Touch Action Visuals (reticle, swipe arrow, reasoning banner)
    var visualTouchPoint by remember { mutableStateOf<Offset?>(null) }
    var visualSwipeStart by remember { mutableStateOf<Offset?>(null) }
    var visualSwipeEnd by remember { mutableStateOf<Offset?>(null) }
    val touchAlpha = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    // Handle incoming dispatched action from autonomous agent
    LaunchedEffect(dispatchedAction) {
        val action = dispatchedAction ?: return@LaunchedEffect
        when (action.action) {
            ActionType.TAP, ActionType.HOLD -> {
                val nx = (action.x ?: 500) / 1000f
                val ny = (action.y ?: 500) / 1000f
                visualTouchPoint = Offset(nx, ny)
                visualSwipeStart = null
                visualSwipeEnd = null
                touchAlpha.snapTo(1f)
                touchAlpha.animateTo(0f, animationSpec = tween(600, easing = FastOutSlowInEasing))

                // Check if agent clicked popup close button
                if (showPopupAd && nx >= 0.75f && ny <= 0.40f) {
                    showPopupAd = false
                    score += 200
                } else if (!showPopupAd) {
                    // Tap a tile based on coordinate
                    val col = (nx * gridSize).toInt().coerceIn(0, gridSize - 1)
                    val row = ((ny - 0.25f) / 0.55f * gridSize).toInt().coerceIn(0, gridSize - 1)
                    val index = row * gridSize + col
                    if (index in 0 until tiles.size) {
                        tiles[index] = tiles[index].copy(type = (tiles[index].type + 1) % GemColors.size)
                        score += 50
                        combos++
                    }
                }
            }
            ActionType.SWIPE -> {
                val sx = (action.startX ?: 400) / 1000f
                val sy = (action.startY ?: 500) / 1000f
                val ex = (action.endX ?: 600) / 1000f
                val ey = (action.endY ?: 500) / 1000f
                visualSwipeStart = Offset(sx, sy)
                visualSwipeEnd = Offset(ex, ey)
                visualTouchPoint = null
                touchAlpha.snapTo(1f)
                touchAlpha.animateTo(0f, animationSpec = tween(800, easing = FastOutSlowInEasing))
                score += 100
                combos++
            }
            ActionType.WAIT -> {
                // Agent is waiting
            }
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0B0F19))
    ) {
        val arenaWidthPx = constraints.maxWidth.toFloat()
        val arenaHeightPx = constraints.maxHeight.toFloat()

        // Periodically synthesize screenshot bitmap for the vision model
        LaunchedEffect(score, showPopupAd, tiles.toList()) {
            val bmp = generateArenaBitmap(
                width = 540,
                height = 720,
                score = score,
                combos = combos,
                tiles = tiles,
                hasPopup = showPopupAd
            )
            onBitmapGenerated(bmp)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Game Header HUD
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF131B2E))
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "NEON QUEST",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00E5FF),
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "SCORE: $score",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFD500F9).copy(alpha = 0.2f))
                            .border(1.dp, Color(0xFFD500F9), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${combos}x COMBO",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD500F9)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            score = 1000
                            combos = 1
                            showPopupAd = false
                        },
                        modifier = Modifier.size(32.dp).testTag("reset_game_arena")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset Arena",
                            tint = Color(0xFF94A3B8)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Game Control Bar (Simulate Ad Popup, Toggle Teacher Recording)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { showPopupAd = !showPopupAd },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (showPopupAd) Color(0xFFFF5252) else Color(0xFF1E293B)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).testTag("trigger_popup_ad_btn")
                ) {
                    Text(
                        text = if (showPopupAd) "Ad Visible (Active)" else "Spawn Ad Popup",
                        fontSize = 11.sp,
                        color = Color.White
                    )
                }

                OutlinedButton(
                    onClick = { isDemonstrationRecording = !isDemonstrationRecording },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (isDemonstrationRecording) Color(0xFFFF1744) else Color(0xFF00E5FF)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).testTag("record_demo_toggle_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.FiberManualRecord,
                        contentDescription = "Record Demo",
                        modifier = Modifier.size(12.dp),
                        tint = if (isDemonstrationRecording) Color(0xFFFF1744) else Color(0xFF00E5FF)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isDemonstrationRecording) "Recording..." else "Demonstrate Move",
                        fontSize = 11.sp
                    )
                }
            }

            if (isDemonstrationRecording) {
                Text(
                    text = "Demonstration Mode Active: Tap/Swipe any tile to record into few-shot memory!",
                    fontSize = 11.sp,
                    color = Color(0xFFFFB300),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 5x5 Game Playfield Board
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF0D1424))
                    .border(2.dp, Color(0xFF1E293B), RoundedCornerShape(20.dp))
                    .pointerInput(isDemonstrationRecording) {
                        detectTapGestures { offset ->
                            val normX = ((offset.x / size.width) * 1000).toInt()
                            val normY = ((offset.y / size.height) * 1000).toInt()

                            if (isDemonstrationRecording) {
                                demonstrationRepository.addDemonstration(
                                    title = "User Tapped Gem Tile ($normX, $normY)",
                                    gameName = "Neon Quest",
                                    action = GameAction(
                                        action = ActionType.TAP,
                                        x = normX,
                                        y = normY,
                                        durationMs = 60L,
                                        reasoning = "User manually demonstrated gem selection move",
                                        targetDescription = "Manual Gem Tap"
                                    ),
                                    description = "Direct touch demonstration captured from user gameplay."
                                )
                            }

                            // Also handle manual user play
                            val c = ((offset.x / size.width) * gridSize).toInt().coerceIn(0, gridSize - 1)
                            val r = ((offset.y / size.height) * gridSize).toInt().coerceIn(0, gridSize - 1)
                            val idx = r * gridSize + c
                            if (idx in 0 until tiles.size) {
                                tiles[idx] = tiles[idx].copy(type = (tiles[idx].type + 1) % GemColors.size)
                                score += 30
                            }
                        }
                    }
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                // Gem Grid Layout
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    for (r in 0 until gridSize) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            for (c in 0 until gridSize) {
                                val tile = tiles.getOrNull(r * gridSize + c)
                                val gemColor = if (tile != null) GemColors[tile.type] else Color.Gray

                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            Brush.radialGradient(
                                                listOf(
                                                    gemColor,
                                                    gemColor.copy(alpha = 0.5f),
                                                    Color(0xFF131B2E)
                                                )
                                            )
                                        )
                                        .border(1.5.dp, gemColor.copy(alpha = 0.8f), RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = "Gem",
                                        tint = Color.White.copy(alpha = 0.9f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Autonomous Agent Visual Touch Overlay (Canvas)
                Canvas(modifier = Modifier.fillMaxSize()) {
                    if (touchAlpha.value > 0f) {
                        visualTouchPoint?.let { pt ->
                            val cx = pt.x * size.width
                            val cy = pt.y * size.height
                            // Concentric pulsing rings
                            drawCircle(
                                color = Color(0xFF00E5FF).copy(alpha = touchAlpha.value * 0.4f),
                                radius = 45f * (2f - touchAlpha.value),
                                center = Offset(cx, cy)
                            )
                            drawCircle(
                                color = Color(0xFF00E5FF).copy(alpha = touchAlpha.value),
                                radius = 20f,
                                center = Offset(cx, cy),
                                style = Stroke(width = 4f)
                            )
                            drawCircle(
                                color = Color.White.copy(alpha = touchAlpha.value),
                                radius = 6f,
                                center = Offset(cx, cy)
                            )
                        }

                        if (visualSwipeStart != null && visualSwipeEnd != null) {
                            val sx = visualSwipeStart!!.x * size.width
                            val sy = visualSwipeStart!!.y * size.height
                            val ex = visualSwipeEnd!!.x * size.width
                            val ey = visualSwipeEnd!!.y * size.height

                            drawLine(
                                color = Color(0xFF76FF03).copy(alpha = touchAlpha.value),
                                start = Offset(sx, sy),
                                end = Offset(ex, ey),
                                strokeWidth = 8f
                            )
                            drawCircle(
                                color = Color(0xFF76FF03).copy(alpha = touchAlpha.value),
                                radius = 12f,
                                center = Offset(ex, ey)
                            )
                        }
                    }
                }

                // Popup Ad / Dialog Overlay (for testing edge-case popup dismissal)
                if (showPopupAd) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.75f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .border(1.dp, Color(0xFFFF5252).copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                                .padding(16.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "SPONSORED REWARD",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFFB300)
                                    )
                                    // Ad Close Button (Agent must locate and tap this!)
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF334155))
                                            .clickable { showPopupAd = false }
                                            .testTag("ad_close_x_button"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Close Ad",
                                            tint = Color.White
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Claim 500 Gold Coins!\n(Or tap 'X' in top-right to dismiss)",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        score += 500
                                        showPopupAd = false
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9100))
                                ) {
                                    Text("Claim Bonus", color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Real-time Reasoning Ticker
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131B2E)),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color(0xFF00E5FF), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "AI VISION AGENT REASONING",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF94A3B8),
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = dispatchedAction?.reasoning ?: "Watching arena canvas for gem combos and popups...",
                            fontSize = 11.sp,
                            color = Color.White,
                            maxLines = 2
                        )
                    }
                }
            }
        }
    }
}

// Generates a simulated Bitmap snapshot of the game arena for Gemini multimodal vision
fun generateArenaBitmap(
    width: Int,
    height: Int,
    score: Int,
    combos: Int,
    tiles: List<GemTile>,
    hasPopup: Boolean
): Bitmap {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // Background
    val bgPaint = Paint().apply { color = android.graphics.Color.rgb(11, 15, 25) }
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

    // Header Text
    val textPaint = Paint().apply {
        color = android.graphics.Color.WHITE
        textSize = 28f
        isFakeBoldText = true
    }
    canvas.drawText("SCORE: $score  COMBO: ${combos}x", 40f, 60f, textPaint)

    // Draw Grid Gems
    val gridStartX = 40f
    val gridStartY = 120f
    val cellSize = (width - 80f) / 5f
    val gemColorsAndroid = intArrayOf(
        android.graphics.Color.rgb(255, 23, 68),
        android.graphics.Color.rgb(0, 229, 255),
        android.graphics.Color.rgb(118, 255, 3),
        android.graphics.Color.rgb(255, 234, 0),
        android.graphics.Color.rgb(213, 0, 249)
    )

    val gemPaint = Paint().apply { isAntiAlias = true }
    tiles.forEach { tile ->
        val cx = gridStartX + tile.col * cellSize + cellSize / 2f
        val cy = gridStartY + tile.row * cellSize + cellSize / 2f
        gemPaint.color = gemColorsAndroid[tile.type % gemColorsAndroid.size]
        canvas.drawCircle(cx, cy, cellSize * 0.35f, gemPaint)
    }

    // Draw Popup Ad if active
    if (hasPopup) {
        val darkOverlay = Paint().apply { color = android.graphics.Color.argb(200, 0, 0, 0) }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), darkOverlay)

        val popupCard = Paint().apply { color = android.graphics.Color.rgb(30, 41, 59) }
        canvas.drawRoundRect(60f, 200f, width - 60f, 480f, 24f, 24f, popupCard)

        val adText = Paint().apply {
            color = android.graphics.Color.YELLOW
            textSize = 30f
            isFakeBoldText = true
        }
        canvas.drawText("REWARD POPUP AD", 100f, 260f, adText)

        // Close 'X' button in top-right (x ≈ 420, y ≈ 240)
        val closeBtnPaint = Paint().apply { color = android.graphics.Color.RED }
        canvas.drawCircle(width - 90f, 240f, 22f, closeBtnPaint)
        val xText = Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 24f
            isFakeBoldText = true
        }
        canvas.drawText("X", width - 98f, 248f, xText)
    }

    return bitmap
}

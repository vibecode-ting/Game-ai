package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.controller.GameAgentController
import com.example.data.model.AgentConfig
import com.example.data.model.AgentState
import com.example.network.GeminiAgentClient
import com.example.service.AgentOverlayService
import com.example.service.ScreenCaptureService
import com.example.service.TouchAutomationService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentDashboard(
    controller: GameAgentController,
    onLaunchMediaProjection: () -> Unit,
    onSwitchToArena: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val telemetry by controller.telemetry.collectAsState()
    val config by controller.config.collectAsState()
    val isCapturing by ScreenCaptureService.isCapturing.collectAsState()
    val isAccessibilityActive by TouchAutomationService.isServiceActive.collectAsState()
    val isOverlayActive by AgentOverlayService.isOverlayActive.collectAsState()

    var showConfigDialog by remember { mutableStateOf(false) }
    var customApiKeyInput by remember { mutableStateOf("") }
    var useInAppArenaMode by remember { mutableStateOf(true) }

    val hasApiKey = controller.geminiClient.customApiKey?.isNotBlank() == true ||
            GeminiAgentClient.getApiKey().isNotBlank()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0B0F19))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Hero Card: Agent Master Status
        item {
            AgentHeroStatusCard(
                state = telemetry.state,
                useInAppArenaMode = useInAppArenaMode,
                onToggleArenaMode = { useInAppArenaMode = it },
                onStart = { controller.startAgent(useInAppArenaMode) },
                onPause = { controller.togglePause() },
                onStop = { controller.stopAgent() }
            )
        }

        // System Permissions & Integration Health Chips
        item {
            Text(
                text = "SYSTEM PERMISSIONS & SERVICES",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF94A3B8),
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 1. Accessibility Service Status
                StatusBadge(
                    title = "Accessibility",
                    isActive = isAccessibilityActive,
                    icon = Icons.Default.TouchApp,
                    activeLabel = "Connected",
                    inactiveLabel = "Enable in Settings",
                    onClick = {
                        try {
                            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    },
                    modifier = Modifier.weight(1f)
                )

                // 2. MediaProjection Screen Capture Status
                StatusBadge(
                    title = "Screen Capture",
                    isActive = isCapturing,
                    icon = Icons.Default.Videocam,
                    activeLabel = "Active",
                    inactiveLabel = "Request Grant",
                    onClick = onLaunchMediaProjection,
                    modifier = Modifier.weight(1f)
                )

                // 3. Gemini API Key Status
                StatusBadge(
                    title = "Gemini API",
                    isActive = hasApiKey,
                    icon = Icons.Default.Key,
                    activeLabel = "Ready",
                    inactiveLabel = "Set Key",
                    onClick = { showConfigDialog = true },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Live Telemetry Readout Grid
        item {
            Text(
                text = "AGENT RUNTIME TELEMETRY",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF94A3B8),
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TelemetryStatCard(
                    title = "INFERENCE LATENCY",
                    value = "${telemetry.lastLatencyMs} ms",
                    accentColor = Color(0xFF00E5FF),
                    modifier = Modifier.weight(1f)
                )
                TelemetryStatCard(
                    title = "AGENT FPS",
                    value = String.format("%.1f", telemetry.currentFps),
                    accentColor = Color(0xFF76FF03),
                    modifier = Modifier.weight(1f)
                )
                TelemetryStatCard(
                    title = "ACTIONS TAKEN",
                    value = "${telemetry.totalActions}",
                    accentColor = Color(0xFFD500F9),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Latest Action & Reasoning Banner
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131B2E)),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "LATEST AGENT REASONING",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00E5FF),
                            letterSpacing = 1.sp
                        )
                        telemetry.lastAction?.let { action ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF00E5FF).copy(alpha = 0.2f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = action.action.name,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00E5FF)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = telemetry.latestReasoning,
                        fontSize = 13.sp,
                        color = Color.White,
                        lineHeight = 18.sp
                    )

                    telemetry.errorMessage?.let { error ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFF5252), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = error, fontSize = 11.sp, color = Color(0xFFFF5252))
                        }
                    }
                }
            }
        }

        // Overlay & Arena Quick Actions
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onSwitchToArena,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                    modifier = Modifier.weight(1f).testTag("go_to_arena_btn")
                ) {
                    Text("Open Interactive Arena", color = Color(0xFF00E5FF), fontSize = 12.sp)
                }

                Button(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                            context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))
                        } else {
                            val action = if (isOverlayActive) AgentOverlayService.ACTION_HIDE else AgentOverlayService.ACTION_SHOW
                            context.startService(Intent(context, AgentOverlayService::class.java).apply { this.action = action })
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isOverlayActive) Color(0xFF76FF03) else Color(0xFF1E293B)
                    ),
                    modifier = Modifier.weight(1f).testTag("toggle_overlay_btn")
                ) {
                    Text(
                        text = if (isOverlayActive) "HUD Overlay (ON)" else "Launch HUD Overlay",
                        color = if (isOverlayActive) Color.Black else Color.White,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Configuration Panel / Dialog
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131B2E)),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "AGENT TUNING & MODEL CONFIG",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF94A3B8),
                            letterSpacing = 1.sp
                        )
                        IconButton(onClick = { showConfigDialog = !showConfigDialog }, modifier = Modifier.size(28.dp)) {
                            Icon(imageVector = Icons.Default.Settings, contentDescription = null, tint = Color(0xFF00E5FF))
                        }
                    }

                    if (showConfigDialog) {
                        Spacer(modifier = Modifier.height(12.dp))

                        // Model Selection
                        var modelMenuExpanded by remember { mutableStateOf(false) }
                        val models = listOf("gemini-3.5-flash", "gemini-3.1-pro-preview", "gemini-flash-latest")

                        ExposedDropdownMenuBox(
                            expanded = modelMenuExpanded,
                            onExpandedChange = { modelMenuExpanded = !modelMenuExpanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = config.modelName,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Gemini Vision Model") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelMenuExpanded) },
                                modifier = Modifier.fillMaxWidth().menuAnchor(),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF00E5FF))
                            )
                            ExposedDropdownMenu(
                                expanded = modelMenuExpanded,
                                onDismissRequest = { modelMenuExpanded = false }
                            ) {
                                models.forEach { m ->
                                    DropdownMenuItem(
                                        text = { Text(m) },
                                        onClick = {
                                            controller.updateConfig(config.copy(modelName = m))
                                            modelMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Target FPS Slider
                        Text(
                            text = "Target Inference Frequency: ${config.targetFps} FPS",
                            fontSize = 12.sp,
                            color = Color.White
                        )
                        Slider(
                            value = config.targetFps.toFloat(),
                            onValueChange = { controller.updateConfig(config.copy(targetFps = it.toInt())) },
                            valueRange = 1f..6f,
                            steps = 4,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF00E5FF),
                                activeTrackColor = Color(0xFF00E5FF)
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Auto-close ads toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = "Auto-Close Interstitial Ads", color = Color.White, fontSize = 13.sp)
                                Text(text = "Detect and click 'X' button immediately", color = Color(0xFF94A3B8), fontSize = 10.sp)
                            }
                            Switch(
                                checked = config.autoCloseAds,
                                onCheckedChange = { controller.updateConfig(config.copy(autoCloseAds = it)) },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00E5FF))
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Custom Instructions
                        OutlinedTextField(
                            value = config.customInstructions,
                            onValueChange = { controller.updateConfig(config.copy(customInstructions = it)) },
                            label = { Text("Custom Agent Gameplay Directives") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF00E5FF))
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Custom API Key Input
                        OutlinedTextField(
                            value = customApiKeyInput,
                            onValueChange = {
                                customApiKeyInput = it
                                controller.geminiClient.customApiKey = it
                            },
                            label = { Text("Manual API Key Override") },
                            placeholder = { Text("Configured via AI Studio Secrets Panel") },
                            modifier = Modifier.fillMaxWidth().testTag("api_key_override_input"),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF00E5FF))
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AgentHeroStatusCard(
    state: AgentState,
    useInAppArenaMode: Boolean,
    onToggleArenaMode: (Boolean) -> Unit,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit
) {
    val isActive = state == AgentState.ANALYZING || state == AgentState.EXECUTING || state == AgentState.CAPTURING

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131B2E)),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 2.dp,
                brush = Brush.horizontalGradient(
                    if (isActive) listOf(Color(0xFF00E5FF), Color(0xFF76FF03))
                    else listOf(Color(0xFF1E293B), Color(0xFF334155))
                ),
                shape = RoundedCornerShape(20.dp)
            )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(
                                when (state) {
                                    AgentState.ANALYZING, AgentState.EXECUTING -> Color(0xFF76FF03)
                                    AgentState.CAPTURING -> Color(0xFF00E5FF)
                                    AgentState.PAUSED -> Color(0xFFFFB300)
                                    AgentState.ERROR -> Color(0xFFFF1744)
                                    else -> Color(0xFF64748B)
                                }
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "AGENT: ${state.name}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // Mode Selector Toggle
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0B0F19))
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (useInAppArenaMode) Color(0xFF00E5FF) else Color.Transparent)
                            .clickable { onToggleArenaMode(true) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "In-App Arena",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (useInAppArenaMode) Color.Black else Color(0xFF94A3B8)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (!useInAppArenaMode) Color(0xFF00E5FF) else Color.Transparent)
                            .clickable { onToggleArenaMode(false) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "External Game",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (!useInAppArenaMode) Color.Black else Color(0xFF94A3B8)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Master Control Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!isActive) {
                    Button(
                        onClick = onStart,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                        modifier = Modifier.weight(2f).height(50.dp).testTag("master_start_agent_btn")
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Start Autonomous Agent", color = Color.Black, fontWeight = FontWeight.ExtraBold)
                    }
                } else {
                    Button(
                        onClick = onPause,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300)),
                        modifier = Modifier.weight(1f).height(50.dp).testTag("master_pause_agent_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Pause, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Pause", color = Color.Black, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onStop,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252)),
                        modifier = Modifier.weight(1f).height(50.dp).testTag("master_stop_agent_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Stop, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Kill Switch", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun StatusBadge(
    title: String,
    isActive: Boolean,
    icon: ImageVector,
    activeLabel: String,
    inactiveLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131B2E)),
        modifier = modifier
            .border(
                1.dp,
                if (isActive) Color(0xFF76FF03).copy(alpha = 0.5f) else Color(0xFFFFB300).copy(alpha = 0.4f),
                RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isActive) Color(0xFF76FF03) else Color(0xFFFFB300),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                fontSize = 10.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (isActive) activeLabel else inactiveLabel,
                fontSize = 9.sp,
                color = if (isActive) Color(0xFF76FF03) else Color(0xFFFFB300),
                maxLines = 1
            )
        }
    }
}

@Composable
fun TelemetryStatCard(
    title: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131B2E)),
        modifier = modifier
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(14.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF94A3B8),
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = accentColor,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

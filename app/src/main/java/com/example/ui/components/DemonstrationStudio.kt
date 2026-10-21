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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TouchApp
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DemonstrationRepository
import com.example.data.model.ActionType
import com.example.data.model.DemonstrationItem
import com.example.data.model.GameAction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DemonstrationStudio(
    demonstrationRepository: DemonstrationRepository,
    modifier: Modifier = Modifier
) {
    val demos by demonstrationRepository.demonstrations.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var newTitle by remember { mutableStateOf("") }
    var newGameName by remember { mutableStateOf("") }
    var selectedActionType by remember { mutableStateOf(ActionType.TAP) }
    var coordX by remember { mutableStateOf("500") }
    var coordY by remember { mutableStateOf("650") }
    var durationMs by remember { mutableStateOf("80") }
    var newReasoning by remember { mutableStateOf("") }
    var actionTypeMenuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0B0F19))
            .padding(16.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "FEW-SHOT LEARNING BUFFER",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00E5FF),
                    letterSpacing = 1.sp
                )
                Text(
                    text = "${demos.size} Active Demonstrations",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = { demonstrationRepository.restoreDefaults() },
                    modifier = Modifier.testTag("restore_demos_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Restore Defaults",
                        tint = Color(0xFF94A3B8)
                    )
                }

                Button(
                    onClick = { showAddDialog = !showAddDialog },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("add_demonstration_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Demo",
                        tint = Color(0xFF0B0F19),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (showAddDialog) "Close Form" else "New Demo",
                        color = Color(0xFF0B0F19),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Info Banner
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131B2E)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = Color(0xFF00E5FF),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "These recorded actions are formatted as few-shot multimodal examples in Gemini prompt turns, enabling in-context rule learning.",
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                )
            }
        }

        // Add Demonstration Form (Collapsible)
        if (showAddDialog) {
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131B2E)),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .padding(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Teach Agent a New Rule",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = newTitle,
                            onValueChange = { newTitle = it },
                            label = { Text("Title / Action Name") },
                            modifier = Modifier.weight(1f).testTag("demo_title_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF00E5FF),
                                focusedLabelColor = Color(0xFF00E5FF)
                            )
                        )

                        OutlinedTextField(
                            value = newGameName,
                            onValueChange = { newGameName = it },
                            label = { Text("Game Name") },
                            modifier = Modifier.weight(1f).testTag("demo_game_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF00E5FF),
                                focusedLabelColor = Color(0xFF00E5FF)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ExposedDropdownMenuBox(
                            expanded = actionTypeMenuExpanded,
                            onExpandedChange = { actionTypeMenuExpanded = !actionTypeMenuExpanded },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = selectedActionType.name,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Action Type") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = actionTypeMenuExpanded) },
                                modifier = Modifier.menuAnchor(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF00E5FF)
                                )
                            )
                            ExposedDropdownMenu(
                                expanded = actionTypeMenuExpanded,
                                onDismissRequest = { actionTypeMenuExpanded = false }
                            ) {
                                ActionType.values().forEach { type ->
                                    DropdownMenuItem(
                                        text = { Text(type.name) },
                                        onClick = {
                                            selectedActionType = type
                                            actionTypeMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = coordX,
                            onValueChange = { coordX = it },
                            label = { Text("Grid X (0-1000)") },
                            modifier = Modifier.weight(1f).testTag("demo_x_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF00E5FF)
                            )
                        )

                        OutlinedTextField(
                            value = coordY,
                            onValueChange = { coordY = it },
                            label = { Text("Grid Y (0-1000)") },
                            modifier = Modifier.weight(1f).testTag("demo_y_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF00E5FF)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = newReasoning,
                        onValueChange = { newReasoning = it },
                        label = { Text("Teacher Reasoning / Gameplay Logic Note") },
                        modifier = Modifier.fillMaxWidth().testTag("demo_reasoning_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00E5FF)
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            val xVal = coordX.toIntOrNull() ?: 500
                            val yVal = coordY.toIntOrNull() ?: 500
                            val dur = durationMs.toLongOrNull() ?: 80L

                            demonstrationRepository.addDemonstration(
                                title = newTitle.ifBlank { "Custom Move (${selectedActionType.name})" },
                                gameName = newGameName.ifBlank { "Custom Game" },
                                action = GameAction(
                                    action = selectedActionType,
                                    x = xVal,
                                    y = yVal,
                                    durationMs = dur,
                                    reasoning = newReasoning.ifBlank { "Human demonstrated rule" },
                                    targetDescription = "Target at ($xVal, $yVal)"
                                ),
                                description = newReasoning
                            )
                            showAddDialog = false
                            newTitle = ""
                            newReasoning = ""
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF76FF03)),
                        modifier = Modifier.fillMaxWidth().testTag("save_demo_button")
                    ) {
                        Text("Save to Few-Shot Buffer", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Demonstrations List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(demos, key = { it.id }) { item ->
                DemonstrationCard(
                    item = item,
                    onDelete = { demonstrationRepository.removeDemonstration(item.id) }
                )
            }
        }
    }
}

@Composable
fun DemonstrationCard(
    item: DemonstrationItem,
    onDelete: () -> Unit
) {
    val actionColor = when (item.action.action) {
        ActionType.TAP -> Color(0xFF00E5FF)
        ActionType.SWIPE -> Color(0xFF76FF03)
        ActionType.HOLD -> Color(0xFFFFEA00)
        ActionType.WAIT -> Color(0xFFD500F9)
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131B2E)),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(actionColor.copy(alpha = 0.2f))
                    .border(1.dp, actionColor, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.TouchApp,
                    contentDescription = null,
                    tint = actionColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = item.title,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 13.sp
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF1E293B))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = item.gameName,
                            fontSize = 9.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = item.action.summary(),
                    fontSize = 11.sp,
                    color = actionColor,
                    fontWeight = FontWeight.SemiBold
                )

                if (item.description.isNotBlank()) {
                    Text(
                        text = item.description,
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8),
                        maxLines = 2
                    )
                }
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp).testTag("delete_demo_${item.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Demo",
                    tint = Color(0xFF64748B)
                )
            }
        }
    }
}

package com.example

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Architecture
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.controller.GameAgentController
import com.example.data.DemonstrationRepository
import com.example.data.model.GameAction
import com.example.network.GeminiAgentClient
import com.example.service.ScreenCaptureService
import com.example.ui.components.AgentDashboard
import com.example.ui.components.ArchitectureInspector
import com.example.ui.components.DemonstrationStudio
import com.example.ui.components.GameArena
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private lateinit var geminiClient: GeminiAgentClient
    private lateinit var demoRepository: DemonstrationRepository
    private lateinit var agentController: GameAgentController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        geminiClient = GeminiAgentClient()
        demoRepository = DemonstrationRepository()
        agentController = GameAgentController(this, geminiClient, demoRepository)

        setContent {
            MyApplicationTheme(darkTheme = true) {
                MainScreen(
                    controller = agentController,
                    demoRepository = demoRepository
                )
            }
        }
    }
}

@Composable
fun MainScreen(
    controller: GameAgentController,
    demoRepository: DemonstrationRepository
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var latestArenaBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var latestDispatchedAction by remember { mutableStateOf<GameAction?>(null) }

    // Connect local arena hooks to controller
    LaunchedEffect(Unit) {
        controller.localArenaFrameProvider = { latestArenaBitmap }
        controller.onLocalActionDispatched = { action ->
            latestDispatchedAction = action
        }
    }

    // MediaProjection permission launcher
    val mediaProjectionManager = remember {
        context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    }

    val mediaProjectionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val serviceIntent = Intent(context, ScreenCaptureService::class.java).apply {
                action = ScreenCaptureService.ACTION_START
                putExtra(ScreenCaptureService.EXTRA_RESULT_CODE, result.resultCode)
                putExtra(ScreenCaptureService.EXTRA_RESULT_DATA, result.data)
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
            Toast.makeText(context, "Screen capture service started", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "MediaProjection permission was cancelled", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color(0xFF0B0F19),
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF131B2E),
                contentColor = Color(0xFF00E5FF)
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                    label = { Text("Hub", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF00E5FF),
                        selectedTextColor = Color(0xFF00E5FF),
                        indicatorColor = Color(0xFF00E5FF).copy(alpha = 0.15f),
                        unselectedIconColor = Color(0xFF64748B),
                        unselectedTextColor = Color(0xFF64748B)
                    ),
                    modifier = Modifier.testTag("nav_tab_hub")
                )

                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Gamepad, contentDescription = "Game Arena") },
                    label = { Text("Arena", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF76FF03),
                        selectedTextColor = Color(0xFF76FF03),
                        indicatorColor = Color(0xFF76FF03).copy(alpha = 0.15f),
                        unselectedIconColor = Color(0xFF64748B),
                        unselectedTextColor = Color(0xFF64748B)
                    ),
                    modifier = Modifier.testTag("nav_tab_arena")
                )

                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.School, contentDescription = "Few-Shot Memory") },
                    label = { Text("Few-Shot", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFFFFEA00),
                        selectedTextColor = Color(0xFFFFEA00),
                        indicatorColor = Color(0xFFFFEA00).copy(alpha = 0.15f),
                        unselectedIconColor = Color(0xFF64748B),
                        unselectedTextColor = Color(0xFF64748B)
                    ),
                    modifier = Modifier.testTag("nav_tab_demos")
                )

                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.Architecture, contentDescription = "Architecture") },
                    label = { Text("Arch & Code", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFFD500F9),
                        selectedTextColor = Color(0xFFD500F9),
                        indicatorColor = Color(0xFFD500F9).copy(alpha = 0.15f),
                        unselectedIconColor = Color(0xFF64748B),
                        unselectedTextColor = Color(0xFF64748B)
                    ),
                    modifier = Modifier.testTag("nav_tab_arch")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFF0B0F19))
        ) {
            when (selectedTab) {
                0 -> AgentDashboard(
                    controller = controller,
                    onLaunchMediaProjection = {
                        mediaProjectionLauncher.launch(mediaProjectionManager.createScreenCaptureIntent())
                    },
                    onSwitchToArena = { selectedTab = 1 }
                )
                1 -> GameArena(
                    demonstrationRepository = demoRepository,
                    onBitmapGenerated = { bitmap ->
                        latestArenaBitmap = bitmap
                    },
                    dispatchedAction = latestDispatchedAction
                )
                2 -> DemonstrationStudio(
                    demonstrationRepository = demoRepository
                )
                3 -> ArchitectureInspector()
            }
        }
    }
}

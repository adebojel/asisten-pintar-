package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.AutoMode
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.AssistantViewModel
import com.example.ui.screens.AppsScreen
import com.example.ui.screens.AssistantConsoleScreen
import com.example.ui.screens.LogsScreen
import com.example.ui.screens.PermissionsGuideScreen
import com.example.ui.screens.QuickControlsScreen
import com.example.ui.screens.RoutinesScreen
import com.example.ui.screens.WhatsAppVoiceAssistantScreen
import com.example.ui.theme.MyApplicationTheme

enum class MainTab(val titleRes: Int, val icon: ImageVector, val tag: String) {
    CONTROLS(R.string.tab_controls, Icons.Default.Tune, "nav_controls"),
    ASSISTANT(R.string.tab_assistant, Icons.Default.Mic, "nav_assistant"),
    APPS(R.string.tab_apps, Icons.Default.Apps, "nav_apps"),
    WHATSAPP(R.string.tab_whatsapp, Icons.Default.RecordVoiceOver, "nav_whatsapp"),
    ROUTINES(R.string.tab_routines, Icons.Default.AutoMode, "nav_routines"),
    GUIDE(R.string.tab_guide, Icons.Default.Security, "nav_guide")
}

class MainActivity : ComponentActivity() {

    private val viewModel: AssistantViewModel by viewModels()
    private val requestedTab = mutableStateOf<MainTab?>(null)

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.action == android.content.Intent.ACTION_ASSIST) {
            requestedTab.value = MainTab.ASSISTANT
            viewModel.startVoiceListening()
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val isAssistLaunch = intent?.action == android.content.Intent.ACTION_ASSIST
        if (isAssistLaunch) {
            requestedTab.value = MainTab.ASSISTANT
            viewModel.startVoiceListening()
        }

        setContent {
            MyApplicationTheme {
                var currentTab by rememberSaveable {
                    mutableStateOf(if (isAssistLaunch) MainTab.ASSISTANT else MainTab.CONTROLS)
                }

                // If launched via Assist or New Intent, switch tab
                androidx.compose.runtime.LaunchedEffect(requestedTab.value) {
                    requestedTab.value?.let {
                        currentTab = it
                        requestedTab.value = null
                    }
                }

                val deviceStatus by viewModel.deviceStatus.collectAsStateWithLifecycle()
                val messages by viewModel.messages.collectAsStateWithLifecycle()
                val routines by viewModel.routines.collectAsStateWithLifecycle()
                val logs by viewModel.logs.collectAsStateWithLifecycle()
                val whatsappAlerts by viewModel.whatsappAlerts.collectAsStateWithLifecycle()
                val isReadMessages by viewModel.isReadWhatsAppMessages.collectAsStateWithLifecycle()
                val isReadCallers by viewModel.isReadWhatsAppCallers.collectAsStateWithLifecycle()
                val isTtsEnabled by viewModel.isTtsEnabled.collectAsStateWithLifecycle()
                val installedApps by viewModel.installedApps.collectAsStateWithLifecycle()

                // Support BackHandler to return to Controls tab if on secondary tab
                BackHandler(enabled = currentTab != MainTab.CONTROLS) {
                    currentTab = MainTab.CONTROLS
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = MaterialTheme.colorScheme.background,
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    text = stringResource(R.string.app_name),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.background,
                                titleContentColor = MaterialTheme.colorScheme.onBackground
                            ),
                            actions = {
                                Box(
                                    modifier = Modifier
                                        .padding(end = 12.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (deviceStatus.isWifiEnabled || deviceStatus.isMobileDataConnected)
                                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                                            else
                                                MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = if (deviceStatus.isWifiEnabled) "Wi-Fi Aktif"
                                        else if (deviceStatus.isMobileDataConnected) "Data Aktif"
                                        else "Offline",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (deviceStatus.isWifiEnabled || deviceStatus.isMobileDataConnected)
                                            MaterialTheme.colorScheme.secondary
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        )
                    },
                    bottomBar = {
                        NavigationBar(
                            modifier = Modifier
                                .windowInsetsPadding(WindowInsets.navigationBars)
                                .testTag("main_navigation_bar"),
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 6.dp
                        ) {
                            MainTab.values().forEach { tab ->
                                val selected = currentTab == tab
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { currentTab = tab },
                                    icon = {
                                        Icon(
                                            imageVector = tab.icon,
                                            contentDescription = stringResource(tab.titleRes),
                                            modifier = Modifier.size(22.dp)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = stringResource(tab.titleRes),
                                            fontSize = 10.sp,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                            maxLines = 1
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    modifier = Modifier.testTag(tab.tag)
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    when (currentTab) {
                        MainTab.CONTROLS -> {
                            QuickControlsScreen(
                                viewModel = viewModel,
                                deviceStatus = deviceStatus,
                                paddingValues = innerPadding,
                                onNavigateToGuide = { currentTab = MainTab.GUIDE }
                            )
                        }
                        MainTab.ASSISTANT -> {
                            AssistantConsoleScreen(
                                viewModel = viewModel,
                                messages = messages,
                                isTtsEnabled = isTtsEnabled,
                                paddingValues = innerPadding
                            )
                        }
                        MainTab.APPS -> {
                            AppsScreen(
                                viewModel = viewModel,
                                apps = installedApps,
                                paddingValues = innerPadding
                            )
                        }
                        MainTab.WHATSAPP -> {
                            WhatsAppVoiceAssistantScreen(
                                viewModel = viewModel,
                                alerts = whatsappAlerts,
                                isReadMessages = isReadMessages,
                                isReadCallers = isReadCallers,
                                paddingValues = innerPadding
                            )
                        }
                        MainTab.ROUTINES -> {
                            RoutinesScreen(
                                viewModel = viewModel,
                                routines = routines,
                                paddingValues = innerPadding
                            )
                        }
                        MainTab.GUIDE -> {
                            PermissionsGuideScreen(
                                viewModel = viewModel,
                                deviceStatus = deviceStatus,
                                paddingValues = innerPadding
                            )
                        }
                    }
                }
            }
        }
    }
}

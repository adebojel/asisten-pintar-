package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.AutoMode
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicNone
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.AssistantViewModel
import com.example.ui.components.ActiveVoiceListeningBanner
import com.example.ui.screens.AppUpdateScreen
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
                var showUpdateScreen by rememberSaveable { mutableStateOf(false) }

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
                val isAutoMicEnabled by viewModel.isAutoMicOnLaunchEnabled.collectAsStateWithLifecycle()
                val isVoiceListening by viewModel.isVoiceListening.collectAsStateWithLifecycle()
                val recognizedVoiceText by viewModel.recognizedVoiceText.collectAsStateWithLifecycle()

                val audioPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    if (isGranted) {
                        viewModel.startVoiceListening()
                    }
                }

                // Otomatis aktifkan mikrofon saat aplikasi dibuka jika preferensi aktif
                LaunchedEffect(Unit) {
                    if (viewModel.isAutoMicOnLaunchEnabled.value) {
                        val hasPermission = ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED
                        if (hasPermission) {
                            viewModel.startVoiceListening()
                        } else {
                            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                }

                // Support BackHandler to return to Controls tab or close update screen
                BackHandler(enabled = showUpdateScreen || currentTab != MainTab.CONTROLS) {
                    if (showUpdateScreen) {
                        showUpdateScreen = false
                    } else {
                        currentTab = MainTab.CONTROLS
                    }
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
                                IconButton(
                                    onClick = { showUpdateScreen = true },
                                    modifier = Modifier.testTag("topbar_update_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudSync,
                                        contentDescription = "Pembaruan Studio",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        if (isVoiceListening) {
                                            viewModel.stopVoiceListening()
                                        } else {
                                            val hasPermission = ContextCompat.checkSelfPermission(
                                                this@MainActivity,
                                                Manifest.permission.RECORD_AUDIO
                                            ) == PackageManager.PERMISSION_GRANTED
                                            if (hasPermission) {
                                                viewModel.startVoiceListening()
                                            } else {
                                                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                            }
                                        }
                                    },
                                    modifier = Modifier.testTag("topbar_mic_toggle_button")
                                ) {
                                    Icon(
                                        imageVector = if (isVoiceListening) Icons.Default.Mic else Icons.Default.MicNone,
                                        contentDescription = if (isVoiceListening) "Hentikan Mikrofon" else "Mulai Bicara",
                                        tint = if (isVoiceListening) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

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
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        ActiveVoiceListeningBanner(
                            isListening = isVoiceListening,
                            recognizedText = recognizedVoiceText,
                            onStopListening = { viewModel.stopVoiceListening() },
                            onOpenAssistantTab = { currentTab = MainTab.ASSISTANT }
                        )

                        Box(modifier = Modifier.weight(1f)) {
                            if (showUpdateScreen) {
                                AppUpdateScreen(
                                    viewModel = viewModel,
                                    paddingValues = PaddingValues(0.dp),
                                    onNavigateBack = { showUpdateScreen = false }
                                )
                            } else {
                                when (currentTab) {
                                    MainTab.CONTROLS -> {
                                        QuickControlsScreen(
                                            viewModel = viewModel,
                                            deviceStatus = deviceStatus,
                                            paddingValues = PaddingValues(0.dp),
                                            onNavigateToGuide = { currentTab = MainTab.GUIDE },
                                            onNavigateToUpdate = { showUpdateScreen = true }
                                        )
                                    }
                                    MainTab.ASSISTANT -> {
                                        AssistantConsoleScreen(
                                            viewModel = viewModel,
                                            messages = messages,
                                            isTtsEnabled = isTtsEnabled,
                                            paddingValues = PaddingValues(0.dp)
                                        )
                                    }
                                    MainTab.APPS -> {
                                        AppsScreen(
                                            viewModel = viewModel,
                                            apps = installedApps,
                                            paddingValues = PaddingValues(0.dp)
                                        )
                                    }
                                    MainTab.WHATSAPP -> {
                                        WhatsAppVoiceAssistantScreen(
                                            viewModel = viewModel,
                                            alerts = whatsappAlerts,
                                            isReadMessages = isReadMessages,
                                            isReadCallers = isReadCallers,
                                            paddingValues = PaddingValues(0.dp)
                                        )
                                    }
                                    MainTab.ROUTINES -> {
                                        RoutinesScreen(
                                            viewModel = viewModel,
                                            routines = routines,
                                            paddingValues = PaddingValues(0.dp)
                                        )
                                    }
                                    MainTab.GUIDE -> {
                                        PermissionsGuideScreen(
                                            viewModel = viewModel,
                                            deviceStatus = deviceStatus,
                                            paddingValues = PaddingValues(0.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

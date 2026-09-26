package com.example.ui.screens

import android.media.AudioManager
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NetworkCell
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.control.DeviceStatus
import com.example.ui.AssistantViewModel
import com.example.ui.components.StatusHeaderCard

@Composable
fun QuickControlsScreen(
    viewModel: AssistantViewModel,
    deviceStatus: DeviceStatus,
    paddingValues: PaddingValues,
    onNavigateToGuide: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            StatusHeaderCard(
                deviceStatus = deviceStatus,
                onRefresh = { viewModel.onRefreshStatus() }
            )
        }

        // Section: Konektivitas (Wi-Fi & Data)
        item {
            Text(
                text = "KONTROL JARINGAN & KONEKSI",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )
        }

        // Wi-Fi Card
        item {
            WifiControlCard(
                isWifiEnabled = deviceStatus.isWifiEnabled,
                isWifiConnected = deviceStatus.isWifiConnected,
                onToggleWifi = { enable -> viewModel.onToggleWifi(enable) },
                onOpenPanel = { viewModel.onOpenInternetPanel() },
                onOpenSettings = { viewModel.onOpenWifiSettings() }
            )
        }

        // Mobile Data Card
        item {
            MobileDataControlCard(
                isMobileDataConnected = deviceStatus.isMobileDataConnected,
                onOpenPanel = { viewModel.onOpenInternetPanel() },
                onOpenSettings = { viewModel.onOpenMobileDataSettings() }
            )
        }

        // Section: Perangkat & Hardware
        item {
            Text(
                text = "PERANGKAT & SUARA",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )
        }

        // Flashlight & Sound Row
        item {
            FlashlightControlCard(
                isTorchOn = deviceStatus.isTorchOn,
                onToggle = { viewModel.onToggleFlashlight() }
            )
        }

        item {
            SoundModeControlCard(
                currentMode = deviceStatus.ringerMode,
                onSelectMode = { mode -> viewModel.onSetRingerMode(mode) }
            )
        }

        // Section: Sistem & Aksesibilitas
        item {
            Text(
                text = "KENDALI SISTEM HP (AKSESIBILITAS)",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )
        }

        item {
            SystemAccessibilityCard(
                isAccessibilityEnabled = deviceStatus.isAccessibilityEnabled,
                onLock = { viewModel.onLockScreen() },
                onQuickSettings = { viewModel.onOpenQuickSettings() },
                onScreenshot = { viewModel.onTakeScreenshot() },
                onOpenAccessibilitySettings = { viewModel.onOpenAccessibilitySettings() },
                onNavigateToGuide = onNavigateToGuide
            )
        }

        // Section: Pintasan Cepat
        item {
            Text(
                text = "PINTASAN SISTEM LAINNYA",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )
        }

        item {
            QuickShortcutsGrid(
                onBluetooth = { viewModel.onOpenBluetooth() },
                onBatterySaver = { viewModel.onOpenBatterySaver() },
                onAirplane = { viewModel.onOpenAirplaneMode() },
                onHotspot = { viewModel.onOpenHotspot() }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun WifiControlCard(
    isWifiEnabled: Boolean,
    isWifiConnected: Boolean,
    onToggleWifi: (Boolean) -> Unit,
    onOpenPanel: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("wifi_control_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(
                                if (isWifiEnabled)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surfaceVariant
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isWifiEnabled) Icons.Default.Wifi else Icons.Default.WifiOff,
                            contentDescription = "Wi-Fi",
                            tint = if (isWifiEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Wi-Fi",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = when {
                                isWifiConnected -> "Terhubung ke Jaringan"
                                isWifiEnabled -> "Aktif (Mencari Jaringan)"
                                else -> "Dinonaktifkan"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isWifiEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Switch(
                    checked = isWifiEnabled,
                    onCheckedChange = { onToggleWifi(it) },
                    modifier = Modifier.testTag("wifi_toggle_switch"),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onOpenPanel,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("wifi_internet_panel_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Panel Internet", fontSize = 13.sp)
                }

                OutlinedButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.testTag("wifi_settings_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Setelan", fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun MobileDataControlCard(
    isMobileDataConnected: Boolean,
    onOpenPanel: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("mobile_data_control_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(
                                if (isMobileDataConnected)
                                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                                else
                                    MaterialTheme.colorScheme.surfaceVariant
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NetworkCell,
                            contentDescription = "Data Seluler",
                            tint = if (isMobileDataConnected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Data Seluler",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isMobileDataConnected) "Aktif Mengalirkan Data" else "Siap Diatur",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isMobileDataConnected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Explanation note on Android security
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Android mengamankan switch data via Panel Koneksi Instan 1-ketuk.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onOpenPanel,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("open_internet_connectivity_panel_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Sakelar Data Seluler", fontSize = 13.sp)
                }

                OutlinedButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.testTag("mobile_data_settings_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Jaringan", fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun FlashlightControlCard(
    isTorchOn: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("flashlight_control_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(
                            if (isTorchOn)
                                Color(0xFFF59E0B).copy(alpha = 0.25f)
                            else
                                MaterialTheme.colorScheme.surfaceVariant
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isTorchOn) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
                        contentDescription = "Senter",
                        tint = if (isTorchOn) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Senter Ponsel",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isTorchOn) "Menyala (Flash Hardware)" else "Padam",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isTorchOn) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Switch(
                checked = isTorchOn,
                onCheckedChange = { onToggle() },
                modifier = Modifier.testTag("flashlight_toggle_switch"),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFFF59E0B)
                )
            )
        }
    }
}

@Composable
private fun SoundModeControlCard(
    currentMode: Int,
    onSelectMode: (Int) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("sound_mode_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Mode Profil Suara",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SoundModeOption(
                    title = "Normal",
                    icon = Icons.Default.Notifications,
                    isSelected = currentMode == AudioManager.RINGER_MODE_NORMAL,
                    onClick = { onSelectMode(AudioManager.RINGER_MODE_NORMAL) },
                    modifier = Modifier.weight(1f)
                )
                SoundModeOption(
                    title = "Getar",
                    icon = Icons.Default.Vibration,
                    isSelected = currentMode == AudioManager.RINGER_MODE_VIBRATE,
                    onClick = { onSelectMode(AudioManager.RINGER_MODE_VIBRATE) },
                    modifier = Modifier.weight(1f)
                )
                SoundModeOption(
                    title = "Senyap",
                    icon = Icons.Default.NotificationsOff,
                    isSelected = currentMode == AudioManager.RINGER_MODE_SILENT,
                    onClick = { onSelectMode(AudioManager.RINGER_MODE_SILENT) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SoundModeOption(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        label = "soundModeBg"
    )
    val contentColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = contentColor,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = contentColor
            )
        }
    }
}

@Composable
private fun SystemAccessibilityCard(
    isAccessibilityEnabled: Boolean,
    onLock: () -> Unit,
    onQuickSettings: () -> Unit,
    onScreenshot: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onNavigateToGuide: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("system_accessibility_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Aksi Sistem Cepat",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (isAccessibilityEnabled)
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                            else
                                MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isAccessibilityEnabled) "Aksesibilitas Aktif" else "Perlu Izin",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isAccessibilityEnabled) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = onLock,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("lock_screen_action_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Lock, contentDescription = "Kunci Layar")
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Kunci HP", fontSize = 11.sp)
                    }
                }

                FilledTonalButton(
                    onClick = onQuickSettings,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_settings_action_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Tune, contentDescription = "Setelan Cepat")
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Setelan Cepat", fontSize = 11.sp)
                    }
                }

                FilledTonalButton(
                    onClick = onScreenshot,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("screenshot_action_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Icon(imageVector = Icons.Default.PhotoCamera, contentDescription = "Screenshot")
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Screenshot", fontSize = 11.sp)
                    }
                }
            }

            if (!isAccessibilityEnabled) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onOpenAccessibilitySettings,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("enable_accessibility_button"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Aktifkan Layanan Asisten di Aksesibilitas", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun QuickShortcutsGrid(
    onBluetooth: () -> Unit,
    onBatterySaver: () -> Unit,
    onAirplane: () -> Unit,
    onHotspot: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ShortcutItem(
            title = "Bluetooth",
            icon = Icons.Default.Bluetooth,
            onClick = onBluetooth,
            modifier = Modifier.weight(1f)
        )
        ShortcutItem(
            title = "Hemat Daya",
            icon = Icons.Default.BatteryChargingFull,
            onClick = onBatterySaver,
            modifier = Modifier.weight(1f)
        )
        ShortcutItem(
            title = "Pesawat",
            icon = Icons.Default.AirplanemodeActive,
            onClick = onAirplane,
            modifier = Modifier.weight(1f)
        )
        ShortcutItem(
            title = "Hotspot",
            icon = Icons.Default.WifiTethering,
            onClick = onHotspot,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ShortcutItem(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

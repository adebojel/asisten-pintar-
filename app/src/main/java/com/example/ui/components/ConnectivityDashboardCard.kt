package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.SignalCellularOff
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.control.DeviceStatus

/**
 * Dashboard UI component displaying toggle switches for WiFi, Bluetooth,
 * and Mobile Data with live status feedback and direct settings interaction.
 */
@Composable
fun ConnectivityDashboardCard(
    deviceStatus: DeviceStatus,
    onToggleWifi: (Boolean) -> Unit,
    onToggleBluetooth: (Boolean) -> Unit,
    onToggleMobileData: (Boolean) -> Unit,
    onOpenWifiSettings: () -> Unit,
    onOpenBluetoothSettings: () -> Unit,
    onOpenMobileDataSettings: () -> Unit,
    onOpenInternetPanel: () -> Unit,
    onRefreshStatus: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeCount = listOf(
        deviceStatus.isWifiEnabled,
        deviceStatus.isBluetoothEnabled,
        deviceStatus.isMobileDataConnected
    ).count { it }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("connectivity_dashboard"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Dashboard Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sensors,
                            contentDescription = "Sensor & Nirkabel",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Dashboard Konektivitas",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "Saklar nirkabel & status seketika",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Active counter badge & Refresh button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (activeCount > 0)
                            MaterialTheme.colorScheme.secondaryContainer
                        else
                            MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (activeCount > 0)
                                            MaterialTheme.colorScheme.secondary
                                        else
                                            MaterialTheme.colorScheme.outline
                                    )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "$activeCount Aktif",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (activeCount > 0)
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onRefreshStatus,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("connectivity_refresh_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Segarkan Status",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 1. Wi-Fi Toggle Tile
            ConnectivityToggleTile(
                title = "Wi-Fi",
                statusText = when {
                    deviceStatus.isWifiConnected -> "Terhubung • ${deviceStatus.activeNetworkName}"
                    deviceStatus.isWifiEnabled -> "Aktif • Mencari Jaringan"
                    else -> "Dinonaktifkan"
                },
                isActive = deviceStatus.isWifiEnabled,
                activeIcon = Icons.Default.Wifi,
                inactiveIcon = Icons.Default.WifiOff,
                activeColor = MaterialTheme.colorScheme.primary,
                activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
                switchTestTag = "wifi_toggle_switch",
                tileTestTag = "wifi_dashboard_tile",
                onToggle = onToggleWifi,
                onSettingsClick = onOpenWifiSettings,
                settingsDescription = "Pengaturan Wi-Fi"
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 2. Bluetooth Toggle Tile
            ConnectivityToggleTile(
                title = "Bluetooth",
                statusText = if (deviceStatus.isBluetoothEnabled) {
                    "Aktif • Siap Terhubung"
                } else {
                    "Nonaktif"
                },
                isActive = deviceStatus.isBluetoothEnabled,
                activeIcon = Icons.Default.Bluetooth,
                inactiveIcon = Icons.Default.BluetoothDisabled,
                activeColor = MaterialTheme.colorScheme.tertiary,
                activeContainerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
                switchTestTag = "bluetooth_toggle_switch",
                tileTestTag = "bluetooth_dashboard_tile",
                onToggle = onToggleBluetooth,
                onSettingsClick = onOpenBluetoothSettings,
                settingsDescription = "Pengaturan Bluetooth"
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 3. Mobile Data Toggle Tile
            ConnectivityToggleTile(
                title = "Data Seluler",
                statusText = if (deviceStatus.isMobileDataConnected) {
                    "Aktif • Mengalirkan Data"
                } else {
                    "Standby / Nonaktif"
                },
                isActive = deviceStatus.isMobileDataConnected,
                activeIcon = Icons.Default.SignalCellularAlt,
                inactiveIcon = Icons.Default.SignalCellularOff,
                activeColor = MaterialTheme.colorScheme.secondary,
                activeContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                switchTestTag = "mobile_data_toggle_switch",
                tileTestTag = "mobile_data_dashboard_tile",
                onToggle = onToggleMobileData,
                onSettingsClick = onOpenMobileDataSettings,
                settingsDescription = "Pengaturan Data Seluler"
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Bottom Quick Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenInternetPanel,
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .testTag("dashboard_internet_panel_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.NetworkCheck,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Panel Internet", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }

                FilledTonalButton(
                    onClick = onOpenBluetoothSettings,
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .testTag("dashboard_bluetooth_settings_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Bluetooth,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Kelola BT", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

/**
 * Individual interactive tile for a connectivity switch with animated color transitions.
 */
@Composable
private fun ConnectivityToggleTile(
    title: String,
    statusText: String,
    isActive: Boolean,
    activeIcon: ImageVector,
    inactiveIcon: ImageVector,
    activeColor: Color,
    activeContainerColor: Color,
    switchTestTag: String,
    tileTestTag: String,
    onToggle: (Boolean) -> Unit,
    onSettingsClick: () -> Unit,
    settingsDescription: String
) {
    val animatedBgColor by animateColorAsState(
        targetValue = if (isActive)
            activeContainerColor.copy(alpha = 0.45f)
        else
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        animationSpec = tween(durationMillis = 250),
        label = "tile_bg"
    )

    val animatedIconBg by animateColorAsState(
        targetValue = if (isActive) activeContainerColor else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = tween(durationMillis = 250),
        label = "icon_bg"
    )

    val animatedIconTint by animateColorAsState(
        targetValue = if (isActive) activeColor else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(durationMillis = 250),
        label = "icon_tint"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                role = Role.Switch,
                onClick = { onToggle(!isActive) }
            )
            .testTag(tileTestTag),
        shape = RoundedCornerShape(16.dp),
        color = animatedBgColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Icon + Label + Subtitle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(animatedIconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isActive) activeIcon else inactiveIcon,
                        contentDescription = title,
                        tint = animatedIconTint,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = if (isActive)
                            activeColor
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Right: Settings shortcut + Switch
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("${switchTestTag}_settings")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = settingsDescription,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Switch(
                    checked = isActive,
                    onCheckedChange = { onToggle(it) },
                    modifier = Modifier.testTag(switchTestTag),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = activeColor,
                        uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            }
        }
    }
}

package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.entity.AutomationRoutineEntity
import com.example.ui.AssistantViewModel

@Composable
fun RoutinesScreen(
    viewModel: AssistantViewModel,
    routines: List<AutomationRoutineEntity>,
    paddingValues: PaddingValues
) {
    var showAddDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Column {
                    Text(
                        text = "Rutinitas Otomatis",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Jalankan kombinasi perintah kontrol hp dalam satu ketukan instan.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(routines) { routine ->
                RoutineCard(
                    routine = routine,
                    onExecute = { viewModel.executeRoutine(routine) },
                    onDelete = if (routine.iconKey == "custom") {
                        { viewModel.deleteRoutine(routine) }
                    } else null
                )
            }

            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }

        // FAB to add routine
        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("add_routine_fab"),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Tambah Rutinitas",
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }

        if (showAddDialog) {
            CreateRoutineDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { name, desc, wifi, data, sound, flash ->
                    viewModel.createCustomRoutine(name, desc, wifi, data, sound, flash)
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
private fun RoutineCard(
    routine: AutomationRoutineEntity,
    onExecute: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val icon = when (routine.iconKey) {
        "bed" -> Icons.Default.Bedtime
        "work" -> Icons.Default.Work
        "battery" -> Icons.Default.BatteryAlert
        else -> Icons.Default.Tune
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("routine_card_${routine.id}"),
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = routine.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = routine.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (onDelete != null) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Hapus",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action summary chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (routine.wifiAction != "KEEP") {
                    ActionChipBadge(
                        text = if (routine.wifiAction == "ON") "Wi-Fi: Nyala" else "Wi-Fi: Mati"
                    )
                }
                if (routine.mobileDataAction != "KEEP") {
                    ActionChipBadge(
                        text = "Data: Panel"
                    )
                }
                if (routine.soundModeAction != "KEEP") {
                    ActionChipBadge(
                        text = "Suara: ${routine.soundModeAction}"
                    )
                }
                if (routine.flashlightAction != "KEEP") {
                    ActionChipBadge(
                        text = if (routine.flashlightAction == "ON") "Senter: Nyala" else "Senter: Mati"
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onExecute,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("execute_routine_${routine.id}"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Jalankan Rutinitas Sekarang")
            }
        }
    }
}

@Composable
private fun ActionChipBadge(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateRoutineDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, desc: String, wifi: String, data: String, sound: String, flash: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var wifiAction by remember { mutableStateOf("OFF") }
    var mobileDataAction by remember { mutableStateOf("OFF_PANEL") }
    var soundAction by remember { mutableStateOf("SILENT") }
    var flashAction by remember { mutableStateOf("OFF") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Buat Rutinitas Kustom") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Rutinitas") },
                    placeholder = { Text("Misal: Mau Belajar") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Deskripsi Singkat") },
                    placeholder = { Text("Fokus dan hening") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Aksi yang Dijalankan:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Wi-Fi:", fontSize = 13.sp)
                    TextButton(onClick = {
                        wifiAction = when (wifiAction) {
                            "OFF" -> "ON"
                            "ON" -> "KEEP"
                            else -> "OFF"
                        }
                    }) {
                        Text(wifiAction)
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Data Seluler:", fontSize = 13.sp)
                    TextButton(onClick = {
                        mobileDataAction = if (mobileDataAction == "OFF_PANEL") "KEEP" else "OFF_PANEL"
                    }) {
                        Text(if (mobileDataAction == "OFF_PANEL") "Matikan (Panel)" else "Biarkan")
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Profil Suara:", fontSize = 13.sp)
                    TextButton(onClick = {
                        soundAction = when (soundAction) {
                            "SILENT" -> "VIBRATE"
                            "VIBRATE" -> "NORMAL"
                            "NORMAL" -> "KEEP"
                            else -> "SILENT"
                        }
                    }) {
                        Text(soundAction)
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Senter:", fontSize = 13.sp)
                    TextButton(onClick = {
                        flashAction = when (flashAction) {
                            "OFF" -> "ON"
                            "ON" -> "KEEP"
                            else -> "OFF"
                        }
                    }) {
                        Text(flashAction)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name, desc.ifBlank { "Rutinitas Kustom" }, wifiAction, mobileDataAction, soundAction, flashAction)
                    }
                }
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

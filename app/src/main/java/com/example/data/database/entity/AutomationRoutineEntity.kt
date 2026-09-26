package com.example.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "automation_routines")
data class AutomationRoutineEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String,
    val iconKey: String, // "bed", "work", "battery", "custom"
    val wifiAction: String, // "OFF", "ON", "KEEP"
    val mobileDataAction: String, // "OFF_PANEL", "ON_PANEL", "KEEP"
    val soundModeAction: String, // "SILENT", "VIBRATE", "NORMAL", "KEEP"
    val flashlightAction: String, // "OFF", "ON", "KEEP"
    val isEnabled: Boolean = true
)

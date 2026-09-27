package com.example.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_apps")
data class FavoriteAppEntity(
    @PrimaryKey
    val packageName: String,
    val name: String,
    val orderIndex: Int = 0,
    val addedAt: Long = System.currentTimeMillis()
)

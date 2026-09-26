package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.database.dao.AutomationRoutineDao
import com.example.data.database.dao.CommandLogDao
import com.example.data.database.entity.AutomationRoutineEntity
import com.example.data.database.entity.CommandLogEntity

@Database(
    entities = [CommandLogEntity::class, AutomationRoutineEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun commandLogDao(): CommandLogDao
    abstract fun automationRoutineDao(): AutomationRoutineDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "offline_assistant.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}

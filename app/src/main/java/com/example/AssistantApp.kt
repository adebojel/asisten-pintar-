package com.example

import android.app.Application
import com.example.data.database.AppDatabase
import com.example.data.repository.AssistantRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AssistantApp : Application() {
    lateinit var repository: AssistantRepository
        private set

    override fun onCreate() {
        super.onCreate()
        val database = AppDatabase.getInstance(this)
        repository = AssistantRepository(
            database.commandLogDao(),
            database.automationRoutineDao()
        )
        CoroutineScope(Dispatchers.IO).launch {
            repository.ensureDefaultRoutines()
        }
    }
}

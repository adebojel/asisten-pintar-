package com.example.data.repository

import com.example.data.database.dao.AutomationRoutineDao
import com.example.data.database.dao.CommandLogDao
import com.example.data.database.entity.AutomationRoutineEntity
import com.example.data.database.entity.CommandLogEntity
import kotlinx.coroutines.flow.Flow

class AssistantRepository(
    private val commandLogDao: CommandLogDao,
    private val automationRoutineDao: AutomationRoutineDao
) {
    val logs: Flow<List<CommandLogEntity>> = commandLogDao.getAllLogs()
    val routines: Flow<List<AutomationRoutineEntity>> = automationRoutineDao.getAllRoutines()

    suspend fun logCommand(
        query: String,
        response: String,
        actionType: String,
        isSuccess: Boolean
    ): Long {
        return commandLogDao.insertLog(
            CommandLogEntity(
                query = query,
                response = response,
                actionType = actionType,
                isSuccess = isSuccess
            )
        )
    }

    suspend fun clearLogs() {
        commandLogDao.clearAllLogs()
    }

    suspend fun ensureDefaultRoutines() {
        val count = automationRoutineDao.countRoutines()
        if (count == 0) {
            val defaultRoutines = listOf(
                AutomationRoutineEntity(
                    name = "Mode Tidur Malam",
                    description = "Senyapkan suara, matikan senter, dan siapkan pemutusan koneksi untuk istirahat tenang.",
                    iconKey = "bed",
                    wifiAction = "OFF",
                    mobileDataAction = "OFF_PANEL",
                    soundModeAction = "SILENT",
                    flashlightAction = "OFF",
                    isEnabled = true
                ),
                AutomationRoutineEntity(
                    name = "Mode Fokus Kerja",
                    description = "Ubah ke mode getar dan aktifkan koneksi Wi-Fi kantor.",
                    iconKey = "work",
                    wifiAction = "ON",
                    mobileDataAction = "KEEP",
                    soundModeAction = "VIBRATE",
                    flashlightAction = "OFF",
                    isEnabled = true
                ),
                AutomationRoutineEntity(
                    name = "Hemat Daya Ekstrem",
                    description = "Putus Wi-Fi dan buka panel pemutus data seluler untuk menghemat baterai kritis.",
                    iconKey = "battery",
                    wifiAction = "OFF",
                    mobileDataAction = "OFF_PANEL",
                    soundModeAction = "SILENT",
                    flashlightAction = "OFF",
                    isEnabled = true
                )
            )
            automationRoutineDao.insertAll(defaultRoutines)
        }
    }

    suspend fun insertRoutine(routine: AutomationRoutineEntity) = automationRoutineDao.insertRoutine(routine)

    suspend fun updateRoutine(routine: AutomationRoutineEntity) = automationRoutineDao.updateRoutine(routine)

    suspend fun deleteRoutine(routine: AutomationRoutineEntity) = automationRoutineDao.deleteRoutine(routine)
}

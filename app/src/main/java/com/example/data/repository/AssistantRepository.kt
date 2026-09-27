package com.example.data.repository

import com.example.control.AppItem
import com.example.data.database.dao.AutomationRoutineDao
import com.example.data.database.dao.CommandLogDao
import com.example.data.database.dao.FavoriteAppDao
import com.example.data.database.entity.AutomationRoutineEntity
import com.example.data.database.entity.CommandLogEntity
import com.example.data.database.entity.FavoriteAppEntity
import kotlinx.coroutines.flow.Flow
import java.util.Locale

class AssistantRepository(
    private val commandLogDao: CommandLogDao,
    private val automationRoutineDao: AutomationRoutineDao,
    private val favoriteAppDao: FavoriteAppDao
) {
    val logs: Flow<List<CommandLogEntity>> = commandLogDao.getAllLogs()
    val routines: Flow<List<AutomationRoutineEntity>> = automationRoutineDao.getAllRoutines()
    val favoriteApps: Flow<List<FavoriteAppEntity>> = favoriteAppDao.getAllFavorites()

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

    suspend fun addFavorite(packageName: String, name: String) {
        val count = favoriteAppDao.countFavorites()
        favoriteAppDao.insertFavorite(
            FavoriteAppEntity(
                packageName = packageName,
                name = name,
                orderIndex = count
            )
        )
    }

    suspend fun removeFavorite(packageName: String) {
        favoriteAppDao.deleteByPackage(packageName)
    }

    suspend fun toggleFavorite(packageName: String, name: String): Boolean {
        return if (favoriteAppDao.isFavorite(packageName)) {
            favoriteAppDao.deleteByPackage(packageName)
            false
        } else {
            addFavorite(packageName, name)
            true
        }
    }

    suspend fun isFavorite(packageName: String): Boolean {
        return favoriteAppDao.isFavorite(packageName)
    }

    suspend fun setFavorites(selectedApps: List<AppItem>) {
        favoriteAppDao.clearAllFavorites()
        val entities = selectedApps.mapIndexed { index, app ->
            FavoriteAppEntity(
                packageName = app.packageName,
                name = app.name,
                orderIndex = index
            )
        }
        favoriteAppDao.insertAll(entities)
    }

    suspend fun ensureDefaultFavorites(installed: List<AppItem>) {
        if (installed.isEmpty()) return
        val count = favoriteAppDao.countFavorites()
        if (count == 0) {
            // Pick popular everyday apps available on the device
            val priorityKeywords = listOf(
                "whatsapp", "youtube", "chrome", "kamera", "camera",
                "telepon", "phone", "dialer", "pesan", "message",
                "galeri", "gallery", "maps", "pengaturan", "settings"
            )
            val selected = mutableListOf<AppItem>()
            for (keyword in priorityKeywords) {
                if (selected.size >= 8) break
                val found = installed.firstOrNull { app ->
                    !selected.any { it.packageName == app.packageName } &&
                    (app.packageName.lowercase(Locale.ROOT).contains(keyword) ||
                     app.name.lowercase(Locale.ROOT).contains(keyword))
                }
                if (found != null) {
                    selected.add(found)
                }
            }
            // If less than 4, fill up with first installed apps
            if (selected.size < 4) {
                for (app in installed) {
                    if (selected.size >= 6) break
                    if (!selected.any { it.packageName == app.packageName }) {
                        selected.add(app)
                    }
                }
            }
            if (selected.isNotEmpty()) {
                val entities = selected.mapIndexed { index, app ->
                    FavoriteAppEntity(
                        packageName = app.packageName,
                        name = app.name,
                        orderIndex = index
                    )
                }
                favoriteAppDao.insertAll(entities)
            }
        }
    }
}

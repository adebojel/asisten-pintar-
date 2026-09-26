package com.example.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.database.entity.AutomationRoutineEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AutomationRoutineDao {
    @Query("SELECT * FROM automation_routines ORDER BY id ASC")
    fun getAllRoutines(): Flow<List<AutomationRoutineEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutine(routine: AutomationRoutineEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(routines: List<AutomationRoutineEntity>)

    @Update
    suspend fun updateRoutine(routine: AutomationRoutineEntity)

    @Delete
    suspend fun deleteRoutine(routine: AutomationRoutineEntity)

    @Query("SELECT COUNT(*) FROM automation_routines")
    suspend fun countRoutines(): Int
}

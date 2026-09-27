package com.example.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.database.entity.FavoriteAppEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteAppDao {
    @Query("SELECT * FROM favorite_apps ORDER BY orderIndex ASC, addedAt ASC")
    fun getAllFavorites(): Flow<List<FavoriteAppEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteAppEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(favorites: List<FavoriteAppEntity>)

    @Delete
    suspend fun deleteFavorite(favorite: FavoriteAppEntity)

    @Query("DELETE FROM favorite_apps WHERE packageName = :packageName")
    suspend fun deleteByPackage(packageName: String)

    @Query("DELETE FROM favorite_apps")
    suspend fun clearAllFavorites()

    @Query("SELECT COUNT(*) FROM favorite_apps")
    suspend fun countFavorites(): Int

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_apps WHERE packageName = :packageName)")
    suspend fun isFavorite(packageName: String): Boolean
}

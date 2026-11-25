package com.samuel.androidepy.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScriptDao {

    @Query("SELECT * FROM scripts WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ScriptEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(script: ScriptEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(script: ScriptEntity)

    @Query("SELECT * FROM scripts ORDER BY updated_at_epoch_ms DESC")
    fun observeAll(): Flow<List<ScriptEntity>>
}

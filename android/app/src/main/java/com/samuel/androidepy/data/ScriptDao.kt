package com.samuel.androidepy.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
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

    @Query("DELETE FROM scripts WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT COALESCE(MAX(version_number), 0) + 1 FROM script_versions WHERE script_id = :scriptId")
    suspend fun nextVersionNumber(scriptId: String): Int

    @Query("SELECT * FROM script_versions WHERE script_id = :scriptId ORDER BY version_number DESC")
    fun observeVersions(scriptId: String): Flow<List<ScriptVersionEntity>>

    @Query("SELECT * FROM script_versions WHERE script_id = :scriptId ORDER BY version_number DESC")
    suspend fun getVersions(scriptId: String): List<ScriptVersionEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertVersionIfAbsent(version: ScriptVersionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVersion(version: ScriptVersionEntity)

    @Query("DELETE FROM script_versions WHERE script_id = :scriptId")
    suspend fun deleteVersions(scriptId: String)

    @Transaction
    suspend fun upsertWithVersion(script: ScriptEntity, version: ScriptVersionEntity) {
        upsert(script)
        insertVersion(version)
    }

    @Transaction
    suspend fun insertDefaultIfAbsent(script: ScriptEntity, version: ScriptVersionEntity) {
        insertIfAbsent(script)
        insertVersionIfAbsent(version)
    }
}

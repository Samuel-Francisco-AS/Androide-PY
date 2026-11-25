package com.samuel.androidepy.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScriptExecutionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(execution: ScriptExecutionEntity)

    @Query("SELECT * FROM script_executions WHERE execution_id = :executionId LIMIT 1")
    suspend fun getById(executionId: String): ScriptExecutionEntity?

    @Query("SELECT * FROM script_executions WHERE script_id = :scriptId ORDER BY queued_at_epoch_ms DESC")
    fun observeForScript(scriptId: String): Flow<List<ScriptExecutionEntity>>
}

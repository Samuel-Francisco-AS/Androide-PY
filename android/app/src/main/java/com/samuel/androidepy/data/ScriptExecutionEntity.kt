package com.samuel.androidepy.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "script_executions",
    indices = [
        Index(value = ["script_id", "queued_at_epoch_ms"]),
        Index(value = ["status"])
    ]
)
data class ScriptExecutionEntity(
    @PrimaryKey @ColumnInfo(name = "execution_id") val executionId: String,
    @ColumnInfo(name = "script_id") val scriptId: String,
    @ColumnInfo(name = "script_name_snapshot") val scriptNameSnapshot: String,
    @ColumnInfo(name = "script_version_number") val scriptVersionNumber: Int?,
    val status: String,
    @ColumnInfo(name = "queued_at_epoch_ms") val queuedAtEpochMs: Long,
    @ColumnInfo(name = "started_at_epoch_ms") val startedAtEpochMs: Long?,
    @ColumnInfo(name = "finished_at_epoch_ms") val finishedAtEpochMs: Long?,
    @ColumnInfo(name = "duration_ms") val durationMs: Long?,
    @ColumnInfo(name = "parameters_json") val parametersJson: String,
    val stdout: String,
    val stderr: String,
    @ColumnInfo(name = "error_json") val errorJson: String?,
    @ColumnInfo(name = "stdout_truncated") val stdoutTruncated: Boolean,
    @ColumnInfo(name = "stderr_truncated") val stderrTruncated: Boolean
)

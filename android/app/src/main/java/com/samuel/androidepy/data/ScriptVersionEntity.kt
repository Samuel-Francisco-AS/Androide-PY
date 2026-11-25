package com.samuel.androidepy.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "script_versions",
    primaryKeys = ["script_id", "version_number"],
    indices = [Index(value = ["script_id", "created_at_epoch_ms"])]
)
data class ScriptVersionEntity(
    @ColumnInfo(name = "script_id") val scriptId: String,
    @ColumnInfo(name = "version_number") val versionNumber: Int,
    val name: String,
    @ColumnInfo(name = "source_code") val sourceCode: String,
    val description: String,
    @ColumnInfo(name = "tags_json") val tagsJson: String,
    @ColumnInfo(name = "parameters_json") val parametersJson: String,
    @ColumnInfo(name = "dependencies_json") val dependenciesJson: String,
    @ColumnInfo(name = "created_at_epoch_ms") val createdAtEpochMs: Long
)

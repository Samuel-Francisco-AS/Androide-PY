package com.samuel.androidepy.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scripts")
data class ScriptEntity(
    @PrimaryKey
    val id: String,

    val name: String,

    @ColumnInfo(name = "source_code")
    val sourceCode: String,

    @ColumnInfo(name = "created_at_epoch_ms")
    val createdAtEpochMs: Long,

    @ColumnInfo(name = "updated_at_epoch_ms")
    val updatedAtEpochMs: Long
)

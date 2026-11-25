package com.samuel.androidepy.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [ScriptEntity::class, ScriptVersionEntity::class, ScriptExecutionEntity::class],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scriptDao(): ScriptDao
    abstract fun scriptExecutionDao(): ScriptExecutionDao

    companion object {
        const val DATABASE_NAME = "androide_py.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE scripts ADD COLUMN description TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE scripts ADD COLUMN tags_json TEXT NOT NULL DEFAULT '[]'")
                database.execSQL("ALTER TABLE scripts ADD COLUMN parameters_json TEXT NOT NULL DEFAULT '[]'")
                database.execSQL("ALTER TABLE scripts ADD COLUMN dependencies_json TEXT NOT NULL DEFAULT '[]'")

                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS script_versions (
                        script_id TEXT NOT NULL,
                        version_number INTEGER NOT NULL,
                        name TEXT NOT NULL,
                        source_code TEXT NOT NULL,
                        description TEXT NOT NULL,
                        tags_json TEXT NOT NULL,
                        parameters_json TEXT NOT NULL,
                        dependencies_json TEXT NOT NULL,
                        created_at_epoch_ms INTEGER NOT NULL,
                        PRIMARY KEY(script_id, version_number)
                    )
                """.trimIndent())
                database.execSQL("CREATE INDEX IF NOT EXISTS index_script_versions_script_id_created_at_epoch_ms ON script_versions(script_id, created_at_epoch_ms)")

                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS script_executions (
                        execution_id TEXT NOT NULL PRIMARY KEY,
                        script_id TEXT NOT NULL,
                        script_name_snapshot TEXT NOT NULL,
                        script_version_number INTEGER,
                        status TEXT NOT NULL,
                        queued_at_epoch_ms INTEGER NOT NULL,
                        started_at_epoch_ms INTEGER,
                        finished_at_epoch_ms INTEGER,
                        duration_ms INTEGER,
                        parameters_json TEXT NOT NULL,
                        stdout TEXT NOT NULL,
                        stderr TEXT NOT NULL,
                        error_json TEXT,
                        stdout_truncated INTEGER NOT NULL,
                        stderr_truncated INTEGER NOT NULL
                    )
                """.trimIndent())
                database.execSQL("CREATE INDEX IF NOT EXISTS index_script_executions_script_id_queued_at_epoch_ms ON script_executions(script_id, queued_at_epoch_ms)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_script_executions_status ON script_executions(status)")
            }
        }

        @Volatile private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { instance = it }
            }
        }
    }
}

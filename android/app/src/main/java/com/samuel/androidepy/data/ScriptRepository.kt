package com.samuel.androidepy.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch

/**
 * Kotlin-owned persistence boundary for scripts.
 *
 * Python receives source code through the execution request and never accesses
 * Room, SQLite, entities, or DAOs directly.
 */
class ScriptRepository(
    private val dao: ScriptDao,
    private val clock: () -> Long = { System.currentTimeMillis() }
) {

    fun observeAll(): Flow<List<ScriptEntity>> =
        dao.observeAll().catch { error ->
            if (error is CancellationException) throw error
            throw ScriptStorageException("observe scripts", error)
        }

    suspend fun getOrCreateDefault(): ScriptEntity {
        return try {
            dao.getById(DEFAULT_SCRIPT_ID)?.let { return it }

            val now = clock()
            val initial = ScriptEntity(
                id = DEFAULT_SCRIPT_ID,
                name = "Meu primeiro script",
                sourceCode = "print(\"Olá, Androide PY!\")",
                createdAtEpochMs = now,
                updatedAtEpochMs = now
            )

            // IGNORE makes first-run creation safe if two callers race.
            dao.insertIfAbsent(initial)

            dao.getById(DEFAULT_SCRIPT_ID)
                ?: throw IllegalStateException(
                    "The default script could not be created or recovered."
                )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            throw ScriptStorageException("load or create the default script", error)
        }
    }

    suspend fun save(
        scriptId: String,
        name: String,
        sourceCode: String
    ): ScriptEntity {
        require(scriptId.isNotBlank()) { "scriptId cannot be blank." }

        return try {
            val previous = dao.getById(scriptId)
            val now = clock()
            val normalizedName = name.trim().ifBlank {
                previous?.name ?: "Script sem título"
            }

            val saved = ScriptEntity(
                id = scriptId,
                name = normalizedName,
                sourceCode = sourceCode,
                createdAtEpochMs = previous?.createdAtEpochMs ?: now,
                updatedAtEpochMs = now
            )

            dao.upsert(saved)
            saved
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            throw ScriptStorageException("save script '$scriptId'", error)
        }
    }

    companion object {
        const val DEFAULT_SCRIPT_ID = "default"
    }
}

class ScriptStorageException(
    operation: String,
    cause: Throwable
) : IllegalStateException("Unable to $operation.", cause)

package com.samuel.androidepy.data

import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import org.json.JSONArray
import org.json.JSONObject

/**
 * Kotlin-owned persistence boundary. Python receives source and parameters
 * through execution requests and never accesses Room, SQLite, entities or DAOs.
 */
class ScriptRepository(
    private val dao: ScriptDao,
    private val clock: () -> Long = { System.currentTimeMillis() }
) {
    fun observeAll(): Flow<List<ScriptEntity>> = dao.observeAll().storageErrors("observe scripts")

    fun observeVersions(scriptId: String): Flow<List<ScriptVersionEntity>> =
        dao.observeVersions(scriptId).storageErrors("observe script versions")

    suspend fun getVersions(scriptId: String): List<ScriptVersionEntity> =
        storage("read script versions") { dao.getVersions(scriptId) }

    suspend fun getById(scriptId: String): ScriptEntity? =
        storage("read script '$scriptId'") { dao.getById(scriptId) }

    suspend fun getOrCreateDefault(): ScriptEntity = storage("load or create the default script") {
        val existing = dao.getById(DEFAULT_SCRIPT_ID)
        if (existing != null) {
            ensureInitialVersion(existing)
            return@storage existing
        }

        val now = clock()
        val initial = ScriptEntity(
            id = DEFAULT_SCRIPT_ID,
            name = "Meu primeiro script",
            sourceCode = "print(\"Olá, Androide PY!\")",
            createdAtEpochMs = now,
            updatedAtEpochMs = now
        )
        val initialVersion = initial.toVersion(1, now)
        dao.insertDefaultIfAbsent(initial, initialVersion)
        dao.getById(DEFAULT_SCRIPT_ID)
            ?: throw IllegalStateException("The default script could not be created or recovered.")
    }

    suspend fun create(
        name: String,
        sourceCode: String = "",
        description: String = "",
        tagsJson: String = "[]",
        parametersJson: String = "[]",
        dependenciesJson: String = "[]"
    ): ScriptEntity = storage("create script") {
        val normalizedName = name.trim()
        require(normalizedName.isNotEmpty()) { "O nome do script não pode ficar vazio." }
        val now = clock()
        val script = ScriptEntity(
            id = UUID.randomUUID().toString(),
            name = normalizedName,
            sourceCode = sourceCode,
            createdAtEpochMs = now,
            updatedAtEpochMs = now,
            description = description.trim(),
            tagsJson = normalizeStringArray(tagsJson, "tags"),
            parametersJson = validateParameters(parametersJson),
            dependenciesJson = validateDependencies(dependenciesJson)
        )
        dao.upsertWithVersion(script, script.toVersion(1, now))
        script
    }

    suspend fun save(
        scriptId: String,
        name: String,
        sourceCode: String,
        description: String = "",
        tagsJson: String = "[]",
        parametersJson: String = "[]",
        dependenciesJson: String = "[]"
    ): ScriptEntity {
        require(scriptId.isNotBlank()) { "scriptId cannot be blank." }
        return storage("save script '$scriptId'") {
            val previous = dao.getById(scriptId)
            val normalizedName = name.trim().ifBlank {
                previous?.name ?: "Script sem título"
            }
            val normalizedTags = normalizeStringArray(tagsJson, "tags")
            val normalizedParameters = validateParameters(parametersJson)
            val normalizedDependencies = validateDependencies(dependenciesJson)
            val now = clock()
            val saved = ScriptEntity(
                id = scriptId,
                name = normalizedName,
                sourceCode = sourceCode,
                createdAtEpochMs = previous?.createdAtEpochMs ?: now,
                updatedAtEpochMs = now,
                description = description.trim(),
                tagsJson = normalizedTags,
                parametersJson = normalizedParameters,
                dependenciesJson = normalizedDependencies
            )

            val changed = previous == null ||
                previous.name != saved.name ||
                previous.sourceCode != saved.sourceCode ||
                previous.description != saved.description ||
                previous.tagsJson != saved.tagsJson ||
                previous.parametersJson != saved.parametersJson ||
                previous.dependenciesJson != saved.dependenciesJson

            if (changed) {
                val nextVersion = dao.nextVersionNumber(scriptId)
                dao.upsertWithVersion(saved, saved.toVersion(nextVersion, now))
            }
            saved
        }
    }

    suspend fun rename(scriptId: String, newName: String): ScriptEntity {
        val current = getById(scriptId) ?: throw IllegalArgumentException("Script not found.")
        return save(
            current.id, newName, current.sourceCode, current.description,
            current.tagsJson, current.parametersJson, current.dependenciesJson
        )
    }

    suspend fun duplicate(scriptId: String, newName: String? = null): ScriptEntity =
        storage("duplicate script '$scriptId'") {
            val source = dao.getById(scriptId) ?: throw IllegalArgumentException("Script not found.")
            val now = clock()
            val copy = source.copy(
                id = UUID.randomUUID().toString(),
                name = newName?.trim()?.takeIf { it.isNotEmpty() } ?: "Cópia de ${source.name}",
                createdAtEpochMs = now,
                updatedAtEpochMs = now
            )
            dao.upsertWithVersion(copy, copy.toVersion(1, now))
            copy
        }

    suspend fun delete(scriptId: String) = storage("delete script '$scriptId'") {
        dao.deleteVersions(scriptId)
        dao.deleteById(scriptId)
    }

    suspend fun restoreVersion(scriptId: String, versionNumber: Int): ScriptEntity =
        storage("restore script version") {
            val current = dao.getById(scriptId) ?: throw IllegalArgumentException("Script not found.")
            val version = dao.getVersions(scriptId).firstOrNull { it.versionNumber == versionNumber }
                ?: throw IllegalArgumentException("Script version not found.")
            val now = clock()
            val restored = current.copy(
                name = version.name,
                sourceCode = version.sourceCode,
                description = version.description,
                tagsJson = version.tagsJson,
                parametersJson = version.parametersJson,
                dependenciesJson = version.dependenciesJson,
                updatedAtEpochMs = now
            )
            dao.upsertWithVersion(restored, restored.toVersion(dao.nextVersionNumber(scriptId), now))
            restored
        }

    private suspend fun ensureInitialVersion(script: ScriptEntity) {
        if (dao.getVersions(script.id).isEmpty()) {
            dao.insertVersionIfAbsent(script.toVersion(1, clock()))
        }
    }

    private fun ScriptEntity.toVersion(number: Int, timestamp: Long) = ScriptVersionEntity(
        scriptId = id,
        versionNumber = number,
        name = name,
        sourceCode = sourceCode,
        description = description,
        tagsJson = tagsJson,
        parametersJson = parametersJson,
        dependenciesJson = dependenciesJson,
        createdAtEpochMs = timestamp
    )

    private fun normalizeStringArray(raw: String, field: String): String {
        val array = JSONArray(raw)
        val normalized = JSONArray()
        val seen = mutableSetOf<String>()
        for (index in 0 until array.length()) {
            val value = array.optString(index, "").trim()
            require(value.isNotEmpty()) { "$field cannot contain empty values." }
            if (seen.add(value)) normalized.put(value)
        }
        return normalized.toString()
    }

    private fun validateParameters(raw: String): String {
        val array = JSONArray(raw)
        val names = mutableSetOf<String>()
        val normalized = JSONArray()
        val allowedTypes = setOf("string", "integer", "number", "boolean", "json")
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index)
                ?: throw IllegalArgumentException("Each parameter must be a JSON object.")
            val name = item.optString("name", "").trim()
            val type = item.optString("type", "string").trim().lowercase()
            require(PARAMETER_NAME.matches(name)) { "Invalid parameter name: '$name'." }
            require(names.add(name)) { "Duplicate parameter name: '$name'." }
            require(type in allowedTypes) { "Unsupported parameter type: '$type'." }
            val parameter = JSONObject().put("name", name).put("type", type)
                .put("required", item.optBoolean("required", false))
            if (item.has("default")) parameter.put("default", item.get("default"))
            normalized.put(parameter)
        }
        return normalized.toString()
    }

    private fun validateDependencies(raw: String): String {
        val array = JSONArray(raw)
        val normalized = JSONArray()
        val seen = mutableSetOf<String>()
        for (index in 0 until array.length()) {
            val requirement = array.optString(index, "").trim()
            require(REQUIREMENT.matches(requirement)) {
                "Invalid dependency requirement: '$requirement'. Use a package name and an optional version constraint."
            }
            if (seen.add(requirement.lowercase())) normalized.put(requirement)
        }
        return normalized.toString()
    }

    private suspend fun <T> storage(operation: String, block: suspend () -> T): T {
        return try {
            block()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            if (error is IllegalArgumentException) throw error
            throw ScriptStorageException(operation, error)
        }
    }

    private fun <T> Flow<T>.storageErrors(operation: String): Flow<T> = catch { error ->
        if (error is CancellationException) throw error
        throw ScriptStorageException(operation, error)
    }

    companion object {
        const val DEFAULT_SCRIPT_ID = "default"
        private val PARAMETER_NAME = Regex("[A-Za-z_][A-Za-z0-9_]*")
        private val REQUIREMENT = Regex(
            "[A-Za-z0-9][A-Za-z0-9._-]*(?:\\[[A-Za-z0-9_,.-]+\\])?(?:\\s*(?:==|>=|<=|~=|!=|>|<)\\s*[A-Za-z0-9.*+!-]+)?"
        )
    }
}

class ScriptStorageException(operation: String, cause: Throwable) :
    IllegalStateException("Unable to $operation.", cause)

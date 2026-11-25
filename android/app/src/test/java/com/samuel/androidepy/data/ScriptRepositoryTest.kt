package com.samuel.androidepy.data

import java.util.concurrent.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ScriptRepositoryTest {
    @Test
    fun createsDefaultScriptOnlyOnceAndSeedsVersionHistory() = runBlocking {
        val dao = FakeScriptDao()
        var timestamp = 100L
        val repository = ScriptRepository(dao) { timestamp++ }
        val first = repository.getOrCreateDefault()
        val second = repository.getOrCreateDefault()
        assertEquals(first, second)
        assertEquals(1, dao.size)
        assertEquals("Meu primeiro script", first.name)
        assertEquals("print(\"Olá, Androide PY!\")", first.sourceCode)
        assertEquals(1, repository.getVersions(first.id).size)
        assertEquals(100L, first.createdAtEpochMs)
    }

    @Test
    fun savePreservesCreationTimeAndCreatesImmutableVersion() = runBlocking {
        val dao = FakeScriptDao()
        var timestamp = 100L
        val repository = ScriptRepository(dao) { timestamp++ }
        val initial = repository.getOrCreateDefault()
        val saved = repository.save(initial.id, "  Meu script novo  ", "print(2)")
        assertEquals(initial.createdAtEpochMs, saved.createdAtEpochMs)
        assertEquals(101L, saved.updatedAtEpochMs)
        assertEquals("Meu script novo", saved.name)
        assertEquals("print(2)", saved.sourceCode)
        val versions = repository.getVersions(initial.id)
        assertEquals(listOf(2, 1), versions.map { it.versionNumber })
        assertEquals(initial.sourceCode, versions.last().sourceCode)
    }

    @Test
    fun unchangedSaveDoesNotCreateDuplicateVersion() = runBlocking {
        val dao = FakeScriptDao()
        val repository = ScriptRepository(dao) { 500L }
        val initial = repository.getOrCreateDefault()
        val unchanged = repository.save(initial.id, initial.name, initial.sourceCode)
        assertEquals(initial, unchanged)
        assertEquals(1, repository.getVersions(initial.id).size)
    }

    @Test
    fun blankNameKeepsExistingNameAndUsesFallbackForNewScript() = runBlocking {
        val repository = ScriptRepository(FakeScriptDao()) { 500L }
        val initial = repository.getOrCreateDefault()
        val updated = repository.save(initial.id, "   ", "print('updated')")
        val created = repository.save("another", "  ", "")
        assertEquals(initial.name, updated.name)
        assertEquals("Script sem título", created.name)
    }

    @Test
    fun createAndDuplicatePreserveMetadataWithNewIdentity() = runBlocking {
        val repository = ScriptRepository(FakeScriptDao()) { 700L }
        val created = repository.create(
            name = "Coletor",
            sourceCode = "print(params)",
            description = "Teste",
            tagsJson = "[\"api\", \"dados\"]",
            parametersJson = "[{\"name\":\"limit\",\"type\":\"integer\",\"required\":true}]",
            dependenciesJson = "[\"requests>=2.31\"]"
        )
        val copy = repository.duplicate(created.id)
        assertTrue(created.id != copy.id)
        assertEquals("Cópia de Coletor", copy.name)
        assertEquals(created.sourceCode, copy.sourceCode)
        assertEquals(created.tagsJson, copy.tagsJson)
        assertEquals(1, repository.getVersions(copy.id).size)
    }

    @Test
    fun restoringVersionCreatesANewSnapshotWithoutOverwritingHistory() = runBlocking {
        val repository = ScriptRepository(FakeScriptDao()) { 750L }
        val script = repository.create("Versioned", sourceCode = "print('v1')")
        repository.save(script.id, script.name, "print('v2')")
        val restored = repository.restoreVersion(script.id, 1)
        assertEquals("print('v1')", restored.sourceCode)
        assertEquals(listOf(3, 2, 1), repository.getVersions(script.id).map { it.versionNumber })
        assertEquals("print('v2')", repository.getVersions(script.id)[1].sourceCode)
    }

    @Test
    fun deleteRemovesScriptAndVersions() = runBlocking {
        val repository = ScriptRepository(FakeScriptDao()) { 800L }
        val script = repository.create("Temporary", sourceCode = "print(1)")
        repository.save(script.id, script.name, "print(2)")
        assertEquals(2, repository.getVersions(script.id).size)
        repository.delete(script.id)
        assertEquals(null, repository.getById(script.id))
        assertTrue(repository.getVersions(script.id).isEmpty())
    }

    @Test
    fun sourceOnlySavePreservesExistingMetadata() = runBlocking {
        val repository = ScriptRepository(FakeScriptDao()) { 900L }
        val script = repository.create(
            "Metadata", description = "Keep me", tagsJson = "[\"tag\"]",
            parametersJson = "[{\"name\":\"limit\",\"type\":\"integer\"}]"
        )
        val saved = repository.save(script.id, script.name, "print(2)")
        assertEquals(script.description, saved.description)
        assertEquals(script.tagsJson, saved.tagsJson)
        assertEquals(script.parametersJson, saved.parametersJson)
    }

    @Test
    fun invalidParameterAndDependencyManifestsAreRejected() = runBlocking {
        val repository = ScriptRepository(FakeScriptDao())
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                repository.create("Invalid", parametersJson = "[{\"name\":\"bad-name\"}]")
            }
        }
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                repository.create("Invalid", dependenciesJson = "[\"--index-url=https://example.com\"]")
            }
        }
            Unit
    }

    @Test
    fun databaseErrorsAreWrapped() {
        val dao = FakeScriptDao().apply { readFailure = IllegalStateException("database unavailable") }
        val repository = ScriptRepository(dao)
        assertThrows(ScriptStorageException::class.java) {
            runBlocking { repository.getOrCreateDefault() }
        }
    }

    @Test
    fun coroutineCancellationIsNotWrappedAsStorageFailure() {
        val dao = FakeScriptDao().apply { readFailure = CancellationException("cancelled") }
        val repository = ScriptRepository(dao)
        assertThrows(CancellationException::class.java) {
            runBlocking { repository.getOrCreateDefault() }
        }
    }

    @Test
    fun observeErrorsAreWrapped() {
        val dao = FakeScriptDao().apply { observeFailure = IllegalStateException("database stream failed") }
        val repository = ScriptRepository(dao)
        assertThrows(ScriptStorageException::class.java) {
            runBlocking { repository.observeAll().first() }
        }
    }

    @Test
    fun blankScriptIdIsRejectedAsInputError() {
        val repository = ScriptRepository(FakeScriptDao())
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { repository.save(" ", "Name", "print(1)") }
        }
    }

    private class FakeScriptDao : ScriptDao {
        private val records = linkedMapOf<String, ScriptEntity>()
        private val versions = linkedMapOf<String, MutableList<ScriptVersionEntity>>()
        private val state = MutableStateFlow<List<ScriptEntity>>(emptyList())
        private val versionStates = mutableMapOf<String, MutableStateFlow<List<ScriptVersionEntity>>>()
        var readFailure: Throwable? = null
        var writeFailure: Throwable? = null
        var observeFailure: Throwable? = null
        val size: Int get() = records.size

        override suspend fun getById(id: String): ScriptEntity? {
            readFailure?.let { throw it }
            return records[id]
        }

        override suspend fun insertIfAbsent(script: ScriptEntity) {
            writeFailure?.let { throw it }
            if (!records.containsKey(script.id)) {
                records[script.id] = script
                publish()
            }
        }

        override suspend fun upsert(script: ScriptEntity) {
            writeFailure?.let { throw it }
            records[script.id] = script
            publish()
        }

        override fun observeAll(): Flow<List<ScriptEntity>> {
            val failure = observeFailure
            return if (failure == null) state else flow { throw failure }
        }

        override suspend fun deleteById(id: String) {
            records.remove(id)
            publish()
        }

        override suspend fun nextVersionNumber(scriptId: String): Int =
            (versions[scriptId]?.maxOfOrNull { it.versionNumber } ?: 0) + 1

        override fun observeVersions(scriptId: String): Flow<List<ScriptVersionEntity>> =
            versionStates.getOrPut(scriptId) {
                MutableStateFlow(versions[scriptId].orEmpty().sortedByDescending { it.versionNumber })
            }

        override suspend fun getVersions(scriptId: String): List<ScriptVersionEntity> =
            versions[scriptId].orEmpty().sortedByDescending { it.versionNumber }

        override suspend fun insertVersionIfAbsent(version: ScriptVersionEntity) {
            val items = versions.getOrPut(version.scriptId) { mutableListOf() }
            if (items.none { it.versionNumber == version.versionNumber }) items.add(version)
            publishVersions(version.scriptId)
        }

        override suspend fun insertVersion(version: ScriptVersionEntity) {
            val items = versions.getOrPut(version.scriptId) { mutableListOf() }
            items.removeAll { it.versionNumber == version.versionNumber }
            items.add(version)
            publishVersions(version.scriptId)
        }

        override suspend fun deleteVersions(scriptId: String) {
            versions.remove(scriptId)
            publishVersions(scriptId)
        }

        override suspend fun upsertWithVersion(script: ScriptEntity, version: ScriptVersionEntity) {
            upsert(script)
            insertVersion(version)
        }

        override suspend fun insertDefaultIfAbsent(script: ScriptEntity, version: ScriptVersionEntity) {
            insertIfAbsent(script)
            insertVersionIfAbsent(version)
        }

        override suspend fun deleteScriptAndVersions(scriptId: String) {
            deleteVersions(scriptId)
            deleteById(scriptId)
        }

        private fun publish() { state.value = records.values.sortedByDescending { it.updatedAtEpochMs } }
        private fun publishVersions(id: String) {
            versionStates[id]?.value = versions[id].orEmpty().sortedByDescending { it.versionNumber }
        }
    }
}

package com.samuel.androidepy.data

import java.util.concurrent.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ScriptRepositoryTest {

    @Test
    fun createsDefaultScriptOnlyOnce() = runBlocking {
        val dao = FakeScriptDao()
        var timestamp = 100L
        val repository = ScriptRepository(dao) { timestamp++ }

        val first = repository.getOrCreateDefault()
        val second = repository.getOrCreateDefault()

        assertEquals(first, second)
        assertEquals(1, dao.size)
        assertEquals("Meu primeiro script", first.name)
        assertEquals("print(\"Olá, Androide PY!\")", first.sourceCode)
        assertEquals(100L, first.createdAtEpochMs)
    }

    @Test
    fun savePreservesCreationTimeAndUpdatesModificationTime() = runBlocking {
        val dao = FakeScriptDao()
        var timestamp = 100L
        val repository = ScriptRepository(dao) { timestamp++ }
        val initial = repository.getOrCreateDefault()

        val saved = repository.save(
            scriptId = ScriptRepository.DEFAULT_SCRIPT_ID,
            name = "  Meu script novo  ",
            sourceCode = "print(2)"
        )

        assertEquals(initial.createdAtEpochMs, saved.createdAtEpochMs)
        assertEquals(101L, saved.updatedAtEpochMs)
        assertEquals("Meu script novo", saved.name)
        assertEquals("print(2)", saved.sourceCode)
    }

    @Test
    fun blankNameKeepsExistingNameAndUsesFallbackForNewScript() = runBlocking {
        val dao = FakeScriptDao()
        val repository = ScriptRepository(dao) { 500L }
        val initial = repository.getOrCreateDefault()

        val updated = repository.save(
            ScriptRepository.DEFAULT_SCRIPT_ID,
            "   ",
            "print('updated')"
        )
        val created = repository.save("another", "  ", "")

        assertEquals(initial.name, updated.name)
        assertEquals("Script sem título", created.name)
    }

    @Test
    fun databaseErrorsAreWrapped() {
        val dao = FakeScriptDao().apply {
            readFailure = IllegalStateException("database unavailable")
        }
        val repository = ScriptRepository(dao)

        assertThrows(ScriptStorageException::class.java) {
            runBlocking { repository.getOrCreateDefault() }
        }
    }

    @Test
    fun coroutineCancellationIsNotWrappedAsStorageFailure() {
        val dao = FakeScriptDao().apply {
            readFailure = CancellationException("cancelled")
        }
        val repository = ScriptRepository(dao)

        assertThrows(CancellationException::class.java) {
            runBlocking { repository.getOrCreateDefault() }
        }
    }

    @Test
    fun observeErrorsAreWrapped() {
        val dao = FakeScriptDao().apply {
            observeFailure = IllegalStateException("database stream failed")
        }
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
        private val state = MutableStateFlow<List<ScriptEntity>>(emptyList())

        var readFailure: Throwable? = null
        var writeFailure: Throwable? = null
        var observeFailure: Throwable? = null

        val size: Int
            get() = records.size

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
            return if (failure == null) {
                state
            } else {
                flow { throw failure }
            }
        }

        private fun publish() {
            state.value = records.values.sortedByDescending { it.updatedAtEpochMs }
        }
    }
}

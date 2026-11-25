package com.samuel.androidepy.data

import com.samuel.androidepy.execution.ExecutionStatus
import com.samuel.androidepy.execution.ExecutionUpdate
import com.samuel.androidepy.execution.ScriptExecutionError
import com.samuel.androidepy.execution.ScriptExecutionResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ScriptExecutionRepositoryTest {
    @Test
    fun recordsQueueRunningAndTerminalResult() = runBlocking {
        val dao = FakeExecutionDao()
        val repository = ScriptExecutionRepository(dao)
        val script = ScriptEntity(
            id = "script-1",
            name = "Example",
            sourceCode = "print(params)",
            createdAtEpochMs = 10L,
            updatedAtEpochMs = 11L
        )

        repository.recordQueued(script, "exec-1", 3, """{"limit":5}""", 12L)
        assertEquals("QUEUED", dao.records["exec-1"]?.status)
        assertEquals(3, dao.records["exec-1"]?.scriptVersionNumber)
        assertEquals("""{"limit":5}""", dao.records["exec-1"]?.parametersJson)

        repository.recordUpdate(ExecutionUpdate("exec-1", ExecutionStatus.RUNNING))
        assertNotNull(dao.records["exec-1"]?.startedAtEpochMs)

        val result = ScriptExecutionResult(
            executionId = "exec-1",
            scriptId = "script-1",
            status = ExecutionStatus.FAILED,
            stdout = "partial",
            stderr = "warning",
            error = ScriptExecutionError("ValueError", "bad input", "trace"),
            startedAtEpochMs = 20L,
            finishedAtEpochMs = 25L,
            durationMs = 5L,
            stdoutTruncated = false,
            stderrTruncated = false
        )
        repository.recordUpdate(ExecutionUpdate("exec-1", ExecutionStatus.FAILED, result))
        val saved = dao.records.getValue("exec-1")
        assertEquals("FAILED", saved.status)
        assertEquals("partial", saved.stdout)
        assertEquals("warning", saved.stderr)
        assertEquals(5L, saved.durationMs)
        assertEquals("ValueError", org.json.JSONObject(saved.errorJson).getString("type"))
    }

    private class FakeExecutionDao : ScriptExecutionDao {
        val records = linkedMapOf<String, ScriptExecutionEntity>()

        override suspend fun upsert(execution: ScriptExecutionEntity) {
            records[execution.executionId] = execution
        }

        override suspend fun getById(executionId: String): ScriptExecutionEntity? = records[executionId]

        override fun observeForScript(scriptId: String): Flow<List<ScriptExecutionEntity>> = emptyFlow()
    }
}

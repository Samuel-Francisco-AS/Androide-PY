package com.samuel.androidepy.data

import com.samuel.androidepy.execution.ExecutionUpdate
import kotlinx.coroutines.flow.Flow
import org.json.JSONObject

class ScriptExecutionRepository(private val dao: ScriptExecutionDao) {
    fun observeForScript(scriptId: String): Flow<List<ScriptExecutionEntity>> =
        dao.observeForScript(scriptId)

    suspend fun recordQueued(
        script: ScriptEntity,
        executionId: String,
        versionNumber: Int?,
        parametersJson: String,
        queuedAtEpochMs: Long
    ) {
        dao.upsert(
            ScriptExecutionEntity(
                executionId = executionId,
                scriptId = script.id,
                scriptNameSnapshot = script.name,
                scriptVersionNumber = versionNumber,
                status = "QUEUED",
                queuedAtEpochMs = queuedAtEpochMs,
                startedAtEpochMs = null,
                finishedAtEpochMs = null,
                durationMs = null,
                parametersJson = parametersJson,
                stdout = "",
                stderr = "",
                errorJson = null,
                stdoutTruncated = false,
                stderrTruncated = false
            )
        )
    }

    suspend fun recordUpdate(update: ExecutionUpdate) {
        val previous = dao.getById(update.executionId) ?: return
        val result = update.result
        val errorJson = result?.error?.let {
            JSONObject().put("type", it.type).put("message", it.message)
                .put("traceback", it.traceback).toString()
        }
        dao.upsert(
            previous.copy(
                status = update.status.name,
                startedAtEpochMs = result?.startedAtEpochMs ?: previous.startedAtEpochMs,
                finishedAtEpochMs = result?.finishedAtEpochMs ?: previous.finishedAtEpochMs,
                durationMs = result?.durationMs ?: previous.durationMs,
                stdout = result?.stdout ?: previous.stdout,
                stderr = result?.stderr ?: previous.stderr,
                errorJson = errorJson ?: previous.errorJson,
                stdoutTruncated = result?.stdoutTruncated ?: previous.stdoutTruncated,
                stderrTruncated = result?.stderrTruncated ?: previous.stderrTruncated
            )
        )
    }
}

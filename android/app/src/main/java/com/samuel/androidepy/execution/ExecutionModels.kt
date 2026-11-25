package com.samuel.androidepy.execution

import org.json.JSONObject
import java.util.UUID

enum class ExecutionStatus { QUEUED, RUNNING, SUCCEEDED, FAILED, CANCELLED }

data class ScriptExecutionRequest(
    val sourceCode: String,
    val scriptId: String = "scratch",
    val parametersJson: String = "{}",
    val executionId: String = UUID.randomUUID().toString(),
    val queuedAtEpochMs: Long = System.currentTimeMillis()
)

data class ScriptExecutionError(val type: String, val message: String, val traceback: String)

data class ScriptExecutionResult(
    val executionId: String,
    val scriptId: String,
    val status: ExecutionStatus,
    val stdout: String,
    val stderr: String,
    val error: ScriptExecutionError?,
    val startedAtEpochMs: Long,
    val finishedAtEpochMs: Long,
    val durationMs: Long,
    val stdoutTruncated: Boolean,
    val stderrTruncated: Boolean
) {
    companion object {
        fun fromJson(json: String): ScriptExecutionResult {
            val root = JSONObject(json)
            val schemaVersion = root.optInt("schema_version", 1)
            require(schemaVersion == 1) { "Unsupported Python execution result schema: $schemaVersion" }
            val errorObject = root.optJSONObject("error")
            val error = errorObject?.let {
                ScriptExecutionError(it.optString("type"), it.optString("message"), it.optString("traceback"))
            }
            return ScriptExecutionResult(
                executionId = root.getString("execution_id"),
                scriptId = root.optString("script_id", "scratch"),
                status = ExecutionStatus.valueOf(root.getString("status")),
                stdout = root.optString("stdout", ""),
                stderr = root.optString("stderr", ""),
                error = error,
                startedAtEpochMs = root.optLong("started_at_ms"),
                finishedAtEpochMs = root.optLong("finished_at_ms"),
                durationMs = root.optLong("duration_ms"),
                stdoutTruncated = root.optBoolean("stdout_truncated"),
                stderrTruncated = root.optBoolean("stderr_truncated")
            )
        }
    }
}

data class ExecutionUpdate(
    val executionId: String,
    val status: ExecutionStatus,
    val result: ScriptExecutionResult? = null,
    val message: String? = null
)

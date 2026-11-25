package com.samuel.androidepy.execution

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ExecutionModelsTest {

    @Test
    fun parsesSuccessfulExecution() {
        val json = """
            {
              "schema_version": 1,
              "execution_id": "exec-1",
              "script_id": "scratch",
              "status": "SUCCEEDED",
              "stdout": "hello\n",
              "stderr": "",
              "error": null,
              "started_at_ms": 100,
              "finished_at_ms": 125,
              "duration_ms": 25,
              "stdout_truncated": false,
              "stderr_truncated": false
            }
        """.trimIndent()

        val result = ScriptExecutionResult.fromJson(json)

        assertEquals("exec-1", result.executionId)
        assertEquals("scratch", result.scriptId)
        assertEquals(ExecutionStatus.SUCCEEDED, result.status)
        assertEquals("hello\n", result.stdout)
        assertEquals("", result.stderr)
        assertNull(result.error)
        assertEquals(25L, result.durationMs)
        assertFalse(result.stdoutTruncated)
    }

    @Test
    fun parsesStructuredPythonException() {
        val json = """
            {
              "schema_version": 1,
              "execution_id": "exec-2",
              "script_id": "scratch",
              "status": "FAILED",
              "stdout": "before\n",
              "stderr": "warning\n",
              "error": {
                "type": "ValueError",
                "message": "boom",
                "traceback": "Traceback...\nValueError: boom"
              },
              "started_at_ms": 200,
              "finished_at_ms": 205,
              "duration_ms": 5,
              "stdout_truncated": false,
              "stderr_truncated": true
            }
        """.trimIndent()

        val result = ScriptExecutionResult.fromJson(json)

        assertEquals(ExecutionStatus.FAILED, result.status)
        assertEquals("before\n", result.stdout)
        assertEquals("warning\n", result.stderr)
        assertNotNull(result.error)
        assertEquals("ValueError", result.error?.type)
        assertEquals("boom", result.error?.message)
        assertTrue(result.stderrTruncated)
    }

    @Test
    fun rejectsUnknownContractVersion() {
        val json = """{"schema_version": 2}"""

        assertThrows(IllegalArgumentException::class.java) {
            ScriptExecutionResult.fromJson(json)
        }
    }
}

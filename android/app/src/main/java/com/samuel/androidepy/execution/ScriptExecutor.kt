package com.samuel.androidepy.execution

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.samuel.androidepy.runtime.PythonRuntime
import java.io.Closeable
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * Serializes Python executions on one worker thread.
 *
 * Closing this manager stops new submissions but deliberately does not
 * interrupt a running Python thread.
 */
class ScriptExecutor(context: Context) : Closeable {

    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val worker = ThreadPoolExecutor(
        1,
        1,
        0L,
        TimeUnit.MILLISECONDS,
        ArrayBlockingQueue(QUEUE_CAPACITY),
        { task ->
            Thread(task, "AndroidePY-Python").apply {
                isDaemon = true
            }
        },
        ThreadPoolExecutor.AbortPolicy()
    )

    fun submit(
        request: ScriptExecutionRequest,
        onUpdate: (ExecutionUpdate) -> Unit
    ): String {
        dispatch(
            onUpdate,
            ExecutionUpdate(
                executionId = request.executionId,
                status = ExecutionStatus.QUEUED
            )
        )

        try {
            worker.execute {
                dispatch(
                    onUpdate,
                    ExecutionUpdate(
                        executionId = request.executionId,
                        status = ExecutionStatus.RUNNING
                    )
                )

                val startedAt = System.currentTimeMillis()
                val result = try {
                    PythonRuntime.initialize(appContext)
                    val json = PythonRuntime
                        .getModule("runner")
                        .callAttr(
                            "execute",
                            request.sourceCode,
                            request.executionId,
                            request.scriptId
                        )
                        .toString()

                    ScriptExecutionResult.fromJson(json)
                } catch (error: Exception) {
                    val finishedAt = System.currentTimeMillis()
                    Log.e(TAG, "Execution ${request.executionId} failed in the host", error)

                    ScriptExecutionResult(
                        executionId = request.executionId,
                        scriptId = request.scriptId,
                        status = ExecutionStatus.FAILED,
                        stdout = "",
                        stderr = "",
                        error = ScriptExecutionError(
                            type = error.javaClass.simpleName,
                            message = error.message ?: "Unexpected host execution error.",
                            traceback = Log.getStackTraceString(error)
                        ),
                        startedAtEpochMs = startedAt,
                        finishedAtEpochMs = finishedAt,
                        durationMs = (finishedAt - startedAt).coerceAtLeast(0),
                        stdoutTruncated = false,
                        stderrTruncated = false
                    )
                }

                dispatch(
                    onUpdate,
                    ExecutionUpdate(
                        executionId = request.executionId,
                        status = result.status,
                        result = result
                    )
                )
            }
        } catch (rejected: RejectedExecutionException) {
            val now = System.currentTimeMillis()
            val result = ScriptExecutionResult(
                executionId = request.executionId,
                scriptId = request.scriptId,
                status = ExecutionStatus.FAILED,
                stdout = "",
                stderr = "",
                error = ScriptExecutionError(
                    type = rejected.javaClass.simpleName,
                    message = rejected.message ?: "The execution queue is unavailable.",
                    traceback = Log.getStackTraceString(rejected)
                ),
                startedAtEpochMs = now,
                finishedAtEpochMs = now,
                durationMs = 0,
                stdoutTruncated = false,
                stderrTruncated = false
            )
            dispatch(
                onUpdate,
                ExecutionUpdate(
                    executionId = request.executionId,
                    status = ExecutionStatus.FAILED,
                    result = result
                )
            )
        }

        return request.executionId
    }

    override fun close() {
        worker.shutdown()
    }

    private fun dispatch(
        callback: (ExecutionUpdate) -> Unit,
        update: ExecutionUpdate
    ) {
        mainHandler.post {
            callback(update)
        }
    }

    private companion object {
        const val TAG = "AndroidePY.Executor"
        const val QUEUE_CAPACITY = 16
    }
}

package com.samuel.androidepy.execution

import android.content.Context
import android.util.Log
import com.samuel.androidepy.bridge.AndroidBridgeAdapter
import com.samuel.androidepy.runtime.PythonRuntime
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * Application-scoped serial executor for Python scripts.
 *
 * Updates are published as a hot flow rather than Activity callbacks, so
 * configuration changes do not retain a destroyed Activity or create a
 * second Python worker.
 */
class ScriptExecutor(context: Context) {

    private val appContext = context.applicationContext

    private val mutableUpdates = MutableSharedFlow<ExecutionUpdate>(
        replay = 1,
        extraBufferCapacity = EVENT_BUFFER_CAPACITY,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    val updates: SharedFlow<ExecutionUpdate> = mutableUpdates.asSharedFlow()

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

    fun submit(request: ScriptExecutionRequest): String {
        publish(
            ExecutionUpdate(
                executionId = request.executionId,
                status = ExecutionStatus.QUEUED
            )
        )

        try {
            worker.execute {
                publish(
                    ExecutionUpdate(
                        executionId = request.executionId,
                        status = ExecutionStatus.RUNNING
                    )
                )

                val startedAt = System.currentTimeMillis()
                val result = try {
                    PythonRuntime.initialize(appContext)
                    PythonRuntime.configureBridge(AndroidBridgeAdapter(appContext))
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

                publish(
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
                    message = rejected.message ?: "The execution queue is full or unavailable.",
                    traceback = Log.getStackTraceString(rejected)
                ),
                startedAtEpochMs = now,
                finishedAtEpochMs = now,
                durationMs = 0,
                stdoutTruncated = false,
                stderrTruncated = false
            )
            publish(
                ExecutionUpdate(
                    executionId = request.executionId,
                    status = ExecutionStatus.FAILED,
                    result = result
                )
            )
        }

        return request.executionId
    }

    private fun publish(update: ExecutionUpdate) {
        mutableUpdates.tryEmit(update)
    }

    private companion object {
        const val TAG = "AndroidePY.Executor"
        const val QUEUE_CAPACITY = 16
        const val EVENT_BUFFER_CAPACITY = 64
    }
}

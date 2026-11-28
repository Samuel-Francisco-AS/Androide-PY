package com.samuel.androidepy.accessibility

import android.os.Handler
import android.os.SystemClock

internal enum class CoordinateCaptureState {
    IDLE,
    STARTING,
    ACTIVE,
    STOPPING,
    FAILED
}

internal enum class CoordinateCaptureOutcome {
    COMPLETED,
    CANCELLED,
    EXPIRED,
    FAILED
}

internal enum class CoordinateCaptureFailure {
    SERVICE_INTERRUPTED,
    SERVICE_DISCONNECTED,
    OVERLAY_CREATION_FAILED,
    OVERLAY_REMOVAL_FAILED,
    INTERNAL_ERROR
}

internal enum class CoordinateCaptureStartResult {
    STARTED,
    SERVICE_UNAVAILABLE,
    ALREADY_RUNNING,
    INVALID_DURATION,
    INVALID_TARGET,
    OVERLAY_CREATION_FAILED,
    INTERNAL_ERROR
}

internal data class CoordinateCaptureSnapshot(
    val state: CoordinateCaptureState,
    val targetPackage: String?,
    val durationSeconds: Int?,
    val remainingSeconds: Int,
    val lastOutcome: CoordinateCaptureOutcome?,
    val lastFailure: CoordinateCaptureFailure?
)

internal interface CoordinateCaptureOverlay {
    fun show(durationSeconds: Int, onCancel: () -> Unit)
    fun showSelection(
        durationSeconds: Int,
        onPoint: (String, Int, Int) -> Unit,
        onRemoveLast: () -> Unit,
        onFinish: () -> Unit,
        onCancel: () -> Unit
    ) = show(durationSeconds, onCancel)
    fun updateRemainingSeconds(remainingSeconds: Int)
    fun remove()
}

internal interface CoordinateCaptureScheduler {
    fun elapsedRealtime(): Long
    fun postDelayed(task: Runnable, delayMs: Long): Boolean
    fun removeCallbacks(task: Runnable)
}

internal class HandlerCoordinateCaptureScheduler(
    private val handler: Handler
) : CoordinateCaptureScheduler {
    override fun elapsedRealtime(): Long = SystemClock.elapsedRealtime()

    override fun postDelayed(task: Runnable, delayMs: Long): Boolean =
        handler.postDelayed(task, delayMs)

    override fun removeCallbacks(task: Runnable) {
        handler.removeCallbacks(task)
    }
}

/**
 * Owns one explicit, time-bounded capture session.
 *
 * All methods are called on the Android main thread by the owning service. The
 * scheduler and overlay are injected so the state machine can be tested on the
 * local JVM without constructing Android framework objects.
 */
internal class CoordinateCaptureController(
    private val overlay: CoordinateCaptureOverlay,
    private val scheduler: CoordinateCaptureScheduler
) {
    private var state = CoordinateCaptureState.IDLE
    private var targetPackage: String? = null
    private var durationSeconds: Int? = null
    private var deadlineElapsedRealtime: Long? = null
    private var remainingSeconds = 0
    private var generation = 0L
    private var tickTask: Runnable? = null
    private var lastOutcome: CoordinateCaptureOutcome? = null
    private var lastFailure: CoordinateCaptureFailure? = null
    private var terminalCallback: ((CoordinateCaptureOutcome, CoordinateCaptureFailure?) -> Unit)? = null

    val snapshot: CoordinateCaptureSnapshot
        get() = CoordinateCaptureSnapshot(
            state = state,
            targetPackage = targetPackage,
            durationSeconds = durationSeconds,
            remainingSeconds = remainingSeconds,
            lastOutcome = lastOutcome,
            lastFailure = lastFailure
        )

    fun start(
        targetPackage: String?,
        durationSeconds: Int = DEFAULT_DURATION_SECONDS,
        onPoint: ((String, Int, Int) -> Unit)? = null,
        onRemoveLast: (() -> Unit)? = null,
        onFinish: (() -> Unit)? = null,
        onTerminal: ((CoordinateCaptureOutcome, CoordinateCaptureFailure?) -> Unit)? = null
    ): CoordinateCaptureStartResult {
        if (state != CoordinateCaptureState.IDLE) {
            return CoordinateCaptureStartResult.ALREADY_RUNNING
        }
        if (durationSeconds !in MIN_DURATION_SECONDS..MAX_DURATION_SECONDS) {
            return CoordinateCaptureStartResult.INVALID_DURATION
        }
        val normalizedTarget = targetPackage
            ?.trim()
            ?.takeIf { PACKAGE_NAME.matches(it) }
            ?: return CoordinateCaptureStartResult.INVALID_TARGET

        state = CoordinateCaptureState.STARTING
        this.targetPackage = normalizedTarget
        this.durationSeconds = durationSeconds
        remainingSeconds = durationSeconds
        lastOutcome = null
        lastFailure = null
        terminalCallback = onTerminal
        val sessionGeneration = ++generation

        try {
            if (onPoint != null && onRemoveLast != null && onFinish != null) {
                overlay.showSelection(
                    durationSeconds,
                    onPoint,
                    onRemoveLast,
                    { onFinish.invoke() ; complete() }
                ) { cancel(sessionGeneration) }
            } else {
                overlay.show(durationSeconds) { cancel(sessionGeneration) }
            }
        } catch (_: Exception) {
            finish(
                CoordinateCaptureOutcome.FAILED,
                CoordinateCaptureFailure.OVERLAY_CREATION_FAILED
            )
            return CoordinateCaptureStartResult.OVERLAY_CREATION_FAILED
        }

        // The monotonic deadline starts only after WindowManager.addView succeeds.
        val durationMs = durationSeconds * MILLIS_PER_SECOND
        deadlineElapsedRealtime = scheduler.elapsedRealtime() + durationMs
        state = CoordinateCaptureState.ACTIVE
        if (!updateTimerAndSchedule(sessionGeneration)) {
            return CoordinateCaptureStartResult.INTERNAL_ERROR
        }
        return CoordinateCaptureStartResult.STARTED
    }

    fun complete() {
        if (state == CoordinateCaptureState.ACTIVE) {
            finish(CoordinateCaptureOutcome.COMPLETED, null)
        }
    }

    fun expire() {
        if (state == CoordinateCaptureState.ACTIVE || state == CoordinateCaptureState.STARTING) {
            finish(CoordinateCaptureOutcome.EXPIRED, null)
        }
    }

    fun cancel() {
        when (state) {
            CoordinateCaptureState.ACTIVE ->
                finish(CoordinateCaptureOutcome.CANCELLED, null)
            CoordinateCaptureState.FAILED -> retryOverlayRemoval()
            else -> Unit
        }
    }

    fun onServiceInterrupted() {
        terminateForServiceFailure(CoordinateCaptureFailure.SERVICE_INTERRUPTED)
    }

    fun onServiceDisconnected() {
        terminateForServiceFailure(CoordinateCaptureFailure.SERVICE_DISCONNECTED)
    }

    /**
     * Retries a previously failed removal. Safe to call from both onUnbind and
     * onDestroy; it never creates or reattaches a window.
     */
    fun shutdown() {
        when (state) {
            CoordinateCaptureState.ACTIVE,
            CoordinateCaptureState.STARTING ->
                terminateForServiceFailure(CoordinateCaptureFailure.SERVICE_DISCONNECTED)
            CoordinateCaptureState.FAILED -> retryOverlayRemoval()
            else -> Unit
        }
    }

    private fun cancel(sessionGeneration: Long) {
        when {
            sessionGeneration == generation && state == CoordinateCaptureState.ACTIVE ->
                finish(CoordinateCaptureOutcome.CANCELLED, null)
            state == CoordinateCaptureState.FAILED -> retryOverlayRemoval()
        }
    }

    private fun terminateForServiceFailure(failure: CoordinateCaptureFailure) {
        if (state == CoordinateCaptureState.ACTIVE ||
            state == CoordinateCaptureState.STARTING
        ) {
            finish(CoordinateCaptureOutcome.FAILED, failure)
        } else if (state == CoordinateCaptureState.FAILED) {
            retryOverlayRemoval()
        }
    }

    private fun updateTimerAndSchedule(sessionGeneration: Long): Boolean {
        if (sessionGeneration != generation || state != CoordinateCaptureState.ACTIVE) {
            return false
        }

        val deadline = deadlineElapsedRealtime ?: return false
        val remainingMs = deadline - scheduler.elapsedRealtime()
        if (remainingMs <= 0L) {
            finish(CoordinateCaptureOutcome.EXPIRED, null)
            return false
        }

        val nextRemaining = ((remainingMs + MILLIS_PER_SECOND - 1L) /
            MILLIS_PER_SECOND).toInt()
        remainingSeconds = nextRemaining
        try {
            overlay.updateRemainingSeconds(nextRemaining)
        } catch (_: Exception) {
            finish(
                CoordinateCaptureOutcome.FAILED,
                CoordinateCaptureFailure.INTERNAL_ERROR
            )
            return false
        }

        val task = Runnable { updateTimerAndSchedule(sessionGeneration) }
        tickTask = task
        val delay = minOf(TIMER_REFRESH_MS, remainingMs)
        if (!scheduler.postDelayed(task, delay)) {
            finish(
                CoordinateCaptureOutcome.FAILED,
                CoordinateCaptureFailure.INTERNAL_ERROR
            )
            return false
        }
        return true
    }

    private fun finish(
        outcome: CoordinateCaptureOutcome,
        failure: CoordinateCaptureFailure?
    ) {
        if (state != CoordinateCaptureState.ACTIVE &&
            state != CoordinateCaptureState.STARTING &&
            state != CoordinateCaptureState.FAILED
        ) {
            return
        }

        state = CoordinateCaptureState.STOPPING
        generation += 1
        cancelTimer()
        deadlineElapsedRealtime = null
        remainingSeconds = 0

        lastOutcome = outcome
        lastFailure = failure
        removeOverlayOrRetainFailedState()
    }

    private fun retryOverlayRemoval() {
        state = CoordinateCaptureState.STOPPING
        generation += 1
        cancelTimer()
        deadlineElapsedRealtime = null
        remainingSeconds = 0
        removeOverlayOrRetainFailedState()
    }

    private fun removeOverlayOrRetainFailedState() {
        try {
            overlay.remove()
            state = CoordinateCaptureState.IDLE
            targetPackage = null
            durationSeconds = null
            terminalCallback?.invoke(lastOutcome ?: CoordinateCaptureOutcome.FAILED, lastFailure)
            terminalCallback = null
        } catch (_: Exception) {
            state = CoordinateCaptureState.FAILED
            lastOutcome = CoordinateCaptureOutcome.FAILED
            lastFailure = CoordinateCaptureFailure.OVERLAY_REMOVAL_FAILED
            terminalCallback?.invoke(CoordinateCaptureOutcome.FAILED, lastFailure)
            terminalCallback = null
        }
    }

    private fun cancelTimer() {
        tickTask?.let(scheduler::removeCallbacks)
        tickTask = null
    }

    companion object {
        const val MIN_DURATION_SECONDS = 15
        const val DEFAULT_DURATION_SECONDS = 120
        const val MAX_DURATION_SECONDS = 300

        private const val MILLIS_PER_SECOND = 1_000L
        private const val TIMER_REFRESH_MS = 1_000L
        private val PACKAGE_NAME =
            Regex("[A-Za-z][A-Za-z0-9_]*(?:\\.[A-Za-z][A-Za-z0-9_]*)+")
    }
}

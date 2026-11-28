package com.samuel.androidepy.accessibility

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CoordinateCaptureControllerTest {

    @Test
    fun startsWithValidTargetAndKeepsTargetSnapshot() {
        val tracker = CoordinateCaptureTargetTracker("com.samuel.androidepy")
        tracker.record("com.example.target")
        val fixture = Fixture()
        val result = fixture.controller.start(tracker.snapshot(), 30)

        assertEquals(CoordinateCaptureStartResult.STARTED, result)
        assertEquals(CoordinateCaptureState.ACTIVE, fixture.controller.snapshot.state)
        assertEquals("com.example.target", fixture.controller.snapshot.targetPackage)
        assertEquals(30, fixture.controller.snapshot.durationSeconds)
        assertEquals(30, fixture.controller.snapshot.remainingSeconds)

        // A later foreground package observation cannot rewrite the active session.
        tracker.record("com.example.other")
        assertEquals("com.example.target", fixture.controller.snapshot.targetPackage)
    }

    @Test
    fun rejectsInvalidDurationAndTargetWithoutShowingOverlay() {
        val fixture = Fixture()

        assertEquals(
            CoordinateCaptureStartResult.INVALID_DURATION,
            fixture.controller.start("com.example.target", 14)
        )
        assertEquals(
            CoordinateCaptureStartResult.INVALID_DURATION,
            fixture.controller.start("com.example.target", 301)
        )
        assertEquals(
            CoordinateCaptureStartResult.INVALID_TARGET,
            fixture.controller.start("not a package", 120)
        )
        assertEquals(0, fixture.overlay.showCount)
        assertEquals(CoordinateCaptureState.IDLE, fixture.controller.snapshot.state)
    }

    @Test
    fun rejectsConcurrentSessionWithoutCreatingAnotherOverlay() {
        val fixture = Fixture()
        assertEquals(
            CoordinateCaptureStartResult.STARTED,
            fixture.controller.start("com.example.target", 60)
        )

        assertEquals(
            CoordinateCaptureStartResult.ALREADY_RUNNING,
            fixture.controller.start("com.example.other", 60)
        )
        assertEquals(1, fixture.overlay.showCount)
        assertEquals("com.example.target", fixture.controller.snapshot.targetPackage)
    }

    @Test
    fun countdownUsesMonotonicDeadlineAndExpiresAtConfiguredDuration() {
        val fixture = Fixture()
        fixture.controller.start("com.example.target", 15)

        fixture.scheduler.advanceBy(5_500L)
        assertEquals(10, fixture.controller.snapshot.remainingSeconds)
        assertEquals(CoordinateCaptureState.ACTIVE, fixture.controller.snapshot.state)

        fixture.scheduler.advanceBy(9_500L)
        assertEquals(CoordinateCaptureState.IDLE, fixture.controller.snapshot.state)
        assertEquals(CoordinateCaptureOutcome.EXPIRED, fixture.controller.snapshot.lastOutcome)
        assertEquals(1, fixture.overlay.removeCount)
        assertFalse(fixture.scheduler.hasPendingTasks())
    }

    @Test
    fun deadlineStartsAfterOverlayWasSuccessfullyAttached() {
        val fixture = Fixture()
        fixture.overlay.onShow = { fixture.scheduler.advanceClockBy(2_000L) }

        fixture.controller.start("com.example.target", 15)
        fixture.scheduler.advanceBy(14_999L)

        assertEquals(CoordinateCaptureState.ACTIVE, fixture.controller.snapshot.state)
        fixture.scheduler.advanceBy(1L)
        assertEquals(CoordinateCaptureOutcome.EXPIRED, fixture.controller.snapshot.lastOutcome)
    }

    @Test
    fun userCancellationRemovesOverlayAndInvalidatesLateTimerCallbacks() {
        val fixture = Fixture()
        fixture.controller.start("com.example.target", 120)
        val cancel = fixture.overlay.cancelAction

        cancel?.invoke()
        assertEquals(CoordinateCaptureState.IDLE, fixture.controller.snapshot.state)
        assertEquals(CoordinateCaptureOutcome.CANCELLED, fixture.controller.snapshot.lastOutcome)
        assertEquals(1, fixture.overlay.removeCount)
        assertFalse(fixture.scheduler.hasPendingTasks())

        fixture.scheduler.advanceBy(120_000L)
        assertEquals(CoordinateCaptureOutcome.CANCELLED, fixture.controller.snapshot.lastOutcome)
        assertEquals(1, fixture.overlay.removeCount)
    }

    @Test
    fun serviceInterruptionAndDisconnectBothTerminateActiveSession() {
        val interrupted = Fixture()
        interrupted.controller.start("com.example.target", 120)
        interrupted.controller.onServiceInterrupted()
        assertEquals(CoordinateCaptureOutcome.FAILED, interrupted.controller.snapshot.lastOutcome)
        assertEquals(CoordinateCaptureFailure.SERVICE_INTERRUPTED, interrupted.controller.snapshot.lastFailure)
        assertEquals(1, interrupted.overlay.removeCount)

        val disconnected = Fixture()
        disconnected.controller.start("com.example.target", 120)
        disconnected.controller.onServiceDisconnected()
        assertEquals(CoordinateCaptureFailure.SERVICE_DISCONNECTED, disconnected.controller.snapshot.lastFailure)
        assertEquals(1, disconnected.overlay.removeCount)
    }

    @Test
    fun overlayCreationFailureDoesNotLeaveAnActiveSession() {
        val fixture = Fixture()
        fixture.overlay.failShow = true

        assertEquals(
            CoordinateCaptureStartResult.OVERLAY_CREATION_FAILED,
            fixture.controller.start("com.example.target", 120)
        )
        assertEquals(CoordinateCaptureState.IDLE, fixture.controller.snapshot.state)
        assertEquals(CoordinateCaptureFailure.OVERLAY_CREATION_FAILED, fixture.controller.snapshot.lastFailure)
        assertEquals(1, fixture.overlay.removeCount)
        assertFalse(fixture.scheduler.hasPendingTasks())
    }

    @Test
    fun removalFailureKeepsControllerUnavailableUntilCleanupRetrySucceeds() {
        val fixture = Fixture()
        fixture.controller.start("com.example.target", 120)
        fixture.overlay.failRemoveCount = 1

        fixture.controller.cancel()
        assertEquals(CoordinateCaptureState.FAILED, fixture.controller.snapshot.state)
        assertEquals(CoordinateCaptureFailure.OVERLAY_REMOVAL_FAILED, fixture.controller.snapshot.lastFailure)
        assertEquals(
            CoordinateCaptureStartResult.ALREADY_RUNNING,
            fixture.controller.start("com.example.other", 120)
        )

        fixture.controller.shutdown()
        assertEquals(CoordinateCaptureState.IDLE, fixture.controller.snapshot.state)
        assertEquals(CoordinateCaptureOutcome.FAILED, fixture.controller.snapshot.lastOutcome)
        assertNull(fixture.controller.snapshot.targetPackage)
    }

    @Test
    fun repeatedTeardownIsIdempotent() {
        val fixture = Fixture()
        fixture.controller.start("com.example.target", 120)

        fixture.controller.onServiceDisconnected()
        fixture.controller.shutdown()
        fixture.controller.shutdown()

        assertEquals(CoordinateCaptureState.IDLE, fixture.controller.snapshot.state)
        assertEquals(1, fixture.overlay.removeCount)
    }

    @Test
    fun acceptsInclusiveDurationBoundsAndUsesDefaultDuration() {
        val minimum = Fixture()
        assertEquals(
            CoordinateCaptureStartResult.STARTED,
            minimum.controller.start("com.example.target", CoordinateCaptureController.MIN_DURATION_SECONDS)
        )
        assertEquals(15, minimum.controller.snapshot.durationSeconds)
        minimum.controller.cancel()

        val maximum = Fixture()
        assertEquals(
            CoordinateCaptureStartResult.STARTED,
            maximum.controller.start("com.example.target", CoordinateCaptureController.MAX_DURATION_SECONDS)
        )
        assertEquals(300, maximum.controller.snapshot.durationSeconds)
        maximum.controller.cancel()

        val defaulted = Fixture()
        assertEquals(
            CoordinateCaptureStartResult.STARTED,
            defaulted.controller.start("com.example.target")
        )
        assertEquals(120, defaulted.controller.snapshot.durationSeconds)
    }

    @Test
    fun schedulerFailureTerminatesSessionAndRemovesOverlay() {
        val fixture = Fixture()
        fixture.scheduler.rejectNextPost = true

        assertEquals(
            CoordinateCaptureStartResult.INTERNAL_ERROR,
            fixture.controller.start("com.example.target", 120)
        )
        assertEquals(CoordinateCaptureState.IDLE, fixture.controller.snapshot.state)
        assertEquals(CoordinateCaptureFailure.INTERNAL_ERROR, fixture.controller.snapshot.lastFailure)
        assertEquals(1, fixture.overlay.removeCount)
    }

    @Test
    fun expiryWinsOverLateCancelCallback() {
        val fixture = Fixture()
        fixture.controller.start("com.example.target", 15)
        val cancel = fixture.overlay.cancelAction

        fixture.scheduler.advanceBy(15_000L)
        cancel?.invoke()

        assertEquals(CoordinateCaptureOutcome.EXPIRED, fixture.controller.snapshot.lastOutcome)
        assertEquals(1, fixture.overlay.removeCount)
    }

    private class Fixture {
        val scheduler = FakeScheduler()
        val overlay = FakeOverlay()
        val controller = CoordinateCaptureController(overlay, scheduler)
    }

    private class FakeOverlay : CoordinateCaptureOverlay {
        var showCount = 0
        var removeCount = 0
        var failShow = false
        var failRemoveCount = 0
        var onShow: (() -> Unit)? = null
        var cancelAction: (() -> Unit)? = null
        val updates = mutableListOf<Int>()

        override fun show(durationSeconds: Int, onCancel: () -> Unit) {
            showCount += 1
            cancelAction = onCancel
            onShow?.invoke()
            if (failShow) error("simulated overlay creation failure")
        }

        override fun updateRemainingSeconds(remainingSeconds: Int) {
            updates += remainingSeconds
        }

        override fun remove() {
            removeCount += 1
            if (failRemoveCount > 0) {
                failRemoveCount -= 1
                error("simulated overlay removal failure")
            }
        }
    }

    private class FakeScheduler : CoordinateCaptureScheduler {
        private val tasks = linkedMapOf<Runnable, Long>()
        private var now = 10_000L
        var rejectNextPost = false

        override fun elapsedRealtime(): Long = now

        override fun postDelayed(task: Runnable, delayMs: Long): Boolean {
            if (rejectNextPost) {
                rejectNextPost = false
                return false
            }
            tasks[task] = now + delayMs
            return true
        }

        override fun removeCallbacks(task: Runnable) {
            tasks.remove(task)
        }

        fun advanceClockBy(millis: Long) {
            now += millis
        }

        fun advanceBy(millis: Long) {
            val target = now + millis
            while (true) {
                val next = tasks.entries.minByOrNull { it.value } ?: break
                if (next.value > target) break
                tasks.remove(next.key)
                now = next.value
                next.key.run()
            }
            now = target
        }

        fun hasPendingTasks(): Boolean = tasks.isNotEmpty()
    }
}

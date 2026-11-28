package com.samuel.androidepy.accessibility

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CoordinateCaptureTargetTrackerTest {

    @Test
    fun recordsOnlyValidExternalPackageNames() {
        val tracker = CoordinateCaptureTargetTracker("com.samuel.androidepy")

        tracker.record(null)
        tracker.record("")
        tracker.record("invalid package")
        tracker.record("com.samuel.androidepy")
        assertNull(tracker.snapshot())

        tracker.record("com.example.target")
        assertEquals("com.example.target", tracker.snapshot())
    }

    @Test
    fun ownPackageAndInvalidEventsDoNotReplaceLastExternalPackage() {
        val tracker = CoordinateCaptureTargetTracker("com.samuel.androidepy")
        tracker.record("com.example.first")
        tracker.record("com.samuel.androidepy")
        tracker.record("bad")
        assertEquals("com.example.first", tracker.snapshot())

        tracker.record("com.example.second")
        assertEquals("com.example.second", tracker.snapshot())

        tracker.reset()
        assertNull(tracker.snapshot())
    }
}

package com.samuel.androidepy.accessibility

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccessibilityErrorTest {

    @Test
    fun wireCodesAreStableAndRetryabilityIsExplicit() {
        assertEquals("SERVICE_NOT_ENABLED", AccessibilityErrorCode.SERVICE_NOT_ENABLED.wireCode)
        assertFalse(AccessibilityErrorCode.SERVICE_NOT_ENABLED.retryable)

        assertEquals("SERVICE_DISCONNECTED", AccessibilityErrorCode.SERVICE_DISCONNECTED.wireCode)
        assertTrue(AccessibilityErrorCode.SERVICE_DISCONNECTED.retryable)

        assertEquals("SERVICE_INTERRUPTED", AccessibilityErrorCode.SERVICE_INTERRUPTED.wireCode)
        assertTrue(AccessibilityErrorCode.SERVICE_INTERRUPTED.retryable)

        assertEquals("UNSUPPORTED_OPERATION", AccessibilityErrorCode.UNSUPPORTED_OPERATION.wireCode)
        assertFalse(AccessibilityErrorCode.UNSUPPORTED_OPERATION.retryable)

        assertEquals("ACTION_UNSUPPORTED", AccessibilityErrorCode.ACTION_UNSUPPORTED.wireCode)
        assertFalse(AccessibilityErrorCode.ACTION_UNSUPPORTED.retryable)

        assertEquals("ACTION_REJECTED", AccessibilityErrorCode.ACTION_REJECTED.wireCode)
        assertFalse(AccessibilityErrorCode.ACTION_REJECTED.retryable)

        assertEquals("TIMEOUT", AccessibilityErrorCode.TIMEOUT.wireCode)
        assertTrue(AccessibilityErrorCode.TIMEOUT.retryable)
    }

    @Test
    fun operationExceptionExposesStableMetadata() {
        val error = AccessibilityOperationException(
            code = AccessibilityErrorCode.TIMEOUT,
            message = "Operation timed out",
            requestId = "request-123"
        )

        assertEquals(AccessibilityErrorCode.TIMEOUT, error.code)
        assertEquals("TIMEOUT", error.code.wireCode)
        assertTrue(error.retryable)
        assertEquals("request-123", error.requestId)
        assertEquals("Operation timed out", error.message)
    }
}

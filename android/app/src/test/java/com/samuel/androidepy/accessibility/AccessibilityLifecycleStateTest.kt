package com.samuel.androidepy.accessibility

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class AccessibilityLifecycleStateTest {

    private class ServiceToken

    @Test
    fun startsNotEnabledAndMovesToDisconnectedWhenAuthorized() {
        val state = AccessibilityLifecycleState<ServiceToken>()

        assertEquals(AccessibilityStatus.NOT_ENABLED, state.status)
        assertEquals(
            AccessibilityStatus.DISCONNECTED,
            state.updateAuthorization(enabled = true)
        )
    }

    @Test
    fun connectionInterruptionAndSignalProduceExpectedStates() {
        val state = AccessibilityLifecycleState<ServiceToken>()
        val service = ServiceToken()

        state.updateAuthorization(enabled = true)
        assertEquals(AccessibilityStatus.CONNECTED, state.onServiceConnected(service))
        assertSame(service, state.connectedServiceOrNull())

        assertEquals(AccessibilityStatus.DISCONNECTED, state.onServiceInterrupted(service))
        assertNull(state.connectedServiceOrNull())
        assertTrue(state.isInterruptedService(service))

        assertEquals(AccessibilityStatus.CONNECTED, state.onServiceSignal(service))
        assertSame(service, state.connectedServiceOrNull())
        assertFalse(state.isInterruptedService(service))
    }

    @Test
    fun revokingAuthorizationClearsTheConnection() {
        val state = AccessibilityLifecycleState<ServiceToken>()
        val service = ServiceToken()

        state.onServiceConnected(service)
        assertEquals(
            AccessibilityStatus.NOT_ENABLED,
            state.updateAuthorization(enabled = false)
        )
        assertNull(state.connectedServiceOrNull())
        assertFalse(state.isCurrentService(service))
    }

    @Test
    fun staleCallbacksFromPreviousServiceCannotChangeCurrentConnection() {
        val state = AccessibilityLifecycleState<ServiceToken>()
        val oldService = ServiceToken()
        val currentService = ServiceToken()

        state.onServiceConnected(oldService)
        state.onServiceConnected(currentService)

        assertEquals(AccessibilityStatus.CONNECTED, state.onServiceDisconnected(oldService))
        assertSame(currentService, state.connectedServiceOrNull())
        assertEquals(AccessibilityStatus.CONNECTED, state.onServiceInterrupted(oldService))
        assertSame(currentService, state.connectedServiceOrNull())
    }

    @Test
    fun disconnectingCurrentServiceKeepsAuthorizationButDropsConnection() {
        val state = AccessibilityLifecycleState<ServiceToken>()
        val service = ServiceToken()

        state.onServiceConnected(service)

        assertEquals(AccessibilityStatus.DISCONNECTED, state.onServiceDisconnected(service))
        assertTrue(state.isCurrentService(service).not())
        assertNull(state.connectedServiceOrNull())
    }
}

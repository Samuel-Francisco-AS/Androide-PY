package com.samuel.androidepy.accessibility

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

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
        assertEquals(AccessibilityStatus.NOT_ENABLED, state.onServiceSignal(service))
        assertEquals(AccessibilityStatus.NOT_ENABLED, state.onServiceDisconnected(service))
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
        assertFalse(state.isCurrentService(service))
        assertNull(state.connectedServiceOrNull())
    }

    @Test
    fun concurrentNewConnectionAndStaleDisconnectKeepNewService() {
        val state = AccessibilityLifecycleState<ServiceToken>()
        val oldService = ServiceToken()
        val currentService = ServiceToken()
        state.onServiceConnected(oldService)

        val start = CountDownLatch(1)
        val completed = CountDownLatch(2)

        val connectThread = Thread {
            start.await()
            state.onServiceConnected(currentService)
            completed.countDown()
        }
        val staleDisconnectThread = Thread {
            start.await()
            state.onServiceDisconnected(oldService)
            completed.countDown()
        }

        connectThread.start()
        staleDisconnectThread.start()
        start.countDown()

        assertTrue("Concurrent callbacks did not finish", completed.await(2, TimeUnit.SECONDS))
        connectThread.join()
        staleDisconnectThread.join()
        assertEquals(AccessibilityStatus.CONNECTED, state.status)
        assertSame(currentService, state.connectedServiceOrNull())
    }

    @Test
    fun statusWireValuesRemainStable() {
        assertEquals("not_enabled", AccessibilityStatus.NOT_ENABLED.wireValue)
        assertEquals("disconnected", AccessibilityStatus.DISCONNECTED.wireValue)
        assertEquals("connected", AccessibilityStatus.CONNECTED.wireValue)
    }
}

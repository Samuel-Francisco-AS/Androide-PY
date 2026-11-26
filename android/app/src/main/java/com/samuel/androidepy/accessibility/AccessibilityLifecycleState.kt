package com.samuel.androidepy.accessibility

/**
 * Pure lifecycle reducer for the Android-managed AccessibilityService connection.
 *
 * The gateway serializes access to this state. Keeping the transition rules free of
 * Android framework types makes authorization, interruption and stale-callback
 * behavior testable on the local JVM without Robolectric or instrumentation.
 */
internal class AccessibilityLifecycleState<T : Any> {

    private var authorizationEnabled = false
    private var connectedService: T? = null
    private var interrupted = false

    @get:Synchronized
    val status: AccessibilityStatus
        get() = when {
            !authorizationEnabled -> AccessibilityStatus.NOT_ENABLED
            connectedService != null && !interrupted -> AccessibilityStatus.CONNECTED
            else -> AccessibilityStatus.DISCONNECTED
        }

    @Synchronized
    fun updateAuthorization(enabled: Boolean): AccessibilityStatus {
        authorizationEnabled = enabled
        if (!enabled) {
            connectedService = null
            interrupted = false
        }
        return status
    }

    @Synchronized
    fun onServiceConnected(service: T): AccessibilityStatus {
        authorizationEnabled = true
        connectedService = service
        interrupted = false
        return status
    }

    @Synchronized
    fun onServiceInterrupted(service: T): AccessibilityStatus {
        if (connectedService === service) interrupted = true
        return status
    }

    @Synchronized
    fun onServiceSignal(service: T): AccessibilityStatus {
        if (connectedService === service && interrupted) interrupted = false
        return status
    }

    @Synchronized
    fun onServiceDisconnected(service: T): AccessibilityStatus {
        if (connectedService === service) {
            connectedService = null
            interrupted = false
        }
        return status
    }

    @Synchronized
    fun isCurrentService(service: T): Boolean = connectedService === service

    @Synchronized
    fun isInterruptedService(service: T): Boolean =
        connectedService === service && interrupted

    @Synchronized
    fun connectedServiceOrNull(): T? =
        if (status == AccessibilityStatus.CONNECTED) connectedService else null
}

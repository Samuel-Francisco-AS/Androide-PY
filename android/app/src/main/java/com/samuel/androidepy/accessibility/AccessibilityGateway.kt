package com.samuel.androidepy.accessibility

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Holds the service instance only while Android reports it as connected.
 *
 * The gateway is owned by [com.samuel.androidepy.AndroidePyApplication].
 * It never creates or binds the service itself.
 */
class AccessibilityGateway {

    private val lock = Any()

    @Volatile
    private var connectedService: AndroidePyAccessibilityService? = null

    private val mutableConnectionState =
        MutableStateFlow(AccessibilityConnectionState.DISCONNECTED)

    internal val connectionState: StateFlow<AccessibilityConnectionState> =
        mutableConnectionState.asStateFlow()

    internal fun onServiceConnected(service: AndroidePyAccessibilityService) {
        synchronized(lock) {
            connectedService = service
            mutableConnectionState.value = AccessibilityConnectionState.CONNECTED
        }
    }

    internal fun onServiceDisconnected(service: AndroidePyAccessibilityService) {
        synchronized(lock) {
            // A late teardown callback from an older instance must not clear a newer bind.
            if (connectedService !== service) return

            connectedService = null
            mutableConnectionState.value = AccessibilityConnectionState.DISCONNECTED
        }
    }

    internal fun connectedServiceOrNull(): AndroidePyAccessibilityService? =
        connectedService
}

internal enum class AccessibilityConnectionState {
    DISCONNECTED,
    CONNECTED
}

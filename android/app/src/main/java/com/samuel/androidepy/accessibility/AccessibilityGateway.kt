package com.samuel.androidepy.accessibility

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.view.accessibility.AccessibilityManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Holds the service instance only while Android reports it as connected.
 *
 * The gateway is owned by [com.samuel.androidepy.AndroidePyApplication].
 * It never creates or binds the service itself.
 */
class AccessibilityGateway(context: Context) {

    private val appContext = context.applicationContext
    private val lock = Any()

    @Volatile
    private var connectedService: AndroidePyAccessibilityService? = null

    @Volatile
    private var interrupted = false

    private val mutableStatus = MutableStateFlow(AccessibilityStatus.NOT_ENABLED)
    val status: StateFlow<AccessibilityStatus> = mutableStatus.asStateFlow()

    /**
     * Refreshes authorization from Android's enabled-service registry and combines
     * it with the actual service lifecycle state. It never enables the service.
     */
    fun refreshStatus(): AccessibilityStatus {
        val manager = appContext.getSystemService(Context.ACCESSIBILITY_SERVICE)
            as? AccessibilityManager
        val expectedComponent = ComponentName(
            appContext,
            AndroidePyAccessibilityService::class.java
        )

        val enabled = manager
            ?.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            .orEmpty()
            .any { info ->
                val serviceInfo = info.resolveInfo?.serviceInfo
                serviceInfo != null &&
                    serviceInfo.packageName == expectedComponent.packageName &&
                    serviceInfo.name == expectedComponent.className
            }

        synchronized(lock) {
            if (!enabled) {
                connectedService = null
                interrupted = false
            }

            val next = when {
                !enabled -> AccessibilityStatus.NOT_ENABLED
                connectedService != null && !interrupted -> AccessibilityStatus.CONNECTED
                else -> AccessibilityStatus.DISCONNECTED
            }
            mutableStatus.value = next
            return next
        }
    }

    internal fun onServiceConnected(service: AndroidePyAccessibilityService) {
        synchronized(lock) {
            connectedService = service
            interrupted = false
            mutableStatus.value = AccessibilityStatus.CONNECTED
        }
    }

    internal fun onServiceInterrupted(service: AndroidePyAccessibilityService) {
        synchronized(lock) {
            if (connectedService !== service) return
            interrupted = true
        }
        refreshStatus()
    }

    /**
     * The configured window-state signal resumes operational availability after
     * an interruption. The event payload is never inspected or retained.
     */
    internal fun onServiceSignal(service: AndroidePyAccessibilityService) {
        synchronized(lock) {
            if (connectedService !== service || !interrupted) return
            interrupted = false
        }
        refreshStatus()
    }

    internal fun onServiceDisconnected(service: AndroidePyAccessibilityService) {
        synchronized(lock) {
            // A late teardown callback from an older instance must not clear a newer bind.
            if (connectedService !== service) return
            connectedService = null
            interrupted = false
        }
        refreshStatus()
    }

    internal fun connectedServiceOrNull(): AndroidePyAccessibilityService? =
        if (mutableStatus.value == AccessibilityStatus.CONNECTED) connectedService else null
}

enum class AccessibilityStatus(val wireValue: String) {
    NOT_ENABLED("not_enabled"),
    DISCONNECTED("disconnected"),
    CONNECTED("connected")
}

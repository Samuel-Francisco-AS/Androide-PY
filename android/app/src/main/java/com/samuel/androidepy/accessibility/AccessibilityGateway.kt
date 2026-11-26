package com.samuel.androidepy.accessibility

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.view.accessibility.AccessibilityManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

/**
 * Holds the service instance only while Android reports it as connected.
 *
 * The gateway is owned by [com.samuel.androidepy.AndroidePyApplication].
 * It never creates or binds the service itself.
 */
class AccessibilityGateway(context: Context) {

    private val appContext = context.applicationContext
    private val lock = Any()
    private val lifecycle = AccessibilityLifecycleState<AndroidePyAccessibilityService>()
    private val operationCoordinator = AccessibilityOperationCoordinator()

    private val mutableStatus = MutableStateFlow(AccessibilityStatus.NOT_ENABLED)
    val status: StateFlow<AccessibilityStatus> = mutableStatus.asStateFlow()

    /**
     * Refreshes authorization from Android's enabled-service registry and combines
     * it with the actual service lifecycle state. It never enables the service.
     */
    fun refreshStatus(): AccessibilityStatus = synchronized(lock) {
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

        lifecycle.updateAuthorization(enabled).also { mutableStatus.value = it }
    }

    internal fun onServiceConnected(service: AndroidePyAccessibilityService) {
        synchronized(lock) {
            mutableStatus.value = lifecycle.onServiceConnected(service)
        }
    }

    internal fun onServiceInterrupted(service: AndroidePyAccessibilityService) {
        synchronized(lock) {
            if (!lifecycle.isCurrentService(service)) return
            mutableStatus.value = lifecycle.onServiceInterrupted(service)
        }
        operationCoordinator.failForService(
            service,
            AccessibilityOperationException(
                AccessibilityErrorCode.SERVICE_INTERRUPTED,
                "O serviço de acessibilidade foi interrompido."
            )
        )
        refreshStatus()
    }

    /**
     * The configured window-state signal resumes operational availability after
     * an interruption. The event payload is never inspected or retained.
     */
    internal fun onServiceSignal(service: AndroidePyAccessibilityService) {
        synchronized(lock) {
            if (!lifecycle.isInterruptedService(service)) return
            mutableStatus.value = lifecycle.onServiceSignal(service)
        }
        refreshStatus()
    }

    internal fun onServiceDisconnected(service: AndroidePyAccessibilityService) {
        synchronized(lock) {
            // A late teardown callback from an older instance must not clear a newer bind.
            if (!lifecycle.isCurrentService(service)) return
            mutableStatus.value = lifecycle.onServiceDisconnected(service)
        }
        operationCoordinator.failForService(
            service,
            AccessibilityOperationException(
                AccessibilityErrorCode.SERVICE_DISCONNECTED,
                "O serviço de acessibilidade foi desconectado."
            )
        )
        refreshStatus()
    }

    internal fun connectedServiceOrNull(): AndroidePyAccessibilityService? =
        synchronized(lock) { lifecycle.connectedServiceOrNull() }

    internal fun executeOperation(request: AccessibilityOperationRequest): JSONObject {
        val currentStatus = refreshStatus()
        if (currentStatus == AccessibilityStatus.NOT_ENABLED) {
            throw AccessibilityOperationException(
                AccessibilityErrorCode.SERVICE_NOT_ENABLED,
                "O serviço de acessibilidade não está habilitado.",
                request.requestId
            )
        }

        val service = connectedServiceOrNull()
            ?: throw AccessibilityOperationException(
                AccessibilityErrorCode.SERVICE_DISCONNECTED,
                "O serviço de acessibilidade não está conectado.",
                request.requestId
            )

        return operationCoordinator.execute(service, request)
    }
}

package com.samuel.androidepy.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.samuel.androidepy.AndroidePyApplication

/**
 * Android-managed entry point for future, explicitly authorized screen operations.
 *
 * P2.1-A intentionally does not inspect event contents, retrieve window nodes,
 * dispatch gestures, or retain UI objects.
 */
class AndroidePyAccessibilityService : AccessibilityService() {

    private val accessibilityGateway: AccessibilityGateway?
        get() = (application as? AndroidePyApplication)?.accessibilityGateway

    override fun onServiceConnected() {
        super.onServiceConnected()
        accessibilityGateway?.onServiceConnected(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // Window-state events are the only configured signal at this stage.
        // Their payload is deliberately not read or retained in P2.1-A.
    }

    override fun onInterrupt() {
        // No accessibility operation is dispatched in P2.1-A.
        // Pending-operation failure will be added with the request gateway.
    }

    override fun onUnbind(intent: Intent): Boolean {
        accessibilityGateway?.onServiceDisconnected(this)
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        accessibilityGateway?.onServiceDisconnected(this)
        super.onDestroy()
    }
}

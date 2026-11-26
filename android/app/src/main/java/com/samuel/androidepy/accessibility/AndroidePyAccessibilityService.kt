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
        // Read only the configured event type; never inspect or retain its payload.
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            accessibilityGateway?.onServiceSignal(this)
        }
    }

    override fun onInterrupt() {
        accessibilityGateway?.onServiceInterrupted(this)
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

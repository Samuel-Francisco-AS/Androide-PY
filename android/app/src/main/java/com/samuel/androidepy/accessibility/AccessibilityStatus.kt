package com.samuel.androidepy.accessibility

/**
 * Publicly observable operational state of the Android-managed accessibility service.
 *
 * Authorization and runtime connection are intentionally distinct: an enabled
 * service may still be disconnected or interrupted.
 */
enum class AccessibilityStatus(val wireValue: String) {
    NOT_ENABLED("not_enabled"),
    DISCONNECTED("disconnected"),
    CONNECTED("connected")
}

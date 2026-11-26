package com.samuel.androidepy.accessibility

/**
 * Stable machine-readable failures shared by native accessibility operations.
 * Messages remain explanatory; callers branch on [wireCode], never on text.
 */
enum class AccessibilityErrorCode(
    val wireCode: String,
    val retryable: Boolean
) {
    SERVICE_NOT_ENABLED("SERVICE_NOT_ENABLED", false),
    SERVICE_DISCONNECTED("SERVICE_DISCONNECTED", true),
    SERVICE_INTERRUPTED("SERVICE_INTERRUPTED", true),
    QUEUE_FULL("QUEUE_FULL", true),
    TIMEOUT("TIMEOUT", true),
    CANCELLED("CANCELLED", false),
    INVALID_REQUEST("INVALID_REQUEST", false),
    INVALID_PAYLOAD("INVALID_PAYLOAD", false),
    UNSUPPORTED_OPERATION("UNSUPPORTED_OPERATION", false),
    ACTION_UNSUPPORTED("ACTION_UNSUPPORTED", false),
    ACTION_REJECTED("ACTION_REJECTED", false),
    INTERNAL_ERROR("INTERNAL_ERROR", false)
}

class AccessibilityOperationException(
    val code: AccessibilityErrorCode,
    override val message: String,
    val requestId: String? = null
) : IllegalStateException(message) {
    val retryable: Boolean
        get() = code.retryable
}

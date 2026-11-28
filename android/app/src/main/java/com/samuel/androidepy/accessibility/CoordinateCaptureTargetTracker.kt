package com.samuel.androidepy.accessibility

/**
 * Keeps the most recently observed external package name only.
 *
 * Callers pass only AccessibilityEvent.packageName. The service deliberately
 * ignores its own package so attaching/updating the overlay cannot replace the
 * target snapshot.
 */
internal class CoordinateCaptureTargetTracker(
    private val ownPackageName: String
) {
    private var lastExternalPackage: String? = null

    fun record(packageName: String?) {
        val candidate = packageName?.trim()?.takeIf { PACKAGE_NAME.matches(it) }
            ?: return
        if (candidate != ownPackageName) {
            lastExternalPackage = candidate
        }
    }

    fun snapshot(): String? = lastExternalPackage

    fun reset() {
        lastExternalPackage = null
    }

    companion object {
        private val PACKAGE_NAME =
            Regex("[A-Za-z][A-Za-z0-9_]*(?:\\.[A-Za-z][A-Za-z0-9_]*)+")
    }
}

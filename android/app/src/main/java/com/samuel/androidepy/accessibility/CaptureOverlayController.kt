package com.samuel.androidepy.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import java.util.Locale

/**
 * Small, visible and cancellable capture indicator owned by one service instance.
 * It deliberately uses TYPE_ACCESSIBILITY_OVERLAY and requires no generic
 * SYSTEM_ALERT_WINDOW permission.
 */
internal class CaptureOverlayController(
    private val service: AccessibilityService
) : CoordinateCaptureOverlay {

    private val windowManager = service.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private var overlayView: View? = null
    private var countdownView: TextView? = null

    override fun show(durationSeconds: Int, onCancel: () -> Unit) {
        check(overlayView == null) { "A captura já possui um overlay associado." }

        val indicator = TextView(service).apply {
            text = "● Captura ativa"
            setTextColor(Color.WHITE)
            textSize = 14f
        }
        val timer = TextView(service).apply {
            text = formatRemaining(durationSeconds)
            setTextColor(Color.WHITE)
            textSize = 14f
            setPadding(dp(12), 0, dp(8), 0)
        }
        val cancelButton = Button(service).apply {
            text = "Cancelar"
            isAllCaps = false
            setOnClickListener { onCancel() }
        }
        val container = LinearLayout(service).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(6), dp(8), dp(6))
            background = GradientDrawable().apply {
                setColor(0xE620252B.toInt())
                cornerRadius = dp(12).toFloat()
                setStroke(dp(1), 0xFF70757A.toInt())
            }
            addView(
                indicator,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            )
            addView(
                timer,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            )
            addView(
                cancelButton,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            )
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = dp(24)
            setTitle("Androide PY — Captura de coordenadas")
        }

        try {
            windowManager.addView(container, params)
            overlayView = container
            countdownView = timer
        } catch (error: Exception) {
            // Preserve ownership if WindowManager attached the view before failing.
            if (container.isAttachedToWindow) {
                overlayView = container
                countdownView = timer
            }
            throw error
        }
    }

    override fun updateRemainingSeconds(remainingSeconds: Int) {
        val view = countdownView
            ?: throw IllegalStateException("O overlay de captura não está anexado.")
        if (overlayView?.isAttachedToWindow != true) {
            throw IllegalStateException("O overlay de captura não está anexado.")
        }
        view.text = formatRemaining(remainingSeconds)
    }

    override fun remove() {
        val view = overlayView ?: return
        if (!view.isAttachedToWindow) {
            clearReferences()
            return
        }

        try {
            windowManager.removeView(view)
        } catch (error: Exception) {
            if (!view.isAttachedToWindow) clearReferences()
            throw error
        }

        if (view.isAttachedToWindow) {
            throw IllegalStateException("O Android manteve o overlay anexado após removeView().")
        }
        clearReferences()
    }

    private fun clearReferences() {
        overlayView = null
        countdownView = null
    }

    private fun formatRemaining(seconds: Int): String {
        val minutes = seconds.coerceAtLeast(0) / 60
        val remainder = seconds.coerceAtLeast(0) % 60
        return String.format(Locale.ROOT, "Tempo restante %02d:%02d", minutes, remainder)
    }

    private fun dp(value: Int): Int =
        (value * service.resources.displayMetrics.density).toInt()

}

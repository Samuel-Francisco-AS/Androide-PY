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
import android.widget.EditText
import android.widget.FrameLayout
import android.view.MotionEvent
import android.view.ViewGroup
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

    private val windowManager: WindowManager by lazy {
        service.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
            ?: throw IllegalStateException("O WindowManager não está disponível para o serviço.")
    }

    private var overlayView: View? = null
    private var countdownView: TextView? = null
    private var selectionPointCount = 0

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

    override fun showSelection(
        durationSeconds: Int,
        onPoint: (String, Int, Int) -> Unit,
        onRemoveLast: () -> Unit,
        onFinish: () -> Unit,
        onCancel: () -> Unit
    ) {
        check(overlayView == null) { "A captura já possui um overlay associado." }
        val metrics = service.resources.displayMetrics
        val root = FrameLayout(service).apply { setBackgroundColor(Color.TRANSPARENT) }
        val panel = LinearLayout(service).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = GradientDrawable().apply {
                setColor(0xF020252B.toInt())
                cornerRadius = dp(14).toFloat()
            }
        }
        val title = TextView(service).apply {
            text = "Captura de coordenadas — toque na tela para marcar"
            setTextColor(Color.WHITE)
            textSize = 14f
        }
        val timer = TextView(service).apply {
            text = formatRemaining(durationSeconds)
            setTextColor(Color.WHITE)
            setPadding(0, dp(4), 0, dp(4))
        }
        val coordinates = TextView(service).apply {
            text = "Nenhum ponto selecionado"
            setTextColor(Color.WHITE)
        }
        val pointName = EditText(service).apply {
            hint = "Nome do ponto"
            singleLine = true
            setTextColor(Color.WHITE)
            setHintTextColor(0xFFBBBBBB.toInt())
        }
        var pending: Pair<Int, Int>? = null
        var pointCount = 0
        val add = Button(service).apply {
            text = "Adicionar ponto"
            isAllCaps = false
            setOnClickListener {
                val coordinate = pending ?: return@setOnClickListener
                val name = pointName.text?.toString()?.trim().orEmpty()
                if (name.isNotEmpty() && name.length <= 80) {
                    onPoint(name, coordinate.first, coordinate.second)
                    pointCount += 1
                    coordinates.text = "Pontos selecionados: $pointCount"
                    pointName.text?.clear()
                    pending = null
                } else {
                    pointName.error = "Informe um nome de até 80 caracteres."
                }
            }
        }
        val actions = LinearLayout(service).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(add, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            addView(Button(service).apply {
                text = "Desfazer último"
                isAllCaps = false
                setOnClickListener {
                    if (pointCount > 0) {
                        onRemoveLast()
                        pointCount -= 1
                        coordinates.text = "Pontos selecionados: $pointCount"
                    }
                }
            }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        }
        val finishRow = LinearLayout(service).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(Button(service).apply {
                text = "Concluir mapa"
                isAllCaps = false
                setOnClickListener { if (pointCount > 0) onFinish() }
            }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            addView(Button(service).apply {
                text = "Cancelar"
                isAllCaps = false
                setOnClickListener { onCancel() }
            }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        }
        panel.addView(title)
        panel.addView(timer)
        panel.addView(coordinates)
        panel.addView(pointName, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ))
        panel.addView(actions)
        panel.addView(finishRow)
        val panelParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            Gravity.BOTTOM
        )
        root.addView(panel, panelParams)
        root.setOnTouchListener { view, event ->
            if (event.action == MotionEvent.ACTION_UP && event.y < panel.top) {
                pending = event.x.toInt().coerceIn(0, metrics.widthPixels - 1) to
                    event.y.toInt().coerceIn(0, metrics.heightPixels - 1)
                coordinates.text = "Ponto em x=${pending!!.first}, y=${pending!!.second}"
                true
            } else {
                true
            }
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            setTitle("Androide PY — Seleção de coordenadas")
        }
        windowManager.addView(root, params)
        overlayView = root
        countdownView = timer
        selectionPointCount = 0
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

package com.samuel.androidepy.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.samuel.androidepy.AndroidePyApplication
import org.json.JSONObject

/**
 * Android-managed entry point for explicitly requested screen operations.
 *
 * Event payloads are not inspected or retained. Window content is accessed only
 * for the focused editable field during an explicit type_text request.
 */
class AndroidePyAccessibilityService : AccessibilityService() {

    private val accessibilityGateway: AccessibilityGateway?
        get() = (application as? AndroidePyApplication)?.accessibilityGateway

    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }

    @Volatile
    private var serviceOperational = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        serviceOperational = true
        accessibilityGateway?.onServiceConnected(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // Read only the configured window-state signal; never inspect its payload.
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            serviceOperational = true
            accessibilityGateway?.onServiceSignal(this)
        }
    }

    override fun onInterrupt() {
        serviceOperational = false
        accessibilityGateway?.onServiceInterrupted(this)
    }

    override fun onUnbind(intent: Intent): Boolean {
        serviceOperational = false
        accessibilityGateway?.onServiceDisconnected(this)
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        serviceOperational = false
        accessibilityGateway?.onServiceDisconnected(this)
        super.onDestroy()
    }

    internal fun dispatchAccessibilityOperation(
        request: AccessibilityOperationRequest,
        completion: (JSONObject?, AccessibilityOperationException?) -> Unit
    ) {
        if (!serviceOperational) {
            completion(
                null,
                AccessibilityOperationException(
                    AccessibilityErrorCode.SERVICE_DISCONNECTED,
                    "O serviço de acessibilidade não está operacional.",
                    request.requestId
                )
            )
            return
        }
        try {
            when (request.operation) {
                "screen.tap" -> {
                    val point = resolvePoint(
                        request.payload.getDouble("x"),
                        request.payload.getDouble("y"),
                        request.payload.optString("coordinate_space", "pixels")
                    )
                    dispatchGestureOperation(
                        request,
                        point.first,
                        point.second,
                        point.first,
                        point.second,
                        TAP_DURATION_MS,
                        completion
                    )
                }

                "screen.swipe" -> {
                    val start = resolvePoint(
                        request.payload.getDouble("x1"),
                        request.payload.getDouble("y1"),
                        request.payload.optString("coordinate_space", "pixels")
                    )
                    val end = resolvePoint(
                        request.payload.getDouble("x2"),
                        request.payload.getDouble("y2"),
                        request.payload.optString("coordinate_space", "pixels")
                    )
                    dispatchGestureOperation(
                        request,
                        start.first,
                        start.second,
                        end.first,
                        end.second,
                        request.payload.getLong("duration_ms"),
                        completion
                    )
                }

                "screen.scroll" -> {
                    val metrics = resources.displayMetrics
                    val width = metrics.widthPixels
                    val height = metrics.heightPixels
                    val direction = request.payload.getString("direction")
                    val distance = request.payload.getDouble("distance")
                    if (!distance.isFinite() || distance <= 0.0 || distance > 1.0) {
                        throw invalidPayload(request, "A distância de rolagem é inválida.")
                    }
                    val duration = request.payload.getLong("duration_ms")
                    val span = when (direction) {
                        "up", "down" -> (height - 1) * distance
                        "left", "right" -> (width - 1) * distance
                        else -> throw invalidPayload(request, "A direção de rolagem é inválida.")
                    }
                    val centerX = (width - 1) / 2.0
                    val centerY = (height - 1) / 2.0
                    val (x1, y1, x2, y2) = when (direction) {
                        "down" -> listOf(centerX, centerY - span / 2, centerX, centerY + span / 2)
                        "up" -> listOf(centerX, centerY + span / 2, centerX, centerY - span / 2)
                        "right" -> listOf(centerX - span / 2, centerY, centerX + span / 2, centerY)
                        else -> listOf(centerX + span / 2, centerY, centerX - span / 2, centerY)
                    }
                    dispatchGestureOperation(
                        request,
                        x1.toFloat(),
                        y1.toFloat(),
                        x2.toFloat(),
                        y2.toFloat(),
                        duration,
                        completion
                    )
                }

                "screen.type_text" -> {
                    val text = request.payload.getString("text")
                    if (text.length > MAX_TEXT_CHARS) {
                        throw invalidPayload(request, "O texto excede o limite permitido.")
                    }
                    typeIntoFocusedField(request, text, completion)
                }

                else -> throw AccessibilityOperationException(
                    AccessibilityErrorCode.UNSUPPORTED_OPERATION,
                    "A operação solicitada não está disponível.",
                    request.requestId
                )
            }
        } catch (error: AccessibilityOperationException) {
            completion(null, error)
        } catch (_: Exception) {
            completion(
                null,
                AccessibilityOperationException(
                    AccessibilityErrorCode.INVALID_PAYLOAD,
                    "Os parâmetros da operação são inválidos.",
                    request.requestId
                )
            )
        }
    }

    private fun resolvePoint(
        x: Double,
        y: Double,
        coordinateSpace: String
    ): Pair<Float, Float> {
        if (!x.isFinite() || !y.isFinite()) {
            throw AccessibilityOperationException(
                AccessibilityErrorCode.INVALID_PAYLOAD,
                "As coordenadas devem ser números finitos."
            )
        }
        val metrics = resources.displayMetrics
        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val point = when (coordinateSpace) {
            "pixels" -> x to y
            "normalized" -> {
                if (x !in 0.0..1.0 || y !in 0.0..1.0) {
                    throw AccessibilityOperationException(
                        AccessibilityErrorCode.INVALID_PAYLOAD,
                        "As coordenadas normalizadas devem estar entre 0 e 1."
                    )
                }
                (x * (width - 1)) to (y * (height - 1))
            }
            else -> throw AccessibilityOperationException(
                AccessibilityErrorCode.INVALID_PAYLOAD,
                "O espaço de coordenadas é inválido."
            )
        }

        if (point.first < 0.0 || point.second < 0.0 ||
            point.first >= width || point.second >= height
        ) {
            throw AccessibilityOperationException(
                AccessibilityErrorCode.INVALID_PAYLOAD,
                "As coordenadas estão fora dos limites do display."
            )
        }
        return point.first.toFloat() to point.second.toFloat()
    }

    private fun dispatchGestureOperation(
        request: AccessibilityOperationRequest,
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        durationMs: Long,
        completion: (JSONObject?, AccessibilityOperationException?) -> Unit
    ) {
        if (durationMs !in 1L..MAX_GESTURE_DURATION_MS) {
            throw invalidPayload(request, "A duração do gesto é inválida.")
        }

        val path = Path().apply {
            moveTo(x1, y1)
            lineTo(x2, y2)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(
                GestureDescription.StrokeDescription(path, 0L, durationMs)
            )
            .build()

        val accepted = dispatchGesture(
            gesture,
            object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription) {
                    completion(operationResult(request), null)
                }

                override fun onCancelled(gestureDescription: GestureDescription) {
                    completion(
                        null,
                        AccessibilityOperationException(
                            AccessibilityErrorCode.ACTION_REJECTED,
                            "O Android cancelou o gesto solicitado.",
                            request.requestId
                        )
                    )
                }
            },
            mainHandler
        )

        if (!accepted) {
            completion(
                null,
                AccessibilityOperationException(
                    AccessibilityErrorCode.ACTION_REJECTED,
                    "O Android não aceitou o gesto solicitado.",
                    request.requestId
                )
            )
        }
    }

    private fun typeIntoFocusedField(
        request: AccessibilityOperationRequest,
        text: String,
        completion: (JSONObject?, AccessibilityOperationException?) -> Unit
    ) {
        val root = rootInActiveWindow
            ?: throw AccessibilityOperationException(
                AccessibilityErrorCode.ACTION_UNSUPPORTED,
                "Não há uma janela ativa disponível para preenchimento.",
                request.requestId
            )

        var node: AccessibilityNodeInfo? = null
        try {
            node = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
            if (node == null) {
                throw AccessibilityOperationException(
                    AccessibilityErrorCode.ACTION_UNSUPPORTED,
                    "Nenhum campo de texto está focado.",
                    request.requestId
                )
            }

            val accessibilitySensitive = Build.VERSION.SDK_INT >= 34 &&
                node.isAccessibilityDataSensitive
            when (
                AccessibilityTextInputPolicy.classify(
                    editable = node.isEditable,
                    focused = node.isFocused,
                    enabled = node.isEnabled,
                    password = node.isPassword,
                    accessibilitySensitive = accessibilitySensitive,
                    inputType = node.inputType
                )
            ) {
                TextInputClassification.SENSITIVE -> throw AccessibilityOperationException(
                    AccessibilityErrorCode.ACTION_UNSUPPORTED,
                    "O preenchimento de campos sensíveis não está autorizado.",
                    request.requestId
                )
                TextInputClassification.AMBIGUOUS -> throw AccessibilityOperationException(
                    AccessibilityErrorCode.ACTION_UNSUPPORTED,
                    "O tipo do campo não pôde ser classificado como texto comum.",
                    request.requestId
                )
                TextInputClassification.UNSUPPORTED -> throw AccessibilityOperationException(
                    AccessibilityErrorCode.ACTION_UNSUPPORTED,
                    "O campo focado não está editável e habilitado.",
                    request.requestId
                )
                TextInputClassification.COMMON -> Unit
            }

            val supportsSetText = node.actionList.any {
                it.id == AccessibilityNodeInfo.ACTION_SET_TEXT
            }
            if (!supportsSetText) {
                throw AccessibilityOperationException(
                    AccessibilityErrorCode.ACTION_UNSUPPORTED,
                    "O campo não oferece suporte à ação de preenchimento.",
                    request.requestId
                )
            }

            val arguments = Bundle().apply {
                putCharSequence(
                    AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                    text
                )
            }
            if (!node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)) {
                throw AccessibilityOperationException(
                    AccessibilityErrorCode.ACTION_REJECTED,
                    "O aplicativo de destino recusou o preenchimento.",
                    request.requestId
                )
            }
            completion(operationResult(request), null)
        } finally {
            node?.recycle()
            root.recycle()
        }
    }

    private fun operationResult(request: AccessibilityOperationRequest) =
        JSONObject()
            .put("request_id", request.requestId)
            .put("operation", request.operation)
            .put("completed", true)

    private fun invalidPayload(
        request: AccessibilityOperationRequest,
        message: String
    ) = AccessibilityOperationException(
        AccessibilityErrorCode.INVALID_PAYLOAD,
        message,
        request.requestId
    )

    private companion object {
        const val TAP_DURATION_MS = 80L
        const val MAX_TEXT_CHARS = 10_000
        const val MAX_GESTURE_DURATION_MS = 10_000L
    }
}

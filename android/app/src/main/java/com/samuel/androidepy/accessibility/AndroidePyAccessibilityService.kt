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
import android.view.WindowManager
import android.view.WindowInsets
import com.samuel.androidepy.data.CoordinateMapDisplayContext
import com.samuel.androidepy.data.CoordinateMapPointInput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONArray
import com.samuel.androidepy.AndroidePyApplication
import org.json.JSONObject

/**
 * Android-managed entry point for explicitly requested screen operations.
 *
 * Configured window-state events contribute only their packageName metadata to
 * the capture target tracker. Window content is accessed only for the focused
 * editable field during an explicit type_text request.
 */
class AndroidePyAccessibilityService : AccessibilityService() {

    private val accessibilityGateway: AccessibilityGateway?
        get() = (application as? AndroidePyApplication)?.accessibilityGateway

    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }
    private val captureTargetTracker by lazy {
        CoordinateCaptureTargetTracker(packageName)
    }

    private var captureController: CoordinateCaptureController? = null
    private var activeCaptureId: String? = null
    private var captureDelayTask: Runnable? = null
    private var captureTimeoutTask: Runnable? = null
    private val captureIoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var serviceOperational = false

    internal fun currentCaptureTargetPackage(): String? = captureTargetTracker.snapshot()

    private fun ensureCaptureController() {
        if (captureController == null) {
            captureController = CoordinateCaptureController(
                overlay = CaptureOverlayController(this),
                scheduler = HandlerCoordinateCaptureScheduler(mainHandler)
            )
        }
    }

    /**
     * Internal native entry point reserved for the later B3 Bridge integration.
     * B2 intentionally does not expose a public Python screen.* operation.
     */
    internal fun startCoordinateCaptureSession(
        durationSeconds: Int = CoordinateCaptureController.DEFAULT_DURATION_SECONDS,
        completion: (CoordinateCaptureStartResult) -> Unit
    ) {
        val accepted = mainHandler.post {
            if (!serviceOperational) {
                completion(CoordinateCaptureStartResult.SERVICE_UNAVAILABLE)
                return@post
            }
            ensureCaptureController()
            val targetPackage = captureTargetTracker.snapshot()
            completion(
                captureController?.start(targetPackage, durationSeconds)
                    ?: CoordinateCaptureStartResult.SERVICE_UNAVAILABLE
            )
        }
        if (!accepted) completion(CoordinateCaptureStartResult.SERVICE_UNAVAILABLE)
    }

    /** Starts a non-queued capture session and returns immediately after scheduling. */
    internal fun startCoordinateMapCapture(captureId: String, delayMs: Long, timeoutMs: Long) {
        val accepted = mainHandler.post {
            val registry = (application as? AndroidePyApplication)?.coordinateCaptureRegistry
            if (!serviceOperational || registry == null) {
                registry?.fail(captureId, "failed", "SERVICE_DISCONNECTED", "O serviço não está operacional.")
                return@post
            }
            if (activeCaptureId != null) {
                registry.fail(captureId, "failed", "CAPTURE_ALREADY_ACTIVE", "Já existe uma captura ativa.")
                return@post
            }
            activeCaptureId = captureId
            ensureCaptureController()
            captureTimeoutTask = Runnable {
                if (activeCaptureId == captureId) {
                    if (captureController?.snapshot?.state == CoordinateCaptureState.ACTIVE) captureController?.expire()
                    else {
                        registry.fail(captureId, "expired", "TIMEOUT", "O prazo total da captura expirou.")
                        clearCaptureTasks(captureId)
                    }
                }
            }
            if (!mainHandler.postDelayed(captureTimeoutTask!!, timeoutMs)) {
                registry.fail(captureId, "failed", "INTERNAL_ERROR", "Não foi possível agendar o timeout.")
                clearCaptureTasks(captureId)
                return@post
            }
            captureDelayTask = Runnable {
                if (activeCaptureId != captureId) return@Runnable
                val target = captureTargetTracker.snapshot()
                if (!registry.activate(captureId)) {
                    registry.fail(captureId, "failed", "INTERNAL_ERROR", "Não foi possível ativar a captura.")
                    clearCaptureTasks(captureId)
                    return@Runnable
                }
                val start = captureController?.start(
                    targetPackage = target,
                    durationSeconds = CoordinateCaptureController.DEFAULT_DURATION_SECONDS,
                    onPoint = { name, x, y -> registry.addPoint(captureId, name, x, y) },
                    onRemoveLast = { registry.removeLastPoint(captureId) },
                    onFinish = {},
                    onTerminal = { outcome, failure -> onCoordinateCaptureTerminal(captureId, target, outcome, failure) }
                ) ?: CoordinateCaptureStartResult.SERVICE_UNAVAILABLE
                if (start != CoordinateCaptureStartResult.STARTED) {
                    registry.fail(captureId, "failed", start.name, "Não foi possível iniciar a seleção.")
                    clearCaptureTasks(captureId)
                }
            }
            if (!mainHandler.postDelayed(captureDelayTask!!, delayMs)) {
                registry.fail(captureId, "failed", "INTERNAL_ERROR", "Não foi possível agendar o início da captura.")
                clearCaptureTasks(captureId)
            }
        }
        if (!accepted) {
            (application as? AndroidePyApplication)?.coordinateCaptureRegistry?.fail(
                captureId, "failed", "SERVICE_DISCONNECTED", "A thread principal não aceitou a solicitação."
            )
        }
    }

    internal fun cancelCoordinateMapCapture(captureId: String) {
        mainHandler.post {
            if (activeCaptureId != captureId) return@post
            if (captureController?.snapshot?.state == CoordinateCaptureState.ACTIVE) captureController?.cancel()
            else {
                val cancelled = (application as? AndroidePyApplication)
                    ?.coordinateCaptureRegistry?.cancelLocally(captureId) == true
                if (cancelled) clearCaptureTasks(captureId)
            }
        }
    }

    private fun onCoordinateCaptureTerminal(captureId: String, target: String, outcome: CoordinateCaptureOutcome, failure: CoordinateCaptureFailure?) {
        val app = application as? AndroidePyApplication ?: return
        if (outcome == CoordinateCaptureOutcome.COMPLETED) {
            clearCaptureTasks(captureId, releaseActive = false)
            if (!app.coordinateCaptureRegistry.markSaving(captureId)) {
                releaseCapture(captureId)
                return
            }
            val points = app.coordinateCaptureRegistry.points(captureId)
            if (points.isEmpty()) {
                app.coordinateCaptureRegistry.fail(captureId, "failed", "INVALID_MAP", "O mapa precisa conter pontos.")
                releaseCapture(captureId)
                return
            }
                val context = captureDisplayContext(target)
                val inputs = points.map { CoordinateMapPointInput(it.name, it.xPx, it.yPx) }
                val mapName = app.coordinateCaptureRegistry.name(captureId) ?: "Mapa"
                captureIoScope.launch {
                    try {
                        val created = app.coordinateMapRepository.create(mapName, context, inputs)
                        val result = JSONObject().put("schema_version", 1).put("name", created.map.name)
                            .put("created_at_epoch_ms", created.map.createdAtEpochMs)
                            .put("target_package", created.map.targetPackage ?: JSONObject.NULL)
                            .put("display", JSONObject().put("width_px", created.map.displayWidthPx)
                                .put("height_px", created.map.displayHeightPx).put("density_dpi", created.map.densityDpi)
                                .put("rotation", created.map.rotation))
                            .put("points", JSONArray().apply { created.points.forEach { p ->
                                put(JSONObject().put("name", p.name).put("x_px", p.xPx).put("y_px", p.yPx)
                                    .put("x_normalized", p.xNormalized).put("y_normalized", p.yNormalized))
                            } })
                        app.coordinateCaptureRegistry.complete(captureId, result)
                    } catch (_: android.database.sqlite.SQLiteConstraintException) {
                        app.coordinateCaptureRegistry.fail(captureId, "failed", "MAP_ALREADY_EXISTS", "Já existe um mapa com esse nome.")
                    } catch (_: IllegalArgumentException) {
                        app.coordinateCaptureRegistry.fail(captureId, "failed", "INVALID_MAP", "Os dados do mapa não passaram pela validação.")
                    } catch (_: Exception) {
                        app.coordinateCaptureRegistry.fail(captureId, "failed", "INTERNAL_ERROR", "Não foi possível persistir o mapa.")
                    } finally {
                        releaseCapture(captureId)
                    }
                }
        } else {
            clearCaptureTasks(captureId)
            when (outcome) {
                CoordinateCaptureOutcome.CANCELLED -> app.coordinateCaptureRegistry.fail(captureId, "cancelled", "CANCELLED", "A captura foi cancelada.")
                CoordinateCaptureOutcome.EXPIRED -> app.coordinateCaptureRegistry.fail(captureId, "expired", "TIMEOUT", "O prazo da captura expirou.")
                CoordinateCaptureOutcome.FAILED -> app.coordinateCaptureRegistry.fail(captureId, "failed",
                    if (failure == CoordinateCaptureFailure.SERVICE_INTERRUPTED) "SERVICE_INTERRUPTED" else if (failure == CoordinateCaptureFailure.SERVICE_DISCONNECTED) "SERVICE_DISCONNECTED" else "INTERNAL_ERROR",
                    "A sessão de captura foi encerrada por uma falha.")
                CoordinateCaptureOutcome.COMPLETED -> Unit
            }
        }
    }


    private fun captureDisplayContext(targetPackage: String): CoordinateMapDisplayContext {
        val metrics = resources.displayMetrics
        val manager = getSystemService(WINDOW_SERVICE) as? WindowManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && manager != null) {
            val current = manager.currentWindowMetrics
            val bounds = current.bounds
            val insets = current.windowInsets.getInsets(
                WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout()
            )
            return CoordinateMapDisplayContext(
                targetPackage = targetPackage,
                widthPx = bounds.width(),
                heightPx = bounds.height(),
                densityDpi = metrics.densityDpi,
                rotation = manager.defaultDisplay.rotation,
                insetLeftPx = insets.left,
                insetTopPx = insets.top,
                insetRightPx = insets.right,
                insetBottomPx = insets.bottom
            )
        }
        return CoordinateMapDisplayContext(
            targetPackage = targetPackage,
            widthPx = metrics.widthPixels,
            heightPx = metrics.heightPixels,
            densityDpi = metrics.densityDpi,
            rotation = manager?.defaultDisplay?.rotation ?: 0
        )
    }

    private fun releaseCapture(captureId: String) {
        mainHandler.post {
            if (activeCaptureId == captureId) activeCaptureId = null
        }
    }

    private fun clearCaptureTasks(id: String, releaseActive: Boolean = true) {
        if (activeCaptureId != id) return
        captureDelayTask?.let(mainHandler::removeCallbacks)
        captureTimeoutTask?.let(mainHandler::removeCallbacks)
        captureDelayTask = null
        captureTimeoutTask = null
        if (releaseActive) activeCaptureId = null
    }

    private fun failScheduledCapture(code: String, message: String) {
        val id = activeCaptureId ?: return
        val registry = (application as? AndroidePyApplication)?.coordinateCaptureRegistry
        if (registry?.state(id) == "saving") return
        registry?.fail(id, "failed", code, message)
        clearCaptureTasks(id)
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        serviceOperational = true
        ensureCaptureController()
        accessibilityGateway?.onServiceConnected(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // Read the configured event type and, for that signal, only packageName.
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            // The package name is the only event field retained for the B2 target snapshot.
            captureTargetTracker.record(event.packageName?.toString())
            serviceOperational = true
            accessibilityGateway?.onServiceSignal(this)
        }
    }

    override fun onInterrupt() {
        serviceOperational = false
        captureController?.onServiceInterrupted()
        failScheduledCapture("SERVICE_INTERRUPTED", "O serviço foi interrompido.")
        captureTargetTracker.reset()
        accessibilityGateway?.onServiceInterrupted(this)
    }

    override fun onUnbind(intent: Intent): Boolean {
        serviceOperational = false
        captureController?.shutdown()
        failScheduledCapture("SERVICE_DISCONNECTED", "O serviço foi desconectado.")
        captureTargetTracker.reset()
        accessibilityGateway?.onServiceDisconnected(this)
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        serviceOperational = false
        captureController?.shutdown()
        failScheduledCapture("SERVICE_DISCONNECTED", "O serviço foi destruído.")
        captureTargetTracker.reset()
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
                        readNumber(request, "x"),
                        readNumber(request, "y"),
                        readString(request, "coordinate_space", "pixels")
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
                        readNumber(request, "x1"),
                        readNumber(request, "y1"),
                        readString(request, "coordinate_space", "pixels")
                    )
                    val end = resolvePoint(
                        readNumber(request, "x2"),
                        readNumber(request, "y2"),
                        readString(request, "coordinate_space", "pixels")
                    )
                    dispatchGestureOperation(
                        request,
                        start.first,
                        start.second,
                        end.first,
                        end.second,
                        readDuration(request),
                        completion
                    )
                }

                "screen.scroll" -> {
                    val metrics = resources.displayMetrics
                    val width = metrics.widthPixels
                    val height = metrics.heightPixels
                    val direction = readString(request, "direction")
                    val distance = readNumber(request, "distance")
                    if (!distance.isFinite() || distance <= 0.0 || distance > 1.0) {
                        throw invalidPayload(request, "A distância de rolagem é inválida.")
                    }
                    val duration = readDuration(request)
                    val span = when (direction) {
                        "up", "down" -> (height - 1) * distance
                        "left", "right" -> (width - 1) * distance
                        else -> throw invalidPayload(request, "A direção de rolagem é inválida.")
                    }
                    val centerX = (width - 1) / 2.0
                    val centerY = (height - 1) / 2.0
                    val (x1, y1, x2, y2) = when (direction) {
                        // Direction describes content movement, so the finger travels oppositely.
                        "down" -> listOf(centerX, centerY + span / 2, centerX, centerY - span / 2)
                        "up" -> listOf(centerX, centerY - span / 2, centerX, centerY + span / 2)
                        "right" -> listOf(centerX + span / 2, centerY, centerX - span / 2, centerY)
                        else -> listOf(centerX - span / 2, centerY, centerX + span / 2, centerY)
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
                    val text = request.payload.opt("text") as? String
                        ?: throw invalidPayload(request, "O texto deve ser uma string.")
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
        } catch (_: SecurityException) {
            completion(
                null,
                AccessibilityOperationException(
                    AccessibilityErrorCode.ACTION_REJECTED,
                    "O Android negou a operação solicitada.",
                    request.requestId
                )
            )
        } catch (_: Exception) {
            completion(
                null,
                AccessibilityOperationException(
                    AccessibilityErrorCode.INTERNAL_ERROR,
                    "Ocorreu uma falha interna ao processar a operação.",
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

        val pathEnd = if (x1 == x2 && y1 == y2) {
            val width = resources.displayMetrics.widthPixels.toFloat()
            val tapEndX = if (x1 < width - 1f) x1 + 1f else (x1 - 1f).coerceAtLeast(0f)
            tapEndX to y1
        } else {
            x2 to y2
        }
        val path = Path().apply {
            moveTo(x1, y1)
            lineTo(pathEnd.first, pathEnd.second)
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

    private fun readNumber(request: AccessibilityOperationRequest, key: String): Double {
        val value = request.payload.opt(key) as? Number
            ?: throw invalidPayload(request, "O parâmetro $key deve ser numérico.")
        return value.toDouble()
    }

    private fun readString(
        request: AccessibilityOperationRequest,
        key: String,
        defaultValue: String? = null
    ): String {
        if (!request.payload.has(key) && defaultValue != null) return defaultValue
        return request.payload.opt(key) as? String
            ?: throw invalidPayload(request, "O parâmetro $key deve ser texto.")
    }

    private fun readDuration(request: AccessibilityOperationRequest): Long {
        val value = request.payload.opt("duration_ms") as? Number
            ?: throw invalidPayload(request, "A duração do gesto deve ser um inteiro.")
        val numeric = value.toDouble()
        val duration = value.toLong()
        if (!numeric.isFinite() || numeric != duration.toDouble()) {
            throw invalidPayload(request, "A duração do gesto deve ser um inteiro.")
        }
        return duration
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

package com.samuel.androidepy.bridge

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.os.Build
import android.os.Looper
import com.samuel.androidepy.AndroidePyApplication
import com.samuel.androidepy.accessibility.AccessibilityErrorCode
import com.samuel.androidepy.accessibility.AccessibilityOperationException
import com.samuel.androidepy.accessibility.AccessibilityOperationRequest
import com.samuel.androidepy.accessibility.AccessibilityStatus
import org.json.JSONException
import org.json.JSONObject
import java.util.UUID
import kotlinx.coroutines.runBlocking
import com.samuel.androidepy.data.CoordinateMapNotFoundException

/**
 * Android-side adapter for the public capabilities exposed to Python.
 */
class AndroidBridgeAdapter(context: Context) {

    private val appContext = context.applicationContext

    /**
     * Internal status bridge for mobile.screen.get_status().
     */
    fun getAccessibilityStatusJson(): String {
        val application = appContext as? AndroidePyApplication
            ?: return JSONObject()
                .put("schema_version", 1)
                .put("ok", false)
                .put("error", JSONObject()
                    .put("code", "BRIDGE_NOT_CONFIGURED")
                    .put("message", "A Android Bridge não está vinculada à aplicação Androide PY.")
                    .put("retryable", false))
                .toString()

        val status = application.accessibilityGateway.refreshStatus()
        return JSONObject()
            .put("schema_version", 1)
            .put("ok", true)
            .put("enabled", status != AccessibilityStatus.NOT_ENABLED)
            .put("connected", status == AccessibilityStatus.CONNECTED)
            .put("state", status.wireValue)
            .toString()
    }

    /**
     * Receives a versioned JSON request from the existing serial Python worker.
     * The only blocking wait occurs inside the native coordinator, which rejects
     * calls from Android's main thread.
     */
    fun executeAccessibilityOperationJson(requestJson: String): String {
        var requestId: String? = null
        return try {
            if (Looper.myLooper() == Looper.getMainLooper()) {
                throw AccessibilityOperationException(
                    AccessibilityErrorCode.INTERNAL_ERROR,
                    "A operação não pode aguardar na thread principal."
                )
            }

            val envelope = JSONObject(requestJson)
            val schemaVersion = envelope.opt("schema_version") as? Number
            if (schemaVersion == null || schemaVersion.toDouble() != PROTOCOL_VERSION.toDouble()) {
                throw AccessibilityOperationException(
                    AccessibilityErrorCode.INVALID_REQUEST,
                    "A versão da solicitação não é suportada."
                )
            }

            requestId = (envelope.opt("request_id") as? String)?.takeIf { it.isNotBlank() }
            try {
                UUID.fromString(requestId ?: "")
            } catch (_: IllegalArgumentException) {
                throw AccessibilityOperationException(
                    AccessibilityErrorCode.INVALID_REQUEST,
                    "O identificador da solicitação é inválido."
                )
            }

            val operation = envelope.opt("operation") as? String
                ?: throw AccessibilityOperationException(
                    AccessibilityErrorCode.INVALID_REQUEST,
                    "A operação deve ser identificada por texto.",
                    requestId
                )
            if (operation !in SUPPORTED_OPERATIONS) {
                throw AccessibilityOperationException(
                    AccessibilityErrorCode.UNSUPPORTED_OPERATION,
                    "A operação solicitada não está disponível.",
                    requestId
                )
            }

            val payload = envelope.optJSONObject("payload")
                ?: throw AccessibilityOperationException(
                    AccessibilityErrorCode.INVALID_PAYLOAD,
                    "O conteúdo da solicitação é inválido.",
                    requestId
                )
            val timeoutValue = envelope.opt("timeout_ms") as? Number
                ?: throw AccessibilityOperationException(
                    AccessibilityErrorCode.INVALID_REQUEST,
                    "O prazo da solicitação deve ser um inteiro.",
                    requestId
                )
            val timeoutNumber = timeoutValue.toDouble()
            val timeoutMs = timeoutValue.toLong()
            if (!timeoutNumber.isFinite() ||
                timeoutNumber != timeoutMs.toDouble() ||
                timeoutMs !in MIN_TIMEOUT_MS..MAX_TIMEOUT_MS
            ) {
                throw AccessibilityOperationException(
                    AccessibilityErrorCode.INVALID_REQUEST,
                    "O prazo da solicitação é inválido.",
                    requestId
                )
            }

            val application = appContext as? AndroidePyApplication
                ?: throw AccessibilityOperationException(
                    AccessibilityErrorCode.INTERNAL_ERROR,
                    "A Android Bridge não está vinculada à aplicação.",
                    requestId
                )

            val request = AccessibilityOperationRequest(
                requestId = requestId!!,
                operation = operation,
                payload = payload,
                timeoutMs = timeoutMs
            )
            val result = application.accessibilityGateway.executeOperation(request)

            JSONObject()
                .put("schema_version", PROTOCOL_VERSION)
                .put("request_id", request.requestId)
                .put("ok", true)
                .put("result", result)
                .put("error", JSONObject.NULL)
                .toString()
        } catch (error: AccessibilityOperationException) {
            operationFailure(requestId ?: error.requestId, error)
        } catch (_: JSONException) {
            operationFailure(
                requestId,
                AccessibilityOperationException(
                    AccessibilityErrorCode.INVALID_REQUEST,
                    "A solicitação JSON está malformada.",
                    requestId
                )
            )
        } catch (_: Exception) {
            operationFailure(
                requestId,
                AccessibilityOperationException(
                    AccessibilityErrorCode.INTERNAL_ERROR,
                    "Não foi possível processar a operação de acessibilidade.",
                    requestId
                )
            )
        }
    }

    private fun operationFailure(
        requestId: String?,
        error: AccessibilityOperationException
    ): String {
        val errorPayload = JSONObject()
            .put("code", error.code.wireCode)
            .put("message", error.message)
            .put("retryable", error.retryable)

        return JSONObject()
            .put("schema_version", PROTOCOL_VERSION)
            .put("request_id", requestId ?: JSONObject.NULL)
            .put("ok", false)
            .put("result", JSONObject.NULL)
            .put("error", errorPayload)
            .toString()
    }

    fun startCoordinateCaptureJson(requestJson: String): String {
        return try {
        if (Looper.myLooper() == Looper.getMainLooper()) return bridgeError("INTERNAL_ERROR", "A captura não pode iniciar na main thread.")
        val request = JSONObject(requestJson)
        val requestId = request.optString("request_id")
        UUID.fromString(requestId)
        val name = request.optString("name").trim()
        val delay = request.optLong("delay_ms", -1L)
        val timeout = request.optLong("timeout_ms", -1L)
        if (request.optInt("schema_version", -1) != 1 || name.isEmpty() || name.length > 80 ||
            delay !in 0L..120_000L || timeout !in 1_000L..300_000L || delay >= timeout
        ) return bridgeError("INVALID_PAYLOAD", "Os parâmetros da captura são inválidos.", requestId)
        val app = appContext as? AndroidePyApplication
            ?: return bridgeError("BRIDGE_NOT_CONFIGURED", "A Bridge não está configurada.", requestId)
        val duplicate = runBlocking {
            try { app.coordinateMapRepository.get(name); true }
            catch (_: CoordinateMapNotFoundException) { false }
        }
        if (duplicate) return bridgeError("MAP_ALREADY_EXISTS", "Já existe um mapa com esse nome.", requestId)
        val captureId = app.coordinateCaptureRegistry.create(name)
        if (!app.accessibilityGateway.startCoordinateMapCapture(captureId, delay, timeout)) {
            app.coordinateCaptureRegistry.fail(captureId, "failed", "SERVICE_DISCONNECTED", "O serviço não está conectado.")
        }
        JSONObject().put("ok", true).put("request_id", requestId)
            .put("result", JSONObject().put("capture_id", captureId).put("state", "scheduled")).toString()
        } catch (_: Exception) { bridgeError("INVALID_REQUEST", "A solicitação de captura é inválida.") }
    }

    fun getCoordinateCaptureStatusJson(captureId: String): String {
        val app = appContext as? AndroidePyApplication
            ?: return bridgeError("BRIDGE_NOT_CONFIGURED", "A Bridge não está configurada.")
        return app.coordinateCaptureRegistry.statusJson(captureId)
    }

    fun cancelCoordinateCaptureJson(captureId: String): String {
        val app = appContext as? AndroidePyApplication
            ?: return bridgeError("BRIDGE_NOT_CONFIGURED", "A Bridge não está configurada.")
        if (!app.coordinateCaptureRegistry.isKnown(captureId)) return bridgeError("CAPTURE_NOT_FOUND", "A captura não existe.", captureId)
        if (app.coordinateCaptureRegistry.state(captureId) == "saving") {
            return bridgeError("CAPTURE_ALREADY_COMPLETED", "A captura já foi concluída e está sendo persistida.", captureId)
        }
        if (!app.accessibilityGateway.cancelCoordinateMapCapture(captureId)) app.coordinateCaptureRegistry.cancelLocally(captureId)
        return JSONObject().put("ok", true).put("result", JSONObject().put("capture_id", captureId).put("cancelled", true)).toString()
    }

    fun listCoordinateMapsJson(): String {
        val app = appContext as? AndroidePyApplication ?: return bridgeError("BRIDGE_NOT_CONFIGURED", "A Bridge não está configurada.")
        return try {
            val maps = runBlocking { app.coordinateMapRepository.list() }
            val array = org.json.JSONArray()
            maps.forEach { item -> array.put(JSONObject().put("name", item.name)
                .put("created_at_epoch_ms", item.createdAtEpochMs).put("updated_at_epoch_ms", item.updatedAtEpochMs)
                .put("point_count", item.pointCount)) }
            JSONObject().put("ok", true).put("result", array).toString()
        } catch (_: Exception) { bridgeError("INTERNAL_ERROR", "Não foi possível listar os mapas.") }
    }

    fun getCoordinateMapJson(name: String): String {
        val app = appContext as? AndroidePyApplication ?: return bridgeError("BRIDGE_NOT_CONFIGURED", "A Bridge não está configurada.")
        return try {
            val map = runBlocking { app.coordinateMapRepository.get(name) }
            val points = org.json.JSONArray()
            map.points.forEach { point -> points.put(JSONObject().put("name", point.name).put("x_px", point.xPx)
                .put("y_px", point.yPx).put("x_normalized", point.xNormalized).put("y_normalized", point.yNormalized)) }
            val result = JSONObject().put("schema_version", 1).put("name", map.map.name)
                .put("created_at_epoch_ms", map.map.createdAtEpochMs).put("target_package", map.map.targetPackage ?: JSONObject.NULL)
                .put("display", JSONObject().put("width_px", map.map.displayWidthPx).put("height_px", map.map.displayHeightPx)
                    .put("density_dpi", map.map.densityDpi).put("rotation", map.map.rotation)
                    .put("insets", JSONObject().put("left", map.map.insetLeftPx).put("top", map.map.insetTopPx)
                        .put("right", map.map.insetRightPx).put("bottom", map.map.insetBottomPx))).put("points", points)
            JSONObject().put("ok", true).put("result", result).toString()
        } catch (_: CoordinateMapNotFoundException) { bridgeError("MAP_NOT_FOUND", "Mapa de coordenadas não encontrado.") }
        catch (_: Exception) { bridgeError("INTERNAL_ERROR", "Não foi possível consultar o mapa.") }
    }

    fun deleteCoordinateMapJson(name: String): String {
        val app = appContext as? AndroidePyApplication ?: return bridgeError("BRIDGE_NOT_CONFIGURED", "A Bridge não está configurada.")
        return try {
            runBlocking { app.coordinateMapRepository.delete(name) }
            JSONObject().put("ok", true).put("result", JSONObject().put("name", name).put("deleted", true)).toString()
        } catch (_: CoordinateMapNotFoundException) { bridgeError("MAP_NOT_FOUND", "Mapa de coordenadas não encontrado.") }
        catch (_: Exception) { bridgeError("INTERNAL_ERROR", "Não foi possível excluir o mapa.") }
    }

    fun tapCoordinatePointJson(requestJson: String): String {
        var requestId: String? = null
        return try {
            if (Looper.myLooper() == Looper.getMainLooper()) return bridgeError("INTERNAL_ERROR", "O toque não pode aguardar na main thread.")
            val request = JSONObject(requestJson)
            val parsedRequestId = request.optString("request_id").takeIf { it.isNotBlank() }
                ?: return bridgeError("INVALID_REQUEST", "Identificador ausente.")
            requestId = parsedRequestId
            UUID.fromString(parsedRequestId)
            val app = appContext as? AndroidePyApplication ?: return bridgeError("BRIDGE_NOT_CONFIGURED", "A Bridge não está configurada.", parsedRequestId)
            val map = runBlocking { app.coordinateMapRepository.get(request.optString("map_name")) }
            val point = map.points.firstOrNull { it.name.equals(request.optString("point_name"), ignoreCase = true) }
                ?: return bridgeError("MAP_POINT_NOT_FOUND", "Ponto não encontrado no mapa.", requestId)
            if (map.map.targetPackage != null && map.map.targetPackage != app.accessibilityGateway.currentTargetPackage())
                return bridgeError("MAP_CONTEXT_MISMATCH", "O aplicativo em primeiro plano não corresponde ao mapa.", requestId)
            val wm = appContext.getSystemService(Context.WINDOW_SERVICE) as? android.view.WindowManager
            if ((wm?.defaultDisplay?.rotation ?: 0) != map.map.rotation)
                return bridgeError("MAP_CONTEXT_MISMATCH", "A orientação do display não corresponde ao mapa.", requestId)
            val timeout = request.optLong("timeout_ms", 10_000L)
            if (timeout !in 1L..120_000L) return bridgeError("INVALID_PAYLOAD", "Timeout inválido.", requestId)
            val metrics = appContext.resources.displayMetrics
            val x = (point.xNormalized * metrics.widthPixels).toInt().coerceIn(0, metrics.widthPixels - 1)
            val y = (point.yNormalized * metrics.heightPixels).toInt().coerceIn(0, metrics.heightPixels - 1)
            val operation = AccessibilityOperationRequest(parsedRequestId, "screen.tap",
                JSONObject().put("x", x).put("y", y).put("coordinate_space", "pixels"), timeout)
            val result = app.accessibilityGateway.executeOperation(operation)
            JSONObject().put("ok", true).put("request_id", requestId).put("result", result).toString()
        } catch (_: CoordinateMapNotFoundException) { bridgeError("MAP_NOT_FOUND", "Mapa de coordenadas não encontrado.", requestId) }
        catch (error: AccessibilityOperationException) { operationFailure(requestId, error) }
        catch (_: Exception) { bridgeError("INTERNAL_ERROR", "Não foi possível executar o ponto salvo.", requestId) }
    }

    private fun bridgeError(code: String, message: String, requestId: String? = null): String =
        JSONObject().put("ok", false).put("request_id", requestId ?: JSONObject.NULL)
            .put("error", JSONObject().put("code", code).put("message", message).put("retryable", false)).toString()

    fun getDeviceInfoJson(): String {
        val result = JSONObject()
            .put("schema_version", 1)
            .put("manufacturer", Build.MANUFACTURER)
            .put("brand", Build.BRAND)
            .put("model", Build.MODEL)
            .put("device", Build.DEVICE)
            .put("android_release", Build.VERSION.RELEASE)
            .put("api_level", Build.VERSION.SDK_INT)
            .put("supported_abis", org.json.JSONArray(Build.SUPPORTED_ABIS.toList()))
            .put("app_package_name", appContext.packageName)
            .put("app_version_name", appVersionName() ?: JSONObject.NULL)

        return result.toString()
    }

    fun launchAppJson(requestedPackageName: String): String {
        val packageName = requestedPackageName.trim()

        if (!PackageNameValidator.isValid(packageName)) {
            return failure(
                packageName,
                "INVALID_PACKAGE_NAME",
                "Informe um nome de pacote Android válido."
            )
        }

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            launchWithIntentSender(packageName)
        } else {
            launchWithLaunchIntent(packageName)
        }
    }

    private fun launchWithIntentSender(packageName: String): String {
        return try {
            val sender = appContext.packageManager
                .getLaunchIntentSenderForPackage(packageName)

            sender.sendIntent(appContext, 0, null, null, null)
            success(packageName)
        } catch (_: IntentSender.SendIntentException) {
            failure(
                packageName,
                "APP_NOT_FOUND_OR_NOT_LAUNCHABLE",
                "O aplicativo não foi encontrado ou não possui uma atividade inicializável."
            )
        } catch (_: SecurityException) {
            failure(
                packageName,
                "LAUNCH_DENIED",
                "O Android não permitiu iniciar este aplicativo."
            )
        } catch (_: ActivityNotFoundException) {
            failure(
                packageName,
                "LAUNCH_ACTIVITY_NOT_FOUND",
                "Nenhuma atividade inicializável foi encontrada para este pacote."
            )
        } catch (_: RuntimeException) {
            failure(
                packageName,
                "LAUNCH_FAILED",
                "O Android não conseguiu iniciar este aplicativo."
            )
        }
    }

    @Suppress("DEPRECATION")
    private fun launchWithLaunchIntent(packageName: String): String {
        val launchIntent = appContext.packageManager.getLaunchIntentForPackage(packageName)
            ?: return failure(
                packageName,
                "APP_NOT_FOUND_OR_NOT_VISIBLE",
                "O aplicativo não foi encontrado ou não está visível para o Androide PY."
            )

        return try {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            appContext.startActivity(launchIntent)
            success(packageName)
        } catch (_: SecurityException) {
            failure(
                packageName,
                "LAUNCH_DENIED",
                "O Android não permitiu iniciar este aplicativo."
            )
        } catch (_: ActivityNotFoundException) {
            failure(
                packageName,
                "LAUNCH_ACTIVITY_NOT_FOUND",
                "Nenhuma atividade inicializável foi encontrada para este pacote."
            )
        } catch (_: RuntimeException) {
            failure(
                packageName,
                "LAUNCH_FAILED",
                "O Android não conseguiu iniciar este aplicativo."
            )
        }
    }

    private fun success(packageName: String): String =
        JSONObject()
            .put("ok", true)
            .put("package_name", packageName)
            .put("error", JSONObject.NULL)
            .toString()

    private fun failure(
        packageName: String,
        code: String,
        message: String
    ): String {
        val error = JSONObject()
            .put("code", code)
            .put("message", message)

        return JSONObject()
            .put("ok", false)
            .put("package_name", packageName)
            .put("error", error)
            .toString()
    }

    private fun appVersionName(): String? {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                appContext.packageManager.getPackageInfo(
                    appContext.packageName,
                    PackageManager.PackageInfoFlags.of(0L)
                )
            } else {
                @Suppress("DEPRECATION")
                appContext.packageManager.getPackageInfo(appContext.packageName, 0)
            }
            packageInfo.versionName
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
    }

    private companion object {
        const val PROTOCOL_VERSION = 1
        const val MIN_TIMEOUT_MS = 1L
        const val MAX_TIMEOUT_MS = 120_000L
        val SUPPORTED_OPERATIONS = setOf(
            "screen.tap",
            "screen.type_text",
            "screen.swipe",
            "screen.scroll"
        )
    }
}

internal object PackageNameValidator {
    private val packagePattern =
        Regex("^[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+$")

    fun isValid(packageName: String): Boolean = packagePattern.matches(packageName)
}

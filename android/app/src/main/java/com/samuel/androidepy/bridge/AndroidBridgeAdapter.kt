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

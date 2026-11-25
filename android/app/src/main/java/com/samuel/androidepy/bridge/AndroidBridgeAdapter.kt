package com.samuel.androidepy.bridge

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.os.Build
import org.json.JSONArray
import org.json.JSONObject

/**
 * Android-side adapter for the public capabilities exposed to Python.
 */
class AndroidBridgeAdapter(context: Context) {

    private val appContext = context.applicationContext

    fun getDeviceInfoJson(): String {
        val result = JSONObject()
            .put("schema_version", 1)
            .put("manufacturer", Build.MANUFACTURER)
            .put("brand", Build.BRAND)
            .put("model", Build.MODEL)
            .put("device", Build.DEVICE)
            .put("android_release", Build.VERSION.RELEASE)
            .put("api_level", Build.VERSION.SDK_INT)
            .put("supported_abis", JSONArray(Build.SUPPORTED_ABIS.toList()))
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
}

internal object PackageNameValidator {
    private val packagePattern =
        Regex("^[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+$")

    fun isValid(packageName: String): Boolean = packagePattern.matches(packageName)
}

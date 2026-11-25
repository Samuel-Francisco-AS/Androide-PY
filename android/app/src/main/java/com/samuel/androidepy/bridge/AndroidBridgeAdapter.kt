package com.samuel.androidepy.bridge

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import org.json.JSONArray
import org.json.JSONObject

/**
 * Android-side adapter for the public, non-sensitive device information
 * exposed to Python as mobile.device.get_info().
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

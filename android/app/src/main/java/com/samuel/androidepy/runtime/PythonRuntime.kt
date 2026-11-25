package com.samuel.androidepy.runtime

import android.content.Context
import android.os.Looper
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform

/**
 * Owns the process-wide Chaquopy runtime lifecycle.
 *
 * Initialization must be called from a worker thread. The runtime is started
 * once per application process and reused by subsequent callers.
 */
object PythonRuntime {

    private val lock = Any()

    @Volatile
    private var instance: Python? = null

    fun initialize(context: Context): Python {
        checkWorkerThread()
        instance?.let { return it }

        return synchronized(lock) {
            instance ?: run {
                if (!Python.isStarted()) {
                    Python.start(AndroidPlatform(context.applicationContext))
                }

                Python.getInstance().also { instance = it }
            }
        }
    }

    fun getModule(name: String): PyObject {
        checkWorkerThread()
        val python = instance ?: error(
            "Python runtime is not initialized. Call initialize() from a worker thread first."
        )
        return python.getModule(name)
    }

    fun runSmokeTest(): String =
        getModule("main").callAttr("smoke_test").toString()

    fun isInitialized(): Boolean = instance != null || Python.isStarted()

    private fun checkWorkerThread() {
        check(Looper.myLooper() != Looper.getMainLooper()) {
            "Python operations must run away from the Android main thread."
        }
    }
}

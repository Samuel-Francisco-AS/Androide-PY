package com.samuel.androidepy

import android.app.Application
import com.samuel.androidepy.execution.ScriptExecutor

class AndroidePyApplication : Application() {

    val scriptExecutor: ScriptExecutor by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        ScriptExecutor(this)
    }
}

package com.samuel.androidepy

import android.app.Application
import com.samuel.androidepy.data.AppDatabase
import com.samuel.androidepy.data.ScriptRepository
import com.samuel.androidepy.execution.ScriptExecutor

class AndroidePyApplication : Application() {

    val scriptExecutor: ScriptExecutor by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        ScriptExecutor(this)
    }

    val scriptRepository: ScriptRepository by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        ScriptRepository(AppDatabase.getInstance(this).scriptDao())
    }
}

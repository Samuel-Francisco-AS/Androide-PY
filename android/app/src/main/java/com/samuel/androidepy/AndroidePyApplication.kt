package com.samuel.androidepy

import android.app.Application
import com.samuel.androidepy.accessibility.AccessibilityGateway
import com.samuel.androidepy.data.AppDatabase
import com.samuel.androidepy.data.ScriptExecutionRepository
import com.samuel.androidepy.data.ScriptRepository
import com.samuel.androidepy.execution.ScriptExecutor

class AndroidePyApplication : Application() {
    val accessibilityGateway: AccessibilityGateway by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        AccessibilityGateway()
    }

    private val database by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        AppDatabase.getInstance(this)
    }

    val scriptExecutor: ScriptExecutor by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        ScriptExecutor(this)
    }

    val scriptRepository: ScriptRepository by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        ScriptRepository(database.scriptDao())
    }

    val scriptExecutionRepository: ScriptExecutionRepository by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        ScriptExecutionRepository(database.scriptExecutionDao())
    }
}

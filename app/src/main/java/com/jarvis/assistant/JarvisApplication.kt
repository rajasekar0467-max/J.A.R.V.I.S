package com.jarvis.assistant

import android.app.Application
import com.jarvis.assistant.core.AppContainer
import com.jarvis.assistant.core.CrashLogger

class JarvisApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        val crashLogger = runCatching { CrashLogger.install(this) }.getOrNull()
        container = AppContainer(applicationContext, crashLogger)
    }
}

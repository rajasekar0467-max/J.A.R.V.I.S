package com.jarvis.assistant.core

import android.app.Application
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CrashLogger private constructor(private val crashFile: File) {

    fun lastCrash(): String? {
        return runCatching {
            if (crashFile.exists()) crashFile.readText(Charsets.UTF_8).takeIf { it.isNotBlank() } else null
        }.getOrNull()
    }

    fun clear() {
        runCatching {
            if (crashFile.exists()) crashFile.delete()
        }
    }

    companion object {
        @Volatile
        private var instance: CrashLogger? = null

        fun install(app: Application): CrashLogger {
            val dir = File(app.filesDir, "diagnostics")
            val file = File(dir, "last_crash.txt")
            val logger = CrashLogger(file)
            instance = logger

            val previous = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
                runCatching {
                    if (!dir.exists()) dir.mkdirs()
                    val ts = SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.US).format(Date())
                    val frames = throwable.stackTrace.take(30).joinToString("\n") { "    at $it" }
                    val report = buildString {
                        appendLine("Timestamp: $ts")
                        appendLine("Thread: ${thread.name}")
                        appendLine("Exception: ${throwable.javaClass.name}")
                        appendLine("Message: ${Redactor.redact(throwable.message)}")
                        appendLine(frames)
                    }
                    file.writeText(report, Charsets.UTF_8)
                }
                previous?.uncaughtException(thread, throwable)
            }
            return logger
        }

        fun getOrNull(): CrashLogger? = instance
    }
}

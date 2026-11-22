package com.jarvis.assistant.core

import android.content.Context
import kotlinx.coroutines.CancellationException

interface KeyValueStore {
    fun getBoolean(key: String, default: Boolean): Boolean
    fun putBoolean(key: String, value: Boolean)
    fun getString(key: String): String?
    fun putString(key: String, value: String)
    fun clear()
}

class SharedPrefsKeyValueStore(context: Context, name: String = "jarvis_startup_prefs") : KeyValueStore {
    private val prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE)
    override fun getBoolean(key: String, default: Boolean): Boolean = prefs.getBoolean(key, default)
    override fun putBoolean(key: String, value: Boolean) { prefs.edit().putBoolean(key, value).apply() }
    override fun getString(key: String): String? = prefs.getString(key, null)
    override fun putString(key: String, value: String) { prefs.edit().putString(key, value).apply() }
    override fun clear() { prefs.edit().clear().apply() }
}

data class StartupStepResult(
    val name: String,
    val ok: Boolean,
    val detail: String,
    val durationMs: Long
)

data class StartupReport(
    val previousLaunchIncomplete: Boolean,
    val previousCrashSummary: String?,
    val steps: List<StartupStepResult>
) {
    val allOk: Boolean get() = steps.all { it.ok }
}

class StartupTracker(
    private val store: KeyValueStore,
    private val errorLog: ErrorLog,
    private val clock: () -> Long = { System.currentTimeMillis() }
) {
    private val steps = mutableListOf<StartupStepResult>()
    private var previousIncomplete = false

    fun begin(): Boolean {
        previousIncomplete = store.getBoolean(KEY_LAUNCH_IN_PROGRESS, false)
        store.putBoolean(KEY_LAUNCH_IN_PROGRESS, true)
        steps.clear()
        return previousIncomplete
    }

    suspend fun <T> step(name: String, block: suspend () -> T): Result<T> {
        val start = clock()
        return try {
            val value = block()
            val elapsed = (clock() - start).coerceAtLeast(0L)
            steps.add(StartupStepResult(name, ok = true, detail = "OK", durationMs = elapsed))
            Result.success(value)
        } catch (ce: CancellationException) {
            throw ce
        } catch (t: Throwable) {
            val elapsed = (clock() - start).coerceAtLeast(0L)
            val detail = Redactor.redact("${t.javaClass.simpleName}: ${t.message ?: "unknown error"}")
            steps.add(StartupStepResult(name, ok = false, detail = detail, durationMs = elapsed))
            errorLog.record("Startup:$name", detail, t)
            Result.failure(t)
        }
    }

    fun buildReport(previousCrash: String?): StartupReport {
        val report = StartupReport(
            previousLaunchIncomplete = previousIncomplete,
            previousCrashSummary = previousCrash,
            steps = steps.toList()
        )
        val summary = report.steps.joinToString("; ") { "${it.name}=${if (it.ok) "OK" else "FAIL"}(${it.durationMs}ms)" }
        store.putString(KEY_LAST_REPORT, summary)
        return report
    }

    fun markFinished() {
        store.putBoolean(KEY_LAUNCH_IN_PROGRESS, false)
    }

    fun lastSummary(): String? = store.getString(KEY_LAST_REPORT)

    fun clear() {
        store.clear()
        steps.clear()
    }

    companion object {
        private const val KEY_LAUNCH_IN_PROGRESS = "launch_in_progress"
        private const val KEY_LAST_REPORT = "last_startup_report"
    }
}

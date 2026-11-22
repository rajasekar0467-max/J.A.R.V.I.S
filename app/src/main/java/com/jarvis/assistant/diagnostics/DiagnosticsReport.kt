package com.jarvis.assistant.diagnostics

import com.jarvis.assistant.core.ErrorEntry
import com.jarvis.assistant.core.Redactor
import com.jarvis.assistant.core.StartupReport

data class DiagnosticsReport(
    val generatedAtMs: Long,
    val appVersion: String,
    val deviceSummary: String,
    val androidVersion: String,
    val storageMode: String,
    val keyStoreStatus: String,
    val hasApiKey: Boolean,
    val endpointUrl: String,
    val activeModel: String,
    val networkStatus: String,
    val micPermissionGranted: Boolean,
    val notificationPermissionGranted: Boolean,
    val speechRecognizerAvailable: Boolean,
    val ttsReady: Boolean,
    val availableTtsVoices: Int,
    val aiEndpointStatus: String,
    val startupReport: StartupReport?,
    val lastCrashSummary: String?,
    val recentErrors: List<ErrorEntry>
) {
    fun toShareableText(): String = Redactor.redact(buildString {
        appendLine("JARVIS Diagnostics Report")
        appendLine("App Version: $appVersion")
        appendLine("Device: $deviceSummary ($androidVersion)")
        appendLine("Storage Mode: $storageMode")
        appendLine("KeyStore: $keyStoreStatus | API Key Saved: $hasApiKey")
        appendLine("Endpoint: $endpointUrl | Model: $activeModel")
        appendLine("AI Reachability: $aiEndpointStatus")
        appendLine("Network: $networkStatus")
        appendLine("Mic Permission: $micPermissionGranted | Notifications: $notificationPermissionGranted")
        appendLine("Speech Recognizer: $speechRecognizerAvailable | TTS Ready: $ttsReady (Voices: $availableTtsVoices)")
        startupReport?.let { sr ->
            appendLine("Startup All OK: ${sr.allOk} | Previous Incomplete: ${sr.previousLaunchIncomplete}")
            sr.steps.forEach { s ->
                appendLine("  - ${s.name}: ${if (s.ok) "OK" else "FAIL"} (${s.durationMs}ms) ${s.detail}")
            }
        }
        if (!lastCrashSummary.isNullOrBlank()) {
            appendLine("Last Crash:")
            appendLine(lastCrashSummary.take(600))
        }
        if (recentErrors.isNotEmpty()) {
            appendLine("Recent Errors (${recentErrors.size}):")
            recentErrors.takeLast(10).forEach { e ->
                appendLine("  [${e.tag}] ${e.message}")
            }
        }
    })
}

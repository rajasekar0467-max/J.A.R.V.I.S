package com.jarvis.assistant.diagnostics

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.jarvis.assistant.BuildConfig
import com.jarvis.assistant.ai.AiEngine
import com.jarvis.assistant.core.CrashLogger
import com.jarvis.assistant.core.ErrorLog
import com.jarvis.assistant.core.NetworkMonitor
import com.jarvis.assistant.core.StartupReport
import com.jarvis.assistant.device.NotificationHelper
import com.jarvis.assistant.security.ApiKeyStore
import com.jarvis.assistant.settings.SettingsSource
import com.jarvis.assistant.voice.SpeechInput
import com.jarvis.assistant.voice.SpeechOutput
import kotlinx.coroutines.flow.first

class DiagnosticsCollector(
    private val context: Context,
    private val storageModeProvider: () -> String,
    private val keyStore: ApiKeyStore,
    private val settingsSource: SettingsSource,
    private val networkMonitor: NetworkMonitor,
    private val speechInput: SpeechInput,
    private val speechOutput: SpeechOutput,
    private val aiEngine: AiEngine,
    private val startupReportProvider: () -> StartupReport?,
    private val crashLogger: CrashLogger?,
    private val errorLog: ErrorLog
) {

    suspend fun collect(runNetworkProbe: Boolean = false): DiagnosticsReport {
        val settings = runCatching { settingsSource.settings.first() }.getOrNull()
        val net = networkMonitor.currentSnapshot()
        val micGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        val notifGranted = NotificationHelper.canPostNotifications(context)

        val apiKey = keyStore.getKey().orEmpty()
        val aiStatus = if (!runNetworkProbe) {
            "Not probed (tap Test AI Connection)"
        } else {
            val diag = aiEngine.diagnose(
                baseUrl = settings?.baseUrl.orEmpty(),
                apiKey = apiKey,
                model = settings?.model.orEmpty()
            )
            "${if (diag.reachable) "OK" else "FAIL"} (${diag.latencyMs}ms): ${diag.detail}"
        }

        return DiagnosticsReport(
            generatedAtMs = System.currentTimeMillis(),
            appVersion = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
            deviceSummary = "${Build.MANUFACTURER} ${Build.MODEL}",
            androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            storageMode = storageModeProvider(),
            keyStoreStatus = keyStore.probeStatus(),
            hasApiKey = keyStore.hasKey(),
            endpointUrl = settings?.baseUrl ?: "Unavailable",
            activeModel = settings?.model ?: "Unavailable",
            networkStatus = if (net.connected) "${net.transport} (validated=${net.validated})" else "Offline",
            micPermissionGranted = micGranted,
            notificationPermissionGranted = notifGranted,
            speechRecognizerAvailable = speechInput.isAvailable(),
            ttsReady = speechOutput.isReady(),
            availableTtsVoices = speechOutput.availableVoiceNames().size,
            aiEndpointStatus = aiStatus,
            startupReport = startupReportProvider(),
            lastCrashSummary = crashLogger?.lastCrash(),
            recentErrors = errorLog.snapshot()
        )
    }
}

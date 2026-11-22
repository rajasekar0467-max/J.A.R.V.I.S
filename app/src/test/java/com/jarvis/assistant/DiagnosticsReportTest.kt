package com.jarvis.assistant

import com.jarvis.assistant.core.ErrorEntry
import com.jarvis.assistant.core.StartupReport
import com.jarvis.assistant.core.StartupStepResult
import com.jarvis.assistant.diagnostics.DiagnosticsReport
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosticsReportTest {

    @Test
    fun shareableReportRedactsLeakedSecrets() {
        val report = DiagnosticsReport(
            generatedAtMs = 1L,
            appVersion = "0.3.0 (3)",
            deviceSummary = "realme RMX3612",
            androidVersion = "Android 14 (API 34)",
            storageMode = "Room (SQLite)",
            keyStoreStatus = "AndroidKeyStore OK",
            hasApiKey = true,
            endpointUrl = "https://generativelanguage.googleapis.com/v1beta/openai/",
            activeModel = "gemini-2.0-flash",
            networkStatus = "Wi-Fi",
            micPermissionGranted = true,
            notificationPermissionGranted = true,
            speechRecognizerAvailable = true,
            ttsReady = true,
            availableTtsVoices = 4,
            aiEndpointStatus = "OK",
            startupReport = StartupReport(false, null, listOf(StartupStepResult("DB", true, "OK", 5L))),
            lastCrashSummary = "Bearer JARVIS_FAKE_SECRET_ALPHA_123456",
            recentErrors = listOf(ErrorEntry(1L, "AI", "Failed with JARVIS_FAKE_SECRET_BETA_123456"))
        )

        val text = report.toShareableText()
        assertTrue(text.contains("JARVIS Diagnostics Report"))
        assertFalse(text.contains("JARVIS_FAKE_SECRET_ALPHA_123456"))
        assertFalse(text.contains("JARVIS_FAKE_SECRET_BETA_123456"))
    }
}

package com.jarvis.assistant.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jarvis.assistant.ui.DiagnosticsViewModel
import com.jarvis.assistant.ui.components.GlassCard
import com.jarvis.assistant.ui.theme.EmeraldGlow
import com.jarvis.assistant.ui.theme.EmeraldPrimary
import com.jarvis.assistant.ui.theme.ErrorRed
import com.jarvis.assistant.ui.theme.TextPrimary
import com.jarvis.assistant.ui.theme.TextSecondary
import com.jarvis.assistant.ui.theme.VoidBlack

@Composable
fun DiagnosticsScreen(
    viewModel: DiagnosticsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val report by viewModel.report.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val status by viewModel.statusMessage.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refresh(runNetworkProbe = false)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("System Diagnostics", style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
            OutlinedButton(onClick = onBack) { Text("Back", color = EmeraldGlow) }
        }

        status?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = EmeraldGlow)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = { viewModel.refresh(runNetworkProbe = true) },
                enabled = !isLoading,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = VoidBlack)
            ) {
                Text(if (isLoading) "Testing..." else "Test AI Connection")
            }
            OutlinedButton(
                onClick = { viewModel.shareReport() },
                enabled = report != null,
                modifier = Modifier.weight(1f)
            ) {
                Text("Share Report", color = EmeraldGlow)
            }
        }

        report?.let { r ->
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                DiagRow("App Version", r.appVersion)
                DiagRow("Device", r.deviceSummary)
                DiagRow("Android", r.androidVersion)
                DiagRow("Storage", r.storageMode)
                DiagRow("KeyStore", r.keyStoreStatus)
                DiagRow("API Key Saved", if (r.hasApiKey) "Yes" else "No")
                DiagRow("Model", r.activeModel)
                DiagRow("AI Probe", r.aiEndpointStatus)
                DiagRow("Network", r.networkStatus)
                DiagRow("Mic Permission", if (r.micPermissionGranted) "Granted" else "Not granted")
                DiagRow("Notifications", if (r.notificationPermissionGranted) "Granted" else "Not granted")
                DiagRow("Speech Recognizer", if (r.speechRecognizerAvailable) "Available" else "Unavailable")
                DiagRow("TTS Engine", if (r.ttsReady) "Ready (${r.availableTtsVoices} voices)" else "Not ready")
            }

            r.startupReport?.let { sr ->
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text("Startup Self-Check", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Spacer(modifier = Modifier.height(6.dp))
                    sr.steps.forEach { step ->
                        DiagRow(step.name, "${if (step.ok) "OK" else "FAIL"} (${step.durationMs}ms)")
                    }
                }
            }

            if (!r.lastCrashSummary.isNullOrBlank() || r.recentErrors.isNotEmpty()) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text("Captured Logs (${r.recentErrors.size})", style = MaterialTheme.typography.titleMedium, color = ErrorRed)
                    Spacer(modifier = Modifier.height(6.dp))
                    r.lastCrashSummary?.let {
                        Text("Last Crash: ${it.take(260)}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    }
                    r.recentErrors.takeLast(6).forEach { e ->
                        Text("[${e.tag}] ${e.message}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(onClick = { viewModel.clearLogs() }) {
                        Text("Clear Crash & Error Logs", color = ErrorRed)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun DiagRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
    }
}

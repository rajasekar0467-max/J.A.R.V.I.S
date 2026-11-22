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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.jarvis.assistant.ai.ProviderPresets
import com.jarvis.assistant.language.LanguageMode
import com.jarvis.assistant.ui.SettingsViewModel
import com.jarvis.assistant.ui.components.GlassCard
import com.jarvis.assistant.ui.theme.EmeraldGlow
import com.jarvis.assistant.ui.theme.EmeraldPrimary
import com.jarvis.assistant.ui.theme.ErrorRed
import com.jarvis.assistant.ui.theme.TextPrimary
import com.jarvis.assistant.ui.theme.TextSecondary
import com.jarvis.assistant.ui.theme.VoidBlack
import com.jarvis.assistant.voice.FridayVoiceProfile
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    notificationsGranted: Boolean,
    onRequestNotifications: () -> Unit,
    onApiKeyChanged: () -> Unit,
    onOpenDiagnostics: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()
    val hasApiKey by viewModel.hasApiKey.collectAsState()
    val keyStoreStatus by viewModel.keyStoreStatus.collectAsState()
    val availableVoices by viewModel.availableVoices.collectAsState()
    val memoryFacts by viewModel.memoryFacts.collectAsState()
    val reminders by viewModel.reminders.collectAsState()
    val status by viewModel.statusMessage.collectAsState()

    var apiKeyInput by remember { mutableStateOf("") }
    var baseUrlInput by remember(settings.baseUrl) { mutableStateOf(settings.baseUrl) }
    var modelInput by remember(settings.model) { mutableStateOf(settings.model) }
    var confirmClearAll by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.refreshHardwareInfo()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        status?.let { msg ->
            GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = 12.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(msg, style = MaterialTheme.typography.bodyMedium, color = EmeraldGlow, modifier = Modifier.weight(1f))
                    TextButton(onClick = { viewModel.clearStatus() }) { Text("OK", color = EmeraldGlow) }
                }
            }
        }

        // AI Provider & KeyStore
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Text("AI Provider & Credentials", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "KeyStore: $keyStoreStatus • Key saved: ${if (hasApiKey) "Yes" else "No"}",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ProviderPresets.ALL.take(4).forEach { preset ->
                    FilterChip(
                        selected = settings.providerPresetId == preset.id,
                        onClick = { viewModel.applyPreset(preset.id) },
                        label = { Text(preset.id.uppercase()) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = baseUrlInput,
                onValueChange = { baseUrlInput = it },
                label = { Text("Base URL (HTTPS)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = modelInput,
                onValueChange = { modelInput = it },
                label = { Text("Model ID") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text("Temperature: ${"%.2f".format(settings.temperature)}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            Slider(
                value = settings.temperature,
                onValueChange = { viewModel.updateTemperature(it) },
                valueRange = 0.0f..1.4f
            )
            Text("Context Window: ${settings.maxContextMessages} messages", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            Slider(
                value = settings.maxContextMessages.toFloat(),
                onValueChange = { viewModel.updateMaxContextMessages(it.roundToInt()) },
                valueRange = 4f..40f
            )
            OutlinedButton(
                onClick = { viewModel.updateEndpoint(baseUrlInput, modelInput) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Endpoint & Model", color = EmeraldGlow)
            }
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = apiKeyInput,
                onValueChange = { apiKeyInput = it },
                label = { Text("API Key (stored encrypted)") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        viewModel.saveApiKey(apiKeyInput)
                        apiKeyInput = ""
                        onApiKeyChanged()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = VoidBlack)
                ) {
                    Text("Save API Key")
                }
                OutlinedButton(
                    onClick = {
                        viewModel.clearStoredSecrets()
                        onApiKeyChanged()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Clear Secrets", color = ErrorRed)
                }
            }
        }

        // Language & FRIDAY Voice Profile
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Text("Voice & Language", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Text(
                text = "Voice Profile: ${FridayVoiceProfile.PROFILE_NAME}",
                style = MaterialTheme.typography.labelMedium,
                color = EmeraldGlow
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LanguageMode.entries.forEach { mode ->
                    FilterChip(
                        selected = settings.language == mode,
                        onClick = { viewModel.updateLanguage(mode) },
                        label = { Text(mode.label.substringBefore(" ")) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Auto-Speak Replies", style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                Switch(checked = settings.autoSpeak, onCheckedChange = { viewModel.updateAutoSpeak(it) })
            }
            Text("Speech Rate: ${"%.2f".format(settings.speechRate)}x", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            Slider(
                value = settings.speechRate,
                onValueChange = { viewModel.updateSpeechRate(it) },
                valueRange = 0.65f..1.45f
            )
            Text("Pitch: ${"%.2f".format(settings.pitch)}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            Slider(
                value = settings.pitch,
                onValueChange = { viewModel.updatePitch(it) },
                valueRange = 0.80f..1.30f
            )
            Text("Listen Timeout: ${settings.listenTimeoutSec}s", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            Slider(
                value = settings.listenTimeoutSec.toFloat(),
                onValueChange = { viewModel.updateListenTimeout(it.roundToInt()) },
                valueRange = 4f..20f
            )
            if (availableVoices.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text("Device TTS Voice", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = settings.ttsVoiceName.isBlank(),
                        onClick = { viewModel.updateTtsVoiceName("") },
                        label = { Text("Auto") }
                    )
                    availableVoices.take(2).forEach { voiceName ->
                        FilterChip(
                            selected = settings.ttsVoiceName == voiceName,
                            onClick = { viewModel.updateTtsVoiceName(voiceName) },
                            label = { Text(voiceName.takeLast(14)) }
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Reduced Motion", style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                Switch(checked = settings.reducedMotion, onCheckedChange = { viewModel.updateReducedMotion(it) })
            }
            if (!notificationsGranted) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(onClick = onRequestNotifications, modifier = Modifier.fillMaxWidth()) {
                    Text("Enable Reminder Notifications", color = EmeraldGlow)
                }
            }
        }

        // Saved Memory & Reminders
        if (memoryFacts.isNotEmpty() || reminders.isNotEmpty()) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("Saved Memory (${memoryFacts.size}) & Reminders (${reminders.size})", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Spacer(modifier = Modifier.height(6.dp))
                memoryFacts.forEach { fact ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("• ${fact.text}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary, modifier = Modifier.weight(1f))
                        IconButton(onClick = { viewModel.deleteMemoryFact(fact.id) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete fact", tint = ErrorRed)
                        }
                    }
                }
                reminders.forEach { rem ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Reminder: ${rem.text}${if (rem.fired) " (Done)" else ""}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { viewModel.deleteReminder(rem.id) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete reminder", tint = ErrorRed)
                        }
                    }
                }
            }
        }

        // Data & Diagnostics
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Text("Maintenance & Diagnostics", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(onClick = onOpenDiagnostics, modifier = Modifier.fillMaxWidth()) {
                Text("Open Diagnostics", color = EmeraldGlow)
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = { confirmClearAll = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Clear All App Data (Keep Secrets)", color = ErrorRed)
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }

    if (confirmClearAll) {
        AlertDialog(
            onDismissRequest = { confirmClearAll = false },
            title = { Text("Clear All App Data?") },
            text = { Text("This deletes all conversations, local memory, reminders, and settings. Your stored API key in Android KeyStore is kept separate and will NOT be deleted.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearAllAppData()
                    confirmClearAll = false
                }) { Text("Clear App Data", color = ErrorRed) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClearAll = false }) { Text("Cancel") }
            }
        )
    }
}

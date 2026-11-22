package com.jarvis.assistant.ui.settings

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jarvis.assistant.security.AppDataResetManager
import com.jarvis.assistant.security.KeystoreManager
import com.jarvis.assistant.security.SecretSlot
import com.jarvis.assistant.security.SecretStore
import com.jarvis.assistant.settings.SettingsRepository
import com.jarvis.assistant.ui.theme.JarvisCyanPrimary
import com.jarvis.assistant.ui.theme.JarvisGreenVerified
import com.jarvis.assistant.ui.theme.JarvisRedAlert
import com.jarvis.assistant.ui.theme.JarvisSurfaceCard

@Composable
fun SettingsScreen(
    keystoreManager: KeystoreManager,
    secretStore: SecretStore,
    settingsRepository: SettingsRepository,
    appDataResetManager: AppDataResetManager
) {
    val settingsState by settingsRepository.state.collectAsState()
    var keyReport by remember { mutableStateOf(keystoreManager.inspectKeySecurityLevel()) }
    var selectedSlot by remember { mutableStateOf(SecretSlot.GEMINI_API_KEY) }
    var secretInput by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf("Ready. Enter an API key to encrypt with AndroidKeyStore AES-256-GCM.") }
    var refreshTick by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = JarvisSurfaceCard),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("ANDROID KEYSTORE RUNTIME DIAGNOSTIC", style = MaterialTheme.typography.titleMedium, color = JarvisCyanPrimary)
                Spacer(modifier = Modifier.height(6.dp))
                Text("Master Alias: ${KeystoreManager.MASTER_KEY_ALIAS}", style = MaterialTheme.typography.labelSmall)
                Text("Algorithm: ${keyReport.algorithm} (AES/GCM/NoPadding 256-bit)", style = MaterialTheme.typography.bodyMedium)
                Text("Reported Security Level: ${keyReport.securityLevelLabel}", style = MaterialTheme.typography.bodyMedium, color = JarvisGreenVerified)
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = JarvisSurfaceCard),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("SECRETSTORE VAULT (API KEYS)", style = MaterialTheme.typography.titleMedium, color = JarvisCyanPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                SecretSlot.values().forEach { slot ->
                    val configured = remember(refreshTick) { secretStore.isSecretConfigured(slot) }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(slot.displayName, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                if (configured) "STATUS: CONFIGURED (Encrypted)" else "STATUS: NOT SET (${slot.phaseUsed})",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (configured) JarvisGreenVerified else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Button(onClick = { selectedSlot = slot }) {
                            Text(if (selectedSlot == slot) "Selected" else "Select")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = secretInput,
                    onValueChange = { secretInput = it },
                    label = { Text("Enter ${selectedSlot.displayName}") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        val res = secretStore.saveSecret(selectedSlot, secretInput)
                        if (res.isSuccess) {
                            secretInput = ""
                            keyReport = keystoreManager.inspectKeySecurityLevel()
                            refreshTick++
                            statusMessage = "Encrypted & saved ${selectedSlot.displayName}."
                        } else {
                            statusMessage = "Validation error: ${res.exceptionOrNull()?.message}"
                        }
                    }) {
                        Text("Encrypt & Save Key")
                    }

                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = JarvisRedAlert),
                        onClick = {
                            secretStore.clearAllSecrets()
                            keyReport = keystoreManager.inspectKeySecurityLevel()
                            refreshTick++
                            statusMessage = "Cleared stored API secrets only (App settings preserved)."
                        }
                    ) {
                        Text("Clear Stored Secrets Only")
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(statusMessage, style = MaterialTheme.typography.labelSmall, color = JarvisCyanPrimary)
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = JarvisSurfaceCard),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Reduced Motion (அசைவு குறைப்பு)", style = MaterialTheme.typography.titleMedium)
                        Text("Disables continuous Orb pulsing animation on the Home screen.", style = MaterialTheme.typography.bodyMedium)
                    }
                    Switch(
                        checked = settingsState.reducedMotion,
                        onCheckedChange = { settingsRepository.setReducedMotion(it) }
                    )
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = JarvisSurfaceCard),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("COMPLETE APPLICATION DATA RESET", style = MaterialTheme.typography.titleMedium, color = JarvisRedAlert)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Separate from 'Clear Stored Secrets Only': Deletes all encrypted secrets, deletes the Keystore master key, resets all user preferences, clears audit logs, and deletes local databases/files.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = JarvisRedAlert),
                    onClick = {
                        val ok = appDataResetManager.performFullApplicationDataReset()
                        keyReport = keystoreManager.inspectKeySecurityLevel()
                        refreshTick++
                        statusMessage = if (ok) "Full application data reset completed." else "Reset encountered an error."
                    }
                ) {
                    Text("Reset All Application Data")
                }
            }
        }
    }
}

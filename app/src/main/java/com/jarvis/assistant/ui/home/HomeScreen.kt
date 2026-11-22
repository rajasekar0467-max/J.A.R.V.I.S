package com.jarvis.assistant.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jarvis.assistant.ui.components.JarvisOrb
import com.jarvis.assistant.voice.VoiceState
import com.jarvis.assistant.ui.theme.JarvisAmberWarning
import com.jarvis.assistant.ui.theme.JarvisCyanPrimary
import com.jarvis.assistant.ui.theme.JarvisSurfaceCard

@Composable
fun HomeScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = JarvisSurfaceCard),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "MIC STATE: OFF (No Audio Service Active)",
                    style = MaterialTheme.typography.labelSmall,
                    color = JarvisCyanPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "மைக் நிலை: ஆஃப் • ஒலிப்பதிவு அல்லது விழிப்புச் சொல் (Wake Word) நிலை 0-ல் இயக்கப்படவில்லை.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        JarvisOrb(state = VoiceState.IDLE)
        Spacer(modifier = Modifier.height(24.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = JarvisSurfaceCard),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "PHASE 0 FOUNDATIONS — HONEST STATUS",
                    style = MaterialTheme.typography.titleMedium,
                    color = JarvisAmberWarning
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• Voice ASR & TTS Spike: NOT IMPLEMENTED (Scheduled for Phase 1)\n" +
                        "• Multi-Provider AI Brain (Gemini/Fallback): NOT IMPLEMENTED (Scheduled for Phase 2)\n" +
                        "• Live Voice & Barge-In State Machine: NOT IMPLEMENTED (Scheduled for Phase 3)\n" +
                        "• 'Hey JARVIS' Wake Word Service: NOT IMPLEMENTED (Scheduled for Phase 7)\n\n" +
                        "Active in Phase 0: AndroidKeyStore AES-256-GCM SecretStore Vault (Settings tab) and Live Runtime Permission & Special Access Inspector (Permissions tab).",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

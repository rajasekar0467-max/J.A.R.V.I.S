package com.jarvis.assistant.ui.automation

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jarvis.assistant.ui.theme.JarvisAmberWarning
import com.jarvis.assistant.ui.theme.JarvisSurfaceCard

@Composable
fun AutomationScreen() {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Card(
            colors = CardDefaults.cardColors(containerColor = JarvisSurfaceCard),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "ANDROID AUTOMATION & TOOLS — NOT IMPLEMENTED IN PHASE 0",
                    style = MaterialTheme.typography.titleMedium,
                    color = JarvisAmberWarning
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "ஆண்ட்ராய்டு தானியங்கி கருவிகள் நிலை 5 மற்றும் நிலை 6-ல் இணைக்கப்படும்.\n\n" +
                        "Core ToolResult truthfulness contracts (Success, HandedOff, NeedsUserAction, Unsupported, Failed) are compiled and unit-tested in Phase 0. Live Android intents (Alarms, Calendar, App Launch, Bluetooth/Wi-Fi Settings Panels, WhatsApp wa.me Handoff) connect in Phases 5 and 6.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

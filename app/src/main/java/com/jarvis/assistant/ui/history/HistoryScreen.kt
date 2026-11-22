package com.jarvis.assistant.ui.history

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
fun HistoryScreen() {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Card(
            colors = CardDefaults.cardColors(containerColor = JarvisSurfaceCard),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "CONVERSATION HISTORY — NOT IMPLEMENTED IN PHASE 0",
                    style = MaterialTheme.typography.titleMedium,
                    color = JarvisAmberWarning
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "உரையாடல் வரலாறு (Conversation History) நிலை 2 மற்றும் நிலை 4-ல் இணைக்கப்படும்.\n\n" +
                        "No mock or simulated conversation turns are shown here. Real bilingual (Tamil, English, Tanglish) transcripts and rolling context window summaries will populate once Phase 2 (AI Brain) and Phase 4 (Room/SQLite Memory Engine) are connected.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

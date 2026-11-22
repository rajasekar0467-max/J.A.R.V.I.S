package com.jarvis.assistant.ui.memory

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
fun MemoryScreen() {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Card(
            colors = CardDefaults.cardColors(containerColor = JarvisSurfaceCard),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "LONG-TERM MEMORY ENGINE — NOT IMPLEMENTED IN PHASE 0",
                    style = MaterialTheme.typography.titleMedium,
                    color = JarvisAmberWarning
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "நீண்ட கால நினைவகம் (Long-Term Memory) நிலை 4-ல் செயல்படுத்தப்படும்.\n\n" +
                        "Planned Phase 4 Schema (6 tables: memory, memory_embedding, project, conversation, message, import_job across 10 categories) and user-reviewed JSON/TXT/Markdown import pipeline are not yet connected in Phase 0.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jarvis.assistant.ui.AssistantViewModel
import com.jarvis.assistant.ui.components.GlassCard
import com.jarvis.assistant.ui.components.JarvisOrb
import com.jarvis.assistant.ui.theme.EmeraldGlow
import com.jarvis.assistant.ui.theme.EmeraldPrimary
import com.jarvis.assistant.ui.theme.TextPrimary
import com.jarvis.assistant.ui.theme.TextSecondary
import com.jarvis.assistant.ui.theme.VoidBlack
import com.jarvis.assistant.ui.theme.WarningAmber

@Composable
fun HomeScreen(
    viewModel: AssistantViewModel,
    onNavigateToChat: () -> Unit,
    onNavigateToVoice: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToDiagnostics: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val voice by viewModel.voiceSnapshot.collectAsState()
    val recent by viewModel.recentConversations.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        JarvisOrb(
            state = voice.state,
            rmsDb = voice.rmsDb,
            reducedMotion = settings.reducedMotion,
            size = 168.dp
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Ready when you are",
            style = MaterialTheme.typography.headlineSmall,
            color = TextPrimary
        )
        Text(
            text = "FRIDAY-inspired voice • ${settings.language.label}",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (!uiState.hasApiKey) {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = WarningAmber.copy(alpha = 0.65f),
                onClick = onNavigateToSettings
            ) {
                Text(
                    text = "Connect an AI API Key",
                    style = MaterialTheme.typography.titleMedium,
                    color = WarningAmber
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Local device commands work offline right now. Add a Gemini, OpenAI, Groq, or OpenRouter key in Settings to unlock live AI replies.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onNavigateToVoice,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = VoidBlack)
            ) {
                Text("Start Voice")
            }
            OutlinedButton(
                onClick = {
                    viewModel.startNewConversation()
                    onNavigateToChat()
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("New Chat", color = EmeraldGlow)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            GlassCard(
                modifier = Modifier.weight(1f),
                onClick = {
                    viewModel.startNewConversation()
                    viewModel.sendMessage("What time is it?")
                    onNavigateToChat()
                }
            ) {
                Text("Quick Check", style = MaterialTheme.typography.labelMedium, color = EmeraldGlow)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Time & Reminders", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            }
            GlassCard(
                modifier = Modifier.weight(1f),
                onClick = onNavigateToDiagnostics
            ) {
                Text("System Health", style = MaterialTheme.typography.labelMedium, color = EmeraldGlow)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Diagnostics", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        if (recent.isNotEmpty()) {
            Text(
                text = "Recent Conversations",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            recent.take(4).forEach { conv ->
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    contentPadding = 14.dp,
                    onClick = {
                        viewModel.openConversation(conv.id)
                        onNavigateToChat()
                    }
                ) {
                    Text(
                        text = conv.title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

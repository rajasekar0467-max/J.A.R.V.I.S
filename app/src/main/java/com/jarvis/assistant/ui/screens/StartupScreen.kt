package com.jarvis.assistant.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jarvis.assistant.ui.components.JarvisOrb
import com.jarvis.assistant.ui.theme.EmeraldGlow
import com.jarvis.assistant.ui.theme.TextPrimary
import com.jarvis.assistant.ui.theme.TextSecondary
import com.jarvis.assistant.ui.theme.VoidBlack
import com.jarvis.assistant.voice.VoiceState

@Composable
fun StartupScreen(
    statusText: String = "Initializing JARVIS core...",
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VoidBlack)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        JarvisOrb(state = VoiceState.PROCESSING, size = 164.dp)
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "JARVIS",
            style = MaterialTheme.typography.headlineLarge,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = statusText,
            style = MaterialTheme.typography.bodyMedium,
            color = EmeraldGlow
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Native Android Assistant • Emerald Core",
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary
        )
    }
}

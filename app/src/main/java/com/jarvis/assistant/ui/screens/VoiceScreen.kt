package com.jarvis.assistant.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jarvis.assistant.language.LanguageMode
import com.jarvis.assistant.ui.AssistantViewModel
import com.jarvis.assistant.ui.components.GlassCard
import com.jarvis.assistant.ui.components.JarvisOrb
import com.jarvis.assistant.ui.components.MicButton
import com.jarvis.assistant.ui.theme.EmeraldGlow
import com.jarvis.assistant.ui.theme.ErrorRed
import com.jarvis.assistant.ui.theme.TextPrimary
import com.jarvis.assistant.ui.theme.TextSecondary
import com.jarvis.assistant.voice.FridayVoiceProfile
import com.jarvis.assistant.voice.MicPermissionState
import com.jarvis.assistant.voice.VoiceState

@Composable
fun VoiceScreen(
    viewModel: AssistantViewModel,
    micPermissionState: MicPermissionState,
    onRequestMicPermission: () -> Unit,
    onOpenAppSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val voice by viewModel.voiceSnapshot.collectAsState()
    val settings by viewModel.settings.collectAsState()

    val isActive = voice.state == VoiceState.LISTENING ||
        voice.state == VoiceState.SPEAKING ||
        voice.state == VoiceState.PROCESSING

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = FridayVoiceProfile.PROFILE_NAME,
                style = MaterialTheme.typography.labelMedium,
                color = EmeraldGlow
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LanguageMode.entries.forEach { mode ->
                    FilterChip(
                        selected = settings.language == mode,
                        onClick = { viewModel.setLanguageMode(mode) },
                        label = {
                            Text(
                                when (mode) {
                                    LanguageMode.EN_IN -> "English (IN)"
                                    LanguageMode.TA_IN -> "தமிழ்"
                                    LanguageMode.TANGLISH -> "Tanglish"
                                }
                            )
                        }
                    )
                }
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            JarvisOrb(
                state = voice.state,
                rmsDb = voice.rmsDb,
                reducedMotion = settings.reducedMotion,
                size = 216.dp
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = when (voice.state) {
                    VoiceState.IDLE -> "Tap the mic to speak"
                    VoiceState.LISTENING -> "Listening..."
                    VoiceState.PROCESSING -> "Thinking..."
                    VoiceState.SPEAKING -> "Speaking..."
                    VoiceState.ERROR -> voice.errorMessage ?: "Voice error"
                },
                style = MaterialTheme.typography.titleMedium,
                color = if (voice.state == VoiceState.ERROR) ErrorRed else TextPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(10.dp))
            if (voice.partialTranscript.isNotBlank()) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = voice.partialTranscript,
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextPrimary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            if (micPermissionState == MicPermissionState.DENIED_PERMANENT) {
                Text(
                    text = "Microphone permission is blocked in Android settings.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(onClick = onOpenAppSettings) {
                    Text("Open App Settings", color = EmeraldGlow)
                }
            } else {
                MicButton(
                    isActive = isActive,
                    onClick = {
                        when {
                            micPermissionState != MicPermissionState.GRANTED -> onRequestMicPermission()
                            voice.state == VoiceState.LISTENING -> viewModel.stopVoiceListening()
                            voice.state == VoiceState.SPEAKING -> viewModel.stopSpeaking()
                            voice.state == VoiceState.PROCESSING -> viewModel.cancelVoice()
                            else -> viewModel.startVoiceListening()
                        }
                    }
                )
            }
        }
    }
}

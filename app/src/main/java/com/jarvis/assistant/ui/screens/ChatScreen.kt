package com.jarvis.assistant.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.jarvis.assistant.conversation.Message
import com.jarvis.assistant.ui.AssistantViewModel
import com.jarvis.assistant.ui.components.GlassCard
import com.jarvis.assistant.ui.components.MicGlyph
import com.jarvis.assistant.ui.components.StopGlyph
import com.jarvis.assistant.ui.components.VolumeGlyph
import com.jarvis.assistant.ui.theme.CardEmerald
import com.jarvis.assistant.ui.theme.EmeraldGlow
import com.jarvis.assistant.ui.theme.EmeraldPrimary
import com.jarvis.assistant.ui.theme.ErrorRed
import com.jarvis.assistant.ui.theme.TextPrimary
import com.jarvis.assistant.ui.theme.TextSecondary

@Composable
fun ChatScreen(
    viewModel: AssistantViewModel,
    onNavigateToVoice: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val messages by viewModel.messages.collectAsState()
    var draft by rememberSaveable { mutableStateOf("") }
    var copiedNotice by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val clipboard = LocalClipboardManager.current

    LaunchedEffect(messages.size, uiState.streamingDraft.length) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (copiedNotice) "Copied to clipboard"
                else if (uiState.activeConversationId == null) "New Conversation"
                else "Active Thread",
                style = MaterialTheme.typography.labelMedium,
                color = if (copiedNotice) EmeraldGlow else TextSecondary
            )
            TextButton(onClick = {
                copiedNotice = false
                viewModel.startNewConversation()
            }) {
                Icon(Icons.Filled.Add, contentDescription = "New Chat", tint = EmeraldGlow)
                Spacer(modifier = Modifier.width(4.dp))
                Text("New", color = EmeraldGlow)
            }
        }

        uiState.statusBanner?.let { banner ->
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                borderColor = ErrorRed.copy(alpha = 0.6f),
                contentPadding = 10.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = banner,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { viewModel.dismissBanner() }) {
                        Text("Dismiss", color = EmeraldGlow)
                    }
                }
            }
        }

        if (messages.isEmpty() && !uiState.isStreaming) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Ask JARVIS anything",
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Supports English (India), Tamil (தமிழ்), Tanglish, and local device commands.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    val displayContent = if (
                        uiState.isStreaming &&
                        msg.role == "assistant" &&
                        msg.content.isEmpty() &&
                        uiState.streamingDraft.isNotEmpty()
                    ) {
                        uiState.streamingDraft
                    } else {
                        msg.content
                    }
                    MessageBubble(
                        message = msg.copy(content = displayContent),
                        onSpeak = { viewModel.speakMessage(displayContent) },
                        onCopy = {
                            clipboard.setText(AnnotatedString(displayContent))
                            copiedNotice = true
                        }
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateToVoice) {
                MicGlyph(color = EmeraldGlow, size = 22.dp)
            }
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Message JARVIS...", color = TextSecondary) },
                singleLine = false,
                maxLines = 4,
                shape = RoundedCornerShape(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            if (uiState.isStreaming) {
                IconButton(onClick = { viewModel.stopGeneration() }) {
                    StopGlyph(color = ErrorRed, size = 22.dp)
                }
            } else {
                IconButton(
                    onClick = {
                        copiedNotice = false
                        val toSend = draft
                        draft = ""
                        viewModel.sendMessage(toSend)
                    },
                    enabled = draft.isNotBlank()
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send message",
                        tint = if (draft.isNotBlank()) EmeraldPrimary else TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(
    message: Message,
    onSpeak: () -> Unit,
    onCopy: () -> Unit
) {
    val isUser = message.role == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = if (isUser) EmeraldPrimary.copy(alpha = 0.22f) else CardEmerald.copy(alpha = 0.85f),
            modifier = Modifier.fillMaxWidth(0.86f)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isUser) "You" else if (message.source == "local") "JARVIS • Local" else "JARVIS",
                        style = MaterialTheme.typography.labelMedium,
                        color = EmeraldGlow
                    )
                    if (!isUser && message.content.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = onCopy, modifier = Modifier.height(28.dp)) {
                                Text("Copy", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                            }
                            IconButton(onClick = onSpeak, modifier = Modifier.height(28.dp)) {
                                VolumeGlyph(color = TextSecondary, size = 18.dp)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = message.content.ifBlank { "..." },
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary
                )
            }
        }
    }
}

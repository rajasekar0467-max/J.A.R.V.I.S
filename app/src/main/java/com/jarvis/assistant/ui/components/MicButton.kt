package com.jarvis.assistant.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jarvis.assistant.ui.theme.EmeraldGlow
import com.jarvis.assistant.ui.theme.EmeraldPrimary
import com.jarvis.assistant.ui.theme.ErrorRed
import com.jarvis.assistant.ui.theme.VoidBlack

@Composable
fun MicButton(
    isActive: Boolean,
    onClick: () -> Unit,
    size: Dp = 72.dp,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .size(size)
            .semantics { contentDescription = if (isActive) "Stop voice" else "Start voice" },
        shape = CircleShape,
        color = if (isActive) ErrorRed else EmeraldPrimary,
        border = BorderStroke(2.dp, if (isActive) ErrorRed.copy(alpha = 0.6f) else EmeraldGlow),
        shadowElevation = 8.dp
    ) {
        if (isActive) {
            StopGlyph(color = VoidBlack, size = size * 0.42f, modifier = Modifier.size(size))
        } else {
            MicGlyph(color = VoidBlack, size = size * 0.44f, modifier = Modifier.size(size))
        }
    }
}

package com.jarvis.assistant.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jarvis.assistant.core.NetworkState
import com.jarvis.assistant.language.LanguageMode
import com.jarvis.assistant.ui.theme.EmeraldGlow
import com.jarvis.assistant.ui.theme.EmeraldPrimary
import com.jarvis.assistant.ui.theme.SurfaceEmerald
import com.jarvis.assistant.ui.theme.TextPrimary
import com.jarvis.assistant.ui.theme.TextSecondary
import com.jarvis.assistant.ui.theme.WarningAmber

@Composable
fun JarvisHeader(
    title: String,
    networkState: NetworkState,
    languageMode: LanguageMode,
    onOpenDiagnostics: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (networkState.connected) EmeraldPrimary else WarningAmber)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = SurfaceEmerald
            ) {
                Text(
                    text = when (languageMode) {
                        LanguageMode.EN_IN -> "EN-IN"
                        LanguageMode.TA_IN -> "தமிழ்"
                        LanguageMode.TANGLISH -> "Tanglish"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = EmeraldGlow,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            IconButton(onClick = onOpenDiagnostics) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = "Open Diagnostics",
                    tint = TextSecondary
                )
            }
        }
    }
}

package com.jarvis.assistant.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val JarvisColorScheme = darkColorScheme(
    primary = EmeraldPrimary,
    onPrimary = VoidBlack,
    secondary = EmeraldGlow,
    onSecondary = VoidBlack,
    tertiary = SoftNeonEmerald,
    background = VoidBlack,
    onBackground = TextPrimary,
    surface = DeepEmerald,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceEmerald,
    onSurfaceVariant = TextSecondary,
    outline = GlassBorder,
    error = ErrorRed,
    onError = TextPrimary
)

@Composable
fun JarvisTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = JarvisColorScheme,
        typography = JarvisTypography,
        content = content
    )
}

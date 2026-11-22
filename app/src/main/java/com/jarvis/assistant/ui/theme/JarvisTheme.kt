package com.jarvis.assistant.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val JarvisDeepBlack = Color(0xFF050907)
val JarvisDarkEmerald = Color(0xFF09140F)
val JarvisSurfaceEmerald = Color(0xFF10251A)
val JarvisEmeraldPrimary = Color(0xFF00C878)
val JarvisNeonGreen = Color(0xFF00F59B)
val JarvisTextPrimary = Color(0xFFE8F5EE)
val JarvisTextMuted = Color(0xFF8FA89B)
val JarvisAmberWarning = Color(0xFFF59E0B)
val JarvisRedAlert = Color(0xFFEF4444)

val LocalReducedMotion = staticCompositionLocalOf { false }

private val JarvisEmeraldColorScheme = darkColorScheme(
    primary = JarvisEmeraldPrimary,
    secondary = JarvisNeonGreen,
    tertiary = JarvisAmberWarning,
    background = JarvisDeepBlack,
    surface = JarvisDarkEmerald,
    surfaceVariant = JarvisSurfaceEmerald,
    onPrimary = JarvisDeepBlack,
    onBackground = JarvisTextPrimary,
    onSurface = JarvisTextPrimary
)

val JarvisTypography = Typography(
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp
    )
)

@Composable
fun JarvisTheme(
    reducedMotion: Boolean = false,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalReducedMotion provides reducedMotion) {
        MaterialTheme(
            colorScheme = JarvisEmeraldColorScheme,
            typography = JarvisTypography,
            content = content
        )
    }
}

package com.jarvis.assistant.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jarvis.assistant.ui.theme.DeepEmerald
import com.jarvis.assistant.ui.theme.EmeraldGlow
import com.jarvis.assistant.ui.theme.EmeraldPrimary
import com.jarvis.assistant.ui.theme.ErrorRed
import com.jarvis.assistant.ui.theme.SoftNeonEmerald
import com.jarvis.assistant.voice.VoiceState
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun JarvisOrb(
    state: VoiceState,
    rmsDb: Float = 0f,
    reducedMotion: Boolean = false,
    size: Dp = 180.dp,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "jarvis_orb")
    val orbitAngle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    VoiceState.PROCESSING -> 1400
                    VoiceState.LISTENING -> 2400
                    VoiceState.SPEAKING -> 2000
                    else -> 6000
                },
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbit"
    )
    val breath by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath"
    )

    val activeColor = when (state) {
        VoiceState.ERROR -> ErrorRed
        VoiceState.LISTENING -> SoftNeonEmerald
        VoiceState.SPEAKING -> EmeraldGlow
        VoiceState.PROCESSING -> EmeraldPrimary
        VoiceState.IDLE -> EmeraldPrimary
    }

    val rmsFactor = if (state == VoiceState.LISTENING) {
        ((rmsDb + 2f) / 14f).coerceIn(0f, 1f) * 0.14f
    } else {
        0f
    }
    val scale = if (reducedMotion) 1f else (breath + rmsFactor).coerceIn(0.88f, 1.18f)
    val angle = if (reducedMotion) 35f else orbitAngle

    Canvas(modifier = modifier.size(size)) {
        val center = Offset(this.size.width / 2f, this.size.height / 2f)
        val baseRadius = this.size.minDimension * 0.32f * scale

        // Outer ambient emerald halo
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    activeColor.copy(alpha = 0.32f),
                    activeColor.copy(alpha = 0.10f),
                    Color.Transparent
                ),
                center = center,
                radius = this.size.minDimension * 0.50f
            ),
            radius = this.size.minDimension * 0.50f,
            center = center
        )

        // Outer glass ring
        val outerRingR = this.size.minDimension * 0.42f
        drawCircle(
            color = activeColor.copy(alpha = 0.25f),
            radius = outerRingR,
            center = center,
            style = Stroke(width = 2.dp.toPx())
        )

        // Animated orbital arc
        drawArc(
            color = activeColor.copy(alpha = 0.90f),
            startAngle = angle,
            sweepAngle = if (state == VoiceState.PROCESSING) 130f else 85f,
            useCenter = false,
            topLeft = Offset(center.x - outerRingR, center.y - outerRingR),
            size = Size(outerRingR * 2f, outerRingR * 2f),
            style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
        )

        // Counter-rotating inner orbital arc
        val midRingR = this.size.minDimension * 0.36f
        drawArc(
            color = EmeraldGlow.copy(alpha = 0.55f),
            startAngle = -angle * 1.2f,
            sweepAngle = 70f,
            useCenter = false,
            topLeft = Offset(center.x - midRingR, center.y - midRingR),
            size = Size(midRingR * 2f, midRingR * 2f),
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )

        // Core dark emerald sphere
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    activeColor.copy(alpha = 0.85f),
                    DeepEmerald.copy(alpha = 0.95f),
                    Color(0xFF040A07)
                ),
                center = Offset(center.x - baseRadius * 0.22f, center.y - baseRadius * 0.22f),
                radius = baseRadius * 1.3f
            ),
            radius = baseRadius,
            center = center
        )

        // Orbiting satellite node
        val rad = Math.toRadians(angle.toDouble())
        val nodeOffset = Offset(
            x = center.x + (outerRingR * cos(rad)).toFloat(),
            y = center.y + (outerRingR * sin(rad)).toFloat()
        )
        drawCircle(
            color = SoftNeonEmerald,
            radius = 4.dp.toPx(),
            center = nodeOffset
        )
    }
}

package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.PinkTertiary
import com.example.ui.theme.VioletSecondary
import kotlin.math.PI
import kotlin.math.sin

/**
 * Real-time continuous fluid audio waveform that reacts directly to microphone RMS input.
 * Displays moving multi-frequency sine wave ribbons and dynamic equalizer bars with glowing cyan-violet gradients.
 */
@Composable
fun RealTimeWaveformIndicator(
    rmsLevel: Float,
    isRecording: Boolean,
    modifier: Modifier = Modifier,
    primaryColor: Color = CyanPrimary,
    secondaryColor: Color = VioletSecondary,
    accentColor: Color = PinkTertiary
) {
    val smoothedRms = remember { Animatable(0f) }

    LaunchedEffect(rmsLevel, isRecording) {
        val target = if (isRecording) rmsLevel.coerceIn(0.08f, 1f) else 0f
        smoothedRms.animateTo(
            targetValue = target,
            animationSpec = tween(durationMillis = 80, easing = LinearEasing)
        )
    }

    val infiniteTransition = rememberInfiniteTransition(label = "waveform_phase")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .testTag("realtime_waveform_canvas")
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val centerY = canvasHeight / 2f
        val currentRms = smoothedRms.value

        if (!isRecording || currentRms <= 0.01f) {
            // Idle subtle glowing baseline
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        primaryColor.copy(alpha = 0.35f),
                        primaryColor.copy(alpha = 0.8f),
                        secondaryColor.copy(alpha = 0.8f),
                        primaryColor.copy(alpha = 0.35f),
                        Color.Transparent
                    )
                ),
                start = Offset(0f, centerY),
                end = Offset(canvasWidth, centerY),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
            return@Canvas
        }

        // Draw dynamic reactive equalizer bars across the center
        val barCount = 36
        val barSpacing = canvasWidth / barCount
        val barWidth = (barSpacing * 0.45f).coerceIn(2.dp.toPx(), 6.dp.toPx())

        for (i in 0 until barCount) {
            val normalizedX = i.toFloat() / barCount
            // Bell curve window so ends taper gracefully
            val window = sin(normalizedX * PI).toFloat()
            // Harmonic wave modulation
            val waveMod = (sin(normalizedX * 4 * PI + phase) * 0.35f + 0.65f).toFloat()
            val reactiveHeight = (canvasHeight * 0.88f * currentRms * window * waveMod)
                .coerceAtLeast(3.dp.toPx())

            val barTop = centerY - reactiveHeight / 2f
            val barX = i * barSpacing + (barSpacing - barWidth) / 2f

            val barColor = when {
                i % 3 == 0 -> primaryColor
                i % 3 == 1 -> secondaryColor
                else -> accentColor
            }

            drawRoundRect(
                color = barColor.copy(alpha = (0.45f + 0.55f * window * currentRms).coerceIn(0.2f, 1f)),
                topLeft = Offset(barX, barTop),
                size = Size(barWidth, reactiveHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }

        // Overlay primary smooth sine wave path
        val primaryPath = Path()
        val secondaryPath = Path()
        val step = 4

        for (x in 0..canvasWidth.toInt() step step) {
            val xFloat = x.toFloat()
            val normX = xFloat / canvasWidth
            val window = sin(normX * PI).toFloat()

            // Wave 1
            val amp1 = (canvasHeight * 0.42f) * currentRms * window
            val y1 = centerY + amp1 * sin((normX * 3.5 * PI + phase)).toFloat()

            // Wave 2
            val amp2 = (canvasHeight * 0.32f) * currentRms * window
            val y2 = centerY + amp2 * sin((normX * 5.0 * PI - phase * 1.2)).toFloat()

            if (x == 0) {
                primaryPath.moveTo(xFloat, y1)
                secondaryPath.moveTo(xFloat, y2)
            } else {
                primaryPath.lineTo(xFloat, y1)
                secondaryPath.lineTo(xFloat, y2)
            }
        }

        // Draw secondary harmonic line
        drawPath(
            path = secondaryPath,
            color = secondaryColor.copy(alpha = 0.8f),
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )

        // Draw primary harmonic line
        drawPath(
            path = primaryPath,
            brush = Brush.horizontalGradient(
                colors = listOf(primaryColor, secondaryColor, accentColor, primaryColor)
            ),
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

/**
 * Microphone pulsing recording badge with animated ripple waves that expands when voice is detected.
 */
@Composable
fun MicPulseWaveFeedback(
    isRecording: Boolean,
    rmsLevel: Float,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
    val rippleScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "mic_ripple"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        if (isRecording) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(this.size.width / 2f, this.size.height / 2f)
                val baseRadius = this.size.minDimension / 2f
                val dynamicRadius = baseRadius * (1f + rmsLevel * 0.45f)

                // Outer pulsing audio wave ring
                val alpha = (1f - (rippleScale - 1f) / 0.6f).coerceIn(0f, 0.6f)
                drawCircle(
                    color = CyanPrimary.copy(alpha = alpha),
                    radius = dynamicRadius * rippleScale,
                    center = center
                )

                // Inner reactive audio glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(CyanPrimary.copy(alpha = 0.5f), Color.Transparent),
                        center = center,
                        radius = dynamicRadius * 1.2f
                    ),
                    radius = dynamicRadius * 1.2f,
                    center = center
                )
            }
        }

        // Center mic badge
        Box(
            modifier = Modifier
                .size(size * 0.85f)
                .clip(CircleShape)
                .background(if (isRecording) CyanPrimary else Color(0xFF1E293B)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = if (isRecording) "Recording microphone" else "Microphone ready",
                tint = if (isRecording) Color(0xFF00363F) else CyanPrimary,
                modifier = Modifier.size(size * 0.5f)
            )
        }
    }
}

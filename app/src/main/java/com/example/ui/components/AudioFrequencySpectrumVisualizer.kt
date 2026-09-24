package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.PinkTertiary
import com.example.ui.theme.VioletSecondary
import kotlin.math.PI
import kotlin.math.sin

/**
 * High-fidelity Audio Frequency Bar & Waveform Spectrum animation that responds dynamically to microphone input.
 * Provides multi-frequency visualizer bands and smooth reactive wave ripples with glowing neon gradients.
 */
@Composable
fun AudioFrequencySpectrumVisualizer(
    rmsLevel: Float,
    isRecording: Boolean,
    modifier: Modifier = Modifier,
    barCount: Int = 28,
    primaryColor: Color = CyanPrimary,
    secondaryColor: Color = VioletSecondary,
    accentColor: Color = PinkTertiary,
    height: Dp = 56.dp
) {
    val smoothedRms = remember { Animatable(0f) }

    LaunchedEffect(rmsLevel, isRecording) {
        val target = if (isRecording) rmsLevel.coerceIn(0.05f, 1f) else 0f
        smoothedRms.animateTo(
            targetValue = target,
            animationSpec = tween(durationMillis = 60, easing = LinearEasing)
        )
    }

    val infiniteTransition = rememberInfiniteTransition(label = "spectrum_phase")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase_anim"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF04101A))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(height - 8.dp)
                .testTag("frequency_spectrum_canvas")
        ) {
            val width = size.width
            val canvasHeight = size.height
            val centerY = canvasHeight / 2f
            val currentRms = smoothedRms.value

            if (!isRecording) {
                // Subtle glowing baseline when idle
                drawLine(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            primaryColor.copy(alpha = 0.2f),
                            primaryColor.copy(alpha = 0.5f),
                            secondaryColor.copy(alpha = 0.5f),
                            primaryColor.copy(alpha = 0.2f),
                            Color.Transparent
                        )
                    ),
                    start = Offset(0f, centerY),
                    end = Offset(width, centerY),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
                return@Canvas
            }

            // Draw Frequency Bands (reactive equalizer bars)
            val slotWidth = width / barCount
            val barWidth = (slotWidth * 0.55f).coerceIn(3.dp.toPx(), 8.dp.toPx())

            for (i in 0 until barCount) {
                val norm = i.toFloat() / (barCount - 1)
                // Windowing envelope (peaks higher around voice frequencies ~ 200Hz - 3kHz in middle)
                val window = (sin(norm * PI).toFloat() * 0.8f + 0.2f)

                // Frequency harmonic modulation
                val harmonic = (sin(norm * 5 * PI + phase) * 0.4f +
                        sin(norm * 9 * PI - phase * 1.3f) * 0.25f + 0.75f).toFloat()

                val barH = (canvasHeight * 0.92f * currentRms * window * harmonic)
                    .coerceIn(4.dp.toPx(), canvasHeight * 0.95f)

                val x = i * slotWidth + (slotWidth - barWidth) / 2f
                val y = centerY - barH / 2f

                // Gradient color for each frequency band
                val barGradient = Brush.verticalGradient(
                    colors = listOf(
                        primaryColor,
                        secondaryColor,
                        accentColor
                    ),
                    startY = y,
                    endY = y + barH
                )

                drawRoundRect(
                    brush = barGradient,
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barH),
                    cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
                    alpha = (0.5f + 0.5f * currentRms).coerceIn(0.3f, 1f)
                )
            }

            // Draw top reactive waveform ribbon
            val wavePath = Path()
            val step = 4
            for (px in 0..width.toInt() step step) {
                val x = px.toFloat()
                val normX = x / width
                val envelope = sin(normX * PI).toFloat()
                val waveY = centerY + (canvasHeight * 0.35f * currentRms * envelope *
                        sin(normX * 4 * PI + phase)).toFloat()

                if (px == 0) {
                    wavePath.moveTo(x, waveY)
                } else {
                    wavePath.lineTo(x, waveY)
                }
            }

            drawPath(
                path = wavePath,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.6f),
                        secondaryColor,
                        accentColor,
                        primaryColor.copy(alpha = 0.6f)
                    )
                ),
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
            )
        }
    }
}

package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.PinkTertiary
import com.example.ui.theme.VioletSecondary
import com.example.ui.theme.VoiceErrorGlow
import com.example.ui.theme.VoiceListeningGlow
import com.example.ui.theme.VoiceSpeakingGlow
import com.example.ui.theme.VoiceThinkingGlow
import com.example.voice.VoiceState
import kotlin.math.cos
import kotlin.math.sin

/**
 * High-tech holographic voice orb that animates dynamically according to VoiceState and audio volume.
 */
@Composable
fun LiveVoiceOrb(
    voiceState: VoiceState,
    rmsLevel: Float,
    modifier: Modifier = Modifier,
    size: Dp = 180.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "voice_orb_transition")

    // Breathing pulse
    val breathingPulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb_pulse"
    )

    // Continuous rotation for thinking/active state
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orb_rotation"
    )

    // Ripple expansion
    val rippleScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple_scale"
    )

    val (primaryColor, secondaryColor, accentColor) = when (voiceState) {
        VoiceState.LISTENING -> Triple(VoiceListeningGlow, CyanPrimary, Color(0xFF38BDF8))
        VoiceState.SPEAKING -> Triple(VoiceSpeakingGlow, VioletSecondary, PinkTertiary)
        VoiceState.THINKING -> Triple(VoiceThinkingGlow, Color(0xFFFBBF24), Color(0xFFEA580C))
        VoiceState.ERROR -> Triple(VoiceErrorGlow, Color(0xFFDC2626), Color(0xFFB91C1C))
        VoiceState.IDLE -> Triple(CyanPrimary, VioletSecondary, Color(0xFF6366F1))
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val baseRadius = (this.size.minDimension / 2f) * 0.55f

            // Dynamic scale factor based on audio RMS when listening, or breathing when idle/speaking
            val dynamicScale = when (voiceState) {
                VoiceState.LISTENING -> 0.9f + (rmsLevel * 0.5f)
                VoiceState.SPEAKING -> breathingPulse * 1.08f
                VoiceState.THINKING -> breathingPulse * 0.95f
                else -> breathingPulse * 0.92f
            }

            val currentRadius = baseRadius * dynamicScale

            // Outer radiating aura rings
            val rippleAlpha = when (voiceState) {
                VoiceState.LISTENING -> (1f - (rippleScale - 0.8f) / 0.7f).coerceIn(0f, 0.7f)
                VoiceState.SPEAKING -> (1f - (rippleScale - 0.8f) / 0.7f).coerceIn(0f, 0.4f)
                else -> 0.15f
            }

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(primaryColor.copy(alpha = rippleAlpha), Color.Transparent),
                    center = center,
                    radius = currentRadius * rippleScale * 1.3f
                ),
                radius = currentRadius * rippleScale * 1.3f,
                center = center
            )

            // Outer thin technological orbital ring
            drawCircle(
                color = primaryColor.copy(alpha = 0.5f),
                radius = currentRadius * 1.25f,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )

            // Orbiting holographic nodes if thinking or listening
            if (voiceState == VoiceState.THINKING || voiceState == VoiceState.LISTENING) {
                val numNodes = 4
                for (i in 0 until numNodes) {
                    val angleRad = Math.toRadians((rotationAngle + (i * 360f / numNodes)).toDouble())
                    val nodeX = center.x + (currentRadius * 1.25f * cos(angleRad)).toFloat()
                    val nodeY = center.y + (currentRadius * 1.25f * sin(angleRad)).toFloat()
                    drawCircle(
                        color = accentColor,
                        radius = 4.dp.toPx(),
                        center = Offset(nodeX, nodeY)
                    )
                }
            }

            // Core glowing sphere with deep gradient
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White,
                        primaryColor,
                        secondaryColor,
                        Color(0xFF0F172A)
                    ),
                    center = Offset(center.x - currentRadius * 0.2f, center.y - currentRadius * 0.2f),
                    radius = currentRadius * 1.1f
                ),
                radius = currentRadius,
                center = center
            )

            // Inner high-luminance core
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.85f),
                        primaryColor.copy(alpha = 0.4f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = currentRadius * 0.55f
                ),
                radius = currentRadius * 0.55f,
                center = center
            )
        }
    }
}

/**
 * Sound wave bars equalizer for live audio indication.
 */
@Composable
fun SoundWaveVisualizer(
    isActive: Boolean,
    color: Color = CyanPrimary,
    barCount: Int = 5,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "soundwave_bars")

    val h1 by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(350, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "h1"
    )
    val h2 by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(280, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "h2"
    )
    val h3 by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0.85f,
        animationSpec = infiniteRepeatable(tween(420, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "h3"
    )
    val h4 by infiniteTransition.animateFloat(
        initialValue = 0.5f, targetValue = 0.95f,
        animationSpec = infiniteRepeatable(tween(310, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "h4"
    )
    val h5 by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 0.75f,
        animationSpec = infiniteRepeatable(tween(380, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "h5"
    )

    val heights = listOf(h1, h2, h3, h4, h5)

    Row(
        modifier = modifier.height(20.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until barCount) {
            val scale = if (isActive) heights[i % heights.size] else 0.2f
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height((20 * scale).dp.coerceAtLeast(4.dp))
                    .clip(RoundedCornerShape(2.dp))
                    .background(color)
            )
        }
    }
}

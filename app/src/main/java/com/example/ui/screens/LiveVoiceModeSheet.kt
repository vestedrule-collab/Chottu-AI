package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import com.example.R
import com.example.ui.components.LiveVoiceOrb
import com.example.ui.components.RealTimeWaveformIndicator
import com.example.ui.components.SoundWaveVisualizer
import com.example.ui.theme.CosmicBackground
import com.example.ui.theme.CosmicBorder
import com.example.ui.theme.CosmicSurface
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.PinkTertiary
import com.example.ui.theme.VioletSecondary
import com.example.voice.VoiceState

@Composable
fun LiveVoiceModeSheet(
    voiceState: VoiceState,
    rmsLevel: Float,
    partialSpeechText: String,
    lastAiReply: String,
    handsFreeContinuousMode: Boolean,
    onToggleHandsFree: (Boolean) -> Unit,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onInterruptSpeaking: () -> Unit,
    onImageSelected: (Uri) -> Unit,
    selectedImageUri: Uri?,
    onClearImage: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            onImageSelected(uri)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF070A14),
                        CosmicBackground,
                        Color(0xFF0F172A)
                    )
                )
            )
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Header: Title & Close
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color.Black)
                            .border(1.dp, CyanPrimary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.foundation.Image(
                            painter = painterResource(id = R.drawable.ic_infinity_logo),
                            contentDescription = "Chottu AI Infinity Logo",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "CHOTTU LIVE VOICE",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        ),
                        color = Color.White
                    )
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(CosmicSurface)
                        .testTag("close_voice_mode_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close voice mode",
                        tint = Color.White
                    )
                }
            }

            // Center Content: Interactive Visualizer & Transcription
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 16.dp)
            ) {
                // Attached image thumbnail if user attached an image to discuss
                if (selectedImageUri != null) {
                    Box(
                        modifier = Modifier
                            .padding(bottom = 16.dp)
                            .size(90.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .border(2.dp, CyanPrimary, RoundedCornerShape(14.dp))
                    ) {
                        AsyncImage(
                            model = selectedImageUri,
                            contentDescription = "Attached photo for voice analysis",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                        IconButton(
                            onClick = onClearImage,
                            modifier = Modifier
                                .size(24.dp)
                                .align(Alignment.TopEnd)
                                .background(Color.Black.copy(alpha = 0.7f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove photo",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                // Glowing Holographic Voice Orb
                Box(
                    modifier = Modifier
                        .clickable {
                            when (voiceState) {
                                VoiceState.SPEAKING -> onInterruptSpeaking()
                                VoiceState.LISTENING -> onStopListening()
                                else -> onStartListening()
                            }
                        }
                        .testTag("interactive_voice_orb"),
                    contentAlignment = Alignment.Center
                ) {
                    LiveVoiceOrb(
                        voiceState = voiceState,
                        rmsLevel = rmsLevel,
                        size = 220.dp
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Status Badge
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = when (voiceState) {
                        VoiceState.LISTENING -> CyanPrimary.copy(alpha = 0.2f)
                        VoiceState.SPEAKING -> VioletSecondary.copy(alpha = 0.2f)
                        VoiceState.THINKING -> Color(0xFFF59E0B).copy(alpha = 0.2f)
                        else -> CosmicSurface
                    },
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        when (voiceState) {
                            VoiceState.LISTENING -> CyanPrimary
                            VoiceState.SPEAKING -> VioletSecondary
                            VoiceState.THINKING -> Color(0xFFF59E0B)
                            else -> CosmicBorder
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (voiceState == VoiceState.SPEAKING || voiceState == VoiceState.LISTENING) {
                            SoundWaveVisualizer(
                                isActive = true,
                                color = if (voiceState == VoiceState.LISTENING) CyanPrimary else VioletSecondary,
                                barCount = 5,
                                modifier = Modifier.height(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = when (voiceState) {
                                VoiceState.LISTENING -> "Listening to you in real-time..."
                                VoiceState.THINKING -> "Chottu is thinking..."
                                VoiceState.SPEAKING -> "Chottu is speaking (tap to interrupt)"
                                VoiceState.ERROR -> "Tap to retry"
                                VoiceState.IDLE -> "Tap orb or mic to speak"
                            },
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Real-Time Audio Reactive Waveform indicator when listening
                if (voiceState == VoiceState.LISTENING) {
                    RealTimeWaveformIndicator(
                        rmsLevel = rmsLevel,
                        isRecording = true,
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .height(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Real-time Speech Transcription / AI Reply
                if (partialSpeechText.isNotBlank()) {
                    Text(
                        text = "\"$partialSpeechText\"",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Medium,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        ),
                        color = CyanPrimary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                } else if (lastAiReply.isNotBlank() && voiceState == VoiceState.SPEAKING) {
                    Text(
                        text = lastAiReply.take(180) + if (lastAiReply.length > 180) "..." else "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFE2E8F0),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 20.dp),
                        maxLines = 4
                    )
                }
            }

            // Bottom Controls
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Hands-free continuous loop switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CosmicSurface)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Repeat,
                            contentDescription = null,
                            tint = CyanPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Hands-free Continuous Mode",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = Color.White
                            )
                            Text(
                                text = "Auto-listens after Chottu replies",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.LightGray
                            )
                        }
                    }

                    Switch(
                        checked = handsFreeContinuousMode,
                        onCheckedChange = onToggleHandsFree,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CyanPrimary,
                            checkedTrackColor = Color(0xFF004D5A)
                        ),
                        modifier = Modifier.testTag("hands_free_switch")
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action buttons row (Attach photo, Large Mic button, Stop Speaking)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Attach photo for visual Q&A
                    IconButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(CosmicSurface)
                            .border(1.dp, CosmicBorder, CircleShape)
                            .testTag("voice_mode_attach_photo")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = "Attach photo for voice analysis",
                            tint = CyanPrimary,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    // Main Mic / State Button
                    Surface(
                        shape = CircleShape,
                        color = when (voiceState) {
                            VoiceState.LISTENING -> CyanPrimary
                            VoiceState.SPEAKING -> VioletSecondary
                            VoiceState.THINKING -> Color(0xFFF59E0B)
                            else -> CyanPrimary
                        },
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .clickable {
                                when (voiceState) {
                                    VoiceState.LISTENING -> onStopListening()
                                    VoiceState.SPEAKING -> onInterruptSpeaking()
                                    else -> onStartListening()
                                }
                            }
                            .testTag("voice_mode_mic_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = when (voiceState) {
                                    VoiceState.LISTENING -> Icons.Default.MicOff
                                    VoiceState.SPEAKING -> Icons.Default.Stop
                                    else -> Icons.Default.Mic
                                },
                                contentDescription = "Voice control button",
                                tint = if (voiceState == VoiceState.LISTENING) Color(0xFF00363F) else Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    // Stop button
                    IconButton(
                        onClick = {
                            onInterruptSpeaking()
                            onStopListening()
                        },
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(CosmicSurface)
                            .border(1.dp, CosmicBorder, CircleShape)
                            .testTag("voice_mode_stop_all")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Stop",
                            tint = PinkTertiary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

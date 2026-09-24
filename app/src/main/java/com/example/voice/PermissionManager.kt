package com.example.voice

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.ui.theme.CosmicSurface
import com.example.ui.theme.CosmicSurfaceVariant
import com.example.ui.theme.CyanPrimary

/**
 * Permission state for Microphone access.
 */
enum class MicrophonePermissionStatus {
    GRANTED,
    DENIED,
    PERMANENTLY_DENIED
}

/**
 * PermissionManager component for handling microphone access across Chottu AI voice features,
 * including AudioRecord, SpeechRecognizer, and live conversational orb.
 */
object PermissionManager {

    const val RECORD_AUDIO_PERMISSION = Manifest.permission.RECORD_AUDIO

    /**
     * Check whether microphone permission (android.permission.RECORD_AUDIO) is granted.
     */
    fun hasMicrophonePermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            RECORD_AUDIO_PERMISSION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Check whether the system recommends showing a rationale before requesting permission.
     */
    fun shouldShowRationale(activity: Activity): Boolean {
        return ActivityCompat.shouldShowRequestPermissionRationale(
            activity,
            RECORD_AUDIO_PERMISSION
        )
    }

    /**
     * Determine current microphone permission status.
     */
    fun getMicrophonePermissionStatus(activity: Activity): MicrophonePermissionStatus {
        return when {
            hasMicrophonePermission(activity) -> MicrophonePermissionStatus.GRANTED
            shouldShowRationale(activity) -> MicrophonePermissionStatus.DENIED
            else -> MicrophonePermissionStatus.DENIED
        }
    }

    /**
     * Open Android App Details Settings page so the user can enable microphone permissions
     * if previously permanently denied or selected "Don't ask again".
     */
    fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}

/**
 * Material 3 Dialog explaining why Chottu AI requires microphone access and offering
 * options to grant permission or open settings.
 */
@Composable
fun MicrophonePermissionDialog(
    isPermanentlyDenied: Boolean = false,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CosmicSurface,
        shape = RoundedCornerShape(20.dp),
        icon = {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = null,
                tint = CyanPrimary,
                modifier = Modifier.size(36.dp)
            )
        },
        title = {
            Text(
                text = "Microphone Access Required",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (isPermanentlyDenied) {
                        "Microphone permission is currently blocked. To speak with Chottu AI and use live voice features or AudioRecord sound visualization, please enable microphone access in App Settings."
                    } else {
                        "Chottu AI needs microphone access to listen to your voice commands, enable hands-free live conversations, and capture live audio input."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFE2E8F0),
                    lineHeight = 20.sp
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CosmicSurfaceVariant, RoundedCornerShape(10.dp))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = null,
                        tint = CyanPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Used exclusively for voice interactions & real-time audio visualization.",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.LightGray
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isPermanentlyDenied) {
                        onOpenSettings()
                    } else {
                        onRequestPermission()
                    }
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                modifier = Modifier.testTag("microphone_permission_confirm_button")
            ) {
                if (isPermanentlyDenied) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = Color(0xFF00363F),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Open Settings",
                        color = Color(0xFF00363F),
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        text = "Grant Permission",
                        color = Color(0xFF00363F),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("microphone_permission_dismiss_button")
            ) {
                Text(text = "Not Now", color = Color.LightGray)
            }
        },
        modifier = modifier
    )
}

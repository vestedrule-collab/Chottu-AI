package com.example

import android.Manifest
import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.voice.AudioRecordManager
import com.example.voice.PermissionManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PermissionManagerTest {

    @Test
    fun testRecordAudioPermissionConstant() {
        assertEquals(Manifest.permission.RECORD_AUDIO, PermissionManager.RECORD_AUDIO_PERMISSION)
    }

    @Test
    fun testHasMicrophonePermissionDefaultDenied() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // By default in Robolectric without granting, permission is not granted
        val shadowApp = Shadows.shadowOf(context as Application)
        shadowApp.denyPermissions(Manifest.permission.RECORD_AUDIO)

        val hasPermission = PermissionManager.hasMicrophonePermission(context)
        assertFalse(hasPermission)
    }

    @Test
    fun testHasMicrophonePermissionWhenGranted() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val shadowApp = Shadows.shadowOf(context as Application)
        shadowApp.grantPermissions(Manifest.permission.RECORD_AUDIO)

        val hasPermission = PermissionManager.hasMicrophonePermission(context)
        assertTrue(hasPermission)
    }

    @Test
    fun testAudioRecordManagerFailsGracefullyWithoutPermission() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val shadowApp = Shadows.shadowOf(context as Application)
        shadowApp.denyPermissions(Manifest.permission.RECORD_AUDIO)

        val audioRecordManager = AudioRecordManager(context)
        val started = audioRecordManager.startCapture()

        assertFalse(started)
        assertEquals("Microphone permission is required for AudioRecord.", audioRecordManager.errorMessage.value)
        audioRecordManager.release()
    }
}

package com.example.voice

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.log10
import kotlin.math.sqrt

/**
 * AudioRecordManager component for Android AudioRecord low-latency audio capture
 * and real-time audio amplitude / RMS visualization for Chottu AI.
 */
class AudioRecordManager(private val context: Context) {

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _audioRmsLevel = MutableStateFlow(0f)
    val audioRmsLevel: StateFlow<Float> = _audioRmsLevel.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    companion object {
        const val SAMPLE_RATE = 44100
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }

    /**
     * Start capturing audio using Android AudioRecord.
     * Verifies microphone permission via PermissionManager before initializing.
     */
    fun startCapture(onAudioData: ((ByteArray, Int) -> Unit)? = null): Boolean {
        if (!PermissionManager.hasMicrophonePermission(context)) {
            _errorMessage.value = "Microphone permission is required for AudioRecord."
            return false
        }

        if (_isRecording.value) return true

        try {
            val minBufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT
            )

            if (minBufferSize <= 0) {
                _errorMessage.value = "AudioRecord hardware configuration unsupported."
                return false
            }

            val bufferSize = (minBufferSize * 2).coerceAtLeast(2048)

            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                _errorMessage.value = "Failed to initialize AudioRecord instance."
                release()
                return false
            }

            audioRecord?.startRecording()
            _isRecording.value = true
            _errorMessage.value = null

            recordingJob = scope.launch {
                val buffer = ShortArray(bufferSize / 2)
                val byteBuffer = ByteArray(bufferSize)

                while (isActive && _isRecording.value) {
                    val readCount = audioRecord?.read(buffer, 0, buffer.size) ?: -1
                    if (readCount > 0) {
                        // Compute RMS amplitude in dB
                        var sum = 0.0
                        for (i in 0 until readCount) {
                            sum += buffer[i] * buffer[i]
                        }
                        val mean = sum / readCount
                        val rms = sqrt(mean)
                        // Calculate normalized dB value (0f..1f)
                        val db = if (rms > 0) 20 * log10(rms / 32768.0) else -100.0
                        // Map -60dB .. 0dB to 0.0f .. 1.0f
                        val normalized = ((db + 60.0) / 60.0).coerceIn(0.0, 1.0).toFloat()
                        _audioRmsLevel.value = normalized

                        // Pass raw audio bytes if consumer requested
                        if (onAudioData != null) {
                            for (i in 0 until readCount) {
                                val s = buffer[i].toInt()
                                byteBuffer[i * 2] = (s and 0xFF).toByte()
                                byteBuffer[i * 2 + 1] = ((s shr 8) and 0xFF).toByte()
                            }
                            onAudioData(byteBuffer, readCount * 2)
                        }
                    }
                }
            }
            return true
        } catch (e: SecurityException) {
            _errorMessage.value = "SecurityException: Microphone permission revoked: ${e.message}"
            release()
            return false
        } catch (e: Exception) {
            _errorMessage.value = "Error starting AudioRecord: ${e.message}"
            release()
            return false
        }
    }

    /**
     * Stop capturing audio and reset live RMS level.
     */
    fun stopCapture() {
        _isRecording.value = false
        recordingJob?.cancel()
        recordingJob = null
        _audioRmsLevel.value = 0f

        try {
            if (audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                audioRecord?.stop()
            }
        } catch (e: Exception) {
            // Ignore stop errors
        }
    }

    /**
     * Release all AudioRecord hardware resources.
     */
    fun release() {
        stopCapture()
        try {
            audioRecord?.release()
        } catch (e: Exception) {
            // Ignore release errors
        } finally {
            audioRecord = null
        }
    }
}

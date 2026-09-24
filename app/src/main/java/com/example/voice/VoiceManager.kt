package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

enum class VoiceState {
    IDLE,
    LISTENING,
    THINKING,
    SPEAKING,
    ERROR
}

class VoiceManager(
    private val context: Context,
    private val onSpeechRecognized: (String) -> Unit,
    private val onSpeechFinishedSpeaking: () -> Unit = {}
) : RecognitionListener, TextToSpeech.OnInitListener {

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsInitialized = false

    private val _voiceState = MutableStateFlow(VoiceState.IDLE)
    val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()

    private val _rmsLevel = MutableStateFlow(0f)
    val rmsLevel: StateFlow<Float> = _rmsLevel.asStateFlow()

    private val _partialSpeechText = MutableStateFlow("")
    val partialSpeechText: StateFlow<String> = _partialSpeechText.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _currentlySpeakingMessageId = MutableStateFlow<Long?>(null)
    val currentlySpeakingMessageId: StateFlow<Long?> = _currentlySpeakingMessageId.asStateFlow()

    // Voice tuning parameters
    var speechRate: Float = 1.05f
        set(value) {
            field = value
            textToSpeech?.setSpeechRate(value)
        }

    var speechPitch: Float = 1.0f
        set(value) {
            field = value
            textToSpeech?.setPitch(value)
        }

    var autoSpeakEnabled: Boolean = true

    init {
        initTts()
    }

    private fun initTts() {
        try {
            textToSpeech = TextToSpeech(context.applicationContext, this)
        } catch (e: Exception) {
            _errorMessage.value = "Unable to initialize Text-to-Speech: ${e.message}"
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTtsInitialized = true
            textToSpeech?.let { tts ->
                // Try Indian English or Default English for Chottu AI's signature persona
                val preferredLocale = Locale.Builder().setLanguage("en").setRegion("IN").build()
                val langResult = tts.setLanguage(preferredLocale)
                if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts.language = Locale.US
                }
                tts.setSpeechRate(speechRate)
                tts.setPitch(speechPitch)

                tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _voiceState.value = VoiceState.SPEAKING
                    }

                    override fun onDone(utteranceId: String?) {
                        _currentlySpeakingMessageId.value = null
                        if (_voiceState.value == VoiceState.SPEAKING) {
                            _voiceState.value = VoiceState.IDLE
                        }
                        onSpeechFinishedSpeaking()
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        _currentlySpeakingMessageId.value = null
                        if (_voiceState.value == VoiceState.SPEAKING) {
                            _voiceState.value = VoiceState.IDLE
                        }
                    }
                })
            }
        } else {
            isTtsInitialized = false
        }
    }

    fun isRecognitionAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    fun startListening() {
        stopSpeaking()
        _errorMessage.value = null
        _partialSpeechText.value = ""

        if (!PermissionManager.hasMicrophonePermission(context)) {
            _errorMessage.value = "Microphone permission is required to listen."
            _voiceState.value = VoiceState.ERROR
            return
        }

        if (!isRecognitionAvailable()) {
            _errorMessage.value = "Speech recognition is not supported or not enabled on this device."
            _voiceState.value = VoiceState.ERROR
            return
        }

        try {
            if (speechRecognizer == null) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context.applicationContext)
                speechRecognizer?.setRecognitionListener(this)
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            }

            _voiceState.value = VoiceState.LISTENING
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            _errorMessage.value = "Failed to start listening: ${e.message}"
            _voiceState.value = VoiceState.ERROR
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
        } catch (e: Exception) {
            // Ignore
        }
        if (_voiceState.value == VoiceState.LISTENING) {
            _voiceState.value = VoiceState.IDLE
        }
        _rmsLevel.value = 0f
    }

    fun cancelListening() {
        try {
            speechRecognizer?.cancel()
        } catch (e: Exception) {
            // Ignore
        }
        _voiceState.value = VoiceState.IDLE
        _rmsLevel.value = 0f
        _partialSpeechText.value = ""
    }

    fun speak(text: String, messageId: Long? = null) {
        if (!isTtsInitialized || textToSpeech == null) return

        stopSpeaking()
        _currentlySpeakingMessageId.value = messageId
        _voiceState.value = VoiceState.SPEAKING

        // Clean markdown tokens from spoken text so it speaks cleanly and naturally
        val spokenCleanText = cleanTextForSpeech(text)
        val params = Bundle()
        val utteranceId = messageId?.toString() ?: "chottu_${System.currentTimeMillis()}"

        textToSpeech?.speak(spokenCleanText, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    }

    fun stopSpeaking() {
        if (textToSpeech?.isSpeaking == true) {
            textToSpeech?.stop()
        }
        _currentlySpeakingMessageId.value = null
        if (_voiceState.value == VoiceState.SPEAKING) {
            _voiceState.value = VoiceState.IDLE
        }
    }

    fun setThinking() {
        stopSpeaking()
        _voiceState.value = VoiceState.THINKING
    }

    fun setIdle() {
        _voiceState.value = VoiceState.IDLE
        _rmsLevel.value = 0f
    }

    private fun cleanTextForSpeech(input: String): String {
        return input
            .replace(Regex("```[a-zA-Z]*\\n[\\s\\S]*?```"), " Here is the code snippet. ")
            .replace(Regex("`[^`]+`"), " ")
            .replace(Regex("[#*_~>\\[\\]()]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    // --- RecognitionListener Callbacks ---

    override fun onReadyForSpeech(params: Bundle?) {
        _voiceState.value = VoiceState.LISTENING
    }

    override fun onBeginningOfSpeech() {
        _voiceState.value = VoiceState.LISTENING
    }

    override fun onRmsChanged(rmsdB: Float) {
        // Map rmsdB (typically -2 to 10) to 0.0f..1.0f range
        val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
        _rmsLevel.value = normalized
    }

    override fun onBufferReceived(buffer: ByteArray?) {}

    override fun onEndOfSpeech() {
        _rmsLevel.value = 0f
    }

    override fun onError(error: Int) {
        _rmsLevel.value = 0f
        if (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
            _voiceState.value = VoiceState.IDLE
        } else {
            val msg = when (error) {
                SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                SpeechRecognizer.ERROR_CLIENT -> "Client recognition error"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required"
                SpeechRecognizer.ERROR_NETWORK -> "Network error during speech recognition"
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech service is busy"
                SpeechRecognizer.ERROR_SERVER -> "Server error"
                else -> "Recognition error ($error)"
            }
            _errorMessage.value = msg
            _voiceState.value = VoiceState.ERROR
        }
    }

    override fun onResults(results: Bundle?) {
        _rmsLevel.value = 0f
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val spokenText = matches?.firstOrNull()?.trim()
        if (!spokenText.isNullOrBlank()) {
            _partialSpeechText.value = spokenText
            onSpeechRecognized(spokenText)
        } else {
            _voiceState.value = VoiceState.IDLE
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val partial = matches?.firstOrNull()
        if (!partial.isNullOrBlank()) {
            _partialSpeechText.value = partial
        }
    }

    override fun onEvent(eventType: Int, params: Bundle?) {}

    fun destroy() {
        try {
            speechRecognizer?.destroy()
            speechRecognizer = null
            textToSpeech?.stop()
            textToSpeech?.shutdown()
            textToSpeech = null
        } catch (e: Exception) {
            // Ignore
        }
    }
}

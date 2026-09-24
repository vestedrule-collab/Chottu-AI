package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.ChatMessageEntity
import com.example.data.local.ChatSessionEntity
import com.example.data.remote.AttachmentData
import com.example.data.remote.GeminiService
import com.example.data.repository.ChatRepository
import com.example.voice.AudioRecordManager
import com.example.voice.PermissionManager
import com.example.voice.VoiceManager
import com.example.voice.VoiceState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("chottu_prefs", Context.MODE_PRIVATE)

    private val database = AppDatabase.getInstance(application)
    private val repository = ChatRepository(database.chatDao())
    val geminiService = GeminiService()

    val audioRecordManager = AudioRecordManager(application)

    val voiceManager = VoiceManager(
        context = application,
        onSpeechRecognized = { spoken ->
            handleSpokenText(spoken)
        },
        onSpeechFinishedSpeaking = {
            handleSpeechFinished()
        }
    )

    // Chat sessions
    val allSessions: StateFlow<List<ChatSessionEntity>> = repository.allSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentSessionId = MutableStateFlow<Long>(0L)
    val currentSessionId: StateFlow<Long> = _currentSessionId.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val currentMessages: StateFlow<List<ChatMessageEntity>> = _currentSessionId
        .flatMapLatest { id ->
            repository.getMessages(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Input state
    val inputText = MutableStateFlow("")
    val isLoading = MutableStateFlow(false)
    val errorMessage = MutableStateFlow<String?>(null)

    // Multimodal attachments
    val attachedUri = MutableStateFlow<Uri?>(null)
    val isAttachedImage = MutableStateFlow(false)
    val attachedFileName = MutableStateFlow<String?>(null)

    // Live Voice Mode UI state
    val isLiveVoiceModeOpen = MutableStateFlow(false)
    val handsFreeContinuousMode = MutableStateFlow(prefs.getBoolean("hands_free", true))
    val lastAiReply = MutableStateFlow("")

    // Settings UI state
    val isSettingsOpen = MutableStateFlow(false)
    val userCustomApiKey = MutableStateFlow(prefs.getString("custom_api_key", "") ?: "")
    val autoSpeakReplies = MutableStateFlow(prefs.getBoolean("auto_speak", true))
    val speechRate = MutableStateFlow(prefs.getFloat("speech_rate", 1.05f))
    val speechPitch = MutableStateFlow(prefs.getFloat("speech_pitch", 1.0f))

    private var activeApiJob: Job? = null

    init {
        voiceManager.speechRate = speechRate.value
        voiceManager.speechPitch = speechPitch.value
        voiceManager.autoSpeakEnabled = autoSpeakReplies.value

        viewModelScope.launch {
            // Observe sessions to select or create first session
            allSessions.collect { list ->
                if (_currentSessionId.value == 0L) {
                    if (list.isNotEmpty()) {
                        _currentSessionId.value = list.first().id
                    } else {
                        val newId = repository.createSession("New Conversation")
                        _currentSessionId.value = newId
                    }
                }
            }
        }
    }

    fun selectSession(sessionId: Long) {
        _currentSessionId.value = sessionId
        stopSpeaking()
        clearAttachment()
        errorMessage.value = null
    }

    fun createNewSession() {
        viewModelScope.launch {
            val newId = repository.createSession("Conversation #${allSessions.value.size + 1}")
            _currentSessionId.value = newId
            clearAttachment()
            stopSpeaking()
        }
    }

    fun deleteSession(session: ChatSessionEntity) {
        viewModelScope.launch {
            repository.deleteSession(session)
            if (_currentSessionId.value == session.id) {
                _currentSessionId.value = 0L
            }
        }
    }

    fun renameSession(sessionId: Long, newTitle: String) {
        viewModelScope.launch {
            val trimmed = newTitle.trim()
            if (trimmed.isNotBlank()) {
                repository.updateSessionTitle(sessionId, trimmed)
            }
        }
    }

    fun autoGenerateSessionTitle(sessionId: Long, userPrompt: String, assistantReply: String) {
        viewModelScope.launch {
            try {
                val titleResult = geminiService.generateConversationTitle(
                    userPrompt = userPrompt,
                    assistantReply = assistantReply,
                    customApiKey = userCustomApiKey.value
                )
                titleResult.onSuccess { cleanTitle ->
                    if (cleanTitle.isNotBlank()) {
                        repository.updateSessionTitle(sessionId, cleanTitle)
                    }
                }.onFailure {
                    // Fallback to intelligent clean summary
                    val fallback = if (userPrompt.length > 26) {
                        userPrompt.take(24).trim() + "..."
                    } else {
                        userPrompt.trim()
                    }
                    if (fallback.isNotBlank()) {
                        repository.updateSessionTitle(sessionId, fallback)
                    }
                }
            } catch (e: Exception) {
                // Background title generation fails gracefully without interrupting chat
            }
        }
    }

    fun attachFile(uri: Uri, isImage: Boolean, fileName: String?) {
        attachedUri.value = uri
        isAttachedImage.value = isImage
        attachedFileName.value = fileName
    }

    fun clearAttachment() {
        attachedUri.value = null
        isAttachedImage.value = false
        attachedFileName.value = null
    }

    fun sendMessage(prompt: String = inputText.value, isVoice: Boolean = false) {
        val trimmed = prompt.trim()
        val currentUri = attachedUri.value
        val isImg = isAttachedImage.value
        val fileName = attachedFileName.value

        if (trimmed.isBlank() && currentUri == null) return

        val sessionId = _currentSessionId.value
        if (sessionId == 0L) return

        inputText.value = ""
        clearAttachment()
        errorMessage.value = null
        isLoading.value = true

        if (isLiveVoiceModeOpen.value) {
            voiceManager.setThinking()
        }

        activeApiJob = viewModelScope.launch {
            try {
                // 1. Save user message to database
                val userMsg = ChatMessageEntity(
                    sessionId = sessionId,
                    role = "user",
                    content = if (trimmed.isBlank() && currentUri != null) "Analyzed attachment" else trimmed,
                    timestamp = System.currentTimeMillis(),
                    mediaUri = currentUri?.toString(),
                    mediaType = if (currentUri != null) if (isImg) "image" else "document" else null,
                    fileName = fileName,
                    isVoice = isVoice
                )
                repository.addMessage(userMsg)

                // Update session title dynamically if first message
                val recentMessages = repository.getRecentMessages(sessionId, 5)
                if (recentMessages.size <= 2 && trimmed.isNotBlank()) {
                    val summaryTitle = if (trimmed.length > 30) trimmed.take(28) + "..." else trimmed
                    repository.updateSessionTitle(sessionId, summaryTitle)
                }

                // 2. Prepare multimodal attachment data if present
                var attachmentData: AttachmentData? = null
                if (currentUri != null) {
                    attachmentData = geminiService.loadAttachment(
                        getApplication(),
                        currentUri,
                        if (isImg) "image/jpeg" else null
                    )
                }

                // 3. Prepare recent turns for current conversation context
                val sessionMessages = repository.getAllMessagesForSession(sessionId)
                val currentHistory = sessionMessages.takeLast(16).map { it.role to it.content }

                // Build long-term memory context from past conversations if user has multiple sessions/past chats
                val allPastMessages = repository.getAllPastMessagesAcrossAllSessions()
                val pastOtherMessages = allPastMessages.filter { it.sessionId != sessionId }
                val longTermMemorySummary = if (pastOtherMessages.isNotEmpty()) {
                    val keyUserInputs = pastOtherMessages
                        .filter { it.role == "user" && it.content.isNotBlank() }
                        .takeLast(15)
                        .joinToString(separator = "\n- ") { it.content.take(150) }
                    if (keyUserInputs.isNotBlank()) {
                        "Past user queries and context discussed in prior chats:\n- $keyUserInputs"
                    } else null
                } else null

                // 4. Call Gemini 3.5 Flash with full conversational context and long-term memory
                val result = geminiService.generateResponse(
                    prompt = trimmed,
                    customApiKey = userCustomApiKey.value,
                    conversationHistory = currentHistory,
                    longTermMemoryContext = longTermMemorySummary,
                    attachmentData = attachmentData
                )

                result.onSuccess { reply ->
                    lastAiReply.value = reply
                    val assistantMsg = ChatMessageEntity(
                        sessionId = sessionId,
                        role = "assistant",
                        content = reply,
                        timestamp = System.currentTimeMillis()
                    )
                    val insertedId = repository.addMessage(assistantMsg)

                    // Auto-generate short, descriptive title based on first few exchanges
                    val currentSession = allSessions.value.find { it.id == sessionId }
                    val shouldAutoTitle = sessionMessages.size <= 2 ||
                            currentSession?.title?.startsWith("New Conversation") == true ||
                            currentSession?.title?.startsWith("Conversation #") == true ||
                            currentSession?.title?.endsWith("...") == true
                    if (shouldAutoTitle && trimmed.isNotBlank()) {
                        autoGenerateSessionTitle(sessionId, trimmed, reply)
                    }

                    // Speak aloud if in Live Voice mode or auto-speak enabled
                    if (isLiveVoiceModeOpen.value || autoSpeakReplies.value) {
                        voiceManager.speak(reply, insertedId)
                    } else {
                        voiceManager.setIdle()
                    }
                }.onFailure { ex ->
                    val err = ex.message ?: "Failed to generate reply"
                    errorMessage.value = err
                    voiceManager.setIdle()
                }
            } catch (e: Exception) {
                errorMessage.value = e.message ?: "An unexpected error occurred"
                voiceManager.setIdle()
            } finally {
                isLoading.value = false
            }
        }
    }

    private fun handleSpokenText(spoken: String) {
        if (spoken.isNotBlank()) {
            sendMessage(prompt = spoken, isVoice = true)
        }
    }

    private fun handleSpeechFinished() {
        // If continuous hands-free voice mode is active and Live Voice sheet is open, automatically resume listening!
        if (isLiveVoiceModeOpen.value && handsFreeContinuousMode.value) {
            viewModelScope.launch {
                // Brief pause before opening microphone
                kotlinx.coroutines.delay(400)
                if (isLiveVoiceModeOpen.value && voiceManager.voiceState.value == VoiceState.IDLE) {
                    voiceManager.startListening()
                }
            }
        }
    }

    fun startListening() {
        voiceManager.startListening()
    }

    fun stopListening() {
        voiceManager.stopListening()
    }

    fun toggleSpeakMessage(message: ChatMessageEntity) {
        if (voiceManager.currentlySpeakingMessageId.value == message.id) {
            voiceManager.stopSpeaking()
        } else {
            voiceManager.speak(message.content, message.id)
        }
    }

    fun stopSpeaking() {
        voiceManager.stopSpeaking()
    }

    fun openLiveVoiceMode() {
        isLiveVoiceModeOpen.value = true
        voiceManager.stopSpeaking()
        voiceManager.startListening()
    }

    fun closeLiveVoiceMode() {
        isLiveVoiceModeOpen.value = false
        voiceManager.stopSpeaking()
        voiceManager.cancelListening()
    }

    fun toggleHandsFree(enabled: Boolean) {
        handsFreeContinuousMode.value = enabled
        prefs.edit().putBoolean("hands_free", enabled).apply()
    }

    fun updateSpeechRate(rate: Float) {
        speechRate.value = rate
        voiceManager.speechRate = rate
        prefs.edit().putFloat("speech_rate", rate).apply()
    }

    fun updateSpeechPitch(pitch: Float) {
        speechPitch.value = pitch
        voiceManager.speechPitch = pitch
        prefs.edit().putFloat("speech_pitch", pitch).apply()
    }

    fun updateAutoSpeak(enabled: Boolean) {
        autoSpeakReplies.value = enabled
        voiceManager.autoSpeakEnabled = enabled
        prefs.edit().putBoolean("auto_speak", enabled).apply()
    }

    fun saveCustomApiKey(key: String) {
        userCustomApiKey.value = key
        prefs.edit().putString("custom_api_key", key).apply()
    }

    fun testVoice() {
        voiceManager.speak("Hello! I am Chottu AI, your voice assistant. Ready to help you with anything!")
    }

    fun exportCurrentConversation(asJson: Boolean) {
        viewModelScope.launch {
            val sessionId = _currentSessionId.value
            val session = allSessions.value.find { it.id == sessionId }
            val messages = currentMessages.value
            if (messages.isEmpty()) {
                errorMessage.value = "No messages to export in this conversation."
                return@launch
            }

            val sanitizedTitle = (session?.title ?: "chat")
                .replace(Regex("[^a-zA-Z0-9_-]"), "_")
                .take(25)
            val timestamp = System.currentTimeMillis()

            if (asJson) {
                val jsonString = com.example.util.ChatExportManager.generateJsonExport(session, messages)
                com.example.util.ChatExportManager.saveAndShareExportFile(
                    context = getApplication(),
                    content = jsonString,
                    fileName = "chottu_chat_${sanitizedTitle}_$timestamp.json",
                    mimeType = "application/json"
                )
            } else {
                val textString = com.example.util.ChatExportManager.generatePlainTextExport(session, messages)
                com.example.util.ChatExportManager.saveAndShareExportFile(
                    context = getApplication(),
                    content = textString,
                    fileName = "chottu_chat_${sanitizedTitle}_$timestamp.txt",
                    mimeType = "text/plain"
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioRecordManager.release()
        voiceManager.destroy()
    }
}

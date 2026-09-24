package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.components.AttachmentPreviewCard
import com.example.ui.components.LiveVoiceOrb
import com.example.ui.components.MessageItem
import com.example.ui.components.MicPulseWaveFeedback
import com.example.ui.components.RealTimeWaveformIndicator
import com.example.ui.components.SoundWaveVisualizer
import com.example.ui.components.WelcomeEmptyState
import com.example.ui.theme.CosmicBackground
import com.example.ui.theme.CosmicBorder
import com.example.ui.theme.CosmicSurface
import com.example.ui.theme.CosmicSurfaceVariant
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.PinkTertiary
import com.example.ui.theme.VioletSecondary
import com.example.ui.viewmodel.ChatViewModel
import com.example.voice.MicrophonePermissionDialog
import com.example.voice.PermissionManager
import com.example.voice.VoiceState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val listState = rememberLazyListState()

    val sessions by viewModel.allSessions.collectAsStateWithLifecycle()
    val currentSessionId by viewModel.currentSessionId.collectAsStateWithLifecycle()
    val messages by viewModel.currentMessages.collectAsStateWithLifecycle()

    val inputText by viewModel.inputText.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    val attachedUri by viewModel.attachedUri.collectAsStateWithLifecycle()
    val isAttachedImage by viewModel.isAttachedImage.collectAsStateWithLifecycle()
    val attachedFileName by viewModel.attachedFileName.collectAsStateWithLifecycle()

    val voiceState by viewModel.voiceManager.voiceState.collectAsStateWithLifecycle()
    val rmsLevel by viewModel.voiceManager.rmsLevel.collectAsStateWithLifecycle()
    val partialSpeechText by viewModel.voiceManager.partialSpeechText.collectAsStateWithLifecycle()
    val currentlySpeakingId by viewModel.voiceManager.currentlySpeakingMessageId.collectAsStateWithLifecycle()

    val isLiveVoiceModeOpen by viewModel.isLiveVoiceModeOpen.collectAsStateWithLifecycle()
    val handsFreeMode by viewModel.handsFreeContinuousMode.collectAsStateWithLifecycle()
    val lastAiReply by viewModel.lastAiReply.collectAsStateWithLifecycle()

    val isSettingsOpen by viewModel.isSettingsOpen.collectAsStateWithLifecycle()
    val customApiKey by viewModel.userCustomApiKey.collectAsStateWithLifecycle()
    val speechRate by viewModel.speechRate.collectAsStateWithLifecycle()
    val speechPitch by viewModel.speechPitch.collectAsStateWithLifecycle()
    val autoSpeakReplies by viewModel.autoSpeakReplies.collectAsStateWithLifecycle()

    // Permission manager state for microphone
    var showMicPermissionDialog by remember { mutableStateOf(false) }
    var isMicPermanentlyDenied by remember { mutableStateOf(false) }
    var pendingVoiceAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    val activity = context as? Activity

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            isMicPermanentlyDenied = false
            showMicPermissionDialog = false
            pendingVoiceAction?.invoke()
            pendingVoiceAction = null
        } else {
            val shouldShowRationale = activity?.let { PermissionManager.shouldShowRationale(it) } ?: false
            isMicPermanentlyDenied = !shouldShowRationale
            showMicPermissionDialog = true
            Toast.makeText(context, "Microphone permission is required for voice interaction.", Toast.LENGTH_SHORT).show()
        }
    }

    val requestMicAccess: (() -> Unit) -> Unit = { action ->
        if (PermissionManager.hasMicrophonePermission(context)) {
            action()
        } else {
            pendingVoiceAction = action
            val shouldShowRationale = activity?.let { PermissionManager.shouldShowRationale(it) } ?: false
            if (shouldShowRationale) {
                isMicPermanentlyDenied = false
                showMicPermissionDialog = true
            } else {
                micPermissionLauncher.launch(PermissionManager.RECORD_AUDIO_PERMISSION)
            }
        }
    }

    // Photo picker launcher (Visual Media - Google Play compliant)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = getFileName(context, uri) ?: "Selected image"
            viewModel.attachFile(uri, isImage = true, fileName = fileName)
        }
    }

    // Document / File picker launcher
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = getFileName(context, uri) ?: "Selected file"
            val mime = context.contentResolver.getType(uri) ?: ""
            val isImg = mime.startsWith("image/")
            viewModel.attachFile(uri, isImage = isImg, fileName = fileName)
        }
    }

    // Auto-scroll to bottom on new messages
    LaunchedEffect(messages.size, isLoading) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // When partial speech text arrives in chat mode, populate input text
    LaunchedEffect(partialSpeechText) {
        if (partialSpeechText.isNotBlank() && !isLiveVoiceModeOpen) {
            viewModel.inputText.value = partialSpeechText
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = CosmicSurface,
                modifier = Modifier.width(300.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Drawer Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color.Black)
                                .border(1.5.dp, Brush.linearGradient(listOf(CyanPrimary, VioletSecondary)), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            androidx.compose.foundation.Image(
                                painter = painterResource(id = R.drawable.ic_infinity_logo),
                                contentDescription = "Chottu AI Infinity Logo",
                                modifier = Modifier.size(30.dp),
                                contentScale = ContentScale.Fit
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Chottu AI",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Text(
                                text = "Conversations",
                                style = MaterialTheme.typography.labelSmall,
                                color = CyanPrimary
                            )
                        }
                    }

                    // New Chat Button
                    Button(
                        onClick = {
                            viewModel.createNewSession()
                            coroutineScope.launch { drawerState.close() }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .testTag("new_chat_drawer_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = Color(0xFF00363F)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "New Conversation",
                            color = Color(0xFF00363F),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    HorizontalDivider(
                        color = CosmicBorder,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    // Sessions List
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(sessions, key = { it.id }) { session ->
                            val isSelected = session.id == currentSessionId
                            NavigationDrawerItem(
                                label = {
                                    Text(
                                        text = session.title,
                                        maxLines = 1,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) CyanPrimary else Color.White
                                    )
                                },
                                selected = isSelected,
                                onClick = {
                                    viewModel.selectSession(session.id)
                                    coroutineScope.launch { drawerState.close() }
                                },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.ChatBubbleOutline,
                                        contentDescription = null,
                                        tint = if (isSelected) CyanPrimary else Color.Gray,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                badge = {
                                    if (sessions.size > 1) {
                                        IconButton(
                                            onClick = { viewModel.deleteSession(session) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete chat",
                                                tint = Color.Gray.copy(alpha = 0.6f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                },
                                colors = NavigationDrawerItemDefaults.colors(
                                    selectedContainerColor = CosmicSurfaceVariant,
                                    unselectedContainerColor = Color.Transparent
                                ),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }

                    // Settings at bottom of drawer
                    NavigationDrawerItem(
                        label = { Text("Settings & Voice", color = Color.White) },
                        selected = false,
                        onClick = {
                            viewModel.isSettingsOpen.value = true
                            coroutineScope.launch { drawerState.close() }
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                tint = CyanPrimary
                            )
                        },
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }
        }
    ) {
        Scaffold(
            modifier = modifier
                .fillMaxSize()
                .background(CosmicBackground),
            containerColor = CosmicBackground,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(CyanPrimary, VioletSecondary)
                                        )
                                    )
                                    .padding(2.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black),
                                contentAlignment = Alignment.Center
                            ) {
                                androidx.compose.foundation.Image(
                                    painter = painterResource(id = R.drawable.ic_infinity_logo),
                                    contentDescription = "Chottu AI Infinity Logo",
                                    modifier = Modifier.size(24.dp),
                                    contentScale = ContentScale.Fit
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Chottu AI",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when (voiceState) {
                                                    VoiceState.LISTENING -> CyanPrimary
                                                    VoiceState.SPEAKING -> VioletSecondary
                                                    VoiceState.THINKING -> Color(0xFFF59E0B)
                                                    else -> Color(0xFF10B981)
                                                }
                                            )
                                    )
                                }
                                Text(
                                    text = when (voiceState) {
                                        VoiceState.LISTENING -> "Listening..."
                                        VoiceState.SPEAKING -> "Speaking aloud..."
                                        VoiceState.THINKING -> "Thinking..."
                                        else -> "Voice & Multimodal Assistant"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CyanPrimary
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { coroutineScope.launch { drawerState.open() } },
                            modifier = Modifier.testTag("open_drawer_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Open Drawer",
                                tint = Color.White
                            )
                        }
                    },
                    actions = {
                        // Live Voice Mode Button (Hero CTA)
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = CosmicSurfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .clickable {
                                    requestMicAccess {
                                        viewModel.openLiveVoiceMode()
                                    }
                                }
                                .testTag("open_live_voice_mode_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.GraphicEq,
                                    contentDescription = null,
                                    tint = CyanPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Live Voice",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyanPrimary
                                )
                            }
                        }

                        // Settings Icon
                        IconButton(
                            onClick = { viewModel.isSettingsOpen.value = true },
                            modifier = Modifier.testTag("top_bar_settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = CosmicSurface
                    )
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .imePadding()
            ) {
                // Error Alert Banner if any
                AnimatedVisibility(visible = errorMessage != null) {
                    errorMessage?.let { err ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF450A0A)),
                            shape = RoundedCornerShape(0.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = PinkTertiary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = err,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White,
                                    modifier = Modifier.weight(1f)
                                )
                                if (err.contains("API key", ignoreCase = true)) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = { viewModel.isSettingsOpen.value = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = PinkTertiary),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text(text = "Configure", fontSize = 11.sp, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }

                // Messages List or Empty State
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (messages.isEmpty()) {
                        WelcomeEmptyState(
                            onPromptSelected = { selectedPrompt ->
                                viewModel.sendMessage(selectedPrompt)
                            },
                            onStartVoice = {
                                requestMicAccess {
                                    viewModel.openLiveVoiceMode()
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            item { Spacer(modifier = Modifier.height(8.dp)) }

                            items(messages, key = { it.id }) { message ->
                                MessageItem(
                                    message = message,
                                    isCurrentlySpeaking = currentlySpeakingId == message.id,
                                    onSpeakToggle = { viewModel.toggleSpeakMessage(message) },
                                    onDeleteMessage = { }
                                )
                            }

                            // Thinking indicator
                            if (isLoading) {
                                item {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(CosmicSurface),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            LiveVoiceOrb(
                                                voiceState = VoiceState.THINKING,
                                                rmsLevel = 0f,
                                                size = 32.dp
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Surface(
                                            shape = RoundedCornerShape(16.dp),
                                            color = CosmicSurfaceVariant,
                                            border = androidx.compose.foundation.BorderStroke(1.dp, CosmicBorder)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                SoundWaveVisualizer(
                                                    isActive = true,
                                                    color = CyanPrimary,
                                                    barCount = 4,
                                                    modifier = Modifier.height(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "Chottu is analyzing & thinking...",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = Color.LightGray
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            item { Spacer(modifier = Modifier.height(8.dp)) }
                        }
                    }
                }

                // Attached File Preview in composer
                if (attachedUri != null) {
                    AttachmentPreviewCard(
                        attachmentUri = attachedUri!!,
                        isImage = isAttachedImage,
                        fileName = attachedFileName,
                        onRemove = { viewModel.clearAttachment() }
                    )
                }

                // Real-Time Reactive Voice Wave Animation & Live Feedback Bar (When recording audio)
                AnimatedVisibility(visible = voiceState == VoiceState.LISTENING && !isLiveVoiceModeOpen) {
                    Surface(
                        color = Color(0xFF041822),
                        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    MicPulseWaveFeedback(
                                        isRecording = true,
                                        rmsLevel = rmsLevel,
                                        size = 36.dp
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = if (partialSpeechText.isNotBlank()) "\"$partialSpeechText\"" else "Listening to your voice...",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = Color.White,
                                            maxLines = 2
                                        )
                                        Text(
                                            text = "Reactive mic audio input • Tap stop when done",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = CyanPrimary.copy(alpha = 0.85f)
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { viewModel.stopListening() },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF2D1515))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Stop,
                                        contentDescription = "Stop listening",
                                        tint = PinkTertiary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Continuous real-time reacting multi-frequency audio waveform
                            RealTimeWaveformIndicator(
                                rmsLevel = rmsLevel,
                                isRecording = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                            )
                        }
                    }
                }

                // Bottom Input Bar
                Surface(
                    color = CosmicSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CosmicBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Multimodal Photo Picker
                        IconButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier
                                .size(42.dp)
                                .testTag("attach_photo_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = "Attach photo",
                                tint = CyanPrimary
                            )
                        }

                        // Multimodal Document Picker
                        IconButton(
                            onClick = {
                                documentPickerLauncher.launch("*/*")
                            },
                            modifier = Modifier
                                .size(42.dp)
                                .testTag("attach_file_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AttachFile,
                                contentDescription = "Attach document or file",
                                tint = VioletSecondary
                            )
                        }

                        // Text Field Input
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { viewModel.inputText.value = it },
                            placeholder = {
                                Text(
                                    text = if (attachedUri != null) "Add instructions for this file..." else "Ask Chottu or speak...",
                                    color = Color.Gray,
                                    fontSize = 14.sp
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 4.dp)
                                .testTag("chat_input_text_field"),
                            maxLines = 4,
                            shape = RoundedCornerShape(22.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = CosmicSurfaceVariant,
                                unfocusedContainerColor = CosmicSurfaceVariant,
                                focusedBorderColor = CyanPrimary,
                                unfocusedBorderColor = CosmicBorder,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )

                        // Voice Dictation Mic Button
                        IconButton(
                            onClick = {
                                if (voiceState == VoiceState.LISTENING) {
                                    viewModel.stopListening()
                                } else {
                                    requestMicAccess {
                                        viewModel.startListening()
                                    }
                                }
                            },
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(if (voiceState == VoiceState.LISTENING) CyanPrimary else CosmicSurfaceVariant)
                                .testTag("mic_input_button")
                        ) {
                            Icon(
                                imageVector = if (voiceState == VoiceState.LISTENING) Icons.Default.MicOff else Icons.Default.Mic,
                                contentDescription = "Voice input",
                                tint = if (voiceState == VoiceState.LISTENING) Color(0xFF00363F) else CyanPrimary
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // Send Button
                        IconButton(
                            onClick = { viewModel.sendMessage() },
                            enabled = !isLoading && (inputText.isNotBlank() || attachedUri != null),
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(
                                    if (!isLoading && (inputText.isNotBlank() || attachedUri != null)) {
                                        CyanPrimary
                                    } else {
                                        CosmicSurfaceVariant
                                    }
                                )
                                .testTag("send_message_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = if (!isLoading && (inputText.isNotBlank() || attachedUri != null)) {
                                    Color(0xFF00363F)
                                } else {
                                    Color.Gray
                                },
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // Full-screen Live Voice Assistant Mode
        AnimatedVisibility(
            visible = isLiveVoiceModeOpen,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            LiveVoiceModeSheet(
                voiceState = voiceState,
                rmsLevel = rmsLevel,
                partialSpeechText = partialSpeechText,
                lastAiReply = lastAiReply,
                handsFreeContinuousMode = handsFreeMode,
                onToggleHandsFree = { viewModel.toggleHandsFree(it) },
                onStartListening = {
                    requestMicAccess {
                        viewModel.startListening()
                    }
                },
                onStopListening = { viewModel.stopListening() },
                onInterruptSpeaking = { viewModel.stopSpeaking() },
                onImageSelected = { uri ->
                    val name = getFileName(context, uri) ?: "Voice image"
                    viewModel.attachFile(uri, isImage = true, fileName = name)
                },
                selectedImageUri = attachedUri,
                onClearImage = { viewModel.clearAttachment() },
                onClose = { viewModel.closeLiveVoiceMode() }
            )
        }

        // Microphone Permission Rationale & Settings Dialog
        if (showMicPermissionDialog) {
            MicrophonePermissionDialog(
                isPermanentlyDenied = isMicPermanentlyDenied,
                onRequestPermission = {
                    micPermissionLauncher.launch(PermissionManager.RECORD_AUDIO_PERMISSION)
                },
                onOpenSettings = {
                    PermissionManager.openAppSettings(context)
                },
                onDismiss = {
                    showMicPermissionDialog = false
                    pendingVoiceAction = null
                }
            )
        }

        // Settings Dialog
        if (isSettingsOpen) {
            SettingsDialog(
                currentApiKey = customApiKey,
                hasConfiguredKey = viewModel.geminiService.hasValidApiKey(customApiKey),
                onSaveApiKey = { viewModel.saveCustomApiKey(it) },
                speechRate = speechRate,
                onSpeechRateChange = { viewModel.updateSpeechRate(it) },
                speechPitch = speechPitch,
                onSpeechPitchChange = { viewModel.updateSpeechPitch(it) },
                autoSpeakReplies = autoSpeakReplies,
                onAutoSpeakChange = { viewModel.updateAutoSpeak(it) },
                onTestVoice = { viewModel.testVoice() },
                onDismiss = { viewModel.isSettingsOpen.value = false }
            )
        }
    }
}

private fun getFileName(context: android.content.Context, uri: Uri): String? {
    var result: String? = null
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index != -1) {
                    result = it.getString(index)
                }
            }
        }
    }
    if (result == null) {
        result = uri.path?.substringAfterLast('/')
    }
    return result
}

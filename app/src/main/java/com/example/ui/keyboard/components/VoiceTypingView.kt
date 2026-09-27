package com.example.ui.keyboard.components

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.keyboard.model.KeyboardColors
import java.util.Locale

@Composable
fun VoiceTypingView(
    colors: KeyboardColors,
    onInsertText: (String) -> Unit,
    onBackspace: () -> Unit,
    onEnter: () -> Unit,
    onSpace: () -> Unit,
    onBackToLetters: () -> Unit
) {
    val context = LocalContext.current
    var isListening by remember { mutableStateOf(false) }
    var recognizedText by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf("Tap microphone to speak") }
    var soundLevelRms by remember { mutableFloatStateOf(0f) }
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
        if (isGranted) {
            statusMessage = "Listening... Speak now"
            isListening = true
        } else {
            statusMessage = "Microphone permission is required for voice typing"
        }
    }

    // Speech recognizer management
    var speechRecognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }

    fun startListening() {
        if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            statusMessage = "Speech service unavailable on this device. You can use quick voice chips below."
            isListening = false
            return
        }

        try {
            speechRecognizer?.destroy()
            val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer = recognizer

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toString())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }

            recognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    isListening = true
                    statusMessage = "Listening... Speak now"
                }

                override fun onBeginningOfSpeech() {
                    statusMessage = "Hearing speech..."
                }

                override fun onRmsChanged(rmsdB: Float) {
                    soundLevelRms = rmsdB.coerceIn(0f, 10f)
                }

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    statusMessage = "Processing speech..."
                    isListening = false
                }

                override fun onError(error: Int) {
                    isListening = false
                    soundLevelRms = 0f
                    statusMessage = when (error) {
                        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                        SpeechRecognizer.ERROR_CLIENT -> "Speech service ready. Tap mic to speak."
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required"
                        SpeechRecognizer.ERROR_NETWORK -> "Network error connecting to speech service"
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout. Tap mic to retry."
                        SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized. Tap mic to speak."
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech service busy"
                        SpeechRecognizer.ERROR_SERVER -> "Server error. Tap to retry."
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech heard. Tap mic to speak."
                        else -> "Tap microphone to speak"
                    }
                }

                override fun onResults(results: Bundle?) {
                    isListening = false
                    soundLevelRms = 0f
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull()?.trim()
                    if (!text.isNullOrEmpty()) {
                        recognizedText = text
                        statusMessage = "Transcribed: \"$text\""
                        onInsertText("$text ")
                    } else {
                        statusMessage = "Tap microphone to speak"
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val partial = matches?.firstOrNull()?.trim()
                    if (!partial.isNullOrEmpty()) {
                        recognizedText = partial
                        statusMessage = "Listening: $partial"
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            recognizer.startListening(intent)
            isListening = true
            statusMessage = "Listening... Speak now"
        } catch (e: Exception) {
            isListening = false
            statusMessage = "Tap mic to speak (${e.message ?: "Ready"})"
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
        } catch (_: Exception) {}
        isListening = false
        soundLevelRms = 0f
        statusMessage = "Stopped. Tap microphone to speak."
    }

    // Auto-start listening when entering voice typing view if permission is granted
    LaunchedEffect(Unit) {
        if (hasPermission) {
            startListening()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                speechRecognizer?.stopListening()
                speechRecognizer?.destroy()
            } catch (_: Exception) {}
        }
    }

    // Pulse animation for the microphone
    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isListening) 1.18f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
            .background(colors.background)
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag("view_voice_typing"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top row: Title, status indicator, back to keyboard button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isListening) Color(0xFF4CAF50) else colors.letterKeySecondaryTextColor)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isListening) "VOICE TYPING ACTIVE" else "VOICE TYPING",
                    color = colors.letterKeyTextColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(colors.functionKeyBackground)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "EN-US",
                        color = colors.letterKeySecondaryTextColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        stopListening()
                        onBackToLetters()
                    },
                    modifier = Modifier
                        .size(34.dp)
                        .testTag("btn_voice_back_to_keyboard")
                ) {
                    Icon(
                        imageVector = Icons.Default.Keyboard,
                        contentDescription = "Return to Keyboard",
                        tint = colors.letterKeyTextColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Live transcription / status card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 4.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(colors.toolbarBackground)
                .border(
                    width = 1.dp,
                    color = if (isListening) colors.enterKeyBackground else colors.functionKeyBackground.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(10.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (recognizedText.isNotEmpty()) {
                    Text(
                        text = "\"$recognizedText\"",
                        color = colors.letterKeyTextColor,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                Text(
                    text = statusMessage,
                    color = if (isListening) colors.enterKeyBackground else colors.letterKeySecondaryTextColor,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Sound level visualizer bars (when listening)
                if (isListening) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val barCount = 7
                        for (i in 0 until barCount) {
                            val factor = ((i - barCount / 2).let { it * it }) / 4f
                            val barHeight = (8.dp + ((soundLevelRms * 2.2f) - factor).coerceAtLeast(0f).dp)
                                .coerceIn(6.dp, 22.dp)
                            Box(
                                modifier = Modifier
                                    .width(3.5.dp)
                                    .height(barHeight)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(colors.enterKeyBackground)
                            )
                        }
                    }
                }
            }
        }

        // Central Microphone Control Button
        Box(
            modifier = Modifier
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            // Ripple background when active
            if (isListening) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(colors.enterKeyBackground.copy(alpha = 0.25f))
                )
            }

            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(if (isListening) colors.enterKeyBackground else colors.functionKeyBackground)
                    .clickable {
                        if (isListening) {
                            stopListening()
                        } else {
                            startListening()
                        }
                    }
                    .testTag("btn_voice_type_mic_toggle"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicOff,
                    contentDescription = if (isListening) "Mute Microphone" else "Start Microphone",
                    tint = if (isListening) colors.enterKeyTextColor else colors.letterKeyTextColor,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        // Quick Speech Templates / Test Phrases (one-tap voice samples)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val quickPhrases = listOf(
                "Hello! 👋",
                "How are you?",
                "Sounds good!",
                "Thank you!",
                "See you soon.",
                "I'm on my way."
            )
            quickPhrases.forEach { phrase ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.letterKeyBackground)
                        .clickable {
                            onInsertText("$phrase ")
                            recognizedText = phrase
                            statusMessage = "Typed: \"$phrase\""
                        }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                        .testTag("chip_voice_phrase_$phrase")
                ) {
                    Text(
                        text = phrase,
                        color = colors.letterKeyTextColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }
        }

        // Quick Punctuation & Editing Bar (Comma, Period, Question, Space, Backspace, Enter)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Punctuation buttons
            listOf(",", ".", "?", "!").forEach { mark ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.letterKeyBackground)
                        .clickable { onInsertText("$mark ") }
                        .testTag("btn_voice_punct_$mark"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = mark,
                        color = colors.letterKeyTextColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Space button
            Box(
                modifier = Modifier
                    .weight(2.2f)
                    .height(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.letterKeyBackground)
                    .clickable { onSpace() }
                    .testTag("btn_voice_space"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Space",
                    color = colors.letterKeyTextColor,
                    fontSize = 13.sp
                )
            }

            // Backspace button
            Box(
                modifier = Modifier
                    .weight(1.3f)
                    .height(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.functionKeyBackground)
                    .clickable { onBackspace() }
                    .testTag("btn_voice_backspace"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                    contentDescription = "Backspace",
                    tint = colors.functionKeyTextColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Enter / Done button
            Box(
                modifier = Modifier
                    .weight(1.3f)
                    .height(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.enterKeyBackground)
                    .clickable {
                        stopListening()
                        onEnter()
                        onBackToLetters()
                    }
                    .testTag("btn_voice_enter"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Done",
                    tint = colors.enterKeyTextColor,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

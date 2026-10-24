package com.example.ui.keyboard.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.automirrored.filled.KeyboardReturn
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.SpaceBar
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppDatabase
import com.example.data.WordSuggestionManager
import com.example.ui.keyboard.glide.HandwritingRecognizer
import com.example.ui.keyboard.model.KeyboardColors
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun GlideTypingView(
    colors: KeyboardColors,
    onInsertText: (String) -> Unit,
    onBackspace: () -> Unit,
    onEnter: () -> Unit,
    onSpace: () -> Unit,
    onBackToLetters: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val wordSuggestionManager = remember {
        WordSuggestionManager(AppDatabase.getDatabase(context).wordFrequencyDao())
    }

    // List of completed strokes: each stroke is a list of Offset points
    val completedStrokes = remember { mutableStateListOf<List<Offset>>() }
    var currentStrokePoints by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var activeTouchPoint by remember { mutableStateOf<Offset?>(null) }

    // Recognition results
    var recognizedWord by remember { mutableStateOf("") }
    var suggestedWords by remember { mutableStateOf<List<String>>(emptyList()) }
    var recognizedLetters by remember { mutableStateOf<List<Char>>(emptyList()) }
    var candidateLetters by remember { mutableStateOf<List<HandwritingRecognizer.LetterCandidate>>(emptyList()) }

    var recognitionJob by remember { mutableStateOf<Job?>(null) }

    // Core English vocabulary for dictionary ranking
    val commonVocabulary = remember {
        listOf(
            "the", "be", "to", "of", "and", "a", "in", "that", "have", "i", "it", "for", "not",
            "on", "with", "he", "as", "you", "do", "at", "this", "but", "his", "by", "from",
            "they", "we", "say", "her", "she", "or", "an", "will", "my", "one", "all", "would",
            "there", "their", "what", "so", "up", "out", "if", "about", "who", "get", "which",
            "go", "me", "when", "make", "can", "like", "time", "no", "just", "him", "know",
            "take", "people", "into", "year", "your", "good", "some", "could", "them", "see",
            "other", "than", "then", "now", "look", "only", "come", "its", "over", "think",
            "also", "back", "after", "use", "two", "how", "our", "work", "first", "well",
            "way", "even", "new", "want", "because", "any", "these", "give", "day", "most",
            "us", "hello", "thanks", "thank", "please", "yes", "yeah", "ok", "okay", "hi",
            "cat", "dog", "sun", "car", "run", "fast", "love", "help", "hope", "fine",
            "great", "cool", "nice", "call", "send", "chat", "type", "pen", "write", "note",
            "today", "tomorrow", "night", "morning", "home", "friend", "happy", "smile"
        )
    }

    fun triggerRecognition() {
        recognitionJob?.cancel()
        recognitionJob = coroutineScope.launch {
            // Fast responsive debounce after lifting finger/stylus
            delay(170)
            if (completedStrokes.isNotEmpty()) {
                val result = HandwritingRecognizer.recognize(completedStrokes, commonVocabulary)
                if (result.primaryWord.isNotBlank() || result.candidateLetters.isNotEmpty()) {
                    recognizedWord = result.primaryWord
                    recognizedLetters = result.recognizedLetters
                    candidateLetters = result.candidateLetters

                    // Query word completion manager for expanded words
                    val moreSuggestions = wordSuggestionManager.getSuggestions(result.primaryWord, "en")
                    val combined = mutableListOf<String>()
                    result.suggestions.forEach { if (!combined.contains(it)) combined.add(it) }
                    moreSuggestions.forEach { if (!combined.contains(it) && !it.equals(recognizedWord, ignoreCase = true)) combined.add(it) }
                    suggestedWords = combined.take(6)
                }
            }
        }
    }

    fun clearPad() {
        completedStrokes.clear()
        currentStrokePoints = emptyList()
        activeTouchPoint = null
        recognizedWord = ""
        suggestedWords = emptyList()
        recognizedLetters = emptyList()
        candidateLetters = emptyList()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.background)
    ) {
        // 1. TOP RECOGNIZED WORD & CANDIDATES BAR (User requirement: show word on top, click to insert)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            color = colors.toolbarBackground,
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (recognizedWord.isNotBlank() || candidateLetters.isNotEmpty()) {
                    // Highlighted primary recognized word/letter button
                    val displayWord = if (recognizedWord.isNotBlank()) recognizedWord else candidateLetters.firstOrNull()?.char?.toString() ?: ""
                    Button(
                        onClick = {
                            val insertText = if (displayWord.length == 1) displayWord else "$displayWord "
                            onInsertText(insertText)
                            coroutineScope.launch { wordSuggestionManager.recordWordUsed(displayWord) }
                            clearPad()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.enterKeyBackground,
                            contentColor = colors.enterKeyTextColor
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .height(38.dp)
                            .testTag("btn_insert_recognized_word")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = displayWord,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Horizontal scrollable list of alternative letter & word suggestion chips
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Letter candidate chips for direct single-letter tap insertion
                        candidateLetters.take(4).forEach { candidate ->
                            AssistChip(
                                onClick = {
                                    onInsertText(candidate.char.toString())
                                    clearPad()
                                },
                                label = {
                                    Text(
                                        text = "${candidate.char}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.letterKeyTextColor
                                    )
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = colors.letterKeyBackground
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("chip_letter_${candidate.char}")
                            )
                        }

                        // Word suggestions
                        suggestedWords.forEach { candidate ->
                            AssistChip(
                                onClick = {
                                    onInsertText("$candidate ")
                                    coroutineScope.launch { wordSuggestionManager.recordWordUsed(candidate) }
                                    clearPad()
                                },
                                label = {
                                    Text(
                                        text = candidate,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = colors.letterKeyTextColor
                                    )
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = colors.letterKeyBackground
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("chip_suggestion_$candidate")
                            )
                        }
                    }

                    // Quick Clear Button on Top Right
                    IconButton(
                        onClick = { clearPad() },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("btn_clear_writing_top")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear Pad",
                            tint = colors.letterKeySecondaryTextColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else {
                    // Empty state instruction with pen icon
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Draw,
                            contentDescription = null,
                            tint = colors.enterKeyBackground,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Write with finger or stylus to recognize words",
                            fontSize = 12.5.sp,
                            color = colors.letterKeySecondaryTextColor
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        // Quick starter word chips
                        listOf("Hello", "Thanks", "Yes", "The", "I").forEach { starter ->
                            AssistChip(
                                onClick = {
                                    onInsertText("$starter ")
                                    clearPad()
                                },
                                label = {
                                    Text(
                                        text = starter,
                                        fontSize = 12.sp,
                                        color = colors.letterKeyTextColor
                                    )
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = colors.letterKeyBackground
                                ),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.height(28.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                    }
                }
            }
        }

        val configuration = androidx.compose.ui.platform.LocalConfiguration.current
        val isTablet = configuration.screenWidthDp >= 600 || configuration.smallestScreenWidthDp >= 600
        val canvasHeight = if (isTablet) 340.dp else 265.dp

        // 2. MAIN WRITING CANVAS AREA (Expanded touch & writing surface)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(canvasHeight)
                .background(colors.background)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            activeTouchPoint = offset
                            currentStrokePoints = listOf(offset)
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            activeTouchPoint = change.position
                            currentStrokePoints = currentStrokePoints + change.position
                        },
                        onDragEnd = {
                            activeTouchPoint = null
                            if (currentStrokePoints.size >= 2) {
                                completedStrokes.add(currentStrokePoints)
                                currentStrokePoints = emptyList()
                                triggerRecognition()
                            }
                        },
                        onDragCancel = {
                            activeTouchPoint = null
                            currentStrokePoints = emptyList()
                        }
                    )
                }
                .testTag("handwriting_canvas_pad")
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasWidth = size.width
                val canvasHeight = size.height

                // Draw ruled guidelines for neat handwriting (waistline at 38%, baseline at 68%)
                val waistlineY = canvasHeight * 0.38f
                val baselineY = canvasHeight * 0.68f

                val dashedEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)

                drawLine(
                    color = Color.Gray.copy(alpha = 0.22f),
                    start = Offset(24f, waistlineY),
                    end = Offset(canvasWidth - 24f, waistlineY),
                    strokeWidth = 1.5f,
                    pathEffect = dashedEffect
                )

                drawLine(
                    color = Color.Gray.copy(alpha = 0.35f),
                    start = Offset(24f, baselineY),
                    end = Offset(canvasWidth - 24f, baselineY),
                    strokeWidth = 2f
                )

                // Render completed strokes with smooth Bézier curves
                val strokeColor = colors.enterKeyBackground
                val strokePaint = Stroke(
                    width = 6.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )

                completedStrokes.forEach { stroke ->
                    if (stroke.size >= 2) {
                        val path = Path()
                        path.moveTo(stroke.first().x, stroke.first().y)
                        for (i in 0 until stroke.size - 1) {
                            val p0 = stroke[i]
                            val p1 = stroke[i + 1]
                            val midX = (p0.x + p1.x) / 2f
                            val midY = (p0.y + p1.y) / 2f
                            path.quadraticTo(p0.x, p0.y, midX, midY)
                        }
                        path.lineTo(stroke.last().x, stroke.last().y)
                        drawPath(path, color = strokeColor, style = strokePaint)
                    }
                }

                // Render active stroke currently being drawn
                if (currentStrokePoints.size >= 2) {
                    val path = Path()
                    path.moveTo(currentStrokePoints.first().x, currentStrokePoints.first().y)
                    for (i in 0 until currentStrokePoints.size - 1) {
                        val p0 = currentStrokePoints[i]
                        val p1 = currentStrokePoints[i + 1]
                        val midX = (p0.x + p1.x) / 2f
                        val midY = (p0.y + p1.y) / 2f
                        path.quadraticTo(p0.x, p0.y, midX, midY)
                    }
                    path.lineTo(currentStrokePoints.last().x, currentStrokePoints.last().y)
                    drawPath(path, color = strokeColor, style = strokePaint)
                }

                // Glowing pen tip indicator while drawing
                activeTouchPoint?.let { pt ->
                    drawCircle(
                        color = strokeColor.copy(alpha = 0.4f),
                        radius = 12.dp.toPx(),
                        center = pt
                    )
                    drawCircle(
                        color = strokeColor,
                        radius = 4.dp.toPx(),
                        center = pt
                    )
                }
            }

            // Canvas Watermark / Hint when empty
            if (completedStrokes.isEmpty() && currentStrokePoints.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Draw,
                            contentDescription = null,
                            tint = colors.letterKeySecondaryTextColor.copy(alpha = 0.4f),
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Glide with finger or stylus here",
                            fontSize = 13.sp,
                            color = colors.letterKeySecondaryTextColor.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }

        // 3. BOTTOM UTILITY & EDITING BAR
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(if (isTablet) 56.dp else 48.dp),
            color = colors.toolbarBackground
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Switch back to normal keyboard
                IconButton(
                    onClick = onBackToLetters,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .testTag("btn_back_to_keyboard")
                ) {
                    Icon(
                        imageVector = Icons.Default.Keyboard,
                        contentDescription = "Switch to Keyboard",
                        tint = colors.toolbarIconTint,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Undo last stroke
                IconButton(
                    onClick = {
                        if (completedStrokes.isNotEmpty()) {
                            completedStrokes.removeAt(completedStrokes.size - 1)
                            if (completedStrokes.isEmpty()) {
                                recognizedWord = ""
                                suggestedWords = emptyList()
                                recognizedLetters = emptyList()
                            } else {
                                triggerRecognition()
                            }
                        }
                    },
                    enabled = completedStrokes.isNotEmpty(),
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("btn_undo_stroke")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Undo,
                        contentDescription = "Undo Stroke",
                        tint = if (completedStrokes.isNotEmpty()) colors.toolbarIconTint else colors.letterKeySecondaryTextColor.copy(alpha = 0.4f),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Clear Pad
                IconButton(
                    onClick = { clearPad() },
                    enabled = completedStrokes.isNotEmpty(),
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("btn_clear_canvas")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Clear Canvas",
                        tint = if (completedStrokes.isNotEmpty()) colors.toolbarIconTint else colors.letterKeySecondaryTextColor.copy(alpha = 0.4f),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Space button
                Button(
                    onClick = onSpace,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.letterKeyBackground,
                        contentColor = colors.letterKeyTextColor
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .height(36.dp)
                        .testTag("btn_glide_space")
                ) {
                    Icon(
                        imageVector = Icons.Default.SpaceBar,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Space", fontSize = 12.sp)
                }

                // Backspace button
                IconButton(
                    onClick = onBackspace,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("btn_glide_backspace")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Backspace,
                        contentDescription = "Backspace",
                        tint = colors.toolbarIconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Enter button
                IconButton(
                    onClick = onEnter,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.enterKeyBackground)
                        .testTag("btn_glide_enter")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardReturn,
                        contentDescription = "Enter",
                        tint = colors.enterKeyTextColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

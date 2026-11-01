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
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.SpaceBar
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import com.example.ui.keyboard.util.KeyboardPreferences
import com.google.mlkit.vision.digitalink.Ink
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class HandwritingStrokePoint(
    val x: Float,
    val y: Float,
    val timestamp: Long
)

@Composable
fun GlideTypingView(
    colors: KeyboardColors,
    onInsertText: (String) -> Unit,
    onBackspace: () -> Unit,
    onEnter: () -> Unit,
    onSpace: () -> Unit,
    onBackToLetters: () -> Unit,
    languageOverride: String? = null,
    preContext: String = ""
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val keyboardPrefs = remember { KeyboardPreferences.getInstance(context) }
    val currentLangPref by keyboardPrefs.currentLanguage.collectAsState()
    val activeLanguage = languageOverride ?: currentLangPref

    val wordSuggestionManager = remember {
        WordSuggestionManager(
            AppDatabase.getDatabase(context).wordFrequencyDao(),
            AppDatabase.getDatabase(context).bigramFrequencyDao()
        )
    }

    // Completed strokes: each stroke is a list of timestamped points
    val completedStrokes = remember { mutableStateListOf<List<HandwritingStrokePoint>>() }
    var currentStrokePoints by remember { mutableStateOf<List<HandwritingStrokePoint>>(emptyList()) }
    var activeTouchPoint by remember { mutableStateOf<Offset?>(null) }

    // Recognition state
    var recognizedWord by remember { mutableStateOf("") }
    var suggestedWords by remember { mutableStateOf<List<String>>(emptyList()) }
    var recognizedLetters by remember { mutableStateOf<List<Char>>(emptyList()) }
    var candidateLetters by remember { mutableStateOf<List<HandwritingRecognizer.LetterCandidate>>(emptyList()) }
    var rawTopMlKitWord by remember { mutableStateOf("") }

    // Model download state
    var modelStatus by remember { mutableStateOf<HandwritingRecognizer.ModelDownloadState>(HandwritingRecognizer.ModelDownloadState.NotDownloaded) }
    var isCheckingModel by remember { mutableStateOf(true) }

    var recognitionJob by remember { mutableStateOf<Job?>(null) }
    var padWidthPx by remember { mutableStateOf(800f) }
    var padHeightPx by remember { mutableStateOf(400f) }

    // Check model download status whenever the active language changes
    LaunchedEffect(activeLanguage) {
        isCheckingModel = true
        val downloaded = HandwritingRecognizer.checkModelDownloaded(activeLanguage)
        modelStatus = if (downloaded) {
            HandwritingRecognizer.ModelDownloadState.Downloaded
        } else {
            HandwritingRecognizer.ModelDownloadState.NotDownloaded
        }
        isCheckingModel = false
    }

    // Proper disposal of recognizer on unmount
    DisposableEffect(Unit) {
        onDispose {
            recognitionJob?.cancel()
            HandwritingRecognizer.close()
        }
    }

    fun triggerRecognition() {
        recognitionJob?.cancel()
        recognitionJob = coroutineScope.launch {
            // Debounce: run recognition ~400 ms after the finger lifts
            delay(400)
            if (completedStrokes.isNotEmpty()) {
                val inkBuilder = Ink.builder()
                for (stroke in completedStrokes) {
                    if (stroke.isEmpty()) continue
                    val strokeBuilder = Ink.Stroke.builder()
                    for (pt in stroke) {
                        strokeBuilder.addPoint(Ink.Point.create(pt.x, pt.y, pt.timestamp))
                    }
                    inkBuilder.addStroke(strokeBuilder.build())
                }
                val ink = inkBuilder.build()

                // Fetch user words for ranking
                val userWords = try {
                    withContext(Dispatchers.IO) {
                        AppDatabase.getDatabase(context).wordFrequencyDao().getTopWords(limit = 30).map { it.word }
                    }
                } catch (_: Exception) {
                    emptyList()
                }

                val result = HandwritingRecognizer.recognizeInk(
                    ink = ink,
                    language = activeLanguage,
                    writingWidth = padWidthPx,
                    writingHeight = padHeightPx,
                    preContext = preContext,
                    userHistoryWords = userWords
                )

                if (result.primaryWord.isNotBlank() || result.candidateLetters.isNotEmpty()) {
                    recognizedWord = result.primaryWord
                    recognizedLetters = result.recognizedLetters
                    candidateLetters = result.candidateLetters
                    rawTopMlKitWord = result.rawCandidates.firstOrNull() ?: result.primaryWord

                    // Combine ML Kit alternative candidates and word suggestions
                    val combined = mutableListOf<String>()
                    result.suggestions.forEach { if (!combined.contains(it)) combined.add(it) }

                    // Also fetch word completions if single word
                    if (result.primaryWord.length >= 2) {
                        val moreSuggestions = wordSuggestionManager.getSuggestions(result.primaryWord, activeLanguage)
                        moreSuggestions.forEach {
                            if (!combined.contains(it) && !it.equals(recognizedWord, ignoreCase = true)) {
                                combined.add(it)
                            }
                        }
                    }
                    suggestedWords = combined.take(8)
                }
            }
        }
    }

    fun clearPad() {
        recognitionJob?.cancel()
        completedStrokes.clear()
        currentStrokePoints = emptyList()
        activeTouchPoint = null
        recognizedWord = ""
        suggestedWords = emptyList()
        recognizedLetters = emptyList()
        candidateLetters = emptyList()
        rawTopMlKitWord = ""
    }

    fun startModelDownload() {
        coroutineScope.launch {
            HandwritingRecognizer.downloadModel(activeLanguage) { state ->
                modelStatus = state
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.background)
    ) {
        // 1. TOP BAR: Display primary recognized word + suggestion chips + model status
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
                // If model is currently downloading or not downloaded, show banner/button
                if (modelStatus is HandwritingRecognizer.ModelDownloadState.Downloading) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = colors.enterKeyBackground,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Downloading ${if (activeLanguage == "si") "Sinhala" else "English"} handwriting model...",
                            fontSize = 12.sp,
                            color = colors.letterKeySecondaryTextColor
                        )
                    }
                } else if (modelStatus is HandwritingRecognizer.ModelDownloadState.NotDownloaded) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = null,
                            tint = colors.enterKeyBackground,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${if (activeLanguage == "si") "Sinhala" else "English"} Model:",
                            fontSize = 12.sp,
                            color = colors.letterKeySecondaryTextColor
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        AssistChip(
                            onClick = { startModelDownload() },
                            label = {
                                Text(
                                    text = "Download Model (Offline ML Kit)",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.enterKeyTextColor
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = colors.enterKeyBackground
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(30.dp)
                        )
                    }
                } else if (recognizedWord.isNotBlank() || candidateLetters.isNotEmpty()) {
                    // Highlighted primary recognized word button
                    val displayWord = if (recognizedWord.isNotBlank()) recognizedWord else candidateLetters.firstOrNull()?.char?.toString() ?: ""
                    Button(
                        onClick = {
                            // Single character inserts without trailing space; multi-character inserts with space
                            val insertText = if (displayWord.length <= 1) displayWord else "$displayWord "
                            onInsertText(insertText)
                            coroutineScope.launch {
                                wordSuggestionManager.recordWordUsed(displayWord)
                            }
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

                    // Horizontal alternative candidate chips
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Candidate letter chips
                        candidateLetters.take(3).forEach { candidate ->
                            AssistChip(
                                onClick = {
                                    val charStr = candidate.char.toString()
                                    onInsertText(charStr)
                                    // Personalization: record picked candidate
                                    HandwritingRecognizer.recordCandidatePicked(charStr)
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

                        // Word suggestions / candidates
                        suggestedWords.forEach { candidate ->
                            AssistChip(
                                onClick = {
                                    val insertText = if (candidate.length <= 1) candidate else "$candidate "
                                    onInsertText(insertText)
                                    // Personalization boost if user picked a non-top candidate
                                    if (!candidate.equals(rawTopMlKitWord, ignoreCase = true)) {
                                        HandwritingRecognizer.recordCandidatePicked(candidate)
                                    }
                                    coroutineScope.launch {
                                        wordSuggestionManager.recordWordUsed(candidate)
                                    }
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

                    // Top right clear button
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
                    // Empty state instruction
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
                            text = "Handwriting Recognition (${if (activeLanguage == "si") "සිංහල" else "English"})",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.letterKeyTextColor
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        val quickStarters = if (activeLanguage == "si") {
                            listOf("ආයුබෝවන්", "ස්තූතියි", "ඔව්", "මම")
                        } else {
                            listOf("Hello", "Thanks", "Yes", "The", "I")
                        }

                        quickStarters.forEach { starter ->
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

        // 2. MAIN HANDWRITING CANVAS AREA
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(canvasHeight)
                .background(colors.background)
                .pointerInput(Unit) {
                    padWidthPx = size.width.toFloat()
                    padHeightPx = size.height.toFloat()
                    detectDragGestures(
                        onDragStart = { offset ->
                            recognitionJob?.cancel() // Cancel pending recognition when new stroke starts
                            val now = System.currentTimeMillis()
                            activeTouchPoint = offset
                            currentStrokePoints = listOf(HandwritingStrokePoint(offset.x, offset.y, now))
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val now = System.currentTimeMillis()
                            activeTouchPoint = change.position
                            currentStrokePoints = currentStrokePoints + HandwritingStrokePoint(change.position.x, change.position.y, now)
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

                // Ruled guidelines
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

                // Smooth Bezier rendering of completed strokes
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
                            text = "Write words or letters with finger or stylus",
                            fontSize = 13.sp,
                            color = colors.letterKeySecondaryTextColor.copy(alpha = 0.6f)
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

                // Undo last stroke button
                IconButton(
                    onClick = {
                        if (completedStrokes.isNotEmpty()) {
                            completedStrokes.removeAt(completedStrokes.size - 1)
                            if (completedStrokes.isEmpty()) {
                                recognizedWord = ""
                                suggestedWords = emptyList()
                                recognizedLetters = emptyList()
                                candidateLetters = emptyList()
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
                        contentDescription = "Undo Last Stroke",
                        tint = if (completedStrokes.isNotEmpty()) colors.toolbarIconTint else colors.letterKeySecondaryTextColor.copy(alpha = 0.4f),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Clear Pad button
                IconButton(
                    onClick = { clearPad() },
                    enabled = completedStrokes.isNotEmpty(),
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("btn_clear_canvas")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Clear Handwriting Canvas",
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

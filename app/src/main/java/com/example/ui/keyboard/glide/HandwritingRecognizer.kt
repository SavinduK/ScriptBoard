package com.example.ui.keyboard.glide

import androidx.compose.ui.geometry.Offset
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.vision.digitalink.DigitalInkRecognition
import com.google.mlkit.vision.digitalink.DigitalInkRecognitionModel
import com.google.mlkit.vision.digitalink.DigitalInkRecognitionModelIdentifier
import com.google.mlkit.vision.digitalink.DigitalInkRecognizer
import com.google.mlkit.vision.digitalink.DigitalInkRecognizerOptions
import com.google.mlkit.vision.digitalink.Ink
import com.google.mlkit.vision.digitalink.RecognitionCandidate as MlKitRecognitionCandidate
import com.google.mlkit.vision.digitalink.RecognitionResult as MlKitRecognitionResult
import com.google.mlkit.vision.digitalink.RecognitionContext
import com.google.mlkit.vision.digitalink.WritingArea
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * Google ML Kit Digital Ink Recognition Engine for KeyPro Keyboard.
 * Supports English (en-US) and Sinhala (si).
 * Handles on-demand model downloads via RemoteModelManager, candidate re-ranking,
 * personalized learning for user-picked candidates, and graceful offline fallback.
 */
object HandwritingRecognizer {

    data class TimedPoint(val x: Float, val y: Float, val timestamp: Long = System.currentTimeMillis())

    data class LetterCandidate(
        val char: Char,
        val score: Float = 0f
    )

    data class RecognitionResult(
        val primaryWord: String,
        val suggestions: List<String>,
        val rawCandidates: List<String> = emptyList(),
        val recognizedLetters: List<Char> = emptyList(),
        val candidateLetters: List<LetterCandidate> = emptyList()
    )

    sealed class ModelDownloadState {
        object NotDownloaded : ModelDownloadState()
        data class Downloading(val progress: Float? = null) : ModelDownloadState()
        object Downloaded : ModelDownloadState()
        data class Error(val message: String) : ModelDownloadState()
    }

    private val remoteModelManager by lazy { RemoteModelManager.getInstance() }
    private val recognizerCache = ConcurrentHashMap<String, DigitalInkRecognizer>()
    private val modelCache = ConcurrentHashMap<String, DigitalInkRecognitionModel>()
    private val modelStatusCache = ConcurrentHashMap<String, ModelDownloadState>()

    // Personalization: when user picks a non-top candidate, boost its ranking for future sessions
    private val personalizedPicks = ConcurrentHashMap<String, Int>()

    fun normalizeLanguageTag(lang: String): String {
        return when (lang.lowercase()) {
            "si", "sinhala" -> "si"
            else -> "en-US"
        }
    }

    private fun getModel(languageTag: String): DigitalInkRecognitionModel? {
        val norm = normalizeLanguageTag(languageTag)
        modelCache[norm]?.let { return it }

        val modelIdentifier = try {
            DigitalInkRecognitionModelIdentifier.fromLanguageTag(norm)
                ?: DigitalInkRecognitionModelIdentifier.fromLanguageTag("en-US")
        } catch (_: Exception) {
            null
        } ?: return null

        val model = DigitalInkRecognitionModel.builder(modelIdentifier).build()
        modelCache[norm] = model
        return model
    }

    /**
     * Checks whether the ML Kit digital ink model for the requested language is downloaded.
     */
    suspend fun checkModelDownloaded(language: String): Boolean = withContext(Dispatchers.IO) {
        val model = getModel(language) ?: return@withContext false
        try {
            val task = remoteModelManager.isModelDownloaded(model)
            val downloaded = Tasks.await(task)
            val state = if (downloaded) ModelDownloadState.Downloaded else ModelDownloadState.NotDownloaded
            modelStatusCache[normalizeLanguageTag(language)] = state
            downloaded
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Initiates downloading the ML Kit Digital Ink Recognition model for English or Sinhala.
     */
    suspend fun downloadModel(
        language: String,
        onStatusChange: (ModelDownloadState) -> Unit
    ) = withContext(Dispatchers.IO) {
        val norm = normalizeLanguageTag(language)
        val model = getModel(language)
        if (model == null) {
            val err = ModelDownloadState.Error("Unsupported language identifier: $language")
            modelStatusCache[norm] = err
            onStatusChange(err)
            return@withContext
        }

        try {
            onStatusChange(ModelDownloadState.Downloading(null))
            modelStatusCache[norm] = ModelDownloadState.Downloading(null)

            val conditions = DownloadConditions.Builder().build()
            val downloadTask = remoteModelManager.download(model, conditions)
            Tasks.await(downloadTask)

            val success = ModelDownloadState.Downloaded
            modelStatusCache[norm] = success
            onStatusChange(success)
        } catch (e: Exception) {
            val err = ModelDownloadState.Error(e.message ?: "Model download failed")
            modelStatusCache[norm] = err
            onStatusChange(err)
        }
    }

    /**
     * Gets or creates a DigitalInkRecognizer for the specified language.
     */
    private fun getRecognizer(language: String): DigitalInkRecognizer? {
        val norm = normalizeLanguageTag(language)
        recognizerCache[norm]?.let { return it }

        val model = getModel(language) ?: return null
        val options = DigitalInkRecognizerOptions.builder(model).build()
        val recognizer = DigitalInkRecognition.getClient(options)
        recognizerCache[norm] = recognizer
        return recognizer
    }

    /**
     * Records when the user picks a candidate that was not the top ML Kit candidate,
     * boosting its ranking score for future recognitions.
     */
    fun recordCandidatePicked(word: String) {
        val clean = word.trim().lowercase()
        if (clean.isNotBlank()) {
            val current = personalizedPicks[clean] ?: 0
            personalizedPicks[clean] = (current + 1).coerceAtMost(10)
        }
    }

    /**
     * Recognizes text from an ML Kit Ink object.
     * Re-ranks candidates using user word history, built-in dictionary, and personalization,
     * while preserving the raw top ML Kit result so names and slang survive.
     */
    suspend fun recognizeInk(
        ink: Ink,
        language: String = "en",
        writingWidth: Float = 800f,
        writingHeight: Float = 400f,
        preContext: String = "",
        knownVocabulary: List<String> = emptyList(),
        userHistoryWords: List<String> = emptyList()
    ): RecognitionResult = withContext(Dispatchers.Default) {
        if (ink.strokes.isEmpty()) {
            return@withContext RecognitionResult(
                primaryWord = "",
                suggestions = emptyList(),
                rawCandidates = emptyList(),
                recognizedLetters = emptyList(),
                candidateLetters = emptyList()
            )
        }

        val recognizer = getRecognizer(language)
        if (recognizer == null) {
            return@withContext fallbackHeuristic(ink)
        }

        val mlKitResult: MlKitRecognitionResult? = try {
            val contextBuilder = RecognitionContext.builder()
                .setWritingArea(WritingArea(writingWidth.coerceAtLeast(100f), writingHeight.coerceAtLeast(100f)))
            if (preContext.isNotBlank()) {
                contextBuilder.setPreContext(preContext.takeLast(20))
            }
            val task = recognizer.recognize(ink, contextBuilder.build())
            Tasks.await(task)
        } catch (_: Exception) {
            null
        }

        val mlKitCandidates: List<MlKitRecognitionCandidate> = mlKitResult?.candidates ?: emptyList()
        if (mlKitCandidates.isEmpty()) {
            return@withContext fallbackHeuristic(ink)
        }

        val rawTopCandidate = mlKitCandidates.first().text
        val rawCandidateTexts = mlKitCandidates.map { it.text }.distinct()

        // Re-ranking step:
        // Boost candidates matching user history, dictionary, or personal picks,
        // but always keep the raw top ML Kit result available so slang and proper nouns survive.
        val lowerUserHistory = userHistoryWords.map { it.lowercase() }.toSet()
        val lowerVocabulary = knownVocabulary.map { it.lowercase() }.toSet()

        data class ScoredCandidate(val text: String, val score: Float)

        val scoredList = mutableListOf<ScoredCandidate>()
        for ((index, item) in mlKitCandidates.withIndex()) {
            val text = item.text
            val lower = text.lowercase()
            // ML Kit rank score (100 down to 10)
            var score = 100f - (index * 8f)

            // Boost for matching user history
            if (lowerUserHistory.contains(lower)) {
                score += 35f
            }
            // Boost for dictionary word
            if (lowerVocabulary.contains(lower)) {
                score += 20f
            }
            // Personalization boost if user previously picked this word
            val personalBoost = personalizedPicks[lower] ?: 0
            if (personalBoost > 0) {
                score += personalBoost * 8f
            }

            scoredList.add(ScoredCandidate(text, score))
        }

        // Sort descending by score
        val sortedCandidates = scoredList.sortedByDescending { it.score }.map { it.text }.distinct()

        // Ensure raw top candidate is always included
        val finalCandidates = mutableListOf<String>()
        if (sortedCandidates.isNotEmpty()) {
            finalCandidates.addAll(sortedCandidates)
            if (!finalCandidates.contains(rawTopCandidate)) {
                finalCandidates.add(1.coerceAtMost(finalCandidates.size), rawTopCandidate)
            }
        } else {
            finalCandidates.add(rawTopCandidate)
        }

        val primary = finalCandidates.firstOrNull() ?: rawTopCandidate
        val suggestions = finalCandidates.filter { it != primary }

        val recognizedChars = primary.toList()
        val letterCandidates = mlKitCandidates
            .filter { it.text.length == 1 }
            .map { LetterCandidate(it.text[0], it.score ?: 0f) }

        RecognitionResult(
            primaryWord = primary,
            suggestions = suggestions,
            rawCandidates = rawCandidateTexts,
            recognizedLetters = recognizedChars,
            candidateLetters = letterCandidates
        )
    }

    /**
     * Fallback heuristic recognition when offline and ML Kit model is not yet downloaded,
     * detecting basic gestures like circles ('o'), vertical lines ('l'/'1'), and horizontal strokes per stroke.
     */
    private fun fallbackHeuristic(ink: Ink): RecognitionResult {
        if (ink.strokes.isEmpty()) {
            return RecognitionResult("", emptyList(), emptyList(), emptyList(), emptyList())
        }

        val recognizedChars = mutableListOf<Char>()
        for (stroke in ink.strokes) {
            val pts = stroke.points
            if (pts.isEmpty()) continue
            val minX = pts.minOf { it.x }
            val maxX = pts.maxOf { it.x }
            val minY = pts.minOf { it.y }
            val maxY = pts.maxOf { it.y }
            val width = (maxX - minX).coerceAtLeast(1f)
            val height = (maxY - minY).coerceAtLeast(1f)
            val aspectRatio = height / width

            val start = pts.first()
            val end = pts.last()
            val dx = start.x - end.x
            val dy = start.y - end.y
            val distStartEnd = kotlin.math.hypot(dx, dy)
            val maxDim = kotlin.math.max(width, height)
            val isClosed = (distStartEnd / maxDim) < 0.35f

            val ch = when {
                isClosed -> 'o'
                aspectRatio > 1.8f -> 'l'
                aspectRatio < 0.45f -> '-'
                else -> 'c'
            }
            recognizedChars.add(ch)
        }

        val primaryWord = recognizedChars.joinToString("")
        val candidateLetters = listOf(
            LetterCandidate('o', 0.1f),
            LetterCandidate('l', 0.2f),
            LetterCandidate('1', 0.3f),
            LetterCandidate('c', 0.4f)
        )

        return RecognitionResult(
            primaryWord = primaryWord,
            suggestions = listOf(primaryWord, "o", "l", "c", "1").filter { it.isNotBlank() }.distinct(),
            rawCandidates = listOf(primaryWord),
            recognizedLetters = recognizedChars,
            candidateLetters = candidateLetters
        )
    }

    /**
     * Backward-compatible stroke-based method for tests and direct point calls.
     */
    fun recognize(
        strokes: List<List<Offset>>,
        commonVocabulary: List<String> = emptyList()
    ): RecognitionResult {
        if (strokes.isEmpty()) {
            return RecognitionResult("", emptyList(), emptyList(), emptyList(), emptyList())
        }

        val inkBuilder = Ink.builder()
        var syntheticTime = 1000L
        for (stroke in strokes) {
            if (stroke.isEmpty()) continue
            val strokeBuilder = Ink.Stroke.builder()
            for (pt in stroke) {
                strokeBuilder.addPoint(Ink.Point.create(pt.x, pt.y, syntheticTime))
                syntheticTime += 20L
            }
            inkBuilder.addStroke(strokeBuilder.build())
            syntheticTime += 100L
        }

        val ink = inkBuilder.build()
        // Synchronous check / run on current thread with fallback
        return runCatching {
            kotlinx.coroutines.runBlocking {
                recognizeInk(ink, "en", 800f, 400f, "", commonVocabulary)
            }
        }.getOrElse {
            fallbackHeuristic(ink)
        }
    }

    /**
     * Releases ML Kit recognizers and frees memory.
     */
    fun close() {
        for ((_, recognizer) in recognizerCache) {
            try {
                recognizer.close()
            } catch (_: Exception) {}
        }
        recognizerCache.clear()
        modelCache.clear()
    }
}

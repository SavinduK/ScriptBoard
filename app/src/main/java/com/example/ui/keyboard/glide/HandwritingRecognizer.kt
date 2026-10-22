package com.example.ui.keyboard.glide

import androidx.compose.ui.geometry.Offset
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * High-performance, offline Handwriting and Glide Stroke Recognizer.
 * Supports single-letter and multi-letter writing with finger or stylus.
 * Uses normalized point-cloud distance matching combined with structural
 * geometric feature analysis, stroke segmentation, and dictionary disambiguation.
 */
object HandwritingRecognizer {

    data class Point(val x: Float, val y: Float)

    data class LetterCandidate(
        val char: Char,
        val score: Float // Lower distance score is better (0.0 = perfect match)
    )

    data class RecognitionResult(
        val primaryWord: String,
        val suggestions: List<String>,
        val recognizedLetters: List<Char>
    )

    // Predefined canonical templates resampled to 32 points in normalized unit space
    private val templates: Map<Char, List<Point>> by lazy {
        generateCanonicalTemplates()
    }

    /**
     * Recognizes letters and words from drawn strokes.
     */
    fun recognize(
        strokes: List<List<Offset>>,
        commonVocabulary: List<String> = emptyList()
    ): RecognitionResult {
        if (strokes.isEmpty()) {
            return RecognitionResult(
                primaryWord = "",
                suggestions = emptyList(),
                recognizedLetters = emptyList()
            )
        }

        // 1. Cluster or segment strokes into individual letter groups
        val clusters = clusterStrokes(strokes)
        if (clusters.isEmpty()) {
            return RecognitionResult("", emptyList(), emptyList())
        }

        // 2. Classify each cluster into candidate letters
        val clusterCandidates = clusters.map { cluster ->
            classifyCluster(cluster)
        }

        val topLetters = clusterCandidates.map { candidates ->
            candidates.firstOrNull()?.char ?: '?'
        }

        val rawWord = topLetters.joinToString("").lowercase()

        // 3. Score against vocabulary with Levenshtein edit distance and candidate permutations
        val matchedWords = findBestWordMatches(clusterCandidates, rawWord, commonVocabulary)

        val primaryWord = matchedWords.firstOrNull() ?: rawWord
        val suggestions = matchedWords.filter { it.isNotBlank() && !it.equals(primaryWord, ignoreCase = true) }
            .take(6)

        return RecognitionResult(
            primaryWord = primaryWord,
            suggestions = suggestions,
            recognizedLetters = topLetters
        )
    }

    /**
     * Groups strokes into separate letters based on horizontal bounds, drawing flow,
     * or segments a continuous glide stroke when it spans multiple characters.
     */
    private fun clusterStrokes(strokes: List<List<Offset>>): List<List<List<Point>>> {
        val validStrokes = strokes.filter { it.size >= 2 }.map { stroke ->
            stroke.map { Point(it.x, it.y) }
        }
        if (validStrokes.isEmpty()) return emptyList()

        // If user made a single continuous stroke, check if it's a multi-letter word
        if (validStrokes.size == 1) {
            val pts = validStrokes[0]
            val minX = pts.minOf { it.x }
            val maxX = pts.maxOf { it.x }
            val minY = pts.minOf { it.y }
            val maxY = pts.maxOf { it.y }
            val width = (maxX - minX).coerceAtLeast(1f)
            val height = (maxY - minY).coerceAtLeast(1f)

            // If width is much greater than height and has enough points, segment into letters
            if (width > height * 1.35f && pts.size >= 12) {
                val segments = segmentContinuousStroke(pts, height)
                if (segments.size > 1) {
                    return segments.map { listOf(it) }
                }
            }
            return listOf(validStrokes)
        }

        // Multi-stroke clustering: Calculate bounding boxes for each stroke
        data class StrokeInfo(
            val points: List<Point>,
            val minX: Float,
            val maxX: Float,
            val minY: Float,
            val maxY: Float
        ) {
            val height = (maxY - minY).coerceAtLeast(1f)
        }

        val strokeInfos = validStrokes.map { pts ->
            val minX = pts.minOf { it.x }
            val maxX = pts.maxOf { it.x }
            val minY = pts.minOf { it.y }
            val maxY = pts.maxOf { it.y }
            StrokeInfo(pts, minX, maxX, minY, maxY)
        }

        val medianHeight = strokeInfos.map { it.height }.sorted()[strokeInfos.size / 2]
        val clusterThreshold = (medianHeight * 0.40f).coerceIn(24f, 100f)

        // Cluster strokes that overlap horizontally or are within threshold
        val clusters = mutableListOf<MutableList<StrokeInfo>>()
        val sortedStrokes = strokeInfos.sortedBy { it.minX }

        for (stroke in sortedStrokes) {
            if (clusters.isEmpty()) {
                clusters.add(mutableListOf(stroke))
                continue
            }

            val lastCluster = clusters.last()
            val clusterMaxX = lastCluster.maxOf { it.maxX }
            val isOverlapping = stroke.minX <= clusterMaxX
            val isClose = (stroke.minX - clusterMaxX) < clusterThreshold

            if (isOverlapping || isClose) {
                lastCluster.add(stroke)
            } else {
                clusters.add(mutableListOf(stroke))
            }
        }

        return clusters.map { cluster -> cluster.map { it.points } }
    }

    /**
     * Splits a continuous horizontal swipe/cursive glide stroke at natural letter transition points.
     */
    private fun segmentContinuousStroke(points: List<Point>, strokeHeight: Float): List<List<Point>> {
        val splitIndices = mutableListOf<Int>()
        val minSegmentPoints = 6
        val minXDist = strokeHeight * 0.45f

        var lastSplitX = points.first().x
        var lastSplitIdx = 0

        for (i in 2 until points.size - 2) {
            val pPrev = points[i - 1]
            val pCurr = points[i]
            val pNext = points[i + 1]

            // Look for local minima in Y (bottom-up connecting strokes) or sharp directional changes
            val isLocalBottom = pCurr.y >= pPrev.y && pCurr.y >= pNext.y
            val hasMovedRight = (pCurr.x - lastSplitX) > minXDist
            val hasEnoughPoints = (i - lastSplitIdx) >= minSegmentPoints

            if (isLocalBottom && hasMovedRight && hasEnoughPoints) {
                splitIndices.add(i)
                lastSplitX = pCurr.x
                lastSplitIdx = i
            }
        }

        if (splitIndices.isEmpty()) {
            return listOf(points)
        }

        val segments = mutableListOf<List<Point>>()
        var startIndex = 0
        for (idx in splitIndices) {
            if (idx > startIndex) {
                segments.add(points.subList(startIndex, idx + 1))
                startIndex = idx
            }
        }
        if (startIndex < points.size) {
            segments.add(points.subList(startIndex, points.size))
        }

        return if (segments.isNotEmpty()) segments else listOf(points)
    }

    /**
     * Classifies a cluster of 1 or more strokes into candidate characters with distance scores.
     */
    fun classifyCluster(strokes: List<List<Point>>): List<LetterCandidate> {
        val allPoints = strokes.flatten()
        if (allPoints.isEmpty()) return emptyList()

        val minX = allPoints.minOf { it.x }
        val maxX = allPoints.maxOf { it.x }
        val minY = allPoints.minOf { it.y }
        val maxY = allPoints.maxOf { it.y }
        val width = (maxX - minX).coerceAtLeast(1f)
        val height = (maxY - minY).coerceAtLeast(1f)
        val aspectRatio = height / width

        // Normalize points into [0, 1] preserving aspect ratio
        val maxDim = max(width, height)
        val normalizedStrokes = strokes.map { stroke ->
            stroke.map { p ->
                Point((p.x - minX) / maxDim, (p.y - minY) / maxDim)
            }
        }

        // Resample normalized cluster to 32 points
        val resampled = resamplePoints(normalizedStrokes.flatten(), 32)
        val centered = centerPoints(resampled)

        val scores = mutableListOf<LetterCandidate>()

        // Distance matching with templates
        for ((char, templatePts) in templates) {
            val dist = pointCloudDistance(centered, templatePts)
            var penalty = 0f

            val strokeCount = strokes.size

            // Two-stroke letters
            if (char in listOf('t', 'x', 'i', 'j', 'k', 'X', 'T', 'K', '+')) {
                if (strokeCount >= 2) penalty -= 0.12f
            }

            // Single stroke smooth letters
            if (char in listOf('c', 'o', 's', 'l', 'u', 'v', 'w', 'z', 'O', 'C', 'S', 'U', 'V', 'W', 'Z', '0', '1', '2', '3', '6', '8', '9')) {
                if (strokeCount == 1) penalty -= 0.08f
            }

            // Aspect ratio heuristics
            if (char in listOf('l', 'i', '1', 'I', 't', 'j')) {
                if (aspectRatio > 1.6f) penalty -= 0.18f
                else if (aspectRatio < 0.8f) penalty += 0.25f
            } else if (char in listOf('m', 'w', 'W', 'M')) {
                if (aspectRatio < 1.1f) penalty -= 0.14f
            }

            // Closed loop check for 'o', '0', 'O', '8', 'e', 'a', 'b', 'd', 'p', 'q'
            val start = resampled.first()
            val end = resampled.last()
            val startEndDist = hypot(start.x - end.x, start.y - end.y)
            if (char in listOf('o', 'O', '0', '8')) {
                if (startEndDist < 0.25f) penalty -= 0.22f
                else penalty += 0.20f
            }

            val finalScore = (dist + penalty).coerceAtLeast(0.01f)
            scores.add(LetterCandidate(char, finalScore))
        }

        return scores.sortedBy { it.score }.take(6)
    }

    /**
     * Resamples a polyline of points into N equidistant points.
     */
    private fun resamplePoints(points: List<Point>, targetCount: Int): List<Point> {
        if (points.size <= 1) return List(targetCount) { points.firstOrNull() ?: Point(0f, 0f) }
        val totalLength = calculatePathLength(points)
        if (totalLength <= 0.0001f) return List(targetCount) { points.first() }

        val interval = totalLength / (targetCount - 1)
        val result = mutableListOf(points.first())
        var currentDist = 0f

        val pts = points.toMutableList()
        var i = 1
        while (i < pts.size && result.size < targetCount) {
            val p0 = pts[i - 1]
            val p1 = pts[i]
            val d = hypot(p1.x - p0.x, p1.y - p0.y)
            if (currentDist + d >= interval) {
                val t = (interval - currentDist) / if (d > 0f) d else 1f
                val qx = p0.x + t * (p1.x - p0.x)
                val qy = p0.y + t * (p1.y - p0.y)
                val q = Point(qx, qy)
                result.add(q)
                pts[i - 1] = q
                currentDist = 0f
            } else {
                currentDist += d
                i++
            }
        }

        while (result.size < targetCount) {
            result.add(points.last())
        }

        return result
    }

    private fun calculatePathLength(points: List<Point>): Float {
        var len = 0f
        for (i in 0 until points.size - 1) {
            len += hypot(points[i + 1].x - points[i].x, points[i + 1].y - points[i].y)
        }
        return len
    }

    /**
     * Centers points around origin (0, 0).
     */
    private fun centerPoints(points: List<Point>): List<Point> {
        if (points.isEmpty()) return points
        val meanX = points.map { it.x }.average().toFloat()
        val meanY = points.map { it.y }.average().toFloat()
        return points.map { Point(it.x - meanX, it.y - meanY) }
    }

    /**
     * Symmetrical nearest-neighbor point cloud distance.
     */
    private fun pointCloudDistance(pts1: List<Point>, pts2: List<Point>): Float {
        if (pts1.isEmpty() || pts2.isEmpty()) return 10f

        var sum = 0f
        for (p1 in pts1) {
            var minDistSq = Float.MAX_VALUE
            for (p2 in pts2) {
                val dx = p1.x - p2.x
                val dy = p1.y - p2.y
                val d = dx * dx + dy * dy
                if (d < minDistSq) minDistSq = d
            }
            sum += sqrt(minDistSq)
        }

        for (p2 in pts2) {
            var minDistSq = Float.MAX_VALUE
            for (p1 in pts1) {
                val dx = p2.x - p1.x
                val dy = p2.y - p1.y
                val d = dx * dx + dy * dy
                if (d < minDistSq) minDistSq = d
            }
            sum += sqrt(minDistSq)
        }

        return sum / (pts1.size + pts2.size)
    }

    /**
     * Disambiguates letter candidates against common English dictionary words
     * using Levenshtein distance, candidate permutations, and prefix completions.
     */
    private fun findBestWordMatches(
        clusterCandidates: List<List<LetterCandidate>>,
        rawWord: String,
        passedVocab: List<String>
    ): List<String> {
        val results = mutableListOf<String>()

        val vocabulary = if (passedVocab.isNotEmpty()) (passedVocab + RICH_ENGLISH_DICTIONARY).distinct() else RICH_ENGLISH_DICTIONARY

        if (clusterCandidates.isEmpty()) {
            if (rawWord.isNotBlank()) results.add(rawWord)
            return results
        }

        val wordLen = clusterCandidates.size

        // Case 1: Single letter
        if (wordLen == 1) {
            val topCandidates = clusterCandidates[0].take(3)
            val primaryChar = topCandidates.firstOrNull()?.char?.toString() ?: rawWord
            results.add(primaryChar)

            // Add top common words starting with this letter
            vocabulary.filter { it.startsWith(primaryChar, ignoreCase = true) }
                .take(5)
                .forEach { if (!results.contains(it)) results.add(it) }

            // Also add other top letter candidates
            topCandidates.drop(1).forEach { cand ->
                val ch = cand.char.toString()
                if (!results.contains(ch)) results.add(ch)
            }
            return results
        }

        // Case 2: Exact match in vocabulary
        val exactMatch = vocabulary.firstOrNull { it.equals(rawWord, ignoreCase = true) }
        if (exactMatch != null) {
            results.add(exactMatch)
        }

        // Case 3: Candidate permutations for short words (2 to 5 letters)
        if (wordLen in 2..5) {
            val candidateSets = clusterCandidates.map { it.take(2).map { c -> c.char.lowercaseChar() } }
            val possibleCombinations = mutableListOf<String>()

            fun generateCombos(index: Int, current: String) {
                if (index == wordLen) {
                    possibleCombinations.add(current)
                    return
                }
                for (ch in candidateSets[index]) {
                    generateCombos(index + 1, current + ch)
                }
            }
            generateCombos(0, "")

            for (combo in possibleCombinations) {
                val vocabMatch = vocabulary.firstOrNull { it.equals(combo, ignoreCase = true) }
                if (vocabMatch != null && !results.contains(vocabMatch)) {
                    results.add(vocabMatch)
                }
            }
        }

        // Case 4: Fuzzy Levenshtein matching (distance <= 2)
        val firstChar = rawWord.firstOrNull()?.lowercaseChar()
        val lastChar = rawWord.lastOrNull()?.lowercaseChar()

        val fuzzyMatches = vocabulary
            .filter { abs(it.length - wordLen) <= 2 }
            .mapNotNull { word ->
                val dist = levenshteinDistance(rawWord, word.lowercase())
                if (dist <= 2) {
                    var score = 100 - (dist * 30)
                    if (firstChar != null && word.startsWith(firstChar, ignoreCase = true)) score += 40
                    if (lastChar != null && word.endsWith(lastChar, ignoreCase = true)) score += 20
                    Pair(word, score)
                } else null
            }
            .sortedByDescending { it.second }
            .map { it.first }

        for (fuzzy in fuzzyMatches) {
            if (!results.contains(fuzzy)) {
                results.add(fuzzy)
                if (results.size >= 6) break
            }
        }

        // Case 5: Prefix matches
        for (vocab in vocabulary) {
            if (vocab.startsWith(rawWord, ignoreCase = true) && !results.contains(vocab)) {
                results.add(vocab)
                if (results.size >= 6) break
            }
        }

        // If rawWord not included, insert it at first or second position
        if (!results.contains(rawWord) && rawWord.isNotBlank()) {
            if (results.isEmpty()) results.add(rawWord)
            else results.add(1, rawWord)
        }

        return results
    }

    private fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j

        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1,
                    dp[i - 1][j - 1] + cost
                )
            }
        }
        return dp[s1.length][s2.length]
    }

    /**
     * Rich built-in English dictionary for glide typing recognition.
     */
    private val RICH_ENGLISH_DICTIONARY: List<String> = listOf(
        "the", "be", "to", "of", "and", "a", "in", "that", "have", "i", "it", "for", "not",
        "on", "with", "he", "as", "you", "do", "at", "this", "but", "his", "by", "from",
        "they", "we", "say", "her", "she", "or", "an", "will", "my", "one", "all", "would",
        "there", "their", "what", "so", "up", "out", "if", "about", "who", "get", "which",
        "go", "me", "when", "make", "can", "like", "time", "no", "just", "him", "know",
        "take", "people", "into", "year", "your", "good", "some", "could", "them", "see",
        "other", "than", "then", "now", "look", "only", "come", "its", "over", "think",
        "also", "back", "after", "use", "two", "how", "our", "work", "first", "well",
        "way", "even", "new", "want", "because", "any", "these", "give", "day", "most",
        "us", "great", "here", "please", "thank", "thanks", "today", "tomorrow", "yesterday",
        "morning", "evening", "night", "meet", "meeting", "call", "message", "email",
        "phone", "address", "where", "why", "let", "check", "send", "received", "nice",
        "happy", "awesome", "perfect", "help", "need", "love", "really", "very", "right",
        "sure", "tell", "much", "before", "while", "should", "going", "been", "being",
        "doing", "having", "working", "trying", "looking", "welcome", "sounds", "fine",
        "talk", "later", "soon", "ready", "done", "start", "stop", "again", "always",
        "never", "maybe", "yes", "yeah", "okay", "alright", "sorry", "excuse", "appreciate",
        "regards", "best", "cheers", "update", "project", "code", "android", "keyboard",
        "mobile", "device", "quick", "fast", "speed", "test", "hello", "hi", "hey",
        "cat", "dog", "sun", "car", "run", "pen", "write", "note", "read", "book",
        "home", "friend", "smile", "laugh", "walk", "drive", "food", "water", "coffee",
        "tea", "lunch", "dinner", "sleep", "dream", "game", "play", "music", "song",
        "watch", "movie", "video", "photo", "picture", "file", "open", "close", "save",
        "share", "copy", "paste", "cut", "undo", "redo", "find", "search", "view",
        "edit", "type", "text", "word", "line", "page", "screen", "button", "icon",
        "list", "card", "view", "menu", "tab", "setting", "system", "data", "info",
        "name", "user", "pass", "login", "sync", "wifi", "bluetooth", "cloud", "store"
    )

    /**
     * Generates normalized 32-point canonical templates for letters and symbols.
     */
    private fun generateCanonicalTemplates(): Map<Char, List<Point>> {
        val map = mutableMapOf<Char, List<Point>>()

        fun createTemplate(char: Char, rawPoints: List<Point>) {
            val resampled = resamplePoints(rawPoints, 32)
            map[char] = centerPoints(resampled)
        }

        // a
        createTemplate('a', listOf(
            Point(0.7f, 0.4f), Point(0.4f, 0.35f), Point(0.2f, 0.6f),
            Point(0.4f, 0.85f), Point(0.7f, 0.65f), Point(0.7f, 0.4f), Point(0.7f, 0.9f)
        ))

        // b
        createTemplate('b', listOf(
            Point(0.25f, 0.1f), Point(0.25f, 0.9f), Point(0.25f, 0.5f),
            Point(0.65f, 0.5f), Point(0.75f, 0.7f), Point(0.65f, 0.9f), Point(0.25f, 0.9f)
        ))

        // c
        createTemplate('c', listOf(
            Point(0.75f, 0.3f), Point(0.5f, 0.2f), Point(0.25f, 0.5f),
            Point(0.5f, 0.8f), Point(0.75f, 0.7f)
        ))

        // d
        createTemplate('d', listOf(
            Point(0.75f, 0.5f), Point(0.35f, 0.5f), Point(0.25f, 0.7f),
            Point(0.35f, 0.9f), Point(0.75f, 0.9f), Point(0.75f, 0.1f), Point(0.75f, 0.9f)
        ))

        // e
        createTemplate('e', listOf(
            Point(0.25f, 0.55f), Point(0.75f, 0.55f), Point(0.5f, 0.25f),
            Point(0.25f, 0.55f), Point(0.4f, 0.85f), Point(0.75f, 0.8f)
        ))

        // f
        createTemplate('f', listOf(
            Point(0.7f, 0.2f), Point(0.5f, 0.1f), Point(0.45f, 0.9f),
            Point(0.25f, 0.45f), Point(0.65f, 0.45f)
        ))

        // g
        createTemplate('g', listOf(
            Point(0.7f, 0.35f), Point(0.35f, 0.35f), Point(0.3f, 0.6f),
            Point(0.7f, 0.6f), Point(0.7f, 0.35f), Point(0.7f, 0.95f), Point(0.35f, 0.95f)
        ))

        // h
        createTemplate('h', listOf(
            Point(0.25f, 0.1f), Point(0.25f, 0.9f), Point(0.25f, 0.5f),
            Point(0.5f, 0.4f), Point(0.75f, 0.55f), Point(0.75f, 0.9f)
        ))

        // i
        createTemplate('i', listOf(
            Point(0.5f, 0.35f), Point(0.5f, 0.9f), Point(0.5f, 0.18f), Point(0.52f, 0.22f)
        ))

        // j
        createTemplate('j', listOf(
            Point(0.6f, 0.35f), Point(0.6f, 0.9f), Point(0.35f, 0.95f),
            Point(0.6f, 0.18f), Point(0.62f, 0.22f)
        ))

        // k
        createTemplate('k', listOf(
            Point(0.3f, 0.1f), Point(0.3f, 0.9f), Point(0.7f, 0.35f),
            Point(0.3f, 0.6f), Point(0.7f, 0.9f)
        ))

        // l
        createTemplate('l', listOf(
            Point(0.5f, 0.1f), Point(0.5f, 0.5f), Point(0.5f, 0.9f)
        ))

        // m
        createTemplate('m', listOf(
            Point(0.15f, 0.9f), Point(0.15f, 0.4f), Point(0.35f, 0.35f), Point(0.5f, 0.6f),
            Point(0.5f, 0.9f), Point(0.5f, 0.4f), Point(0.75f, 0.35f), Point(0.85f, 0.9f)
        ))

        // n
        createTemplate('n', listOf(
            Point(0.25f, 0.9f), Point(0.25f, 0.4f), Point(0.5f, 0.35f),
            Point(0.75f, 0.55f), Point(0.75f, 0.9f)
        ))

        // o
        createTemplate('o', (0..16).map { i ->
            val angle = (i.toDouble() / 16.0) * 2.0 * Math.PI
            Point((0.5 + 0.35 * Math.cos(angle)).toFloat(), (0.5 + 0.35 * Math.sin(angle)).toFloat())
        })

        // p
        createTemplate('p', listOf(
            Point(0.25f, 0.35f), Point(0.25f, 0.95f), Point(0.25f, 0.35f),
            Point(0.65f, 0.35f), Point(0.75f, 0.55f), Point(0.65f, 0.7f), Point(0.25f, 0.7f)
        ))

        // q
        createTemplate('q', listOf(
            Point(0.7f, 0.35f), Point(0.35f, 0.35f), Point(0.3f, 0.55f),
            Point(0.7f, 0.7f), Point(0.7f, 0.35f), Point(0.7f, 0.95f)
        ))

        // r
        createTemplate('r', listOf(
            Point(0.3f, 0.4f), Point(0.3f, 0.9f), Point(0.3f, 0.55f),
            Point(0.55f, 0.4f), Point(0.75f, 0.45f)
        ))

        // s
        createTemplate('s', listOf(
            Point(0.75f, 0.35f), Point(0.45f, 0.25f), Point(0.3f, 0.45f),
            Point(0.65f, 0.65f), Point(0.55f, 0.85f), Point(0.25f, 0.75f)
        ))

        // t
        createTemplate('t', listOf(
            Point(0.5f, 0.15f), Point(0.5f, 0.85f), Point(0.65f, 0.85f),
            Point(0.25f, 0.4f), Point(0.75f, 0.4f)
        ))

        // u
        createTemplate('u', listOf(
            Point(0.25f, 0.35f), Point(0.25f, 0.75f), Point(0.5f, 0.85f),
            Point(0.75f, 0.75f), Point(0.75f, 0.35f), Point(0.75f, 0.9f)
        ))

        // v
        createTemplate('v', listOf(
            Point(0.2f, 0.3f), Point(0.5f, 0.85f), Point(0.8f, 0.3f)
        ))

        // w
        createTemplate('w', listOf(
            Point(0.15f, 0.3f), Point(0.35f, 0.85f), Point(0.5f, 0.5f),
            Point(0.65f, 0.85f), Point(0.85f, 0.3f)
        ))

        // x
        createTemplate('x', listOf(
            Point(0.25f, 0.25f), Point(0.75f, 0.85f),
            Point(0.75f, 0.25f), Point(0.25f, 0.85f)
        ))

        // y
        createTemplate('y', listOf(
            Point(0.25f, 0.35f), Point(0.5f, 0.65f), Point(0.75f, 0.35f),
            Point(0.5f, 0.65f), Point(0.3f, 0.95f)
        ))

        // z
        createTemplate('z', listOf(
            Point(0.25f, 0.35f), Point(0.75f, 0.35f), Point(0.25f, 0.85f), Point(0.75f, 0.85f)
        ))

        // Uppercase templates
        createTemplate('A', listOf(
            Point(0.15f, 0.9f), Point(0.5f, 0.1f), Point(0.85f, 0.9f),
            Point(0.3f, 0.6f), Point(0.7f, 0.6f)
        ))
        createTemplate('B', listOf(
            Point(0.25f, 0.1f), Point(0.25f, 0.9f), Point(0.65f, 0.3f),
            Point(0.35f, 0.5f), Point(0.7f, 0.7f), Point(0.25f, 0.9f)
        ))
        createTemplate('C', listOf(
            Point(0.8f, 0.25f), Point(0.45f, 0.15f), Point(0.2f, 0.5f),
            Point(0.45f, 0.85f), Point(0.8f, 0.75f)
        ))
        createTemplate('D', listOf(
            Point(0.25f, 0.1f), Point(0.25f, 0.9f), Point(0.3f, 0.1f),
            Point(0.75f, 0.5f), Point(0.3f, 0.9f)
        ))
        createTemplate('E', listOf(
            Point(0.75f, 0.15f), Point(0.25f, 0.15f), Point(0.25f, 0.85f),
            Point(0.75f, 0.85f), Point(0.25f, 0.5f), Point(0.65f, 0.5f)
        ))
        createTemplate('H', listOf(
            Point(0.25f, 0.1f), Point(0.25f, 0.9f),
            Point(0.25f, 0.5f), Point(0.75f, 0.5f),
            Point(0.75f, 0.1f), Point(0.75f, 0.9f)
        ))
        createTemplate('I', listOf(
            Point(0.3f, 0.15f), Point(0.7f, 0.15f),
            Point(0.5f, 0.15f), Point(0.5f, 0.85f),
            Point(0.3f, 0.85f), Point(0.7f, 0.85f)
        ))
        createTemplate('O', (0..16).map { i ->
            val angle = (i.toDouble() / 16.0) * 2.0 * Math.PI
            Point((0.5 + 0.38 * Math.cos(angle)).toFloat(), (0.5 + 0.42 * Math.sin(angle)).toFloat())
        })
        createTemplate('S', listOf(
            Point(0.75f, 0.25f), Point(0.45f, 0.15f), Point(0.25f, 0.35f),
            Point(0.65f, 0.55f), Point(0.55f, 0.85f), Point(0.25f, 0.75f)
        ))
        createTemplate('T', listOf(
            Point(0.15f, 0.15f), Point(0.85f, 0.15f),
            Point(0.5f, 0.15f), Point(0.5f, 0.9f)
        ))

        // Numbers 0-9
        createTemplate('0', (0..16).map { i ->
            val angle = (i.toDouble() / 16.0) * 2.0 * Math.PI
            Point((0.5 + 0.3 * Math.cos(angle)).toFloat(), (0.5 + 0.42 * Math.sin(angle)).toFloat())
        })
        createTemplate('1', listOf(
            Point(0.35f, 0.25f), Point(0.5f, 0.15f), Point(0.5f, 0.9f),
            Point(0.3f, 0.9f), Point(0.7f, 0.9f)
        ))
        createTemplate('2', listOf(
            Point(0.3f, 0.3f), Point(0.5f, 0.15f), Point(0.75f, 0.3f),
            Point(0.25f, 0.85f), Point(0.8f, 0.85f)
        ))
        createTemplate('3', listOf(
            Point(0.25f, 0.2f), Point(0.75f, 0.2f), Point(0.45f, 0.5f),
            Point(0.75f, 0.65f), Point(0.3f, 0.85f)
        ))
        createTemplate('4', listOf(
            Point(0.7f, 0.85f), Point(0.7f, 0.15f),
            Point(0.25f, 0.65f), Point(0.85f, 0.65f)
        ))
        createTemplate('5', listOf(
            Point(0.75f, 0.15f), Point(0.3f, 0.15f), Point(0.3f, 0.5f),
            Point(0.75f, 0.6f), Point(0.3f, 0.85f)
        ))
        createTemplate('7', listOf(
            Point(0.25f, 0.15f), Point(0.75f, 0.15f), Point(0.45f, 0.9f)
        ))
        createTemplate('8', listOf(
            Point(0.5f, 0.5f), Point(0.3f, 0.3f), Point(0.5f, 0.15f),
            Point(0.7f, 0.3f), Point(0.5f, 0.5f), Point(0.3f, 0.7f),
            Point(0.5f, 0.85f), Point(0.7f, 0.7f), Point(0.5f, 0.5f)
        ))

        return map
    }
}

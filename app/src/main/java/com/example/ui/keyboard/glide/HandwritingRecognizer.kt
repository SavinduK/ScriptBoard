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
 * Uses ordered trajectory path distance combined with structural nearest-neighbor
 * point-clouds, closed-loop detection, aspect ratio heuristics, and dictionary matching.
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
        val recognizedLetters: List<Char>,
        val candidateLetters: List<LetterCandidate> = emptyList()
    )

    data class StrokeTemplate(
        val char: Char,
        val points: List<Point>,
        val isClosed: Boolean = false,
        val isTall: Boolean = false,
        val isWide: Boolean = false,
        val expectedStrokes: Int = 1,
        val weight: Float = 1.0f
    )

    // Predefined canonical templates resampled to 32 points in normalized unit space
    private val templates: List<StrokeTemplate> by lazy {
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
                recognizedLetters = emptyList(),
                candidateLetters = emptyList()
            )
        }

        // 1. Cluster or segment strokes into individual letter groups
        val clusters = clusterStrokes(strokes)
        if (clusters.isEmpty()) {
            return RecognitionResult("", emptyList(), emptyList(), emptyList())
        }

        // 2. Classify each cluster into candidate letters
        val clusterCandidates = clusters.map { cluster ->
            classifyCluster(cluster)
        }

        val topLetters = clusterCandidates.map { candidates ->
            candidates.firstOrNull()?.char ?: '?'
        }

        val rawWord = topLetters.joinToString("").lowercase()
        val allTopCandidates = clusterCandidates.firstOrNull() ?: emptyList()

        // 3. Score against vocabulary with Levenshtein edit distance and candidate permutations
        val matchedWords = findBestWordMatches(clusterCandidates, rawWord, commonVocabulary)

        val primaryWord = matchedWords.firstOrNull() ?: rawWord
        val suggestions = matchedWords.filter { it.isNotBlank() && !it.equals(primaryWord, ignoreCase = true) }
            .take(8)

        return RecognitionResult(
            primaryWord = primaryWord,
            suggestions = suggestions,
            recognizedLetters = topLetters,
            candidateLetters = allTopCandidates
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

            // If width is significantly greater than height and has enough points, segment into letters
            if (width > height * 1.45f && pts.size >= 14) {
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

        val strokeCount = strokes.size
        val startPt = resampled.first()
        val endPt = resampled.last()
        val startEndDist = hypot(startPt.x - endPt.x, startPt.y - endPt.y)
        val isClosedStroke = startEndDist < 0.28f
        val isTallStroke = aspectRatio > 1.35f
        val isWideStroke = aspectRatio < 0.80f

        val charBestScores = mutableMapOf<Char, Float>()

        for (tmpl in templates) {
            val templatePts = if (tmpl.isClosed) {
                var bestOffset = 0
                var minStartDistSq = Float.MAX_VALUE
                val c0 = centered.first()
                for (k in 0 until 32) {
                    val dx = c0.x - tmpl.points[k].x
                    val dy = c0.y - tmpl.points[k].y
                    val d = dx * dx + dy * dy
                    if (d < minStartDistSq) {
                        minStartDistSq = d
                        bestOffset = k
                    }
                }
                List(32) { idx -> tmpl.points[(idx + bestOffset) % 32] }
            } else {
                tmpl.points
            }
            if (templatePts.size != 32) continue

            // 1. Forward ordered path distance
            var fwdDist = 0f
            for (i in 0 until 32) {
                val dx = centered[i].x - templatePts[i].x
                val dy = centered[i].y - templatePts[i].y
                fwdDist += sqrt(dx * dx + dy * dy)
            }
            fwdDist /= 32f

            // 2. Reverse ordered path distance (if drawn backwards or clockwise vs counter-clockwise)
            var revDist = 0f
            for (i in 0 until 32) {
                val dx = centered[i].x - templatePts[31 - i].x
                val dy = centered[i].y - templatePts[31 - i].y
                revDist += sqrt(dx * dx + dy * dy)
            }
            revDist /= 32f

            val orderedDist = min(fwdDist, revDist)

            // 3. Nearest neighbor point cloud distance (tolerates small phase shifts)
            val nnDist = pointCloudDistance(centered, templatePts)

            // Combined primary distance
            var baseDist = (orderedDist * 0.65f + nnDist * 0.35f) * tmpl.weight

            // 4. Structural Feature Checks
            // Closed loop feature matching
            if (tmpl.isClosed) {
                if (isClosedStroke) {
                    baseDist -= 0.14f
                } else {
                    baseDist += 0.22f
                }
            } else {
                if (isClosedStroke && tmpl.char !in listOf('B', 'D', 'P', 'R', 'Q', 'b', 'd', 'p', 'q', 'g', 'e', 'a')) {
                    baseDist += 0.20f
                }
            }

            // Aspect ratio matching
            if (tmpl.isTall) {
                if (isTallStroke) {
                    baseDist -= 0.12f
                } else if (isWideStroke) {
                    baseDist += 0.22f
                }
            }
            if (tmpl.isWide) {
                if (isWideStroke) {
                    baseDist -= 0.12f
                } else if (isTallStroke) {
                    baseDist += 0.18f
                }
            }

            // Multi-stroke matching
            if (tmpl.expectedStrokes >= 2) {
                if (strokeCount >= 2) baseDist -= 0.10f
            } else {
                if (strokeCount == 1) baseDist -= 0.06f
            }

            val finalScore = baseDist.coerceAtLeast(0.01f)
            val currentBest = charBestScores[tmpl.char]
            if (currentBest == null || finalScore < currentBest) {
                charBestScores[tmpl.char] = finalScore
            }
        }

        return charBestScores.map { (char, score) ->
            LetterCandidate(char, score)
        }.sortedBy { it.score }.take(10)
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
            val topCandidates = clusterCandidates[0].take(5)
            val primaryChar = topCandidates.firstOrNull()?.char?.toString() ?: rawWord
            results.add(primaryChar)

            // Include top alternative recognized letters directly in suggestions
            topCandidates.drop(1).forEach { cand ->
                val ch = cand.char.toString()
                if (!results.contains(ch)) results.add(ch)
            }

            // Also suggest top common words starting with this letter
            vocabulary.filter { it.startsWith(primaryChar, ignoreCase = true) }
                .take(4)
                .forEach { if (!results.contains(it)) results.add(it) }

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
                if (results.size >= 8) break
            }
        }

        // Case 5: Prefix matches
        for (vocab in vocabulary) {
            if (vocab.startsWith(rawWord, ignoreCase = true) && !results.contains(vocab)) {
                results.add(vocab)
                if (results.size >= 8) break
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
     * Generates comprehensive normalized 32-point canonical templates for:
     * - All uppercase letters ('A'..'Z')
     * - All lowercase letters ('a'..'z')
     * - Common single-stroke cursive and print variants
     * - Digits ('0'..'9')
     */
    private fun generateCanonicalTemplates(): List<StrokeTemplate> {
        val list = mutableListOf<StrokeTemplate>()

        fun addTemplate(
            char: Char,
            rawPoints: List<Point>,
            isClosed: Boolean = false,
            isTall: Boolean = false,
            isWide: Boolean = false,
            expectedStrokes: Int = 1,
            weight: Float = 1.0f
        ) {
            val resampled = resamplePoints(rawPoints, 32)
            val centered = centerPoints(resampled)
            list.add(
                StrokeTemplate(
                    char = char,
                    points = centered,
                    isClosed = isClosed,
                    isTall = isTall,
                    isWide = isWide,
                    expectedStrokes = expectedStrokes,
                    weight = weight
                )
            )
        }

        // Helper for circular shapes
        fun circlePoints(radiusX: Double, radiusY: Double, startAngle: Double = -Math.PI / 2): List<Point> {
            return (0..24).map { i ->
                val angle = startAngle + (i.toDouble() / 24.0) * 2.0 * Math.PI
                Point((0.5 + radiusX * Math.cos(angle)).toFloat(), (0.5 + radiusY * Math.sin(angle)).toFloat())
            }
        }

        // ==========================================
        // LOWERCASE LETTERS ('a'..'z')
        // ==========================================

        // a - standard rounded bowl + stem
        addTemplate('a', listOf(
            Point(0.75f, 0.35f), Point(0.45f, 0.30f), Point(0.25f, 0.55f),
            Point(0.45f, 0.85f), Point(0.75f, 0.70f), Point(0.75f, 0.35f), Point(0.75f, 0.90f)
        ))

        // b - tall stem + bottom loop
        addTemplate('b', listOf(
            Point(0.25f, 0.05f), Point(0.25f, 0.90f), Point(0.25f, 0.50f),
            Point(0.65f, 0.50f), Point(0.75f, 0.70f), Point(0.65f, 0.90f), Point(0.25f, 0.90f)
        ), isTall = true)

        // c - smooth open curve
        addTemplate('c', listOf(
            Point(0.75f, 0.32f), Point(0.45f, 0.22f), Point(0.25f, 0.50f),
            Point(0.45f, 0.82f), Point(0.75f, 0.72f)
        ))

        // d - bowl + tall stem
        addTemplate('d', listOf(
            Point(0.75f, 0.50f), Point(0.35f, 0.50f), Point(0.25f, 0.70f),
            Point(0.35f, 0.90f), Point(0.75f, 0.90f), Point(0.75f, 0.05f), Point(0.75f, 0.90f)
        ), isTall = true)

        // e - horizontal crossbar into curve
        addTemplate('e', listOf(
            Point(0.25f, 0.55f), Point(0.75f, 0.55f), Point(0.50f, 0.25f),
            Point(0.25f, 0.55f), Point(0.40f, 0.85f), Point(0.75f, 0.80f)
        ))

        // f - top hook + stem + crossbar
        addTemplate('f', listOf(
            Point(0.70f, 0.15f), Point(0.50f, 0.05f), Point(0.45f, 0.90f),
            Point(0.25f, 0.45f), Point(0.65f, 0.45f)
        ), isTall = true, expectedStrokes = 2)

        // f - single stroke cursive loop
        addTemplate('f', listOf(
            Point(0.65f, 0.15f), Point(0.45f, 0.05f), Point(0.45f, 0.90f),
            Point(0.30f, 0.50f), Point(0.60f, 0.50f)
        ), isTall = true, expectedStrokes = 1)

        // g - bowl + descender hook
        addTemplate('g', listOf(
            Point(0.70f, 0.35f), Point(0.35f, 0.35f), Point(0.25f, 0.60f),
            Point(0.70f, 0.60f), Point(0.70f, 0.35f), Point(0.70f, 0.95f), Point(0.35f, 0.95f)
        ))

        // h - stem + arch
        addTemplate('h', listOf(
            Point(0.25f, 0.05f), Point(0.25f, 0.90f), Point(0.25f, 0.50f),
            Point(0.50f, 0.35f), Point(0.75f, 0.55f), Point(0.75f, 0.90f)
        ), isTall = true)

        // i - short stem
        addTemplate('i', listOf(
            Point(0.50f, 0.30f), Point(0.50f, 0.90f)
        ), isTall = true)

        // i - stem + dot
        addTemplate('i', listOf(
            Point(0.50f, 0.35f), Point(0.50f, 0.90f), Point(0.50f, 0.15f), Point(0.52f, 0.18f)
        ), isTall = true, expectedStrokes = 2)

        // j - hook descender
        addTemplate('j', listOf(
            Point(0.60f, 0.35f), Point(0.60f, 0.90f), Point(0.35f, 0.95f)
        ), isTall = true)

        // k - stem + angled branches
        addTemplate('k', listOf(
            Point(0.30f, 0.05f), Point(0.30f, 0.90f), Point(0.70f, 0.35f),
            Point(0.30f, 0.60f), Point(0.70f, 0.90f)
        ), isTall = true, expectedStrokes = 2)

        // l - single vertical line
        addTemplate('l', listOf(
            Point(0.50f, 0.05f), Point(0.50f, 0.50f), Point(0.50f, 0.95f)
        ), isTall = true)

        // m - double arch
        addTemplate('m', listOf(
            Point(0.15f, 0.90f), Point(0.15f, 0.35f), Point(0.35f, 0.30f), Point(0.50f, 0.55f),
            Point(0.50f, 0.90f), Point(0.50f, 0.35f), Point(0.70f, 0.30f), Point(0.85f, 0.90f)
        ), isWide = true)

        // n - single arch
        addTemplate('n', listOf(
            Point(0.25f, 0.90f), Point(0.25f, 0.35f), Point(0.50f, 0.30f),
            Point(0.75f, 0.55f), Point(0.75f, 0.90f)
        ))

        // o - closed circle
        addTemplate('o', circlePoints(0.32, 0.32), isClosed = true, weight = 0.88f)

        // p - descender stem + upper loop
        addTemplate('p', listOf(
            Point(0.25f, 0.30f), Point(0.25f, 0.95f), Point(0.25f, 0.30f),
            Point(0.65f, 0.30f), Point(0.75f, 0.50f), Point(0.65f, 0.70f), Point(0.25f, 0.70f)
        ), isTall = true)

        // q - bowl + descender stem
        addTemplate('q', listOf(
            Point(0.70f, 0.35f), Point(0.35f, 0.35f), Point(0.25f, 0.55f),
            Point(0.70f, 0.70f), Point(0.70f, 0.35f), Point(0.70f, 0.95f)
        ), isTall = true)

        // r - stem + shoulder curve
        addTemplate('r', listOf(
            Point(0.30f, 0.90f), Point(0.30f, 0.35f), Point(0.30f, 0.50f),
            Point(0.55f, 0.35f), Point(0.75f, 0.42f)
        ))

        // s - serpentine curve
        addTemplate('s', listOf(
            Point(0.75f, 0.32f), Point(0.45f, 0.22f), Point(0.30f, 0.45f),
            Point(0.65f, 0.65f), Point(0.55f, 0.85f), Point(0.25f, 0.75f)
        ))

        // t - stem with bottom hook + crossbar
        addTemplate('t', listOf(
            Point(0.50f, 0.10f), Point(0.50f, 0.85f), Point(0.65f, 0.85f),
            Point(0.25f, 0.35f), Point(0.75f, 0.35f)
        ), isTall = true, expectedStrokes = 2)

        // t - single stroke hook
        addTemplate('t', listOf(
            Point(0.48f, 0.08f), Point(0.48f, 0.88f), Point(0.70f, 0.88f)
        ), isTall = true, expectedStrokes = 1)

        // u - cup curve
        addTemplate('u', listOf(
            Point(0.25f, 0.30f), Point(0.25f, 0.75f), Point(0.50f, 0.85f),
            Point(0.75f, 0.75f), Point(0.75f, 0.30f), Point(0.75f, 0.90f)
        ))

        // v - angle wedge
        addTemplate('v', listOf(
            Point(0.20f, 0.30f), Point(0.50f, 0.88f), Point(0.80f, 0.30f)
        ))

        // w - double V
        addTemplate('w', listOf(
            Point(0.15f, 0.30f), Point(0.35f, 0.88f), Point(0.50f, 0.50f),
            Point(0.65f, 0.88f), Point(0.85f, 0.30f)
        ), isWide = true)

        // x - diagonal cross
        addTemplate('x', listOf(
            Point(0.25f, 0.25f), Point(0.75f, 0.85f),
            Point(0.75f, 0.25f), Point(0.25f, 0.85f)
        ), expectedStrokes = 2)

        // x - single stroke cursive loop
        addTemplate('x', listOf(
            Point(0.30f, 0.25f), Point(0.70f, 0.85f), Point(0.50f, 0.55f),
            Point(0.70f, 0.25f), Point(0.30f, 0.85f)
        ), expectedStrokes = 1)

        // y - V into descender
        addTemplate('y', listOf(
            Point(0.25f, 0.30f), Point(0.50f, 0.65f), Point(0.75f, 0.30f),
            Point(0.50f, 0.65f), Point(0.30f, 0.95f)
        ))

        // z - zigzag
        addTemplate('z', listOf(
            Point(0.25f, 0.30f), Point(0.75f, 0.30f), Point(0.25f, 0.85f), Point(0.75f, 0.85f)
        ))

        // ==========================================
        // UPPERCASE LETTERS ('A'..'Z')
        // ==========================================

        // A - standard apex with crossbar
        addTemplate('A', listOf(
            Point(0.15f, 0.95f), Point(0.50f, 0.05f), Point(0.85f, 0.95f),
            Point(0.30f, 0.60f), Point(0.70f, 0.60f)
        ), isTall = true, expectedStrokes = 2)

        // A - single stroke loop
        addTemplate('A', listOf(
            Point(0.15f, 0.95f), Point(0.50f, 0.05f), Point(0.85f, 0.95f),
            Point(0.65f, 0.55f), Point(0.35f, 0.55f)
        ), isTall = true, expectedStrokes = 1)

        // B - vertical stem + double loop
        addTemplate('B', listOf(
            Point(0.20f, 0.95f), Point(0.20f, 0.05f), Point(0.65f, 0.05f),
            Point(0.75f, 0.27f), Point(0.35f, 0.50f), Point(0.80f, 0.73f),
            Point(0.65f, 0.95f), Point(0.20f, 0.95f)
        ), isTall = true)

        // C - large open curve
        addTemplate('C', listOf(
            Point(0.80f, 0.20f), Point(0.45f, 0.05f), Point(0.20f, 0.50f),
            Point(0.45f, 0.95f), Point(0.80f, 0.80f)
        ))

        // D - vertical stem + large loop
        addTemplate('D', listOf(
            Point(0.25f, 0.95f), Point(0.25f, 0.05f), Point(0.55f, 0.05f),
            Point(0.80f, 0.50f), Point(0.55f, 0.95f), Point(0.25f, 0.95f)
        ), isTall = true)

        // E - vertical + 3 arms
        addTemplate('E', listOf(
            Point(0.80f, 0.05f), Point(0.25f, 0.05f), Point(0.25f, 0.50f),
            Point(0.65f, 0.50f), Point(0.25f, 0.50f), Point(0.25f, 0.95f), Point(0.80f, 0.95f)
        ), isTall = true)

        // F - vertical + 2 arms
        addTemplate('F', listOf(
            Point(0.80f, 0.05f), Point(0.25f, 0.05f), Point(0.25f, 0.50f),
            Point(0.65f, 0.50f), Point(0.25f, 0.50f), Point(0.25f, 0.95f)
        ), isTall = true)

        // G - C shape with shelf
        addTemplate('G', listOf(
            Point(0.80f, 0.20f), Point(0.45f, 0.05f), Point(0.20f, 0.50f),
            Point(0.45f, 0.95f), Point(0.80f, 0.95f), Point(0.80f, 0.55f), Point(0.55f, 0.55f)
        ))

        // H - 2 uprights + crossbar
        addTemplate('H', listOf(
            Point(0.25f, 0.05f), Point(0.25f, 0.95f), Point(0.25f, 0.50f),
            Point(0.75f, 0.50f), Point(0.75f, 0.05f), Point(0.75f, 0.95f)
        ), isTall = true)

        // I - vertical line
        addTemplate('I', listOf(
            Point(0.50f, 0.05f), Point(0.50f, 0.95f)
        ), isTall = true)

        // I - serif top and bottom
        addTemplate('I', listOf(
            Point(0.30f, 0.05f), Point(0.70f, 0.05f), Point(0.50f, 0.05f),
            Point(0.50f, 0.95f), Point(0.30f, 0.95f), Point(0.70f, 0.95f)
        ), isTall = true)

        // J - top bar + hook
        addTemplate('J', listOf(
            Point(0.70f, 0.05f), Point(0.70f, 0.80f), Point(0.50f, 0.95f), Point(0.30f, 0.80f)
        ), isTall = true)

        // K - vertical + angled diagonal arms
        addTemplate('K', listOf(
            Point(0.25f, 0.05f), Point(0.25f, 0.95f), Point(0.25f, 0.50f),
            Point(0.75f, 0.05f), Point(0.25f, 0.50f), Point(0.75f, 0.95f)
        ), isTall = true)

        // L - vertical + bottom base
        addTemplate('L', listOf(
            Point(0.25f, 0.05f), Point(0.25f, 0.95f), Point(0.75f, 0.95f)
        ), isTall = true)

        // M - 4-point zigzag
        addTemplate('M', listOf(
            Point(0.15f, 0.95f), Point(0.15f, 0.05f), Point(0.50f, 0.65f),
            Point(0.85f, 0.05f), Point(0.85f, 0.95f)
        ), isWide = true)

        // N - diagonal connector
        addTemplate('N', listOf(
            Point(0.20f, 0.95f), Point(0.20f, 0.05f), Point(0.80f, 0.95f), Point(0.80f, 0.05f)
        ), isTall = true)

        // O - large closed circle
        addTemplate('O', circlePoints(0.38, 0.42), isClosed = true, weight = 0.94f)

        // P - stem + upper round bowl
        addTemplate('P', listOf(
            Point(0.25f, 0.95f), Point(0.25f, 0.05f), Point(0.70f, 0.05f),
            Point(0.80f, 0.28f), Point(0.70f, 0.50f), Point(0.25f, 0.50f)
        ), isTall = true)

        // Q - large circle + bottom diagonal tail
        addTemplate('Q', circlePoints(0.36, 0.40) + listOf(Point(0.60f, 0.65f), Point(0.85f, 0.95f)), isClosed = true)

        // R - stem + upper bowl + diagonal leg
        addTemplate('R', listOf(
            Point(0.25f, 0.95f), Point(0.25f, 0.05f), Point(0.70f, 0.05f),
            Point(0.80f, 0.28f), Point(0.70f, 0.50f), Point(0.25f, 0.50f),
            Point(0.50f, 0.50f), Point(0.80f, 0.95f)
        ), isTall = true)

        // S - large serpentine
        addTemplate('S', listOf(
            Point(0.75f, 0.20f), Point(0.50f, 0.05f), Point(0.25f, 0.25f),
            Point(0.75f, 0.70f), Point(0.50f, 0.95f), Point(0.25f, 0.80f)
        ))

        // T - top crossbar + vertical stem
        addTemplate('T', listOf(
            Point(0.15f, 0.05f), Point(0.85f, 0.05f), Point(0.50f, 0.05f), Point(0.50f, 0.95f)
        ), isTall = true)

        // U - large cup
        addTemplate('U', listOf(
            Point(0.20f, 0.05f), Point(0.20f, 0.75f), Point(0.50f, 0.95f),
            Point(0.80f, 0.75f), Point(0.80f, 0.05f)
        ), isTall = true)

        // V - sharp wedge
        addTemplate('V', listOf(
            Point(0.15f, 0.05f), Point(0.50f, 0.95f), Point(0.85f, 0.05f)
        ), isTall = true)

        // W - wide double wedge
        addTemplate('W', listOf(
            Point(0.15f, 0.05f), Point(0.35f, 0.95f), Point(0.50f, 0.45f),
            Point(0.65f, 0.95f), Point(0.85f, 0.05f)
        ), isWide = true)

        // X - large cross
        addTemplate('X', listOf(
            Point(0.20f, 0.05f), Point(0.80f, 0.95f), Point(0.80f, 0.05f), Point(0.20f, 0.95f)
        ))

        // Y - diagonal arms meeting central stem
        addTemplate('Y', listOf(
            Point(0.15f, 0.05f), Point(0.50f, 0.55f), Point(0.85f, 0.05f),
            Point(0.50f, 0.55f), Point(0.50f, 0.95f)
        ), isTall = true)

        // Z - large zigzag
        addTemplate('Z', listOf(
            Point(0.20f, 0.05f), Point(0.80f, 0.05f), Point(0.20f, 0.95f), Point(0.80f, 0.95f)
        ))

        // ==========================================
        // DIGITS ('0'..'9')
        // ==========================================

        addTemplate('0', circlePoints(0.30, 0.42), isClosed = true, weight = 1.08f)

        addTemplate('1', listOf(
            Point(0.35f, 0.25f), Point(0.50f, 0.10f), Point(0.50f, 0.95f),
            Point(0.30f, 0.95f), Point(0.70f, 0.95f)
        ), isTall = true)

        addTemplate('2', listOf(
            Point(0.25f, 0.25f), Point(0.50f, 0.10f), Point(0.75f, 0.25f),
            Point(0.25f, 0.85f), Point(0.80f, 0.85f)
        ))

        addTemplate('3', listOf(
            Point(0.25f, 0.15f), Point(0.75f, 0.15f), Point(0.45f, 0.50f),
            Point(0.75f, 0.65f), Point(0.30f, 0.85f)
        ))

        addTemplate('4', listOf(
            Point(0.70f, 0.85f), Point(0.70f, 0.15f),
            Point(0.25f, 0.65f), Point(0.85f, 0.65f)
        ), expectedStrokes = 2)

        addTemplate('5', listOf(
            Point(0.75f, 0.15f), Point(0.30f, 0.15f), Point(0.30f, 0.50f),
            Point(0.75f, 0.60f), Point(0.30f, 0.85f)
        ))

        addTemplate('6', listOf(
            Point(0.70f, 0.15f), Point(0.30f, 0.50f), Point(0.30f, 0.85f),
            Point(0.70f, 0.85f), Point(0.70f, 0.55f), Point(0.30f, 0.55f)
        ), isClosed = true)

        addTemplate('7', listOf(
            Point(0.20f, 0.15f), Point(0.80f, 0.15f), Point(0.45f, 0.90f)
        ))

        addTemplate('8', listOf(
            Point(0.50f, 0.50f), Point(0.30f, 0.30f), Point(0.50f, 0.15f),
            Point(0.70f, 0.30f), Point(0.50f, 0.50f), Point(0.30f, 0.70f),
            Point(0.50f, 0.85f), Point(0.70f, 0.70f), Point(0.50f, 0.50f)
        ), isClosed = true)

        addTemplate('9', listOf(
            Point(0.70f, 0.50f), Point(0.30f, 0.50f), Point(0.30f, 0.15f),
            Point(0.70f, 0.15f), Point(0.70f, 0.90f), Point(0.35f, 0.90f)
        ), isClosed = true)

        return list
    }
}

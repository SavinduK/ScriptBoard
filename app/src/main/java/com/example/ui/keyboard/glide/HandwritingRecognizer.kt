package com.example.ui.keyboard.glide

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * High-performance, offline Handwriting and Glide Stroke Recognizer.
 * Supports single-letter and multi-letter writing with finger or stylus.
 * Uses normalized point-cloud distance matching combined with structural
 * geometric feature analysis and dictionary disambiguation.
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

    // Predefined canonical templates resampled to 32 points in normalized [0, 1] unit square
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

        // 1. Cluster strokes into individual letter groups based on horizontal separation
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

        // 3. Score against vocabulary if multiple letters or search for best word match
        val matchedWords = findBestWordMatches(clusterCandidates, rawWord, commonVocabulary)

        val primaryWord = matchedWords.firstOrNull() ?: rawWord
        val suggestions = matchedWords.filter { it.isNotBlank() && !it.equals(primaryWord, ignoreCase = true) }
            .take(4)

        return RecognitionResult(
            primaryWord = primaryWord,
            suggestions = suggestions,
            recognizedLetters = topLetters
        )
    }

    /**
     * Groups strokes into separate letters based on horizontal bounds and drawing flow.
     */
    private fun clusterStrokes(strokes: List<List<Offset>>): List<List<List<Point>>> {
        val validStrokes = strokes.filter { it.size >= 2 }.map { stroke ->
            stroke.map { Point(it.x, it.y) }
        }
        if (validStrokes.isEmpty()) return emptyList()

        if (validStrokes.size == 1) {
            return listOf(validStrokes)
        }

        // Calculate bounding boxes for each stroke
        data class StrokeInfo(
            val points: List<Point>,
            val minX: Float,
            val maxX: Float,
            val minY: Float,
            val maxY: Float
        ) {
            val width = (maxX - minX).coerceAtLeast(1f)
            val height = (maxY - minY).coerceAtLeast(1f)
            val centerX = (minX + maxX) / 2f
        }

        val strokeInfos = validStrokes.map { pts ->
            val minX = pts.minOf { it.x }
            val maxX = pts.maxOf { it.x }
            val minY = pts.minOf { it.y }
            val maxY = pts.maxOf { it.y }
            StrokeInfo(pts, minX, maxX, minY, maxY)
        }

        // Calculate median height to define clustering proximity threshold
        val medianHeight = strokeInfos.map { it.height }.sorted()[strokeInfos.size / 2]
        val clusterThreshold = (medianHeight * 0.35f).coerceIn(24f, 90f)

        // Cluster strokes that overlap horizontally or are within threshold
        val clusters = mutableListOf<MutableList<StrokeInfo>>()

        // Sort by minX so letters are processed from left to right
        val sortedStrokes = strokeInfos.sortedBy { it.minX }

        for (stroke in sortedStrokes) {
            if (clusters.isEmpty()) {
                clusters.add(mutableListOf(stroke))
                continue
            }

            val lastCluster = clusters.last()
            val clusterMinX = lastCluster.minOf { it.minX }
            val clusterMaxX = lastCluster.maxOf { it.maxX }

            // Check if this stroke overlaps with the cluster or is within clusterThreshold
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
        val aspectRatio = height / width // > 2.0 = tall & narrow, < 0.6 = wide

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

        // 1. Point Cloud Distance matching with templates
        for ((char, templatePts) in templates) {
            val dist = pointCloudDistance(centered, templatePts)
            var penalty = 0f

            // Adjust score based on structural heuristics
            val strokeCount = strokes.size

            // Two-stroke letters like 't', 'x', 'i', 'j', 'k'
            if (char in listOf('t', 'x', 'i', 'j', 'k', 'X', 'T', 'K')) {
                if (strokeCount == 2) penalty -= 0.15f
                else if (strokeCount == 1) penalty += 0.20f
            }

            // Single stroke smooth letters like 'c', 'o', 's', 'l', 'u', 'v', 'w', 'z'
            if (char in listOf('c', 'o', 's', 'l', 'u', 'v', 'w', 'z', 'O', 'C', 'S', 'U', 'V', 'W', 'Z')) {
                if (strokeCount == 1) penalty -= 0.10f
                else if (strokeCount > 2) penalty += 0.35f
            }

            // Aspect ratio heuristics
            if (char in listOf('l', 'i', '1', 'I', 't', 'j')) {
                if (aspectRatio > 1.8f) penalty -= 0.20f
                else if (aspectRatio < 0.9f) penalty += 0.30f
            } else if (char in listOf('m', 'w', 'W', 'M')) {
                if (aspectRatio < 1.1f) penalty -= 0.15f
            }

            // Closed loop check for 'o', '0', 'O', '8', 'e', 'a'
            if (char in listOf('o', 'O', '0', '8')) {
                val start = resampled.first()
                val end = resampled.last()
                val startEndDist = hypot(start.x - end.x, start.y - end.y)
                if (startEndDist < 0.35f) penalty -= 0.30f
                else penalty += 0.25f
            }

            val finalScore = (dist + penalty).coerceAtLeast(0.01f)
            scores.add(LetterCandidate(char, finalScore))
        }

        return scores.sortedBy { it.score }.take(5)
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
     * Disambiguates letter candidates against common English dictionary words.
     */
    private fun findBestWordMatches(
        clusterCandidates: List<List<LetterCandidate>>,
        rawWord: String,
        vocabulary: List<String>
    ): List<String> {
        val results = mutableListOf<String>()

        if (vocabulary.isEmpty() || clusterCandidates.isEmpty()) {
            results.add(rawWord)
            return results
        }

        val wordLen = clusterCandidates.size

        // If single letter:
        if (wordLen == 1) {
            val primaryChar = clusterCandidates[0].firstOrNull()?.char?.toString() ?: rawWord
            results.add(primaryChar)
            // Add top 3 words starting with this letter
            vocabulary.filter { it.startsWith(primaryChar, ignoreCase = true) }
                .take(3)
                .forEach { if (!results.contains(it)) results.add(it) }
            return results
        }

        // Check if rawWord directly exists in vocabulary
        val exactMatch = vocabulary.firstOrNull { it.equals(rawWord, ignoreCase = true) }
        if (exactMatch != null) {
            results.add(exactMatch)
        }

        // Try candidate permutations for short words (up to 4 letters)
        if (wordLen in 2..5) {
            val candidateSets = clusterCandidates.map { it.take(3).map { c -> c.char.lowercaseChar() } }
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

            // Match against vocabulary with length prioritization
            for (combo in possibleCombinations) {
                val vocabMatch = vocabulary.firstOrNull { it.equals(combo, ignoreCase = true) }
                if (vocabMatch != null && !results.contains(vocabMatch)) {
                    results.add(vocabMatch)
                }
            }
        }

        // Add prefix completions from vocabulary
        for (vocab in vocabulary) {
            if (vocab.startsWith(rawWord, ignoreCase = true) && !results.contains(vocab)) {
                results.add(vocab)
                if (results.size >= 5) break
            }
        }

        if (results.isEmpty() || !results.contains(rawWord)) {
            results.add(0, rawWord)
        }

        return results
    }

    /**
     * Generates normalized 32-point canonical templates for letters and symbols.
     */
    private fun generateCanonicalTemplates(): Map<Char, List<Point>> {
        val map = mutableMapOf<Char, List<Point>>()

        fun createTemplate(char: Char, rawPoints: List<Point>) {
            val resampled = resamplePoints(rawPoints, 32)
            map[char] = centerPoints(resampled)
        }

        // a: loop counter-clockwise from (0.7, 0.4) down to (0.3, 0.8) then stem down (0.7, 0.4) to (0.7, 0.9)
        createTemplate('a', listOf(
            Point(0.7f, 0.4f), Point(0.4f, 0.35f), Point(0.2f, 0.6f),
            Point(0.4f, 0.85f), Point(0.7f, 0.65f), Point(0.7f, 0.4f), Point(0.7f, 0.9f)
        ))

        // b: stem down then loop right
        createTemplate('b', listOf(
            Point(0.25f, 0.1f), Point(0.25f, 0.9f), Point(0.25f, 0.5f),
            Point(0.65f, 0.5f), Point(0.75f, 0.7f), Point(0.65f, 0.9f), Point(0.25f, 0.9f)
        ))

        // c: arc from top-right to bottom-right
        createTemplate('c', listOf(
            Point(0.75f, 0.3f), Point(0.5f, 0.2f), Point(0.25f, 0.5f),
            Point(0.5f, 0.8f), Point(0.75f, 0.7f)
        ))

        // d: loop left then stem down
        createTemplate('d', listOf(
            Point(0.75f, 0.5f), Point(0.35f, 0.5f), Point(0.25f, 0.7f),
            Point(0.35f, 0.9f), Point(0.75f, 0.9f), Point(0.75f, 0.1f), Point(0.75f, 0.9f)
        ))

        // e: horizontal bar then arc left and down
        createTemplate('e', listOf(
            Point(0.25f, 0.55f), Point(0.75f, 0.55f), Point(0.5f, 0.25f),
            Point(0.25f, 0.55f), Point(0.4f, 0.85f), Point(0.75f, 0.8f)
        ))

        // f: hook top down and cross
        createTemplate('f', listOf(
            Point(0.7f, 0.2f), Point(0.5f, 0.1f), Point(0.45f, 0.9f),
            Point(0.25f, 0.45f), Point(0.65f, 0.45f)
        ))

        // g: circle top + descender hook bottom
        createTemplate('g', listOf(
            Point(0.7f, 0.35f), Point(0.35f, 0.35f), Point(0.3f, 0.6f),
            Point(0.7f, 0.6f), Point(0.7f, 0.35f), Point(0.7f, 0.95f), Point(0.35f, 0.95f)
        ))

        // h: stem down then arch
        createTemplate('h', listOf(
            Point(0.25f, 0.1f), Point(0.25f, 0.9f), Point(0.25f, 0.5f),
            Point(0.5f, 0.4f), Point(0.75f, 0.55f), Point(0.75f, 0.9f)
        ))

        // i: vertical line + dot
        createTemplate('i', listOf(
            Point(0.5f, 0.35f), Point(0.5f, 0.9f), Point(0.5f, 0.18f), Point(0.52f, 0.22f)
        ))

        // j: hook with dot
        createTemplate('j', listOf(
            Point(0.6f, 0.35f), Point(0.6f, 0.9f), Point(0.35f, 0.95f),
            Point(0.6f, 0.18f), Point(0.62f, 0.22f)
        ))

        // k: vertical stem + diagonal arms
        createTemplate('k', listOf(
            Point(0.3f, 0.1f), Point(0.3f, 0.9f), Point(0.7f, 0.35f),
            Point(0.3f, 0.6f), Point(0.7f, 0.9f)
        ))

        // l: straight vertical line
        createTemplate('l', listOf(
            Point(0.5f, 0.1f), Point(0.5f, 0.5f), Point(0.5f, 0.9f)
        ))

        // m: two arches
        createTemplate('m', listOf(
            Point(0.15f, 0.9f), Point(0.15f, 0.4f), Point(0.35f, 0.35f), Point(0.5f, 0.6f),
            Point(0.5f, 0.9f), Point(0.5f, 0.4f), Point(0.75f, 0.35f), Point(0.85f, 0.9f)
        ))

        // n: one arch
        createTemplate('n', listOf(
            Point(0.25f, 0.9f), Point(0.25f, 0.4f), Point(0.5f, 0.35f),
            Point(0.75f, 0.55f), Point(0.75f, 0.9f)
        ))

        // o: smooth circle
        createTemplate('o', (0..16).map { i ->
            val angle = (i.toDouble() / 16.0) * 2.0 * Math.PI
            Point((0.5 + 0.35 * Math.cos(angle)).toFloat(), (0.5 + 0.35 * Math.sin(angle)).toFloat())
        })
        createTemplate('O', (0..16).map { i ->
            val angle = (i.toDouble() / 16.0) * 2.0 * Math.PI
            Point((0.5 + 0.38 * Math.cos(angle)).toFloat(), (0.5 + 0.42 * Math.sin(angle)).toFloat())
        })

        // p: stem down below baseline + loop top
        createTemplate('p', listOf(
            Point(0.25f, 0.35f), Point(0.25f, 0.95f), Point(0.25f, 0.35f),
            Point(0.65f, 0.35f), Point(0.75f, 0.55f), Point(0.65f, 0.7f), Point(0.25f, 0.7f)
        ))

        // q: loop + descender
        createTemplate('q', listOf(
            Point(0.7f, 0.35f), Point(0.35f, 0.35f), Point(0.3f, 0.55f),
            Point(0.7f, 0.7f), Point(0.7f, 0.35f), Point(0.7f, 0.95f)
        ))

        // r: stem down + hook right
        createTemplate('r', listOf(
            Point(0.3f, 0.4f), Point(0.3f, 0.9f), Point(0.3f, 0.55f),
            Point(0.55f, 0.4f), Point(0.75f, 0.45f)
        ))

        // s: s-curve
        createTemplate('s', listOf(
            Point(0.75f, 0.35f), Point(0.45f, 0.25f), Point(0.3f, 0.45f),
            Point(0.65f, 0.65f), Point(0.55f, 0.85f), Point(0.25f, 0.75f)
        ))

        // t: cross vertical + horizontal
        createTemplate('t', listOf(
            Point(0.5f, 0.15f), Point(0.5f, 0.85f), Point(0.65f, 0.85f),
            Point(0.25f, 0.4f), Point(0.75f, 0.4f)
        ))

        // u: cup shape
        createTemplate('u', listOf(
            Point(0.25f, 0.35f), Point(0.25f, 0.75f), Point(0.5f, 0.85f),
            Point(0.75f, 0.75f), Point(0.75f, 0.35f), Point(0.75f, 0.9f)
        ))

        // v: point down
        createTemplate('v', listOf(
            Point(0.2f, 0.3f), Point(0.5f, 0.85f), Point(0.8f, 0.3f)
        ))

        // w: double v
        createTemplate('w', listOf(
            Point(0.15f, 0.3f), Point(0.35f, 0.85f), Point(0.5f, 0.5f),
            Point(0.65f, 0.85f), Point(0.85f, 0.3f)
        ))

        // x: two crossing diagonals
        createTemplate('x', listOf(
            Point(0.25f, 0.25f), Point(0.75f, 0.85f),
            Point(0.75f, 0.25f), Point(0.25f, 0.85f)
        ))

        // y: v shape + descending tail
        createTemplate('y', listOf(
            Point(0.25f, 0.35f), Point(0.5f, 0.65f), Point(0.75f, 0.35f),
            Point(0.5f, 0.65f), Point(0.3f, 0.95f)
        ))

        // z: zigzag
        createTemplate('z', listOf(
            Point(0.25f, 0.35f), Point(0.75f, 0.35f), Point(0.25f, 0.85f), Point(0.75f, 0.85f)
        ))

        // Uppercase templates:
        createTemplate('A', listOf(
            Point(0.15f, 0.9f), Point(0.5f, 0.1f), Point(0.85f, 0.9f),
            Point(0.3f, 0.6f), Point(0.7f, 0.6f)
        ))
        createTemplate('T', listOf(
            Point(0.15f, 0.15f), Point(0.85f, 0.15f),
            Point(0.5f, 0.15f), Point(0.5f, 0.9f)
        ))
        createTemplate('I', listOf(
            Point(0.3f, 0.15f), Point(0.7f, 0.15f),
            Point(0.5f, 0.15f), Point(0.5f, 0.85f),
            Point(0.3f, 0.85f), Point(0.7f, 0.85f)
        ))
        createTemplate('H', listOf(
            Point(0.25f, 0.1f), Point(0.25f, 0.9f),
            Point(0.25f, 0.5f), Point(0.75f, 0.5f),
            Point(0.75f, 0.1f), Point(0.75f, 0.9f)
        ))
        createTemplate('E', listOf(
            Point(0.75f, 0.15f), Point(0.25f, 0.15f), Point(0.25f, 0.85f),
            Point(0.75f, 0.85f), Point(0.25f, 0.5f), Point(0.65f, 0.5f)
        ))

        return map
    }
}

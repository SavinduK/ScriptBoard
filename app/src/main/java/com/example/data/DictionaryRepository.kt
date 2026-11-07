package com.example.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Locale
import kotlin.math.abs

/**
 * Compact, fast in-memory prefix dictionary repository for KeyPro.
 * Loads 'assets/dictionaries/en_freq.txt' and 'assets/dictionaries/si_freq.txt'
 * once on a background thread and caches sorted prefix indices for instantaneous lookups.
 */
class DictionaryRepository private constructor(private val appContext: Context) {

    data class WordEntry(val word: String, val count: Long)

    private val mutex = Mutex()
    private var isLoaded = false

    // Sorted by lowercase word for binary search prefix matching
    private var enWordsSorted: List<WordEntry> = emptyList()
    private var siWordsSorted: List<WordEntry> = emptyList()

    // Quick exact count lookup maps
    private var enCountMap: Map<String, Long> = emptyMap()
    private var siCountMap: Map<String, Long> = emptyMap()

    // Fallback starter words if dictionary fails to load
    private val fallbackEnglishStarters = listOf("the", "to", "and", "a", "in", "it", "is", "i", "that", "you")
    private val fallbackSinhalaStarters = listOf("මම", "ඔබ", "ඔයා", "අපි", "ඔව්", "නැහැ", "ආයුබෝවන්", "ස්තූතියි", "හොඳයි")

    suspend fun ensureLoaded() = withContext(Dispatchers.IO) {
        if (isLoaded) return@withContext
        mutex.withLock {
            if (isLoaded) return@withLock
            val enList = mutableListOf<WordEntry>()
            val enMap = mutableMapOf<String, Long>()
            try {
                appContext.assets.open("dictionaries/en_freq.txt").use { input ->
                    BufferedReader(InputStreamReader(input, Charsets.UTF_8)).useLines { lines ->
                        for (line in lines) {
                            val trimmed = line.trim()
                            if (trimmed.isEmpty()) continue
                            val parts = trimmed.split('\t')
                            if (parts.size >= 2) {
                                val w = parts[0].trim().lowercase(Locale.ROOT)
                                val c = parts[1].trim().toLongOrNull() ?: 1L
                                if (w.isNotEmpty()) {
                                    val entry = WordEntry(w, c)
                                    enList.add(entry)
                                    enMap[w] = c
                                }
                            }
                        }
                    }
                }
            } catch (_: Exception) {}

            val siList = mutableListOf<WordEntry>()
            val siMap = mutableMapOf<String, Long>()
            try {
                appContext.assets.open("dictionaries/si_freq.txt").use { input ->
                    BufferedReader(InputStreamReader(input, Charsets.UTF_8)).useLines { lines ->
                        for (line in lines) {
                            val trimmed = line.trim()
                            if (trimmed.isEmpty()) continue
                            val parts = trimmed.split('\t')
                            if (parts.size >= 2) {
                                val w = parts[0].trim()
                                val c = parts[1].trim().toLongOrNull() ?: 1L
                                if (w.isNotEmpty()) {
                                    val entry = WordEntry(w, c)
                                    siList.add(entry)
                                    siMap[w] = c
                                }
                            }
                        }
                    }
                }
            } catch (_: Exception) {}

            enWordsSorted = enList.sortedBy { it.word }
            siWordsSorted = siList.sortedBy { it.word }
            enCountMap = enMap
            siCountMap = siMap
            isLoaded = true
        }
    }

    fun getExactCount(word: String, language: String): Long {
        val lower = word.lowercase(Locale.ROOT)
        return if (isSinhalaLanguage(language)) {
            siCountMap[word] ?: siCountMap[lower] ?: 0L
        } else {
            enCountMap[lower] ?: 0L
        }
    }

    /**
     * Binary search prefix matching in sorted array.
     */
    fun findPrefixMatches(
        prefix: String,
        language: String,
        limit: Int = 15
    ): List<WordEntry> {
        val targetList = if (isSinhalaLanguage(language)) siWordsSorted else enWordsSorted
        if (targetList.isEmpty()) {
            val fallbacks = if (isSinhalaLanguage(language)) fallbackSinhalaStarters else fallbackEnglishStarters
            return fallbacks.filter { it.startsWith(prefix) }.map { WordEntry(it, 100L) }
        }

        val cleanPrefix = if (isSinhalaLanguage(language)) prefix else prefix.lowercase(Locale.ROOT)
        val startIndex = binarySearchPrefixStart(targetList, cleanPrefix)
        if (startIndex < 0 || startIndex >= targetList.size) return emptyList()

        val matches = mutableListOf<WordEntry>()
        var idx = startIndex
        while (idx < targetList.size && matches.size < 50) {
            val item = targetList[idx]
            if (item.word.startsWith(cleanPrefix)) {
                matches.add(item)
                idx++
            } else {
                break
            }
        }

        // Rank prefix matches by dictionary frequency count descending
        return matches.sortedByDescending { it.count }.take(limit)
    }

    /**
     * Returns edit-distance-1 candidates when prefix yields fewer than 3 results.
     */
    fun findTypoMatches(
        prefix: String,
        language: String,
        limit: Int = 5
    ): List<WordEntry> {
        val targetList = if (isSinhalaLanguage(language)) siWordsSorted else enWordsSorted
        if (targetList.isEmpty() || prefix.length < 3) return emptyList()

        val clean = if (isSinhalaLanguage(language)) prefix else prefix.lowercase(Locale.ROOT)
        val candidates = mutableListOf<WordEntry>()

        for (item in targetList) {
            if (abs(item.word.length - clean.length) <= 1) {
                if (isEditDistanceOne(clean, item.word)) {
                    candidates.add(item)
                    if (candidates.size >= 25) break
                }
            }
        }

        return candidates.sortedByDescending { it.count }.take(limit)
    }

    /**
     * Returns starter words for empty buffer matching the specified language.
     */
    fun getStarterWords(language: String, limit: Int = 5): List<String> {
        val targetList = if (isSinhalaLanguage(language)) siWordsSorted else enWordsSorted
        if (targetList.isNotEmpty()) {
            return targetList.sortedByDescending { it.count }.take(limit).map { it.word }
        }
        return if (isSinhalaLanguage(language)) fallbackSinhalaStarters.take(limit) else fallbackEnglishStarters.take(limit)
    }

    private fun binarySearchPrefixStart(list: List<WordEntry>, prefix: String): Int {
        var low = 0
        var high = list.size - 1
        var result = -1

        while (low <= high) {
            val mid = (low + high) ushr 1
            val midWord = list[mid].word
            if (midWord >= prefix) {
                result = mid
                high = mid - 1
            } else {
                low = mid + 1
            }
        }
        return result
    }

    private fun isEditDistanceOne(s1: String, s2: String): Boolean {
        val len1 = s1.length
        val len2 = s2.length
        if (abs(len1 - len2) > 1) return false

        var i = 0
        var j = 0
        var edits = 0

        while (i < len1 && j < len2) {
            if (s1[i] != s2[j]) {
                if (edits++ > 0) return false
                if (len1 > len2) {
                    i++
                } else if (len2 > len1) {
                    j++
                } else {
                    i++
                    j++
                }
            } else {
                i++
                j++
            }
        }

        if (i < len1 || j < len2) edits++
        return edits == 1
    }

    companion object {
        fun isSinhalaChar(c: Char): Boolean {
            return (c in '\u0D80'..'\u0DFF') || c == '\u200D' || c == '\u200C'
        }

        fun isSinhalaWord(word: String): Boolean {
            if (word.isEmpty()) return false
            return word.any { it in '\u0D80'..'\u0DFF' } && word.all { isSinhalaChar(it) || it in "'-" }
        }

        fun isSinhalaLanguage(lang: String): Boolean {
            return lang == "si" || lang == "si-trans"
        }

        @Volatile
        private var instance: DictionaryRepository? = null

        fun getInstance(context: Context): DictionaryRepository {
            return instance ?: synchronized(this) {
                instance ?: DictionaryRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}

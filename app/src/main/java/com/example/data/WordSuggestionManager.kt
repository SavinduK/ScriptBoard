package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.ln
import kotlin.math.min
import kotlin.math.pow

data class ScoringConfig(
    val wStatic: Double = 1.0,
    val wUser: Double = 2.0,
    val wBigram: Double = 3.0,
    val prefixBonus: Double = 5.0
)

/**
 * Intelligent, privacy-preserving Word Suggestion and Next-Word Prediction Manager.
 * Implements:
 * - Dynamic prevWord reading without stale state.
 * - Formula: score = w_static*log(1+dictCount) + w_user*log(1+min(userCount,50))*recencyDecay + w_bigram*log(1+bigramCount) + prefixBonus
 * - Write-time recency decay (halved per 30 days) stored directly in Room.
 * - Strict language separation: 'en' displays only English; 'si' / 'si-trans' display only Sinhala (plus personal-dict user words).
 * - Correction pairs (typed -> corrected) for learning user typos and custom expansions (e.g. omw -> on my way).
 * - Light typo tolerance with edit-distance-1 lookup when prefix yields < 3 results.
 */
class WordSuggestionManager(
    private val wordFrequencyDao: WordFrequencyDao,
    private val bigramFrequencyDao: BigramFrequencyDao? = null,
    private val correctionPairDao: CorrectionPairDao? = null,
    private val translitChoiceDao: TranslitChoiceDao? = null,
    private val dictionaryRepo: DictionaryRepository? = null,
    val scoringConfig: ScoringConfig = ScoringConfig()
) {

    companion object {
        const val MAX_USER_FREQ_CAP = 50.0

        fun isSinhalaChar(c: Char): Boolean {
            return (c in '\u0D80'..'\u0DFF') || c == '\u200D' || c == '\u200C'
        }

        fun isSinhalaWord(word: String): Boolean {
            if (word.isEmpty()) return false
            return word.any { it in '\u0D80'..'\u0DFF' } && word.all { isSinhalaChar(it) || it in "'-" }
        }

        fun isEnglishWord(word: String): Boolean {
            return !isSinhalaWord(word)
        }

        fun isInvalidToLearn(word: String): Boolean {
            val trimmed = word.trim()
            if (trimmed.length < 2) return true
            if (trimmed.all { it.isDigit() }) return true
            if (trimmed.contains("@") && trimmed.contains(".")) return true
            if (trimmed.startsWith("http://", ignoreCase = true) ||
                trimmed.startsWith("https://", ignoreCase = true) ||
                trimmed.startsWith("www.", ignoreCase = true)
            ) return true
            return false
        }
    }

    private fun computeRecencyDecay(lastUsedMillis: Long): Double {
        val daysSince = (System.currentTimeMillis() - lastUsedMillis).coerceAtLeast(0L) / (1000.0 * 60 * 60 * 24)
        return 0.5.pow(daysSince / 30.0)
    }

    private fun computeWriteTimeDecayedCount(oldCount: Int, lastUsedMillis: Long, now: Long): Int {
        val daysSince = (now - lastUsedMillis).coerceAtLeast(0L) / (1000.0 * 60 * 60 * 24)
        val decayed = (oldCount * 0.5.pow(daysSince / 30.0)).toInt()
        return (decayed + 1).coerceAtLeast(1)
    }

    private fun hasCustomCasing(str: String): Boolean {
        if (str.length < 2) return false
        val hasUpperAfterFirst = str.drop(1).any { it.isUpperCase() }
        val hasLower = str.any { it.isLowerCase() }
        val hasUpper = str.any { it.isUpperCase() }
        return hasUpperAfterFirst || (hasUpper && hasLower && str.first().isLowerCase())
    }

    private fun applyCasing(typedPrefix: String, word: String, originalCase: String?): String {
        if (!originalCase.isNullOrBlank() && hasCustomCasing(originalCase)) {
            return originalCase
        }
        return when {
            typedPrefix.length >= 2 && typedPrefix.all { it.isUpperCase() } -> {
                word.uppercase(Locale.ROOT)
            }
            typedPrefix.firstOrNull()?.isUpperCase() == true -> {
                word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
            }
            else -> {
                word.lowercase(Locale.ROOT)
            }
        }
    }

    suspend fun getSuggestions(
        prefix: String,
        language: String = "en",
        prevWord: String? = null
    ): List<String> = withContext(Dispatchers.IO) {
        dictionaryRepo?.ensureLoaded()
        val cleanPrefix = prefix.trim()
        val isSinhalaLang = language == "si" || language == "si-trans"

        // 1. EMPTY PREFIX: Rank by bigram and user frequency only
        if (cleanPrefix.isEmpty()) {
            val suggestions = mutableListOf<String>()

            // 1A. Bigram predictions based on current dynamic prevWord
            if (!prevWord.isNullOrBlank() && bigramFrequencyDao != null) {
                try {
                    val cleanPrev = prevWord.trim().lowercase(Locale.ROOT)
                    val continuations = bigramFrequencyDao.getTopContinuations(cleanPrev, limit = 10)
                    for (item in continuations) {
                        if (suggestions.size >= 3) break
                        val matchesLang = if (isSinhalaLang) isSinhalaWord(item.word) else isEnglishWord(item.word)
                        if (matchesLang && !suggestions.contains(item.word)) {
                            suggestions.add(item.word)
                        }
                    }
                } catch (_: Exception) {}
            }

            // 1B. Top user history words matching the active keyboard language
            if (suggestions.size < 3) {
                try {
                    val topLearned = wordFrequencyDao.getTopLearnedWords(minFrequency = 2, limit = 20)
                    for (entity in topLearned) {
                        if (suggestions.size >= 3) break
                        val matchesLang = if (isSinhalaLang) isSinhalaWord(entity.word) else isEnglishWord(entity.word)
                        if (matchesLang && !suggestions.contains(entity.word)) {
                            val text = if (hasCustomCasing(entity.originalCase)) entity.originalCase else entity.word
                            suggestions.add(text)
                        }
                    }
                } catch (_: Exception) {}
            }

            // 1C. Fallback to dictionary starter words if needed
            if (suggestions.size < 3) {
                val dictStarters = dictionaryRepo?.getStarterWords(language, limit = 5) ?: emptyList()
                for (starter in dictStarters) {
                    if (suggestions.size >= 3) break
                    if (!suggestions.any { it.equals(starter, ignoreCase = true) }) {
                        suggestions.add(starter)
                    }
                }
            }

            return@withContext suggestions.take(3)
        }

        val prefixLower = cleanPrefix.lowercase(Locale.ROOT)

        data class CandidateInfo(
            val word: String,
            val originalCase: String?,
            var userFrequency: Int = 0,
            var lastUsed: Long = 0L,
            var bigramCount: Int = 0,
            var dictCount: Long = 0L,
            var isPersonalWord: Boolean = false,
            var isExactPrefix: Boolean = false,
            var isCorrectionBoost: Boolean = false,
            var correctionCount: Int = 0,
            var isTypoMatch: Boolean = false
        )

        val candidateMap = mutableMapOf<String, CandidateInfo>()

        // 2A. Transliteration keyboard (si-trans)
        if (language == "si-trans") {
            val translitSuggestions = com.example.ui.keyboard.util.SinhalaTransliterator.getSuggestions(
                prefix = cleanPrefix,
                translitChoiceDao = translitChoiceDao,
                limit = 5
            )
            for ((idx, transWord) in translitSuggestions.withIndex()) {
                candidateMap[transWord] = CandidateInfo(
                    word = transWord,
                    originalCase = transWord,
                    dictCount = if (idx == 0) 50000L else 10000L,
                    isExactPrefix = idx == 0
                )
            }
        }

        // 2B. Learned correction pairs (covers personal typos and abbreviations like omw -> on my way)
        if (correctionPairDao != null) {
            try {
                val corrections = correctionPairDao.getCorrections(cleanPrefix, language)
                for (cp in corrections) {
                    candidateMap[cp.corrected.lowercase(Locale.ROOT)] = CandidateInfo(
                        word = cp.corrected,
                        originalCase = cp.corrected,
                        userFrequency = cp.count * 10,
                        lastUsed = cp.lastUsed,
                        isCorrectionBoost = true,
                        correctionCount = cp.count
                    )
                }
            } catch (_: Exception) {}
        }

        // 2C. User learned frequency words for prefix
        try {
            val learned = wordFrequencyDao.getLearnedSuggestions(prefixLower, minFrequency = 1, limit = 20)
            for (item in learned) {
                val isSinhala = isSinhalaWord(item.word)
                val matchesLang = if (isSinhalaLang) isSinhala else !isSinhala
                // Exception rule: personal-dictionary words typed on this keyboard may appear even if in another script
                val allowPersonal = item.frequency >= 2
                if (matchesLang || allowPersonal) {
                    val key = item.word.lowercase(Locale.ROOT)
                    val existing = candidateMap[key]
                    if (existing != null) {
                        existing.userFrequency = item.frequency
                        existing.lastUsed = item.lastUsed
                        existing.isPersonalWord = !matchesLang && allowPersonal
                    } else {
                        candidateMap[key] = CandidateInfo(
                            word = item.word,
                            originalCase = item.originalCase,
                            userFrequency = item.frequency,
                            lastUsed = item.lastUsed,
                            isPersonalWord = !matchesLang && allowPersonal,
                            isExactPrefix = item.word.equals(cleanPrefix, ignoreCase = true)
                        )
                    }
                }
            }
        } catch (_: Exception) {}

        // 2D. Bigram count lookup for candidates if prevWord exists
        if (!prevWord.isNullOrBlank() && bigramFrequencyDao != null) {
            try {
                val cleanPrev = prevWord.trim().lowercase(Locale.ROOT)
                for (info in candidateMap.values) {
                    val bg = bigramFrequencyDao.getBigram(cleanPrev, info.word.lowercase(Locale.ROOT))
                    if (bg != null) {
                        info.bigramCount = bg.count
                    }
                }
            } catch (_: Exception) {}
        }

        // 2E. Bundled static dictionary prefix matching
        if (dictionaryRepo != null) {
            val dictMatches = dictionaryRepo.findPrefixMatches(cleanPrefix, language, limit = 20)
            for (entry in dictMatches) {
                val key = entry.word.lowercase(Locale.ROOT)
                val existing = candidateMap[key]
                if (existing != null) {
                    existing.dictCount = entry.count
                    if (entry.word.equals(cleanPrefix, ignoreCase = true)) {
                        existing.isExactPrefix = true
                    }
                } else {
                    candidateMap[key] = CandidateInfo(
                        word = entry.word,
                        originalCase = entry.word,
                        dictCount = entry.count,
                        isExactPrefix = entry.word.equals(cleanPrefix, ignoreCase = true)
                    )
                }
            }

            // 2F. Typo tolerance: if prefix yields fewer than 3 results, add edit-distance-1 candidates
            if (candidateMap.size < 3 && cleanPrefix.length >= 3) {
                val typoMatches = dictionaryRepo.findTypoMatches(cleanPrefix, language, limit = 5)
                for (entry in typoMatches) {
                    val key = entry.word.lowercase(Locale.ROOT)
                    if (!candidateMap.containsKey(key)) {
                        candidateMap[key] = CandidateInfo(
                            word = entry.word,
                            originalCase = entry.word,
                            dictCount = entry.count / 2, // slight penalty for typo correction
                            isTypoMatch = true
                        )
                    }
                }
            }
        }

        // 3. SCORING
        // score = w_static*log(1+dictCount) + w_user*log(1+min(userCount,50))*recencyDecay + w_bigram*log(1+bigramCount) + prefixBonus
        data class ScoredCandidate(val text: String, val score: Double)

        val scored = candidateMap.values.map { info ->
            val staticScore = if (info.dictCount > 0L) {
                scoringConfig.wStatic * ln(1.0 + info.dictCount.toDouble())
            } else 0.0

            val userDecay = if (info.lastUsed > 0L) computeRecencyDecay(info.lastUsed) else 1.0
            val userScore = if (info.userFrequency > 0) {
                scoringConfig.wUser * ln(1.0 + min(info.userFrequency.toDouble(), MAX_USER_FREQ_CAP)) * userDecay
            } else 0.0

            val bigramScore = if (info.bigramCount > 0) {
                scoringConfig.wBigram * ln(1.0 + info.bigramCount.toDouble())
            } else 0.0

            val prefixBonus = if (info.isExactPrefix) scoringConfig.prefixBonus else 0.0
            val correctionBonus = if (info.isCorrectionBoost) 8.0 * ln(1.0 + info.correctionCount) else 0.0

            val rawScore = staticScore + userScore + bigramScore + prefixBonus + correctionBonus

            val formattedText = if (isSinhalaWord(info.word)) {
                info.word
            } else {
                applyCasing(cleanPrefix, info.word, info.originalCase)
            }

            ScoredCandidate(formattedText, rawScore)
        }.sortedByDescending { it.score }

        // Filter: English vs Sinhala separation (except personal user words)
        val filtered = scored.map { it.text }
            .filter { word ->
                if (isSinhalaLang) {
                    isSinhalaWord(word) || candidateMap[word.lowercase(Locale.ROOT)]?.isPersonalWord == true
                } else {
                    isEnglishWord(word) || candidateMap[word.lowercase(Locale.ROOT)]?.isPersonalWord == true
                }
            }
            .distinct()

        val results = mutableListOf<String>()
        results.addAll(filtered.take(3))

        // If fewer than 3, pad strictly from the active language starter words
        if (results.size < 3) {
            val starters = dictionaryRepo?.getStarterWords(language, limit = 5) ?: emptyList()
            for (st in starters) {
                if (results.size >= 3) break
                val matches = if (isSinhalaLang) isSinhalaWord(st) else isEnglishWord(st)
                if (matches && !results.contains(st)) {
                    results.add(st)
                }
            }
        }

        return@withContext results.take(3)
    }

    /**
     * Records a word used with write-time recency decay.
     */
    suspend fun recordWordUsed(
        word: String,
        prevWord: String? = null,
        isPrivateOrSensitive: Boolean = false
    ) = withContext(Dispatchers.IO) {
        if (isPrivateOrSensitive) return@withContext

        val trimmed = word.trim()
        if (isInvalidToLearn(trimmed)) return@withContext

        val clean = if (isSinhalaWord(trimmed)) {
            trimmed.trim { it.isWhitespace() || it in listOf('.', ',', '!', '?', ';', ':', '(', ')', '"', '\'') }
        } else {
            trimmed.trim { !it.isLetterOrDigit() && it !in listOf('\'', '-') }
        }

        if (clean.length < 2) return@withContext

        val lookupKey = clean.lowercase(Locale.ROOT)
        val now = System.currentTimeMillis()

        try {
            val existing = wordFrequencyDao.getWord(lookupKey)
            if (existing != null) {
                val decayedCount = computeWriteTimeDecayedCount(existing.frequency, existing.lastUsed, now)
                wordFrequencyDao.updateFrequencyWithDecay(lookupKey, decayedCount, clean, now)
            } else {
                wordFrequencyDao.insertOrUpdate(
                    WordFrequencyEntity(
                        word = lookupKey,
                        frequency = 1,
                        lastUsed = now,
                        originalCase = clean
                    )
                )
            }

            // Record bigram with write-time decay
            if (!prevWord.isNullOrBlank() && bigramFrequencyDao != null) {
                val cleanPrev = prevWord.trim().lowercase(Locale.ROOT)
                if (!isInvalidToLearn(cleanPrev)) {
                    val existingBg = bigramFrequencyDao.getBigram(cleanPrev, lookupKey)
                    if (existingBg != null) {
                        val decayedBgCount = computeWriteTimeDecayedCount(existingBg.count, existingBg.lastUsed, now)
                        bigramFrequencyDao.updateBigramWithDecay(cleanPrev, lookupKey, decayedBgCount, now)
                    } else {
                        bigramFrequencyDao.insertOrUpdate(
                            BigramFrequencyEntity(
                                prevWord = cleanPrev,
                                word = lookupKey,
                                count = 1,
                                lastUsed = now
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {}
    }

    /**
     * Records a user correction pair (e.g. typed 'teh' -> tapped 'the', or 'omw' -> 'on my way').
     * Boosts corrected word immediately when typed string appears next time.
     */
    suspend fun recordCorrection(
        typed: String,
        corrected: String,
        language: String
    ) = withContext(Dispatchers.IO) {
        val cleanTyped = typed.trim()
        val cleanCorrected = corrected.trim()
        if (cleanTyped.isEmpty() || cleanCorrected.isEmpty() || cleanTyped.equals(cleanCorrected, ignoreCase = true)) {
            return@withContext
        }

        val now = System.currentTimeMillis()
        try {
            if (correctionPairDao != null) {
                val existing = correctionPairDao.getPair(cleanTyped, cleanCorrected, language)
                if (existing != null) {
                    val decayed = computeWriteTimeDecayedCount(existing.count, existing.lastUsed, now)
                    correctionPairDao.updateCount(cleanTyped, cleanCorrected, language, decayed, now)
                } else {
                    correctionPairDao.insertOrUpdate(
                        CorrectionPairEntity(
                            typed = cleanTyped,
                            corrected = cleanCorrected,
                            language = language,
                            count = 1,
                            lastUsed = now
                        )
                    )
                }
            }
            // Explicit correction tap is learned immediately in word frequency
            recordWordUsed(cleanCorrected, prevWord = null, isPrivateOrSensitive = false)
        } catch (_: Exception) {}
    }

    /**
     * Records a user transliteration selection for Latin -> Sinhala.
     */
    suspend fun recordTranslitChoice(
        latin: String,
        sinhala: String
    ) = withContext(Dispatchers.IO) {
        val cleanLatin = latin.trim().lowercase(Locale.ROOT)
        val cleanSinhala = sinhala.trim()
        if (cleanLatin.isEmpty() || cleanSinhala.isEmpty()) return@withContext

        try {
            if (translitChoiceDao != null) {
                val updated = translitChoiceDao.incrementChoice(cleanLatin, cleanSinhala)
                if (updated == 0) {
                    translitChoiceDao.insertOrUpdate(
                        TranslitChoiceEntity(cleanLatin, cleanSinhala, count = 1)
                    )
                }
            }
        } catch (_: Exception) {}
    }

    /**
     * Returns count of learned words for a specific language.
     */
    suspend fun getLearnedWordsCount(language: String): Int = withContext(Dispatchers.IO) {
        try {
            val all = wordFrequencyDao.getAllWords()
            val isSi = language == "si" || language == "si-trans"
            all.count { if (isSi) isSinhalaWord(it.word) else isEnglishWord(it.word) }
        } catch (_: Exception) {
            0
        }
    }

    /**
     * Clears learned data for a specific language (English vs Sinhala).
     */
    suspend fun clearLearnedData(language: String) = withContext(Dispatchers.IO) {
        try {
            val isSi = language == "si" || language == "si-trans"
            val all = wordFrequencyDao.getAllWords()
            val toDelete = all.filter { if (isSi) isSinhalaWord(it.word) else isEnglishWord(it.word) }.map { it.word }
            if (toDelete.isNotEmpty()) {
                wordFrequencyDao.deleteWords(toDelete)
            }
            correctionPairDao?.clearByLanguage(if (isSi) "si" else "en")
            if (isSi) {
                translitChoiceDao?.clearAll()
            }
        } catch (_: Exception) {}
    }

    /**
     * Clears all learned words and bigram history across all languages.
     */
    suspend fun clearLearnedWords() = withContext(Dispatchers.IO) {
        try {
            wordFrequencyDao.clearAll()
            bigramFrequencyDao?.clearAll()
            correctionPairDao?.clearAll()
            translitChoiceDao?.clearAll()
        } catch (_: Exception) {}
    }
}

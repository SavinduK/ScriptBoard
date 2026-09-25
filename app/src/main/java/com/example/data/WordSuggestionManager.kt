package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

class WordSuggestionManager(private val wordFrequencyDao: WordFrequencyDao) {

    // Common starter words when nothing is typed yet
    private val defaultStarterWords = listOf("I", "The", "Thanks", "Hello", "Yes", "Please")

    // Common core English dictionary words for word completions
    private val commonVocabulary = listOf(
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
        "doing", "having", "working", "trying", "looking", "welcome", "sounds", "good",
        "fine", "talk", "later", "soon", "ready", "done", "start", "stop", "again",
        "always", "never", "maybe", "yes", "yeah", "okay", "alright", "sorry", "excuse",
        "welcome", "appreciate", "regards", "best", "cheers", "update", "project", "code",
        "android", "keyboard", "mobile", "device", "quick", "fast", "speed", "test"
    )

    /**
     * Returns exactly 3 suggested words matching the current typed prefix.
     * Prioritizes user typing history based on frequency (same word used more -> suggest more).
     * If prefix is blank, returns top 3 words from user history or starter defaults.
     */
    suspend fun getSuggestions(prefix: String): List<String> = withContext(Dispatchers.IO) {
        val cleanPrefix = prefix.trim()

        if (cleanPrefix.isEmpty()) {
            // Suggest top 3 most used words from user typing history
            val topHistory = try {
                wordFrequencyDao.getTopWords(limit = 3).map { it.word }
            } catch (_: Exception) {
                emptyList()
            }

            val suggestions = topHistory.toMutableList()
            for (defaultWord in defaultStarterWords) {
                if (suggestions.size >= 3) break
                if (!suggestions.any { it.equals(defaultWord, ignoreCase = true) }) {
                    suggestions.add(defaultWord)
                }
            }
            return@withContext suggestions.take(3)
        }

        val prefixLower = cleanPrefix.lowercase(Locale.ROOT)

        // 1. Fetch user words from history matching prefix
        val userWords = try {
            wordFrequencyDao.getSuggestionsForPrefix(prefixLower, limit = 10)
        } catch (_: Exception) {
            emptyList()
        }

        // 2. Score candidates: User words get heavy weighting based on frequency
        // (same word used more -> suggested more)
        val scoreMap = mutableMapOf<String, Int>()

        for (item in userWords) {
            val score = 1000 + (item.frequency * 50) + if (item.word == prefixLower) 200 else 0
            scoreMap[item.word] = score
        }

        // 3. Match from common vocabulary
        for (vocab in commonVocabulary) {
            if (vocab.startsWith(prefixLower)) {
                if (!scoreMap.containsKey(vocab)) {
                    // Vocabulary words have lower base score than user history
                    scoreMap[vocab] = 100 + if (vocab == prefixLower) 50 else 0
                }
            }
        }

        // 4. Sort candidates by score descending
        val sortedCandidates = scoreMap.entries
            .sortedByDescending { it.value }
            .map { it.key }
            .toMutableList()

        // If fewer than 3, add fallback words starting with or containing prefix or prefix itself
        if (sortedCandidates.size < 3) {
            if (!sortedCandidates.contains(prefixLower)) {
                sortedCandidates.add(prefixLower)
            }
            for (vocab in commonVocabulary) {
                if (sortedCandidates.size >= 3) break
                if (vocab.contains(prefixLower) && !sortedCandidates.contains(vocab)) {
                    sortedCandidates.add(vocab)
                }
            }
        }

        // 5. Adjust casing according to how user typed the prefix
        val formatted = sortedCandidates.take(3).map { word ->
            applyCasing(cleanPrefix, word)
        }

        formatted
    }

    /**
     * Records a word used by the user in typing history to improve future suggestions.
     * Increments the frequency count for this word.
     */
    suspend fun recordWordUsed(word: String) = withContext(Dispatchers.IO) {
        val clean = word.trim().trim { !it.isLetterOrDigit() }.lowercase(Locale.ROOT)
        if (clean.length < 2) return@withContext

        try {
            val updated = wordFrequencyDao.incrementFrequency(clean, System.currentTimeMillis())
            if (updated == 0) {
                wordFrequencyDao.insertOrUpdate(
                    WordFrequencyEntity(
                        word = clean,
                        frequency = 1,
                        lastUsed = System.currentTimeMillis()
                    )
                )
            }
        } catch (_: Exception) {}
    }

    private fun applyCasing(typedPrefix: String, word: String): String {
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
}

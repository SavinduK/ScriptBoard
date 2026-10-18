package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

class WordSuggestionManager(private val wordFrequencyDao: WordFrequencyDao) {

    // Common starter words when nothing is typed yet (English)
    private val defaultEnglishStarterWords = listOf("I", "The", "Thanks", "Hello", "Yes", "Please", "How", "Good", "You")

    // Common starter words when nothing is typed yet (Sinhala)
    private val defaultSinhalaStarterWords = listOf("ආයුබෝවන්", "ස්තූතියි", "කොහොමද", "සුභ", "ඔව්", "හොඳයි", "මම", "ඔබ", "අපි")

    // Common core English dictionary words for word completions
    private val commonEnglishVocabulary = listOf(
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

    // Common core Sinhala dictionary words for word completions
    private val commonSinhalaVocabulary = listOf(
        "ආයුබෝවන්", "ස්තූතියි", "ඔව්", "නැහැ", "කොහොමද", "සුභ", "උදෑසනක්", "දවසක්", "හොඳයි", "මම",
        "ඔබ", "අපි", "ඔවුන්", "එන්න", "යන්න", "කරන්න", "දන්නවා", "ලස්සන", "ශ්‍රී", "ලංකා",
        "ගෙදර", "පාසල", "රස්සාව", "පොත", "කෑම", "වතුර", "මිතුරා", "පවුල", "ආදරය", "සතුට",
        "වැඩ", "වෙලාව", "දැන්", "පස්සේ", "හරි", "කමක්", "නෑ", "ඉක්මනින්", "කියන්න", "අහන්න",
        "බලන්න", "ලියන්න", "කියවන්න", "යමු", "එමු", "දෙන්න", "ගන්න", "මොකක්ද", "ඇයි", "කොහෙද",
        "කවදාද", "කවුද", "කොහොම", "හොඳ", "නරක", "ලොකු", "පොඩි", "අලුත්", "පරණ", "මිල",
        "මුදල්", "දුරකථනය", "පණිවිඩය", "ස්ථානය", "මාර්ගය", "රට", "නගරය", "ගම", "කාලගුණය", "වැස්ස",
        "හිරු", "දවල්", "රෑ", "හවස", "ඊයේ", "අද", "හෙට", "සතිය", "මාසය", "අවුරුද්ද",
        "මිතුරිය", "මල්ලී", "අයියා", "අක්කා", "නංගී", "අම්මා", "තාත්තා", "ගුරුවරයා", "වෛද්‍යවරයා",
        "කඩේ", "බස්", "කෝච්චිය", "කාර්", "පාපැදිය", "ගමන", "නිවාඩුව", "පාඩම", "විභාගය", "ප්‍රශ්නය",
        "පිළිතුර", "සත්‍යය", "මල්", "ගස්", "නැවුම්", "රස", "බොන්න", "නිදාගන්න", "නැගිටින්න"
    )

    companion object {
        fun isSinhalaWord(word: String): Boolean {
            return word.any { it in '\u0D80'..'\u0DFF' }
        }

        fun isEnglishWord(word: String): Boolean {
            return !isSinhalaWord(word)
        }
    }

    /**
     * Returns exactly 3 suggested words matching the current typed prefix in the SELECTED language.
     * When language == "si", only suggests Sinhala words.
     * When language == "en", only suggests English words.
     * Prioritizes user typing history based on frequency (same word used more -> suggest more).
     */
    suspend fun getSuggestions(prefix: String, language: String = "en"): List<String> = withContext(Dispatchers.IO) {
        val cleanPrefix = prefix.trim()
        val isSinhala = language == "si"

        if (cleanPrefix.isEmpty()) {
            val starterWords = if (isSinhala) defaultSinhalaStarterWords else defaultEnglishStarterWords
            // Suggest top 3 most used words from user typing history matching language
            val topHistory = try {
                wordFrequencyDao.getTopWords(limit = 20)
                    .map { it.word }
                    .filter { if (isSinhala) isSinhalaWord(it) else isEnglishWord(it) }
                    .take(3)
            } catch (_: Exception) {
                emptyList()
            }

            val suggestions = topHistory.toMutableList()
            for (defaultWord in starterWords) {
                if (suggestions.size >= 3) break
                if (!suggestions.any { it.equals(defaultWord, ignoreCase = true) }) {
                    suggestions.add(defaultWord)
                }
            }
            return@withContext suggestions.take(3)
        }

        val prefixCleaned = if (isSinhala) cleanPrefix else cleanPrefix.lowercase(Locale.ROOT)
        val targetVocabulary = if (isSinhala) commonSinhalaVocabulary else commonEnglishVocabulary
        val starterFallback = if (isSinhala) defaultSinhalaStarterWords else defaultEnglishStarterWords

        // 1. Fetch user words from history matching prefix
        val userWords = try {
            wordFrequencyDao.getSuggestionsForPrefix(prefixCleaned, limit = 20)
                .filter { if (isSinhala) isSinhalaWord(it.word) else isEnglishWord(it.word) }
        } catch (_: Exception) {
            emptyList()
        }

        // 2. Score candidates: User words get heavy weighting based on frequency
        val scoreMap = mutableMapOf<String, Int>()

        for (item in userWords) {
            val score = 1000 + (item.frequency * 50) + if (item.word == prefixCleaned) 200 else 0
            scoreMap[item.word] = score
        }

        // 3. Match from common vocabulary of the selected language
        for (vocab in targetVocabulary) {
            if (vocab.startsWith(prefixCleaned, ignoreCase = !isSinhala)) {
                if (!scoreMap.containsKey(vocab)) {
                    scoreMap[vocab] = 100 + if (vocab.equals(prefixCleaned, ignoreCase = !isSinhala)) 50 else 0
                }
            }
        }

        // 4. Sort candidates by score descending
        val sortedCandidates = scoreMap.entries
            .sortedByDescending { it.value }
            .map { it.key }
            .toMutableList()

        // 5. If fewer than 3, add fallback words starting with or containing prefix from the same language
        if (sortedCandidates.size < 3) {
            if (!sortedCandidates.contains(prefixCleaned)) {
                sortedCandidates.add(prefixCleaned)
            }
            for (vocab in targetVocabulary) {
                if (sortedCandidates.size >= 3) break
                if (vocab.contains(prefixCleaned, ignoreCase = !isSinhala) && !sortedCandidates.contains(vocab)) {
                    sortedCandidates.add(vocab)
                }
            }
            for (fallback in starterFallback) {
                if (sortedCandidates.size >= 3) break
                if (!sortedCandidates.contains(fallback)) {
                    sortedCandidates.add(fallback)
                }
            }
        }

        // 6. Adjust casing if English, or preserve as-is for Sinhala
        val formatted = sortedCandidates.take(3).map { word ->
            if (isSinhala) word else applyCasing(cleanPrefix, word)
        }

        formatted
    }

    /**
     * Records a word used by the user in typing history to improve future suggestions.
     * Increments the frequency count for this word.
     */
    suspend fun recordWordUsed(word: String) = withContext(Dispatchers.IO) {
        val isSinhala = isSinhalaWord(word)
        val clean = if (isSinhala) {
            word.trim().trim { it.isWhitespace() || it in listOf('.', ',', '!', '?', ';', ':', '(', ')', '"', '\'') }
        } else {
            word.trim().trim { !it.isLetterOrDigit() }.lowercase(Locale.ROOT)
        }
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

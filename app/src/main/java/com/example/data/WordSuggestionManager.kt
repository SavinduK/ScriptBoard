package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.ln
import kotlin.math.min
import kotlin.math.pow

/**
 * Intelligent, privacy-preserving Word Suggestion and Next-Word Prediction Manager.
 * Features:
 * - Privacy protection: never learns numbers, emails, URLs, or from sensitive fields.
 * - Anti-snowballing: blended scoring with user frequency, bigrams, dictionary, and prefix matches.
 * - Recency decay: score * 0.5^(daysSinceLastUse / 30).
 * - Minimum typing threshold: new words must be typed at least twice before appearing.
 * - Bigram next-word prediction when buffer is empty.
 * - Keyboard-adjacency fuzzy matching and typo tolerance.
 * - Mixed-language & Singlish-to-Sinhala transliteration.
 * - Preserves original casing (iPhone, McDonald's) with contextual titlecasing.
 * - Confidence-based strip placement (center slot threshold).
 */
class WordSuggestionManager(
    private val wordFrequencyDao: WordFrequencyDao,
    private val bigramFrequencyDao: BigramFrequencyDao? = null
) {

    companion object {
        // Tunable scoring weights
        const val W_USER = 3.5f
        const val W_BIGRAM = 4.0f
        const val W_DICT = 2.0f
        const val EXACT_PREFIX_BONUS = 5.0f
        const val MAX_USER_FREQ_CAP = 50.0f
        const val CONFIDENCE_THRESHOLD = 4.0f

        fun isSinhalaWord(word: String): Boolean {
            return word.any { it in '\u0D80'..'\u0DFF' }
        }

        fun isEnglishWord(word: String): Boolean {
            return !isSinhalaWord(word)
        }

        /**
         * Privacy filter: prevents storing numbers-only tokens, email addresses, and URLs.
         */
        fun isInvalidToLearn(word: String): Boolean {
            val trimmed = word.trim()
            if (trimmed.length < 2) return true
            // Numbers only
            if (trimmed.all { it.isDigit() }) return true
            // Email address pattern
            if (trimmed.contains("@") && trimmed.contains(".")) return true
            // URL pattern
            if (trimmed.startsWith("http://", ignoreCase = true) ||
                trimmed.startsWith("https://", ignoreCase = true) ||
                trimmed.startsWith("www.", ignoreCase = true)
            ) return true
            return false
        }
    }

    // Common starter words when nothing is typed yet (English)
    private val defaultEnglishStarterWords = listOf("I", "The", "Thanks", "Hello", "Yes", "Please", "How", "Good", "You")

    // Common starter words when nothing is typed yet (Sinhala)
    private val defaultSinhalaStarterWords = listOf("ආයුබෝවන්", "ස්තූතියි", "කොහොමද", "සුභ", "ඔව්", "හොඳයි", "මම", "ඔබ", "අපි")

    // Core English dictionary vocabulary (expanded high-frequency vocabulary)
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
        "doing", "having", "working", "trying", "looking", "welcome", "sounds", "fine",
        "talk", "later", "soon", "ready", "done", "start", "stop", "again", "always",
        "never", "maybe", "yes", "yeah", "okay", "alright", "sorry", "excuse", "appreciate",
        "regards", "best", "cheers", "update", "project", "code", "android", "keyboard",
        "mobile", "device", "quick", "fast", "speed", "test", "computer", "screen",
        "application", "system", "network", "internet", "online", "offline", "account",
        "profile", "password", "security", "privacy", "settings", "options", "change",
        "select", "button", "click", "tap", "type", "text", "audio", "video", "photo",
        "image", "camera", "music", "sound", "volume", "quiet", "silent", "important",
        "urgent", "schedule", "calendar", "appointment", "reminder", "office", "home",
        "family", "friend", "colleague", "manager", "team", "client", "customer", "support",
        "service", "price", "cost", "payment", "bank", "card", "money", "free", "discount",
        "order", "delivery", "shopping", "store", "market", "food", "dinner", "lunch",
        "breakfast", "coffee", "tea", "drink", "water", "restaurant", "hotel", "travel",
        "flight", "ticket", "train", "bus", "car", "drive", "ride", "walk", "city",
        "country", "place", "location", "map", "directions", "road", "street", "weather",
        "rain", "sun", "cloud", "temperature", "cold", "warm", "hot", "cool", "clean",
        "clear", "dark", "light", "color", "red", "blue", "green", "black", "white",
        "yellow", "orange", "purple", "brown", "gray", "big", "small", "huge", "tiny",
        "short", "long", "tall", "wide", "deep", "heavy", "soft", "hard", "easy",
        "simple", "difficult", "tough", "smart", "quick", "slow", "early", "late",
        "young", "old", "different", "same", "similar", "special", "general", "public",
        "private", "personal", "official", "standard", "normal", "usual", "common",
        "rare", "possible", "impossible", "true", "false", "correct", "wrong", "safe",
        "dangerous", "strong", "weak", "rich", "poor", "healthy", "sick", "tired",
        "excited", "bored", "worried", "calm", "proud", "brave", "funny", "serious",
        "friendly", "polite", "kind", "honest", "generous", "helpful", "careful",
        "bring", "carry", "hold", "keep", "leave", "stay", "move", "stand", "sit",
        "open", "close", "shut", "lock", "unlock", "push", "pull", "turn", "switch",
        "watch", "listen", "hear", "smell", "taste", "touch", "feel", "enjoy", "prefer",
        "choose", "decide", "agree", "disagree", "accept", "refuse", "allow", "prevent",
        "create", "build", "design", "develop", "fix", "repair", "break", "destroy",
        "explain", "describe", "discuss", "argue", "agree", "promise", "suggest",
        "recommend", "advise", "warn", "inform", "notify", "announce", "share", "post",
        "like", "comment", "subscribe", "follow", "download", "upload", "install",
        "delete", "remove", "add", "insert", "copy", "paste", "cut", "undo", "redo",
        "save", "load", "import", "export", "sync", "backup", "restore", "connect"
    )

    // Core Sinhala dictionary vocabulary (expanded high-frequency vocabulary)
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
        "පිළිතුර", "සත්‍යය", "මල්", "ගස්", "නැවුම්", "රස", "බොන්න", "නිදාගන්න", "නැගිටින්න",
        "දුවන්න", "ඇවිදින්න", "නටන්න", "ගයන්න", "සෙල්ලම්", "ක්‍රීඩාව", "චිත්‍රය", "කතාව",
        "ප්‍රවෘත්ති", "ගුවන්", "මුහුද", "නැව", "කන්ද", "ගඟ", "වැව", "වෙරළ", "සත්වයා", "බල්ලා",
        "පූසා", "ඇතා", "කුරුල්ලා", "මාළුවා", "ගොවිතැන", "වෙළඳාම", "රාජකාරිය", "කාර්යාලය",
        "පරිගණකය", "යතුරුපුවරුව", "ජංගම", "දුරකථන", "පණිවිඩ", "විද්‍යුත්", "තැපෑල", "අන්තර්ජාලය",
        "සම්බන්ධතාවය", "සේවාව", "ගිණුම", "මුරපදය", "ආරක්ෂාව", "පෞද්ගලිකත්වය", "සැකසුම්",
        "වෙනස්", "තෝරන්න", "බොත්තම", "ස්පර්ශ", "අකුරු", "ශබ්දය", "රූපය", "කැමරාව", "සංගීතය",
        "වැදගත්", "හදිසි", "දිනදර්ශනය", "මතක්කිරීම", "කණ්ඩායම", "පාරිභෝගිකයා", "ගෙවීම",
        "බැංකුව", "කාඩ්පත", "නොමිලේ", "වට්ටම්", "ඇණවුම", "බෙදාහැරීම", "මිලදී", "සාප්පුව",
        "තේ", "කෝපි", "ආපනශාලාව", "හෝටලය", "ප්‍රවේශපත්‍රය", "නවාතැන", "පාර", "සිතියම",
        "උණුසුම්", "සීතල", "පැහැදිලි", "අඳුරු", "පාට", "රතු", "නිල්", "කොළ", "කළු", "සුදු",
        "කහ", "තැඹිලි", "දම්", "දුඹුරු", "අළු", "දිග", "කෙටි", "උස", "පළල", "ගැඹුරු", "බර",
        "සැහැල්ලු", "මෘදු", "තද", "පහසු", "සරල", "අපහසු", "බුද්ධිමත්", "වේගවත්", "මන්දගාමී",
        "කලින්", "ප්‍රමාද", "තරුණ", "වයස", "වෙනස්", "සමාන", "විශේෂ", "පොදු", "නිල", "සාමාන්‍ය",
        "සැබෑ", "නිවැරදි", "වැරදි", "ආරක්ෂිත", "ශක්තිමත්", "නිරෝගී", "සතුටුදායක", "විශ්වාසය"
    )

    // Common Singlish-to-Sinhala phonetic transliteration dictionary
    private val singlishToSinhalaMap = mapOf(
        "mama" to "මම",
        "oya" to "ඔයා",
        "api" to "අපි",
        "ow" to "ඔව්",
        "ne" to "නෑ",
        "nae" to "නෑ",
        "subha" to "සුභ",
        "sthuthi" to "ස්තූතියි",
        "kohomada" to "කොහොමද",
        "hondai" to "හොඳයි",
        "hoda" to "හොඳ",
        "hari" to "හරි",
        "gedara" to "ගෙදර",
        "kaema" to "කෑම",
        "wathura" to "වතුර",
        "mokakda" to "මොකක්ද",
        "ai" to "ඇයි",
        "koheda" to "කොහෙද",
        "dan" to "දැන්",
        "passe" to "පස්සේ",
        "kamak" to "කමක්",
        "ikmanin" to "ඉක්මනින්",
        "kiyanna" to "කියන්න",
        "ahanna" to "අහන්න",
        "balanna" to "බලන්න",
        "karanna" to "කරන්න",
        "yanna" to "යන්න",
        "enna" to "එන්න",
        "yamu" to "යමු",
        "dawasak" to "දවසක්",
        "udasanak" to "උදෑසනක්",
        "ada" to "අද",
        "heta" to "හෙට",
        "iye" to "ඊයේ",
        "wada" to "වැඩ",
        "office" to "කාර්යාලය",
        "poth" to "පොත්",
        "mal" to "මල්",
        "sathutu" to "සතුටු",
        "adaraya" to "ආදරය",
        "sri" to "ශ්‍රී",
        "lanka" to "ලංකා"
    )

    // Keyboard adjacency map for QWERTY
    private val keyboardAdjacency: Map<Char, Set<Char>> = mapOf(
        'q' to setOf('w', 'a', 's'),
        'w' to setOf('q', 'e', 'a', 's', 'd'),
        'e' to setOf('w', 'r', 's', 'd', 'f'),
        'r' to setOf('e', 't', 'd', 'f', 'g'),
        't' to setOf('r', 'y', 'f', 'g', 'h'),
        'y' to setOf('t', 'u', 'g', 'h', 'j'),
        'u' to setOf('y', 'i', 'h', 'j', 'k'),
        'i' to setOf('u', 'o', 'j', 'k', 'l'),
        'o' to setOf('i', 'p', 'k', 'l'),
        'p' to setOf('o', 'l'),
        'a' to setOf('q', 'w', 's', 'z'),
        's' to setOf('w', 'e', 'a', 'd', 'z', 'x'),
        'd' to setOf('e', 'r', 's', 'f', 'x', 'c'),
        'f' to setOf('r', 't', 'd', 'g', 'c', 'v'),
        'g' to setOf('t', 'y', 'f', 'h', 'v', 'b'),
        'h' to setOf('y', 'u', 'g', 'j', 'b', 'n'),
        'j' to setOf('u', 'i', 'h', 'k', 'n', 'm'),
        'k' to setOf('i', 'o', 'j', 'l', 'm'),
        'l' to setOf('o', 'p', 'k'),
        'z' to setOf('a', 's', 'x'),
        'x' to setOf('z', 's', 'd', 'c'),
        'c' to setOf('x', 'd', 'f', 'v'),
        'v' to setOf('c', 'f', 'g', 'b'),
        'b' to setOf('v', 'g', 'h', 'n'),
        'n' to setOf('b', 'h', 'j', 'm'),
        'm' to setOf('n', 'j', 'k')
    )

    private fun computeRecencyDecay(lastUsedMillis: Long): Double {
        val daysSince = (System.currentTimeMillis() - lastUsedMillis).coerceAtLeast(0L) / (1000.0 * 60 * 60 * 24)
        return 0.5.pow(daysSince / 30.0)
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

    /**
     * Returns 3 suggested words matching the current context and typed prefix.
     * When prefix is empty:
     * - Returns next-word predictions from bigram frequencies based on prevWord.
     * - Falls back to top learned words and common starter words.
     * When prefix is typed:
     * - Blended score: w_user * log(1 + min(freq, 50)) + w_bigram * bigramScore + w_dict * dictScore + exactPrefixBonus
     * - Recency weighting: score * 0.5^(daysSinceLastUse / 30).
     * - Confidence threshold decides whether best candidate appears in the center slot or literal word appears.
     */
    suspend fun getSuggestions(
        prefix: String,
        language: String = "en",
        prevWord: String? = null
    ): List<String> = withContext(Dispatchers.IO) {
        val cleanPrefix = prefix.trim()
        val isSinhalaLanguage = language == "si" || language == "si-trans"

        // 1. EMPTY BUFFER: Show next-word predictions from bigrams or starter words
        if (cleanPrefix.isEmpty()) {
            val suggestions = mutableListOf<String>()

            // 1A. Try bigram next-word prediction
            if (!prevWord.isNullOrBlank() && bigramFrequencyDao != null) {
                try {
                    val cleanPrev = prevWord.trim().lowercase(Locale.ROOT)
                    val continuations = bigramFrequencyDao.getTopContinuations(cleanPrev, limit = 10)
                    for (item in continuations) {
                        if (suggestions.size >= 3) break
                        val matchesLang = if (isSinhalaLanguage) isSinhalaWord(item.word) else isEnglishWord(item.word)
                        if (matchesLang && !suggestions.contains(item.word)) {
                            suggestions.add(item.word)
                        }
                    }
                } catch (_: Exception) {}
            }

            // 1B. Top user history words matching the active keyboard language (frequency >= 2)
            if (suggestions.size < 3) {
                try {
                    val topLearned = wordFrequencyDao.getTopLearnedWords(minFrequency = 2, limit = 20)
                    for (entity in topLearned) {
                        if (suggestions.size >= 3) break
                        val matchesLang = if (isSinhalaLanguage) isSinhalaWord(entity.word) else isEnglishWord(entity.word)
                        if (matchesLang && !suggestions.contains(entity.word)) {
                            val text = if (hasCustomCasing(entity.originalCase)) entity.originalCase else entity.word
                            suggestions.add(text)
                        }
                    }
                } catch (_: Exception) {}
            }

            // 1C. Fallback to starter words strictly matched to keyboard language
            val starterWords = if (isSinhalaLanguage) defaultSinhalaStarterWords else defaultEnglishStarterWords
            for (defaultWord in starterWords) {
                if (suggestions.size >= 3) break
                if (!suggestions.any { it.equals(defaultWord, ignoreCase = true) }) {
                    suggestions.add(defaultWord)
                }
            }

            return@withContext suggestions.take(3)
        }

        val prefixLower = cleanPrefix.lowercase(Locale.ROOT)

        // 2. FETCH CANDIDATES
        data class CandidateInfo(
            val word: String,
            val originalCase: String?,
            var userFrequency: Int = 0,
            var lastUsed: Long = 0L,
            var bigramCount: Int = 0,
            var isDictionary: Boolean = false,
            var isSinglish: Boolean = false,
            var editDistance: Float = 0f
        )

        val candidateMap = mutableMapOf<String, CandidateInfo>()

        // 2A. Special Handling for English -> Sinhala Transliteration Keyboard (si-trans)
        if (language == "si-trans") {
            // Get phonetic transliteration candidates
            val transSuggestions = com.example.ui.keyboard.util.SinhalaTransliterator.getSuggestions(prefixLower, limit = 5)
            for ((idx, transWord) in transSuggestions.withIndex()) {
                candidateMap[transWord] = CandidateInfo(
                    word = transWord,
                    originalCase = transWord,
                    isDictionary = true,
                    isSinglish = true,
                    userFrequency = if (idx == 0) 50 else 20
                )
            }

            // Also check if any known Sinhala vocabulary words start with the primary transliterated word
            val primaryTrans = transSuggestions.firstOrNull() ?: ""
            if (primaryTrans.isNotEmpty()) {
                for (dictWord in commonSinhalaVocabulary) {
                    if (dictWord.startsWith(primaryTrans) && !candidateMap.containsKey(dictWord)) {
                        candidateMap[dictWord] = CandidateInfo(
                            word = dictWord,
                            originalCase = dictWord,
                            isDictionary = true
                        )
                        if (candidateMap.size > 15) break
                    }
                }
            }
        } else if (language == "si") {
            // 2B. Sinhala Wijesekara Keyboard: ONLY Sinhala words
            try {
                val learned = wordFrequencyDao.getLearnedSuggestions(prefixLower, minFrequency = 2, limit = 20)
                for (item in learned) {
                    if (isSinhalaWord(item.word)) {
                        candidateMap[item.word.lowercase()] = CandidateInfo(
                            word = item.word,
                            originalCase = item.originalCase,
                            userFrequency = item.frequency,
                            lastUsed = item.lastUsed
                        )
                    }
                }
            } catch (_: Exception) {}

            for (dictWord in commonSinhalaVocabulary) {
                val lower = dictWord.lowercase()
                if (lower.startsWith(prefixLower)) {
                    val existing = candidateMap[lower]
                    if (existing != null) {
                        existing.isDictionary = true
                    } else {
                        candidateMap[lower] = CandidateInfo(
                            word = dictWord,
                            originalCase = dictWord,
                            isDictionary = true
                        )
                    }
                }
            }
        } else {
            // 2C. English Keyboard: ONLY English words (No Sinhala words or Singlish conversions)
            try {
                val learned = wordFrequencyDao.getLearnedSuggestions(prefixLower, minFrequency = 2, limit = 20)
                for (item in learned) {
                    if (isEnglishWord(item.word)) {
                        candidateMap[item.word.lowercase()] = CandidateInfo(
                            word = item.word,
                            originalCase = item.originalCase,
                            userFrequency = item.frequency,
                            lastUsed = item.lastUsed
                        )
                    }
                }
            } catch (_: Exception) {}

            for (dictWord in commonEnglishVocabulary) {
                val lower = dictWord.lowercase()
                if (lower.startsWith(prefixLower)) {
                    val existing = candidateMap[lower]
                    if (existing != null) {
                        existing.isDictionary = true
                    } else {
                        candidateMap[lower] = CandidateInfo(
                            word = dictWord,
                            originalCase = dictWord,
                            isDictionary = true
                        )
                    }
                }
            }

            // Typo / Fuzzy correction for English
            if (candidateMap.size < 3 && prefixLower.length >= 3) {
                for (dictWord in commonEnglishVocabulary) {
                    if (kotlin.math.abs(dictWord.length - prefixLower.length) <= 1) {
                        val dist = calculateKeyboardAdjacencyDistance(prefixLower, dictWord)
                        if (dist <= 1.5f && !candidateMap.containsKey(dictWord)) {
                            candidateMap[dictWord] = CandidateInfo(
                                word = dictWord,
                                originalCase = dictWord,
                                isDictionary = true,
                                editDistance = dist
                            )
                        }
                    }
                }
            }
        }

        // 3. SCORE CANDIDATES
        data class ScoredCandidate(val text: String, val score: Double)

        val scored = candidateMap.values.map { info ->
            val userScore = if (info.userFrequency > 0) {
                W_USER * ln(1.0 + min(info.userFrequency.toFloat(), MAX_USER_FREQ_CAP))
            } else 0.0

            val bigramScore = if (info.bigramCount > 0) {
                W_BIGRAM * ln(1.0 + min(info.bigramCount.toFloat(), 30.0f))
            } else 0.0

            val dictScore = if (info.isDictionary) W_DICT.toDouble() else 0.0
            val singlishScore = if (info.isSinglish) 4.5 else 0.0
            val exactBonus = if (info.word.equals(cleanPrefix, ignoreCase = true)) EXACT_PREFIX_BONUS.toDouble() else 0.0
            val typoPenalty = if (info.editDistance > 0f) -(info.editDistance * 2.0) else 0.0

            var rawScore = userScore + bigramScore + dictScore + singlishScore + exactBonus + typoPenalty

            if (info.lastUsed > 0L) {
                val decay = computeRecencyDecay(info.lastUsed)
                rawScore *= decay
            }

            val formattedText = if (isSinhalaWord(info.word)) {
                info.word
            } else {
                applyCasing(cleanPrefix, info.word, info.originalCase)
            }

            ScoredCandidate(formattedText, rawScore)
        }.sortedByDescending { it.score }

        // Filter strictly by keyboard language
        val languageStrictCandidates = scored.map { it.text }
            .filter { word ->
                if (isSinhalaLanguage) isSinhalaWord(word) else isEnglishWord(word)
            }
            .distinct()

        val results = mutableListOf<String>()

        if (language == "si-trans") {
            // For transliteration, show top transliterated candidates directly
            results.addAll(languageStrictCandidates.take(3))
            var padIndex = 0
            while (results.size < 3 && padIndex < defaultSinhalaStarterWords.size) {
                val starter = defaultSinhalaStarterWords[padIndex++]
                if (!results.contains(starter)) results.add(starter)
            }
        } else {
            val topCandidate = scored.firstOrNull()
            val isConfident = topCandidate != null && topCandidate.score >= CONFIDENCE_THRESHOLD

            if (isConfident && languageStrictCandidates.isNotEmpty()) {
                val best = languageStrictCandidates[0]
                val second = languageStrictCandidates.getOrNull(1) ?: best
                val third = languageStrictCandidates.getOrNull(2) ?: if (second != best) second else best

                results.add(second)
                results.add(best)
                results.add(third)
            } else {
                val left = languageStrictCandidates.firstOrNull() ?: cleanPrefix
                val center = cleanPrefix
                val right = languageStrictCandidates.drop(1).firstOrNull() ?: ""

                // Ensure words in strip match active keyboard language
                val safeCenter = if (isSinhalaLanguage && !isSinhalaWord(center)) {
                    languageStrictCandidates.firstOrNull() ?: defaultSinhalaStarterWords.first()
                } else center

                results.add(if (left != safeCenter) left else (languageStrictCandidates.getOrNull(1) ?: ""))
                results.add(safeCenter)
                results.add(if (right != safeCenter && right != left) right else (languageStrictCandidates.getOrNull(2) ?: ""))
            }
        }

        // Final safety guarantee: Ensure strictly 3 non-empty slots matching language
        val fallbackList = if (isSinhalaLanguage) defaultSinhalaStarterWords else defaultEnglishStarterWords
        val finalClean = results.filter { word ->
            word.isNotBlank() && (if (isSinhalaLanguage) isSinhalaWord(word) else isEnglishWord(word))
        }.toMutableList()

        for (fallback in fallbackList) {
            if (finalClean.size >= 3) break
            if (!finalClean.contains(fallback)) finalClean.add(fallback)
        }

        return@withContext finalClean.take(3)
    }

    /**
     * Calculates edit distance with keyboard-adjacency discount (0.5 for adjacent keys).
     */
    private fun calculateKeyboardAdjacencyDistance(s1: String, s2: String): Float {
        val dp = Array(s1.length + 1) { FloatArray(s2.length + 1) }
        for (i in 0..s1.length) dp[i][0] = i.toFloat()
        for (j in 0..s2.length) dp[0][j] = j.toFloat()

        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val c1 = s1[i - 1].lowercaseChar()
                val c2 = s2[j - 1].lowercaseChar()
                val cost = when {
                    c1 == c2 -> 0.0f
                    keyboardAdjacency[c1]?.contains(c2) == true -> 0.5f
                    else -> 1.0f
                }
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1.0f,
                    dp[i][j - 1] + 1.0f,
                    dp[i - 1][j - 1] + cost
                )
            }
        }
        return dp[s1.length][s2.length]
    }

    /**
     * Records a word used by the user, respecting privacy constraints:
     * - Discards if isPrivateOrSensitive is true (password, email, URI, or NO_PERSONALIZED_LEARNING).
     * - Discards numbers-only tokens, email addresses, and URLs.
     * - Updates bigrams if prevWord is provided.
     * - Preserves original casing.
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
            val updated = wordFrequencyDao.incrementFrequencyWithCase(lookupKey, clean, now)
            if (updated == 0) {
                wordFrequencyDao.insertOrUpdate(
                    WordFrequencyEntity(
                        word = lookupKey,
                        frequency = 1,
                        lastUsed = now,
                        originalCase = clean
                    )
                )
            }

            // Record bigram frequency if prevWord exists and is valid
            if (!prevWord.isNullOrBlank() && bigramFrequencyDao != null) {
                val cleanPrev = prevWord.trim().lowercase(Locale.ROOT)
                if (!isInvalidToLearn(cleanPrev)) {
                    val bigramUpdated = bigramFrequencyDao.incrementBigram(cleanPrev, lookupKey, now)
                    if (bigramUpdated == 0) {
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
     * Clears all learned words and bigram history (Privacy Requirement A).
     */
    suspend fun clearLearnedWords() = withContext(Dispatchers.IO) {
        try {
            wordFrequencyDao.clearAll()
            bigramFrequencyDao?.clearAll()
        } catch (_: Exception) {}
    }
}

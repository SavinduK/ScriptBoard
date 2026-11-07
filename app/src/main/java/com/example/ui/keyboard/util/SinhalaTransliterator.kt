package com.example.ui.keyboard.util

import com.example.data.TranslitChoiceDao
import java.util.Locale

/**
 * Phonetic Transliteration Engine: English (Singlish) ➔ Sinhala.
 * Provides high-speed phonetic conversion, dictionary hits, learned user choices,
 * and rule-based variants handling ambiguous character pairs:
 * - ත (dental t) vs ට (retroflex t)
 * - ද (dental d) vs ඩ (retroflex d)
 * - ල (dental l) vs ළ (retroflex l)
 */
object SinhalaTransliterator {

    // Common Singlish word dictionary for 100% accurate natural spelling
    private val dictionary: Map<String, String> = mapOf(
        "mama" to "මම",
        "mage" to "මගේ",
        "mata" to "මට",
        "magen" to "මගෙන්",
        "oya" to "ඔයා",
        "oyage" to "ඔයාගේ",
        "oyata" to "ඔයාට",
        "oyala" to "ඔයාලා",
        "oyaala" to "ඔයාලා",
        "api" to "අපි",
        "ape" to "අපේ",
        "apita" to "අපිට",
        "apiwa" to "අපිව",
        "eyaa" to "එයා",
        "eya" to "එයා",
        "eyata" to "එයාට",
        "eyage" to "එයාගේ",
        "ow" to "ඔව්",
        "ou" to "ඔව්",
        "ne" to "නෑ",
        "nae" to "නෑ",
        "na" to "නෑ",
        "naha" to "නැහැ",
        "nahae" to "නැහැ",
        "subha" to "සුභ",
        "sthuthi" to "ස්තූතියි",
        "sthuthiy" to "ස්තූතියි",
        "kohomada" to "කොහොමද",
        "hondai" to "හොඳයි",
        "hondin" to "හොඳින්",
        "hoda" to "හොඳ",
        "hodai" to "හොඳයි",
        "hari" to "හරි",
        "gedara" to "ගෙදර",
        "kaema" to "කෑම",
        "kema" to "කෑම",
        "wathura" to "වතුර",
        "mokakda" to "මොකක්ද",
        "mokada" to "මොකද",
        "monawada" to "මොනවද",
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
        "emuda" to "එමුද",
        "dawasak" to "දවසක්",
        "udasanak" to "උදෑසනක්",
        "ada" to "අද",
        "heta" to "හෙට",
        "iye" to "ඊයේ",
        "wada" to "වැඩ",
        "wadaak" to "වැඩක්",
        "office" to "කාර්යාලය",
        "poth" to "පොත්",
        "potha" to "පොත",
        "mal" to "මල්",
        "mala" to "මල",
        "sathutu" to "සතුටු",
        "sathuta" to "සතුට",
        "adaraya" to "ආදරය",
        "adarei" to "ආදරෙයි",
        "sri" to "ශ්‍රී",
        "lanka" to "ලංකා",
        "sinhala" to "සිංහල",
        "sinhale" to "සිංහලේ",
        "puluwan" to "පුළුවන්",
        "ba" to "බෑ",
        "bae" to "බෑ",
        "nathnam" to "නැත්නම්",
        "ehema" to "එහෙම",
        "mehema" to "මෙහෙම",
        "onna" to "ඔන්න",
        "menna" to "මෙන්න",
        "methana" to "මෙතන",
        "ethana" to "එතන",
        "lassana" to "ලස්සන",
        "rathu" to "රතු",
        "nil" to "නිල්",
        "kaha" to "කහ",
        "sudu" to "සුදු",
        "kalu" to "කළු",
        "kolamba" to "කොළඹ",
        "gamata" to "ගමට",
        "rata" to "රට",
        "nagara" to "නගර",
        "kello" to "කෙල්ලෝ",
        "kollo" to "කොල්ලෝ",
        "machan" to "මචං",
        "machang" to "මචං",
        "bath" to "බත්",
        "tea" to "තේ",
        "coffee" to "කෝපි",
        "ayubowan" to "ආයුබෝවන්",
        "aayubowan" to "ආයුබෝවන්",
        "bohoma" to "බොහොම",
        "godak" to "ගොඩක්",
        "tikak" to "ටිකක්",
        "kauda" to "කවුද",
        "kawadaada" to "කවදාද",
        "kawadada" to "කවදාද",
        "mokuth" to "මොකුත්",
        "kisima" to "කිසිම",
        "eka" to "එක",
        "deka" to "දෙක",
        "thuna" to "තුන",
        "hathara" to "හතර",
        "paha" to "පහ"
    )

    private val multiConsonants = listOf(
        "thth" to ("ත්" to "ත"),
        "dhdh" to ("ද්" to "ද"),
        "chch" to ("ච්" to "ච"),
        "shsh" to ("ශ්" to "ශ"),
        "kk" to ("ක්" to "ක"),
        "gg" to ("ග්" to "ග"),
        "tt" to ("ට්" to "ට"),
        "dd" to ("ඩ්" to "ඩ"),
        "pp" to ("ප්" to "ප"),
        "bb" to ("බ්" to "බ"),
        "mm" to ("ම්" to "ම"),
        "nn" to ("න්" to "න"),
        "ll" to ("ල්" to "ල"),
        "ss" to ("ස්" to "ස"),
        "th" to ("ත්" to "ත"),
        "dh" to ("ද්" to "ද"),
        "ch" to ("ච්" to "ච"),
        "sh" to ("ශ්" to "ශ"),
        "zh" to ("ෂ්" to "ෂ"),
        "kh" to ("ඛ්" to "ඛ"),
        "gh" to ("ඝ්" to "ඝ"),
        "jh" to ("ඣ්" to "ඣ"),
        "ph" to ("ඵ්" to "ඵ"),
        "bh" to ("භ්" to "භ"),
        "gn" to ("ඥ්" to "ඥ"),
        "kn" to ("ඤ්" to "ඤ"),
        "ng" to ("ං" to "ඞ"),
        "mb" to ("ඹ්" to "ඹ"),
        "nd" to ("ඳ්" to "ඳ")
    )

    private val singleConsonants = mapOf(
        'k' to ("ක්" to "ක"),
        'g' to ("ග්" to "ග"),
        'c' to ("ච්" to "ච"),
        'j' to ("ජ්" to "ජ"),
        't' to ("ට්" to "ට"),
        'd' to ("ඩ්" to "ඩ"),
        'n' to ("න්" to "න"),
        'p' to ("ප්" to "ප"),
        'b' to ("බ්" to "බ"),
        'm' to ("ම්" to "ම"),
        'y' to ("ය්" to "ය"),
        'r' to ("ර්" to "ර"),
        'l' to ("ල්" to "ල"),
        'v' to ("ව්" to "ව"),
        'w' to ("ව්" to "ව"),
        's' to ("ස්" to "ස"),
        'h' to ("හ්" to "හ"),
        'f' to ("ෆ්" to "ෆ")
    )

    private val independentVowels = listOf(
        "aae" to "ඈ",
        "ae" to "ඇ",
        "a." to "ඇ",
        "aa" to "ආ",
        "ai" to "අයි",
        "au" to "අවු",
        "ii" to "ඊ",
        "ee" to "ඒ",
        "uu" to "ඌ",
        "oo" to "ඕ",
        "a" to "අ",
        "i" to "ඉ",
        "u" to "උ",
        "e" to "එ",
        "o" to "ඔ"
    )

    private val vowelModifiers = listOf(
        "aae" to "ෑ",
        "ae" to "ැ",
        "a." to "ැ",
        "aa" to "ා",
        "ai" to "ෛ",
        "au" to "ෞ",
        "ii" to "ී",
        "ee" to "ේ",
        "uu" to "ූ",
        "oo" to "ෝ",
        "a" to "",
        "i" to "ි",
        "u" to "ු",
        "e" to "ෙ",
        "o" to "ො"
    )

    fun transliterateWord(rawWord: String): String {
        val word = rawWord.trim()
        if (word.isEmpty()) return ""

        val lower = word.lowercase(Locale.ROOT)
        dictionary[lower]?.let { return it }

        val sb = StringBuilder()
        var i = 0
        val len = lower.length

        while (i < len) {
            val remaining = lower.substring(i)

            // 1. Independent vowel
            if (i == 0 || (i > 0 && !lower[i - 1].isLetter())) {
                var matchedVowel = false
                for ((vKey, vVal) in independentVowels) {
                    if (remaining.startsWith(vKey)) {
                        sb.append(vVal)
                        i += vKey.length
                        matchedVowel = true
                        break
                    }
                }
                if (matchedVowel) continue
            }

            // 2. Consonants
            var matchedConsonant: Pair<String, String>? = null
            var consKeyLen = 0

            for ((cKey, cVal) in multiConsonants) {
                if (remaining.startsWith(cKey)) {
                    matchedConsonant = cVal
                    consKeyLen = cKey.length
                    break
                }
            }

            if (matchedConsonant == null) {
                val ch = remaining[0]
                val single = singleConsonants[ch]
                if (single != null) {
                    matchedConsonant = single
                    consKeyLen = 1
                }
            }

            if (matchedConsonant != null) {
                i += consKeyLen
                val afterCons = lower.substring(i)

                var matchedModifier: String? = null
                var modKeyLen = 0

                for ((vKey, vVal) in vowelModifiers) {
                    if (afterCons.startsWith(vKey)) {
                        matchedModifier = vVal
                        modKeyLen = vKey.length
                        break
                    }
                }

                if (matchedModifier != null) {
                    val base = matchedConsonant.second
                    sb.append(base).append(matchedModifier)
                    i += modKeyLen
                } else {
                    val hal = matchedConsonant.first
                    sb.append(hal)
                }
                continue
            }

            sb.append(word[i])
            i++
        }

        return sb.toString()
    }

    /**
     * Generates rule-based variants for ambiguous pairs:
     * - ත vs ට
     * - ද vs ඩ
     * - ල vs ළ
     */
    fun generateAmbiguousVariants(baseSinhala: String): List<String> {
        val variants = mutableListOf<String>()

        // ත <-> ට variants
        if (baseSinhala.contains("ත") || baseSinhala.contains("ත්")) {
            val v = baseSinhala.replace("ත", "ට").replace("ත්", "ට්")
            if (v != baseSinhala) variants.add(v)
        } else if (baseSinhala.contains("ට") || baseSinhala.contains("ට්")) {
            val v = baseSinhala.replace("ට", "ත").replace("ට්", "ත්")
            if (v != baseSinhala) variants.add(v)
        }

        // ද <-> ඩ variants
        if (baseSinhala.contains("ද") || baseSinhala.contains("ද්")) {
            val v = baseSinhala.replace("ද", "ඩ").replace("ද්", "ඩ්")
            if (v != baseSinhala) variants.add(v)
        } else if (baseSinhala.contains("ඩ") || baseSinhala.contains("ඩ්")) {
            val v = baseSinhala.replace("ඩ", "ද").replace("ඩ්", "ද්")
            if (v != baseSinhala) variants.add(v)
        }

        // ල <-> ළ variants
        if (baseSinhala.contains("ල") || baseSinhala.contains("ල්")) {
            val v = baseSinhala.replace("ල", "ළ").replace("ල්", "ළ්")
            if (v != baseSinhala) variants.add(v)
        } else if (baseSinhala.contains("ළ") || baseSinhala.contains("ළ්")) {
            val v = baseSinhala.replace("ළ", "ල").replace("ළ්", "ල්")
            if (v != baseSinhala) variants.add(v)
        }

        return variants
    }

    /**
     * Returns up to [limit] Sinhala candidates for an English prefix:
     * 1. Learned mapping from TranslitChoiceDao
     * 2. Common Singlish dictionary exact match
     * 3. Rule-based transliterated word
     * 4. Ambiguous pair variants (ත/ට, ද/ඩ, ල/ළ)
     * 5. Dictionary prefix matches
     */
    suspend fun getSuggestions(
        prefix: String,
        translitChoiceDao: TranslitChoiceDao? = null,
        limit: Int = 3
    ): List<String> {
        val clean = prefix.trim().lowercase(Locale.ROOT)
        if (clean.isEmpty()) {
            return listOf("ආයුබෝවන්", "ස්තූතියි", "කොහොමද").take(limit)
        }

        val result = mutableListOf<String>()

        // 1. Learned user choices from TranslitChoiceDao (ranked highest)
        if (translitChoiceDao != null) {
            try {
                val learned = translitChoiceDao.getTopChoicesForLatin(clean, limit = 3)
                for (item in learned) {
                    if (!result.contains(item.sinhala)) {
                        result.add(item.sinhala)
                    }
                }
            } catch (_: Exception) {}
        }

        // 2. Direct dictionary match
        dictionary[clean]?.let {
            if (!result.contains(it)) result.add(it)
        }

        // 3. Rule-based transliteration
        val transliterated = transliterateWord(clean)
        if (transliterated.isNotBlank() && !result.contains(transliterated)) {
            result.add(transliterated)
        }

        // 4. Ambiguous pair variants (ත/ට, ද/ඩ, ල/ළ)
        if (transliterated.isNotBlank()) {
            val variants = generateAmbiguousVariants(transliterated)
            for (v in variants) {
                if (result.size >= limit) break
                if (!result.contains(v)) {
                    result.add(v)
                }
            }
        }

        // 5. Prefix matches in dictionary
        for ((key, value) in dictionary) {
            if (result.size >= limit) break
            if (key.startsWith(clean) && !result.contains(value)) {
                result.add(value)
            }
        }

        return result.take(limit)
    }
}

package com.example.ui.keyboard.model

data class LanguagePack(
    val id: String,
    val name: String,
    val nativeName: String,
    val flag: String,
    val description: String,
    val sizeDisplay: String,
    val isBuiltIn: Boolean = false
)

object LanguagePackRegistry {
    val BUILT_IN_ENGLISH = LanguagePack(
        id = "en",
        name = "English",
        nativeName = "English",
        flag = "🇺🇸",
        description = "Standard QWERTY layout with symbols and numeric alternates",
        sizeDisplay = "Built-in",
        isBuiltIn = true
    )

    val SINHALA = LanguagePack(
        id = "si",
        name = "Sinhala",
        nativeName = "සිංහල",
        flag = "🇱🇰",
        description = "Official 5-row Wijesekara touch layout with vowel modifiers (පිල්ලම්)",
        sizeDisplay = "1.8 MB",
        isBuiltIn = false
    )

    val TAMIL = LanguagePack(
        id = "ta",
        name = "Tamil",
        nativeName = "தமிழ்",
        flag = "🇱🇰",
        description = "Tamil Anjal script layout for multilingual typing",
        sizeDisplay = "1.6 MB",
        isBuiltIn = false
    )

    val SPANISH = LanguagePack(
        id = "es",
        name = "Spanish",
        nativeName = "Español",
        flag = "🇪🇸",
        description = "Spanish QWERTY with ñ, inverted punctuation and accented vowels",
        sizeDisplay = "1.2 MB",
        isBuiltIn = false
    )

    val HINDI = LanguagePack(
        id = "hi",
        name = "Hindi",
        nativeName = "हिन्दी",
        flag = "🇮🇳",
        description = "Hindi Devanagari script layout with matras",
        sizeDisplay = "1.9 MB",
        isBuiltIn = false
    )

    val ALL_PACKAGES: List<LanguagePack> = listOf(
        BUILT_IN_ENGLISH,
        SINHALA,
        TAMIL,
        SPANISH,
        HINDI
    )

    fun getPack(id: String): LanguagePack? {
        return ALL_PACKAGES.firstOrNull { it.id == id }
    }
}

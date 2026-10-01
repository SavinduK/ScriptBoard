package com.example.ui.keyboard.model

data class EmojiPackItem(
    val symbol: String,
    val name: String,
    val meaning: String = ""
)

data class EmojiPack(
    val id: String,
    val name: String,
    val nativeName: String,
    val icon: String,
    val description: String,
    val sizeDisplay: String,
    val itemCount: Int = 0,
    val category: String = "Special"
)

object EmojiPackRegistry {
    val RUNIC = EmojiPack(
        id = "runic",
        name = "Runic Symbols",
        nativeName = "ᚱᚢᚾᛖᛋ",
        icon = "ᚱ",
        description = "Elder Futhark and Anglo-Saxon magical runic alphabet symbols with glyph meanings",
        sizeDisplay = "18 KB",
        itemCount = 29,
        category = "Ancient Alphabets"
    )

    val HIEROGLYPHS = EmojiPack(
        id = "hieroglyphs",
        name = "Egyptian Hieroglyphs",
        nativeName = "𓀀 𓌃𓂧𓅱𓀁",
        icon = "𓀀",
        description = "Ancient Egyptian Hieroglyphs Unicode block (Gardiner's sign list animals, gods, vessels)",
        sizeDisplay = "28 KB",
        itemCount = 35,
        category = "Ancient Egypt"
    )

    val KAOMOJI = EmojiPack(
        id = "kaomoji",
        name = "Japanese Kaomoji",
        nativeName = "顔文字",
        icon = "(^‿^)",
        description = "Expressive Japanese text emoticons for cute reactions, table flips and hugs",
        sizeDisplay = "16 KB",
        itemCount = 21,
        category = "Expressive Emoticons"
    )

    val ALCHEMY = EmojiPack(
        id = "alchemy",
        name = "Alchemical & Astrological",
        nativeName = "🜁 ☉ ☽",
        icon = "🜁",
        description = "Medieval alchemy elements, astronomical planets and zodiac symbols",
        sizeDisplay = "14 KB",
        itemCount = 32,
        category = "Occult & Hermetic"
    )

    val ALL_PACKS: List<EmojiPack> = listOf(
        RUNIC,
        HIEROGLYPHS,
        KAOMOJI,
        ALCHEMY
    )

    fun getPack(id: String): EmojiPack? {
        return ALL_PACKS.firstOrNull { it.id == id }
    }
}

package com.example

import android.view.inputmethod.EditorInfo
import com.example.ui.keyboard.model.EmojiPackRegistry
import com.example.ui.keyboard.util.StickerFormatHelper
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testEmojiPackRegistry_hasExpectedPacks() {
    val runic = EmojiPackRegistry.getPack("runic")
    assertNotNull("Runic pack should exist", runic)
    assertEquals("ᚱ", runic?.icon)
    assertEquals("Runic Symbols", runic?.name)

    val hieroglyphs = EmojiPackRegistry.getPack("hieroglyphs")
    assertNotNull("Hieroglyphs pack should exist", hieroglyphs)
    assertEquals("𓀀", hieroglyphs?.icon)
    assertEquals("Egyptian Hieroglyphs", hieroglyphs?.name)

    val kaomoji = EmojiPackRegistry.getPack("kaomoji")
    assertNotNull("Kaomoji pack should exist", kaomoji)
    assertEquals("(^‿^)", kaomoji?.icon)

    val alchemy = EmojiPackRegistry.getPack("alchemy")
    assertNotNull("Alchemy pack should exist", alchemy)
    assertEquals("🜁", alchemy?.icon)

    assertEquals(4, EmojiPackRegistry.ALL_PACKS.size)
  }

  @Test
  fun testStickerFormatHelper_detectsWhatsApp() {
    val whatsAppEditorInfo = EditorInfo().apply {
      packageName = "com.whatsapp"
    }
    assertTrue("Should detect WhatsApp package", StickerFormatHelper.isWhatsApp(whatsAppEditorInfo))

    val whatsAppBusinessEditorInfo = EditorInfo().apply {
      packageName = "com.whatsapp.w4b"
    }
    assertTrue("Should detect WhatsApp Business package", StickerFormatHelper.isWhatsApp(whatsAppBusinessEditorInfo))

    val telegramEditorInfo = EditorInfo().apply {
      packageName = "org.telegram.messenger"
    }
    assertFalse("Should return false for non-WhatsApp app", StickerFormatHelper.isWhatsApp(telegramEditorInfo))

    assertFalse("Should handle null editorInfo safely", StickerFormatHelper.isWhatsApp(null))
  }
}


package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ui.keyboard.model.EmojiData
import com.example.ui.keyboard.util.WebpAnimationHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("KeyPro Keyboard", appName)
  }

  @Test
  fun `math symbols section contains integrals and calculus symbols`() {
    val mathCategory = EmojiData.mathSymbolsCategory
    assertEquals("Math", mathCategory.title)
    assertTrue("Should contain integral ∫", mathCategory.emojis.contains("∫"))
    assertTrue("Should contain double integral ∬", mathCategory.emojis.contains("∬"))
    assertTrue("Should contain contour integral ∮", mathCategory.emojis.contains("∮"))
    assertTrue("Should contain partial derivative ∂", mathCategory.emojis.contains("∂"))
    assertTrue("Should contain summation ∑", mathCategory.emojis.contains("∑"))
    assertTrue("Should contain square root √", mathCategory.emojis.contains("√"))
    assertTrue("Should contain infinity ∞", mathCategory.emojis.contains("∞"))
  }

  @Test
  fun `greek letters section contains uppercase and lowercase`() {
    val greekCategory = EmojiData.greekLettersCategory
    assertEquals("Greek", greekCategory.title)
    assertTrue("Should contain alpha α", greekCategory.emojis.contains("α"))
    assertTrue("Should contain beta β", greekCategory.emojis.contains("β"))
    assertTrue("Should contain pi π", greekCategory.emojis.contains("π"))
    assertTrue("Should contain omega ω", greekCategory.emojis.contains("ω"))
    assertTrue("Should contain Alpha Α", greekCategory.emojis.contains("Α"))
    assertTrue("Should contain Omega Ω", greekCategory.emojis.contains("Ω"))
  }

  @Test
  fun `sample animated webp sticker is detected as animated`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val assetManager = context.assets
    val list = assetManager.list("sample_stickers")
    assertTrue("Sample stickers should not be empty", !list.isNullOrEmpty())

    // Copy one sample sticker to cache and test WebpAnimationHelper
    val first = list!!.first()
    val testFile = File(context.cacheDir, first)
    assetManager.open("sample_stickers/$first").use { input ->
      FileOutputStream(testFile).use { output ->
        input.copyTo(output)
      }
    }

    val isAnim = WebpAnimationHelper.isAnimated(testFile)
    assertTrue("Sticker $first should be detected as animated WebP", isAnim)
  }

  @Test
  fun `number key group alternates map maps 1 to tilde as primary alternate`() {
    val alternates1 = com.example.ui.keyboard.components.NumberKeyAlternates.map["1"]
    assertTrue("Should have alternates for 1", alternates1 != null)
    assertEquals("Primary alternate for 1 should be ~", "~", alternates1!!.first())

    val alternates2 = com.example.ui.keyboard.components.NumberKeyAlternates.map["2"]
    assertEquals("Primary alternate for 2 should be @", "@", alternates2!!.first())

    val alternates3 = com.example.ui.keyboard.components.NumberKeyAlternates.map["3"]
    assertEquals("Primary alternate for 3 should be #", "#", alternates3!!.first())
  }

  @Test
  fun `sinhala keyboard layout generates 6 rows with pillam and consonants`() {
    val rows = com.example.ui.keyboard.model.KeyboardLayoutGenerator.getSinhalaRows()
    assertEquals("Sinhala layout should have 6 rows", 6, rows.size)

    val row1 = rows[0]
    assertTrue("Row 1 should contain වු", row1.any { it.primaryText == "වු" })
    assertTrue("Row 1 should contain ර", row1.any { it.primaryText == "ර" })

    val row6 = rows[5]
    assertTrue("Row 6 should contain space key with සිංහල", row6.any { it.primaryText == "සිංහල" })
  }

  @Test
  fun `language pack registry contains Sinhala downloadable package`() {
    val sinhalaPack = com.example.ui.keyboard.model.LanguagePackRegistry.getPack("si")
    assertTrue("Sinhala pack should exist in registry", sinhalaPack != null)
    assertEquals("Sinhala", sinhalaPack!!.name)
    assertEquals("සිංහල", sinhalaPack.nativeName)
    assertEquals("🇱🇰", sinhalaPack.flag)
  }

  @Test
  fun `toggleable keys map contains pairs for 1 to tilde, at to dollar, and paren to angle bracket`() {
    val toggleable1 = com.example.ui.keyboard.model.ToggleableKeys.getToggleable("1")
    assertEquals("~", toggleable1)

    val toggleableTilde = com.example.ui.keyboard.model.ToggleableKeys.getToggleable("~")
    assertEquals("1", toggleableTilde)

    val toggleableAt = com.example.ui.keyboard.model.ToggleableKeys.getToggleable("@")
    assertEquals("$", toggleableAt)

    val toggleableDollar = com.example.ui.keyboard.model.ToggleableKeys.getToggleable("$")
    assertEquals("@", toggleableDollar)

    val toggleableParen = com.example.ui.keyboard.model.ToggleableKeys.getToggleable("(")
    assertEquals("<", toggleableParen)

    val toggleableAngle = com.example.ui.keyboard.model.ToggleableKeys.getToggleable("<")
    assertEquals("(", toggleableAngle)
  }

  @Test
  fun `sinhala keyboard changes vowel modifier keys according to selected letter`() {
    val kaRows = com.example.ui.keyboard.model.KeyboardLayoutGenerator.getSinhalaRows(selectedLetter = "ක")
    val kaRow1 = kaRows[0]
    assertTrue("Row 1 should contain කු when ක is selected", kaRow1.any { it.primaryText == "කු" })
    assertTrue("Row 1 should contain කැ when ක is selected", kaRow1.any { it.primaryText == "කැ" })

    val kaRow2 = kaRows[1]
    assertTrue("Row 2 should contain කා when ක is selected", kaRow2.any { it.primaryText == "කා" })
    assertTrue("Row 2 should contain කෙ when ක is selected", kaRow2.any { it.primaryText == "කෙ" })

    val kaRow4 = kaRows[3]
    assertTrue("Row 4 should contain ක් when ක is selected", kaRow4.any { it.primaryText == "ක්" })

    // Test with another letter (e.g. ම)
    val maRows = com.example.ui.keyboard.model.KeyboardLayoutGenerator.getSinhalaRows(selectedLetter = "ම")
    val maRow1 = maRows[0]
    assertTrue("Row 1 should contain මු when ම is selected", maRow1.any { it.primaryText == "මු" })
    val maRow2 = maRows[1]
    assertTrue("Row 2 should contain මා when ම is selected", maRow2.any { it.primaryText == "මා" })
  }

  @Test
  fun `holdForSymbols toggle disables secondary text and toggleables in dedicated number row and symbols`() {
    // When enabled (true)
    val numRowEnabled = com.example.ui.keyboard.model.KeyboardLayoutGenerator.getDedicatedNumberRow(holdForSymbols = true)
    assertEquals("Dedicated number row should have 10 keys", 10, numRowEnabled.size)
    assertEquals("~", numRowEnabled[0].secondaryText)
    assertEquals("@", numRowEnabled[1].secondaryText)

    val sym1Enabled = com.example.ui.keyboard.model.KeyboardLayoutGenerator.getSymbols1Rows(holdForSymbols = true)
    val sym1Row1Enabled = sym1Enabled[0]
    assertEquals("~", sym1Row1Enabled[0].secondaryText)

    // When disabled (false)
    val numRowDisabled = com.example.ui.keyboard.model.KeyboardLayoutGenerator.getDedicatedNumberRow(holdForSymbols = false)
    assertEquals("Dedicated number row should have 10 keys", 10, numRowDisabled.size)
    assertTrue("All keys in dedicated number row should have null secondaryText when holdForSymbols is disabled",
      numRowDisabled.all { it.secondaryText == null })

    val sym1Disabled = com.example.ui.keyboard.model.KeyboardLayoutGenerator.getSymbols1Rows(holdForSymbols = false)
    val sym1Row1Disabled = sym1Disabled[0]
    assertTrue("Numbers row in symbols should have null secondaryText when holdForSymbols is disabled",
      sym1Row1Disabled.all { it.secondaryText == null })

    val numpadDisabled = com.example.ui.keyboard.model.KeyboardLayoutGenerator.getNumpadRows(holdForSymbols = false)
    assertTrue("Numpad rows should have null secondaryText when holdForSymbols is disabled",
      numpadDisabled.flatten().all { it.secondaryText == null })
  }

  @Test
  fun `github repository languages folder contains json pack files`() {
    val languagesDir = if (File("languages").exists()) File("languages") else File("../languages")
    assertTrue("languages directory should exist in repo root", languagesDir.exists() && languagesDir.isDirectory)

    val indexFile = File(languagesDir, "index.json")
    assertTrue("index.json should exist in languages dir", indexFile.exists())
    assertTrue("index.json should contain packages", indexFile.readText().contains("packages"))

    val siFile = File(languagesDir, "si.json")
    assertTrue("si.json should exist in languages dir", siFile.exists())
    val siContent = siFile.readText()
    assertTrue("si.json should contain wijesekara layout definition", siContent.contains("wijesekara"))
    assertTrue("si.json should contain Sinhala consonants", siContent.contains("ක"))

    val taFile = File(languagesDir, "ta.json")
    assertTrue("ta.json should exist", taFile.exists())

    // Also verify asset access
    val context = ApplicationProvider.getApplicationContext<Context>()
    val assetStream = context.assets.open("languages/si.json")
    val assetBytes = assetStream.readBytes()
    assetStream.close()
    assertTrue("Bundled asset languages/si.json should not be empty", assetBytes.isNotEmpty())
  }

  @Test
  fun `keyboard preferences computes valid github raw base url`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefs = com.example.ui.keyboard.util.KeyboardPreferences.getInstance(context)

    prefs.setGithubRepoConfig("myuser", "myrepo", "main")
    val url = prefs.getGithubRawBaseUrl()
    assertEquals("https://raw.githubusercontent.com/myuser/myrepo/main/languages/", url)
  }

  @Test
  fun `complete laptop keyboard generates 6 standard laptop rows with functions and navigation`() {
    val rows = com.example.ui.keyboard.model.KeyboardLayoutGenerator.getCompleteLaptopRows()
    assertEquals("Complete laptop keyboard should have 6 rows", 6, rows.size)

    // Row 0: Function row (Esc, F1..F12, Del)
    val row0 = rows[0]
    assertTrue("Row 0 should have Esc", row0.any { it.primaryText == "Esc" })
    assertTrue("Row 0 should have F1", row0.any { it.primaryText == "F1" })
    assertTrue("Row 0 should have F12", row0.any { it.primaryText == "F12" })
    assertTrue("Row 0 should have Del", row0.any { it.primaryText == "Del" })

    // Row 1: Number row with backspace
    val row1 = rows[1]
    assertTrue("Row 1 should have `", row1.any { it.primaryText == "`" })
    assertTrue("Row 1 should have 1", row1.any { it.primaryText == "1" })
    assertTrue("Row 1 should have ⌫", row1.any { it.primaryText == "⌫" })

    // Row 2: Tab and QWERTY top row
    val row2 = rows[2]
    assertTrue("Row 2 should have Tab", row2.any { it.primaryText.contains("Tab") })
    assertTrue("Row 2 should have q", row2.any { it.primaryText.equals("q", ignoreCase = true) })

    // Row 3: Caps Lock and Enter
    val row3 = rows[3]
    assertTrue("Row 3 should have Caps", row3.any { it.primaryText == "Caps" })
    assertTrue("Row 3 should have Enter", row3.any { it.primaryText.contains("Enter") })

    // Row 4: Shift and Arrow Up
    val row4 = rows[4]
    assertTrue("Row 4 should have Shift", row4.any { it.primaryText.contains("Shift") })
    assertTrue("Row 4 should have ↑", row4.any { it.primaryText == "↑" })

    // Row 5: Ctrl, Fn, Laptop (toggle back), Alt, Space, Arrow Left/Down/Right
    val row5 = rows[5]
    assertTrue("Row 5 should have Ctrl", row5.any { it.primaryText == "Ctrl" })
    assertTrue("Row 5 should have Fn", row5.any { it.primaryText == "Fn" })
    assertTrue("Row 5 should have Laptop toggle key", row5.any { it.primaryText == "Laptop" && it.action is com.example.ui.keyboard.model.KeyAction.SwitchToLetters && it.icon != null })
    assertTrue("Row 5 should have Alt", row5.any { it.primaryText == "Alt" })
    assertTrue("Row 5 should have ←", row5.any { it.primaryText == "←" })
    assertTrue("Row 5 should have ↓", row5.any { it.primaryText == "↓" })
    assertTrue("Row 5 should have →", row5.any { it.primaryText == "→" })
  }

  @Test
  fun `emoji pack downloader defines valid constant repository locations`() {
    assertEquals("keypro-org", com.example.ui.keyboard.model.EmojiPackDownloader.DEFAULT_GITHUB_OWNER)
    assertEquals("keyboard-assets", com.example.ui.keyboard.model.EmojiPackDownloader.DEFAULT_GITHUB_REPO)
    assertEquals("main", com.example.ui.keyboard.model.EmojiPackDownloader.DEFAULT_GITHUB_BRANCH)
    assertEquals("https://raw.githubusercontent.com", com.example.ui.keyboard.model.EmojiPackDownloader.GITHUB_RAW_BASE)
  }

  @Test
  fun `zip output stream creates valid zip archive of sample files`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val testZipFile = File(context.cacheDir, "test_stickers.zip")
    if (testZipFile.exists()) testZipFile.delete()

    java.io.FileOutputStream(testZipFile).use { fos ->
      java.util.zip.ZipOutputStream(java.io.BufferedOutputStream(fos)).use { zos ->
        val entry = java.util.zip.ZipEntry("sample.webp")
        zos.putNextEntry(entry)
        zos.write("RIFFtestWEBP".toByteArray())
        zos.closeEntry()
      }
    }

    assertTrue("ZIP file should exist", testZipFile.exists())
    assertTrue("ZIP file should not be empty", testZipFile.length() > 0)

    // Verify reading zip
    var foundSample = false
    java.util.zip.ZipInputStream(testZipFile.inputStream()).use { zis ->
      var zipEntry = zis.nextEntry
      while (zipEntry != null) {
        if (zipEntry.name == "sample.webp") {
          foundSample = true
        }
        zipEntry = zis.nextEntry
      }
    }
    assertTrue("ZIP should contain sample.webp", foundSample)
  }
}

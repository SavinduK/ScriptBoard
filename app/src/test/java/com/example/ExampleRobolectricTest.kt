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
}

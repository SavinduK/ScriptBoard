package com.example

import android.graphics.Bitmap
import android.view.inputmethod.EditorInfo
import androidx.core.view.inputmethod.EditorInfoCompat
import androidx.test.core.app.ApplicationProvider
import com.example.ui.keyboard.util.AnimationFrame
import com.example.ui.keyboard.util.GifEncoder
import com.example.ui.keyboard.util.StickerFormatHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StickerPipelineTest {

    @Test
    fun testGifEncoder_createsValidMultiFrameGif() {
        val bmp1 = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        val bmp2 = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)

        val frames = listOf(
            AnimationFrame(bmp1, 100),
            AnimationFrame(bmp2, 100)
        )

        val tempFile = File.createTempFile("test_anim", ".gif")
        tempFile.deleteOnExit()

        val success = GifEncoder.encodeAnimation(frames, tempFile)
        assertTrue("Encoding should succeed", success)
        assertTrue("File should exist", tempFile.exists())
        assertTrue("File length should be positive", tempFile.length() > 0)
        assertTrue("File size should be under 1MB", tempFile.length() < 1024 * 1024)
        assertTrue("File should validate as multi-frame GIF89a", GifEncoder.validateGif(tempFile, requireMultiFrame = true))
    }

    @Test
    fun testFormatResolution_forAnimatedSticker() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val stickerFile = File(context.filesDir, "test.webp").apply {
            parentFile?.mkdirs()
            writeBytes(ByteArray(100))
        }
        val gifFile = File(context.filesDir, "test.gif").apply {
            writeBytes(ByteArray(100))
        }
        val pngFile = File(context.filesDir, "test_static.png").apply {
            writeBytes(ByteArray(100))
        }

        // 1. WhatsApp / Telegram / Instagram supporting image/gif
        val editorInfoGif = EditorInfo().apply {
            packageName = "org.telegram.messenger"
        }
        EditorInfoCompat.setContentMimeTypes(editorInfoGif, arrayOf("image/gif", "image/png"))

        val candidates = StickerFormatHelper.resolveCandidatesForTarget(
            context = context,
            stickerFile = stickerFile,
            gifPath = gifFile.absolutePath,
            staticPngPath = pngFile.absolutePath,
            isAnimatedSticker = true,
            editorInfo = editorInfoGif
        )

        assertTrue("Candidates should not be empty", candidates.isNotEmpty())
        assertEquals("First candidate for animated should be image/gif", "image/gif", candidates.first().mimeType)
        assertEquals("Candidate file should be the GIF file", gifFile.absolutePath, candidates.first().file.absolutePath)

        // 2. Verified WebP app (Google Messages) supporting WebP
        val editorInfoMessages = EditorInfo().apply {
            packageName = "com.google.android.apps.messaging"
        }
        EditorInfoCompat.setContentMimeTypes(editorInfoMessages, arrayOf("image/webp"))

        val candidatesMessages = StickerFormatHelper.resolveCandidatesForTarget(
            context = context,
            stickerFile = stickerFile,
            gifPath = null,
            staticPngPath = pngFile.absolutePath,
            isAnimatedSticker = true,
            editorInfo = editorInfoMessages
        )

        assertTrue("Candidates for Google Messages should not be empty", candidatesMessages.isNotEmpty())
        assertEquals("Candidate should be image/webp for Google Messages", "image/webp", candidatesMessages.first().mimeType)

        // Clean up
        stickerFile.delete()
        gifFile.delete()
        pngFile.delete()
    }

    @Test
    fun testFormatResolution_forStaticSticker() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val stickerFile = File(context.filesDir, "static_sticker.webp").apply {
            parentFile?.mkdirs()
            writeBytes(ByteArray(100))
        }
        val pngFile = File(context.filesDir, "static_sticker_static.png").apply {
            writeBytes(ByteArray(100))
        }

        val editorInfo = EditorInfo().apply {
            packageName = "com.whatsapp"
        }
        EditorInfoCompat.setContentMimeTypes(editorInfo, arrayOf("image/webp", "image/png"))

        val candidates = StickerFormatHelper.resolveCandidatesForTarget(
            context = context,
            stickerFile = stickerFile,
            gifPath = null,
            staticPngPath = pngFile.absolutePath,
            isAnimatedSticker = false,
            editorInfo = editorInfo
        )

        assertTrue("Static sticker candidates should not be empty", candidates.isNotEmpty())
        assertEquals("First choice for static should be image/webp", "image/webp", candidates.first().mimeType)

        stickerFile.delete()
        pngFile.delete()
    }
}

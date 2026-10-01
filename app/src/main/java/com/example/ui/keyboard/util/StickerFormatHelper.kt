package com.example.ui.keyboard.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.os.Build
import android.view.inputmethod.EditorInfo
import androidx.core.view.inputmethod.EditorInfoCompat
import java.io.File
import java.io.FileOutputStream

object StickerFormatHelper {

    data class FormattedSticker(
        val file: File,
        val mimeType: String,
        val isGif: Boolean
    )

    fun isWhatsApp(editorInfo: EditorInfo?): Boolean {
        val pkg = editorInfo?.packageName?.lowercase() ?: return false
        return pkg.contains("whatsapp")
    }

    fun isAnimated(file: File): Boolean {
        return WebpAnimationHelper.isAnimated(file)
    }

    /**
     * Resolves the best sticker file and MIME type for the target editor.
     * If the target is WhatsApp (which does not render raw WebP commits as animated stickers),
     * OR if the receiving app does not support WebP but supports GIF, this provides an animated GIF version.
     */
    fun resolveStickerForTarget(
        context: Context,
        originalFile: File,
        editorInfo: EditorInfo?
    ): FormattedSticker {
        if (!originalFile.exists()) {
            return FormattedSticker(originalFile, "image/webp", false)
        }

        val supportedMimes = editorInfo?.let { EditorInfoCompat.getContentMimeTypes(it) } ?: emptyArray()
        val appSupportsWebp = supportedMimes.any { it.equals("image/webp", ignoreCase = true) || it == "image/*" || it == "*/*" }
        val appSupportsGif = supportedMimes.any { it.equals("image/gif", ignoreCase = true) || it == "image/*" || it == "*/*" }
        val targetIsWhatsApp = isWhatsApp(editorInfo)
        val isAnimatedSticker = isAnimated(originalFile)

        // WhatsApp does NOT render pasted/committed WebP stickers as animated items unless sent as GIF.
        // Also if the target app doesn't support WebP, send as GIF if supported.
        val shouldSendAsGif = isAnimatedSticker && (targetIsWhatsApp || (!appSupportsWebp && appSupportsGif) || originalFile.extension.equals("gif", ignoreCase = true))

        if (shouldSendAsGif) {
            val gifFile = getOrGenerateGif(context, originalFile)
            if (gifFile != null && gifFile.exists() && gifFile.length() > 0) {
                return FormattedSticker(gifFile, "image/gif", true)
            }
        }

        val mime = if (originalFile.extension.equals("gif", ignoreCase = true)) "image/gif" else "image/webp"
        return FormattedSticker(originalFile, mime, mime == "image/gif")
    }

    private fun getOrGenerateGif(context: Context, webpFile: File): File? {
        if (webpFile.extension.equals("gif", ignoreCase = true)) {
            return webpFile
        }

        // 1. Check if companion .gif file exists in the same directory (e.g. stickers dir)
        val companionGif = File(webpFile.parentFile, "${webpFile.nameWithoutExtension}.gif")
        if (companionGif.exists() && companionGif.length() > 0) {
            return companionGif
        }

        // 2. Check if companion .gif is bundled in assets/sample_stickers
        val assetGifName = "${webpFile.nameWithoutExtension}.gif"
        try {
            val assetList = context.assets.list("sample_stickers") ?: emptyArray()
            if (assetList.contains(assetGifName)) {
                val cacheDir = File(context.cacheDir, "gif_stickers")
                if (!cacheDir.exists()) cacheDir.mkdirs()
                val cachedFile = File(cacheDir, assetGifName)
                if (!cachedFile.exists() || cachedFile.length() == 0L) {
                    context.assets.open("sample_stickers/$assetGifName").use { input ->
                        FileOutputStream(cachedFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                }
                if (cachedFile.exists() && cachedFile.length() > 0) {
                    return cachedFile
                }
            }
        } catch (_: Exception) {}

        // 3. Check in app cache directory
        val cacheDir = File(context.cacheDir, "gif_stickers")
        if (!cacheDir.exists()) cacheDir.mkdirs()
        val cachedGif = File(cacheDir, "${webpFile.nameWithoutExtension}.gif")
        if (cachedGif.exists() && cachedGif.length() > 0) {
            return cachedGif
        }

        // 4. Fallback generation: Convert WebP frames/bitmap to GIF
        return convertWebpToGifFallback(context, webpFile, cachedGif)
    }

    private fun convertWebpToGifFallback(context: Context, webpFile: File, outputFile: File): File? {
        try {
            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                try {
                    val source = ImageDecoder.createSource(webpFile)
                    ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    }
                } catch (_: Exception) {
                    BitmapFactory.decodeFile(webpFile.absolutePath)
                }
            } else {
                BitmapFactory.decodeFile(webpFile.absolutePath)
            } ?: return null

            BasicGifWriter.writeSingleFrameGif(bitmap, outputFile)
            return if (outputFile.exists() && outputFile.length() > 0) outputFile else null
        } catch (_: Exception) {
            return null
        }
    }
}

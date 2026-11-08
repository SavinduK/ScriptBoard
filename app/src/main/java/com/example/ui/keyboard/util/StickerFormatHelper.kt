package com.example.ui.keyboard.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.inputmethod.EditorInfo
import androidx.core.view.inputmethod.EditorInfoCompat
import java.io.File
import java.io.FileOutputStream

object StickerFormatHelper {

    /**
     * Overridable allowlist of packages verified to render keyboard-sent animated WebP stickers.
     * (Default: Google Messages). All other apps receive animated GIFs or static PNGs.
     */
    val animatedWebpAllowlist: MutableSet<String> = mutableSetOf(
        "com.google.android.apps.messaging"
    )

    fun isWhatsApp(editorInfo: EditorInfo?): Boolean {
        val pkg = editorInfo?.packageName?.lowercase() ?: return false
        return pkg.contains("whatsapp")
    }

    data class ResolvedPayload(
        val file: File,
        val mimeType: String,
        val isAnimated: Boolean
    )

    /**
     * Resolves the ordered candidate payloads to attempt sending via commitContent.
     * User requested flow:
     * For any type of animated stickers:
     *   1. Convert all animated types to animated GIF before sending
     *   2. Then send the generated animated GIF as the sticker
     *   3. For backup send the original sticker format (e.g. animated webp)
     * For non animated stickers:
     *   Send in original format (webp/png/jpg), then static png, then original sticker format backup
     */
    fun resolveCandidatesForTarget(
        context: Context,
        stickerFile: File,
        gifPath: String?,
        staticPngPath: String?,
        isAnimatedSticker: Boolean,
        editorInfo: EditorInfo?
    ): List<ResolvedPayload> {
        val supportedMimes = editorInfo?.let { EditorInfoCompat.getContentMimeTypes(it) } ?: emptyArray()
        fun supports(mime: String): Boolean {
            if (supportedMimes.isEmpty()) return true // default allow attempt if app doesn't advertise
            return supportedMimes.any {
                it.equals(mime, ignoreCase = true) || it == "image/*" || it == "*/*"
            }
        }

        val candidates = mutableListOf<ResolvedPayload>()

        if (isAnimatedSticker) {
            // Step 1 & 2: Convert all animated types to animated GIF, then send generated animated GIF as sticker
            val gifFile = gifPath?.let { File(it) }?.takeIf { it.exists() && it.length() > 0 }
                ?: File(stickerFile.parentFile, "${stickerFile.nameWithoutExtension}.gif").takeIf { it.exists() && it.length() > 0 }
                ?: getOrGenerateCompanionGif(context, stickerFile)

            if (gifFile != null && gifFile.exists() && gifFile.length() > 0 && supports("image/gif")) {
                candidates.add(ResolvedPayload(gifFile, "image/gif", true))
            }

            // Step 3 (Backup): Send the original sticker format (e.g. .webp or other animated format)
            val origExt = stickerFile.extension.lowercase()
            val origMime = when (origExt) {
                "webp" -> "image/webp"
                "gif" -> "image/gif"
                "png" -> "image/png"
                else -> "image/webp"
            }

            if (stickerFile.exists() && stickerFile.length() > 0 && supports(origMime) && !candidates.any { it.file == stickerFile }) {
                candidates.add(ResolvedPayload(stickerFile, origMime, true))
            }

            // Secondary Fallback: Static PNG of frame 1
            val pngFile = staticPngPath?.let { File(it) }?.takeIf { it.exists() && it.length() > 0 }
                ?: File(stickerFile.parentFile, "${stickerFile.nameWithoutExtension}_static.png").takeIf { it.exists() && it.length() > 0 }
                ?: getOrGenerateStaticPng(context, stickerFile)

            if (pngFile != null && pngFile.exists() && pngFile.length() > 0 && supports("image/png") && !candidates.any { it.file == pngFile }) {
                candidates.add(ResolvedPayload(pngFile, "image/png", false))
            }
        } else {
            // NON-ANIMATED stickers:
            // Send in original format first, then static png, then backup original
            val origExt = stickerFile.extension.lowercase()
            val origMime = when (origExt) {
                "webp" -> "image/webp"
                "png" -> "image/png"
                "gif" -> "image/gif"
                "jpg", "jpeg" -> "image/jpeg"
                else -> "image/webp"
            }

            if (stickerFile.exists() && stickerFile.length() > 0 && supports(origMime)) {
                candidates.add(ResolvedPayload(stickerFile, origMime, false))
            }

            // Static PNG representation if different
            val pngFile = staticPngPath?.let { File(it) }?.takeIf { it.exists() && it.length() > 0 }
                ?: File(stickerFile.parentFile, "${stickerFile.nameWithoutExtension}_static.png").takeIf { it.exists() && it.length() > 0 }
                ?: getOrGenerateStaticPng(context, stickerFile)

            if (pngFile != null && pngFile.exists() && pngFile.length() > 0 && supports("image/png") && !candidates.any { it.file == pngFile }) {
                candidates.add(ResolvedPayload(pngFile, "image/png", false))
            }

            // Backup original sticker format if not already in candidates
            if (stickerFile.exists() && stickerFile.length() > 0 && !candidates.any { it.file == stickerFile }) {
                candidates.add(ResolvedPayload(stickerFile, origMime, false))
            }
        }

        return candidates
    }

    /**
     * Generates a valid multi-frame GIF from an animated WebP file and writes it to disk.
     */
    fun generateCompanionGif(context: Context, webpFile: File): File? {
        if (!webpFile.exists() || webpFile.length() == 0L) return null
        if (webpFile.extension.equals("gif", ignoreCase = true)) return webpFile

        val stickersDir = File(context.filesDir, "stickers")
        if (!stickersDir.exists()) stickersDir.mkdirs()
        val destGif = File(stickersDir, "${webpFile.nameWithoutExtension}.gif")

        // If companion GIF was already bundled in assets, copy it directly
        val assetGifName = "${webpFile.nameWithoutExtension}.gif"
        try {
            val assetList = context.assets.list("sample_stickers") ?: emptyArray()
            if (assetList.contains(assetGifName)) {
                context.assets.open("sample_stickers/$assetGifName").use { input ->
                    FileOutputStream(destGif).use { output ->
                        input.copyTo(output)
                    }
                }
                if (destGif.exists() && destGif.length() > 0 && GifEncoder.validateGif(destGif, false)) {
                    return destGif
                }
            }
        } catch (_: Exception) {}

        // Otherwise decode frames and encode multi-frame GIF
        val frames = WebpFrameExtractor.extractFrames(webpFile)
        if (frames.isEmpty()) return null

        val ok = GifEncoder.encodeAnimation(frames, destGif)
        return if (ok && destGif.exists() && destGif.length() > 0) destGif else null
    }

    /**
     * Generates a static PNG from frame 1 of a sticker.
     */
    fun generateStaticPng(context: Context, stickerFile: File): File? {
        if (!stickerFile.exists() || stickerFile.length() == 0L) return null
        val stickersDir = File(context.filesDir, "stickers")
        if (!stickersDir.exists()) stickersDir.mkdirs()
        val destPng = File(stickersDir, "${stickerFile.nameWithoutExtension}_static.png")

        try {
            val frames = if (WebpAnimationHelper.isAnimated(stickerFile)) {
                WebpFrameExtractor.extractFrames(stickerFile)
            } else {
                emptyList()
            }

            val bmp = if (frames.isNotEmpty()) {
                frames.first().bitmap
            } else {
                BitmapFactory.decodeFile(stickerFile.absolutePath)
            } ?: return null

            FileOutputStream(destPng).use { out ->
                bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            return if (destPng.exists() && destPng.length() > 0) destPng else null
        } catch (_: Exception) {
            return null
        }
    }

    private fun getOrGenerateCompanionGif(context: Context, webpFile: File): File? {
        return generateCompanionGif(context, webpFile)
    }

    private fun getOrGenerateStaticPng(context: Context, stickerFile: File): File? {
        return generateStaticPng(context, stickerFile)
    }
}

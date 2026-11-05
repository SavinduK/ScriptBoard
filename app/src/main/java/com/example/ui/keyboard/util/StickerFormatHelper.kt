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
     * Order for ANIMATED stickers:
     *   1. image/gif if advertised (or image/wildcard is advertised)
     *   2. image/webp ONLY for apps verified to animate it (default: com.google.android.apps.messaging)
     *   3. static PNG of frame 1 via image/png (or image/wildcard)
     *   4. clipboard fallback
     * Order for STATIC stickers:
     *   1. image/webp
     *   2. image/png
     *   3. clipboard fallback
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
            return supportedMimes.any {
                it.equals(mime, ignoreCase = true) || it == "image/*" || it == "*/*"
            }
        }

        val targetPkg = editorInfo?.packageName?.lowercase() ?: ""
        val candidates = mutableListOf<ResolvedPayload>()

        if (isAnimatedSticker) {
            // Option 1: image/gif if advertised
            val gifFile = gifPath?.let { File(it) }?.takeIf { it.exists() && it.length() > 0 }
                ?: File(stickerFile.parentFile, "${stickerFile.nameWithoutExtension}.gif").takeIf { it.exists() && it.length() > 0 }
                ?: getOrGenerateCompanionGif(context, stickerFile)

            if (gifFile != null && gifFile.exists() && gifFile.length() > 0 && supports("image/gif")) {
                candidates.add(ResolvedPayload(gifFile, "image/gif", true))
            }

            // Option 2: image/webp ONLY for apps verified to animate it
            val isVerifiedToAnimateWebp = animatedWebpAllowlist.any { targetPkg == it.lowercase() }
            if (isVerifiedToAnimateWebp && supports("image/webp") && stickerFile.exists() && stickerFile.length() > 0) {
                candidates.add(ResolvedPayload(stickerFile, "image/webp", true))
            }

            // Option 3: static PNG of frame 1 via image/png
            val pngFile = staticPngPath?.let { File(it) }?.takeIf { it.exists() && it.length() > 0 }
                ?: File(stickerFile.parentFile, "${stickerFile.nameWithoutExtension}_static.png").takeIf { it.exists() && it.length() > 0 }
                ?: getOrGenerateStaticPng(context, stickerFile)

            if (pngFile != null && pngFile.exists() && pngFile.length() > 0 && supports("image/png")) {
                candidates.add(ResolvedPayload(pngFile, "image/png", false))
            }

            // Option 3b: If target supports WebP (even if not verified to animate) and nothing else matched
            if (supports("image/webp") && stickerFile.exists() && stickerFile.length() > 0 && !candidates.any { it.file == stickerFile }) {
                candidates.add(ResolvedPayload(stickerFile, "image/webp", false))
            }
        } else {
            // STATIC stickers:
            // 1. image/webp
            if (supports("image/webp") && stickerFile.exists() && stickerFile.length() > 0) {
                candidates.add(ResolvedPayload(stickerFile, "image/webp", false))
            }

            // 2. image/png
            val pngFile = staticPngPath?.let { File(it) }?.takeIf { it.exists() && it.length() > 0 }
                ?: File(stickerFile.parentFile, "${stickerFile.nameWithoutExtension}_static.png").takeIf { it.exists() && it.length() > 0 }
                ?: getOrGenerateStaticPng(context, stickerFile)

            if (pngFile != null && pngFile.exists() && pngFile.length() > 0 && supports("image/png")) {
                candidates.add(ResolvedPayload(pngFile, "image/png", false))
            }

            // 3. image/gif if static GIF
            val gifFile = gifPath?.let { File(it) }?.takeIf { it.exists() && it.length() > 0 }
            if (gifFile != null && supports("image/gif")) {
                candidates.add(ResolvedPayload(gifFile, "image/gif", false))
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

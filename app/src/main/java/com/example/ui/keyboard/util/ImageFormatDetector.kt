package com.example.ui.keyboard.util

import java.io.File
import java.io.FileInputStream

enum class ImageFormat(val ext: String, val mime: String) {
    WEBP("webp", "image/webp"),
    PNG("png", "image/png"),
    GIF("gif", "image/gif"),
    JPEG("jpg", "image/jpeg");

    companion object {
        fun fromMime(mime: String): ImageFormat? {
            return entries.firstOrNull { it.mime.equals(mime, ignoreCase = true) }
        }
    }
}

object ImageFormatDetector {

    /**
     * Reads magic bytes from the file content to detect the real image format.
     * Returns null if the file is unrecognized or unsupported.
     */
    fun detectFormat(file: File): ImageFormat? {
        if (!file.exists() || file.length() < 12) return null

        try {
            FileInputStream(file).use { input ->
                val header = ByteArray(12)
                val read = input.read(header)
                if (read < 12) return null

                // 1. JPEG: FF D8
                if (header[0] == 0xFF.toByte() && header[1] == 0xD8.toByte()) {
                    return ImageFormat.JPEG
                }

                // 2. PNG: 89 50 4E 47 0D 0A 1A 0A
                if (header[0] == 0x89.toByte() && header[1] == 0x50.toByte() &&
                    header[2] == 0x4E.toByte() && header[3] == 0x47.toByte() &&
                    header[4] == 0x0D.toByte() && header[5] == 0x0A.toByte() &&
                    header[6] == 0x1A.toByte() && header[7] == 0x0A.toByte()
                ) {
                    return ImageFormat.PNG
                }

                // 3. GIF: GIF87a or GIF89a
                if (header[0] == 'G'.code.toByte() && header[1] == 'I'.code.toByte() &&
                    header[2] == 'F'.code.toByte() && header[3] == '8'.code.toByte()
                ) {
                    return ImageFormat.GIF
                }

                // 4. WebP: RIFF....WEBP
                if (header[0] == 'R'.code.toByte() && header[1] == 'I'.code.toByte() &&
                    header[2] == 'F'.code.toByte() && header[3] == 'F'.code.toByte() &&
                    header[8] == 'W'.code.toByte() && header[9] == 'E'.code.toByte() &&
                    header[10] == 'B'.code.toByte() && header[11] == 'P'.code.toByte()
                ) {
                    return ImageFormat.WEBP
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ImageFormatDetector", "Failed to detect image format", e)
            return null
        }

        return null
    }

    /**
     * Checks if the image is animated:
     * - WebP via the VP8X animation flag (bit 0x02 at offset 20) or presence of ANIM/ANMF chunks.
     * - GIF by counting image frame descriptors (0x2C); more than 1 frame = animated.
     * - PNG and JPEG are strictly static (APNG is treated as static; first frame only).
     */
    fun isAnimated(file: File, format: ImageFormat): Boolean {
        return when (format) {
            ImageFormat.WEBP -> isAnimatedWebp(file)
            ImageFormat.GIF -> isAnimatedGif(file)
            ImageFormat.PNG, ImageFormat.JPEG -> false
        }
    }

    private fun isAnimatedWebp(file: File): Boolean {
        if (!file.exists() || file.length() < 30) return false
        try {
            FileInputStream(file).use { input ->
                val header = ByteArray(64)
                val read = input.read(header)
                if (read >= 21) {
                    // Check RIFF and WEBP
                    if (header[0] == 'R'.code.toByte() && header[1] == 'I'.code.toByte() &&
                        header[2] == 'F'.code.toByte() && header[3] == 'F'.code.toByte() &&
                        header[8] == 'W'.code.toByte() && header[9] == 'E'.code.toByte() &&
                        header[10] == 'B'.code.toByte() && header[11] == 'P'.code.toByte()
                    ) {
                        // Check VP8X chunk
                        if (header[12] == 'V'.code.toByte() && header[13] == 'P'.code.toByte() &&
                            header[14] == '8'.code.toByte() && header[15] == 'X'.code.toByte()
                        ) {
                            // Flag byte at offset 20: bit 1 (0x02) is Animation flag
                            val flags = header[20].toInt()
                            if ((flags and 0x02) != 0) {
                                return true
                            }
                        }
                    }
                }
            }

            // Fallback scan for 'ANIM' or 'ANMF' chunks
            FileInputStream(file).use { input ->
                val buffer = ByteArray(minOf(file.length().toInt(), 8192))
                val bytesRead = input.read(buffer)
                val animPattern = "ANIM".toByteArray()
                val anmfPattern = "ANMF".toByteArray()
                if (containsBytes(buffer, bytesRead, animPattern) || containsBytes(buffer, bytesRead, anmfPattern)) {
                    return true
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ImageFormatDetector", "Error checking animated WebP for ${file.name}", e)
            return false
        }
        return false
    }

    private fun isAnimatedGif(file: File): Boolean {
        if (!file.exists() || file.length() < 30) return false
        try {
            FileInputStream(file).use { input ->
                val buffer = ByteArray(minOf(file.length().toInt(), 65536))
                val bytesRead = input.read(buffer)
                var frameCount = 0
                for (i in 0 until bytesRead) {
                    if (buffer[i] == 0x2C.toByte()) { // Image descriptor separator
                        frameCount++
                        if (frameCount > 1) return true
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ImageFormatDetector", "Error checking animated GIF for ${file.name}", e)
            return false
        }
        return false
    }

    private fun containsBytes(source: ByteArray, length: Int, target: ByteArray): Boolean {
        if (target.isEmpty() || length < target.size) return false
        val max = length - target.size
        for (i in 0..max) {
            var found = true
            for (j in target.indices) {
                if (source[i + j] != target[j]) {
                    found = false
                    break
                }
            }
            if (found) return true
        }
        return false
    }
}

package com.example.ui.keyboard.util

import java.io.File
import java.io.FileInputStream

object WebpAnimationHelper {
    /**
     * Checks if a file is an animated WebP or animated GIF.
     * WebP:
     * - RIFF header (bytes 0..3: RIFF)
     * - WEBP format (bytes 8..11: WEBP)
     * - VP8X chunk (bytes 12..15: VP8X)
     * - In VP8X chunk, byte 20 has Animation flag at bit 1 (0x02)
     * OR contains 'ANIM' / 'ANMF' chunk FourCC.
     * GIF:
     * - Starts with GIF89a or GIF87a, and contains more than 1 image descriptor (0x2C).
     */
    fun isAnimated(file: File): Boolean {
        if (!file.exists() || file.length() < 30) return false
        val ext = file.extension.lowercase()
        return if (ext == "gif") {
            isAnimatedGif(file)
        } else {
            isAnimatedWebp(file)
        }
    }

    fun isAnimatedWebp(file: File): Boolean {
        try {
            FileInputStream(file).use { input ->
                val header = ByteArray(64)
                val read = input.read(header)
                if (read < 30) return false

                // Check RIFF and WEBP
                if (header[0] == 'R'.code.toByte() &&
                    header[1] == 'I'.code.toByte() &&
                    header[2] == 'F'.code.toByte() &&
                    header[3] == 'F'.code.toByte() &&
                    header[8] == 'W'.code.toByte() &&
                    header[9] == 'E'.code.toByte() &&
                    header[10] == 'B'.code.toByte() &&
                    header[11] == 'P'.code.toByte()
                ) {
                    // Check VP8X chunk
                    if (header[12] == 'V'.code.toByte() &&
                        header[13] == 'P'.code.toByte() &&
                        header[14] == '8'.code.toByte() &&
                        header[15] == 'X'.code.toByte()
                    ) {
                        // Flag byte is at index 20
                        val flags = header[20].toInt()
                        if ((flags and 0x02) != 0) {
                            return true
                        }
                    }
                }
            }

            // Fallback scan first 4KB for 'ANIM' or 'ANMF' chunk identifier
            FileInputStream(file).use { input ->
                val buffer = ByteArray(minOf(file.length().toInt(), 4096))
                val bytesRead = input.read(buffer)
                val animPattern = "ANIM".toByteArray()
                val anmfPattern = "ANMF".toByteArray()
                if (containsBytes(buffer, bytesRead, animPattern) || containsBytes(buffer, bytesRead, anmfPattern)) {
                    return true
                }
            }
        } catch (_: Exception) {}
        return false
    }

    private fun isAnimatedGif(file: File): Boolean {
        try {
            FileInputStream(file).use { input ->
                val buffer = ByteArray(minOf(file.length().toInt(), 8192))
                val bytesRead = input.read(buffer)
                var frameCount = 0
                for (i in 0 until bytesRead - 1) {
                    if (buffer[i] == 0x2C.toByte()) { // Image separator
                        frameCount++
                        if (frameCount > 1) return true
                    }
                }
            }
        } catch (_: Exception) {}
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

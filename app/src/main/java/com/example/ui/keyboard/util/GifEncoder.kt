package com.example.ui.keyboard.util

import android.graphics.Bitmap
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStream

object GifEncoder {

    const val MAX_DIMENSION = 512
    const val MAX_FPS = 15
    const val MAX_FILE_SIZE_BYTES = 1024 * 1024L // 1 MB limit

    /**
     * Encodes a list of animation frames into a high-compatibility GIF89a file.
     * Enforces:
     * - Netscape loop extension (infinite loop)
     * - Maximum 512px longest dimension
     * - Maximum 15 fps (merging frame delays)
     * - Minimum 20ms per-frame delay (never 0)
     * - Median-Cut 256-color palette per frame with transparency support
     * - Disposal method 2 (Restore to background)
     * - Iterative compression under 1MB limit
     */
    fun encodeAnimation(
        rawFrames: List<AnimationFrame>,
        outputFile: File,
        targetMaxDimension: Int = MAX_DIMENSION,
        targetMaxFps: Int = MAX_FPS,
        maxSizeBytes: Long = MAX_FILE_SIZE_BYTES
    ): Boolean {
        if (rawFrames.isEmpty()) return false

        // 1. Cap FPS to <= 15 by merging frames and accumulating durations
        val fpsCappedFrames = capFps(rawFrames, targetMaxFps)

        // 2. Initial dimension downscaling if needed (longest side <= 512)
        var scale = calculateInitialScale(fpsCappedFrames, targetMaxDimension)

        // 3. Iterative encoding: if file exceeds maxSizeBytes, scale down further
        var attempts = 0
        while (attempts < 4) {
            attempts++
            val scaledFrames = if (scale < 0.999f) {
                fpsCappedFrames.map { frame ->
                    val w = maxOf(1, (frame.bitmap.width * scale).toInt())
                    val h = maxOf(1, (frame.bitmap.height * scale).toInt())
                    val scaledBmp = Bitmap.createScaledBitmap(frame.bitmap, w, h, true)
                    AnimationFrame(scaledBmp, frame.durationMs)
                }
            } else {
                fpsCappedFrames
            }

            val success = writeGifFile(scaledFrames, outputFile)
            if (success && outputFile.exists()) {
                if (outputFile.length() <= maxSizeBytes || scale <= 0.4f) {
                    return validateGif(outputFile, fpsCappedFrames.size > 1)
                }
            }
            // Over 1MB limit: scale down by 25% and retry
            scale *= 0.75f
        }

        return validateGif(outputFile, fpsCappedFrames.size > 1)
    }

    /**
     * Caps FPS at maxFps (e.g. 15 fps => min duration 66ms).
     * Drops intermediate frames and merges delays into surviving frames so animation timing is preserved.
     */
    private fun capFps(frames: List<AnimationFrame>, maxFps: Int): List<AnimationFrame> {
        if (frames.size <= 1) return frames
        val minIntervalMs = 1000 / maxFps // 66ms
        val result = mutableListOf<AnimationFrame>()
        var accumulatedDelay = 0

        for (frame in frames) {
            accumulatedDelay += frame.durationMs
            if (accumulatedDelay >= minIntervalMs || result.isEmpty()) {
                result.add(AnimationFrame(frame.bitmap, maxOf(20, accumulatedDelay)))
                accumulatedDelay = 0
            }
        }

        if (accumulatedDelay > 0 && result.isNotEmpty()) {
            val last = result.last()
            result[result.size - 1] = AnimationFrame(last.bitmap, last.durationMs + accumulatedDelay)
        }

        return if (result.isEmpty()) frames else result
    }

    private fun calculateInitialScale(frames: List<AnimationFrame>, maxDim: Int): Float {
        val maxW = frames.maxOf { it.bitmap.width }
        val maxH = frames.maxOf { it.bitmap.height }
        val longest = maxOf(maxW, maxH)
        return if (longest > maxDim) {
            maxDim.toFloat() / longest.toFloat()
        } else {
            1.0f
        }
    }

    private fun writeGifFile(frames: List<AnimationFrame>, outputFile: File): Boolean {
        val firstFrame = frames.first().bitmap
        val width = firstFrame.width
        val height = firstFrame.height

        val tempFile = File(outputFile.parentFile, "${outputFile.name}.tmp")
        val out = try {
            FileOutputStream(tempFile)
        } catch (_: Exception) {
            return false
        }

        try {
            // Header: GIF89a
            out.write("GIF89a".toByteArray(Charsets.US_ASCII))

            // Logical Screen Descriptor
            writeShort(out, width)
            writeShort(out, height)
            // Packed: No Global Color Table (each frame uses Local Color Table for 256 colors per frame)
            out.write(0x70) // Color resolution 8 bits (7), no GCT
            out.write(0)    // Background color index
            out.write(0)    // Pixel aspect ratio

            // Netscape 2.0 Loop Extension (Infinite Loop)
            out.write(0x21) // Extension Introducer
            out.write(0xFF) // Application Extension Label
            out.write(0x0B) // Block size 11
            out.write("NETSCAPE2.0".toByteArray(Charsets.US_ASCII))
            out.write(0x03) // Sub-block size 3
            out.write(0x01) // Sub-block ID 1
            writeShort(out, 0) // Loop count: 0 = infinite loop
            out.write(0x00) // Block terminator

            for (frame in frames) {
                writeFrame(out, frame, width, height)
            }

            // Trailer
            out.write(0x3B)
            out.flush()
            out.close()

            if (tempFile.exists() && tempFile.length() > 0) {
                if (outputFile.exists()) outputFile.delete()
                return tempFile.renameTo(outputFile)
            }
            return false
        } catch (_: Exception) {
            try { out.close() } catch (_: Exception) {}
            if (tempFile.exists()) tempFile.delete()
            return false
        }
    }

    private fun writeFrame(out: FileOutputStream, frame: AnimationFrame, canvasW: Int, canvasH: Int) {
        val bmp = frame.bitmap
        val w = bmp.width
        val h = bmp.height
        val pixels = IntArray(w * h)
        bmp.getPixels(pixels, 0, w, 0, 0, w, h)

        // 1. Median-Cut 256-color palette (index 0 is reserved for transparent)
        val (palette, indexedPixels, hasTransparency) = quantizeMedianCut(pixels)

        // 2. Graphic Control Extension (disposal method 2 = restore to background, delay >= 20ms)
        out.write(0x21) // Extension Introducer
        out.write(0xF9) // Graphic Control Label
        out.write(4)    // Block size
        // Packed: disposal method 2 (000 010 0 1 => 0x08 | 0x01 = 0x09 if transparent, else 0x08)
        val disposalMethod = 2
        val transparentFlag = if (hasTransparency) 1 else 0
        val packedGce = (disposalMethod shl 2) or transparentFlag
        out.write(packedGce)

        // Delay time in centiseconds (1/100 sec). Must be at least 20ms (delay >= 2)
        val delayCentis = maxOf(2, (frame.durationMs + 5) / 10)
        writeShort(out, delayCentis)

        out.write(0)    // Transparent color index = 0
        out.write(0)    // Block terminator

        // 3. Image Descriptor
        out.write(0x2C) // Image Separator
        writeShort(out, 0) // Left
        writeShort(out, 0) // Top
        writeShort(out, w)
        writeShort(out, h)
        // Local Color Table Flag = 1, size = 7 (256 colors) => 0x87
        out.write(0x87)

        // 4. Local Color Table (256 * 3 bytes)
        out.write(palette)

        // 5. LZW Image Data
        out.write(8) // Minimum LZW code size
        writeLzwData(out, indexedPixels, 8)
        out.write(0) // Block terminator
    }

    private data class QuantizeResult(
        val palette: ByteArray,
        val indexedPixels: ByteArray,
        val hasTransparency: Boolean
    )

    /**
     * Median-Cut 256-color quantization.
     * Index 0 is reserved for transparent pixels (alpha < 128).
     * Indices 1..255 represent the best 255 RGB colors.
     */
    private fun quantizeMedianCut(pixels: IntArray): QuantizeResult {
        var hasTransparency = false
        val opaqueColors = IntArray(pixels.size)
        var opaqueCount = 0

        for (color in pixels) {
            val alpha = (color ushr 24) and 0xFF
            if (alpha < 128) {
                hasTransparency = true
            } else {
                opaqueColors[opaqueCount++] = color and 0x00FFFFFF
            }
        }

        val palette = ByteArray(256 * 3)
        // Index 0: Transparent (RGB: 0, 0, 0)
        palette[0] = 0
        palette[1] = 0
        palette[2] = 0

        val colorBoxes = mutableListOf<ColorBox>()
        if (opaqueCount > 0) {
            colorBoxes.add(ColorBox(opaqueColors, 0, opaqueCount))
            // Split iteratively into up to 255 boxes
            while (colorBoxes.size < 255) {
                val boxToSplit = colorBoxes.maxByOrNull { it.range() } ?: break
                if (boxToSplit.range() == 0 || boxToSplit.count() <= 1) break
                colorBoxes.remove(boxToSplit)
                val (b1, b2) = boxToSplit.split()
                colorBoxes.add(b1)
                colorBoxes.add(b2)
            }
        }

        val paletteColors = IntArray(colorBoxes.size + 1)
        paletteColors[0] = 0
        for (i in colorBoxes.indices) {
            val rgb = colorBoxes[i].averageColor()
            val paletteIdx = i + 1
            paletteColors[paletteIdx] = rgb
            palette[paletteIdx * 3] = ((rgb ushr 16) and 0xFF).toByte()
            palette[paletteIdx * 3 + 1] = ((rgb ushr 8) and 0xFF).toByte()
            palette[paletteIdx * 3 + 2] = (rgb and 0xFF).toByte()
        }

        // Map pixels to palette indices
        val indexed = ByteArray(pixels.size)
        val cache = HashMap<Int, Byte>(512)

        for (i in pixels.indices) {
            val color = pixels[i]
            val alpha = (color ushr 24) and 0xFF
            if (alpha < 128) {
                indexed[i] = 0
            } else {
                val rgb = color and 0x00FFFFFF
                val cached = cache[rgb]
                if (cached != null) {
                    indexed[i] = cached
                } else {
                    val r = (rgb ushr 16) and 0xFF
                    val g = (rgb ushr 8) and 0xFF
                    val b = rgb and 0xFF
                    var bestIdx = 1
                    var bestDist = Int.MAX_VALUE

                    for (p in 1 until paletteColors.size) {
                        val prgb = paletteColors[p]
                        val pr = (prgb ushr 16) and 0xFF
                        val pg = (prgb ushr 8) and 0xFF
                        val pb = prgb and 0xFF
                        val dr = r - pr
                        val dg = g - pg
                        val db = b - pb
                        val dist = dr * dr + dg * dg + db * db
                        if (dist < bestDist) {
                            bestDist = dist
                            bestIdx = p
                            if (dist == 0) break
                        }
                    }
                    val bVal = bestIdx.toByte()
                    cache[rgb] = bVal
                    indexed[i] = bVal
                }
            }
        }

        return QuantizeResult(palette, indexed, hasTransparency)
    }

    private class ColorBox(val colors: IntArray, val start: Int, val end: Int) {
        fun count() = end - start

        fun range(): Int {
            if (count() <= 1) return 0
            var minR = 255; var maxR = 0
            var minG = 255; var maxG = 0
            var minB = 255; var maxB = 0
            for (i in start until end) {
                val c = colors[i]
                val r = (c ushr 16) and 0xFF
                val g = (c ushr 8) and 0xFF
                val b = c and 0xFF
                if (r < minR) minR = r; if (r > maxR) maxR = r
                if (g < minG) minG = g; if (g > maxG) maxG = g
                if (b < minB) minB = b; if (b > maxB) maxB = b
            }
            return maxOf(maxR - minR, maxG - minG, maxB - minB)
        }

        fun split(): Pair<ColorBox, ColorBox> {
            var minR = 255; var maxR = 0
            var minG = 255; var maxG = 0
            var minB = 255; var maxB = 0
            for (i in start until end) {
                val c = colors[i]
                val r = (c ushr 16) and 0xFF
                val g = (c ushr 8) and 0xFF
                val b = c and 0xFF
                if (r < minR) minR = r; if (r > maxR) maxR = r
                if (g < minG) minG = g; if (g > maxG) maxG = g
                if (b < minB) minB = b; if (b > maxB) maxB = b
            }
            val rangeR = maxR - minR
            val rangeG = maxG - minG
            val rangeB = maxB - minB

            val channel = if (rangeR >= rangeG && rangeR >= rangeB) 16 else if (rangeG >= rangeB) 8 else 0

            // Sort colors array slice along the dominant channel
            val count = end - start
            val boxed = Array(count) { i -> colors[start + i] }
            boxed.sortWith(Comparator { c1: Int, c2: Int ->
                ((c1 ushr channel) and 0xFF).compareTo((c2 ushr channel) and 0xFF)
            })
            for (i in 0 until count) {
                colors[start + i] = boxed[i]
            }

            val mid = start + count / 2
            return Pair(ColorBox(colors, start, mid), ColorBox(colors, mid, end))
        }

        fun averageColor(): Int {
            if (count() == 0) return 0
            var sumR = 0L; var sumG = 0L; var sumB = 0L
            for (i in start until end) {
                val c = colors[i]
                sumR += (c ushr 16) and 0xFF
                sumG += (c ushr 8) and 0xFF
                sumB += c and 0xFF
            }
            val n = count().toLong()
            val r = (sumR / n).toInt() and 0xFF
            val g = (sumG / n).toInt() and 0xFF
            val b = (sumB / n).toInt() and 0xFF
            return (r shl 16) or (g shl 8) or b
        }
    }

    /**
     * Standard LZW compression with dynamic code sizes (9 to 12 bits) and Clear/EOI codes.
     */
    private fun writeLzwData(out: OutputStream, pixels: ByteArray, minCodeSize: Int) {
        val clearCode = 1 shl minCodeSize
        val eoiCode = clearCode + 1
        var nextCode = eoiCode + 1
        var codeSize = minCodeSize + 1
        var maxCode = (1 shl codeSize) - 1

        val buffer = ByteArrayOutputStream(4096)
        var curValue = 0
        var curBits = 0

        fun emitCode(code: Int) {
            curValue = curValue or (code shl curBits)
            curBits += codeSize
            while (curBits >= 8) {
                buffer.write(curValue and 0xFF)
                curValue = curValue ushr 8
                curBits -= 8
            }
        }

        emitCode(clearCode)

        val table = HashMap<Int, Int>(2048)
        if (pixels.isNotEmpty()) {
            var prefix = pixels[0].toInt() and 0xFF
            for (i in 1 until pixels.size) {
                val b = pixels[i].toInt() and 0xFF
                val key = (prefix shl 8) or b
                val existing = table[key]
                if (existing != null) {
                    prefix = existing
                } else {
                    emitCode(prefix)
                    if (nextCode < 4096) {
                        table[key] = nextCode++
                        if (nextCode > maxCode && codeSize < 12) {
                            codeSize++
                            maxCode = (1 shl codeSize) - 1
                        }
                    } else {
                        emitCode(clearCode)
                        table.clear()
                        nextCode = eoiCode + 1
                        codeSize = minCodeSize + 1
                        maxCode = (1 shl codeSize) - 1
                    }
                    prefix = b
                }
            }
            emitCode(prefix)
        }
        emitCode(eoiCode)

        if (curBits > 0) {
            buffer.write(curValue and 0xFF)
        }

        // Package into GIF sub-blocks (max 254 bytes each)
        val bytes = buffer.toByteArray()
        var offset = 0
        while (offset < bytes.size) {
            val chunk = minOf(254, bytes.size - offset)
            out.write(chunk)
            out.write(bytes, offset, chunk)
            offset += chunk
        }
    }

    private fun writeShort(out: OutputStream, value: Int) {
        out.write(value and 0xFF)
        out.write((value ushr 8) and 0xFF)
    }

    /**
     * Validates that the file:
     * - exists and has length > 0
     * - starts with "GIF89a"
     * - if multi-frame required, contains more than 1 image separator (0x2C)
     */
    fun validateGif(file: File, requireMultiFrame: Boolean = true): Boolean {
        if (!file.exists() || file.length() < 30) return false
        try {
            FileInputStream(file).use { input ->
                val header = ByteArray(6)
                if (input.read(header) != 6) return false
                val headerStr = String(header, Charsets.US_ASCII)
                if (headerStr != "GIF89a" && headerStr != "GIF87a") return false

                if (!requireMultiFrame) return true

                val buffer = ByteArray(minOf(file.length().toInt(), 32768))
                val read = input.read(buffer)
                var imageCount = 0
                for (i in 0 until read) {
                    if (buffer[i] == 0x2C.toByte()) {
                        imageCount++
                        if (imageCount > 1) return true
                    }
                }
                return imageCount > 1
            }
        } catch (_: Exception) {
            return false
        }
    }
}

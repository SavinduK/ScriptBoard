package com.example.ui.keyboard.util

import android.graphics.Bitmap
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

/**
 * Pure-Java NeuQuant / Median-Cut streaming GIF writer fallback in case native JNI libraries fail to load.
 */
class PureJavaNeuQuantGifWriter(
    private val width: Int,
    private val height: Int,
    outputFile: File
) {
    private val out: FileOutputStream = FileOutputStream(outputFile)
    private var isFirstFrame = true

    init {
        // GIF89a Header
        out.write("GIF89a".toByteArray(Charsets.US_ASCII))
        // Logical Screen Descriptor
        writeShort(out, width)
        writeShort(out, height)
        out.write(0x70) // No Global Color Table (each frame has Local Color Table)
        out.write(0)    // Background color
        out.write(0)    // Pixel aspect ratio

        // Netscape 2.0 Loop Extension (Infinite Loop)
        out.write(0x21)
        out.write(0xFF)
        out.write(0x0B)
        out.write("NETSCAPE2.0".toByteArray(Charsets.US_ASCII))
        out.write(0x03)
        out.write(0x01)
        writeShort(out, 0) // Loop count 0 = infinite
        out.write(0x00)
    }

    fun encodeFrame(bitmap: Bitmap, delayCentiseconds: Int) {
        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

        var hasTransparency = false
        val opaqueColors = IntArray(pixels.size)
        var opaqueCount = 0

        for (c in pixels) {
            val a = (c ushr 24) and 0xFF
            if (a < 128) {
                hasTransparency = true
            } else {
                opaqueColors[opaqueCount++] = c and 0x00FFFFFF
            }
        }

        val palette = ByteArray(256 * 3)
        // Index 0: Transparent
        palette[0] = 0
        palette[1] = 0
        palette[2] = 0

        val colorBoxes = mutableListOf<MedianBox>()
        if (opaqueCount > 0) {
            colorBoxes.add(MedianBox(opaqueColors, 0, opaqueCount))
            while (colorBoxes.size < 255) {
                val box = colorBoxes.maxByOrNull { it.range() } ?: break
                if (box.range() == 0 || box.count <= 1) break
                colorBoxes.remove(box)
                val (b1, b2) = box.split()
                colorBoxes.add(b1)
                colorBoxes.add(b2)
            }
        }

        val paletteColors = IntArray(colorBoxes.size + 1)
        paletteColors[0] = 0
        for (i in colorBoxes.indices) {
            val rgb = colorBoxes[i].averageColor()
            val idx = i + 1
            paletteColors[idx] = rgb
            palette[idx * 3] = ((rgb ushr 16) and 0xFF).toByte()
            palette[idx * 3 + 1] = ((rgb ushr 8) and 0xFF).toByte()
            palette[idx * 3 + 2] = (rgb and 0xFF).toByte()
        }

        // Map pixels to palette indices
        val indexedPixels = ByteArray(pixels.size)
        val colorCache = HashMap<Int, Byte>()
        for (i in pixels.indices) {
            val c = pixels[i]
            val a = (c ushr 24) and 0xFF
            if (a < 128) {
                indexedPixels[i] = 0
            } else {
                val rgb = c and 0x00FFFFFF
                val cached = colorCache[rgb]
                if (cached != null) {
                    indexedPixels[i] = cached
                } else {
                    var bestIdx = 1
                    var bestDist = Int.MAX_VALUE
                    val r = (rgb ushr 16) and 0xFF
                    val g = (rgb ushr 8) and 0xFF
                    val b = rgb and 0xFF

                    for (j in 1 until paletteColors.size) {
                        val pr = (paletteColors[j] ushr 16) and 0xFF
                        val pg = (paletteColors[j] ushr 8) and 0xFF
                        val pb = paletteColors[j] and 0xFF
                        val dr = r - pr
                        val dg = g - pg
                        val db = b - pb
                        val dist = dr * dr + dg * dg + db * db
                        if (dist < bestDist) {
                            bestDist = dist
                            bestIdx = j
                            if (dist == 0) break
                        }
                    }
                    val bIdx = bestIdx.toByte()
                    colorCache[rgb] = bIdx
                    indexedPixels[i] = bIdx
                }
            }
        }

        // Graphic Control Extension (disposal method 2 = restore to background)
        out.write(0x21)
        out.write(0xF9)
        out.write(4)
        val disposal = 2
        val transFlag = if (hasTransparency) 1 else 0
        out.write((disposal shl 2) or transFlag)
        writeShort(out, maxOf(2, delayCentiseconds))
        out.write(0) // Transparent index 0
        out.write(0)

        // Image Descriptor
        out.write(0x2C)
        writeShort(out, 0)
        writeShort(out, 0)
        writeShort(out, w)
        writeShort(out, h)
        out.write(0x87) // Local Color Table (256 colors)

        // Local Color Table
        out.write(palette)

        // LZW Compressed Pixel Data
        out.write(8) // Init code size 8
        writeLzw(out, indexedPixels, 8)
        out.write(0) // Block terminator
    }

    fun close() {
        try {
            out.write(0x3B) // GIF Trailer
            out.flush()
            out.close()
        } catch (e: Exception) {
            android.util.Log.e("PureJavaNeuQuantGif", "Error closing GIF stream", e)
        }
    }

    private fun writeShort(out: OutputStream, value: Int) {
        out.write(value and 0xFF)
        out.write((value ushr 8) and 0xFF)
    }

    private fun writeLzw(out: OutputStream, pixels: ByteArray, initCodeSize: Int) {
        val clearCode = 1 shl initCodeSize
        val eoiCode = clearCode + 1
        var codeSize = initCodeSize + 1
        var maxCode = 1 shl codeSize

        val prefixTable = IntArray(5003) { -1 }
        val suffixTable = IntArray(5003)
        val codeTable = IntArray(5003)

        var curAccum = 0
        var curBits = 0

        fun sendCode(code: Int) {
            curAccum = curAccum or (code shl curBits)
            curBits += codeSize
            while (curBits >= 8) {
                out.write(curAccum and 0xFF)
                curAccum = curAccum ushr 8
                curBits -= 8
            }
        }

        fun flushBits() {
            if (curBits > 0) {
                out.write(curAccum and 0xFF)
                curBits = 0
                curAccum = 0
            }
        }

        fun clearTable() {
            prefixTable.fill(-1)
            codeSize = initCodeSize + 1
            maxCode = 1 shl codeSize
        }

        // LZW buffering into 254-byte sub-blocks
        val subBlockBuffer = ByteArrayOutputStream()
        val origOut = out

        val bufferOut = object : OutputStream() {
            override fun write(b: Int) {
                subBlockBuffer.write(b)
                if (subBlockBuffer.size() >= 254) {
                    origOut.write(subBlockBuffer.size())
                    subBlockBuffer.writeTo(origOut)
                    subBlockBuffer.reset()
                }
            }
        }

        var nextCode = eoiCode + 1
        clearTable()

        // Send clear code
        curAccum = curAccum or (clearCode shl curBits)
        curBits += codeSize
        while (curBits >= 8) {
            bufferOut.write(curAccum and 0xFF)
            curAccum = curAccum ushr 8
            curBits -= 8
        }

        if (pixels.isNotEmpty()) {
            var ent = pixels[0].toInt() and 0xFF
            for (i in 1 until pixels.size) {
                val c = pixels[i].toInt() and 0xFF
                var disp = (c shl 4) xor ent
                if (disp == 0) disp = 1

                var hit = -1
                var probe = ((c shl 8) or ent) % 5003
                while (prefixTable[probe] != -1) {
                    if (prefixTable[probe] == ent && suffixTable[probe] == c) {
                        hit = codeTable[probe]
                        break
                    }
                    probe = (probe + disp) % 5003
                }

                if (hit != -1) {
                    ent = hit
                } else {
                    // Send code for ent
                    curAccum = curAccum or (ent shl curBits)
                    curBits += codeSize
                    while (curBits >= 8) {
                        bufferOut.write(curAccum and 0xFF)
                        curAccum = curAccum ushr 8
                        curBits -= 8
                    }

                    if (nextCode < 4096) {
                        prefixTable[probe] = ent
                        suffixTable[probe] = c
                        codeTable[probe] = nextCode++
                        if (nextCode > maxCode && codeSize < 12) {
                            codeSize++
                            maxCode = 1 shl codeSize
                        }
                    } else {
                        // Clear table
                        clearTable()
                        nextCode = eoiCode + 1
                        curAccum = curAccum or (clearCode shl curBits)
                        curBits += codeSize
                        while (curBits >= 8) {
                            bufferOut.write(curAccum and 0xFF)
                            curAccum = curAccum ushr 8
                            curBits -= 8
                        }
                    }
                    ent = c
                }
            }

            // Output remaining ent
            curAccum = curAccum or (ent shl curBits)
            curBits += codeSize
            while (curBits >= 8) {
                bufferOut.write(curAccum and 0xFF)
                curAccum = curAccum ushr 8
                curBits -= 8
            }
        }

        // Output EOI code
        curAccum = curAccum or (eoiCode shl curBits)
        curBits += codeSize
        while (curBits >= 8) {
            bufferOut.write(curAccum and 0xFF)
            curAccum = curAccum ushr 8
            curBits -= 8
        }
        if (curBits > 0) {
            bufferOut.write(curAccum and 0xFF)
        }

        if (subBlockBuffer.size() > 0) {
            origOut.write(subBlockBuffer.size())
            subBlockBuffer.writeTo(origOut)
        }
    }

    private class MedianBox(val colors: IntArray, val offset: Int, val count: Int) {
        var minR = 255; var maxR = 0
        var minG = 255; var maxG = 0
        var minB = 255; var maxB = 0

        init {
            for (i in offset until offset + count) {
                val c = colors[i]
                val r = (c ushr 16) and 0xFF
                val g = (c ushr 8) and 0xFF
                val b = c and 0xFF
                if (r < minR) minR = r
                if (r > maxR) maxR = r
                if (g < minG) minG = g
                if (g > maxG) maxG = g
                if (b < minB) minB = b
                if (b > maxB) maxB = b
            }
        }

        fun range(): Int {
            return maxOf(maxR - minR, maxG - minG, maxB - minB)
        }

        fun averageColor(): Int {
            var sumR = 0L; var sumG = 0L; var sumB = 0L
            for (i in offset until offset + count) {
                val c = colors[i]
                sumR += (c ushr 16) and 0xFF
                sumG += (c ushr 8) and 0xFF
                sumB += c and 0xFF
            }
            val avgR = (sumR / count).toInt()
            val avgG = (sumG / count).toInt()
            val avgB = (sumB / count).toInt()
            return (avgR shl 16) or (avgG shl 8) or avgB
        }

        fun split(): Pair<MedianBox, MedianBox> {
            val dr = maxR - minR
            val dg = maxG - minG
            val db = maxB - minB

            val sortChannel = when {
                dr >= dg && dr >= db -> 16 // Red
                dg >= dr && dg >= db -> 8  // Green
                else -> 0                  // Blue
            }

            val boxed = colors.sliceArray(offset until offset + count).toTypedArray()
            boxed.sortWith(Comparator { c1, c2 ->
                val v1 = (c1 ushr sortChannel) and 0xFF
                val v2 = (c2 ushr sortChannel) and 0xFF
                v1.compareTo(v2)
            })
            for (i in 0 until count) {
                colors[offset + i] = boxed[i]
            }

            val mid = count / 2
            return Pair(
                MedianBox(colors, offset, mid),
                MedianBox(colors, offset + mid, count - mid)
            )
        }
    }
}

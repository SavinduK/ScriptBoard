package com.example.ui.keyboard.util

import android.graphics.Bitmap
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

/**
 * Lightweight, zero-dependency GIF writer for converting bitmaps to standard GIF89a format.
 */
object BasicGifWriter {

    fun writeSingleFrameGif(bitmap: Bitmap, outputFile: File) {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val out = FileOutputStream(outputFile)
        try {
            // Header
            out.write("GIF89a".toByteArray(Charsets.US_ASCII))

            // Logical Screen Descriptor
            writeShort(out, width)
            writeShort(out, height)
            out.write(0xF7) // Global color table flag = 1, color resolution = 7, sort = 0, size = 7 (256 colors)
            out.write(0)    // Background color index
            out.write(0)    // Pixel aspect ratio

            // 256-color palette (quantized)
            val palette = ByteArray(256 * 3)
            val colorMap = HashMap<Int, Int>()
            var colorCount = 0

            // Reserve index 0 for transparent
            palette[0] = 0
            palette[1] = 0
            palette[2] = 0
            colorCount++

            val indexedPixels = ByteArray(pixels.size)
            for (i in pixels.indices) {
                val color = pixels[i]
                val alpha = (color ushr 24) and 0xFF
                if (alpha < 128) {
                    indexedPixels[i] = 0
                } else {
                    val rgb = color and 0x00FFFFFF
                    val r = ((color ushr 16) and 0xE0).toByte()
                    val g = ((color ushr 8) and 0xE0).toByte()
                    val b = (color and 0xC0).toByte()
                    val quantized = (r.toInt() shl 16) or (g.toInt() shl 8) or b.toInt()

                    var idx = colorMap[quantized]
                    if (idx == null) {
                        if (colorCount < 256) {
                            idx = colorCount
                            palette[idx * 3] = ((color ushr 16) and 0xFF).toByte()
                            palette[idx * 3 + 1] = ((color ushr 8) and 0xFF).toByte()
                            palette[idx * 3 + 2] = (color and 0xFF).toByte()
                            colorMap[quantized] = idx
                            colorCount++
                        } else {
                            idx = 1
                        }
                    }
                    indexedPixels[i] = idx.toByte()
                }
            }

            out.write(palette)

            // Graphic Control Extension (transparency & delay)
            out.write(0x21) // Extension Introducer
            out.write(0xF9) // Graphic Control Label
            out.write(4)    // Block size
            out.write(0x01) // Transparent color flag = 1, disposal method = 0
            writeShort(out, 10) // Delay time (10/100 sec)
            out.write(0)    // Transparent color index = 0
            out.write(0)    // Block terminator

            // Image Descriptor
            out.write(0x2C) // Image separator
            writeShort(out, 0) // Left
            writeShort(out, 0) // Top
            writeShort(out, width)
            writeShort(out, height)
            out.write(0)    // No local color table

            // LZW Image Data
            out.write(8) // Minimum LZW code size for 256 colors
            writeLzwData(out, indexedPixels, 8)
            out.write(0) // Block terminator

            // Trailer
            out.write(0x3B)
            out.flush()
        } finally {
            out.close()
        }
    }

    private fun writeShort(out: FileOutputStream, value: Int) {
        out.write(value and 0xFF)
        out.write((value ushr 8) and 0xFF)
    }

    private fun writeLzwData(out: FileOutputStream, pixels: ByteArray, minCodeSize: Int) {
        val clearCode = 1 shl minCodeSize
        val eoiCode = clearCode + 1

        val buffer = ByteArrayOutputStream()
        var curBits = 0
        var curValue = 0
        var codeSize = minCodeSize + 1

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
        for (b in pixels) {
            emitCode(b.toInt() and 0xFF)
        }
        emitCode(eoiCode)

        if (curBits > 0) {
            buffer.write(curValue and 0xFF)
        }

        // Write in sub-blocks of max 254 bytes
        val bytes = buffer.toByteArray()
        var offset = 0
        while (offset < bytes.size) {
            val chunkSize = minOf(254, bytes.size - offset)
            out.write(chunkSize)
            out.write(bytes, offset, chunkSize)
            offset += chunkSize
        }
    }
}

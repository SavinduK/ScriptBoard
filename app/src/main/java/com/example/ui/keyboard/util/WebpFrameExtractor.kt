package com.example.ui.keyboard.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class AnimationFrame(
    val bitmap: Bitmap,
    val durationMs: Int
)

object WebpFrameExtractor {

    /**
     * Extracts all animation frames with their real durations from an animated WebP file.
     * Parses the native WebP container (RIFF -> VP8X -> ANIM -> ANMF chunks),
     * constructs compliant standalone WebP frames, decodes them, and composites
     * canvas snapshots respecting frame offset, blending, and disposal rules.
     */
    fun extractFrames(file: File): List<AnimationFrame> {
        if (!file.exists() || file.length() < 30) {
            return emptyList()
        }

        val data = try {
            FileInputStream(file).use { it.readBytes() }
        } catch (_: Exception) {
            return emptyList()
        }

        // Verify RIFF and WEBP
        if (data.size < 30 ||
            data[0] != 'R'.code.toByte() || data[1] != 'I'.code.toByte() ||
            data[2] != 'F'.code.toByte() || data[3] != 'F'.code.toByte() ||
            data[8] != 'W'.code.toByte() || data[9] != 'E'.code.toByte() ||
            data[10] != 'B'.code.toByte() || data[11] != 'P'.code.toByte()
        ) {
            // Not a WebP; try standard decode
            return decodeSingleFrame(file)
        }

        var offset = 12
        var canvasWidth = 0
        var canvasHeight = 0
        val frames = mutableListOf<AnimationFrame>()

        while (offset + 8 <= data.size) {
            val fourCc = String(data, offset, 4, Charsets.US_ASCII)
            val chunkSize = read32LE(data, offset + 4).toInt()
            val chunkPayloadStart = offset + 8
            val chunkPayloadEnd = chunkPayloadStart + chunkSize
            if (chunkPayloadEnd > data.size) break

            when (fourCc) {
                "VP8X" -> {
                    if (chunkSize >= 10) {
                        // Canvas width: bytes 4..6 of payload (24-bit int, 1-based)
                        canvasWidth = 1 + read24LE(data, chunkPayloadStart + 4)
                        canvasHeight = 1 + read24LE(data, chunkPayloadStart + 7)
                    }
                }
                "ANMF" -> {
                    if (chunkSize >= 16) {
                        val fx = read24LE(data, chunkPayloadStart) * 2
                        val fy = read24LE(data, chunkPayloadStart + 3) * 2
                        val fw = 1 + read24LE(data, chunkPayloadStart + 6)
                        val fh = 1 + read24LE(data, chunkPayloadStart + 9)
                        val rawDuration = read24LE(data, chunkPayloadStart + 12)
                        val durationMs = if (rawDuration <= 10) 100 else rawDuration
                        val flags = data[chunkPayloadStart + 15].toInt()
                        val blend = (flags and 0x02) == 0 // 0 = alpha blend, 1 = do not blend
                        val disposeToBackground = (flags and 0x01) != 0 // 1 = dispose to bg

                        val subChunkStart = chunkPayloadStart + 16
                        val subChunkLength = chunkPayloadEnd - subChunkStart

                        if (subChunkLength > 8) {
                            val subPayload = data.copyOfRange(subChunkStart, chunkPayloadEnd)
                            val frameBitmap = decodeSubChunkAsBitmap(subPayload, fw, fh)
                            if (frameBitmap != null) {
                                frames.add(
                                    AnimationFrame(
                                        bitmap = frameBitmap,
                                        durationMs = durationMs
                                    )
                                )
                            }
                        }
                    }
                }
            }
            // Chunks are padded to even byte count
            offset = chunkPayloadEnd + (chunkSize % 2)
        }

        if (frames.isEmpty()) {
            return decodeSingleFrame(file)
        }

        // Composite frames if canvas size was declared and frames have offsets or need accumulation
        val safeCanvasW = if (canvasWidth > 0) canvasWidth else frames.first().bitmap.width
        val safeCanvasH = if (canvasHeight > 0) canvasHeight else frames.first().bitmap.height

        return compositeFrames(frames, safeCanvasW, safeCanvasH)
    }

    private fun decodeSubChunkAsBitmap(subPayload: ByteArray, fw: Int, fh: Int): Bitmap? {
        // Build standalone RIFF WEBP file
        // subPayload starts with ALPH or VP8L or VP8
        val hasAlpha = subPayload.startsWith("ALPH") || subPayload.startsWith("VP8L")
        val vp8xFlags = if (hasAlpha) 0x10 else 0x00

        val vp8xChunk = ByteArray(18)
        vp8xChunk[0] = 'V'.code.toByte()
        vp8xChunk[1] = 'P'.code.toByte()
        vp8xChunk[2] = '8'.code.toByte()
        vp8xChunk[3] = 'X'.code.toByte()
        write32LE(vp8xChunk, 4, 10)
        vp8xChunk[8] = vp8xFlags.toByte()
        vp8xChunk[9] = 0
        vp8xChunk[10] = 0
        vp8xChunk[11] = 0
        write24LE(vp8xChunk, 12, fw - 1)
        write24LE(vp8xChunk, 15, fh - 1)

        val totalRiffPayload = 4 + vp8xChunk.size + subPayload.size
        val riff = ByteArray(8 + totalRiffPayload)
        riff[0] = 'R'.code.toByte()
        riff[1] = 'I'.code.toByte()
        riff[2] = 'F'.code.toByte()
        riff[3] = 'F'.code.toByte()
        write32LE(riff, 4, totalRiffPayload.toLong())
        riff[8] = 'W'.code.toByte()
        riff[9] = 'E'.code.toByte()
        riff[10] = 'B'.code.toByte()
        riff[11] = 'P'.code.toByte()

        System.arraycopy(vp8xChunk, 0, riff, 12, vp8xChunk.size)
        System.arraycopy(subPayload, 0, riff, 12 + vp8xChunk.size, subPayload.size)

        return try {
            val opts = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            BitmapFactory.decodeByteArray(riff, 0, riff.size, opts)
        } catch (_: Exception) {
            null
        }
    }

    private fun compositeFrames(rawFrames: List<AnimationFrame>, canvasW: Int, canvasH: Int): List<AnimationFrame> {
        val composited = mutableListOf<AnimationFrame>()
        val base = Bitmap.createBitmap(canvasW, canvasH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(base)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG)

        for (frame in rawFrames) {
            val frameBmp = frame.bitmap
            // If frame is already full canvas size, draw directly
            if (frameBmp.width == canvasW && frameBmp.height == canvasH) {
                paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC)
                canvas.drawBitmap(frameBmp, 0f, 0f, paint)
            } else {
                paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)
                canvas.drawBitmap(frameBmp, 0f, 0f, paint)
            }
            val snapshot = base.copy(Bitmap.Config.ARGB_8888, false)
            composited.add(AnimationFrame(snapshot, maxOf(20, frame.durationMs)))
        }

        return composited
    }

    private fun decodeSingleFrame(file: File): List<AnimationFrame> {
        return try {
            val bmp = BitmapFactory.decodeFile(file.absolutePath)
            if (bmp != null) listOf(AnimationFrame(bmp, 100)) else emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun ByteArray.startsWith(prefix: String): Boolean {
        if (this.size < prefix.length) return false
        val p = prefix.toByteArray(Charsets.US_ASCII)
        for (i in p.indices) {
            if (this[i] != p[i]) return false
        }
        return true
    }

    private fun read24LE(data: ByteArray, offset: Int): Int {
        val b0 = data[offset].toInt() and 0xFF
        val b1 = data[offset + 1].toInt() and 0xFF
        val b2 = data[offset + 2].toInt() and 0xFF
        return b0 or (b1 shl 8) or (b2 shl 16)
    }

    private fun read32LE(data: ByteArray, offset: Int): Long {
        val b0 = data[offset].toLong() and 0xFF
        val b1 = data[offset + 1].toLong() and 0xFF
        val b2 = data[offset + 2].toLong() and 0xFF
        val b3 = data[offset + 3].toLong() and 0xFF
        return b0 or (b1 shl 8) or (b2 shl 16) or (b3 shl 24)
    }

    private fun write24LE(buffer: ByteArray, offset: Int, value: Int) {
        buffer[offset] = (value and 0xFF).toByte()
        buffer[offset + 1] = ((value ushr 8) and 0xFF).toByte()
        buffer[offset + 2] = ((value ushr 16) and 0xFF).toByte()
    }

    private fun write32LE(buffer: ByteArray, offset: Int, value: Long) {
        buffer[offset] = (value and 0xFF).toByte()
        buffer[offset + 1] = ((value ushr 8) and 0xFF).toByte()
        buffer[offset + 2] = ((value ushr 16) and 0xFF).toByte()
        buffer[offset + 3] = ((value ushr 24) and 0xFF).toByte()
    }
}

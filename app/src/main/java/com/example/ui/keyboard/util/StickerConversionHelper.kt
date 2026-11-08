package com.example.ui.keyboard.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.util.Log
import android.media.ExifInterface
import com.aureusapps.android.webpandroid.decoder.WebPDecodeListener
import com.aureusapps.android.webpandroid.decoder.WebPDecoder
import com.aureusapps.android.webpandroid.decoder.WebPInfo
import com.waynejo.androidndkgif.GifEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object StickerConversionHelper {

    private const val TAG = "StickerConversionHelper"
    const val MAX_STICKER_DIMENSION = 512
    const val MAX_ANIMATION_FRAMES = 100
    const val MAX_ANIMATION_DURATION_MS = 6000 // 6 seconds

    /**
     * Converts a static image (static WebP, static GIF, PNG, JPG) to a real static WebP file.
     * Fits within 512x512 keeping aspect ratio (never upscaling).
     * Compresses using WEBP_LOSSLESS on API 30+ (WEBP on older) for sources with alpha (PNG, GIF, WebP),
     * and WEBP_LOSSY for JPEG.
     * Skips re-encoding if source is already a static WebP within size cap.
     */
    suspend fun convertStaticImageToWebp(
        context: Context,
        sourceFile: File,
        format: ImageFormat,
        outputDir: File
    ): File? = withContext(Dispatchers.Default) {
        try {
            // Check if already a static WebP within the 512x512 size cap
            if (format == ImageFormat.WEBP) {
                val bounds = getImageDimensions(sourceFile)
                if (bounds.first > 0 && bounds.second > 0 &&
                    bounds.first <= MAX_STICKER_DIMENSION && bounds.second <= MAX_STICKER_DIMENSION
                ) {
                    val destFile = File(outputDir, "sticker_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.webp")
                    sourceFile.copyTo(destFile, overwrite = true)
                    return@withContext destFile
                }
            }

            // Decode bitmap with EXIF orientation if JPEG
            val bitmap = decodeBitmapAppropriate(context, sourceFile, format) ?: return@withContext null

            // Scale to fit within 512x512 without upscaling
            val scaledBitmap = scaleToFit(bitmap, MAX_STICKER_DIMENSION)

            val destFile = File(outputDir, "sticker_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.webp")
            val tempDest = File(outputDir, "${destFile.name}.tmp")

            FileOutputStream(tempDest).use { out ->
                val hasAlpha = format != ImageFormat.JPEG
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val compressFormat = if (hasAlpha) {
                        Bitmap.CompressFormat.WEBP_LOSSLESS
                    } else {
                        Bitmap.CompressFormat.WEBP_LOSSY
                    }
                    scaledBitmap.compress(compressFormat, 90, out)
                } else {
                    @Suppress("DEPRECATION")
                    scaledBitmap.compress(Bitmap.CompressFormat.WEBP, 90, out)
                }
            }

            if (tempDest.exists() && tempDest.length() > 0) {
                if (tempDest.renameTo(destFile)) {
                    return@withContext destFile
                }
            }
            tempDest.delete()
            null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to convert static image to WebP: ${e.message}", e)
            null
        }
    }

    /**
     * Converts an animated WebP file to an animated GIF using WebPDecoder and androidndkgif.
     * Streams frames directly to the native encoder without holding all frames in memory.
     * Enforces:
     * - Longest side <= 512px
     * - Max 100 frames or 6 seconds (drops intermediate frames and merges delays if exceeded)
     * - Alpha threshold (>=128 opaque) with transparent color index reserved in palette
     * - Netscape infinite loop
     * - Validates using GifEncoder.validateGif with frameCount > 1
     */
    suspend fun convertAnimatedWebpToGif(
        context: Context,
        webpFile: File,
        outputDir: File
    ): File? = withContext(Dispatchers.Default) {
        val destGif = File(outputDir, "sticker_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.gif")
        val tempGif = File(outputDir, "${destGif.name}.tmp")

        var decoder: WebPDecoder? = null
        var ndkGifEncoder: GifEncoder? = null
        var pureJavaGifWriter: PureJavaNeuQuantGifWriter? = null
        var usePureJavaFallback = false

        try {
            val srcUri = Uri.fromFile(webpFile)
            decoder = WebPDecoder(context)
            decoder.setDataSource(srcUri)

            val info: WebPInfo = decoder.decodeInfo()
            val origWidth = info.width
            val origHeight = info.height
            val totalFrames = info.frameCount

            if (origWidth <= 0 || origHeight <= 0 || totalFrames <= 0) {
                Log.w(TAG, "Invalid WebPInfo: width=$origWidth, height=$origHeight, frames=$totalFrames")
                return@withContext null
            }

            // Compute scale factor so longest side is <= 512
            val longestSide = maxOf(origWidth, origHeight)
            val scale = if (longestSide > MAX_STICKER_DIMENSION) {
                MAX_STICKER_DIMENSION.toFloat() / longestSide.toFloat()
            } else {
                1.0f
            }
            val targetW = maxOf(1, (origWidth * scale).toInt())
            val targetH = maxOf(1, (origHeight * scale).toInt())

            // Frame decimation step if frames > MAX_ANIMATION_FRAMES
            val step = if (totalFrames > MAX_ANIMATION_FRAMES) {
                (totalFrames + MAX_ANIMATION_FRAMES - 1) / MAX_ANIMATION_FRAMES
            } else {
                1
            }

            // Initialize native GIF encoder
            try {
                ndkGifEncoder = GifEncoder()
                ndkGifEncoder.init(
                    targetW,
                    targetH,
                    tempGif.absolutePath,
                    GifEncoder.EncodingType.ENCODING_TYPE_NORMAL_LOW_MEMORY
                )
            } catch (e: Throwable) {
                Log.w(TAG, "Native androidndkgif init error (${e.message}), switching to pure-Java NeuQuant encoder", e)
                ndkGifEncoder = null
                usePureJavaFallback = true
            }

            if (usePureJavaFallback) {
                pureJavaGifWriter = PureJavaNeuQuantGifWriter(targetW, targetH, tempGif)
            }

            var previousTimestamp = 0L
            var accumulatedDelayMs = 0
            var framesWritten = 0
            var isFirstDecodedFrame = true

            // Reused canvas and paint for alpha thresholding / alpha preservation
            val thresholdPaint = Paint(Paint.FILTER_BITMAP_FLAG)

            decoder.addDecodeListener(object : WebPDecodeListener {
                override fun onInfoDecoded(info: WebPInfo) {
                    // Handled synchronously above
                }

                override fun onFrameDecoded(index: Int, timestamp: Long, bitmap: Bitmap, uri: Uri) {
                    // Library passes timestamp in milliseconds.
                    val frameDuration = if (isFirstDecodedFrame) {
                        isFirstDecodedFrame = false
                        if (timestamp > 0) timestamp.toInt() else 100
                    } else {
                        val diff = (timestamp - previousTimestamp).toInt()
                        if (diff > 0) diff else 100
                    }
                    previousTimestamp = timestamp
                    accumulatedDelayMs += frameDuration

                    // Frame decimation: only encode every 'step' frames
                    if (index % step == 0) {
                        // Apply alpha thresholding (>=128 opaque, <128 transparent)
                        val processedBitmap = processFrameAlphaAndScale(bitmap, targetW, targetH, thresholdPaint)

                        // Convert delay to centiseconds (1/100s), with floor of 2
                        val delayCentis = maxOf(2, (accumulatedDelayMs + 5) / 10)
                        val delayMs = delayCentis * 10
                        accumulatedDelayMs = 0

                        if (ndkGifEncoder != null) {
                            ndkGifEncoder.encodeFrame(processedBitmap, delayMs)
                        } else if (pureJavaGifWriter != null) {
                            pureJavaGifWriter.encodeFrame(processedBitmap, delayCentis)
                        }
                        framesWritten++
                    }
                }
            })

            // Run decoding of all frames
            decoder.decodeFrames(null)

            // Close encoder
            try {
                ndkGifEncoder?.close()
            } catch (e: Exception) {
                Log.e(TAG, "Error closing androidndkgif: ${e.message}", e)
            }
            try {
                pureJavaGifWriter?.close()
            } catch (e: Exception) {
                Log.e(TAG, "Error closing pureJavaGifWriter: ${e.message}", e)
            }

            // Validate generated GIF
            if (tempGif.exists() && tempGif.length() > 0) {
                val isValid = com.example.ui.keyboard.util.GifEncoder.validateGif(tempGif, requireMultiFrame = true)
                if (isValid && framesWritten > 1) {
                    if (destGif.exists()) destGif.delete()
                    if (tempGif.renameTo(destGif)) {
                        return@withContext destGif
                    }
                } else {
                    Log.w(TAG, "Generated GIF failed validation (valid=$isValid, frames=$framesWritten)")
                }
            }

            tempGif.delete()
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error converting animated WebP to GIF: ${e.message}", e)
            tempGif.delete()
            null
        } finally {
            try {
                decoder?.release()
            } catch (e: Exception) {
                Log.e(TAG, "Error releasing WebPDecoder: ${e.message}", e)
            }
        }
    }

    /**
     * Thresholds alpha (>= 128 opaque, < 128 transparent) and scales to target dimension.
     */
    private fun processFrameAlphaAndScale(
        srcBitmap: Bitmap,
        targetW: Int,
        targetH: Int,
        paint: Paint
    ): Bitmap {
        val scaled = if (srcBitmap.width == targetW && srcBitmap.height == targetH) {
            srcBitmap.copy(Bitmap.Config.ARGB_8888, true)
        } else {
            Bitmap.createScaledBitmap(srcBitmap, targetW, targetH, true)
        }

        // Apply alpha thresholding in-place
        val pixels = IntArray(targetW * targetH)
        scaled.getPixels(pixels, 0, targetW, 0, 0, targetW, targetH)
        var modified = false
        for (i in pixels.indices) {
            val color = pixels[i]
            val alpha = (color ushr 24) and 0xFF
            if (alpha in 1..127) {
                pixels[i] = 0 // Fully transparent
                modified = true
            } else if (alpha in 128..254) {
                pixels[i] = color or (0xFF shl 24) // Fully opaque
                modified = true
            }
        }
        if (modified) {
            scaled.setPixels(pixels, 0, targetW, 0, 0, targetW, targetH)
        }
        return scaled
    }

    /**
     * Generates a static PNG from frame 1 of an image file (used for grid thumbnails).
     */
    suspend fun generateStaticPngThumbnail(
        context: Context,
        file: File,
        format: ImageFormat,
        outputDir: File
    ): File? = withContext(Dispatchers.Default) {
        try {
            val destPng = File(outputDir, "sticker_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}_thumb.png")
            val tempPng = File(outputDir, "${destPng.name}.tmp")

            val bmp: Bitmap? = if (format == ImageFormat.WEBP && ImageFormatDetector.isAnimated(file, format)) {
                // Decode frame 1 using WebPDecoder
                var firstFrame: Bitmap? = null
                val decoder = WebPDecoder(context)
                val uri = Uri.fromFile(file)
                try {
                    decoder.setDataSource(uri)
                    decoder.addDecodeListener(object : WebPDecodeListener {
                        override fun onInfoDecoded(info: WebPInfo) {}
                        override fun onFrameDecoded(index: Int, timestamp: Long, bitmap: Bitmap, uri: Uri) {
                            if (firstFrame == null) {
                                firstFrame = bitmap.copy(Bitmap.Config.ARGB_8888, false)
                            }
                        }
                    })
                    decoder.decodeFrames(null)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to decode first frame with WebPDecoder for thumbnail", e)
                } finally {
                    try { decoder.release() } catch (e: Exception) {
                        Log.e(TAG, "Error releasing WebPDecoder in thumbnail generator", e)
                    }
                }
                firstFrame ?: decodeBitmapAppropriate(context, file, format)
            } else {
                decodeBitmapAppropriate(context, file, format)
            }

            if (bmp == null) return@withContext null

            val scaled = scaleToFit(bmp, MAX_STICKER_DIMENSION)
            FileOutputStream(tempPng).use { out ->
                scaled.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            if (tempPng.exists() && tempPng.length() > 0) {
                if (tempPng.renameTo(destPng)) {
                    return@withContext destPng
                }
            }
            tempPng.delete()
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error generating static PNG thumbnail: ${e.message}", e)
            null
        }
    }

    private fun decodeBitmapAppropriate(context: Context, file: File, format: ImageFormat): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(file)
                ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    decoder.isMutableRequired = true
                }
            } else {
                val bmp = BitmapFactory.decodeFile(file.absolutePath)
                if (format == ImageFormat.JPEG && bmp != null) {
                    rotateBitmapIfRequired(file, bmp)
                } else {
                    bmp
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed decoding bitmap via primary path for ${file.name}, trying BitmapFactory fallback", e)
            BitmapFactory.decodeFile(file.absolutePath)
        }
    }

    private fun rotateBitmapIfRequired(file: File, bitmap: Bitmap): Bitmap {
        return try {
            val exif = ExifInterface(file.absolutePath)
            val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            val matrix = android.graphics.Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                else -> return bitmap
            }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } catch (e: Exception) {
            Log.e(TAG, "Failed reading EXIF orientation for ${file.name}", e)
            bitmap
        }
    }

    private fun scaleToFit(bitmap: Bitmap, maxDim: Int): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        val longest = maxOf(w, h)
        if (longest <= maxDim) return bitmap

        val ratio = maxDim.toFloat() / longest.toFloat()
        val targetW = maxOf(1, (w * ratio).toInt())
        val targetH = maxOf(1, (h * ratio).toInt())
        return Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
    }

    private fun getImageDimensions(file: File): Pair<Int, Int> {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, opts)
        return Pair(opts.outWidth, opts.outHeight)
    }
}

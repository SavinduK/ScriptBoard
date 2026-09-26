package com.example.ui.stickers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.Segmentation
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.InputStream
import kotlin.coroutines.resume

object StickerSegmentationHelper {

    suspend fun loadAndDownsampleBitmap(context: Context, uri: Uri, maxDimension: Int = 1024): Bitmap? = withContext(Dispatchers.IO) {
        try {
            var stream: InputStream? = context.contentResolver.openInputStream(uri)
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(stream, null, boundsOptions)
            stream?.close()

            val srcWidth = boundsOptions.outWidth
            val srcHeight = boundsOptions.outHeight
            if (srcWidth <= 0 || srcHeight <= 0) return@withContext null

            var inSampleSize = 1
            while (srcWidth / (inSampleSize * 2) >= maxDimension || srcHeight / (inSampleSize * 2) >= maxDimension) {
                inSampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            stream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(stream, null, decodeOptions)
            stream?.close()
            bitmap
        } catch (_: Exception) {
            null
        }
    }

    suspend fun removeBackground(bitmap: Bitmap): Bitmap = withContext(Dispatchers.Default) {
        // Attempt 1: ML Kit Subject Segmentation
        try {
            val subjectResult = runSubjectSegmentation(bitmap)
            if (subjectResult != null) {
                return@withContext subjectResult
            }
        } catch (_: Exception) {}

        // Attempt 2: ML Kit Selfie Segmentation fallback
        try {
            val selfieResult = runSelfieSegmentation(bitmap)
            if (selfieResult != null) {
                return@withContext selfieResult
            }
        } catch (_: Exception) {}

        // Fallback: Rounded oval center crop with transparent background
        createCenterSubjectFallback(bitmap)
    }

    private suspend fun runSubjectSegmentation(bitmap: Bitmap): Bitmap? = suspendCancellableCoroutine { cont ->
        try {
            val options = SubjectSegmenterOptions.Builder()
                .enableForegroundBitmap()
                .build()
            val segmenter = SubjectSegmentation.getClient(options)
            val inputImage = InputImage.fromBitmap(bitmap, 0)

            segmenter.process(inputImage)
                .addOnSuccessListener { result ->
                    val fg = result.foregroundBitmap
                    if (cont.isActive) {
                        cont.resume(fg)
                    }
                }
                .addOnFailureListener {
                    if (cont.isActive) {
                        cont.resume(null)
                    }
                }
        } catch (_: Exception) {
            if (cont.isActive) {
                cont.resume(null)
            }
        }
    }

    private suspend fun runSelfieSegmentation(bitmap: Bitmap): Bitmap? = suspendCancellableCoroutine { cont ->
        try {
            val options = SelfieSegmenterOptions.Builder()
                .setDetectorMode(SelfieSegmenterOptions.SINGLE_IMAGE_MODE)
                .build()
            val segmenter = Segmentation.getClient(options)
            val inputImage = InputImage.fromBitmap(bitmap, 0)

            segmenter.process(inputImage)
                .addOnSuccessListener { mask ->
                    try {
                        val maskBuffer = mask.buffer
                        val maskWidth = mask.width
                        val maskHeight = mask.height

                        // Create output transparent bitmap matching source bitmap
                        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
                        val pixels = IntArray(bitmap.width * bitmap.height)
                        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

                        val xRatio = maskWidth.toFloat() / bitmap.width
                        val yRatio = maskHeight.toFloat() / bitmap.height

                        for (y in 0 until bitmap.height) {
                            val my = (y * yRatio).toInt().coerceIn(0, maskHeight - 1)
                            for (x in 0 until bitmap.width) {
                                val mx = (x * xRatio).toInt().coerceIn(0, maskWidth - 1)
                                val bufferIndex = my * maskWidth + mx
                                maskBuffer.position(bufferIndex * 4)
                                val confidence = maskBuffer.float

                                val pixelIndex = y * bitmap.width + x
                                if (confidence < 0.45f) {
                                    pixels[pixelIndex] = Color.TRANSPARENT
                                } else {
                                    val alphaScale = ((confidence - 0.45f) / 0.25f).coerceIn(0f, 1f)
                                    val original = pixels[pixelIndex]
                                    val originalAlpha = Color.alpha(original)
                                    val newAlpha = (originalAlpha * alphaScale).toInt()
                                    pixels[pixelIndex] = Color.argb(
                                        newAlpha,
                                        Color.red(original),
                                        Color.green(original),
                                        Color.blue(original)
                                    )
                                }
                            }
                        }
                        output.setPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
                        if (cont.isActive) {
                            cont.resume(output)
                        }
                    } catch (_: Exception) {
                        if (cont.isActive) cont.resume(null)
                    }
                }
                .addOnFailureListener {
                    if (cont.isActive) {
                        cont.resume(null)
                    }
                }
        } catch (_: Exception) {
            if (cont.isActive) {
                cont.resume(null)
            }
        }
    }

    private fun createCenterSubjectFallback(source: Bitmap): Bitmap {
        val width = source.width
        val height = source.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val rectF = android.graphics.RectF(
            width * 0.05f,
            height * 0.05f,
            width * 0.95f,
            height * 0.95f
        )
        val cornerRadius = minOf(width, height) * 0.25f
        canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, paint)

        paint.xfermode = android.graphics.PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(source, 0f, 0f, paint)
        return output
    }

    fun addStickerOutline(
        source: Bitmap,
        strokeWidthPx: Int = 14,
        strokeColor: Int = Color.WHITE
    ): Bitmap {
        val width = source.width
        val height = source.height
        val padding = strokeWidthPx + 6
        val output = Bitmap.createBitmap(width + padding * 2, height + padding * 2, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        val alphaBitmap = source.extractAlpha() ?: return source
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = PorterDuffColorFilter(strokeColor, PorterDuff.Mode.SRC_IN)
        }

        val radius = strokeWidthPx
        val step = 2
        for (dx in -radius..radius step step) {
            for (dy in -radius..radius step step) {
                if (dx * dx + dy * dy <= radius * radius) {
                    canvas.drawBitmap(alphaBitmap, (padding + dx).toFloat(), (padding + dy).toFloat(), fillPaint)
                }
            }
        }

        canvas.drawBitmap(source, padding.toFloat(), padding.toFloat(), null)
        return output
    }

    fun scaleTo512Sticker(source: Bitmap): Bitmap {
        val targetSize = 512
        val output = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        val maxDim = maxOf(source.width, source.height)
        val scale = (targetSize - 32).toFloat() / maxDim
        val scaledW = (source.width * scale).toInt().coerceAtLeast(1)
        val scaledH = (source.height * scale).toInt().coerceAtLeast(1)

        val scaled = Bitmap.createScaledBitmap(source, scaledW, scaledH, true)
        val left = (targetSize - scaledW) / 2f
        val top = (targetSize - scaledH) / 2f

        canvas.drawBitmap(scaled, left, top, null)
        return output
    }
}

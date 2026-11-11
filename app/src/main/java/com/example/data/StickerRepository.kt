package com.example.data

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.example.ui.keyboard.util.GifEncoder
import com.example.ui.keyboard.util.ImageFormat
import com.example.ui.keyboard.util.ImageFormatDetector
import com.example.ui.keyboard.util.StickerConversionHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

class StickerRepository(private val stickerDao: StickerDao) {

    companion object {
        private const val TAG = "StickerRepository"
    }

    val allStickers: Flow<List<StickerEntity>> = stickerDao.getAllStickers()

    fun getStickersDirectory(context: Context): File {
        val dir = File(context.filesDir, "stickers")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    suspend fun recordStickerUsed(stickerId: Long) = withContext(Dispatchers.IO) {
        stickerDao.incrementUsage(stickerId)
    }

    suspend fun ensureSampleStickersSeeded(context: Context) = withContext(Dispatchers.IO) {
        // User requested removing the 5 example stickers: clean up any existing sample stickers
        removeSampleStickers(context)
        ensureCompanionFilesGenerated(context)
    }

    /**
     * Deletes sample stickers from the database and removes their files from disk.
     */
    suspend fun removeSampleStickers(context: Context) = withContext(Dispatchers.IO) {
        try {
            val samples = stickerDao.getSampleStickers()
            for (sample in samples) {
                try {
                    File(sample.filePath).takeIf { it.exists() }?.delete()
                    sample.gifPath?.let { File(it).takeIf { f -> f.exists() }?.delete() }
                    sample.staticPngPath?.let { File(it).takeIf { f -> f.exists() }?.delete() }
                } catch (e: Exception) {
                    Log.e(TAG, "Error deleting sample sticker file ${sample.filePath}: ${e.message}", e)
                }
            }
            stickerDao.deleteSampleStickers()
        } catch (e: Exception) {
            Log.e(TAG, "Error removing sample stickers: ${e.message}", e)
        }
    }

    /**
     * Computes the SHA-256 hash of a file for content-based duplicate detection.
     */
    fun computeFileSha256(file: File): String? {
        if (!file.exists() || file.length() == 0L) return null
        return try {
            val digest = java.security.MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to compute sha256 for file ${file.name}: ${e.message}", e)
            null
        }
    }

    /**
     * Builds a set of SHA-256 hashes of all existing stickers currently stored on the device.
     */
    suspend fun getExistingStickerHashes(): MutableSet<String> = withContext(Dispatchers.IO) {
        val hashes = mutableSetOf<String>()
        try {
            val allStickers = stickerDao.getAllStickersList()
            for (st in allStickers) {
                val f = File(st.filePath)
                if (f.exists()) {
                    computeFileSha256(f)?.let { hashes.add(it) }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating existing sticker hashes: ${e.message}", e)
        }
        hashes
    }

    /**
     * Regenerates missing companion GIFs and static PNGs for animated stickers using the new pipeline.
     */
    suspend fun ensureCompanionFilesGenerated(context: Context) = withContext(Dispatchers.IO) {
        try {
            val missing = stickerDao.getAnimatedStickersMissingCompanions()
            val stickersDir = getStickersDirectory(context)
            for (sticker in missing) {
                val file = File(sticker.filePath)
                if (!file.exists() || file.length() == 0L) continue

                val format = ImageFormatDetector.detectFormat(file) ?: ImageFormat.WEBP
                var gifFile = sticker.gifPath?.let { File(it) }?.takeIf { it.exists() && it.length() > 0 }
                if (gifFile == null || !GifEncoder.validateGif(gifFile, requireMultiFrame = true)) {
                    if (format == ImageFormat.WEBP) {
                        gifFile = StickerConversionHelper.convertAnimatedWebpToGif(context, file, stickersDir)
                    } else if (format == ImageFormat.GIF) {
                        gifFile = file
                    }
                }

                var pngFile = sticker.staticPngPath?.let { File(it) }?.takeIf { it.exists() && it.length() > 0 }
                if (pngFile == null || !pngFile.exists() || pngFile.length() == 0L) {
                    pngFile = StickerConversionHelper.generateStaticPngThumbnail(context, file, format, stickersDir)
                }

                if (gifFile != null || pngFile != null) {
                    stickerDao.update(
                        sticker.copy(
                            gifPath = gifFile?.absolutePath ?: sticker.gifPath,
                            staticPngPath = pngFile?.absolutePath ?: sticker.staticPngPath,
                            mimeType = format.mime
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in ensureCompanionFilesGenerated: ${e.message}", e)
        }
    }

    /**
     * Imports a list of stickers from URIs (e.g. SAF file picker).
     * Automatically skips duplicates based on SHA-256 hash comparison.
     */
    suspend fun importStickersFromUris(
        context: Context,
        uris: List<Uri>,
        source: String = "imported"
    ): ImportResult = withContext(Dispatchers.IO) {
        val importedEntities = mutableListOf<StickerEntity>()
        val failedList = mutableListOf<FailedImport>()
        val knownHashes = getExistingStickerHashes()

        for ((index, uri) in uris.withIndex()) {
            val displayName = uri.lastPathSegment?.substringAfterLast('/')?.take(25) ?: "Sticker ${index + 1}"
            try {
                val entity = importOneInternal(
                    context = context,
                    sourceStreamProvider = { context.contentResolver.openInputStream(uri) },
                    displayName = displayName,
                    source = source,
                    knownHashes = knownHashes
                )
                if (entity != null) {
                    importedEntities.add(entity)
                } else {
                    failedList.add(FailedImport(displayName, "Duplicate or unsupported format"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to import sticker from URI $uri: ${e.message}", e)
                failedList.add(FailedImport(displayName, e.localizedMessage ?: "Unknown error"))
            }
        }

        if (importedEntities.isNotEmpty()) {
            stickerDao.insertAll(importedEntities)
        }

        ImportResult(imported = importedEntities.size, failed = failedList)
    }

    /**
     * Imports a list of stickers from Files (e.g. WhatsApp sticker folder).
     * Automatically skips duplicates based on SHA-256 hash comparison.
     */
    suspend fun importStickersFromFiles(
        context: Context,
        files: List<File>,
        source: String = "whatsapp"
    ): ImportResult = withContext(Dispatchers.IO) {
        val importedEntities = mutableListOf<StickerEntity>()
        val failedList = mutableListOf<FailedImport>()
        val knownHashes = getExistingStickerHashes()

        for (file in files) {
            val displayName = file.nameWithoutExtension.take(25).ifBlank { "WA Sticker" }
            try {
                if (!file.exists() || file.length() == 0L) {
                    failedList.add(FailedImport(displayName, "File is missing or empty"))
                    continue
                }
                val entity = importOneInternal(
                    context = context,
                    sourceStreamProvider = { file.inputStream() },
                    displayName = displayName,
                    source = source,
                    knownHashes = knownHashes
                )
                if (entity != null) {
                    importedEntities.add(entity)
                } else {
                    failedList.add(FailedImport(displayName, "Duplicate or unsupported format"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to import sticker from file ${file.name}: ${e.message}", e)
                failedList.add(FailedImport(displayName, e.localizedMessage ?: "Conversion error"))
            }
        }

        if (importedEntities.isNotEmpty()) {
            stickerDao.insertAll(importedEntities)
        }

        ImportResult(imported = importedEntities.size, failed = failedList)
    }

    /**
     * Shared internal pipeline for importing a single sticker:
     * 1. Copy input to temp file in stickers directory.
     * 2. Read magic bytes to detect ImageFormat (WebP, PNG, GIF, JPEG). Reject unsupported.
     * 3. Check if animated (WebP VP8X bit 0x02, GIF frame count > 1; PNG/JPEG are static).
     * 4. Branch:
     *    - Static image: convert to real static WebP (scaled <= 512x512). Produce static PNG thumbnail.
     *    - Animated WebP: keep original .webp, convert to animated GIF via WebPDecoder & androidndkgif. Produce static PNG thumbnail.
     *    - Animated GIF: keep original .gif, set gifPath to same file (or re-encode if > size cap). Produce static PNG thumbnail.
     * 5. Validate outputs, rename into final file names, then return constructed StickerEntity.
     * 6. Clean up temporary files on error (no orphan files).
     */
    private suspend fun importOneInternal(
        context: Context,
        sourceStreamProvider: () -> InputStream?,
        displayName: String,
        source: String,
        preGeneratedGif: File? = null,
        knownHashes: MutableSet<String>? = null
    ): StickerEntity? {
        val stickersDir = getStickersDirectory(context)
        val tempId = "${System.nanoTime()}_${UUID.randomUUID().toString().take(6)}"
        val tempRawFile = File(stickersDir, "import_raw_$tempId.tmp")

        // Track files created during this import so we clean up on failure
        val createdFiles = mutableListOf<File>()

        try {
            // Step 1: Copy input to temp file
            val inStream = sourceStreamProvider()
            if (inStream == null) {
                Log.e(TAG, "Could not open stream for $displayName")
                return null
            }
            inStream.use { input ->
                FileOutputStream(tempRawFile).use { output ->
                    input.copyTo(output)
                }
            }
            createdFiles.add(tempRawFile)

            if (!tempRawFile.exists() || tempRawFile.length() < 12) {
                Log.e(TAG, "Input file is empty or too small for $displayName")
                return null
            }

            // Check for duplicates using SHA-256
            val fileHash = computeFileSha256(tempRawFile)
            if (fileHash != null) {
                if (knownHashes != null && knownHashes.contains(fileHash)) {
                    Log.i(TAG, "Skipping duplicate sticker $displayName (hash $fileHash already exists)")
                    return null
                }
            }

            // Step 2: Read magic bytes and detect format
            val format = ImageFormatDetector.detectFormat(tempRawFile)
            if (format == null) {
                Log.e(TAG, "Unsupported format (magic bytes check failed) for $displayName")
                return null
            }

            val isAnimated = ImageFormatDetector.isAnimated(tempRawFile, format)

            var finalMainFile: File? = null
            var finalGifFile: File? = null
            var finalPngFile: File? = null
            val finalMimeType: String

            if (!isAnimated) {
                // Step 2 (Static): Re-encode to real static WebP
                finalMimeType = ImageFormat.WEBP.mime
                val convertedWebp = StickerConversionHelper.convertStaticImageToWebp(context, tempRawFile, format, stickersDir)
                if (convertedWebp == null || !convertedWebp.exists() || convertedWebp.length() == 0L) {
                    Log.e(TAG, "Static WebP conversion failed for $displayName")
                    return null
                }
                createdFiles.add(convertedWebp)
                finalMainFile = convertedWebp

                // Static PNG thumbnail
                finalPngFile = StickerConversionHelper.generateStaticPngThumbnail(context, convertedWebp, ImageFormat.WEBP, stickersDir)
                if (finalPngFile != null) createdFiles.add(finalPngFile)
            } else {
                // Step 3 (Animated): Keep original + make/keep GIF copy
                finalMimeType = format.mime

                when (format) {
                    ImageFormat.WEBP -> {
                        // Move original to permanent .webp name
                        val permWebpFile = File(stickersDir, "sticker_${System.currentTimeMillis()}_$tempId.webp")
                        if (!tempRawFile.renameTo(permWebpFile)) {
                            tempRawFile.copyTo(permWebpFile, overwrite = true)
                            tempRawFile.delete()
                        }
                        createdFiles.add(permWebpFile)
                        finalMainFile = permWebpFile

                        // Step 4: Animated WebP -> GIF pipeline
                        val companionGif = preGeneratedGif?.takeIf { it.exists() && it.length() > 0 }
                            ?: StickerConversionHelper.convertAnimatedWebpToGif(context, permWebpFile, stickersDir)

                        if (companionGif != null && companionGif.exists() && companionGif.length() > 0) {
                            createdFiles.add(companionGif)
                            finalGifFile = companionGif
                        }

                        // Static PNG thumbnail
                        finalPngFile = StickerConversionHelper.generateStaticPngThumbnail(context, permWebpFile, ImageFormat.WEBP, stickersDir)
                        if (finalPngFile != null) createdFiles.add(finalPngFile)
                    }

                    ImageFormat.GIF -> {
                        // Move original to permanent .gif name
                        val permGifFile = File(stickersDir, "sticker_${System.currentTimeMillis()}_$tempId.gif")
                        if (!tempRawFile.renameTo(permGifFile)) {
                            tempRawFile.copyTo(permGifFile, overwrite = true)
                            tempRawFile.delete()
                        }
                        createdFiles.add(permGifFile)
                        finalMainFile = permGifFile
                        finalGifFile = permGifFile // Same file for animated GIF

                        // Static PNG thumbnail
                        finalPngFile = StickerConversionHelper.generateStaticPngThumbnail(context, permGifFile, ImageFormat.GIF, stickersDir)
                        if (finalPngFile != null) createdFiles.add(finalPngFile)
                    }

                    else -> {
                        // Static format flagged animated (APNG): treated as static first frame
                        finalMainFile = StickerConversionHelper.convertStaticImageToWebp(context, tempRawFile, format, stickersDir)
                        if (finalMainFile != null) createdFiles.add(finalMainFile)
                    }
                }
            }

            if (finalMainFile == null || !finalMainFile.exists() || finalMainFile.length() == 0L) {
                Log.e(TAG, "Final sticker file missing or empty for $displayName")
                return null
            }

            // Cleanup raw temp file if not already deleted
            if (tempRawFile.exists()) {
                tempRawFile.delete()
            }

            // Record newly imported hash so same-batch duplicates are also rejected
            if (fileHash != null && knownHashes != null) {
                knownHashes.add(fileHash)
            }

            // Also record hash of final main file
            computeFileSha256(finalMainFile)?.let { knownHashes?.add(it) }

            return StickerEntity(
                filePath = finalMainFile.absolutePath,
                name = displayName,
                source = source,
                isAnimated = isAnimated,
                dateAdded = System.currentTimeMillis(),
                gifPath = finalGifFile?.absolutePath,
                staticPngPath = finalPngFile?.absolutePath,
                mimeType = finalMimeType
            )
        } catch (e: Exception) {
            Log.e(TAG, "Unhandled exception importing sticker $displayName: ${e.message}", e)
            // Clean up any files created during failed attempt
            for (f in createdFiles) {
                try {
                    if (f.exists()) f.delete()
                } catch (delEx: Exception) {
                    Log.e(TAG, "Error cleaning up temporary file ${f.absolutePath}", delEx)
                }
            }
            return null
        }
    }

    suspend fun deleteSticker(sticker: StickerEntity) = withContext(Dispatchers.IO) {
        try {
            val file = File(sticker.filePath)
            if (file.exists()) file.delete()
            sticker.gifPath?.let { path ->
                val gifFile = File(path)
                if (gifFile.exists() && gifFile.absolutePath != sticker.filePath) {
                    gifFile.delete()
                }
            }
            sticker.staticPngPath?.let { path ->
                val pngFile = File(path)
                if (pngFile.exists()) pngFile.delete()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting sticker ${sticker.id}: ${e.message}", e)
        }
        stickerDao.delete(sticker)
    }

    suspend fun clearAllStickers(context: Context) = withContext(Dispatchers.IO) {
        try {
            val dir = getStickersDirectory(context)
            dir.listFiles()?.forEach { it.delete() }
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing stickers directory: ${e.message}", e)
        }
        stickerDao.clearAll()
    }

    /**
     * Obtains a secure content sharing URI for the sticker, respecting its actual MIME type.
     */
    fun getStickerShareUri(context: Context, sticker: StickerEntity): Uri? {
        return try {
            val file = File(sticker.filePath)
            if (file.exists() && file.length() > 0) {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting share URI for sticker ${sticker.id}: ${e.message}", e)
            null
        }
    }
}

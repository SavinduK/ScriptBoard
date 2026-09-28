package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import com.example.ui.keyboard.util.WebpAnimationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class StickerRepository(private val stickerDao: StickerDao) {

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
        if (stickerDao.getStickerCount() > 0) return@withContext
        seedSampleStickers(context)
    }

    suspend fun seedSampleStickers(context: Context): Int = withContext(Dispatchers.IO) {
        val stickersDir = getStickersDirectory(context)
        val assetManager = context.assets
        val sampleList = try {
            assetManager.list("sample_stickers")?.toList() ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }

        val entities = mutableListOf<StickerEntity>()
        for ((index, fileName) in sampleList.withIndex()) {
            try {
                val destFile = File(stickersDir, fileName)
                if (!destFile.exists() || destFile.length() == 0L) {
                    assetManager.open("sample_stickers/$fileName").use { input ->
                        FileOutputStream(destFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                }
                if (destFile.exists() && destFile.length() > 0) {
                    val isAnim = WebpAnimationHelper.isAnimated(destFile)
                    val readableName = fileName.removeSuffix(".webp").replace('_', ' ')
                        .split(" ").joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }
                    entities.add(
                        StickerEntity(
                            filePath = destFile.absolutePath,
                            name = readableName,
                            source = "sample",
                            isAnimated = isAnim,
                            dateAdded = System.currentTimeMillis() + index
                        )
                    )
                }
            } catch (_: Exception) {}
        }

        if (entities.isNotEmpty()) {
            stickerDao.insertAll(entities)
        }
        entities.size
    }

    suspend fun importStickersFromUris(
        context: Context,
        uris: List<Uri>,
        source: String = "imported"
    ): Int = withContext(Dispatchers.IO) {
        val stickersDir = getStickersDirectory(context)
        var count = 0
        val entities = mutableListOf<StickerEntity>()

        for ((index, uri) in uris.withIndex()) {
            try {
                val fileName = "sticker_import_${System.currentTimeMillis()}_$index.webp"
                val destFile = File(stickersDir, fileName)

                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }

                if (destFile.exists() && destFile.length() > 0) {
                    val isAnim = WebpAnimationHelper.isAnimated(destFile)
                    entities.add(
                        StickerEntity(
                            filePath = destFile.absolutePath,
                            name = "Sticker ${System.currentTimeMillis() % 10000}",
                            source = source,
                            isAnimated = isAnim,
                            dateAdded = System.currentTimeMillis() + index
                        )
                    )
                    count++
                }
            } catch (_: Exception) {}
        }

        if (entities.isNotEmpty()) {
            stickerDao.insertAll(entities)
        }
        count
    }

    suspend fun importStickersFromFiles(
        context: Context,
        files: List<File>,
        source: String = "whatsapp"
    ): Int = withContext(Dispatchers.IO) {
        val stickersDir = getStickersDirectory(context)
        var count = 0
        val entities = mutableListOf<StickerEntity>()

        for ((index, file) in files.withIndex()) {
            try {
                val fileName = "sticker_wa_${System.currentTimeMillis()}_$index.webp"
                val destFile = File(stickersDir, fileName)

                file.inputStream().use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }

                if (destFile.exists() && destFile.length() > 0) {
                    val isAnim = WebpAnimationHelper.isAnimated(destFile)
                    entities.add(
                        StickerEntity(
                            filePath = destFile.absolutePath,
                            name = file.nameWithoutExtension.take(20).ifBlank { "WA Sticker" },
                            source = source,
                            isAnimated = isAnim,
                            dateAdded = System.currentTimeMillis() + index
                        )
                    )
                    count++
                }
            } catch (_: Exception) {}
        }

        if (entities.isNotEmpty()) {
            stickerDao.insertAll(entities)
        }
        count
    }

    suspend fun deleteSticker(sticker: StickerEntity) = withContext(Dispatchers.IO) {
        try {
            val file = File(sticker.filePath)
            if (file.exists()) {
                file.delete()
            }
        } catch (_: Exception) {}
        stickerDao.delete(sticker)
    }

    suspend fun clearAllStickers(context: Context) = withContext(Dispatchers.IO) {
        try {
            val dir = getStickersDirectory(context)
            dir.listFiles()?.forEach { it.delete() }
        } catch (_: Exception) {}
        stickerDao.clearAll()
    }

    fun getStickerShareUri(context: Context, sticker: StickerEntity): Uri? {
        return try {
            val file = File(sticker.filePath)
            if (file.exists()) {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }
}

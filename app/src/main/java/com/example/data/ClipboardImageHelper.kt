package com.example.data

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.InputStream
import java.util.UUID

object ClipboardImageHelper {

    /**
     * Copies any external Uri (content://, file://, etc.) or file path into internal
     * app storage so that external content provider permissions never expire and images
     * are never rendered blank in clipboard history or across keyboard sessions.
     *
     * Returns the persistent absolute file path, or the original string on fallback.
     */
    fun persistImageLocally(context: Context, sourceUri: String?): String {
        if (sourceUri.isNullOrBlank()) return ""
        try {
            val fileCheck = File(sourceUri)
            if (fileCheck.exists() && fileCheck.isFile && fileCheck.length() > 0) {
                return fileCheck.absolutePath
            }

            val imagesDir = File(context.filesDir, "clipboard_images").apply { mkdirs() }
            val targetFile = File(imagesDir, "clip_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.png")

            val stream: InputStream? = try {
                val parsed = Uri.parse(sourceUri)
                if (sourceUri.startsWith("content://") || sourceUri.startsWith("android.resource://")) {
                    context.contentResolver.openInputStream(parsed)
                } else if (sourceUri.startsWith("file://")) {
                    parsed.path?.let { File(it).inputStream() }
                } else if (sourceUri.startsWith("/")) {
                    File(sourceUri).inputStream()
                } else {
                    context.contentResolver.openInputStream(parsed)
                }
            } catch (_: Exception) {
                null
            }

            if (stream != null) {
                stream.use { input ->
                    targetFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                if (targetFile.exists() && targetFile.length() > 0) {
                    return targetFile.absolutePath
                }
            }
        } catch (_: Exception) {}
        return sourceUri
    }
}

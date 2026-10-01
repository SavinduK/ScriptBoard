package com.example.ui.keyboard.model

import android.content.Context
import com.example.ui.keyboard.util.KeyboardPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit

class EmojiPackDownloader(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val prefs by lazy {
        KeyboardPreferences.getInstance(context)
    }

    /**
     * Downloads an emoji/symbol package from the GitHub repository.
     * URL pattern: https://raw.githubusercontent.com/{owner}/{repo}/{branch}/emojis/{id}.json
     */
    suspend fun downloadEmojiPack(
        pack: EmojiPack,
        onProgress: (progress: Float, statusText: String) -> Unit
    ): Result<DownloadResult> = withContext(Dispatchers.IO) {
        val owner = prefs.githubRepoOwner.value.trim()
        val repo = prefs.githubRepoName.value.trim()
        val branch = prefs.githubRepoBranch.value.trim().ifEmpty { "main" }
        val filename = "${pack.id}.json"
        val githubRawUrl = "https://raw.githubusercontent.com/$owner/$repo/$branch/emojis/$filename"

        val emojisDir = File(context.filesDir, "emojis")
        if (!emojisDir.exists()) {
            emojisDir.mkdirs()
        }
        val targetFile = File(emojisDir, filename)

        try {
            onProgress(0.1f, "Connecting to GitHub repository...")

            val request = Request.Builder()
                .url(githubRawUrl)
                .header("User-Agent", "KeyPro-Android-Keyboard")
                .header("Accept", "application/json")
                .build()

            var downloadedFromRemote = false

            try {
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful && response.body != null) {
                        onProgress(0.4f, "Downloading ${pack.name} from GitHub...")
                        val body = response.body!!
                        val contentLength = body.contentLength()
                        val inputStream: InputStream = body.byteStream()
                        val outputStream = FileOutputStream(targetFile)

                        val buffer = ByteArray(4096)
                        var bytesRead: Int
                        var totalRead: Long = 0

                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                            outputStream.write(buffer, 0, bytesRead)
                            totalRead += bytesRead
                            if (contentLength > 0) {
                                val p = 0.4f + 0.4f * (totalRead.toFloat() / contentLength.toFloat()).coerceIn(0f, 1f)
                                onProgress(p, "Downloading... ${(p * 100).toInt()}%")
                            }
                        }
                        outputStream.flush()
                        outputStream.close()
                        inputStream.close()
                        downloadedFromRemote = true
                    }
                }
            } catch (_: Exception) {
                downloadedFromRemote = false
            }

            // Fallback to bundled asset if GitHub not reachable
            if (!downloadedFromRemote) {
                onProgress(0.5f, "Extracting repository emoji pack...")
                val assetPath = "emojis/$filename"
                try {
                    context.assets.open(assetPath).use { assetInput ->
                        FileOutputStream(targetFile).use { fileOutput ->
                            assetInput.copyTo(fileOutput)
                        }
                    }
                } catch (assetEx: Exception) {
                    return@withContext Result.failure(
                        Exception("Could not download $githubRawUrl and no bundled asset found: ${assetEx.message}")
                    )
                }
            }

            // Register installed pack
            prefs.installEmojiPack(pack.id)
            onProgress(1.0f, "Completed")

            val source = if (downloadedFromRemote) "GitHub ($owner/$repo)" else "Repository Bundle"

            Result.success(
                DownloadResult(
                    file = targetFile,
                    source = source,
                    githubUrl = githubRawUrl,
                    downloadedFromRemote = downloadedFromRemote,
                    wordsImported = 0,
                    fileSizeBytes = targetFile.length()
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Loads the symbols/items of an installed or bundled emoji pack
     */
    fun loadPackItems(packId: String): List<EmojiPackItem> {
        val targetFile = File(context.filesDir, "emojis/$packId.json")
        val jsonString = if (targetFile.exists() && targetFile.length() > 0) {
            targetFile.readText()
        } else {
            try {
                context.assets.open("emojis/$packId.json").bufferedReader().use { it.readText() }
            } catch (_: Exception) {
                null
            }
        } ?: return emptyList()

        return try {
            val json = JSONObject(jsonString)
            val itemsArray = json.getJSONArray("items")
            val result = mutableListOf<EmojiPackItem>()
            for (i in 0 until itemsArray.length()) {
                val obj = itemsArray.getJSONObject(i)
                result.add(
                    EmojiPackItem(
                        symbol = obj.getString("symbol"),
                        name = obj.optString("name", ""),
                        meaning = obj.optString("meaning", "")
                    )
                )
            }
            result
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun isPackDownloaded(packId: String): Boolean {
        val file = File(context.filesDir, "emojis/$packId.json")
        return file.exists() && file.length() > 0
    }

    fun deletePack(packId: String): Boolean {
        val file = File(context.filesDir, "emojis/$packId.json")
        return if (file.exists()) file.delete() else false
    }
}

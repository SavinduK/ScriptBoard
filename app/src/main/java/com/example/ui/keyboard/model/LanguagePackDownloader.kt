package com.example.ui.keyboard.model

import android.content.Context
import com.example.data.AppDatabase
import com.example.data.WordFrequencyEntity
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

class LanguagePackDownloader(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val wordDao by lazy {
        AppDatabase.getDatabase(context).wordFrequencyDao()
    }

    private val prefs by lazy {
        KeyboardPreferences.getInstance(context)
    }

    /**
     * Downloads a language package from the GitHub repository.
     * Target URL pattern:
     * https://raw.githubusercontent.com/{owner}/{repo}/{branch}/languages/{id}.json
     */
    suspend fun downloadLanguagePack(
        pack: LanguagePack,
        onProgress: (progress: Float, statusText: String) -> Unit
    ): Result<DownloadResult> = withContext(Dispatchers.IO) {
        val owner = prefs.githubRepoOwner.value.trim()
        val repo = prefs.githubRepoName.value.trim()
        val branch = prefs.githubRepoBranch.value.trim().ifEmpty { "main" }
        val filename = "${pack.id}.json"
        val githubRawUrl = "https://raw.githubusercontent.com/$owner/$repo/$branch/languages/$filename"

        val languagesDir = File(context.filesDir, "languages")
        if (!languagesDir.exists()) {
            languagesDir.mkdirs()
        }
        val targetFile = File(languagesDir, filename)

        try {
            onProgress(0.1f, "Connecting to GitHub repository...")

            val request = Request.Builder()
                .url(githubRawUrl)
                .header("User-Agent", "KeyPro-Android-Keyboard")
                .header("Accept", "application/json")
                .build()

            var downloadedFromRemote = false
            var responseContent = ""

            try {
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful && response.body != null) {
                        onProgress(0.4f, "Downloading from GitHub ($filename)...")
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
                                onProgress(p, "Downloading from GitHub... ${(p * 100).toInt()}%")
                            }
                        }
                        outputStream.flush()
                        outputStream.close()
                        inputStream.close()

                        responseContent = targetFile.readText()
                        downloadedFromRemote = true
                    }
                }
            } catch (networkEx: Exception) {
                // Network unreachable or repo raw file not yet published
                downloadedFromRemote = false
            }

            // Fallback to local bundled asset if remote not reachable
            if (!downloadedFromRemote) {
                onProgress(0.5f, "Extracting repository package files...")
                val assetPath = "languages/$filename"
                try {
                    context.assets.open(assetPath).use { assetInput ->
                        FileOutputStream(targetFile).use { fileOutput ->
                            assetInput.copyTo(fileOutput)
                        }
                    }
                    responseContent = targetFile.readText()
                } catch (assetEx: Exception) {
                    return@withContext Result.failure(
                        Exception("Could not download from $githubRawUrl and no local asset found ($assetPath): ${assetEx.message}")
                    )
                }
            }

            // Parse language file to extract predictive words and layout details
            onProgress(0.85f, "Installing language assets & dictionary...")
            var wordsExtracted = 0
            try {
                if (responseContent.isNotBlank()) {
                    val json = JSONObject(responseContent)
                    if (json.has("sampleWords")) {
                        val wordsArray = json.getJSONArray("sampleWords")
                        for (i in 0 until wordsArray.length()) {
                            val word = wordsArray.getString(i).trim()
                            if (word.isNotBlank()) {
                                wordDao.insertOrUpdate(
                                    WordFrequencyEntity(
                                        word = word,
                                        frequency = 25,
                                        lastUsed = System.currentTimeMillis()
                                    )
                                )
                                wordsExtracted++
                            }
                        }
                    }
                }
            } catch (_: Exception) {
                // Non-fatal if sample words fail
            }

            // Save installation state
            prefs.installLanguage(pack.id)
            prefs.setCurrentLanguage(pack.id)
            onProgress(1.0f, "Completed")

            val source = if (downloadedFromRemote) {
                "GitHub (${owner}/${repo})"
            } else {
                "Repository Bundle (${targetFile.length() / 1024} KB)"
            }

            Result.success(
                DownloadResult(
                    file = targetFile,
                    source = source,
                    githubUrl = githubRawUrl,
                    downloadedFromRemote = downloadedFromRemote,
                    wordsImported = wordsExtracted,
                    fileSizeBytes = targetFile.length()
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Checks if a language package is locally available on disk
     */
    fun isPackageDownloaded(langId: String): Boolean {
        val file = File(context.filesDir, "languages/$langId.json")
        return file.exists() && file.length() > 0
    }

    /**
     * Retrieves downloaded package file if present
     */
    fun getDownloadedFile(langId: String): File? {
        val file = File(context.filesDir, "languages/$langId.json")
        return if (file.exists() && file.length() > 0) file else null
    }

    /**
     * Removes local language pack file on uninstall
     */
    fun deletePackageFile(langId: String): Boolean {
        val file = File(context.filesDir, "languages/$langId.json")
        return if (file.exists()) file.delete() else false
    }
}

data class DownloadResult(
    val file: File,
    val source: String,
    val githubUrl: String,
    val downloadedFromRemote: Boolean,
    val wordsImported: Int,
    val fileSizeBytes: Long
)

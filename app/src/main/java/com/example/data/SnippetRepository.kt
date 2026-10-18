package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SnippetRepository(private val snippetDao: SnippetDao) {
    val allSnippets: Flow<List<SnippetEntity>> = snippetDao.getAllSnippets()
    val pinnedSnippets: Flow<List<SnippetEntity>> = snippetDao.getPinnedSnippets()
    val historySnippets: Flow<List<SnippetEntity>> = snippetDao.getHistorySnippets()
    val imageSnippets: Flow<List<SnippetEntity>> = snippetDao.getAllSnippets().map { list ->
        list.filter { it.isImage || !it.imageUri.isNullOrBlank() }
    }

    suspend fun insertSnippet(
        title: String,
        content: String,
        isPinned: Boolean,
        category: String,
        shortcut: String = "",
        imageUri: String? = null,
        isImage: Boolean = false,
        mimeType: String? = null
    ): Long {
        val cleanShortcut = if (shortcut.isNotBlank() && !shortcut.startsWith("@")) "@$shortcut" else shortcut.trim()
        val defaultTitle = if (isImage) {
            "Image Clip (${java.text.SimpleDateFormat("MMM d, HH:mm", java.util.Locale.getDefault()).format(java.util.Date())})"
        } else {
            content.lineSequence().firstOrNull()?.trim()?.take(25) ?: "Snippet"
        }

        return snippetDao.insertSnippet(
            SnippetEntity(
                title = title.ifBlank { defaultTitle },
                content = content,
                isPinned = isPinned,
                category = category,
                shortcut = cleanShortcut,
                timestamp = System.currentTimeMillis(),
                imageUri = imageUri,
                isImage = isImage,
                mimeType = mimeType
            )
        )
    }

    suspend fun saveImageToClipboard(
        uri: String,
        title: String? = null,
        isPinned: Boolean = false,
        mimeType: String = "image/*"
    ): Long {
        val dateStr = java.text.SimpleDateFormat("MMM d, HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
        val defaultTitle = title ?: "Image ($dateStr)"
        return snippetDao.insertSnippet(
            SnippetEntity(
                title = defaultTitle,
                content = uri,
                isPinned = isPinned,
                category = "Images",
                shortcut = "",
                timestamp = System.currentTimeMillis(),
                imageUri = uri,
                isImage = true,
                mimeType = mimeType
            )
        )
    }

    suspend fun togglePin(id: Long, currentPinState: Boolean) {
        snippetDao.updatePinStatus(id, !currentPinState)
    }

    suspend fun updateSnippet(snippet: SnippetEntity) {
        snippetDao.updateSnippet(snippet)
    }

    suspend fun deleteSnippet(id: Long) {
        snippetDao.deleteSnippet(id)
    }

    suspend fun clearUnpinned() {
        snippetDao.clearUnpinned()
    }

    suspend fun saveToClipboardHistory(text: String) {
        if (text.isBlank()) return
        val existing = snippetDao.findByContent(text)
        if (existing == null) {
            val title = text.lineSequence().firstOrNull()?.trim()?.take(25)?.ifEmpty { "Clipboard Text" } ?: "Clipboard Text"
            snippetDao.insertSnippet(
                SnippetEntity(
                    title = title,
                    content = text,
                    isPinned = false,
                    category = "History",
                    timestamp = System.currentTimeMillis()
                )
            )
        } else if (!existing.isPinned) {
            snippetDao.updateSnippet(existing.copy(timestamp = System.currentTimeMillis()))
        }
    }
}

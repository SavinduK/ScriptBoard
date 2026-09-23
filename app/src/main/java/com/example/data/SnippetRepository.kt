package com.example.data

import kotlinx.coroutines.flow.Flow

class SnippetRepository(private val snippetDao: SnippetDao) {
    val allSnippets: Flow<List<SnippetEntity>> = snippetDao.getAllSnippets()
    val pinnedSnippets: Flow<List<SnippetEntity>> = snippetDao.getPinnedSnippets()

    suspend fun insertSnippet(title: String, content: String, isPinned: Boolean, category: String): Long {
        return snippetDao.insertSnippet(
            SnippetEntity(
                title = title.ifBlank { content.take(20) },
                content = content,
                isPinned = isPinned,
                category = category
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
            snippetDao.insertSnippet(
                SnippetEntity(
                    title = text.lineSequence().firstOrNull()?.take(25) ?: "Snippet",
                    content = text,
                    isPinned = false,
                    category = "Clipboard"
                )
            )
        }
    }
}

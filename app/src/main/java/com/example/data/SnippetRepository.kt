package com.example.data

import kotlinx.coroutines.flow.Flow

class SnippetRepository(private val snippetDao: SnippetDao) {
    val allSnippets: Flow<List<SnippetEntity>> = snippetDao.getAllSnippets()
    val pinnedSnippets: Flow<List<SnippetEntity>> = snippetDao.getPinnedSnippets()
    val historySnippets: Flow<List<SnippetEntity>> = snippetDao.getHistorySnippets()

    suspend fun insertSnippet(title: String, content: String, isPinned: Boolean, category: String): Long {
        return snippetDao.insertSnippet(
            SnippetEntity(
                title = title.ifBlank { content.lineSequence().firstOrNull()?.trim()?.take(25) ?: "Snippet" },
                content = content,
                isPinned = isPinned,
                category = category,
                timestamp = System.currentTimeMillis()
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

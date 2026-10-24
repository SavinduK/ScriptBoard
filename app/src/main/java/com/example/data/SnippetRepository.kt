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
        // Prevent duplicate image copies in clipboard history
        val existing = snippetDao.findByContent(uri)
        if (existing != null) {
            val updated = existing.copy(
                timestamp = System.currentTimeMillis(),
                isPinned = isPinned || existing.isPinned
            )
            snippetDao.updateSnippet(updated)
            return existing.id
        }

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

    suspend fun togglePin(id: Long, currentPinState: Boolean): Boolean {
        val newPinnedState = !currentPinState
        snippetDao.updatePinStatus(id, newPinnedState)
        return newPinnedState
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

    /**
     * Finds and deletes duplicate copies of entries in clipboard history.
     * If an entry is pinned, any unpinned copies of the exact same content are removed.
     * For unpinned entries with duplicate content, keeps the newest one and deletes older duplicates.
     */
    suspend fun deleteDuplicateHistoryEntries(): Int {
        val allHistory = snippetDao.getAllHistoryList()
        if (allHistory.isEmpty()) return 0

        var deletedCount = 0
        val grouped = allHistory.groupBy { it.content.trim() }
        val idsToDelete = mutableListOf<Long>()

        for ((content, duplicates) in grouped) {
            // Check if there is also a pinned version of this content
            val pinnedVersion = snippetDao.findByContent(content)?.takeIf { it.isPinned }
            if (pinnedVersion != null) {
                // All unpinned copies are duplicates of the pinned item
                idsToDelete.addAll(duplicates.map { it.id })
            } else if (duplicates.size > 1) {
                // Keep the most recent, delete older duplicates
                val sorted = duplicates.sortedByDescending { it.timestamp }
                idsToDelete.addAll(sorted.drop(1).map { it.id })
            }
        }

        if (idsToDelete.isNotEmpty()) {
            deletedCount = snippetDao.deleteSnippetsByIds(idsToDelete)
        }
        return deletedCount
    }

    /**
     * Purges unpinned clipboard history older than [retentionDays] days.
     * Pinned snippets are safely preserved and never deleted.
     */
    suspend fun purgeExpiredHistory(retentionDays: Int): Int {
        if (retentionDays <= 0) return 0
        val cutoff = System.currentTimeMillis() - (retentionDays.toLong() * 24L * 60L * 60L * 1000L)
        return snippetDao.deleteHistoryOlderThan(cutoff)
    }

    suspend fun saveToClipboardHistory(
        text: String,
        syncId: String = "",
        originDeviceId: String = ""
    ): SnippetEntity? {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return null

        // 1. Search for existing copies (both trimmed and raw) to prevent duplicates
        val matches = (snippetDao.findAllByContent(trimmed) + snippetDao.findAllByContent(text)).distinctBy { it.id }

        if (matches.isNotEmpty()) {
            val pinnedMatch = matches.firstOrNull { it.isPinned }
            if (pinnedMatch != null) {
                // Content is already pinned: remove any redundant unpinned copies
                val unpinnedDuplicates = matches.filter { !it.isPinned }.map { it.id }
                if (unpinnedDuplicates.isNotEmpty()) {
                    snippetDao.deleteSnippetsByIds(unpinnedDuplicates)
                }
                // Refresh timestamp
                val updated = pinnedMatch.copy(timestamp = System.currentTimeMillis())
                snippetDao.updateSnippet(updated)
                return updated
            } else {
                // Content is in history: keep the newest, delete all older duplicate copies
                val newest = matches.maxByOrNull { it.timestamp } ?: matches.first()
                val duplicatesToDelete = matches.filter { it.id != newest.id }.map { it.id }
                if (duplicatesToDelete.isNotEmpty()) {
                    snippetDao.deleteSnippetsByIds(duplicatesToDelete)
                }
                // Update timestamp so this entry jumps to the top of clipboard history
                val updated = newest.copy(
                    timestamp = System.currentTimeMillis(),
                    syncId = if (syncId.isNotBlank()) syncId else newest.syncId,
                    originDeviceId = if (originDeviceId.isNotBlank()) originDeviceId else newest.originDeviceId
                )
                snippetDao.updateSnippet(updated)
                return updated
            }
        }

        // 2. No existing copy found: insert new history entry
        val title = trimmed.lineSequence().firstOrNull()?.trim()?.take(25)?.ifEmpty { "Clipboard Text" } ?: "Clipboard Text"
        val entity = SnippetEntity(
            title = title,
            content = trimmed,
            isPinned = false,
            category = "History",
            timestamp = System.currentTimeMillis(),
            syncId = syncId,
            originDeviceId = originDeviceId
        )
        val newId = snippetDao.insertSnippet(entity)
        return entity.copy(id = newId)
    }

    suspend fun saveSyncedSnippet(
        title: String,
        content: String,
        isPinned: Boolean,
        category: String,
        timestamp: Long,
        imageUri: String?,
        isImage: Boolean,
        mimeType: String?,
        syncId: String,
        originDeviceId: String
    ): SnippetEntity {
        // Prevent duplicate insert if already present by syncId
        if (syncId.isNotBlank()) {
            val existing = snippetDao.findBySyncId(syncId)
            if (existing != null) {
                val shouldBePinned = isPinned || existing.isPinned
                if (shouldBePinned != existing.isPinned) {
                    val updated = existing.copy(isPinned = shouldBePinned)
                    snippetDao.updateSnippet(updated)
                    return updated
                }
                return existing
            }
        }

        // Compare content to eliminate duplicate copies across devices
        val existingByContent = snippetDao.findByContent(content)
        if (existingByContent != null) {
            val shouldBePinned = isPinned || existingByContent.isPinned
            val updated = existingByContent.copy(
                isPinned = shouldBePinned,
                timestamp = maxOf(timestamp, existingByContent.timestamp),
                syncId = if (syncId.isNotBlank()) syncId else existingByContent.syncId
            )
            snippetDao.updateSnippet(updated)
            return updated
        }

        val entity = SnippetEntity(
            title = title,
            content = content,
            isPinned = isPinned,
            category = category,
            timestamp = timestamp,
            imageUri = imageUri,
            isImage = isImage,
            mimeType = mimeType,
            syncId = syncId,
            originDeviceId = originDeviceId
        )
        val id = snippetDao.insertSnippet(entity)
        return entity.copy(id = id)
    }
}

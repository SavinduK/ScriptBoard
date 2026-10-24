package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SnippetDao {
    @Query("SELECT * FROM snippets ORDER BY isPinned DESC, timestamp DESC")
    fun getAllSnippets(): Flow<List<SnippetEntity>>

    @Query("SELECT * FROM snippets WHERE isPinned = 1 ORDER BY timestamp DESC")
    fun getPinnedSnippets(): Flow<List<SnippetEntity>>

    @Query("SELECT * FROM snippets WHERE isPinned = 0 ORDER BY timestamp DESC")
    fun getHistorySnippets(): Flow<List<SnippetEntity>>

    @Query("SELECT * FROM snippets WHERE content = :text LIMIT 1")
    suspend fun findByContent(text: String): SnippetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnippet(snippet: SnippetEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(snippets: List<SnippetEntity>)

    @Update
    suspend fun updateSnippet(snippet: SnippetEntity)

    @Query("UPDATE snippets SET isPinned = :isPinned WHERE id = :id")
    suspend fun updatePinStatus(id: Long, isPinned: Boolean)

    @Query("DELETE FROM snippets WHERE id = :id")
    suspend fun deleteSnippet(id: Long)

    @Query("DELETE FROM snippets WHERE isPinned = 0")
    suspend fun clearUnpinned()

    @Query("DELETE FROM snippets WHERE isPinned = 0 AND timestamp < :cutoffTimestamp")
    suspend fun deleteHistoryOlderThan(cutoffTimestamp: Long): Int

    @Query("SELECT * FROM snippets WHERE syncId = :syncId LIMIT 1")
    suspend fun findBySyncId(syncId: String): SnippetEntity?

    @Query("SELECT * FROM snippets WHERE timestamp > :sinceTimestamp ORDER BY timestamp DESC LIMIT 50")
    suspend fun getClipsSince(sinceTimestamp: Long): List<SnippetEntity>

    @Query("SELECT * FROM snippets WHERE content = :text")
    suspend fun findAllByContent(text: String): List<SnippetEntity>

    @Query("DELETE FROM snippets WHERE id IN (:ids)")
    suspend fun deleteSnippetsByIds(ids: List<Long>): Int

    @Query("SELECT * FROM snippets WHERE isPinned = 0")
    suspend fun getAllHistoryList(): List<SnippetEntity>

    @Query("SELECT * FROM snippets WHERE isPinned = 1")
    suspend fun getPinnedList(): List<SnippetEntity>
}

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
}

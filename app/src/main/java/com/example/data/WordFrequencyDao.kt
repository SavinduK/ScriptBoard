package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface WordFrequencyDao {
    @Query("SELECT * FROM word_frequency WHERE word LIKE :prefix || '%' ORDER BY frequency DESC, lastUsed DESC LIMIT :limit")
    suspend fun getSuggestionsForPrefix(prefix: String, limit: Int = 10): List<WordFrequencyEntity>

    @Query("SELECT * FROM word_frequency ORDER BY frequency DESC, lastUsed DESC LIMIT :limit")
    suspend fun getTopWords(limit: Int = 10): List<WordFrequencyEntity>

    @Query("SELECT * FROM word_frequency WHERE word = :word LIMIT 1")
    suspend fun getWord(word: String): WordFrequencyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entity: WordFrequencyEntity)

    @Query("UPDATE word_frequency SET frequency = frequency + 1, lastUsed = :timestamp WHERE word = :word")
    suspend fun incrementFrequency(word: String, timestamp: Long = System.currentTimeMillis()): Int
}

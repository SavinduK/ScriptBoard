package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface WordFrequencyDao {
    @Query("SELECT * FROM word_frequency WHERE word LIKE :prefix || '%' ORDER BY frequency DESC, lastUsed DESC LIMIT :limit")
    suspend fun getSuggestionsForPrefix(prefix: String, limit: Int = 10): List<WordFrequencyEntity>

    @Query("SELECT * FROM word_frequency WHERE word LIKE :prefix || '%' AND frequency >= :minFrequency ORDER BY frequency DESC, lastUsed DESC LIMIT :limit")
    suspend fun getLearnedSuggestions(prefix: String, minFrequency: Int = 2, limit: Int = 20): List<WordFrequencyEntity>

    @Query("SELECT * FROM word_frequency WHERE frequency >= :minFrequency ORDER BY frequency DESC, lastUsed DESC LIMIT :limit")
    suspend fun getTopLearnedWords(minFrequency: Int = 2, limit: Int = 20): List<WordFrequencyEntity>

    @Query("SELECT * FROM word_frequency ORDER BY frequency DESC, lastUsed DESC LIMIT :limit")
    suspend fun getTopWords(limit: Int = 10): List<WordFrequencyEntity>

    @Query("SELECT * FROM word_frequency WHERE word = :word LIMIT 1")
    suspend fun getWord(word: String): WordFrequencyEntity?

    @Query("SELECT * FROM word_frequency")
    suspend fun getAllWords(): List<WordFrequencyEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entity: WordFrequencyEntity)

    @Query("UPDATE word_frequency SET frequency = frequency + 1, lastUsed = :timestamp WHERE word = :word")
    suspend fun incrementFrequency(word: String, timestamp: Long = System.currentTimeMillis()): Int

    @Query("UPDATE word_frequency SET frequency = frequency + 1, lastUsed = :timestamp, originalCase = :originalCase WHERE word = :word")
    suspend fun incrementFrequencyWithCase(word: String, originalCase: String, timestamp: Long = System.currentTimeMillis()): Int

    @Query("UPDATE word_frequency SET frequency = :frequency, lastUsed = :timestamp, originalCase = :originalCase WHERE word = :word")
    suspend fun updateFrequencyWithDecay(word: String, frequency: Int, originalCase: String, timestamp: Long = System.currentTimeMillis()): Int

    @Query("DELETE FROM word_frequency WHERE word = :word")
    suspend fun deleteWord(word: String)

    @Query("DELETE FROM word_frequency WHERE word IN (:words)")
    suspend fun deleteWords(words: List<String>)

    @Query("DELETE FROM word_frequency WHERE frequency < :minFrequency AND lastUsed < :cutoffTimestamp")
    suspend fun deleteStaleWords(cutoffTimestamp: Long, minFrequency: Int = 3): Int

    @Query("DELETE FROM word_frequency")
    suspend fun clearAll()
}

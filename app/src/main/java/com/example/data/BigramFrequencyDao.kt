package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface BigramFrequencyDao {
    @Query("SELECT * FROM bigram_frequencies WHERE prevWord = :prevWord ORDER BY count DESC, lastUsed DESC LIMIT :limit")
    suspend fun getTopContinuations(prevWord: String, limit: Int = 5): List<BigramFrequencyEntity>

    @Query("SELECT * FROM bigram_frequencies WHERE prevWord = :prevWord AND word = :word LIMIT 1")
    suspend fun getBigram(prevWord: String, word: String): BigramFrequencyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entity: BigramFrequencyEntity)

    @Query("UPDATE bigram_frequencies SET count = count + 1, lastUsed = :timestamp WHERE prevWord = :prevWord AND word = :word")
    suspend fun incrementBigram(prevWord: String, word: String, timestamp: Long = System.currentTimeMillis()): Int

    @Query("DELETE FROM bigram_frequencies WHERE count < :minCount AND lastUsed < :cutoffTimestamp")
    suspend fun deleteStaleBigrams(cutoffTimestamp: Long, minCount: Int = 3): Int

    @Query("DELETE FROM bigram_frequencies")
    suspend fun clearAll()
}

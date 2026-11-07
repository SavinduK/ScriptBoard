package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CorrectionPairDao {
    @Query("SELECT * FROM correction_pairs WHERE typed = :typed AND language = :language ORDER BY count DESC, lastUsed DESC")
    suspend fun getCorrections(typed: String, language: String): List<CorrectionPairEntity>

    @Query("SELECT * FROM correction_pairs WHERE typed = :typed AND corrected = :corrected AND language = :language LIMIT 1")
    suspend fun getPair(typed: String, corrected: String, language: String): CorrectionPairEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entity: CorrectionPairEntity)

    @Query("UPDATE correction_pairs SET count = :count, lastUsed = :timestamp WHERE typed = :typed AND corrected = :corrected AND language = :language")
    suspend fun updateCount(typed: String, corrected: String, language: String, count: Int, timestamp: Long): Int

    @Query("SELECT COUNT(*) FROM correction_pairs WHERE language = :language")
    suspend fun countByLanguage(language: String): Int

    @Query("DELETE FROM correction_pairs WHERE language = :language")
    suspend fun clearByLanguage(language: String)

    @Query("DELETE FROM correction_pairs")
    suspend fun clearAll()
}

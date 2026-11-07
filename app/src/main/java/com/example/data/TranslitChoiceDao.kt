package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TranslitChoiceDao {
    @Query("SELECT * FROM translit_choices WHERE latin = :latin ORDER BY count DESC LIMIT :limit")
    suspend fun getTopChoicesForLatin(latin: String, limit: Int = 5): List<TranslitChoiceEntity>

    @Query("SELECT * FROM translit_choices WHERE latin = :latin AND sinhala = :sinhala LIMIT 1")
    suspend fun getChoice(latin: String, sinhala: String): TranslitChoiceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entity: TranslitChoiceEntity)

    @Query("UPDATE translit_choices SET count = count + 1 WHERE latin = :latin AND sinhala = :sinhala")
    suspend fun incrementChoice(latin: String, sinhala: String): Int

    @Query("DELETE FROM translit_choices")
    suspend fun clearAll()
}

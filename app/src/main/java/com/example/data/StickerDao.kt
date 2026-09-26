package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface StickerDao {
    @Query("SELECT * FROM stickers ORDER BY dateAdded DESC")
    fun getAllStickers(): Flow<List<StickerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(sticker: StickerEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(stickers: List<StickerEntity>)

    @Update
    suspend fun update(sticker: StickerEntity)

    @Delete
    suspend fun delete(sticker: StickerEntity)

    @Query("DELETE FROM stickers WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM stickers")
    suspend fun getStickerCount(): Int

    @Query("DELETE FROM stickers")
    suspend fun clearAll()
}

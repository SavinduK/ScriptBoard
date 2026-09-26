package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stickers")
data class StickerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val filePath: String,
    val name: String = "Sticker",
    val source: String = "gallery", // "whatsapp", "gallery", "imported"
    val dateAdded: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val usageCount: Int = 0
)

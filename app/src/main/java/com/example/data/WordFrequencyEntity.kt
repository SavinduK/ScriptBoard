package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "word_frequency")
data class WordFrequencyEntity(
    @PrimaryKey
    val word: String,
    val frequency: Int = 1,
    val lastUsed: Long = System.currentTimeMillis()
)

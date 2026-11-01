package com.example.data

import androidx.room.Entity

@Entity(
    tableName = "bigram_frequencies",
    primaryKeys = ["prevWord", "word"]
)
data class BigramFrequencyEntity(
    val prevWord: String,
    val word: String,
    val count: Int = 1,
    val lastUsed: Long = System.currentTimeMillis()
)

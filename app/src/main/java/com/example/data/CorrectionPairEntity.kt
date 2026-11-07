package com.example.data

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "correction_pairs",
    primaryKeys = ["typed", "corrected", "language"],
    indices = [
        Index(value = ["typed", "language"]),
        Index(value = ["language"])
    ]
)
data class CorrectionPairEntity(
    val typed: String,
    val corrected: String,
    val language: String,
    val count: Int = 1,
    val lastUsed: Long = System.currentTimeMillis()
)

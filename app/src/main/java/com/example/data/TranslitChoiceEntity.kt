package com.example.data

import androidx.room.Entity

@Entity(
    tableName = "translit_choices",
    primaryKeys = ["latin", "sinhala"]
)
data class TranslitChoiceEntity(
    val latin: String,
    val sinhala: String,
    val count: Int = 1
)

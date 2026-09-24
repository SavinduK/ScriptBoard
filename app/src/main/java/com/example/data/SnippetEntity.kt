package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "snippets")
data class SnippetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val content: String,
    val isPinned: Boolean = true,
    val category: String = "Quick Text",
    val shortcut: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

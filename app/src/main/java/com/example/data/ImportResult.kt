package com.example.data

data class FailedImport(
    val name: String,
    val reason: String
)

data class ImportResult(
    val imported: Int,
    val failed: List<FailedImport>
)

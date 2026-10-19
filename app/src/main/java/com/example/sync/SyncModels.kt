package com.example.sync

data class PairingPayload(
    val type: String = "KEYPRO_PAIR",
    val version: Int = 1,
    val deviceId: String,
    val deviceName: String,
    val ip: String,
    val port: Int,
    val token: String
)

data class SyncClipPayload(
    val syncId: String,
    val originDeviceId: String,
    val title: String,
    val content: String,
    val isPinned: Boolean,
    val category: String,
    val timestamp: Long,
    val isImage: Boolean,
    val mimeType: String? = null,
    val imageBase64: String? = null
)

data class SyncActivityEvent(
    val id: String = java.util.UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val title: String,
    val description: String,
    val isOutgoing: Boolean,
    val isImage: Boolean = false,
    val isLoopPrevented: Boolean = false
)

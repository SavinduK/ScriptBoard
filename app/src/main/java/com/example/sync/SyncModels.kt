package com.example.sync

data class PairingPayload(
    val type: String = "KEYPRO_PAIR",
    val version: Int = 2,
    val deviceId: String,
    val deviceName: String,
    val sharedSecret: String,
    val ip: String,
    val port: Int = 8765,
    val bluetoothMac: String = ""
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
    val isLoopPrevented: Boolean = false,
    val transport: String = "Wi-Fi (P2P)"
)

/**
 * Streaming wire message for persistent TCP and RFCOMM sockets.
 */
data class SocketFrame(
    val type: String, // CHALLENGE, AUTH, AUTH_OK, AUTH_FAIL, CLIP, PING, PONG, IP_UPDATE
    val nonce: String? = null,
    val deviceId: String? = null,
    val hmac: String? = null,
    val newIp: String? = null,
    val newPort: Int? = null,
    val clip: SyncClipPayload? = null
)

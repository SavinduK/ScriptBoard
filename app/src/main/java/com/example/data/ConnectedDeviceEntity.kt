package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "connected_devices")
data class ConnectedDeviceEntity(
    @PrimaryKey
    val deviceId: String,
    val name: String,
    val ipAddress: String,
    val port: Int = 8765,
    val authToken: String = "",
    val sharedSecret: String = "",
    val bluetoothMac: String = "",
    val isOnline: Boolean = true,
    val connectionStatus: String = "Connected",
    val transportType: String = "Wi-Fi (P2P)",
    val lastSyncedTimestamp: Long = System.currentTimeMillis(),
    val syncText: Boolean = true,
    val syncImages: Boolean = true,
    val addedTimestamp: Long = System.currentTimeMillis()
)

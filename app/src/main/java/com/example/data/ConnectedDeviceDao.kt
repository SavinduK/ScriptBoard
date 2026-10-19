package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ConnectedDeviceDao {
    @Query("SELECT * FROM connected_devices ORDER BY lastSyncedTimestamp DESC")
    fun getAllDevices(): Flow<List<ConnectedDeviceEntity>>

    @Query("SELECT * FROM connected_devices")
    suspend fun getAllDevicesList(): List<ConnectedDeviceEntity>

    @Query("SELECT * FROM connected_devices WHERE deviceId = :deviceId LIMIT 1")
    suspend fun getDeviceById(deviceId: String): ConnectedDeviceEntity?

    @Query("SELECT * FROM connected_devices WHERE bluetoothMac = :mac LIMIT 1")
    suspend fun getDeviceByMac(mac: String): ConnectedDeviceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(device: ConnectedDeviceEntity)

    @Update
    suspend fun update(device: ConnectedDeviceEntity)

    @Query("DELETE FROM connected_devices WHERE deviceId = :deviceId")
    suspend fun deleteDevice(deviceId: String)

    @Query("UPDATE connected_devices SET isOnline = :isOnline, lastSyncedTimestamp = :timestamp WHERE deviceId = :deviceId")
    suspend fun updateStatus(deviceId: String, isOnline: Boolean, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE connected_devices SET ipAddress = :ipAddress, port = :port, isOnline = 1, lastSyncedTimestamp = :timestamp WHERE deviceId = :deviceId")
    suspend fun updateAddressAndPort(deviceId: String, ipAddress: String, port: Int, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE connected_devices SET isOnline = :isOnline, connectionStatus = :status, transportType = :transport, lastSyncedTimestamp = :timestamp WHERE deviceId = :deviceId")
    suspend fun updateStatusAndTransport(
        deviceId: String,
        isOnline: Boolean,
        status: String,
        transport: String,
        timestamp: Long = System.currentTimeMillis()
    )
}

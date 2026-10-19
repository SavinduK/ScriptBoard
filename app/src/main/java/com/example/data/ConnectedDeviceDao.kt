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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(device: ConnectedDeviceEntity)

    @Update
    suspend fun update(device: ConnectedDeviceEntity)

    @Query("DELETE FROM connected_devices WHERE deviceId = :deviceId")
    suspend fun deleteDevice(deviceId: String)

    @Query("UPDATE connected_devices SET isOnline = :isOnline, lastSyncedTimestamp = :timestamp WHERE deviceId = :deviceId")
    suspend fun updateStatus(deviceId: String, isOnline: Boolean, timestamp: Long = System.currentTimeMillis())
}

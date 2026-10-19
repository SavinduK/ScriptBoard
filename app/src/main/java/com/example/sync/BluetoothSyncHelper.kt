package com.example.sync

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import com.example.data.ConnectedDeviceDao
import com.example.data.ConnectedDeviceEntity
import com.example.ui.keyboard.util.KeyboardPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class BluetoothSyncHelper(
    private val context: Context,
    private val scope: CoroutineScope,
    private val preferences: KeyboardPreferences,
    private val connectedDeviceDao: ConnectedDeviceDao,
    private val onClipReceived: (clip: SyncClipPayload, deviceId: String) -> Unit,
    private val onLogEvent: (event: SyncActivityEvent) -> Unit
) {
    companion object {
        val KEYPRO_BT_UUID: UUID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e")
        private const val TAG = "BluetoothSyncHelper"
    }

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        try {
            BluetoothAdapter.getDefaultAdapter()
        } catch (_: Exception) {
            null
        }
    }

    private var serverSocket: BluetoothServerSocket? = null
    private var serverJob: Job? = null
    private val activeBtSockets = ConcurrentHashMap<String, BluetoothSocket>()

    fun isBluetoothAvailable(): Boolean = bluetoothAdapter != null
    fun isBluetoothEnabled(): Boolean = bluetoothAdapter?.isEnabled == true

    @SuppressLint("HardwareIds")
    fun getLocalBluetoothMac(): String {
        return try {
            val addr = bluetoothAdapter?.address
            if (!addr.isNullOrBlank() && addr != "02:00:00:00:00:00") {
                addr
            } else {
                // If randomized/hidden on modern Android, use device-specific MAC hint
                val id = preferences.getLocalDeviceId()
                "BT:${id.take(2)}:${id.drop(2).take(2)}:${id.drop(4).take(2)}:${id.drop(6).take(2)}"
            }
        } catch (_: Exception) {
            ""
        }
    }

    fun startListening() {
        if (!preferences.bluetoothSyncEnabled.value || bluetoothAdapter?.isEnabled != true) return
        if (serverJob?.isActive == true) return

        serverJob = scope.launch(Dispatchers.IO) {
            try {
                @SuppressLint("MissingPermission")
                serverSocket = bluetoothAdapter?.listenUsingRfcommWithServiceRecord(
                    "KeyProClipboardSync",
                    KEYPRO_BT_UUID
                )
                onLogEvent(
                    SyncActivityEvent(
                        title = "Bluetooth RFCOMM Listener Started",
                        description = "Listening for paired peer devices (UUID: KeyPro)",
                        isOutgoing = false,
                        transport = "Bluetooth RFCOMM"
                    )
                )

                while (isActive) {
                    val socket = serverSocket?.accept() ?: break
                    handleIncomingSocket(socket)
                }
            } catch (e: Exception) {
                Log.d(TAG, "Bluetooth server listener stopped: ${e.message}")
            }
        }
    }

    private fun handleIncomingSocket(socket: BluetoothSocket) {
        scope.launch(Dispatchers.IO) {
            var peerDeviceId = ""
            var peerDeviceName = "Bluetooth Peer"
            try {
                val reader = BufferedReader(InputStreamReader(socket.inputStream, Charsets.UTF_8))
                val writer = BufferedWriter(OutputStreamWriter(socket.outputStream, Charsets.UTF_8))

                // Send challenge
                val nonce = CryptoUtil.generateNonce(16)
                val challengeFrame = JSONObject().apply {
                    put("type", "CHALLENGE")
                    put("nonce", nonce)
                    put("deviceId", preferences.getLocalDeviceId())
                }.toString()
                writer.write(challengeFrame + "\n")
                writer.flush()

                // Read AUTH response
                val authLine = reader.readLine() ?: return@launch
                val authJson = JSONObject(authLine)
                peerDeviceId = authJson.optString("deviceId", "")
                val clientHmac = authJson.optString("hmac", "")

                val pairedDevice = connectedDeviceDao.getDeviceById(peerDeviceId)
                val secret = pairedDevice?.sharedSecret?.ifBlank { pairedDevice.authToken } ?: ""

                val authenticated = if (secret.isNotBlank()) {
                    CryptoUtil.verifyHmac(secret, nonce, clientHmac)
                } else {
                    true // Backward compatibility
                }

                if (!authenticated) {
                    writer.write(JSONObject().apply { put("type", "AUTH_FAIL") }.toString() + "\n")
                    writer.flush()
                    socket.close()
                    return@launch
                }

                writer.write(JSONObject().apply { put("type", "AUTH_OK") }.toString() + "\n")
                writer.flush()

                peerDeviceName = pairedDevice?.name ?: "Bluetooth Device"
                activeBtSockets[peerDeviceId] = socket
                connectedDeviceDao.updateStatusAndTransport(
                    peerDeviceId,
                    isOnline = true,
                    status = "Connected",
                    transport = "Bluetooth RFCOMM"
                )

                onLogEvent(
                    SyncActivityEvent(
                        title = "Bluetooth RFCOMM Connected",
                        description = "Established channel with $peerDeviceName",
                        isOutgoing = false,
                        transport = "Bluetooth RFCOMM"
                    )
                )

                // Read continuous incoming clip frames
                while (isActive) {
                    val line = reader.readLine() ?: break
                    if (line.isBlank()) continue
                    val frame = JSONObject(line)
                    when (frame.optString("type")) {
                        "CLIP" -> {
                            val clipObj = frame.optJSONObject("payload")
                            if (clipObj != null) {
                                val clip = SyncClipPayload(
                                    syncId = clipObj.optString("syncId"),
                                    originDeviceId = clipObj.optString("originDeviceId"),
                                    title = clipObj.optString("title"),
                                    content = clipObj.optString("content"),
                                    isPinned = clipObj.optBoolean("isPinned", false),
                                    category = clipObj.optString("category", "Synced"),
                                    timestamp = clipObj.optLong("timestamp", System.currentTimeMillis()),
                                    isImage = clipObj.optBoolean("isImage", false),
                                    mimeType = clipObj.optString("mimeType", null),
                                    imageBase64 = clipObj.optString("imageBase64", null)
                                )
                                onClipReceived(clip, peerDeviceId)
                            }
                        }
                        "PING" -> {
                            writer.write(JSONObject().apply { put("type", "PONG") }.toString() + "\n")
                            writer.flush()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "Bluetooth connection ended for $peerDeviceId: ${e.message}")
            } finally {
                if (peerDeviceId.isNotBlank()) {
                    activeBtSockets.remove(peerDeviceId)
                    connectedDeviceDao.updateStatusAndTransport(
                        peerDeviceId,
                        isOnline = false,
                        status = "Disconnected",
                        transport = "Bluetooth RFCOMM"
                    )
                }
                try { socket.close() } catch (_: Exception) {}
            }
        }
    }

    /**
     * Connects outgoing to a paired Bluetooth device via RFCOMM
     */
    fun connectToDevice(device: ConnectedDeviceEntity) {
        if (!preferences.bluetoothSyncEnabled.value || bluetoothAdapter?.isEnabled != true) return
        if (device.bluetoothMac.isBlank() || activeBtSockets.containsKey(device.deviceId)) return

        scope.launch(Dispatchers.IO) {
            try {
                @SuppressLint("MissingPermission")
                val remoteDevice = bluetoothAdapter?.getRemoteDevice(device.bluetoothMac) ?: return@launch
                @SuppressLint("MissingPermission")
                val socket = remoteDevice.createRfcommSocketToServiceRecord(KEYPRO_BT_UUID)
                socket.connect()

                val reader = BufferedReader(InputStreamReader(socket.inputStream, Charsets.UTF_8))
                val writer = BufferedWriter(OutputStreamWriter(socket.outputStream, Charsets.UTF_8))

                // Read challenge
                val challengeLine = reader.readLine() ?: return@launch
                val challengeJson = JSONObject(challengeLine)
                val nonce = challengeJson.optString("nonce")

                val secret = device.sharedSecret.ifBlank { device.authToken }
                val hmac = CryptoUtil.computeHmac(secret, nonce)

                val authResp = JSONObject().apply {
                    put("type", "AUTH")
                    put("deviceId", preferences.getLocalDeviceId())
                    put("hmac", hmac)
                }.toString()
                writer.write(authResp + "\n")
                writer.flush()

                val authResultLine = reader.readLine() ?: return@launch
                val authResultJson = JSONObject(authResultLine)
                if (authResultJson.optString("type") != "AUTH_OK") {
                    socket.close()
                    return@launch
                }

                activeBtSockets[device.deviceId] = socket
                connectedDeviceDao.updateStatusAndTransport(
                    device.deviceId,
                    isOnline = true,
                    status = "Connected",
                    transport = "Bluetooth RFCOMM"
                )

                onLogEvent(
                    SyncActivityEvent(
                        title = "Connected via Bluetooth",
                        description = "RFCOMM channel established to ${device.name} (${device.bluetoothMac})",
                        isOutgoing = true,
                        transport = "Bluetooth RFCOMM"
                    )
                )

                // Reader loop
                while (isActive) {
                    val line = reader.readLine() ?: break
                    if (line.isBlank()) continue
                    val frame = JSONObject(line)
                    if (frame.optString("type") == "CLIP") {
                        val clipObj = frame.optJSONObject("payload")
                        if (clipObj != null) {
                            val clip = SyncClipPayload(
                                syncId = clipObj.optString("syncId"),
                                originDeviceId = clipObj.optString("originDeviceId"),
                                title = clipObj.optString("title"),
                                content = clipObj.optString("content"),
                                isPinned = clipObj.optBoolean("isPinned", false),
                                category = clipObj.optString("category", "Synced"),
                                timestamp = clipObj.optLong("timestamp", System.currentTimeMillis()),
                                isImage = clipObj.optBoolean("isImage", false),
                                mimeType = clipObj.optString("mimeType", null),
                                imageBase64 = clipObj.optString("imageBase64", null)
                            )
                            onClipReceived(clip, device.deviceId)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "Outgoing Bluetooth connect failed for ${device.deviceId}: ${e.message}")
            } finally {
                activeBtSockets.remove(device.deviceId)
                connectedDeviceDao.updateStatusAndTransport(
                    device.deviceId,
                    isOnline = false,
                    status = "Standby",
                    transport = "Bluetooth RFCOMM"
                )
            }
        }
    }

    /**
     * Sends a clip frame over Bluetooth RFCOMM to a specific or all active sockets
     */
    fun sendClip(deviceId: String, payloadJson: String): Boolean {
        val socket = activeBtSockets[deviceId] ?: return false
        return try {
            val writer = BufferedWriter(OutputStreamWriter(socket.outputStream, Charsets.UTF_8))
            val frame = JSONObject().apply {
                put("type", "CLIP")
                put("payload", JSONObject(payloadJson))
            }.toString()
            writer.write(frame + "\n")
            writer.flush()
            true
        } catch (_: Exception) {
            activeBtSockets.remove(deviceId)
            false
        }
    }

    fun stop() {
        try {
            serverSocket?.close()
        } catch (_: Exception) {}
        serverJob?.cancel()
        for ((_, socket) in activeBtSockets) {
            try { socket.close() } catch (_: Exception) {}
        }
        activeBtSockets.clear()
    }
}

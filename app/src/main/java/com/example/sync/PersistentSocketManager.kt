package com.example.sync

import android.content.Context
import android.util.Log
import com.example.data.ConnectedDeviceDao
import com.example.data.ConnectedDeviceEntity
import com.example.ui.keyboard.util.KeyboardPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap

class PersistentSocketManager(
    private val context: Context,
    private val scope: CoroutineScope,
    private val preferences: KeyboardPreferences,
    private val connectedDeviceDao: ConnectedDeviceDao,
    private val onClipReceived: (clip: SyncClipPayload, deviceId: String) -> Unit,
    private val onLogEvent: (event: SyncActivityEvent) -> Unit
) {
    companion object {
        private const val TAG = "PersistentSocketManager"
    }

    // Active authenticated sockets mapped by peer deviceId
    private val activeSockets = ConcurrentHashMap<String, Socket>()
    private val reconnectJobs = ConcurrentHashMap<String, Job>()

    fun getConnectedDeviceCount(): Int = activeSockets.size

    fun isDeviceSocketConnected(deviceId: String): Boolean {
        val socket = activeSockets[deviceId] ?: return false
        return socket.isConnected && !socket.isClosed
    }

    /**
     * Handles an incoming persistent TCP stream from a peer.
     * Conducts cryptographic HMAC challenge-response before trusting the connection.
     */
    fun handleIncomingStream(socket: Socket) {
        scope.launch(Dispatchers.IO) {
            var peerDeviceId = ""
            var peerDeviceName = "Peer"
            try {
                socket.soTimeout = 0 // Keep alive
                socket.tcpNoDelay = true
                val reader = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.UTF_8))
                val writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream(), Charsets.UTF_8))

                // 1. Send HMAC Challenge
                val nonce = CryptoUtil.generateNonce(16)
                val challenge = JSONObject().apply {
                    put("type", "CHALLENGE")
                    put("nonce", nonce)
                    put("deviceId", preferences.getLocalDeviceId())
                }.toString()
                writer.write(challenge + "\n")
                writer.flush()

                // 2. Read Auth response
                val authLine = reader.readLine() ?: return@launch
                val authJson = JSONObject(authLine)
                peerDeviceId = authJson.optString("deviceId", "")
                val clientHmac = authJson.optString("hmac", "")

                val pairedDevice = connectedDeviceDao.getDeviceById(peerDeviceId)
                val secret = pairedDevice?.sharedSecret?.ifBlank { pairedDevice.authToken } ?: ""

                val isAuthorized = if (secret.isNotBlank()) {
                    CryptoUtil.verifyHmac(secret, nonce, clientHmac)
                } else {
                    true // Backward compatibility
                }

                if (!isAuthorized) {
                    writer.write(JSONObject().apply { put("type", "AUTH_FAIL") }.toString() + "\n")
                    writer.flush()
                    socket.close()
                    onLogEvent(
                        SyncActivityEvent(
                            title = "Authentication Rejected",
                            description = "HMAC verification failed for incoming socket from $peerDeviceId",
                            isOutgoing = false,
                            transport = "Wi-Fi (TCP)"
                        )
                    )
                    return@launch
                }

                // Auth succeeded
                writer.write(JSONObject().apply { put("type", "AUTH_OK") }.toString() + "\n")
                writer.flush()

                peerDeviceName = pairedDevice?.name ?: "Device ($peerDeviceId)"
                activeSockets[peerDeviceId]?.let { try { it.close() } catch (_: Exception) {} }
                activeSockets[peerDeviceId] = socket

                connectedDeviceDao.updateStatusAndTransport(
                    peerDeviceId,
                    isOnline = true,
                    status = "Connected (Live Socket)",
                    transport = "Wi-Fi (TCP Socket)"
                )

                onLogEvent(
                    SyncActivityEvent(
                        title = "Live TCP Channel Open",
                        description = "Persistent streaming socket active with $peerDeviceName",
                        isOutgoing = false,
                        transport = "Wi-Fi (TCP)"
                    )
                )

                // 3. Reader loop for continuous incoming frames
                while (isActive && !socket.isClosed) {
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
                        "IP_UPDATE" -> {
                            val newIp = frame.optString("newIp", "")
                            val newPort = frame.optInt("newPort", 8765)
                            if (newIp.isNotBlank() && peerDeviceId.isNotBlank()) {
                                connectedDeviceDao.updateAddressAndPort(peerDeviceId, newIp, newPort)
                                onLogEvent(
                                    SyncActivityEvent(
                                        title = "Peer IP Updated",
                                        description = "$peerDeviceName reported new IP: $newIp:$newPort",
                                        isOutgoing = false,
                                        transport = "Wi-Fi (TCP)"
                                    )
                                )
                            }
                        }
                        "PING" -> {
                            writer.write(JSONObject().apply { put("type", "PONG") }.toString() + "\n")
                            writer.flush()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "Incoming socket closed for $peerDeviceId: ${e.message}")
            } finally {
                if (peerDeviceId.isNotBlank()) {
                    activeSockets.remove(peerDeviceId)
                    connectedDeviceDao.updateStatusAndTransport(
                        peerDeviceId,
                        isOnline = false,
                        status = "Reconnecting",
                        transport = "Wi-Fi (TCP)"
                    )
                }
                try { socket.close() } catch (_: Exception) {}
            }
        }
    }

    /**
     * Connects an outgoing long-lived socket to a paired peer device.
     * Auto-reconnects with exponential backoff on disconnect.
     */
    fun startPersistentConnection(device: ConnectedDeviceEntity) {
        if (!preferences.clipboardSyncEnabled.value || !preferences.wifiSyncEnabled.value) return
        if (device.ipAddress.isBlank() || isDeviceSocketConnected(device.deviceId)) return

        reconnectJobs[device.deviceId]?.cancel()
        reconnectJobs[device.deviceId] = scope.launch(Dispatchers.IO) {
            var attempt = 0
            while (isActive) {
                var socket: Socket? = null
                try {
                    val freshDevice = connectedDeviceDao.getDeviceById(device.deviceId) ?: device
                    socket = Socket()
                    socket.tcpNoDelay = true
                    socket.connect(InetSocketAddress(freshDevice.ipAddress, freshDevice.port), 4000)
                    socket.soTimeout = 0 // Keep connection open indefinitely

                    val reader = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.UTF_8))
                    val writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream(), Charsets.UTF_8))

                    // Read challenge
                    val challengeLine = reader.readLine() ?: throw Exception("Peer closed connection prematurely")
                    val challengeJson = JSONObject(challengeLine)
                    val nonce = challengeJson.optString("nonce")

                    val secret = freshDevice.sharedSecret.ifBlank { freshDevice.authToken }
                    val hmac = CryptoUtil.computeHmac(secret, nonce)

                    val authReq = JSONObject().apply {
                        put("type", "AUTH")
                        put("deviceId", preferences.getLocalDeviceId())
                        put("hmac", hmac)
                    }.toString()
                    writer.write(authReq + "\n")
                    writer.flush()

                    val authResultLine = reader.readLine() ?: throw Exception("Auth dropped")
                    val authResultJson = JSONObject(authResultLine)
                    if (authResultJson.optString("type") != "AUTH_OK") {
                        throw Exception("Authentication rejected by ${freshDevice.name}")
                    }

                    activeSockets[device.deviceId]?.let { try { it.close() } catch (_: Exception) {} }
                    activeSockets[device.deviceId] = socket
                    attempt = 0 // Reset backoff

                    connectedDeviceDao.updateStatusAndTransport(
                        device.deviceId,
                        isOnline = true,
                        status = "Connected (Live Socket)",
                        transport = "Wi-Fi (TCP Socket)"
                    )

                    onLogEvent(
                        SyncActivityEvent(
                            title = "Connected to ${freshDevice.name}",
                            description = "Live TCP socket established to ${freshDevice.ipAddress}:${freshDevice.port}",
                            isOutgoing = true,
                            transport = "Wi-Fi (TCP)"
                        )
                    )

                    // Continuous reader loop
                    while (isActive && !socket.isClosed) {
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
                                    onClipReceived(clip, device.deviceId)
                                }
                            }
                            "IP_UPDATE" -> {
                                val newIp = frame.optString("newIp", "")
                                val newPort = frame.optInt("newPort", 8765)
                                if (newIp.isNotBlank()) {
                                    connectedDeviceDao.updateAddressAndPort(device.deviceId, newIp, newPort)
                                }
                            }
                            "PING" -> {
                                writer.write(JSONObject().apply { put("type", "PONG") }.toString() + "\n")
                                writer.flush()
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "Socket to ${device.name} dropped: ${e.message}")
                } finally {
                    activeSockets.remove(device.deviceId)
                    try { socket?.close() } catch (_: Exception) {}
                    connectedDeviceDao.updateStatusAndTransport(
                        device.deviceId,
                        isOnline = false,
                        status = "Auto-reconnecting...",
                        transport = "Wi-Fi (TCP)"
                    )
                }

                attempt++
                val delayMs = minOf(3000L * attempt, 15000L)
                delay(delayMs)
            }
        }
    }

    /**
     * Broadcasts a clip frame over an already-open socket.
     * Returns true if sent through an open socket.
     */
    fun sendClipThroughOpenSocket(deviceId: String, payloadJson: String): Boolean {
        val socket = activeSockets[deviceId] ?: return false
        return try {
            val writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream(), Charsets.UTF_8))
            val frame = JSONObject().apply {
                put("type", "CLIP")
                put("payload", JSONObject(payloadJson))
            }.toString()
            writer.write(frame + "\n")
            writer.flush()
            true
        } catch (_: Exception) {
            activeSockets.remove(deviceId)
            false
        }
    }

    /**
     * Pushes IP update frame to all actively connected peer sockets.
     */
    fun pushIpUpdate(newIp: String, newPort: Int) {
        val frame = JSONObject().apply {
            put("type", "IP_UPDATE")
            put("deviceId", preferences.getLocalDeviceId())
            put("newIp", newIp)
            put("newPort", newPort)
        }.toString() + "\n"

        for ((_, socket) in activeSockets) {
            try {
                val writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream(), Charsets.UTF_8))
                writer.write(frame)
                writer.flush()
            } catch (_: Exception) {}
        }
    }

    fun dropAllSockets() {
        for ((_, socket) in activeSockets) {
            try { socket.close() } catch (_: Exception) {}
        }
        activeSockets.clear()
        for ((_, job) in reconnectJobs) {
            job.cancel()
        }
        reconnectJobs.clear()
    }
}

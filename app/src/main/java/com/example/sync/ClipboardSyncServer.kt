package com.example.sync

import android.content.Context
import android.util.Base64
import com.example.data.ConnectedDeviceDao
import com.example.data.ConnectedDeviceEntity
import com.example.data.SnippetRepository
import com.example.ui.keyboard.util.KeyboardPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket

class ClipboardSyncServer(
    private val context: Context,
    private val preferences: KeyboardPreferences,
    private val snippetRepository: SnippetRepository,
    private val connectedDeviceDao: ConnectedDeviceDao,
    private val syncManager: ClipboardSyncManager
) {
    private val serverScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var serverSocket: ServerSocket? = null
    @Volatile
    private var isRunning = false

    val activePort: Int
        get() = serverSocket?.localPort ?: 8765

    fun start(port: Int = 8765) {
        if (isRunning) return
        serverScope.launch {
            try {
                // Try preferred port or fall back to any available ephemeral port
                serverSocket = try {
                    ServerSocket(port)
                } catch (_: Exception) {
                    ServerSocket(0)
                }
                isRunning = true

                while (isRunning) {
                    val clientSocket = try {
                        serverSocket?.accept()
                    } catch (_: Exception) {
                        null
                    } ?: break

                    serverScope.launch {
                        handleClient(clientSocket)
                    }
                }
            } catch (_: Exception) {
            } finally {
                isRunning = false
            }
        }
    }

    fun stop() {
        isRunning = false
        try {
            serverSocket?.close()
        } catch (_: Exception) {}
        serverSocket = null
    }

    private suspend fun handleClient(socket: Socket) {
        var keepSocketAlive = false
        try {
            socket.soTimeout = 8000
            val reader = BufferedReader(InputStreamReader(socket.getInputStream(), "UTF-8"))
            val output = socket.getOutputStream()

            val requestLine = reader.readLine() ?: return

            // Check if this is a raw socket stream connection
            if (requestLine.startsWith("{") || requestLine.startsWith("KEYPRO_STREAM")) {
                keepSocketAlive = true
                syncManager.persistentSocketManager.handleIncomingStream(socket)
                return
            }

            val parts = requestLine.split(" ")
            if (parts.size < 2) {
                sendResponse(output, 400, "Bad Request", "{}")
                return
            }

            val method = parts[0].uppercase()
            val path = parts[1]

            // If client is upgrading to persistent streaming socket
            if (path == "/api/sync/stream") {
                keepSocketAlive = true
                val upgradeHeader = "HTTP/1.1 200 OK\r\nConnection: keep-alive\r\nContent-Type: application/octet-stream\r\n\r\n"
                output.write(upgradeHeader.toByteArray(Charsets.UTF_8))
                output.flush()
                syncManager.persistentSocketManager.handleIncomingStream(socket)
                return
            }

            // Read headers
            var contentLength = 0
            var line: String? = reader.readLine()
            while (!line.isNullOrBlank()) {
                val headerLower = line.lowercase()
                if (headerLower.startsWith("content-length:")) {
                    contentLength = headerLower.substringAfter("content-length:").trim().toIntOrNull() ?: 0
                }
                line = reader.readLine()
            }

            // Read body if present
            val bodyBuilder = StringBuilder()
            if (contentLength > 0) {
                val buffer = CharArray(1024)
                var bytesReadTotal = 0
                while (bytesReadTotal < contentLength) {
                    val toRead = minOf(buffer.size, contentLength - bytesReadTotal)
                    val read = reader.read(buffer, 0, toRead)
                    if (read == -1) break
                    bodyBuilder.append(buffer, 0, read)
                    bytesReadTotal += read
                }
            }
            val body = bodyBuilder.toString()

            when {
                path.startsWith("/api/ping") -> {
                    val nonce = CryptoUtil.generateNonce(16)
                    val secret = preferences.getLocalSharedSecret()
                    val hmac = CryptoUtil.computeHmac(secret, nonce)
                    val responseJson = JSONObject().apply {
                        put("status", "ok")
                        put("deviceId", preferences.getLocalDeviceId())
                        put("deviceName", preferences.localDeviceName.value)
                        put("port", activePort)
                        put("nonce", nonce)
                        put("hmac", hmac)
                        put("version", 2)
                        put("timestamp", System.currentTimeMillis())
                    }.toString()
                    sendResponse(output, 200, "OK", responseJson)
                }

                path == "/api/pair" && method == "POST" -> {
                    handlePairRequest(body, output)
                }

                path == "/api/sync/push" && method == "POST" -> {
                    handleSyncPush(body, output)
                }

                else -> {
                    sendResponse(output, 404, "Not Found", "{\"error\":\"Not Found\"}")
                }
            }
        } catch (_: Exception) {
        } finally {
            if (!keepSocketAlive) {
                try {
                    socket.close()
                } catch (_: Exception) {}
            }
        }
    }

    private suspend fun handlePairRequest(body: String, output: OutputStream) {
        try {
            val json = JSONObject(body)
            val remoteDeviceId = json.optString("deviceId")
            val remoteDeviceName = json.optString("deviceName", "Remote Device")
            val remoteIp = json.optString("ip")
            val remotePort = json.optInt("port", 8765)
            val remoteSecret = json.optString("sharedSecret", json.optString("token", ""))
            val remoteBtMac = json.optString("bluetoothMac", "")

            if (remoteDeviceId.isNotBlank() && remoteIp.isNotBlank()) {
                // Register device in local DB with shared secret
                val entity = ConnectedDeviceEntity(
                    deviceId = remoteDeviceId,
                    name = remoteDeviceName,
                    ipAddress = remoteIp,
                    port = remotePort,
                    authToken = remoteSecret,
                    sharedSecret = remoteSecret,
                    bluetoothMac = remoteBtMac,
                    isOnline = true,
                    connectionStatus = "Connected (Paired)",
                    transportType = if (remoteBtMac.isNotBlank()) "Wi-Fi & Bluetooth" else "Wi-Fi (P2P)",
                    lastSyncedTimestamp = System.currentTimeMillis()
                )
                connectedDeviceDao.insertOrUpdate(entity)

                syncManager.logEvent(
                    SyncActivityEvent(
                        title = "Paired with $remoteDeviceName",
                        description = "Connected via Wi-Fi ($remoteIp:$remotePort) & Shared Secret",
                        isOutgoing = false,
                        transport = "Wi-Fi (P2P)"
                    )
                )

                // Start persistent socket stream connection immediately
                syncManager.persistentSocketManager.startPersistentConnection(entity)

                val responseJson = JSONObject().apply {
                    put("status", "paired")
                    put("deviceId", preferences.getLocalDeviceId())
                    put("deviceName", preferences.localDeviceName.value)
                    put("sharedSecret", preferences.getLocalSharedSecret())
                    put("bluetoothMac", preferences.getLocalBluetoothMac())
                }.toString()
                sendResponse(output, 200, "OK", responseJson)
            } else {
                sendResponse(output, 400, "Bad Request", "{\"error\":\"Missing deviceId or ip\"}")
            }
        } catch (_: Exception) {
            sendResponse(output, 500, "Internal Server Error", "{\"error\":\"Pairing failed\"}")
        }
    }

    private suspend fun handleSyncPush(body: String, output: OutputStream) {
        try {
            val json = JSONObject(body)
            val syncId = json.optString("syncId")
            val originDeviceId = json.optString("originDeviceId")
            val title = json.optString("title", "Synced Clip")
            val content = json.optString("content", "")
            val isPinned = json.optBoolean("isPinned", false)
            val category = json.optString("category", "Synced")
            val timestamp = json.optLong("timestamp", System.currentTimeMillis())
            val isImage = json.optBoolean("isImage", false)
            val mimeType = json.optString("mimeType", if (isImage) "image/jpeg" else "text/plain")
            val imageBase64 = json.optString("imageBase64", "")

            val localDeviceId = preferences.getLocalDeviceId()

            // 1. Check if originated from local device (Cyclic loop prevention)
            if (originDeviceId == localDeviceId) {
                syncManager.logEvent(
                    SyncActivityEvent(
                        title = "Cyclic Sync Prevented",
                        description = "Dropped clip originated from this device ($syncId)",
                        isOutgoing = false,
                        isLoopPrevented = true,
                        transport = "Wi-Fi"
                    )
                )
                sendResponse(output, 200, "OK", "{\"status\":\"ignored_origin_local\"}")
                return
            }

            // 2. Check if this syncId was already processed (Echo loop prevention)
            if (syncId.isNotBlank() && syncManager.isSyncIdProcessed(syncId)) {
                syncManager.logEvent(
                    SyncActivityEvent(
                        title = "Duplicate Clip Prevented",
                        description = "Clip with syncId $syncId already processed",
                        isOutgoing = false,
                        isLoopPrevented = true,
                        transport = "Wi-Fi"
                    )
                )
                sendResponse(output, 200, "OK", "{\"status\":\"ignored_duplicate\"}")
                return
            }

            // Mark as processed immediately
            if (syncId.isNotBlank()) {
                syncManager.markSyncIdProcessed(syncId)
            }

            var localImageUri: String? = null
            if (isImage && imageBase64.isNotBlank() && preferences.clipboardSyncImages.value) {
                try {
                    val bytes = Base64.decode(imageBase64, Base64.DEFAULT)
                    val syncedDir = File(context.filesDir, "synced_clips").apply { mkdirs() }
                    val extension = if (mimeType.contains("png")) "png" else if (mimeType.contains("webp")) "webp" else "jpg"
                    val file = File(syncedDir, "clip_${syncId.take(8)}_${System.currentTimeMillis()}.$extension")
                    FileOutputStream(file).use { fos ->
                        fos.write(bytes)
                    }
                    localImageUri = file.absolutePath
                } catch (_: Exception) {}
            }

            // Save directly to Room DB without touching system clipboard
            snippetRepository.saveSyncedSnippet(
                title = title,
                content = content,
                isPinned = isPinned,
                category = "Synced",
                timestamp = timestamp,
                imageUri = localImageUri,
                isImage = isImage,
                mimeType = mimeType,
                syncId = syncId,
                originDeviceId = originDeviceId
            )

            // Update peer device last synced timestamp
            if (originDeviceId.isNotBlank()) {
                connectedDeviceDao.updateStatus(originDeviceId, true, System.currentTimeMillis())
            }

            syncManager.logEvent(
                SyncActivityEvent(
                    title = if (isImage) "Received Image Clip" else "Received Text Clip",
                    description = if (isImage) "Image received from device $originDeviceId" else "'${content.take(30)}'",
                    isOutgoing = false,
                    isImage = isImage,
                    transport = "Wi-Fi (P2P)"
                )
            )

            sendResponse(output, 200, "OK", "{\"status\":\"received\",\"syncId\":\"$syncId\"}")
        } catch (_: Exception) {
            sendResponse(output, 500, "Internal Server Error", "{\"error\":\"Failed to save sync payload\"}")
        }
    }

    private fun sendResponse(output: OutputStream, code: Int, statusText: String, json: String) {
        val bytes = json.toByteArray(Charsets.UTF_8)
        val header = "HTTP/1.1 $code $statusText\r\n" +
                "Content-Type: application/json; charset=utf-8\r\n" +
                "Content-Length: ${bytes.size}\r\n" +
                "Connection: close\r\n\r\n"
        output.write(header.toByteArray(Charsets.UTF_8))
        output.write(bytes)
        output.flush()
    }
}

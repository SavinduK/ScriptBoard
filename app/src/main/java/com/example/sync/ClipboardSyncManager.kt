package com.example.sync

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.example.data.AppDatabase
import com.example.data.ConnectedDeviceDao
import com.example.data.ConnectedDeviceEntity
import com.example.data.SnippetEntity
import com.example.data.SnippetRepository
import com.example.ui.keyboard.util.KeyboardPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Collections
import java.util.LinkedHashSet
import java.util.UUID
import java.util.concurrent.TimeUnit

class ClipboardSyncManager private constructor(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val preferences = KeyboardPreferences.getInstance(context)
    private val database = AppDatabase.getDatabase(context)
    val snippetRepository = SnippetRepository(database.snippetDao())
    val connectedDeviceDao: ConnectedDeviceDao = database.connectedDeviceDao()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .writeTimeout(5, TimeUnit.SECONDS)
        .build()

    // Activity and logging stream
    private val _syncActivityLog = MutableStateFlow<List<SyncActivityEvent>>(emptyList())
    val syncActivityLog: StateFlow<List<SyncActivityEvent>> = _syncActivityLog.asStateFlow()

    private val _isServerRunning = MutableStateFlow(false)
    val isServerRunning: StateFlow<Boolean> = _isServerRunning.asStateFlow()

    // In-memory cache of processed sync IDs to prevent echo and cyclic loops
    private val processedSyncIds = Collections.synchronizedSet(LinkedHashSet<String>())
    private val maxCacheSize = 300

    val persistentSocketManager = PersistentSocketManager(
        context = context,
        scope = scope,
        preferences = preferences,
        connectedDeviceDao = connectedDeviceDao,
        onClipReceived = { clip, deviceId -> handleIncomingClip(clip, deviceId, "Wi-Fi (TCP Socket)") },
        onLogEvent = { logEvent(it) }
    )

    val bluetoothSyncHelper = BluetoothSyncHelper(
        context = context,
        scope = scope,
        preferences = preferences,
        connectedDeviceDao = connectedDeviceDao,
        onClipReceived = { clip, deviceId -> handleIncomingClip(clip, deviceId, "Bluetooth RFCOMM") },
        onLogEvent = { logEvent(it) }
    )

    private val nsdHelper = NsdHelper(
        context = context,
        scope = scope,
        preferences = preferences,
        connectedDeviceDao = connectedDeviceDao,
        onPeerDiscovered = { deviceId, ip, port ->
            scope.launch {
                val dev = connectedDeviceDao.getDeviceById(deviceId)
                if (dev != null) {
                    persistentSocketManager.startPersistentConnection(dev)
                    logEvent(
                        SyncActivityEvent(
                            title = "Peer Discovered via mDNS",
                            description = "Re-connected to ${dev.name} at $ip:$port",
                            isOutgoing = false,
                            transport = "mDNS / NSD"
                        )
                    )
                }
            }
        }
    )

    private val networkChangeMonitor = NetworkChangeMonitor(context) { newIp ->
        onLocalNetworkChanged(newIp)
    }

    val subnetScanner = SubnetScanner(connectedDeviceDao) { device, newIp, port ->
        scope.launch {
            persistentSocketManager.startPersistentConnection(device)
            logEvent(
                SyncActivityEvent(
                    title = "Subnet Fallback Discovery",
                    description = "Authenticated and re-connected to ${device.name} at $newIp:$port",
                    isOutgoing = false,
                    transport = "Subnet /24 Scan"
                )
            )
        }
    }

    val server = ClipboardSyncServer(
        context = context,
        preferences = preferences,
        snippetRepository = snippetRepository,
        connectedDeviceDao = connectedDeviceDao,
        syncManager = this
    )

    init {
        startServices()
    }

    fun startServices() {
        if (!_isServerRunning.value) {
            server.start(8765)
            _isServerRunning.value = true
            logEvent(
                SyncActivityEvent(
                    title = "KeyPro Sync Engine Ready",
                    description = "Listening on port ${server.activePort} (Wi-Fi mDNS & Bluetooth RFCOMM)",
                    isOutgoing = false,
                    transport = "Daemon"
                )
            )
        }

        // Register mDNS and start discovery
        if (preferences.wifiSyncEnabled.value) {
            nsdHelper.registerService(server.activePort)
            nsdHelper.startDiscovery()
            networkChangeMonitor.start()
        }

        // Start Bluetooth listener
        if (preferences.bluetoothSyncEnabled.value) {
            bluetoothSyncHelper.startListening()
        }

        // Connect persistent sockets to already paired devices
        connectToPairedDevices()

        // Start persistent foreground service if enabled
        if (preferences.persistentSyncServiceEnabled.value) {
            ClipboardSyncForegroundService.start(context)
        }
    }

    fun connectToPairedDevices() {
        scope.launch {
            val paired = connectedDeviceDao.getAllDevicesList()
            for (device in paired) {
                if (device.ipAddress.isNotBlank()) {
                    persistentSocketManager.startPersistentConnection(device)
                }
                if (device.bluetoothMac.isNotBlank()) {
                    bluetoothSyncHelper.connectToDevice(device)
                }
            }
        }
    }

    private fun onLocalNetworkChanged(newIp: String) {
        logEvent(
            SyncActivityEvent(
                title = "Local IP Changed",
                description = "New network address: $newIp. Pushing update to peers.",
                isOutgoing = true,
                transport = "Network Monitor"
            )
        )
        // 1. Re-register NSD service
        nsdHelper.unregisterService()
        nsdHelper.registerService(server.activePort)

        // 2. Push IP update over existing sockets
        persistentSocketManager.pushIpUpdate(newIp, server.activePort)

        // 3. Drop stale sockets and reconnect
        persistentSocketManager.dropAllSockets()
        connectToPairedDevices()
    }

    fun onBluetoothDeviceConnected(mac: String) {
        scope.launch {
            val dev = connectedDeviceDao.getDeviceByMac(mac)
            if (dev != null) {
                bluetoothSyncHelper.connectToDevice(dev)
            }
        }
    }

    fun onBluetoothDeviceDisconnected(mac: String) {
        scope.launch {
            val dev = connectedDeviceDao.getDeviceByMac(mac)
            if (dev != null) {
                connectedDeviceDao.updateStatusAndTransport(
                    dev.deviceId,
                    isOnline = false,
                    status = "Standby (Disconnected)",
                    transport = "Bluetooth"
                )
            }
        }
    }

    fun restartBluetoothListener() {
        bluetoothSyncHelper.startListening()
    }

    fun triggerSubnetScan() {
        scope.launch {
            val localIp = getLocalIpAddress()
            logEvent(
                SyncActivityEvent(
                    title = "Scanning Local Subnet",
                    description = "Probing /24 subnet for paired peers with HMAC verification...",
                    isOutgoing = true,
                    transport = "Subnet /24 Scan"
                )
            )
            val found = subnetScanner.scanSubnet(localIp, server.activePort)
            logEvent(
                SyncActivityEvent(
                    title = "Subnet Scan Finished",
                    description = "Probed subnet. Authenticated $found paired device(s).",
                    isOutgoing = false,
                    transport = "Subnet /24 Scan"
                )
            )
        }
    }

    fun getActivePort(): Int = server.activePort

    fun getLocalIpAddress(): String {
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                if (intf.isLoopback || !intf.isUp) continue
                val addrs = Collections.list(intf.inetAddresses)
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        val ip = addr.hostAddress ?: ""
                        if (!ip.startsWith("127.")) {
                            return ip
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        return "192.168.1.100"
    }

    fun markSyncIdProcessed(syncId: String) {
        if (syncId.isBlank()) return
        synchronized(processedSyncIds) {
            if (processedSyncIds.size > maxCacheSize) {
                val it = processedSyncIds.iterator()
                if (it.hasNext()) {
                    it.next()
                    it.remove()
                }
            }
            processedSyncIds.add(syncId)
        }
    }

    fun isSyncIdProcessed(syncId: String): Boolean {
        if (syncId.isBlank()) return false
        return processedSyncIds.contains(syncId)
    }

    fun logEvent(event: SyncActivityEvent) {
        val current = _syncActivityLog.value.toMutableList()
        current.add(0, event)
        _syncActivityLog.value = current.take(50)
    }

    /**
     * Handles an incoming synced clip from any transport (Persistent TCP Socket, Bluetooth RFCOMM, or HTTP).
     * Enforces cyclic sync prevention and inserts directly into the keyboard's own database.
     * Never touches system clipboard, avoiding OS clip event loops.
     */
    fun handleIncomingClip(clip: SyncClipPayload, fromDeviceId: String, transport: String) {
        val localDeviceId = preferences.getLocalDeviceId()

        // 1. Prevent cyclic sync if originated from this device
        if (clip.originDeviceId == localDeviceId) {
            logEvent(
                SyncActivityEvent(
                    title = "Cyclic Sync Prevented",
                    description = "Dropped clip originated from this device (${clip.syncId.take(8)})",
                    isOutgoing = false,
                    isLoopPrevented = true,
                    transport = transport
                )
            )
            return
        }

        // 2. Prevent duplicate processing
        if (isSyncIdProcessed(clip.syncId)) {
            logEvent(
                SyncActivityEvent(
                    title = "Duplicate Clip Ignored",
                    description = "Clip with syncId ${clip.syncId.take(8)} already recorded",
                    isOutgoing = false,
                    isLoopPrevented = true,
                    transport = transport
                )
            )
            return
        }
        markSyncIdProcessed(clip.syncId)

        scope.launch {
            var localImageUri: String? = null
            if (clip.isImage && !clip.imageBase64.isNullOrBlank() && preferences.clipboardSyncImages.value) {
                try {
                    val bytes = Base64.decode(clip.imageBase64, Base64.DEFAULT)
                    val syncedDir = File(context.filesDir, "synced_clips").apply { mkdirs() }
                    val extension = if (clip.mimeType?.contains("png") == true) "png" else if (clip.mimeType?.contains("webp") == true) "webp" else "jpg"
                    val file = File(syncedDir, "clip_${clip.syncId.take(8)}_${System.currentTimeMillis()}.$extension")
                    FileOutputStream(file).use { fos ->
                        fos.write(bytes)
                    }
                    localImageUri = file.absolutePath
                } catch (_: Exception) {}
            }

            // Save to keyboard database without touching Android system clipboard
            snippetRepository.saveSyncedSnippet(
                title = clip.title,
                content = clip.content,
                isPinned = clip.isPinned,
                category = "Synced",
                timestamp = clip.timestamp,
                imageUri = localImageUri,
                isImage = clip.isImage,
                mimeType = clip.mimeType,
                syncId = clip.syncId,
                originDeviceId = clip.originDeviceId
            )

            connectedDeviceDao.updateStatus(fromDeviceId, true, System.currentTimeMillis())

            logEvent(
                SyncActivityEvent(
                    title = if (clip.isImage) "Received Image Clip" else "Received Text Clip",
                    description = if (clip.isImage) "Image received from peer ($fromDeviceId)" else "'${clip.content.take(32)}'",
                    isOutgoing = false,
                    isImage = clip.isImage,
                    transport = transport
                )
            )
        }
    }

    /**
     * Broadcasts a snippet to all connected peer devices.
     * Writes to already-open sockets (TCP stream or Bluetooth RFCOMM) with fallback to HTTP push.
     */
    fun broadcastSnippet(snippet: SnippetEntity, customImageBytes: ByteArray? = null) {
        if (!preferences.clipboardSyncEnabled.value) return

        val localDeviceId = preferences.getLocalDeviceId()

        // Never re-broadcast clips originated from peers
        if (snippet.originDeviceId.isNotBlank() && snippet.originDeviceId != localDeviceId) {
            logEvent(
                SyncActivityEvent(
                    title = "Cyclic Sync Blocked",
                    description = "Skipped re-broadcasting peer-originated clip (${snippet.syncId.take(8)})",
                    isOutgoing = true,
                    isLoopPrevented = true,
                    transport = "Loop Filter"
                )
            )
            return
        }

        val syncId = if (snippet.syncId.isNotBlank()) snippet.syncId else UUID.randomUUID().toString()
        markSyncIdProcessed(syncId)

        scope.launch {
            val devices = connectedDeviceDao.getAllDevicesList()
            if (devices.isEmpty()) return@launch

            // Prepare image payload if image
            var imageBase64: String? = null
            if (snippet.isImage && preferences.clipboardSyncImages.value) {
                imageBase64 = customImageBytes?.let { Base64.encodeToString(it, Base64.NO_WRAP) }
                    ?: readImageAsBase64(snippet.imageUri)
            }

            val payloadJson = JSONObject().apply {
                put("syncId", syncId)
                put("originDeviceId", localDeviceId)
                put("title", snippet.title)
                put("content", snippet.content)
                put("isPinned", snippet.isPinned)
                put("category", snippet.category)
                put("timestamp", snippet.timestamp)
                put("isImage", snippet.isImage)
                put("mimeType", snippet.mimeType ?: if (snippet.isImage) "image/jpeg" else "text/plain")
                if (imageBase64 != null) {
                    put("imageBase64", imageBase64)
                }
            }.toString()

            for (device in devices) {
                if (snippet.isImage && !device.syncImages) continue
                if (!snippet.isImage && !device.syncText) continue

                var sent = false

                // 1. Try sending over already-open TCP streaming socket
                if (persistentSocketManager.sendClipThroughOpenSocket(device.deviceId, payloadJson)) {
                    sent = true
                    connectedDeviceDao.updateStatus(device.deviceId, true, System.currentTimeMillis())
                    logEvent(
                        SyncActivityEvent(
                            title = if (snippet.isImage) "Sent Image (Live Socket)" else "Sent Text (Live Socket)",
                            description = "Streamed to ${device.name} via persistent TCP socket",
                            isOutgoing = true,
                            isImage = snippet.isImage,
                            transport = "Wi-Fi (TCP Socket)"
                        )
                    )
                }

                // 2. Try sending over already-open Bluetooth RFCOMM socket
                if (!sent && device.bluetoothMac.isNotBlank()) {
                    if (bluetoothSyncHelper.sendClip(device.deviceId, payloadJson)) {
                        sent = true
                        connectedDeviceDao.updateStatus(device.deviceId, true, System.currentTimeMillis())
                        logEvent(
                            SyncActivityEvent(
                                title = if (snippet.isImage) "Sent Image (Bluetooth)" else "Sent Text (Bluetooth)",
                                description = "Streamed to ${device.name} via Bluetooth RFCOMM",
                                isOutgoing = true,
                                isImage = snippet.isImage,
                                transport = "Bluetooth RFCOMM"
                            )
                        )
                    }
                }

                // 3. Fallback: HTTP Push
                if (!sent && device.ipAddress.isNotBlank()) {
                    try {
                        val mediaType = "application/json; charset=utf-8".toMediaType()
                        val url = "http://${device.ipAddress}:${device.port}/api/sync/push"
                        val request = Request.Builder()
                            .url(url)
                            .post(payloadJson.toRequestBody(mediaType))
                            .build()

                        val response = okHttpClient.newCall(request).execute()
                        if (response.isSuccessful) {
                            connectedDeviceDao.updateStatus(device.deviceId, true, System.currentTimeMillis())
                            logEvent(
                                SyncActivityEvent(
                                    title = if (snippet.isImage) "Synced Image (HTTP)" else "Synced Text (HTTP)",
                                    description = "Delivered to ${device.name} (${device.ipAddress})",
                                    isOutgoing = true,
                                    isImage = snippet.isImage,
                                    transport = "Wi-Fi (HTTP)"
                                )
                            )
                        } else {
                            connectedDeviceDao.updateStatus(device.deviceId, false)
                        }
                        response.close()
                    } catch (_: Exception) {
                        connectedDeviceDao.updateStatus(device.deviceId, false)
                    }
                }
            }
        }
    }

    private fun readImageAsBase64(imageUri: String?): String? {
        if (imageUri.isNullOrBlank()) return null
        return try {
            val fileCheck = File(imageUri)
            val stream: InputStream? = if (fileCheck.exists() && fileCheck.isFile) {
                fileCheck.inputStream()
            } else if (imageUri.startsWith("content://") || imageUri.startsWith("file://") || imageUri.startsWith("android.resource://")) {
                context.contentResolver.openInputStream(Uri.parse(imageUri))
            } else {
                File(imageUri).inputStream()
            }
            stream?.use { input ->
                val originalBitmap = BitmapFactory.decodeStream(input) ?: return null
                val scaled = if (originalBitmap.width > 800 || originalBitmap.height > 800) {
                    val scale = 800f / maxOf(originalBitmap.width, originalBitmap.height)
                    Bitmap.createScaledBitmap(
                        originalBitmap,
                        (originalBitmap.width * scale).toInt(),
                        (originalBitmap.height * scale).toInt(),
                        true
                    )
                } else originalBitmap

                val byteStream = ByteArrayOutputStream()
                scaled.compress(Bitmap.CompressFormat.JPEG, 75, byteStream)
                Base64.encodeToString(byteStream.toByteArray(), Base64.NO_WRAP)
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Connects to a remote peer device given PairingPayload (from QR code or manual entry).
     */
    suspend fun pairWithDevice(
        ip: String,
        port: Int,
        sharedSecret: String,
        givenName: String = "",
        bluetoothMac: String = ""
    ): Result<ConnectedDeviceEntity> = withContext(Dispatchers.IO) {
        try {
            val localDeviceId = preferences.getLocalDeviceId()
            val localName = preferences.localDeviceName.value
            val localIp = getLocalIpAddress()
            val localSecret = preferences.getLocalSharedSecret()
            val localBtMac = preferences.getLocalBluetoothMac()

            val pairPayload = JSONObject().apply {
                put("deviceId", localDeviceId)
                put("deviceName", localName)
                put("ip", localIp)
                put("port", server.activePort)
                put("sharedSecret", localSecret)
                put("token", localSecret)
                put("bluetoothMac", localBtMac)
            }.toString()

            val request = Request.Builder()
                .url("http://$ip:$port/api/pair")
                .post(pairPayload.toRequestBody("application/json".toMediaType()))
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val respBody = response.body?.string() ?: "{}"
                val json = JSONObject(respBody)
                val remoteDeviceId = json.optString("deviceId", "dev_${ip.replace(".", "_")}")
                val remoteName = json.optString("deviceName", givenName.ifEmpty { "Device ($ip)" })
                val remoteSecret = json.optString("sharedSecret", sharedSecret)
                val remoteBt = json.optString("bluetoothMac", bluetoothMac)

                val entity = ConnectedDeviceEntity(
                    deviceId = remoteDeviceId,
                    name = remoteName,
                    ipAddress = ip,
                    port = port,
                    authToken = remoteSecret,
                    sharedSecret = remoteSecret,
                    bluetoothMac = remoteBt,
                    isOnline = true,
                    connectionStatus = "Connected (Paired)",
                    transportType = if (remoteBt.isNotBlank()) "Wi-Fi & Bluetooth" else "Wi-Fi (TCP)",
                    lastSyncedTimestamp = System.currentTimeMillis()
                )
                connectedDeviceDao.insertOrUpdate(entity)
                logEvent(
                    SyncActivityEvent(
                        title = "Paired with $remoteName",
                        description = "Handshake authenticated with shared secret. Connecting live streams...",
                        isOutgoing = true,
                        transport = "Wi-Fi (TCP)"
                    )
                )

                // Establish live streaming socket
                persistentSocketManager.startPersistentConnection(entity)
                if (remoteBt.isNotBlank()) {
                    bluetoothSyncHelper.connectToDevice(entity)
                }

                response.close()
                Result.success(entity)
            } else {
                response.close()
                Result.failure(Exception("Pairing rejected with code ${response.code}"))
            }
        } catch (e: Exception) {
            // If offline, store paired device anyway as pending peer
            val remoteDeviceId = "dev_${ip.replace(".", "_")}"
            val entity = ConnectedDeviceEntity(
                deviceId = remoteDeviceId,
                name = givenName.ifEmpty { "Device ($ip)" },
                ipAddress = ip,
                port = port,
                authToken = sharedSecret,
                sharedSecret = sharedSecret,
                bluetoothMac = bluetoothMac,
                isOnline = false,
                connectionStatus = "Standby (Pending peer online)",
                transportType = "Wi-Fi / Bluetooth",
                lastSyncedTimestamp = System.currentTimeMillis()
            )
            connectedDeviceDao.insertOrUpdate(entity)
            logEvent(
                SyncActivityEvent(
                    title = "Registered Device ($ip)",
                    description = "Added to paired list. Auto-reconnect active.",
                    isOutgoing = true,
                    transport = "Wi-Fi"
                )
            )
            persistentSocketManager.startPersistentConnection(entity)
            Result.success(entity)
        }
    }

    /**
     * Creates a simulated connected device for immediate testing in single-device or emulator environments.
     */
    suspend fun simulateTestDevicePairing() = withContext(Dispatchers.IO) {
        val testDeviceId = "sim_samsung_s24_" + (1000..9999).random()
        val testSecret = "sim_secret_samsung_galaxy_" + (1000..9999).random()
        val testDevice = ConnectedDeviceEntity(
            deviceId = testDeviceId,
            name = "Samsung Galaxy S24 Ultra",
            ipAddress = "192.168.1.185",
            port = 8765,
            authToken = testSecret,
            sharedSecret = testSecret,
            bluetoothMac = "00:1A:7D:DA:71:13",
            isOnline = true,
            connectionStatus = "Connected (Live Socket)",
            transportType = "Wi-Fi & Bluetooth RFCOMM",
            lastSyncedTimestamp = System.currentTimeMillis()
        )
        connectedDeviceDao.insertOrUpdate(testDevice)

        // Simulate incoming synced text clip
        val testTextSyncId = UUID.randomUUID().toString()
        markSyncIdProcessed(testTextSyncId)
        snippetRepository.saveSyncedSnippet(
            title = "Samsung Notes Link",
            content = "https://developer.samsung.com/continuity-clipboard-sync",
            isPinned = false,
            category = "Synced",
            timestamp = System.currentTimeMillis(),
            imageUri = null,
            isImage = false,
            mimeType = "text/plain",
            syncId = testTextSyncId,
            originDeviceId = testDeviceId
        )

        // Simulate incoming sample image clip
        try {
            val sampleImageFile = File(context.filesDir, "synced_clips").apply { mkdirs() }
            val imgFile = File(sampleImageFile, "sim_test_${System.currentTimeMillis()}.png")
            val testBmp = Bitmap.createBitmap(120, 120, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(testBmp)
            canvas.drawColor(android.graphics.Color.parseColor("#00B4D8"))
            val paint = android.graphics.Paint().apply {
                color = android.graphics.Color.WHITE
                textSize = 28f
                textAlign = android.graphics.Paint.Align.CENTER
            }
            canvas.drawText("SYNC", 60f, 68f, paint)
            FileOutputStream(imgFile).use { fos ->
                testBmp.compress(Bitmap.CompressFormat.PNG, 90, fos)
            }

            val testImgSyncId = UUID.randomUUID().toString()
            markSyncIdProcessed(testImgSyncId)
            snippetRepository.saveSyncedSnippet(
                title = "Galaxy Camera Photo",
                content = imgFile.absolutePath,
                isPinned = false,
                category = "Synced",
                timestamp = System.currentTimeMillis() + 10,
                imageUri = imgFile.absolutePath,
                isImage = true,
                mimeType = "image/png",
                syncId = testImgSyncId,
                originDeviceId = testDeviceId
            )
        } catch (_: Exception) {}

        logEvent(
            SyncActivityEvent(
                title = "Paired with ${testDevice.name}",
                description = "Handshake authenticated with HMAC. Live socket connected.",
                isOutgoing = false,
                transport = "Wi-Fi & Bluetooth"
            )
        )
        logEvent(
            SyncActivityEvent(
                title = "Received Text Clip",
                description = "'https://developer.samsung.com...'",
                isOutgoing = false,
                transport = "Wi-Fi (TCP Socket)"
            )
        )
        logEvent(
            SyncActivityEvent(
                title = "Received Image Clip",
                description = "Image clip synced directly into history database",
                isOutgoing = false,
                isImage = true,
                transport = "Bluetooth RFCOMM"
            )
        )
    }

    suspend fun unpairDevice(deviceId: String) = withContext(Dispatchers.IO) {
        connectedDeviceDao.deleteDevice(deviceId)
        logEvent(
            SyncActivityEvent(
                title = "Device Unpaired",
                description = "Removed device $deviceId from sync mesh",
                isOutgoing = true,
                transport = "Mesh"
            )
        )
    }

    companion object {
        @Volatile
        private var instance: ClipboardSyncManager? = null

        fun getInstance(context: Context): ClipboardSyncManager {
            return instance ?: synchronized(this) {
                instance ?: ClipboardSyncManager(context.applicationContext).also { instance = it }
            }
        }
    }
}

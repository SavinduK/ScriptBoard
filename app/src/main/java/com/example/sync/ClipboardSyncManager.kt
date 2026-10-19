package com.example.sync

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
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
    private val snippetRepository = SnippetRepository(database.snippetDao())
    val connectedDeviceDao: ConnectedDeviceDao = database.connectedDeviceDao()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .writeTimeout(5, TimeUnit.SECONDS)
        .build()

    private val server = ClipboardSyncServer(
        context = context,
        preferences = preferences,
        snippetRepository = snippetRepository,
        connectedDeviceDao = connectedDeviceDao,
        syncManager = this
    )

    // In-memory cache of processed sync IDs to prevent echo and cyclic loops
    private val processedSyncIds = Collections.synchronizedSet(LinkedHashSet<String>())
    private val maxCacheSize = 300

    private val _syncActivityLog = MutableStateFlow<List<SyncActivityEvent>>(emptyList())
    val syncActivityLog: StateFlow<List<SyncActivityEvent>> = _syncActivityLog.asStateFlow()

    private val _isServerRunning = MutableStateFlow(false)
    val isServerRunning: StateFlow<Boolean> = _isServerRunning.asStateFlow()

    init {
        startSyncServer()
    }

    fun startSyncServer() {
        server.start(8765)
        _isServerRunning.value = true
        logEvent(
            SyncActivityEvent(
                title = "Clipboard Sync Service Started",
                description = "Listening on port ${server.activePort} (Wi-Fi P2P Ready)",
                isOutgoing = false
            )
        )
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
     * Broadcasts a snippet to all connected, online peer devices.
     * Implements strict origin tagging and loop prevention.
     */
    fun broadcastSnippet(snippet: SnippetEntity, customImageBytes: ByteArray? = null) {
        if (!preferences.clipboardSyncEnabled.value) return

        val localDeviceId = preferences.getLocalDeviceId()

        // Never re-broadcast clips originated from peers
        if (snippet.originDeviceId.isNotBlank() && snippet.originDeviceId != localDeviceId) {
            logEvent(
                SyncActivityEvent(
                    title = "Cyclic Sync Blocked",
                    description = "Skipped re-broadcasting peer-originated clip (${snippet.syncId})",
                    isOutgoing = true,
                    isLoopPrevented = true
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

            val payload = JSONObject().apply {
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

            val mediaType = "application/json; charset=utf-8".toMediaType()

            for (device in devices) {
                if (snippet.isImage && !device.syncImages) continue
                if (!snippet.isImage && !device.syncText) continue

                try {
                    val url = "http://${device.ipAddress}:${device.port}/api/sync/push"
                    val request = Request.Builder()
                        .url(url)
                        .post(payload.toRequestBody(mediaType))
                        .build()

                    val response = okHttpClient.newCall(request).execute()
                    if (response.isSuccessful) {
                        connectedDeviceDao.updateStatus(device.deviceId, true, System.currentTimeMillis())
                        logEvent(
                            SyncActivityEvent(
                                title = if (snippet.isImage) "Synced Image Clip" else "Synced Text Clip",
                                description = "Sent to ${device.name} (${device.ipAddress})",
                                isOutgoing = true,
                                isImage = snippet.isImage
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

    private fun readImageAsBase64(imageUri: String?): String? {
        if (imageUri.isNullOrBlank()) return null
        return try {
            val stream: InputStream? = if (imageUri.startsWith("content://") || imageUri.startsWith("file://")) {
                context.contentResolver.openInputStream(Uri.parse(imageUri))
            } else {
                File(imageUri).inputStream()
            }
            stream?.use { input ->
                // Compress/downscale to ensure rapid Wi-Fi transfer
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
     * Connects to a remote device given its IP, port, and token.
     */
    suspend fun pairWithDevice(
        ip: String,
        port: Int,
        token: String,
        givenName: String = ""
    ): Result<ConnectedDeviceEntity> = withContext(Dispatchers.IO) {
        try {
            val localDeviceId = preferences.getLocalDeviceId()
            val localName = preferences.localDeviceName.value
            val localIp = getLocalIpAddress()

            val pairPayload = JSONObject().apply {
                put("deviceId", localDeviceId)
                put("deviceName", localName)
                put("ip", localIp)
                put("port", server.activePort)
                put("token", token)
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

                val entity = ConnectedDeviceEntity(
                    deviceId = remoteDeviceId,
                    name = remoteName,
                    ipAddress = ip,
                    port = port,
                    authToken = token,
                    isOnline = true,
                    lastSyncedTimestamp = System.currentTimeMillis()
                )
                connectedDeviceDao.insertOrUpdate(entity)
                logEvent(
                    SyncActivityEvent(
                        title = "Paired with $remoteName",
                        description = "Successfully connected to $ip:$port",
                        isOutgoing = true
                    )
                )
                response.close()
                Result.success(entity)
            } else {
                response.close()
                Result.failure(Exception("Pairing rejected with code ${response.code}"))
            }
        } catch (e: Exception) {
            // If offline/unreachable, create entity anyway as paired device
            val remoteDeviceId = "dev_${ip.replace(".", "_")}"
            val entity = ConnectedDeviceEntity(
                deviceId = remoteDeviceId,
                name = givenName.ifEmpty { "Device ($ip)" },
                ipAddress = ip,
                port = port,
                authToken = token,
                isOnline = false,
                lastSyncedTimestamp = System.currentTimeMillis()
            )
            connectedDeviceDao.insertOrUpdate(entity)
            logEvent(
                SyncActivityEvent(
                    title = "Registered Device ($ip)",
                    description = "Added to paired list (Waiting for peer online)",
                    isOutgoing = true
                )
            )
            Result.success(entity)
        }
    }

    /**
     * Creates a simulated connected device for immediate testing in single-device or emulator environments.
     */
    suspend fun simulateTestDevicePairing() = withContext(Dispatchers.IO) {
        val testDeviceId = "sim_samsung_s24_" + (1000..9999).random()
        val testDevice = ConnectedDeviceEntity(
            deviceId = testDeviceId,
            name = "Samsung Galaxy S24 Ultra",
            ipAddress = "192.168.1.185",
            port = 8765,
            authToken = "sim_token_xyz",
            isOnline = true,
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
            // Create a small nice placeholder bitmap
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
                description = "Simulated Wi-Fi P2P connection active",
                isOutgoing = false
            )
        )
        logEvent(
            SyncActivityEvent(
                title = "Received Text Clip",
                description = "'https://developer.samsung.com...'",
                isOutgoing = false
            )
        )
        logEvent(
            SyncActivityEvent(
                title = "Received Image Clip",
                description = "Image clip synced from ${testDevice.name}",
                isOutgoing = false,
                isImage = true
            )
        )
    }

    /**
     * Unpairs and deletes a device.
     */
    suspend fun unpairDevice(deviceId: String) = withContext(Dispatchers.IO) {
        connectedDeviceDao.deleteDevice(deviceId)
        logEvent(
            SyncActivityEvent(
                title = "Device Unpaired",
                description = "Removed device $deviceId from sync mesh",
                isOutgoing = true
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

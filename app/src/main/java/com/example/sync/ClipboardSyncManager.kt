package com.example.sync

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.Base64
import android.util.Log
import com.example.R
import com.example.data.AppDatabase
import com.example.data.ConnectedDeviceDao
import com.example.data.ConnectedDeviceEntity
import com.example.data.SnippetEntity
import com.example.data.SnippetRepository
import com.example.ui.keyboard.util.KeyboardPreferences
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.Collections
import java.util.LinkedHashSet
import java.util.UUID

class ClipboardSyncManager private constructor(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val preferences = KeyboardPreferences.getInstance(context)
    private val database = AppDatabase.getDatabase(context)
    val snippetRepository = SnippetRepository(database.snippetDao())
    val connectedDeviceDao: ConnectedDeviceDao = database.connectedDeviceDao()
    val authManager = FirebaseAuthManager.getInstance()

    // Activity and logging stream
    private val _syncActivityLog = MutableStateFlow<List<SyncActivityEvent>>(emptyList())
    val syncActivityLog: StateFlow<List<SyncActivityEvent>> = _syncActivityLog.asStateFlow()

    private val _isCloudConnected = MutableStateFlow(false)
    val isCloudConnected: StateFlow<Boolean> = _isCloudConnected.asStateFlow()

    // In-memory cache of processed sync IDs to prevent echo and cyclic loops
    private val processedSyncIds = Collections.synchronizedSet(LinkedHashSet<String>())
    private val maxCacheSize = 300

    private var clipboardListenerRegistration: ListenerRegistration? = null
    private var devicesListenerRegistration: ListenerRegistration? = null

    private fun getAuthSafe(): FirebaseAuth? {
        return try {
            FirebaseAuth.getInstance()
        } catch (_: Exception) {
            null
        }
    }

    private fun getFirestoreSafe(): FirebaseFirestore? {
        return try {
            FirebaseFirestore.getInstance(context.applicationContext.getString(R.string.firestore_database_id))
        } catch (_: Exception) {
            null
        }
    }

    init {
        // Observe Auth state and connect/disconnect Firestore listeners
        try {
            val auth = getAuthSafe()
            auth?.addAuthStateListener { firebaseAuth ->
                val user = firebaseAuth.currentUser
                if (user != null) {
                    _isCloudConnected.value = true
                    startCloudSync(user)
                } else {
                    _isCloudConnected.value = false
                    stopCloudSync()
                }
            }
        } catch (e: Exception) {
            Log.w("FirebaseSync", "Failed to register auth listener: ${e.message}")
        }

        // Attempt silent sign-in on initialization if previously signed in
        authManager.attemptAutoSignIn(context)
    }

    fun startServices() {
        val user = getAuthSafe()?.currentUser
        if (user != null) {
            _isCloudConnected.value = true
            startCloudSync(user)
        } else {
            authManager.attemptAutoSignIn(context)
        }
    }

    private fun startCloudSync(user: FirebaseUser) {
        val db = getFirestoreSafe() ?: return
        val uid = user.uid
        val localDeviceId = preferences.getLocalDeviceId()
        val localDeviceName = preferences.localDeviceName.value.ifBlank { Build.MODEL ?: "Android Device" }

        // 1. Register this device under /users/{uid}/devices/{localDeviceId}
        val deviceData = hashMapOf<String, Any>(
            "userId" to uid,
            "deviceId" to localDeviceId,
            "deviceName" to localDeviceName,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            db.collection("users").document(uid)
                .collection("devices").document(localDeviceId)
                .set(deviceData, SetOptions.merge())
                .addOnFailureListener { e ->
                    Log.w("FirebaseSync", "Device registration non-fatal notice: ${e.message}")
                }
        } catch (e: Exception) {
            Log.w("FirebaseSync", "Device reg error: ${e.message}")
        }

        // 2. Listen to /users/{uid}/devices to populate paired devices list
        devicesListenerRegistration?.remove()
        try {
            devicesListenerRegistration = db.collection("users").document(uid)
                .collection("devices")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("FirebaseSync", "Devices listen error: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        scope.launch {
                            for (doc in snapshot.documents) {
                                val devId = doc.getString("deviceId") ?: doc.id
                                if (devId != localDeviceId) {
                                    val devName = doc.getString("deviceName") ?: "Android Peer"
                                    val entity = ConnectedDeviceEntity(
                                        deviceId = devId,
                                        name = devName,
                                        ipAddress = "Cloud (${user.email ?: "Google Account"})",
                                        isOnline = true,
                                        connectionStatus = "Synced via Firebase",
                                        transportType = "Google Cloud",
                                        lastSyncedTimestamp = System.currentTimeMillis()
                                    )
                                    connectedDeviceDao.insertOrUpdate(entity)
                                }
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Failed to attach devices listener", e)
        }

        // 3. Listen to /users/{uid}/clipboard_items for real-time broadcasts
        clipboardListenerRegistration?.remove()
        try {
            clipboardListenerRegistration = db.collection("users").document(uid)
                .collection("clipboard_items")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(40)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("FirebaseSync", "Clipboard listen error: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        for (change in snapshot.documentChanges) {
                            if (change.type == DocumentChange.Type.ADDED) {
                                val doc = change.document
                                handleIncomingRemoteClip(doc.id, doc.data)
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Failed to attach clipboard listener", e)
        }

        logEvent(
            SyncActivityEvent(
                title = "Firebase Cloud Sync Active",
                description = "Connected with account ${user.email ?: user.uid}",
                isOutgoing = false,
                transport = "Firebase Cloud"
            )
        )
    }

    private fun stopCloudSync() {
        clipboardListenerRegistration?.remove()
        clipboardListenerRegistration = null
        devicesListenerRegistration?.remove()
        devicesListenerRegistration = null
        _isCloudConnected.value = false
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
     * Handles incoming broadcast clip from Firestore snapshot stream.
     */
    private fun handleIncomingRemoteClip(clipId: String, data: Map<String, Any>) {
        val originDeviceId = data["originDeviceId"] as? String ?: ""
        val localDeviceId = preferences.getLocalDeviceId()

        // 1. Prevent cyclic echo if originated from this device
        if (originDeviceId == localDeviceId) {
            return
        }

        // 2. Prevent duplicate processing
        if (isSyncIdProcessed(clipId)) {
            return
        }
        markSyncIdProcessed(clipId)

        val title = data["title"] as? String ?: "Synced Clip"
        val content = data["content"] as? String ?: ""
        val isImage = (data["isImage"] as? Boolean) ?: false
        val isPinned = (data["isPinned"] as? Boolean) ?: false
        val category = data["category"] as? String ?: "Synced"
        val mimeType = data["mimeType"] as? String ?: if (isImage) "image/png" else "text/plain"

        scope.launch {
            var localImageUri: String? = null
            if (isImage && content.isNotBlank() && preferences.clipboardSyncImages.value) {
                try {
                    val bytes = Base64.decode(content, Base64.DEFAULT)
                    val syncedDir = File(context.filesDir, "synced_clips").apply { mkdirs() }
                    val extension = if (mimeType.contains("png")) "png" else if (mimeType.contains("webp")) "webp" else "jpg"
                    val file = File(syncedDir, "cloud_clip_${clipId.take(8)}_${System.currentTimeMillis()}.$extension")
                    FileOutputStream(file).use { fos ->
                        fos.write(bytes)
                    }
                    localImageUri = file.absolutePath
                } catch (_: Exception) {}
            }

            // Save to keyboard Room database
            snippetRepository.saveSyncedSnippet(
                title = title,
                content = if (isImage && localImageUri != null) localImageUri else content,
                isPinned = isPinned,
                category = category,
                timestamp = System.currentTimeMillis(),
                imageUri = localImageUri,
                isImage = isImage,
                mimeType = mimeType,
                syncId = clipId,
                originDeviceId = originDeviceId
            )

            // Auto-update device system clipboard if sync enabled
            if (preferences.clipboardSyncEnabled.value) {
                withContext(Dispatchers.Main) {
                    try {
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        if (!isImage && content.isNotBlank()) {
                            val clip = ClipData.newPlainText("Synced: $title", content)
                            cm?.setPrimaryClip(clip)
                        }
                    } catch (_: Exception) {}
                }
            }

            logEvent(
                SyncActivityEvent(
                    title = if (isImage) "Received Cloud Image" else "Received Cloud Text",
                    description = if (isImage) "Image received from peer ($originDeviceId)" else "'${content.take(32)}'",
                    isOutgoing = false,
                    isImage = isImage,
                    transport = "Firebase Cloud"
                )
            )
        }
    }

    /**
     * Broadcasts a snippet to Firestore.
     * Firebase real-time listeners on other devices signed in with the same email
     * receive the broadcast instantly.
     */
    fun broadcastSnippet(snippet: SnippetEntity, customImageBytes: ByteArray? = null) {
        if (!preferences.clipboardSyncEnabled.value) return

        val user = getAuthSafe()?.currentUser ?: return
        val db = getFirestoreSafe() ?: return
        val uid = user.uid
        val localDeviceId = preferences.getLocalDeviceId()

        // Never re-broadcast clips originated from peers
        if (snippet.originDeviceId.isNotBlank() && snippet.originDeviceId != localDeviceId) {
            logEvent(
                SyncActivityEvent(
                    title = "Cyclic Sync Blocked",
                    description = "Skipped re-broadcasting peer clip (${snippet.syncId.take(8)})",
                    isOutgoing = true,
                    isLoopPrevented = true,
                    transport = "Loop Filter"
                )
            )
            return
        }

        val syncId = if (snippet.syncId.isNotBlank()) snippet.syncId else "clip_${UUID.randomUUID().toString().replace("-", "")}"
        markSyncIdProcessed(syncId)

        scope.launch {
            var payloadContent = snippet.content
            if (snippet.isImage && preferences.clipboardSyncImages.value) {
                val bytes = customImageBytes ?: readImageBytes(snippet.imageUri)
                if (bytes != null) {
                    payloadContent = Base64.encodeToString(bytes, Base64.NO_WRAP)
                }
            }

            val payload = hashMapOf<String, Any>(
                "userId" to uid,
                "originDeviceId" to localDeviceId,
                "title" to snippet.title.take(200),
                "content" to payloadContent.take(500000),
                "isPinned" to snippet.isPinned,
                "category" to snippet.category.take(50),
                "isImage" to snippet.isImage,
                "mimeType" to (snippet.mimeType ?: if (snippet.isImage) "image/png" else "text/plain").take(100),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            try {
                db.collection("users").document(uid)
                    .collection("clipboard_items").document(syncId)
                    .set(payload)
                    .addOnSuccessListener {
                        logEvent(
                            SyncActivityEvent(
                                title = if (snippet.isImage) "Synced Image (Cloud)" else "Synced Text (Cloud)",
                                description = "Broadcasted to account ${user.email ?: uid}",
                                isOutgoing = true,
                                isImage = snippet.isImage,
                                transport = "Firebase Cloud"
                            )
                        )
                    }
                    .addOnFailureListener { e ->
                        Log.e("FirebaseSync", "Failed to broadcast to Firestore: ${e.message}", e)
                    }
            } catch (e: Exception) {
                Log.e("FirebaseSync", "Exception broadcasting to Firestore", e)
            }
        }
    }

    private fun readImageBytes(uriString: String?): ByteArray? {
        if (uriString.isNullOrBlank()) return null
        return try {
            val file = File(uriString)
            if (file.exists() && file.isFile) {
                val rawBytes = file.readBytes()
                if (rawBytes.size > 350 * 1024) {
                    val bmp = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size)
                    if (bmp != null) {
                        val baos = ByteArrayOutputStream()
                        bmp.compress(Bitmap.CompressFormat.JPEG, 70, baos)
                        baos.toByteArray()
                    } else rawBytes
                } else rawBytes
            } else {
                val uri = Uri.parse(uriString)
                val stream = context.contentResolver.openInputStream(uri)
                val rawBytes = stream?.readBytes()
                stream?.close()
                rawBytes
            }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun simulateTestDevicePairing() {
        val testDevice = ConnectedDeviceEntity(
            deviceId = "dev_samsung_tab_simulated",
            name = "Samsung Galaxy Tab S9",
            ipAddress = "Cloud (Google Account)",
            isOnline = true,
            connectionStatus = "Synced via Firebase",
            transportType = "Google Cloud"
        )
        connectedDeviceDao.insertOrUpdate(testDevice)
        snippetRepository.saveSyncedSnippet(
            title = "Samsung Continuity Link",
            content = "https://developer.samsung.com/continuity-clipboard-sync",
            isPinned = false,
            category = "Synced",
            timestamp = System.currentTimeMillis(),
            imageUri = null,
            isImage = false,
            mimeType = "text/plain",
            syncId = "test_clip_samsung_1",
            originDeviceId = "dev_samsung_tab_simulated"
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

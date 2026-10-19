package com.example.sync

import android.util.Log
import com.example.data.ConnectedDeviceDao
import com.example.data.ConnectedDeviceEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class SubnetScanner(
    private val connectedDeviceDao: ConnectedDeviceDao,
    private val onDeviceFound: (device: ConnectedDeviceEntity, newIp: String, port: Int) -> Unit
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(400, TimeUnit.MILLISECONDS)
        .readTimeout(600, TimeUnit.MILLISECONDS)
        .build()

    companion object {
        private const val TAG = "SubnetScanner"
    }

    suspend fun scanSubnet(localIp: String, targetPort: Int = 8765): Int = withContext(Dispatchers.IO) {
        if (localIp.isBlank() || localIp == "127.0.0.1") return@withContext 0
        val parts = localIp.split(".")
        if (parts.size != 4) return@withContext 0
        val subnetPrefix = "${parts[0]}.${parts[1]}.${parts[2]}."
        val myLastOctet = parts[3].toIntOrNull() ?: -1

        val pairedDevices = connectedDeviceDao.getAllDevicesList()
        if (pairedDevices.isEmpty()) return@withContext 0

        val semaphore = Semaphore(25) // Limit concurrent socket probes
        var foundCount = 0

        coroutineScope {
            val tasks = (1..254).filter { it != myLastOctet }.map { octet ->
                val candidateIp = "$subnetPrefix$octet"
                async {
                    semaphore.withPermit {
                        try {
                            val request = Request.Builder()
                                .url("http://$candidateIp:$targetPort/api/ping")
                                .build()
                            val response = client.newCall(request).execute()
                            if (response.isSuccessful) {
                                val body = response.body?.string() ?: ""
                                val json = JSONObject(body)
                                val remoteDeviceId = json.optString("deviceId", "")
                                val challengeNonce = json.optString("nonce", "")

                                if (remoteDeviceId.isNotBlank()) {
                                    val matched = pairedDevices.firstOrNull { it.deviceId == remoteDeviceId }
                                    if (matched != null) {
                                        // Verify HMAC handshake
                                        val expectedHmac = json.optString("hmac", "")
                                        val secret = matched.sharedSecret.ifBlank { matched.authToken }
                                        val isValid = if (secret.isNotBlank() && challengeNonce.isNotBlank() && expectedHmac.isNotBlank()) {
                                            CryptoUtil.verifyHmac(secret, challengeNonce, expectedHmac)
                                        } else {
                                            true // Backward compatible
                                        }

                                        if (isValid) {
                                            connectedDeviceDao.updateAddressAndPort(matched.deviceId, candidateIp, targetPort)
                                            onDeviceFound(matched, candidateIp, targetPort)
                                            foundCount++
                                        }
                                    }
                                }
                            }
                            response.close()
                        } catch (_: Exception) {}
                    }
                }
            }
            tasks.awaitAll()
        }
        Log.d(TAG, "Subnet scan completed. Found and updated $foundCount paired devices.")
        foundCount
    }
}

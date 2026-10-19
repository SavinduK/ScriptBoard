package com.example.sync

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import com.example.data.ConnectedDeviceDao
import com.example.ui.keyboard.util.KeyboardPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NsdHelper(
    private val context: Context,
    private val scope: CoroutineScope,
    private val preferences: KeyboardPreferences,
    private val connectedDeviceDao: ConnectedDeviceDao,
    private val onPeerDiscovered: (deviceId: String, ip: String, port: Int) -> Unit
) {
    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as? NsdManager
    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager

    private var registrationListener: NsdManager.RegistrationListener? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private var multicastLock: WifiManager.MulticastLock? = null

    private var isRegistered = false
    private var isDiscovering = false

    companion object {
        const val SERVICE_TYPE = "_clipsync._tcp."
        private const val TAG = "NsdHelper"
    }

    fun registerService(port: Int) {
        if (nsdManager == null || isRegistered || port <= 0) return
        val deviceId = preferences.getLocalDeviceId()
        val deviceName = preferences.localDeviceName.value

        val serviceInfo = NsdServiceInfo().apply {
            serviceType = SERVICE_TYPE
            serviceName = "KeyPro_${deviceId.take(8)}"
            setPort(port)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                setAttribute("deviceId", deviceId)
                setAttribute("deviceName", deviceName)
            }
        }

        registrationListener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(serviceInfo: NsdServiceInfo?) {
                isRegistered = true
                Log.d(TAG, "NSD Service registered: ${serviceInfo?.serviceName}")
            }

            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
                isRegistered = false
                Log.w(TAG, "NSD Registration failed: $errorCode")
            }

            override fun onServiceUnregistered(serviceInfo: NsdServiceInfo?) {
                isRegistered = false
            }

            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
                isRegistered = false
            }
        }

        try {
            nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
        } catch (e: Exception) {
            Log.e(TAG, "Error registering NSD service", e)
        }
    }

    fun unregisterService() {
        if (nsdManager != null && isRegistered && registrationListener != null) {
            try {
                nsdManager.unregisterService(registrationListener)
            } catch (_: Exception) {}
            isRegistered = false
            registrationListener = null
        }
    }

    fun startDiscovery() {
        if (nsdManager == null || isDiscovering) return

        try {
            multicastLock = wifiManager?.createMulticastLock("KeyPro:NsdDiscoveryLock")?.apply {
                setReferenceCounted(false)
                acquire()
            }
        } catch (_: Exception) {}

        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String?) {
                isDiscovering = true
                Log.d(TAG, "NSD Discovery started for $regType")
            }

            override fun onServiceFound(serviceInfo: NsdServiceInfo?) {
                if (serviceInfo == null) return
                val localDeviceId = preferences.getLocalDeviceId()
                if (serviceInfo.serviceName.contains(localDeviceId.take(8))) {
                    return // Ignore self
                }

                // Resolve the service
                resolveService(serviceInfo)
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo?) {
                Log.d(TAG, "NSD Service lost: ${serviceInfo?.serviceName}")
            }

            override fun onDiscoveryStopped(serviceType: String?) {
                isDiscovering = false
            }

            override fun onStartDiscoveryFailed(serviceType: String?, errorCode: Int) {
                isDiscovering = false
                try {
                    nsdManager.stopServiceDiscovery(this)
                } catch (_: Exception) {}
            }

            override fun onStopDiscoveryFailed(serviceType: String?, errorCode: Int) {
                isDiscovering = false
            }
        }

        try {
            nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start discovery", e)
            isDiscovering = false
        }
    }

    private fun resolveService(serviceInfo: NsdServiceInfo) {
        if (nsdManager == null) return
        val resolveListener = object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
                Log.w(TAG, "NSD Resolve failed: $errorCode")
            }

            override fun onServiceResolved(resolvedInfo: NsdServiceInfo?) {
                if (resolvedInfo == null) return
                val host = resolvedInfo.host?.hostAddress ?: return
                val port = resolvedInfo.port
                var remoteDeviceId = ""

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    val rawId = resolvedInfo.attributes["deviceId"]
                    if (rawId != null) {
                        remoteDeviceId = String(rawId)
                    }
                }

                if (remoteDeviceId.isBlank()) {
                    // Extract from serviceName e.g. KeyPro_a1b2c3d4
                    val match = Regex("KeyPro_([a-zA-Z0-9]+)").find(resolvedInfo.serviceName)
                    remoteDeviceId = match?.groupValues?.getOrNull(1) ?: ""
                }

                if (remoteDeviceId.isNotBlank() && remoteDeviceId != preferences.getLocalDeviceId()) {
                    scope.launch(Dispatchers.IO) {
                        // Check if this device is paired
                        val allDevices = connectedDeviceDao.getAllDevicesList()
                        val matched = allDevices.firstOrNull { it.deviceId == remoteDeviceId || remoteDeviceId.startsWith(it.deviceId.take(8)) }
                        if (matched != null) {
                            if (matched.ipAddress != host || matched.port != port) {
                                connectedDeviceDao.updateAddressAndPort(matched.deviceId, host, port)
                            }
                            onPeerDiscovered(matched.deviceId, host, port)
                        }
                    }
                }
            }
        }

        try {
            nsdManager.resolveService(serviceInfo, resolveListener)
        } catch (e: Exception) {
            Log.e(TAG, "Error resolving service", e)
        }
    }

    fun stopDiscovery() {
        if (nsdManager != null && isDiscovering && discoveryListener != null) {
            try {
                nsdManager.stopServiceDiscovery(discoveryListener)
            } catch (_: Exception) {}
            isDiscovering = false
            discoveryListener = null
        }
        try {
            if (multicastLock?.isHeld == true) {
                multicastLock?.release()
            }
            multicastLock = null
        } catch (_: Exception) {}
    }

    fun restart() {
        unregisterService()
        stopDiscovery()
    }
}

package com.example.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log

class NetworkChangeMonitor(
    private val context: Context,
    private val onNetworkChanged: (newIp: String) -> Unit
) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    private var callback: ConnectivityManager.NetworkCallback? = null
    private var lastKnownIp = ""

    companion object {
        private const val TAG = "NetworkChangeMonitor"
    }

    fun start() {
        if (connectivityManager == null || callback != null) return

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                checkIpChange("onAvailable")
            }

            override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) {
                checkIpChange("onLinkPropertiesChanged")
            }

            override fun onLost(network: Network) {
                Log.d(TAG, "Network lost")
            }
        }

        try {
            connectivityManager.registerNetworkCallback(request, callback!!)
            // Initial check
            lastKnownIp = getLocalIpAddress()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register network callback", e)
        }
    }

    fun stop() {
        val cb = callback
        if (connectivityManager != null && cb != null) {
            try {
                connectivityManager.unregisterNetworkCallback(cb)
            } catch (_: Exception) {}
            callback = null
        }
    }

    private fun checkIpChange(reason: String) {
        val currentIp = getLocalIpAddress()
        if (currentIp.isNotBlank() && currentIp != "127.0.0.1" && currentIp != lastKnownIp) {
            Log.d(TAG, "IP changed from '$lastKnownIp' to '$currentIp' ($reason)")
            lastKnownIp = currentIp
            onNetworkChanged(currentIp)
        }
    }

    private fun getLocalIpAddress(): String {
        try {
            val interfaces = java.util.Collections.list(java.net.NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                if (intf.isLoopback || !intf.isUp) continue
                val addrs = java.util.Collections.list(intf.inetAddresses)
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress && addr is java.net.Inet4Address) {
                        val ip = addr.hostAddress ?: ""
                        if (!ip.startsWith("127.")) {
                            return ip
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        return ""
    }
}

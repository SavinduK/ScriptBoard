package com.example.sync

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BluetoothConnectionReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BluetoothReceiver"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        when (action) {
            BluetoothDevice.ACTION_ACL_CONNECTED -> {
                val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                val mac = device?.address ?: ""
                Log.d(TAG, "Bluetooth ACL Connected for: $mac")
                if (mac.isNotBlank()) {
                    ClipboardSyncManager.getInstance(context).onBluetoothDeviceConnected(mac)
                }
            }
            BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                val mac = device?.address ?: ""
                Log.d(TAG, "Bluetooth ACL Disconnected for: $mac")
                if (mac.isNotBlank()) {
                    ClipboardSyncManager.getInstance(context).onBluetoothDeviceDisconnected(mac)
                }
            }
            BluetoothAdapter.ACTION_STATE_CHANGED -> {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                if (state == BluetoothAdapter.STATE_ON) {
                    Log.d(TAG, "Bluetooth turned ON, restarting listener")
                    ClipboardSyncManager.getInstance(context).restartBluetoothListener()
                }
            }
        }
    }
}

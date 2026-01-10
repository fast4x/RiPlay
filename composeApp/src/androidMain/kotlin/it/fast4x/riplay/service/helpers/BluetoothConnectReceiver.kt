package it.fast4x.riplay.service.helpers

import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter

class BluetoothConnectReceiver(
    private val context: Context,
    private val onDeviceConnected: () -> Unit
) {

    private val filter = IntentFilter(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED) {

                val state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1)

                if (state == BluetoothProfile.STATE_CONNECTED) {
                    onDeviceConnected()
                }
            }
        }
    }

    private var isRegistered = false

    fun register() {
        if (!isRegistered) {
            context.registerReceiver(receiver, filter)
            isRegistered = true
        }
    }

    fun unregister() {
        if (isRegistered) {
            context.unregisterReceiver(receiver)
            isRegistered = false
        }
    }
}
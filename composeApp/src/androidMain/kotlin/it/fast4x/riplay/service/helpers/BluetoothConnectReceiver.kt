package it.fast4x.riplay.service.helpers

import android.Manifest
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

class BluetoothConnectReceiver(
    private val context: Context,
    private val onDeviceConnected: () -> Unit,
    private val onDeviceDisconnected: () -> Unit = {}
) {

    private val filter = IntentFilter(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED) {

                val state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1)
                val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)

                when (state) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        if (device != null && isHeadphoneDevice(device)) {
                            onDeviceConnected()
                        }
                    }
                    BluetoothProfile.STATE_DISCONNECTED -> {
                        if (device != null && isHeadphoneDevice(device)) {
                            onDeviceDisconnected()
                        }
                    }
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

    private fun isHeadphoneDevice(device: BluetoothDevice): Boolean {
        // Requires (Android 12+)
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }

        val btClass = device.bluetoothClass ?: return false

        if (btClass.majorDeviceClass != BluetoothClass.Device.Major.AUDIO_VIDEO) {
            return false
        }

        val deviceClass = btClass.deviceClass

        val allowedClasses = listOf(
            BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES,
            BluetoothClass.Device.AUDIO_VIDEO_WEARABLE_HEADSET,
            BluetoothClass.Device.AUDIO_VIDEO_HANDSFREE
        )

        val excludedClasses = listOf(
            BluetoothClass.Device.AUDIO_VIDEO_CAR_AUDIO,
            BluetoothClass.Device.AUDIO_VIDEO_HIFI_AUDIO
        )

        return when {
            deviceClass in allowedClasses -> true
            deviceClass in excludedClasses -> false
            deviceClass == BluetoothClass.Device.AUDIO_VIDEO_UNCATEGORIZED -> false
            else -> false
        }
    }
}
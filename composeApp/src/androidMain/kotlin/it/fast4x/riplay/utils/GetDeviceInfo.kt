package it.fast4x.riplay.utils

import android.os.Build


fun getDeviceInfo() : DeviceInfo? {

     try {
        val deviceModel = Build.MODEL
        val deviceBrand = Build.MANUFACTURER
        val deviceName = Build.DEVICE
         val deviceVersion = Build.VERSION.RELEASE
         val deviceApiLevel = Build.VERSION.SDK_INT
         val deviceBoard = Build.BOARD
         val deviceBootloader = Build.BOOTLOADER
         val deviceFingerprint = Build.FINGERPRINT

        return DeviceInfo(
            deviceModel = deviceModel,
            deviceBrand = deviceBrand,
            deviceName = deviceName,
            deviceVersion = deviceVersion,
            deviceApiLevel = deviceApiLevel,
            deviceBoard = deviceBoard,
            deviceBootloader = deviceBootloader,
            deviceFingerprint = deviceFingerprint
        )

    } catch (e: Exception) {
        println("Device Info Error: ${e.message}")
    }

    return null

}

data class DeviceInfo(
    val deviceName: String? = null,
    val deviceBrand: String? = null,
    val deviceModel: String? = null,
    val deviceVersion: String? = null,
    val deviceApiLevel: Int? = null,
    val deviceBoard: String? = null,
    val deviceBootloader: String? = null,
    val deviceFingerprint: String? = null
)

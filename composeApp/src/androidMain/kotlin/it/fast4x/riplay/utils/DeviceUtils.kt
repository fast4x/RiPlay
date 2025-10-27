package it.fast4x.riplay.utils

import android.app.ActivityManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat.getSystemService


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
        val deviceHardware = Build.HARDWARE
        val deviceHost = Build.HOST
        val deviceId = Build.ID
        val deviceProduct = Build.PRODUCT
        val deviceType = Build.TYPE
        val deviceTags = Build.TAGS


        return DeviceInfo(
            deviceModel = deviceModel,
            deviceBrand = deviceBrand,
            deviceName = deviceName,
            deviceVersion = deviceVersion,
            deviceApiLevel = deviceApiLevel,
            deviceBoard = deviceBoard,
            deviceBootloader = deviceBootloader,
            deviceFingerprint = deviceFingerprint,
            deviceHardware = deviceHardware,
            deviceHost = deviceHost,
            deviceId = deviceId,
            deviceProduct = deviceProduct,
            deviceType = deviceType,
            deviceTags = deviceTags
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
    val deviceFingerprint: String? = null,
    val deviceHardware: String? = null,
    val deviceHost: String? = null,
    val deviceId: String? = null,
    val deviceProduct: String? = null,
    val deviceType: String? = null,
    val deviceTags: String? = null
)


@RequiresApi(Build.VERSION_CODES.M)
private fun getAvailableMemory(): ActivityManager.MemoryInfo {
    val activityManager = context().getSystemService(ActivityManager::class.java) as ActivityManager
    return ActivityManager.MemoryInfo().also { memoryInfo ->
        activityManager.getMemoryInfo(memoryInfo)
    }
}
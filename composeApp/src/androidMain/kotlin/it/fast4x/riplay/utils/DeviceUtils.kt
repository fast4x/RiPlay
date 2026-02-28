package it.fast4x.riplay.utils

import android.app.ActivityManager
import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.annotation.RequiresApi
import timber.log.Timber
import kotlin.math.pow
import kotlin.math.sqrt


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


fun isTVDevice(): Boolean {
    val uiModeManager = globalContext().getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
    val isTv = uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
    Timber.d("isTVDevice: $isTv")
    return isTv
}

fun isWatchDevice(): Boolean {
    val uiModeManager = globalContext().getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
    val isWatch = uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_WATCH
    Timber.d("isWatchDevice: $isWatch")
    return isWatch
}

fun isTabletDevice(): Boolean {
    // Tablet is a device with >= 7 inch diagonal
    val screenDimensions = getScreenDimensions()
    val metrics = screenDimensions.metrics

    val widthInches = metrics.widthPixels / metrics.xdpi
    val heightInches = metrics.heightPixels / metrics.ydpi
    val diagonalInches = sqrt(widthInches.toDouble().pow(2.0) + heightInches.toDouble().pow(2.0))
    val isTablet = diagonalInches >= 7.0
    Timber.d("isTabletDevice: $isTablet")
    return isTablet
}

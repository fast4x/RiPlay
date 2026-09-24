package it.fast4x.riplay.extensions.storagewarning

import android.os.Environment
import android.os.StatFs

object StorageUtils {

    private const val LOW_STORAGE_THRESHOLD_BYTES = 1_073_741_824L   // 1 GB
    private const val CRITICAL_STORAGE_THRESHOLD_BYTES = 209_715_200L // 200 MB

    enum class StorageStatus {
        OK, LOW, CRITICAL
    }

    data class StorageInfo(
        val freeBytes: Long,
        val totalBytes: Long,
        val status: StorageStatus
    ) {
        val freeMB: Long get() = freeBytes / (1024 * 1024)
        val freeGB: Double get() = freeBytes / (1024.0 * 1024.0 * 1024.0)
        val totalGB: Double get() = totalBytes / (1024.0 * 1024.0 * 1024.0)
        val usedPercent: Float get() = ((totalBytes - freeBytes).toFloat() / totalBytes) * 100f
    }

    fun getStorageInfo(): StorageInfo {
        val stat = StatFs(Environment.getDataDirectory().path)
        val freeBytes = stat.availableBlocksLong * stat.blockSizeLong
        val totalBytes = stat.blockCountLong * stat.blockSizeLong

        val status = when {
            freeBytes < CRITICAL_STORAGE_THRESHOLD_BYTES -> StorageStatus.CRITICAL
            freeBytes < LOW_STORAGE_THRESHOLD_BYTES -> StorageStatus.LOW
            else -> StorageStatus.OK
        }

        return StorageInfo(freeBytes, totalBytes, status)
    }
}
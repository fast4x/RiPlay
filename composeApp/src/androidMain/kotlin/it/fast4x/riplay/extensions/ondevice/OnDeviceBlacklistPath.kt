package it.fast4x.riplay.extensions.ondevice

import androidx.room.Entity
import androidx.room.PrimaryKey

const val blackListedPathsFilename = "Blacklisted_paths.txt"


@Entity("OnDeviceBlacklist")
data class OnDeviceBlacklistPath(
    @PrimaryKey(autoGenerate = true)
    val id: Int? = null,
    val path: String
) {
    fun startWith(relativePath: String): Boolean {
        return relativePath.startsWith(path)
    }
}

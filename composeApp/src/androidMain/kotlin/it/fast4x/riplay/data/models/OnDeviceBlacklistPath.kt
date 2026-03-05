package it.fast4x.riplay.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

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
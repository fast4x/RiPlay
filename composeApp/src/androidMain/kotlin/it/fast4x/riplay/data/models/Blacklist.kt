package it.fast4x.riplay.data.models

import androidx.compose.runtime.Immutable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Immutable
@Entity
data class Blacklist (
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(index = true) val type: String, // Album, Artist, Song, Folder
    val path: String,
    val enabled: Int = 1
) {
    fun startWith(path: String): Boolean {
        return path.startsWith(this.path)
    }

    fun toggleEnabled(): Blacklist {
        return copy(
            enabled = if (enabled == 1) 0 else 1
        )
    }

    val isEnabled: Boolean
        get() = enabled == 1
}

package it.fast4x.riplay.models

import androidx.compose.runtime.Immutable
import androidx.media3.common.MediaItem
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Immutable
@Entity
class QueuedMediaItem(
    @PrimaryKey val mediaId: String,
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB) val mediaItem: MediaItem,
    var position: Long?,
    val idQueue: Long?
)

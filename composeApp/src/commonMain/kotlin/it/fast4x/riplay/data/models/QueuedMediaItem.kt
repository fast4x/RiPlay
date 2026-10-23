package it.fast4x.riplay.data.models

import androidx.compose.runtime.Immutable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Immutable
@Entity
class QueuedMediaItem(
    @PrimaryKey val mediaId: String,
    @ColumnInfo(name = "mediaItem", typeAffinity = ColumnInfo.BLOB) val mediaItemBytes: ByteArray,
    var position: Long?,
    val idQueue: Long?
)

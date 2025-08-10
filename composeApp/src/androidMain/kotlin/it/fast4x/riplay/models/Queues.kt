package it.fast4x.riplay.models

import androidx.compose.runtime.Immutable
import androidx.media3.common.MediaItem
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Immutable
@Entity
data class Queues(
    @PrimaryKey(autoGenerate = true) var id: Long = 0,
    val title: String? = null,
    val acceptSong: Boolean = true,
    val acceptVideo: Boolean = true,
    val acceptPodcast: Boolean = true,
    var position: Long?,
    val isSelected: Boolean? = false
)

fun defaultQueue() = Queues(
        id = defaultQueueId(),
        title = "Default",
        acceptSong = true,
        acceptVideo = true,
        acceptPodcast = true,
        position = null,
        isSelected = false
    )

fun defaultQueueId() = (-1).toLong()

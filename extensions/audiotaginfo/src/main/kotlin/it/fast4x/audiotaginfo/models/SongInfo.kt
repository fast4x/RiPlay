package it.fast4x.audiotaginfo.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SongInfo(
    val title: String?,
    val artist: String?,
    val album: String?,
    @SerialName("release_date")
    val releaseDate: String?,
    @SerialName("track_number")
    val trackNumber: Int?,
    val genre: String?
)

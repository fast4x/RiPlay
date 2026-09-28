package it.fast4x.spotifymeta

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SpotifyOEmbedResponse(
    val title: String? = null,
    @SerialName("thumbnail_url")   val thumbnailUrl: String? = null,
    @SerialName("thumbnail_width") val thumbnailWidth: Int? = null,
    @SerialName("thumbnail_height")val thumbnailHeight: Int? = null
)

enum class SpotifyEntityType { TRACK, ALBUM, ARTIST }

sealed interface SpotifyEntityMeta {
    val id: String
    val name: String?
    val thumbnailUrl: String?
}

data class SpotifyTrackMeta(
    override val id: String,
    override val name: String? = null,
    override val thumbnailUrl: String? = null,
    val artistId: String? = null,
    val artist: String? = null,
    val albumId: String? = null,
    val album: String? = null,
    val previewUrl: String? = null          // preview mp3 30s (solo track)
) : SpotifyEntityMeta

data class SpotifyAlbumMeta(
    override val id: String,
    override val name: String? = null,
    override val thumbnailUrl: String? = null,
    val artistId: String? = null,
    val artist: String? = null,
    val releaseYear: String? = null,
    val trackCount: Int? = null
) : SpotifyEntityMeta

data class SpotifyArtistMeta(
    override val id: String,
    override val name: String? = null,
    override val thumbnailUrl: String? = null,
    val verified: Boolean? = null
) : SpotifyEntityMeta
package it.fast4x.environment.models.responses.providers

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Deezer Api Response es: https://api.deezer.com/2.0/track/isrc:USWB11200587

@Serializable
data class DeezerTrack(
    @SerialName("id") val id: Long,
    @SerialName("readable") val readable: Boolean = false,
    @SerialName("title") val title: String,
    @SerialName("title_short") val titleShort: String = "",
    @SerialName("title_version") val titleVersion: String? = null,
    @SerialName("isrc") val isrc: String? = null,
    @SerialName("link") val link: String? = null,
    @SerialName("share") val share: String? = null,
    @SerialName("duration") val duration: Int = 0,
    @SerialName("track_position") val trackPosition: Int = 0,
    @SerialName("disk_number") val diskNumber: Int = 1,
    @SerialName("rank") val rank: Long = 0,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("explicit_lyrics") val explicitLyrics: Boolean = false,
    @SerialName("explicit_content_lyrics") val explicitContentLyrics: Int = 0,
    @SerialName("explicit_content_cover") val explicitContentCover: Int = 0,
    @SerialName("preview") val preview: String? = null,
    @SerialName("bpm") val bpm: Double? = null,
    @SerialName("gain") val gain: Double? = null,
    @SerialName("available_countries") val availableCountries: List<String> = emptyList(),
    @SerialName("contributors") val contributors: List<DeezerContributor> = emptyList(),
    @SerialName("md5_image") val md5Image: String? = null,
    @SerialName("track_token") val trackToken: String? = null,
    @SerialName("artist") val artist: DeezerArtist,
    @SerialName("album") val album: DeezerAlbum,
    @SerialName("type") val type: String = "track"
)

@Serializable
data class DeezerArtist(
    @SerialName("id") val id: Long,
    @SerialName("name") val name: String,
    @SerialName("link") val link: String? = null,
    @SerialName("share") val share: String? = null,
    @SerialName("picture") val picture: String? = null,
    @SerialName("picture_small") val pictureSmall: String? = null,
    @SerialName("picture_medium") val pictureMedium: String? = null,
    @SerialName("picture_big") val pictureBig: String? = null,
    @SerialName("picture_xl") val pictureXl: String? = null,
    @SerialName("radio") val radio: Boolean = false,
    @SerialName("tracklist") val tracklist: String? = null,
    @SerialName("type") val type: String? = null
)

@Serializable
data class DeezerContributor(
    @SerialName("id") val id: Long,
    @SerialName("name") val name: String,
    @SerialName("link") val link: String? = null,
    @SerialName("share") val share: String? = null,
    @SerialName("picture") val picture: String? = null,
    @SerialName("picture_small") val pictureSmall: String? = null,
    @SerialName("picture_medium") val pictureMedium: String? = null,
    @SerialName("picture_big") val pictureBig: String? = null,
    @SerialName("picture_xl") val pictureXl: String? = null,
    @SerialName("radio") val radio: Boolean = false,
    @SerialName("tracklist") val tracklist: String? = null,
    @SerialName("type") val type: String? = null,
    @SerialName("role") val role: String? = null
)

@Serializable
data class DeezerAlbum(
    @SerialName("id") val id: Long,
    @SerialName("title") val title: String,
    @SerialName("link") val link: String? = null,
    @SerialName("cover") val cover: String? = null,
    @SerialName("cover_small") val coverSmall: String? = null,
    @SerialName("cover_medium") val coverMedium: String? = null,
    @SerialName("cover_big") val coverBig: String? = null,
    @SerialName("cover_xl") val coverXl: String? = null,
    @SerialName("md5_image") val md5Image: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("tracklist") val tracklist: String? = null,
    @SerialName("type") val type: String? = null
)
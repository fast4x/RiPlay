package it.fast4x.lastfm.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LastFmResponse<T>(
    @SerialName("error") val error: Int? = null,
    @SerialName("message") val message: String? = null,
    @SerialName("artist") val artist: T? = null,
    @SerialName("results") val results: T? = null,
    @SerialName("nowplaying") val nowPlaying: T? = null,
    @SerialName("scrobbles") val scrobbles: T? = null,
    @SerialName("session") val session: T? = null
)

@Serializable
data class ArtistInfo(
    @SerialName("name") val name: String,
    @SerialName("stats") val stats: ArtistStats,
    @SerialName("bio") val bio: ArtistBio,
    @SerialName("image") val images: List<LastFmImage>
)

@Serializable
data class LastFmImage(
    @SerialName("#text") val url: String,
    @SerialName("size") val size: String
)

@Serializable
data class ArtistStats(
    @SerialName("listeners") val listeners: String,
    @SerialName("playcount") val playcount: String
)

@Serializable
data class ArtistBio(
    @SerialName("summary") val summary: String
)

@Serializable
data class ScrobbleResponse(
    @SerialName("@attr") val attr: ScrobbleAttr,
    @SerialName("scrobble") val scrobble: ScrobbleStatus? = null
)

@Serializable
data class ScrobbleAttr(
    @SerialName("accepted") val accepted: Int,
    @SerialName("ignored") val ignored: Int
)

@Serializable
data class ScrobbleStatus(
    val message: String?
)

@Serializable
data class NowPlayingResponse(
    @SerialName("artist") val artist: NowPlayingCorrected,
    @SerialName("track") val track: NowPlayingCorrected,
    @SerialName("ignoredMessage") val ignoredMessage: NowPlayingCorrected
)

@Serializable
data class NowPlayingCorrected(
    @SerialName("#text") val text: String = "",
    @SerialName("corrected") val corrected: Int = 0
)

// Auth Session
@Serializable
data class SessionKey(
    @SerialName("key") val key: String,
    @SerialName("name") val name: String
)
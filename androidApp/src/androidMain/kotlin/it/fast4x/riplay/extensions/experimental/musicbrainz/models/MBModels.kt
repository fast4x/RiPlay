package it.fast4x.riplay.extensions.experimental.musicbrainz.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MBSearchArtistResponse(
    val artists: List<MBArtist> = emptyList()
)

@Serializable
data class MBSearchReleaseGroupResponse(
    // MusicBrainz restituisce la lista con la chiave "release-groups" (con il trattino)
    @SerialName("release-groups")
    val releaseGroups: List<MBReleaseGroupSearchResult> = emptyList()
)

@Serializable
data class MBReleaseGroupSearchResult(
    val id: String,       // L'MBID del Release Group
    val title: String,    // Il titolo dell'album
    val score: Int        // Rilevanza della ricerca (0-100)
)

@Serializable
data class MBArtist(
    val id: String, // Questo è l'MBID
    val name: String,
    val score: Int // Rilevanza della ricerca (0-100)
)

@Serializable
data class MBArtistDetailResponse(
    val id: String,
    val name: String,
    val genres: List<MBGenre> = emptyList()
)

@Serializable
data class MBGenre(
    val name: String,
    val count: Int, // Quante volte la community ha votato questo genere
    val disambiguation: String? = null
)

@Serializable
data class MBReleaseGroupDetailResponse(
    val id: String,
    val title: String,
    // La lista dei generi, esattamente come per gli artisti
    val genres: List<MBGenre> = emptyList()
)

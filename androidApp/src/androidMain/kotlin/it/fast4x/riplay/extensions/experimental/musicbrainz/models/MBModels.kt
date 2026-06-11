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
data class MBLifeSpan(
    val begin: String? = null, // Es. "1992" o "1992-05-10"
    val end: String? = null,   // Es. "2004" o null se attivo
    val ended: Boolean? = null // true se l'artista si è sciolto/deceduto
)

@Serializable
data class MBRating(
    val value: Float? = null, // Es. 4.5 (su 5) o 85 (su 100)
    @SerialName("votes-count") val votesCount: Int? = null
)

@Serializable
data class MBRelationList(
    @SerialName("target-type") val targetType: String? = null,
    val relations: List<MBRelation> = emptyList()
)

@Serializable
data class MBRelation(
    val type: String? = null,
    val url: MBRelatedUrl? = null // Ignoriamo l'artista, ci serve solo l'URL
)

@Serializable
data class MBRelatedUrl(
    val resource: String // L'URL es: "https://en.wikipedia.org/wiki/Nirvana_(band)"
)

@Serializable
data class MBTag(
    val name: String,
    val count: Int
)

@Serializable
data class MBArtistDetailResponse(
    val id: String,
    val name: String,
    val genres: List<MBGenre> = emptyList(),

    val type: String? = null, // "Person", "Group", ecc.
    val country: String? = null, // "US", "GB", "IT", "JP"

    // L'oggetto life-span annidato
    @SerialName("life-span")
    val lifeSpan: MBLifeSpan? = null,

    val tags: List<MBTag>? = null,
    val rating: MBRating? = null,

    @SerialName("relation-list") val relationList: List<MBRelationList>? = null,
    val disambiguation: String? = null,
)

@Serializable
data class MBGenre(
    val name: String,
    val count: Int, // Quante volte la community ha votato questo genere
)

@Serializable
data class MBReleaseGroupDetailResponse(
    val id: String,
    val title: String,
    val genres: List<MBGenre> = emptyList(),

    @SerialName("primary-type")
    val primaryType: String? = null, // Es. "Album", "Single", "EP"

    @SerialName("secondary-types")
    val secondaryTypes: List<String> = emptyList(), // Es. ["Compilation", "Live"]

    @SerialName("first-release-date")
    val firstReleaseDate: String? = null, // Es. "1994-03-08" o "1994"
)

data class MBAlbumMetadata(
    val genres: List<String>,
    val albumType: String?, // Es. "Album", "EP", "Live"
    val originalYear: Int?  // Es. 1994
)

data class MBArtistMetadata(
    val genres: List<String>,
    val artistType: String?,  // Es. "Person", "Group"
    val countryCode: String?, // Es. "JP", "IT"
    val beginYear: Int? ,      // Es. "1994"

    val topTags: List<String>,
    val ratingValue: Float?,       // Es. 4.5
    val ratingVotes: Int?,         // Es. 150
    val wikipediaUrl: String?,      // Link alla bio
    val disambiguation: String?,   // Info addizionali su artista
)
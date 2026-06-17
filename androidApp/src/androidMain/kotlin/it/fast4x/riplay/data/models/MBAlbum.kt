package it.fast4x.riplay.data.models

import it.fast4x.riplay.extensions.musicbrainz.models.ExternalLink
import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Immutable
@Entity(
    tableName = "mb_album",
    indices = [
        Index(value = ["rating", "ratingVotes"]),
        Index(value = ["originalYear"]),
        Index(value = ["primaryType"]),
        Index(value = ["fetchedAt"]),
        Index(value = ["matchedAlbumId"])
    ]
)
data class MBAlbum(
    @PrimaryKey
    val id: String,                              // MBID del release group

    val title: String,
    val primaryType: String? = null,             // "Album", "EP", "Single"
    val secondaryTypes: List<String>? = null,    // ["Compilation", "Live"]
    val firstReleaseDate: String? = null,
    val originalYear: Int? = null,

    // Metadati quality
    val genres: List<String>? = null,
    val tags: List<String>? = null,
    val rating: Float? = null,
    val ratingVotes: Int? = null,

    // Cross-reference
    val wikipediaUrl: String? = null,
    val links: List<ExternalLink>? = null,

    // Artisti (per UI e per match locale)
    val artistCredit: String? = null,            // "Iron Maiden"
    val artistMbIds: List<String>? = null,       // ["mbid1", "mbid2"]

    // Matching con tabella Album YTM
    val matchedAlbumId: String? = null,          // null = non cercato, "" = cercato senza match, FK = matchato
    val matchScore: Float? = null,
    val matchedAt: Long? = null,

    // Tracking fetch
    val fetchedAt: Long,
    val popularityScore: Float = 0f              // precomputato per sorting
) {
    /**
     * Conta i tag per sorting/proxy qualità.
     */
    val tagCount: Int
        get() = (tags?.size ?: 0) + (genres?.size ?: 0)
}
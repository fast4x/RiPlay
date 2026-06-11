package it.fast4x.riplay.data.models

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey
import it.fast4x.riplay.enums.LinkType
import it.fast4x.riplay.extensions.experimental.musicbrainz.models.ExternalLink
import kotlinx.serialization.Serializable

@Serializable
@Immutable
@Entity
data class Album(
    @PrimaryKey val id: String,
    val title: String? = null,
    val thumbnailUrl: String? = null,
    val year: String? = null,
    val authorsText: String? = null,
    val shareUrl: String? = null,
    val timestamp: Long? = null,
    val bookmarkedAt: Long? = null,
    val isYoutubeAlbum: Boolean = false,
    val genres: List<String>? = null,
    val originalYear: Int? = null, // L'anno reale di uscita secondo MB
    val albumType: String? = null, // "Album", "EP", "Live", secondo MB

    val tags: List<String>? = null,
    val rating: Float? = null,
    val ratingVotes: Int? = null,
    val wikipediaUrl: String? = null,
    val wikipediaInfo: String? = null,
    val links: List<ExternalLink>? = null
) {

    fun shareUrlByType(typeOfUrl: LinkType): String? {
        return when(typeOfUrl) {
            LinkType.Main -> this.shareYTUrl
            LinkType.Alternative -> this.shareYTMUrl
        }
    }

    val shareYTUrl: String?
        get() = shareUrl?.replace("music.","www.")

    val shareYTMUrl: String?
        get() = shareUrl?.replace("www.","music.")

    fun toggleBookmark(): Album {
        return copy(
            bookmarkedAt = if (bookmarkedAt == null) System.currentTimeMillis() else null
        )
    }

    val info: String
        get() = buildList {
        originalYear?.let { add(it.toString()) }
        albumType?.let { add(it) } // Aggiunge "Album", "Live", "EP"
    }.joinToString("    ")

    val keywords: List<String>
        get() = (genres.orEmpty() + tags.orEmpty())
            .distinctBy { it.lowercase() } // Evita "Rock" e "rock"
            .take(8)

}

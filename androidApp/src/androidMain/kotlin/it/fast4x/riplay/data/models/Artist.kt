package it.fast4x.riplay.data.models

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey
import it.fast4x.riplay.commonutils.YTM_ARTIST_SHARE_BASEURL
import it.fast4x.riplay.commonutils.YT_ARTIST_SHARE_BASEURL
import it.fast4x.riplay.enums.LinkType
import it.fast4x.riplay.utils.toFlagEmoji

@Immutable
@Entity
data class Artist(
    @PrimaryKey val id: String,
    val name: String? = null,
    val thumbnailUrl: String? = null,
    val timestamp: Long? = null,
    val bookmarkedAt: Long? = null,
    val isYoutubeArtist: Boolean = false,
    val genres: List<String>? = null, // null = da cercare, emptyList = cercato ma assenti
    val artistType: String? = null,   // Single, Band
    val countryCode: String? = null,
    val beginYear: Int? = null,
    val tags: List<String>? = null,
    val rating: Float? = null,
    val ratingVotes: Int? = null,
    val wikipediaUrl: String? = null,
    val description: String? = null,
    val disambiguation: String? = null,
) {

    fun shareUrlByType(typeOfUrl: LinkType): String? {
        return when(typeOfUrl) {
            LinkType.Main -> this.shareYTUrl
            LinkType.Alternative -> this.shareYTMUrl
        }
    }

    val shareYTUrl: String?
        get() = id.let { "$YT_ARTIST_SHARE_BASEURL$it" }
    val shareYTMUrl: String?
        get() = id.let { "$YTM_ARTIST_SHARE_BASEURL$it" }

    val info: String
        get() = buildList {
            artistType?.let { add(it) }
            beginYear?.let { add(it) }
            countryCode?.let { add(it.toFlagEmoji()) }
        }.joinToString(" . ")

    val keywords: List<String>
        get() = (genres.orEmpty() + tags.orEmpty())
        .distinctBy { it.lowercase() } // Evita "Rock" e "rock"
        .take(8)

}

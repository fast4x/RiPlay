package it.fast4x.riplay.data.models

import androidx.compose.runtime.Immutable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import it.fast4x.riplay.commonutils.YTM_ARTIST_SHARE_BASEURL
import it.fast4x.riplay.commonutils.YT_ARTIST_SHARE_BASEURL
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.enums.ArtistNature
import it.fast4x.riplay.enums.LinkType
import it.fast4x.riplay.extensions.musicbrainz.models.ExternalLink
import it.fast4x.riplay.utils.toFlagEmoji


@Immutable
@Entity(
    //tableName = "artist",
    indices = [
        Index(value = ["mbId"]),
        Index(value = ["youtubeChannelId"]),
        Index(value = ["name"])
    ]
)
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
    val wikipediaBio: String? = null,
    val description: String? = null,
    val disambiguation: String? = null,
    val links: List<ExternalLink>? = null,

    val mbId: String? = null, // id di MusicBrainz
    val youtubeChannelId: String? = null,

    // Ponte ZoundHub: Spotify artist ID estratto dagli url-rels MusicBrainz
    // (open.spotify.com/artist/<id>). ZoundHub cataloga gli artisti per
    // Spotify ID: con questo campo il match AI e' ESATTO, senza fuzzy.
    val spotifyId: String? = null,

    // Provenienza del match MusicBrainz: quanto ci si puo' fidare del mbId
    // (0..1, combinazione score MB + similarita' nome) e quando e' stato matchato.
    val mbMatchScore: Float? = null,
    val mbMatchedAt: Long? = null,

    // Negative cache: epoch millis dell'ultimo tentativo FALLITO di risolvere
    // il mbId. Evita che i backfiller settimanali ri-proviino all'infinito gli
    // artisti che non hanno match (null = mai tentato / ultimo tentativo ok).
    val mbLookupFailedAt: Long? = null,

    @ColumnInfo(defaultValue = "UNKNOWN")
    val nature: ArtistNature = ArtistNature.UNKNOWN
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
            //artistType?.let { add(it) }
            beginYear?.let { add(it) }
            countryCode?.let { add(it.toFlagEmoji()) }
        }.joinToString("    ")

    val keywords: List<String>
        get() = (genres.orEmpty() + tags.orEmpty())
        .distinctBy { it.lowercase() } // Evita "Rock" e "rock"
        .take(8)

}

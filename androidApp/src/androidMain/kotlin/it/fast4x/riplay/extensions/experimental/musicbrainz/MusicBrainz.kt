package it.fast4x.riplay.extensions.experimental.musicbrainz

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.BrowserUserAgent
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.json
import it.fast4x.musicbrainz.utils.ProxyPreferences
import it.fast4x.musicbrainz.utils.getProxy
import it.fast4x.riplay.BuildConfig
import it.fast4x.riplay.extensions.experimental.musicbrainz.models.MBAlbumMetadata
import it.fast4x.riplay.extensions.experimental.musicbrainz.models.MBArtistDetailResponse
import it.fast4x.riplay.extensions.experimental.musicbrainz.models.MBArtistMetadata
import it.fast4x.riplay.extensions.experimental.musicbrainz.models.MBReleaseGroupDetailResponse
import it.fast4x.riplay.extensions.experimental.musicbrainz.models.MBSearchArtistResponse
import it.fast4x.riplay.extensions.experimental.musicbrainz.models.MBSearchReleaseGroupResponse
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import java.net.URLEncoder

class MusicBrainz {

    private val baseUrl = "https://musicbrainz.org/ws/2"
    private val appVersion = BuildConfig.VERSION_NAME
    private val userAgent = "riplay/$appVersion ( https://github.com/fast4x/RiPlay )"

    private val rateLimiter = Mutex()

    private suspend fun <T> makeRateLimitedRequest(block: suspend () -> T): T {
        rateLimiter.withLock {
            delay(1050) // 1 secondo + margine
            return block()
        }
    }

    private val client by lazy {
        HttpClient(OkHttp) {
            BrowserUserAgent()

            expectSuccess = true

            install(ContentNegotiation) {
                val feature = Json {
                    ignoreUnknownKeys = true
                    explicitNulls = false
                    encodeDefaults = true
                    isLenient = true
                }

                json(feature)
                //json(feature, ContentType.Text.Html)
                //json(feature, ContentType.Text.Plain)
            }

            install(ContentEncoding) {
                gzip()
                deflate()
            }

            ProxyPreferences.preference?.let {
                engine {
                    proxy = getProxy(it)
                }
            }

            defaultRequest {
                url(baseUrl)
            }
        }
    }

    // Cerca i generi dell'artista
    suspend fun fetchArtistMetadata(artistName: String): MBArtistMetadata {
        return makeRateLimitedRequest {
            // 1. Cerca l'artista per ottenere l'MBID
            val searchResponse = client.get("$baseUrl/artist?query=$artistName&fmt=json") {
                header("User-Agent", userAgent)
            }
            val searchResult = searchResponse.body<MBSearchArtistResponse>()
            val mbid = searchResult.artists.maxByOrNull { it.score }?.id ?: return@makeRateLimitedRequest MBArtistMetadata(emptyList(), null, null, null, emptyList(), null, null, null)

            // 2. Ottiene dettagli con generi
            val detailResponse = client.get("$baseUrl/artist/$mbid?inc=genres+tags+ratings+url-rels&fmt=json") {
                header("User-Agent", userAgent)
            }
            val detailResult = detailResponse.body<MBArtistDetailResponse>()

            val genres = detailResult.genres
                .sortedByDescending { it.count }
                .map { it.name.lowercase() }

            val beginYear = detailResult.lifeSpan?.begin?.take(4)?.toIntOrNull()

            val topTags = detailResult.tags
                ?.sortedByDescending { it.count }
                ?.take(5)
                ?.map { it.name.lowercase() }
                ?: emptyList()

            val ratingValue = detailResult.rating?.value
            val ratingVotes = detailResult.rating?.votesCount

            val wikiUrl = detailResult.relationList
                ?.filter { it.targetType == "url" }
                ?.flatMap { it.relations }
                ?.firstOrNull { it.url?.resource?.contains("wikipedia") == true }
                ?.url?.resource

            MBArtistMetadata(
                genres = genres,
                artistType = detailResult.type,
                countryCode = detailResult.country,
                beginYear = beginYear,
                topTags = topTags,
                ratingValue = ratingValue,
                ratingVotes = ratingVotes,
                wikipediaUrl = wikiUrl
            )

        }
    }

    // Cerca i generi dell'album (Release Group)
    suspend fun fetchAlbumGenres(albumTitle: String, artistName: String): List<String> {
        return makeRateLimitedRequest {
            // 1. Cerca il Release Group
            val query = URLEncoder.encode("releasegroup:\"$albumTitle\" AND artist:\"$artistName\"", "UTF-8")
            val searchResponse = client.get("$baseUrl/release-group?query=$query&fmt=json") {
                header("User-Agent", userAgent)
            }
            val searchResult = searchResponse.body<MBSearchReleaseGroupResponse>()
            val mbid = searchResult.releaseGroups.maxByOrNull { it.score }?.id ?: return@makeRateLimitedRequest emptyList()

            // 2. Ottiene dettagli con generi
            val detailResponse = client.get("$baseUrl/release-group/$mbid?inc=genres&fmt=json") {
                header("User-Agent", userAgent)
            }
            val detailResult = detailResponse.body<MBReleaseGroupDetailResponse>()

            detailResult.genres
                .sortedByDescending { it.count }
                .map { it.name.lowercase() }
        }
    }


    suspend fun fetchAlbumMetadata(albumTitle: String, artistName: String): MBAlbumMetadata {
        return makeRateLimitedRequest {
            // 1. Cerca il Release Group
            val query = URLEncoder.encode("releasegroup:\"$albumTitle\" AND artist:\"$artistName\"", "UTF-8")
            val searchResponse = client.get("$baseUrl/release-group?query=$query&fmt=json") {
                header("User-Agent", userAgent)
            }
            val searchResult = searchResponse.body<MBSearchReleaseGroupResponse>()
            val mbid = searchResult.releaseGroups.maxByOrNull { it.score }?.id
                ?: return@makeRateLimitedRequest MBAlbumMetadata(emptyList(), null, null)

            // 2. Ottiene dettagli
            val detailResponse = client.get("$baseUrl/release-group/$mbid?inc=genres&fmt=json") {
                header("User-Agent", userAgent)
            }
            val detailResult = detailResponse.body<MBReleaseGroupDetailResponse>()

            // 3. Estrae e formatta i nuovi dati
            val genres = detailResult.genres
                .sortedByDescending { it.count }
                .map { it.name.lowercase() }

            // Logica per il tipo: se è un Album ma è anche Live, preferiamo scrivere "Live"
            val albumType = when {
                detailResult.secondaryTypes.contains("Live") -> "Live"
                detailResult.secondaryTypes.contains("Compilation") -> "Compilation"
                detailResult.secondaryTypes.contains("Remix") -> "Remix"
                else -> detailResult.primaryType // "Album", "Single", "EP"
            }

            // Estrae l'anno dalla data YYYY
            val originalYear = detailResult.firstReleaseDate?.take(4)?.toIntOrNull()

            MBAlbumMetadata(
                genres = genres,
                albumType = albumType,
                originalYear = originalYear
            )
        }
    }

}

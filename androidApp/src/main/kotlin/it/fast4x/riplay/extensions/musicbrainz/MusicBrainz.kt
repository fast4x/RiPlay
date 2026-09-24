package it.fast4x.riplay.extensions.musicbrainz

import android.util.Log
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.BrowserUserAgent
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.encodeURLParameter
import io.ktor.serialization.kotlinx.json.json
import it.fast4x.musicbrainz.utils.ProxyPreferences
import it.fast4x.musicbrainz.utils.getProxy
import it.fast4x.riplay.BuildConfig
import it.fast4x.riplay.extensions.musicbrainz.models.ExternalLink
import it.fast4x.riplay.extensions.musicbrainz.models.MBAlbumMetadata
import it.fast4x.riplay.extensions.musicbrainz.models.MBArtist
import it.fast4x.riplay.extensions.musicbrainz.models.MBArtistDetailResponse
import it.fast4x.riplay.extensions.musicbrainz.models.MBArtistMetadata
import it.fast4x.riplay.extensions.musicbrainz.models.MBArtistRelationEntry
import it.fast4x.riplay.extensions.musicbrainz.models.MBArtistRelationResponse
import it.fast4x.riplay.extensions.musicbrainz.models.MBReleaseGroupDetailResponse
import it.fast4x.riplay.extensions.musicbrainz.models.MBReleaseGroupSearchResult
import it.fast4x.riplay.extensions.musicbrainz.models.MBSearchArtistResponse
import it.fast4x.riplay.extensions.musicbrainz.models.MBSearchReleaseGroupResponse
import it.fast4x.riplay.extensions.musicbrainz.models.WikiInfoResult
import it.fast4x.riplay.utils.cleanWikipediaText
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import timber.log.Timber
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

    // Cerca i metadata dell'artista
    suspend fun fetchArtistMetadata(artistName: String): MBArtistMetadata {
        return makeRateLimitedRequest {
            // 1. Cerca l'artista per ottenere l'MBID
            val searchResponse = client.get("$baseUrl/artist?query=$artistName&fmt=json") {
                header("User-Agent", userAgent)
            }
            val searchResult = searchResponse.body<MBSearchArtistResponse>()
            val mbid = searchResult.artists.maxByOrNull { it.score }?.id
                ?: return@makeRateLimitedRequest MBArtistMetadata(emptyList(), null, null, null, emptyList(), null, null, null, null,null, emptyList(), null)

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

            detailResult.relations?.forEach { relation ->
                Timber.d("MBMetadataHelper MB_RELATION - Type: ${relation.type} | URL: ${relation.url?.resource}")
            }

            val links = detailResult.relations
                ?.filter { it.url != null && (it.type == "social network" || it.type == "official homepage")}
                ?.map { relation ->
                    val url = relation.url!!.resource
                    ExternalLink(
                        type = relation.type ?: "unknown",
                        url = url,
                        platform = extractPlatformFromUrl(url, relation.type)
                    )
                } ?: emptyList()

            val wikiUrl = detailResult.relations
                ?.firstOrNull { it.url?.resource?.contains("wikipedia.org") == true }
                ?.url?.resource


            MBArtistMetadata(
                genres = genres,
                artistType = detailResult.type,
                countryCode = detailResult.country,
                beginYear = beginYear,
                topTags = topTags,
                ratingValue = ratingValue,
                ratingVotes = ratingVotes,
                wikipediaUrl = wikiUrl,
                disambiguation = detailResult.disambiguation,
                wikipediaBio = null,
                links = links,
                mbId = mbid
            )

        }
    }
    suspend fun searchArtistByName(name: String): List<MBArtist> {
        val encoded = URLEncoder.encode(name, "UTF-8")
        val response = client.get("$baseUrl/artist/?query=artist:\"$encoded\"&limit=5&fmt=json") {
            header("User-Agent", userAgent)
        }
        return response.body<MBSearchArtistResponse>().artists
            .filter { it.score >= 80 }  // match rilevante
    }

    suspend fun fetchArtistDetail(mbId: String): MBArtistDetailResponse {
        val response = client.get("$baseUrl/artist/$mbId?inc=genres+tags+ratings+url-rels&fmt=json") {
            header("User-Agent", userAgent)
        }
        return response.body()
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
                ?: return@makeRateLimitedRequest MBAlbumMetadata(emptyList(), null, null, emptyList(), null, null, null)

            // 2. Ottiene dettagli
//            val detailResponse = client.get("$baseUrl/release-group/$mbid?inc=genres+tags+ratings+url-rels&fmt=json") {
//                header("User-Agent", userAgent)
//            }
//            val detailResult = detailResponse.body<MBReleaseGroupDetailResponse>()
            val detailResponse = client.get("$baseUrl/release-group/$mbid?inc=genres+tags+ratings+url-rels&fmt=json") {
                header("User-Agent", userAgent)
            }
            val rawBody = detailResponse.bodyAsText()
            Timber.tag("MB_DEBUG").d("=== RAW MB RESPONSE for $mbid ===")
            Timber.tag("MB_DEBUG").d(rawBody)
            Timber.tag("MB_DEBUG").d("=== END RAW ===")

            val detailResult = detailResponse.body<MBReleaseGroupDetailResponse>()
            Timber.tag("MB_DEBUG")
                .d("Parsed: rating=${detailResult.rating?.value}, votes=${detailResult.rating?.votesCount}, genres=${detailResult.genres.size}")

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

            val topTags = detailResult.tags
                ?.sortedByDescending { it.count }
                ?.take(5)
                ?.map { it.name.lowercase() }
                ?: emptyList()


            val ratingValue = detailResult.rating?.value
            val ratingVotes = detailResult.rating?.votesCount


            val wikiUrl = detailResult.relations
                ?.firstOrNull { it.url?.resource?.contains("wikipedia.org") == true }
                ?.url?.resource

            detailResult.relations?.forEach { relation ->
                Timber.d("MBMetadataHelper MB_RELATION - Type: ${relation.type} | URL: ${relation.url?.resource}")
            }

            val links = detailResult.relations
                ?.filter { it.url != null && (it.type == "social network" || it.type == "official homepage")}
                ?.map { relation ->
                    val url = relation.url!!.resource
                    ExternalLink(
                        type = relation.type ?: "unknown",
                        url = url,
                        platform = extractPlatformFromUrl(url, relation.type)
                    )
                } ?: emptyList()

            MBAlbumMetadata(
                genres = genres,
                albumType = albumType,
                originalYear = originalYear,
                topTags = topTags,
                ratingValue = ratingValue,
                ratingVotes = ratingVotes,
                wikipediaUrl = wikiUrl,
                links = links
            )
        }
    }

    suspend fun fetchWikipediaExtractByArtist(artistTerm: String): WikiInfoResult? {
        return try {
            val cleanName = artistTerm
                .replace(Regex("(?i)VEVO$|- Topic$|Official"), "")
                .trim()
                .encodeURLParameter()

            val searchUrl = "https://en.wikipedia.org/w/api.php?" +
                    "action=query&titles=$cleanName&prop=extracts&exintro&explaintext&format=json&redirects=true"

            val searchResponse = client.get(searchUrl)
            //Timber.d("MBMetadataHelper fetchWikipediaExtractByArtist searchResponse ${searchResponse.bodyAsText()}")
            val searchJson = Json.parseToJsonElement(searchResponse.bodyAsText()).jsonObject

            val pages = searchJson["query"]?.jsonObject?.get("pages")?.jsonObject

            // Il JSON di Wikipedia è una mappa dove la chiave è l'ID della pagina (es. "12345")
            // Se la pagina non esiste, la chiave è "-1"
            val pageEntry = pages?.entries?.firstOrNull()

            // Controllo se Wikipedia ci dice che la pagina manca
            val isMissing = pageEntry?.value?.jsonObject?.containsKey("missing")

            if (isMissing == true) {
                // L'artista non ha una pagina su Wikipedia
                return null
            }

            val pageData = pageEntry?.value?.jsonObject

            // Estraiamo il testo
            val bio = pageData?.get("extract")?.jsonPrimitive?.contentOrNull

            // Estraiamo il titolo finale (dopo eventuali redirect di Wikipedia)
            val title = pageData?.get("title")?.jsonPrimitive?.contentOrNull



            if (bio != null && title != null) {
                // Costruisco l'URL Es. "Nirvana (band)" -> "Nirvana_(band)"
                val formattedTitle = title.replace(" ", "_")
                val wikiUrl = "https://en.wikipedia.org/wiki/${formattedTitle.encodeURLParameter()}"

                val wikiInfoResult = WikiInfoResult(
                    info = bio.cleanWikipediaText(),
                    url = wikiUrl
                )

                //Timber.d("MBMetadataHelper fetchWikipediaExtractByArtist WikiBioResult $wikiBioResult ")

                wikiInfoResult
            } else {
                null
            }

        } catch (e: Exception) {
            Timber.e(e, "MBMetadataHelper Errore nel fetch della biografia Wikipedia per $artistTerm")
            null
        }
    }

    suspend fun searchReleaseGroupByTag(
        tag: String,
        limit: Int = 50
    ): List<MBReleaseGroupSearchResult> {
        val encodedTag = URLEncoder.encode(tag, "UTF-8")
        val response = client.get("$baseUrl/release-group/?query=tag:\"$encodedTag\"&limit=$limit&fmt=json") {
            header("User-Agent", userAgent)
        }
        val searchResponse = response.body<MBSearchReleaseGroupResponse>()
        // Filtra per score alto (match relevance)
        return searchResponse.releaseGroups.filter { it.score >= 60 }
    }

    /**
     * Cerca per genere con combinazione tag + release group type.
     * Più efficace di searchReleaseGroupByTag perché filtra per tipo "Album".
     */
    suspend fun searchAlbumsByGenre(
        genre: String,
        limit: Int = 50
    ): List<MBReleaseGroupSearchResult> {
        val encodedGenre = URLEncoder.encode(genre, "UTF-8")
        // Usa genre: invece di tag: — più curato
        val response = client.get(
            "$baseUrl/release-group/?query=genre:\"$encodedGenre\" AND primarytype:Album&limit=$limit&fmt=json"
        ) {
            header("User-Agent", userAgent)
        }
        val searchResponse = response.body<MBSearchReleaseGroupResponse>()
        return searchResponse.releaseGroups.filter { it.score >= 50 }
    }

    /**
     * Fetch dettagli release group con artist credit incluso.
     * Usa la tua funzione esistente se già fa questo.
     */
    suspend fun getReleaseGroupDetailWithArtist(mbid: String): MBReleaseGroupDetailResponse {
        return client.get("$baseUrl/release-group/$mbid?inc=genres+tags+ratings+url-rels+artist-credits&fmt=json") {
            header("User-Agent", userAgent)
        }.body()
    }

    suspend fun fetchArtistRelations(artistMbId: String): List<MBArtistRelationEntry> {
        val response = client.get("$baseUrl/artist/$artistMbId?inc=artist-rels&fmt=json") {
            header("User-Agent", userAgent)
        }
        return response.body<MBArtistRelationResponse>().relations
    }

    // In MBClient.kt

    /**
     * Cerca release groups su MusicBrainz per titolo e artista.
     * Ritorna una lista di risultati ordinati per relevance score (0-100).
     */
    suspend fun searchReleaseGroup(
        title: String,
        artist: String,
        limit: Int = 10
    ): List<MBReleaseGroupSearchResult> {
        val encodedTitle = URLEncoder.encode(title, "UTF-8")
        val encodedArtist = URLEncoder.encode(artist, "UTF-8")

        // Query combinata: titolo + artista per match più preciso
        val query = "releasegroup:\"$encodedTitle\" AND artist:\"$encodedArtist\""
        val encodedQuery = URLEncoder.encode(query, "UTF-8")

        val response = client.get("$baseUrl/release-group/?query=$encodedQuery&limit=$limit&fmt=json") {
            header("User-Agent", userAgent)
        }

        val searchResponse = response.body<MBSearchReleaseGroupResponse>()

        // Filtra per score rilevante (MB assegna score 0-100)
        return searchResponse.releaseGroups.filter { it.score >= 70 }
    }

    /**
     * Cerca release groups su MusicBrainz solo per titolo (senza artista).
     * Utile come fallback quando l'artista non matcha.
     */
    suspend fun searchReleaseGroupByTitle(
        title: String,
        limit: Int = 10
    ): List<MBReleaseGroupSearchResult> {
        val encodedTitle = URLEncoder.encode(title, "UTF-8")
        val query = "releasegroup:\"$encodedTitle\""
        val encodedQuery = URLEncoder.encode(query, "UTF-8")

        val response = client.get("$baseUrl/release-group/?query=$encodedQuery&limit=$limit&fmt=json") {
            header("User-Agent", userAgent)
        }

        val searchResponse = response.body<MBSearchReleaseGroupResponse>()
        return searchResponse.releaseGroups.filter { it.score >= 70 }
    }


    /**
     * Cerca release groups usciti dopo una certa data.
     * Usa la query syntax di MB: date:[date TO *]
     */
    suspend fun searchRecentReleaseGroups(
        sinceDate: String,  // formato "YYYY-MM-DD"
        limit: Int = 100
    ): List<MBReleaseGroupSearchResult> {
        val query = "date:[$sinceDate TO *] AND primarytype:Album"
        val encodedQuery = URLEncoder.encode(query, "UTF-8")

        val response = client.get("$baseUrl/release-group/?query=$encodedQuery&limit=$limit&fmt=json") {
            header("User-Agent", userAgent)
        }

        val searchResponse = response.body<MBSearchReleaseGroupResponse>()
        return searchResponse.releaseGroups
    }

    /**
     * Cerca release groups per artista specifico (MBID).
     */
    suspend fun searchReleaseGroupsByArtist(
        artistMbId: String,
        limit: Int = 20
    ): List<MBReleaseGroupSearchResult> {
        val query = "arid:$artistMbId AND primarytype:Album"
        val encodedQuery = URLEncoder.encode(query, "UTF-8")

        val response = client.get("$baseUrl/release-group/?query=$encodedQuery&limit=$limit&fmt=json") {
            header("User-Agent", userAgent)
        }

        return response.body<MBSearchReleaseGroupResponse>().releaseGroups
    }

    // Helper per dedurre la piattaforma dall'URL
    private fun extractPlatformFromUrl(url: String, type: String?): String {
        return when {
            type == "official homepage" -> "home"
            "instagram.com" in url -> "instagram"
            "facebook.com" in url -> "facebook"
            "twitter.com" in url || "x.com" in url -> "twitter"
            "youtube.com" in url || "youtu.be" in url -> "youtube"
            "open.spotify.com" in url -> "spotify"
            "music.apple.com" in url -> "applemusic"
            "deezer.com" in url -> "deezer"
            "tidal.com" in url -> "tidal"
            "soundcloud.com" in url -> "soundcloud"
            "discogs.com" in url -> "discogs"
            "rateyourmusic.com" in url -> "rateyourmusic"
            "last.fm" in url -> "lastfm"
            else -> "" // Fallback generico
        }
    }

}

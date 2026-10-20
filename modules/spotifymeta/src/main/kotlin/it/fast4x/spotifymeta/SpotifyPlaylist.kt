package it.fast4x.spotifymeta

import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import it.fast4x.spotifymeta.utils.jsonArrayOrNull
import it.fast4x.spotifymeta.utils.jsonObjectOrNull
import it.fast4x.spotifymeta.utils.jsonPrimitiveContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.jsoup.Jsoup

/**
 * Lettura di una playlist di Spotify.
 *
 * La pagina pubblica di Spotify e' renderizzata lato client e non contiene le tracce, ma
 * la pagina "embed" espone un token anonimo e le prime tracce. Con quel token e' possibile
 * interrogare l'API GraphQL interna del web player ("pathfinder") che restituisce l'intera
 * playlist, paginata.
 */
object SpotifyPlaylist {

    private const val EMBED_URL = "https://open.spotify.com/embed/playlist/%s"
    private const val PATHFINDER_URL = "https://api-partner.spotify.com/pathfinder/v2/query"

    /**
     * Hash delle persisted query usate dal web player per leggere una playlist.
     * Il primo e' quello attualmente in uso, gli altri sono fallback nel caso in cui
     * Spotify lo cambi (a quel punto si ripiega sulla lista parziale dell'embed).
     */
    private val FETCH_PLAYLIST_HASHES = listOf(
        "21ea11a6ac75a5f3e01fd365d29c18e0a4a8b7dc09f89bbb5d73910190c6acaf"
    )

    private const val PAGE_SIZE = 100
    private const val MAX_PAGES = 100

    private val playlistIdRegex = Regex("""playlist/([A-Za-z0-9]{22})""", RegexOption.IGNORE_CASE)
    private val bareIdRegex = Regex("^[A-Za-z0-9]{22}$")

    /** Estrae l'id della playlist da un url (o da un id nudo). */
    fun parseId(urlOrId: String): String? {
        playlistIdRegex.find(urlOrId)?.let { return it.groupValues[1] }
        return urlOrId.trim().takeIf { it.matches(bareIdRegex) }
    }

    /**
     * Recupera una playlist completa.
     *
     * @return null se la playlist non esiste, e' privata o non e' raggiungibile.
     */
    suspend fun fetch(urlOrId: String): SpotifyPlaylistMeta? = withContext(Dispatchers.IO) {
        val id = parseId(urlOrId) ?: return@withContext null

        val embed = fetchEmbed(id)
        val token = embed?.accessToken

        // Dati completi via GraphQL; se fallisce restano i dati (eventualmente parziali) dell'embed
        val full = token?.let { fetchViaGraphQL(it, id) }

        val tracks = full?.tracks?.takeIf { it.isNotEmpty() } ?: embed?.tracks.orEmpty()

        if (full == null && tracks.isEmpty()) return@withContext null

        SpotifyPlaylistMeta(
            id = id,
            name = full?.name ?: embed?.name,
            thumbnailUrl = full?.thumbnailUrl ?: embed?.thumbnailUrl,
            description = full?.description ?: embed?.description,
            owner = full?.owner ?: embed?.owner,
            tracks = tracks,
            totalCount = full?.totalCount ?: (embed?.totalCount ?: tracks.size)
        )
    }

    // ---------------------------------------------------------------- embed

    private data class EmbedData(
        val accessToken: String?,
        val name: String?,
        val thumbnailUrl: String?,
        val description: String?,
        val owner: String?,
        val tracks: List<SpotifyTrackMeta>,
        val totalCount: Int
    )

    private suspend fun fetchEmbed(id: String): EmbedData? = runCatching {
        val html = SpotifyMeta.getHtml(EMBED_URL.format(id)) ?: return@runCatching null
        val nextData = Jsoup.parse(html).selectFirst("script#__NEXT_DATA__")?.data()
            ?: return@runCatching null
        val root = SpotifyMeta.json.parseToJsonElement(nextData).jsonObjectOrNull() ?: return@runCatching null

        val state = root["props"]?.jsonObjectOrNull()
            ?.get("pageProps")?.jsonObjectOrNull()
            ?.get("state")?.jsonObjectOrNull() ?: return@runCatching null

        val data = state["data"]?.jsonObjectOrNull()
        val entity = data?.get("entity")?.jsonObjectOrNull()

        // il token anonimo cambia posizione nel tempo: si cerca in tutto il documento
        val accessToken = findAccessToken(state)

        val tracks = entity?.get("trackList")?.jsonArrayOrNull().orEmpty()
            .mapNotNull { it.jsonObjectOrNull()?.toTrackMeta() }

        EmbedData(
            accessToken = accessToken,
            name = entity?.get("name")?.jsonPrimitiveContent(),
            thumbnailUrl = entity?.get("coverArt")?.jsonObjectOrNull()
                ?.get("sources")?.jsonArrayOrNull()
                ?.mapNotNull { it.jsonObjectOrNull() }
                ?.maxByOrNull { it.get("width")?.jsonPrimitiveContent()?.toIntOrNull() ?: 0 }
                ?.get("url")?.jsonPrimitiveContent(),
            description = null,
            owner = entity?.get("subtitle")?.jsonPrimitiveContent(),
            tracks = tracks,
            totalCount = tracks.size
        )
    }.getOrNull()

    /** cerca il token anonimo ("accessToken") in tutto il documento __NEXT_DATA__ */
    private fun findAccessToken(element: JsonElement?): String? {
        when (element) {
            is JsonObject -> {
                element["accessToken"]?.jsonPrimitiveContent()
                    ?.takeIf { it.isNotBlank() }?.let { return it }
                element.values.forEach { value ->
                    findAccessToken(value)?.let { return it }
                }
            }
            is JsonArray -> {
                element.forEach { value ->
                    findAccessToken(value)?.let { return it }
                }
            }
            else -> {}
        }
        return null
    }

    private fun JsonObject.toTrackMeta(): SpotifyTrackMeta? {
        val uri = this["uri"]?.jsonPrimitiveContent() ?: return null
        val trackId = uri.substringAfterLast(':').takeIf { it.isNotBlank() } ?: return null
        return SpotifyTrackMeta(
            id = trackId,
            name = this["title"]?.jsonPrimitiveContent(),
            artist = this["subtitle"]?.jsonPrimitiveContent(),
            durationMs = this["duration"]?.jsonPrimitiveContent()?.toLongOrNull()
        )
    }

    // ------------------------------------------------------------- graphQL

    private suspend fun fetchViaGraphQL(token: String, id: String): GraphQLPlaylist? {
        for (hash in FETCH_PLAYLIST_HASHES) {
            val result = fetchViaGraphQLWithHash(token, id, hash)
            if (result != null) return result
        }
        return null
    }

    private suspend fun fetchViaGraphQLWithHash(token: String, id: String, hash: String): GraphQLPlaylist? {
        val uri = "spotify:playlist:$id"
        val tracks = mutableListOf<SpotifyTrackMeta>()
        var offset = 0
        var totalCount = 0
        var header: GraphQLPlaylistV2? = null

        var page = 0
        while (page++ < MAX_PAGES) {
            val response = graphQLPost(
                token = token,
                operationName = "fetchPlaylist",
                hash = hash,
                variables = buildJsonObject {
                    put("uri", uri)
                    put("offset", offset)
                    put("limit", PAGE_SIZE)
                    put("enableWatchFeedEntrypoint", false)
                }
            ) ?: return null

            val playlist = response.data?.playlistV2
            if (playlist == null || playlist.errorMessage != null) return null

            if (header == null) header = playlist

            totalCount = playlist.content?.totalCount ?: 0
            val pageTracks = playlist.content?.items.orEmpty().mapNotNull { it.toTrackMeta() }
            tracks += pageTracks

            if (pageTracks.isEmpty() || totalCount <= tracks.size) break
            offset += PAGE_SIZE
        }

        return GraphQLPlaylist(
            name = header?.name,
            thumbnailUrl = header?.coverUrl(),
            description = header?.description,
            owner = header?.ownerName,
            totalCount = totalCount,
            tracks = tracks
        )
    }

    private data class GraphQLPlaylist(
        val name: String?,
        val thumbnailUrl: String?,
        val description: String?,
        val owner: String?,
        val totalCount: Int,
        val tracks: List<SpotifyTrackMeta>
    )

    private suspend fun graphQLPost(
        token: String,
        operationName: String,
        hash: String,
        variables: JsonObject
    ): GraphQLResponse? = runCatching {
        val request = GraphQLRequest(
            variables = variables,
            operationName = operationName,
            extensions = GraphQLExtensions(
                persistedQuery = GraphQLPersistedQuery(version = 1, sha256Hash = hash)
            )
        )

        val response = SpotifyMeta.client.post(PATHFINDER_URL) {
            header(HttpHeaders.Authorization, "Bearer $token")
            header("App-Platform", "WebPlayer")
            // senza il content type json l'API interpreta il corpo come una query GraphQL
            // "in chiaro" e lo rifiuta
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body<GraphQLResponse>()

        response.takeIf { it.errors.isNullOrEmpty() }
    }.onFailure {
        println("SpotifyPlaylist graphQLPost error ${it.message}")
    }.getOrNull()

    // ------------------------------------------------------------- modelli

    @Serializable
    private data class GraphQLRequest(
        val variables: JsonObject,
        val operationName: String,
        val extensions: GraphQLExtensions
    )

    @Serializable
    private data class GraphQLExtensions(
        val persistedQuery: GraphQLPersistedQuery
    )

    @Serializable
    private data class GraphQLPersistedQuery(
        val version: Int,
        val sha256Hash: String
    )

    @Serializable
    private data class GraphQLResponse(
        val data: GraphQLData? = null,
        val errors: List<GraphQLError>? = null
    )

    @Serializable
    private data class GraphQLError(
        val message: String? = null
    )

    @Serializable
    private data class GraphQLData(
        val playlistV2: GraphQLPlaylistV2? = null
    )

    @Serializable
    private data class GraphQLPlaylistV2(
        @SerialName("__typename") val type: String? = null,
        val name: String? = null,
        val description: String? = null,
        val uri: String? = null,
        val images: GraphQLImages? = null,
        val ownerV2: GraphQLOwner? = null,
        val content: GraphQLContent? = null,
        val message: String? = null
    ) {
        val errorMessage: String? get() = message

        fun coverUrl(): String? = images?.items
            ?.mapNotNull { it.sources?.maxByOrNull { src -> src.width ?: 0 }?.url }
            ?.firstOrNull { it.isNotBlank() }

        val ownerName: String? get() = ownerV2?.data?.name
    }

    @Serializable
    private data class GraphQLOwner(
        val data: GraphQLOwnerData? = null
    )

    @Serializable
    private data class GraphQLOwnerData(
        val name: String? = null,
        val uri: String? = null
    )

    @Serializable
    private data class GraphQLImages(
        val items: List<GraphQLImage>? = null
    )

    @Serializable
    private data class GraphQLImage(
        val sources: List<GraphQLImageSource>? = null
    )

    @Serializable
    private data class GraphQLImageSource(
        val url: String? = null,
        val width: Int? = null,
        val height: Int? = null
    )

    @Serializable
    private data class GraphQLContent(
        val totalCount: Int? = null,
        val items: List<GraphQLItem>? = null
    )

    @Serializable
    private data class GraphQLItem(
        val itemV2: GraphQLItemV2? = null
    )

    @Serializable
    private data class GraphQLItemV2(
        @SerialName("__typename") val type: String? = null,
        val data: GraphQLTrack? = null
    )

    @Serializable
    private data class GraphQLTrack(
        @SerialName("__typename") val type: String? = null,
        val uri: String? = null,
        val name: String? = null,
        val trackDuration: GraphQLDuration? = null,
        val artists: GraphQLArtists? = null,
        val albumOfTrack: GraphQLAlbum? = null
    )

    @Serializable
    private data class GraphQLDuration(
        val totalMilliseconds: Long? = null
    )

    @Serializable
    private data class GraphQLArtists(
        val items: List<GraphQLArtist>? = null
    )

    @Serializable
    private data class GraphQLArtist(
        val profile: GraphQLArtistProfile? = null
    )

    @Serializable
    private data class GraphQLArtistProfile(
        val name: String? = null,
        val uri: String? = null
    )

    @Serializable
    private data class GraphQLAlbum(
        val name: String? = null,
        val uri: String? = null
    )

    private fun GraphQLItem.toTrackMeta(): SpotifyTrackMeta? {
        val track = itemV2?.data ?: return null
        // solo tracce musicali: podcast/episodi non hanno un equivalente su YouTube Music
        if (track.type != null && track.type != "Track") return null

        val uri = track.uri ?: return null
        val id = uri.substringAfterLast(':').takeIf { it.isNotBlank() } ?: return null

        return SpotifyTrackMeta(
            id = id,
            name = track.name,
            artist = track.artists?.items
                ?.mapNotNull { it.profile?.name }
                ?.takeIf { it.isNotEmpty() }
                ?.joinToString(", "),
            album = track.albumOfTrack?.name,
            durationMs = track.trackDuration?.totalMilliseconds
        )
    }
}

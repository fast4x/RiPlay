package it.fast4x.spotifymeta

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.BrowserUserAgent
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.serialization.kotlinx.json.json
import it.fast4x.spotifymeta.utils.ProxyPreferences
import it.fast4x.spotifymeta.utils.getProxy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import org.jsoup.Jsoup

object SpotifyMeta {

    val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }

    // Matcha /track/, /album/, /artist/ ovunque nell'input:
    // url diretta, iframe_url dell'oEmbed, path intl-xx (…/intl-it/track/{id})
    private val entityRegex =
        Regex("""(track|album|artist)/([A-Za-z0-9]{22})""", RegexOption.IGNORE_CASE)
    private val bareIdRegex = Regex("^[A-Za-z0-9]{22}$")

    private val schemaTypeOf = mapOf(
        SpotifyEntityType.TRACK  to "MusicRecording",
        SpotifyEntityType.ALBUM  to "MusicAlbum",
        SpotifyEntityType.ARTIST to "MusicGroup"
    )

    @OptIn(ExperimentalSerializationApi::class)
    private val client by lazy {
        HttpClient(OkHttp) {
            BrowserUserAgent()

            expectSuccess = true

            install(ContentNegotiation) {
                json(json)
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
                url("https://open.spotify.com")
            }
        }
    }

    suspend fun oEmbedInfo(trackId: String): Result<SpotifyOEmbedResponse> =
        runCatching {
            val oembedUrl = "/oembed"
            val trackUrl = "/track/$trackId"
            val response = client.get(oembedUrl) {
                parameter("url", trackUrl)
            }.body<SpotifyOEmbedResponse>()

            response
        }.onFailure {
            println("Spotify spotifyOEmbedInfo error ${it.message}")
        }


    private const val UA =
        "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124 Mobile Safari/537.36"

    /** Entry point unico: accetta url, iframe_url o id nudo (→ assume track). */
    /*
    // Riconosce automaticamente il tipo dal link (track, album o artist)
    when (val meta = fetch("https://open.spotify.com/album/1DFixLWuPkv3KT3TnV35m3")) {
        is SpotifyTrackMeta  -> saveTrackMeta(meta)      // .artist, .album, .previewUrl
        is SpotifyAlbumMeta  -> saveAlbumMeta(meta)      // .artist, .releaseYear, .trackCount
        is SpotifyArtistMeta -> saveArtistMeta(meta)     // .name, .thumbnailUrl
        null                 -> Timber.d("No meta for link")
    }
    // Oppure, se chiamata diretta per il tipo voluto
    val album = fetchAs<SpotifyAlbumMeta>(albumUrl)
     */
    suspend fun fetch(urlOrId: String): SpotifyEntityMeta? = withContext(Dispatchers.IO) {
        val (type, id) = parse(urlOrId) ?: return@withContext null

        runCatching {
            val pageHtml = getHtml(pageUrl(type, id)) ?: return@runCatching null
            val schema = parseLdJson(type, id, pageHtml)

            // Embed solo se serve: track (preview/artists) o schema incompleto
            val needsEmbed = type == SpotifyEntityType.TRACK || schema == null
            val embedHtml = if (needsEmbed) getHtml(embedUrl(type, id)) else null
            val embed = embedHtml?.let { parseNextData(type, id, it) }

            val thumbnail = schema?.thumbnailUrl
                ?: embed?.thumbnailUrl
                ?: parseOgImage(pageHtml)
                ?: fetchOEmbedThumbnail(pageUrl(type, id))

            var result = merge(type, id, schema, embed, thumbnail)

            // Artist: fallback DOM (uniforme track/album)
            val domArtist = extractArtistsFromDom(pageHtml).firstOrNull()   // header = principale
            result = when (result) {
                is SpotifyTrackMeta  -> result.copy(
                    artist   = result.artist ?: domArtist?.second,
                    artistId = result.artistId ?: domArtist?.first
                )
                is SpotifyAlbumMeta  -> result.copy(
                    artist   = result.artist ?: domArtist?.second,
                    artistId = result.artistId ?: domArtist?.first
                )
                else -> result
            }

            // Track: album dal link HTML, con pulizia
            if (type == SpotifyEntityType.TRACK && result is SpotifyTrackMeta && result.album == null) {
                val a = Jsoup.parse(pageHtml).selectFirst("a[href*='/album/']")
                result = result.copy(
                    albumId = a?.attr("href")?.let { albumIdRegex.find(it)?.groupValues?.get(1) },
                    album   = cleanAlbumName(a?.ownText()?.trim()?.takeIf { it.isNotEmpty() } ?: a?.text())
                )
            }
            result?.takeIf { it.name != null }
        }.onFailure { println("SpotifyMeta fetch failed: $urlOrId message = ${it.message}") }
            .getOrNull()
    }

    /** Chiamata tipizzata, quando il chiamante conosce già la forma attesa. */
    suspend inline fun <reified T : SpotifyEntityMeta> fetchAs(urlOrId: String): T? =
        fetch(urlOrId) as? T

    // Parsing input
    private fun parse(input: String): Pair<SpotifyEntityType, String>? {
        entityRegex.find(input)?.let { m ->
            val type = SpotifyEntityType.valueOf(m.groupValues[1].uppercase())
            return type to m.groupValues[2]
        }
        return if (input.matches(bareIdRegex)) SpotifyEntityType.TRACK to input else null
    }

    private fun pageUrl(type: SpotifyEntityType, id: String) =
        "https://open.spotify.com/${type.name.lowercase()}/$id"

    private fun embedUrl(type: SpotifyEntityType, id: String) =
        "https://open.spotify.com/embed/${type.name.lowercase()}/$id"

    suspend fun getHtml(url: String): String? = runCatching {
        val response = client.get(url)
        response.bodyAsText()
    }.getOrNull()

    // Fonte 1: ld+json (schema.org)
    private fun parseLdJson(type: SpotifyEntityType, id: String, html: String): SpotifyEntityMeta? {
        val expected = schemaTypeOf.getValue(type)
        val nodes = Jsoup.parse(html)
            .select("script[type=application/ld+json]")
            .mapNotNull { runCatching { json.parseToJsonElement(it.data()) }.getOrNull() }
            .flatMap { flattenDeep(it) }

        // 1° il nodo tipizzato (annidato incluso) — 2° il wrapper MusicPlaylist
        val node = nodes.firstOrNull { it.typeIs(expected) }
            ?: nodes.firstOrNull { it.typeIs("MusicPlaylist") && type != SpotifyEntityType.ARTIST }
            ?: return null

        val thumbnailUrl = node.path("visualIdentity", "image")?.jsonArray
            ?.filterIsInstance<JsonObject>()
            ?.maxByOrNull { it.path("maxWidth").str()?.toIntOrNull() ?: 0 }
            ?.path("url").str()

        val artists = node.artists()
            ?: artistFromDescription(node.path("description").str())

        return when (type) {
            SpotifyEntityType.TRACK -> SpotifyTrackMeta(
                id = id,
                name = node.path("name").str(),
                thumbnailUrl = thumbnailUrl,
                artist = artists,
                // album può stare nel MusicRecording annidato → cerca anche lì
                album = node.path("inAlbum", "name").str()
                    ?: node.path("album", "name").str()
                    ?: nodes.firstOrNull { it.typeIs("MusicRecording") }
                        ?.path("inAlbum", "name").str()
            )
            SpotifyEntityType.ALBUM -> SpotifyAlbumMeta(
                id = id,
                name = node.path("name").str(),
                thumbnailUrl = thumbnailUrl,
                artist = artists,
                releaseYear = node.path("datePublished").str()?.take(4),
                trackCount = node.path("numTracks").str()?.toIntOrNull()
                    ?: node.path("track")?.jsonArray?.size
            )
            SpotifyEntityType.ARTIST -> SpotifyArtistMeta(
                id = id,
                name = node.path("name").str(),
                thumbnailUrl = thumbnailUrl
            )
        }
    }

    // Fonte 2: __NEXT_DATA__ dell'embed
    private fun parseNextData(type: SpotifyEntityType, id: String, html: String): SpotifyEntityMeta? {
        val node = Jsoup.parse(html).selectFirst("script#__NEXT_DATA__") ?: return null
        val root = runCatching { json.parseToJsonElement(node.data()) }.getOrNull() ?: return null
        val e = root.path("props", "pageProps", "state", "data", "entity") as? JsonObject
            ?: return null

        return when (type) {
            SpotifyEntityType.TRACK -> SpotifyTrackMeta(
                id = id,
                name = e.path("title").str(),
                thumbnailUrl = e.path("visualIdentity", "0", "image", "url").str()
                    ?: e.path("coverArt", "sources", "0", "url").str(),
                artist = e.embedArtists(),
                album = e.path("albumName").str(),
                previewUrl = e.path("audioPreview", "url").str()
            )
            SpotifyEntityType.ALBUM -> SpotifyAlbumMeta(
                id = id,
                name = e.path("title").str(),
                thumbnailUrl = e.path("coverArt", "sources", "0", "url").str(),
                artist = e.path("subtitle").str(),
                releaseYear = e.path("date", "year").str(),
                trackCount = e.path("tracks", "totalCount").str()?.toIntOrNull()
            )
            SpotifyEntityType.ARTIST -> SpotifyArtistMeta(
                id = id,
                name = e.path("title").str(),
                thumbnailUrl = e.path("visuals", "avatarImage", "sources", "0", "url").str()
                    ?: e.path("image", "url").str(),
                verified = when (e["verified"]) {
                    is JsonPrimitive -> (e["verified"] as JsonPrimitive).contentOrNull == "true"
                    else -> null
                }
            )
        }
    }

    private val artistIdRegex = Regex("""/artist/([A-Za-z0-9]{22})""")
    private val albumIdRegex  = Regex("""/album/([A-Za-z0-9]{22})""")

    /** Il primo link /artist/ nel document order è quello dell'header (sotto il titolo). */
    private fun extractArtistsFromDom(html: String): List<Pair<String, String>> {
        val seen = LinkedHashSet<Pair<String, String>>()
        for (a in Jsoup.parse(html).select("a[href*='/artist/']")) {
            val id = artistIdRegex.find(a.attr("href"))?.groupValues?.get(1) ?: continue
            val name = a.ownText().trim().takeIf { it.isNotEmpty() }
                ?: a.text().trim().takeIf { it.isNotEmpty() } ?: continue
            seen += id to name
            if (seen.size >= 5) break
        }
        return seen.toList()
    }

    /** "Listen to X on Spotify. Song · Rebel Road Music · 2026" → "Rebel Road Music" */
    private fun artistFromDescription(description: String?): String? {
        val tail = description?.substringAfterLast("Spotify.", "") ?: return null
        val parts = tail.split("·").map { it.trim() }
        return parts.getOrNull(1)?.takeIf { it.isNotEmpty() && it.toIntOrNull() == null }
    }

    /** "The Roadworker's Sessions, Vol. 2 EP • 2026" → "The Roadworker's Sessions, Vol. 2" */
    private fun cleanAlbumName(raw: String?): String? =
        raw?.substringBefore("•")?.trim()                        // via l'anno
            ?.replace(Regex("""\s+(Album|EP|Single|Compilation)$"""), "")  // via il tipo
            ?.takeIf { it.isNotBlank() }

    /** Cerca il link all'album nella pagina track: restituisce (albumId, albumName). */
    private fun extractAlbumLink(html: String): Pair<String, String>? {
        val doc = Jsoup.parse(html)
        // il primo link /album/ nel contenuto principale è quello del track
        for (a in doc.select("a[href*='/album/']")) {
            val id = "/album/([A-Za-z0-9]{22})".toRegex()
                .find(a.attr("href"))?.groupValues?.get(1) ?: continue
            val name = a.text().trim().takeIf { it.isNotEmpty() }
                ?: a.attr("aria-label").takeIf { it.isNotEmpty() }
            return id to (name ?: "")
        }
        return null
    }

    // Merge: schema vince su name/artist/album, embed vince su cover, preview solo embed
    private fun merge(
        type: SpotifyEntityType, id: String,
        a: SpotifyEntityMeta?, b: SpotifyEntityMeta?,
        thumbnail: String?
    ): SpotifyEntityMeta? {
        if (a == null && b == null) return null

        fun pick(pref: String?, fb: String?) = pref?.takeIf { it.isNotBlank() } ?: fb

        return when (type) {
            SpotifyEntityType.TRACK -> {
                val x = a as? SpotifyTrackMeta; val y = b as? SpotifyTrackMeta
                SpotifyTrackMeta(
                    id = id,
                    name = pick(x?.name, y?.name),
                    thumbnailUrl = thumbnail, //pick(y?.thumbnailUrl, x?.thumbnailUrl),
                    artist = pick(x?.artist, y?.artist),
                    album = pick(x?.album, y?.album),
                    previewUrl = y?.previewUrl
                )
            }
            SpotifyEntityType.ALBUM -> {
                val x = a as? SpotifyAlbumMeta; val y = b as? SpotifyAlbumMeta
                SpotifyAlbumMeta(
                    id = id,
                    name = pick(x?.name, y?.name),
                    thumbnailUrl = thumbnail, //pick(y?.thumbnailUrl, x?.thumbnailUrl),
                    artist = pick(x?.artist, y?.artist),
                    releaseYear = pick(x?.releaseYear, y?.releaseYear),
                    trackCount = x?.trackCount ?: y?.trackCount
                )
            }
            SpotifyEntityType.ARTIST -> {
                val x = a as? SpotifyArtistMeta; val y = b as? SpotifyArtistMeta
                SpotifyArtistMeta(
                    id = id,
                    name = pick(x?.name, y?.name),
                    thumbnailUrl = thumbnail, //pick(y?.thumbnailUrl, x?.thumbnailUrl),
                    verified = x?.verified ?: y?.verified
                )
            }
        }.takeIf { it.name != null }
    }

    // Helpers JSON
    private fun flatten(root: JsonElement): List<JsonObject> = when (root) {
        is JsonObject -> (root["@graph"] as? JsonArray)
            ?.mapNotNull { it as? JsonObject } ?: listOf(root)
        is JsonArray -> root.mapNotNull { it as? JsonObject }
        else -> emptyList()
    }

    /** Scende ricorsivamente in tutto il documento: becca anche MusicRecording
     *  annidati dentro MusicPlaylist.track e nodi dentro array. */
    private fun flattenDeep(
        root: JsonElement,
        out: MutableList<JsonObject> = mutableListOf()
    ): List<JsonObject> {
        when (root) {
            is JsonObject -> { out += root; root.values.forEach { flattenDeep(it, out) } }
            is JsonArray  -> root.forEach { flattenDeep(it, out) }
            else -> {}
        }
        return out
    }

    private fun JsonObject.typeIs(expected: String): Boolean = when (val t = this["@type"]) {
        is JsonPrimitive -> t.contentOrNull == expected
        is JsonArray -> t.any { (it as? JsonPrimitive)?.contentOrNull == expected }
        else -> false
    }

    private val artistKeys = listOf("byArtist", "artist", "artists", "creator")
    private val albumKeys  = listOf("inAlbum", "album")

    private fun JsonObject.firstField(keys: List<String>): JsonElement? =
        keys.firstNotNullOfOrNull { this[it] }

    /** byArtist può essere: array di oggetti, oggetto singolo, o stringa. */
    private fun JsonObject.artists(): String? {
        val raw = firstField(artistKeys) ?: return null
        return when (raw) {
            is JsonArray -> raw.mapNotNull {
                (it as? JsonObject)?.path("name").str() ?: (it as? JsonPrimitive)?.contentOrNull
            }.joinToString(", ").takeIf { it.isNotBlank() }
            is JsonObject -> raw.path("name").str()
            is JsonPrimitive -> raw.contentOrNull
            else -> null
        }
    }

    private fun JsonElement?.jsonArrayOrNull(): JsonArray? = this as? JsonArray

    private fun JsonObject.embedArtists(): String? =
        this["artists"]?.jsonArrayOrNull()
            ?.mapNotNull { (it as? JsonObject)?.path("name").str() }
            ?.joinToString(", ")
            ?.takeIf { it.isNotBlank() }
            ?: path("subtitle").str()
            ?: path("artist", "name").str()


    private fun JsonElement?.path(vararg keys: String): JsonElement? {
        var current: JsonElement? = this
        for (key in keys) {
            current = when (current) {
                is JsonObject -> current[key]
                is JsonArray -> key.toIntOrNull()?.let { current.getOrNull(it) }
                else -> null
            } ?: return null
        }
        return current
    }

    private fun JsonElement?.str(): String? = (this as? JsonPrimitive)?.contentOrNull


    // Layer 0: og:image dalla STESSA pagina del ld+json (zero richieste extra)
    private fun parseOgImage(html: String): String? =
        Jsoup.parse(html)
            .selectFirst("meta[property=og:image], meta[name=twitter:image]")
            ?.attr("content")
            ?.takeIf { it.isNotBlank() }

    // Layer 1: oEmbed — funziona per track, album E artist
    private suspend fun fetchOEmbedThumbnail(entityUrl: String): String? = runCatching {
        val oembedUrl = "/oembed"
        val response = client.get(oembedUrl) {
            parameter("url", entityUrl)
        }.body<SpotifyOEmbedResponse>()
        response.thumbnailUrl
    }.getOrNull()

    // Layer 2: deep-scan dei CDN immagine dentro __NEXT_DATA__
    private val imageUrlRegex = Regex(
        """https://(?:i\.scdn\.co|image-cdn[a-z.\-]*\.spotifycdn\.com)/image/[A-Za-z0-9]+"""
    )

    private fun deepScanImageUrls(html: String, preferArtistArt: Boolean): String? {
        val data = Jsoup.parse(html).selectFirst("script#__NEXT_DATA__")?.data() ?: return null
        val root = runCatching { json.parseToJsonElement(data) }.getOrNull() ?: return null

        val found = LinkedHashSet<String>()
        fun walk(e: JsonElement) {
            when (e) {
                is JsonPrimitive -> e.contentOrNull?.let {
                    imageUrlRegex.find(it)?.value?.let(found::add)
                }
                is JsonObject -> e.values.forEach(::walk)
                is JsonArray   -> e.forEach(::walk)
            }
        }
        walk(root)
        if (found.isEmpty()) return null

        // euristica sui prefissi hash: ab676161 = immagini artista, ab67616d = cover album
        val preferred = if (preferArtistArt) "ab676161" else "ab67616d"
        return found.firstOrNull { it.contains(preferred) } ?: found.first()
    }

    /** Utility per verificare il contenuto della pagina sorgente */
    suspend fun dumpSource(url: String) {   // es. dumpSource("https://open.spotify.com/track/46nK...")
        val (type, id) = parse(url) ?: return
        getHtml(pageUrl(type, id))?.let { html ->
            Jsoup.parse(html).select("script[type=application/ld+json]")
                .forEachIndexed { i, s -> println("SpotifyMeta LDJSON[$i]: ${s.data().take(2500)}") }
        }
        getHtml(embedUrl(type, id))?.let { html ->
            val entity = Jsoup.parse(html).selectFirst("script#__NEXT_DATA__")?.data()
                ?.let { json.parseToJsonElement(it).path("props", "pageProps", "state", "data", "entity") as? JsonObject }
            println("SpotifyMeta EMBED entity keys: ${ entity?.keys ?: "NULL" }")
            println("SpotifyMeta EMBED entity: ${entity?.toString()?.take(2500) ?: " - "}")
        }
    }

}
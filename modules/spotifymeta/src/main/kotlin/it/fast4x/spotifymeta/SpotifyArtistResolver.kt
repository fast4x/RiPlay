package it.fast4x.spotifymeta

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.BrowserUserAgent
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import it.fast4x.spotifymeta.utils.ProxyPreferences
import it.fast4x.spotifymeta.utils.getProxy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * RISOLUZIONE SPOTIFY ARTIST ID (backend, piattaforma-agnostico).
 *
 * Perche' esiste: il resolver MusicBrainz (url-rels → spotifyId) non copre
 * tutti gli artisti: molti (soprattutto indie/nicchia) non hanno un link
 * spotify su MusicBrainz. La ricerca testuale IN-APP su Spotify NON e'
 * fattibile (verificato 2025-11): l'endpoint dei token web-player e'
 * bloccato per policy ("Usage of this endpoint is not permitted...") e la
 * pagina /search/<query> e' una SPA che non contiene risultati nel HTML.
 *
 * STRATEGIA IN DUE LIVELLI:
 *
 * LIVELLO 1 — AUTOMATICO via Wikidata (P1902 = "Spotify artist ID"):
 *   API pubblica e libera di Wikimedia (wbsearchentities + EntityData),
 *   nessuna API key. Copre artisti assenti da MusicBrainz ma con l'ID
 *   Spotify sulla loro scheda Wikidata. Verificato: Taylor Swift ->
 *   Q26876 -> P1902 = 06HL4z0CvFAxyc27GXpf02 (ID esatto).
 *
 * LIVELLO 2 — MANUALE ASSISTITO (la "ricerca testuale" dell'utente, usata
 *   in modo conforme): l'app apre nel BROWSER dell'utente la ricerca
 *   Spotify (vedi [searchUrl]), l'utente tocca l'artista giusto, copia
 *   l'URL e lo incolla nell'app. L'ID viene validato con [parseArtistId]
 *   e il match confermabile con SpotifyMeta.fetch() (la pagina artistica
 *   SI puo' leggere: e' lo stesso meccanismo gia' in produzione).
 *
 * Entrambi i livelli restituiscono lo stesso tipo: un ID 22 char base62
 * pronto per Artist.spotifyId e per il form GitHub suggestion di ZoundHub.
 */
object SpotifyArtistResolver {

    private val json = Json { ignoreUnknownKeys = true }

    /** 22 char base62: il formato canonico degli ID Spotify. */
    private val artistIdRegex = Regex("""^[A-Za-z0-9]{22}$""")

    /** Estrae l'ID da …/artist/<id> in qualunque posizione (url, intl-xx, embed). */
    private val artistPathRegex = Regex("""artist/([A-Za-z0-9]{22})""", RegexOption.IGNORE_CASE)

    /**
     * Validazione/estrazione dell'ID artista da un input incollato
     * dall'utente. Accetta:
     *  - url completo   https://open.spotify.com/artist/<id>[?...]
     *  - url con locale https://open.spotify.com/intl-it/artist/<id>
     *  - uri spotify    spotify:artist:<id>
     *  - id nudo        <id> (22 char base62)
     * Restituisce null se l'input non contiene un ID artista valido.
     */
    fun parseArtistId(input: String?): String? {
        val trimmed = input?.trim().orEmpty()
        if (trimmed.isEmpty()) return null
        artistPathRegex.find(trimmed)?.let { return it.groupValues[1] }
        if (trimmed.startsWith("spotify:artist:")) {
            val id = trimmed.substringAfter("spotify:artist:").substringBefore("?")
            if (artistIdRegex.matches(id)) return id
        }
        return if (artistIdRegex.matches(trimmed)) trimmed else null
    }

    /**
     * URL della ricerca testuale su open.spotify.com, da aprire nel BROWSER
     * dell'utente (non scaricabile come HTML: la pagina e' una SPA).
     * Il titolo "Artist suggestion" nel form GitHub resta pre-compilato:
     * questo link serve solo a far trovare e copiare l'URL giusto.
     */
    fun searchUrl(artistName: String): String =
        "https://open.spotify.com/search/" +
                java.net.URLEncoder.encode(artistName, Charsets.UTF_8).replace("+", "%20")

    /** Risultato del livello automatico: entita' Wikidata + ID Spotify trovato. */
    data class WikidataMatch(
        val entityId: String,      // es. "Q26876"
        val entityLabel: String?,  // es. "Taylor Swift" (label Wikidata)
        val spotifyId: String      // 22 char base62
    )

    private val userAgent =
        "RiPlay/1.0 (https://github.com/fast4x/RiPlay)"

    /**
     * HTTP client dedicato: base wikidata.org (diversa da SpotifyMeta),
     * stessa filosofia dei client del modulo (proxy utente incluso).
     */
    private val wikidataClient by lazy {
        HttpClient(OkHttp) {
            BrowserUserAgent()
            install(ContentEncoding) {
                gzip()
                deflate()
            }
            ProxyPreferences.preference?.let {
                engine {
                    proxy = getProxy(it)
                }
            }
        }
    }

    /**
     * LIVELLO 1: cerca l'artista su Wikidata e restituisce il primo match
     * con l'ID Spotify (P1902). null = nessun risultato utilizzabile:
     * il chiamante passa al livello 2 (ricerca manuale nel browser).
     *
     * Passi: wbsearchentities (limit 5, etichette fuzzy) -> per ogni
     * entita' EntityData -> claims.P1902 -> primo value con rank non
     * deprecated. Matcha la label in modo case-insensitive quando
     * presente per scartare gli omonimi evidenti (es. videogiochi).
     */
    suspend fun resolveViaWikidata(artistName: String): Result<WikidataMatch?> =
        withContext(Dispatchers.IO) {
            runCatching {
                val name = artistName.trim()
                if (name.isEmpty()) return@runCatching null

                val candidates = searchEntities(name)
                for ((entityId, label) in candidates) {
                    val spotifyId = spotifyIdOf(entityId) ?: continue
                    // anti-omonimia soft: se la label Wikidata esiste e
                    // non somiglia al nome cercato, si scarta il candidato
                    // (evita di restituire l'ID di un'entita' omonima).
                    if (!label.isNullOrBlank() && !similarEnough(label, name)) continue
                    return@runCatching WikidataMatch(entityId, label, spotifyId)
                }
                null
            }.onFailure {
                println("SpotifyArtistResolver wikidata error: ${it.message}")
            }
        }

    /**
     * Confronto permissivo (prefisso/parola iniziale, senza accenti):
     * serve solo a filtrare gli omonimi evidenti, non a fare fuzzy matching
     * serio — il match definitivo resta all'utente nella conferma.
     */
    private fun similarEnough(a: String, b: String): Boolean {
        val x = a.lowercase().replace(Regex("""[\p{M}]"""), "")
        val y = b.lowercase().replace(Regex("""[\p{M}]"""), "")
        if (x == y) return true
        if (x.startsWith(y) || y.startsWith(x)) return true
        if (x.contains(y) || y.contains(x)) return true
        return false
    }

    /**
     * wbsearchentities: restituisce fino a [limit] entita' come
     * (id, label). Richiede un User-Agent descrittivo (policy Wikimedia).
     */
    private suspend fun searchEntities(name: String, limit: Int = 5): List<Pair<String, String?>> =
        runCatching {
            val body = wikidataClient.get(
                "https://www.wikidata.org/w/api.php"
            ) {
                header("User-Agent", userAgent)
                url {
                    parameters.apply {
                        append("action", "wbsearchentities")
                        append("search", name)
                        append("language", "en")
                        append("type", "item")
                        append("limit", limit.toString())
                        append("format", "json")
                    }
                }
            }.bodyAsText()

            json.parseToJsonElement(body)
                .pathElements("search").asArrayOrNull()
                ?.mapNotNull { el ->
                    val obj = el as? JsonObject ?: return@mapNotNull null
                    val id = (obj["id"] as? JsonPrimitive)?.contentOrNull ?: return@mapNotNull null
                    val label = obj.pathElements("label").asPrimitiveOrNull()?.contentOrNull
                    id to label
                }.orEmpty()
        }.getOrElse { emptyList() }

    /**
     * claims.P1902 dell'entita': primo ID Spotify con rank non deprecated.
     * (P1902 puo' avere qualificatori di validita' temporale per artisti
     * rinominati: i rank deprecated scartati evitano ID revocati.)
     */
    private suspend fun spotifyIdOf(entityId: String): String? = runCatching {
        val body = wikidataClient.get(
            "https://www.wikidata.org/wiki/Special:EntityData/$entityId.json"
        ) {
            header("User-Agent", userAgent)
        }.bodyAsText()

        val claims = json.parseToJsonElement(body)
            .pathElements("entities", entityId, "claims")
            .asObjectOrNull() ?: return@runCatching null

        val p1902 = claims["P1902"].asArrayOrNull() ?: return@runCatching null
        for (claim in p1902) {
            val obj = claim as? JsonObject ?: continue
            if ((obj["rank"] as? JsonPrimitive)?.contentOrNull == "deprecated") continue
            val value = obj.pathElements("mainsnak", "datavalue", "value")
                .asPrimitiveOrNull()?.contentOrNull ?: continue
            if (artistIdRegex.matches(value)) return@runCatching value
        }
        null
    }.getOrNull()

    // ── Helpers JSON (path-based, stesso stile di SpotifyMeta) ─────────────

    private fun JsonElement?.pathElements(vararg keys: String): JsonElement? {
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

    private fun JsonElement?.asArrayOrNull(): JsonArray? = this as? JsonArray
    private fun JsonElement?.asObjectOrNull(): JsonObject? = this as? JsonObject
    private fun JsonElement?.asPrimitiveOrNull(): JsonPrimitive? = this as? JsonPrimitive
}

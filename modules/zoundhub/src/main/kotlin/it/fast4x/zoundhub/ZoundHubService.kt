package it.fast4x.zoundhub

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.BrowserUserAgent
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.compression.brotli
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import it.fast4x.zoundhub.models.ZoundHubArtistPage
import it.fast4x.zoundhub.models.ZoundHubCatalog
import it.fast4x.zoundhub.models.ZoundHubNameEntry
import it.fast4x.zoundhub.models.ZoundHubStats
import it.fast4x.zoundhub.utils.ProxyPreferences
import it.fast4x.zoundhub.utils.getProxy
import kotlinx.serialization.json.Json

/**
 * Client HTTP per i dati pubblici di ZoundHub (https://zoundhub.com).
 *
 * L'"Artist API" di ZoundHub e' un set di file JSON statici pubblicati dal
 * sito (risorsa gratuita e open source, nessuna API key necessaria):
 *
 *  - /data/artists-all.json         -> catalogo COMPLETO (~3 MB, ~9.700 artisti)
 *  - /data/artists-initial.json     -> sottoinsieme usato dal sito al primo load
 *  - /data/artist-names.json        -> mappa { spotifyId: {name, slug} } (~680 KB)
 *  - /data/artist-stats.json        -> contatori globali
 *  - /data/artist-pages/{0..63}.json -> schede dettagliate partizionate per
 *    slug (hash Java-31 % 64), usate dal sito per la pagina singola artista
 *
 * NOTA TECNICA IMPORTANTE (verificata empiricamente): zoundhub.com e' dietro
 * Cloudflare e gli endpoint sotto /data/ rispondono 403 (challenge) ai client che
 * non dichiarano uno User-Agent da browser. Con un User-Agent browser la
 * risposta e' 200: per questo il client usa BrowserUserAgent() come i
 * service esistenti (LastFmService, ecc.).
 */
class ZoundHubService(
    private val baseUrl: String = BASE_URL
) {

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
            }

            install(ContentEncoding) {
                gzip()
                deflate()
                brotli()
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

    /** Catalogo completo di tutti gli artisti segnalati AI. */
    suspend fun fetchCatalog(): Result<ZoundHubCatalog> = try {
        Result.success(client.get { url(PATH_CATALOG_ALL) }.body())
    } catch (e: Exception) {
        Result.failure(e)
    }

    /** Mappa spotifyId -> {name, slug} (leggera, utile per match rapidi). */
    suspend fun fetchArtistNames(): Result<Map<String, ZoundHubNameEntry>> = try {
        Result.success(client.get { url(PATH_ARTIST_NAMES) }.body())
    } catch (e: Exception) {
        Result.failure(e)
    }

    /** Contatori globali del catalogo (artisti, release, elementi flaggati AI). */
    suspend fun fetchStats(): Result<ZoundHubStats> = try {
        Result.success(client.get { url(PATH_ARTIST_STATS) }.body())
    } catch (e: Exception) {
        Result.failure(e)
    }

    /** Bucket shardato di schede artista (0..63). */
    suspend fun fetchArtistPage(bucket: Int): Result<ZoundHubArtistPage> = try {
        Result.success(client.get { url("$PATH_ARTIST_PAGES/$bucket.json") }.body())
    } catch (e: Exception) {
        Result.failure(e)
    }

    /** Schede del bucket che contiene (con alta probabilita') lo slug dato. */
    suspend fun fetchBucketForSlug(slug: String): Result<ZoundHubArtistPage> =
        fetchArtistPage(slugBucket(slug))

    companion object {
        const val BASE_URL = "https://zoundhub.com"

        const val PATH_CATALOG_ALL = "/data/artists-all.json"
        const val PATH_CATALOG_INITIAL = "/data/artists-initial.json"
        const val PATH_ARTIST_NAMES = "/data/artist-names.json"
        const val PATH_ARTIST_STATS = "/data/artist-stats.json"
        const val PATH_ARTIST_PAGES = "/data/artist-pages"

        /** URL pubblico della scheda artista (usato dai link "apri pagina"). */
        fun artistPageUrl(slug: String): String = "$BASE_URL/artist/$slug"

        /**
         * Bucket (0..63) di una scheda artista, replica esatta della
         * funzione JavaScript del sito:
         *   R = slug => { let t = 0; for (ch of slug) t = (t*31 + ch.charCodeAt(0)) >>> 0; return t % 64 }
         *
         * In Kotlin/Java hashCode() della String e' lo stesso algoritmo
         * h*31+c con overflow a 32 bit; per un modulo potenza di due
         * (2^6 = 64) il risultato coincide con t % 64 su valore unsigned:
         * basta mascherare i 6 bit bassi (and 63), anche per hash negativi.
         */
        fun slugBucket(slug: String): Int = slug.hashCode() and 63
    }
}

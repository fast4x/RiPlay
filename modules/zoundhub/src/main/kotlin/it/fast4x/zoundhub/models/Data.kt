package it.fast4x.zoundhub.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Record di un artista nel catalogo ZoundHub (https://zoundhub.com).
 *
 * ZoundHub e' una directory open source (MIT) di "segnali AI" associati ad
 * artisti musicali: aggrega etichette delle piattaforme (es. rilevamento AI
 * di Deezer), analisi di terze parti (SubmitHub AI Song Checker) e metadati
 * delle release.
 *
 * ATTENZIONE SEMANTICA: ZoundHub segnala a livello di ARTISTA. Il campo
 * `releases` e' SOLO un contatore delle release pubblicate dall'artista e
 * NON una lista di release con flag individuale. Eventuali badge su album o
 * brani sono quindi una PROPAGAZIONE del segnale dell'artista.
 *
 * Nota sui nomi dei campi: il JSON pubblico usa snake_case, mappato qui su
 * camelCase via @SerialName (il Json del service ha ignoreUnknownKeys = true,
 * quindi eventuali campi nuovi aggiunti da ZoundHub non rompono il parsing).
 */
@Serializable
data class ZoundHubArtist(
    /** Spotify artist ID (base62 a 22 caratteri, es. "7jL3ObaJuAVhTAK0VEF2qL"). */
    val id: String,
    /** Slug usato negli URL publici: https://zoundhub.com/artist/{slug} */
    val slug: String,
    /** Nome dell'artista cosi' come pubblicato dal catalogo. */
    val name: String,
    /** True se il catalogo dispone di un'immagine per l'artista. */
    val image: Boolean? = null,
    /** Timestamp ISO-8601 dell'inserimento nel catalogo. */
    val added: String? = null,
    /** Timestamp ISO-8601 dell'ultimo aggiornamento del record. */
    val updated: String? = null,
    /**
     * Punteggio SubmitHub "AI song checker" (0..100). Valori alti indicano
     * segnali AI piu' forti secondo l'analisi di terze parti.
     */
    @SerialName("sh_score") val shScore: Double? = null,
    /** Numero di release pubblicate (SOLO un conteggio, non un elenco). */
    val releases: Int? = null,
    /** Anno di debutto pubblicato dal catalogo (stringa, es. "2026"). */
    val debut: String? = null,
    /**
     * Segnalazione del rilevamento AI di Deezer:
     * true = flaggato, false = non flaggato, null = dato assente.
     */
    val deezer: Boolean? = null,
    val followers: Long? = null,
    val listeners: Long? = null,
    @SerialName("top_streams") val topStreams: Long? = null,
    /** Valutazione "AI" del catalogo ZoundHub su scala 1..5. */
    val rating: Int? = null,
    /** Z-score interno del catalogo (0..1), indicatore statistico del segnale. */
    val zscore: Double? = null,
)

/** Catalogo completo: payload di https://zoundhub.com/data/artists-all.json */
@Serializable
data class ZoundHubCatalog(
    /** Anno di riferimento delle statistiche aggregate (es. 2026). */
    @SerialName("reference_year") val referenceYear: Int? = null,
    /** Totale dichiarato degli artisti (coerente con artists.size). */
    val total: Int? = null,
    val artists: List<ZoundHubArtist> = emptyList(),
)

/** Singola voce di https://zoundhub.com/data/artist-names.json (id -> nome/slug). */
@Serializable
data class ZoundHubNameEntry(
    val name: String,
    val slug: String,
)

/** Statistiche globali del catalogo: payload di /data/artist-stats.json */
@Serializable
data class ZoundHubStats(
    @SerialName("artist_count") val artistCount: Int = 0,
    @SerialName("release_count") val releaseCount: Long = 0,
    @SerialName("ai_flagged_count") val aiFlaggedCount: Int = 0,
)

/**
 * Pagina artista "shardata": le schede dettagliate sono partizionate in 64
 * bucket (/data/artist-pages/{0..63}.json) via hash Java-31 dello slug.
 * Payload di un bucket.
 */
@Serializable
data class ZoundHubArtistPage(
    @SerialName("reference_year") val referenceYear: Int? = null,
    val artists: List<ZoundHubArtist> = emptyList(),
)

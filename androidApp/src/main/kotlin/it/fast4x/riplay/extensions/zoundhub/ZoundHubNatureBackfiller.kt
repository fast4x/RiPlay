package it.fast4x.riplay.extensions.zoundhub

import it.fast4x.riplay.data.Database
import it.fast4x.riplay.enums.ArtistNature
import it.fast4x.zoundhub.ZoundHubCatalogState
import it.fast4x.zoundhub.ZoundHubCatalogStore
import it.fast4x.zoundhub.ZoundHubMatch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Backfill della natura ArtistNature.AI_GENERATED nella libreria (tabella Room
 * Artist) usando il catalogo ZoundHub.
 *
 * DESIGN concordato (4 regole):
 *  1. SOLO ARTISTI: ZoundHub segnala a livello artista (releases è solo un
 *     conteggio) — non si toccano Song/Album.
 *  2. SOLO MATCH ESATTI PERSISTITI: si scrive AI_GENERATED solo quando
 *      - lo Spotify ID dell'artista (da MusicBrainz url-rels) è nel catalogo
 *        -> match per ID, affidabilità massima;
 *      - oppure il nome normalizzato coincide esattamente (match.exact).
 *     I match FUZZY ("AI?", Jaro-Winkler >= 0.92) restano confinati nella UI:
 *     non si etichetta permanentemente un artista partendo da una probabilità.
 *  3. PRECEDENZA: ZoundHub (catalogo verificato) vince sulle keyword euristiche
 *     per AI_GENERATED (sovrascrive HUMAN e UNKNOWN); VIRTUAL/COVER_TRIBUTE/
 *     COMPILATION derivate dalle keyword restano intatte (ZoundHub non esprime
 *     nulla su quelle categorie).
 *  4. MAI SUL MAIN THREAD: gira su Dispatchers.IO, una volta per versione di
 *     catalogo (fetchedAt), protetto da Mutex contro run concorrenti.
 *
 * Il marcamento è DUREVOLE: sopravvive alla cancellazione della cache del
 * catalogo, funziona 100% offline, ed è QUERYABILE via
 * ArtistDao.getArtistsByNature(ArtistNature.AI_GENERATED).
 */
object ZoundHubNatureBackfiller {

    private val mutex = Mutex()

    /** Versione di catalogo (fetchedAt) dell'ultimo backfill completato. */
    @Volatile
    private var lastBackfilledFor: Long? = null

    /**
     * Esegue il backfill una sola volta per versione di catalogo.
     * Chiamato quando lo store emette stato Ready (cache a freddo o refresh).
     */
    suspend fun runIfNeeded(
        store: ZoundHubCatalogStore,
        force: Boolean = false
    ): BackfillResult? = withContext(Dispatchers.IO) {
        val state = store.state.value
        if (state.status != ZoundHubCatalogState.Status.Ready) return@withContext null
        val catalogVersion = state.fetchedAt ?: return@withContext null
        if (!force && lastBackfilledFor == catalogVersion) return@withContext null

        mutex.withLock {
            // doppio controllo dentro il lock (run concorrenti)
            if (!force && lastBackfilledFor == catalogVersion) return@withLock null
            val result = try {
                backfill(store)
            } catch (e: Exception) {
                Timber.tag("ZoundHubNature").w(e, "Backfill natura fallito")
                null
            }
            if (result != null) lastBackfilledFor = catalogVersion
            result
        }
    }

    /**
     * Marchia gli artisti della libreria presenti nel catalogo ZoundHub.
     */
    suspend fun backfill(store: ZoundHubCatalogStore): BackfillResult =
        withContext(Dispatchers.IO) {
            val dao = Database.artistDao()
            val artists = dao.getAllWithName()

            var markedById = 0
            var markedByName = 0

            for (artist in artists) {
                // Regola 3: le classificazioni keyword che ZoundHub non valuta restano
                if (artist.nature == ArtistNature.VIRTUAL ||
                    artist.nature == ArtistNature.COVER_TRIBUTE ||
                    artist.nature == ArtistNature.COMPILATION ||
                    artist.nature == ArtistNature.AI_GENERATED
                ) continue

                // Regola 2: prima lo Spotify ID (esatto per ID), poi il nome esatto
                val byIdMatch = artist.spotifyId?.let { store.lookupBySpotifyId(it) }
                val match: ZoundHubMatch? = byIdMatch?.let {
                    ZoundHubMatch(artist = it, exact = true, similarity = 1.0)
                } ?: store.lookup(artist.name)?.takeIf { it.exact }

                if (match != null) {
                    dao.upsert(artist.copy(nature = ArtistNature.AI_GENERATED))
                    if (byIdMatch != null) markedById++ else markedByName++
                    Timber.tag("ZoundHubNature")
                        .i("  ♪ ${artist.name} → AI_GENERATED via ${match.artist.name}" +
                                (if (byIdMatch != null) " (spotifyId)" else " (nome esatto)"))
                }
            }

            val result = BackfillResult(
                scanned = artists.size,
                markedById = markedById,
                markedByName = markedByName
            )
            Timber.tag("ZoundHubNature")
                .i("=== DONE: scanned=${result.scanned}, byId=${result.markedById}, byName=${result.markedByName} ===")
            result
        }

    data class BackfillResult(
        val scanned: Int,
        val markedById: Int,
        val markedByName: Int
    )
}

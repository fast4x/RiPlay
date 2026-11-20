package it.fast4x.zoundhub

import it.fast4x.zoundhub.models.ZoundHubArtist
import it.fast4x.zoundhub.models.ZoundHubCatalog
import it.fast4x.zoundhub.models.ZoundHubStats
import it.fast4x.zoundhub.ZoundHubMatcher.Index
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/** Stato corrente del catalogo ZoundHub osservabile dalla UI. */
data class ZoundHubCatalogState(
    val status: ZoundHubCatalogState.Status = ZoundHubCatalogState.Status.Idle,
    /** Catalogo corrente (dalla cache o dal refresh). */
    val catalog: ZoundHubCatalog? = null,
    /** Epoch millis dell'ultimo download riuscito (null = mai aggiornato). */
    val fetchedAt: Long? = null,
    /** Statistiche globali scaricate azienda all'ultimo refresh (opzionali). */
    val stats: ZoundHubStats? = null,
    /** Messaggio d'errore dell'ultimo refresh fallito. */
    val error: String? = null,
) {
    enum class Status { Idle, Loading, Ready, Failed }

    /** Numero di artisti AI disponibili nell'indice corrente. */
    val artistCount: Int
        get() = catalog?.artists?.size ?: 0
}

/**
 * Store locale del catalogo ZoundHub: scarica una volta (3 MB circa per
 * ~9.700 artisti), persiste su file e risponde ai lookup in memoria
 * (100% offline dopo il primo refresh).
 *
 * Thread-safety: l'indice e lo state vengono sostituiti ATOMICAMENTE
 * (riferimenti immutabili), i lookup non bloccano mai la lettura.
 *
 * PERF v1.3 (filtro reattivo "nascondi contenuti AI"): [isAiHidden] espone
 * la decisione "questo testo autori e' di un artista AI?" con una CACHE
 * PROCESSUALE (ConcurrentHashMap) condivisa da TUTTE le schermate: il costo
 * del match (esatto O(1), fuzzy con early-exit aritmetico) per un testo viene
 * pagato UNA sola volta per processo, non per schermata/per composizione.
 * La cache e' thread-safe (si puo' chiamare da Dispatchers.Default o IO
 * durante il filtraggio in background) e viene invalidata ad ogni
 * [rebuild] (cache a freddo, refresh manuale) tramite il contatore
 * [generation].
 *
 * PERF v1.4 (dispositivi vecchi/lenti) — DUE nuovita':
 *
 *  1. PERSISTENZA DELLE DECISIONI (opzionale, tramite
 *     [decisionPersistence]): le decisioni calcolate dal filtro vengono
 *     salvate su disco (implementazione Room lato app) taggate con la
 *     versione del catalogo (fetchedAt). Al riavvio [rebuild] le
 *     ri-precarica in [decisionCache] PRIMA che l'utente navighi: il
 *     costo fuzzy (0.5-3 ms/miss desktop, 3-5x su telefoni datati) viene
 *     pagato UNA volta per dispositivo, non una volta per processo. Il
 *     salvataggio e' write-through BUFFERIZZATO: le decisioni nuove
 *     finiscono in un buffer e vengono scaricate in batch (soglia
 *     [DECISION_FLUSH_THRESHOLD]) senza toccare il percorso critico del
 *     filtro; [refresh] scarica il buffer PRIMA di sostituire il catalogo,
 *     cosi' ogni decisione e' salvata col fetchedAt del catalogo con cui
 *     e' stata calcolata. Il purge elimina le versioni vecchie dopo il
 *     preload di quella nuova: a regime sul disco resta solo l'ultima.
 *
 *  2. SCRITTURA ATOMICA DEL FILE DI CACHE: il JSON non viene piu'
 *     scritto direttamente su [cacheFile] (una kill a meta' scrittura
 *     lo corrompeva e l'avvio successivo lo scartava, ripagando un
 *     refresh) ma su un file temporaneo promosso con rename (con backup
 *     della versione precedente: su Windows il rename non sovrascrive).
 *
 * La cache vive in un file JSON dentro [cacheDir] (Android: filesDir/zoundhub,
 * desktop: data dir utente/RiPlay/zoundhub); il formato e' un envelope con
 * fetchedAt + catalog, cosi' l'informazione "ultimo aggiornamento" sopravvive
 * ai riavvii senza file separati.
 */
class ZoundHubCatalogStore(
    private val cacheDir: File,
    private val service: ZoundHubService = ZoundHubService(),
    /**
     * Persistenza su disco delle decisioni del filtro (Room lato app).
     * null = comportamento v1.3 (decisioni solo in memoria, per processo).
     */
    private val decisionPersistence: ZoundHubDecisionPersistence? = null,
) {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
        isLenient = true
    }

    @Serializable
    private data class CacheEnvelope(
        val fetchedAt: Long,
        val catalog: ZoundHubCatalog,
    )

    private val cacheFile: File
        get() = File(cacheDir, CACHE_FILE_NAME)

    private val _state = MutableStateFlow(ZoundHubCatalogState())
    val state: StateFlow<ZoundHubCatalogState> = _state.asStateFlow()

    /**
     * Catalogo attivo: coppia (versione, indice) IMMUTABLE pubblicata con
     * UNA sola assegnazione volatile. I lettori (lookup, isAiHidden,
     * backfiller) vedono sempre una coppia coerente: mai un indice nuovo
     * taggato con il fetchedAt vecchio (che persisterebbe decisioni
     * sbagliate) ne' uno stato Ready con indice non ancora costruito.
     */
    private data class ActiveCatalog(val fetchedAt: Long?, val index: Index?)

    @Volatile
    private var active: ActiveCatalog = ActiveCatalog(null, null)

    /**
     * Generazione del catalogo: incrementa ad ogni [rebuild] (loadFromCache,
     * refresh). Utile per chiave istanze di predicato/pipeline nella UI:
     * cambio di generazione -> le liste si ri-filtrano.
     */
    @Volatile
    var generation: Long = 0L
        private set

    /**
     * Cap della cache decisioni: oltre, si svuota (memoria bounded).
     * Le combinazioni "artista A, artista B" sono potenzialmente moltissime:
     * 32.000 decisioni coprono ampiamente ogni sessione reale.
     */
    private val DECISION_CACHE_MAX = 32_000

    /**
     * Soglia di scarico del buffer decisioni: raggiunta, il buffer viene
     * salvato su disco in batch (write-through asincrono).
     */
    private val DECISION_FLUSH_THRESHOLD = 64

    /**
     * Cache processuale delle decisioni "testo autori -> e' AI?".
     * Condivisa da tutte le schermate (home, ricerca, pagine desktop): la UI
     * non crea piu' cache per-composizione che ripagano il fuzzy ad ogni
     * navigazione. Invalidata ad ogni rebuild del catalogo.
     */
    private val decisionCache = ConcurrentHashMap<String, Boolean>()

    /** Scope interno per preload/write-through/purge delle decisioni (best-effort). */
    private val persistenceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Buffer delle decisioni nuove da persistere (chiave testo -> decisione
     * con la versione catalogo con cui e' stata calcolata). Protetto da
     * [pendingLock]; lo scarico e' a batch (soglia) + flush su refresh.
     */
    private data class PendingDecision(val fetchedAt: Long, val isAi: Boolean)

    private val pendingDecisions = HashMap<String, PendingDecision>()
    private val pendingLock = Any()

    /**
     * Carica la cache da disco (chiamata all'avvio dell'app). Non fa rete:
     * se non esiste cache lo stato resta Idle e la UI puo' proporre il
     * refresh dall'impostazione dedicata.
     *
     * @return true se una cache valida e' stata caricata.
     */
    suspend fun loadFromCache(): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = cacheFile
            if (!file.isFile) return@withContext false

            val envelope = json.decodeFromString<CacheEnvelope>(file.readText())
            if (envelope.catalog.artists.isEmpty()) return@withContext false

            rebuild(envelope.fetchedAt, envelope.catalog)
            true
        } catch (e: Exception) {
            // cache corrotta: si scarta silenziosamente, il refresh la riscrivera'
            false
        }
    }

    /**
     * Scarica il catalogo completo (+ stats) e lo persiste. Operazione idempotente:
     * i fallimenti NON toccano la cache esistente (dopo un refresh fallito
     * i badge continuano a funzionare coi dati gia' in memoria).
     *
     * @param includeStats scarica anche i contatori globali (piccolo, opzionale).
     */
    suspend fun refresh(includeStats: Boolean = true): Result<ZoundHubCatalog> =
        withContext(Dispatchers.IO) {
            _state.value = _state.value.copy(status = ZoundHubCatalogState.Status.Loading, error = null)

            // v1.4: le decisioni calcolate col catalogo CORRENTE vengono
            // salvate col PROPRIO fetchedAt PRIMA di sostituire il catalogo
            flushDecisionsNow()

            val result = service.fetchCatalog()

            result.onSuccess { catalog ->
                val now = System.currentTimeMillis()
                try {
                    persistCacheFile(CacheEnvelope(now, catalog))
                } catch (e: Exception) {
                    // la persistenza e' best-effort: il catalogo resta valido in memoria
                }

                var stats: ZoundHubStats? = null
                if (includeStats) {
                    stats = service.fetchStats().getOrNull()
                }

                rebuild(now, catalog, stats)
            }.onFailure { e ->
                _state.value = _state.value.copy(
                    status = ZoundHubCatalogState.Status.Failed,
                    error = e.message ?: e.javaClass.simpleName
                )
            }

            result
        }

    /** Ricerca il miglior match per un testo autori (null se nessun dato). */
    fun lookup(artistsText: String?): ZoundHubMatch? {
        val idx = active.index ?: return null
        return ZoundHubMatcher.match(artistsText, idx)
    }

    /**
     * DECISIONE "questo testo autori appartiene a un artista AI?" per il
     * filtro "nascondi contenuti AI".
     *
     * - Catalogo non pronto o testo vuoto: false (lista completa, niente
     *   falsi vuoti in attesa del catalogo).
     * - Testo gia' valutato: risposta O(1) dalla cache processuale.
     * - Nuovo testo: match esatto O(1) o fuzzy (scan prunato), poi memoizzato
     *   e (se configurata) accodato per la persistenza su disco.
     *
     * THREAD-SAFE e NON-BLOCCANTE: pensato per essere invocato anche da
     * Dispatchers.Default/IO durante il filtraggio reattivo delle liste.
     * NON chiamarlo direttamente in composizione su liste grandi: usa gli
     * helper reattivi della UI (rememberZoundHubAiFiltered / produceState),
     * cosi' il costo dei primi miss fuzzy resta fuori dal main thread.
     */
    fun isAiHidden(artistsText: String?): Boolean {
        if (artistsText.isNullOrBlank()) return false
        // una sola lettura volatile: versione e indice sono sempre coerenti
        val (fetchedAt, idx) = active
        if (idx == null) return false

        if (decisionCache.size > DECISION_CACHE_MAX) {
            decisionCache.clear()
            // v1.4: dopo lo svuotamento si ri-precaricano le decisioni gia'
            // salvate su disco, cosi' la cache si riempie a caldo
            fetchedAt?.let { scheduleDecisionPreload(it) }
        }

        var computedHere = false
        val decision = decisionCache.computeIfAbsent(artistsText) {
            computedHere = true
            ZoundHubMatcher.match(artistsText, idx) != null
        }

        if (computedHere && fetchedAt != null) {
            recordPendingDecision(artistsText, fetchedAt, decision)
        }
        return decision
    }

    /**
     * Ponte MusicBrainz: ricerca ESATTA per Spotify artist ID
     * (il campo `id` del catalogo e' proprio lo Spotify ID).
     * Usata dal backfill della natura via Artist.spotifyId: zero fuzzy,
     * zero falsi positivi. null se il catalogo non e' pronto o l'ID non
     * e' nel catalogo.
     */
    fun lookupBySpotifyId(spotifyId: String?): ZoundHubArtist? {
        if (spotifyId.isNullOrBlank()) return null
        return active.index?.byId?.get(spotifyId)
    }

    /** Match "incerto"? (fuzzy sotto similarita' perfetta) */
    fun isUncertainMatch(match: ZoundHubMatch?): Boolean =
        match != null && !match.exact

    private fun rebuild(fetchedAt: Long, catalog: ZoundHubCatalog, stats: ZoundHubStats? = null) {
        // buildIndex e' la parte lenta (normalizzazione di ~9.700 nomi): gira
        // con il catalogo/indice VECCHIO ancora attivi, poi la coppia
        // (versione, indice nuovo) viene pubblicata ATOMICAMENTE
        val newIndex = ZoundHubMatcher.buildIndex(catalog.artists)
        active = ActiveCatalog(fetchedAt, newIndex)
        generation += 1
        // il nuovo catalogo puo' cambiare le risposte: le decisioni vecchie
        // non sono piu' valide, si riparte da cache vuota (e poi si ri-empie
        // a caldo con le decisioni persistenti della stessa versione)
        decisionCache.clear()
        _state.value = ZoundHubCatalogState(
            status = ZoundHubCatalogState.Status.Ready,
            catalog = catalog,
            fetchedAt = fetchedAt,
            stats = _state.value.stats.takeIf { stats == null } ?: stats,
            error = null
        )
        scheduleDecisionPreload(fetchedAt)
    }

    // ── Persistenza decisioni (v1.4) ─────────────────────────────────────────

    /**
     * Accoda una decisione nuova per la persistenza; alla soglia scarica
     * il buffer in batch su disco (asincrono: mai nel percorso del filtro).
     */
    private fun recordPendingDecision(text: String, fetchedAt: Long, isAi: Boolean) {
        if (decisionPersistence == null) return
        var shouldFlush = false
        synchronized(pendingLock) {
            pendingDecisions[text] = PendingDecision(fetchedAt, isAi)
            if (pendingDecisions.size >= DECISION_FLUSH_THRESHOLD) shouldFlush = true
        }
        if (shouldFlush) persistenceScope.launch { flushDecisionsNow() }
    }

    /** Svuota il buffer (synchronized: un solo scarico per snapshot). */
    private fun drainPendingDecisions(): HashMap<String, PendingDecision>? =
        synchronized(pendingLock) {
            if (pendingDecisions.isEmpty()) null
            else {
                val copy = HashMap(pendingDecisions)
                pendingDecisions.clear()
                copy
            }
        }

    /**
     * Salva su disco le decisioni nel buffer, raggruppate per versione
     * catalogo (una decisione e' valida SOLO per il fetchedAt con cui
     * e' stata calcolata). Best-effort: fallire degrada alla v1.3.
     */
    private suspend fun flushDecisionsNow() {
        val persistence = decisionPersistence ?: return
        val snapshot = drainPendingDecisions() ?: return
        try {
            snapshot.entries
                .groupBy { it.value.fetchedAt }
                .forEach { (fetchedAt, entries) ->
                    val map = HashMap<String, Boolean>(entries.size)
                    for ((text, decision) in entries) map[text] = decision.isAi
                    persistence.save(fetchedAt, map)
                }
        } catch (t: Throwable) {
            // decisioni non salvate: verranno ricalcolate al prossimo giro
        }
    }

    /**
     * Ri-precarica dal disco le decisioni della versione corrente del
     * catalogo dentro [decisionCache] (putIfAbsent: mai sopra una
     * decisione appena calcolata dal filtro in contemporanea), poi
     * elimina le righe di versioni vecchie. Asincrono e best-effort.
     */
    private fun scheduleDecisionPreload(fetchedAt: Long) {
        val persistence = decisionPersistence ?: return
        persistenceScope.launch {
            try {
                val saved = persistence.load(fetchedAt)
                // cap: non superare la capienza della cache processuale
                val preloadCap = DECISION_CACHE_MAX - 1_000
                for ((text, isAi) in saved) {
                    if (decisionCache.size >= preloadCap) break
                    decisionCache.putIfAbsent(text, isAi)
                }
                persistence.purgeOthers(fetchedAt)
            } catch (t: Throwable) {
                // preload fallito: le decisioni vengono ricalcolate in background
            }
        }
    }

    // ── Persistenza file cache (scrittura atomica, v1.4) ─────────────────────

    /**
     * Scrittura ATOMICA del file cache: payload su .tmp, cache corrente
     * spostata su .bak, .tmp promossa a cache, .bak eliminata. Una kill a
     * meta' scrittura lascia al peggio la versione precedente (mai un file
     * a meta' JSON come prima della v1.4). Il rename su Windows non
     * sovrascrive il target esistente: da qui la danza col .bak, con
     * ripristino se la promozione fallisce.
     */
    private fun persistCacheFile(envelope: CacheEnvelope) {
        cacheDir.mkdirs()
        val payload = json.encodeToString(CacheEnvelope.serializer(), envelope)
        val tmp = File(cacheDir, "$CACHE_FILE_NAME.tmp")
        val bak = File(cacheDir, "$CACHE_FILE_NAME.bak")

        tmp.writeText(payload)

        val hadTarget = cacheFile.isFile
        if (hadTarget) {
            bak.delete()
            if (!cacheFile.renameTo(bak)) {
                // fallback: senza .bak il rename successivo su Windows
                // fallirebbe comunque, si libera il posto
                bak.delete()
                cacheFile.delete()
            }
        }

        if (tmp.renameTo(cacheFile)) {
            bak.delete()
        } else if (hadTarget && bak.isFile) {
            // ripristino: la cache precedente torna al suo posto
            cacheFile.delete()
            bak.renameTo(cacheFile)
            tmp.delete()
            throw IllegalStateException("persistenza cache non riuscita (rename fallito)")
        } else {
            tmp.delete()
            // fallback ultimo: scrittura diretta come nelle versioni precedenti
            cacheFile.writeText(payload)
        }
    }

    companion object {
        const val CACHE_FILE_NAME = "zoundhub-catalog.json"
    }
}

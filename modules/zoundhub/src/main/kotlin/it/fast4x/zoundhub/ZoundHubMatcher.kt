package it.fast4x.zoundhub

import it.fast4x.zoundhub.models.ZoundHubArtist
import java.text.Normalizer

/**
 * Risultato dell'incrocio tra un nome artista locale (RiPlay, testo libero
 * da YouTube Music) e un record del catalogo ZoundHub.
 *
 * @param artist     record ZoundHub individuato
 * @param exact      true se il match e' avvenuto sul nome normalizzato esatto
 * @param similarity similarita' Jaro-Winkler (1.0 per match esatti)
 */
data class ZoundHubMatch(
    val artist: ZoundHubArtist,
    val exact: Boolean,
    val similarity: Double,
)

/**
 * Incrocio nome-artista <-> catalogo ZoundHub.
 *
 * Livelli di match (scelta di progetto concordata: "Norm + fuzzy"):
 *  1. ESATTO  - chiave = nome normalizzato (minuscolo, senza accenti,
 *               punteggiatura, suffissi " - Topic", "the " iniziale, "&"->"and")
 *               -> lookup O(1) su mappa.
 *  2. FUZZY   - fallback Jaro-Winkler >= [FUZZY_THRESHOLD] (0.92) su tutti i
 *               nomi del catalogo, con guardia di lunghezza minima per evitare
 *               falsi positivi su nomi corti. Il risultato e' marcato come
 *               match "incerto" (badge "AI?") cosi' l'utente puo' verificarlo.
 *
 * I nomi locali provengono da `Song.artistsText` / `Album.authorsText`
 * (testo libero tipo "Artist A, Artist B") e da `Artist.name` (canale YT):
 * per questo [splitArtistParts] spezza sulle virgole e [match] prova ogni
 * parte, restituendo il risultato migliore (esatti > fuzzy).
 *
 * Nota KMP: il modulo e' kotlin("jvm") puro, quindi java.text.Normalizer e'
 * disponibile sia su Android sia su desktop JVM senza dipendenze extra.
 *
 * NOTE PERF v1.3 (filtro reattivo "nascondi contenuti AI"): l'algoritmo di
 * match resta IDENTICO alla v1.1 (verificato con benchmark di equivalenza su
 * catalogo sintetico da 9.700 nomi: esatti, fuzzy-hit, miss a tre bande di
 * lunghezza, multi-autore, tie-break su duplicati normalizzati). Il costo di
 * un miss fuzzy (0.5-3 ms su ~9.700 nomi, gia' prunato dalla guardia lunghezze
 * e dalla prenormalizzazione) NON e' piu' pagato dal main thread ne' ripetuto
 * per schermata: la UI chiama ZoundHubCatalogStore.isAiHidden() (cache
 * decisionale processuale) DENTRO pipeline di filtraggio su
 * Dispatchers.Default (produceState/flowOn). Un'intera libreria da 2.000
 * brani con ~800 nomi unici costa ~1-2 s UNA volta per processo, in
 * background, poi tutto e' servito dalle cache.
 *
 * NOTE PERF v1.4 (dispositivi vecchi/lenti) — DUE leve, SEMPRE provabilmente
 * equivalenti allo scan v1.1/v1.3 (stesso risultato: stesso artista, stessa
 * similarita', stesso tie-break):
 *
 *  1. EARLY-EXIT ARITMETICO nello scan (jaroWinklerAboveThreshold):
 *     JW = j + p*0.1*(1-j) con p <= 4 => JW <= 0.4 + 0.6*j, e
 *     j = (m/|a| + m/|b| + (m - t/2)/m)/3 <= (m/|a| + m/|b| + 1)/3.
 *     Quindi JW >= soglia richiede m/|a| + m/|b| >= 3*jaroMin - 1 con
 *     jaroMin = (soglia - 0.4)/0.6 (per 0.92: somma >= 1.6, cioe' almeno
 *     ~80% dei caratteri in match). Se DURANTE il conteggio dei match il
 *     massimo di match ancora ottenibili non arriva alla somma richiesta,
 *     il candidato e' abortito SENZA contare le trasposizioni (il secondo
 *     loop, la meta' costosa); i candidati che passano il bound sono
 *     calcolati ESATTI. Nota: un PRUNING a trigrammi NON sarebbe equivalente
 *     (es. JW("abcd","acbd") = 0.925 >= 0.92 con ZERO trigrammi comuni:
 *     l'indice trigramma perderebbe il match), per questo si usa il bound
 *     aritmetico, che e' provabilmente safe.
 *
 *  2. SCRATCH BUFFER riutilizzabili per thread (ThreadLocal): niente piu'
 *     2 allocazioni BooleanArray per candidato (su un miss erano ~migliaia
 *     di allocazioni: pressione GC pagata cara sui telefoni vecchi).
 *
 *  In piu' le decisioni del filtro ora SOPRAVVIVONO al processo (persistenza
 *  Room lato app, vedi ZoundHubCatalogStore v1.4): il costo fuzzy residuo
 *  viene pagato una volta per dispositivo, non per processo.
 */
object ZoundHubMatcher {

    /** Soglia minima Jaro-Winkler per considerare valido un match fuzzy. */
    const val FUZZY_THRESHOLD = 0.92

    /** Lunghezza minima (normalizzata) dei due nomi per tentare il fuzzy. */
    const val MIN_FUZZY_LENGTH = 4

    /** Cap della cache fuzzy: oltre, si svuota (memoria bounded). */
    private const val FUZZY_CACHE_MAX = 2048

    private var fuzzyCache = HashMap<String, ZoundHubMatch?>()
    private var fuzzyCacheBuiltFor = 0

    // ── scratch riutilizzabili per lo scan fuzzy (v1.4) ─────────────────────
    // Lo scan gira su Dispatchers.Default (pipeline reattive): piu' thread
    // possono matchare in contemporanea. ThreadLocal = un paio di buffer per
    // thread, ZERO allocazioni per candidato (prima: 2 BooleanArray a testa).
    private val scratchAMatched = ThreadLocal<BooleanArray?>()
    private val scratchBMatched = ThreadLocal<BooleanArray?>()

    /**
     * Restituisce un BooleanArray di lunghezza >= [len] con la porzione
     * [0, len) azzerata, riutilizzando quello del thread quando basta.
     */
    private fun borrowScratch(len: Int, holder: ThreadLocal<BooleanArray?>): BooleanArray {
        val current = holder.get()
        if (current == null || current.size < len) {
            val fresh = BooleanArray(maxOf(len, 64))
            holder.set(fresh)
            return fresh
        }
        current.fill(false, 0, len)
        return current
    }

    /**
     * Normalizza un nome artista per il confronto:
     *  - trim + lowercase
     *  - suffix "- Topic" dei canali YouTube Music rimosso
     *  - "&" -> " and "
     *  - rimozione accenti/diacritici (NFKD, senza combining marks)
     *  - rimozione punteggiatura/simboli (restano lettere, cifre, spazi)
     *  - spazi compatti
     *  - "the " iniziale rimosso
     */
    fun normalize(name: String): String {
        var s = name.trim().lowercase()

        // Canali auto-generati di YouTube Music: "Artist Name - Topic"
        if (s.endsWith("- topic")) s = s.removeSuffix("- topic").trim()

        // L'ampersand in italiano/inglese equivale alla parola "and":
        // lo sostituiamo PRIMA della pulizia della punteggiatura, cosi'
        // il separatore diventa uno spazio e le parole restano confrontabili.
        s = s.replace("&", " and ")

        // NFKD + rimozione diacritici
        s = Normalizer.normalize(s, Normalizer.Form.NFKD)
            .replace(Regex("\\p{M}+"), "")

        // Solo lettere/cifre/spazi: il resto diventa spazio
        s = s.replace(Regex("[^\\p{L}\\p{Nd}]+"), " ")

        // Compatta gli spazi
        s = s.replace(Regex("\\s+"), " ").trim()

        // Articolo iniziale (inglese)
        if (s.startsWith("the ")) s = s.removePrefix("the ").trim()

        return s
    }

    /** Spezza un testo autori ("A, B feat. C") nelle parti candidate. */
    fun splitArtistParts(artistsText: String?): List<String> {
        if (artistsText.isNullOrBlank()) return emptyList()
        return artistsText.split(',', ';', '/')
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }

    /**
     * Indice del catalogo usato dal matcher (costruito con [buildIndex]).
     *
     * PERF: [normalizedNames] contiene i nomi GIA' NORMALIZZATI nello stesso
     * ordine di [all]. Normalizzare ~9.700 nomi (3 regex + NFKD ciascuno)
     * costa centinaia di ms: farlo UNA volta alla costruzione dell'indice
     * invece che ad ogni scan fuzzy e' la differenza tra UI fluida e UI
     * congelata quando il fallback fuzzy gira durante la composizione.
     *
     * Ponte MusicBrainz (Fase 2): [byId] indicizza il catalogo per Spotify
     * artist ID (il campo `id` del catalogo e' proprio quello) — il backfill
     * della natura via `Artist.spotifyId` e' ESATTO, senza fuzzy.
     */
    class Index(
        val byName: Map<String, ZoundHubArtist>,
        val all: List<ZoundHubArtist>,
        val normalizedNames: List<String>,
        val byId: Map<String, ZoundHubArtist> = emptyMap(),
    )

    /** Costruisce l'indice a partire dal catalogo. */
    fun buildIndex(artists: List<ZoundHubArtist>): Index {
        val byName = HashMap<String, ZoundHubArtist>(artists.size * 2)
        val byId = HashMap<String, ZoundHubArtist>(artists.size * 2)
        val normalized = ArrayList<String>(artists.size)
        for (artist in artists) {
            val key = normalize(artist.name)
            normalized.add(key)
            if (key.isNotEmpty() && !byName.containsKey(key)) {
                byName[key] = artist
            }
            // chiave aggiuntiva sullo slug: gestisce nomi con separatori
            // (es. "Eardrum_Music" -> slug "eardrum-music" -> "eardrum music")
            val slugKey = normalize(artist.slug)
            if (slugKey.isNotEmpty() && !byName.containsKey(slugKey)) {
                byName[slugKey] = artist
            }
            // indice per Spotify ID: match esatto per il backfill della natura
            if (artist.id.isNotBlank() && !byId.containsKey(artist.id)) {
                byId[artist.id] = artist
            }
        }
        return Index(byName, artists, normalized, byId)
    }

    /**
     * Cerca il miglior match per un testo autori. Prova prima le singole
     * parti (es. "A, B" -> "A" e "B") e poi il testo intero; preferisce i
     * match esatti, altrimenti il fuzzy con similarita' massima.
     */
    fun match(artistsText: String?, index: Index): ZoundHubMatch? {
        val text = artistsText ?: return null
        val parts = splitArtistParts(text)
        if (parts.isEmpty()) return null

        val candidates: List<String> =
            if (parts.size > 1) parts + listOf(text.trim()) else parts

        var best: ZoundHubMatch? = null

        for (candidate in candidates) {
            val normalized = normalize(candidate)
            if (normalized.isEmpty()) continue

            // 1) match esatto su mappa: per questa parte e' il massimo possibile
            index.byName[normalized]?.let {
                val m = ZoundHubMatch(artist = it, exact = true, similarity = 1.0)
                if (best == null || !best.exact) best = m
                continue
            }

            // 2) fuzzy Jaro-Winkler (inutile se abbiamo gia' un esatto)
            if (best?.exact == true) continue
            if (normalized.length < MIN_FUZZY_LENGTH) continue

            val fuzzy = cachedFuzzyMatch(normalized, index) ?: continue
            if (best == null || fuzzy.similarity > best.similarity) {
                best = fuzzy
            }
        }

        return best
    }

    /**
     * Fuzzy con memoizzazione (scan lineare su ~9.7k nomi per miss).
     * PERF: dopo la prenormalizzazione nell'indice e la guardia lunghezze,
     * un miss costa ~0.5-3 ms la prima volta e poi e' servito dalla cache;
     * prima dell'ottimizzazione costava ~200 ms per miss (rinormalizzando
     * tutto il catalogo), il che congelava l'interfaccia su liste grandi.
     *
     * CON L'ARCHITETTURA v1.3 questo scan non gira MAI sul main thread:
     * i chiamanti della UI lo invocano solo tramite
     * ZoundHubCatalogStore.isAiHidden() dentro pipeline su
     * Dispatchers.Default (produceState/flowOn), quindi il costo resta
     * invisibile all'utente anche alla prima attivazione del filtro.
     *
     * v1.4: il punteggio per candidato passa da jaroWinkler() a
     * jaroWinklerAboveThreshold() (early-exit provabilmente equivalente,
     * vedi KDoc) e gli array di match sono scratch per thread.
     */
    private fun cachedFuzzyMatch(normalized: String, index: Index): ZoundHubMatch? {
        synchronized(this) {
            if (fuzzyCacheBuiltFor != index.all.size) {
                fuzzyCache = HashMap()
                fuzzyCacheBuiltFor = index.all.size
            } else if (fuzzyCache.size > FUZZY_CACHE_MAX) {
                fuzzyCache.clear()
            }
            if (fuzzyCache.containsKey(normalized)) return fuzzyCache[normalized]
        }

        var bestScore = 0.0
        var bestArtist: ZoundHubArtist? = null

        // PERF: scan sui nomi precalcolati (nessuna rinormalizzazione per candiato).
        val all = index.all
        val names = index.normalizedNames
        val queryLen = normalized.length

        for (i in all.indices) {
            val candidate = names[i]
            if (candidate.length < MIN_FUZZY_LENGTH) continue

            // Guardia lunghezze (solo aritmetica intera): con min/max < 0.6 il
            // Jaro-Winkler NON puo' raggiungere 0.92 anche con bonus prefisso
            // massimo 4 (jaro <= (min/max + 2)/3 -> JW <= 0.4 + 0.6*jaro < 0.92).
            // Scarta subito le coppie troppo diverse: pruning ~60% del catalogo.
            val candLen = candidate.length
            val minLen = if (candLen < queryLen) candLen else queryLen
            val maxLen = if (candLen > queryLen) candLen else queryLen
            if (minLen * 5 < maxLen * 3) continue

            val score = jaroWinklerAboveThreshold(candidate, normalized, FUZZY_THRESHOLD)
            if (score >= FUZZY_THRESHOLD && score > bestScore) {
                bestScore = score
                bestArtist = all[i]
            }
        }

        val result = bestArtist?.let {
            ZoundHubMatch(artist = it, exact = false, similarity = bestScore)
        }

        synchronized(this) {
            fuzzyCache[normalized] = result
        }
        return result
    }

    /**
     * Jaro-Winkler "a soglia" per lo scan fuzzy (v1.4): restituisce il
     * punteggio ESATTO quando questo e' >= [threshold], altrimenti un
     * qualsiasi valore SICURAMENTE < [threshold] (un upper bound aritmetico
     * del punteggio reale, sempre < soglia).
     *
     * EQUIVALENZA PROVATA con lo scan completo:
     *  - JW = j + p*0.1*(1-j), p <= 4  =>  JW <= 0.4 + 0.6*j;
     *  - j = (m/|a| + m/|b| + (m - t/2)/m)/3, (m - t/2)/m <= 1
     *    =>  j <= (m/|a| + m/|b| + 1)/3.
     *  Se nemmeno col MASSIMO di match ottenibili la somma m/|a| + m/|b|
     *  resta sotto 3*jaroMin - 1 (jaroMin = (threshold-0.4)/0.6), nessun
     *  completamento delle trasposizioni puo' portare il punteggio a soglia:
     *  si abortisce il candidato restituendo il bound. I candidati che
     *  superano il bound vengono calcolati ESATTI (trasposizioni + prefisso,
     *  identici a [jaroWinkler]); il tie-break per indice e il
     *  strictly-greater dello scan restano invariati => stessa selezione.
     *
     * NON sostituisce [jaroWinkler] per usi generici: e' valido SOLO per i
     * chiamanti che scartano i punteggi sotto soglia.
     */
    private fun jaroWinklerAboveThreshold(a: String, b: String, threshold: Double): Double {
        if (a == b) return 1.0
        val aLen = a.length
        val bLen = b.length
        if (aLen == 0 || bLen == 0) return 0.0

        // soglia raggiungibile solo con m/|a| + m/|b| >= sumMin.
        // MARGINE CONSERVATIVO (1e-12 >> rounding double ~1e-16): i candidati
        // entro il margine vengono calcolati ESATTI, cosi' nessun candidato
        // con punteggio reale >= soglia puo' essere perso per un errore di
        // rappresentazione, e il bound restituito resta rigorosamente < soglia.
        val jaroMin = (threshold - 0.4) / 0.6
        val sumMinSafe = 3.0 * jaroMin - 1.0 - 1e-12

        val matchWindow = maxOf(0, maxOf(aLen, bLen) / 2 - 1)
        val aMatched = borrowScratch(aLen, scratchAMatched)
        val bMatched = borrowScratch(bLen, scratchBMatched)

        // inversi precalcolati: sumMax = mMax * (invA + invB) senza divisioni in loop
        val invA = 1.0 / aLen
        val invB = 1.0 / bLen
        val invSum = invA + invB

        var matches = 0
        for (i in 0 until aLen) {
            // early-exit provabile: nemmeno col massimo di match ancora
            // ottenibili (i restanti di a, i liberi di b) si arriva a soglia
            val mMax = matches + minOf(aLen - i, bLen - matches)
            val sumMax = mMax * invSum
            if (sumMax < sumMinSafe) {
                return 0.4 + 0.6 * ((mMax * invA + mMax * invB + 1.0) / 3.0) // < threshold per costruzione
            }

            val start = maxOf(0, i - matchWindow)
            val end = minOf(bLen - 1, i + matchWindow)
            val ca = a[i]
            for (j in start..end) {
                if (bMatched[j] || ca != b[j]) continue
                aMatched[i] = true
                bMatched[j] = true
                matches++
                break
            }
        }

        val sumFinal = matches * invSum
        if (sumFinal < sumMinSafe) {
            return 0.4 + 0.6 * ((matches * invA + matches * invB + 1.0) / 3.0) // < threshold per costruzione
        }

        // percorso esatto: trasposizioni + bonus prefisso, identico a jaroWinkler
        var transpositions = 0
        var k = 0
        for (i in 0 until aLen) {
            if (!aMatched[i]) continue
            while (!bMatched[k]) k++
            if (a[i] != b[k]) transpositions++
            k++
        }

        val m = matches.toDouble()
        val jaro = (m / aLen + m / bLen + (m - transpositions / 2.0) / m) / 3.0
        if (jaro < 0.7) return jaro

        val prefix = commonPrefixLength(a, b)
        return jaro + prefix * 0.1 * (1.0 - jaro)
    }

    private fun commonPrefixLength(a: String, b: String): Int {
        val max = minOf(a.length, b.length, 4)
        var i = 0
        while (i < max && a[i] == b[i]) i++
        return i
    }

    /**
     * Jaro-Winkler (p = 0.1, prefisso max 4) — implementazione pura Kotlin,
     * equivalente semanticamente alle librerie di riferimento.
     */
    fun jaroWinkler(a: String, b: String): Double {
        if (a.isEmpty() && b.isEmpty()) return 1.0
        if (a.isEmpty() || b.isEmpty()) return 0.0
        if (a == b) return 1.0

        val jaro = jaro(a, b)
        if (jaro < 0.7) return jaro

        val prefix = commonPrefixLength(a, b)
        return jaro + prefix * 0.1 * (1.0 - jaro)
    }

    private fun jaro(a: String, b: String): Double {
        val aLen = a.length
        val bLen = b.length

        val matchWindow = maxOf(0, maxOf(aLen, bLen) / 2 - 1)

        val aMatched = BooleanArray(aLen)
        val bMatched = BooleanArray(bLen)

        var matches = 0
        for (i in 0 until aLen) {
            val start = maxOf(0, i - matchWindow)
            val end = minOf(bLen - 1, i + matchWindow)
            for (j in start..end) {
                if (bMatched[j] || a[i] != b[j]) continue
                aMatched[i] = true
                bMatched[j] = true
                matches++
                break
            }
        }
        if (matches == 0) return 0.0

        // trasposizioni
        var transpositions = 0
        var k = 0
        for (i in 0 until aLen) {
            if (!aMatched[i]) continue
            while (!bMatched[k]) k++
            if (a[i] != b[k]) transpositions++
            k++
        }

        val m = matches.toDouble()
        return (m / aLen + m / bLen + (m - transpositions / 2.0) / m) / 3.0
    }
}

import it.fast4x.zoundhub.ZoundHubMatch
import it.fast4x.zoundhub.ZoundHubMatcher
import it.fast4x.zoundhub.models.ZoundHubArtist
import java.text.Normalizer
import kotlin.random.Random

/**
 * Benchmark + verifica di equivalenza dell'algoritmo di match ZoundHub
 * (usato per validare la v1.4: il SELETTORE del match resta identico alla
 * v1.1/v1.3, cambia SOLO il modo in cui il punteggio JW viene ottenuto
 * nella scansione — early-exit aritmetico provabilmente equivalente
 * (jaroWinklerAboveThreshold) + scratch buffer per thread. La REPLICA
 * inline (LegacyMatcher) conserva l'algoritmo COMPLETO originale e fa da
 * riferimento: l'equivalenza verifica artista, exact e similarity).
 *
 * Cosa fa:
 *  - genera un catalogo sintetico da 9.700 nomi realistici (accenti, "&",
 *    " - Topic", "the ", duplicati normalizzati);
 *  - confronta il matcher ATTUALE (ZoundHubMatcher) con una REPLICA inline
 *    dell'algoritmo (LegacyMatcher) su 1.050 query: esatti, fuzzy-hit
 *    (nomi con typo), miss a tre bande di lunghezza, multi-autore;
 *  - verifica il tie-break su nomi duplicati normalizzati;
 *  - misura i tempi a freddo (cache svuotata) e la simulazione del filtro
 *    su una lista da 2.000 item.
 *
 * Risultato atteso: EQUIVALENZA OK (0 mismatch), TIE-BREAK OK, conteggio
 * nascosti identico, miss-cold piu' veloci della v1.3 (early-exit).
 *
 * Compilazione standalone (fuori da Gradle):
 *   kotlinc -Xplugin <kotlinx-serialization-compiler-plugin.jar> \
 *     -cp <kotlinx-serialization-core-jvm.jar>:<kotlinx-coroutines-core-jvm.jar> \
 *     -d out src/main/kotlin/it/fast4x/zoundhub/ZoundHubMatcher.kt \
 *        src/main/kotlin/it/fast4x/zoundhub/models/Data.kt \
 *        src/main/kotlin/it/fast4x/zoundhub/ZoundHubCatalogStore.kt \
 *        benchmark/BenchmarkMatcher.kt
 *   java -cp out:<gli stessi jar> BenchmarkMatcherKt
 */

// ─── replica dell'algoritmo legacy (scan completo) ───────────────────────────
object LegacyMatcher {
    const val FUZZY_THRESHOLD = 0.92
    const val MIN_FUZZY_LENGTH = 4
    private const val FUZZY_CACHE_MAX = 2048

    private var fuzzyCache = HashMap<String, ZoundHubMatch?>()
    private var fuzzyCacheBuiltFor = 0

    fun normalize(name: String): String {
        var s = name.trim().lowercase()
        if (s.endsWith("- topic")) s = s.removeSuffix("- topic").trim()
        s = s.replace("&", " and ")
        s = Normalizer.normalize(s, Normalizer.Form.NFKD).replace(Regex("\\p{M}+"), "")
        s = s.replace(Regex("[^\\p{L}\\p{Nd}]+"), " ")
        s = s.replace(Regex("\\s+"), " ").trim()
        if (s.startsWith("the ")) s = s.removePrefix("the ").trim()
        return s
    }

    fun splitArtistParts(artistsText: String?): List<String> {
        if (artistsText.isNullOrBlank()) return emptyList()
        return artistsText.split(',', ';', '/').map { it.trim() }.filter { it.isNotBlank() }
    }

    fun match(artistsText: String?, index: ZoundHubMatcher.Index): ZoundHubMatch? {
        val text = artistsText ?: return null
        val parts = splitArtistParts(text)
        if (parts.isEmpty()) return null
        val candidates: List<String> = if (parts.size > 1) parts + listOf(text.trim()) else parts
        var best: ZoundHubMatch? = null
        for (candidate in candidates) {
            val normalized = normalize(candidate)
            if (normalized.isEmpty()) continue
            index.byName[normalized]?.let {
                val m = ZoundHubMatch(artist = it, exact = true, similarity = 1.0)
                if (best == null || !best.exact) best = m
                continue
            }
            if (best?.exact == true) continue
            if (normalized.length < MIN_FUZZY_LENGTH) continue
            val fuzzy = cachedFuzzyMatch(normalized, index) ?: continue
            if (best == null || fuzzy.similarity > best.similarity) best = fuzzy
        }
        return best
    }

    private fun cachedFuzzyMatch(normalized: String, index: ZoundHubMatcher.Index): ZoundHubMatch? {
        synchronized(this) {
            if (fuzzyCacheBuiltFor != index.all.size) {
                fuzzyCache = HashMap(); fuzzyCacheBuiltFor = index.all.size
            } else if (fuzzyCache.size > FUZZY_CACHE_MAX) fuzzyCache.clear()
            if (fuzzyCache.containsKey(normalized)) return fuzzyCache[normalized]
        }
        var bestScore = 0.0
        var bestArtist: ZoundHubArtist? = null
        val all = index.all
        val names = index.normalizedNames
        val queryLen = normalized.length
        for (i in all.indices) {
            val candidate = names[i]
            if (candidate.length < MIN_FUZZY_LENGTH) continue
            val candLen = candidate.length
            val minLen = if (candLen < queryLen) candLen else queryLen
            val maxLen = if (candLen > queryLen) candLen else queryLen
            if (minLen * 5 < maxLen * 3) continue
            val score = jaroWinkler(candidate, normalized)
            if (score >= FUZZY_THRESHOLD && score > bestScore) { bestScore = score; bestArtist = all[i] }
        }
        val result = bestArtist?.let { ZoundHubMatch(artist = it, exact = false, similarity = bestScore) }
        synchronized(this) { fuzzyCache[normalized] = result }
        return result
    }

    fun jaroWinkler(a: String, b: String): Double {
        if (a.isEmpty() && b.isEmpty()) return 1.0
        if (a.isEmpty() || b.isEmpty()) return 0.0
        if (a == b) return 1.0
        val jaro = jaro(a, b)
        if (jaro < 0.7) return jaro
        val max = minOf(a.length, b.length, 4)
        var i = 0
        while (i < max && a[i] == b[i]) i++
        return jaro + i * 0.1 * (1.0 - jaro)
    }

    private fun jaro(a: String, b: String): Double {
        val aLen = a.length; val bLen = b.length
        val matchWindow = maxOf(0, maxOf(aLen, bLen) / 2 - 1)
        val aMatched = BooleanArray(aLen); val bMatched = BooleanArray(bLen)
        var matches = 0
        for (i in 0 until aLen) {
            val start = maxOf(0, i - matchWindow)
            val end = minOf(bLen - 1, i + matchWindow)
            for (j in start..end) {
                if (bMatched[j] || a[i] != b[j]) continue
                aMatched[i] = true; bMatched[j] = true; matches++; break
            }
        }
        if (matches == 0) return 0.0
        var transpositions = 0; var k = 0
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

// ─── generatore di catalogo sintetico realista ───────────────────────────────
fun buildSyntheticCatalog(n: Int): List<ZoundHubArtist> {
    val a1 = listOf("Nova","Crystal","Lone","Midnight","Neon","Silent","Golden","Wild","Urban","Frozen","Velvet","Electric","Hollow","Crimson","Paper","Sonic","Distant","Feral","Marble","Cobalt","Violet","Savage","Gentle","Iron","Lucid","Mellow","Rusty","Sable","Sterling","Tender")
    val a2 = listOf("Echo","River","Drift","Pulse","Wave","Dust","Ember","Shadow","Sky","Fog","Harbor","Mirage","Orbit","Prism","Signal","Tempo","Vault","Willow","Atlas","Bloom","Canyon","Delta","Fable","Grove","Halo","Iris","Jungle","Kite","Lagoon","Mosaic")
    val a3 = listOf("Band","Collective","Machine","Theory","Society","Club","Union","Project","Syndicate","Orchestra","Ensemble","Trio","Duo","Gang","Crew","Family","Brigade","Chapter","Council","League")
    val single = listOf("Auryn","Bexlith","Cephora","Druvane","Elowen","Fenris","Glomar","Hyndell","Isvara","Jorveth","Kryzzik","Lomadia","Myrvold","Nyvexa","Ozrenn","Pythian","Quorval","Rynovar","Skarneth","Tyvexa","Ulmorath","Vexillon","Wyrmark","Xanthea","Yzermann","Zorvath","Amphixa","Belzoric","Cindrovex","Dranmoor")
    val rng = Random(42)
    val names = LinkedHashSet<String>()
    var i = 0
    while (names.size < n) {
        val mode = i % 4
        val name = when (mode) {
            0 -> "${a1[rng.nextInt(a1.size)]} ${a2[rng.nextInt(a2.size)]} ${a3[rng.nextInt(a3.size)]}"
            1 -> "${a1[rng.nextInt(a1.size)]} ${a2[rng.nextInt(a2.size)]}"
            2 -> "${single[rng.nextInt(single.size)]}${if (rng.nextInt(3) == 0) " ${a3[rng.nextInt(a3.size)]}" else ""}"
            else -> {
                val base = "${a2[rng.nextInt(a2.size)]} ${a3[rng.nextInt(a3.size)]}"
                if (rng.nextInt(4) == 0) "The $base" else base
            }
        }
        val decorated = when (rng.nextInt(12)) {
            0 -> "$name & ${a2[rng.nextInt(a2.size)]}"
            1 -> "Café $name"
            2 -> "$name - Topic"
            else -> name
        }
        names.add(decorated)
        i++
    }
    return names.mapIndexed { idx, name ->
        ZoundHubArtist(
            id = "id$idx",
            slug = name.lowercase().replace(" ", "-").replace("&", "and").replace("é", "e"),
            name = name,
            releases = rng.nextInt(40)
        )
    }
}

fun corrupt(name: String, rng: Random): String {
    val s = name.toCharArray()
    return when (rng.nextInt(4)) {
        0 -> { // swap adiacenti
            if (s.size < 2) name
            else { val p = rng.nextInt(s.size - 1); val t = s[p]; s[p] = s[p + 1]; s[p + 1] = t; String(s) }
        }
        1 -> { // drop char
            val p = rng.nextInt(s.size); String(s.filterIndexed { idx, _ -> idx != p }.toCharArray())
        }
        2 -> { // insert char
            val p = rng.nextInt(s.size + 1); String(s, 0, p) + 'x' + String(s, p, s.size - p)
        }
        else -> name + (if (name.endsWith("e")) "s" else "z")
    }
}

fun resetCachesFor(index: ZoundHubMatcher.Index) {
    // forza il reset della cache fuzzy: un match con un indice di dimensione
    // diversa invalida fuzzyCacheBuiltFor
    val tiny = ZoundHubMatcher.buildIndex(listOf(ZoundHubArtist(id = "t", slug = "t", name = "Tiny")))
    ZoundHubMatcher.match("Tiny", tiny)
    LegacyMatcher.match("Tiny", tiny)
    check(index.all.size > 1)
}

fun measure(block: () -> Any?): Long { val t = System.nanoTime(); block(); return System.nanoTime() - t }

data class QuerySet(val queries: List<String>, val label: String)

fun main() {
    println("== Build catalogo sintetico (9.700 artisti) ==")
    val catalog = buildSyntheticCatalog(9_700)
    val tBuild0 = System.nanoTime()
    val index = ZoundHubMatcher.buildIndex(catalog)
    val tBuild1 = System.nanoTime()
    println("buildIndex: ${(tBuild1 - tBuild0) / 1_000_000} ms (${catalog.size} artisti)")

    val rng = Random(7)

    // Query: esatti, fuzzy-hit (nomi corrotti), miss (nomi assenti), multi-autore
    val exactQueries = catalog.shuffled(Random(99)).take(200).map { it.name }
    val fuzzyQueries = catalog.shuffled(Random(101)).take(200).map { corrupt(it.name, rng) }
    val missQueries = (0 until 200).map { i ->
        // lunghezze REALISTICHE di nomi artista (15-35 caratteri), assenti dal catalogo
        val pool = listOf(
            "Kravlox Hymnstorm Collective", "Vantablack Choir Cult", "Dravnmoth Signal Order",
            "Pellucid Wyrm Assembly", "Thornwake Vivisection Unit", "Gloamhurst Beacon Tribe",
            "Sallowfen Cartographers", "Wraithline Revival Society", "Bogstandard Meridian Choir",
            "Ultraviolent Waltz Division", "Cindercloister Dirge Ensemble", "Ferrofluid Panorama Trio"
        )
        pool[i % pool.size] + if (i % 3 == 0) " Nr${i}" else ""
    }
    val multiQueries = exactQueries.take(50).map { "${it}, ${missQueries[rng.nextInt(missQueries.size)]}" }

    val shortMissQueries = (0 until 200).map { i ->
        // nomi corti realistici (6-14 caratteri), assenti dal catalogo: qui lo
        // scan legacy confronta TUTTI i candidati compatibili per lunghezza,
        // l'indice n-gram riduce i candidati a decine
        val pool = listOf("Zqfvrkt", "Nyxolm", "Brevlond", "Chaskrin", "Wovendir",
            "Glamsket", "Fyrrelix", "Druphnak", "Lomskrit", "Vandrelux", "Okhrivane", "Tzelmoth")
        pool[i % pool.size] + (if (i % 4 == 0) "x" else "")
    }

    val mediumMissQueries = (0 until 200).map { i ->
        // CASO PEGGIORE REALISTICO: nomi 13-22 caratteri, compatibili per
        // lunghezza con la maggior parte del catalogo (la guardia lunghezze
        // non pota: e' qui che lo scan legacy paga JW su migliaia di candidati)
        val pool = listOf("Larkspur Mirage", "Vellum Orchid", "Quartz Bellamy", "Harrow Vale",
            "Emberlyn Frost", "Solace Wrenfield", "Tarnwick Holly", "Mirelle Fontaine",
            "Ashgrove Lumen", "Briony Castellan", "Fenwick Oracle", "Selwyn Marrow")
        pool[i % pool.size] + (if (i % 5 == 0) " Jr" else "")
    }

    val sets = listOf(
        QuerySet(exactQueries, "esatti"),
        QuerySet(fuzzyQueries, "fuzzy-hit"),
        QuerySet(missQueries, "miss-lunghi"),
        QuerySet(mediumMissQueries, "miss-medi"),
        QuerySet(shortMissQueries, "miss-corti"),
        QuerySet(multiQueries, "multi-autore")
    )

    // ── EQUIVALENZA ──
    println("\n== Verifica equivalenza REPLICA inline vs MATCHER ATTUALE ==")
    var mismatches = 0
    for (set in sets) {
        for (q in set.queries) {
            resetCachesFor(index)
            val legacy = LegacyMatcher.match(q, index)
            resetCachesFor(index)
            val nuovo = ZoundHubMatcher.match(q, index)
            val ok = (legacy?.artist?.id == nuovo?.artist?.id) &&
                     (legacy?.exact == nuovo?.exact) &&
                     (legacy?.similarity == nuovo?.similarity)
            if (!ok) {
                mismatches++
                if (mismatches <= 5) println("MISMATCH [${set.label}] '$q': legacy=${legacy?.artist?.name}/${legacy?.exact}/${legacy?.similarity} nuovo=${nuovo?.artist?.name}/${nuovo?.exact}/${nuovo?.similarity}")
            }
        }
    }
    println(if (mismatches == 0) "EQUIVALENZA OK: 0 mismatch su ${sets.sumOf { it.queries.size }} query" else "EQUIVALENZA FALLITA: $mismatches mismatch")

    // ── EQUIVALENZA tie-break: nomi duplicati normalizzati nel catalogo ──
    println("\n== Tie-break su nomi duplicati normalizzati ==")
    val dupCatalog = catalog + listOf(
        catalog[0].copy(id = "dup-a", name = "${catalog[0].name}"),
        catalog[1].copy(id = "dup-b", name = "The ${catalog[1].name}"),
        catalog[2].copy(id = "dup-c", name = "${catalog[2].name} - Topic")
    )
    val dupIndex = ZoundHubMatcher.buildIndex(dupCatalog)
    val dupQueries = listOf(catalog[0].name, catalog[1].name, catalog[2].name,
        corrupt(catalog[0].name, rng), corrupt(catalog[1].name, rng), corrupt(catalog[2].name, rng))
    var dupMismatch = 0
    for (q in dupQueries) {
        resetCachesFor(dupIndex)
        val legacy = LegacyMatcher.match(q, dupIndex)
        resetCachesFor(dupIndex)
        val nuovo = ZoundHubMatcher.match(q, dupIndex)
        if (legacy?.artist?.id != nuovo?.artist?.id || legacy?.exact != nuovo?.exact || legacy?.similarity != nuovo?.similarity) {
            dupMismatch++
            println("DUP MISMATCH '$q': legacy=${legacy?.artist?.name} nuovo=${nuovo?.artist?.name}")
        }
    }
    println(if (dupMismatch == 0) "TIE-BREAK OK: stesso record scelto su $dupQueries.size query con duplicati" else "TIE-BREAK FALLITO: $dupMismatch")

    // ── PERFORMANCE (cold: cache fuzzy svuotata ad ogni query) ──
    println("\n== Performance miss-cold (200 query, cache svuotata ad ogni giro) ==")
    var t = 0L
    val t0Legacy = System.nanoTime()
    for (q in missQueries) { resetCachesFor(index); t += measure { LegacyMatcher.match(q, index) } }
    val tLegacy = (System.nanoTime() - t0Legacy) / 1_000_000

    val t0New = System.nanoTime()
    for (q in missQueries) { resetCachesFor(index); t += measure { ZoundHubMatcher.match(q, index) } }
    val tNew = (System.nanoTime() - t0New) / 1_000_000

    println("REPLICA inline: $tLegacy ms totali, ${tLegacy / missQueries.size} ms/query (miss lunghi)")
    println("MATCHER attuale: $tNew ms totali, ${tNew / missQueries.size} ms/query (miss lunghi)")
    println("Delta miss lunghi: ${"%.1f".format(tLegacy.toDouble() / tNew)}x")

    var tS = 0L
    val t0LegacyS = System.nanoTime()
    for (q in shortMissQueries) { resetCachesFor(index); tS += measure { LegacyMatcher.match(q, index) } }
    val tLegacyS = (System.nanoTime() - t0LegacyS) / 1_000_000
    val t0NewS = System.nanoTime()
    for (q in shortMissQueries) { resetCachesFor(index); tS += measure { ZoundHubMatcher.match(q, index) } }
    val tNewS = (System.nanoTime() - t0NewS) / 1_000_000
    println("REPLICA miss corti: $tLegacyS ms totali, ${tLegacyS / shortMissQueries.size} ms/query")
    println("MATCHER miss corti: $tNewS ms totali, ${tNewS / shortMissQueries.size} ms/query")
    println("Delta miss corti: ${"%.1f".format(tLegacyS.toDouble() / tNewS)}x")

    var tM = 0L
    val t0LegacyM = System.nanoTime()
    for (q in mediumMissQueries) { resetCachesFor(index); tM += measure { LegacyMatcher.match(q, index) } }
    val tLegacyM = (System.nanoTime() - t0LegacyM) / 1_000_000
    val t0NewM = System.nanoTime()
    for (q in mediumMissQueries) { resetCachesFor(index); tM += measure { ZoundHubMatcher.match(q, index) } }
    val tNewM = (System.nanoTime() - t0NewM) / 1_000_000
    println("REPLICA miss medi (peggiore caso realistico): $tLegacyM ms totali, ${tLegacyM / mediumMissQueries.size} ms/query")
    println("MATCHER miss medi (peggiore caso realistico): $tNewM ms totali, ${tNewM / mediumMissQueries.size} ms/query")
    println("Delta miss medi: ${"%.1f".format(tLegacyM.toDouble() / tNewM)}x")

    // ── Simulazione filtro lista: 2.000 brani, testi autori misti ──
    println("\n== Simulazione: filtro lista 2.000 item (primo giro, cache fredda) ==")
    val items = (0 until 2_000).map { i ->
        when (i % 5) {
            0 -> exactQueries[i % exactQueries.size]
            1 -> fuzzyQueries[i % fuzzyQueries.size]
            2 -> missQueries[i % missQueries.size]
            3 -> multiQueries[i % multiQueries.size]
            else -> catalog[(i * 37) % catalog.size].name
        }
    }
    resetCachesFor(index)
    val tFilter0 = System.nanoTime()
    val hiddenLegacy = items.count { LegacyMatcher.match(it, index) != null }
    val tFilter1 = System.nanoTime()
    resetCachesFor(index)
    val tFilter2 = System.nanoTime()
    val hiddenNew = items.count { ZoundHubMatcher.match(it, index) != null }
    val tFilter3 = System.nanoTime()
    println("REPLICA: ${(tFilter1 - tFilter0) / 1_000_000} ms (nascosti=$hiddenLegacy)")
    println("MATCHER: ${(tFilter3 - tFilter2) / 1_000_000} ms (nascosti=$hiddenNew)")
    check(hiddenLegacy == hiddenNew) { "Conteggi filtrati diversi!" }
    println("Conteggio nascosti identico: OK")

    // secondo giro (cache calda): costo per item ~0
    val tWarm0 = System.nanoTime()
    items.count { ZoundHubMatcher.match(it, index) != null }
    val tWarm1 = System.nanoTime()
    println("MATCHER secondo giro (cache calda): ${(tWarm1 - tWarm0) / 1_000_000} ms")

    println("\nBENCHMARK COMPLETATO")
}

package it.fast4x.zoundhub

/**
 * Persistenza FACOLTATIVA delle decisioni del filtro AI ZoundHub
 * ("questo testo autori e' di un artista AI?").
 *
 * PERCHE' ESISTE (v1.4): con il filtro reattivo v1.3 il costo fuzzy non
 * blocca piu' la UI, ma resta un costo PER PROCESSO — la decisionCache
 * muore col processo e su dispositivi vecchi/lenti il primo giro di
 * filtraggio dopo ogni avvio ripaga i miss fuzzy (0.5-3 ms a nome su
 * hardware desktop, 3-5x su telefoni datati). Persistendo le decisioni
 * su disco il costo fuzzy viene pagato UNA volta per dispositivo, non
 * una volta per processo: al riavvio il filtro parte a caldo e le liste
 * si filtrano praticamente all'istante anche su hardware lento.
 *
 * CHIAVE E INVALIDAZIONE: le decisioni sono taggate con la versione del
 * catalogo (fetchedAt, epoch millis dello snapshot ZoundHub). Un refresh
 * del catalogo cambia fetchedAt -> le decisioni vecchie non sono piu'
 * valide e vengono eliminate dal purge dopo il preload di quelle nuove.
 *
 * CONTRATTO (implementato lato app con Room, vedi composeApp
 * ZoundHubDecisionPersistenceRoom + dao.ZoundHubDecisionDao):
 * il modulo :zoundhub resta kotlin("jvm") puro e dichiara SOLO
 * l'interfaccia; nessuna dipendenza da Room dentro il modulo.
 *
 * TUTTI I METODI SONO suspend e girano su dispatcher IO lato chiamante
 * (lo store li invoca dal suo scope interno o da refresh su Dispatchers.IO).
 * Fallimenti = best-effort: una persistenza che fallisce degrada il
 * sistema alla v1.3 (ricalcolo in background), non rompe nulla.
 */
interface ZoundHubDecisionPersistence {

    /**
     * Decisioni salvate per il catalogo con questa versione (fetchedAt):
     * testo autori RAW (stessa chiave usata da
     * [ZoundHubCatalogStore.isAiHidden]) -> true se il testo e' AI.
     */
    suspend fun load(fetchedAt: Long): Map<String, Boolean>

    /**
     * Salva/aggiorna (INSERT OR REPLACE) le decisioni con la versione del
     * catalogo indicata. Chiamato in batch (le implementazioni devono
     * chunkare gli upsert per non creare transazioni giganti).
     */
    suspend fun save(fetchedAt: Long, decisions: Map<String, Boolean>)

    /**
     * Elimina le decisioni di versioni di catalogo DIVERSE da
     * [keepFetchedAt]: a regime sul disco resta solo la versione corrente.
     */
    suspend fun purgeOthers(keepFetchedAt: Long)
}

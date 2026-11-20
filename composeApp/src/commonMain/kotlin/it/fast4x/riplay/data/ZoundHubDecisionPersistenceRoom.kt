package it.fast4x.riplay.data

import it.fast4x.riplay.data.dao.ZoundHubDecisionDao
import it.fast4x.riplay.data.models.ZoundHubDecision
import it.fast4x.zoundhub.ZoundHubDecisionPersistence

/**
 * Implementazione Room di [ZoundHubDecisionPersistence] (interfaccia del
 * modulo :zoundhub, kotlin("jvm") puro: il modulo dichiara solo il
 * contratto, l'app fornisce il backend disco).
 *
 * v1.4 "dispositivi vecchi/lenti": le decisioni del filtro "nascondi
 * contenuti AI" sopravvivono al processo -> il costo fuzzy viene pagato
 * UNA volta per dispositivo. La tabella e' una cache derivata: capillow
 * ulteriori righe di nessuna tabella utente, cancellabile senza perdite.
 *
 * @param dao DAO Room (DatabaseInitializer.zoundHubDecisionDao())
 */
class ZoundHubDecisionPersistenceRoom(
    private val dao: ZoundHubDecisionDao,
) : ZoundHubDecisionPersistence {

    override suspend fun load(fetchedAt: Long): Map<String, Boolean> =
        dao.loadForCatalog(fetchedAt).associate { it.artistsText to it.isAi }

    override suspend fun save(fetchedAt: Long, decisions: Map<String, Boolean>) {
        // chunk: evita transazioni giganti con migliaia di decisioni nuove
        // (primo giro col filtro attivo su una libreria grande)
        decisions.entries.chunked(UPSERT_CHUNK).forEach { chunk ->
            dao.upsertAll(
                chunk.map { (text, isAi) ->
                    ZoundHubDecision(
                        artistsText = text,
                        isAi = isAi,
                        catalogFetchedAt = fetchedAt,
                    )
                }
            )
        }
    }

    override suspend fun purgeOthers(keepFetchedAt: Long) {
        dao.purgeOtherCatalogs(keepFetchedAt)
    }

    companion object {
        private const val UPSERT_CHUNK = 500

        /** Istanza pronta collegata al database corrente (per il wiring delle app). */
        fun fromCurrentDatabase(): ZoundHubDecisionPersistence =
            ZoundHubDecisionPersistenceRoom(getDatabaseInitializer().zoundHubDecisionDao())
    }
}

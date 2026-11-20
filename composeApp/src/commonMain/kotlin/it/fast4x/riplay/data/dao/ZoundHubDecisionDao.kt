package it.fast4x.riplay.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import it.fast4x.riplay.data.models.ZoundHubDecision

/**
 * DAO della cache delle decisioni del filtro AI ZoundHub (v1.4).
 *
 * Non e' dati utente: e' una cache derivata, ricostruibile dal catalogo.
 * Le scritture avvengono in batch (chunk di 500 righe lato chiamante) e il
 * purge delle versioni vecchie gira DOPO il preload della versione nuova
 * (un refresh fallito non tocca le decisioni del catalogo corrente).
 */
@Dao
interface ZoundHubDecisionDao {

    @Upsert
    suspend fun upsertAll(decisions: List<ZoundHubDecision>)

    @Query("SELECT * FROM zoundhub_decisions WHERE catalogFetchedAt = :fetchedAt")
    suspend fun loadForCatalog(fetchedAt: Long): List<ZoundHubDecision>

    @Query("DELETE FROM zoundhub_decisions WHERE catalogFetchedAt <> :keepFetchedAt")
    suspend fun purgeOtherCatalogs(keepFetchedAt: Long)

    @Query("SELECT COUNT(*) FROM zoundhub_decisions WHERE catalogFetchedAt = :fetchedAt")
    suspend fun countForCatalog(fetchedAt: Long): Int

    @Query("DELETE FROM zoundhub_decisions")
    suspend fun clearAll()
}

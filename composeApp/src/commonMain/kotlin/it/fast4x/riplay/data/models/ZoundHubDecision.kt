package it.fast4x.riplay.data.models

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Decisione persistente del filtro AI ZoundHub ("questo testo autori e'
 * di un artista AI?" — v1.4, dispositivi vecchi/lenti).
 *
 * Cache DISCO delle decisioni gia' calcolate da ZoundHubCatalogStore: al
 * riavvio il filtro "nascondi contenuti AI" parte a CALDO anche su
 * dispositivi lenti, senza ripagare i miss fuzzy (0.5-3 ms a nome su
 * desktop, 3-5x su telefoni datati) per i testi autori gia' visti.
 * Il costo del match fuzzy viene cosi' pagato UNA volta per dispositivo,
 * non una volta per processo.
 *
 * INVALIDAZIONE: ogni riga e' taggata con la versione del catalogo
 * (catalogFetchedAt, epoch millis dello snapshot ZoundHub). Un refresh
 * cambia fetchedAt: le righe della versione nuova vengono caricate dallo
 * store al rebuild e quelle delle versioni vecchie eliminate
 * (ZoundHubDecisionDao.purgeOtherCatalogs). A regime sul disco resta solo
 * la versione corrente.
 *
 * CONTENUTO SOLO DERIVATO: la tabella e' una cache ricostruibile al 100%
 * dal catalogo (si puo' cancellare senza perdite); il catalogo originale
 * resta nel file JSON della cache (zoundhub-catalog.json).
 */
@Entity(
    tableName = "zoundhub_decisions",
    indices = [Index(value = ["catalogFetchedAt"])]
)
data class ZoundHubDecision(
    /** Testo autori RAW (la stessa chiave usata da isAiHidden, es. "Artist A, Artist B"). */
    @PrimaryKey val artistsText: String,
    /** true = il testo matcha (esatto o fuzzy) un artista AI con QUESTA versione di catalogo. */
    val isAi: Boolean,
    /** Epoch millis del catalogo con cui la decisione e' stata calcolata. */
    val catalogFetchedAt: Long,
)

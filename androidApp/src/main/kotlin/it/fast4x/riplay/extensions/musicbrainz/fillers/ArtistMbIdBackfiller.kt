package it.fast4x.riplay.extensions.musicbrainz.fillers

import it.fast4x.riplay.data.Database
import it.fast4x.riplay.extensions.musicbrainz.MusicBrainz
import it.fast4x.riplay.extensions.musicbrainz.repository.ArtistRepository
import it.fast4x.riplay.extensions.musicbrainz.utils.IdentityUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import timber.log.Timber

class ArtistMbIdBackfiller() {

    val mbClient = MusicBrainz()
    val artistRepository = ArtistRepository()

    suspend fun backfill(limit: Int = 50): Result = withContext(Dispatchers.IO) {
        // Negative cache (Fase 3): non si ri-tentano gli artisti falliti da meno
        // di NEGATIVE_CACHE_TTL giorni — prima la stessa lista veniva
        // ri-selezionata a ogni run, sprecando chiamate MB su nomi senza match.
        val failedBefore = System.currentTimeMillis() - NEGATIVE_CACHE_TTL_MS
        val artists = Database.artistDao().getArtistsWithoutMbId(limit, failedBefore)
        Timber.tag("MbIdBackfill").i("Artists to enrich: ${artists.size}")

        var success = 0
        var failed = 0

        for (artist in artists) {
            try {
                delay(1100) // rate limit MB
                val artistName = artist.name ?: continue
                val searchResult = mbClient.searchArtistByName(artistName)

                // FIX (Fase 2): prima si prendeva il PRIMO risultato (firstOrNull)
                // e per nomi generici si bindava il mbId dell'artista sbagliato.
                // Ora: scoring di disambiguazione (similarita' nome + score MB +
                // coerenza tipo/paese) con soglia anti-falsi-positivi.
                val best = IdentityUtils.pickBestCandidate(
                    localName = artistName,
                    localType = artist.artistType,
                    localCountry = artist.countryCode,
                    candidates = searchResult
                ) { c -> IdentityUtils.CandidateInfo(c.name, c.score, c.type, c.country) }

                if (best != null) {
                    val mbid = best.candidate.id
                    artistRepository.upsertSmart(
                        artist.copy(
                            mbId = mbid,
                            mbMatchScore = best.score,
                            mbMatchedAt = System.currentTimeMillis(),
                            mbLookupFailedAt = null
                        )
                    )
                    success++
                    Timber.tag("MbIdBackfill")
                        .i("  ✓ ${artist.name} → $mbid (score=%.2f, sim=%.2f)".format(best.score, best.nameSimilarity))
                } else {
                    // Nessun candidato affidabile: si registra il fallimento
                    // nella negative cache invece di riprovare ogni run.
                    Database.artistDao().upsert(
                        artist.copy(mbLookupFailedAt = System.currentTimeMillis())
                    )
                    Timber.tag("MbIdBackfill").w("  ⊘ ${artist.name} not found on MB (cached for retry)")
                }
            } catch (e: Exception) {
                failed++
                Timber.tag("MbIdBackfill").w("  ✗ ${artist.name}: ${e.message}")
            }
        }

        Result(success, failed)
    }

    data class Result(val success: Int, val failed: Int)

    companion object {
        /** Un tentativo fallito non si ripete prima di 30 giorni. */
        const val NEGATIVE_CACHE_TTL_MS = 30L * 24 * 60 * 60 * 1000
    }
}

package it.fast4x.riplay.extensions.musicbrainz.repository

import android.util.Log
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.data.models.Artist
import it.fast4x.riplay.enums.ArtistNature
import it.fast4x.riplay.extensions.experimental.recommendationstrategy.classifiers.ArtistClassifier
import it.fast4x.riplay.extensions.musicbrainz.utils.IdentityUtils
import timber.log.Timber
import java.util.UUID

class ArtistRepository() {

    /**
     * Inserisce o aggiorna un artista con deduplicazione intelligente.
     * Ritorna l'ID stabile dell'artista (esistente o nuovo).
     *
     * Logica di match (in ordine di affidabilità):
     * 1. mbId (MusicBrainz ID) — match perfetto
     * 2. youtubeChannelId — match perfetto
     * 3. Nome normalizzato — match fuzzy
     * 4. Nessun match → crea nuovo
     */
    suspend fun upsertSmart(artist: Artist): String {

        val artistDao = Database.artistDao()

        // 1. Search by mbId
        if (artist.mbId != null) {
            Database.artistDao().getByMbId(artist.mbId)?.let { existing ->
                val merged = mergeArtists(existing, artist)
                val classifiedArtist = if (merged.nature == ArtistNature.UNKNOWN) {
                    merged.copy(nature = ArtistClassifier.classify(artist))
                } else {
                    merged
                }
                artistDao.upsert(classifiedArtist)
                Timber.tag("ArtistRepo").d("Match by mbId: ${existing.name} (${existing.id})")
                return existing.id
            }
        }

        // 2. Search by youtubeChannelId
        if (artist.youtubeChannelId != null) {
            artistDao.getByYoutubeChannelId(artist.youtubeChannelId)?.let { existing ->
                val merged = mergeArtists(existing, artist)
                val classifiedArtist = if (merged.nature == ArtistNature.UNKNOWN) {
                    merged.copy(nature = ArtistClassifier.classify(artist))
                } else {
                    merged
                }
                artistDao.upsert(classifiedArtist)
                Timber.tag("ArtistRepo")
                    .d("Match by ytChannelId: ${existing.name} (${existing.id})")
                return existing.id
            }
        }

        // 3. Search by normalized name
        if (!artist.name.isNullOrBlank()) {
            // Per match fuzzy migliore, scarichiamo tutti e confrontiamo in-memory
            // (potrebbe essere lento con migliaia di artisti — ottimizza con cache se serve)
            val candidates = artistDao.getAllWithName()
            val normalizedTarget = IdentityUtils.normalizeArtistName(artist.name)
            candidates.firstOrNull { existing ->
                IdentityUtils.normalizeArtistName(existing.name) == normalizedTarget
            }?.let { existing ->
                val merged = mergeArtists(existing, artist)
                val classifiedArtist = if (merged.nature == ArtistNature.UNKNOWN) {
                    merged.copy(nature = ArtistClassifier.classify(artist))
                } else {
                    merged
                }
                artistDao.upsert(classifiedArtist)
                Timber.tag("ArtistRepo").d("Match by name: ${existing.name} (${existing.id})")
                return existing.id
            }
        }

        // 4. Nessun match → crea nuovo con ID stabile
        val newId = artist.id.ifBlank {
            // Se l'artista viene da MB, usa mb-<mbid>; se da YT, usa ytChannelId; altrimenti UUID
            when {
                artist.mbId != null -> "mb-${artist.mbId}"
                artist.youtubeChannelId != null -> artist.youtubeChannelId
                else -> "artist_${UUID.randomUUID()}"
            }
        }
        val classifiedArtist = if (artist.nature == ArtistNature.UNKNOWN) {
            artist.copy(nature = ArtistClassifier.classify(artist))
        } else {
            artist
        }
        artistDao.upsert(classifiedArtist.copy(id = newId))
        Timber.tag("ArtistRepo").d("Created new artist: ${artist.name} ($newId)")
        return newId
    }

    /**
     * Merge di due artisti: combina i campi, preferendo i valori non-null dell'incoming.
     */
    private fun mergeArtists(existing: Artist, incoming: Artist): Artist {
        return existing.copy(
            mbId = incoming.mbId ?: existing.mbId,
            youtubeChannelId = incoming.youtubeChannelId ?: existing.youtubeChannelId,
            isYoutubeArtist = incoming.isYoutubeArtist || existing.isYoutubeArtist,
            name = incoming.name ?: existing.name,
            thumbnailUrl = incoming.thumbnailUrl ?: existing.thumbnailUrl,
            timestamp = incoming.timestamp ?: existing.timestamp,
            bookmarkedAt = incoming.bookmarkedAt ?: existing.bookmarkedAt,
            genres = incoming.genres ?: existing.genres,
            artistType = incoming.artistType ?: existing.artistType,
            countryCode = incoming.countryCode ?: existing.countryCode,
            beginYear = incoming.beginYear ?: existing.beginYear,
            tags = incoming.tags ?: existing.tags,
            rating = incoming.rating ?: existing.rating,
            ratingVotes = incoming.ratingVotes ?: existing.ratingVotes,
            wikipediaUrl = incoming.wikipediaUrl ?: existing.wikipediaUrl,
            wikipediaBio = incoming.wikipediaBio ?: existing.wikipediaBio,
            description = incoming.description ?: existing.description,
            disambiguation = incoming.disambiguation ?: existing.disambiguation,
            links = incoming.links ?: existing.links,
            nature = if (incoming.nature != ArtistNature.UNKNOWN) incoming.nature else existing.nature
        )
    }
}
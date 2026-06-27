package it.fast4x.riplay.extensions.experimental.recommendationstrategy.service

import android.util.Log
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.data.models.Artist
import it.fast4x.riplay.data.models.Song
import it.fast4x.riplay.data.models.SongArtistCrossRef
import it.fast4x.riplay.extensions.musicbrainz.MusicBrainz
import it.fast4x.riplay.extensions.musicbrainz.repository.AlbumRepository
import it.fast4x.riplay.extensions.musicbrainz.repository.ArtistRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

class SongEnricherService() {


    private val songDao = Database.songDao()
    private val artistDao = Database.artistDao()
    private val albumDao = Database.albumDao()
    private val songArtistRefDao = Database.songArtistCrossRefDao()
    private val artistRepository = ArtistRepository()
    private val albumRepository = AlbumRepository()
    private val mbClient = MusicBrainz()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Stato di enrich per la UI (mostra spinner "Recuperando info...")
    private val _enrichmentState = MutableStateFlow<EnrichmentState>(EnrichmentState.Idle)
    val enrichmentState: StateFlow<EnrichmentState> = _enrichmentState

    /**
     * Chiamato dal PlayerService quando un nuovo brano inizia.
     * Verifica il livello di ricchezza e avvia enrich se necessario.
     */
    fun onSongPlayed(songId: String) {
        scope.launch {
            val song = songDao.getById(songId) ?: return@launch

            // Calcola il livello attuale
            val level = assessRichnessLevel(song)
            Timber.tag("SongEnricherService").d("Song '${song.title}' at level $level")

            // Se è già Livello 2+, non fare nulla
            if (level >= RichnessLevel.LEVEL_2) {
                _enrichmentState.value = EnrichmentState.Complete(songId)
                return@launch
            }

            // Avvia enrich asincrono
            _enrichmentState.value = EnrichmentState.Loading(songId)

            try {
                enrichSong(song)
                _enrichmentState.value = EnrichmentState.Complete(songId)
            } catch (e: Exception) {
                Timber.tag("SongEnricherService").w("Enrichment failed for ${song.title}: ${e.message}")
                _enrichmentState.value = EnrichmentState.Failed(songId, e.message ?: "Unknown")
            }
        }
    }

    private suspend fun assessRichnessLevel(song: Song): RichnessLevel {
        // Livello 2: ha genres E (albumId o artist con mbId)
        if (!song.genres.isNullOrEmpty() && (song.albumId != null)) {
            return RichnessLevel.LEVEL_2
        }

        // Livello 1: ha SongArtistCrossRef
        val artistCount = songArtistRefDao.countArtistsForSong(song.id)
        if (artistCount > 0) return RichnessLevel.LEVEL_1

        // Altrimenti Livello 0
        return RichnessLevel.LEVEL_0
    }

    private suspend fun enrichSong(song: Song) {
        val artistName = song.artistsText?.trim() ?: return

        // Step 1: Cerca o crea l'artista via MB
        val artist = ensureArtistEnriched(artistName) ?: return

        // Step 2: Crea SongArtistCrossRef
        songArtistRefDao.upsert(
            SongArtistCrossRef(
                songId = song.id,
                artistId = artist.id,
                role = "main",
                order = 0
            )
        )

        // Step 3: Aggiorna la song con genres dell'artista (se la song non li ha)
        if (song.genres.isNullOrEmpty() && !artist.genres.isNullOrEmpty()) {
            songDao.upsert(song.copy(genres = artist.genres))
        }

        // Step 4: Cerca album su MB per popolare albumId (opzionale, più lento)
        // Lo facciamo solo se non abbiamo già albumId
        if (song.albumId == null) {
            tryEnrichAlbum(song, artist)
        }
    }

    private suspend fun ensureArtistEnriched(artistName: String): Artist? {
        // Cerca nel DB locale prima
        val existing = artistDao.findByNameExactIgnoreCase(artistName)
        if (existing != null && existing.mbId != null && !existing.genres.isNullOrEmpty()) {
            return existing  // Già arricchito
        }

        // Fetch da MB
        return try {
            val searchResults = mbClient.searchArtistByName(artistName)
            val mbid = searchResults.firstOrNull()?.id ?: return null

            val details = mbClient.fetchArtistDetail(mbid)
            val genres = details.genres.map { it.name }.takeIf { it.isNotEmpty() }
            val tags = details.tags?.map { it.name }?.takeIf { it.isNotEmpty() }

            val artist = Artist(
                id = existing?.id ?: "mb-$mbid",
                name = artistName,
                mbId = mbid,
                genres = genres,
                tags = tags,
                artistType = details.type,
                countryCode = details.country,
                beginYear = details.lifeSpan?.begin?.substring(0, 4)?.toIntOrNull(),
                timestamp = System.currentTimeMillis()
            )
            artistRepository.upsertSmart(artist)
            artist
        } catch (e: Exception) {
            Timber.tag("SongEnricherService").w("Artist fetch failed for '$artistName': ${e.message}")
            null
        }
    }

    private suspend fun tryEnrichAlbum(song: Song, artist: Artist) {
        // Richiede titolo album che non abbiamo nella song...
        // Possibile solo se l'utente ha aperto la pagina album prima.
        // Per ora skip — l'album viene arricchito on-demand quando l'utente lo apre.
    }

    enum class RichnessLevel { LEVEL_0, LEVEL_1, LEVEL_2 }

    sealed class EnrichmentState {
        object Idle : EnrichmentState()
        data class Loading(val songId: String) : EnrichmentState()
        data class Complete(val songId: String) : EnrichmentState()
        data class Failed(val songId: String, val error: String) : EnrichmentState()
    }
}
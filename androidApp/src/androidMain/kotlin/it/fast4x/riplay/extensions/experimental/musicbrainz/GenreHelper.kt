package it.fast4x.riplay.extensions.experimental.musicbrainz

import it.fast4x.riplay.data.Database
import it.fast4x.riplay.data.models.Artist
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber

class Genrehelper(
    private val mbClient: MusicBrainz
) {
    val coroutineScope = CoroutineScope(Dispatchers.IO)

    /**
     * Chiamato quando l'utente visualizza la pagina dell'artista
     */
    suspend fun onArtistViewed(artistId: String) {
        val artist = Database.artist(artistId).first() ?: return

        // Se è null, non abbiamo ancora cercato. Se è emptyList, abbiamo già cercato ma non c'erano.
        if (artist.genres != null || artist.name == null) return

        try {
            val apiGenres = mbClient.fetchArtistGenres(artist.name)
            Timber.d("Genrehelper onArtistViewed genres fetched $apiGenres")
            // Se l'API ritorna vuoto, salviamo emptyList per non ripetere la chiamata in futuro
            coroutineScope.launch {
                Database.update(artist.copy(genres = apiGenres.ifEmpty { emptyList() }))
            }
            Timber.d("Genrehelper onArtistViewed genres updated $apiGenres")
        } catch (e: Exception) {
            // Errore di rete: non facciamo nulla, genres rimarrà null e riproverà la prossima volta
            Timber.e("Genrehelper onArtistViewed error ${e.stackTraceToString()}")
        }
    }

    /**
     * Chiamato quando l'utente visualizza la pagina dell'album
     */
    suspend fun onAlbumViewed(albumId: String) {
        val album = Database.album(albumId).first() ?: return
        if (album.genres != null || album.title == null || album.authorsText == null) return

        val artist = Database.artistByName(album.authorsText).first() ?: return

        try {
            // Proviamo a prendere i generi specifici dell'album
            val apiGenres = mbClient.fetchAlbumGenres(album.title, artist?.name ?: "")

            Timber.d("Genrehelper onAlbumViewed genres fetched $apiGenres")

            if (apiGenres.isNotEmpty()) {
                // L'album ha generi propri, usiamo quelli
                coroutineScope.launch {
                    Database.update(album.copy(genres = apiGenres))
                }
                Timber.d("Genrehelper onAlbumViewed genres updated $apiGenres")
            } else {
                // L'album non ha generi su MB. Fallback: usiamo quelli dell'artista
                val artistGenres = getOrFetchArtistGenres(artist)
                coroutineScope.launch {
                    Database.update(album.copy(genres = artistGenres.ifEmpty { emptyList() }))
                }
                Timber.d("Genrehelper onAlbumViewed genres updated from artistGenres $artistGenres")
            }
        } catch (e: Exception) {
            // Errore di rete
            Timber.e("Genrehelper onAlbumViewed error ${e.stackTraceToString()}")
        }
    }

    /**
     * Chiamato quando l'utente ascolta una canzone
     */
    suspend fun onSongPlayed(songId: String) {
        Timber.d("Genrehelper onSongPlayed $songId")
        val song = Database.song(songId).first() ?: return
        if (song.genres != null || song.artistsText == null) return

        val artist = Database.artistByName(song.artistsText).first() ?: return
        Timber.d("Genrehelper onSongPlayed artist $artist")

        val artistGenres = getOrFetchArtistGenres(artist)
        Timber.d("Genrehelper onSongPlayed genres fetched $artistGenres")

        // La canzone eredita i generi dell'artista
        coroutineScope.launch {
            Database.upsert(song.copy(genres = artistGenres.ifEmpty { emptyList() }))
        }
        Timber.d("Genrehelper onSongPlayed genres updated from artistGenres $artistGenres")
    }

    /**
     * Helper: Ritorna i generi dell'artista se già in DB, altrimenti li fetcha da MB
     */
    private suspend fun getOrFetchArtistGenres(artist: Artist?): List<String> {
        if (artist == null) return emptyList()
        // Se abbiamo già i generi dell'artista (anche lista vuota), li usiamo
        if (artist.genres != null) return artist.genres

        // Altrimenti facciamo la chiamata per l'artista
        onArtistViewed(artist.id)

        // Rileggiamo l'artista dal DB ora che è aggiornato
        return Database.artist(artist.id).first()?.genres ?: emptyList()
    }
}
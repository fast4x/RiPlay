package it.fast4x.riplay.utils

import it.fast4x.riplay.data.Database
import it.fast4x.riplay.data.models.Playlist
import it.fast4x.riplay.data.models.SongPlaylistMap
import it.fast4x.spotifymeta.SpotifyPlaylist
import it.fast4x.spotifymeta.SpotifyPlaylistMeta
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** avanzamento dell'importazione */
data class SpotifyImportProgress(
    val current: Int,
    val total: Int,
    val trackName: String? = null
)

/** esito dell'importazione */
data class SpotifyImportResult(
    val playlistName: String? = null,
    val playlistId: Long = 0L,
    val imported: Int = 0,
    val skipped: Int = 0,
    val total: Int = 0,
    /** true se da Spotify e' stata letta solo una parte della playlist */
    val partial: Boolean = false,
    val error: String? = null
) {
    val isSuccess: Boolean get() = error == null && imported > 0
}

/**
 * Importa una playlist di Spotify dentro RiPlay.
 *
 * Viene letta la playlist da Spotify, viene cercato il brano corrispondente su YouTube Music
 * per ogni traccia e il risultato viene aggiunto a una nuova playlist. Le tracce per cui non
 * e' stato trovato un brano corrispondente vengono saltate.
 */
object SpotifyPlaylistImporter {

    /**
     * @param url url della playlist di Spotify (o il suo id)
     * @param onProgress invocata dopo ogni traccia elaborata
     */
    suspend fun import(
        url: String,
        onProgress: (SpotifyImportProgress) -> Unit = {}
    ): SpotifyImportResult = withContext(Dispatchers.IO) {
        val playlist = SpotifyPlaylist.fetch(url)
            ?: return@withContext SpotifyImportResult(
                error = "Playlist non trovata o non accessibile"
            )

        if (playlist.tracks.isEmpty()) {
            return@withContext SpotifyImportResult(
                playlistName = playlist.name,
                total = 0,
                error = "La playlist non contiene brani"
            )
        }

        val playlistName = playlist.name?.takeIf { it.isNotBlank() } ?: "Spotify Playlist"
        val newPlaylist = Playlist(name = playlistName)
        val playlistId = Database.insert(newPlaylist)

        var imported = 0
        var skipped = 0

        playlist.tracks.forEachIndexed { index, track ->
            val match = findMatchingSong(track)

            if (match == null) {
                skipped++
            } else {
                try {
                    saveMatchedSong(match, playlistId, index)
                    imported++
                } catch (e: Exception) {
                    e.printStackTrace()
                    skipped++
                }
            }

            onProgress(
                SpotifyImportProgress(
                    current = index + 1,
                    total = playlist.tracks.size,
                    trackName = track.name
                )
            )
        }

        // se non e' stato trovato nessun brano la playlist vuota viene rimossa
        if (imported == 0) {
            runCatching { Database.delete(newPlaylist.copy(id = playlistId)) }
            return@withContext SpotifyImportResult(
                playlistName = playlistName,
                imported = 0,
                skipped = skipped,
                total = playlist.tracks.size,
                error = "Nessun brano della playlist e' stato trovato su YouTube Music"
            )
        }

        SpotifyImportResult(
            playlistName = playlistName,
            playlistId = playlistId,
            imported = imported,
            skipped = skipped,
            total = playlist.tracks.size,
            partial = !playlist.isComplete
        )
    }

    /** salva il brano trovato su YouTube Music e lo aggiunge alla playlist */
    private suspend fun saveMatchedSong(
        match: MatchedTrack,
        playlistId: Long,
        position: Int
    ) {
        val candidate = match.candidate
        val mediaItem = candidate.asMediaItem

        Database.asyncTransaction {
            insert(mediaItem)
            insert(
                SongPlaylistMap(
                    songId = mediaItem.mediaId,
                    playlistId = playlistId,
                    position = position
                ).default()
            )
        }
    }
}

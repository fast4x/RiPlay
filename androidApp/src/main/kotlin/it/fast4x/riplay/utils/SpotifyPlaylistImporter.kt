package it.fast4x.riplay.utils

import androidx.room.withTransaction
import it.fast4x.riplay.R
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.data.models.Playlist
import it.fast4x.riplay.data.models.Song
import it.fast4x.riplay.data.models.SongPlaylistMap
import it.fast4x.spotifymeta.SpotifyPlaylist
import it.fast4x.spotifymeta.SpotifyTrackMeta
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
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
    /** Unmatched tracks retained as metadata; these are not playable imports. */
    val pending: Int = 0,
    val total: Int = 0,
    /** true se da Spotify e' stata letta solo una parte della playlist */
    val partial: Boolean = false,
    val error: String? = null
) {
    val isSuccess: Boolean get() = error == null && (imported > 0 || pending > 0)
}

/**
 * Importa una playlist di Spotify dentro RiPlay.
 *
 * Viene letta la playlist da Spotify, viene cercato il brano corrispondente su YouTube Music
 * per ogni traccia e il risultato viene aggiunto a una nuova playlist. Le tracce senza
 * corrispondenza conservano i metadati per personalizzare i consigli, senza risultare ascoltate.
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
        currentCoroutineContext().ensureActive()
        if (playlist == null) return@withContext SpotifyImportResult(
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
        val playlistId = try {
            Database.insert(newPlaylist)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            error.printStackTrace()
            return@withContext SpotifyImportResult(
                playlistName = playlistName,
                total = playlist.tracks.size,
                error = appContext().getString(R.string.spotify_import_recommendations_save_error)
            )
        }

        var imported = 0
        var skipped = 0
        var pending = 0

        playlist.tracks.forEachIndexed { index, track ->
            currentCoroutineContext().ensureActive()
            val match = try {
                findMatchingSong(track)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                error.printStackTrace()
                null
            }
            currentCoroutineContext().ensureActive()

            var playableSaved = false
            var pendingSaved = false
            try {
                if (match != null) {
                    playableSaved = saveMatchedSong(match, playlistId, index)
                } else {
                    pendingSaved = savePendingSong(track, playlist.thumbnailUrl, playlistId, index)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                error.printStackTrace()
            }
            if (playableSaved) imported++ else skipped++
            if (pendingSaved) pending++

            onProgress(
                SpotifyImportProgress(
                    current = index + 1,
                    total = playlist.tracks.size,
                    trackName = track.name
                )
            )
        }

        // Remove only an actually empty import, retaining useful pending metadata.
        if (imported == 0 && pending == 0) {
            runCatching { Database.delete(newPlaylist.copy(id = playlistId)) }
            return@withContext SpotifyImportResult(
                playlistName = playlistName,
                imported = 0,
                skipped = skipped,
                total = playlist.tracks.size,
                error = appContext().getString(R.string.spotify_import_recommendations_save_error)
            )
        }

        SpotifyImportResult(
            playlistName = playlistName,
            playlistId = playlistId,
            imported = imported,
            skipped = skipped,
            pending = pending,
            total = playlist.tracks.size,
            partial = !playlist.isComplete
        )
    }

    /** salva il brano trovato su YouTube Music e lo aggiunge alla playlist */
    private suspend fun saveMatchedSong(
        match: MatchedTrack,
        playlistId: Long,
        position: Int
    ): Boolean {
        val candidate = match.candidate
        val mediaItem = candidate.asMediaItem

        return Database.getInstance.withTransaction {
            Database.insert(mediaItem)
            Database.insert(
                SongPlaylistMap(
                    songId = mediaItem.mediaId,
                    playlistId = playlistId,
                    position = position
                ).default()
            ) != -1L
        }
    }

    private suspend fun savePendingSong(
        track: SpotifyTrackMeta,
        playlistThumbnail: String?,
        playlistId: Long,
        position: Int
    ): Boolean {
        val song = track.asPendingSong(playlistThumbnail) ?: return false
        return Database.getInstance.withTransaction {
            // IGNORE preserves an existing placeholder's history/favorite state and metadata.
            Database.insert(song)
            Database.insert(
                SongPlaylistMap(songId = song.id, playlistId = playlistId, position = position).default()
            ) != -1L
        }
    }
}

/** Pure metadata mapping; no synthetic listen or favorite is created. */
internal fun SpotifyTrackMeta.asPendingSong(playlistThumbnail: String? = null): Song? {
    val trackId = id.substringAfterLast(':').trim().takeIf { it.isNotEmpty() } ?: return null
    val title = name?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    return Song(
        id = "spotify:track:$trackId",
        title = title,
        artistsText = artist,
        durationText = durationMs?.takeIf { it > 0 }?.let(::formatAsDuration),
        thumbnailUrl = thumbnailUrl ?: playlistThumbnail
    )
}

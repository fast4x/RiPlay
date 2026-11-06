package it.fast4x.riplay.utils

import it.fast4x.riplay.data.update
import it.fast4x.riplay.data.insert
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.data.models.Playlist
import it.fast4x.riplay.data.models.SongPlaylistMap
import it.fast4x.spotifymeta.SpotifyPlaylist
import it.fast4x.spotifymeta.SpotifyPlaylistMeta
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

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
 *
 * Robustezza: nessun errore (rete, parsing, database) deve propagarsi all'app causando un
 * crash. Ogni operazione e' protetta singolarmente, ogni traccia che fallisce viene scartata
 * e l'esito reale viene sempre riportato al chiamante tramite SpotifyImportResult.
 * La cancellazione della coroutine (es. composable rimosso) e' sempre rispettata.
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
        try {
            doImport(url, onProgress)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            // rete di sicurezza: qualunque errore imprevisto non deve crashare l'app,
            // il chiamante riceve comunque un esito strutturato
            Timber.e(e, "SpotifyPlaylistImporter: unexpected error during import")
            SpotifyImportResult(
                error = "Importazione non riuscita a causa di un errore imprevisto"
            )
        }
    }

    /**
     * Corpo effettivo dell'importazione, protetto operazione per operazione.
     */
    private suspend fun doImport(
        url: String,
        onProgress: (SpotifyImportProgress) -> Unit
    ): SpotifyImportResult {

        // 1. lettura della playlist da Spotify: protetta (problemi di rete o di parsing)
        val playlist = try {
            SpotifyPlaylist.fetch(url)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "SpotifyPlaylistImporter: unable to fetch playlist for url $url")
            null
        }

        if (playlist == null) {
            return SpotifyImportResult(
                error = "Playlist non trovata o non accessibile"
            )
        }

        if (playlist.tracks.isEmpty()) {
            return SpotifyImportResult(
                playlistName = playlist.name,
                total = 0,
                error = "La playlist non contiene brani"
            )
        }

        val playlistName = playlist.name?.takeIf { it.isNotBlank() } ?: "Spotify Playlist"
        val newPlaylist = Playlist(name = playlistName)

        // 2. creazione della playlist nel database: protetta.
        //    Se l'inserimento fallisce non si continua: senza un id valido non ha
        //    senso aggiungere brani a una playlist che non esiste.
        val playlistId = try {
            Database.insert(newPlaylist)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "SpotifyPlaylistImporter: unable to create the playlist in the db")
            0L
        }

        if (playlistId <= 0L) {
            return SpotifyImportResult(
                playlistName = playlistName,
                total = playlist.tracks.size,
                error = "Errore del database durante la creazione della playlist"
            )
        }

        var imported = 0
        var skipped = 0

        // 3. ogni traccia e' protetta: un errore durante la ricerca (rete) o il
        //    salvataggio (database) scarta la singola traccia senza interrompere
        //    ne' le altre ne' l'importazione.
        playlist.tracks.forEachIndexed { index, track ->
            try {
                val match = try {
                    findMatchingSong(track)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Timber.e(e, "SpotifyPlaylistImporter: match lookup failed for ${track.name}")
                    null
                }

                if (match == null) {
                    skipped++
                } else {
                    // l'esito di saveMatchedSong riflette il reale esito della scrittura
                    if (saveMatchedSong(match, playlistId, index)) {
                        imported++
                    } else {
                        skipped++
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "SpotifyPlaylistImporter: track ${index + 1} skipped due to an error")
                skipped++
            }

            // 4. aggiornamento del progresso: protetto, un errore nella ui
            //    non deve interrompere l'importazione
            runCatching {
                onProgress(
                    SpotifyImportProgress(
                        current = index + 1,
                        total = playlist.tracks.size,
                        trackName = track.name
                    )
                )
            }.onFailure {
                Timber.w(it, "SpotifyPlaylistImporter: progress callback failed")
            }
        }

        // se non e' stato trovato nessun brano la playlist vuota viene rimossa
        if (imported == 0) {
            runCatching { Database.delete(newPlaylist.copy(id = playlistId)) }
                .onFailure {
                    Timber.w(it, "SpotifyPlaylistImporter: unable to remove the empty playlist")
                }
            return SpotifyImportResult(
                playlistName = playlistName,
                imported = 0,
                skipped = skipped,
                total = playlist.tracks.size,
                error = "Nessun brano della playlist e' stato trovato su YouTube Music"
            )
        }

        return SpotifyImportResult(
            playlistName = playlistName,
            playlistId = playlistId,
            imported = imported,
            skipped = skipped,
            total = playlist.tracks.size,
            partial = !playlist.isComplete
        )
    }

    /**
     * Salva il brano trovato su YouTube Music e lo aggiunge alla playlist.
     *
     * Le scritture sul database (prima causa di crash) non usano piu' una transazione
     * asincrona: ogni operazione e' eseguita direttamente sul dispatcher IO e protetta
     * singolarmente, cosi' che nessuna eccezione possa sfuggire al chiamante e l'esito
     * riportato rifletta il reale esito della scrittura.
     *
     * @return true se il brano e' stato salvato e collegato alla playlist,
     *         false se il brano va considerato scartato
     */
    private suspend fun saveMatchedSong(
        match: MatchedTrack,
        playlistId: Long,
        position: Int
    ): Boolean {
        val candidate = match.candidate

        // conversione protetta: un candidate incompleto non deve crashare l'app
        val mediaItem = try {
            candidate.asMediaItem
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "SpotifyPlaylistImporter: unable to build MediaItem from the match candidate")
            return false
        }

        // un mediaId vuoto renderebbe il salvataggio inutile e potenzialmente dannoso
        if (mediaItem.mediaId.isBlank()) {
            Timber.e("SpotifyPlaylistImporter: matched song has an empty mediaId, skipped")
            return false
        }

        // salvataggio del brano: protetto
        try {
            Database.insert(mediaItem)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "SpotifyPlaylistImporter: db insert failed for ${mediaItem.mediaId}")
            return false
        }

        // collegamento alla playlist: protetto
        try {
            Database.insert(
                SongPlaylistMap(
                    songId = mediaItem.mediaId,
                    playlistId = playlistId,
                    position = position
                ).default()
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "SpotifyPlaylistImporter: db link to playlist failed for ${mediaItem.mediaId}")
            return false
        }

        return true
    }
}

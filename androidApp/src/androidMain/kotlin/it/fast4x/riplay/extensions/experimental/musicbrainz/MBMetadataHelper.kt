package it.fast4x.riplay.extensions.experimental.musicbrainz

import it.fast4x.riplay.data.Database
import it.fast4x.riplay.data.models.Artist
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber

class MBMetadataHelper(
    private val mbClient: MusicBrainz
) {
    val coroutineScope = CoroutineScope(Dispatchers.IO)

    /**
     * Chiamato quando l'utente visualizza la pagina dell'artista
     */
    suspend fun onArtistViewed(artistId: String) {
        val artist = Database.artist(artistId).first() ?: return

        // Se è null, non abbiamo ancora cercato. Se è emptyList, abbiamo già cercato ma non c'erano.
        if ((artist.genres != null && artist.artistType != null && artist.countryCode != null
                    && artist.beginYear != null && artist.links != null
                    && artist.wikipediaBio != null && artist.wikipediaUrl != null
                    && artist.rating != null)
            || artist.name == null) return

        try {
            var metadata = mbClient.fetchArtistMetadata(artist.name)
            Timber.d("MBMetadataHelper onArtistViewed metadata fetched $metadata")

            // Se l'artista non ha un url di wikipedia provo a cercarlo direttamente da wikipedia
            if (metadata.wikipediaUrl == null) {
                val searchArtistTerm = if (artist.artistType == "Group") {
                    "${artist.name} band" // Cerca "Nirvana band"
                } else if (artist.artistType == "Person") {
                    "${artist.name} singer" // Cerca "Adele singer"
                } else {
                    artist.name
                }
                val wikipediaResult = mbClient.fetchWikipediaExtractByArtist(searchArtistTerm)
                metadata = metadata.copy(wikipediaBio = wikipediaResult?.info, wikipediaUrl = wikipediaResult?.url)
                Timber.d("MBMetadataHelper onArtistViewed wikipediaBio fetched $metadata")
            }

            // Se l'API ritorna vuoto, salviamo emptyList per non ripetere la chiamata in futuro
            coroutineScope.launch {
                Database.update(
                    artist.copy(
                        genres = metadata.genres.ifEmpty { emptyList() },
                        artistType = metadata.artistType ?: artist.artistType,
                        countryCode = metadata.countryCode ?: artist.countryCode,
                        beginYear = metadata.beginYear ?: artist.beginYear,
                        tags = metadata.topTags.ifEmpty { emptyList() },
                        rating = metadata.ratingValue ?: artist.rating,
                        ratingVotes = metadata.ratingVotes ?: artist.ratingVotes,
                        wikipediaUrl = metadata.wikipediaUrl ?: artist.wikipediaUrl,
                        disambiguation = metadata.disambiguation ?: artist.disambiguation,
                        wikipediaBio = metadata.wikipediaBio ?: artist.wikipediaBio,
                        links = metadata.links ?: artist.links
                    )
                )
            }
            Timber.d("MBMetadataHelper onArtistViewed metadata updated $metadata")
        } catch (e: Exception) {
            // Errore di rete: non facciamo nulla, genres rimarrà null e riproverà la prossima volta
            Timber.e("MBMetadataHelper onArtistViewed error ${e.stackTraceToString()}")
        }
    }

    /**
     * Chiamato quando l'utente visualizza la pagina dell'album
     */
    suspend fun onAlbumViewed(albumId: String) {
        val album = Database.album(albumId).first() ?: return
        if ((album.genres != null && album.albumType != null && album.originalYear != null
                    && album.links != null && album.wikipediaInfo != null && album.tags != null
                    && album.rating != null) || album.title == null || album.authorsText == null) return

        val artist = Database.artistByName(album.authorsText).first() ?: return

        try {
            // Proviamo a prendere i generi specifici dell'album
            var metadata  = mbClient.fetchAlbumMetadata(album.title, artist.name ?: "")

            Timber.d("MBMetadataHelper onAlbumViewed metadata fetched $metadata")

            val finalGenres = metadata.genres.ifEmpty {
                getOrFetchArtistGenres(artist).ifEmpty { emptyList() }
            }

            // Se l'artista non ha un url di wikipedia provo a cercarlo direttamente da wikipedia
            if (metadata.wikipediaUrl == null) {
                val searchAlbumTerm = "${album.title} ${album.authorsText} album"
                val wikipediaResult = mbClient.fetchWikipediaExtractByArtist(searchAlbumTerm)
                metadata = metadata.copy(wikipediaInfo = wikipediaResult?.info, wikipediaUrl = wikipediaResult?.url)
                Timber.d("MBMetadataHelper onAlbumViewed wikipediaInfo fetched $metadata")
            }

            // L'album ha generi propri, usiamo quelli
            coroutineScope.launch {
                Database.update(
                    album.copy(
                        genres = finalGenres,
                        albumType = metadata.albumType ?: album.albumType,
                        originalYear = metadata.originalYear ?: album.originalYear,
                        tags = metadata.topTags ?: album.tags,
                        rating = metadata.ratingValue ?: album.rating,
                        ratingVotes = metadata.ratingVotes ?: album.ratingVotes,
                        wikipediaUrl = metadata.wikipediaUrl ?: album.wikipediaUrl,
                        wikipediaInfo = metadata.wikipediaInfo ?: album.wikipediaInfo,
                        links = metadata.links ?: album.links
                    )
                )
            }
            Timber.d("MBMetadataHelper onAlbumViewed genres updated $finalGenres")

        } catch (e: Exception) {
            // Errore di rete
            Timber.e("MBMetadataHelper onAlbumViewed error ${e.stackTraceToString()}")
        }
    }

    /**
     * Chiamato quando l'utente ascolta una canzone
     */
    suspend fun onSongPlayed(songId: String) {
        Timber.d("MBMetadataHelper onSongPlayed $songId")
        val song = Database.song(songId).first() ?: return
        if (song.genres != null || song.artistsText == null) return

        val artist = Database.artistByName(song.artistsText).first() ?: return
        Timber.d("MBMetadataHelper onSongPlayed artist $artist")

        val artistGenres = getOrFetchArtistGenres(artist)
        Timber.d("MBMetadataHelper onSongPlayed genres fetched $artistGenres")

        // La canzone eredita i generi dell'artista
        coroutineScope.launch {
            Database.upsert(song.copy(genres = artistGenres.ifEmpty { emptyList() }))
        }
        Timber.d("MBMetadataHelper onSongPlayed genres updated from artistGenres $artistGenres")
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
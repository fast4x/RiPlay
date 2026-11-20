package it.fast4x.riplay.extensions.musicbrainz

import it.fast4x.riplay.data.Database
import it.fast4x.riplay.data.models.Artist
import it.fast4x.riplay.enums.AlbumNature
import it.fast4x.riplay.enums.ArtistNature
import it.fast4x.riplay.extensions.experimental.recommendationstrategy.classifiers.AlbumClassifier
import it.fast4x.riplay.extensions.experimental.recommendationstrategy.classifiers.ArtistClassifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import timber.log.Timber

class MBMetadataHelper(
    private val mbClient: MusicBrainz
) {

    /**
     * Marker "tentato ma non disponibile": stringa vuota invece di null.
     *
     * FIX (Fase 1): prima la completezza richiedeva wikipediaBio != null ma
     * quando la pagina Wikipedia non esiste il campo restava null per sempre:
     * OGNI visita alla pagina artista ripeteva la sequenza MB + Wikipedia.
     * Con il marker la verifica di completezza passa e non si riprova più
     * (le UI filtrano comunque le bio vuote con isNotBlank()).
     */
    private val WIKI_NOT_AVAILABLE = ""

    /**
     * Chiamato quando l'utente visualizza la pagina dell'artista.
     * Il write è AWAITABLE (FIX Fase 1: prima era fire-and-forget e gareggiava
     * con getOrFetchArtistGenres, che rileggeva il DB prima della scrittura).
     */
    suspend fun onArtistViewed(artistId: String) = withContext(Dispatchers.IO) {
        val artist = Database.artist(artistId).firstOrNull() ?: return@withContext

        // Se è null, non abbiamo ancora cercato. Se è emptyList, abbiamo già cercato ma non c'erano.
        // Il marker WIKI_NOT_AVAILABLE ("") conta come "già cercato": wikipediaBio != null.
        if ((artist.genres != null && artist.artistType != null && artist.countryCode != null
                    && artist.beginYear != null && artist.links != null
                    && artist.wikipediaBio != null && artist.wikipediaUrl != null
                    && artist.rating != null)
            || artist.name == null) return@withContext

        val artistName = artist.name ?: return@withContext
        try {
            var metadata = mbClient.fetchArtistMetadata(artistName)
            Timber.d("MBMetadataHelper onArtistViewed metadata fetched $metadata")

            // Se l'artista non ha un url di wikipedia provo a cercarlo direttamente da wikipedia
            var wikiFound = false
            if (metadata.wikipediaUrl == null) {
                val searchArtistTerm = if (artist.artistType == "Group") {
                    "${artistName} band" // Cerca "Nirvana band"
                } else if (artist.artistType == "Person") {
                    "${artistName} singer" // Cerca "Adele singer"
                } else {
                    artistName
                }
                val wikipediaResult = mbClient.fetchWikipediaExtractByArtist(searchArtistTerm)
                wikiFound = wikipediaResult != null
                metadata = metadata.copy(wikipediaBio = wikipediaResult?.info, wikipediaUrl = wikipediaResult?.url)
                Timber.d("MBMetadataHelper onArtistViewed wikipediaBio fetched $metadata")
            }

            val updated = artist.copy(
                genres = metadata.genres.ifEmpty { emptyList() },
                artistType = metadata.artistType ?: artist.artistType,
                countryCode = metadata.countryCode ?: artist.countryCode,
                beginYear = metadata.beginYear ?: artist.beginYear,
                tags = metadata.topTags.ifEmpty { emptyList() },
                rating = metadata.ratingValue ?: artist.rating,
                ratingVotes = metadata.ratingVotes ?: artist.ratingVotes,
                wikipediaUrl = metadata.wikipediaUrl ?: artist.wikipediaUrl,
                disambiguation = metadata.disambiguation ?: artist.disambiguation,
                // FIX (Fase 1): su pagina assente si persiste il marker "" invece
                // di lasciare null (che avrebbe riprovato all'infinito).
                wikipediaBio = metadata.wikipediaBio ?: if (wikiFound) artist.wikipediaBio else WIKI_NOT_AVAILABLE,
                links = metadata.links ?: artist.links,
                mbId = metadata.mbId ?: artist.mbId,
                // Ponte ZoundHub (Fase 2): Spotify ID + provenienza del match MB
                spotifyId = metadata.spotifyId ?: artist.spotifyId,
                mbMatchScore = metadata.matchScore ?: artist.mbMatchScore,
                mbMatchedAt = if (metadata.mbId != null) System.currentTimeMillis() else artist.mbMatchedAt
            )

            // FIX (Fase 1): la natura ora è ricalcolata ANCHE qui (prima restava
            // UNKNOWN fino al prossimo NatureBackfiller generale), classificando
            // l'oggetto GIA' arricchito coi nuovi generi/tag.
            val finalArtist = if (updated.nature == ArtistNature.UNKNOWN) {
                updated.copy(nature = ArtistClassifier.classify(updated))
            } else updated

            Database.update(finalArtist)
            Timber.d("MBMetadataHelper onArtistViewed metadata updated ${finalArtist.name} nature=${finalArtist.nature}")
        } catch (e: Exception) {
            // Errore di rete: non facciamo nulla, genres rimarrà null e riproverà la prossima volta
            Timber.e("MBMetadataHelper onArtistViewed error ${e.stackTraceToString()}")
        }
    }

    /**
     * Chiamato quando l'utente visualizza la pagina dell'album.
     * FIX (Fase 1): ora persiste ANCHE il mbId del release-group trovato
     * (prima andava perso: gli album restavano "senza match" per sempre).
     */
    suspend fun onAlbumViewed(albumId: String) = withContext(Dispatchers.IO) {
        val album = Database.album(albumId).firstOrNull() ?: return@withContext
        if ((album.genres != null && album.albumType != null && album.originalYear != null
                    && album.links != null && album.wikipediaInfo != null && album.tags != null
                    && album.rating != null) || album.title == null || album.authorsText == null) return@withContext

        val albumAuthorsText = album.authorsText ?: return@withContext
        val albumTitle = album.title ?: return@withContext
        val artist = Database.artistByName(albumAuthorsText).firstOrNull() ?: return@withContext

        try {
            // Proviamo a prendere i generi specifici dell'album
            var metadata = mbClient.fetchAlbumMetadata(albumTitle, artist.name ?: "")

            Timber.d("MBMetadataHelper onAlbumViewed metadata fetched $metadata")

            val finalGenres = metadata.genres.ifEmpty {
                getOrFetchArtistGenres(artist).ifEmpty { emptyList() }
            }

            // Se l'artista non ha un url di wikipedia provo a cercarlo direttamente da wikipedia
            var wikiFound = false
            if (metadata.wikipediaUrl == null) {
                val searchAlbumTerm = "${album.title} ${album.authorsText} album"
                val wikipediaResult = mbClient.fetchWikipediaExtractByArtist(searchAlbumTerm)
                wikiFound = wikipediaResult != null
                metadata = metadata.copy(wikipediaInfo = wikipediaResult?.info, wikipediaUrl = wikipediaResult?.url)
                Timber.d("MBMetadataHelper onAlbumViewed wikipediaInfo fetched $metadata")
            }

            // L'album ha generi propri, usiamo quelli
            val updated = album.copy(
                genres = finalGenres,
                albumType = metadata.albumType ?: album.albumType,
                originalYear = metadata.originalYear ?: album.originalYear,
                tags = metadata.topTags ?: album.tags,
                rating = metadata.ratingValue ?: album.rating,
                ratingVotes = metadata.ratingVotes ?: album.ratingVotes,
                wikipediaUrl = metadata.wikipediaUrl ?: album.wikipediaUrl,
                wikipediaInfo = metadata.wikipediaInfo ?: if (wikiFound) album.wikipediaInfo else WIKI_NOT_AVAILABLE,
                links = metadata.links ?: album.links,
                // FIX (Fase 1): il MBID trovato ora viene salvato
                mbId = metadata.mbId ?: album.mbId,
                mbMatchedAt = if (metadata.mbId != null) System.currentTimeMillis() else album.mbMatchedAt
            )

            val finalAlbum = if (updated.nature == AlbumNature.UNKNOWN) {
                updated.copy(nature = AlbumClassifier.classify(updated))
            } else updated

            Database.update(finalAlbum)
            Timber.d("MBMetadataHelper onAlbumViewed updated ${finalAlbum.title} mbId=${finalAlbum.mbId} nature=${finalAlbum.nature}")

        } catch (e: Exception) {
            // Errore di rete
            Timber.e("MBMetadataHelper onAlbumViewed error ${e.stackTraceToString()}")
        }
    }

    /**
     * Chiamato quando l'utente ascolta una canzone
     */
    suspend fun onSongPlayed(songId: String) = withContext(Dispatchers.IO) {
        Timber.d("MBMetadataHelper onSongPlayed $songId")
        val song = Database.song(songId).firstOrNull() ?: return@withContext
        if (song.genres != null || song.artistsText == null) return@withContext

        val songArtistsText = song.artistsText ?: return@withContext
        val artist = Database.artistByName(songArtistsText).firstOrNull() ?: return@withContext
        Timber.d("MBMetadataHelper onSongPlayed artist $artist")

        val artistGenres = getOrFetchArtistGenres(artist)
        Timber.d("MBMetadataHelper onSongPlayed genres fetched $artistGenres")

        // La canzone eredita i generi dell'artista
        Database.upsert(song.copy(genres = artistGenres.ifEmpty { emptyList() }))
        Timber.d("MBMetadataHelper onSongPlayed genres updated from artistGenres $artistGenres")
    }

    /**
     * Helper: Ritorna i generi dell'artista se già in DB, altrimenti li fetcha da MB
     */
    private suspend fun getOrFetchArtistGenres(artist: Artist?): List<String> {
        if (artist == null) return emptyList()
        // Se abbiamo già i generi dell'artista (anche lista vuota), li usiamo
        artist.genres?.let { return it }

        // Altrimenti facciamo la chiamata per l'artista
        onArtistViewed(artist.id)

        // Rileggiamo l'artista dal DB ora che è aggiornato
        return Database.artist(artist.id).firstOrNull()?.genres ?: emptyList()
    }

    /**
     * Database.artist()/song()/album()/artistByName() espongono Flow<List<T>>:
     * qui serve il primo elemento o null.
     */
    private suspend fun <T> kotlinx.coroutines.flow.Flow<List<T>>.firstOrNullSafe(): T? =
        firstOrNull()?.firstOrNull()

}

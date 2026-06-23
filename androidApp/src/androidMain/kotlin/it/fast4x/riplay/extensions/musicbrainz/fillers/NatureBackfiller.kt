package it.fast4x.riplay.extensions.musicbrainz.fillers

import android.util.Log
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.enums.AlbumNature
import it.fast4x.riplay.enums.ArtistNature
import it.fast4x.riplay.extensions.experimental.recommendationstrategy.classifiers.AlbumClassifier
import it.fast4x.riplay.extensions.experimental.recommendationstrategy.classifiers.ArtistClassifier
import it.fast4x.riplay.extensions.musicbrainz.repository.AlbumRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber


class NatureBackfiller() {

    private val artistDao = Database.artistDao()
    private val albumDao = Database.albumDao()
    private val mbAlbumDao = Database.mbAlbumDao()

    suspend fun backfillAll(): BackfillResult = withContext(Dispatchers.IO) {
        Timber.tag("NatureBackfill").i("=== START ===")

        // 1. Artisti
        val artists = artistDao.getAllWithName()
        Timber.tag("NatureBackfill").i("Artists to classify: ${artists.size}")
        var artistClassified = 0
        val artistStats = mutableMapOf<ArtistNature, Int>()

        for (artist in artists) {
            val nature = ArtistClassifier.classify(artist)
            if (nature != ArtistNature.UNKNOWN) {
                artistDao.upsert(artist.copy(nature = nature))
                artistClassified++
                artistStats[nature] = (artistStats[nature] ?: 0) + 1
            }
        }
        Timber.tag("NatureBackfill").i("Artists classified: $artistClassified")
        artistStats.forEach { (nature, count) ->
            Timber.tag("NatureBackfill").i("  $nature: $count")
        }

        // 2. Album
        val albums = albumDao.getAllAlbums()
        Timber.tag("NatureBackfill").i("Albums to classify: ${albums.size}")
        var albumClassified = 0
        val albumStats = mutableMapOf<AlbumNature, Int>()

        for (album in albums) {
            val nature = AlbumClassifier.classify(album)
            if (nature != AlbumNature.UNKNOWN) {
                albumDao.upsert(album.copy(nature = nature))
                albumClassified++
                albumStats[nature] = (albumStats[nature] ?: 0) + 1
            }
        }
        Timber.tag("NatureBackfill").i("Albums classified: $albumClassified")
        albumStats.forEach { (nature, count) ->
            Timber.tag("NatureBackfill").i("  $nature: $count")
        }

        // 3. MBAlbum
        val mbAlbums = mbAlbumDao.getAll()
        Timber.tag("NatureBackfill").i("MBAlbums to classify: ${mbAlbums.size}")
        var mbClassified = 0
        val mbStats = mutableMapOf<AlbumNature, Int>()

        for (mb in mbAlbums) {
            val nature = AlbumClassifier.classify(mb)
            if (nature != AlbumNature.UNKNOWN) {
                mbAlbumDao.upsert(mb.copy(nature = nature))
                mbClassified++
                mbStats[nature] = (mbStats[nature] ?: 0) + 1
            }
        }
        Timber.tag("NatureBackfill").i("MBAlbums classified: $mbClassified")
        mbStats.forEach { (nature, count) ->
            Timber.tag("NatureBackfill").i("  $nature: $count")
        }

        Timber.tag("NatureBackfill").i("=== DONE ===")
        BackfillResult(artistClassified, albumClassified, mbClassified)
    }

}

data class BackfillResult(
    val artistsClassified: Int,
    val albumsClassified: Int,
    val mbAlbumsClassified: Int
)

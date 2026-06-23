package it.fast4x.riplay.extensions.musicbrainz.repository

import it.fast4x.riplay.data.Database
import it.fast4x.riplay.data.models.Album
import it.fast4x.riplay.enums.AlbumNature
import it.fast4x.riplay.extensions.experimental.recommendationstrategy.classifiers.AlbumClassifier
import it.fast4x.riplay.extensions.musicbrainz.utils.IdentityUtils
import java.util.UUID

class AlbumRepository() {

    val albumDao = Database.albumDao()

    suspend fun upsertSmart(album: Album): String {

        // 1. Search by mbId
        if (album.mbId != null) {
            albumDao.getByMbId(album.mbId)?.let { existing ->
                val merged = mergeAlbums(existing, album)
                val classifiedAlbum = if (album.nature == AlbumNature.UNKNOWN) {
                    merged.copy(nature = AlbumClassifier.classify(merged))
                } else {
                    merged
                }
                albumDao.upsert(classifiedAlbum)
                return existing.id
            }
        }

        // 2. Search by youtubeAlbumId
        if (album.youtubeAlbumId != null) {
            albumDao.getByYoutubeAlbumId(album.youtubeAlbumId)?.let { existing ->
                val merged = mergeAlbums(existing, album)
                val classifiedAlbum = if (album.nature == AlbumNature.UNKNOWN) {
                    merged.copy(nature = AlbumClassifier.classify(merged))
                } else {
                    merged
                }
                albumDao.upsert(classifiedAlbum)
                return existing.id
            }
        }

        // 3. Search by title + artist (KEY per album)
        if (!album.title.isNullOrBlank() && !album.authorsText.isNullOrBlank()) {
            // Match normalizzato in-memory
            val normalizedTitle = IdentityUtils.normalizeAlbumTitle(album.title)
            val normalizedArtist = IdentityUtils.normalizeArtistName(album.authorsText)

            // Query grezza per ridurre il set, poi filtriamo in-memory
            val candidates = albumDao.getAllAlbums()  // aggiungi questa query se manca
            candidates.firstOrNull { existing ->
                IdentityUtils.normalizeAlbumTitle(existing.title) == normalizedTitle &&
                        IdentityUtils.normalizeArtistName(existing.authorsText) == normalizedArtist
            }?.let { existing ->
                val merged = mergeAlbums(existing, album)
                val classifiedAlbum = if (album.nature == AlbumNature.UNKNOWN) {
                    merged.copy(nature = AlbumClassifier.classify(merged))
                } else {
                    merged
                }
                albumDao.upsert(classifiedAlbum)
                return existing.id
            }
        }

        // 4. Nessun match → crea nuovo
        val newId = album.id.ifBlank {
            when {
                album.mbId != null -> "mb-${album.mbId}"
                album.youtubeAlbumId != null -> album.youtubeAlbumId
                else -> "album_${UUID.randomUUID()}"
            }
        }
        val classifiedAlbum = if (album.nature == AlbumNature.UNKNOWN) {
            album.copy(nature = AlbumClassifier.classify(album))
        } else {
            album
        }
        albumDao.upsert(classifiedAlbum.copy(id = newId))
        return newId
    }

    private fun mergeAlbums(existing: Album, incoming: Album): Album {
        return existing.copy(
            mbId = incoming.mbId ?: existing.mbId,
            youtubeAlbumId = incoming.youtubeAlbumId ?: existing.youtubeAlbumId,
            isYoutubeAlbum = incoming.isYoutubeAlbum || existing.isYoutubeAlbum,
            title = incoming.title ?: existing.title,
            authorsText = incoming.authorsText ?: existing.authorsText,
            thumbnailUrl = incoming.thumbnailUrl ?: existing.thumbnailUrl,
            year = incoming.year ?: existing.year,
            shareUrl = incoming.shareUrl ?: existing.shareUrl,
            timestamp = incoming.timestamp ?: existing.timestamp,
            bookmarkedAt = incoming.bookmarkedAt ?: existing.bookmarkedAt,
            genres = incoming.genres ?: existing.genres,
            originalYear = incoming.originalYear ?: existing.originalYear,
            albumType = incoming.albumType ?: existing.albumType,
            tags = incoming.tags ?: existing.tags,
            rating = incoming.rating ?: existing.rating,
            ratingVotes = incoming.ratingVotes ?: existing.ratingVotes,
            wikipediaUrl = incoming.wikipediaUrl ?: existing.wikipediaUrl,
            wikipediaInfo = incoming.wikipediaInfo ?: existing.wikipediaInfo,
            links = incoming.links ?: existing.links,
            nature = if (incoming.nature != AlbumNature.UNKNOWN) incoming.nature else existing.nature
        )
    }
}
package it.fast4x.riplay.extensions.experimental.recommendationstrategy.classifiers

import it.fast4x.riplay.data.models.Album
import it.fast4x.riplay.data.models.MBAlbum
import it.fast4x.riplay.enums.AlbumNature

object AlbumClassifier {

    fun classify(album: Album): AlbumNature {
        val titleLower = album.title?.lowercase()?.trim() ?: ""
        val authorsLower = album.authorsText?.lowercase()?.trim() ?: ""
        val allKeywords = (album.genres.orEmpty() + album.tags.orEmpty())
            .map { it.lowercase().trim() }
            .toSet()

        if (allKeywords.any { it in ClassificationKeywords.AI }) {
            return AlbumNature.AI_GENERATED
        }

        when (album.albumType?.lowercase()?.trim()) {
            "single" -> return AlbumNature.SINGLE
            "ep" -> return AlbumNature.EP
            "broadcast" -> return AlbumNature.SOUNDTRACK
        }

        if (allKeywords.any { it == "live" } || titleLower.contains("(live")) {
            return AlbumNature.LIVE
        }

        if (allKeywords.any { it in ClassificationKeywords.SOUNDTRACK } ||
            titleLower.contains("soundtrack") || titleLower.contains("ost")) {
            return AlbumNature.SOUNDTRACK
        }

        if (allKeywords.any { it in ClassificationKeywords.COMPILATION_ALBUM } ||
            authorsLower.contains("various artists") ||
            titleLower.contains("greatest hits") ||
            titleLower.contains("best of")) {
            return AlbumNature.COMPILATION
        }

        return if (album.albumType?.equals("album", ignoreCase = true) == true ||
            allKeywords.isNotEmpty()) {
            AlbumNature.STUDIO_ALBUM
        } else {
            AlbumNature.UNKNOWN
        }
    }

    fun classify(mbAlbum: MBAlbum): AlbumNature {
        val allKeywords = (mbAlbum.genres.orEmpty() + mbAlbum.tags.orEmpty())
            .map { it.lowercase().trim() }
            .toSet()

        if (allKeywords.any { it in ClassificationKeywords.AI }) return AlbumNature.AI_GENERATED

        when (mbAlbum.primaryType?.lowercase()?.trim()) {
            "single" -> return AlbumNature.SINGLE
            "ep" -> return AlbumNature.EP
            "broadcast" -> return AlbumNature.SOUNDTRACK
        }

        if (mbAlbum.secondaryTypes?.any {
                it.equals("Compilation", ignoreCase = true)
            } == true) return AlbumNature.COMPILATION

        if (mbAlbum.secondaryTypes?.any {
                it.equals("Live", ignoreCase = true)
            } == true) return AlbumNature.LIVE

        if (allKeywords.any { it in ClassificationKeywords.SOUNDTRACK }) return AlbumNature.SOUNDTRACK

        return AlbumNature.STUDIO_ALBUM
    }
}
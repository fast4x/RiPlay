package it.fast4x.riplay.extensions.experimental.recommendationstrategy.classifiers

import it.fast4x.riplay.data.models.Artist
import it.fast4x.riplay.enums.ArtistNature

object ArtistClassifier {

    fun classify(artist: Artist): ArtistNature {
        val name = artist.name?.lowercase()?.trim() ?: return ArtistNature.UNKNOWN
        val allKeywords = (artist.genres.orEmpty() + artist.tags.orEmpty())
            .map { it.lowercase().trim() }
            .toSet()

        // 1. AI — priorità alta
        if (allKeywords.any { it in ClassificationKeywords.AI }) {
            return ArtistNature.AI_GENERATED
        }

        // 2. Virtual (vocaloid, vtuber)
        if (allKeywords.any { it in ClassificationKeywords.VIRTUAL }) {
            return ArtistNature.VIRTUAL
        }

        // 3. Cover/tribute
        if (allKeywords.any { it in ClassificationKeywords.COVER }) {
            return ArtistNature.COVER_TRIBUTE
        }

        // 4. Compilation (per nome)
        if (ClassificationKeywords.COMPILATION_ARTIST_NAMES.any { name.contains(it) }) {
            return ArtistNature.COMPILATION
        }

        // Se è un artista reale (visitato dall'utente), è HUMAN
        return if (artist.youtubeChannelId != null
            || artist.mbId != null
            || artist.isYoutubeArtist
            || allKeywords.isNotEmpty()) {
            ArtistNature.HUMAN
        } else {
            ArtistNature.UNKNOWN
        }
    }
}
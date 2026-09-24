package it.fast4x.riplay.extensions.musicbrainz.fillers

import android.util.Log
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.data.models.Artist
import it.fast4x.riplay.data.models.SongArtistCrossRef
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class SongArtistBackfiller {

    /**
     * Popola SongArtistCrossRef parsando artistsText da Song esistenti.
     * Match case-insensitive con Artist.name.
     */
    suspend fun backfill(limit: Int = 1000): BackfillResult = withContext(Dispatchers.IO) {
        Timber.tag("Backfill").i("=== START SongArtistCrossRef backfill ===")

        // Prendi song con artistsText non null, senza cross-ref esistenti
        val songs = Database.getSongsWithArtistsText(limit)
        Timber.tag("Backfill").i("Songs to process: ${songs.size}")

        var totalRefs = 0
        var songsMatched = 0
        var songsUnmatched = 0
        val unmatchedSamples = mutableListOf<String>()

        // Cache locale degli artisti per evitare query ripetute
        val artistCache = mutableMapOf<String, Artist?>()

        for (song in songs) {
            // Skip se ha già cross-ref
            if (Database.songArtistCrossRefDao().countArtistsForSong(song.id) > 0) continue

            val artistNames = parseArtistNames(song.artistsText ?: continue)
            if (artistNames.isEmpty()) {
                songsUnmatched++
                continue
            }

            val refs = mutableListOf<SongArtistCrossRef>()
            var matched = 0

            artistNames.forEachIndexed { index, name ->
                val cleanedName = name.trim()
                if (cleanedName.isEmpty()) return@forEachIndexed

                val artist = artistCache.getOrPut(cleanedName.lowercase()) {
                    Database.findByNameIgnoreCase(cleanedName)
                }

                if (artist != null) {
                    refs.add(
                        SongArtistCrossRef(
                            songId = song.id,
                            artistId = artist.id,
                            role = if (index == 0) "main" else "feature",
                            order = index
                        )
                    )
                    matched++
                }
            }

            if (refs.isNotEmpty()) {
                Database.songArtistCrossRefDao().insertAll(refs)
                totalRefs += refs.size
                songsMatched++
            } else {
                songsUnmatched++
                if (unmatchedSamples.size < 5) {
                    unmatchedSamples.add("'${song.title}' by ${song.artistsText}")
                }
            }
        }

        Timber.tag("Backfill")
            .i("=== DONE: $totalRefs refs, $songsMatched songs matched, $songsUnmatched unmatched ===")
        if (unmatchedSamples.isNotEmpty()) {
            Timber.tag("Backfill").i("Unmatched samples:")
            unmatchedSamples.forEach { Log.i("Backfill", "  - $it") }
        }
        Timber.tag("Backfill")
            .i("Total cross-refs in DB: ${Database.songArtistCrossRefDao().count()}")
        Timber.tag("Backfill")
            .i("Songs with artist: ${Database.songArtistCrossRefDao().countSongsWithArtist()}")

        BackfillResult(totalRefs, songsMatched, songsUnmatched, unmatchedSamples)
    }

    /**
     * Parsa una stringa "artistsText" in una lista di nomi di artisti.
     * Gestisce separatori comuni: " & ", " feat. ", " featuring ", ", ", " x "
     */
    private fun parseArtistNames(artistsText: String): List<String> {
        if (artistsText.isBlank()) return emptyList()

        // Rimuovi parentesi contenti feat. (es. "Artist1 (feat. Artist2)")
        var cleaned = artistsText
            .replace(Regex("\\(\\s*feat\\.[^)]*\\)", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\(\\s*featuring\\s[^)]*\\)", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\(\\s*with\\s[^)]*\\)", RegexOption.IGNORE_CASE), "")
            .trim()

        // Split sui separatori
        val separators = listOf(
            Regex("\\s+feat\\.\\s+", RegexOption.IGNORE_CASE),
            Regex("\\s+featuring\\s+", RegexOption.IGNORE_CASE),
            Regex("\\s+&\\s+"),
            Regex("\\s+,\\s+"),
            Regex("\\s+,\\s*"),
            Regex("\\s+x\\s+", RegexOption.IGNORE_CASE),
            Regex("\\s+and\\s+", RegexOption.IGNORE_CASE)
        )

        var parts = listOf(cleaned)
        for (sep in separators) {
            val newParts = mutableListOf<String>()
            for (p in parts) {
                newParts.addAll(sep.split(p))
            }
            parts = newParts
        }

        return parts
            .map { it.trim() }
            .filter { it.isNotEmpty() && it.length > 1 }
            .distinct()
    }

    data class BackfillResult(
        val refsCreated: Int,
        val songsMatched: Int,
        val songsUnmatched: Int,
        val unmatchedSamples: List<String>
    )
}
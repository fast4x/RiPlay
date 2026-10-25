package it.fast4x.riplay.extensions.playlistrecommendations

import it.fast4x.riplay.data.models.Blacklist
import it.fast4x.riplay.data.models.PlaylistRecommendationSource
import it.fast4x.riplay.data.models.Song
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class PlaylistTasteAnalyzerTest {
    @Test
    fun playlistWithOneArtistProvidesRecommendationsWithoutListeningHistory() {
        val source = source(song(1, artist = "M83"))
        val profile = PlaylistTasteAnalyzer.analyze(listOf(source))

        assertEquals(0L, source.song.totalPlayTimeMs)
        assertEquals(1, profile.songCount)
        assertEquals(1, profile.playlistCount)
        assertTrue(profile.seeds.isNotEmpty())
        assertTrue(profile.topArtists.isNotEmpty())
        assertTrue(profile.artistWeights.values.any { it > 0 })
        assertEquals(
            listOf(id(2)),
            PlaylistTasteAnalyzer.rank(profile, listOf(candidate(song(2, artist = "M83"))))
                .map { it.song.id }
        )
    }

    @Test
    fun repeatedMembershipDoesNotIncreaseSongCountOrArtistAffinity() {
        val track = song(1, artist = "M83", genres = listOf("Synthpop"))
        val single = PlaylistTasteAnalyzer.analyze(listOf(source(track)))
        val repeated = PlaylistTasteAnalyzer.analyze(
            listOf(source(track), source(track, playlistId = 2), source(track, playlistId = 2))
        )

        assertEquals(1, repeated.songCount)
        assertEquals(2, repeated.playlistCount)
        assertEquals(single.artistWeights, repeated.artistWeights)
        assertEquals(single.genreWeights, repeated.genreWeights)
        assertEquals(single.seeds.size, repeated.seeds.size)
    }

    @Test
    fun smallPlaylistStillContributesASeedBesideAHugePlaylist() {
        val largePlaylist = (1..120).map { source(song(it, artist = "Large Playlist Artist")) }
        val smallPlaylist = (121..122).map { source(song(it, artist = "Small Playlist Artist"), playlistId = 2) }
        val profile = PlaylistTasteAnalyzer.analyze(largePlaylist + smallPlaylist, maxSeeds = 6)

        assertTrue(profile.seeds.size <= 6)
        assertTrue(profile.seeds.any { seed -> largePlaylist.any { it.song.id == seed.id } })
        assertTrue(profile.seeds.any { seed -> smallPlaylist.any { it.song.id == seed.id } })
    }

    @Test
    fun absentArtistMetadataDoesNotCrashOrProduceInvalidAffinities() {
        val track = song(1, artist = null, genres = null)
        val profile = PlaylistTasteAnalyzer.analyze(listOf(source(track)))

        assertTrue(profile.topArtists.none { it.isBlank() })
        assertTrue(profile.artistWeights.keys.none { it.isBlank() })
        assertTrue(profile.artistWeights.values.all { it.isFinite() })
        assertTrue(profile.genreWeights.values.all { it.isFinite() })
    }

    @Test
    fun unmatchedSpotifyImportStillProvidesTasteWithoutPlaybackOrMatching() {
        val pending = song(1, title = "Midnight City", artist = "M83").copy(id = "spotify:pending-track")
        val profile = PlaylistTasteAnalyzer.analyze(listOf(source(pending)))

        assertEquals(1, profile.songCount)
        assertTrue(profile.seeds.any { it.id == pending.id })
        assertTrue(profile.artistWeights.values.any { it > 0 })
        assertEquals(
            listOf(id(2)),
            PlaylistTasteAnalyzer.rank(profile, listOf(candidate(song(2, artist = "M83"), seedId = pending.id)))
                .map { it.song.id }
        )
    }

    @Test
    fun canonicalDuplicatesPreferTheMatchedPlayableSeed() {
        val playable = song(1, title = "Midnight City", artist = "M83")
        val pending = playable.copy(id = "spotify:pending-track")
        val profile = PlaylistTasteAnalyzer.analyze(listOf(source(pending), source(playable, playlistId = 2)))

        assertEquals(1, profile.songCount)
        assertEquals(listOf(playable.id), profile.seeds.map { it.id })
    }

    @Test
    fun playbackTimeAndMembershipOrderDoNotChangePlaylistTaste() {
        val sources = listOf(source(song(1)), source(song(2, artist = "Other Artist"), playlistId = 2))
        val original = PlaylistTasteAnalyzer.analyze(sources)
        val changed = PlaylistTasteAnalyzer.analyze(
            sources.reversed().map { it.copy(song = it.song.copy(totalPlayTimeMs = 9_000_000L)) }
        )

        assertEquals(original.fingerprint, changed.fingerprint)
        assertEquals(original.artistWeights, changed.artistWeights)
        assertEquals(original.genreWeights, changed.genreWeights)
    }

    @Test
    fun replacingAMemberWithTheSamePlaylistSizeChangesFingerprint() {
        val original = PlaylistTasteAnalyzer.analyze(listOf(source(song(1))))
        val replaced = PlaylistTasteAnalyzer.analyze(listOf(source(song(2))))

        assertEquals(original.songCount, replaced.songCount)
        assertEquals(original.playlistCount, replaced.playlistCount)
        assertNotEquals(original.fingerprint, replaced.fingerprint)
        assertFalse(id(1) in replaced.knownSongIds)
        assertTrue(id(2) in replaced.knownSongIds)
    }

    @Test
    fun artistMetadataArrivingAfterImportChangesTasteWithoutChangingMembership() {
        val pendingMetadata = source(song(1, artist = null, genres = null))
        val enriched = pendingMetadata.copy(song = pendingMetadata.song.copy(artistsText = "M83", genres = listOf("Synthpop")))
        val before = PlaylistTasteAnalyzer.analyze(listOf(pendingMetadata))
        val after = PlaylistTasteAnalyzer.analyze(listOf(enriched))

        assertEquals(before.knownSongIds, after.knownSongIds)
        assertNotEquals(before.fingerprint, after.fingerprint)
        assertTrue(after.artistWeights.isNotEmpty())
        assertTrue(after.genreWeights.isNotEmpty())
    }

    @Test
    fun deletingTheLastMemberClearsTasteAndChangesFingerprint() {
        val original = PlaylistTasteAnalyzer.analyze(listOf(source(song(1))))
        val empty = PlaylistTasteAnalyzer.analyze(emptyList())

        assertNotEquals(original.fingerprint, empty.fingerprint)
        assertEquals(0, empty.songCount)
        assertTrue(empty.seeds.isEmpty())
        assertTrue(empty.artistWeights.isEmpty())
        assertTrue(empty.genreWeights.isEmpty())
        assertTrue(empty.knownSongIds.isEmpty())
    }

    @Test
    fun activeSongAndVideoBlocksPreventTracksFromSeedingTheProfile() {
        for (type in listOf("Song", "Video")) {
            val profile = PlaylistTasteAnalyzer.analyze(
                sources = listOf(source(song(1))),
                blacklists = listOf(Blacklist(type = type, path = id(1)))
            )

            assertTrue(profile.seeds.isEmpty(), "$type block should exclude the seed")
        }
    }

    @Test
    fun blockedPlaylistDoesNotContributeTaste() {
        val profile = PlaylistTasteAnalyzer.analyze(
            sources = listOf(source(song(1), playlistId = 42)),
            blacklists = listOf(Blacklist(type = "Playlist", path = "42"))
        )

        assertTrue(profile.seeds.isEmpty())
        assertTrue(profile.artistWeights.isEmpty())
    }

    @Test
    fun disabledSongBlockAllowsPlaylistTasteAndCandidate() {
        val profile = PlaylistTasteAnalyzer.analyze(
            sources = listOf(source(song(1))),
            blacklists = listOf(
                Blacklist(type = "Song", path = id(1), enabled = 0),
                Blacklist(type = "Song", path = id(2), enabled = 0)
            )
        )

        assertTrue(profile.seeds.isNotEmpty())
        assertEquals(listOf(id(2)), PlaylistTasteAnalyzer.rank(profile, listOf(candidate(song(2)))).map { it.song.id })
    }

    @Test
    fun activeBlocksExcludeRelatedCandidatesByIdOrNormalizedArtistName() {
        val profile = PlaylistTasteAnalyzer.analyze(
            sources = listOf(source(song(1))),
            blacklists = listOf(
                Blacklist(type = "Artist", path = "blocked-artist", name = "Blocked Artist"),
                Blacklist(type = "Album", path = "blocked-album"),
                Blacklist(type = "Song", path = id(5)),
                Blacklist(type = "Video", path = id(6))
            )
        )
        val candidates = listOf(
            candidate(song(2), artistIds = setOf("blocked-artist")),
            candidate(song(3), albumId = "blocked-album"),
            candidate(song(4)),
            candidate(song(5)),
            candidate(song(6)),
            candidate(song(7, artist = "  BLOCKED ARTIST  "))
        )

        assertEquals(listOf(id(4)), PlaylistTasteAnalyzer.rank(profile, candidates).map { it.song.id })
    }

    @Test
    fun artistAndAlbumBlocksExcludePlaylistSeedsByTheirRelations() {
        val byArtist = PlaylistTasteAnalyzer.analyze(
            sources = listOf(source(song(1)).copy(artistIds = "blocked-artist")),
            blacklists = listOf(Blacklist(type = "Artist", path = "blocked-artist"))
        )
        val byAlbum = PlaylistTasteAnalyzer.analyze(
            sources = listOf(source(song(1)).copy(albumIds = "blocked-album")),
            blacklists = listOf(Blacklist(type = "Album", path = "blocked-album"))
        )

        assertTrue(byArtist.seeds.isEmpty())
        assertTrue(byAlbum.seeds.isEmpty())
    }

    @Test
    fun dislikesDoNotSeedTasteAndAreNeverRecommended() {
        val dislikedSeed = PlaylistTasteAnalyzer.analyze(listOf(source(song(1).copy(likedAt = -1L))))
        assertTrue(dislikedSeed.seeds.isEmpty())

        val profile = PlaylistTasteAnalyzer.analyze(listOf(source(song(1))), dislikedSongIds = setOf(id(2)))
        val candidates = listOf(candidate(song(2)), candidate(song(3).copy(likedAt = -1L)), candidate(song(4)))

        assertEquals(listOf(id(4)), PlaylistTasteAnalyzer.rank(profile, candidates).map { it.song.id })
    }

    @Test
    fun savedTrackAndItsCanonicalCopyWithAnotherIdAreExcluded() {
        val saved = song(1, title = "Midnight City", artist = "M83")
        val profile = PlaylistTasteAnalyzer.analyze(listOf(source(saved)))
        val candidates = listOf(
            candidate(saved),
            candidate(song(2, title = "  MIDNIGHT CITY  ", artist = "m83")),
            candidate(song(3, title = "Reunion", artist = "M83"))
        )

        assertEquals(listOf(id(3)), PlaylistTasteAnalyzer.rank(profile, candidates).map { it.song.id })
    }

    @Test
    fun candidateReturnedBySeveralSeedsAppearsOnce() {
        val profile = PlaylistTasteAnalyzer.analyze(listOf(source(song(1)), source(song(2))))
        val repeated = song(3)
        val ranked = PlaylistTasteAnalyzer.rank(
            profile,
            listOf(candidate(repeated), candidate(repeated).copy(seedId = id(2), sourceRank = 1), candidate(song(4)))
        )

        assertEquals(2, ranked.size)
        assertEquals(1, ranked.count { it.song.id == repeated.id })
    }

    @Test
    fun rankingHonorsTheRequestedResultLimit() {
        val profile = PlaylistTasteAnalyzer.analyze(listOf(source(song(1))))
        val candidates = (2..8).map { candidate(song(it, artist = "Artist $it")) }

        assertEquals(2, PlaylistTasteAnalyzer.rank(profile, candidates, limit = 2).size)
    }

    @Test
    fun rankingLimitsOneArtistAndKeepsOtherArtistsRepresented() {
        val profile = PlaylistTasteAnalyzer.analyze(
            listOf(source(song(1, artist = "Artist A")), source(song(2, artist = "Artist B")), source(song(3, artist = "Artist C")))
        )
        val candidates = (10..17).map { candidate(song(it, artist = "Artist A")) } +
            (20..23).map { candidate(song(it, artist = "Artist B"), seedId = id(2)) } +
            (30..31).map { candidate(song(it, artist = "Artist C"), seedId = id(3)) }
        val ranked = PlaylistTasteAnalyzer.rank(profile, candidates, limit = 20)

        assertTrue(ranked.isNotEmpty())
        assertTrue(ranked.map { it.song.artistsText }.distinct().size >= 2)
        assertTrue(ranked.groupingBy { it.song.artistsText }.eachCount().values.all { it <= 3 })
        assertEquals(ranked.size, ranked.map { it.song.id }.distinct().size)
    }

    private fun id(number: Int): String = "video${number.toString().padStart(6, '0')}"

    private fun song(
        number: Int,
        title: String = "Track $number",
        artist: String? = "Seed Artist",
        genres: List<String>? = listOf("Alternative")
    ) = Song(
        id = id(number),
        title = title,
        artistsText = artist,
        durationText = "3:00",
        thumbnailUrl = null,
        genres = genres
    )

    private fun source(song: Song, playlistId: Long = 1) = PlaylistRecommendationSource(
        song = song,
        playlistId = playlistId,
        playlistName = "Playlist $playlistId"
    )

    private fun candidate(
        song: Song,
        seedId: String = id(1),
        artistIds: Set<String> = emptySet(),
        albumId: String? = null
    ) = PlaylistRecommendationCandidate(
        song = song,
        seedId = seedId,
        artistIds = artistIds,
        albumId = albumId
    )
}

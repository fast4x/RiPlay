package it.fast4x.riplay.extensions.playlistrecommendations

import it.fast4x.riplay.data.models.Blacklist
import it.fast4x.riplay.data.models.PlaylistRecommendationSource
import it.fast4x.riplay.data.models.Song
import java.security.MessageDigest
import java.text.Normalizer
import java.util.Locale
import kotlin.math.ln

/** A playlist-derived taste snapshot. Listening history is deliberately not an input. */
data class PlaylistTasteProfile(
    val playlistCount: Int,
    val songCount: Int,
    val seeds: List<Song>,
    val topArtists: List<String>,
    val artistWeights: Map<String, Double>,
    val genreWeights: Map<String, Double>,
    val knownSongIds: Set<String>,
    val knownTrackKeys: Set<String>,
    val blockedSongIds: Set<String>,
    val blockedArtistIds: Set<String>,
    val blockedArtistNames: Set<String>,
    val blockedAlbumIds: Set<String>,
    val fingerprint: String,
    val blockedFolderPaths: Set<String> = emptySet(),
    val artistIdWeights: Map<String, Double> = emptyMap()
)

data class PlaylistRecommendationCandidate(
    val song: Song,
    val seedId: String,
    val sourceRank: Int = 0,
    val artistIds: Set<String> = emptySet(),
    val albumId: String? = null
)

/** Pure selection and ranking: no database, network, playback state, or side effects. */
object PlaylistTasteAnalyzer {
    private val spaces = Regex("\\s+")
    private val marks = Regex("\\p{M}+")
    private val punctuation = Regex("[^\\p{L}\\p{N}]+")
    private val artistSeparator = Regex("(?i)\\s*(?:[,;|•]|\\s+&\\s+|\\s+feat\\.?\\s+|\\s+ft\\.?\\s+|\\s+featuring\\s+)\\s*")
    private val videoId = Regex("[A-Za-z0-9_-]{11}")
    private val prefixes = Regex("(?i)^(?:(?:e|pinned|modified|monthly|account|piped):)+")
    private val titleDecoration = Regex("(?i)[\\[(][^\\])]*(?:official\\s*(?:music\\s*)?(?:audio|video)|lyric(?:s|\\s+video)?|visuali[sz]er|remaster(?:ed)?(?:\\s+\\d{4})?)[^\\])]*[\\])]")
    private val titleSuffix = Regex("(?i)\\s*(?:[-–—]\\s*)?(?:official\\s*(?:music\\s*)?(?:audio|video)|lyrics?|remaster(?:ed)?(?:\\s+\\d{4})?)\\s*$")

    fun normalize(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFKD)
        .replace(marks, "")
        .lowercase(Locale.ROOT)
        .replace("'", "").replace("’", "")
        .replace(punctuation, " ")
        .trim().replace(spaces, " ")

    /** Keeps meaningful live/remix versions, while removing provider presentation labels. */
    fun trackKey(song: Song): String {
        val title = normalize(song.title.replace(prefixes, "")
            .replace(titleDecoration, " ").replace(titleSuffix, " "))
        val artists = artistNames(song).map(::normalize).filter(String::isNotEmpty).distinct().sorted()
        return if (title.isEmpty() || artists.isEmpty()) "" else "$title|${artists.joinToString("|")}"
    }

    fun analyze(
        sources: List<PlaylistRecommendationSource>,
        blacklists: List<Blacklist> = emptyList(),
        dislikedSongIds: Set<String> = emptySet(),
        maxSeeds: Int = 6
    ): PlaylistTasteProfile {
        val enabled = blacklists.filter { it.isEnabled }
        fun paths(type: String) = enabled.filter { normalize(it.type) == type }
            .map { it.path.trim() }.filter(String::isNotEmpty).toSet()
        val blockedSongs = paths("song") + paths("video") + dislikedSongIds +
            sources.filter { it.song.isDisliked }.map { it.song.id }
        val blockedArtists = paths("artist")
        val blockedArtistNames = enabled.filter { normalize(it.type) == "artist" }
            .flatMap { listOfNotNull(it.name, it.path) }.map(::normalize)
            .filter(String::isNotEmpty).toSet()
        val blockedAlbums = paths("album")
        val blockedFolders = paths("folder")
        val blockedPlaylists = enabled.filter { normalize(it.type) == "playlist" }
        val knownIds = sources.flatMap { listOfNotNull(it.song.id, it.song.mediaId) }
            .filter(String::isNotBlank).toSet()
        val knownKeys = sources.map { trackKey(it.song) }.filter(String::isNotEmpty).toSet()

        val eligible = sources.filter { source ->
            val song = source.song
            song.isPodcast == 0 && !song.isVideo && !song.isDisliked &&
                song.id !in blockedSongs && song.mediaId !in blockedSongs &&
                splitIds(source.artistIds).none { it in blockedArtists } &&
                artistNames(song).none { normalize(it) in blockedArtistNames } &&
                (splitIds(source.albumIds) + listOfNotNull(song.albumId)).none { it in blockedAlbums } &&
                !isFolderBlocked(song.folder, blockedFolders) &&
                blockedPlaylists.none { playlistMatches(source, it) }
        }
        val groups = groupTracks(eligible, { it.song }).map { rows ->
            val representative = rows.sortedWith(
                compareByDescending<PlaylistRecommendationSource> { isPlayableId(it.song.id) }
                    .thenByDescending { metadataQuality(it.song) }
                    .thenBy { it.song.id }
            ).first()
            Track(
                song = representative.song,
                key = trackKey(representative.song).ifBlank { "id:${representative.song.id}" },
                playlists = rows.map { it.playlistId }.toSet(),
                artists = rows.flatMap { artistNames(it.song) }.sorted().distinctBy(::normalize),
                artistIds = rows.flatMap { splitIds(it.artistIds) }.toSet(),
                genres = rows.flatMap { it.song.genres.orEmpty() }.map(::normalize)
                    .filter(String::isNotEmpty).toSet()
            )
        }.sortedBy { it.key }

        val artistRaw = mutableMapOf<String, Double>()
        val genreRaw = mutableMapOf<String, Double>()
        val artistIdRaw = mutableMapOf<String, Double>()
        val displayNames = mutableMapOf<String, String>()
        groups.forEach { track ->
            track.artists.forEach { name ->
                val key = normalize(name)
                artistRaw[key] = (artistRaw[key] ?: 0.0) + 1.0 / track.artists.size
                displayNames.putIfAbsent(key, name)
            }
            track.genres.forEach { genre ->
                genreRaw[genre] = (genreRaw[genre] ?: 0.0) + 1.0 / track.genres.size
            }
            track.artistIds.forEach { id ->
                artistIdRaw[id] = (artistIdRaw[id] ?: 0.0) + 1.0 / track.artistIds.size
            }
        }
        val artists = normalizedWeights(artistRaw)
        return PlaylistTasteProfile(
            playlistCount = eligible.map { it.playlistId }.distinct().size,
            songCount = groups.size,
            seeds = selectSeeds(groups, maxSeeds.coerceAtLeast(0)),
            topArtists = artists.keys.take(12).mapNotNull(displayNames::get),
            artistWeights = artists,
            genreWeights = normalizedWeights(genreRaw),
            knownSongIds = knownIds,
            knownTrackKeys = knownKeys,
            blockedSongIds = blockedSongs,
            blockedArtistIds = blockedArtists,
            blockedArtistNames = blockedArtistNames,
            blockedAlbumIds = blockedAlbums,
            fingerprint = fingerprint(sources, enabled, dislikedSongIds, maxSeeds),
            blockedFolderPaths = blockedFolders,
            artistIdWeights = normalizedWeights(artistIdRaw)
        )
    }

    fun rank(
        profile: PlaylistTasteProfile,
        candidates: List<PlaylistRecommendationCandidate>,
        limit: Int = 20
    ): List<PlaylistRecommendationCandidate> {
        if (limit <= 0) return emptyList()
        val eligible = candidates.filter { candidate ->
            val song = candidate.song
            val key = trackKey(song)
            song.isPodcast == 0 && !song.isVideo && !song.isDisliked &&
                song.id !in profile.knownSongIds && song.mediaId !in profile.knownSongIds &&
                (key.isEmpty() || key !in profile.knownTrackKeys) &&
                song.id !in profile.blockedSongIds && song.mediaId !in profile.blockedSongIds &&
                candidate.artistIds.none { it in profile.blockedArtistIds } &&
                artistNames(song).none { normalize(it) in profile.blockedArtistNames } &&
                candidate.albumId !in profile.blockedAlbumIds && song.albumId !in profile.blockedAlbumIds &&
                !isFolderBlocked(song.folder, profile.blockedFolderPaths)
        }
        val scored = groupTracks(eligible, { it.song }).map { matches ->
            val best = matches.sortedWith(
                compareByDescending<PlaylistRecommendationCandidate> { isPlayableId(it.song.id) }
                    .thenBy { it.sourceRank.coerceAtLeast(0) }
                    .thenByDescending { metadataQuality(it.song) }.thenBy { it.song.id }.thenBy { it.seedId }
            ).first()
            val names = matches.flatMap { artistNames(it.song) }.map(::normalize).toSet()
            val ids = matches.flatMap { it.artistIds }.toSet()
            val artistAffinity = maxOf(
                names.maxOfOrNull { profile.artistWeights[it] ?: 0.0 } ?: 0.0,
                ids.maxOfOrNull { profile.artistIdWeights[it] ?: 0.0 } ?: 0.0
            )
            val genres = matches.flatMap { it.song.genres.orEmpty() }.map(::normalize).toSet()
            val genreAffinity = genres.maxOfOrNull { profile.genreWeights[it] ?: 0.0 } ?: 0.0
            val support = matches.map { it.seedId }.distinct().size
            val rankScore = 1.0 / (1.0 + matches.minOf { it.sourceRank.coerceAtLeast(0) } * 0.18)
            val score = 0.45 * artistAffinity + 0.12 * genreAffinity +
                0.25 * rankScore + 0.18 * (ln(1.0 + support) / ln(7.0)).coerceAtMost(1.0)
            Ranked(best, score, names.map { "name:$it" }.toSet() + ids.map { "id:$it" })
        }.sortedWith(compareByDescending<Ranked> { it.score }.thenBy { it.candidate.sourceRank }
            .thenBy { it.candidate.song.id })
        val artistCounts = mutableMapOf<String, Int>()
        val result = mutableListOf<PlaylistRecommendationCandidate>()
        for (entry in scored) {
            if (entry.artistKeys.any { (artistCounts[it] ?: 0) >= 3 }) continue
            result += entry.candidate
            entry.artistKeys.forEach { artistCounts[it] = (artistCounts[it] ?: 0) + 1 }
            if (result.size == limit) break
        }
        return result
    }

    private data class Track(
        val song: Song,
        val key: String,
        val playlists: Set<Long>,
        val artists: List<String>,
        val artistIds: Set<String>,
        val genres: Set<String>
    )

    private data class Ranked(
        val candidate: PlaylistRecommendationCandidate,
        val score: Double,
        val artistKeys: Set<String>
    )

    private fun selectSeeds(tracks: List<Track>, limit: Int): List<Song> {
        val queues = tracks.filter { it.song.title.isNotBlank() &&
            (isPlayableId(it.song.id) || !it.song.artistsText.isNullOrBlank()) }
            .flatMap { track -> track.playlists.map { it to track } }
            .groupBy({ it.first }, { it.second }).toSortedMap()
            .mapValues { (_, values) -> values.toMutableList() }
        val selectedKeys = mutableSetOf<String>()
        val artistCounts = mutableMapOf<String, Int>()
        val result = mutableListOf<Song>()
        while (result.size < limit) {
            var added = false
            for ((_, queue) in queues) {
                queue.removeAll { it.key in selectedKeys }
                val next = queue.minWithOrNull(compareBy<Track> { track ->
                    track.artists.maxOfOrNull { artistCounts[normalize(it)] ?: 0 } ?: 0
                }.thenBy { it.key }) ?: continue
                result += next.song
                selectedKeys += next.key
                next.artists.forEach { name ->
                    val key = normalize(name)
                    artistCounts[key] = (artistCounts[key] ?: 0) + 1
                }
                added = true
                if (result.size == limit) break
            }
            if (!added) break
        }
        return result
    }

    private fun artistNames(song: Song): List<String> = song.artistsText.orEmpty()
        .replace(Regex("(?i)\\s+-\\s+topic\\s*$"), "")
        .split(artistSeparator).map(String::trim).filter(String::isNotEmpty).distinctBy(::normalize)

    private fun splitIds(value: String?): Set<String> = value.orEmpty().split('|')
        .map(String::trim).filter(String::isNotEmpty).toSet()

    private fun isPlayableId(id: String): Boolean = videoId.matches(id)
    private fun metadataQuality(song: Song): Int =
        (if (song.artistsText.isNullOrBlank()) 0 else 4) +
            (if (song.genres.isNullOrEmpty()) 0 else 2) + (if (song.thumbnailUrl.isNullOrBlank()) 0 else 1)

    private fun normalizedWeights(raw: Map<String, Double>): Map<String, Double> {
        val max = raw.values.maxOrNull()?.takeIf { it > 0.0 } ?: return emptyMap()
        return raw.entries.sortedWith(compareByDescending<Map.Entry<String, Double>> { it.value }.thenBy { it.key })
            .associate { it.key to it.value / max }
    }

    private fun isFolderBlocked(folder: String?, blocked: Set<String>): Boolean {
        if (folder.isNullOrBlank()) return false
        val value = folder.replace('\\', '/').trimEnd('/')
        return blocked.any { path ->
            val prefix = path.replace('\\', '/').trimEnd('/')
            if (path == "/") value.startsWith('/')
            else prefix.isNotEmpty() && (value == prefix || value.startsWith("$prefix/"))
        }
    }

    private fun playlistMatches(source: PlaylistRecommendationSource, blocked: Blacklist): Boolean {
        val path = blocked.path.trim()
        if (path == source.playlistId.toString() ||
            path.matches(Regex("(?i)(?:localplaylist|playlist)[/:]${source.playlistId}"))) return true
        if (path.toLongOrNull() != null) return false
        val names = listOf(source.playlistName, source.playlistName.replace(prefixes, "")).map(::normalize)
        return normalize(path) in names ||
            (!blocked.name.isNullOrBlank() && normalize(blocked.name) in names)
    }

    /** ID and canonical-track equality are transitive, including pending/matched aliases. */
    private fun <T> groupTracks(items: List<T>, song: (T) -> Song): List<List<T>> {
        val parents = IntArray(items.size) { it }
        fun root(index: Int): Int {
            var current = index
            while (parents[current] != current) {
                parents[current] = parents[parents[current]]
                current = parents[current]
            }
            return current
        }
        val ids = mutableMapOf<String, Int>()
        val keys = mutableMapOf<String, Int>()
        items.forEachIndexed { index, item ->
            val value = song(item)
            listOfNotNull(value.id, value.mediaId).filter(String::isNotBlank).forEach { id ->
                ids.putIfAbsent(id, index)?.let { parents[root(index)] = root(it) }
            }
            val key = trackKey(value)
            if (key.isNotEmpty()) keys.putIfAbsent(key, index)?.let { parents[root(index)] = root(it) }
        }
        return items.indices.groupBy(::root).values.map { indices -> indices.map(items::get) }
    }

    private fun fingerprint(
        sources: List<PlaylistRecommendationSource>,
        enabled: List<Blacklist>,
        dislikes: Set<String>,
        maxSeeds: Int
    ): String {
        fun encode(values: List<String?>) = values.joinToString("") { value ->
            val text = value.orEmpty()
            "${text.length}:$text"
        }
        val records = sources.map { source ->
            val song = source.song
            encode(listOf("song", source.playlistId.toString(), source.playlistName, song.id,
                song.mediaId, song.title, song.artistsText, song.durationText, song.thumbnailUrl,
                song.isAudioOnly.toString(), song.isPodcast.toString(), song.folder, song.albumId,
                song.isDisliked.toString(), song.genres.orEmpty().map(::normalize).distinct().sorted().joinToString("|"),
                splitIds(source.artistIds).sorted().joinToString("|"), splitIds(source.albumIds).sorted().joinToString("|")))
        } + enabled.map { encode(listOf("block", normalize(it.type), it.path, it.name)) } +
            dislikes.map { encode(listOf("dislike", it)) } + listOf("seeds:$maxSeeds")
        val digest = MessageDigest.getInstance("SHA-256")
        records.distinct().sorted().forEach { record ->
            digest.update(record.toByteArray(Charsets.UTF_8))
            digest.update(0.toByte())
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}

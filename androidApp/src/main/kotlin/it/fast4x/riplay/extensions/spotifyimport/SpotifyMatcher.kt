package it.fast4x.riplay.extensions.spotifyimport

import androidx.media3.common.util.UnstableApi
import it.fast4x.environment.Environment
import it.fast4x.environment.models.bodies.SearchBody
import it.fast4x.environment.requests.searchPage
import it.fast4x.environment.utils.fromMusicShelfRenderer
import it.fast4x.riplay.commonutils.cleanPrefix
import it.fast4x.riplay.commonutils.durationTextToMillis
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.data.models.Song
import it.fast4x.riplay.data.models.SongPlaylistMap
import it.fast4x.riplay.utils.asMediaItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.absoluteValue

/**
 * Converts songs imported from a Spotify CSV (id "spotify:track:…") into playable
 * YouTube Music songs, keeping playlist order and "liked" state.
 * Runs in the background, one song at a time, and survives screen changes.
 */
object SpotifyMatcher {

    data class Progress(val done: Int = 0, val total: Int = 0, val failed: Int = 0) {
        val running get() = total > 0 && done < total
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val running = AtomicBoolean(false)
    @Volatile private var rerun = false

    private val _progress = MutableStateFlow(Progress())
    val progress: StateFlow<Progress> = _progress.asStateFlow()

    /** Safe to call many times: if already running, it simply runs one more pass at the end. */
    fun request() {
        if (!running.compareAndSet(false, true)) { rerun = true; return }
        scope.launch {
            try {
                do {
                    rerun = false
                    runPass()
                } while (rerun)
            } finally {
                running.set(false)
            }
        }
    }

    private suspend fun runPass() {
        val pending = runCatching { Database.pendingSpotifySongs() }.getOrElse { emptyList() }
        if (pending.isEmpty()) return
        var failed = 0
        _progress.value = Progress(0, pending.size, 0)
        pending.forEachIndexed { index, song ->
            val ok = runCatching { matchOne(song) }
                .onFailure { Timber.e(it, "SpotifyMatcher failed for ${song.title}") }
                .getOrDefault(false)
            if (!ok) failed++
            _progress.value = Progress(index + 1, pending.size, failed)
            delay(150) // be gentle with YouTube Music
        }
    }

    private fun normalize(text: String) = text
        .lowercase()
        .replace(Regex("\\(.*?\\)|\\[.*?]"), " ")
        .replace(Regex("- .*remaster.*|feat\\..*|ft\\..*"), " ")
        .filter { it.isLetterOrDigit() || it.isWhitespace() }
        .replace(Regex("\\s+"), " ")
        .trim()

    @androidx.annotation.OptIn(UnstableApi::class)
    private suspend fun matchOne(song: Song): Boolean {
        val title = cleanPrefix(song.title)
        val mainArtist = song.artistsText.orEmpty().split(",").firstOrNull()?.trim().orEmpty()

        val items = Environment.searchPage(
            body = SearchBody(
                query = "$title $mainArtist".trim(),
                params = Environment.SearchFilter.Song.value
            ),
            fromMusicShelfRendererContent = Environment.SongItem.Companion::fromMusicShelfRenderer
        )?.getOrNull()?.items.orEmpty().filterNotNull().take(6)

        if (items.isEmpty()) return false

        val wantedTitle = normalize(title)
        val wantedArtist = normalize(mainArtist)
        val wantedMs = durationTextToMillis(song.durationText ?: "")

        // Score: title similarity + artist match + duration closeness
        val best = items.maxByOrNull { item ->
            val t = normalize(item.info?.name ?: "")
            val a = normalize(item.authors?.joinToString(" ") { it.name ?: "" } ?: "")
            var score = 0
            if (t == wantedTitle) score += 6 else if (t.contains(wantedTitle) || wantedTitle.contains(t)) score += 3
            if (wantedArtist.isNotEmpty() && a.contains(wantedArtist)) score += 4
            val ms = durationTextToMillis(item.durationText ?: "")
            if (wantedMs > 0 && ms > 0) {
                val diff = (ms - wantedMs).absoluteValue
                score += when {
                    diff <= 3_000 -> 4
                    diff <= 10_000 -> 2
                    diff > 60_000 -> -4
                    else -> 0
                }
            }
            score
        } ?: return false

        val mediaItem = best.asMediaItem
        val newId = mediaItem.mediaId
        if (newId.isBlank()) return false

        val usages = Database.playlistsUsedForSong(song.id)
        Database.insert(mediaItem)
        usages.forEach { use ->
            Database.deleteSongFromPlaylist(song.id, use.playlistId)
            Database.insert(
                SongPlaylistMap(songId = newId, playlistId = use.playlistId, position = use.position).default()
            )
        }
        if (song.likedAt != null) Database.like(newId, song.likedAt)
        Database.delete(song)
        Timber.d("SpotifyMatcher ${song.title} -> ${best.info?.name} ($newId)")
        return true
    }
}

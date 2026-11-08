package it.fast4x.riplay.extensions.voice

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import it.fast4x.environment.Environment
import it.fast4x.environment.models.bodies.SearchBody
import it.fast4x.environment.requests.searchPage
import it.fast4x.environment.utils.fromMusicShelfRenderer
import it.fast4x.riplay.R
import it.fast4x.riplay.commonutils.cleanPrefix
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.enums.QueueLoopType
import it.fast4x.riplay.extensions.appsettings.AppSettingsManager
import it.fast4x.riplay.services.playback.PlayerService
import it.fast4x.riplay.utils.asMediaItem
import it.fast4x.riplay.utils.forcePlay
import it.fast4x.riplay.utils.forcePlayFromBeginning
import it.fast4x.riplay.utils.playNext
import it.fast4x.riplay.utils.playPrevious
import it.fast4x.riplay.utils.shuffleQueue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import timber.log.Timber

data class VoiceCommandResult(
    val message: String,
    val isError: Boolean = false,
)

/**
 * Applies a [VoiceCommand] to the running player. Songs are searched exactly like
 * the app's own search (YouTube Music, falling back to the local library when
 * offline); playlists are resolved against the playlists already on the device.
 */
@OptIn(UnstableApi::class)
class VoiceCommandExecutor(
    private val context: Context,
    private val binder: PlayerService.Binder,
    private val appSettingsManager: AppSettingsManager,
) {

    suspend fun execute(command: VoiceCommand): VoiceCommandResult = when (command) {
        is VoiceCommand.PlaySong -> playSong(command.query)
        is VoiceCommand.PlayPlaylist -> playPlaylist(command.name)
        VoiceCommand.Pause -> onMain {
            binder.hybridPlayer.pause()
            success(R.string.voice_paused)
        }
        VoiceCommand.Resume -> onMain {
            if (binder.hybridPlayer.mediaItemCount == 0) error(R.string.voice_nothing_to_play)
            else {
                binder.hybridPlayer.play()
                success(R.string.voice_resumed)
            }
        }
        VoiceCommand.Next -> onMain {
            if (binder.hybridPlayer.mediaItemCount == 0) error(R.string.voice_nothing_to_play)
            else {
                binder.hybridPlayer.playNext()
                success(R.string.voice_next_song)
            }
        }
        VoiceCommand.Previous -> onMain {
            if (binder.hybridPlayer.mediaItemCount == 0) error(R.string.voice_nothing_to_play)
            else {
                binder.hybridPlayer.playPrevious()
                success(R.string.voice_previous_song)
            }
        }
        VoiceCommand.Replay -> onMain {
            if (binder.hybridPlayer.mediaItemCount == 0) error(R.string.voice_nothing_to_play)
            else {
                binder.hybridPlayer.seekTo(0L)
                binder.hybridPlayer.play()
                success(R.string.voice_replaying)
            }
        }
        VoiceCommand.LoopSong -> setLoop(QueueLoopType.RepeatOne, R.string.voice_loop_song)
        VoiceCommand.LoopQueue -> setLoop(QueueLoopType.RepeatAll, R.string.voice_loop_queue)
        VoiceCommand.LoopOff -> setLoop(QueueLoopType.Default, R.string.voice_loop_off)
        VoiceCommand.Shuffle -> onMain {
            if (binder.hybridPlayer.mediaItemCount == 0) error(R.string.voice_nothing_to_play)
            else {
                binder.hybridPlayer.shuffleModeEnabled = true
                binder.hybridPlayer.shuffleQueue()
                success(R.string.voice_shuffle_on)
            }
        }
        VoiceCommand.Unknown -> error(R.string.voice_unknown_command)
    }

    private suspend fun playSong(query: String): VoiceCommandResult {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return onMain { success(R.string.voice_resumed) }

        val onlineItems = withContext(Dispatchers.IO) {
            runCatching {
                Environment.searchPage(
                    body = SearchBody(
                        query = trimmed,
                        params = Environment.SearchFilter.Song.value
                    ),
                    fromMusicShelfRendererContent = Environment.SongItem.Companion::fromMusicShelfRenderer
                )?.getOrNull()?.items.orEmpty()
            }.onFailure { Timber.e(it, "VoiceCommandExecutor online search failed for $trimmed") }
                .getOrDefault(emptyList())
        }

        val bestIndex = VoiceNameMatcher.bestMatchIndex(
            spoken = trimmed,
            candidates = onlineItems.map { it.title.orEmpty() }
        )
        val song = bestIndex?.let { onlineItems.getOrNull(it) }
        if (song != null) {
            return onMain {
                binder.stopRadio()
                binder.hybridPlayer.forcePlay(song.asMediaItem)
                success(R.string.voice_now_playing_song, song.title ?: trimmed)
            }
        }

        val localSong = withContext(Dispatchers.IO) {
            runCatching { Database.search("%$trimmed%").first().firstOrNull() }
                .onFailure { Timber.e(it, "VoiceCommandExecutor local search failed for $trimmed") }
                .getOrNull()
        }
        if (localSong != null) {
            return onMain {
                binder.stopRadio()
                binder.hybridPlayer.forcePlay(localSong.asMediaItem)
                success(R.string.voice_now_playing_song, localSong.title)
            }
        }

        return error(R.string.voice_song_not_found, trimmed)
    }

    private suspend fun playPlaylist(spokenName: String): VoiceCommandResult {
        val name = spokenName.trim()
        if (name.isEmpty()) return error(R.string.voice_playlist_not_found, spokenName)

        val previews = withContext(Dispatchers.IO) {
            runCatching { Database.playlistPreviewsByNameAsc().first() }
                .onFailure { Timber.e(it, "VoiceCommandExecutor could not load playlists") }
                .getOrDefault(emptyList())
        }

        val displayNames = previews.map { cleanPrefix(it.playlist.name) }
        val matchedDisplayName = VoiceNameMatcher.bestMatch(name, displayNames)
        val playlist = previews.firstOrNull {
            matchedDisplayName != null && cleanPrefix(it.playlist.name) == matchedDisplayName
        }?.playlist

        if (playlist == null) return error(R.string.voice_playlist_not_found, name)

        val songs = withContext(Dispatchers.IO) {
            runCatching { Database.playlistWithSongsNoFlow(playlist.name)?.songs.orEmpty() }
                .onFailure { Timber.e(it, "VoiceCommandExecutor could not load playlist ${playlist.name}") }
                .getOrDefault(emptyList())
        }
        if (songs.isEmpty()) return error(R.string.voice_playlist_empty, cleanPrefix(playlist.name))

        val mediaItems = songs.map { it.asMediaItem }
        onMain {
            binder.stopRadio()
            binder.hybridPlayer.forcePlayFromBeginning(mediaItems)
        }
        return success(R.string.voice_now_playing_playlist, cleanPrefix(playlist.name))
    }

    private suspend fun setLoop(mode: QueueLoopType, messageRes: Int): VoiceCommandResult {
        onMain { binder.hybridPlayer.repeatMode = mode.type }
        runCatching {
            appSettingsManager.updateSettings(
                appSettingsManager.activeSettings.value.copy(queueLoopType = mode)
            )
        }.onFailure { Timber.e(it, "VoiceCommandExecutor could not persist loop mode") }
        return success(messageRes)
    }

    private suspend fun <T> onMain(block: () -> T): T = withContext(Dispatchers.Main) { block() }

    private fun success(messageRes: Int, vararg args: Any): VoiceCommandResult =
        VoiceCommandResult(context.getString(messageRes, *args))

    private fun error(messageRes: Int, vararg args: Any): VoiceCommandResult =
        VoiceCommandResult(context.getString(messageRes, *args), isError = true)
}

package it.fast4x.riplay.service

import android.content.Intent
import android.os.Bundle
import android.support.v4.media.session.MediaSessionCompat
import android.view.KeyEvent
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.SessionCommand
import it.fast4x.riplay.data.models.Song
import it.fast4x.riplay.utils.asMediaItem
import it.fast4x.riplay.utils.forcePlayAtIndex
import it.fast4x.riplay.utils.playNext
import it.fast4x.riplay.utils.playPrevious
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import kotlin.collections.emptyList

@UnstableApi
class PlayerMediaSessionCallback (
    val binder: PlayerService.Binder,
    val onPlayClick: () -> Unit,
    val onPauseClick: () -> Unit,
    val onSeekToPos: (Long) -> Unit,
    val onPlayNext: () -> Unit,
    val onPlayPrevious: () -> Unit,
    val onPlayQueueItem: (Long) -> Unit,
    val onCustomClick: (String) -> Unit,
) : MediaSessionCompat.Callback() {

    override fun onPlay() {
        Timber.d("MediaSessionCallback onPlay()")
        onPlayClick()
    }
    override fun onPause() {
        Timber.d("MediaSessionCallback onPause()")
        onPauseClick()
    }

    override fun onStop() {
        Timber.d("MediaSessionCallback onStop()")
        onPause()
    }
    override fun onSkipToPrevious() {
        Timber.d("MediaSessionCallback onSkipToPrevious()")
        onPlayPrevious()
        //binder.player.playPrevious()
    }
    override fun onSkipToNext() {
        Timber.d("MediaSessionCallback onSkipToNext()")
        onPlayNext()
        //binder.player.playNext()
    }
    override fun onSeekTo(pos: Long) {
        Timber.d("MediaSessionCallback onSeekTo() $pos")
        onSeekToPos(pos)
    }

    override fun onRewind() {
        Timber.d("MediaSessionCallback onRewind()")
        binder.player.seekToDefaultPosition()
    }
    override fun onSkipToQueueItem(id: Long) {
        Timber.d("MediaSessionCallback onSkipToQueueItem() $id")
        onPlayQueueItem(id)
        //binder.player.seekToDefaultPosition(id.toInt())
    }
    override fun onCustomAction(action: String, extras: Bundle?) {
        Timber.d("MediaSessionCallback onCustomAction() action $action")
        onCustomClick(action)
    }
    override fun onPlayFromSearch(query: String?, extras: Bundle?) {
        if (query.isNullOrBlank()) return
        binder.playFromSearch(query)
    }
    @OptIn(UnstableApi::class)
    override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {
        Timber.d("MediaSessionCallback onPlayFromMediaId mediaId ${mediaId} called")
        val data = mediaId?.split('/') ?: return
        var index = 0
        //var mediaItemSelected: MediaItem? = null

        Timber.d("MediaSessionCallback onPlayFromMediaId mediaId ${mediaId} data $data processing")

        CoroutineScope(Dispatchers.IO).launch {
            val mediaItems = when (data.getOrNull(0)) {

                PlayerMediaBrowserService.MediaId.SONGS ->  data
                    .getOrNull(1)
                    ?.let { songId ->
                        index = PlayerMediaBrowserService.lastSongs.indexOfFirst { it.id == songId }

                        if (index < 0) return@launch // index not found

                        //mediaItemSelected = PlayerMediaBrowserService.lastSongs[index].asMediaItem
                        PlayerMediaBrowserService.lastSongs
                    }
                    .also { Timber.d("MediaSessionCallback onPlayFromMediaId processing songs, mediaId ${mediaId} index $index songs ${it?.size}") }

                PlayerMediaBrowserService.MediaId.SEARCHED -> data
                    .getOrNull(1)
                    ?.let { songId ->
                        index = PlayerMediaBrowserService.searchedSongs.indexOfFirst { it.id == songId }

                        if (index < 0) return@launch // index not found

                        //mediaItemSelected = PlayerMediaBrowserService.searchedSongs[index].asMediaItem
                        PlayerMediaBrowserService.searchedSongs

                    }

                // Maybe it needed in the future
                /*
                PlayerMediaBrowserService.MediaId.shuffle -> lastSongs.shuffled()

                PlayerMediaBrowserService.MediaId.favorites -> Database
                    .favorites()
                    .first()

                PlayerMediaBrowserService.MediaId.ondevice -> Database
                    .songsOnDevice()
                    .first()

                PlayerMediaBrowserService.MediaId.top -> {
                    val maxTopSongs = context().preferences.getEnum(MaxTopPlaylistItemsKey,
                        MaxTopPlaylistItems.`10`).number.toInt()

                    Database.trending(maxTopSongs)
                        .first()
                }

                PlayerMediaBrowserService.MediaId.playlists -> data
                    .getOrNull(1)
                    ?.toLongOrNull()
                    ?.let(Database::playlistWithSongs)
                    ?.first()
                    ?.songs

                PlayerMediaBrowserService.MediaId.albums -> data
                    .getOrNull(1)
                    ?.let(Database::albumSongs)
                    ?.first()

                PlayerMediaBrowserService.MediaId.artists -> {
                    data
                        .getOrNull(1)
                        ?.let(Database::artistSongsByname)
                        ?.first()
                }


                */

                else -> emptyList()
            }?.map(Song::asMediaItem) ?: return@launch

            withContext(Dispatchers.Main) {
                Timber.d("MediaSessionCallback onPlayFromMediaId mediaId ${mediaId} index $index mediaItems ${mediaItems.size} ready to play")
                //binder.stopRadio()
                binder.player.forcePlayAtIndex(mediaItems, index)
            }
        }

        // END PROCESSING

    }

    override fun onMediaButtonEvent(mediaButtonEvent: Intent?): Boolean {
        mediaButtonEvent?.let {
            if (it.action == Intent.ACTION_MEDIA_BUTTON) {
                if (it.extras?.getBoolean(Intent.EXTRA_KEY_EVENT) == true) {
                    val keyEvent = it.extras?.getParcelable<KeyEvent>(Intent.EXTRA_KEY_EVENT)
                    when(keyEvent?.keyCode) {
                        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                            if (binder.player.isPlaying || binder.onlinePlayerPlayingState)
                                onPause()
                            else onPlay()

                            return true
                        }
                        KeyEvent.KEYCODE_MEDIA_NEXT -> {
                            onSkipToNext()
                            return true
                        }
                        KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                            onSkipToPrevious()
                            return true
                        }
                        KeyEvent.KEYCODE_MEDIA_STOP -> {
                            onStop()
                            return true
                        }
                        KeyEvent.KEYCODE_MEDIA_PLAY -> {
                            onPlay()
                            return true
                        }
                        KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                            onPause()
                            return true
                        }
                    }
                }
            }
        }
        //return super.onMediaButtonEvent(mediaButtonEvent)
        return false
    }

}

object MediaSessionConstants {
    const val ACTION_TOGGLE_LIKE = "TOGGLE_LIKE"
    const val ACTION_TOGGLE_SHUFFLE = "TOGGLE_SHUFFLE"
    const val ACTION_TOGGLE_REPEAT_MODE = "TOGGLE_REPEAT_MODE"
    const val ACTION_START_RADIO = "START_RADIO"
    const val ACTION_SEARCH = "ACTION_SEARCH"
    val CommandToggleLike = SessionCommand(ACTION_TOGGLE_LIKE, Bundle.EMPTY)
    val CommandToggleShuffle = SessionCommand(ACTION_TOGGLE_SHUFFLE, Bundle.EMPTY)
    val CommandToggleRepeatMode = SessionCommand(ACTION_TOGGLE_REPEAT_MODE, Bundle.EMPTY)
    val CommandStartRadio = SessionCommand(ACTION_START_RADIO, Bundle.EMPTY)
    val CommandSearch = SessionCommand(ACTION_SEARCH, Bundle.EMPTY)
}
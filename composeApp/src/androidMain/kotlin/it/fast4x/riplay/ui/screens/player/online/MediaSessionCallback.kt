package it.fast4x.riplay.ui.screens.player.online

import android.os.Bundle
import android.support.v4.media.session.MediaSessionCompat
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import it.fast4x.riplay.models.Song
import it.fast4x.riplay.service.AndroidAutoService
import it.fast4x.riplay.service.LocalPlayerService
import it.fast4x.riplay.utils.asMediaItem
import it.fast4x.riplay.utils.forcePlayAtIndex
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import kotlin.collections.emptyList

@UnstableApi
class MediaSessionCallback (
    val binder: LocalPlayerService.Binder,
    val onPlayClick: () -> Unit,
    val onPauseClick: () -> Unit,
    val onSeekToPos: (Long) -> Unit,
    val onPlayNext: () -> Unit,
    val onPlayPrevious: () -> Unit,
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
    override fun onSkipToPrevious() {
        Timber.d("MediaSessionCallback onSkipToPrevious()")
        onPlayPrevious()
    }
    override fun onSkipToNext() {
        Timber.d("MediaSessionCallback onSkipToNext()")
        onPlayNext()
    }

    override fun onSeekTo(pos: Long) {
        Timber.d("MediaSessionCallback onSeekTo() $pos")
        onSeekToPos(pos)
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

                AndroidAutoService.MediaId.SONGS ->  data
                    .getOrNull(1)
                    ?.let { songId ->
                        index = AndroidAutoService.lastSongs.indexOfFirst { it.id == songId }

                        if (index < 0) return@launch // index not found

                        //mediaItemSelected = AndroidAutoService.lastSongs[index].asMediaItem
                        AndroidAutoService.lastSongs
                    }
                    .also { Timber.d("MediaSessionCallback onPlayFromMediaId processing songs, mediaId ${mediaId} index $index songs ${it?.size}") }

                AndroidAutoService.MediaId.SEARCHED -> data
                    .getOrNull(1)
                    ?.let { songId ->
                        index = AndroidAutoService.searchedSongs.indexOfFirst { it.id == songId }

                        if (index < 0) return@launch // index not found

                        //mediaItemSelected = AndroidAutoService.searchedSongs[index].asMediaItem
                        AndroidAutoService.searchedSongs

                    }

                // Maybe it needed in the future
                /*
                AndroidAutoService.MediaId.shuffle -> lastSongs.shuffled()

                AndroidAutoService.MediaId.favorites -> Database
                    .favorites()
                    .first()

                AndroidAutoService.MediaId.ondevice -> Database
                    .songsOnDevice()
                    .first()

                AndroidAutoService.MediaId.top -> {
                    val maxTopSongs = context().preferences.getEnum(MaxTopPlaylistItemsKey,
                        MaxTopPlaylistItems.`10`).number.toInt()

                    Database.trending(maxTopSongs)
                        .first()
                }

                AndroidAutoService.MediaId.playlists -> data
                    .getOrNull(1)
                    ?.toLongOrNull()
                    ?.let(Database::playlistWithSongs)
                    ?.first()
                    ?.songs

                AndroidAutoService.MediaId.albums -> data
                    .getOrNull(1)
                    ?.let(Database::albumSongs)
                    ?.first()

                AndroidAutoService.MediaId.artists -> {
                    data
                        .getOrNull(1)
                        ?.let(Database::artistSongsByname)
                        ?.first()
                }


                */

                else -> emptyList()
            }?.map(Song::asMediaItem) ?: return@launch

            withContext(Dispatchers.Main) {
                Timber.d("MediaSessionCallback onPlayFromMediaId mediaId ${mediaId} mediaItems ${mediaItems.size} ready to play")
                onPauseClick() // Try to pause all before play with new mediaItems
                binder.stopRadio()
                binder.player.forcePlayAtIndex(mediaItems, index)
            }
        }

        // END PROCESSING

    }

    override fun onCustomAction(action: String, extras: Bundle?) {
        Timber.d("MediaSessionCallback onCustomAction() action $action")
        onCustomClick(action)
    }
}
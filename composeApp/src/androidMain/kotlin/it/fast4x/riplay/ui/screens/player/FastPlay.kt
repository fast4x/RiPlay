package it.fast4x.riplay.ui.screens.player

import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import it.fast4x.riplay.Database
import it.fast4x.riplay.context
import it.fast4x.riplay.enums.MaxSongs
import it.fast4x.riplay.extensions.preferences.getEnum
import it.fast4x.riplay.extensions.preferences.maxSongsInQueueKey
import it.fast4x.riplay.extensions.preferences.preferences
import it.fast4x.riplay.service.LocalPlayerService
import it.fast4x.riplay.service.isLocal
import it.fast4x.riplay.utils.forcePlay
import it.fast4x.riplay.utils.mediaItems
import it.fast4x.riplay.utils.playAtIndex
import it.fast4x.riplay.utils.playOnline
import it.fast4x.riplay.utils.playOnlineAtIndex
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

@OptIn(UnstableApi::class)
fun fastPlay(
    mediaItem: MediaItem? = null,
    binder: LocalPlayerService.Binder?,
    mediaItems: List<MediaItem>? = emptyList(),
    withReplace: Boolean = false,
    withShuffle: Boolean = false,
) {

    CoroutineScope(Dispatchers.IO).launch {
        Database.asyncTransaction {
            mediaItem?.let { insert(it) }
            mediaItems?.onEach { insert(it) }
        }

        val maxSongsInQueue = context().preferences
            .getEnum(maxSongsInQueueKey, MaxSongs.`500` )
            .number
            .toInt()

        withContext(Dispatchers.Main) {
            binder?.stopRadio()
            mediaItems?.let {
                binder?.player?.setMediaItems(if (withShuffle) it.shuffled().take( maxSongsInQueue ) else it.take( maxSongsInQueue ))
            }
            val mediaItemToPlay = mediaItem
                ?: if (!withShuffle) binder?.player?.mediaItems?.first()
                else binder?.player?.mediaItems?.get(Random.nextInt(binder.player.mediaItems.size-1))
            if (mediaItemToPlay?.isLocal == true) {
                binder?.player?.forcePlay(mediaItemToPlay, withReplace)
            } else {
                mediaItemToPlay?.let { binder?.player?.playOnline(it, withReplace) }
            }
        }
    }

}

@OptIn(UnstableApi::class)
fun fastPlayAtIndex(
    index: Int,
    mediaItem: MediaItem,
    binder: LocalPlayerService.Binder?,
) {
    binder?.stopRadio()
    if (mediaItem.isLocal) {
        binder?.player?.playAtIndex(index)

    } else {
        binder?.player?.playOnlineAtIndex(index)
    }
}
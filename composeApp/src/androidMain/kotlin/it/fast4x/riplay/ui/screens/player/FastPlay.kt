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
import it.fast4x.riplay.utils.findMediaItemIndexById
import it.fast4x.riplay.utils.forcePlay
import it.fast4x.riplay.utils.forcePlayAtIndex
import it.fast4x.riplay.utils.getQueueWindows
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
fun __fastPlay(
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


        // todo implement isLiked and manage isExplicit in mediaItem
        withContext(Dispatchers.Main) {

            if (mediaItems?.isNotEmpty() == true && mediaItem != null)
                binder?.player?.forcePlayAtIndex(
                    if (withShuffle) mediaItems.shuffled().take( maxSongsInQueue ) else mediaItems.take( maxSongsInQueue ),
                    binder.player.findMediaItemIndexById(mediaItem.mediaId),
                )

            if (mediaItem != null)
                binder?.player?.forcePlay(mediaItem,true)




            //binder?.stopRadio()
//            mediaItems?.let {
//                binder?.player?.setMediaItems(
//                    if (withShuffle) it.shuffled().take( maxSongsInQueue ) else it.take( maxSongsInQueue ),
//                    if (mediaItem != null) binder.player.findMediaItemIndexById(mediaItem.mediaId) else 0,
//                    0
//                )
//            }


//            if (mediaItemToPlay?.isLocal == true) {
//                binder?.player?.forcePlay(mediaItemToPlay, withReplace)
//            } else {
//                mediaItemToPlay?.let { binder?.player?.playOnline(it, withReplace) }
//            }
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
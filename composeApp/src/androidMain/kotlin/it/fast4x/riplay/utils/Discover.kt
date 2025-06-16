package it.fast4x.riplay.utils

import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.util.UnstableApi
import it.fast4x.riplay.Database
import it.fast4x.riplay.LocalPlayerServiceBinder
import it.fast4x.riplay.R
import it.fast4x.riplay.enums.PopupType
import it.fast4x.riplay.ui.components.themed.SmartMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(UnstableApi::class)
@Composable
fun ApplyDiscoverToQueue() {
    /*   DISCOVER  */
    val discoverIsEnabled by rememberPreference(discoverKey, false)
    if (!discoverIsEnabled) return

    val binder = LocalPlayerServiceBinder.current

    binder?.player ?: return

    val context = LocalContext.current
    val player = binder.player

    var listMediaItemsIndex = remember {
        mutableListOf<Int>()
    }
    val windows by remember {
        mutableStateOf(player.currentTimeline.windows)
    }

    var songInPlaylist by remember {
        mutableStateOf(0)
    }
    var songIsLiked by remember {
        mutableStateOf(0)
    }

    LaunchedEffect(Unit) {
        listMediaItemsIndex.clear()
        windows.forEach { window ->
            if (window.firstPeriodIndex != player.currentMediaItemIndex) {
                withContext(Dispatchers.IO) {
                    songInPlaylist = Database.songUsedInPlaylists(window.mediaItem.mediaId)
                    songIsLiked = Database.songliked(window.mediaItem.mediaId)
                }
                if (songInPlaylist > 0 || songIsLiked > 0) {
                    listMediaItemsIndex.add(window.firstPeriodIndex)
                }
            }
        }.also {
            if (listMediaItemsIndex.isNotEmpty()) {
                val mediacount = listMediaItemsIndex.size - 1
                listMediaItemsIndex.sort()
                for (i in mediacount.downTo(0)) {
                    binder.player.removeMediaItem(listMediaItemsIndex[i])
                }
                SmartMessage(context.resources.getString(R.string.discover_has_been_applied_to_queue).format(listMediaItemsIndex.size), PopupType.Success, context = context)
                listMediaItemsIndex.clear()
            }
        }

    }

    /*   DISCOVER  */

}
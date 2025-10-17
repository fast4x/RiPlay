package it.fast4x.riplay.ui.components.tab.toolbar

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import it.fast4x.riplay.LocalPlayerServiceBinder
import it.fast4x.riplay.R
import it.fast4x.riplay.utils.appContext
import it.fast4x.riplay.enums.MaxSongs
import it.fast4x.riplay.extensions.preferences.getEnum
import it.fast4x.riplay.extensions.preferences.maxSongsInQueueKey
import it.fast4x.riplay.extensions.preferences.preferences
import it.fast4x.riplay.service.LocalPlayerService
import it.fast4x.riplay.ui.components.themed.SmartMessage
import it.fast4x.riplay.utils.forcePlayFromBeginning
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

@UnstableApi
class SongsShuffle private constructor(
    private val binder: LocalPlayerService.Binder?,
    private val songs: () -> Flow<List<MediaItem>>
): MenuIcon, Descriptive {

    companion object {
        @JvmStatic
        @Composable
        fun init( songs: () -> Flow<List<MediaItem>> ) =
            SongsShuffle( LocalPlayerServiceBinder.current, songs )
    }

    override val iconId: Int = R.drawable.shuffle
    override val messageId: Int = R.string.shuffle
    override val menuIconTitle: String
        @Composable
        get() = stringResource( messageId )

    override fun onShortClick() {
        CoroutineScope( Dispatchers.IO ).launch {
            songs().collect {
                //fastPlay(binder = binder, mediaItems = it, withShuffle = true )
                playShuffledSongs( it, appContext(), binder )
                throw CancellationException()
            }
        }
    }
}

@UnstableApi
fun playShuffledSongs( mediaItems: List<MediaItem>, context: Context, binder: LocalPlayerService.Binder? ) {

    if ( binder == null ) return

    // Send message saying that there's no song to play
    if( mediaItems.isEmpty() ) {
        SmartMessage(
            message = "There's no song to play",
            context = context
        )
        return
    }

    val maxSongsInQueue = context.preferences
        .getEnum( maxSongsInQueueKey, MaxSongs.`500` )
        .number
        .toInt()

    mediaItems.let { songs ->

        // Return whole list if its size is less than queue size
        val songsInQueue = songs.shuffled().take( maxSongsInQueue )
        CoroutineScope( Dispatchers.Main ).launch {
            binder.stopRadio()
            binder.player.forcePlayFromBeginning( songsInQueue )
        }
    }
}
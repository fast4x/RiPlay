package it.fast4x.riplay.ui.components.tab.toolbar

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import it.fast4x.riplay.LocalPlayerServiceBinder
import it.fast4x.riplay.R
import it.fast4x.riplay.service.LocalPlayerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import it.fast4x.riplay.appContext
import it.fast4x.riplay.ui.screens.player.fastPlay
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
                fastPlay(binder = binder, mediaItems = it, withShuffle = true )
                throw CancellationException()
            }
        }
    }
}
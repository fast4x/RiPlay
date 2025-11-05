package it.fast4x.riplay.ui.components.tab

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.res.stringResource
import androidx.media3.common.util.UnstableApi
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.LocalPlayerServiceBinder
import it.fast4x.riplay.R
import it.fast4x.riplay.service.PlayerService
import it.fast4x.riplay.ui.components.LocalGlobalSheetState
import it.fast4x.riplay.ui.components.GlobalSheetState

@UnstableApi
class DeleteHiddenSongsDialog private constructor(
    private val binder: PlayerService.Binder?,
    activeState: MutableState<Boolean>,
    globalSheetState: GlobalSheetState
): DelSongDialog(binder, activeState, globalSheetState) {

    companion object {
        @JvmStatic
        @Composable
        fun init() = DeleteHiddenSongsDialog(
            LocalPlayerServiceBinder.current,
            rememberSaveable { mutableStateOf( false ) },
            LocalGlobalSheetState.current
        )
    }

    override val messageId: Int = R.string.delete_hidden_songs_message
    override val iconId: Int = R.drawable.trash
    override val dialogTitle: String
        @Composable
        get() = stringResource( R.string.delete_hidden_songs )

    override fun onConfirm() {

            Database.asyncTransaction {
                globalSheetState.hide()
                deleteHiddenSongs()
                cleanSongArtistMap()
                cleanSongAlbumMap()
                cleanSongPlaylistMap()
            }

        onDismiss()
    }
}
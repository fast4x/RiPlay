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
import it.fast4x.riplay.data.models.SongEntity
import it.fast4x.riplay.service.LocalPlayerService
import it.fast4x.riplay.service.PlayerService
import it.fast4x.riplay.ui.components.LocalMenuState
import it.fast4x.riplay.ui.components.MenuState
import it.fast4x.riplay.ui.components.themed.SmartMessage
import it.fast4x.riplay.utils.appContext
import it.fast4x.riplay.ui.components.themed.DeleteDialog
import java.util.Optional

@UnstableApi
open class DelSongDialog protected constructor(
    private val binder: PlayerService.Binder?,
    activeState: MutableState<Boolean>,
    menuState: MenuState,
): DeleteDialog( activeState, menuState ) {

    companion object {
        @JvmStatic
        @Composable
        fun init() = DelSongDialog(
            LocalPlayerServiceBinder.current,
            rememberSaveable { mutableStateOf( false ) },
            LocalMenuState.current
        )
    }

    override val dialogTitle: String
        @Composable
        get() = stringResource( R.string.delete_song )

    var song: Optional<SongEntity> = Optional.empty()

    override fun onDismiss() {
        // Always override current value with empty Optional
        // to prevent unwanted outcomes
        song = Optional.empty()
        super.onDismiss()
    }

    override fun onConfirm() {
        println("Deleting song ${song}")
        song.ifPresent {
            println("Deleting song ${it.song.title}")
            Database.asyncTransaction {
                menuState.hide()
                binder?.cache?.removeResource(it.song.id)
                deleteSongFromPlaylists(it.song.id)
                deleteFormat(it.song.id)
                delete(it.song)
            }
            SmartMessage(
                message = appContext().resources.getString(R.string.deleted),
                context = appContext()
            )
        }

        onDismiss()
    }
}
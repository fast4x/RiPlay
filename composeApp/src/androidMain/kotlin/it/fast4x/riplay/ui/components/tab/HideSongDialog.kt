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
import it.fast4x.riplay.ui.components.LocalMenuState
import it.fast4x.riplay.ui.components.MenuState

@UnstableApi
class HideSongDialog private constructor(
    private val binder: PlayerService.Binder?,
    activeState: MutableState<Boolean>,
    menuState: MenuState
): DelSongDialog(binder, activeState, menuState) {

    companion object {
        @JvmStatic
        @Composable
        fun init() = HideSongDialog(
            LocalPlayerServiceBinder.current,
            rememberSaveable { mutableStateOf( false ) },
            LocalMenuState.current
        )
    }

    override val messageId: Int = R.string.hide
    override val iconId: Int = R.drawable.eye_off
    override val dialogTitle: String
        @Composable
        get() = stringResource( R.string.hidesong )

    override fun onConfirm() {
        song.ifPresent {
            Database.asyncTransaction {
                menuState.hide()
                binder?.cache?.removeResource(it.song.id)
                resetFormatContentLength(it.song.id)
                incrementTotalPlayTimeMs(
                    it.song.id,
                    -it.song.totalPlayTimeMs
                )
            }
        }

        onDismiss()
    }
}
package it.fast4x.riplay.ui.components.tab

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.res.stringResource
import it.fast4x.riplay.R
import it.fast4x.riplay.ui.components.LocalGlobalSheetState
import it.fast4x.riplay.ui.components.GlobalSheetState
import it.fast4x.riplay.ui.components.themed.Menu
import it.fast4x.riplay.ui.components.themed.MenuEntry
import it.fast4x.riplay.ui.components.tab.toolbar.Descriptive
import it.fast4x.riplay.ui.components.tab.toolbar.MenuIcon
import it.fast4x.riplay.enums.HomeItemSize
import it.fast4x.riplay.extensions.preferences.Preference

class ItemSize private constructor(
    val globalSheetState: GlobalSheetState,
    private val sizeState: MutableState<HomeItemSize>
): MenuIcon, Descriptive {

    companion object {
        @JvmStatic
        @Composable
        fun init(key: Preference.Key<HomeItemSize>): ItemSize =
            ItemSize(
                LocalGlobalSheetState.current,
                Preference.remember(key)
            )
    }

    override val iconId: Int = R.drawable.resize
    override val messageId: Int = R.string.size
    override val menuIconTitle: String
        @Composable
        get() = stringResource( R.string.size )

    var size: HomeItemSize = sizeState.value
        set(value) {
            sizeState.value = value
            field = value
        }

    @Composable
    private fun Entry( size: HomeItemSize) {
        MenuEntry(
            size.iconId,
            stringResource( size.textId ),
            onClick = {
                sizeState.value = size
                globalSheetState::hide
            }
        )
    }

    override fun onShortClick() {
        globalSheetState.display {
            Menu {
                HomeItemSize.entries.forEach { Entry(it) }
            }
        }
    }
}
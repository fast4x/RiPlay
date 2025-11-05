package it.fast4x.riplay.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import it.fast4x.riplay.R
import it.fast4x.riplay.enums.MenuStyle
import it.fast4x.riplay.enums.TopPlaylistPeriod
import it.fast4x.riplay.extensions.preferences.autoShuffleKey
import it.fast4x.riplay.extensions.preferences.menuStyleKey
import it.fast4x.riplay.extensions.preferences.rememberPreference
import it.fast4x.riplay.extensions.preferences.topPlaylistPeriodKey
import it.fast4x.riplay.ui.components.LocalGlobalSheetState
import it.fast4x.riplay.ui.components.GlobalSheetState
import it.fast4x.riplay.ui.components.themed.PeriodMenu
import it.fast4x.riplay.ui.components.tab.toolbar.Descriptive
import it.fast4x.riplay.ui.components.tab.toolbar.DualIcon
import it.fast4x.riplay.ui.components.tab.toolbar.DynamicColor
import it.fast4x.riplay.ui.components.tab.toolbar.Menu
import it.fast4x.riplay.ui.components.tab.toolbar.MenuIcon

class HiddenSongs private constructor(
    private val showHiddenState: MutableState<Boolean>
): MenuIcon, DualIcon, Descriptive {

    companion object {
        @JvmStatic
        @Composable
        fun init() = HiddenSongs(
            rememberSaveable { mutableStateOf( false ) }
        )
    }

    override val iconId: Int = R.drawable.eye
    override val secondIconId: Int = R.drawable.eye_off
    override val messageId: Int = R.string.info_show_hide_hidden_songs
    override val menuIconTitle: String
        @Composable
        get() = stringResource( messageId )

    override var isFirstIcon: Boolean = showHiddenState.value
        set(value) {
            showHiddenState.value = value
            field = value
        }

    fun isShown() = if( isFirstIcon ) 0 else 1

    override fun onShortClick() { isFirstIcon = !isFirstIcon }
}

@Composable
fun randomSort(): MenuIcon = object: MenuIcon, DynamicColor, Descriptive {

    override var isFirstColor: Boolean by rememberPreference(autoShuffleKey, false)
    override val iconId: Int = R.drawable.random
    override val messageId: Int = R.string.random_sorting
    override val menuIconTitle: String
        @Composable
        get() = stringResource( messageId )

    override fun onShortClick() { isFirstColor = !isFirstColor }
}

class PeriodSelector private constructor(
    private val periodState: MutableState<TopPlaylistPeriod>,
    override val globalSheetState: GlobalSheetState,
    override val styleState: MutableState<MenuStyle>
):  MenuIcon, Descriptive, Menu {

    companion object {
        @JvmStatic
        @Composable
        fun init() = PeriodSelector(
            rememberPreference(topPlaylistPeriodKey, TopPlaylistPeriod.PastWeek),
            LocalGlobalSheetState.current,
            rememberPreference(menuStyleKey, MenuStyle.List)
        )
    }

    var period: TopPlaylistPeriod = periodState.value
        set(value) {
            periodState.value = value
            field = value
        }

    override val iconId: Int = period.iconId
    override val messageId: Int = R.string.statistics
    override val menuIconTitle: String
        @Composable
        get() = stringResource( messageId )

    fun onDismiss( period: TopPlaylistPeriod ) {
        this.period = period
        globalSheetState.hide()
    }

    @Composable
    override fun ListMenu() { /* Does nothing */ }

    @Composable
    override fun GridMenu() { /* Does nothing */ }

    @Composable
    override fun MenuComponent() = PeriodMenu(::onDismiss)

    override fun onShortClick() = super.onShortClick()
}
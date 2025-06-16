package it.fast4x.riplay.ui.components.tab.toolbar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import it.fast4x.riplay.enums.MenuStyle
import it.fast4x.riplay.ui.components.MenuState

interface Menu: Icon {

    val menuState: MenuState
    val styleState: MutableState<MenuStyle>

    @Composable
    fun ListMenu()

    @Composable
    fun GridMenu()

    @Composable
    fun MenuComponent()

    override fun onShortClick() = menuState.display { MenuComponent() }
}
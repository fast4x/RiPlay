package it.fast4x.riplay.ui.components.tab.toolbar

import androidx.compose.runtime.Composable
import it.fast4x.riplay.R
import it.fast4x.riplay.ui.components.navigation.header.TabToolBar
import it.fast4x.riplay.utils.colorPalette
import kotlin.random.Random

interface Randomizer<T>: Button {

    companion object {
        /**
         * To ensure true randomness, Random must not
         * be a constant variable
         */
        fun nextInt( until: Int ) =
            Random( System.currentTimeMillis() ).nextInt( until )
    }

    fun getItems(): List<T>

    fun onClick( index: Int )

    @Composable
    override fun ToolBarButton() {
        TabToolBar.Icon(
            iconId = R.drawable.dice,
            tint = colorPalette().text,
            enabled = getItems().isNotEmpty(),
            onClick = { onClick( nextInt( getItems().size ) ) }
        )
    }
}
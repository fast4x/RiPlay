package it.fast4x.riplay.ui.components.tab.toolbar

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.fast4x.riplay.LocalAppSettingsManager
import it.fast4x.riplay.R
import it.fast4x.riplay.enums.MenuStyle
import it.fast4x.riplay.ui.components.LocalGlobalSheetState
import it.fast4x.riplay.ui.components.GlobalSheetState
import it.fast4x.riplay.ui.components.themed.Menu

class EllipsisMenuComponent private constructor(
    private val buttons: () -> List<Button>,
    override val globalSheetState: GlobalSheetState,
    override val styleState: MutableState<MenuStyle>
) : Menu {

    companion object {
        @JvmStatic
        @Composable
        fun init( items: () -> List<Button> ): EllipsisMenuComponent {
            val appSettingsManager = LocalAppSettingsManager.current
            val appSettings = appSettingsManager.activeSettings.collectAsStateWithLifecycle().value
            return EllipsisMenuComponent(
                items,
                LocalGlobalSheetState.current,
                remember { mutableStateOf(appSettings.menuStyle)}
            )
        }
    }

    var style: MenuStyle = styleState.value
        set(value) {
            styleState.value = value
            field = value
        }
    override val iconId: Int = R.drawable.ellipsis_horizontal

    @Composable
    override fun ListMenu() {
        Menu(
            Modifier
                .fillMaxHeight(0.4f)
                .onPlaced { it.size.height.dp * 0.5f }
        ) {
            buttons().forEach {
                if( it is MenuIcon)
                    it.ListMenuItem()
            }
        }
    }

    @Composable
    override fun GridMenu() {
        it.fast4x.riplay.ui.components.themed.GridMenu(
            contentPadding = PaddingValues(
                start = 8.dp,
                top = 8.dp,
                end = 8.dp,
                bottom = 8.dp + WindowInsets.systemBars.asPaddingValues()
                    .calculateBottomPadding()
            )
        ) {
            items( buttons(), Button::hashCode ) {
                if( it is MenuIcon)
                    it.GridMenuItem()
            }
        }
    }

    @Composable
    override fun MenuComponent() {
        if( style == MenuStyle.Grid )
            GridMenu()
        else
            ListMenu()
    }
}
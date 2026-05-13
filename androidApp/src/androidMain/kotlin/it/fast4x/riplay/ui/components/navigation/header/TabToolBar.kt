package it.fast4x.riplay.ui.components.navigation.header

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import it.fast4x.riplay.ui.components.ActionPillButton
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.ui.components.tab.toolbar.Button
import it.fast4x.riplay.ui.components.tab.toolbar.EllipsisMenuComponent

object TabToolBar {

    val TOOLBAR_ICON_SIZE = 22.dp
    val HORIZONTAL_PADDING = 12.dp
    val VERTICAL_PADDING = 4.dp

    @Composable
    fun Buttons(buttons: List<Button>) {
        val configuration = LocalConfiguration.current
        val availableWidth = configuration.screenWidthDp.dp - (HORIZONTAL_PADDING * 2)
        val canDisplay = (availableWidth / TOOLBAR_ICON_SIZE).toInt()
        val isClustered by rememberSaveable(canDisplay) {
            mutableStateOf(buttons.size > canDisplay)
        }

        val ellipsisMenu = EllipsisMenuComponent.init {
            buttons.takeLast((buttons.size - canDisplay + 1).coerceAtLeast(0))
        }

        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(HORIZONTAL_PADDING, VERTICAL_PADDING)
        ) {
            val visibleButtons = if (isClustered)
                buttons.take(canDisplay - 1)
            else
                buttons

            visibleButtons.forEach { it.ToolBarButton() }

            if (isClustered)
                ellipsisMenu.ToolBarButton()
        }
    }

    @Composable
    fun Buttons(vararg buttons: Button) = Buttons(listOf(*buttons))

    /**
     * Icona base della toolbar.
     * [onLongClick] è opzionale: se non passato, il long press è disabilitato.
     */
    @Composable
    fun Icon(
        icon: Painter,
        tint: Color = colorPalette().text,
        size: Dp = TOOLBAR_ICON_SIZE,
        enabled: Boolean = true,
        active: Boolean = false,
        activeColor: Color = Color.Unspecified,
        modifier: Modifier = Modifier,
        onClick: () -> Unit,
        onLongClick: (() -> Unit)? = null
    ) {
        ActionPillButton(
            modifier = modifier,
            icon = icon,
            iconSize = size,
            enabled = enabled,
            active = active,
            activeColor = activeColor,
            tint = tint,
            onClick = onClick,
            onLongClick = onLongClick
        )
    }

    /**
     * Overload con resource ID invece di Painter.
     */
    @Composable
    fun Icon(
        @DrawableRes iconId: Int,
        tint: Color = colorPalette().text,
        size: Dp = TOOLBAR_ICON_SIZE,
        enabled: Boolean = true,
        active: Boolean = false,
        activeColor: Color = Color.Unspecified,
        modifier: Modifier = Modifier,
        onClick: () -> Unit,
        onLongClick: (() -> Unit)? = null
    ) = Icon(
        icon = painterResource(iconId),
        tint = tint,
        size = size,
        enabled = enabled,
        active = active,
        activeColor = activeColor,
        modifier = modifier,
        onClick = onClick,
        onLongClick = onLongClick
    )

    /**
     * Icona toggleable: cambia icona in base a [toggleCondition].
     */
    @Composable
    fun Toggleable(
        @DrawableRes onIconId: Int,
        @DrawableRes offIconId: Int,
        toggleCondition: Boolean,
        tint: Color = colorPalette().text,
        size: Dp = TOOLBAR_ICON_SIZE,
        enabled: Boolean = true,
        modifier: Modifier = Modifier,
        onClick: () -> Unit,
        onLongClick: (() -> Unit)? = null
    ) = Icon(
        iconId = if (toggleCondition) onIconId else offIconId,
        tint = tint,
        size = size,
        enabled = enabled,
        modifier = modifier,
        onClick = onClick,
        onLongClick = onLongClick
    )

    /**
     * Icona toggleable: cambia tint in base a [toggleCondition].
     */
    @Composable
    fun Toggleable(
        @DrawableRes iconId: Int,
        tintOn: Color = colorPalette().text,
        tintOff: Color = colorPalette().textDisabled,
        toggleCondition: Boolean,
        enabled: Boolean = true,
        size: Dp = TOOLBAR_ICON_SIZE,
        modifier: Modifier = Modifier,
        onClick: () -> Unit,
        onLongClick: (() -> Unit)? = null
    ) = Icon(
        iconId = iconId,
        tint = if (toggleCondition) tintOn else tintOff,
        size = size,
        enabled = enabled,
        modifier = modifier,
        onClick = onClick,
        onLongClick = onLongClick
    )
}
package it.fast4x.riplay.ui.components.tab

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.media3.common.util.UnstableApi
import it.fast4x.riplay.R
import it.fast4x.riplay.ui.components.tab.toolbar.Descriptive
import it.fast4x.riplay.ui.components.tab.toolbar.MenuIcon

@UnstableApi
class ToolbarMenuButton private constructor(
    private var iconX: Int,
    private var titleX: Int,
    private var onClick: () -> Unit = {}
): MenuIcon, Descriptive {

    companion object {
        @JvmStatic
        @Composable
        fun init(
            iconId: Int = R.drawable.horizontal_bold_line,
            titleId: Int = R.string.unknown_title,
            onClick: () -> Unit = {}
        ) =
            ToolbarMenuButton(
                iconX = iconId,
                titleX = titleId,
                onClick = onClick
            )
    }

    override val iconId: Int = iconX
    override val messageId: Int = titleX
    override val menuIconTitle: String
        @Composable
        get() = stringResource( messageId )

    override fun onShortClick() = onClick()

}
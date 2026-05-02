package it.fast4x.riplay.ui.components.themed

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.res.stringResource
import it.fast4x.riplay.R
import it.fast4x.riplay.ui.components.GlobalSheetState
import it.fast4x.riplay.ui.components.tab.toolbar.ConfirmDialog
import it.fast4x.riplay.ui.components.tab.toolbar.Descriptive
import it.fast4x.riplay.ui.components.tab.toolbar.MenuIcon

abstract class DeleteDialog protected constructor(
    protected val activeState: MutableState<Boolean>,
    protected val globalSheetState: GlobalSheetState
): ConfirmDialog, MenuIcon, Descriptive {

    override val iconId: Int = R.drawable.trash
    override val messageId: Int = R.string.delete
    override val menuIconTitle: String
        @Composable
        get() = stringResource( messageId )

    override var isActive: Boolean = activeState.value
        set(value) {
            activeState.value = value
            field = value
        }

    override fun onShortClick() = super.onShortClick()

    override fun onDismiss() {
        super.onDismiss()
        globalSheetState.hide()
    }
}
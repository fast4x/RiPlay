package it.fast4x.riplay.ui.components.tab.toolbar

import androidx.annotation.StringRes
import it.fast4x.riplay.ui.components.themed.SmartMessage
import it.fast4x.riplay.appContext

interface Descriptive: Clickable {

    @get:StringRes
    val messageId: Int

    /**
     * What happens when user holds this icon for a while.
     *
     * By default, this will send out message
     */
    override fun onLongClick() {
        SmartMessage(
            appContext().resources.getString( messageId ),
            context = appContext()
        )
    }
}
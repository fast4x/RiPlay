package it.fast4x.riplay.ui.components.themed

import android.content.Context
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.compose.ui.graphics.Color
import androidx.media3.common.util.UnstableApi
import es.dmoral.toasty.Toasty
import it.fast4x.riplay.enums.MessageType
import it.fast4x.riplay.enums.PopupType
import it.fast4x.riplay.utils.getEnum
import it.fast4x.riplay.utils.messageTypeKey
import it.fast4x.riplay.utils.preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
fun SmartMessage(
    message: String,
    type: PopupType? = PopupType.Info,
    backgroundColor: Color? = Color.DarkGray,
    durationLong: Boolean = false,
    context: Context,
) {
    CoroutineScope(Dispatchers.Main).launch {
        val length = if (durationLong) Toast.LENGTH_LONG else Toast.LENGTH_SHORT

        if (context.preferences.getEnum(messageTypeKey, MessageType.Modern) == MessageType.Modern) {
            when (type) {
                PopupType.Info -> Toasty.info(context, message, length, true).show()
                PopupType.Success -> Toasty.success(context, message, length, true).show()
                PopupType.Error -> Toasty.error(context, message, length, true).show()
                PopupType.Warning -> Toasty.warning(context, message, length, true).show()
                null -> Toasty.normal(context, message, length).show()
            }

        } else
        //if (durationLong == true) context.toastLong(message) else context.toast(message)
        Toasty.normal(context, message, length).show()
    }
}


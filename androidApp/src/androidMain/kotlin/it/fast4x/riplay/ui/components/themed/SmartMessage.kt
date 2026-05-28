package it.fast4x.riplay.ui.components.themed

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.compose.ui.graphics.Color
import androidx.media3.common.util.UnstableApi
import es.dmoral.toasty.Toasty
import it.fast4x.riplay.enums.MessageType
import it.fast4x.riplay.enums.PopupType
import it.fast4x.riplay.extensions.preferences.getEnum
import it.fast4x.riplay.extensions.preferences.PreferenceKey.MESSAGE_TYPE
import it.fast4x.riplay.extensions.preferences.preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicReference

private val currentToast = AtomicReference<Toast?>(null)

@OptIn(UnstableApi::class)
fun SmartMessage(
    message: String,
    type: PopupType? = PopupType.Info,
    backgroundColor: Color? = Color.DarkGray,
    durationLong: Boolean = false,
    context: Context,
) {
    val length = if (durationLong) Toast.LENGTH_LONG else Toast.LENGTH_SHORT

    // Handler diretto: zero overhead, esecuzione immediata sul Main thread
    Handler(Looper.getMainLooper()).post {

        // Cancella il toast in coda prima di mostrarne uno nuovo
        currentToast.getAndSet(null)?.cancel()

        val toast = if (context.preferences.getEnum(
                MESSAGE_TYPE.key, MessageType.Modern
            ) == MessageType.Modern
        ) {
            when (type) {
                PopupType.Info    -> Toasty.info(context, message, length, true)
                PopupType.Success -> Toasty.success(context, message, length, true)
                PopupType.Error   -> Toasty.error(context, message, length, true)
                PopupType.Warning -> Toasty.warning(context, message, length, true)
                null              -> Toasty.normal(context, message, length)
            }
        } else {
            Toasty.normal(context, message, length)
        }

        currentToast.set(toast)
        toast.show()
    }
}


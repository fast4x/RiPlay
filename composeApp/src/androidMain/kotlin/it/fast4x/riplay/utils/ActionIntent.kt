package it.fast4x.riplay.utils

import android.app.PendingIntent
import android.content.Intent
import it.fast4x.riplay.appContext

@JvmInline
value class ActionIntent(val value: String) {
    val pendingIntent: PendingIntent
        get() = PendingIntent.getBroadcast(
            appContext(),
            100,
            Intent(value).setPackage(appContext().packageName),
            PendingIntent.FLAG_UPDATE_CURRENT.or(if (isAtLeastAndroid6) PendingIntent.FLAG_IMMUTABLE else 0)
        )
}
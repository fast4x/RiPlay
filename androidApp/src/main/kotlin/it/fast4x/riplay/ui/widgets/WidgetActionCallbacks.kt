package it.fast4x.riplay.ui.widgets

import android.content.Context
import android.content.Intent
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import it.fast4x.riplay.services.playback.PlayerService
import timber.log.Timber

/**
 * Azioni dei widget RiPlay.
 *
 * Fino a oggi i widget chiamavano direttamente startService: funziona, ma
 * bypassa completamente il LegacyActionReceiver del PlayerService, quindi
 * le azioni play/pause non venivano instradate al cast RiTune attivo e le
 * azioni extra (like, shuffle) non erano disponibili.
 *
 * Nuova strategia:
 *  1. se il PlayerService è attivo ([PlayerService.isServiceReady]) l'azione
 *     viaggia come broadcast verso il LegacyActionReceiver, che applica
 *     esattamente la stessa logica della notifica (compreso il cast);
 *  2. se il servizio non è attivo, per i comandi di trasporto (play, pause,
 *     next, previous) si avvia il servizio con l'azione, ripristinando la
 *     coda persistente;
 *  3. le azioni contestuali (like, shuffle) senza servizio attivo vengono
 *     ignorate: non c'è alcun brano su cui agire.
 */
object WidgetActions {

    fun send(context: Context, action: String, transportFallback: Boolean) {
        if (PlayerService.isServiceReady.value) {
            // Il receiver dinamico del PlayerService gestisce broadcast di questo tipo
            runCatching {
                context.sendBroadcast(Intent(action).setPackage(context.packageName))
            }.onFailure { Timber.w(it, "WidgetActions: broadcast non inviato per $action") }
        } else if (transportFallback) {
            runCatching {
                context.startService(
                    Intent(context, PlayerService::class.java).apply { this.action = action }
                )
            }.onFailure { Timber.w(it, "WidgetActions: avvio servizio fallito per $action") }
        } else {
            Timber.d("WidgetActions: azione $action ignorata, PlayerService non attivo")
        }
    }
}

class PlayAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        WidgetActions.send(context, PlayerService.Action.play.value, transportFallback = true)
    }
}

class PauseAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        WidgetActions.send(context, PlayerService.Action.pause.value, transportFallback = true)
    }
}

class NextAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        WidgetActions.send(context, PlayerService.Action.next.value, transportFallback = true)
    }
}

class PreviousAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        WidgetActions.send(context, PlayerService.Action.previous.value, transportFallback = true)
    }
}

class ToggleLikeAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        WidgetActions.send(context, PlayerService.Action.like.value, transportFallback = false)
    }
}

class ToggleShuffleAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        WidgetActions.send(context, PlayerService.Action.shuffle.value, transportFallback = false)
    }
}

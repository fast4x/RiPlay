package it.fast4x.riplay.ui.widgets

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.annotation.OptIn
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.media3.common.util.UnstableApi
import it.fast4x.riplay.services.playback.PlayerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlayAction : ActionCallback {
    @OptIn(UnstableApi::class)
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {

        //performOptimisticUpdate(context, isPlaying = true) // Aggiorna UI

        val intent = Intent(context, PlayerService::class.java).apply {
            action = PlayerService.Action.play.value
        }
        context.startService(intent)
    }

}

class PauseAction : ActionCallback {
    @OptIn(UnstableApi::class)
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {

        //performOptimisticUpdate(context, isPlaying = false) // Aggiorna UI

        val intent = Intent(context, PlayerService::class.java).apply {
            action = PlayerService.Action.pause.value
        }
        context.startService(intent)
    }

}

class NextAction : ActionCallback {
    @OptIn(UnstableApi::class)
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val intent = Intent(context, PlayerService::class.java).apply {
            action = PlayerService.Action.next.value
        }
        context.startService(intent)
    }
}

class PreviousAction : ActionCallback {
    @OptIn(UnstableApi::class)
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val intent = Intent(context, PlayerService::class.java).apply {
            action = PlayerService.Action.previous.value
        }
        context.startService(intent)
    }
}

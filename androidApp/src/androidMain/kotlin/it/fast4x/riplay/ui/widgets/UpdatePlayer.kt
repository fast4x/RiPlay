package it.fast4x.riplay.ui.widgets

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@UnstableApi
// Aggiungi 'inline' e '<reified T : GlanceAppWidget>'
suspend inline fun <reified T : GlanceAppWidget> T.updateState(
    context: Context,
    title: String,
    artist: String,
    isPlaying: Boolean,
    artworkBase64: String?
) {
    withContext(Dispatchers.IO) {
        val glanceIds = GlanceAppWidgetManager(context).getGlanceIds(T::class.java)

        for (glanceId in glanceIds) {
            updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
                prefs.toMutablePreferences().apply {
                    this[stringPreferencesKey("title")] = title
                    this[stringPreferencesKey("artist")] = artist
                    this[booleanPreferencesKey("isPlaying")] = isPlaying
                    this[stringPreferencesKey("artworkBase64")] = artworkBase64 ?: ""
                }
            }
            update(context, glanceId)
        }
    }
}
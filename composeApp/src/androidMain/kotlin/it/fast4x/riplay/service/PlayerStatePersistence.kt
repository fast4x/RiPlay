package it.fast4x.riplay.service

import android.content.Context
import it.fast4x.riplay.extensions.preferences.preferences
import it.fast4x.riplay.extensions.preferences.stateIsPlaying
import it.fast4x.riplay.extensions.preferences.stateMediaIdKey
import it.fast4x.riplay.extensions.preferences.statePositionKey
import androidx.core.content.edit
import timber.log.Timber

class PlayerStatePersistence(context: Context) {

    val prefs = context.preferences

    fun saveState(mediaId: String, position: Long, isPlaying: Boolean) {
        prefs.edit().apply {
            putString(stateMediaIdKey, mediaId)
            putLong(statePositionKey, position)
            putBoolean(stateIsPlaying, isPlaying)
            apply()
        }
        Timber.d("PlayerService > PlayerStatePersistence saveState mediaId $mediaId position $position isPlaying $isPlaying")
    }

    fun clearState() {
        prefs.edit { clear() }
    }

    fun getSavedMediaId(): String? = prefs.getString(stateMediaIdKey, null)
    fun getSavedPosition(): Long = prefs.getLong(statePositionKey, 0L)
    fun getSavedIsPlaying(): Boolean = prefs.getBoolean(stateIsPlaying, false)
}
package it.fast4x.riplay.services.playback

import android.content.Context
import it.fast4x.riplay.extensions.preferences.preferences
import it.fast4x.riplay.extensions.preferences.stateDurationKey
import it.fast4x.riplay.extensions.preferences.stateIsPlayingKey
import it.fast4x.riplay.extensions.preferences.stateMediaIdKey
import it.fast4x.riplay.extensions.preferences.statePositionKey
import timber.log.Timber

class PlayerStatePersistence(context: Context) {

    val prefs = context.preferences

    fun saveState(mediaId: String, position: Long, duration: Long, isPlaying: Boolean) {
        prefs.edit().apply {
            putString(stateMediaIdKey, mediaId)
            putLong(statePositionKey, position)
            putLong(stateDurationKey, duration)
            putBoolean(stateIsPlayingKey, isPlaying)
            apply()
        }
        Timber.d("PlayerService > PlayerStatePersistence saveState mediaId $mediaId position $position isPlaying $isPlaying")
    }

    fun clearState() {
        prefs.edit().apply {
            putString(stateMediaIdKey, "")
            putLong(statePositionKey, 0)
            putBoolean(stateIsPlayingKey, false)
            apply()
        }
    }

    fun getSavedMediaId(): String? = prefs.getString(stateMediaIdKey, null)
    fun getSavedPosition(): Long = prefs.getLong(statePositionKey, 0L)
    fun getSavedDuration(): Long = prefs.getLong(stateDurationKey, 0L)
    fun getSavedIsPlaying(): Boolean = prefs.getBoolean(stateIsPlayingKey, false)
}
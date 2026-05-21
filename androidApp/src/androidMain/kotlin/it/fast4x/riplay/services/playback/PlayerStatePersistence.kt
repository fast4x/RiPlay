package it.fast4x.riplay.services.playback

import android.content.Context
import it.fast4x.riplay.extensions.preferences.preferences
import it.fast4x.riplay.extensions.preferences.PreferenceKey.STATE_DURATION
import it.fast4x.riplay.extensions.preferences.PreferenceKey.STATE_IS_PLAYING
import it.fast4x.riplay.extensions.preferences.PreferenceKey.STATE_MEDIA_ID
import it.fast4x.riplay.extensions.preferences.PreferenceKey.STATE_POSITION
import timber.log.Timber

class PlayerStatePersistence(context: Context) {

    val prefs = context.preferences

    fun saveState(mediaId: String, position: Long, duration: Long, isPlaying: Boolean) {
        prefs.edit().apply {
            putString(STATE_MEDIA_ID.key, mediaId)
            putLong(STATE_POSITION.key, position)
            putLong(STATE_DURATION.key, duration)
            putBoolean(STATE_IS_PLAYING.key, isPlaying)
            apply()
        }
        Timber.d("PlayerService > PlayerStatePersistence saveState mediaId $mediaId position $position isPlaying $isPlaying")
    }

    fun clearState() {
        prefs.edit().apply {
            putString(STATE_MEDIA_ID.key, "")
            putLong(STATE_POSITION.key, 0)
            putBoolean(STATE_IS_PLAYING.key, false)
            apply()
        }
    }

    fun getSavedMediaId(): String? = prefs.getString(STATE_MEDIA_ID.key, null)
    fun getSavedPosition(): Long = prefs.getLong(STATE_POSITION.key, 0L)
    fun getSavedDuration(): Long = prefs.getLong(STATE_DURATION.key, 0L)
    fun getSavedIsPlaying(): Boolean = prefs.getBoolean(STATE_IS_PLAYING.key, false)
}
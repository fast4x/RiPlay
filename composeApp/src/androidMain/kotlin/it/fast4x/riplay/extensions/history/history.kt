package it.fast4x.riplay.extensions.history

import androidx.media3.common.MediaItem
import it.fast4x.environment.EnvironmentExt
import it.fast4x.environment.models.Context
import it.fast4x.environment.models.PlayerResponse
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.utils.globalContext
import it.fast4x.riplay.extensions.preferences.pauseListenHistoryKey
import it.fast4x.riplay.extensions.preferences.preferences
import it.fast4x.riplay.data.models.Format
import it.fast4x.riplay.extensions.players.getOnlineMetadata
import it.fast4x.riplay.utils.isLocal
import it.fast4x.riplay.ui.screens.settings.isYtSyncEnabled
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber


fun updateOnlineHistory(mediaItem: MediaItem) {
    if (globalContext().preferences.getBoolean(pauseListenHistoryKey, false)) return

    Timber.d("UpdateOnlineHistory called with mediaItem $mediaItem")

    if (!mediaItem.isLocal && isYtSyncEnabled()) {
        CoroutineScope(Dispatchers.IO).launch {
            val playbackUrl = Database.format(mediaItem.mediaId).first()?.playbackUrl
                ?: getOnlineMetadata(mediaItem.mediaId)
                    ?.playbackTracking?.videostatsPlaybackUrl?.baseUrl

                playbackUrl?.let { playbackUrl ->
                    Timber.d("UpdateOnlineHistory upsert playbackUrl in database")
                    Database.upsert(Format(songId = mediaItem.mediaId, playbackUrl = playbackUrl))

                    Timber.d("UpdateOnlineHistory addPlaybackToHistory playbackUrl $playbackUrl")
                    EnvironmentExt.addPlaybackToHistory(null, playbackUrl)
                        .onFailure {
                            Timber.e("UpdateOnlineHistory addPlaybackToHistory ${it.stackTraceToString()}")
                        }
                }
        }
    }
}
package it.fast4x.riplay.extensions.history

import androidx.media3.common.MediaItem
import it.fast4x.environment.EnvironmentExt
import it.fast4x.environment.models.Context
import it.fast4x.environment.models.PlayerResponse
import it.fast4x.riplay.Database
import it.fast4x.riplay.context
import it.fast4x.riplay.extensions.preferences.pauseListenHistoryKey
import it.fast4x.riplay.extensions.preferences.preferences
import it.fast4x.riplay.models.Format
import it.fast4x.riplay.service.isLocal
import it.fast4x.riplay.ui.screens.settings.isSyncEnabled
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * The main client is used for metadata and initial streams.
 * Do not use other clients for this because it can result in inconsistent metadata.
 * For example other clients can have different normalization targets (loudnessDb).
 *
 * should be preferred here because currently it is the only client which provides:
 * - the correct metadata (like loudnessDb)
 * - premium formats
 */
private val MAIN_CLIENT: Context.Client = Context.DefaultWeb.client

/**
 * Simple player response intended to use for metadata only.
 * Stream URLs of this response might not work so don't use them.
 */
suspend fun getOnlineMetadata(
    videoId: String,
    playlistId: String? = null,
): Result<PlayerResponse> =
    EnvironmentExt.simplePlayer(videoId, playlistId, client = MAIN_CLIENT)


fun updateOnlineHistory(mediaItem: MediaItem) {
    if (context().preferences.getBoolean(pauseListenHistoryKey, false)) return

    Timber.d("UpdateOnlineHistory called with mediaItem $mediaItem")

    if (!mediaItem.isLocal && isSyncEnabled()) {
        CoroutineScope(Dispatchers.IO).launch {
            val playbackUrl = Database.format(mediaItem.mediaId).first()?.playbackUrl
                ?: getOnlineMetadata(mediaItem.mediaId)
                .getOrNull()?.playbackTracking?.videostatsPlaybackUrl?.baseUrl

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
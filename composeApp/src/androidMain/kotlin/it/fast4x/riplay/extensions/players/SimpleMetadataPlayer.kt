package it.fast4x.riplay.extensions.players

import it.fast4x.environment.EnvironmentExt
import it.fast4x.environment.models.Context
import it.fast4x.environment.models.PlayerResponse
import it.fast4x.environment.utils.NewPipeUtils
import timber.log.Timber

suspend fun getOnlineMetadata(
    videoId: String,
    playlistId: String? = null,
): PlayerResponse? {

    val signatureTimestamp = getSignatureTimestamp(videoId)

    val metaData = EnvironmentExt.simpleMetadataPlayer(
        videoId, playlistId,
        client = Context.DefaultWeb.client,
        signatureTimestamp = signatureTimestamp
    ).getOrNull()
    Timber.d("getOnlineMetadata $metaData")
    return metaData
}

private fun getSignatureTimestamp(
    videoId: String
): Int? {
    return NewPipeUtils.getSignatureTimestamp(videoId)
        .onFailure {
            Timber.e("SimplePlayer getSignatureTimestampOrNull Could not get signature timestamp: $videoId")
        }
        .getOrNull()
}
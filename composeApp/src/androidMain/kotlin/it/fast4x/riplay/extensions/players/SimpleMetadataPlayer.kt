package it.fast4x.riplay.extensions.players

import it.fast4x.environment.EnvironmentExt
import it.fast4x.environment.models.Context
import it.fast4x.environment.models.PlayerResponse
import timber.log.Timber


private val MAIN_CLIENT: Context.Client = Context.DefaultWeb.client

suspend fun getOnlineMetadata(
    videoId: String,
    playlistId: String? = null,
): PlayerResponse? {
    val metaData = EnvironmentExt.simpleMetadataPlayer(videoId, playlistId, client = MAIN_CLIENT).getOrNull()
    //Timber.d("getOnlineMetadata $metaData")
    return metaData
}

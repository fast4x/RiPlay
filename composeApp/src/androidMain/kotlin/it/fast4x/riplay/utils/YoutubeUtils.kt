package it.fast4x.riplay.utils

import it.fast4x.environment.models.PlayerResponse
import it.fast4x.environment.utils.NewPipeUtils
import timber.log.Timber

fun getSignatureTimestampOrNull(
    videoId: String
): Int? {
    return NewPipeUtils.getSignatureTimestamp(videoId)
        .onFailure {
            Timber.e("NewPipeUtils getSignatureTimestampOrNull Error while getting signature timestamp ${it.stackTraceToString()}")
            println("NewPipeUtils getSignatureTimestampOrNull Error while getting signature timestamp ${it.stackTraceToString()}")
        }
        .getOrNull()
}

fun getStreamUrl(
    format: PlayerResponse.StreamingData.Format,
    videoId: String
): String? {
    val streamUrl =  NewPipeUtils.getStreamUrl(format, videoId)
        .onFailure {
            Timber.e("NewPipe Utils getStreamUrlOrNull Error while getting stream url ${it.stackTraceToString()}")
            println("NewPipe Utils getStreamUrlOrNull Error while getting stream url ${it.stackTraceToString()}")
        }
        .getOrNull()

    println("NewPipe Utils getStreamUrlOrNull streamUrl $streamUrl")

    return streamUrl
}

data class InvalidHttpCodeException(val code: Int) :
    IllegalStateException("Invalid http code received: $code")


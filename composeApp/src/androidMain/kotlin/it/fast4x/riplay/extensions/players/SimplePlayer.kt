package it.fast4x.riplay.extensions.players

import android.os.Build
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import it.fast4x.environment.Environment
import it.fast4x.environment.EnvironmentExt
import it.fast4x.environment.models.Context
import it.fast4x.environment.models.Context.Companion.DefaultWeb3
import it.fast4x.environment.models.Context.Companion.TVHTML5_SIMPLY_EMBEDDED_PLAYER
import it.fast4x.environment.models.PlayerResponse
import it.fast4x.environment.utils.NewPipeUtils
import it.fast4x.riplay.enums.AudioQualityFormat
import it.fast4x.riplay.extensions.players.models.PlaybackData
import it.fast4x.riplay.extensions.webpotoken.PoTokenGenerator
import it.fast4x.riplay.extensions.webpotoken.PoTokenResult
import it.fast4x.riplay.isConnectionMetered
import it.fast4x.riplay.isConnectionMeteredEnabled
import it.fast4x.riplay.models.Format
import okhttp3.OkHttpClient
import timber.log.Timber

object SimplePlayer {
    private val httpClient = OkHttpClient.Builder()
        .proxy(Environment.proxy)
        .build()

    private val poTokenGenerator = PoTokenGenerator()

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
     * Clients used for fallback streams in case the streams of the main client do not work.
     */
    private val STREAM_FALLBACK_CLIENTS: Array<Context.Client> = arrayOf(
        TVHTML5_SIMPLY_EMBEDDED_PLAYER.client,
        DefaultWeb3.client,
    )



    /**
     * Custom player response intended to use for playback.
     * Metadata like audioConfig and videoDetails are from [MAIN_CLIENT].
     * Format & stream can be from [MAIN_CLIENT] or [STREAM_FALLBACK_CLIENTS].
     */
    @OptIn(UnstableApi::class)
    suspend fun playerResponseForPlayback(
        videoId: String,
        playlistId: String? = null,
        playedFormat: Format?,
        audioQuality: AudioQualityFormat,
    ): Result<PlaybackData> = runCatching {
        Timber.d("SimplePlayer playerResponseForPlayback: $videoId")
        println("SimplePlayer playerResponseForPlayback: $videoId")
        /**
         * This is required for some clients to get working streams however
         * it should not be forced for the [MAIN_CLIENT] because the response of the [MAIN_CLIENT]
         * is required even if the streams won't work from this client.
         * This is why it is allowed to be null.
         */
        val signatureTimestamp = getSignatureTimestampOrNull(videoId)
        val mainPlayerResponse =
            EnvironmentExt.simplePlayer(videoId, playlistId, MAIN_CLIENT, signatureTimestamp).getOrThrow()
        val audioConfig = mainPlayerResponse.playerConfig?.audioConfig
        val videoDetails = mainPlayerResponse.videoDetails
        val playbackTracking = mainPlayerResponse.playbackTracking
        var format: PlayerResponse.StreamingData.Format? = null
        var streamUrl: String? = null
        var streamExpiresInSeconds: Int? = null
        var streamPlayerResponse: PlayerResponse? = null
        for (clientIndex in (-1 until STREAM_FALLBACK_CLIENTS.size)) {
            // reset for each client
            format = null
            streamUrl = null
            streamExpiresInSeconds = null
            // decide which client to use
            if (clientIndex == -1) {
                // try with streams from main client first
                streamPlayerResponse = mainPlayerResponse
            } else {
                // after main client use fallback clients
                val client = STREAM_FALLBACK_CLIENTS[clientIndex]
                if (client.loginRequired && Environment.cookie == null) {
                    // skip client if it requires login but user is not logged in
                    continue
                }
                streamPlayerResponse =
                    EnvironmentExt.simplePlayer(videoId, playlistId, client, signatureTimestamp).getOrNull()
            }
            // process current client response
            if (streamPlayerResponse?.playabilityStatus?.status == "OK") {
                format =
                    findFormat(
                        streamPlayerResponse,
                        playedFormat,
                        audioQuality,
                        //connectivityManager,
                    ) ?: continue
                streamUrl = findUrlOrNull(format, videoId) ?: continue
                streamExpiresInSeconds =
                    streamPlayerResponse.streamingData?.expiresInSeconds ?: continue
                if (clientIndex == STREAM_FALLBACK_CLIENTS.size - 1) {
                    /** skip [validateStatus] for last client */
                    break
                }
                if (validateStatus(streamUrl)) {
                    // working stream found
                    break
                }
            }
        }
        if (streamPlayerResponse == null) {
            throw Exception("SimplePlayer playerResponseForPlayback Bad stream player response")
        }
        if (streamPlayerResponse.playabilityStatus?.status != "OK") {
            throw PlaybackException(
                streamPlayerResponse.playabilityStatus?.reason,
                null,
                PlaybackException.ERROR_CODE_REMOTE_ERROR
            )
        }
        if (streamExpiresInSeconds == null) {
            throw Exception("SimplePlayer playerResponseForPlayback Missing stream expire time")
        }
        if (format == null) {
            throw Exception("SimplePlayer playerResponseForPlayback Could not find format")
        }
        if (streamUrl == null) {
            throw Exception("SimplePlayer playerResponseForPlayback Could not find stream url")
        }
        PlaybackData(
            audioConfig,
            videoDetails,
            playbackTracking,
            format,
            streamUrl,
            streamExpiresInSeconds,
        )
    }

    @OptIn(UnstableApi::class)
    suspend fun playerResponseForPlaybackWithPotoken(
        videoId: String,
        playlistId: String? = null,
        playedFormat: Format?,
        audioQuality: AudioQualityFormat,
    ): Result<PlaybackData> = runCatching {
        Timber.d("SimplePlayer playerResponseForPlaybackWithPotoken: $videoId")
        println("SimplePlayer playerResponseForPlaybackWithPotoken: $videoId")
        /**
         * This approach is alternative and experimental
         */
        val mainPlayerResponse =
            EnvironmentExt.simplePlayerWithPotoken(videoId, playlistId).getOrThrow()
        val audioConfig = mainPlayerResponse.second?.playerConfig?.audioConfig
        val videoDetails = mainPlayerResponse.second?.videoDetails
        val playbackTracking = mainPlayerResponse.second?.playbackTracking
        var format: PlayerResponse.StreamingData.Format? = null
        var streamUrl: String? = null
        var streamExpiresInSeconds: Int? = null
        val streamPlayerResponse: PlayerResponse? = mainPlayerResponse.second
        //val cpn = mainPlayerResponse.first

            // process current client response
        if (streamPlayerResponse?.playabilityStatus?.status == "OK") {
            format =
                findFormat(
                    streamPlayerResponse,
                    playedFormat,
                    audioQuality,
                )

            streamUrl = format?.let { findUrlOrNull(
                // maybe cpn isn't mandatory
//                if (cpn != null) it.copy(url = it.url.plus("&cpn=${cpn}"))
//                else it,
                  it, videoId

            ) }

            streamExpiresInSeconds =
                streamPlayerResponse.streamingData?.expiresInSeconds

        }

        if (streamPlayerResponse == null) {
            throw Exception("playerResponseForPlaybackWithPotoken Bad stream player response")
        }
        if (streamPlayerResponse.playabilityStatus?.status != "OK") {
            throw PlaybackException(
                streamPlayerResponse.playabilityStatus?.reason,
                null,
                PlaybackException.ERROR_CODE_REMOTE_ERROR
            )
        }
        if (streamExpiresInSeconds == null) {
            throw Exception("playerResponseForPlaybackWithPotoken Missing stream expire time")
        }
        if (format == null) {
            throw Exception("playerResponseForPlaybackWithPotoken Could not find format")
        }
        if (streamUrl == null) {
            throw Exception("playerResponseForPlaybackWithPotoken Could not find stream url")
        }
        PlaybackData(
            audioConfig,
            videoDetails,
            playbackTracking,
            format,
            streamUrl,
            streamExpiresInSeconds,
        )
    }

    /**
     * Custom player response intended to use for playback.
     * Metadata like audioConfig and videoDetails are from [MAIN_CLIENT].
     * Format & stream can be from [MAIN_CLIENT] or [STREAM_FALLBACK_CLIENTS].
     */
    @RequiresApi(Build.VERSION_CODES.O)
    @OptIn(UnstableApi::class)
    suspend fun playerResponseForPlaybackWithWebPotoken(
        videoId: String,
        playlistId: String? = null,
        playedFormat: Format?,
        audioQuality: AudioQualityFormat,
    ): Result<PlaybackData> = runCatching {
        Timber.d("SimplePlayer playerResponseForPlaybackWithWebPotoken: $videoId")
        println("SimplePlayer playerResponseForPlaybackWithWebPotoken: $videoId")
        /**
         * This is required for some clients to get working streams however
         * it should not be forced for the [MAIN_CLIENT] because the response of the [MAIN_CLIENT]
         * is required even if the streams won't work from this client.
         * This is why it is allowed to be null.
         */
        val signatureTimestamp = getSignatureTimestampOrNull(videoId)

        val isLoggedIn = Environment.cookie != null
        val sessionId =
            if (isLoggedIn) {
                // signed in sessions use dataSyncId as identifier
                Environment.dataSyncId
            } else {
                // signed out sessions use visitorData as identifier
                Environment.visitorData
            }
        Timber.d("SimplePlayer playerResponseForPlaybackWithWebPotoken [$videoId] signatureTimestamp: $signatureTimestamp, isLoggedIn: $isLoggedIn")
        println("SimplePlayer playerResponseForPlaybackWithWebPotoken [$videoId] signatureTimestamp: $signatureTimestamp, isLoggedIn: $isLoggedIn")

        val (webPlayerPot, webStreamingPot) = getWebClientPoTokenOrNull(videoId, sessionId)?.let {
            Pair(it.playerRequestPoToken, it.streamingDataPoToken)
        } ?: Pair(null, null).also {
            Timber.d("SimplePlayer playerResponseForPlaybackWithWebPotoken [$videoId] No po token")
            println("SimplePlayer playerResponseForPlaybackWithWebPotoken [$videoId] No po token")
        }

        val mainPlayerResponse =
            EnvironmentExt.simplePlayer(videoId, playlistId, MAIN_CLIENT, signatureTimestamp, webPlayerPot).getOrThrow()

        val audioConfig = mainPlayerResponse.playerConfig?.audioConfig
        val videoDetails = mainPlayerResponse.videoDetails
        val playbackTracking = mainPlayerResponse.playbackTracking
        var format: PlayerResponse.StreamingData.Format? = null
        var streamUrl: String? = null
        var streamExpiresInSeconds: Int? = null
        var streamPlayerResponse: PlayerResponse? = null
        var client: Context.Client
        for (clientIndex in (-1 until STREAM_FALLBACK_CLIENTS.size)) {
            // reset for each client
            format = null
            streamUrl = null
            streamExpiresInSeconds = null
            // decide which client to use
            if (clientIndex == -1) {
                // try with streams from main client first
                client = MAIN_CLIENT
                streamPlayerResponse = mainPlayerResponse
            } else {
                // after main client use fallback clients
                client = STREAM_FALLBACK_CLIENTS[clientIndex]
                if (client.loginRequired && !isLoggedIn) {
                    // skip client if it requires login but user is not logged in
                    continue
                }
                streamPlayerResponse =
                    EnvironmentExt.simplePlayer(videoId, playlistId, client, signatureTimestamp, webPlayerPot).getOrNull()
            }
            // process current client response
            if (streamPlayerResponse?.playabilityStatus?.status == "OK") {
                format =
                    findFormat(
                        streamPlayerResponse,
                        playedFormat,
                        audioQuality,
                        //connectivityManager,
                    ) ?: continue

                streamUrl = findUrlOrNull(format, videoId) ?: continue
                streamExpiresInSeconds =
                    streamPlayerResponse.streamingData?.expiresInSeconds ?: continue

                if (client.useWebPoTokens && webStreamingPot != null) {
                    streamUrl += "&pot=$webStreamingPot";
                }

                if (clientIndex == STREAM_FALLBACK_CLIENTS.size - 1) {
                    /** skip [validateStatus] for last client */
                    break
                }
                if (validateStatus(streamUrl)) {
                    // working stream found
                    break
                }
            }
        }
        if (streamPlayerResponse == null) {
            throw Exception("SimplePlayer playerResponseForPlayback Bad stream player response")
        }
        if (streamPlayerResponse.playabilityStatus?.status != "OK") {
            throw PlaybackException(
                streamPlayerResponse.playabilityStatus?.reason,
                null,
                PlaybackException.ERROR_CODE_REMOTE_ERROR
            )
        }
        if (streamExpiresInSeconds == null) {
            throw Exception("SimplePlayer playerResponseForPlayback Missing stream expire time")
        }
        if (format == null) {
            throw Exception("SimplePlayer playerResponseForPlayback Could not find format")
        }
        if (streamUrl == null) {
            throw Exception("SimplePlayer playerResponseForPlayback Could not find stream url")
        }
        PlaybackData(
            audioConfig,
            videoDetails,
            playbackTracking,
            format,
            streamUrl,
            streamExpiresInSeconds,
        )
    }


    /**
     * Simple player response intended to use for metadata only.
     * Stream URLs of this response might not work so don't use them.
     */
    suspend fun playerResponseForMetadata(
        videoId: String,
        playlistId: String? = null,
    ): Result<PlayerResponse> =
        EnvironmentExt.simplePlayer(videoId, playlistId, client = MAIN_CLIENT)

    private fun findFormat(
        playerResponse: PlayerResponse,
        playedFormat: Format?,
        audioQuality: AudioQualityFormat,
    ): PlayerResponse.StreamingData.Format? {
        val formats = playerResponse.streamingData?.adaptiveFormats?.filter { it.mimeType.startsWith("video/") }?.map { it.itag }
        println("SimplePlayer findFormat: $formats")

        return if (playedFormat != null) {
            playerResponse.streamingData?.adaptiveFormats?.find { it.itag == playedFormat.itag }
        } else {
            playerResponse.streamingData?.adaptiveFormats
                //?.filter { it.isAudio }
                ?.filter { it.isVideo }
                ?.maxByOrNull {
                    it.bitrate *
                            when (audioQuality) {
                                AudioQualityFormat.Auto -> if (isConnectionMeteredEnabled() && isConnectionMetered()) -1 else 2
                                AudioQualityFormat.High -> 1
                                AudioQualityFormat.Medium -> -1
                                AudioQualityFormat.Low -> -2
                            } + (if (it.mimeType.startsWith("video/webm")) 10240 else 0) // prefer opus stream
                }
        }

    }

    /**
     * Checks if the stream url returns a successful status.
     * If this returns true the url is likely to work.
     * If this returns false the url might cause an error during playback.
     */
    private fun validateStatus(url: String): Boolean {
        try {
            val requestBuilder = okhttp3.Request.Builder()
                .head()
                .url(url)
            val response = httpClient.newCall(requestBuilder.build()).execute()
            return response.isSuccessful
        } catch (e: Exception) {
            Timber.e("SimplePlayer validateStatus Could not validate stream url: $url")
            println("SimplePlayer validateStatus Could not validate stream url: $url")
        }
        return false
    }

    /**
     * Wrapper around the [NewPipeUtils.getSignatureTimestamp] function which reports exceptions
     */
    private fun getSignatureTimestampOrNull(
        videoId: String
    ): Int? {
        return NewPipeUtils.getSignatureTimestamp(videoId)
            .onFailure {
                Timber.e("SimplePlayer getSignatureTimestampOrNull Could not get signature timestamp: $videoId")
                println("SimplePlayer getSignatureTimestampOrNull Could not get signature timestamp: $videoId")
            }
            .getOrNull()
    }

    /**
     * Wrapper around the [NewPipeUtils.getStreamUrl] function which reports exceptions
     */
    private fun findUrlOrNull(
        format: PlayerResponse.StreamingData.Format,
        videoId: String
    ): String? {
        return NewPipeUtils.getStreamUrl(format, videoId)
            .onFailure {
                Timber.e("SimplePlayer findUrlOrNull Could not get stream url: $videoId")
                println("SimplePlayer findUrlOrNull Could not get stream url: $videoId")
            }
            .getOrNull()
    }

    /**
     * Wrapper around the [PoTokenGenerator.getWebClientPoToken] function which reports exceptions
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun getWebClientPoTokenOrNull(videoId: String, sessionId: String?): PoTokenResult? {
        if (sessionId == null) {
            Timber.d("SimplePlayer getWebClientPoTokenOrNull [$videoId] Session identifier is null")
            println("SimplePlayer getWebClientPoTokenOrNull [$videoId] Session identifier is null")
            return null
        }
        try {
            return poTokenGenerator.getWebClientPoToken(videoId, sessionId)
        } catch (e: Exception) {
            Timber.e("SimplePlayer getWebClientPoTokenOrNull Could not get web client po token: $videoId")
            println("SimplePlayer getWebClientPoTokenOrNull Could not get web client po token: $videoId")
        }
        return null
    }

}
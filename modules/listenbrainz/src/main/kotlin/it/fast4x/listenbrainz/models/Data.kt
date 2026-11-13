package it.fast4x.listenbrainz.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request per POST /1/submit-listens
 * https://listenbrainz.readthedocs.io/en/latest/users/api/core.html
 */
@Serializable
data class SubmitListensRequest(
    @SerialName("listen_type") val listenType: String, // "single" oppure "playing_now"
    @SerialName("payload") val payload: List<ListenPayload>
)

@Serializable
data class ListenPayload(
    // Unix timestamp (secondi), obbligatorio per "single", assente per "playing_now"
    @SerialName("listened_at") val listenedAt: Long? = null,
    @SerialName("track_metadata") val trackMetadata: TrackMetadata
)

@Serializable
data class TrackMetadata(
    @SerialName("artist_name") val artistName: String,
    @SerialName("track_name") val trackName: String,
    @SerialName("release_name") val releaseName: String? = null,
    @SerialName("additional_info") val additionalInfo: AdditionalInfo? = null
)

@Serializable
data class AdditionalInfo(
    @SerialName("media_player") val mediaPlayer: String? = null,
    @SerialName("submission_client") val submissionClient: String? = null,
    // MSID generato dal client alla submission, usato poi per il feedback (love/unlove)
    @SerialName("recording_msid") val recordingMsid: String? = null
)

/**
 * Request per POST /1/feedback/recording-feedback
 * score: 1 = loved, 0 = rimuove il feedback
 */
@Serializable
data class RecordingFeedbackRequest(
    @SerialName("recording_msid") val recordingMsid: String,
    @SerialName("score") val score: Int
)

/**
 * Response di GET /1/validate-token
 */
@Serializable
data class ValidateTokenResponse(
    @SerialName("code") val code: Int? = null,
    @SerialName("message") val message: String? = null,
    @SerialName("valid") val valid: Boolean = false,
    @SerialName("user_name") val userName: String? = null
)

/**
 * Response generica di POST /1/submit-listens e POST /1/feedback/recording-feedback
 */
@Serializable
data class StatusResponse(
    @SerialName("status") val status: String? = null,
    @SerialName("code") val code: Int? = null,
    @SerialName("message") val message: String? = null
)

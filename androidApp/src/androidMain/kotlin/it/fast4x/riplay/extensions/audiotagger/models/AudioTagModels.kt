package it.fast4x.riplay.extensions.audiotagger.models

import kotlinx.serialization.Serializable

@Serializable
data class ApiInfoResponse(
    val success: Boolean,
    val status: String,
    val api_ver: String,
    val action: String,
    val error: String? = null
)

@Serializable
data class ApiStatsResponse(
    val success: Boolean,
    val expiration_date: String,
    val queries_count: Int,
    val uploaded_duration_sec: Int,
    val uploaded_size_bytes: Long,
    val credits_spent: Int,
    val current_credit_balance: Int,
    val identification_free_sec_remainder: Int,
    val error: String? = null
)

@Serializable
data class IdentifyResponse(
    val success: Boolean,
    val token: String? = null,
    val job_status: String? = null,
    val start_time: Int? = null,
    val time_len: Int? = null,
    val error: String? = null
)

@Serializable
data class TrackInfo(
    val track_name: String,
    val artist_name: String,
    val album_name: String,
    val album_year: Int
)

@Serializable
data class RecognitionResult(
    val confidence: Int,
    val time: String,
    val tracks: List<TrackInfo>
)

@Serializable
data class GetResultResponse(
    val success: Boolean,
    val result: String? = null,
    val data: List<RecognitionResult>? = null,
    val error: String? = null
)

@Serializable
data class IdentifyOfflineStreamResponse(
    val success: Boolean,
    val token: String? = null,
    val status: String? = null,
    val result: String? = null,
    val error: String? = null
)

@Serializable
data class OfflineStreamTrack(
    val time_start: String,
    val time_end: String,
    val track: TrackInfo,
    val confidence: Int
)

@Serializable
data class GetOfflineStreamResultResponse(
    val success: Boolean,
    val result: Map<String, String>? = null,
    val status: String? = null,
    val progress: String? = null,
    val base_time: String? = null,
    val error: String? = null
)
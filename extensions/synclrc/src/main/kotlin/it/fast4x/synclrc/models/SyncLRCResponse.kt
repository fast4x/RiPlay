package it.fast4x.synclrc.models

import kotlinx.serialization.Serializable

@Serializable
data class SyncLRCResponse(
    val artist: String,
    val id: String,
    val lyrics: String,
    val track: String,
    val type: String,
)


@Serializable
data class SyncLRCSearchResponse(
    val results: List<LyricsResult>,
    val total: Int,
    val limit: Int,
    val offset: Int
)


@Serializable
data class LyricsResult(
    val id: String,
    val track: String,
    val artist: String,
    val lyrics: LyricsData
)


@Serializable
data class LyricsData(
    val plain: String? = null,
    val synced: String? = null,
    val karaoke: String? = null
)
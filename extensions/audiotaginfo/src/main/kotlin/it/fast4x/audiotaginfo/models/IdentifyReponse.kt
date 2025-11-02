package it.fast4x.audiotaginfo.models

import kotlinx.serialization.Serializable

@Serializable
data class IdentifyResponse(
    val success: Boolean? = false,
    val token: String? = null,
    val error: String? = null,
    val jobStatus: String? = null,
    val startTime: Int? = 0,
    val timeLen: Int? = 0
)
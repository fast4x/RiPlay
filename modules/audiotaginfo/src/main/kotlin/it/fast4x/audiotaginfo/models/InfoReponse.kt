package it.fast4x.audiotaginfo.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class InfoResponse(
    val success: Boolean? = false,
    val status: String? = null,
    @SerialName("api_ver")
    val apiVersion: String? = null,
    val action: String? = null,
)
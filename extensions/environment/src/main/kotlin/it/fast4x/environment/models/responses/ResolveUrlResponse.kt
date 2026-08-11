package it.fast4x.environment.models.responses

import kotlinx.serialization.*

@Serializable
data class ResolveUrlResponse (
    val responseContext: ResponseContext? = null,
    val endpoint: Endpoint? = null
)

@Serializable
data class Endpoint (
    val clickTrackingParams: String? = null,
    val commandMetadata: CommandMetadata? = null,
    val browseEndpoint: BrowseEndpoint? = null
)

@Serializable
data class BrowseEndpoint (
    val browseId: String? = null,
    val params: String? = null
)

@Serializable
data class CommandMetadata (
    val webCommandMetadata: WebCommandMetadata? = null,

    @SerialName("resolveUrlCommandMetadata")
    val resolveURLCommandMetadata: ResolveURLCommandMetadata? = null
)

@Serializable
data class ResolveURLCommandMetadata (
    @SerialName("isVanityUrl")
    val isVanityURL: Boolean? = null
)

@Serializable
data class WebCommandMetadata (
    val url: String? = null,
    val webPageType: String? = null,
    val rootVe: Long? = null,

    @SerialName("apiUrl")
    val apiURL: String? = null
)


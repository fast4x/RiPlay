package it.fast4x.lrclib.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.abs
import kotlin.time.Duration

@Serializable
data class Track(
    val id: Long,
    val name: String,
    val trackName: String,
    val artistName: String,
    val albumName: String,
    @SerialName("duration") val tDuration: JsonElement,
    val instrumental: Boolean,
    val plainLyrics: String?,
    val syncedLyrics: String?
){
    val duration: Long
        get() = (tDuration as? JsonPrimitive)?.toString()?.substringBefore(".")?.toLong() ?:
                (tDuration as JsonArray).first().jsonPrimitive.toString().substringBefore(".").toLong()
}


internal fun List<Track>.bestMatchingFor(title: String, duration: Duration) =
    firstOrNull { it.duration == duration.inWholeSeconds }
        ?: minByOrNull { abs(it.trackName.length - title.length) }

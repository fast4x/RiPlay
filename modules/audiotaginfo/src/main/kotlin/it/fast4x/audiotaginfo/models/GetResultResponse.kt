package it.fast4x.audiotaginfo.models

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class GetResultResponse(
    val success: Boolean,
    val error: String,
    @SerialName("result")
    val jobStatus: String,
    val data: List<DetailedResult?>? = null,
    //val raw: List<RecognitionResult>
)

@Serializable
data class DetailedResult(
    val time: String,
    val confidence: Int,
    @Serializable(with = TrackListSerializer::class)
    val tracks: List<Track>,
    val aid: Long
)

@Serializable
data class Track(
    val title: String,
    val artist: String,
    val album: String,
    val year: Int
)

object TrackListSerializer : KSerializer<List<Track>> {
    override val descriptor: SerialDescriptor = ListSerializer(Track.serializer()).descriptor

    override fun deserialize(decoder: Decoder): List<Track> {
        val input = decoder as? JsonDecoder ?: throw SerializationException("Expected JsonDecoder")
        val jsonArray = input.decodeJsonElement().jsonArray

        // L'espressione 'when' aiuta il compilatore a inferire correttamente i tipi
        return jsonArray.map { element ->
            val trackArray = element.jsonArray
            when (trackArray.size) {
                4 -> Track(
                    title = trackArray[0].jsonPrimitive.content,
                    artist = trackArray[1].jsonPrimitive.content,
                    album = trackArray[2].jsonPrimitive.content,
                    year = trackArray[3].jsonPrimitive.int
                )
                else -> throw SerializationException("Invalid track array format: $trackArray")
            }
        }
    }

    override fun serialize(encoder: Encoder, value: List<Track>) {
        throw NotImplementedError("Serialization of TrackList is not required for this use case")
    }
}
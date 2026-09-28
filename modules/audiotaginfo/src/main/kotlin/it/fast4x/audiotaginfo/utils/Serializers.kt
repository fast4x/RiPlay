package it.fast4x.audiotaginfo.utils

import io.ktor.http.Url
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.LocalDateTime

object UrlSerializer : KSerializer<Url> {
    override val descriptor = PrimitiveSerialDescriptor("Url", PrimitiveKind.STRING)
    override fun deserialize(decoder: Decoder) = Url(decoder.decodeString())
    override fun serialize(encoder: Encoder, value: Url) = encoder.encodeString(value.toString())
}

typealias SerializableUrl = @Serializable(with = UrlSerializer::class) Url

object Iso8601DateSerializer : KSerializer<LocalDateTime> {
    override val descriptor = PrimitiveSerialDescriptor("Iso8601LocalDateTime", PrimitiveKind.STRING)
    override fun deserialize(decoder: Decoder) = LocalDateTime.parse(decoder.decodeString().removeSuffix("Z"))
    override fun serialize(encoder: Encoder, value: LocalDateTime) = encoder.encodeString(value.toString())
}

typealias SerializableIso8601Date = @Serializable(with = Iso8601DateSerializer::class) LocalDateTime

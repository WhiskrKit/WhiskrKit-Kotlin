package eu.whiskrkit.core.serialization

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * The single Json configuration used for everything that crosses the wire or is
 * persisted. `ignoreUnknownKeys` keeps old SDK versions tolerant of new backend
 * fields; `explicitNulls = false` omits null fields, matching the iOS SDK's
 * `encodeIfPresent` behaviour.
 */
@OptIn(ExperimentalSerializationApi::class)
internal val WireJson: Json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}

/**
 * ISO-8601 instant serializer matching Swift's `.iso8601` strategy
 * (`2026-06-11T12:00:00Z`, second precision, UTC).
 */
internal object IsoInstantSerializer : KSerializer<Instant> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("eu.whiskrkit.IsoInstant", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Instant) {
        encoder.encodeString(DateTimeFormatter.ISO_INSTANT.format(value.truncatedTo(ChronoUnit.SECONDS)))
    }

    override fun deserialize(decoder: Decoder): Instant = Instant.parse(decoder.decodeString())
}

internal typealias IsoInstant = @Serializable(with = IsoInstantSerializer::class) Instant

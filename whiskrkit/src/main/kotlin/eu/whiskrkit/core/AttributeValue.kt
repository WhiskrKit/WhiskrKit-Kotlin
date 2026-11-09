package eu.whiskrkit.core

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull

/**
 * A survey attribute value. The backend accepts a string, a number or a boolean.
 * Integers stay integers on the wire (`3`, not `3.0`).
 */
internal sealed interface AttributeValue {
    data class Text(val value: String) : AttributeValue
    data class Integer(val value: Long) : AttributeValue
    data class Decimal(val value: Double) : AttributeValue
    data class Bool(val value: Boolean) : AttributeValue

    companion object {
        /**
         * Converts a supported Kotlin value, or returns null for unsupported types
         * and for non-finite numbers (NaN, Infinity).
         */
        fun of(value: Any?): AttributeValue? = when (value) {
            is String -> Text(value)
            is Boolean -> Bool(value)
            is Int -> Integer(value.toLong())
            is Long -> Integer(value)
            is Short -> Integer(value.toLong())
            is Byte -> Integer(value.toLong())
            is Double -> if (value.isFinite()) Decimal(value) else null
            // Via the shortest decimal form so 1.1f stays 1.1, not 1.100000023841858.
            is Float -> if (value.isFinite()) Decimal(value.toString().toDouble()) else null
            else -> null
        }
    }
}

internal object AttributeValueSerializer : KSerializer<AttributeValue> {
    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("eu.whiskrkit.AttributeValue")

    override fun serialize(encoder: Encoder, value: AttributeValue) {
        val jsonEncoder = encoder as? JsonEncoder
            ?: throw SerializationException("AttributeValue supports JSON only")
        val element = when (value) {
            is AttributeValue.Text -> JsonPrimitive(value.value)
            is AttributeValue.Integer -> JsonPrimitive(value.value)
            is AttributeValue.Decimal -> {
                if (!value.value.isFinite()) throw SerializationException("Attribute numbers must be finite")
                JsonPrimitive(value.value)
            }
            is AttributeValue.Bool -> JsonPrimitive(value.value)
        }
        jsonEncoder.encodeJsonElement(element)
    }

    override fun deserialize(decoder: Decoder): AttributeValue {
        val jsonDecoder = decoder as? JsonDecoder
            ?: throw SerializationException("AttributeValue supports JSON only")
        val element = jsonDecoder.decodeJsonElement()
        val primitive = element as? JsonPrimitive
        if (primitive == null || element is JsonNull) {
            throw SerializationException("Attribute value must be a string, number or boolean")
        }
        if (primitive.isString) return AttributeValue.Text(primitive.content)
        primitive.booleanOrNull?.let { return AttributeValue.Bool(it) }
        primitive.longOrNull?.let { return AttributeValue.Integer(it) }
        primitive.doubleOrNull?.let { return AttributeValue.Decimal(it) }
        throw SerializationException("Unsupported attribute value")
    }
}

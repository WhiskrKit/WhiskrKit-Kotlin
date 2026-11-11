package eu.whiskrkit.core.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull

/**
 * Branding the server asks the SDK to render in a survey. The server decides
 * (from the account's entitlements); the SDK only renders what it is told.
 *
 * Decoding never fails: anything other than `{ "visible": true }` (a missing
 * key, `null`, a wrong type, a malformed `visible`) means the badge stays
 * hidden, so a malformed `branding` can neither break the survey nor show the
 * badge.
 */
@Serializable(with = BrandingSerializer::class)
internal data class Branding(
    /** Whether the "Powered by WhiskrKit" badge is shown. */
    val visible: Boolean = false,
)

/** True only when the server explicitly asked for the badge. */
internal val Branding?.isVisible: Boolean get() = this?.visible == true

/** Reads `branding` as raw JSON so a wrong type can't fail the survey around it. */
internal object BrandingSerializer : KSerializer<Branding> {
    private val delegate = BrandingSurrogate.serializer()

    override val descriptor: SerialDescriptor = delegate.descriptor

    override fun deserialize(decoder: Decoder): Branding {
        // Without a JsonDecoder there's no wire data to be lenient about.
        val element = (decoder as? JsonDecoder)?.decodeJsonElement() ?: return Branding()
        val visible = ((element as? JsonObject)?.get("visible") as? JsonPrimitive)
            ?.takeUnless { it.isString }
            ?.booleanOrNull
        return Branding(visible = visible == true)
    }

    override fun serialize(encoder: Encoder, value: Branding) {
        encoder.encodeSerializableValue(delegate, BrandingSurrogate(value.visible))
    }
}

@Serializable
private class BrandingSurrogate(val visible: Boolean)

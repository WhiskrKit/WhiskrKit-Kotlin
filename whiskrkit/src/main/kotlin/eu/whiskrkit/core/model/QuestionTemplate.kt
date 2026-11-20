package eu.whiskrkit.core.model

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * A single question inside a survey; the wire discriminator is the
 * `template` field.
 *
 * The inconsistent `A11yLabel` / `a11yHint` JSON key casing is intentional —
 * it matches the backend contract byte-for-byte.
 */
@Serializable(with = QuestionTemplateSerializer::class)
internal sealed interface QuestionTemplate {
    val id: String
    val isRequired: Boolean
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
internal data class ScaleRatingTemplate(
    override val id: String,
    val title: String? = null,
    val subtitle: String? = null,
    val ratingRange: RatingRange,
    override val isRequired: Boolean,
    @SerialName("A11yLabel") val a11yLabel: String? = null,
    @SerialName("A11yHint") val a11yHint: String? = null,
    /**
     * The metric the server calculates from the score. It sets the button colors
     * and the default end labels; the SDK only sends the chosen number.
     */
    val metric: RatingMetric = RatingMetric.CUSTOM,
    /** How the server asks for the scale to be shown; render [resolvedDisplayStyle]. */
    val displayStyle: RatingDisplayStyle = RatingDisplayStyle.NUMBERS,
    /** End labels for a `custom` metric; null for the presets, which use the SDK's own. */
    val minLabel: String? = null,
    val maxLabel: String? = null,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS) val template: String = "scaleRating",
) : QuestionTemplate {
    @Serializable
    data class RatingRange(val min: Int, val max: Int) {
        val values: List<Int> get() = (min..max).toList()
    }

    /** Stars only fit a 1 to 5 range; any other range is shown as numbers. */
    val resolvedDisplayStyle: RatingDisplayStyle
        get() = if (displayStyle == RatingDisplayStyle.STARS && ratingRange == RatingRange(1, 5)) {
            RatingDisplayStyle.STARS
        } else {
            RatingDisplayStyle.NUMBERS
        }
}

/** Unknown values from a newer server decode as [CUSTOM]. */
@Serializable(with = RatingMetricSerializer::class)
internal enum class RatingMetric(val wireName: String) {
    NPS("nps"),
    CSAT("csat"),
    CES("ces"),
    CUSTOM("custom"),
    ;

    companion object {
        fun fromWire(value: String?): RatingMetric = entries.firstOrNull { it.wireName == value } ?: CUSTOM
    }
}

/** Unknown values from a newer server decode as [NUMBERS]. */
@Serializable(with = RatingDisplayStyleSerializer::class)
internal enum class RatingDisplayStyle(val wireName: String) {
    NUMBERS("numbers"),
    STARS("stars"),
    ;

    companion object {
        fun fromWire(value: String?): RatingDisplayStyle = entries.firstOrNull { it.wireName == value } ?: NUMBERS
    }
}

/**
 * Decodes an enum from its wire name without ever failing: a value this SDK
 * version doesn't know, a null or a value of the wrong type all become the
 * fallback in [parse]. `ignoreUnknownKeys` covers new fields, but not new
 * values of a known field.
 */
internal abstract class LenientEnumSerializer<T : Enum<T>>(
    serialName: String,
    private val parse: (String?) -> T,
    private val wireName: (T) -> String,
) : KSerializer<T> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(serialName, PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: T) = encoder.encodeString(wireName(value))

    override fun deserialize(decoder: Decoder): T {
        val jsonDecoder = decoder as? JsonDecoder ?: return parse(decoder.decodeString())
        val primitive = jsonDecoder.decodeJsonElement() as? JsonPrimitive
        return parse(primitive?.takeIf { it.isString }?.content)
    }
}

internal object RatingMetricSerializer :
    LenientEnumSerializer<RatingMetric>("eu.whiskrkit.RatingMetric", RatingMetric::fromWire, RatingMetric::wireName)

internal object RatingDisplayStyleSerializer :
    LenientEnumSerializer<RatingDisplayStyle>("eu.whiskrkit.RatingDisplayStyle", RatingDisplayStyle::fromWire, RatingDisplayStyle::wireName)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
internal data class SymbolRatingTemplate(
    override val id: String,
    val title: String? = null,
    val description: String? = null,
    override val isRequired: Boolean,
    @SerialName("A11yLabel") val a11yLabel: String? = null,
    @SerialName("A11yHint") val a11yHint: String? = null,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS) val template: String = "symbolRating",
) : QuestionTemplate

@OptIn(ExperimentalSerializationApi::class)
@Serializable
internal data class ThumbsSurveyTemplate(
    override val id: String,
    val title: String? = null,
    val subtitle: String? = null,
    override val isRequired: Boolean,
    @SerialName("A11yLabel") val a11yLabel: String? = null,
    @SerialName("A11yHint") val a11yHint: String? = null,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS) val template: String = "thumbsRating",
) : QuestionTemplate

@OptIn(ExperimentalSerializationApi::class)
@Serializable
internal data class TextSurveyTemplate(
    override val id: String,
    val title: String? = null,
    val description: String? = null,
    val maxLength: Int? = null,
    override val isRequired: Boolean,
    @SerialName("A11yLabel") val a11yLabel: String? = null,
    @SerialName("a11yHint") val a11yHint: String? = null,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS) val template: String = "textualSurvey",
) : QuestionTemplate {
    companion object {
        const val DEFAULT_MAX_LENGTH: Int = 200
    }

    val characterLimit: Int get() = maxLength ?: DEFAULT_MAX_LENGTH
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
internal data class MultipleChoiceTemplate(
    override val id: String,
    val title: String? = null,
    val subtitle: String? = null,
    override val isRequired: Boolean,
    val options: List<MultipleChoiceOption>,
    val allowsMultiSelection: Boolean,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS) val template: String = "multipleChoice",
) : QuestionTemplate

@Serializable
internal data class MultipleChoiceOption(
    val id: String,
    val label: String,
)

/**
 * Lenient-decoding fallback: question types this SDK version does not
 * understand decode to this placeholder instead of failing the template.
 * Validation in [validated] decides whether the survey can still be shown.
 */
@Serializable
internal data class UnknownQuestionTemplate(
    override val id: String = "",
    override val isRequired: Boolean = false,
) : QuestionTemplate

internal object QuestionTemplateSerializer :
    JsonContentPolymorphicSerializer<QuestionTemplate>(QuestionTemplate::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<QuestionTemplate> =
        when (element.jsonObject["template"]?.jsonPrimitive?.contentOrNull) {
            "scaleRating" -> ScaleRatingTemplate.serializer()
            "symbolRating" -> SymbolRatingTemplate.serializer()
            "thumbsRating" -> ThumbsSurveyTemplate.serializer()
            "textualSurvey" -> TextSurveyTemplate.serializer()
            "multipleChoice" -> MultipleChoiceTemplate.serializer()
            else -> UnknownQuestionTemplate.serializer()
        }
}

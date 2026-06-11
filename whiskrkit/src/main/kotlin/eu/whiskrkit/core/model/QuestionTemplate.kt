package eu.whiskrkit.core.model

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * A single question inside a survey. Mirrors the iOS `SurveyPresentation.SurveyBase`
 * union; the wire discriminator is the `template` field.
 *
 * The inconsistent `A11yLabel` / `a11yHint` JSON key casing is intentional —
 * it matches the backend contract byte-for-byte (decision P3).
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
    @EncodeDefault(EncodeDefault.Mode.ALWAYS) val template: String = "scaleRating",
) : QuestionTemplate {
    @Serializable
    data class RatingRange(val min: Int, val max: Int) {
        val values: List<Int> get() = (min..max).toList()
    }
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
internal data class SymbolRatingTemplate(
    override val id: String,
    val title: String? = null,
    val description: String? = null,
    /** Decoded but unimplemented, matching iOS (decision P5 / #8). */
    val opensStoreReview: Boolean = false,
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
 * Lenient-decoding fallback (decision B1): question types this SDK version does
 * not understand decode to this placeholder instead of failing the template.
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

package eu.whiskrkit.core.model

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Top-level survey template — one of three presentation modes, discriminated by
 * the `template` wire field (`sheet` / `fullScreenForm` / `toast`).
 *
 * The wire value `toast` is fixed by the backend; on Android the concept is
 * called "banner" in code and docs to avoid confusion with `android.widget.Toast`
 * (decision #3).
 */
@Serializable(with = SurveyTemplateSerializer::class)
internal sealed interface SurveyTemplate {
    val id: String
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
internal data class SheetTemplate(
    override val id: String,
    val title: String? = null,
    val description: String? = null,
    val followUpQuestion: String? = null,
    val survey: QuestionTemplate,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS) val template: String = "sheet",
) : SurveyTemplate

@OptIn(ExperimentalSerializationApi::class)
@Serializable
internal data class FullScreenFormTemplate(
    override val id: String,
    val title: String? = null,
    val subtitle: String? = null,
    val description: String? = null,
    val surveys: List<QuestionTemplate>,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS) val template: String = "fullScreenForm",
) : SurveyTemplate

@OptIn(ExperimentalSerializationApi::class)
@Serializable
internal data class BannerTemplate(
    override val id: String,
    val title: String? = null,
    val description: String? = null,
    val followUpIdentifier: String? = null,
    val survey: QuestionTemplate? = null,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS) val template: String = "toast",
) : SurveyTemplate

internal object SurveyTemplateSerializer :
    JsonContentPolymorphicSerializer<SurveyTemplate>(SurveyTemplate::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<SurveyTemplate> =
        when (val kind = element.jsonObject["template"]?.jsonPrimitive?.contentOrNull) {
            "sheet" -> SheetTemplate.serializer()
            "fullScreenForm" -> FullScreenFormTemplate.serializer()
            "toast" -> BannerTemplate.serializer()
            else -> throw SerializationException("Unknown presentation template '$kind'")
        }
}

/**
 * Applies the lenient-decoding rules (decision B1):
 * - unknown *optional* questions are silently dropped,
 * - an unknown *required* question invalidates the whole template (a submission
 *   missing a required answer would be incomplete from the backend's view),
 * - a sheet or banner whose single question is unknown is invalid,
 * - a full-screen form with no remaining questions is invalid.
 *
 * Returns the cleaned template, or null if the survey cannot be shown.
 */
internal fun SurveyTemplate.validated(): SurveyTemplate? = when (this) {
    is SheetTemplate -> takeUnless { survey is UnknownQuestionTemplate }
    is BannerTemplate -> takeUnless { survey is UnknownQuestionTemplate }
    is FullScreenFormTemplate -> when {
        surveys.any { it is UnknownQuestionTemplate && it.isRequired } -> null
        else -> copy(surveys = surveys.filterNot { it is UnknownQuestionTemplate })
            .takeIf { it.surveys.isNotEmpty() }
    }
}

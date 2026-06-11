package eu.whiskrkit.core.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * The user's answers, keyed by question id. Wire format matches the iOS
 * `SurveyResponse`: each answer is a single-key object tagged with its type,
 * e.g. `{"results": {"q1": {"npsRating": 7}, "q2": {"textualSurvey": "..."}}}`.
 */
@Serializable
internal data class SurveyResponse(
    val results: Map<String, SurveyAnswer> = emptyMap(),
)

@Serializable(with = SurveyAnswerSerializer::class)
internal sealed interface SurveyAnswer {
    data class NpsRating(val score: Int) : SurveyAnswer
    data class SymbolRating(val score: Int) : SurveyAnswer
    data class Thumbs(val rating: ThumbsRating) : SurveyAnswer
    data class Text(val feedback: String) : SurveyAnswer
    data class MultipleChoice(val optionIds: List<String>) : SurveyAnswer
}

@Serializable
internal enum class ThumbsRating {
    @SerialName("thumbsUp")
    THUMBS_UP,

    @SerialName("thumbsDown")
    THUMBS_DOWN,

    @SerialName("none")
    NONE,
}

internal object SurveyAnswerSerializer : KSerializer<SurveyAnswer> {
    private const val NPS = "npsRating"
    private const val SYMBOL = "symbolRating"
    private const val THUMBS = "thumbsRating"
    private const val TEXT = "textualSurvey"
    private const val MULTIPLE_CHOICE = "multipleChoice"

    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("eu.whiskrkit.SurveyAnswer")

    override fun serialize(encoder: Encoder, value: SurveyAnswer) {
        val jsonEncoder = encoder as? JsonEncoder
            ?: throw SerializationException("SurveyAnswer supports JSON only")
        val element = when (value) {
            is SurveyAnswer.NpsRating -> buildJsonObject { put(NPS, value.score) }
            is SurveyAnswer.SymbolRating -> buildJsonObject { put(SYMBOL, value.score) }
            is SurveyAnswer.Thumbs -> buildJsonObject {
                put(THUMBS, jsonEncoder.json.encodeToJsonElement(ThumbsRating.serializer(), value.rating))
            }
            is SurveyAnswer.Text -> buildJsonObject { put(TEXT, value.feedback) }
            is SurveyAnswer.MultipleChoice -> buildJsonObject {
                put(MULTIPLE_CHOICE, JsonArray(value.optionIds.map(::JsonPrimitive)))
            }
        }
        jsonEncoder.encodeJsonElement(element)
    }

    override fun deserialize(decoder: Decoder): SurveyAnswer {
        val jsonDecoder = decoder as? JsonDecoder
            ?: throw SerializationException("SurveyAnswer supports JSON only")
        val obj = jsonDecoder.decodeJsonElement().jsonObject
        return when {
            NPS in obj -> SurveyAnswer.NpsRating(obj.getValue(NPS).jsonPrimitive.int)
            SYMBOL in obj -> SurveyAnswer.SymbolRating(obj.getValue(SYMBOL).jsonPrimitive.int)
            THUMBS in obj -> SurveyAnswer.Thumbs(
                jsonDecoder.json.decodeFromJsonElement(ThumbsRating.serializer(), obj.getValue(THUMBS)),
            )
            TEXT in obj -> SurveyAnswer.Text(obj.getValue(TEXT).jsonPrimitive.content)
            MULTIPLE_CHOICE in obj -> SurveyAnswer.MultipleChoice(
                obj.getValue(MULTIPLE_CHOICE).jsonArray.map { it.jsonPrimitive.content },
            )
            else -> throw SerializationException("No valid survey answer type found")
        }
    }
}

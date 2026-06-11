package eu.whiskrkit.core.model

import eu.whiskrkit.core.serialization.WireJson
import kotlinx.serialization.SerializationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * Wire-format tests: the encoded JSON must match the iOS `SurveyResponse`
 * encoding byte-for-byte — single-key tagged objects per answer.
 */
class SurveyResponseSerializationTest {

    private fun encode(response: SurveyResponse): String =
        WireJson.encodeToString(SurveyResponse.serializer(), response)

    private fun decode(json: String): SurveyResponse =
        WireJson.decodeFromString(SurveyResponse.serializer(), json)

    @Test
    fun `nps rating encodes as npsRating key`() {
        val response = SurveyResponse(mapOf("q1" to SurveyAnswer.NpsRating(7)))
        assertEquals("""{"results":{"q1":{"npsRating":7}}}""", encode(response))
    }

    @Test
    fun `symbol rating encodes as symbolRating key`() {
        val response = SurveyResponse(mapOf("q1" to SurveyAnswer.SymbolRating(4)))
        assertEquals("""{"results":{"q1":{"symbolRating":4}}}""", encode(response))
    }

    @Test
    fun `thumbs rating encodes as raw enum string`() {
        val response = SurveyResponse(mapOf("q1" to SurveyAnswer.Thumbs(ThumbsRating.THUMBS_UP)))
        assertEquals("""{"results":{"q1":{"thumbsRating":"thumbsUp"}}}""", encode(response))
    }

    @Test
    fun `text encodes as textualSurvey key`() {
        val response = SurveyResponse(mapOf("q1" to SurveyAnswer.Text("hello")))
        assertEquals("""{"results":{"q1":{"textualSurvey":"hello"}}}""", encode(response))
    }

    @Test
    fun `multiple choice encodes option ids as array`() {
        val response = SurveyResponse(
            mapOf("q1" to SurveyAnswer.MultipleChoice(listOf("a", "b"))),
        )
        assertEquals("""{"results":{"q1":{"multipleChoice":["a","b"]}}}""", encode(response))
    }

    @Test
    fun `all answer types round-trip`() {
        val original = SurveyResponse(
            mapOf(
                "nps" to SurveyAnswer.NpsRating(9),
                "stars" to SurveyAnswer.SymbolRating(5),
                "thumbs" to SurveyAnswer.Thumbs(ThumbsRating.THUMBS_DOWN),
                "text" to SurveyAnswer.Text("feedback with émoji 🎉"),
                "choice" to SurveyAnswer.MultipleChoice(listOf("opt-1")),
            ),
        )
        assertEquals(original, decode(encode(original)))
    }

    @Test
    fun `unknown answer key fails decoding`() {
        assertThrows(SerializationException::class.java) {
            decode("""{"results":{"q1":{"emojiRating":3}}}""")
        }
    }

    @Test
    fun `empty results round-trips`() {
        assertEquals(SurveyResponse(), decode(encode(SurveyResponse())))
    }
}
